# Java Interview Preparation

A complete knowledge base for Java interviews at product companies (3–5 YOE level). Everything is organized by topic — follow the learning path below to build from fundamentals to advanced internals.

---

## Learning Path

Work through phases in order. Each phase builds on the previous one.

### Phase 1 — Language Foundations
Start here if you need to solidify the basics or if it's been a while.

| File | What You'll Learn |
|---|---|
| [Java.md](./Java.md) | Primitives, variables, OOP pillars, constructors, packages, scope |
| [String/String.md](./String/String.md) | String immutability, pool, intern(), StringBuilder, all key methods |
| [Exception/Exception.md](./Exception/Exception.md) | Checked vs unchecked, try-with-resources, chaining, custom exceptions |
| [InnerClass/InnerClass.md](./InnerClass/InnerClass.md) | Member, anonymous, local, static nested classes |
| [OperatorsAndStatements/OperatorsAndStatements.md](./OperatorsAndStatements/OperatorsAndStatements.md) | Operators, control flow, autoboxing, numeric promotion |

**Interview questions you should be able to answer after Phase 1:**

*OOP*
- What are the four OOP principles? Give a code example of each.
- When would you use an interface vs an abstract class?
- What is the difference between method overloading and method overriding?
- What is the equals/hashCode contract? What breaks if you only override `equals`?
- What does `final` do when applied to a class, method, and variable?
- Explain inheritance vs composition. Why "favor composition over inheritance"?
- What is the difference between `Comparable` and `Comparator`?

*Strings*
- Why is String immutable? Name at least 3 reasons.
- What is the String pool? What's the difference between `"hello"` and `new String("hello")`?
- Why should you use `equals()` and not `==` to compare strings?
- When would you use `StringBuilder` instead of `+` concatenation?
- What is the difference between `StringBuilder` and `StringBuffer`?
- What does `String.intern()` do?

*Exceptions*
- What is the difference between checked and unchecked exceptions?
- What is `try-with-resources`? What interface must a resource implement?
- When would you create a checked exception vs an unchecked exception?
- What happens if an exception is thrown inside a `finally` block?
- What is exception chaining and why is swallowing exceptions bad?
- Can you have `try` without `catch`? Without `finally`?

*Inner Classes*
- What is the difference between a static nested class and an inner class?
- Why did lambdas replace most anonymous inner class usage in Java 8?
- Why can a local inner class only access effectively final variables?
- What memory leak risk do non-static inner classes introduce?

*Operators and Types*
- What is the Integer cache? What does `Integer a = 127; Integer b = 127; a == b` return?
- What is autoboxing? When can it cause a NullPointerException?
- What is short-circuit evaluation? Give an example where it prevents a NullPointerException.
- What is numeric promotion? Why does `short a = 1; short b = 2; short c = a + b;` not compile?
- What is the output of `"1" + 2 + 3` vs `1 + 2 + "3"`?

---

### Phase 2 — Core Internals (Most Interview-Tested)
This is what interviewers probe most. Spend the most time here.

| File | What You'll Learn |
|---|---|
| [ModernJava/MemoryModel.md](./ModernJava/MemoryModel.md) | Stack, Heap, Metaspace, GC algorithms, reference types |
| [Generics/Generics.md](./Generics/Generics.md) | Type erasure, wildcards, PECS rule, invariance |
| [ModernJava/Collections.md](./ModernJava/Collections.md) | HashMap internals, variants, PriorityQueue, fail-fast/fail-safe |
| [ModernJava/Streams.md](./ModernJava/Streams.md) | Pipeline, lazy evaluation, flatMap, groupingBy, reduce, collectors |

**Interview questions you should be able to answer after Phase 2:**

*Memory Model*
- Where do local variables, object fields, and static variables live in memory?
- What is the difference between stack and heap?
- What is Metaspace? How does it differ from PermGen?
- Walk through the GC lifecycle: Eden → Survivor → Old Gen. Why do objects bounce between S0 and S1?
- What is the difference between Minor GC and Full GC?
- How does G1 GC work? Why is it preferred for large heaps?
- What causes a memory leak in Java despite GC? Name 3 patterns.
- How would you reduce GC-related request latency?
- What are the 4 reference types? When would you use SoftReference vs WeakReference?

*Generics*
- What is type erasure? What does it prevent you from doing?
- Why is `List<Dog>` not a `List<Animal>`? How would you fix it?
- What is PECS? Give an example.
- Can you overload methods that differ only by generic type parameter?
- What's the difference between `List<?>` and `List<Object>`?

*Collections*
- Walk through `HashMap.put("key", value)` step by step.
- What is a hash collision? How does HashMap resolve it?
- Why must HashMap capacity be a power of 2?
- What is the load factor and why is 0.75 the default?
- What is treeification and when does it happen?
- How would you implement a frequency counter map?
- When would you use `computeIfAbsent`? `merge`? `getOrDefault`?
- What's the difference between ConcurrentHashMap and synchronizedMap?
- How would you implement an LRU cache using LinkedHashMap?
- What is a fail-fast iterator? How do you safely remove during iteration?
- When would you use TreeMap over HashMap?

*Streams*
- Explain lazy evaluation in streams. How does `findFirst` benefit from it?
- What's the difference between `map` and `flatMap`?
- What does `groupingBy` return? How would you count elements per group?
- When would you NOT use parallel streams?
- What is `Optional`? What are the correct and incorrect ways to use it?
- What's the difference between `reduce` and `collect`?

---

### Phase 3 — Concurrency (Critical for Backend Roles)
Most commonly a dedicated screen topic at senior levels.

| File | What You'll Learn |
|---|---|
| [MultiThreading/MultiThreading.md](./MultiThreading/MultiThreading.md) | Thread lifecycle, sleep/join, daemon threads, Callable/Future |
| [Synchronization/_Synchronization.md](./Synchronization/_Synchronization.md) | synchronized, wait/notify, deadlock, inter-thread communication |
| [ModernJava/Concurrency.md](./ModernJava/Concurrency.md) | volatile, AtomicInteger/CAS, happens-before, ExecutorService, CompletableFuture, all coordination tools |
| [ModernJava/ConcurrencyPractice.md](./ModernJava/ConcurrencyPractice.md) | 12 coding problems with solutions — practice before interviews |

**How to learn this section:**
1. Read `MultiThreading.md` for the Java thread model (NEW → RUNNABLE → BLOCKED → TERMINATED)
2. Read `Synchronization` for the low-level locking primitives (synchronized, wait/notify)
3. Read `Concurrency.md` — the full modern toolkit with deep explanations
4. **Practice**: work through `ConcurrencyPractice.md` — write each solution yourself before reading the answer

**The 3 core problems and what solves them:**
- **Visibility** (stale reads) → `volatile`
- **Atomicity** (lost updates) → `AtomicInteger` / `synchronized`
- **Coordination** (ordering between threads) → `CountDownLatch`, `CyclicBarrier`, `BlockingQueue`, `CompletableFuture`

**Interview questions you should be able to answer after Phase 3:**

*Fundamentals*
- What is a race condition? Give a concrete example.
- What is the difference between `volatile` and `synchronized`?
- Why is `x++` not atomic even with `volatile`?
- What is happens-before? Give 3 examples of what establishes it.
- What is CAS? Why is it faster than locking under low contention?

*Tools*
- What is the difference between `execute()` and `submit()` in ExecutorService?
- How do you size a thread pool for CPU-bound vs I/O-bound work?
- What is the difference between `thenApply` and `thenCompose` in CompletableFuture?
- When would you use CompletableFuture over a thread pool with Future.get()?
- What is the difference between CountDownLatch and CyclicBarrier?
- When would you use a Semaphore?
- What is ThreadLocal? What is the memory leak risk in thread pools?
- When would you use ReadWriteLock over synchronized?

*Problems*
- What is a deadlock? What are the 4 conditions? How do you prevent it?
- What is thread starvation? What causes livelock?
- How do you stop a thread cleanly?
- Why use `while` instead of `if` before `await()` / `wait()`?

*Coding* (write these from scratch)
- Thread-safe counter (3 ways: volatile/AtomicInteger/synchronized — explain tradeoffs)
- Producer-consumer with BlockingQueue
- Run N tasks in parallel, collect all results with CompletableFuture
- Thread-safe singleton with double-checked locking
- Bounded blocking stack with ReentrantLock + Condition

---

### Phase 4 — Modern Java Features (Differentiator)
Shows you keep up with the language. Usually asked as "what's new in modern Java?"

| File | What You'll Learn |
|---|---|
| [ModernJava/Java11to17.md](./ModernJava/Java11to17.md) | var, records, sealed classes, switch expressions, text blocks, pattern matching |

**Key concepts after Phase 4:**
- `var` is compile-time type inference, not dynamic typing
- Records: immutable data carriers, auto-generated equals/hashCode/toString
- Sealed classes: closed hierarchies for exhaustive pattern matching
- Switch expressions: arrow syntax, yield, no fall-through

---

## Topic Index

Quick lookup by interview topic:

| Interview Topic | Where to Read |
|---|---|
| OOP principles | [Java.md — OOP section](./Java.md) |
| Interface vs Abstract Class | [Java.md — OOP section](./Java.md) |
| equals() / hashCode() contract | [Java.md — OOP section](./Java.md) |
| Comparable vs Comparator | [Java.md — OOP section](./Java.md) |
| String immutability / pool | [String/String.md](./String/String.md) |
| Exception hierarchy / handling | [Exception/Exception.md](./Exception/Exception.md) |
| try-with-resources | [Exception/Exception.md](./Exception/Exception.md) |
| Generics / type erasure / PECS | [Generics/Generics.md](./Generics/Generics.md) |
| JVM memory (stack/heap/metaspace) | [ModernJava/MemoryModel.md](./ModernJava/MemoryModel.md) |
| Garbage collection | [ModernJava/MemoryModel.md](./ModernJava/MemoryModel.md) |
| HashMap internals | [ModernJava/Collections.md](./ModernJava/Collections.md) |
| ConcurrentHashMap | [ModernJava/Collections.md](./ModernJava/Collections.md) |
| LRU cache implementation | [ModernJava/Collections.md](./ModernJava/Collections.md) |
| Fail-fast vs fail-safe iterators | [ModernJava/Collections.md](./ModernJava/Collections.md) |
| Stream API | [ModernJava/Streams.md](./ModernJava/Streams.md) |
| Thread lifecycle | [MultiThreading/MultiThreading.md](./MultiThreading/MultiThreading.md) |
| volatile / synchronized | [ModernJava/Concurrency.md](./ModernJava/Concurrency.md) |
| ExecutorService / thread pools | [ModernJava/Concurrency.md](./ModernJava/Concurrency.md) |
| CompletableFuture | [ModernJava/Concurrency.md](./ModernJava/Concurrency.md) |
| Deadlock | [Synchronization/_Synchronization.md](./Synchronization/_Synchronization.md) + [ModernJava/Concurrency.md](./ModernJava/Concurrency.md) |
| CountDownLatch / Semaphore | [ModernJava/Concurrency.md](./ModernJava/Concurrency.md) |
| ThreadLocal | [ModernJava/Concurrency.md](./ModernJava/Concurrency.md) |
| Concurrency coding practice | [ModernJava/ConcurrencyPractice.md](./ModernJava/ConcurrencyPractice.md) |
| Records / var / sealed classes | [ModernJava/Java11to17.md](./ModernJava/Java11to17.md) |
