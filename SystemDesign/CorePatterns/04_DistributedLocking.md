# Distributed Locking

## 1. What It Is

A distributed lock ensures mutual exclusion across multiple processes or machines — only one process holds the lock at a time for a shared resource. It exists because traditional OS mutexes only work within a single process, and distributed systems require coordination across machines where shared memory is unavailable and network failures are expected.

---

## 2. The Problem It Solves

**Scenario: inventory reservation system.**

Without a distributed lock:
- Two order services simultaneously check inventory: both see 1 unit of item X available.
- Both decrement inventory and confirm the sale.
- Result: inventory goes to -1. Oversold.

Traditional database locks work if both services use the same DB transaction — but in microservices, the lock must work across service instances, across data centers, across language runtimes.

Other scenarios requiring distributed locks:
- Cron job that should run on exactly one node (leader election).
- Cache stampede prevention: only one thread rebuilds a cache entry.
- Distributed counter with read-modify-write.

---

## 3. How It Works

### Why Single-Node Redis SETNX Is Unsafe

```
SETNX lock:resource processId    # Set if Not eXists
EXPIRE lock:resource 10          # Set TTL separately

RACE CONDITION:
  Process A:  SETNX lock → success
  Process A:  crashes before EXPIRE
  → lock never expires → deadlock forever

FIX:
  SET lock:resource processId NX EX 10  # Atomic SET + NX + TTL
```

But even the atomic SET has problems:
1. **GC pause:** Process A holds lock. JVM GC pause for 15 seconds. Lock expires (TTL=10s). Process B acquires lock. GC resumes. Now BOTH think they hold the lock.
2. **Network partition:** Lock expired while process was working. It writes back to shared resource as if it still holds the lock.

### Redlock Algorithm (5-Node Redis)

Designed for single-node failure tolerance. Uses a majority quorum of independent Redis instances.

```
5 independent Redis nodes (no replication between them):

Process A acquiring lock "resource:X":

Step 1: Note current time T1
Step 2: Try to SET lock on all 5 nodes sequentially:
        SET "lock:resource:X" uniqueId NX PX 10000  (TTL=10s)
        Use a small per-node timeout (e.g., 50ms) to not block on slow nodes.

Step 3: Count successes. Need majority = 3 of 5.

Step 4: Compute elapsed time: T2 = now - T1
        Valid lock time = TTL - T2 - clock_drift_factor
        e.g., 10000ms - 200ms - 200ms = 9600ms

Step 5: If acquired on ≥ 3 nodes AND valid_lock_time > 0:
            Lock held successfully for valid_lock_time
        Else:
            Release lock on ALL nodes (even failed acquisitions)
            and retry after random backoff

ASCII: Redlock Quorum

Node 1: [LOCKED ✓]
Node 2: [LOCKED ✓]    → 3 of 5 = majority → LOCK ACQUIRED
Node 3: [LOCKED ✓]
Node 4: [TIMEOUT ✗]   → node slow/down
Node 5: [FAILED ✗]    → network partition

Release: send DEL to all 5 nodes:
  if GET lock:resource:X == uniqueId:
      DEL lock:resource:X
  (Lua script for atomicity)
```

**Why uniqueId?** Process must only release its own lock. Without uniqueId check:
- Process A's lock expires.
- Process B acquires lock.
- Process A (recovered from GC pause) releases lock → accidentally releases B's lock.

**Release Lua script (atomic check-and-delete):**
```lua
if redis.call("GET", KEYS[1]) == ARGV[1] then
    return redis.call("DEL", KEYS[1])
else
    return 0
end
```

### The Clock Skew Problem (Kleppmann's Critique)

Martin Kleppmann (author of "Designing Data-Intensive Applications") argued that Redlock is fundamentally unsafe because:

1. **Clock jumps:** Redis uses `gettimeofday()` for TTL. NTP corrections can cause a clock to jump forward, causing a key to expire sooner than expected.
2. **GC pauses:** Even with majority quorum, after acquiring the lock, a GC pause can last longer than the TTL. The lock expires mid-critical-section.
3. **No crash safety:** If a Redis node crashes after acquiring the lock on 3 nodes and restarts quickly (before its TTL expires from another node's perspective), it may grant the lock again.

Kleppmann's conclusion: **Redlock provides neither the safety guarantees of a CP system nor the high availability of an AP system.** For strong guarantees, use a CP system (ZooKeeper, etcd).

**Antirez (Redlock author) disagrees:** He argues clock drift is bounded in practice and the algorithm is safe under reasonable operational assumptions. The debate is unresolved.

**Practical guidance:**
- **Redlock for:** efficiency optimization (e.g., prevent cache stampede, deduplicate work). A failure means duplicate work, not data corruption.
- **ZooKeeper/etcd for:** safety-critical locks (e.g., exactly-once write to a financial system). Failure must not lead to incorrect behavior.

### ZooKeeper / etcd Locking

ZooKeeper uses **ephemeral nodes** — they are automatically deleted when the client session ends (crash, disconnect).

```
ZooKeeper Distributed Lock:

Process A creates ephemeral node: /locks/resource/lock-0000000001
Process B creates ephemeral node: /locks/resource/lock-0000000002

Each process lists /locks/resource/ and sorts by sequence number.
Lowest sequence number = lock holder.

Process A holds lock (has lock-0000000001).
Process B watches lock-0000000001 for deletion.

Process A crashes:
  → Session expires (ZK session timeout ~30s by default)
  → ZK deletes ephemeral node lock-0000000001 automatically
  → Process B is notified → acquires lock

No lock renewal needed; the session is the lease.
```

**etcd locking** (used in Kubernetes):
```
PUT /locks/resource  value=processId  lease=leaseId
// leaseId has a TTL; process must KeepAlive (heartbeat) to renew
// If process crashes, lease expires → lock released automatically
```

---

## 4. Fencing Tokens

Fencing tokens solve the GC-pause / lock-expiry problem even when you use distributed locks.

```
Problem:
  1. Client A acquires lock, receives token 33
  2. Client A pauses for 45s (GC / network hiccup)
  3. Lock expires. Client B acquires lock, receives token 34
  4. Client B writes with fencing token 34 → storage accepts
  5. Client A resumes, tries to write with fencing token 33
  6. Storage REJECTS token 33 (33 < 34 = stale)

ASCII: Fencing Tokens

Lock Service:        33          34
                     │           │
Client A ──[acquire]─┘  [pause]  │
Client B          [acquire]──────┘──[write:34]──OK
Client A                     [write:33]──REJECTED (stale)

Storage layer enforces: only accept writes with token >= highest_seen_token
```

Fencing token = monotonically increasing counter from the lock service. Every lock acquisition gets a new, higher token. The resource (DB, storage) must enforce the ordering.

Implementation: ZooKeeper's sequential ephemeral nodes provide fencing naturally — the zxid (transaction ID) is monotonically increasing.

---

## 5. Lease-Based Locking

A **lease** is a lock with a time-bounded validity granted by a trusted authority.

```
Client ─────────────────────────── Lock Server
  │  request lease (TTL=30s)          │
  │ ────────────────────────────────► │
  │  grant lease + token (fencing)    │
  │ ◄──────────────────────────────── │
  │                                   │
  │  renew lease every ~10s           │
  │ ────────────────────────────────► │  (heartbeat)
  │  renewed                          │
  │ ◄──────────────────────────────── │
  │                                   │
  │  [crash — no renewal]             │
  │                                   │
                          30s expires → lock released
                          Other client can acquire
```

Lease renewal must happen on a different thread/goroutine than the critical section. If the critical section is slow (DB latency, GC), the renewal thread keeps the lease alive.

**Lease vs Redlock:** Lease requires a trusted lock service (ZooKeeper, etcd, Chubby). Redlock uses a quorum of simple KV stores but provides weaker guarantees.

---

## 6. When NOT to Use Distributed Locks

Before reaching for a distributed lock, consider these alternatives:

| Scenario | Better Alternative |
|----------|-------------------|
| Preventing duplicate writes | Idempotency key + DB unique constraint |
| Cron job deduplication | Database-level advisory lock / idempotent check |
| Cache stampede | Promise coalescing / single-flight pattern |
| Inventory reservation | Optimistic locking (CAS) in DB + retry |
| Leader election | ZooKeeper / etcd (purpose-built, not Redis) |
| Rate limiting | Atomic counters (INCR/DECR) — no lock needed |

**Idempotent operations are almost always better than locks.** If your write operation is idempotent (same result if applied multiple times), you don't need a lock — just apply it and let the last writer win.

---

## 7. Trade-offs

### Redlock
| Aspect | Detail |
|--------|--------|
| Safety | Probabilistic (depends on clock drift bounds) |
| Availability | High (works if 3/5 nodes are up) |
| Latency | 5 round-trips to Redis nodes (can be parallelized) |
| Failure mode | GC pause > TTL → two processes think they hold lock |
| Use case | Efficiency (not safety-critical) |

### ZooKeeper / etcd
| Aspect | Detail |
|--------|--------|
| Safety | Strong (CP system; linearizable writes) |
| Availability | Lower (quorum required; ZK has session overhead) |
| Latency | Higher (consensus protocol, fsync to disk) |
| Failure mode | Client disconnect → session timeout → lock release (may be slow ~30s) |
| Use case | Safety-critical coordination (leader election, schema changes) |

---

## 8. Where It Appears in Real Systems

| System | Usage |
|--------|-------|
| **Google Chubby** | Distributed lock service for GFS, Bigtable leader election |
| **Apache ZooKeeper** | Hadoop leader election, Kafka controller election |
| **etcd** | Kubernetes leader election for controller manager, scheduler |
| **Redis (Redlock)** | Cache stampede prevention, job deduplication in many startups |
| **HBase** | ZooKeeper for master election and region assignment |
| **Apache Kafka** | ZooKeeper (legacy) / KRaft (new) for controller election |
| **Consul** | Distributed locks via sessions for service registration |

---

## 9. Numbers to Know

| Metric | Value |
|--------|-------|
| Recommended Redlock TTL | 10–30 seconds |
| Redlock: nodes needed for safety | 3 of 5 (majority quorum) |
| ZooKeeper session timeout (default) | 30 seconds |
| ZooKeeper write latency (fsync) | ~1–5ms per write |
| etcd write latency | ~1–10ms per write |
| Redis SET NX PX latency | ~0.1ms |
| GC pause that breaks Redlock | > lock TTL (e.g., > 10s for a 10s lock) |
| Fencing token: ZooKeeper zxid | 64-bit monotonically increasing integer |
| etcd lease TTL typical | 5–30 seconds with heartbeat every TTL/3 |

---

## 10. Interview Tips

### What You'll Be Asked

**"How do you implement a distributed lock?"**
- Start with `SET key value NX EX ttl` — explain atomicity.
- Immediately address the GC pause problem → introduce fencing tokens.
- If multi-node needed → Redlock, then mention Kleppmann's critique.
- Pivot to ZooKeeper/etcd for safety-critical use cases.

**"What's wrong with using Redis for distributed locking?"**
- Single node: GC pause can outlast TTL.
- Redlock: clock drift, persistence issue on crash+restart.
- No fencing tokens built in — must implement in the storage layer.
- "For safety-critical locks, use etcd or ZooKeeper."

**"How does ZooKeeper solve the GC pause problem?"**
- It doesn't fully — a GC pause longer than session timeout still causes lock loss.
- Fencing tokens solve it: storage layer rejects stale writes regardless of lock state.

### Common Follow-ups
- "What happens if the lock holder never releases the lock?" → TTL/lease expiry + ephemeral nodes in ZK.
- "How do you prevent a lock from being released by the wrong process?" → Unique value check before DEL (Lua script in Redis; handled automatically in ZK via sessions).
- "What's the difference between a lock and a lease?" → Lease has time-bounded validity with renewal; lock is indefinite until released. In practice, distributed locks are always leases (must have TTL).
- "What if two lock service nodes disagree on who holds the lock?" → This is the split-brain problem. ZK/etcd prevent it via Raft/ZAB consensus. Redlock handles it via majority quorum.

### Mistakes to Avoid
- Using `SETNX` + `EXPIRE` separately (race condition) instead of atomic `SET NX EX`.
- Releasing a lock without checking ownership (use Lua script).
- Claiming Redlock is safe for all use cases — it's not safe for correctness-critical scenarios.
- Forgetting fencing tokens — the most commonly missed concept in lock interviews.
- Recommending locks when idempotent writes would suffice.
