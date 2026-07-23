# Interview Preparation Roadmap
**Timeline:** 4 Weeks

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

| Day | Topic | Repo File |
|---|---|---|
| Mon | Java Memory Model: heap/stack, GC, strong/weak/soft references | `Java/ModernJava/MemoryModel.md` |
| Tue | Collections internals: HashMap, LinkedHashMap, TreeMap, ConcurrentHashMap | `Java/ModernJava/Collections.md` |
| Wed | Concurrency: CompletableFuture, ExecutorService, volatile, AtomicInteger | `Java/ModernJava/Concurrency.md` |
| Thu | Java 11-17: records, sealed classes, var, pattern matching for instanceof | `Java/ModernJava/Java11to17.md` |
| Fri | Stream API: advanced collectors, flatMap, groupingBy, reduce | `Java/ModernJava/Streams.md` |
| Sat | Mock: 2 DSA problems timed (45 min each) + Java quiz from your notes | — |
| Sun | Repo: Write `Java/ModernJava/` files. Add Big-O comments to DSA files | — |

### Repo Deliverables This Week
- [ ] `Java/ModernJava/` — 5 note files (one per day above)
- [ ] `DSA/BinarySearch/` — solve the 9 problems listed in `DSA/DSA.md`
- [ ] `DSA/SlidingWindow/` — 5 problems (min window substring, max sliding window, fruits in baskets)
- [ ] Add `// Time: O(...) Space: O(...)` comment to every existing DSA `.java` file

---

## Week 2 — System Design + DSA Medium
> Goal: Build HLD from zero. Cover 5 missing design patterns. Push DSA to medium.

### DSA Focus This Week
Trees · Graphs · Heaps · Stack/Queue patterns

### Daily System Design Deep-Dive

| Day | Topic | Repo File |
|---|---|---|
| Mon | HLD Foundations: load balancing (L4/L7), caching (Redis, write-through/write-back), CDN | `SystemDesign/HLD/Foundations.md` |
| Tue | Databases at scale: sharding, replication, consistent hashing, read replicas | `SystemDesign/HLD/Databases.md` |
| Wed | Async + Deployment: Kafka/RabbitMQ, pub-sub, Docker/K8s, CI/CD, microservices | `SystemDesign/HLD/AsyncSystems.md` |
| Thu | HLD Walkthrough: Design URL Shortener (end-to-end) | `SystemDesign/HLD/URLShortener.md` |
| Fri | HLD Walkthroughs: Rate Limiter + Notification System | `SystemDesign/HLD/RateLimiter.md` |
| Sat | Mock: 1 medium DSA (45 min) + 1 system design simulation (45 min) | — |
| Sun | Repo: Add `SystemDesign/HLD/`, add 5 design patterns with code | — |

### Repo Deliverables This Week
- [ ] `SystemDesign/HLD/Foundations.md` — caching, load balancing, CDN notes
- [ ] `SystemDesign/HLD/Databases.md` — sharding, replication, consistent hashing
- [ ] `SystemDesign/HLD/AsyncSystems.md` — Kafka vs RabbitMQ, Docker/K8s, CI/CD
- [ ] `SystemDesign/HLD/URLShortener.md` — full design walkthrough
- [ ] `SystemDesign/HLD/RateLimiter.md` — token bucket, leaky bucket algorithms
- [ ] `SystemDesign/HLD/NotificationSystem.md` — fan-out design
- [ ] `SystemDesign/DesignPatterns/Creational/Singleton.java` — thread-safe, double-checked locking
- [ ] `SystemDesign/DesignPatterns/Creational/Builder.java`
- [ ] `SystemDesign/DesignPatterns/Behavioural/Observer.java`
- [ ] `SystemDesign/DesignPatterns/Behavioural/Strategy.java`
- [ ] `SystemDesign/DesignPatterns/Behavioural/Command.java`
- [ ] `DSA/Heap/` — Top K elements, Merge K sorted lists, K closest points

---

## Week 3 — DBMS + Frontend + DSA Hard
> Goal: SQL fluency beyond basics. JS/React core for full-stack roles. Hard DSA.

### DSA Focus This Week
Dynamic Programming (hard) · Backtracking · Dijkstra · Topological Sort · Union-Find

### Daily Deep-Dive

| Day | Topic | Repo File |
|---|---|---|
| Mon | SQL Window Functions: ROW_NUMBER, RANK, DENSE_RANK, LAG, LEAD | `DBMS/Queries/WindowFunctions.md` |
| Tue | Query Optimization + Cloud: EXPLAIN plan, indexes, CTEs + AWS/GCP services | `DBMS/Queries/Optimization.md` + `SystemDesign/HLD/Cloud.md` |
| Wed | 10 SQL Practice Problems: self-joins, GROUP BY/HAVING, subqueries + Observability | `DBMS/Queries/Problems.md` + `SystemDesign/HLD/Observability.md` |
| Thu | JavaScript Core: event loop, closures, promises, async/await, `this` context | `Frontend/JavaScript.md` |
| Fri | JavaScript Advanced + React: prototypal inheritance, hooks, reconciliation, state mgmt | `Frontend/React.md` |
| Sat | Mock: 1 hard DSA (45 min) + SQL query round (30 min) | — |
| Sun | Repo: Finalize all Week 3 files | — |

### Repo Deliverables This Week
- [ ] `Frontend/JavaScript.md` — event loop diagram, closure examples, async/await, this
- [ ] `Frontend/React.md` — hooks cheatsheet, reconciliation, controlled vs uncontrolled, state tradeoffs
- [ ] `DBMS/Queries/WindowFunctions.md` — ROW_NUMBER, RANK, LAG/LEAD with examples
- [ ] `DBMS/Queries/Optimization.md` — EXPLAIN plan, index tips, CTEs
- [ ] `DBMS/Queries/Problems.md` — 10 SQL problems with full solutions
- [ ] `SystemDesign/HLD/Cloud.md` — S3, EC2, Lambda, RDS vs DynamoDB, API Gateway
- [ ] `SystemDesign/HLD/Observability.md` — structured logging, distributed tracing, Grafana, ELK

---

## Week 4 — Behavioral + AI/LLM + Mock Rounds
> Goal: Interview-ready end-to-end. No new foundational topics. Consolidate + differentiate.

### DSA Focus This Week
Mixed simulation — pick randomly across all patterns, simulate real interview conditions

### Daily Plan

| Day | Focus | Repo File |
|---|---|---|
| Mon | Write STAR answers for 5 questions already in `Resources.txt` | `Behavioural/STAR.md` |
| Tue | Add 5 more STAR answers: leadership, failure, conflict with manager, best project, tech debt | `Behavioural/STAR.md` |
| Wed | AI/LLM: prompt engineering, RAG architecture, vector DBs, fine-tuning vs RAG | `SystemDesign/HLD/AI_LLM.md` |
| Thu | Full mock day: DSA (1 hr) + System Design (45 min) + Behavioral (30 min) back-to-back | — |
| Fri | Gap fill: revisit 3 weakest topics. Review all HLD designs out loud | — |
| Sat | Full mock interview — completely timed, no notes, treat as the real thing | — |
| Sun | Final repo polish: READMEs, commit everything, review once through | — |

### Repo Deliverables This Week
- [ ] `Behavioural/STAR.md` — 10 full STAR answers from your real work
- [ ] `SystemDesign/HLD/AI_LLM.md` — RAG, prompt engineering, vector DBs, LLM API integration
- [ ] `SystemDesign/Examples/ParkingLot/README.md` — patterns used, class diagram, edge cases
- [ ] `SystemDesign/Examples/VendingMachine/README.md` — patterns used, class diagram, edge cases
- [ ] Final pass: every folder has a README or index

---

## DSA Problem Targets

| Week | Difficulty | Daily | Cumulative | Platform |
|---|---|---|---|---|
| 1 | Easy / Easy-Medium | 2 | ~14 | NeetCode — Arrays, Strings, Binary Search |
| 2 | Medium | 2 | ~28 | NeetCode — Trees, Graphs, Heap |
| 3 | Medium / Hard | 2 | ~42 | NeetCode — DP, Backtracking, Graphs |
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
| Kth largest / Top K | Heap |
| Dependency ordering | Topological Sort |
| Prefix search / autocomplete | Trie |
| Connected components / cycle | Union-Find |

---

## Topic Priority Matrix

| Topic | Weight at Interview | Status | Week |
|---|---|---|---|
| DSA | Very High | Good — push harder | All |
| System Design HLD | Very High | Missing — build from scratch | 2 |
| Java (modern) | High | Java 8 only — extend to 17 | 1 |
| Design Patterns (LLD) | High | 2 of 23 — add 5 key ones | 2 |
| Behavioral | High (filter) | Questions only — write STAR | 4 |
| DBMS (SQL practice) | Medium-High | Theory solid — add queries | 3 |
| Frontend / React | Medium-High | Zero — build essentials | 3 |
| Deployment / DevOps | Medium | Zero — conceptual fluency | 2 |
| AI / LLM | Medium (differentiator) | Zero — conceptual fluency | 4 |
| Cloud (AWS/GCP) | Medium | Zero — conceptual fluency | 3 |
| Observability | Low-Medium | Zero — conceptual fluency | 3 |

---

## STAR Answer Template

Use this structure for every behavioral answer. Pull from **real work only**.

```
Situation: What was the context? (1-2 sentences, set the scene)
Task:       What was YOUR specific responsibility?
Action:     What did YOU do, step by step? (most important — be specific)
Result:     What was the measurable outcome? (numbers, impact, what changed)
```

---

## Repo Structure (Target — End of Week 4)

```
RoadEl/
├── ROADMAP.md                          ← You are here
├── DSA/
│   ├── HowToPrepareDSA.md              ← Pattern guide + study process (merged)
│   ├── BinarySearch/                   ← Week 1
│   ├── SlidingWindow/                  ← Week 1 (problems)
│   ├── Heap/                           ← Week 2
│   └── [all existing folders + Big-O comments]
├── SystemDesign/
│   ├── HLD/
│   │   ├── Foundations.md              ← Week 2
│   │   ├── Databases.md                ← Week 2
│   │   ├── AsyncSystems.md             ← Week 2
│   │   ├── URLShortener.md             ← Week 2
│   │   ├── RateLimiter.md              ← Week 2
│   │   ├── NotificationSystem.md       ← Week 2
│   │   ├── Cloud.md                    ← Week 3
│   │   ├── Observability.md            ← Week 3
│   │   └── AI_LLM.md                   ← Week 4
│   ├── DesignPatterns/
│   │   ├── Creational/Singleton.java   ← Week 2
│   │   ├── Creational/Builder.java     ← Week 2
│   │   └── Behavioural/               ← Week 2 (Observer, Strategy, Command)
│   └── Examples/
│       ├── ParkingLot/README.md        ← Week 4
│       └── VendingMachine/README.md    ← Week 4
├── Java/
│   └── ModernJava/                     ← Week 1 (Java 11-17)
├── Frontend/                           ← Week 3
│   ├── JavaScript.md
│   └── React.md
├── DBMS/
│   └── Queries/                        ← Week 3
│       ├── WindowFunctions.md
│       ├── Optimization.md
│       └── Problems.md
└── Behavioural/                        ← Week 4
    └── STAR.md
```

---

## Resources

| Topic | Link |
|---|---|
| DSA Problems | https://neetcode.io/practice |
| DSA Video Playlist | Already in `Resources.txt` |
| System Design | https://github.com/ashishps1/awesome-system-design-resources |
| System Design Course | https://bytebytego.com/courses/system-design-interview |
| Low Level Design | https://github.com/ashishps1/awesome-low-level-design |
| Tech Engineering Blogs | See `Resources.txt` for 33 blogs |
