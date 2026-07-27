# Fan-Out

## 1. What It Is

Fan-out is the pattern of delivering one event to multiple consumers — broadcasting a single write to N destinations. It exists because social graphs and pub/sub systems require one action (a tweet, a post, a message) to be visible to potentially millions of followers, which naive on-read queries cannot serve at low latency at scale.

---

## 2. The Problem It Solves

**Without fan-out (pure pull / fan-out on read):**

User opens their Twitter timeline. To build it:
1. Fetch IDs of all people they follow (say 500 users).
2. For each followed user, fetch their latest N tweets — 500 DB queries.
3. Merge-sort by timestamp to produce the timeline.

At 100M DAU, each page load triggers 500 queries. That's 50 billion queries per day for timeline generation alone. Latency is O(followed_count) per read.

**Fan-out on write (push model) solves read latency** by pre-computing timelines at write time: when User A tweets, push that tweet ID into the timeline cache of every follower. A timeline read becomes a single cache lookup.

---

## 3. How It Works

### Fan-Out on Write (Push Model)

```
User A tweets "Hello"
         │
         ▼
    Tweet Service
         │ (async)
         ▼
    Fan-Out Service
    ┌────┴────────────────────────────────────────┐
    │  Look up A's followers: [B, C, D, ... 1000] │
    │  For each follower, write tweet ID to their  │
    │  timeline cache (Redis sorted set by time)   │
    └─┬──────┬──────┬──────┬─────────────────────┘
      ▼      ▼      ▼      ▼
   Cache   Cache  Cache  Cache
   [B]     [C]    [D]    [...]
   
Timeline read by B: O(1) cache lookup
```

**Pro:** Timeline reads are instant (pre-built cache lookup).
**Con:** A tweet from a celebrity with 50M followers triggers 50M cache writes.

### Fan-Out on Read (Pull Model)

```
User B opens timeline
         │
         ▼
    Timeline Service
         │
         ▼
    Fetch B's follow list: [A, C, X, ...]
         │
         ├─→ Fetch A's recent tweets  ─┐
         ├─→ Fetch C's recent tweets   ├─→ Merge + Sort
         └─→ Fetch X's recent tweets  ─┘
         
Timeline read by B: O(N) where N = following count
```

**Pro:** No write amplification — one tweet write = one write.
**Con:** Read is slow and expensive; hard to cache because personalized.

### Hybrid Model (Twitter's Actual Approach)

The insight: **most users have small follower counts, but celebrities have huge ones.**

```
                    Tweet from User A
                           │
                    Is A a "celebrity"?
                    (follower count > threshold, e.g., 1M)
                     /              \
                   YES               NO
                    │                │
            Stay in tweet         Async fan-out to
            store only            all follower caches
            (pull at read)        (push model)
                    │                │
                    └──────┬─────────┘
                           ▼
              User B opens timeline
              ┌─────────────────────────────────────┐
              │  1. Read pre-built cache (fast path) │
              │  2. For each celebrity B follows,    │
              │     fetch their latest tweets from   │
              │     tweet store                      │
              │  3. Merge celebrity tweets into       │
              │     pre-built timeline               │
              └─────────────────────────────────────┘
```

The hybrid model:
- Push tweets to follower caches for normal users (writes are manageable).
- Pull celebrity tweets at read time (avoids 50M cache writes per tweet).
- At read time, merge the two sets — typically only a handful of celebrities followed by any user, so the merge is O(celebrity_count × recent_tweets), which is small.

**Celebrity threshold:** Twitter historically used ~1M followers as the cutoff for switching from push to pull.

---

## 4. Algorithm / Implementation

### Async Fan-Out with Message Queue

Synchronous fan-out blocks the tweet write until all followers are updated — unacceptable at scale.

```
Tweet Service (synchronous path):
  1. Write tweet to tweet store (DB + S3)
  2. Enqueue fan-out task: { tweetId, authorId, timestamp }
  3. Return success to client immediately

Fan-Out Worker (async):
  1. Dequeue task
  2. Fetch follower list (paginated; e.g., 1000 per page)
  3. For each follower:
       ZADD "timeline:{followerId}" timestamp tweetId
       (Redis sorted set, score = timestamp for ordering)
  4. Trim cache to last 800 entries:
       ZREMRANGEBYRANK "timeline:{followerId}" 0 -801

Fan-Out Queue Depth Estimation:
  - 100K tweets/sec × avg 500 followers = 50M cache writes/sec
  - Each Redis write: ~0.1ms
  - Workers needed: 50M × 0.1ms / 1000ms = 5000 concurrent writes
  - Solution: partition fan-out queue by author_id (or follower_id range)
              to distribute across many worker instances
```

### Ordering Guarantees

Fan-out does NOT guarantee ordering across multiple tweets from the same author if done in parallel. To maintain order:
- Use sequence numbers from the tweet store.
- Partition the fan-out queue by `author_id` — all tweets from the same author go to the same partition → processed in order.

```
Kafka Fan-Out Partition Strategy:

Tweet from Author A → Partition hash(authorId) → Partition 3
Tweet from Author A → Partition hash(authorId) → Partition 3
Tweet from Author B → Partition hash(authorId) → Partition 7

Consumer on Partition 3 processes Author A's tweets in order.
```

### Failure Handling

```
Fan-Out Worker failure scenarios:

1. Worker crashes mid-fan-out (partial delivery):
   - Message is not ACK'd → queue re-delivers
   - Fan-out is idempotent: ZADD overwrites existing entry (same tweetId, same score)
   - Some followers get duplicate writes (harmless; sorted set deduplicates)

2. Follower cache miss (cache eviction):
   - Timeline read falls back to DB query: fetch author's recent tweets
   - Cache rebuilt on miss ("lazy fan-out" fallback)

3. Fan-out lag for high-follower users:
   - Under normal conditions: fan-out completes in ~5s for 1M followers
   - During spikes: lag can reach minutes → show stale timeline
   - Solution: prioritize fan-out for active/online followers only
```

### Fan-Out to Active Followers Only

Twitter optimization: skip fan-out to followers who have been inactive for >30 days. When they come back, rebuild their cache on demand.

```
For each follower in fan-out:
  if last_active[follower] < now - 30 days:
      skip cache write
      mark follower as "stale timeline"
  else:
      ZADD timeline cache
```

---

## 5. Trade-offs

### Fan-Out on Write
**Pros:**
- O(1) timeline reads — just a cache lookup.
- Predictable read latency regardless of following count.
- Scales read traffic easily (more cache replicas).

**Cons:**
- **Write amplification:** 1 tweet → up to N cache writes (N = follower count).
- **Storage cost:** Each user's timeline is a separate sorted set in Redis.
- **Stale timelines:** If a user is not online, their cache still gets updated. Wasted writes.
- **Celebrity problem:** 1 tweet from @BarackObama (100M followers) → 100M writes in fan-out queue.

### Fan-Out on Read
**Pros:**
- Minimal write complexity — one tweet = one write.
- Always fresh (reads latest data).
- No celebrity problem.

**Cons:**
- Slow reads — O(following_count) DB queries per timeline load.
- Not cacheable at the timeline level (personalized).
- Does not scale at high read traffic.

### Hybrid
**Pros:** Best of both; push for normal users (fast reads), pull for celebrities (avoids write amplification).

**Cons:**
- Complex implementation: must classify users, handle threshold changes, merge at read time.
- Edge case: what if a user with 999K followers adds one more and crosses the threshold? Need a background job to retroactively fan out or switch to pull.

---

## 6. Where It Appears in Real Systems

| System | Model |
|--------|-------|
| **Twitter (legacy)** | Pure push → moved to hybrid after celebrity problem |
| **Instagram** | Hybrid; large accounts pulled at read time |
| **Facebook News Feed** | Hybrid fan-out with heavyweight ranking at read time |
| **YouTube notifications** | Async fan-out via Pub/Sub to subscriber inboxes |
| **Apache Kafka** | Consumer groups fan-out a topic to multiple consumers |
| **Redis Pub/Sub** | Pure fan-out (no persistence — fire and forget) |
| **SNS → SQS** | AWS fan-out pattern: one SNS message to multiple SQS queues |
| **Slack channels** | Fan-out message to all channel members' notification streams |

---

## 7. Numbers to Know

| Metric | Value |
|--------|-------|
| Twitter peak tweet rate | ~150,000 tweets/sec (Super Bowl peaks) |
| Average Twitter fan-out writes/sec | ~50M/sec at peak |
| Celebrity follower threshold for pull mode | ~1M (Twitter historical) |
| Redis ZADD latency | ~0.1ms |
| Fan-out lag for 1M followers (normal) | ~5 seconds |
| Fan-out lag under spike | Up to minutes |
| Twitter: followers of @BarackObama | ~130M |
| Typical timeline cache size kept in Redis | 800 tweet IDs per user |
| Redis sorted set memory per entry | ~64 bytes (id + score) |
| 100M user timeline cache size | 100M × 800 × 64 bytes ≈ 4.8 TB (just IDs, not content) |

---

## 8. Interview Tips

### What You'll Be Asked

**"Design Twitter's timeline feature."**
This is the canonical fan-out question. Expected flow:
1. Start with naive pull model → show it doesn't scale.
2. Introduce fan-out on write with async message queue.
3. Identify the celebrity problem proactively — don't wait to be asked.
4. Propose hybrid model with threshold-based switching.
5. Discuss failure handling: idempotent writes, partial fan-out, stale cache fallback.

**"How do you handle a user with 100M followers posting?"**
- Fan-out on read for celebrity accounts.
- At read time, fetch celebrity tweets directly from tweet store and merge with pre-built timeline.
- Merge is fast because you only check celebrities you follow (typically <10 per user).

**"What happens to timeline consistency during fan-out lag?"**
- User sees stale timeline for 5–30 seconds after tweet is posted.
- This is acceptable eventual consistency for social media.
- Alternative: show user's own tweets immediately (client-side optimistic update) while fan-out completes.

### Common Follow-ups
- "How do you handle deletions?" → Delete tweet from tweet store + async fan-out to remove from caches. Race condition: re-added vs deleted. Use tombstones.
- "How do you order tweets in the cache?" → Redis sorted set, score = Unix timestamp (milliseconds for resolution). Tweet IDs can encode timestamp (Twitter Snowflake IDs are time-ordered).
- "What message queue would you use?" → Kafka (high throughput, partitioned by author_id for ordering). Justify partition key choice.
- "How do you scale the fan-out workers?" → Partition fan-out queue; auto-scale workers based on queue depth.

### Mistakes to Avoid
- Designing pure push model without recognizing the celebrity/high-fan-out problem.
- Designing pure pull model without recognizing the read scalability problem.
- Not discussing async fan-out (synchronous fan-out in the write path is a critical mistake).
- Forgetting idempotency in the fan-out worker (crash recovery must be correct).
- Not mentioning that timeline storage is tweet IDs, not full tweet content (content fetched separately).
