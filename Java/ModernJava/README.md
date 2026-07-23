# Java — Modern Depth (Week 1 Deep-Dive)

Files in this folder cover Java internals and modern features that interviewers at product companies actually probe for 3 YOE candidates.

## Index

| File | Topic | Key Interview Areas |
|---|---|---|
| [MemoryModel.md](./MemoryModel.md) | Heap, Stack, GC, Reference types | OutOfMemoryError, GC algorithms, weak/soft refs |
| [Collections.md](./Collections.md) | HashMap internals, variants, PriorityQueue | HashMap collision, ConcurrentHashMap, LRU cache |
| [Concurrency.md](./Concurrency.md) | volatile, AtomicInteger, ExecutorService, CompletableFuture | Race conditions, thread pool sizing, async pipelines |
| [Java11to17.md](./Java11to17.md) | var, records, sealed classes, switch expressions, text blocks | "What's new in modern Java?" |
| [Streams.md](./Streams.md) | flatMap, groupingBy, reduce, parallel streams | Stream pipeline, lazy evaluation, collectors |

## Study Order
1. `MemoryModel.md` — foundational, everything builds on this
2. `Collections.md` — most frequently asked in interviews
3. `Concurrency.md` — critical for backend roles
4. `Streams.md` — used in every coding round
5. `Java11to17.md` — differentiator, shows you keep up

## Existing Java Coverage
The main `Java/Java.md` already covers Java 8 fundamentals:
- OOP (inheritance, polymorphism, interfaces)
- Primitives, variables, scope
- final / finally / finalize
- Stream API basics
- Lambda expressions and functional interfaces
- Exception handling → `Java/Exception/`
- Multithreading basics → `Java/MultiThreading/`
- Synchronization → `Java/Synchronization/`
