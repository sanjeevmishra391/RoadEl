# Java Concurrency — Modern Approach

## The Problem: Why Concurrency is Hard
```
Thread A reads x = 0
Thread B reads x = 0
Thread A writes x = 1
Thread B writes x = 1   ← Thread A's update is lost
Final x = 1  (expected: 2)
```
This is a **race condition** — result depends on thread scheduling.

---

## volatile — Visibility, Not Atomicity

```java
// Without volatile: thread may read stale cached value
// With volatile: always reads from main memory
private volatile boolean running = true;

// Thread 1
while (running) { doWork(); }

// Thread 2
running = false;  // immediately visible to Thread 1
```

**volatile guarantees:**
- Visibility: writes visible to all threads immediately
- Ordering: prevents instruction reordering around the variable

**volatile does NOT guarantee:**
- Atomicity: `volatile int x; x++` is still NOT atomic (read-modify-write = 3 steps)

---

## Atomic Classes — Lock-Free Thread Safety

```java
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();          // atomic i++
counter.compareAndSet(expected, newVal);  // CAS operation

AtomicLong, AtomicBoolean, AtomicReference<T>
// All use hardware CAS (Compare-And-Swap) — faster than synchronized
```

---

## ExecutorService — Thread Pool Management

```java
// Fixed pool — cap concurrent threads
ExecutorService executor = Executors.newFixedThreadPool(4);

// Cached pool — creates threads on demand, reuses idle ones
ExecutorService executor = Executors.newCachedThreadPool();

// Single thread — sequential execution, no concurrency
ExecutorService executor = Executors.newSingleThreadExecutor();

// Scheduled — for periodic/delayed tasks
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

// Submit tasks
executor.execute(() -> doWork());            // fire and forget (Runnable)
Future<String> future = executor.submit(() -> "result");  // Callable with result

// Always shutdown when done
executor.shutdown();               // graceful — waits for tasks to finish
executor.shutdownNow();            // forceful — interrupts running tasks
```

**Thread pool sizing rule of thumb:**
- CPU-bound tasks: `Runtime.getRuntime().availableProcessors()`
- I/O-bound tasks: `availableProcessors * (1 + wait_time / service_time)`

---

## CompletableFuture — Async Pipelines (Java 8+)

```java
// Basic async execution
CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
    return fetchDataFromDB();   // runs in ForkJoinPool
});

// Chain transformations (non-blocking)
cf.thenApply(data -> process(data))        // transform result
  .thenAccept(result -> save(result))      // consume result, no return
  .thenRun(() -> log("done"))             // run after, no input/output
  .exceptionally(ex -> handleError(ex));  // handle exception

// Combine two futures
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "World");

f1.thenCombine(f2, (s1, s2) -> s1 + " " + s2)
  .thenAccept(System.out::println);  // Hello World

// Wait for all
CompletableFuture.allOf(f1, f2, f3).join();

// Wait for first
CompletableFuture.anyOf(f1, f2, f3).thenAccept(result -> use(result));

// Run with custom executor (avoid blocking ForkJoinPool)
CompletableFuture.supplyAsync(() -> fetchFromDB(), ioExecutor);
```

**thenApply vs thenCompose:**
```java
// thenApply: transform (like map) — wraps result in new CF
cf.thenApply(x -> x.toUpperCase())  // CF<String>

// thenCompose: flatten (like flatMap) — when transform returns CF
cf.thenCompose(x -> fetchAnotherAsync(x))  // CF<Result>, not CF<CF<Result>>
```

---

## synchronized vs Lock vs ReentrantLock

```java
// synchronized method — lock on 'this'
public synchronized void increment() { count++; }

// synchronized block — lock on specific object
synchronized (lockObject) { count++; }

// ReentrantLock — more control
ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    count++;
} finally {
    lock.unlock();  // ALWAYS unlock in finally
}

// tryLock — non-blocking attempt
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    try { count++; } finally { lock.unlock(); }
}
```

| | synchronized | ReentrantLock |
|---|---|---|
| Syntax | Simpler | Verbose |
| Fairness | No guarantee | Can set fair=true |
| Try-lock | Not possible | `tryLock()` |
| Interruptible | No | `lockInterruptibly()` |
| Condition | wait/notify | Condition objects |
| Default choice | Yes | Only when you need extra features |

---

## Common Concurrency Problems

### Deadlock
```java
// Thread 1 holds lockA, wants lockB
// Thread 2 holds lockB, wants lockA → deadlock
// Prevention: always acquire locks in the same order
```

### Race Condition
Two threads read-modify-write shared state without synchronization. Fix with `synchronized`, `AtomicInteger`, or `volatile`.

### Thread Starvation
Low-priority threads never get CPU time. Fix with fair locks or equal priorities.

---

## Common Interview Questions

**Q: volatile vs synchronized — when to use which?**
A: Use `volatile` when one thread writes, others only read (visibility only). Use `synchronized` when multiple threads read AND write (both visibility and atomicity needed).

**Q: What is the happens-before relationship?**
A: A guarantee that memory writes by one operation are visible to another. Established by: synchronized blocks, volatile writes/reads, thread start/join, and CompletableFuture chains.

**Q: What's the difference between `submit()` and `execute()` in ExecutorService?**
A: `execute()` takes a Runnable, no return value, exceptions are swallowed. `submit()` takes Runnable or Callable, returns a Future — exceptions are captured in the Future.

**Q: Why should you avoid creating threads directly (new Thread()) in production?**
A: Thread creation is expensive (OS-level resource). Unbounded thread creation can exhaust OS limits. Thread pools (ExecutorService) reuse threads and control concurrency.

**Q: What is CAS and why is it preferred over locking?**
A: Compare-And-Swap is a hardware instruction: "set value to new only if current value equals expected". It's non-blocking — no thread is ever suspended waiting for a lock, reducing context switches and deadlock risk.
