# System Design: Notification System

> **Interview Format:** 45-minute Senior/Staff Engineer system design  
> **Difficulty:** Medium-Hard  
> **Core Themes:** Fan-out architecture, message queues, multi-channel delivery, idempotency

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [Clarifying Questions](#2-clarifying-questions)
3. [Functional Requirements](#3-functional-requirements)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [Capacity Estimation](#5-capacity-estimation)
6. [High-Level Design](#6-high-level-design)
7. [Deep Dives](#7-deep-dives)
8. [Trade-offs and Alternatives](#8-trade-offs-and-alternatives)
9. [Failure Scenarios](#9-failure-scenarios)
10. [Interview Tips](#10-interview-tips)

---

## 1. Problem Statement

Design a notification system that can send messages to users across multiple channels: push notifications (iOS/Android), SMS, email, and in-app notifications. The system must handle 10 million notifications per day to 1 million daily active users.

This is a rich design problem because notifications touch almost every part of a modern application. The generating event (a like, a comment, a payment) triggers a notification. The system must figure out: which channel to use? What template to render? Has this user already received this notification (deduplication)? Are we sending too many notifications to this user (rate limiting)? Did the notification actually get delivered? If not, retry?

At first glance, a notification system sounds simple: "just send an email." The complexity emerges when you think through:
- A user has 3 devices, preferences for morning-only push, no SMS
- The notification is "payment received" — it must deliver even at 3am
- A bug in your code sends the same notification 1,000 times
- APNs (Apple Push Notification service) goes down for 2 hours
- Your system needs to send 2 million notifications in 30 seconds for a flash sale

These are the problems you need to address. The architecture patterns here — fan-out, priority queues, idempotency, retry with exponential backoff — are broadly applicable to any async distributed system.

---

## 2. Clarifying Questions

**Q1: What notification channels do we need to support?**

> Expected answer: Push notifications (APNs for iOS, FCM for Android), SMS (Twilio), email (SendGrid), and in-app notifications (read from API).

Why this matters: Four channels = four different external service integrations, each with their own rate limits, delivery semantics, and failure modes. This tells you the architecture must be extensible — adding a fifth channel (WhatsApp, Slack) should not require a redesign.

**Q2: Who generates notifications — do upstream services call our system directly, or do we consume events?**

> Expected answer: Upstream services publish events to a shared event bus (Kafka/SNS). The notification system consumes these events and decides how to notify the user.

Why this matters: Event-driven decoupling is the right answer. If upstream services call the notification system directly, every service that needs to send a notification becomes tightly coupled to it. Event-based design means the notification system can be updated, redeployed, or replaced without touching upstream services.

**Q3: Are all notifications equal in priority, or do some need to be delivered faster?**

> Expected answer: Yes — critical notifications (payment alerts, security alerts, account compromised) must deliver within seconds. Non-critical (marketing, recommendations) can be delayed by minutes.

Why this matters: This immediately tells you that you need multiple queue tiers. A critical payment alert should not be stuck behind a queue of 10 million marketing emails. Priority queues are a central architectural requirement.

**Q4: How does the user control their notification preferences?**

> Expected answer: Users can set channel preferences (push only, no SMS), time-of-day preferences (no notifications 10pm-8am), and category opt-outs (unsubscribe from marketing).

Why this matters: Every notification must pass through a preference check before being sent. This preference lookup must be fast (cached in Redis) and must be checked before any expensive external API calls (sending SMS costs money).

**Q5: Do we need delivery receipts — knowing whether a notification was actually delivered?**

> Expected answer: Yes. For critical notifications, we need delivery acknowledgment. For marketing notifications, best-effort delivery is fine.

Why this matters: Delivery receipts require a callback mechanism from APNs/FCM/Twilio back to your system. This adds complexity but is essential for billing, compliance (GDPR requires consent and delivery tracking), and SLA guarantees.

**Q6: What should the system do if a notification fails to deliver — retry or drop?**

> Expected answer: Retry with exponential backoff for up to 24 hours. After 24 hours, mark as undeliverable and alert the relevant service. For push notifications, fall back to email if push fails after 3 attempts.

Why this matters: Retry logic is the difference between a system that works in theory and one that works in production. External services fail — APNs has outages, Twilio rate limits you. Your retry design must handle this gracefully.

**Q7: Is deduplication required? What if our system crashes and restarts mid-processing?**

> Expected answer: Yes. Each notification has a unique `notification_id`. If we receive the same event twice, we should not send the notification twice.

Why this matters: Exactly-once delivery is impossible in distributed systems. At-most-once loses notifications. At-least-once + deduplication gives you exactly-once semantics. This is the standard pattern. The deduplication store must be cheap and fast (Redis with TTL).

---

## 3. Functional Requirements

1. **Multi-channel delivery:** Send push (APNs, FCM), SMS (Twilio), email (SendGrid), and in-app notifications.
2. **Event-driven trigger:** Consume events from Kafka and decide which notifications to send.
3. **User preference management:** Respect per-user channel preferences, notification categories, and quiet hours.
4. **Template rendering:** Render notification content from templates with user-specific data.
5. **Priority tiers:** Critical notifications (security, payment) bypass queues and deliver within seconds.
6. **Retry with fallback:** Retry failed deliveries with exponential backoff; fall back across channels if primary channel fails repeatedly.
7. **Deduplication:** Idempotent delivery — the same notification_id is never sent twice.
8. **Delivery tracking:** Track delivery status (sent, delivered, failed) per notification per channel.
9. **Rate limiting:** Limit notifications per user per day per category to avoid spam.
10. **Unsubscribe handling:** Honor unsubscribe requests across all channels immediately.

Out of scope:
- Two-way messaging (SMS replies, email replies)
- Notification scheduling (schedule a notification for a future time — noted as extension)
- Notification templates with images/media (noted as extension)

---

## 4. Non-Functional Requirements

1. **Latency:** Critical notifications delivered within 5 seconds of event receipt. Non-critical within 5 minutes.
2. **Reliability:** No notification loss. All notifications are persisted before being sent. Failed notifications are retried.
3. **Scale:** Handle 10M notifications/day (115 notifications/sec average). Handle spikes of 2M notifications in 30 minutes (1,111/sec).
4. **Deduplication window:** Idempotency guaranteed for 24 hours. After 24 hours, duplicate events are assumed to be legitimate retry events.
5. **Preference freshness:** User preference changes must take effect within 60 seconds.
6. **Extensibility:** Adding a new channel (WhatsApp, Slack DM) should require only a new channel handler, not an architecture change.
7. **Compliance:** GDPR — maintain records of notification consent and delivery history for 2 years. CAN-SPAM — honor unsubscribes within 10 business days (our system does it instantly).

---

## 5. Capacity Estimation

### Notification Volume

```
Total notifications per day:      10,000,000 (10M)
Seconds per day:                  86,400
Average throughput:               10M / 86,400 ≈ 116 notifications/sec
Peak throughput (flash sale):     2M notifications in 30 min
                                  = 2M / 1,800 ≈ 1,111 notifications/sec
```

### Channel Distribution (estimated)

```
Push notifications (APNs + FCM):  60% of volume = 6M/day
Email (SendGrid):                 30% of volume = 3M/day
In-app:                           8% of volume  = 800K/day
SMS (Twilio):                     2% of volume  = 200K/day
```

### Storage

**Notification log (delivery history):**
```
Each notification record:         ~500 bytes
                                  (notification_id, user_id, channel, template_id, 
                                   status, created_at, delivered_at, content_hash)
Records per day:                  10M
Storage per day:                  10M × 500 bytes = 5 GB/day
Storage per year:                 5 GB × 365 = ~1.8 TB/year
```

A time-series optimized store (Cassandra or ClickHouse) handles this well. Hot data (last 30 days) kept on fast SSD storage. Cold data archived to S3 with Parquet for analytical queries.

**Deduplication store (Redis):**
```
Deduplication window:             24 hours
Notifications per 24 hours:       10M
Redis key per notification:       notification_id (UUID) → "1"
Size per key:                     ~80 bytes (UUID + overhead)
Total Redis memory for dedup:     10M × 80 bytes = 800 MB
```

800MB fits in a single Redis instance with plenty of headroom. Use a Redis Cluster with 3 nodes for HA.

**User preference store:**
```
Users:                            Let's say 50M registered users
Preference record per user:       ~200 bytes (channel preferences, quiet hours, category opts)
Total preference storage:         50M × 200 bytes = 10 GB
```

Preferences are small, frequently read, rarely written. Redis is an excellent store for this — 10 GB fits in a single node, and preferences can be cached locally on each notification worker.

### Worker Capacity

```
Peak throughput needed:           1,111 notifications/sec
Processing time per notification: ~50ms (preference check + template render + channel API call)
Workers needed:                   1,111 / (1000ms / 50ms) = 1,111 / 20 = ~56 workers
With 2x buffer:                   ~112 worker instances
```

These are lightweight workers. Each runs a Kafka consumer, does a Redis lookup, renders a template, and calls an external API. 112 workers in a Kubernetes deployment auto-scaled based on Kafka consumer lag.

---

## 6. High-Level Design

### Architecture Overview

```
                 ┌───────────────────────────────────────────────────────────────────────┐
                 │                        UPSTREAM SERVICES                              │
                 │   (Payment Service, Social Service, Auth Service, Marketing Service)  │
                 └──────────────────────────────────┬────────────────────────────────────┘
                                                    │ Publish events
                                                    ▼
                 ┌───────────────────────────────────────────────────────────────────────┐
                 │                          Kafka Event Bus                              │
                 │  Topics:                                                              │
                 │    notifications.critical  (payment, security) — high priority        │
                 │    notifications.standard  (social, system)    — normal priority      │
                 │    notifications.marketing (promos, recs)      — low priority         │
                 └────────┬────────────────────────────────────────────────────────────  ┘
                          │
                          ▼
                 ┌──────────────────────────────────────────────────────────────────────┐
                 │                   Notification Dispatcher Service                    │
                 │   (Kafka Consumer — one consumer group per priority tier)            │
                 │                                                                      │
                 │   1. Consume event from Kafka                                        │
                 │   2. Deduplication check (Redis)                                     │
                 │   3. Load user notification preferences (Redis cache)                │
                 │   4. Check rate limits (Redis sliding window)                        │
                 │   5. Route to appropriate channel queue(s)                           │
                 │   6. Persist notification record to DB (status=PENDING)              │
                 └──────────┬──────────────┬──────────────┬───────────────┬─────────────┘
                            │              │              │               │
           Push Queue       │   SMS Queue  │  Email Queue │  In-App Queue │
                            ▼              ▼              ▼               ▼
           ┌────────────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
           │  Push Worker Pool  │ │  SMS Workers │ │Email Workers │ │ In-App Store │
           │                    │ │              │ │              │ │              │
           │  APNs (iOS)        │ │  Twilio API  │ │  SendGrid    │ │  DynamoDB    │
           │  FCM  (Android)    │ │              │ │  API         │ │  (read by    │
           │  Web Push          │ │              │ │              │ │   app API)   │
           └─────────┬──────────┘ └──────┬───────┘ └──────┬───────┘ └──────────────┘
                     │                   │                 │
                     └──────────┬────────┘                 │
                                │                          │
                                ▼                          ▼
                 ┌───────────────────────────────────────────────────────────────────────┐
                 │                    Notification DB (Cassandra)                        │
                 │   Delivery status updates: PENDING → SENT → DELIVERED / FAILED       │
                 │   Delivery receipt callbacks from APNs/FCM/Twilio stored here         │
                 └───────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
                 ┌───────────────────────────────────────────────────────────────────────┐
                 │                     Support Infra                                     │
                 │                                                                       │
                 │  Redis Cluster          — dedup store, pref cache, rate limit state   │
                 │  Template Service       — renders notification content from templates  │
                 │  Preference Service     — manages user channel/category preferences   │
                 │  Delivery Receipt API   — receives callbacks from APNs/FCM/Twilio     │
                 └───────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

**Notification Dispatcher Service**

This is the brain of the system. It consumes events from Kafka and makes all the decisions:
- Is this a duplicate? (check Redis dedup store)
- What are this user's preferences? (Redis cache of user preferences)
- Is this user rate-limited for this category? (Redis sliding window)
- Which channels should this notification go to? (routing decision)
- What content should the notification have? (template rendering)

The Dispatcher does NOT send the notification itself. It fans out tasks to channel-specific queues. This decoupling lets each channel scale independently and fail independently.

**Channel Workers (Push / SMS / Email)**

Each channel has its own worker pool that reads from its queue and calls the respective external API. Channel workers are responsible for:
- Retry logic (exponential backoff with jitter)
- Channel-specific error handling (APNs token expiry, Twilio rate limit)
- Updating delivery status in the Notification DB
- Publishing delivery events (success/failure) back to Kafka for the Dispatcher to handle fallback

**Template Service**

Decoupled from the Dispatcher for performance. Renders Mustache/Jinja2 templates with user-specific data fetched from a user profile service. Returns rendered strings for each channel (push title+body, SMS text, email HTML+text).

**Preference Service**

Stores and serves user notification preferences. Redis-backed with a DynamoDB persistent layer. Preferences change infrequently (user changes notification settings) but are read on every notification.

**Notification DB (Cassandra)**

Chosen for its write-heavy workload (10M status updates/day) and time-series access patterns (give me all notifications for user X in the last 30 days).

```
Table: notifications (partition by user_id, sort by created_at)
  user_id:         UUID
  notification_id: UUID
  created_at:      timestamp
  channel:         "push" | "sms" | "email" | "in_app"
  status:          "PENDING" | "SENT" | "DELIVERED" | "FAILED"
  template_id:     String
  rendered_content: String (JSON, compressed)
  retry_count:     Int
  delivered_at:    timestamp (null until delivered)
  failure_reason:  String (null unless FAILED)
```

---

## 7. Deep Dives

### 7.1 Channel Routing and Fan-out Architecture

**The Routing Decision**

When the Dispatcher receives an event (e.g., `{type: "payment_received", user_id: "u123", amount: "$50.00"}`), it must decide:
1. Does user u123 want to be notified about this event?
2. Which channels should receive this notification?
3. What content should each channel receive?

This logic lives in a **Routing Engine** within the Dispatcher:

```python
class RoutingEngine:
    def route(self, event: Event) -> List[ChannelTask]:
        user_id = event.user_id
        
        # Step 1: Check if user has opted out of this event category
        prefs = self.preference_cache.get(user_id)
        if event.category in prefs.opted_out_categories:
            return []  # User doesn't want this notification
        
        # Step 2: Check quiet hours
        if prefs.quiet_hours_enabled:
            user_local_time = convert_to_user_timezone(now(), prefs.timezone)
            if prefs.quiet_start <= user_local_time <= prefs.quiet_end:
                if event.priority != Priority.CRITICAL:
                    # Delay non-critical notifications until quiet hours end
                    return [ScheduledTask(event, deliver_at=prefs.quiet_end)]
        
        # Step 3: Determine channels based on event priority and user preferences
        channels = []
        if event.priority == Priority.CRITICAL:
            # Critical: send to ALL channels the user has registered
            channels = prefs.registered_channels  # push + SMS + email
        else:
            # Non-critical: respect user channel preference
            channels = prefs.preferred_channels   # e.g., push only
        
        # Step 4: For each channel, create a task
        tasks = []
        for channel in channels:
            rendered = self.template_service.render(
                template_id=event.template_id,
                channel=channel,
                context=event.data
            )
            tasks.append(ChannelTask(
                notification_id=event.notification_id,
                user_id=user_id,
                channel=channel,
                content=rendered,
                priority=event.priority
            ))
        
        return tasks
```

**Fan-out to Channel Queues**

After routing, the Dispatcher writes to multiple queues simultaneously:

```python
tasks = routing_engine.route(event)
notification_db.insert(notification_id, user_id, "PENDING")

# Fan-out: write to all applicable channel queues in parallel
futures = [
    channel_queues[task.channel].enqueue(task)
    for task in tasks
]
await asyncio.gather(*futures)  # parallel, not sequential
```

**Fan-out at Scale: When a User Has Multiple Devices**

A user might have 3 iOS devices, 1 Android device. They want push notifications on all of them. The Dispatcher fetches all device tokens for the user from the Device Registry (stored in DynamoDB with TTL on inactive tokens), and fans out to a push task per device.

```
User u123 has:
  - iPhone 14 Pro     → APNs token: "aaabbb..."
  - iPad              → APNs token: "cccddd..."
  - Android Pixel 7   → FCM token:  "eeefff..."

Fan-out creates 3 push tasks: 2 APNs tasks + 1 FCM task
Each enqueued independently to the push queue
```

This fan-out can be large for users with many devices or heavy notification volumes. Limit fan-out: max 5 device tokens per user (oldest tokens pruned when new tokens are registered).

**Push Notification Fan-out for Bulk Events (1,000+ users)**

For system-wide notifications (e.g., scheduled maintenance, product announcement), the Dispatcher receives a single event with `target: "all_users"`. Fan-out to 50M users must be done asynchronously:

1. Write a "campaign" record to Cassandra
2. Enqueue a fan-out task to a separate "campaign queue"
3. Campaign worker reads users in batches (1,000 users/batch), creates channel tasks
4. Channel tasks flow through the normal push/email/SMS pipelines

This prevents a single event from overwhelming the Dispatcher with 50M tasks.

### 7.2 Priority Queues and Retry Logic

**Three-Tier Priority Queue Architecture**

Not all notifications are equal. A security alert (your account was logged in from a new device) must arrive within seconds. A weekly newsletter can wait in a queue for minutes.

```
Priority 1 — CRITICAL (dedicated workers, never delayed)
  Examples: payment received, account compromised, OTP/2FA codes
  Queue: notifications-critical (SQS FIFO queue or Kafka topic with priority consumer)
  Workers: 20 dedicated workers, always running
  SLA: delivered within 5 seconds of event
  
Priority 2 — STANDARD (normal workers, 99% of volume)  
  Examples: new comment, new follower, friend request
  Queue: notifications-standard
  Workers: 80 workers (auto-scaled)
  SLA: delivered within 5 minutes
  
Priority 3 — MARKETING (batch workers, lower priority)
  Examples: weekly digest, promo emails, recommendations
  Queue: notifications-marketing
  Workers: 20 workers (scaled down during off-peak hours)
  SLA: delivered within 60 minutes
```

Why separate queues and not a single queue with a priority field? Because a single queue with 10M marketing notifications + 100 critical notifications means the 100 critical ones wait behind 10M marketing ones even if they have a higher priority field — most queue implementations don't efficiently support priority. Separate queues with dedicated consumers is simpler and more reliable.

**Retry Architecture with Exponential Backoff**

External APIs fail. APNs returns a 429 during peak hours. Twilio returns a 503. Your worker must retry without hammering the external service and without losing the notification.

```
Retry Schedule (with jitter to prevent thundering herd):

Attempt 1:  0s (immediate)
Attempt 2:  5s + random(0, 5s)
Attempt 3:  30s + random(0, 10s)
Attempt 4:  5min + random(0, 30s)
Attempt 5:  30min + random(0, 5min)
Attempt 6:  2hr + random(0, 15min)
Attempt 7:  6hr + random(0, 1hr)
Attempt 8:  24hr (final attempt)

After 8 attempts:  Mark as PERMANENTLY_FAILED, alert on-call, log to dead-letter queue
```

**Implementation using SQS visibility timeout or Kafka retry topics:**

With SQS: When processing fails, don't delete the message. Let it re-appear after the visibility timeout. Set visibility timeout = retry delay. A message attribute tracks the retry count.

With Kafka: On failure, publish to a retry topic (`notifications-retry-5m`, `notifications-retry-30m`, etc.). A separate consumer reads from retry topics only after the delay has elapsed. This is the "dead letter queue as a tier" pattern.

```
Push worker fails to send APNs notification:
  
  attempt=1: 429 Too Many Requests from APNs
    → publish to notifications-push-retry-5m topic
    → update DB: status=RETRY, retry_count=1
    
  [5 minutes pass]
  
  Retry worker reads from notifications-push-retry-5m:
  attempt=2: 429 still (APNs degraded)
    → publish to notifications-push-retry-30m topic
    → update DB: retry_count=2
    
  [30 minutes pass]
  
  Retry worker reads from notifications-push-retry-30m:
  attempt=3: 200 OK — APNs recovered
    → update DB: status=SENT
    → wait for APNs delivery callback
```

**Channel Fallback on Repeated Failure**

If a user's push token is invalid (APNS returns `BadDeviceToken`) and their app hasn't been opened in 7 days, fall back to email:

```python
def handle_push_failure(task: ChannelTask, error: APNsError):
    if error.reason == "BadDeviceToken":
        # Token is no longer valid — device uninstalled the app
        device_registry.invalidate_token(task.device_token)
        
        # If this was the user's only push device and they have email registered:
        if not device_registry.has_active_devices(task.user_id):
            fallback_task = ChannelTask(
                notification_id=task.notification_id,
                user_id=task.user_id,
                channel="email",
                content=template_service.render(task.template_id, "email", task.context)
            )
            channel_queues["email"].enqueue(fallback_task)
            notification_db.update_status(task.notification_id, "CHANNEL_FALLBACK")
    else:
        # Transient error — retry
        enqueue_retry(task, delay=calculate_backoff(task.retry_count))
```

### 7.3 Deduplication and Idempotency

**Why Duplicates Happen**

In distributed systems, duplicate messages are inevitable:

1. Kafka guarantees at-least-once delivery. If a consumer crashes after processing a message but before committing the offset, the message is re-delivered.

2. An upstream service retries a failed API call to the notification system, sending the same event twice.

3. A deploymen restart during processing replays the last uncommitted batch.

4. A developer bug accidentally publishes the same notification event twice.

Without deduplication, a user receives a double push notification, two SMS messages (at $0.01/SMS × millions of users = real money), or 3 "payment confirmed" emails — a terrible user experience.

**Idempotency Key Design**

Every notification event must carry a globally unique, stable `notification_id`. This is the idempotency key.

```
notification_id generation:
  Good:   UUID v4 (random, collision probability negligible)
  Better: Content-addressed ID based on {event_type + user_id + event_id + timestamp_minute}
          → same payment event always generates same notification_id
          → automatically idempotent if the upstream event is idempotent

Example:
  event = {type: "payment_received", user_id: "u123", payment_id: "p456", amount: "$50"}
  notification_id = sha256("payment_received:u123:p456") → "4f3a9b..."
  
  If this event is re-published, it generates the same notification_id.
  The dedup check catches it.
```

**Redis-Based Deduplication**

```python
def is_duplicate(notification_id: str) -> bool:
    key = f"dedup:{notification_id}"
    # SETNX (SET if Not eXists) atomically: returns 1 if key was set, 0 if already existed
    result = redis.set(key, "1", nx=True, ex=86400)  # TTL: 24 hours
    return result is None  # None means key already existed → duplicate

def process_event(event: Event):
    if is_duplicate(event.notification_id):
        metrics.increment("notifications.deduplicated")
        return  # Skip — already processed
    
    # Continue with normal processing
    route_and_dispatch(event)
```

The Redis `SET NX EX` operation is atomic. Two concurrent workers processing the same event will: worker A sets the key (returns "OK"), worker B tries to set the key (returns None because it already exists). Worker A processes the notification; worker B discards it. No duplicate.

**Deduplication at the Channel Level**

Even after Dispatcher-level dedup, a channel worker might retry and send the same notification twice (if it crashes after sending but before updating the DB). External services provide their own idempotency:

- **SendGrid:** Include a custom `X-Unique-ID` header per send. SendGrid deduplicates sends with the same `X-Unique-ID` within 24 hours.
- **Twilio:** Include `IdempotencyKey` in the API request. Same key = same SMS = deduplicated.
- **APNs:** APNs does not natively deduplicate. Store the APNs message ID in your DB before sending. On retry, check if an APNs ID already exists for this `notification_id` — if yes, skip.

**Notification-Level State Machine**

Use a state machine in the Notification DB to prevent double-processing:

```
States: PENDING → SENT → DELIVERED
                ↓
              FAILED → RETRY → SENT / PERMANENTLY_FAILED
```

Before sending, use a conditional update (compare-and-swap):
```
UPDATE notifications 
SET status = 'SENT', sent_at = now()
WHERE notification_id = $id AND status = 'PENDING'

If 0 rows updated → status is not PENDING (already SENT or FAILED) → skip
```

This prevents a scenario where two retry workers both read status=PENDING and both attempt to send the same notification. The first update changes status to SENT; the second update finds status != PENDING and aborts.

### 7.4 Delivery Receipt Tracking

**The Problem**

Sending a notification is not the same as delivering it. An email sent to a spam folder, a push notification to a phone with no internet connection, an SMS to a full voicemail box — all "sent" but not "delivered." For critical notifications (2FA codes, payment alerts), you need to know if delivery actually happened.

**APNs Delivery Feedback**

APNs operates asynchronously. You send a notification request; APNs queues it and delivers it when the device connects. APNs provides two feedback mechanisms:

1. **Synchronous response:** APNs returns an error immediately if the token is invalid, the payload is malformed, or APNs is rate-limiting you. Store the APNs request ID in your DB.

2. **Delivery status via APNs HTTP/2 response code:**
   - `200 OK` → notification accepted, queued for delivery
   - `410 Gone` → device token is no longer valid (app uninstalled)
   - `429 Too Many Requests` → rate limited by APNs

3. **APNs does not send delivery receipts for individual notifications.** There is no "notification X was delivered to device Y at time Z" callback. To know if a notification was actually seen, you need app-level acknowledgment: the mobile app calls your backend when it receives and displays the notification.

**App-Level Acknowledgment**

When the mobile app receives a push notification:

```swift
// iOS app code — called when push notification is received
func application(_ application: UIApplication,
                 didReceiveRemoteNotification userInfo: [AnyHashable: Any],
                 fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
    
    let notificationId = userInfo["notification_id"] as? String
    
    // Call your backend to acknowledge receipt
    APIClient.shared.acknowledgeNotification(notificationId: notificationId) { _ in
        completionHandler(.noData)
    }
}
```

Backend endpoint:
```
POST /api/notifications/{notification_id}/acknowledge
Body: {device_token: "...", received_at: "2024-01-24T10:30:00Z"}

Action: UPDATE notifications SET status='DELIVERED', delivered_at=now() WHERE notification_id=$id
```

**SMS Delivery Receipts via Twilio**

Twilio provides delivery receipts natively. When you send an SMS, you specify a `StatusCallback` URL. Twilio POSTs to this URL when the message status changes:

```
Twilio callback payload:
{
  "SmsSid": "SM...",
  "MessageStatus": "delivered",  // "sent" | "delivered" | "failed" | "undelivered"
  "To": "+14155552671",
  "From": "+18555550001"
}
```

Your Delivery Receipt API receives this callback and updates the Notification DB.

**Email Open/Click Tracking via SendGrid**

SendGrid inserts a 1x1 tracking pixel in HTML emails. When the recipient opens the email, the pixel loads from SendGrid's servers. SendGrid then POSTs a webhook event to your callback URL.

```
SendGrid event payload:
{
  "event": "open",  // "processed" | "delivered" | "open" | "click" | "bounce" | "unsubscribe"
  "email": "user@example.com",
  "timestamp": 1706000000,
  "sg_message_id": "..."
}
```

Map `sg_message_id` to your `notification_id` (stored when the email was sent) and update status.

**Delivery Receipt Architecture**

```
                    [APNs / Twilio / SendGrid / Mobile App]
                                    │
                                    │ Callbacks / webhooks / app acknowledgment
                                    ▼
                    ┌──────────────────────────────────┐
                    │    Delivery Receipt API           │
                    │  (POST /callbacks/apns, etc.)    │
                    │                                  │
                    │  1. Validate webhook signature   │
                    │  2. Map external ID → notif_id   │
                    │  3. Update Cassandra status       │
                    │  4. Publish receipt event to     │
                    │     Kafka (for downstream subs)  │
                    └──────────────────────────────────┘
```

**Timeout-Based Fallback**

For critical notifications: if no delivery acknowledgment is received within 10 minutes, trigger a fallback:

```python
async def check_critical_delivery():
    """
    Runs every minute. Checks for critical notifications 
    that haven't been acknowledged within 10 minutes.
    """
    unacknowledged = await notification_db.query(
        status=["SENT"],
        priority=Priority.CRITICAL,
        sent_before=now() - timedelta(minutes=10)
    )
    
    for notif in unacknowledged:
        # Escalate: try the next channel in the fallback chain
        if notif.channel == "push":
            dispatch_to_channel(notif, "sms")  # fallback to SMS
        elif notif.channel == "sms":
            dispatch_to_channel(notif, "email")  # fallback to email
        
        notification_db.update(notif.id, status="ESCALATED")
```

---

## 8. Trade-offs and Alternatives

### Fan-out Strategy: Push vs. Pull for In-App Notifications

**Push (our design for push/SMS/email):** The notification system actively pushes notifications to the user's device via APNs/FCM/Twilio. Pros: low latency, works in background. Cons: requires a device token, complex delivery acknowledgment.

**Pull (in-app notifications):** The app polls an API endpoint for new notifications. Pros: simple, no device tokens, works in web browsers. Cons: higher latency (polling interval), server load from constant polling.

**Long-polling / Server-Sent Events (SSE):** The browser/app opens a persistent connection; the server pushes new notifications as they arrive. Better than polling for web. Cons: connection management complexity, load balancer/proxy timeout handling.

**WebSockets:** Bidirectional persistent connection. Best for real-time in-app notification badges. Cons: connection state management at scale (use Redis pub/sub or a message broker to fan out to the right WebSocket connection).

My design uses pull (read-from-API) for in-app to keep the core notification system simple. Real-time badge updates use a separate WebSocket/SSE service that reads from the same Kafka delivery stream.

### Template Storage: Database vs. Code vs. CMS

**Option 1: Templates in code (Git-managed):** Developers write and deploy templates with the service. Version-controlled, but requires a deployment to change a template.

**Option 2: Templates in database:** Templates stored as strings in PostgreSQL/DynamoDB. Updated via an admin UI without deployment. Risk: a bad template update takes effect immediately in production.

**Option 3: External CMS (Contentful, Strapi):** Content teams manage templates without engineer involvement. Templates are localized, versioned, and can be A/B tested. Complexity: another external service dependency.

**My recommendation:** Option 2 for programmatic notifications (tight coupling between template and business logic). Option 3 for marketing notifications (content teams must be able to update promotional emails without a deployment).

### Queue Technology: SQS vs Kafka vs RabbitMQ

| | SQS | Kafka | RabbitMQ |
|---|---|---|---|
| Managed service | Yes (AWS) | Yes (Confluent) / Self-hosted | Self-hosted |
| Message ordering | FIFO queue only | Per partition | Per queue |
| Replay | No (messages deleted after consume) | Yes (configurable retention) | No |
| Dead letter queue | Native support | Manual (DLQ topic) | Native support |
| Scale | Auto (millions/sec) | Very high, needs tuning | Limited |
| Cost at scale | ~$0.40/million messages | Complex pricing | Operational cost |

**Recommendation:** Kafka for event ingestion and channel fan-out. SQS for individual channel queues (push, SMS, email) — SQS integrates natively with Lambda, provides dead letter queues, and visibility timeout makes retry logic trivial. Different tools for different jobs.

### Notification Rate Limiting: How to Avoid Spamming Users

A user should not receive 50 notifications in an hour because your system has a bug or a marketing campaign is poorly configured. Rate limiting at the user level:

```
Per-user rate limits:
  Max 5 push notifications per hour (non-critical)
  Max 3 SMS per day
  Max 2 marketing emails per week

Implementation: Redis sliding window counter keyed by {user_id, channel, category, window}
On rate limit exceeded: drop notification (log to audit trail), alert operations team
```

This is separate from API-level rate limiting. This is user-experience rate limiting — protecting users from notification fatigue.

---

## 9. Failure Scenarios

### Scenario 1: APNs Outage

**Symptom:** APNs returns 503 for all push notification requests. This happens ~1-2 times per year.

**Impact without resilience:** 6M push notifications/day are stuck. Push workers back up. Queue depth grows. Alerts fire. Engineers manually drain the queue.

**Impact with our design:**
- Push workers begin retrying with exponential backoff
- Queue depth grows, but Kafka retains messages durably (7-day retention)
- After APNs recovers (usually 30 min - 2 hours), workers drain the queue
- Critical notifications are already being failed over to SMS (after 10-minute timeout)
- Marketing notifications are dropped after 24 hours (acceptable — stale promos are spam)

### Scenario 2: User Preferences Cache Miss Storm

**Scenario:** After a Redis restart, all user preference caches are cold. Every notification triggers a DB lookup.

**Impact:** Preference DB receives 116 req/sec instead of near-zero. PostgreSQL handles this comfortably. But if the restart happens during a bulk campaign (2M notifications in 30 min = 1,111 req/sec), the DB may be overwhelmed.

**Mitigation:**
- Warm the cache on Redis startup by pre-loading preferences for active users (those who logged in within 7 days)
- Use a read-through cache with a staggered TTL (60-120 seconds random) so not all entries expire simultaneously
- Preference DB has read replicas — a Redis outage drives reads to the replica, not the primary

### Scenario 3: Duplicate Notification Bug

**Scenario:** A code deploy introduces a bug that publishes every event to Kafka twice. Users receive duplicate notifications.

**What saves us:** The deduplication layer in Redis. If the same `notification_id` appears twice, the second occurrence is discarded. The dedup check happens before any channel dispatch.

**What doesn't save us:** If the bug generates different `notification_id` values for the same logical event (e.g., two different UUIDs for the same payment event). Content-addressed notification IDs solve this:
```
notification_id = sha256(f"{event_type}:{user_id}:{event_id}")
```
Same payment event always produces the same notification_id, regardless of how many times it's published.

### Scenario 4: Dead Letter Queue Buildup

**Scenario:** Twilio is rate-limiting your account. SMS notifications retry 8 times over 24 hours and land in the dead-letter queue. The DLQ now has 1M messages.

**Mitigation:**
- Monitor DLQ depth and alert when it exceeds a threshold (e.g., 10,000 messages)
- Implement a DLQ processor that redrives messages after a configurable delay
- For SMS-specific failures: reach out to Twilio to increase rate limits; implement multiple Twilio accounts with round-robin load balancing
- After SLA expires, mark as PERMANENTLY_FAILED and expose in user notification history as "notification could not be delivered"

### Scenario 5: GDPR Data Deletion Request

**Scenario:** A user submits a GDPR "right to erasure" request. All their notification data must be deleted within 30 days.

**What to delete:**
- Notification DB: delete all rows for user_id (Cassandra partition delete — efficient)
- Redis: delete dedup keys (by pattern `dedup:{notification_id}` — requires storing notification_ids per user, or a time-based scan)
- Template rendered content: may contain PII (user's name, account details) — delete rendered content, keep metadata
- Delivery logs: anonymize (replace user_id with "DELETED_{hash}")

This is primarily a data engineering problem. Design the Notification DB to support efficient user-scoped deletion (Cassandra partition key = user_id makes this a single partition delete).

---

## 10. Interview Tips

### Time Management for 45 Minutes

```
0-2 min:   Read problem, clarify scope
2-8 min:   Clarifying questions (channel types, priority, dedup, delivery receipts)
8-12 min:  Requirements and capacity estimation
12-18 min: High-level design with architecture diagram
18-38 min: Deep dives:
           - Channel routing and fan-out (5 min)
           - Priority queues + retry (6 min)
           - Deduplication and idempotency (5 min)
           - Delivery receipts (4 min)
38-42 min: Trade-offs
42-45 min: Failure scenarios
```

### What Separates Staff from Senior Candidates

**Senior candidate:** Correct channel architecture, Kafka for decoupling, retry with backoff, basic dedup.

**Staff candidate** additionally:
- Immediately identifies that analytics are wrong without content-addressed notification IDs (same event → same notification_id → automatic dedup even with duplicate event publishing)
- Designs the three-tier priority queue architecture unprompted
- Explains the delivery receipt challenge (APNs doesn't provide delivery receipts at the message level)
- Addresses GDPR right to erasure as a failure mode
- Discusses channel fallback (push fails → SMS → email) with a concrete timeout-based trigger
- Identifies the fan-out-to-many-devices problem and proposes a device registry with token expiry
- Addresses the bulk campaign fan-out problem (50M users) differently from per-user notifications

### Common Mistakes to Avoid

1. **Synchronous external API calls in the critical path.** Never call APNs/Twilio/SendGrid synchronously in the request that triggers the notification. Always use a queue. External APIs have variable latency and rate limits.

2. **Missing deduplication.** This is one of the most commonly forgotten components. Duplicate notifications are one of the worst user experiences and one of the easiest to prevent.

3. **Single notification queue.** A marketing email should never delay a critical security alert. Always propose separate queues for different priority tiers.

4. **Not addressing preference freshness.** A user who turns off SMS notifications expects it to take effect immediately. 24-hour cache TTL is not acceptable. Redis with a 60-second TTL + cache invalidation on preference changes is correct.

5. **Ignoring cost.** SMS costs money (Twilio charges per SMS). Sending 200K SMS/day at $0.01/SMS = $2,000/day. Rate limiting SMS notifications per user per day is not just a UX concern — it's a financial control.

6. **Not discussing compliance.** Notifications must honor unsubscribes (CAN-SPAM), consent requirements (GDPR), and quiet hours. These are not optional features — they're legal requirements.

### Strong Closing Statement

> "To summarize: this notification system is built around three architectural pillars. First, complete decoupling between event producers and the notification system via Kafka — upstream services never call us directly, they just publish events. Second, fan-out through separate priority-tiered channel queues — critical notifications are never delayed by marketing volume, and each channel fails independently. Third, idempotency at every layer — content-addressed notification IDs prevent duplicates even when upstream services re-publish events, and Redis deduplication catches anything that slips through. Delivery receipts flow back through a webhook API and complete the feedback loop, enabling channel fallback for undelivered critical notifications."

---

*End of Notification System Design — estimated interview preparation time: 4-5 hours of active practice*
