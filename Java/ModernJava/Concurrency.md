# Java Concurrency — Modern Approach

---

## Why Concurrency is Hard — The Core Problem

Every concurrency bug comes from one of three things:

| Problem | What happens | Fix |
|---|---|---|
| **Visibility** | Thread A writes a value; Thread B never sees it (cached in CPU register) | `volatile`, `synchronized` |
| **Atomicity** | Read-modify-write happens in 3 steps; another thread sneaks in between | `synchronized`, `AtomicInteger` |
| **Ordering** | JVM/CPU reorders instructions for performance; another thread sees wrong order | `volatile`, `synchronized`, `happens-before` |

```
Thread A reads x = 0        ← reads stale value from CPU cache (visibility)
Thread B reads x = 0
Thread A writes x = 1       ← write-modify-write is 3 steps (atomicity)
Thread B writes x = 1       ← Thread A's update is lost
Final x = 1  (expected: 2)
```

---

## volatile — Visibility, Not Atomicity

```java
private volatile boolean running = true;

// Thread 1
while (running) { doWork(); }   // always reads from main memory, not CPU cache

// Thread 2
running = false;   // write immediately flushed to main memory, visible to Thread 1
```

**volatile guarantees:**
- **Visibility**: write by any thread immediately visible to all others
- **Ordering**: prevents instruction reordering *across* the variable — everything before the volatile write happens-before everything after the volatile read

**volatile does NOT guarantee atomicity:**
```java
volatile int x = 0;
x++;   // NOT atomic — this is: read x, add 1, write x (3 steps)
       // two threads can both read 0, both compute 1, both write 1 → lost update
```

**When to use volatile (and when not to):**
```java
// CORRECT: one thread writes, others only read — visibility is enough
volatile boolean shutdownRequested = false;

// WRONG: multiple threads write — need atomicity too
volatile int counter = 0;
counter++;   // race condition even with volatile
// Fix: use AtomicInteger or synchronized
```

---

## Atomic Classes — Lock-Free Thread Safety

```java
AtomicInteger counter = new AtomicInteger(0);

counter.get()                          // read
counter.incrementAndGet()              // atomic ++i, returns new value
counter.getAndIncrement()              // atomic i++, returns old value
counter.addAndGet(5)                   // atomic += 5
counter.compareAndSet(expected, next)  // CAS: set to next only if current == expected

AtomicLong, AtomicBoolean, AtomicReference<T>
```

### How CAS works (Compare-And-Swap)

CAS is a single hardware instruction: "set this memory location to `newValue` only if it currently equals `expected`."

```
Thread A: CAS(x, 0, 1)   reads 0, sets to 1 — SUCCESS
Thread B: CAS(x, 0, 1)   reads 1 (already changed), does NOT set — FAILS → retries
```

This is **lock-free** — no thread is ever blocked waiting. Threads spin and retry instead of sleeping. Faster than `synchronized` under low contention because there's no context switch.

**ABA problem:** CAS sees value `A`, another thread changes it to `B` then back to `A` — the CAS succeeds even though the value changed. Fix: `AtomicStampedReference` (carries a version stamp alongside the value).

---

## synchronized — Mutual Exclusion + Visibility

```java
// synchronized method — intrinsic lock on 'this' object
public synchronized void increment() {
    count++;   // only one thread at a time; count changes visible after release
}

// synchronized block — lock on specific object (more granular)
private final Object lock = new Object();

public void increment() {
    synchronized (lock) {
        count++;
    }
}

// static synchronized — lock on the Class object (shared across all instances)
public static synchronized void classWideOp() { }
```

**What synchronized actually does:**
1. Acquires the intrinsic lock (monitor) on the object
2. Establishes a **happens-before** relationship — all writes before `unlock` are visible to any thread that subsequently `lock`s the same monitor
3. Only one thread holds the lock at a time — others block

**synchronized is reentrant** — if a thread already holds a lock, it can re-enter synchronized blocks on the same object without deadlocking (counts re-entries).

---

## happens-before — The Memory Model Contract

The Java Memory Model defines when one thread's writes are guaranteed visible to another thread's reads. The key `happens-before` rules:

| Action | What it establishes |
|---|---|
| Unlock of monitor M | happens-before every subsequent lock of M |
| Write to `volatile` field | happens-before every subsequent read of that field |
| `Thread.start()` | happens-before any action in the started thread |
| All actions in thread T | happens-before `T.join()` returns |
| Constructor completes | happens-before finalizer begins |

```java
int x = 0;
volatile boolean flag = false;

// Thread A:
x = 42;         // (1)
flag = true;    // (2) volatile write — happens-before any subsequent flag read

// Thread B:
while (!flag) {} // (3) volatile read — once flag is true...
System.out.println(x);  // (4) guaranteed to see x = 42
                         // because (1) happens-before (2), (2) h-b (3), (3) h-b (4)
```

Without the volatile, Thread B might print `0` — x write isn't guaranteed visible.

---

## ReentrantLock — Explicit Lock with More Control

```java
ReentrantLock lock = new ReentrantLock();

lock.lock();
try {
    count++;
} finally {
    lock.unlock();   // ALWAYS in finally — never omit
}

// tryLock — attempt without blocking (deadlock avoidance)
if (lock.tryLock()) {
    try { count++; } finally { lock.unlock(); }
} else {
    // do something else / retry later
}

// tryLock with timeout
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    try { count++; } finally { lock.unlock(); }
}

// Interruptible lock — throws InterruptedException if thread interrupted while waiting
lock.lockInterruptibly();
```

**Condition variables — replacing wait/notify:**
```java
ReentrantLock lock = new ReentrantLock();
Condition notEmpty = lock.newCondition();
Condition notFull  = lock.newCondition();

// Producer
lock.lock();
try {
    while (buffer.isFull()) notFull.await();   // releases lock, waits
    buffer.add(item);
    notEmpty.signal();   // wake one consumer
} finally { lock.unlock(); }

// Consumer
lock.lock();
try {
    while (buffer.isEmpty()) notEmpty.await();
    Item item = buffer.remove();
    notFull.signal();    // wake one producer
} finally { lock.unlock(); }
```

| | `synchronized` | `ReentrantLock` |
|---|---|---|
| Syntax | Simpler | More verbose |
| Fairness | No guarantee | `new ReentrantLock(true)` — fair |
| Try-lock | Not possible | `tryLock()`, `tryLock(timeout)` |
| Interruptible | No | `lockInterruptibly()` |
| Multiple conditions | One (wait/notify) | Multiple `Condition` objects |
| Default choice | Yes | Only when you need the extras |

---

## ExecutorService — Thread Pool Management

Never create raw `new Thread()` in production code — it's not reusable and has no lifecycle control.

```java
// Fixed pool — bounded concurrency, good for CPU-bound work
ExecutorService executor = Executors.newFixedThreadPool(4);

// Cached pool — creates threads on demand, reuses idle ones (60s TTL)
// Good for I/O-bound work with variable load. Risk: unbounded thread creation
ExecutorService executor = Executors.newCachedThreadPool();

// Single thread — sequential execution, tasks queued; preserves task order
ExecutorService executor = Executors.newSingleThreadExecutor();

// Scheduled — delayed or periodic tasks
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
scheduler.scheduleAtFixedRate(() -> checkHealth(), 0, 30, TimeUnit.SECONDS);

// Submit tasks
executor.execute(() -> doWork());                    // Runnable — no result
Future<String> future = executor.submit(() -> compute());  // Callable — has result

// Get result (blocks until done)
String result = future.get();
String result = future.get(5, TimeUnit.SECONDS);     // with timeout

// Shutdown
executor.shutdown();      // stop accepting new tasks; wait for running tasks to finish
executor.shutdownNow();   // interrupt running tasks; return list of queued tasks
```

**Thread pool sizing:**
```java
int cpus = Runtime.getRuntime().availableProcessors();

// CPU-bound (computation only, no I/O)
int poolSize = cpus;          // or cpus + 1

// I/O-bound (waiting on network, DB, disk)
// Threads spend most time waiting → can have more threads than CPUs
// Rule: poolSize = cpus * (1 + waitTime / serviceTime)
// If wait:service = 9:1 → poolSize = cpus * 10
int poolSize = cpus * 10;    // rough estimate for I/O-bound
```

---

## CompletableFuture — Non-Blocking Async Pipelines

```java
// Start async computation (runs in ForkJoinPool.commonPool() by default)
CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> fetchFromDB());

// Always use a custom executor for I/O to avoid blocking the ForkJoinPool
ExecutorService ioPool = Executors.newCachedThreadPool();
CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> fetchFromDB(), ioPool);
```

### Pipeline operations
```java
cf
  .thenApply(data -> data.toUpperCase())      // transform T → U  (like map)
  .thenApply(data -> parse(data))             // chain transforms
  .thenAccept(result -> save(result))         // consume, no return value
  .thenRun(() -> log("pipeline done"))        // run after, no input or output
  .exceptionally(ex -> {                      // handle any exception in the chain
      log("error: " + ex.getMessage());
      return "fallback";
  });
```

### thenApply vs thenCompose
```java
// thenApply — transform the result (function returns plain value)
cf.thenApply(s -> s.length())    // CompletableFuture<Integer>

// thenCompose — when your transform itself returns a CompletableFuture (flatMap)
cf.thenCompose(userId -> fetchUserAsync(userId))   // CF<User>, not CF<CF<User>>

// Rule: if your lambda returns CF<T>, use thenCompose. Otherwise thenApply.
```

### Combining futures
```java
CompletableFuture<String> user    = CompletableFuture.supplyAsync(() -> fetchUser(id));
CompletableFuture<String> account = CompletableFuture.supplyAsync(() -> fetchAccount(id));

// Both run in parallel — combine when both done
user.thenCombine(account, (u, a) -> u + " | " + a)
    .thenAccept(System.out::println);

// Wait for all N futures (run them in parallel)
CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2, f3);
all.join();   // blocks until all complete

// Race — first one to finish wins
CompletableFuture.anyOf(f1, f2, f3)
    .thenAccept(result -> use((String) result));
```

### Exception handling
```java
cf
  .thenApply(this::riskyTransform)
  .exceptionally(ex -> "fallback value")           // recover from any exception above
  .thenApply(this::continueWithResult);

// handle — always runs, whether exception or not (like finally for CF)
cf.handle((result, ex) -> {
    if (ex != null) return "error: " + ex.getMessage();
    return result.toUpperCase();
});
```

---

## CountDownLatch, CyclicBarrier, Semaphore

### CountDownLatch — one thread waits for N others
```java
CountDownLatch latch = new CountDownLatch(3);

for (int i = 0; i < 3; i++) {
    executor.submit(() -> {
        try { doWork(); }
        finally { latch.countDown(); }   // always count down, even on exception
    });
}

latch.await();              // blocks main thread until count hits 0
latch.await(5, SECONDS);    // with timeout
// One-shot: count only goes down, cannot be reset
```

### CyclicBarrier — N threads synchronize between phases
```java
CyclicBarrier barrier = new CyclicBarrier(3, () -> mergeResults());  // runs when all arrive

// Each worker thread:
doPhase1Work();
barrier.await();   // blocks until all 3 call await()
doPhase2Work();    // all 3 start phase 2 together
barrier.await();   // can reuse — cyclic
doPhase3Work();
```

### Semaphore — rate limiting, resource pools
```java
Semaphore sem = new Semaphore(5);   // max 5 concurrent access

public void callExternalAPI() {
    sem.acquire();                  // blocks if 5 already running
    try {
        http.call(url);
    } finally {
        sem.release();              // always in finally
    }
}

// Semaphore(1) = mutex (like synchronized but can be released by different thread)
```

| | CountDownLatch | CyclicBarrier | Semaphore |
|---|---|---|---|
| Use case | Wait for N events | Sync N threads at a point | Limit N concurrent |
| Resettable | No | Yes | N/A |
| Who waits | One (or few) thread waits | All N threads wait each other | Each thread waits for a permit |

---

## BlockingQueue — Producer-Consumer

```java
BlockingQueue<Task> queue = new ArrayBlockingQueue<>(100);  // bounded — backpressure!

// Producer thread
while (running) {
    Task t = generate();
    queue.put(t);       // blocks if queue full — automatic backpressure
}

// Consumer thread
while (running) {
    Task t = queue.take();   // blocks if queue empty
    process(t);
}
```

| Method | Throws exception | Returns false/null | Blocks | Times out |
|---|---|---|---|---|
| Insert | `add()` | `offer()` | `put()` | `offer(e, t, unit)` |
| Remove | `remove()` | `poll()` | `take()` | `poll(t, unit)` |

**Implementations:**
- `ArrayBlockingQueue(n)` — bounded, circular array, fair-ordering option
- `LinkedBlockingQueue` — optionally bounded (default Integer.MAX_VALUE)
- `PriorityBlockingQueue` — priority-ordered, unbounded
- `SynchronousQueue` — zero capacity; put() waits for take() and vice versa (direct handoff)

---

## ThreadLocal — Per-Thread Isolation

```java
// Each thread gets its own copy — no sharing, no synchronization needed
ThreadLocal<DateFormat> formatter = ThreadLocal.withInitial(
    () -> new SimpleDateFormat("yyyy-MM-dd")
);

public String format(Date date) {
    return formatter.get().format(date);   // thread-safe: each thread has its own formatter
}

// CRITICAL in thread pools: threads are reused — always remove after use
public String processRequest(Request req) {
    try {
        formatter.set(new SimpleDateFormat(req.getFormat()));
        return formatter.get().format(req.getDate());
    } finally {
        formatter.remove();   // if you skip this, the old value pollutes the next request
    }
}
```

**Common use cases:** per-request user context, database connections per transaction, SimpleDateFormat (not thread-safe).

---

## ReadWriteLock — Optimize Read-Heavy Workloads

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();

// Many threads can READ simultaneously
public String get(String key) {
    rwLock.readLock().lock();
    try { return cache.get(key); }
    finally { rwLock.readLock().unlock(); }
}

// Only ONE thread can WRITE (blocks all readers while writing)
public void put(String key, String value) {
    rwLock.writeLock().lock();
    try { cache.put(key, value); }
    finally { rwLock.writeLock().unlock(); }
}
```

**Trade-off:** Better than synchronized when reads >> writes (config maps, reference data caches). Unnecessary overhead if write frequency is high — use ConcurrentHashMap instead.

---

## Deadlock — Detection and Prevention

**4 conditions required for deadlock (all must hold):**
1. Mutual exclusion — resources are non-shareable
2. Hold and wait — thread holds one resource and waits for another
3. No preemption — resources can't be forcibly taken
4. Circular wait — A waits for B, B waits for A

```java
// Classic deadlock
synchronized (lockA) {              // Thread 1 holds A, wants B
    synchronized (lockB) { ... }    // Thread 2 holds B, wants A → deadlock
}

// Prevention 1: consistent lock ordering (breaks circular wait)
// Always acquire in alphabetical/ID order — EVERYWHERE in the codebase
if (System.identityHashCode(a) < System.identityHashCode(b)) {
    synchronized (a) { synchronized (b) { transfer(a, b); } }
} else {
    synchronized (b) { synchronized (a) { transfer(a, b); } }
}

// Prevention 2: tryLock with timeout (breaks hold-and-wait)
boolean locked = false;
while (!locked) {
    lockA.lock();
    if (lockB.tryLock(50, TimeUnit.MILLISECONDS)) {
        locked = true;
    } else {
        lockA.unlock();   // release what you hold before retrying
        Thread.sleep(random backoff);
    }
}

// Prevention 3: avoid nested locks — use lock-free structures (ConcurrentHashMap, AtomicXxx)
```

---

## Common Concurrency Problems — Recognized Patterns

### Race condition
```java
// Two threads check-then-act on shared state
if (!map.containsKey(key)) {        // Thread A checks: not present
    map.put(key, compute(key));     // Thread A: about to insert
}                                   // Thread B also checked: not present → both insert!

// Fix: computeIfAbsent is atomic
map.computeIfAbsent(key, k -> compute(k));
```

### Visibility problem (stale read)
```java
boolean done = false;               // no volatile

Thread worker = new Thread(() -> {
    while (!done) { }               // may loop forever — reads cached value
    System.out.println("done");
});
worker.start();
done = true;                        // write not visible to worker's CPU cache

// Fix: volatile boolean done = true;
```

### Thread starvation
```java
// Low-priority threads never get CPU time when high-priority threads dominate
// Fix: ReentrantLock(true) — fair lock, FIFO ordering
ReentrantLock fairLock = new ReentrantLock(true);
```

### Livelock
Two threads keep responding to each other but make no progress (like two people stepping aside for each other forever).
```
Fix: introduce randomness in retry timing, or use a backing-off strategy.
```

---

## Common Interview Questions

**Q: What is the difference between `volatile` and `synchronized`?**
A: `volatile` solves visibility — writes to a volatile variable are immediately visible to all threads. It does NOT give atomicity, so `volatile int x; x++` is still a race condition. `synchronized` solves both: only one thread executes the block (mutual exclusion), AND all writes inside are visible to the next thread that acquires the same lock. Rule: use `volatile` when one thread writes and others only read; use `synchronized` (or `AtomicInteger`) when multiple threads write.

**Q: What is happens-before?**
A: A guarantee in the Java Memory Model that if action A happens-before action B, then all memory writes by A are visible to B. Key rules: unlock happens-before lock of the same monitor; volatile write happens-before subsequent volatile read; `Thread.start()` happens-before anything in the started thread; everything in a thread happens-before `thread.join()` returns.

**Q: Why is `x++` not atomic even with `volatile`?**
A: `x++` compiles to three operations: read x, add 1, write x. `volatile` only ensures each individual read and write is visible — it cannot prevent another thread from reading the old value between your read and your write. Fix: `AtomicInteger.incrementAndGet()` which uses a single CAS hardware instruction.

**Q: What is CAS and why is it faster than locking?**
A: Compare-And-Swap is a hardware instruction: "set memory location to new value only if it currently equals expected." It's atomic at the CPU level — no thread suspension needed. `synchronized` requires the OS to park/unpark threads (context switch, syscall), which is expensive. CAS fails fast and retries, avoiding the scheduling overhead. Better under low contention; can waste CPU under high contention (spinning).

**Q: What's the difference between `execute()` and `submit()` in ExecutorService?**
A: `execute(Runnable)` fires and forgets — no return value, exceptions are lost (only visible via `Thread.UncaughtExceptionHandler`). `submit(Callable)` returns a `Future` — you can call `future.get()` to retrieve the result or catch any exception that was thrown.

**Q: How do you size a thread pool?**
A: CPU-bound tasks: `availableProcessors` or `+1`. I/O-bound tasks: `availableProcessors * (1 + waitTime/serviceTime)`. If threads spend 90% of time waiting on I/O, you can have ~10× more threads than CPUs and still keep all CPUs busy. Profile first — wrong sizing causes either thread starvation (too small) or context-switch overhead (too large).

**Q: What is a deadlock? How do you prevent it?**
A: Deadlock is when two or more threads hold locks and each waits for a lock the other holds — circular wait, no progress forever. Prevention: (1) Always acquire multiple locks in the same global order everywhere in the codebase. (2) Use `tryLock` with timeout and release held locks on failure. (3) Reduce lock scope — use `ConcurrentHashMap`, `AtomicXxx` to avoid needing multiple locks.

**Q: When would you use CompletableFuture over a thread pool directly?**
A: When you need non-blocking pipelines — chain async operations, combine results from multiple parallel calls, handle errors in the chain. Direct thread pools with `Future.get()` block the calling thread. CompletableFuture chains are non-blocking callbacks — much better for reactive/async service code.

**Q: What is ThreadLocal and what is the risk in thread pools?**
A: `ThreadLocal` gives each thread its own independent copy of a variable — no sharing, no synchronization needed. In thread pools, threads are reused across requests. If you set a ThreadLocal value in request 1 and don't remove it, the same thread processing request 2 will see request 1's value. Always call `remove()` in a `finally` block.

**Q: What is the difference between CountDownLatch and CyclicBarrier?**
A: `CountDownLatch`: one (or more) threads wait for N events to happen. Count is decremented by workers; the waiter blocks. One-shot — cannot reset. `CyclicBarrier`: N threads all wait for each other at a synchronization point before any proceeds. Reusable — resets automatically after all threads arrive. Use latch for "wait for tasks to complete"; use barrier for "all threads reach a phase together."
