# System Design — Master Index

> Target: Senior / Staff engineer interviews at FAANG / MAANG.
> Structure: three layers that mirror how interviewers actually probe.

---

## How to Use This Section

System design interviews test two different skills. Know which one is being tested:

| Round Type | What's Tested | Time |
|---|---|---|
| **HLD (High-Level Design)** | Scale, architecture, trade-offs, distributed systems | 45-60 min |
| **LLD (Low-Level Design)** | OOP, design patterns, class design, extensibility | 45-60 min |

Some companies (Google, Meta) separate these rounds. Others (Amazon) mix them. Either way, you need both.

**Study order:**
1. Foundations → Core Patterns → Systems (for HLD)
2. SOLID → Design Patterns → Case Studies (for LLD)
3. Cross-reference: most system walkthroughs reference patterns from the LLD section

---

## Layer 1 — Foundations

The building blocks that appear in every system design answer. Know these cold before attempting any system walkthroughs.

| File | Core Topics |
|---|---|
| [01_Networking.md](./Foundations/01_Networking.md) | DNS, TCP/UDP, HTTP/2/3, WebSockets, CDN, REST vs gRPC |
| [02_Caching.md](./Foundations/02_Caching.md) | Cache-aside vs write-through, Redis vs Memcached, eviction, thundering herd |
| [03_Databases.md](./Foundations/03_Databases.md) | SQL vs NoSQL, B-tree vs LSM, sharding, replication, CAP theorem |
| [04_Messaging.md](./Foundations/04_Messaging.md) | Kafka architecture, delivery guarantees, backpressure, consumer groups |
| [05_LoadBalancing.md](./Foundations/05_LoadBalancing.md) | L4 vs L7, algorithms, sticky sessions, health checks |
| [06_Storage.md](./Foundations/06_Storage.md) | Object vs block vs file storage, erasure coding, storage tiers |

---

## Layer 2 — Core Patterns

Recurring design patterns that appear across every system. Each walkthrough in Layer 3 references 2-3 of these.

| File | Core Topics |
|---|---|
| [01_ConsistentHashing.md](./CorePatterns/01_ConsistentHashing.md) | Ring, virtual nodes, Cassandra/DynamoDB/Redis |
| [02_RateLimiting.md](./CorePatterns/02_RateLimiting.md) | Token bucket, sliding window, Redis distributed rate limiting |
| [03_FanOut.md](./CorePatterns/03_FanOut.md) | Push vs pull vs hybrid, celebrity problem, Twitter's model |
| [04_DistributedLocking.md](./CorePatterns/04_DistributedLocking.md) | Redlock, ZooKeeper, fencing tokens, when NOT to use locks |
| [05_Idempotency.md](./CorePatterns/05_Idempotency.md) | Idempotency keys, outbox pattern, exactly-once delivery |
| [06_APIGateway.md](./CorePatterns/06_APIGateway.md) | BFF, circuit breaker, bulkhead, service mesh |

---

## Layer 3 — System Walkthroughs

Full 45-minute interview simulations. Each one: requirements → estimation → architecture → deep dives → trade-offs.

| File | Key Concepts |
|---|---|
| [01_URLShortener.md](./Systems/01_URLShortener.md) | Base62, key-value DB, caching hot URLs, 301 vs 302 |
| [02_RateLimiter.md](./Systems/02_RateLimiter.md) | Token bucket at scale, Redis Lua scripts, API gateway placement |
| [03_NotificationSystem.md](./Systems/03_NotificationSystem.md) | Fan-out, APNs/FCM/SMS, priority queues, deduplication |
| [04_TwitterFeed.md](./Systems/04_TwitterFeed.md) | Timeline generation, hybrid fan-out, Redis sorted sets |
| [05_DistributedCache.md](./Systems/05_DistributedCache.md) | Consistent hashing for cache, eviction, cache warming |
| [06_SearchAutocomplete.md](./Systems/06_SearchAutocomplete.md) | Trie, top-K, offline aggregation, real-time trends |
| [07_Uber.md](./Systems/07_Uber.md) | Geohash/S2, driver matching, trip state machine, surge |
| [08_WebCrawler.md](./Systems/08_WebCrawler.md) | BFS frontier, politeness, Bloom filter dedup, spider traps |

## HLD Reference Docs

Supporting reference material for system design rounds.

| File | Core Topics |
|---|---|
| [HLD/Cloud.md](./HLD/Cloud.md) | EC2/Lambda/ECS, S3, DynamoDB, RDS, SQS/SNS, CloudFront, Route 53 |
| [HLD/Security.md](./HLD/Security.md) | JWT/OAuth 2.0, secrets management, TLS, SQLi/XSS/CSRF/DDoS/SSRF |
| [HLD/Observability.md](./HLD/Observability.md) | Logs/Metrics/Traces, RED/USE, SLO/SLI/SLA, error budget, runbooks |

---

## LLD Track

### Start Here
| File | What It Covers |
|---|---|
| [LLD/README.md](./LLD/README.md) | The 4-step LLD approach, trigger table, time management, what NOT to do |
| [LLD/SOLID.md](./LLD/SOLID.md) | All 5 SOLID principles + DRY/KISS/YAGNI with before/after Java code |

### Design Patterns

> [LLD/Patterns/README.md](./LLD/Patterns/README.md) — Which 12 to know, which 10 to just name, confusion pairs, trigger table, study order.

**Creational** — how objects are created

| Pattern | Key Use Case |
|---|---|
| [Singleton](./LLD/Patterns/Creational/Singleton.md) | Config manager, connection pool — thread-safe variants |
| [Builder](./LLD/Patterns/Creational/Builder.md) | Complex object construction, fluent API |
| [Factory Method](./LLD/Patterns/Creational/Factory.md) | Create objects without specifying exact class |
| [Abstract Factory](./LLD/Patterns/Creational/AbstractFactory.md) | Families of related objects |

**Structural** — how objects are composed

| Pattern | Key Use Case |
|---|---|
| [Decorator](./LLD/Patterns/Structural/Decorator.md) | Add behavior without subclassing — HTTP middleware |
| [Adapter](./LLD/Patterns/Structural/Adapter.md) | Legacy system integration |
| [Proxy](./LLD/Patterns/Structural/Proxy.md) | Lazy loading, protection, remote proxy |
| [Facade](./LLD/Patterns/Structural/Facade.md) | Simplify complex subsystem |

**Behavioural** — how objects communicate

| Pattern | Key Use Case |
|---|---|
| [Observer](./LLD/Patterns/Behavioural/Observer.md) | Event-driven notification |
| [Strategy](./LLD/Patterns/Behavioural/Strategy.md) | Swappable algorithms at runtime |
| [Command](./LLD/Patterns/Behavioural/Command.md) | Undo/redo, request queuing |
| [State](./LLD/Patterns/Behavioural/State.md) | State machines — vending machine, ATM, order status |
| [Template Method](./LLD/Patterns/Behavioural/Template.md) | Algorithm skeleton with overridable steps |

### LLD Case Studies

| File | Patterns Used | Tricky Parts |
|---|---|---|
| [ParkingLot.md](./LLD/CaseStudies/ParkingLot.md) | Strategy, State, Factory | Concurrent spot assignment, vehicle size matrix |
| [VendingMachine.md](./LLD/CaseStudies/VendingMachine.md) | State | Partial payment, exact change, concurrent access |
| [BookMyShow.md](./LLD/CaseStudies/BookMyShow.md) | Strategy, Observer, Facade, Command | Concurrent seat booking race condition, seat locking |
| [ATM.md](./LLD/CaseStudies/ATM.md) | State, Chain of Responsibility, Command | Atomicity of debit+dispense, PIN lockout |
| [Elevator.md](./LLD/CaseStudies/Elevator.md) | State, Strategy, Observer | LOOK scheduling algorithm, same-floor conflict, emergency |

### LLD Supporting Docs

| File | Core Topics |
|---|---|
| [LLD/ConcurrencyPatterns.md](./LLD/ConcurrencyPatterns.md) | Producer-Consumer, RW Lock, Thread Pool, CompletableFuture, thread-safe LRU/rate limiter/scheduler |

---

## HLD Interview Cheat Sheet

**The 6 questions to clarify before designing anything:**
1. Scale — how many users/requests?
2. Read vs write ratio?
3. Consistency requirements — eventual OK?
4. Latency requirements — P99?
5. Global vs single-region?
6. Any special constraints (compliance, cost)?

**The 5 trade-offs interviewers test:**
1. Consistency vs Availability (CAP)
2. SQL vs NoSQL — always justify with access patterns
3. Sync vs Async — when does async hurt?
4. Push vs Pull — fan-out patterns
5. Monolith vs Microservices — don't say microservices by default

**Numbers every senior engineer knows:**
```
Memory access:           ~100 ns
SSD random read:         ~100 µs
Network (same region):   ~1 ms
Network (cross-region):  ~100 ms
HDD seek:                ~10 ms

Redis throughput:        ~100K ops/sec per instance
Kafka throughput:        ~1M messages/sec per partition (writes)
MySQL:                   ~1K writes/sec (single primary, no sharding)
PostgreSQL:              ~5K writes/sec
S3 throughput:           ~3,500 PUT/sec, ~5,500 GET/sec per prefix
```

---

## LLD Interview Cheat Sheet

**Pattern trigger table — when the interviewer says X, think Y:**

| Interviewer says | Pattern to reach for |
|---|---|
| "Add behavior dynamically without modifying the class" | Decorator |
| "Support multiple algorithms / strategies" | Strategy |
| "Notify many objects when state changes" | Observer |
| "Ensure only one instance exists" | Singleton |
| "Build complex objects step by step" | Builder |
| "The object behavior depends on its current state" | State |
| "Undo / redo operations" | Command |
| "Adapt an existing class to a new interface" | Adapter |
| "Hide a complex subsystem behind a simple interface" | Facade |
| "Create objects without specifying exact class" | Factory Method |
| "Algorithm skeleton with swappable steps" | Template Method |

---

## Quick Reference — What to Read the Night Before

| Interview type | Files to review |
|---|---|
| HLD round | Foundations/02_Caching + 03_Databases + 04_Messaging → CorePatterns/01_ConsistentHashing + 03_FanOut → most relevant System |
| LLD round | LLD/README + LLD/SOLID → relevant Pattern docs → relevant Case Study |
| Mixed round | CorePatterns/06_APIGateway + LLD/README → Pattern trigger table |
