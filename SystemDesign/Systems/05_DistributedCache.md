# Distributed Cache (Redis Cluster / Memcached) — Full System Design Walkthrough

> **Interview Format:** 45 minutes | **Level:** Senior / Staff Engineer  
> **Analogous real systems:** Redis Cluster, Memcached, Amazon ElastiCache, Twemproxy, Pelikan (Twitter)

---

## Table of Contents
1. [Problem Statement](#1-problem-statement)
2. [Clarifying Questions](#2-clarifying-questions)
3. [Functional Requirements](#3-functional-requirements)
4. [Non-Functional Requirements](#4-non-functional-requirements)
5. [Capacity Estimation](#5-capacity-estimation)
6. [High-Level Design](#6-high-level-design)
7. [Deep Dives](#7-deep-dives)
8. [Trade-offs & Alternatives](#8-trade-offs--alternatives)
9. [Failure Scenarios](#9-failure-scenarios)
10. [Interview Tips](#10-interview-tips)

---

## 1. Problem Statement

Design a **distributed, in-memory cache** that sits in front of a database tier to dramatically reduce read latency and database load. The cache must:

- Store key-value pairs entirely in RAM for sub-millisecond access
- Distribute data across a cluster of nodes to scale beyond a single machine's memory
- Survive individual node failures without losing the entire cache
- Support tens of millions of QPS with consistent low latency at the 99th percentile
- Provide flexible eviction when memory pressure increases

Think of this as designing the backbone that powers Facebook's social graph reads, Twitter's timeline service, or any e-commerce product catalog — situations where the same hot data is read millions of times per second and database roundtrips would be catastrophically slow and expensive.

The core tension you are resolving: **memory is finite, data is not**. A distributed cache must make intelligent decisions about which data to keep, how to distribute it evenly, and how to stay available when nodes crash.

---

## 2. Clarifying Questions

Use these questions to drive scope and demonstrate systems thinking. In a real interview, ask 3-4 and then proceed — do not interrogate the interviewer.

### Q1: What are the primary access patterns — mostly reads, or mixed read/write?
**Expected answer:** Heavy read bias, approximately 95% reads and 5% writes. This is the classic pattern for product catalogs, user profiles, session data, and computed feeds.  
**Why it matters:** Dictates replication strategy. Read-heavy workloads benefit from read replicas. A write-heavy workload changes the conversation toward write-through vs write-back and invalidation complexity.

### Q2: Do we need strong consistency between cache and the backing database, or is eventual consistency acceptable?
**Expected answer:** Eventual consistency is fine. A slightly stale profile photo or product price is acceptable for a few seconds. Strong consistency would be required for inventory counts or financial balances.  
**Why it matters:** Strong consistency requires synchronous write-through on every mutation, which adds latency and eliminates the write-back model. Eventual consistency allows async write-behind and background invalidation.

### Q3: What is the expected data size per value, and what is the key space size?
**Expected answer:** Values range from 100B (session tokens) to 100KB (serialized user objects or rendered HTML fragments). Total dataset is approximately 10TB. Active working set is likely 20-30% of that.  
**Why it matters:** Small values allow millions per node. Large values (>100KB) change memory layout, potentially requiring chunking or pointer-based storage. The total dataset size drives cluster node count.

### Q4: What is the acceptable cache miss penalty?
**Expected answer:** A cache miss falls back to the database, which takes 5-20ms. The SLA for a cache hit should be under 1ms at p99.  
**Why it matters:** If the miss penalty is extreme (e.g., an ML inference call that takes 500ms), the thundering herd problem becomes critical and we need distributed locks or probabilistic early expiration.

### Q5: Should the cache be shared across multiple services (look-aside) or embedded in each service tier?
**Expected answer:** Shared cache cluster, accessed as a remote service. Embedding would waste memory and destroy hit rates because each service instance would maintain its own cold cache.  
**Why it matters:** A shared cache centralizes memory budget and maximizes hit rate, but introduces network hops and requires careful key namespacing to prevent cross-service collisions.

### Q6: Do we need multi-region support?
**Expected answer:** Yes, two regions (US-East and EU-West) with independent cache clusters per region. Cross-region cache replication is not required — each region's cache is independently warmed.  
**Why it matters:** Cross-region synchronous replication adds too much latency. Each region maintains its own cluster and warms independently from its local database.

### Q7: What eviction policy should be applied when the cache is full?
**Expected answer:** LRU as the default. Some use cases (video thumbnails, pre-computed feed items) would benefit from LFU. We should support configurable eviction per cache cluster or even per key prefix.  
**Why it matters:** Choosing the wrong eviction policy means the wrong data gets evicted, degrading hit rates and causing thundering herds on the backing store.

---

## 3. Functional Requirements

These are the **must-have** capabilities. Scope tightly in the first 10 minutes of the interview.

| # | Requirement |
|---|-------------|
| FR1 | `GET(key)` — retrieve a cached value by key; return null on miss |
| FR2 | `SET(key, value, ttl)` — store a key-value pair with optional TTL in seconds |
| FR3 | `DELETE(key)` — explicitly invalidate a cached key |
| FR4 | `MGET(keys[])` / `MSET(pairs[])` — batch operations to amortize network round-trips |
| FR5 | `INCREMENT(key, delta)` / `DECREMENT(key, delta)` — atomic counter operations |
| FR6 | TTL-based expiration — keys automatically expire after their configured TTL |
| FR7 | Automatic eviction — when memory is full, evict keys per configured policy |
| FR8 | Cache partitioning across nodes — data is distributed across the cluster |
| FR9 | Node failure handling — cache remains accessible when individual nodes go down |

**Out of scope for this design:**  
- Distributed transactions across keys  
- Secondary indexes or range queries (that's a database, not a cache)  
- Persistence to disk (Redis offers RDB/AOF, but for this design we treat the cache as ephemeral)

---

## 4. Non-Functional Requirements

| Category | Target |
|----------|--------|
| **Latency** | p50 < 0.3ms, p99 < 1ms, p999 < 5ms for GET |
| **Throughput** | 1M QPS reads across the cluster |
| **Availability** | 99.99% uptime (52 minutes downtime/year) — cache miss ≠ outage, but prolonged unavailability will cascade |
| **Consistency** | Eventual consistency; cache-to-DB lag acceptable up to 30 seconds |
| **Durability** | Not required — cache is a performance layer, not a source of truth |
| **Scalability** | Horizontal scale-out by adding nodes without downtime (online resharding) |
| **Memory Efficiency** | Target 70-80% memory utilization per node; leave headroom for replication buffer |
| **Observability** | Per-key hit rate, eviction rate, memory fragmentation ratio, replication lag |

---

## 5. Capacity Estimation

**Be explicit about every assumption. Interviewers want to see structured thinking, not memorized numbers.**

### 5.1 Data Size

```
Total dataset:          10 TB
Working set (hot 20%):   2 TB  ← this is what the cache actually needs to hold
Average value size:      1 KB  (mix of 100B session tokens and 100KB objects)
Number of unique keys:   10 TB / 1 KB = 10 billion keys total
Hot keys in cache:        2 TB / 1 KB  = 2 billion keys
```

### 5.2 Node Count

```
RAM per cache node:          64 GB (standard cloud instance, e.g., r6g.2xlarge)
Usable RAM after OS/overhead: 50 GB (leave 14GB for OS, Redis process, replication buffer)
Nodes for data capacity:     2,000 GB / 50 GB = 40 nodes

With replication factor 2 (1 primary + 1 replica per shard):
Total node count:            40 × 2 = 80 nodes

With 20% buffer for resharding + failure headroom:
Production node count:       ~96 nodes (round up to power of 2 for consistent hashing)
```

### 5.3 Network Bandwidth

```
Read QPS:               1,000,000 QPS
Average value size:     1 KB
Read bandwidth:         1M × 1KB = 1 GB/s cluster-wide

With 40 primary nodes:  1 GB/s / 40 = 25 MB/s per node  ← very comfortable
NIC capacity per node:  10 Gbps = ~1.2 GB/s
Utilization per node:   25 MB/s / 1200 MB/s ≈ 2%  ← network is not the bottleneck
```

### 5.4 CPU / QPS

```
Redis single-threaded throughput:    ~100,000-200,000 QPS per core for GET/SET
Multi-threaded I/O (Redis 6+):       I/O threads handle network, main thread handles commands

QPS per node:           1M / 40 primary nodes = 25,000 QPS per node
Well within Redis capacity per node  ← CPU is not the bottleneck either

Memory is the primary constraint — this is typical for caches
```

### 5.5 Write QPS

```
Total QPS (5% writes):       50,000 writes/second cluster-wide
Writes per node:             50,000 / 40 = 1,250 writes/second per node  ← trivial
Replication lag:             Async replication → replica lags ~1-5ms behind primary
```

### 5.6 TTL / Expiration

```
Keys with TTL:               80% of all keys (20% are permanent session or config data)
Average TTL:                 3600 seconds (1 hour)
Key churn rate:              2B hot keys / 3600 seconds = ~556,000 expirations/second
Lazy expiration:             Redis checks TTL on access; background sweep runs every 100ms
```

---

## 6. High-Level Design

### 6.1 Architecture Overview

```
                          ┌─────────────────────────────────────────────────────┐
                          │                   CLIENT TIER                        │
                          │   App Server 1    App Server 2    App Server 3      │
                          │   ┌──────────┐   ┌──────────┐   ┌──────────┐       │
                          │   │ Service  │   │ Service  │   │ Service  │       │
                          │   │  Logic   │   │  Logic   │   │  Logic   │       │
                          │   └────┬─────┘   └────┬─────┘   └────┬─────┘       │
                          └────────┼──────────────┼──────────────┼─────────────┘
                                   │              │              │
                          ┌────────▼──────────────▼──────────────▼─────────────┐
                          │              SMART CLIENT LIBRARY                    │
                          │   (consistent hashing ring, connection pool,         │
                          │    circuit breaker, pipeline batching)               │
                          └────────┬──────────────┬──────────────┬─────────────┘
                                   │              │              │
               ┌───────────────────┼──────────────┼──────────────┼───────────────────┐
               │                   │   CACHE CLUSTER (Redis Cluster)                  │
               │   Shard 0         │         Shard 1         Shard 2    ...Shard N    │
               │  ┌─────────┐      │        ┌─────────┐   ┌─────────┐               │
               │  │Primary 0│◄─────┘        │Primary 1│   │Primary 2│               │
               │  │ slots   │               │ slots   │   │ slots   │               │
               │  │0-1365   │               │1366-2730│   │2731-4095│               │
               │  └────┬────┘               └────┬────┘   └────┬────┘               │
               │       │ async replication        │             │                    │
               │  ┌────▼────┐               ┌────▼────┐   ┌────▼────┐               │
               │  │Replica 0│               │Replica 1│   │Replica 2│               │
               │  └─────────┘               └─────────┘   └─────────┘               │
               └──────────────────────────────────────────────────────────────────────┘
                                          │             │
                           ┌──────────────▼─────────────▼───────────────┐
                           │           CONTROL PLANE                      │
                           │  ┌─────────────────────────────────────┐    │
                           │  │  Cluster Manager / Gossip Protocol   │    │
                           │  │  (Redis Sentinel or Cluster Bus)     │    │
                           │  └─────────────────────────────────────┘    │
                           │  ┌─────────────────────────────────────┐    │
                           │  │  Config Service  (ZooKeeper / etcd)  │    │
                           │  │  - Cluster topology                  │    │
                           │  │  - Slot-to-node mapping              │    │
                           │  │  - Health state                      │    │
                           │  └─────────────────────────────────────┘    │
                           └──────────────────────────────────────────────┘
                                          │
                           ┌──────────────▼───────────────┐
                           │       BACKING DATABASE         │
                           │   PostgreSQL / DynamoDB /      │
                           │   Cassandra (source of truth)  │
                           └──────────────────────────────┘
```

### 6.2 Component Responsibilities

**Smart Client Library**  
The client library lives in each application server process and is responsible for all routing logic. It maintains a copy of the slot-to-node mapping and routes each key directly to the correct primary node without an intermediary proxy. This is the Redis Cluster model — it eliminates a proxy hop and reduces latency by ~0.2ms. The client handles:
- Consistent hashing or hash-slot routing
- Connection pooling (typically 10-20 connections per shard per client process)
- MOVED and ASK redirect handling (when resharding is in progress)
- Circuit breaking on node failures
- Pipelined multi-key operations (coalescing MGET calls)

**Cache Cluster — Primary Nodes**  
Each primary node is responsible for a contiguous range of hash slots (Redis Cluster uses 16,384 slots total). The primary accepts all reads and writes for its slot range. It propagates writes asynchronously to its replica. Primary nodes participate in the gossip protocol — they ping each other every 100ms and mark nodes as "possibly failed" after 15 seconds of no response, triggering a failover vote.

**Cache Cluster — Replica Nodes**  
Each replica shadows its primary. In the default configuration, replicas serve reads to distribute read load (using READONLY mode in Redis). They are the failover target if the primary dies. Replication is asynchronous — the replica may lag a few milliseconds behind the primary. This means after a primary failure and failover, a small number of recent writes may be lost (the replication buffer not yet flushed).

**Cluster Manager / Gossip Protocol**  
Redis Cluster uses a peer-to-peer gossip protocol for cluster state propagation. Every 100ms each node sends a heartbeat to a random subset of peers, piggybacking the latest state of nodes it has communicated with. There is no single master orchestrator — this makes the system resilient to split-brain. Sentinel (for standalone Redis, not cluster) uses a quorum-based approach to trigger failover.

**Config Service (etcd / ZooKeeper)**  
Stores the canonical cluster topology, used by client libraries to bootstrap and refresh their routing tables. Clients cache this locally and refresh periodically (every 5-10 seconds) or on receiving a MOVED redirect from the server.

**Cache Warming Service**  
A background process that pre-populates the cache after a node failure or cold start by reading from the database in batches. Without warming, a cold start triggers a thundering herd on the backing store (see Deep Dive #4).

### 6.3 Cache Write Strategies

**Write-Through:** Every write to the application goes to the database AND the cache synchronously before returning success. Pros: cache is always warm, no stale data. Cons: write latency doubles, cache is polluted with data that may never be read.

**Write-Back (Write-Behind):** Write to cache first, return success, then asynchronously flush to the database. Pros: extremely low write latency. Cons: data loss window if cache node crashes before flush; complex failure recovery.

**Write-Around:** Write directly to the database, skip the cache. Cache is only populated on read miss (look-aside). Pros: cache not polluted with write-heavy data that won't be read again. Cons: first read after a write always misses the cache.

**Recommendation for this design:** Write-around + look-aside for the majority of use cases. Write-through for high-read, low-write data like user profiles. Write-back only for counters and non-critical analytics.

---

## 7. Deep Dives

### 7.1 Consistent Hashing for Node Placement

**The problem:** When you have N cache nodes and you use `hash(key) % N` to route keys, adding or removing a node changes N, which remaps almost every key (on average `(N-1)/N` of all keys change). This causes a thundering herd — suddenly the entire dataset is a cache miss and the database is hammered.

**Consistent Hashing** solves this by mapping both nodes and keys onto a ring of hash values from 0 to 2^32. Each key is assigned to the first node clockwise on the ring.

```
         0
    330 ──┼────────── 30
   /      |            \
300  ─── RING ──────    60
   \      |            /
    270 ──┼────────── 90
        180

Node A lives at position 60  (hash(NodeA_IP) % 2^32)
Node B lives at position 180
Node C lives at position 270
Node D lives at position 330

Key "user:12345" hashes to position 100 → assigned to Node B (next node clockwise)
Key "product:99"  hashes to position 350 → assigned to Node A (wraps around ring)
```

**Virtual nodes (vnodes):** A single physical node is represented by K virtual nodes at different ring positions (typically K=100-200 in production). This solves two problems:
1. **Load balance:** Without vnodes, if nodes are unevenly distributed on the ring, one node may hold 30% of data while another holds only 5%. With 150 vnodes per physical node, the distribution approximates uniform.
2. **Graceful addition/removal:** When you add a physical node, it takes over slots from its neighbors. With 150 vnodes, it takes small slices from many neighbors rather than a large chunk from one.

**Redis Cluster's approach:** Redis Cluster does not use pure consistent hashing. Instead it uses a fixed hash slot space of 16,384 slots. The slot is computed as `CRC16(key) % 16384`. Each primary is responsible for a range of slots. This makes it easy to move a slot from one node to another — you move the slot's data atomically. The client caches the slot-to-node mapping and receives MOVED redirects when it routes to the wrong node.

```
Hash slot computation:
  slot = CRC16("user:12345") % 16384
  slot = 7823  → assigned to Node 3 (slots 6554-8191)

Hash tags for multi-key operations:
  "user:{12345}:profile" and "user:{12345}:friends"
  Both hash on the substring inside {}, so both land on the same shard
  This allows MGET on keys belonging to the same logical entity
```

**When adding a node:**
1. New node joins the cluster announcing itself via gossip
2. Admin runs `CLUSTER REBALANCE` or manually assigns slots
3. Data for migrating slots is moved key-by-key: source node sends `MIGRATE` commands to destination
4. During migration, ASK redirects temporarily point clients to the new node for those keys
5. Once migration completes, slot ownership updates propagate via gossip (converges in <1 second)
6. Clients receive MOVED redirects on next access, update their routing table

### 7.2 Eviction Policies — LRU vs LFU vs ARC

**Context:** When `used_memory` reaches `maxmemory`, Redis must evict keys to make room. The policy determines which keys are sacrificed.

**Available policies in Redis:**

| Policy | Description |
|--------|-------------|
| `noeviction` | Refuse writes when memory is full. Good for critical data, bad UX (errors) |
| `allkeys-lru` | Evict the least recently used key from all keys |
| `volatile-lru` | Evict LRU only from keys with TTL set |
| `allkeys-lfu` | Evict the least frequently used key from all keys |
| `volatile-lfu` | Evict LFU only from keys with TTL set |
| `allkeys-random` | Evict a random key — almost never appropriate |
| `volatile-ttl` | Evict the key with the nearest expiration time |

**LRU — Least Recently Used**

LRU evicts whichever key was accessed least recently. Pure LRU requires a doubly-linked list maintained in O(1) with a hash map. Redis does not implement exact LRU (it would require too much memory per key). Instead it uses **approximate LRU**: sample 5 (configurable) random keys, evict the one with the oldest access timestamp. At sample size 10, approximation quality is >99% of exact LRU.

```
LRU strength:  Works well when access patterns have temporal locality
               (recently accessed keys will be accessed again soon)
LRU weakness:  Scanning access patterns (e.g., batch jobs reading every key once)
               cause LRU to evict the entire working set before the scan completes,
               destroying hit rate for interactive traffic
```

**LFU — Least Frequently Used**

LFU evicts the key accessed fewest times overall. Redis's LFU implementation uses a Morris counter (logarithmic counter that fits in 8 bits). The counter increments probabilistically: at low counts it increments frequently; at high counts it increments rarely. This prevents popular keys from accumulating unboundedly large counts that would prevent them from ever being evicted after a shift in access patterns.

Redis LFU also applies decay: the counter decays every `lfu-decay-time` minutes (default 1). If a key hasn't been accessed in 5 minutes and decay is 1, the counter is decremented by 5. This allows LFU to adapt to changing access patterns.

```
LFU strength:  Better than LRU for frequency-skewed workloads
               (e.g., social media where 1% of posts get 80% of reads)
               Resistant to cache scan pollution
LFU weakness:  New keys start with counter=5 and may be immediately evicted
               if memory is very tight. Startup penalty for newly hot keys.
```

**ARC — Adaptive Replacement Cache**

ARC maintains two LRU lists: T1 (recently added, accessed once) and T2 (frequently accessed, accessed 2+ times). It also maintains two ghost lists B1 and B2 (contain keys that were evicted from T1 and T2 but no longer their data). The parameter p (boundary between T1 and T2) is dynamically adjusted: if B1 misses are frequent, expand T2 (favor recency); if B2 misses are frequent, expand T1 (favor frequency).

ARC is not natively available in Redis, but is used in systems like ZFS (as the ZFS Adaptive Replacement Cache). For a Redis-based system, you cannot use ARC without significant modifications.

**Recommendation:**
- Default: `allkeys-lru` for most caching use cases (product pages, API responses)
- High cardinality hot-keys: `allkeys-lfu` (top influencer timelines, trending content)
- Mixed: `volatile-lru` if you can set TTLs on non-critical keys and never-evict critical keys (no TTL)
- Never use `noeviction` in a shared cache that holds non-critical data; it will make your service throw errors on writes

### 7.3 Cache Replication and Failover

**Replication topology:** Each primary has exactly one replica in a different availability zone. Do not co-locate primary and replica in the same AZ — an AZ outage would wipe out both.

```
         AZ-1                    AZ-2
    ┌────────────┐          ┌────────────┐
    │ Primary-0  │──async──►│ Replica-0  │
    │ Primary-1  │◄──async──│ Replica-1  │ (cross-AZ primaries)
    └────────────┘          └────────────┘
```

**Replication mechanics (Redis):**

1. **Full resync:** When a replica first connects (or reconnects after a long disconnect), the primary creates an RDB snapshot in memory and streams it to the replica. Simultaneously, writes are buffered in the replication backlog (circular buffer, default 1MB).
2. **Partial resync:** After a short disconnection, the replica sends its replication offset. If the offset is within the backlog window, only the missed commands are replayed. This is much faster than a full resync.
3. **Replication lag:** Typical lag is 1-5ms in the same region. Under heavy write load, it can reach 100ms. The primary does not wait for replica acknowledgment before returning to the client.

**Failover process (Redis Cluster):**

```
T=0:   Primary-3 stops responding
T=15s: Replicas and peers mark Primary-3 as PFAIL (possibly failed)
       after 15 seconds of ping failures (cluster-node-timeout)

T=15s: Replica-3 sends FAILOVER_AUTH_REQUEST to all cluster nodes
       "I have epoch 42, offset 99827, do you authorize my promotion?"

T=15s: Masters (quorum = majority of masters) vote for Replica-3
       Vote: n/2 + 1 masters must agree

T=15s: Replica-3 becomes Primary-3 with new epoch 43
       Gossip propagates new topology to all nodes

T=16s: Clients receive MOVED redirect or detect topology change
       Client library updates routing table

T=∞:   Original Primary-3 recovers (if ever) → rejoins as a replica of new Primary-3
```

**Tuning failover parameters:**
- `cluster-node-timeout 15000` (15s) — balance between fast failover and false positives under network hiccups. Do not set below 5s in cloud environments.
- `cluster-require-full-coverage no` — allow cluster to serve requests even when some slots are unavailable (recommended for high availability at the cost of partial unavailability)
- `min-replicas-to-write 1` — primary refuses writes if it has zero reachable replicas, preventing data loss. Use carefully — it reduces availability.

**Data loss window:** Because replication is asynchronous, some writes acknowledged to the client may not have been replicated when the primary fails. Redis Cluster accepts this as a trade-off. For critical data, use `WAIT numreplicas timeout` to synchronously wait for replica acknowledgment before returning — this trades latency for durability.

### 7.4 Thundering Herd / Cache Stampede

**The problem:** Imagine a product page for a viral post has a 60-second TTL. At second 60, the key expires. Simultaneously, 50,000 requests arrive for that page. All 50,000 see a cache miss, all 50,000 hit the database simultaneously, and the database falls over.

This is the **thundering herd** (also called cache stampede or dogpiling).

**Solution 1: Mutex / Distributed Lock (Cache Lock)**

Only one request is allowed to recompute the value. Other requests wait.

```
function getCached(key):
    value = cache.get(key)
    if value != null:
        return value

    # Cache miss — try to acquire lock
    lock_key = "lock:" + key
    acquired = cache.set(lock_key, "1", NX=True, EX=5)  # 5 second lock TTL
    
    if acquired:
        # This request won the lock — recompute
        value = db.query(key)
        cache.set(key, value, ttl=60)
        cache.delete(lock_key)
        return value
    else:
        # Another request is recomputing — wait and retry
        sleep(50ms)
        return getCached(key)   # recursive retry
```

**Problem with mutex:** If the lock holder crashes, the lock TTL must expire before others can proceed. Sleeping and retrying adds latency for all waiting requests. Under extreme load, the retry loop can become a tight spin.

**Solution 2: Probabilistic Early Expiration (XFetch)**

Don't wait for the key to expire. Probabilistically recompute it slightly before it expires. Each request, when it fetches a key, runs:

```
function getWithEarlyExpiration(key, beta=1.0):
    value, expiry = cache.get_with_ttl(key)
    if value == null:
        return recompute_and_cache(key)

    remaining_ttl = expiry - now()
    delta = compute_time  # how long the last recompute took

    # Probabilistic check: should we refresh early?
    # P(refresh) increases as TTL decreases
    if -delta * beta * log(random()) >= remaining_ttl:
        # Refresh now (in background or inline)
        value = recompute_and_cache(key)

    return value
```

This is the XFetch algorithm. With `beta=1`, one request will refresh the key at the "right" statistical time, spreading the refresh load. No locks required.

**Solution 3: Stale-While-Revalidate**

Serve the stale value while asynchronously refreshing it in the background. The key has two TTLs: a "soft" TTL (when to start background refresh) and a "hard" TTL (when to evict entirely).

```
Soft TTL = 55s (start background refresh)
Hard TTL = 120s (delete key entirely)

At t=55: request receives stale value (from cache)
         background thread kicks off DB query + cache update
At t=57: cache is updated with fresh value
At t=60 and beyond: all requests get fresh value with no DB spike
```

This is the approach used by Varnish (`stale-while-revalidate` header) and by Cloudflare's caching infrastructure.

**Solution 4: Cache Warming Before Cold Start**

For predictable cache invalidation events (deploys, maintenance windows), pre-warm the cache before traffic arrives:

```
Warming pipeline:
  1. Identify top-K keys by frequency (from access logs or analytics)
  2. Batch-read from database
  3. Load into cache via MSET
  4. Only then route traffic to the new node/cluster

Warmup tool reads from database in batches of 1000 keys/batch
Sequential to avoid hammering DB:
  - 1000 keys × 20ms DB roundtrip = 20s for 1000 keys
  - 1M keys = 20,000 seconds → parallelize across 50 workers = 400s
```

**Solution 5: Request Coalescing at the Load Balancer Layer**

For very expensive cache misses, coalesce duplicate in-flight requests at the application tier (similar to how nginx's `proxy_cache_lock` works). The first request for a key triggers the DB call; all subsequent requests for the same key within that window wait for the first request to complete and return the same result.

---

## 8. Trade-offs & Alternatives

### Redis vs Memcached

| Dimension | Redis | Memcached |
|-----------|-------|-----------|
| Data structures | Rich (strings, hashes, lists, sets, sorted sets, streams) | Strings only |
| Persistence | Optional (RDB snapshots, AOF log) | None |
| Clustering | Native (Redis Cluster) | Client-side sharding or Twemproxy |
| Multi-threading | I/O multithreaded (Redis 6+); command execution single-threaded | True multi-threaded |
| Memory efficiency | Higher overhead per key (~64B) | Lower overhead per key (~40B) |
| Replication | Built-in async replication | No built-in replication |
| Pub/Sub | Yes | No |
| Lua scripting | Yes | No |
| Best for | Feature-rich caching, session store, leaderboards, rate limiting | Pure high-throughput string caching |

**When to pick Memcached:** You need extreme memory efficiency (billions of small keys), you need true multi-core utilization, and your access patterns are purely string GET/SET. Facebook's Memcached cluster (Tao) handles billions of reads/second.

**When to pick Redis:** You need data structure operations (sorted sets for leaderboards, streams for event log), pub/sub, or persistence. Redis Cluster is also significantly easier to operate than client-side sharding of Memcached.

### Proxy Layer (Twemproxy / mcrouter)

Instead of a smart client, you can add a proxy tier:
```
  App Servers → Twemproxy → Cache Nodes
```
**Pros:** Client libraries stay simple; proxy handles sharding, connection multiplexing.  
**Cons:** Proxy becomes a bottleneck and single point of failure; adds ~0.1ms latency per hop; harder to scale.

**Recommendation:** Use smart client (no proxy) for new systems. Proxy layer made sense when client libraries were immature; modern Redis cluster clients are excellent.

### Local vs Remote Cache

You can add an **L1 cache** (in-process memory, e.g., Caffeine in Java) in front of the remote Redis cluster:

```
  Request → L1 (Caffeine, 100ms TTL, 256MB) → L2 (Redis, 60s TTL) → DB
```

L1 hits are sub-microsecond. L1 miss falls through to Redis. This dramatically reduces Redis QPS for extremely hot keys (the top 0.01% of keys that get millions of reads/second).

**Problem:** L1 cache is per-process. With 200 app servers, each has its own L1. Invalidation is hard — when a key changes, you must invalidate all 200 L1 caches. Solutions: short TTL (accept brief staleness), pub/sub invalidation messages (Redis Keyspace Notifications), or versioned keys (embed version in key name, old key naturally expires).

### Geo-Distributed Cache

For multi-region deployments, each region runs its own Redis cluster. The global data store (e.g., DynamoDB Global Tables or CockroachDB) is the source of truth, and each region's cache is independently warmed from its local database replica. Cross-region cache replication is generally not worthwhile due to the latency involved.

---

## 9. Failure Scenarios

### Scenario 1: Single Cache Node Failure

**What happens:** Primary node crashes (OOM, hardware failure, network partition).  
**Detection:** Gossip protocol; peers mark node as PFAIL after `cluster-node-timeout` (15s default).  
**Recovery:** Replica is promoted to primary in ~15-20 seconds. During this window:
- All reads/writes to that shard fail
- Application should implement graceful degradation (serve from DB with higher latency)
- Cache miss rate spikes for this shard's key range

**Mitigation:**
- Reduce `cluster-node-timeout` to 5-10s (careful: too low causes false failovers)
- Implement circuit breaker in client: on repeated failures to a shard, fall back to DB immediately
- Log and alert on elevated cache miss rate per shard

### Scenario 2: Network Partition (Split-Brain)

**What happens:** Network divides into two halves. Primary-A can still reach some nodes; Replica-A is in the other partition and gets promoted.

Now there are two primaries serving the same slot range. Writes to old-Primary-A are lost when the partition heals and it steps down.

**Prevention:** Redis Cluster requires a majority of master nodes to agree on promotion. If the partition leaves a minority of masters with some primaries, those primaries can continue serving reads but will refuse writes if `min-replicas-to-write 1` is configured.

### Scenario 3: Cache Node Running Out of Memory

**What happens:** `used_memory` exceeds `maxmemory`. Eviction policy kicks in.  
**If `noeviction`:** Redis returns `OOM command not allowed` on write commands. The application must handle this error.  
**If `allkeys-lru`:** Redis silently evicts old keys. Hit rate degrades. DB load increases.

**Detection:** Alert on `used_memory_rss / maxmemory > 90%`. Alert on eviction rate > 0 if critical keys are involved.  
**Response:** Scale out (add nodes), reduce TTLs to shrink dataset, optimize value sizes.

### Scenario 4: Hot Key / Hot Shard

**What happens:** One key (e.g., a celebrity's profile) receives 500,000 QPS while the shard handles only 50,000 QPS total capacity. The single cache node for that shard becomes a bottleneck.

**Solutions:**
1. **Key replication:** Store the hot key on all nodes: `celeb:12345:shard0`, `celeb:12345:shard1`, ..., read round-robin. Redis does not do this automatically.
2. **Client-side local cache:** L1 Caffeine cache for that specific key (100ms TTL) across all app servers absorbs the load.
3. **Read replicas per shard:** Increase replica count for hot shards to 3-5, route reads round-robin.

### Scenario 5: Mass Invalidation / Cache Flush

**What happens:** A backend data migration invalidates 80% of cache keys simultaneously. All requests suddenly miss the cache.  
**Response:** Controlled invalidation — invalidate in batches with rate limiting. Warm the cache from DB in parallel before completing invalidation. Use canary invalidation (invalidate 1% of keys, observe DB load, proceed in waves).

---

## 10. Interview Tips

### Structure your time — 45 minutes is short

```
0-5 min:   Clarifying questions (pick 3-4, not all 7)
5-10 min:  Functional + non-functional requirements
10-15 min: Capacity estimation (show the math, don't memorize)
15-25 min: High-level design + architecture diagram
25-40 min: Deep dives (let the interviewer guide; have all 4 ready)
40-45 min: Trade-offs, failure scenarios, wrap-up
```

### Common mistakes to avoid

1. **Starting with a single-node Redis diagram.** The interviewer asked for a *distributed* cache. Start distributed.
2. **Ignoring the thundering herd.** This is the most common follow-up question. Have your answer ready (probabilistic early expiration or mutex-based).
3. **Confusing cache invalidation strategies.** Know the difference: TTL expiration (passive), explicit DELETE on write (active invalidation), and event-driven invalidation (via DB change data capture or pub/sub).
4. **Not talking about consistency.** What happens when the cache has stale data? How long is acceptable? What does the application do?
5. **Saying "we'll use Redis" without explaining why.** Compare to Memcached; justify the choice for the specific workload.
6. **Forgetting replication lag.** After primary failure + failover, recently written keys may be missing from the new primary. Is this acceptable? In most caches yes, but for counters (rate limiters, inventory) you need a strategy.

### Phrases that impress interviewers

- "I'd use consistent hashing with virtual nodes to avoid rehashing the entire dataset when we add a node."
- "For thundering herd, I'd implement probabilistic early expiration using the XFetch algorithm — it avoids distributed locks entirely."
- "The eviction policy depends on the access pattern. For this use case, LFU handles the 80/20 distribution better than LRU because it resists scan pollution."
- "We should separate the hot standby (replica) into a different AZ to ensure it survives the same hardware failure that killed the primary."
- "Cache-aside (look-aside) is my default. Write-through adds latency on every write, and write-back introduces data loss risk that's hard to reason about in distributed failure scenarios."
- "To handle the hot shard problem, I'd either fan out the hot key to multiple shards manually or use a local L1 cache in front of Redis for that specific key."

### What to draw on the whiteboard

1. The hash ring with virtual nodes — label a key's path from client to node
2. The write-through vs write-around flow with timing annotations
3. The thundering herd timeline: key expires, concurrent misses, lock or XFetch resolution
4. Failure timeline: primary down → gossip detection → replica promotion → client update

### Know your numbers cold

- Redis: ~100K-200K QPS per node, p99 < 1ms, ~64B overhead per key
- Consistent hashing: O(log N) lookup with sorted ring, O(1) with hash map
- LRU approximation: 5-10 random samples, >99% quality at sample=10
- Replication lag: 1-5ms typical, up to 100ms under heavy write load
- Failover time: ~15-20 seconds with default cluster-node-timeout=15s
- Memory: 64GB usable per r6g.2xlarge, ~50GB after overhead
- Bloom filter: 10 bits/element → ~1% false positive rate

---

*Total estimated reading / prep time: 90 minutes. Practice explaining each Deep Dive section verbally in 5 minutes. The thundering herd deep dive is the highest probability follow-up question at FAANG-level interviews.*
