# Idempotency

## 1. What It Is

An operation is idempotent if applying it multiple times produces the same result as applying it once. In distributed systems, idempotency is the primary mechanism for making at-least-once delivery safe — if a message or request is retried (due to network failure, timeouts, crashes), the outcome is the same as if it happened exactly once. It exists because networks are unreliable and retries are inevitable; without idempotency, retries cause duplicate charges, duplicate shipments, or corrupted state.

---

## 2. The Problem It Solves

**Payment processing without idempotency:**

1. Client sends `POST /payments` — $100 charge.
2. Server processes the payment, charges the card, but crashes before sending the response.
3. Client receives a timeout. Was the charge made? Unknown.
4. Client retries: `POST /payments` — $100 charge again.
5. Customer is charged $200.

This is the core problem: in a distributed system, a timeout means "I don't know if the operation succeeded." The safe response is to retry. But retrying non-idempotent operations causes duplication.

**The goal:** Make every operation safe to retry. If the operation already succeeded, the retry returns the same result without re-executing the side effect.

---

## 3. How It Works

### Idempotency Key Pattern

The client generates a unique key (UUID) for each logical operation and includes it in every request for that operation. The server stores the result keyed by this idempotency key and returns the stored result on any retry.

```
First Request:
  POST /payments
  Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
  Body: { "amount": 100, "currency": "USD", "cardToken": "tok_abc" }

  Server:
    1. Check: SELECT * FROM idempotency_keys WHERE key = '550e8400...'
    2. Not found → proceed with payment
    3. Charge card → success, charge_id = "ch_xyz"
    4. Store: INSERT INTO idempotency_keys
              (key, response_status, response_body, created_at)
              VALUES ('550e8400...', 200, '{"chargeId":"ch_xyz"}', NOW())
    5. Return 200 {"chargeId": "ch_xyz"}

Retry (same Idempotency-Key):
  POST /payments
  Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000

  Server:
    1. Check: SELECT * FROM idempotency_keys WHERE key = '550e8400...'
    2. Found → return stored response immediately
    3. Return 200 {"chargeId": "ch_xyz"}  ← SAME result, NO re-charge
```

**Key properties:**
- The idempotency key must be in the DB before the side effect (payment) is executed — or atomically with it.
- The key must have a deduplication window (e.g., 24 hours) — after which it can be cleaned up.
- Different logical operations (same user, different payment) must use different keys.

**Deduplication window sizing:**
- Too short: legitimate retries after long network outages are not deduplicated.
- Too long: key table grows indefinitely.
- Stripe: 24 hours. AWS: 7 days for SQS deduplication. Choose based on your retry window.

### At-Least-Once + Idempotent Consumer = Effectively-Once

Message queues (Kafka, SQS, RabbitMQ) guarantee at-least-once delivery: a message may be delivered more than once. To achieve effectively-once processing:

```
At-least-once delivery + idempotent consumer = effectively-once semantics

Consumer must be idempotent:
  - Check if message already processed (by message ID / dedup key)
  - If processed: skip (ack the message, do nothing)
  - If not: process + mark as processed atomically

Without idempotency:
  Message: "Transfer $100 from A to B"
  Delivered twice → two transfers → $200 moved
  
With idempotency:
  Message: "Transfer $100 from A to B [msgId=abc123]"
  First delivery: process + store msgId=abc123 in processed_messages
  Second delivery: check msgId=abc123 → already processed → skip
  Result: exactly one $100 transfer
```

---

## 4. Algorithm / Implementation

### Idempotency Key in Database (Transactional)

```sql
-- Schema
CREATE TABLE idempotency_keys (
    idempotency_key  UUID PRIMARY KEY,
    request_hash     VARCHAR(64),    -- hash of request body (detect different requests with same key)
    response_code    INT,
    response_body    JSONB,
    created_at       TIMESTAMP,
    expires_at       TIMESTAMP       -- for cleanup
);

CREATE TABLE payments (
    id          UUID PRIMARY KEY,
    amount      DECIMAL,
    currency    VARCHAR(3),
    charge_id   VARCHAR(100),
    created_at  TIMESTAMP
);
```

```java
@Transactional
public PaymentResponse processPayment(String idempotencyKey, PaymentRequest request) {
    // 1. Check for existing result
    IdempotencyRecord existing = idempotencyRepo.findById(idempotencyKey);
    if (existing != null) {
        // Verify same request body (detect key reuse for different operations)
        if (!existing.getRequestHash().equals(hash(request))) {
            throw new ConflictException("Idempotency key used for different request");
        }
        return existing.getResponse(); // Return cached response
    }

    // 2. Lock the key to prevent concurrent execution (optimistic or pessimistic)
    // Option A: INSERT first (will fail on duplicate key if concurrent request)
    try {
        idempotencyRepo.insert(new IdempotencyRecord(idempotencyKey, hash(request), "IN_PROGRESS"));
    } catch (DuplicateKeyException e) {
        // Another request with same key is in flight — wait and retry
        throw new RetryableException("Request in progress, retry in 1s");
    }

    // 3. Execute the operation
    String chargeId = paymentGateway.charge(request);
    Payment payment = new Payment(UUID.randomUUID(), request.getAmount(), chargeId);
    paymentRepo.save(payment);

    // 4. Store result (within same transaction)
    PaymentResponse response = new PaymentResponse(chargeId);
    idempotencyRepo.updateResult(idempotencyKey, 200, response);

    return response;
}
```

**Critical:** Steps 3 and 4 must be in the same transaction, or you use the outbox pattern (below).

---

## 5. Inbox / Outbox Pattern (Transactional Messaging)

The core problem: you cannot atomically commit to a DB AND publish to a message queue in a single transaction. If you write to DB first and then the app crashes before publishing to Kafka, the event is lost. If you publish to Kafka first and then the DB write fails, you have an event with no corresponding DB state.

### Outbox Pattern

```
ASCII: Outbox Pattern

Application Service
        │
        │ (single DB transaction)
        ├──────────────────────────────────────────┐
        │  INSERT INTO orders (id, ...)            │
        │  INSERT INTO outbox (event_type,         │
        │                      payload,            │
        │                      status='PENDING')   │
        └──────────────────────────────────────────┘
                                │
                                │ (outbox poller / CDC)
                                ▼
                       Outbox Poller reads
                       PENDING rows
                                │
                                ▼
                        Publish to Kafka
                                │
                        On success:
                        UPDATE outbox SET status='SENT'
                        (or DELETE row)

If app crashes after DB commit but before publish:
  → Poller retries → publishes again (at-least-once)
  → Consumer must be idempotent (by event_id)
```

**Outbox table schema:**
```sql
CREATE TABLE outbox (
    id           UUID PRIMARY KEY,
    aggregate_id UUID,           -- e.g., orderId
    event_type   VARCHAR(100),   -- e.g., "ORDER_CREATED"
    payload      JSONB,
    status       VARCHAR(20),    -- PENDING, SENT, FAILED
    created_at   TIMESTAMP,
    sent_at      TIMESTAMP
);
```

**Outbox implementations:**
- **Polling-based:** A background thread queries `SELECT * FROM outbox WHERE status='PENDING'` every 100ms. Simple but adds DB load.
- **CDC (Change Data Capture):** Debezium reads the DB's WAL (write-ahead log) and publishes changes to Kafka without polling. Zero application-level polling. Used by LinkedIn, Uber.

### Inbox Pattern (Idempotent Consumer Side)

```
Inbox table deduplicates incoming events:

Message arrives with event_id = "abc123"
  1. INSERT INTO inbox (event_id) ON CONFLICT DO NOTHING
  2. If rows_affected = 0: already processed → skip
  3. If rows_affected = 1: new → process within same transaction

Ensures: even if same event arrives twice (Kafka at-least-once),
         it is only processed once.
```

---

## 6. Distributed Transactions: 2PC vs Saga

### Two-Phase Commit (2PC)

Coordinator ensures all participants commit or all abort:

```
Phase 1 (Prepare):
  Coordinator → Participant A: "Prepare to commit"
  Coordinator → Participant B: "Prepare to commit"
  A, B respond: "Ready"

Phase 2 (Commit):
  Coordinator → A: "Commit"
  Coordinator → B: "Commit"

If any participant votes "Abort" in Phase 1 → all abort.
```

**Problems with 2PC:**
- Blocking: if coordinator crashes after Phase 1, participants hold locks indefinitely.
- Not suitable for microservices across different databases.
- Tight coupling between services.

### Saga Pattern

Break distributed transaction into a sequence of local transactions, each with a compensating transaction for rollback.

```
Order Saga (choreography-based):

1. Order Service:     Create order (status=PENDING)
                      → emit OrderCreated event

2. Payment Service:   Receive OrderCreated
                      → charge payment
                      → emit PaymentCharged OR PaymentFailed

3. Inventory Service: Receive PaymentCharged
                      → reserve inventory
                      → emit InventoryReserved OR InventoryFailed

4. Shipping Service:  Receive InventoryReserved
                      → schedule shipment
                      → emit ShipmentScheduled

FAILURE PATH:
If Inventory fails after Payment succeeded:
  InventoryService emits InventoryFailed
  PaymentService listens → triggers refund (compensating transaction)
  OrderService listens → marks order CANCELLED

Compensating transactions must be idempotent!
```

**Choreography vs Orchestration:**
- **Choreography:** Services react to events from each other. Decoupled but hard to trace.
- **Orchestration:** A central saga orchestrator (e.g., a state machine) tells each service what to do. Easier to trace; orchestrator becomes a bottleneck.

---

## 7. Trade-offs

### Idempotency Keys
**Pros:** Simple, widely applicable, client controls uniqueness.
**Cons:** Client must generate and track keys; keys table grows (needs cleanup); in-flight deduplication (two concurrent requests with same key) requires careful locking.

### Outbox Pattern
**Pros:** Guarantees exactly-once event publication relative to DB commit; no dual-write problem.
**Cons:** Polling adds DB load; CDC requires Debezium/WAL setup; slightly delayed event delivery.

### Saga
**Pros:** No distributed locking; services can use different databases; highly available.
**Cons:** No atomicity — partial states are visible during execution; compensation logic is complex; difficult to debug long-running sagas.

### 2PC
**Pros:** True atomicity across participants.
**Cons:** Blocking on coordinator failure; not used in modern microservices; requires XA transactions.

---

## 8. Where It Appears in Real Systems

| System | Usage |
|--------|-------|
| **Stripe** | `Idempotency-Key` header for all payment mutations |
| **AWS SQS** | `MessageDeduplicationId` for FIFO queues (5-min window) |
| **Kafka** | Producer `enable.idempotence=true` + exactly-once transactions |
| **Debezium** | CDC-based outbox pattern implementation |
| **Uber (Cadence/Temporal)** | Orchestrated saga for trip booking, payments |
| **Netflix** | Outbox pattern for event-driven microservices |
| **PayPal** | Idempotency keys + saga for payment processing |
| **Shopify** | Idempotency keys for order mutations |

---

## 9. Numbers to Know

| Metric | Value |
|--------|-------|
| Stripe idempotency key retention | 24 hours |
| AWS SQS deduplication window | 5 minutes |
| UUID v4 collision probability (10B keys) | ~10^-18 (negligible) |
| Typical outbox polling interval | 100ms–1s |
| CDC (Debezium) lag | < 1 second (reads WAL) |
| Saga compensating transaction latency | Same as forward transaction |
| 2PC: blocking window on coordinator crash | Until coordinator recovers (minutes to hours) |
| DB unique constraint check cost | O(log N) B-tree lookup — negligible |

---

## 10. Interview Tips

### What You'll Be Asked

**"Design a payment system. How do you prevent double charges?"**
- Idempotency key in the request header.
- Server checks DB before executing.
- Store result atomically with the operation in the same transaction.
- Discuss deduplication window.

**"How do you ensure exactly-once processing in a message-driven system?"**
- At-least-once delivery + idempotent consumer.
- Use inbox table or check message ID before processing.
- Mention that Kafka can provide exactly-once if producer idempotence + transactions are enabled (but this only works within Kafka; cross-system still needs application-level idempotency).

**"How do you handle a crash between writing to DB and publishing to a message queue?"**
- The dual-write problem → outbox pattern.
- Both writes in one DB transaction; separate poller/CDC publishes the outbox events.
- Consumer must be idempotent.

### Common Follow-ups
- "What if two requests with the same idempotency key arrive concurrently?" → Use a DB unique constraint on the key + handle the `IN_PROGRESS` state; return 409 Conflict or wait/retry.
- "How do you clean up old idempotency keys?" → Scheduled job or DB TTL (`expires_at` + index). Don't delete too early — within the retry window.
- "What's the difference between idempotent and safe?" → Safe: no side effects (read-only). Idempotent: side effects, but same result if repeated. GET is both. DELETE is idempotent (second delete is a no-op) but not safe. POST is neither by default.
- "When would you use saga over 2PC?" → Always for microservices across different databases/services. 2PC requires all participants to support XA and is too blocking.

### Mistakes to Avoid
- Storing the idempotency key after the operation instead of before (or atomically with it).
- Not verifying the request body hash — allows key reuse for different operations.
- Saying Kafka provides exactly-once delivery to consumers (it provides it within Kafka; consuming and processing in another DB requires the inbox pattern).
- Not mentioning that compensating transactions in sagas must also be idempotent.
- Confusing "idempotent" (same result on retry) with "stateless" (no stored state).
