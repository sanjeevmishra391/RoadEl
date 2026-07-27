# System Design: Rate Limiter

> **Interview Format:** 45-minute Senior/Staff Engineer system design  
> **Difficulty:** Hard  
> **Core Themes:** Distributed systems, Redis, algorithm design, middleware architecture

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

Design a rate limiter that can be deployed at scale to protect your API infrastructure from abuse, DoS attacks, and runaway clients. The rate limiter should throttle requests that exceed defined limits and return appropriate responses with headers that tell clients how to back off.

This is not a simple problem. A rate limiter that works correctly on a single server is easy. A rate limiter that works correctly across 100 API gateway servers, handling 1 million users at 1,000 RPS each limit check, with no race conditions, no false positives (incorrectly blocking valid traffic), and no false negatives (letting through too much traffic) — that is the hard problem.

At FAANG scale, a rate limiter is critical infrastructure. A bug in it either takes down your entire API (too restrictive) or exposes your backends to DoS attacks (too permissive). It must be correct, fast, and highly available.

The interesting tensions in this design are:
- **Accuracy vs. performance:** The more accurate the limit, the more synchronization overhead you need
- **Local vs. distributed state:** Local state is fast but can let through N × (number_of_servers) times the allowed traffic
- **Hard reject vs. graceful degradation:** Should you drop requests or queue them?
- **Where to place it:** Client-side, gateway, or per-service?

---

## 2. Clarifying Questions

**Q1: What should the rate limit be based on — user ID, IP address, API key, or endpoint?**

> Expected answer: All of the above. We need per-user limits (authenticated users), per-IP limits (unauthenticated/anonymous), per-endpoint limits (some endpoints are more expensive), and per-API-key limits (for partner integrations).

Why this matters: This tells you the rate limiter needs to support multiple "limit dimensions" simultaneously. A request might be checked against three different limits at once. Your data model and Redis key schema must accommodate this.

**Q2: What should happen when a limit is exceeded — hard reject (HTTP 429) or queue the request?**

> Expected answer: Hard reject with HTTP 429. Queueing adds latency complexity and doesn't solve DoS; it just delays it. Inform the client with retry-after headers.

Why this matters: Queueing sounds appealing but is a trap. If you queue requests during a spike, you accumulate a backlog. When the spike ends, the queue drains and hits your backend with a burst — a delayed DoS. Hard reject + exponential backoff on the client is the correct pattern.

**Q3: Should the rate limiter be synchronous (in the request path) or asynchronous (post-processing)?**

> Expected answer: Synchronous. It must block the request before it reaches the service. An async rate limiter that checks after the fact is not a rate limiter — it's an audit log.

Why this matters: Synchronous means the rate limiter adds latency to every request. It must be extremely fast (< 5ms P99). This constrains your algorithm choices to those that can execute in a single Redis round trip.

**Q4: What are the rate limit policies — fixed window, sliding window, or token bucket?**

> Expected answer: The business wants flexible policies. Per-user: 1,000 requests/minute. Per-IP: 100 requests/minute. Per-endpoint (POST /upload): 10 requests/minute.

Why this matters: This tells you the rate limiter must support multiple algorithms or a single flexible algorithm. Sliding window is more accurate than fixed window. Token bucket is more appropriate for burst scenarios (API clients that want to use 1,000 credits all at once).

**Q5: How should the rate limiter handle distributed deployments — can it use a central store?**

> Expected answer: Yes. We have a Redis cluster available. The rate limiter should use it as the shared state store.

Why this matters: Confirms you should design a distributed rate limiter using Redis, not a local in-memory one. Knowing Redis is available lets you design around its atomic primitives (INCR, EVAL with Lua).

**Q6: Do we need to provide rate limit information back to the client (headers)?**

> Expected answer: Yes. Return `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, and `Retry-After` headers.

Why this matters: Headers add complexity — you need to compute remaining quota for every request, not just when limits are exceeded. This affects your Redis data model.

**Q7: Is there a bypass mechanism for internal services or admin users?**

> Expected answer: Yes. Internal services (identified by a trusted header or IP range) bypass rate limiting. Admin users have elevated limits.

Why this matters: The rate limiter must check an allowlist before doing any Redis operations. This is also a security concern — the bypass mechanism must not be exploitable.

---

## 3. Functional Requirements

1. **Request throttling:** Limit requests based on configurable rules (per-user, per-IP, per-endpoint, per-API-key).
2. **Multiple limit types:** Support fixed window, sliding window, and token bucket algorithms per rule.
3. **Configurable policies:** Rules are stored in a configuration service, not hardcoded. Changing a limit does not require a deployment.
4. **Standard headers:** Return `X-RateLimit-*` headers on every response (both allowed and throttled).
5. **429 Too Many Requests:** Return HTTP 429 with a `Retry-After` header when a limit is exceeded.
6. **Allowlisting:** Specific user IDs, IP ranges, or API keys can bypass rate limiting.
7. **Admin override:** Admins can set per-user custom limits (e.g., a partner gets 10,000 req/min).
8. **Audit log:** Log every throttled request with user, IP, endpoint, and rule that triggered for forensics.

Out of scope:
- DDoS mitigation at the network layer (handled by AWS Shield / Cloudflare)
- Request queuing / buffering
- Circuit breaker functionality (related but distinct)

---

## 4. Non-Functional Requirements

1. **Latency:** < 5ms P99 added latency per request. The rate limiter is in the critical path of every API call — it must be invisible.
2. **Availability:** 99.99% uptime. If the rate limiter is unavailable, it should fail open (allow requests through) with an alert, not fail closed (block everything). A broken rate limiter should never take down the entire API.
3. **Accuracy:** No more than 0.1% false positives (valid requests incorrectly blocked) under normal operation. No more than 1% false negatives (excess requests let through) in the distributed case.
4. **Consistency:** Eventual consistency is acceptable. A burst of 1,010 requests when the limit is 1,000 is tolerable; a burst of 10,000 is not.
5. **Scalability:** Must handle 10M users, 1,000 RPS limit check throughput per limit check operation (10B checks/day). Must scale horizontally without redesign.
6. **Correctness under concurrency:** No race conditions where two concurrent requests are both allowed past the final quota slot.

---

## 5. Capacity Estimation

### Traffic

```
Users:                      10M
Average request rate:       Let's assume 100 req/min average per active user
Active users at peak:       1M concurrent users (10% of total)
Peak rate limit checks:     1M users × 100 req/min / 60 sec ≈ 1,667,000/sec
                            ≈ 1.67M rate limit checks/second at peak
```

This is the throughput your rate limiter must sustain. This is extremely high. A single Redis instance can handle ~100,000 operations/second. You need a Redis cluster.

### Redis Memory

Each rate limiter entry stores a counter or token state per user per minute:

```
Per-user entry size:        ~50 bytes (key + counter + TTL metadata)
Users with active sessions: 1M concurrent
Entries per user:           ~5 (user limit, IP limit, 3 endpoints they're hitting)
Total entries:              1M × 5 = 5M entries
Memory:                     5M × 50 bytes = 250 MB
```

Rate limiter state is tiny. Even with 100M entries it's only 5 GB. This fits in a single Redis node, but we cluster for availability, not memory.

### Redis Cluster Sizing

```
Peak operations/second:     1.67M
Redis ops per check:        2 (INCR + EXPIRE, or 1 Lua script)
Total Redis ops/sec:        3.34M
Redis instances needed:     3.34M / 100,000 ops/instance = ~34 instances
Redis cluster:              12 primaries × 3 replicas = 36 nodes (covers with headroom)
```

In practice, Lua scripts are more efficient than individual commands. A Redis Cluster with 6-8 shard primaries (each handling ~500,000 ops/sec via pipelining) is realistic for this load.

### API Gateway Capacity

```
Rate limit checks:          1.67M/sec
API gateway instances:      Assume each handles 10,000 req/sec
Instances needed:           1.67M / 10,000 = 167 instances
```

Each gateway instance runs the rate limiter middleware. Redis connections are pooled (connection pool per instance). With 167 instances each maintaining 10 connections to Redis: 1,670 total connections. Manageable.

---

## 6. High-Level Design

### Architecture Overview

```
                    ┌──────────────────────────────────────────────────────────┐
                    │                      INTERNET                            │
                    └────────────────────────────┬─────────────────────────────┘
                                                 │
                                                 ▼
                    ┌──────────────────────────────────────────────────────────┐
                    │              Load Balancer (AWS ALB / Nginx)             │
                    │         SSL termination, health checks, routing          │
                    └──────────────────────────┬───────────────────────────────┘
                                               │
                                               ▼
              ┌────────────────────────────────────────────────────────────────┐
              │                    API Gateway Cluster                         │
              │                (Kong / Envoy / Custom)                         │
              │                                                                │
              │  ┌──────────────────────────────────────────────────────────┐  │
              │  │               Rate Limiter Middleware                    │  │
              │  │                                                          │  │
              │  │   1. Extract identity (user_id / IP / api_key)          │  │
              │  │   2. Check allowlist (local LRU cache)                  │  │
              │  │   3. Load rate limit rules (local LRU cache, 60s TTL)   │  │
              │  │   4. Execute Lua script against Redis cluster            │  │
              │  │   5. If allowed: pass request + set response headers     │  │
              │  │   6. If blocked: return 429 + Retry-After header        │  │
              │  └──────────────────────────────────────────────────────────┘  │
              └────────┬───────────────────────────────────────────────────────┘
                       │                                │
         Redis Cluster │                   Config DB    │
                       ▼                                ▼
      ┌─────────────────────────────┐   ┌───────────────────────────────┐
      │      Redis Cluster          │   │     Rate Limit Config Store   │
      │  (Sliding Window Counters)  │   │     (DynamoDB / PostgreSQL)   │
      │                             │   │                               │
      │  Key: rl:{user}:{window}    │   │  Rules: user_id, endpoint,    │
      │  Value: count               │   │  limit, window_sec, algorithm │
      │  TTL: window duration       │   │  Cached locally for 60s       │
      │                             │   │                               │
      │  6 primaries + 6 replicas   │   │  Custom overrides per user    │
      └─────────────────────────────┘   └───────────────────────────────┘
                       │
                       ▼
      ┌────────────────────────────────────┐
      │         Audit Log (Kafka)          │
      │                                    │
      │  Every throttled request logged:   │
      │  {user, ip, endpoint, rule,        │
      │   timestamp, request_id}           │
      │                                    │
      │  Consumer: writes to Elasticsearch │
      │  for forensics / dashboards        │
      └────────────────────────────────────┘
                       │
                       ▼
      ┌────────────────────────────────────┐
      │        Upstream Services           │
      │   (Only reached if rate limit OK)  │
      └────────────────────────────────────┘
```

### Component Responsibilities

**Rate Limiter Middleware (runs inside API Gateway)**

This is the hot path. It must complete in < 5ms. Every millisecond matters:

1. **Identity extraction:** Parse `Authorization` header → `user_id`. Parse `X-Forwarded-For` → `client_ip`. Parse `X-API-Key` header → `api_key`. This is done with regex/string parsing — no I/O.

2. **Allowlist check:** Check a local LRU cache (in-memory, no network hop) for known-trusted identities. Internal service IPs, admin accounts. This eliminates Redis round trips for your own services.

3. **Rule loading:** Load rate limit rules from a local LRU cache (TTL: 60 seconds). On cache miss, fetch from Config Store. Rules rarely change — 60s TTL means rule updates propagate within a minute.

4. **Redis Lua script execution:** Single network round trip to Redis. The Lua script atomically increments the counter and checks the limit. Returns remaining count and whether the request is allowed. This is the heart of the system.

5. **Response headers:** Set `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset` on every response.

6. **Throttle path:** On throttle, immediately return 429 without forwarding the request upstream. Publish throttle event to Kafka asynchronously.

**Redis Cluster**

Stores all rate limit state. Organized as a Redis Cluster with hash slot distribution across 6 primary shards. Connection pool of 10 connections per gateway instance. Lua scripts run atomically — no WATCH/MULTI/EXEC transactions needed.

**Rate Limit Config Store**

Stores rate limit policies. A simple key-value or row-per-rule table. Example schema:

```
Table: rate_limit_rules
  rule_id:         UUID
  entity_type:     "user" | "ip" | "api_key" | "global"
  entity_id:       user_id / IP / api_key value, or "*" for global
  endpoint:        "/api/upload" or "*" for all endpoints
  limit:           1000
  window_seconds:  60
  algorithm:       "sliding_window" | "token_bucket"
  priority:        1 (higher priority rules override lower priority)
  is_active:       true
```

Rules are cached in a local LRU cache per gateway instance with a 60-second TTL. When a rule changes, it propagates to all instances within 60 seconds — acceptable for most use cases. For emergency throttles (blocking an attacker), you can send a cache invalidation message via Kafka to force immediate propagation.

---

## 7. Deep Dives

### 7.1 Token Bucket Algorithm Deep Dive

**Conceptual Model**

A token bucket is the most natural rate limiting algorithm for APIs that allow controlled bursting. Imagine a bucket that holds N tokens. Each second, R tokens are added to the bucket (up to the maximum N). Each request consumes one token. If no tokens are available, the request is rejected.

This models how real APIs should behave: a user who hasn't made any requests for 30 seconds has accumulated 30 seconds × R tokens/second = a burst allowance. A continuous stream of requests at exactly R/second is always allowed. A burst above R/second is allowed up to the bucket capacity, then throttled.

**Parameters**

```
Capacity (N):   Maximum tokens the bucket can hold (burst limit)
Refill rate (R): Tokens added per second (sustained request rate)
```

Example: `N=100, R=10/sec`
- A user can burst 100 requests instantly
- Sustained rate is 10 req/second
- After a burst of 100, the user must wait 10 seconds before making another 100-request burst
- A user who hasn't made any requests in 10 seconds has a full bucket and can burst 100 again

**Redis Implementation**

Store two values per user in Redis:
```
tokens:       current token count (float)
last_refill:  timestamp of last refill (Unix time, float for sub-second precision)
```

The naive approach uses GET-compute-SET which has a race condition. Two concurrent requests can both read `tokens=1`, both decide they can proceed, and both decrement — resulting in `tokens=-1` (over-allowed). Fix this with a Lua script (atomic execution on Redis).

**Lua Script for Token Bucket:**

```lua
-- KEYS[1] = rate limit key (e.g., "rl:tb:user:12345")
-- ARGV[1] = max_tokens (capacity N)
-- ARGV[2] = refill_rate (tokens per second, R)
-- ARGV[3] = current_time (Unix timestamp as float, e.g., "1706000000.123")
-- ARGV[4] = tokens_requested (usually 1)
-- Returns: {allowed (0/1), remaining_tokens, next_refill_time}

local key = KEYS[1]
local max_tokens = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- Load current state from Redis
local data = redis.call("HMGET", key, "tokens", "last_refill")
local current_tokens = tonumber(data[1]) or max_tokens  -- default: full bucket for new users
local last_refill = tonumber(data[2]) or now

-- Compute how many tokens to add based on elapsed time
local elapsed = now - last_refill
local new_tokens = math.min(max_tokens, current_tokens + elapsed * refill_rate)

-- Check if request can proceed
local allowed = 0
if new_tokens >= requested then
    new_tokens = new_tokens - requested
    allowed = 1
end

-- Save updated state with TTL (2 × window to auto-cleanup inactive users)
redis.call("HMSET", key, "tokens", new_tokens, "last_refill", now)
redis.call("EXPIRE", key, math.ceil(max_tokens / refill_rate) * 2)

-- Return: allowed flag, remaining tokens (floor), time until next token
local time_to_next_token = 0
if allowed == 0 then
    time_to_next_token = (requested - new_tokens) / refill_rate
end

return {allowed, math.floor(new_tokens), time_to_next_token}
```

**This Lua script is atomic.** Redis executes it without interruption. No race conditions possible.

**Response Headers from Token Bucket:**

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 47
X-RateLimit-Reset: 1706000010   (Unix time when bucket fills back to max)
Retry-After: 3                   (seconds to wait if throttled; = time_to_next_token)
```

**Token Bucket Advantages:**
- Natural burst handling — clients that accumulate tokens can make quick bursts
- Smooth refill — no hard window boundaries where a client gets a fresh 1,000 tokens every 60 seconds
- Intuitive to configure: "1,000 requests/minute with a burst of 200" maps directly to N=200, R=16.67/sec

**Token Bucket Disadvantages:**
- Slightly more complex to implement than a counter (two values per user instead of one)
- Requires float arithmetic — small floating-point precision errors accumulate over hours (use integer arithmetic with millisecond precision to avoid this)

### 7.2 Redis Sliding Window Counter with Lua Scripts

**Why Not Fixed Window?**

The fixed window counter is simple: increment a counter keyed to the current window (e.g., `rl:user:12345:2024-01-24-10:30`) and compare against the limit. Reset happens at the window boundary.

The fatal flaw: **boundary bursting.** If the limit is 100 req/minute, a user can make 100 requests at 10:30:59 and 100 requests at 10:31:00 — 200 requests in 2 seconds, which exceeds the intent of "100 per minute."

```
Fixed Window Boundary Attack:
  10:30:59  ████████████████████ 100 requests allowed (last second of window)
  10:31:00  ████████████████████ 100 requests allowed (first second of new window)
  Result:   200 requests in 2 seconds. Intent violated.
```

**Sliding Window Log (Exact but Memory-Heavy)**

Store the timestamp of each request in a sorted set. To check the limit, count entries in the last N seconds:

```lua
-- Remove entries older than window
redis.call("ZREMRANGEBYSCORE", key, 0, now - window_ms)
-- Count remaining entries (requests in the window)
local count = redis.call("ZCARD", key)
-- Add new request if allowed
if count < limit then
    redis.call("ZADD", key, now, request_id)
    return {1, limit - count - 1}
end
return {0, 0}
```

Problem: Stores one sorted set entry per request. For a user making 1,000 requests/minute, the sorted set has 1,000 entries. At 1M active users: 1M × 1,000 × 16 bytes = **16 GB just for the sorted set keys**. Too expensive for high-volume users.

**Sliding Window Counter (Approximate, Efficient — My Recommendation)**

This approximates the sliding window using two fixed-window counters:

```
Current window counter:   count of requests in the current fixed window
Previous window counter:  count of requests in the previous fixed window

Approximate sliding window count:
  = (previous_count × overlap_fraction) + current_count

Where overlap_fraction = (window_size - time_elapsed_in_current_window) / window_size
```

**Example:**

```
Window: 60 seconds. Limit: 1,000 requests.
Previous window (10:30:00 - 10:30:59): 800 requests
Current window  (10:31:00 - 10:31:59): 300 requests
Time elapsed in current window: 40 seconds (we're at 10:31:40)

Overlap fraction = (60 - 40) / 60 = 0.333 (the previous window overlaps 33% with our sliding window)

Estimated count = (800 × 0.333) + 300 = 266.4 + 300 = 566.4

566 < 1,000, so allow the request.
```

**Lua Script for Sliding Window Counter:**

```lua
-- KEYS[1] = current window key   (e.g., "rl:sw:user:12345:1706000060")
-- KEYS[2] = previous window key  (e.g., "rl:sw:user:12345:1706000000")
-- ARGV[1] = limit (max requests)
-- ARGV[2] = window_size_seconds
-- ARGV[3] = current_time_ms (milliseconds since epoch)

local curr_key = KEYS[1]
local prev_key = KEYS[2]
local limit = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2]) * 1000
local now_ms = tonumber(ARGV[3])

-- Calculate position within the current window (0.0 to 1.0)
local window_start_ms = math.floor(now_ms / window_ms) * window_ms
local elapsed_in_window_ms = now_ms - window_start_ms
local overlap_fraction = (window_ms - elapsed_in_window_ms) / window_ms

-- Read both counters atomically
local curr_count = tonumber(redis.call("GET", curr_key)) or 0
local prev_count = tonumber(redis.call("GET", prev_key)) or 0

-- Estimate sliding window count
local estimated_count = math.floor(prev_count * overlap_fraction) + curr_count

-- Decide
if estimated_count >= limit then
    -- Calculate remaining time in current window + reset time
    local reset_time_ms = window_start_ms + window_ms
    return {0, 0, reset_time_ms}
end

-- Allow request: increment current window counter
local new_count = redis.call("INCR", curr_key)
-- Set TTL on first write (2 windows to cover the overlap period)
if new_count == 1 then
    redis.call("PEXPIRE", curr_key, window_ms * 2)
end

local remaining = limit - (math.floor(prev_count * overlap_fraction) + new_count)
local reset_time_ms = window_start_ms + window_ms
return {1, math.max(0, remaining), reset_time_ms}
```

**Why Lua Scripts are Critical Here**

Without Lua, implementing the sliding window requires multiple Redis commands: GET prev, GET curr, check, INCR curr, EXPIRE curr. Between the GET and INCR, another request from the same user can execute on another gateway instance. Both read `curr=999`, both decide they're under the limit, both increment to 1000. Two requests went through for the price of one slot.

Lua scripts execute atomically on a single Redis node. No other command executes between the first line and last line of the script. This eliminates all race conditions inherent in multi-command rate limit checks.

**Accuracy of the Approximation**

The sliding window counter approximation can over-estimate by at most:
```
error ≤ limit × overlap_fraction_of_overlap
```

In the worst case (previous window was exactly at the limit), the approximation allows up to `limit × 1.1` requests (10% over-limit). This is acceptable. The alternative (exact sliding window log) costs 100x more memory. This trade-off is well worth it.

### 7.3 Distributed Race Conditions

**The Core Problem**

You have 100 API gateway servers. Each handles ~1,670 req/sec. A single user sends 100 requests simultaneously from 100 connections — each landing on a different gateway server. All 100 gateway servers simultaneously execute their rate limit check against Redis. Without proper synchronization, some may slip through.

**Race Condition Illustration:**

```
Timeline: User has 1 request remaining (count = 999, limit = 1000)

Gateway 1: reads count=999, decides allowed, increments → count=1000
Gateway 2: reads count=999, decides allowed, increments → count=1001 ← over limit!
Gateway 3: reads count=999, decides allowed, increments → count=1002 ← over limit!
...

Result: 3 requests went through. Only 1 should have.
```

**Solution: Lua Scripts + Redis Cluster Hashing**

Lua scripts eliminate the race condition — the read-check-write is atomic per Redis node. But there's a subtlety: in Redis Cluster, a key is routed to a specific shard based on the hash slot of the key. All operations for a given user's counter are on the same shard (same key), so the Lua script atomicity guarantee holds.

**The Multi-Key Problem**

If you check multiple limits simultaneously (user limit + IP limit + endpoint limit), each check involves a different Redis key, potentially on different shards. You cannot run a single Lua script across multiple shards.

**Solution: Run checks in parallel, all-or-none semantics:**

```python
def check_rate_limits(request) -> (bool, dict):
    # Identify all applicable rules
    rules = [
        ("user",     f"rl:sw:user:{request.user_id}",    1000, 60),
        ("ip",       f"rl:sw:ip:{request.client_ip}",     100, 60),
        ("endpoint", f"rl:sw:ep:{request.endpoint}",     5000, 60),
    ]
    
    # Fire all Redis Lua checks in parallel (pipeline)
    pipe = redis.pipeline(transaction=False)
    for (name, key, limit, window) in rules:
        pipe.evalsha(SLIDING_WINDOW_SHA, 2, 
                     key, prev_key(key, window), 
                     limit, window, current_time_ms())
    results = pipe.execute()
    
    # If ANY rule is violated, reject the request
    # But we've already incremented counters for the allowed rules!
    # We need to handle this carefully.
    for i, (name, key, limit, window) in enumerate(rules):
        allowed, remaining, reset = results[i]
        if not allowed:
            # Rollback the increments for rules that did allow
            # ... (decrement counters for allowed rules)
            return False, {"violated_rule": name, "reset": reset}
    
    return True, {name: {"remaining": rem, "reset": rst} 
                  for (name, _, _, _), (_, rem, rst) in zip(rules, results)}
```

**The Rollback Problem:** If user-limit check passes (counter incremented) but IP-limit check fails, you need to decrement the user-limit counter. This is a two-phase operation. Simplification: execute the most restrictive check first. If it fails, skip the others. This avoids the rollback problem but means you don't always check all limits:

```python
# Order rules from most restrictive to least restrictive
# If the tightest limit is already exceeded, no point checking others
rules = sorted(rules, key=lambda r: r.limit_per_second)
for rule in rules:
    allowed, remaining, reset = execute_lua(rule)
    if not allowed:
        return False, {"violated_rule": rule.name}
```

This is the pragmatic production approach. The rollback-free design is worth the slight over-counting risk.

**Sticky Sessions (Alternative):** Route all requests from a given user to the same gateway instance. Then use a local counter — no Redis needed. Problem: failover moves sessions to other instances, resetting counters. Not suitable for user-facing APIs.

### 7.4 Middleware Placement and Bypass Prevention

**Where Should the Rate Limiter Live?**

There are four possible placements:

```
Option 1: Client-side rate limiting
  [Client App] → check local counter → [API Gateway]

  Pros: Zero latency, no server resources
  Cons: Trivially bypassed. Clients are untrusted. Never the primary control.
  Verdict: OK as a courtesy mechanism only (don't spam your own endpoints).

Option 2: API Gateway (our design)
  [Client] → [API Gateway with Rate Limiter Middleware] → [Service]
  
  Pros: Single enforcement point. All traffic passes through.
       Easy to update rules without deploying services.
       Visibility across all endpoints.
  Cons: Gateway is now a critical path component with added latency.
       Gateway team owns rate limiting logic.
  Verdict: Correct for most production systems.

Option 3: Per-service middleware
  [Client] → [API Gateway] → [Service with local rate limiter]
  
  Pros: Services own their own rate limit logic.
       Different services can have different algorithms.
  Cons: Multiple implementation points. Easy to misconfigure one service.
       No global view of a user's total API consumption.
  Verdict: Good as a defense-in-depth layer, not as the primary control.

Option 4: Sidecar proxy (service mesh)
  [Client] → [Envoy/Istio sidecar → Service]
  
  Pros: Language-agnostic. Declarative configuration.
       Handles mutual TLS and rate limiting in the same infrastructure.
  Cons: Operationally complex. Adds a proxy hop.
  Verdict: Excellent for organizations already using a service mesh.
```

**My Recommendation: API Gateway as primary, per-service as defense-in-depth.**

The gateway enforces global user-level limits. Individual services enforce their own endpoint-level limits. If the gateway is bypassed (direct service-to-service calls, internal traffic), the service-level limits still protect the service.

**Bypass Prevention**

The most common rate limiter bypass is someone calling your service directly, bypassing the gateway. Mitigations:

1. **Network-level enforcement:** Services only accept traffic from the gateway's IP range (security group rule). Direct connections from the internet are blocked.

2. **Mutual TLS:** The gateway presents a client certificate to the service. Services only accept requests with a valid gateway cert. A request without a valid cert is rejected at the transport layer.

3. **Signed headers:** The gateway adds a `X-Rate-Limit-Passed: true` header with an HMAC signature. Services validate this header. If missing or invalid, reject with 403 — don't even process the request.

4. **Service mesh authorization policy:** In Istio/Linkerd, an `AuthorizationPolicy` can require that all requests come from the gateway service account. This is enforced by the sidecar proxy, not application code.

**Rate Limiter Bypass in Testing**

During integration tests, you want to call your service without rate limits. Use a special header `X-Internal-Request: true` with an HMAC signed value derived from an internal secret key. The gateway middleware checks for this header and skips rate limiting. This header is never forwarded from external clients (the gateway strips unknown `X-Internal-*` headers).

**Graceful Degradation: What Happens If Redis is Down?**

Two philosophies:

- **Fail open:** Allow all requests through. Attackers can DoS your services during a Redis outage.
- **Fail closed:** Block all requests. An attacker can trigger a Redis outage to block your API.

**Recommended: Fail open with a circuit breaker and aggressive alerting.**

Rationale: A Redis outage is either a real infrastructure failure (allow traffic, keep the business running, fix Redis) or a targeted attack on Redis to bypass rate limiting (unlikely for most systems, and sophisticated attackers have other vectors). The business impact of failing closed (complete API outage) almost always exceeds the risk of a temporary DoS during a Redis failure.

Implementation:
```python
def check_rate_limit(key, limit, window):
    try:
        return redis_client.evalsha(SCRIPT_SHA, ..., timeout=2ms)
    except (RedisConnectionError, RedisTimeoutError):
        circuit_breaker.record_failure()
        if circuit_breaker.is_open():
            metrics.increment("rate_limiter.circuit_open")
            alert_pagerduty("Rate limiter circuit breaker open - failing open")
        return (True, limit, None)  # fail open
```

---

## 8. Trade-offs and Alternatives

### Algorithm Comparison

| Algorithm | Burst Handling | Accuracy | Memory | Complexity |
|---|---|---|---|---|
| Fixed window counter | Poor (boundary attack) | Low | Very low (1 key/user) | Simple |
| Sliding window log | Perfect | Exact | High (1 entry/request) | Medium |
| Sliding window counter | Good | ~90% | Low (2 keys/user) | Medium |
| Token bucket | Excellent | Exact | Low (2 fields/user) | Medium |
| Leaky bucket | N/A (queues) | Exact | Medium | Complex |

**My recommendation:** Token bucket for per-user API limits (handles burst allowances naturally). Sliding window counter for IP/endpoint limits where bursting should not be allowed.

### Local vs. Distributed State

| | Local (in-memory) | Distributed (Redis) |
|---|---|---|
| Latency | Sub-microsecond | 1-3ms |
| Consistency | Per-instance only | Global across all instances |
| Accuracy at scale | Off by N × instances | Near-perfect |
| Failure handling | N/A | Circuit breaker needed |

With 100 gateway instances and a 1,000 req/min limit using local state: users can send 100,000 req/min (100 instances × 1,000 each). The accuracy is unacceptably poor. Redis shared state is non-negotiable for a production distributed rate limiter.

### Rate Limit Granularity Options

Fine-grained limiting (per user + per endpoint + per IP simultaneously):
- More accurate control
- 3 Redis ops per request instead of 1
- Higher latency, more Redis load

Coarse-grained limiting (per user only):
- Single Redis op per request
- 3x lower latency, 3x less Redis load
- Less flexible — hard to protect specific expensive endpoints

**Production recommendation:** Two tiers:
- Global user limit checked on every request (primary defense)
- Per-endpoint limit checked only for expensive endpoints (POST, large GET with pagination)

---

## 9. Failure Scenarios

### Scenario 1: Redis Primary Shard Failure

**What happens:** Redis Cluster detects the failure and promotes a replica to primary within ~10-30 seconds. During this window, all keys on the failed shard are unavailable.

**Impact:** Rate limiting for users whose keys hash to the failed shard fails open (circuit breaker activates, all requests pass through). This is ~1/6 of users if you have 6 shards.

**Mitigation:**
- Redis Sentinel or Cluster mode with automatic failover (built-in)
- Circuit breaker fails open during the failover window
- Alert immediately — the window is short but real
- Accept the 30-second window of no rate limiting for 1/6 of users as an acceptable trade-off

### Scenario 2: Redis Memory Exhaustion

**Symptom:** Redis OOM killer starts evicting keys. Rate limit counters are evicted, effectively resetting counters for some users.

**Impact:** Users who had consumed 999/1000 requests get their counter reset to 0. They can consume another 1000 requests immediately.

**Mitigation:**
- Allocate Redis memory generously. Rate limiter keys are tiny (50 bytes each). 1M active users needs only 50MB.
- Set `maxmemory-policy volatile-ttl` — Redis evicts keys with TTL set (rate limit keys) before keys without TTL
- Monitor memory usage and alert at 70% capacity
- All rate limit keys have TTL set — they auto-expire. Memory is bounded naturally.

### Scenario 3: Clock Skew Between Gateway Instances

**Symptom:** Two gateway instances have clocks that differ by 500ms. Sliding window boundaries are computed differently, causing some requests to land in the wrong window.

**Impact:** A user might be allowed slightly more or fewer requests than the configured limit.

**Mitigation:**
- Use NTP synchronization on all instances (standard cloud practice)
- Tolerate ≤ 100ms clock skew in the algorithm (sliding window calculation uses the server's timestamp, not the client's)
- Use Redis server time (`redis.call("TIME")`) inside Lua scripts for a single authoritative time source

### Scenario 4: Lua Script Not Loaded (Redis Restart)

**Symptom:** After a Redis restart, the Lua script SHA is no longer registered. `EVALSHA` returns a `NOSCRIPT` error.

**Impact:** Rate limiter crashes on every request (or falls back to unprotected path).

**Mitigation:**
- Catch `NOSCRIPT` errors explicitly
- On `NOSCRIPT`, call `SCRIPT LOAD` to reload the script, then retry `EVALSHA`
- Script loading happens once per Redis connection pool initialization and on reconnect

```python
def execute_rate_limit_script(redis, keys, args):
    try:
        return redis.evalsha(SCRIPT_SHA, len(keys), *keys, *args)
    except redis.exceptions.NoScriptError:
        SCRIPT_SHA = redis.script_load(SLIDING_WINDOW_LUA)
        return redis.evalsha(SCRIPT_SHA, len(keys), *keys, *args)
```

### Scenario 5: Rule Configuration Update During Traffic

**Concern:** An operator reduces a user's limit from 10,000 req/min to 100 req/min (blocking an abuser). The new rule needs to propagate to all 100 gateway instances.

**Current design:** Local rule cache has 60-second TTL. Propagation is eventually consistent within 60 seconds.

**For emergency blocks:** Publish an invalidation event to Kafka (`rule_invalidation` topic). Each gateway instance subscribes and immediately evicts the affected rule from its local cache. Next request forces a cache miss and loads the new rule from Config Store.

```
Operator action → Config Store update → Kafka invalidation event → 
All 167 gateway instances evict cache entry → Next request loads new rule
Total propagation time: < 5 seconds
```

---

## 10. Interview Tips

### Time Management for 45 Minutes

```
0-2 min:   Read problem, take notes
2-8 min:   Clarifying questions (critical — algorithm choice depends heavily on Q1-Q4)
8-12 min:  Requirements and capacity estimation (show the 1.67M ops/sec calculation)
12-18 min: High-level design diagram
18-38 min: Deep dives:
           - Token bucket Lua script (8 min — write pseudocode)
           - Sliding window counter Lua (6 min)
           - Race conditions (5 min)
           - Middleware placement (5 min)
38-42 min: Trade-offs
42-45 min: Failure scenarios (Redis down, clock skew)
```

### What Separates Staff from Senior Candidates

**Senior candidate:** Correctly implements a token bucket or sliding window, uses Redis, mentions Lua scripts for atomicity.

**Staff candidate** additionally:
- Quantifies the race condition problem precisely ("100 gateways × 1,000 limit = 100,000 effective limit without coordination")
- Shows the actual Lua script with proper comments
- Discusses fail-open vs fail-closed with a clear recommendation and rationale
- Addresses bypass prevention (mTLS, signed headers, network ACLs)
- Designs multi-dimensional limits (user + IP + endpoint) and addresses the rollback problem
- Mentions clock skew as a failure mode and explains why using Redis `TIME` in Lua eliminates it
- Proactively distinguishes token bucket (for burst-tolerant per-user limits) vs sliding window counter (for per-endpoint hard limits)

### Common Mistakes to Avoid

1. **Using a simple counter with GET/INCR/EXPIRE.** This has a race condition. Always use Lua scripts for atomicity. Interviewers will probe for this.

2. **Fixed window algorithm.** The boundary attack is a known, documented vulnerability. Interviewers will ask about it. Use sliding window or token bucket.

3. **Not addressing the "N servers" problem.** If you say "use in-memory counters," the interviewer will ask "what if you have 100 servers?" Have the Redis-based distributed answer ready.

4. **Forgetting response headers.** The headers `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset` are standard practice. Forgetting them shows you haven't operated a real API.

5. **Designing a rate limiter that can be trivially bypassed.** Always discuss how services validate that requests came through the rate limiter.

6. **Recommending "fail closed" without caveats.** A rate limiter that blocks all traffic when Redis is unavailable will take down your entire API. Fail open with alerting is the correct production answer.

### Strong Closing Statement

> "To summarize: I designed a distributed rate limiter using a sliding window counter algorithm implemented as an atomic Redis Lua script, which eliminates race conditions across 100+ distributed gateway instances. The rate limiter is placed at the API gateway layer and uses a local LRU cache for rule loading (60s TTL) and an allowlist check to minimize Redis round trips to the hot path. I chose to fail open when Redis is unavailable to prioritize availability over perfect rate limiting — with aggressive alerting to ensure fast recovery. The multi-dimensional limit check (user + IP + endpoint) is handled by ordered rule evaluation, executing the most restrictive check first to avoid the counter rollback problem."

---

*End of Rate Limiter System Design — estimated interview preparation time: 4-5 hours of active practice (write the Lua script from memory)*
