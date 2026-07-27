# 04 — Messaging Systems

---

## 1. What It Is

A messaging system decouples producers (senders) from consumers (receivers), allowing them to operate at different speeds, fail independently, and scale separately. The three major abstractions — message queue, pub-sub, and event streaming — differ in semantics around fan-out, retention, replay, and ordering guarantees.

At Staff level the key is knowing which abstraction maps to which problem, what guarantees each provides under failure, and what you give up when you choose one over another.

---

## 2. How It Works

### Message Queue vs Pub-Sub vs Event Streaming

```
MESSAGE QUEUE (SQS Standard, RabbitMQ)
  Producer --> [Queue] --> Consumer A
                     (message deleted after ACK)
  One message, one consumer.

PUB-SUB (SNS, Google Pub/Sub)
  Producer --> [Topic] --> Consumer A
                       --> Consumer B
                       --> Consumer C
  One message, many consumers (fan-out).
  No retention after delivery.

EVENT STREAMING (Kafka, Kinesis)
  Producer --> [Topic / Partition Log]
                  |-- Consumer Group A (offset 150)
                  |-- Consumer Group B (offset 80)
                  |-- Consumer Group C (offset 200)
  One message, many consumer groups.
  Log is retained; consumers own their position (offset).
  Replay is possible.
```

The critical difference with event streaming: the log is the source of truth. Consumers move through it at their own pace. This enables replay, time-travel debugging, and multiple independent consumer groups without re-publishing.

---

## 3. Deep Dive

### Kafka Architecture

Kafka is a distributed, partitioned, replicated commit log. It is not a traditional message queue — it is closer to a durable, indexed append-only file that multiple consumers can read independently.

#### Topics, Partitions, Segments

```
Topic: "user-events"

Partition 0: [msg0][msg1][msg2][msg3]...[msg999][msg1000]...[msgN]
              ^-- segment 0.log -------^  ^-- segment 1.log ------^
              
Partition 1: [msg0][msg1][msg2]...[msgN]

Partition 2: [msg0][msg1][msg2]...[msgN]

Each partition lives on one broker (the leader for that partition).
Replicas of each partition live on different brokers.
```

- **Topic:** Logical grouping of related messages.
- **Partition:** The unit of parallelism and ordering. Messages within a partition are strictly ordered. Messages across partitions are not.
- **Segment:** A partition is split into segment files on disk (default: 1GB or 7 days). The active segment is written to; older segments are candidates for deletion or compaction.

#### Consumer Groups and Offsets

```
Topic: "orders" (3 partitions)

Consumer Group "shipping-service":
  Consumer 1 --> Partition 0  (committed offset: 450)
  Consumer 2 --> Partition 1  (committed offset: 302)
  Consumer 3 --> Partition 2  (committed offset: 711)
  
Consumer Group "analytics-service":
  Consumer A --> Partition 0  (committed offset: 120)
  Consumer A --> Partition 1  (committed offset: 80)   <-- same consumer, 2 partitions
  Consumer B --> Partition 2  (committed offset: 400)
  
Rule: Each partition is assigned to exactly ONE consumer within a group.
      Max parallelism = number of partitions.
      Adding consumers beyond partition count = idle consumers.
```

- **Committed offset:** The offset the consumer has durably saved to `__consumer_offsets` (Kafka's internal topic). Defines where to resume after a crash.
- **Current offset (latest):** The offset the consumer is actively processing.
- **Consumer lag:** `latest offset - committed offset`. This is the primary health metric for consumers.

The `__consumer_offsets` topic itself has 50 partitions by default and stores offset commits as compacted key-value records (key = group + topic + partition, value = offset).

#### Retention: Deletion vs Log Compaction

**Time/size-based deletion:** Segments older than `retention.ms` (default: 7 days) or exceeding `retention.bytes` are deleted. Simplest model — good for event streams where old data has no meaning.

**Log compaction:** Kafka keeps only the *last* message for each key. Like a key-value store backed by a log. Used for:
- Change data capture (CDC) — keep the latest state of each database row.
- Configuration/offset stores — only the current value matters.
- Consumer group offsets (the `__consumer_offsets` topic uses this).

```
Before compaction:
  [key=user1, v=A] [key=user2, v=X] [key=user1, v=B] [key=user1, v=C]

After compaction:
  [key=user2, v=X] [key=user1, v=C]
```

Compaction does not run continuously — it runs in background threads. The "head" of the log (recent messages) is always uncompacted.

#### ISR, Leader Election, and `min.insync.replicas`

```
Partition 0 on Broker cluster:

  Leader: Broker 1  (accepts all reads and writes)
  ISR (In-Sync Replicas): {Broker 1, Broker 2, Broker 3}
  
  Broker 2 starts lagging (replica.lag.time.max.ms exceeded):
  ISR becomes: {Broker 1, Broker 3}
  
  Broker 1 crashes:
  ZooKeeper (or KRaft in newer versions) elects new leader from ISR.
  Leader becomes: Broker 3
  
  If ISR = {Broker 1} only and Broker 1 crashes:
  - With unclean.leader.election=false: partition is unavailable until Broker 1 recovers
  - With unclean.leader.election=true: out-of-sync replica elected (potential data loss)
```

- **`replication.factor=3`** is standard. Tolerates one broker failure without data loss.
- **`min.insync.replicas=2`** (used with `acks=all`): Producer write only succeeds if at least 2 replicas acknowledge. If only 1 broker is available, writes fail rather than succeed with potential data loss.
- **`acks=all` + `min.insync.replicas=2` + `replication.factor=3`**: The gold standard for durable writes.

---

### Delivery Guarantees

#### At-Most-Once (`acks=0`)

```
Producer --> Broker (fire and forget, no ACK waited)
         --> Message may be lost if broker crashes before writing to disk
Consumer --> Reads message, updates offset BEFORE processing
         --> If consumer crashes after offset commit but before processing: message skipped
```

Use case: Metrics/logs where occasional loss is acceptable and throughput is paramount.

#### At-Least-Once (`acks=all`, commit offset after processing)

```
Producer --> Broker (waits for all ISR to ACK)
         --> On network timeout, retries --> possible duplicate if first write succeeded
Consumer --> Reads message, PROCESSES first, then commits offset
         --> If consumer crashes after processing but before commit: message reprocessed
```

The most common default. Requires idempotent consumers — your processing logic must handle duplicate messages (e.g., use `ON CONFLICT DO NOTHING`, upsert by idempotency key, or check a processed-IDs set).

#### Exactly-Once (Kafka Transactions + Idempotent Producer)

```
Producer side:
  transactional.id = "my-producer-1"
  enable.idempotence = true
  
  producer.beginTransaction()
  producer.send(record)         --> Broker assigns sequence number per (producer_id, partition)
  producer.commitTransaction()  --> Atomic commit marker written to all involved partitions
  
Consumer side (read-process-produce pattern):
  isolation.level = "read_committed"  --> Only reads messages from committed transactions
  
  consumer.poll()
  // process: transform record
  producer.send(outputRecord)   --> Part of same transaction
  producer.sendOffsetsToTransaction(offsets, groupId)
  producer.commitTransaction()  --> Atomically commits output + offset advance
```

- The idempotent producer ensures retries do not produce duplicates (broker deduplicates by sequence number within a session).
- Transactions extend this across multiple partitions and topics — either all or nothing.
- Performance cost: ~5–10% throughput reduction vs at-least-once.
- **Exactly-once at consumer side (alternative):** Write output + offset to the same database transaction. E.g., process Kafka message, write result to PostgreSQL, save offset to PostgreSQL in same transaction. No Kafka transactions needed — idempotency comes from the database.

---

### Backpressure

Backpressure is the mechanism by which a slow consumer signals to the upstream that it cannot keep up, preventing unbounded memory growth or data loss.

```
Without backpressure:
  Fast Producer (10k/s) --> [Unbounded Queue] --> Slow Consumer (1k/s)
  Queue grows 9k messages/second --> OOM crash or unbounded latency

With backpressure:
  Fast Producer (10k/s) --> [Bounded Queue, max=1000] --> Slow Consumer (1k/s)
  Queue full --> Producer blocked or drops (configurable policy)
```

**Handling strategies:**

| Strategy | Mechanism | Trade-off |
|---|---|---|
| **Bounded queues** | Block producer when queue is full | Adds latency to producer; prevents OOM |
| **Rate limiting** | Token bucket / leaky bucket at producer | Smooths bursts; requires capacity planning |
| **Load shedding** | Drop lowest-priority messages when overloaded | Loses data; requires idempotency on retry |
| **Circuit breaker** | Stop sending when downstream error rate exceeds threshold | Fast failure; requires retry queue |
| **Scale consumers** | Add consumer instances | Correct long-term solution; slow to react |
| **Consumer lag alerting** | Alert when lag > 10k messages | Triggers scaling or investigation |

In Kafka, backpressure is implicit: the log buffers indefinitely (within retention), so producers are never blocked. The risk shifts to consumer lag growing unboundedly. Monitor consumer lag; alert and scale consumers proactively.

---

### Dead-Letter Queues (DLQ)

A DLQ receives messages that failed processing after all retries are exhausted.

```
Normal path:
  [Queue] --> Consumer --> ACK (success)

Failure path:
  [Queue] --> Consumer --> NACK (failure)
               |
               v retry with exponential backoff + jitter
               |
               v retry 2
               |
               v retry N (max retries exhausted)
               |
               v --> [Dead-Letter Queue]
                       |
                       v alert / manual inspection / replay
```

**Retry strategy — exponential backoff with full jitter:**

```
delay = min(cap, base * 2^attempt) * random(0, 1)

Example (base=1s, cap=60s):
  Attempt 1: ~0.5s
  Attempt 2: ~1s
  Attempt 3: ~2s
  Attempt 4: ~4s
  Attempt 5: ~8s
  ...
  Attempt N: up to 60s
```

Full jitter (multiply by random 0–1) prevents thundering herd: all consumers do not retry simultaneously after a downstream outage recovers.

**When messages go to DLQ:**
- Poison pills: malformed messages that always fail deserialization.
- Transient downstream failures that exceed retry budget.
- Business logic rejections that should be reviewed (e.g., order for deleted user).
- Messages too large for downstream to process.

**DLQ best practices:** Always set up alerts on DLQ depth. Messages silently rotting in a DLQ is a silent data loss scenario. Design DLQ messages with enough context to replay or manually remediate.

---

### Ordering Guarantees

**Partition-level ordering in Kafka:**

```
Producer: send(key="user123", value=event)
  --> hash(key) % partitions = Partition 2
  
All events with key="user123" land on Partition 2, in order.
Events for different keys may be on different partitions (no cross-partition order).
```

Ordering guarantee is per-key (when using keyed messages) within a partition. If you need total order, use a single partition — but that limits throughput to one consumer and one broker's write speed.

**SQS FIFO queues:** Guarantee ordering per message group ID. Within a group, messages are delivered in order. Throughput: 3,000 messages/s per queue (with batching: 300 API calls × 10 messages each).

**Total order vs partial order:**
- Total order: every message has a global sequence number. Requires serialization — only one writer (or a distributed consensus protocol like Raft). Kafka achieves this within a single partition.
- Partial order: messages are ordered within a partition/group but not globally. Kafka across partitions, SQS FIFO across message groups.

---

## 4. Trade-offs

### Decision Table: Kafka vs RabbitMQ vs SQS vs SNS

| Dimension | Kafka | RabbitMQ | SQS Standard | SQS FIFO | SNS |
|---|---|---|---|---|---|
| **Ordering** | Per-partition | Per-queue (with single consumer) | No guarantee | Per message group | No guarantee |
| **Throughput** | 100MB/s–1GB/s per broker | 20k–50k msg/s | Unlimited (soft) | 3,000 msg/s | Unlimited |
| **Retention** | Configurable (days to forever) | Until consumed (or TTL) | 4 days default, max 14 days | Same as standard | No retention (push only) |
| **Push vs Pull** | Pull (consumer polls) | Push (broker pushes) | Pull (long-polling) | Pull | Push |
| **Fan-out** | Consumer groups (pull) | Exchange bindings | No native fan-out | No | Yes (primary use case) |
| **Delay queues** | No (use timestamps in consumer) | Yes (per-message TTL + DLX) | Yes (0–900s delay) | Yes | No |
| **Dead-letter queue** | Manual (use topic as DLQ) | Built-in (DLX) | Built-in | Built-in | With SQS subscription |
| **Replay** | Yes (seek to offset/timestamp) | No | No | No | No |
| **Ops complexity** | High (brokers, ZooKeeper/KRaft, schema registry, monitoring) | Medium (broker, exchanges, queues) | Very low (managed) | Very low (managed) | Very low (managed) |
| **Exactly-once** | Yes (transactions) | No (at-least-once) | No | No | No |
| **Message size** | Default max 1MB (configurable) | 128MB | 256KB | 256KB | 256KB |
| **Protocol** | Custom binary (TCP) | AMQP, MQTT, STOMP | HTTPS (REST) | HTTPS | HTTPS |

**When to use each:**
- **Kafka:** High-throughput event streaming, audit logs, CDC, event sourcing, stream processing (Flink, Kafka Streams). When replay and consumer group independence matter.
- **RabbitMQ:** Complex routing logic (topic/header exchanges), task queues, per-message TTL, when you need push delivery semantics.
- **SQS Standard:** Simple work queues in AWS, decoupling microservices, when ordering does not matter and managed simplicity is valued.
- **SQS FIFO:** Ordered processing (e.g., state machine transitions, financial transactions in sequence) without Kafka's operational overhead.
- **SNS:** Fan-out to multiple endpoints (email, SMS, Lambda, SQS queues) with minimal setup.

---

## 5. Numbers to Know

| Metric | Value | Context |
|---|---|---|
| Kafka throughput per broker | 100MB/s–1GB/s | With appropriate hardware (NVMe SSDs, high-throughput NICs) |
| Kafka recommended partition count | 1–2x brokers (small cluster) | Start conservative; partitions can be added, not easily reduced |
| Kafka partition count (large cluster) | Up to 10x brokers | But each partition has overhead: file handles, memory, election cost |
| Kafka replication factor | 3 | Standard for production |
| Kafka min.insync.replicas | 2 | Use with acks=all for durable writes |
| Consumer lag alert threshold | > 10k messages OR > 5 minutes | Tune to your SLA; burst tolerance matters |
| Message size sweet spot | 1KB–1MB | Smaller = more overhead per message; larger = increased latency per message |
| Kafka max message size (default) | 1MB | `message.max.bytes`; can increase but watch broker memory |
| SQS Standard throughput | Effectively unlimited | AWS manages scaling |
| SQS FIFO throughput | 3,000 msg/s (300 API calls × 10 batch) | Per queue |
| RabbitMQ throughput | 20k–50k msg/s | Single node; depends on message size, persistence, acks |
| Kafka segment size default | 1GB or 7 days | Whichever comes first triggers segment roll |
| Kafka default retention | 7 days (`retention.ms=604800000`) | Size-based: `retention.bytes=-1` (unlimited) by default |
| __consumer_offsets partitions | 50 | Internal topic; rarely needs tuning |
| Exactly-once throughput overhead | ~5–10% reduction | vs at-least-once with acks=all |

---

## 6. Interview Tips

### What Interviewers Actually Probe

1. **"Walk me through what happens when a Kafka consumer crashes mid-processing."**
Expected answer path: Consumer was processing message at offset 500, hadn't committed yet. On restart, consumer reads committed offset (say 490) from `__consumer_offsets` and reprocesses from there. Messages 490–500 are reprocessed — this is at-least-once delivery. To handle: make consumers idempotent (upsert by event ID, or check a deduplication store).

2. **"How do you guarantee ordering in a distributed system?"**
The full answer: Ordering is always scoped. Per-partition ordering in Kafka is free. Cross-partition (global) ordering requires either a single partition (bottleneck) or a distributed sequence (Lamport clocks, vector clocks — adds latency and complexity). Most systems don't need total order — they need causal order or per-entity order.

3. **"Your Kafka consumer group has falling behind by 500k messages. What do you do?"**
Scale out consumers (up to partition count), check for consumer processing bottleneck (slow DB write, downstream service), check if it's a traffic spike or permanent growth trend. If traffic spike: temporary horizontal scaling. If permanent: increase partition count + add consumer instances. Also check for poison pill messages causing retries.

4. **"When would you use SQS over Kafka?"**
When your team cannot operate Kafka, the throughput doesn't justify it, you don't need replay, and the message volume fits SQS pricing. Kafka is operationally expensive. SQS at AWS is a managed no-ops choice for simple work queues.

5. **"How does log compaction work and when would you use it?"**
Walk through the mechanics: background compaction threads merge segments, keeping only the last value per key. Use cases: CDC snapshots (Debezium + Kafka), building materialized views, configuration propagation. Contrast with deletion: deletion removes by time/size; compaction removes by supersession.

### Common Mistakes

- **Conflating consumer group lag with message delivery latency.** Lag measures the gap between produced and consumed offsets. Delivery latency is per-message end-to-end time. A consumer can have high lag but still process each message quickly once it gets to it.

- **"Kafka guarantees ordering" without qualification.** Kafka guarantees ordering per partition. Cross-partition ordering requires the consumer to handle it (e.g., sorting by event timestamp, using a single partition, or using Kafka Streams with windowed joins).

- **Not knowing what happens when ISR shrinks below `min.insync.replicas`.** Producers get `NotEnoughReplicasException`. The partition is still available for reads but not writes. This is a CP trade-off — prefer consistency over accepting writes that might be lost.

- **Recommending Kafka for everything.** Kafka is expensive to operate. For a simple task queue with 100 msg/s, SQS is the right answer. Reserve Kafka for use cases that need its specific properties: high throughput, replay, consumer group independence, event sourcing.

- **Forgetting to handle duplicate messages.** Almost every "exactly-once" claim at the application level is actually at-least-once + idempotent consumer. Design for this explicitly.

### Questions You Will Be Asked

- "What is the maximum parallelism you can get from a Kafka topic with N partitions?"
- "How would you implement a saga pattern using Kafka?"
- "What is the difference between `auto.offset.reset=earliest` and `latest`?"
- "How do you prevent a slow consumer from starving other consumer group members during a rebalance?"
- "Walk me through Kafka's exactly-once semantics — what does `transactional.id` do?"
- "How would you handle a poison pill message (a message that always fails processing)?"
- "What happens to messages in flight during a Kafka leader election?"

---

## 7. Resources

1. **Martin Kleppmann, "Designing Data-Intensive Applications" (DDIA)** — Chapter 11 (Stream Processing) covers Kafka's log architecture, consumer groups, delivery semantics, and event sourcing with exceptional depth. https://dataintensive.net/

2. **Confluent Documentation — "Kafka: The Definitive Guide" (free PDF)** and Confluent's engineering blog. Covers ISR, exactly-once semantics, and compaction with authoritative detail from Kafka's creators. https://www.confluent.io/resources/kafka-the-definitive-guide/

3. **ByteByteGo — "How does Kafka work?" and "Message Queue vs Event Streaming"** — Visual comparisons of Kafka architecture and messaging patterns, useful for quickly building mental models for the decision framework. https://bytebytego.com/
