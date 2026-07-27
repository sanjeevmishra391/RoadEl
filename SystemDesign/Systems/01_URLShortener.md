# System Design: URL Shortener

> **Interview Format:** 45-minute Senior/Staff Engineer system design  
> **Difficulty:** Medium-Hard  
> **Core Themes:** Read-heavy systems, key-value storage, encoding, caching, analytics

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

Design a URL shortening service similar to bit.ly or TinyURL. The service accepts a long URL and returns a short, unique alias. When a user visits the short URL, they are redirected to the original long URL.

This sounds deceptively simple. The interesting engineering challenges emerge at scale: how do you generate collision-free short codes at 100 million URLs per day? How do you serve 10 billion redirects per day with single-digit millisecond latency? How do you track analytics without slowing down the critical redirect path? How do you handle hot URLs — a link that gets retweeted and suddenly receives 50 million hits in an hour?

At its core, this is a read-heavy distributed system with a write-heavy analytics side channel. Getting both right simultaneously is the real challenge.

---

## 2. Clarifying Questions

Before drawing a single box, ask these questions. Each one either eliminates an entire class of complexity or forces you to add one.

**Q1: What is the expected read-to-write ratio?**

> Expected answer: Heavily read-skewed. Roughly 100:1. We create 100M URLs/day, but serve 10B redirects/day.

Why this matters: This immediately tells you the system is read-heavy. Your caching strategy needs to be aggressive. The write path is not your bottleneck. You should design your data model and infrastructure with reads as the first-class concern.

**Q2: Do we need custom aliases? (e.g., bit.ly/my-company-sale)**

> Expected answer: Yes, users can optionally provide a custom alias. If not provided, the system generates one.

Why this matters: Custom aliases bypass your hash/encoding scheme entirely. They also introduce collision potential — what if someone requests an alias that already exists? You need a check-then-insert operation that must be atomic, which has distributed systems implications.

**Q3: Do short URLs expire? Is TTL configurable?**

> Expected answer: Yes. Default TTL is 1 year. Users can configure expiration or set URLs as permanent.

Why this matters: Expiration logic affects storage planning, cache invalidation, and database cleanup jobs. TTL on a key-value store like Redis/DynamoDB is free. On a relational DB it requires a background job. This is a great segue into your database choice discussion.

**Q4: Do we need analytics? If so, what granularity?**

> Expected answer: Yes, we need click counts at minimum. Ideally, per-country, per-device, per-referrer, and time-series data.

Why this matters: This is the single biggest scope-creeping question in this problem. Analytics on 10B events/day is a full data warehouse problem. You need to explicitly decouple analytics from the redirect path. The redirect must be fast; analytics writes can be async and slightly delayed. Don't let analytics become the bottleneck of your critical path.

**Q5: Should we support link previews / Open Graph metadata?**

> Expected answer: Nice to have, not required for core design.

Why this matters: Knowing when to defer scope is as important as knowing what to include. Saying "out of scope for now, but I'd add a metadata service that fetches OG tags asynchronously at creation time" shows good judgment.

**Q6: Is there a maximum URL length for the original URL?**

> Expected answer: Standard browser URL limit is ~2,048 characters. Assume we store up to 2KB per URL.

Why this matters: Affects storage estimation directly. Multiply 2KB × 100M URLs/day × 365 days = significant storage.

**Q7: What consistency model do we need for URL creation — strong or eventual?**

> Expected answer: The URL creation write should be strongly consistent (the user needs their short URL immediately). Analytics can be eventually consistent.

Why this matters: This justifies using two different storage tiers: a strongly consistent primary store for URL mappings, and an eventually consistent analytics pipeline. Separating these concerns keeps your architecture clean.

---

## 3. Functional Requirements

These are the features the system must support:

1. **URL Shortening:** Given a long URL, return a unique short URL (7-character default alias).
2. **URL Redirection:** Given a short URL, redirect the user to the original long URL.
3. **Custom Aliases:** Users can optionally supply their own short alias.
4. **Link Expiration:** URLs expire after a configurable TTL (default: 1 year). Expired URLs return 410 Gone.
5. **Analytics:** Track click count, geographic location, device type, referrer, and timestamp per redirect.
6. **Link Management API:** Users can delete their own short URLs (returns 410 Gone afterward).

Out of scope for this design (call these out explicitly in the interview):
- User authentication and dashboards (assume a separate auth service exists)
- A/B testing links
- QR code generation
- Link previews

---

## 4. Non-Functional Requirements

These constraints shape your architecture more than any functional requirement:

1. **Availability:** 99.99% uptime (52 minutes downtime/year). URL redirection is a hard dependency for many downstream services — downtime means broken links across the internet.

2. **Latency:** P99 redirect latency < 10ms. This is the critical path. Users clicking a link expect near-instant response. Every millisecond added increases bounce rate.

3. **Durability:** Once a URL is created, it must never be lost. A broken short link is catastrophic for the user who shared it.

4. **Scalability:** Must handle 115,000 redirects/second at peak (10B/day). Must handle traffic spikes of 10x normal load (a viral link).

5. **Consistency:** URL creation must be strongly consistent. Two users creating the same custom alias simultaneously must result in exactly one succeeding. Analytics can tolerate eventual consistency and some loss (< 0.1% of events).

6. **Security:** Short codes must not be guessable or enumerable. An attacker should not be able to walk short codes to discover URLs.

---

## 5. Capacity Estimation

**Always show your math in the interview. Interviewers want to see your reasoning, not just the final number.**

### Write Path (URL Creation)

```
URL creations per day:     100,000,000 (100M)
Seconds per day:           86,400
Write QPS (average):       100,000,000 / 86,400 ≈ 1,157 writes/sec
Write QPS (peak, 2x):      ~2,300 writes/sec
```

### Read Path (Redirects)

```
Redirects per day:         10,000,000,000 (10B)
Seconds per day:           86,400
Read QPS (average):        10,000,000,000 / 86,400 ≈ 115,740 reads/sec
Read QPS (peak, 2x):       ~231,000 reads/sec
```

**The read-to-write ratio is exactly 100:1.** This is a deeply read-skewed system.

### Storage

```
Average URL size:          ~2KB (long URL string + metadata JSON)
Short URL mapping size:    ~500 bytes (short code + long URL + TTL + user_id + created_at)
URLs created per day:      100M
URLs created per year:     100M × 365 = 36.5 billion URLs
Storage per year:          36.5B × 500 bytes = ~18.25 TB/year
Storage for 5 years:       ~91 TB
```

We can compress URLs (many share common prefixes like "https://www."). LZ4 compression on the value achieves ~50% reduction. Actual storage for 5 years ≈ **45 TB**.

A single DynamoDB table can handle this with ease. Even a single sharded Cassandra cluster handles this comfortably.

### Bandwidth

```
Inbound (writes):          1,157 writes/sec × 2KB = ~2.3 MB/s
Outbound (reads):          115,740 reads/sec × 500 bytes = ~58 MB/s
```

Bandwidth is not a constraint here. A single 1Gbps network interface handles this.

### Cache Sizing

Applying the 80/20 rule: 20% of URLs generate 80% of traffic.

```
Total URLs stored:         36.5B/year. Let's think about hot URLs.
Daily active URLs:         Only a fraction of 36.5B are hit daily.
Hot URLs to cache:         Top 20% of daily traffic = top URLs from recent 30 days
                           = ~20% × 100M × 30 = ~600M URLs
Cache size at 500B each:   600M × 500B = 300 GB
```

300 GB fits comfortably in a Redis cluster with 10 nodes at 64GB RAM each (640GB total, leaving headroom).

### Summary Table

| Metric | Value |
|---|---|
| Write QPS (avg) | ~1,200/sec |
| Write QPS (peak) | ~2,400/sec |
| Read QPS (avg) | ~115,000/sec |
| Read QPS (peak) | ~230,000/sec |
| Storage (5 years) | ~45 TB |
| Cache size | ~300 GB |
| Bandwidth (inbound) | ~2.3 MB/s |
| Bandwidth (outbound) | ~58 MB/s |

---

## 6. High-Level Design

### Architecture Overview

```
                         ┌─────────────────────────────────────────────────────────┐
                         │                    CLIENTS                              │
                         │          (Browsers, Mobile, API consumers)              │
                         └────────────────────────┬────────────────────────────────┘
                                                  │ HTTPS
                                                  ▼
                         ┌────────────────────────────────────────────────────────┐
                         │                  CDN (CloudFront)                      │
                         │   Caches 301 redirects for non-analytics use cases     │
                         └────────────────────────┬───────────────────────────────┘
                                                  │ Cache miss
                                                  ▼
                         ┌────────────────────────────────────────────────────────┐
                         │              API Gateway / Load Balancer               │
                         │          (AWS ALB, Nginx, or Kong)                     │
                         │   Rate limiting, SSL termination, routing              │
                         └────────┬──────────────────────────────┬────────────────┘
                                  │                              │
                    POST /shorten │                 GET /{code}  │
                                  ▼                              ▼
               ┌──────────────────────────┐    ┌───────────────────────────────┐
               │    URL Creation Service  │    │    URL Redirect Service       │
               │    (Write Path)          │    │    (Read Path)                │
               │                          │    │                               │
               │  1. Validate long URL    │    │  1. Check Redis cache         │
               │  2. Generate short code  │    │  2. On miss: query DynamoDB   │
               │  3. Check for collision  │    │  3. Return 302 redirect       │
               │  4. Write to DynamoDB    │    │  4. Publish to analytics      │
               │  5. Warm Redis cache     │    │     Kafka topic (async)       │
               └──────────┬───────────────┘    └───────────┬───────────────────┘
                          │                                │
                          │ Write                          │ Read
                          ▼                                ▼
               ┌──────────────────────────────────────────────────────────────┐
               │                  Redis Cluster (Cache)                       │
               │           short_code → long_url mapping                     │
               │           300 GB, TTL-aware, LRU eviction                   │
               └──────────────────────────────────────────────────────────────┘
                          │                                │
                          │ Write (primary store)          │ Read (on cache miss)
                          ▼                                ▼
               ┌──────────────────────────────────────────────────────────────┐
               │                    DynamoDB (Primary Store)                  │
               │   Partition key: short_code                                  │
               │   Attributes: long_url, user_id, created_at, expires_at,    │
               │               is_custom, click_count (approximate)           │
               │   TTL enabled on expires_at column                           │
               └──────────────────────────────────────────────────────────────┘
                          │
                          │ Async publish on every redirect
                          ▼
               ┌──────────────────────────────────────────────────────────────┐
               │                   Kafka (Analytics Events)                   │
               │   Topic: url.clicks                                          │
               │   Schema: {short_code, timestamp, ip, user_agent, referrer}  │
               └──────────────────────────────────────────────────────────────┘
                          │
                          ▼
               ┌──────────────────────────────────────────────────────────────┐
               │                 Analytics Consumer (Flink/Spark)             │
               │   Aggregates click counts, geo, device, time-series          │
               │   Writes to ClickHouse (OLAP) or DynamoDB analytics table   │
               └──────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

**API Gateway / Load Balancer**
- SSL/TLS termination
- Rate limiting (prevent abuse of the shortening API — 100 creates/minute per IP)
- Routes `POST /api/shorten` to the Creation Service
- Routes `GET /{code}` to the Redirect Service
- Routes `DELETE /{code}` to the Creation Service (authenticated)

**URL Creation Service**
- Validates the input URL (must be a valid URL, not a blocked/malicious domain)
- Generates a unique 7-character Base62 short code (or accepts a custom alias)
- Performs an atomic write to DynamoDB with a conditional expression to prevent overwriting existing codes
- Warms the Redis cache proactively on creation
- Returns the short URL to the client

**URL Redirect Service**
- This is the hottest code path. It must be lean and fast.
- Looks up the short code in Redis first (sub-millisecond)
- On cache miss, reads from DynamoDB and back-fills the cache
- Returns a 302 redirect response with the `Location` header set to the long URL
- Publishes a click event to Kafka asynchronously (fire-and-forget — do not wait for Kafka acknowledgment in the critical path)
- Checks TTL — if expired, returns 410 Gone

**Redis Cluster**
- Stores `short_code → {long_url, expires_at}` mappings
- TTL set on each key matching the URL expiration time
- LRU eviction when memory is full (evicts coldest URLs)
- Read replicas for read scaling (Redis Cluster with 6 nodes: 3 primaries, 3 replicas)

**DynamoDB (Primary Store)**
- Source of truth for all URL mappings
- Partition key: `short_code` (gives perfect key-value lookup O(1))
- TTL enabled: DynamoDB natively deletes items past their `expires_at` attribute
- Provisioned capacity: On-demand mode to handle traffic spikes
- Global Secondary Index (GSI) on `user_id` for "show me all my links" feature

**Kafka**
- Decouples analytics writes from the critical redirect path
- Durable event log — no click event is lost
- Consumers can be scaled independently of the redirect service
- Topic: `url.clicks`, partitioned by `short_code` (ensures ordering per URL)

**Analytics Consumer (Flink)**
- Reads from Kafka, performs windowed aggregations (clicks per minute, per hour, per day)
- Writes aggregated data to ClickHouse for fast analytical queries
- Updates a `click_count` approximation in DynamoDB for the simple "total clicks" view

---

## 7. Deep Dives

### 7.1 Base62 Encoding and Collision Handling

**The Core Problem**

We need to generate a unique 7-character identifier for each of the 100M URLs created per day. These identifiers must be:
- Short (7 characters is our target)
- URL-safe (no special characters)
- Not sequential (prevents enumeration attacks)
- Collision-resistant at scale

**Why 7 Characters?**

Base62 uses the alphabet: `a-z` (26) + `A-Z` (26) + `0-9` (10) = 62 characters.

```
1 character:  62^1  =          62 unique codes
2 characters: 62^2  =       3,844
3 characters: 62^3  =     238,328
4 characters: 62^4  =  14,776,336
5 characters: 62^5  = 916,132,832 (~916M)
6 characters: 62^6  = 56,800,235,648 (~56B)
7 characters: 62^7  = 3,521,614,606,208 (~3.5 trillion)
```

At 100M URLs/day for 5 years = 182.5B URLs. 7 characters gives us 3.5 trillion unique codes — a safety margin of 19x. We could use 6 characters (56B codes) for 5 years of data, but 7 gives comfortable headroom.

**Approach 1: Hash-based encoding (MD5/SHA + truncation)**

Generate the short code by hashing the long URL:

```
short_code = base62_encode(md5(long_url)[:4 bytes])
```

Problem: This is deterministic. The same long URL always produces the same short code — which might be desirable (idempotent creation) or not (you might want two short URLs for the same destination for A/B testing). More critically: two different long URLs can hash to the same 7-character prefix. **Hash collisions are a real concern.**

**Collision Handling Strategy:**

When a collision occurs (trying to write a short code that already maps to a different long URL), you have two options:

Option A: Append a counter and rehash:
```
attempt = 0
while true:
    candidate = base62_encode(md5(long_url + str(attempt))[:4 bytes])
    if not exists(candidate):
        write(candidate, long_url)
        return candidate
    attempt += 1
```

This works but each collision adds a read-before-write round trip. At scale, popular URLs will collide frequently in the first 4-5 attempts.

Option B: DynamoDB conditional write:
```
Put item with condition: attribute_not_exists(short_code)
```
If the write fails with `ConditionalCheckFailedException`, generate a new candidate and retry. This is atomic at the database level — no race condition where two services think they both own the same code.

**Approach 2: Counter-based with ID service (preferred at scale)**

Maintain a global atomic counter. Convert each counter value to Base62.

```
counter = redis.incr("global_url_counter")  # atomic increment
short_code = to_base62(counter)
```

Advantages:
- Zero collision by construction — each ID is unique
- Guaranteed 7-character codes for counter values up to 62^7

Disadvantages:
- Sequential IDs are guessable (counter 1000001 exists, so probably 1000002 does too)
- Single point of failure (the counter)

**Fix the guessability problem:** Apply a simple bijective scrambling function (Feistel network or a lookup-table shuffle) to make the output non-sequential while remaining reversible:

```python
def encode(n: int) -> str:
    shuffled = feistel_encrypt(n, key=SECRET_KEY)
    return to_base62(shuffled)

def decode(s: str) -> int:
    n = from_base62(s)
    return feistel_decrypt(n, key=SECRET_KEY)
```

**Fix the single point of failure:** Use a range-based ID allocation. Each creation service instance requests a range of 1,000 IDs from a central coordinator (like ZooKeeper or a DynamoDB atomic counter). It then generates IDs locally until the range is exhausted, then requests another range. This gives:
- No coordination required for 999 of every 1000 ID generations
- Linear horizontal scalability of the creation service

**Custom Aliases**

Custom aliases bypass the encoding scheme entirely. The user provides a string like `my-sale-2024`. Process:

1. Validate: length 3-50 chars, alphanumeric + hyphens only, not a reserved word (`api`, `admin`, `health`)
2. Attempt a conditional write to DynamoDB: `attribute_not_exists(short_code)`
3. If the write succeeds, the alias is theirs
4. If it fails, return HTTP 409 Conflict — alias already taken

**Never allow custom aliases to shadow system-generated codes.** Maintain a separate namespace: system codes go in the `s/` partition (internal routing), custom aliases in the top-level namespace. Or alternatively, mark all custom aliases with a `is_custom` flag and use a bloom filter to quickly check alias availability before hitting DynamoDB.

### 7.2 Database Choice: Why DynamoDB (Key-Value) Over PostgreSQL

This is one of the most important decisions in this design, and you should articulate it clearly.

**The access pattern is a perfect key-value lookup:**
- Write: `PUT short_code → long_url`
- Read: `GET short_code → long_url`

There are no complex joins, no multi-table transactions, no aggregations on the primary data. This is literally what key-value stores were built for.

**Why NOT PostgreSQL:**

PostgreSQL is excellent when you need:
- Complex queries (JOINs, GROUP BY, subqueries)
- ACID transactions across multiple rows
- Foreign key constraints
- Full-text search

For this system, PostgreSQL gives you all this power and you use none of it. Worse:
- At 115,000 reads/sec, a single PostgreSQL instance maxes out at ~10,000-15,000 QPS for point reads
- You would need to add read replicas, connection pooling (PgBouncer), and a separate caching layer
- Schema migrations on a 36.5B-row table are extremely painful
- Horizontal sharding PostgreSQL is complex and operationally expensive

**Why DynamoDB:**

1. **Single-digit millisecond P99 latency** at any scale — this is a hard SLA Amazon guarantees
2. **Horizontal scaling is automatic** — you never manage shards
3. **On-demand capacity** — handles 10x traffic spikes without pre-provisioning
4. **Native TTL** — items with an `expires_at` attribute are automatically deleted (no background job needed)
5. **Point-in-time recovery** — 35 days of continuous backups built-in
6. **DynamoDB Streams** — if you need to react to URL creations (warm a secondary cache, trigger a webhook), streams give you a CDC feed for free

**The cost argument:** DynamoDB on-demand for 115,000 reads/sec = ~$0.25/hour. Adding a Redis cache in front reduces DynamoDB reads by ~99% (cache hit rate), so actual DynamoDB cost is negligible.

**Alternatives worth mentioning:**
- **Redis alone:** Not durable enough as a primary store. Data loss on node failure without AOF persistence, which slows writes.
- **Cassandra:** Good choice if you're self-hosting and need multi-region active-active writes. More operationally complex than DynamoDB.
- **MongoDB:** Has a key-value lookup mode but brings document-store overhead. Not a natural fit.

**DynamoDB Schema:**

```
Table: url_mappings
  Partition Key: short_code (String)  — e.g., "dK3mZ9p"
  
  Attributes:
    long_url         (String)   — the original URL
    user_id          (String)   — owner of this short link
    created_at       (Number)   — Unix timestamp (ms)
    expires_at       (Number)   — Unix timestamp (ms), used as DynamoDB TTL
    is_custom        (Boolean)  — true if user provided the alias
    is_active        (Boolean)  — false if user deleted the link

GSI: user_id-index
  Partition Key: user_id
  Sort Key: created_at
  — For "list all my links" sorted by creation time
```

### 7.3 Redirect Type: 301 vs 302 and Why It Matters for Analytics

This is a classic interview question embedded in this problem. Get it right.

**301 Moved Permanently**
- Tells the browser: "This resource has permanently moved. Cache this redirect forever."
- On subsequent visits to the same short URL, the browser goes directly to the long URL **without contacting your servers**.
- Pros: Dramatically reduces server load. Once a user has visited `short.ly/abc123`, all future visits are handled entirely by the browser cache.
- Cons: **You lose all analytics after the first visit.** The browser never contacts you again, so you never see the click.

**302 Found (Moved Temporarily)**
- Tells the browser: "This resource is temporarily at this other location. Don't cache it."
- Every visit to the short URL goes through your redirect service.
- Pros: You see every click. Full analytics fidelity.
- Cons: Every redirect consumes server resources and adds latency vs. a cached 301.

**307 Temporary Redirect**
- Same as 302, but guarantees the HTTP method is preserved (POST stays POST). Not relevant for URL shorteners since users always GET a short URL.

**My Recommendation: Use 302.**

Here is why: The entire value proposition of a URL shortener as a business is analytics. If you use 301, you lose the ability to track clicks after the first visit, invalidate/update redirects, or charge customers for analytics features. You're essentially giving away your server-side cache benefit at the cost of your business model.

The performance cost of 302 vs 301 is: one round-trip through your CDN/redirect service per click. With a Redis cache hit at < 1ms and a CDN edge node geographically close to the user, this round trip adds 10-30ms to the user experience — imperceptible.

**The CDN Nuance:**

If you put a CDN (CloudFront) in front and cache the 302 response for 60 seconds, you get most of the performance benefit of 301 (CDN edge handles the redirect) while still sending 302 to the browser (browser doesn't cache it). This is the production-grade solution:

- CDN caches the `short_code → long_url` mapping for 60 seconds
- Browser always makes a round trip (never caches the 302)
- Your analytics pipeline sees every click at the CDN edge via access logs
- Your origin servers handle only cache misses (~1% of traffic after cache warm-up)

**For analytics-disabled accounts (if you offer a free tier without analytics):** Use 301 + a long CDN TTL. This maximizes performance.

### 7.4 Hot URL Caching

**The Problem**

Not all URLs are created equal. When a celebrity tweets a short link to 50M followers, your system receives 500,000 requests/second for a single `short_code` key within minutes. This is the "thundering herd" problem.

Without caching, every request hits DynamoDB. DynamoDB has per-partition throughput limits. A single partition can handle ~3,000 RCU/second (read capacity units). 500,000 requests/second for one key = ~167x the per-partition limit. DynamoDB will throttle these requests.

**Solution: Multi-Layer Caching**

```
Layer 1: CDN (CloudFront edge)
  — TTL: 10-60 seconds for hot URLs
  — Geographic distribution: requests served from the closest edge node
  — Hit rate for hot URLs: ~99.9%
  — Adds ~5-10ms to first request per region, near-zero for cached

Layer 2: In-Process Cache (each Redirect Service instance)
  — Local LRU cache, ~10,000 entries, TTL: 5 seconds
  — Zero network round trip
  — Hit rate for hot URLs: ~95% (traffic is bursty but repeated from same pods)
  — Implementation: Caffeine (Java) or functools.lru_cache with TTL (Python)

Layer 3: Redis Cluster
  — 300 GB of URL mappings
  — TTL: matches URL expiration
  — Hit rate: ~99.99% for any URL touched in the last week
  — Sub-millisecond reads

Layer 4: DynamoDB
  — Last resort, source of truth
  — Only reached on first-ever cache miss or after cache eviction
```

**Cache Population Strategy**

On URL creation, proactively write to Redis:
```python
def create_short_url(long_url: str) -> str:
    short_code = generate_code()
    dynamodb.put_item(short_code, long_url, expires_at)
    redis.set(short_code, long_url, ex=ttl_seconds)  # warm cache immediately
    return short_code
```

This ensures the cache is warm before the first redirect, eliminating the thundering herd on the initial burst after a popular URL is created.

**Cache Invalidation**

When a user deletes or updates their URL:
1. Mark `is_active = false` in DynamoDB
2. Delete from Redis (`redis.delete(short_code)`)
3. Issue a CDN cache invalidation (CloudFront has a `CreateInvalidation` API)

CDN invalidations propagate in ~10 seconds globally. During that window, some users may still get the old redirect — this is acceptable.

**Hot Key Detection**

Implement a sliding window counter in Redis to detect hot keys:
```
INCR url:hitcount:{short_code}
EXPIRE url:hitcount:{short_code} 60  # count hits per 60-second window
```

If a key exceeds 10,000 hits/minute, flag it as "hot" and:
1. Extend its CDN TTL to 60 seconds (via a CloudFront cache policy override)
2. Pin it in the in-process cache with a longer TTL
3. Alert the analytics team (a hot URL means a viral event is happening)

---

## 8. Trade-offs and Alternatives

### Approach A: Hash-based vs Counter-based ID Generation

| | Hash-based | Counter-based (with scrambling) |
|---|---|---|
| Collision risk | Yes (small but real) | None |
| Idempotent | Yes (same URL → same code) | No |
| Sequential? | No | No (after scrambling) |
| Complexity | Low | Medium |
| Preferred when | Low scale, simplicity desired | High scale, collision-free required |

**My recommendation:** Counter-based with a Feistel scramble. At 1,200 writes/sec, even a 0.001% collision rate means ~1.2 collisions/second — each requiring a retry. Over a day, that's 100,000 collisions. Counter-based eliminates this entirely.

### Approach B: Separate Analytics Service vs Inline Analytics

**Option 1: Inline (synchronous)** — update click count in DynamoDB on every redirect.

```
Pros: Simple, always consistent
Cons: Adds 1-2ms to redirect latency. At 115,000 RPS, this is 115,000 DynamoDB writes/second 
      for analytics alone. Expensive and slow.
```

**Option 2: Async Kafka pipeline (our design)** — publish event to Kafka, consumer aggregates.

```
Pros: Zero impact on redirect latency. Kafka absorbs bursts. Analytics is independently scalable.
Cons: Analytics is eventually consistent (seconds to minutes behind). Slightly more complex.
```

**Option 3: CDN log-based analytics** — extract analytics from CloudFront access logs.

```
Pros: Zero code changes. Logs are always there.
Cons: Coarse granularity (5-minute batches). Hard to compute per-user metrics.
      Only captures CDN-level events, not direct requests.
```

**Verdict:** Async Kafka pipeline is the right answer for a production system at this scale.

### Approach C: Separate Read and Write Services vs Monolith

At 115,000 reads/sec vs 1,200 writes/sec, the read path needs 100x more compute than the write path. If you run them in the same service, you either over-provision for writes or under-provision for reads. Separate services allow independent scaling: run 50 redirect service pods and 2 creation service pods. This is standard CQRS (Command Query Responsibility Segregation) applied at the service level.

---

## 9. Failure Scenarios

### Scenario 1: Redis Cluster Goes Down

**Impact:** All 115,000 reads/second hit DynamoDB directly.
**DynamoDB capacity:** On-demand mode can scale, but a sudden 115,000 RPS spike will trigger throttling during the scaling period (~1-2 minutes).
**Mitigation:**
- Circuit breaker on Redis client: if Redis is unavailable, immediately fall back to DynamoDB without waiting for Redis timeout
- Pre-provision DynamoDB for 20% of peak load (handles cache failover gracefully)
- In-process L1 cache absorbs the first 5-second spike before DynamoDB can scale

### Scenario 2: DynamoDB Partition Throttling (Hot Key)

**Symptom:** Redirect service gets `ProvisionedThroughputExceededException` for a specific short code.
**Root cause:** 500,000 RPS for one key exceeds DynamoDB's per-partition limit.
**Mitigation:**
- Multi-layer caching means this almost never reaches DynamoDB. CDN and Redis absorb >99.9% of hot key traffic.
- If it still happens, use DAX (DynamoDB Accelerator) — an in-memory cache native to DynamoDB with sub-microsecond read latency

### Scenario 3: Short Code Collision Race Condition

**Scenario:** Two creation service instances simultaneously try to claim the same custom alias.
**Mitigation:** DynamoDB conditional write (`attribute_not_exists(short_code)`) is atomic. Exactly one write succeeds. The loser gets `ConditionalCheckFailedException` and returns HTTP 409 to the client.

### Scenario 4: Kafka Consumer Lag (Analytics Backlog)

**Symptom:** Kafka consumer falls behind — click events are being produced faster than consumed.
**Impact:** Analytics data is delayed. No impact on redirect service (they're decoupled).
**Mitigation:**
- Scale up Flink consumer parallelism (add more task managers)
- Kafka retains 7 days of events — consumers can always catch up
- Alert when consumer lag exceeds 10 minutes

### Scenario 5: Redirect Service Deployment / Rolling Restart

**Concern:** During a rolling deploy, 50% of pods restart simultaneously. In-process caches are cleared.
**Mitigation:**
- Rolling deploys restart pods one at a time (Kubernetes `maxUnavailable: 1`)
- Redis cluster is unaffected — in-process cache loss means a short traffic spike to Redis, which handles it
- Pre-warm L1 cache on pod startup by pre-loading top-1000 hot URLs from Redis

### Scenario 6: User Wants to "Update" a Short URL

This is a design decision, not a failure, but treat it as one:
- URL shorteners generally don't allow editing a short code's destination (immutability is a feature — it prevents phishing redirects)
- If update is required, soft-delete the old record (set `is_active = false`), create a new short code, and issue a redirect from old to new
- Invalidate both CDN and Redis for the old code

---

## 10. Interview Tips

### Time Management for 45 Minutes

```
0-2 min:   Read problem, take notes
2-8 min:   Clarifying questions (get through all 7)
8-12 min:  Requirements and capacity estimation
12-18 min: High-level design (draw the diagram, explain components)
18-38 min: Deep dives (spend 5 minutes each on 4 topics)
38-42 min: Trade-offs and alternatives
42-45 min: Failure scenarios (pick the 2 most interesting)
```

### What Separates Staff from Senior Candidates

**Senior candidate** correctly identifies that this is a read-heavy system, chooses a key-value store, adds caching, and uses async analytics.

**Staff candidate** additionally:
- Proactively discusses the 301 vs 302 trade-off and its business model implications
- Identifies the per-partition hot key problem in DynamoDB before being asked
- Proposes multi-layer caching (L1 in-process + L2 Redis + L3 CDN) as a coherent strategy
- Quantifies the impact of each design decision ("using 301 reduces server load by ~95% but kills analytics fidelity")
- Handles custom aliases with conditional writes rather than check-then-insert
- Recognizes that the analytics pipeline is a separate design problem and explicitly defers it

### Common Mistakes to Avoid

1. **Using SQL for primary URL storage.** When the interviewer asks why, say it clearly: "Every access pattern is a point lookup by primary key. SQL's power (joins, aggregations, constraints) is entirely unused here and its vertical scaling model is a liability at 115K reads/sec."

2. **Ignoring TTL/expiration.** DynamoDB native TTL is a beautiful feature. Use it. Don't design a background job to clean up expired URLs when the database can do it for free.

3. **Putting analytics on the critical path.** Analytics is important but not latency-sensitive. A click happened. We need to record it eventually. This is a perfect fire-and-forget Kafka publish.

4. **Not discussing the 301 vs 302 trade-off.** This is a famous interview trap. If you use 301, you lose analytics. If you use 302 naively (no CDN), your servers handle every redirect. The right answer is 302 + CDN caching.

5. **Forgetting cache invalidation.** If a user deletes their URL, you must invalidate all three cache layers: CDN, Redis, and in-process. Forgetting any one of them means deleted links continue working for some users.

6. **Using random UUIDs as short codes.** UUIDs are 36 characters. The whole point is to be short. Base62 gives you 7 characters with 3.5 trillion unique values.

### Strong Closing Statement

> "To summarize the key design decisions: I chose a counter-based Base62 encoding scheme with Feistel scrambling to eliminate collisions entirely while preventing enumeration attacks. The primary store is DynamoDB for its native TTL, horizontal scalability, and conditional write semantics. The redirect service is intentionally thin — it checks a multi-layer cache hierarchy (in-process LRU → Redis → DynamoDB) and publishes analytics events to Kafka asynchronously. I chose 302 redirects with CDN caching to preserve analytics fidelity while achieving near-301 performance. The analytics pipeline is completely decoupled from the redirect path, allowing both to scale independently."

This is a clean, confident close that demonstrates you understand the full system.

---

*End of URL Shortener System Design — estimated interview preparation time: 3-4 hours of active practice*
