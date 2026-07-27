# Rate Limiting

## 1. What It Is

Rate limiting controls how many requests a client can make in a given time window, protecting downstream services from overload and ensuring fair usage. It exists because without it, a single misbehaving client (or a traffic spike) can starve all other clients and collapse services that have finite compute and I/O capacity.

---

## 2. The Problem It Solves

Without rate limiting:
- A single client sends 50,000 requests/second to your API. Your service can handle 10,000 req/s total. All other clients get timeouts.
- A DDoS or runaway retry loop overwhelms your database connection pool.
- A viral event causes 100x normal traffic — no graceful degradation, just outage.

With rate limiting, you enforce a contract: "Client X may make at most N requests per second/minute." Excess requests get 429 Too Many Requests, and your service remains available to well-behaved clients.

---

## 3. How It Works — Five Algorithms

### Algorithm 1: Fixed Window Counter

Divide time into fixed windows (e.g., 1-minute buckets). Count requests per window. Reset at window boundary.

```
Window: [00:00 - 01:00)  Counter: 95/100
Window: [01:00 - 02:00)  Counter: 0/100

Timeline:
|----window 1----|----window 2----|
0s              60s             120s

BURST ATTACK at window boundary:
95 requests at 00:59 → allowed (counter = 95)
100 requests at 01:00 → allowed (new window, counter = 100)
Result: 195 requests in 2 seconds, 2x the intended limit!
```

**The boundary burst flaw:** An attacker can send N requests just before window end and N requests just after window start, getting 2N requests in a very short window.

- **Data structure:** `INCR key; EXPIRE key 60`  (one Redis key per client per window)
- **Memory:** O(1) per client
- **Complexity:** O(1) per request

### Algorithm 2: Sliding Window Log

Store a timestamp log of every request. On each new request, remove entries older than the window, count remaining entries.

```
Window size: 60s, Limit: 5 req/window
Current time: T=100s

Log for client X: [45s, 60s, 70s, 85s, 92s, 99s]
                   ↑remove (< 40s)   ↑keep (≥ 40s)

After cleanup: [60s, 70s, 85s, 92s, 99s] → count=5 → DENY next request
At T=106s: remove entry at 60s → count=4 → ALLOW
```

- **Data structure:** Sorted set (Redis ZSET) — `ZADD key timestamp requestId; ZREMRANGEBYSCORE key 0 (now-window); ZCARD key`
- **Memory:** O(requests_in_window) per client — can be large for high-traffic clients
- **Complexity:** O(log N) per request (sorted set operations)
- **Pro:** No boundary burst problem; exact counting
- **Con:** Memory intensive — storing every request timestamp

### Algorithm 3: Sliding Window Counter (Approximate)

Hybrid: use two fixed windows (current + previous) and interpolate.

```
Window size: 60s, Limit: 100 req/window
Current time: 75s (25s into current window, 35s into previous window)

Previous window [0-60s]:   count = 80
Current window [60-120s]:  count = 30

Overlap ratio = (60 - 25) / 60 = 35/60 ≈ 0.583

Estimated count = 0.583 × 80 + 30 = 46.7 + 30 = 76.7
76.7 < 100 → ALLOW
```

- **Data structure:** Two counters per client (two Redis keys or a hash)
- **Memory:** O(1) per client
- **Accuracy:** ~0.003% error rate under uniform traffic; worse under bursty traffic
- **Used by:** Cloudflare's rate limiter

```
ASCII: Sliding Window Estimate

     prev window         curr window
|────────────────────|──────────────────|
0                   60                 120
                         ↑ "now" = 75s

Weight of prev = (60 - 15) / 60 = 0.75   [15s elapsed in curr window]
Estimated = 0.75 × prev_count + curr_count
```

### Algorithm 4: Token Bucket

A bucket holds tokens. Tokens are added at a fixed rate (refill rate R). Each request consumes 1 token. If bucket is empty, request is denied (or queued). Bucket has a maximum capacity B (burst size).

```
ASCII: Token Bucket

Refill rate R = 10 tokens/sec
Bucket capacity B = 50 tokens

        ┌─────────────┐
  R →   │ ● ● ● ● ●  │  ← tokens accumulate up to B
tokens/ │ ● ● ● ● ●  │
  sec   │ ● ● ● ● ●  │
        └──────┬──────┘
               │ 1 token per request
               ▼
          Request arrives
          token > 0? → ALLOW, decrement
          token = 0? → DENY (429)
```

**Burst handling:** A client that is idle for T seconds accumulates min(R×T, B) tokens, allowing a burst of B requests before being rate-limited. This is intentional — it allows bursty workloads.

- **Data structure:** Per-client `{tokens: float, last_refill_time: timestamp}`
- **Implementation:**
  ```
  function allowRequest(client):
    now = currentTimeMillis()
    elapsed = now - client.last_refill_time
    client.tokens = min(B, client.tokens + R * elapsed / 1000)
    client.last_refill_time = now
    if client.tokens >= 1:
        client.tokens -= 1
        return ALLOW
    return DENY
  ```
- **Memory:** O(1) per client
- **Complexity:** O(1) per request
- **Pros:** Allows bursts up to B; smooth average rate; intuitive
- **Cons:** Two parameters (R and B) to tune; not perfectly smooth output

### Algorithm 5: Leaky Bucket

Requests enter a queue (the "bucket"). A processor drains the queue at a fixed rate R. If queue is full, incoming requests are dropped.

```
ASCII: Leaky Bucket

Requests → [●][●][●][●][  ][  ]  ← queue (capacity Q)
                                        ↓ drains at rate R
                                   → processed at steady R req/s

If queue full: DROP new requests (or return 429)
```

- **Output rate:** Always R (constant) — unlike token bucket which allows bursts
- **Used for:** Traffic shaping (e.g., ensuring downstream never sees more than R req/s regardless of input spikes)
- **Data structure:** Queue + rate-controlled processor
- **Difference from Token Bucket:** Token bucket allows bursts (good for API clients); leaky bucket enforces constant output rate (good for protecting downstream services)

---

## 4. Algorithm / Implementation

### Distributed Rate Limiting with Redis

**Option 1: INCR + EXPIRE (Fixed Window)**
```
# Atomic enough for fixed window, race condition on first INCR
count = INCR "rl:{clientId}:{windowStart}"
if count == 1:
    EXPIRE "rl:{clientId}:{windowStart}" windowSizeSeconds
if count > limit:
    return 429
```
**Race condition:** Between INCR and EXPIRE, process can die → key never expires → client permanently blocked. Fix: use `SET key 0 EX windowSize NX` first.

**Option 2: Lua Script (Atomic Sliding Window Counter)**
```lua
-- Called atomically via EVAL
local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])

-- Remove old entries
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
local count = redis.call('ZCARD', key)

if count < limit then
    redis.call('ZADD', key, now, now)  -- score=timestamp, member=timestamp
    redis.call('EXPIRE', key, window)
    return 1  -- allowed
else
    return 0  -- denied
end
```
Lua scripts in Redis are executed atomically — no race conditions. Drawback: log storage per client.

**Option 3: Redis Cell (CRDT-based Token Bucket)**

Redis Cell is a Redis module implementing the GCRA (Generic Cell Rate Algorithm), a variant of leaky bucket:
```
CL.THROTTLE user123 99 30 60 1
             │       │  │  │  └─ number of tokens requested (default 1)
             │       │  │  └─ rate: 30 requests per 60 seconds
             │       └─ burst capacity (max tokens)
             └─ key

Returns:
1) (integer) 0     → allowed (1 = denied)
2) (integer) 100   → total limit
3) (integer) 99    → remaining tokens
4) (integer) -1    → seconds until retry (-1 = not needed)
5) (integer) 2     → seconds until bucket is full
```
Redis Cell does all math server-side atomically. Single round-trip. Most production-grade approach.

---

## 5. Trade-offs

| Algorithm | Memory | Accuracy | Burst | Smoothness | Best For |
|-----------|--------|----------|-------|------------|----------|
| Fixed Window | O(1) | Poor at boundary | 2x burst | No | Simple internal tools |
| Sliding Window Log | O(requests) | Exact | None | No | Low-traffic, exact counts |
| Sliding Window Counter | O(1) | ~99.97% | Slight | Approximate | High-scale public APIs |
| Token Bucket | O(1) | Good | Yes (up to B) | No | Client-friendly APIs |
| Leaky Bucket | O(queue) | Good | No | Yes | Protecting downstream services |

### Failure Modes
- **Redis failure:** Rate limiter becomes unavailable. Strategies: fail open (allow all requests) vs fail closed (deny all). Most systems fail open to avoid outages.
- **Clock skew in distributed systems:** Two rate limiter nodes with different clocks give inconsistent window boundaries. Use NTP + Redis server time (`TIME` command) as authoritative clock.
- **Key expiration race:** As shown above — fix with Lua scripts or atomic operations.
- **Thundering herd on reset:** All clients get reset simultaneously at fixed window boundary → coordinated burst. Sliding window or token bucket avoids this.

---

## 6. Where to Place Rate Limiters

```
Client → [Client-side] → [API Gateway] → [Service-level] → Downstream

1. Client-side:   SDK throttles itself before sending. Useful for SDK authors.
                  Problem: can't enforce limits (client controls itself).

2. API Gateway:   Best for coarse-grained limits (per user, per API key).
                  AWS API Gateway, Kong, Nginx all support this natively.
                  Problem: gateway becomes a bottleneck; harder to do
                  service-specific limits.

3. Service-level: Fine-grained limits per endpoint, per feature.
                  Problem: every service reimplements rate limiting.
                  Solution: shared rate-limit library or sidecar.

Recommended: API Gateway for global limits + service-level for critical endpoints.
```

---

## 7. Where It Appears in Real Systems

| System | Approach |
|--------|----------|
| **Stripe** | Token bucket; idempotency keys to safely retry |
| **GitHub API** | Fixed window (5000 req/hr for authenticated, 60 for anonymous) |
| **Twitter API** | Sliding window per 15-minute windows |
| **Cloudflare** | Sliding window counter (approximate) at massive scale |
| **AWS API Gateway** | Token bucket (burst + steady-state rate configurable) |
| **Google Cloud** | Token bucket with quotas per project |
| **Nginx** | `limit_req_zone` uses leaky bucket (smooth rate) |
| **Redis Cell module** | GCRA algorithm for Redis-native rate limiting |
| **Kong Gateway** | Fixed window, sliding window, token bucket (pluggable) |

---

## 8. Numbers to Know

| Metric | Value |
|--------|-------|
| Typical API rate limit (public) | 100–1000 req/min |
| GitHub API limit (authenticated) | 5000 req/hr |
| Redis INCR latency | ~0.1ms (local), ~1ms (network) |
| Redis Lua script overhead | Same as INCR — atomic, single round-trip |
| Sliding window log memory per client at 1000 req/min | ~8 bytes × 1000 = ~8 KB |
| Token bucket: AWS API Gateway burst | 5000 req/s (configurable) |
| Cloudflare rate limit accuracy | ~0.003% error (sliding window counter) |
| 429 retry-after header | Standard — tells client when to retry |

---

## 9. Interview Tips

### What You'll Be Asked

**"Design a rate limiter for an API gateway serving 10M users."**
- Choose algorithm (token bucket or sliding window counter for distributed), explain why.
- Discuss where to store state (Redis for distributed), data structure, atomicity.
- Cover failure modes: Redis down → fail open.
- Mention headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`.

**"What's the difference between token bucket and leaky bucket?"**
- Token bucket: bursty input, bursty output (up to B). Good for user-facing APIs.
- Leaky bucket: bursty input, smooth output. Good for protecting downstream.

**"How do you rate limit in a multi-region deployment?"**
- Options: (a) per-region limits (easier, may allow 2× global limit), (b) global Redis cluster (adds cross-region latency ~100ms), (c) approximate with local counter + periodic sync.
- Real answer: most systems use per-region limits and accept the imprecision.

### Common Follow-ups
- "What HTTP status code and headers?" → 429, `Retry-After`, `X-RateLimit-*`
- "How do you rate limit by IP vs by user vs by API key?" → Different key namespaces in Redis
- "How does your rate limiter handle Redis being down?" → Fail open or fail closed — justify your choice
- "What about soft vs hard limits?" → Soft: allow brief overage, alert. Hard: immediately reject at limit.

### Mistakes to Avoid
- Choosing fixed window without mentioning the boundary burst problem.
- Not discussing atomicity — naive INCR+EXPIRE has a race condition.
- Saying "just use Redis" without discussing what happens when Redis is unavailable.
- Forgetting to mention the response headers — they're part of the design.
- Treating rate limiting as pure deny/allow — real systems throttle (slow down) or queue.
