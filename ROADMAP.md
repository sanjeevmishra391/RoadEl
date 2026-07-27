# Interview Preparation Roadmap
**Timeline:** 4 Weeks | **Target:** Senior / Staff — FAANG / MAANG

---

## Core Rules

1. **DSA every single day** — 2 problems minimum, no exceptions. Do it first.
2. **Every study session = a file in this repo.** If you studied it, it's written down.
3. **Speak system design out loud** — knowing it and explaining it are different skills.
4. **STAR answers must come from your real work** — generic answers are filtered out.
5. **AI/LLM knowledge is your differentiator**

---

## Daily Schedule (3-4 hrs/day)

| Block | Time | Non-Negotiable? |
|---|---|---|
| DSA problems (2 minimum) | 1.5 hrs | YES |
| Weekly deep-dive topic | 1.5 hrs | YES |
| Flashcard review (prev topics) | 30 min | Recommended |

**Saturday** → Timed mock interview (no notes, treat as real)
**Sunday** → Repo cleanup + identify weakest topic for next week

---

## Week 1 — Java Depth + DSA Foundations
> Goal: Warm up DSA rhythm. Fill Java gaps interviewers actually probe.

### DSA Focus This Week
Arrays · Strings · Two Pointers · Sliding Window · Binary Search
Platform: [NeetCode.io](https://neetcode.io/practice) — use the structured roadmap

### Daily Java Deep-Dive

| Day | Topic | File |
|---|---|---|
| Mon | Java Memory Model: heap/stack, GC, strong/weak/soft references | `Java/ModernJava/MemoryModel.md` |
| Tue | Collections internals: HashMap, LinkedHashMap, TreeMap, ConcurrentHashMap | `Java/ModernJava/Collections.md` |
| Wed | Concurrency: CompletableFuture, ExecutorService, volatile, AtomicInteger | `Java/ModernJava/Concurrency.md` |
| Thu | Java 11-17: records, sealed classes, var, pattern matching for instanceof | `Java/ModernJava/Java11to17.md` |
| Fri | Stream API: advanced collectors, flatMap, groupingBy, reduce | `Java/ModernJava/Streams.md` |
| Sat | Mock: 2 DSA problems timed (45 min each) + Java quiz from your notes | — |
| Sun | Review weakest topic. Add Big-O comments to DSA files you solved this week | — |

### Repo Deliverables This Week
- [ ] Read all `Java/ModernJava/` files (5 files)
- [ ] Solve 9 problems from `DSA/BinarySearch/BinarySearch.md`
- [ ] Solve problems in `DSA/SlidingWindow/SlidingWindow.md`
- [ ] Solve problems in `DSA/TwoPointer/TwoPointers.md`
- [ ] Add `// Time: O(...) Space: O(...)` to every DSA file you touch

---

## Week 2 — System Design Foundations + DSA Medium
> Goal: Build HLD intuition from scratch. Understand the building blocks before the systems.

### DSA Focus This Week
Trees · Graphs · Heaps · Stack/Queue patterns

### Daily System Design Deep-Dive

| Day | Topic | File |
|---|---|---|
| Mon | Networking + Caching: DNS, HTTP/2, CDN, Redis, write strategies, thundering herd | `SystemDesign/Foundations/01_Networking.md` + `02_Caching.md` |
| Tue | Databases at scale: SQL vs NoSQL, sharding, replication, CAP, PACELC | `SystemDesign/Foundations/03_Databases.md` |
| Wed | Messaging + Load Balancing: Kafka internals, delivery guarantees, LB algorithms | `SystemDesign/Foundations/04_Messaging.md` + `05_LoadBalancing.md` |
| Thu | Core Patterns: Consistent Hashing, Rate Limiting, Fan-Out | `SystemDesign/CorePatterns/01_ConsistentHashing.md` + `02_RateLimiting.md` + `03_FanOut.md` |
| Fri | Core Patterns: Distributed Locking, Idempotency, API Gateway + Circuit Breaker | `SystemDesign/CorePatterns/04_DistributedLocking.md` + `05_Idempotency.md` + `06_APIGateway.md` |
| Sat | Mock: 1 medium DSA (45 min) + 1 system design simulation (45 min) | — |
| Sun | Design URL Shortener and Rate Limiter end-to-end out loud | `SystemDesign/Systems/01_URLShortener.md` + `02_RateLimiter.md` |

### Repo Deliverables This Week
- [ ] Read all 6 `SystemDesign/Foundations/` files
- [ ] Read all 6 `SystemDesign/CorePatterns/` files
- [ ] Walk through `Systems/01_URLShortener.md` and `02_RateLimiter.md`
- [ ] Solve `DSA/Heap/Heap.md` problems (Top K, Merge K, Median from Stream)
- [ ] Solve `DSA/Tree/Tree.md` problems
- [ ] Solve `DSA/Graph/Graph.md` problems

---

## Week 3 — LLD + System Walkthroughs + DSA Hard
> Goal: LLD fluency. Walk through 4 real systems. Push DSA to hard.

### DSA Focus This Week
Dynamic Programming · Backtracking · Intervals · LinkedList patterns

### Daily Deep-Dive

| Day | Topic | File |
|---|---|---|
| Mon | LLD foundations: SOLID, pattern trigger table, 4-step approach | `SystemDesign/LLD/SOLID.md` + `SystemDesign/LLD/README.md` |
| Tue | Design Patterns: Creational (Singleton, Builder, Factory, AbstractFactory) | `SystemDesign/LLD/Patterns/Creational/` |
| Wed | Design Patterns: Structural (Decorator, Adapter, Proxy, Facade) + Behavioural (Observer, Strategy, Command, State, Template) | `SystemDesign/LLD/Patterns/Structural/` + `Behavioural/` |
| Thu | LLD Case Studies: ParkingLot + VendingMachine + Elevator | `SystemDesign/LLD/CaseStudies/ParkingLot.md` + `VendingMachine.md` + `Elevator.md` |
| Fri | LLD Case Studies: BookMyShow + ATM + Concurrency Patterns | `SystemDesign/LLD/CaseStudies/BookMyShow.md` + `ATM.md` + `ConcurrencyPatterns.md` |
| Sat | Mock: 1 hard DSA (45 min) + 1 LLD round (45 min) | — |
| Sun | HLD systems: walk through TwitterFeed + NotificationSystem out loud | `SystemDesign/Systems/03_NotificationSystem.md` + `04_TwitterFeed.md` |

### Repo Deliverables This Week
- [ ] Read `SystemDesign/LLD/SOLID.md` + `LLD/README.md`
- [ ] Read all 13 design pattern docs in `SystemDesign/LLD/Patterns/`
- [ ] Read all 5 LLD case studies
- [ ] Walk through 4 HLD system walkthroughs
- [ ] Solve `DSA/Dynamic/DP.md` problems
- [ ] Solve `DSA/Backtracking/Backtracking.md` problems
- [ ] Solve `DSA/Intervals/Intervals.md` problems
- [ ] Solve `DSA/LinkedList/LinkedList.md` problems

---

## Week 4 — Behavioral + HLD Polish + Final Mock Rounds
> Goal: Interview-ready end-to-end. No new foundational topics. Consolidate + differentiate.

### DSA Focus This Week
Mixed simulation — pick randomly across all patterns, simulate real interview conditions

### Daily Plan

| Day | Focus | File |
|---|---|---|
| Mon | DBMS deep read + SQL window functions | `DBMS/DBMS.md` |
| Tue | Cloud services + Security in system design | `SystemDesign/HLD/Cloud.md` + `SystemDesign/HLD/Security.md` |
| Wed | Observability + remaining HLD systems (DistributedCache, Autocomplete, Uber, WebCrawler) | `SystemDesign/HLD/Observability.md` + `SystemDesign/Systems/05-08` |
| Thu | Write STAR answers for 10 behavioral questions | `Behavioural/STAR.md` |
| Fri | Full mock day: DSA (1 hr) + System Design (45 min) + Behavioral (30 min) back-to-back | — |
| Sat | Full mock interview — completely timed, no notes, treat as the real thing | — |
| Sun | Gap fill: revisit 3 weakest topics. Review all HLD designs out loud | — |

### Repo Deliverables This Week
- [ ] Read `SystemDesign/HLD/Cloud.md` + `Security.md` + `Observability.md`
- [ ] Walk through `Systems/05_DistributedCache.md` through `08_WebCrawler.md`
- [ ] Write 10 real STAR answers in `Behavioural/STAR.md`
- [ ] Final pass: review pattern trigger table + numbers cheat sheet in `SystemDesign/README.md`

---

## DSA Problem Targets

| Week | Difficulty | Daily | Cumulative | Platform |
|---|---|---|---|---|
| 1 | Easy / Easy-Medium | 2 | ~14 | NeetCode — Arrays, Strings, Binary Search |
| 2 | Medium | 2 | ~28 | NeetCode — Trees, Graphs, Heap |
| 3 | Medium / Hard | 2 | ~42 | NeetCode — DP, Backtracking, Intervals |
| 4 | Mixed | 2 | ~56 | Random from all patterns |

---

## Pattern Recognition Quick Reference

> Full guide → [`DSA/HowToPrepareDSA.md`](./DSA/HowToPrepareDSA.md)

| Trigger | Pattern |
|---|---|
| Contiguous subarray + condition | Sliding Window |
| Sorted array + pair/triplet | Two Pointers |
| Shortest path, unweighted | BFS |
| All combinations / permutations | Backtracking |
| Optimal decision at each step | Dynamic Programming |
| Next greater / smaller element | Monotonic Stack |
| Kth largest / Top K / Median | Heap |
| Dependency ordering | Topological Sort |
| Prefix search / autocomplete | Trie |
| Connected components / cycle | Union-Find |
| Overlapping intervals | Interval sweep (sort by start) |
| Reverse / cycle in list | Fast/Slow pointer |

---

## Topic Priority Matrix

| Topic | Weight at Interview | Status | Week |
|---|---|---|---|
| DSA | Very High | Covered — push harder | All |
| System Design HLD | Very High | Covered — Foundations + Patterns + 8 systems | 2-4 |
| LLD + Design Patterns | High | Covered — 13 patterns + 5 case studies | 3 |
| Java (modern) | High | Covered — Java 8-17 | 1 |
| Behavioral | High (filter) | Covered — STAR guide + 12 questions | 4 |
| DBMS | Medium-High | Theory solid | 4 |
| Cloud (AWS) | Medium-High | Covered — Cloud.md | 4 |
| Security | Medium-High | Covered — Security.md | 4 |
| Observability | Medium | Covered — Observability.md | 4 |
| Concurrency Patterns (LLD) | Medium | Covered — ConcurrencyPatterns.md | 3 |

---

## STAR Answer Template

```
Situation: What was the context? (1-2 sentences)
Task:       What was YOUR specific responsibility?
Action:     What did YOU do, step by step? (most important — be specific, 5-7 sentences)
Result:     Measurable outcome — numbers, impact, what changed
```

Full guide with 12 questions, Amazon LPs, and calibration signals → [`Behavioural/STAR.md`](./Behavioural/STAR.md)

---

## HLD Quick-Reference Numbers

```
Memory access:           ~100 ns
SSD random read:         ~100 µs
Network same region:     ~1 ms
Network cross-region:    ~100 ms

Redis:                   ~100K ops/sec per instance
Kafka:                   ~1M msgs/sec per broker
MySQL single primary:    ~1K writes/sec
PostgreSQL:              ~5K writes/sec
S3:                      ~3,500 PUT/sec, ~5,500 GET/sec per prefix
DynamoDB on-demand:      unlimited (you pay per request)
```

Full cheat sheet → [`SystemDesign/README.md`](./SystemDesign/README.md)

---

## Quick Decision Guide

| Question | Go to |
|---|---|
| "I don't know which DSA pattern to use" | [DSA/HowToPrepareDSA.md](./DSA/HowToPrepareDSA.md) |
| "I know the pattern but not the template" | `DSA/Algorithms/<pattern>.md` or the topic `.md` |
| "I need a solved problem" | `DSA/<Topic>/<Topic>.md` |
| "I need to prep for HLD" | [SystemDesign/README.md](./SystemDesign/README.md) → Foundations → CorePatterns → Systems |
| "I need to prep for LLD" | [SystemDesign/LLD/README.md](./SystemDesign/LLD/README.md) → SOLID → Patterns → Case Studies |
| "I'm doing an Amazon loop" | [Behavioural/STAR.md](./Behavioural/STAR.md) — read the LP section |
| "I need Java internals" | [Java/ModernJava/README.md](./Java/ModernJava/README.md) |

---

## Complete File Map

```
RoadEl/
│
├── ROADMAP.md                 ← You are here
├── Resources.txt              ← External links
│
├── DSA/
│   ├── HowToPrepareDSA.md     ← Pattern trigger table + study process
│   ├── Arrays/Arrays.md
│   ├── Algorithms/            ← TwoPointers, SlidingWindow, Kadane (theory)
│   ├── TwoPointer/            ← 10 problems with solutions
│   ├── SlidingWindow/         ← 7 problems with solutions
│   ├── BinarySearch/          ← 3 templates + 9 problems
│   ├── Heap/Heap.md           ← Top K, Merge K, Median from Stream
│   ├── Tree/Tree.md           ← DFS/BFS templates + 10 problems
│   ├── Graph/Graph.md         ← All graph algorithms
│   ├── Stack/Stack.md         ← Monotonic stack
│   ├── Sorting/Sorting.md     ← 8 algorithms compared
│   ├── Dynamic/DP.md          ← 5-step framework + problems
│   ├── Backtracking/          ← Template + 8 problems
│   ├── Intervals/             ← Sweep + 7 problems
│   ├── LinkedList/            ← Fast/slow pointer + LRU
│   ├── Trie/Trie.md
│   └── String/String.md
│
├── Java/
│   ├── Java.md
│   └── ModernJava/            ← MemoryModel, Collections, Concurrency,
│                                 Streams, Java11to17
│
├── SystemDesign/
│   ├── README.md              ← Master index + cheat sheets
│   ├── Foundations/           ← Networking, Caching, Databases,
│   │                             Messaging, LoadBalancing, Storage
│   ├── CorePatterns/          ← ConsistentHashing, RateLimiting,
│   │                             FanOut, DistributedLocking, Idempotency,
│   │                             APIGateway
│   ├── Systems/               ← 8 HLD walkthroughs: URLShortener,
│   │                             RateLimiter, Notifications, TwitterFeed,
│   │                             DistributedCache, SearchAutocomplete,
│   │                             Uber, WebCrawler
│   ├── HLD/                   ← Cloud, Security, Observability
│   └── LLD/
│       ├── README.md          ← 4-step approach + trigger table
│       ├── SOLID.md           ← All 5 principles + DRY/KISS/YAGNI
│       ├── ConcurrencyPatterns.md
│       ├── Patterns/
│       │   ├── README.md      ← 12 essential vs 10 to-know
│       │   ├── Creational/    ← Singleton, Builder, Factory, AbstractFactory
│       │   ├── Structural/    ← Decorator, Adapter, Proxy, Facade
│       │   └── Behavioural/   ← Observer, Strategy, Command, State, Template
│       └── CaseStudies/       ← ParkingLot, VendingMachine, BookMyShow,
│                                 ATM, Elevator
│
├── DBMS/
│   └── DBMS.md                ← Keys, joins, ACID, CAP, indexing
│
└── Behavioural/
    └── STAR.md                ← 12 questions, Amazon LPs, story bank
```

---

## Resources

| Topic | Link |
|---|---|
| DSA Problems | https://neetcode.io/practice |
| System Design | https://github.com/ashishps1/awesome-system-design-resources |
| Low Level Design | https://github.com/ashishps1/awesome-low-level-design |
| System Design Book | https://bytebytego.com/courses/system-design-interview |
| Engineering Blogs | See `Resources.txt` |
