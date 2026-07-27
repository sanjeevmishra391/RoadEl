# 03 — Databases

---

## 1. What It Is

A database is a system for durably storing, indexing, and querying structured or semi-structured data. The choice of database model, storage engine, and replication topology directly determines a system's consistency guarantees, failure behavior, and operational ceiling.

At Staff level the interesting question is never "SQL or NoSQL" — it is which consistency model, access pattern, and operational complexity your system can afford.

---

## 2. How It Works

### Storage Engine Fundamentals

Every relational and most NoSQL databases sit on top of one of two storage engine families:

**B-tree (MySQL InnoDB, PostgreSQL, SQLite)**

```
                        [Root Page]
                       /     |     \
              [Internal]  [Internal]  [Internal]
             /     \        /   \       /    \
         [Leaf]  [Leaf]  [Leaf][Leaf][Leaf][Leaf]
          data    data    data  data  data  data
```

- Pages are typically 4KB–16KB, stored on disk.
- Writes update pages in-place (copy-on-write with WAL for crash recovery).
- A lookup traverses O(log_B N) pages where B is branching factor (~100–1000).
- For 1 billion rows: log_100(1B) ≈ 4–5 page reads to reach a leaf.

**LSM-tree (RocksDB, Cassandra, LevelDB, ScyllaDB)**

```
Write Path:
  Write --> [MemTable (RAM)] --> Flush --> [L0 SSTable]
                                               |
                                          Compaction
                                               |
                                     [L1 SSTables] --> [L2] --> [L3]

Read Path:
  Read --> MemTable --> L0 SSTables (check all!) --> L1 --> L2 --> ...
           (bloom filter prunes most SSTable checks)
```

- All writes go to the in-memory MemTable first, then flushed to immutable SSTables.
- Compaction merges and rewrites SSTables periodically to reclaim space and bound read amplification.
- Bloom filters (false positive rate ~1%) avoid disk reads for non-existent keys.

---

## 3. Deep Dive

### B-tree vs LSM-tree: The Three Amplifications

| Dimension | B-tree | LSM-tree |
|---|---|---|
| **Read amplification** | Low (log N pages) | High without bloom filters; 1–10 SSTable reads |
| **Write amplification** | Low to medium (1 in-place write + WAL) | High: **10–30x** (data rewritten across compaction levels) |
| **Space amplification** | Low (pages reused) | Medium–high (dead data lives until compaction) |
| **Best for** | Read-heavy, mixed, point lookups | Write-heavy, append-heavy (logs, time-series, event stores) |

**RocksDB Compaction Strategies:**

- **Leveled compaction (default):** Each level is ~10x the size of the previous. Bounded space amplification (~1.1x), but high write amplification (10–30x). Good for reads after compaction settles.
- **Tiered / Size-tiered (STCS):** Merge SSTables of similar size. Lower write amplification (~10x) but higher space amplification. Better for pure write throughput (Cassandra default for write-heavy workloads).
- **FIFO compaction:** Just delete oldest files. Used for time-series data with TTL — no compaction overhead, but no merging either.

**When each wins:**
- B-tree: OLTP with balanced reads/writes, financial transactions, anything needing strong point-read performance.
- LSM: Kafka-like log stores, Cassandra write-heavy workloads, RocksDB-backed state stores in stream processing (Flink, Kafka Streams).

---

### SQL vs NoSQL Decision Framework

The question is not "ACID vs eventual" — it is "what access patterns does your application have, and what anomalies can it tolerate?"

**Choose SQL (PostgreSQL, MySQL, Aurora) when:**
- You need multi-table transactions (e.g., debit account A, credit account B must be atomic).
- Your schema is stable and normalization reduces duplication.
- You need complex joins, aggregations, or ad-hoc queries.
- Consistency anomalies (dirty reads, non-repeatable reads) would cause user-visible bugs.
- Team operational familiarity matters.

**Choose NoSQL when:**
- Access pattern is a single-entity lookup by a known key (DynamoDB: ~1ms P99 at any scale).
- Write throughput exceeds what a single primary can handle (~10k–50k writes/s is the practical ceiling before you need sharding).
- Schema-on-read wins: heterogeneous documents, evolving fields, sparse attributes (MongoDB, DynamoDB).
- You need horizontal scale-out built into the data model (Cassandra: add nodes, capacity scales linearly).

**When eventual consistency actually hurts you:**
- Inventory management: Two users both see "1 item in stock" and both complete purchase. Without strong consistency or optimistic locking, you oversell.
- Financial ledgers: A balance read by one service must reflect a concurrent debit by another. Stale reads mean incorrect balances.
- Auth token revocation: After revoking a session, a stale replica might still accept the old token for seconds–minutes.
- Leaderboard/counter accuracy: Approximate is usually fine; exact ranking is not.

**When schema-on-read wins:**
- User-generated content with free-form fields (MongoDB documents).
- Event sourcing where each event type has different attributes.
- Rapid iteration where the schema changes every sprint and ALTER TABLE on a 500M-row table is a multi-hour operation.

---

### Database Sharding Strategies

Sharding = splitting a dataset horizontally across multiple database nodes, each owning a disjoint subset.

**Why shard?** A single PostgreSQL primary handles ~10k–50k writes/s and ~1TB–2TB reasonably before latency and vacuuming become problems. Beyond ~500M–1B rows (or ~1TB), sharding or a purpose-built distributed DB becomes necessary.

#### Range Sharding

```
Shard 1: user_id [0, 1M)
Shard 2: user_id [1M, 2M)
Shard 3: user_id [2M, 3M)
```

- Pro: Range scans are efficient (all records in a range on one shard).
- Con: **Hotspots.** If user_ids are assigned sequentially, all new users land on the last shard. Timestamps as shard keys are almost always wrong for write-heavy systems.

#### Hash Sharding

```
shard = hash(user_id) % N
```

- Pro: Even distribution of writes.
- Con: **Rebalancing pain.** Adding a shard from N to N+1 means ~50% of keys move (on average). Must either pre-split (create 1024 virtual shards, map to 4 physical) or use consistent hashing.

#### Consistent Hashing (Dynamo-style, Cassandra, Redis Cluster)

```
Hash Ring (0 to 2^32):

         0
        /|\
       / | \
  270 /  |  \ 90
     /   |   \
    /    |    \
  180----+----
      
  Node A owns arc [0, 90)
  Node B owns arc [90, 180)
  Node C owns arc [180, 270)
  Node D owns arc [270, 360)
  
  Virtual nodes: each physical node has K tokens
  spread around the ring (e.g., K=150 in Cassandra).
  
  Adding Node E: steals one arc slice from each
  existing node — only ~1/N keys move.
```

- Pro: Adding/removing a node moves only ~1/N keys. Virtual nodes (vnodes) balance load even with heterogeneous hardware.
- Con: Routing layer needed (client-side or proxy). Hot partitions still possible if one key receives disproportionate traffic (use key salting).

#### Directory-Based Sharding

A lookup table maps key ranges → shard. Flexible (arbitrary assignment), but the directory is a single point of failure and a bottleneck if not cached.

#### Cross-Shard Queries

- **Avoid them by design.** Denormalize: store all data needed for a query in the same shard.
- Scatter-gather: fan out to all shards, merge results. Works but O(N shards) latency.
- Two-phase commit across shards: correct but expensive (~10ms vs ~1ms single-shard). Avoid unless you absolutely need cross-shard transactions.
- Resharding: requires a dual-write + backfill strategy. Typically: write to old and new shards simultaneously, backfill old data, flip reads, drain old shard. Downtime-free but operationally complex.

---

### Replication

Replication = keeping copies of data on multiple nodes for durability and availability.

#### Primary-Replica (Single Leader)

```
Client Writes               Client Reads
     |                           |
     v                           v
  [Primary] ----async/sync--> [Replica 1]
                           -> [Replica 2]
                           -> [Replica 3]
```

- **Async replication:** Primary acknowledges write immediately. Replicas lag by 1–100ms under normal conditions, can lag by minutes during network issues or replica overload. If primary dies before replica catches up, data is lost (RPO > 0).
- **Sync replication:** Primary waits for at least one replica to acknowledge before confirming to client. Adds 1–5ms RTT per commit (network round-trip). RPO = 0 for synchronous replica, but blocks if that replica is slow.
- **Semi-sync (MySQL):** Primary waits for one replica acknowledgment, then proceeds. Balance of durability and performance.

Replication lag is the #1 source of consistency bugs. A user writes data, then reads from a replica that hasn't caught up — they see stale data. Solutions: read-your-writes consistency (route reads to primary after a write), monotonic reads (always read from the same replica), bounded staleness.

#### Multi-Primary (Multi-Leader)

- Multiple nodes accept writes. Used for multi-datacenter deployments (each DC has its own primary) or offline-capable clients.
- **Conflict resolution is hard.** Last-write-wins (LWW) using timestamps is the simplest but silently discards data when clocks drift. CRDTs (Conflict-free Replicated Data Types) merge concurrent updates mathematically (sets, counters, registers) without data loss but are complex and limited in data model expressiveness.
- Avoid multi-primary unless you need multi-datacenter active-active or offline sync. The conflicts are not worth it for most systems.

#### Leaderless Replication (Dynamo-style: Cassandra, DynamoDB, Riak)

```
         Client
        /  |   \
       W   W    W     (write to W nodes)
      /    |    \
  [N1]   [N2]   [N3]  (N total replicas)
      \    |    /
       R   R    R     (read from R nodes, take latest version)
```

- **Quorum condition: W + R > N** ensures at least one node in the read set has the latest write.
- Common configuration: N=3, W=2, R=2. Tolerates one node failure for both reads and writes.
- Sloppy quorum: During a network partition, writes are accepted by non-home nodes (hinted handoff). More available, but R+W>N no longer guarantees reading the latest value.
- Last-write-wins with vector clocks: Cassandra uses timestamps; Riak uses vector clocks to detect concurrent writes.
- Read repair: On a read, if replicas disagree, the reader updates stale replicas in the background.

---

### CAP Theorem

**The actual statement:** In the presence of a network Partition, a distributed system must choose between Consistency (every read reflects the most recent write) and Availability (every request receives a response, possibly stale).

**"CA systems don't exist"** in distributed settings: If you have a partition (which you cannot prevent in a real network), you must choose C or A. A "CA" system is really just a single-node system — no network partition means no distribution.

**What "partition tolerance" actually means:** It does not mean the system is immune to partitions. It means the system is designed to keep operating when partitions occur, making a deliberate choice between C and A. All distributed systems must be "partition tolerant" in this sense — the question is just C vs A when a partition happens.

| System | CAP Choice | Behavior during partition |
|---|---|---|
| HBase | CP | Refuses writes if RegionServer cannot reach ZooKeeper; availability drops |
| Cassandra | AP | Accepts writes with quorum degraded; may return stale reads |
| ZooKeeper | CP | Read/write requests fail until quorum is restored |
| Couchbase | CP (configurable) | Rejects writes to ensure consistency |
| DynamoDB | AP (default) | Eventually consistent reads; strong consistency available at cost |
| Spanner | CP (external consistency) | Uses TrueTime; sacrifices latency, not consistency |

---

### PACELC: The Extension That Matters

CAP only considers behavior *during partitions*. PACELC adds: even when the system is running normally (no partition), there is a trade-off between **Latency** and **Consistency**.

```
If Partition: choose Consistency (C) or Availability (A)
Else:         choose Latency (L) or Consistency (C)
```

| System | PAC | ELC |
|---|---|---|
| DynamoDB | PA | EL (default eventual; strong consistency doubles latency) |
| Cassandra | PA | EL (ONE vs QUORUM — lower consistency = lower latency) |
| Spanner | PC | EC (always consistent; latency is the cost) |
| PostgreSQL (sync replication) | PC | EC |
| MySQL async replication | PA | EL |

**Why this matters for interviews:** When someone says "Cassandra is AP," the right follow-up is: "Even without partitions, choosing consistency level QUORUM vs ONE is an EL trade-off. Cassandra lets you tune per-query, which means you can implement read-your-writes by routing certain reads at QUORUM."

---

### Read Replicas, Connection Pooling, and PgBouncer

**Read replicas:** Route SELECT queries to replicas to offload the primary. Effective when read:write ratio is high (>10:1). Risk: replica lag means reads may be stale by 1–100ms (normal) to minutes (during lag spikes or failover).

**Connection pooling — why it matters:** Each PostgreSQL connection costs ~5–10MB RAM and a backend process. At 10k concurrent connections, that is 50–100GB RAM on the DB host just for connection overhead, before any actual query work. Connection poolers multiplex many application connections onto a smaller pool of database connections.

**PgBouncer modes:**

| Mode | How it works | Use case |
|---|---|---|
| **Session pooling** | Application gets a dedicated DB connection for the session lifetime | Stateful sessions (SET, prepared statements, advisory locks) |
| **Transaction pooling** | DB connection is held only during a transaction; returned to pool afterward | Most OLTP workloads — highest multiplexing ratio |
| **Statement pooling** | Connection returned after each statement | Rare; breaks multi-statement transactions |

Transaction mode is the recommended default. It allows 1000 application connections to share 20–50 DB connections, reducing per-connection overhead dramatically.

**Connection pool sizing rule of thumb:**
- Target: **10–20x CPU cores** on the database server.
- Example: 32-core DB host → pool size 50–100 DB connections.
- Oversizing the pool does not help; queries queue inside PostgreSQL anyway, and contention increases.

---

## 4. Trade-offs

### Storage Engine Selection

| Scenario | Recommendation | Reason |
|---|---|---|
| OLTP, mixed reads/writes | B-tree (PostgreSQL) | Predictable read latency, mature tooling |
| High write throughput (>50k/s) | LSM (Cassandra, RocksDB) | Sequential writes avoid random I/O |
| Time-series / append-only | LSM with FIFO or TTL | Compaction aligned with data lifecycle |
| Read-heavy analytics on small dataset | B-tree | No bloom filter overhead, fewer page reads |
| Write-heavy with occasional full scan | LSM with leveled compaction | Bounded read amplification post-compaction |

### Sharding Strategy Selection

| Strategy | Best for | Avoid when |
|---|---|---|
| Range | Range scans (time-series queries, reports) | Sequential key writes (hotspot) |
| Hash | Even write distribution, simple key lookup | Range scans across shards |
| Consistent hashing | Elastic scaling, cache clusters | Need strict range queries |
| Directory-based | Arbitrary assignment, migration control | Directory is not highly available |

### Replication Mode Selection

| Mode | Durability | Latency impact | Use case |
|---|---|---|---|
| Async primary-replica | RPO > 0 (up to seconds of data loss) | None | Analytics replicas, reporting |
| Sync primary-replica | RPO = 0 | +1–5ms per commit | Financial data, audit logs |
| Multi-primary | Complex (conflict risk) | Low cross-DC | Multi-DC active-active |
| Leaderless (quorum) | Configurable (W+R>N) | Tunable | Cassandra, DynamoDB-style scale |

---

## 5. Numbers to Know

| Metric | Value | Context |
|---|---|---|
| Max rows before sharding | ~500M–1B rows / ~1TB | PostgreSQL practical ceiling before ops pain |
| B-tree lookup cost | O(log_B N) disk pages | ~4–5 pages for 1B rows with B=100 |
| LSM write amplification | 10–30x | Data rewritten multiple times during compaction |
| Async replication lag (normal) | 1–100ms | Network RTT + apply time |
| Async replication lag (stressed) | Seconds to minutes | Replica overload, network issues |
| Sync replication overhead | +1–5ms per commit | One additional network RTT |
| Connection pool size rule | 10–20x CPU cores | E.g., 32 cores → ~100 DB connections max |
| PostgreSQL max connections | 100–500 (typical config) | Beyond this, overhead dominates |
| PgBouncer multiplexing ratio | 10–50x | 1000 app connections → 20–50 DB connections |
| Quorum formula | W + R > N | N=3, W=2, R=2 is standard |
| Consistent hash ring vnodes | 150–256 per node (Cassandra default) | Balances load across heterogeneous nodes |
| Schema change on large table | Minutes to hours | ALTER TABLE on 500M rows; use online DDL tools |

---

## 6. Interview Tips

### What Interviewers Actually Probe

1. **"Walk me through what happens when a write hits PostgreSQL."** They want: WAL write → buffer pool update → async fsync → replica streaming. Bonus: mention WAL ensures durability even if the process crashes before fsync.

2. **"Why would you choose Cassandra over PostgreSQL?"** Weak answer: "Cassandra is NoSQL." Strong answer: tie it to a specific access pattern — e.g., "write-heavy time-series data where I know the partition key, need sub-millisecond writes at scale, and can tolerate eventual consistency on reads." Then mention the trade-offs you're accepting (no multi-row transactions, complex conflict resolution).

3. **"How do you handle a hot partition?"** Expected: key salting (append a random suffix to the partition key, scatter writes, scatter reads and merge), or redesign the access pattern to avoid the hotspot entirely.

4. **"You have a 2-billion-row user table. How do you shard it?"** Walk through: choose shard key (user_id — hash, not range), consistent hashing for future elasticity, cross-shard query avoidance, resharding strategy (dual-write + backfill).

5. **"Is Cassandra CP or AP? What does that mean operationally?"** CP/AP is tunable in Cassandra via consistency level. At CL=QUORUM, you get stronger consistency. At CL=ONE, you maximize availability and minimize latency. The PACELC frame is more useful — even without partitions, you choose between latency and consistency per query.

### Common Mistakes

- Saying "NoSQL scales, SQL doesn't." SQL can scale with read replicas, sharding, and Aurora-style distributed engines. The real trade-off is operational model and access pattern fit.
- Conflating "eventual consistency" with "fast." Eventual consistency is a *consistency model*, not a performance guarantee.
- Recommending multi-primary without discussing conflict resolution. Interviewers will immediately ask "what happens when two datacenters write to the same key simultaneously?"
- Forgetting that consistent hashing alone does not solve hot partitions — you still need virtual nodes and possibly key salting for skewed key distributions.
- Not knowing the quorum formula. W+R>N is essential for leaderless replication discussions.

### Questions You Will Be Asked

- "Explain write amplification in LSM-trees and when it matters."
- "How does PgBouncer transaction pooling break prepared statements?" (It does — prepared statements are connection-scoped; in transaction mode, the connection changes per transaction. Use parse-on-execute or disable prepared statements with PgBouncer in transaction mode.)
- "What is the difference between read-your-writes consistency and monotonic reads?"
- "How would you do a zero-downtime schema migration on a table with 500M rows?"
- "What does PACELC add to CAP that CAP doesn't capture?"

---

## 7. Resources

1. **Martin Kleppmann, "Designing Data-Intensive Applications" (DDIA)** — Chapters 3 (Storage Engines), 5 (Replication), 6 (Partitioning), 9 (Consistency and Consensus). The canonical reference for everything in this file. https://dataintensive.net/

2. **ByteByteGo — "Database Indexing Strategies"** and "How does sharding work?" — Alex Xu's visual explanations are excellent for interview prep. https://bytebytego.com/

3. **AWS Aurora and DynamoDB documentation** — Aurora's approach to distributed storage (6-way replication, no replica lag on reads) and DynamoDB's single-table design patterns are real-world implementations of these concepts. https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html
