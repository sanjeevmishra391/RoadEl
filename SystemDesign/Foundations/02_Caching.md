# Caching — Staff-Level System Design Reference

---

## 1. What It Is

Caching is the practice of storing computed or fetched results in a faster, closer layer to serve future requests without repeating expensive work. At Staff level, caching is less about "use Redis" and more about consistency guarantees, failure modes, invalidation strategies, and where in the stack to cache (L1 heap vs L2 shared vs CDN vs DB query cache). The hardest problems in distributed systems — thundering herd, stale reads, cache coherence — all live here.

---

## 2. How It Works

### Cache-Aside Pattern (Lazy Loading)

The most common pattern. The application owns the cache interaction explicitly.

```
                 ┌──────────────────────────────────────────────┐
                 │            CACHE-ASIDE PATTERN                │
                 └──────────────────────────────────────────────┘

  Application                  Cache (Redis)              Database
      │                             │                         │
      │──── GET user:123 ──────────>│                         │
      │                             │                         │
      │<─── MISS (nil) ─────────────│                         │
      │                             │                         │
      │──────────────────── SELECT * FROM users WHERE id=123 >│
      │<─────────────────── {id:123, name:"Alice", ...} ───────│
      │                             │                         │
      │──── SET user:123 {..} EX 300 ──────────────────────>  │
      │                             │                         │
      │<─── OK ─────────────────────│                         │
      │                             │                         │
      │  [Next request]             │                         │
      │──── GET user:123 ──────────>│                         │
      │<─── HIT {id:123, ...} ──────│  (no DB hit)            │

  Write path (separate — cache is NOT automatically updated):
      │──────────────────── UPDATE users SET name=... ───────>│
      │<─────────────────── OK ────────────────────────────────│
      │──── DEL user:123 ──────────>│  (invalidate)            │
      │<─── OK ─────────────────────│
```

**Properties:**
- Application code is tightly coupled to cache logic
- Cache only contains data that was actually requested (no wasted memory)
- Write path: DEL (invalidate) is safer than SET (avoids write-race where a slow DB read overwrites a newer value)
- On cache restart: cold start causes DB stampede (mitigate with warm-up or probabilistic early expiration)

---

### Write-Through

Every write goes to cache AND database synchronously before returning to client.

```
  Application          Cache (Redis)            Database
      │                     │                       │
      │── SET user:123 ────>│                       │
      │                     │── INSERT/UPDATE ─────>│
      │                     │<── OK ────────────────│
      │<── OK ───────────────│                       │

Reads:  always cache hits (data always present after first write)
Latency: write latency = cache write + DB write (synchronous, higher)
Risk: cache restarts lose all data → cold cache problem on restart
```

**Best for:** Read-heavy workloads where you can't tolerate cache misses. User sessions, profile data, product catalog.

---

### Write-Back (Write-Behind)

Writes go to cache only. Cache asynchronously flushes to database in batches.

```
  Application          Cache (Redis)            Database
      │                     │                       │
      │── SET user:123 ────>│                       │
      │<── OK (immediate)───│                       │
      │                     │                       │
      │                     │  [async, batched]      │
      │                     │── flush ─────────────>│
      │                     │<── OK ────────────────│

Latency: write = cache only (~sub-millisecond)
Risk: data loss if cache dies before flush
Consistency: eventual — DB is stale until flush interval
```

**Best for:** Write-heavy workloads where durability can be relaxed. Gaming leaderboards, analytics counters, session activity. NOT for financial transactions, order state, anything requiring ACID guarantees.

---

### Write-Around

Writes bypass cache, go directly to database. Cache is populated only on read misses.

```
  Application          Cache (Redis)            Database
  [WRITE]
      │───────────────────────────────── INSERT ────>│
      │<─────────────────────────────── OK ──────────│
      (cache not touched on write)

  [READ - first time]
      │── GET user:123 ────>│  MISS                  │
      │──────────────────── SELECT ... ─────────────>│
      │<────────────────────────────────────────────-│
      │── SET user:123 ────>│                        │

Purpose: prevents cache pollution from write-once-read-never data
Example: bulk import of historical data that will never be queried live
```

---

## 3. Deep Dive

### Redis vs Memcached

| Dimension | Redis | Memcached |
|---|---|---|
| Data structures | String, Hash, List, Set, Sorted Set, HyperLogLog, Stream, Geo | String only |
| Persistence | RDB snapshots + AOF log | None (pure in-memory) |
| Replication | Primary-replica (async) + Sentinel + Cluster | None (client-side sharding) |
| Lua scripting | Yes (atomic multi-key operations) | No |
| Pub/Sub | Yes | No |
| Transactions | MULTI/EXEC (optimistic, no rollback) | No |
| Memory efficiency | Overhead per key ~100 bytes | Overhead per key ~60 bytes |
| Multi-threading | Single-threaded event loop (Redis 6+ has I/O threading) | Multi-threaded |
| Max throughput | ~100k-200k ops/sec (single node, simple ops) | ~200k-400k ops/sec (multi-threaded) |
| Cluster sharding | Built-in (Redis Cluster, 16384 hash slots) | External (consistent hash at client) |
| Use Redis when | You need sorted sets, pub/sub, persistence, Lua atomicity, geospatial | Simple KV cache, max raw throughput, simpler ops model |
| Use Memcached when | Pure string cache, multi-threaded performance matters, horizontal simplicity | |

**When to reach for Memcached in 2024:** Rarely. Redis has closed most performance gaps and Memcached's lack of persistence, replication, and data structures makes it harder to operate. Main remaining case: you need >200k simple string GET/SET ops/sec from a single node and every byte of memory overhead matters.

**Redis Cluster:** Keyspace partitioned into 16384 hash slots. Each master owns a slice. Keys are hashed with CRC16 mod 16384. Multi-key operations across slots require hash tags: `{user}.profile` and `{user}.sessions` always map to the same slot. Cluster rebalancing is online but can cause brief pauses.

---

### Cache Eviction Policies

**LRU (Least Recently Used):**
- Evicts the item not accessed for the longest time
- Approximated in Redis using a probabilistic algorithm: sample N random keys, evict the one with oldest access time (configurable via `maxmemory-samples`, default 5)
- True O(1) LRU requires a doubly-linked list + hash map (LinkedHashMap in Java)
- Weakness: scan resistance — a sequential scan of rarely-used data pollutes the cache and evicts hot items

**LFU (Least Frequently Used):**
- Evicts the item accessed least often
- Redis LFU: uses a Morris counter (logarithmic frequency approximation) + decay factor (`lfu-decay-time`) to handle aging
- Better than LRU for Zipfian access patterns (small hot set + large cold tail)
- Redis config: `maxmemory-policy allkeys-lfu`
- Weakness: new items start with frequency 0 → vulnerable to eviction before warming up

**TTL (Time-To-Live):**
- Not strictly an eviction policy — items expire regardless of access pattern
- Redis: lazy expiration (checked on access) + active expiration (background thread samples 20 random keys with TTL every 100ms, removes expired ones if >25% expired → repeats)
- Use TTL for: session tokens, rate limit windows, OTP codes, external API response caches where freshness matters

**Redis `maxmemory-policy` options:**
```
noeviction        — reject writes when full (safe for primary data store)
allkeys-lru       — evict any key by LRU (general cache)
volatile-lru      — evict only TTL-bearing keys by LRU
allkeys-lfu       — evict any key by LFU (Zipfian workloads)
volatile-lfu      — evict only TTL-bearing keys by LFU
allkeys-random    — random eviction (don't use)
volatile-ttl      — evict keys closest to expiration first
```

---

### Cache Stampede / Thundering Herd

When a hot cache key expires, all concurrent requests find a MISS and simultaneously query the database — generating a load spike that can cascade into an outage.

```
  t=0: key expires
  t=0: 500 concurrent requests hit cache → all see MISS
  t=0: 500 database queries fire simultaneously
  t=0: DB CPU spikes to 100%, latency climbs, queries start timing out
  t=1: some requests fail, retry storm begins
  t=1: cascading failure
```

**Solution 1: Mutex / distributed lock**
```python
def get_data(key):
    value = cache.get(key)
    if value:
        return value

    lock_key = f"lock:{key}"
    acquired = cache.set(lock_key, 1, nx=True, ex=5)  # SET NX EX
    if acquired:
        try:
            value = db.query(key)
            cache.set(key, value, ex=300)
            return value
        finally:
            cache.delete(lock_key)
    else:
        # Another thread is fetching — wait briefly and retry
        time.sleep(0.05)
        return get_data(key)  # recursive retry
```
Risk: lock holder dies → thundering herd after lock TTL expires. Lock TTL must be > max DB query time.

**Solution 2: Probabilistic Early Expiration (XFetch)**

Instead of expiring at TTL, probabilistically recompute early. Developed by Vattani et al. (2015), adopted by several caches.

```python
import math, random, time

def get_with_xfetch(key, ttl, beta=1.0):
    value, delta, expiry = cache.get_with_metadata(key)
    # delta = time to recompute last time (measured)
    # expiry = absolute expiry time
    if value is None or (time.time() - beta * delta * math.log(random.random())) >= expiry:
        # Recompute
        start = time.time()
        value = db.query(key)
        delta = time.time() - start
        cache.set_with_metadata(key, value, delta, ttl)
    return value
```

This allows a single request to start recomputing before expiry, proportional to how expensive recomputation is. No lock needed; only one or a small number of requests do early recomputation.

**Solution 3: Background Refresh (Stale-While-Revalidate)**

Cache always returns current value (possibly stale), recomputes asynchronously when TTL crosses a threshold.

```
[Normal flow]
  t=270s into 300s TTL:
  Thread A: GET → returns stale value + triggers async refresh job
  Thread B: GET → returns same stale value (no blocking)
  [Background] → DB query → SET new value → Thread C gets fresh value

Latency: always cache speed (no blocking DB call on critical path)
Staleness window: up to `stale-while-revalidate` seconds (configurable)
```

This is the HTTP `Cache-Control: stale-while-revalidate` semantic, and can be replicated in application caches.

**Solution 4: Request Coalescing (Collapse)**

Nginx, Varnish, and Fastly have built-in request coalescing: if 100 requests arrive for the same uncached resource, only ONE upstream request is made; the other 99 wait and receive the same response. This is `proxy_cache_lock` in Nginx.

---

### Hotspot Problem

A single cache key receives disproportionate traffic (e.g., a celebrity tweet, a viral product page). Even in a Redis Cluster, all traffic routes to the single shard owning that key.

**Mitigation strategies:**

1. **Key replication / local mirroring:** Replicate the hot key across N shards as `key:0`, `key:1`, ..., `key:N-1`. Client randomly picks one. Trades N× memory for N× throughput. Best for read-only or infrequently-updated data.

2. **Client-side (L1) heap cache:** Cache in the application process heap (JVM HashMap, Python dict, Caffeine in Java). Eliminates network entirely. Typical: 500ms-5s TTL, small capacity (top-K keys only). Must be invalidated via pub/sub or accept brief staleness.

3. **Read replicas:** Redis read replicas can serve GET traffic. Add replicas for the shard under pressure. Adds ~replication lag (~1-10ms) for reads.

4. **Request batching at application layer:** If multiple in-flight requests need the same key, batch them with a Promise/Future so only one Redis call is made. (Dataloader pattern from Facebook/GraphQL.)

---

### Cache Coherence in Distributed Systems

Multiple application servers each have L1 in-process caches pointing at the same Redis + same database. A write on Server A invalidates Redis but Server B still holds the stale value in its heap cache.

```
  Server A (writes)         Redis (L2)           Server B (reads)
      │                        │                       │
      │── UPDATE user:123 ─────────────────────────────────> DB
      │── DEL user:123 ────────>│                       │
      │                        │                       │
      │                        │  Server B's L1 cache: │
      │                        │  still has stale data │
      │                        │  for 500ms TTL        │
      │                        │                       │
      │  [pub/sub invalidation]│                       │
      │── PUBLISH invalidate:user:123 ─────────────────────>│
      │                        │  Server B: DEL from L1│
```

**Patterns:**
- **Write-invalidate:** On write, invalidate all copies. Simple, safe. Causes temporary cache misses.
- **Write-update:** On write, push new value to all caches. More complex (need to reach all nodes), avoids miss window.
- **Eventual consistency + TTL:** Accept brief staleness. Set L1 TTL to 100-500ms. Works for most read-heavy use cases.
- **Pub/sub invalidation:** Redis pub/sub channel per entity type. Writers publish invalidation events. All servers subscribe and DEL from local cache. Near-real-time coherence with low overhead.

---

### Multi-Level Cache Hierarchy

```
                    ┌──────────────────────────────────────────┐
                    │        MULTI-LEVEL CACHE HIERARCHY        │
                    └──────────────────────────────────────────┘

 User Request
      │
      ▼
 ┌──────────┐   HIT (5ms)    ──────────────────────────────> Response
 │  CDN PoP │   MISS ─────────────────────────────────────────────┐
 │  (Akamai/│                                                      │
 │Cloudfront│                                                      ▼
 └──────────┘                                         ┌───────────────────┐
      │ MISS                                           │  App Server (L1)  │
      ▼                                               │  In-process cache │
 ┌──────────┐   HIT (1-5ms)  ──────────────────────> │  Caffeine/LRU Map │
 │  App L1  │   MISS ──────────────────────────────> │  ~10k entries     │
 │  (heap)  │                                        │  TTL 100-500ms    │
 └──────────┘                                        └───────────────────┘
      │ MISS
      ▼
 ┌──────────┐   HIT (1-5ms)  ──────────────────────> Response
 │  Redis   │   MISS ────────────────────────────────────────────┐
 │  (L2)    │                                                     │
 └──────────┘                                                     ▼
      │ MISS                                         ┌───────────────────┐
      ▼                                              │    Database       │
 ┌──────────┐   Query (10-100ms)                    │  Postgres/MySQL   │
 │    DB    │<──────────────────────────────────────│                   │
 │  (L3)   │                                        └───────────────────┘
 └──────────┘

Typical hit rates:
  CDN: 85-95% (static assets), <5% (personalized API responses)
  L1 heap: 60-80% (hot key coverage depends on heap size)
  Redis L2: 90-99% (most DB-backed data)
  DB: last resort
```

---

## 4. Trade-offs

### Write Strategy Selection

| Strategy | Consistency | Write Latency | Read Latency | Data Loss Risk | Best For |
|---|---|---|---|---|---|
| Cache-aside | Eventual (miss window) | DB only | Low (after warm-up) | None | General read-heavy CRUD |
| Write-through | Strong | High (cache + DB sync) | Low (always warm) | Low | User sessions, profiles |
| Write-back | Eventual | Very low (cache only) | Low | High (unflushed writes) | Counters, leaderboards |
| Write-around | Eventual | Low (DB only) | High (cold on first read) | None | Bulk imports, write-once data |

### Redis Persistence Trade-offs

| Config | Durability | Performance impact | Recovery time |
|---|---|---|---|
| No persistence | None | Maximum throughput | Instant start, all data lost |
| RDB (snapshot every 60s) | Lose up to 60s of writes | Minimal (fork-based) | Fast (load snapshot) |
| AOF (fsync every second) | Lose up to 1s of writes | ~10-20% overhead | Slow (replay log) |
| AOF (fsync always) | Lose 0 writes | 2-3x slower writes | Slow |
| RDB + AOF | Near-zero loss | Moderate | AOF used on restart |

For a pure cache (data reconstructible from DB), disable persistence. For Redis as a primary data store (sessions, rate-limit counters), use RDB+AOF.

---

## 5. Numbers to Know

| Metric | Value | Notes |
|---|---|---|
| Redis GET/SET throughput | ~100k–200k ops/sec | Single node, simple operations, commodity hardware |
| Redis GET/SET throughput (pipelining) | ~500k–1M ops/sec | Batch multiple commands, fewer round trips |
| Redis ZADD (sorted set) | ~50k–80k ops/sec | O(log N) per operation |
| Redis latency (same DC) | 0.1–0.5 ms | Sub-millisecond RTT on LAN |
| Redis latency (cross-AZ) | 0.5–2 ms | Additional network hop |
| Memcached GET/SET throughput | ~200k–400k ops/sec | Multi-threaded advantage |
| Typical cache hit rate (target) | 90–99% | Below 80% = cache not effective |
| Cache hit rate breakeven | ~95% | At lower rates, DB is often cheaper |
| L1 heap cache latency | <0.01 ms | In-process, no network |
| Redis cluster max keys | 2^32 per shard | ~4 billion keys per Redis instance |
| Redis memory per simple string key | ~50–100 bytes overhead | Plus value size |
| Redis memory per hash field | ~50 bytes overhead | Use hashes to pack related fields |
| Memory rule of thumb | 10-20% more than dataset | AOF rewrite needs 2x memory temporarily |
| CDN cache hit latency | 5–30 ms | Depends on PoP geography |
| Thundering herd threshold | >50 concurrent misses | Rule of thumb for when stampede becomes dangerous |
| XFetch beta parameter | 1.0 (default) | Higher = earlier recomputation, lower staleness |
| Redis key expiry scan rate | ~20 random keys per 100ms | Active expiration background loop |
| Redis replication lag (same DC) | 1–10 ms | Async replication |
| Write-back flush interval (typical) | 100ms–5s | Depends on loss tolerance |

**Memory sizing formula:**
```
Required Redis memory = (number of keys) × (avg value size + key overhead)
                      × 1.2 (Redis memory allocator overhead)
                      × 1.5 (headroom for eviction + AOF rewrite)

Example: 10M keys × 500 bytes avg × 1.2 × 1.5 = ~9 GB
Rule of thumb: provision 50% more than the raw data size
```

**Cache sizing for hit rate target:**
Use Zipf's law: top 20% of items receive 80% of traffic. To achieve 90% hit rate, you typically need to cache the top 20% most popular items. For 10M items, 2M entries = 90% hit rate. This is why small caches can be very effective — the access distribution is highly skewed.

---

## 6. Interview Tips

### What Interviewers Probe

1. **"How do you handle cache invalidation?"** — This is one of the two hard problems in CS. Strong answer: distinguish TTL-based expiry (simple, eventual) from event-driven invalidation (DEL on write, pub/sub broadcast for L1 caches). Discuss why SET on write is dangerous (race condition: read A starts → DB write B → cache write B → cache write A overwrites B). Always prefer DEL over SET on the write path.

2. **"How would you design a caching layer for a social media feed?"** — Feeds are user-specific (low CDN utility) + write-heavy (fan-out on write). Strong answer: pre-computed feed per user (write-through on post creation), Redis sorted set keyed by `feed:{user_id}` sorted by timestamp, LRU eviction for inactive users. Discuss: celebrity problem (user with 50M followers = 50M sorted set inserts per tweet → fan-out on read for celebrities instead).

3. **"What is a thundering herd and how do you solve it?"** — Expect to name at least 2 solutions: mutex lock with queuing, probabilistic early expiration (XFetch), background refresh, request coalescing.

4. **"Redis vs Memcached — when would you pick each?"** — Memcached: pure string cache, multi-threaded throughput, simpler. Redis: any time you need sorted sets, pub/sub, persistence, Lua scripting, or geospatial. In 2024, the answer is almost always Redis.

5. **"How do you avoid hotspot problems in Redis Cluster?"** — Key replication with random shard selection, local L1 in-process cache, read replicas, request coalescing. The interviewer wants to see that you know Redis Cluster doesn't help for single hot keys.

### Common Mistakes

- **Using SET instead of DEL on write:** Creates a window where a slow database read overwrites a newer write. Always invalidate (DEL) unless you can guarantee the write order.
- **Ignoring the cold cache problem:** Write-through looks great on paper but after a Redis restart, every read is a DB miss. Need warm-up strategy or graceful degradation.
- **Flat TTL without jitter:** If 1M keys all expire at exactly TTL=3600s after a deployment, you get a synchronized stampede. Add random jitter: `TTL = base_ttl + random(0, jitter_window)`.
- **Not sizing Redis memory correctly:** Forgetting the 1.2× allocator overhead and 2× temporary spike during AOF rewrite/RDB fork leads to OOM kills in production.
- **Cache everything:** Caching data with very low hit rates (unique user-specific, highly dynamic) wastes memory and adds complexity without benefit. Measure hit rate; if below ~80%, reconsider.
- **Neglecting cache stampede in distributed systems:** Mutex lock works for single-node; in distributed systems you need a distributed lock (Redis SET NX EX) or probabilistic approaches.

### Questions You'll Be Asked

- "Explain cache-aside vs write-through. When would you use each?"
- "What is cache stampede / thundering herd? How do you prevent it?"
- "How would you cache a user's social media timeline?"
- "Redis vs Memcached — which would you choose and why?"
- "How do you handle cache coherence across 100 application servers?"
- "How do you size a Redis cluster for 10M active users?"
- "What eviction policy would you use for a recommendation cache with Zipfian access patterns?"
- "How does write-back caching work, and what are its risks?"
- "Design a multi-level cache for a product page serving 1M RPS globally."

---

## 7. Resources

- [Redis Documentation — Eviction Policies](https://redis.io/docs/manual/eviction/) — Official Redis docs on maxmemory-policy, LRU/LFU approximation algorithms, and memory limits
- [Vattani et al. — Optimal Probabilistic Cache Stampede Prevention (XFetch)](https://cseweb.ucsd.edu/~avattani/papers/cache_stampede.pdf) — The academic paper behind probabilistic early expiration; readable and directly applicable
- [Martin Fowler — Patterns of Enterprise Application Architecture (Cache patterns)](https://martinfowler.com/bliki/TwoHardThings.html) — Foundational reference; his writing on cache-aside, read-through, and invalidation patterns remains the clearest summary
