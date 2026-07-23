# Start Here — How to Use This Repo

This repo is a 4-week interview preparation guide. Every file has a purpose. Follow this flow and you won't miss anything.

---

## Step 0 — Read These First (Day 1, 30 min)

Before opening any topic file, read these two docs. They tell you **how to study**, not what to study.

1. **[ROADMAP.md](./ROADMAP.md)** — Your 4-week daily plan. Open this every morning. Check off deliverables as you complete them.
2. **[DSA/HowToPrepareDSA.md](./DSA/HowToPrepareDSA.md)** — The problem-solving process. The 5-step method, the 5-minute stuck rule, the mock interview protocol, and anti-patterns to avoid.

---

## Week 1 — Java Depth + DSA Foundations

### Java (read in this order)

| # | File | What You Learn |
|---|---|---|
| 1 | [Java/Java.md](./Java/Java.md) | OOP, primitives, constructors, overriding, Java 8 features — baseline |
| 2 | [Java/ModernJava/MemoryModel.md](./Java/ModernJava/MemoryModel.md) | Heap vs stack, GC algorithms, 4 reference types |
| 3 | [Java/ModernJava/Collections.md](./Java/ModernJava/Collections.md) | HashMap internals, all Map/List variants, ConcurrentHashMap |
| 4 | [Java/ModernJava/Concurrency.md](./Java/ModernJava/Concurrency.md) | volatile, AtomicInteger, ExecutorService, CompletableFuture |
| 5 | [Java/ModernJava/Streams.md](./Java/ModernJava/Streams.md) | flatMap, groupingBy, reduce, parallel streams |
| 6 | [Java/ModernJava/Java11to17.md](./Java/ModernJava/Java11to17.md) | var, records, sealed classes, switch expressions, text blocks |
| 7 | [Java/MultiThreading/MultiThreading.md](./Java/MultiThreading/MultiThreading.md) | Thread lifecycle, thread pool, Callable/Future |
| 8 | [Java/Synchronization/_Synchronization.md](./Java/Synchronization/_Synchronization.md) | synchronized, wait/notify, deadlock, inter-thread communication |
| 9 | [Java/Exception/Exception.md](./Java/Exception/Exception.md) | Checked vs unchecked, custom exceptions, try-with-resources |

### DSA — Week 1 Topics

**Start with the pattern guide, consult it before every problem:**

| # | File | What You Learn |
|---|---|---|
| 1 | [DSA/HowToPrepareDSA.md](./DSA/HowToPrepareDSA.md) | Master trigger table, 3-Read method, complexity limits, weekly problem plan |
| 2 | [DSA/Arrays/Arrays.md](./DSA/Arrays/Arrays.md) | Prefix sum, Kadane's, Dutch flag, cyclic sort, Boyer-Moore, matrix rotation |
| 3 | [DSA/Algorithms/TwoPointers.md](./DSA/Algorithms/TwoPointers.md) | 3 sub-pattern templates (opposite ends, fast/slow, two arrays) |
| 4 | [DSA/TwoPointer/TwoPointers.md](./DSA/TwoPointer/TwoPointers.md) | 10 problems with full solutions (Two Sum II, 3Sum, 4Sum, Move Zeroes, Dutch Flag…) |
| 5 | [DSA/Algorithms/SlidingWindow.md](./DSA/Algorithms/SlidingWindow.md) | Fixed vs variable window templates |
| 6 | [DSA/SlidingWindow/SlidingWindow.md](./DSA/SlidingWindow/SlidingWindow.md) | 7 problems with full solutions (Min Window Substring, Sliding Window Max…) |
| 7 | [DSA/Searching/Searching.md](./DSA/Searching/Searching.md) | Linear, binary, jump, rotated array, 2D matrix, binary search on answer |
| 8 | [DSA/BinarySearch/BinarySearch.md](./DSA/BinarySearch/BinarySearch.md) | 3 templates + 9 problems (Koko, Ship Packages, Split Array…) |
| 9 | [DSA/Sorting/Sorting.md](./DSA/Sorting/Sorting.md) | All 8 algorithms with code, Quick Select, decision guide |
| 10 | [DSA/Algorithms/Kadane.md](./DSA/Algorithms/Kadane.md) | Maximum subarray, sliding window variant |
| 11 | [DSA/Bit_Manipulation/BitManipulation.md](./DSA/Bit_Manipulation/BitManipulation.md) | Bit tricks cheat sheet, XOR patterns, subset generation |

---

## Week 2 — System Design + DSA Medium

### System Design

| # | File | What You Learn |
|---|---|---|
| 1 | [SystemDesign/SystemDesign.md](./SystemDesign/SystemDesign.md) | SOLID principles, DRY principle — LLD foundation |
| 2 | [SystemDesign/DesignPatterns/DesignPattern.md](./SystemDesign/DesignPatterns/DesignPattern.md) | Overview of all 23 patterns across 3 groups |
| 3 | [SystemDesign/DesignPatterns/Creational/FactoryMethod.md](./SystemDesign/DesignPatterns/Creational/FactoryMethod.md) | Factory Method pattern |
| 4 | [SystemDesign/DesignPatterns/Creational/AbstractFactoryPattern.md](./SystemDesign/DesignPatterns/Creational/AbstractFactoryPattern.md) | Abstract Factory pattern |
| 5 | [SystemDesign/DesignPatterns/Structure/Decorators/Decorators.md](./SystemDesign/DesignPatterns/Structure/Decorators/Decorators.md) | Decorator pattern with Coffee example |

> **To create this week** (per ROADMAP.md):
> `SystemDesign/HLD/Foundations.md` · `Databases.md` · `AsyncSystems.md` · `URLShortener.md` · `RateLimiter.md` · `NotificationSystem.md`
> `SystemDesign/DesignPatterns/Creational/Singleton.java` · `Builder.java`
> `SystemDesign/DesignPatterns/Behavioural/Observer.java` · `Strategy.java` · `Command.java`

### LLD Examples (study these alongside design patterns)

| File | Patterns Used |
|---|---|
| [SystemDesign/Examples/ParkingLot/](./SystemDesign/Examples/ParkingLot/) | Strategy, State, Factory |
| [SystemDesign/Examples/VendingMachine/](./SystemDesign/Examples/VendingMachine/) | State pattern (full state machine) |

### DSA — Week 2 Topics

| # | File | What You Learn |
|---|---|---|
| 1 | [DSA/Tree/Tree.md](./DSA/Tree/Tree.md) | DFS/BFS templates, 10 key problems, complexity cheat sheet |
| 2 | [DSA/Graph/Graph.md](./DSA/Graph/Graph.md) | BFS/DFS templates, Dijkstra, Topological Sort, Union-Find, MST |
| 3 | [DSA/Stack/Stack.md](./DSA/Stack/Stack.md) | Monotonic stack (increasing + decreasing), key problems |
| 4 | [DSA/Sorting/Sorting.md](./DSA/Sorting/Sorting.md) | All 8 algorithms compared, custom comparator, Java sort gotchas |

> **To create this week:** `DSA/Heap/` — Top K, Merge K sorted lists, K closest points

---

## Week 3 — DBMS + Frontend + DSA Hard

### DBMS

| # | File | What You Learn |
|---|---|---|
| 1 | [DBMS/DBMS.md](./DBMS/DBMS.md) | Keys, joins, normalization, ACID, CAP, indexing, SQL commands, NoSQL types |

> **To create this week:**
> `DBMS/Queries/WindowFunctions.md` · `Optimization.md` · `Problems.md`

### Frontend

> **To create this week:**
> `Frontend/JavaScript.md` · `Frontend/React.md`

### DSA — Week 3 Topics

| # | File | What You Learn |
|---|---|---|
| 1 | [DSA/Dynamic/DP.md](./DSA/Dynamic/DP.md) | 5-step framework, 1D/2D/Knapsack templates, problems by category |
| 2 | [DSA/Trie/Trie.md](./DSA/Trie/Trie.md) | Full implementation, key problems, common mistakes |
| 3 | [DSA/String/String.md](./DSA/String/String.md) | Frequency map, KMP, Rabin-Karp, key problems |

---

## Week 4 — Behavioral + AI/LLM + Final Polish

> **To create this week:**
> `Behavioural/STAR.md` · `SystemDesign/HLD/AI_LLM.md`
> `SystemDesign/Examples/ParkingLot/README.md` · `VendingMachine/README.md`

### Review Order for Week 4

Go through files in this order for your final revision pass:

1. [DSA/HowToPrepareDSA.md](./DSA/HowToPrepareDSA.md) — trigger table + complexity reference
2. [DSA/BinarySearch/BinarySearch.md](./DSA/BinarySearch/BinarySearch.md) — templates
3. [DSA/TwoPointer/TwoPointers.md](./DSA/TwoPointer/TwoPointers.md) — problems
4. [DSA/SlidingWindow/SlidingWindow.md](./DSA/SlidingWindow/SlidingWindow.md) — problems
5. [DSA/Tree/Tree.md](./DSA/Tree/Tree.md) — templates
6. [DSA/Graph/Graph.md](./DSA/Graph/Graph.md) — algorithms
7. [DSA/Dynamic/DP.md](./DSA/Dynamic/DP.md) — framework
8. [Java/ModernJava/Collections.md](./Java/ModernJava/Collections.md) — HashMap internals
9. [Java/ModernJava/Concurrency.md](./Java/ModernJava/Concurrency.md) — volatile, CompletableFuture
10. [DBMS/DBMS.md](./DBMS/DBMS.md) — ACID, CAP, indexing
11. [SystemDesign/SystemDesign.md](./SystemDesign/SystemDesign.md) — SOLID

---

## File Type Reference

Understanding the two types of files helps you use them correctly:

| Type | Purpose | How to Use |
|---|---|---|
| **Theory file** | Templates, sub-patterns, complexity | Read once, consult when stuck |
| **Problems file** | Full solutions with code + key insight | Solve the problem first, then compare |

| Theory | Problems |
|---|---|
| `Algorithms/TwoPointers.md` | `TwoPointer/TwoPointers.md` |
| `Algorithms/SlidingWindow.md` | `SlidingWindow/SlidingWindow.md` |
| `BinarySearch/BinarySearch.md` | (templates + problems in same file) |
| `Tree/Tree.md` | (templates + problems in same file) |
| `Graph/Graph.md` | (templates + problems in same file) |
| `Dynamic/DP.md` | (templates + problems in same file) |

---

## Complete File Map

```
RoadEl/
│
├── START_HERE.md              ← You are here
├── ROADMAP.md                 ← Daily plan, weekly deliverables, checkboxes
├── Resources.txt              ← External links (NeetCode, ByteByteGo, blogs)
│
├── DSA/
│   ├── DSA.md                 ← DSA index (all topics + problem sets)
│   ├── HowToPrepareDSA.md     ← Pattern trigger table + study process (merged)
│   ├── HowToPrepareDSA.md     ← Study process, daily routine, mock protocol
│   │
│   ├── Arrays/Arrays.md            ← Prefix sum, Kadane's, Dutch flag, matrix
│   ├── Algorithms/
│   │   ├── TwoPointers.md          ← Templates (theory)
│   │   ├── SlidingWindow.md        ← Templates (theory)
│   │   └── Kadane.md               ← Max subarray
│   ├── TwoPointer/TwoPointers.md   ← 10 problems with solutions
│   ├── SlidingWindow/SlidingWindow.md  ← 7 problems with solutions
│   ├── BinarySearch/BinarySearch.md    ← 3 templates + 9 problems
│   │
│   ├── Tree/Tree.md            ← DFS/BFS templates + 10 problems
│   ├── Graph/Graph.md          ← All graph algorithms + problems
│   ├── Stack/Stack.md          ← Monotonic stack + problems
│   ├── Sorting/Sorting.md      ← All 8 algorithms compared
│   ├── Dynamic/DP.md           ← 5-step framework + problems
│   ├── Trie/Trie.md            ← Implementation + problems
│   ├── String/String.md        ← Techniques + KMP + problems
│   ├── Bit_Manipulation/BitManipulation.md  ← Bit tricks + problems
│   └── LinkedList/             ← LRU, reverse, merge (existing Java files)
│
├── Java/
│   ├── Java.md                 ← Java 8 fundamentals (OOP, primitives, streams)
│   ├── ModernJava/
│   │   ├── README.md           ← Index + study order
│   │   ├── MemoryModel.md      ← Heap/stack/GC
│   │   ├── Collections.md      ← HashMap internals, variants
│   │   ├── Concurrency.md      ← volatile, ExecutorService, CompletableFuture
│   │   ├── Streams.md          ← Advanced collectors, flatMap, parallel
│   │   └── Java11to17.md       ← var, records, sealed, switch expressions
│   ├── MultiThreading/MultiThreading.md   ← Thread lifecycle, thread pool
│   ├── Synchronization/_Synchronization.md  ← synchronized, wait/notify
│   └── Exception/Exception.md  ← Exception hierarchy
│
├── SystemDesign/
│   ├── SystemDesign.md         ← SOLID principles, DRY
│   ├── DesignPatterns/
│   │   ├── DesignPattern.md    ← Overview of all 23 patterns
│   │   ├── Creational/         ← Factory, Abstract Factory (+ Singleton, Builder to add)
│   │   └── Structure/Decorators/  ← Decorator pattern
│   ├── Examples/
│   │   ├── ParkingLot/         ← Full LLD implementation
│   │   └── VendingMachine/     ← State machine LLD
│   └── HLD/                    ← (to create in Week 2-3)
│
├── DBMS/
│   ├── DBMS.md                 ← Full theory: keys, joins, ACID, CAP, indexing
│   └── Queries/                ← (to create in Week 3)
│
├── Frontend/                   ← (to create in Week 3)
│   ├── JavaScript.md
│   └── React.md
│
└── Behavioural/                ← (to create in Week 4)
    └── STAR.md
```

---

## Quick Decision Guide

**"I don't know which pattern to use"** → [HowToPrepareDSA.md](./DSA/HowToPrepareDSA.md)

**"I know the pattern but not the template"** → `DSA/Algorithms/<pattern>.md` or the topic `.md` file

**"I want to see a solved problem"** → `DSA/<Topic>/<Topic>.md` problems file

**"I need to review Java internals"** → [Java/ModernJava/](./Java/ModernJava/README.md)

**"I need to prep for a system design round"** → [SystemDesign/SystemDesign.md](./SystemDesign/SystemDesign.md) → then HLD files

**"What should I do today?"** → [ROADMAP.md](./ROADMAP.md)
