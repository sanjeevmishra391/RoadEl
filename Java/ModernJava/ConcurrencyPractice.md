# Concurrency — Coding Practice

> For each problem: read it, close the file, write the code yourself, then check.
> The skill being tested is not memorizing code — it's knowing *which tool* to reach for and *why*.

---

## Level 1 — Core Mechanics

### P1: Thread-Safe Counter

**Problem:** Implement a counter that can be safely incremented by multiple threads concurrently. Show 3 different ways.

**What concept:** volatile vs AtomicInteger vs synchronized — know when each is appropriate.

<details>
<summary>Solution</summary>

```java
// Way 1: synchronized — simplest, correct, but blocks
public class SyncCounter {
    private int count = 0;

    public synchronized void increment() { count++; }
    public synchronized int get() { return count; }
}

// Way 2: AtomicInteger — lock-free, faster under contention
public class AtomicCounter {
    private AtomicInteger count = new AtomicInteger(0);

    public void increment() { count.incrementAndGet(); }
    public int get() { return count.get(); }
}

// Way 3: volatile — WRONG for a counter (shows you know the difference)
// volatile int count; count++; is NOT atomic — race condition
// Correct use of volatile: a flag read by many threads, written by one
public class VolatileFlag {
    private volatile boolean running = true;
    public void stop() { running = false; }
    public boolean isRunning() { return running; }
}
```

**Key point to say in interview:** `volatile` only fixes visibility. `x++` is read-modify-write — three steps. Another thread can read between the read and write. `AtomicInteger` uses a CAS hardware instruction that makes the entire operation atomic without blocking.

</details>

---

### P2: Stop a Thread Cleanly

**Problem:** Start a background thread that does work in a loop. Stop it cleanly from another thread.

**What concept:** interruption protocol — the right way to stop threads.

<details>
<summary>Solution</summary>

```java
public class BackgroundWorker implements Runnable {
    @Override
    public void run() {
        // Check interrupted flag on every loop iteration
        while (!Thread.currentThread().isInterrupted()) {
            try {
                doUnitOfWork();
                Thread.sleep(100);          // sleep throws InterruptedException
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // restore the flag — important!
                break;                               // then exit
            }
        }
        cleanup();
    }
}

Thread worker = new Thread(new BackgroundWorker());
worker.start();

// From another thread, to stop:
worker.interrupt();   // sets the interrupted flag; wakes it from sleep
worker.join();        // wait for it to actually finish
```

**Why restore the flag?** `catch (InterruptedException)` clears the flag. If you don't restore it, code further up the call stack that checks `isInterrupted()` won't know the thread was interrupted.

</details>

---

### P3: Thread-Safe Singleton (Double-Checked Locking)

**Problem:** Implement a singleton that is lazily initialized and thread-safe.

**What concept:** double-checked locking + volatile — a classic interview question.

<details>
<summary>Solution</summary>

```java
public class Singleton {
    // volatile prevents partial construction being visible to other threads
    private static volatile Singleton instance;

    private Singleton() { }

    public static Singleton getInstance() {
        if (instance == null) {                    // first check — no lock (fast path)
            synchronized (Singleton.class) {
                if (instance == null) {            // second check — with lock (safe)
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

// Why volatile is essential:
// new Singleton() is 3 steps: allocate memory, init fields, assign reference
// Without volatile, JVM can reorder to: allocate, assign reference, init fields
// Another thread sees non-null instance but partially constructed object → crash

// Simpler alternative: use enum (handles serialization too)
public enum SingletonEnum {
    INSTANCE;
    public void doSomething() { }
}
```

</details>

---

## Level 2 — Coordination Patterns

### P4: Producer-Consumer with BlockingQueue

**Problem:** Implement a producer that generates tasks and multiple consumers that process them. Producers should block when the queue is full (backpressure).

**What concept:** BlockingQueue — the standard producer-consumer solution.

<details>
<summary>Solution</summary>

```java
public class ProducerConsumer {
    private static final int QUEUE_SIZE = 10;
    private final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private final ExecutorService pool = Executors.newFixedThreadPool(5);

    public void start() {
        // 1 producer
        pool.submit(() -> {
            int item = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    queue.put(item++);          // blocks if queue is full
                    System.out.println("Produced: " + item);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        // 3 consumers
        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        int item = queue.take();    // blocks if queue is empty
                        process(item);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }

    public void stop() {
        pool.shutdownNow();
    }
}
```

**Why ArrayBlockingQueue over LinkedBlockingQueue?** Bounded queue provides backpressure — the producer slows down when consumers can't keep up, preventing OOM. LinkedBlockingQueue defaults to `Integer.MAX_VALUE` capacity — effectively unbounded, can OOM under load.

</details>

---

### P5: Run N Tasks in Parallel, Wait for All

**Problem:** You have a list of URLs to fetch. Fetch all of them in parallel, wait for all to complete, then process the results.

**What concept:** CompletableFuture.allOf — the standard "fan-out, collect" pattern.

<details>
<summary>Solution</summary>

```java
ExecutorService ioPool = Executors.newCachedThreadPool();

List<String> urls = List.of("url1", "url2", "url3", "url4");

// Fan out — start all in parallel
List<CompletableFuture<String>> futures = urls.stream()
    .map(url -> CompletableFuture.supplyAsync(() -> fetch(url), ioPool))
    .collect(Collectors.toList());

// Wait for all, collect results
CompletableFuture<List<String>> allResults = CompletableFuture
    .allOf(futures.toArray(new CompletableFuture[0]))
    .thenApply(v -> futures.stream()
        .map(CompletableFuture::join)   // join is safe here — all are already done
        .collect(Collectors.toList()));

List<String> results = allResults.get(10, TimeUnit.SECONDS);

// Important: use a custom executor (ioPool) for I/O tasks
// Don't use the default ForkJoinPool.commonPool() for blocking I/O
// — it's shared and blocking it affects all parallel streams and other CFs
```

</details>

---

### P6: Wait for First Successful Result

**Problem:** Query 3 replica databases in parallel. Return the first successful response; ignore slow/failed ones.

**What concept:** CompletableFuture.anyOf + exception handling.

<details>
<summary>Solution</summary>

```java
public CompletableFuture<String> queryWithFallback(List<String> replicas) {
    List<CompletableFuture<String>> futures = replicas.stream()
        .map(replica -> CompletableFuture
            .supplyAsync(() -> queryDB(replica), ioPool)
            .exceptionally(ex -> null))   // failed → null instead of exception
        .collect(Collectors.toList());

    // anyOf returns the first to complete (including failures)
    // We need to filter — keep polling until we get a non-null result
    CompletableFuture<String> result = new CompletableFuture<>();
    futures.forEach(f -> f.thenAccept(val -> {
        if (val != null) result.complete(val);  // complete only once (first wins)
    }));

    // Fail if all fail
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenRun(() -> result.completeExceptionally(
            new RuntimeException("All replicas failed")));

    return result;
}
```

</details>

---

### P7: Rate Limiter using Semaphore

**Problem:** Implement a rate limiter that allows at most N concurrent calls to an external API.

**What concept:** Semaphore as a concurrency limiter.

<details>
<summary>Solution</summary>

```java
public class RateLimiter {
    private final Semaphore semaphore;

    public RateLimiter(int maxConcurrent) {
        this.semaphore = new Semaphore(maxConcurrent);
    }

    public <T> T call(Supplier<T> task) throws InterruptedException {
        semaphore.acquire();   // block until permit available
        try {
            return task.get();
        } finally {
            semaphore.release();   // always release
        }
    }

    // With timeout — don't wait forever
    public <T> Optional<T> callWithTimeout(Supplier<T> task, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (!semaphore.tryAcquire(timeout, unit)) {
            return Optional.empty();   // couldn't get permit in time
        }
        try {
            return Optional.of(task.get());
        } finally {
            semaphore.release();
        }
    }
}

// Usage
RateLimiter limiter = new RateLimiter(5);  // max 5 concurrent external calls
String result = limiter.call(() -> externalApi.fetch(id));
```

</details>

---

### P8: Parallel Phase Execution with CyclicBarrier

**Problem:** You have 3 worker threads that must all complete phase 1 before any starts phase 2, and all complete phase 2 before any starts phase 3.

**What concept:** CyclicBarrier — synchronize threads between phases.

<details>
<summary>Solution</summary>

```java
int numWorkers = 3;
CyclicBarrier barrier = new CyclicBarrier(numWorkers,
    () -> System.out.println("All workers reached barrier — next phase"));

ExecutorService pool = Executors.newFixedThreadPool(numWorkers);

for (int i = 0; i < numWorkers; i++) {
    final int workerId = i;
    pool.submit(() -> {
        try {
            // Phase 1
            doPhase1Work(workerId);
            System.out.println("Worker " + workerId + " done phase 1");
            barrier.await();   // wait until all 3 finish phase 1

            // Phase 2
            doPhase2Work(workerId);
            System.out.println("Worker " + workerId + " done phase 2");
            barrier.await();   // wait until all 3 finish phase 2

            // Phase 3 — no barrier needed at the end
            doPhase3Work(workerId);

        } catch (InterruptedException | BrokenBarrierException e) {
            Thread.currentThread().interrupt();
        }
    });
}

pool.shutdown();
pool.awaitTermination(30, TimeUnit.SECONDS);
```

**BrokenBarrierException:** thrown if one thread times out or is interrupted while waiting — the barrier is "broken" and all waiting threads get this exception. Handle it to avoid hanging.

</details>

---

## Level 3 — Classic Interview Problems

### P9: Bounded Thread-Safe Stack

**Problem:** Implement a thread-safe bounded stack (push blocks if full, pop blocks if empty).

**What concept:** ReentrantLock + Condition variables — the correct way to implement blocking data structures.

<details>
<summary>Solution</summary>

```java
public class BoundedStack<T> {
    private final Object[] elements;
    private int top = -1;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public BoundedStack(int capacity) {
        elements = new Object[capacity];
    }

    public void push(T item) throws InterruptedException {
        lock.lock();
        try {
            while (top == elements.length - 1) {
                notFull.await();    // stack full — release lock and wait
            }
            elements[++top] = item;
            notEmpty.signal();      // wake one thread waiting to pop
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public T pop() throws InterruptedException {
        lock.lock();
        try {
            while (top == -1) {
                notEmpty.await();   // stack empty — release lock and wait
            }
            T item = (T) elements[top];
            elements[top--] = null; // help GC
            notFull.signal();       // wake one thread waiting to push
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

**Why `while` not `if` before await()?** Spurious wakeups — a thread can wake up from `await()` without being notified. Always check the condition again after waking up.

</details>

---

### P10: Print Numbers Alternately from Two Threads

**Problem:** Thread 1 prints odd numbers (1, 3, 5...), Thread 2 prints even numbers (2, 4, 6...). Output must be 1, 2, 3, 4, 5...

**What concept:** wait/notify coordination — inter-thread communication.

<details>
<summary>Solution</summary>

```java
public class AlternatePrinter {
    private final Object lock = new Object();
    private volatile int current = 1;
    private final int max;

    public AlternatePrinter(int max) { this.max = max; }

    public void printOdd() {
        synchronized (lock) {
            while (current <= max) {
                if (current % 2 == 0) {          // not my turn
                    try { lock.wait(); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); return;
                    }
                } else {
                    System.out.println("Odd:  " + current++);
                    lock.notify();               // wake the even thread
                }
            }
        }
    }

    public void printEven() {
        synchronized (lock) {
            while (current <= max) {
                if (current % 2 != 0) {          // not my turn
                    try { lock.wait(); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); return;
                    }
                } else {
                    System.out.println("Even: " + current++);
                    lock.notify();               // wake the odd thread
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        AlternatePrinter p = new AlternatePrinter(10);
        Thread t1 = new Thread(p::printOdd);
        Thread t2 = new Thread(p::printEven);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
```

</details>

---

### P11: Read-Write Cache with ReadWriteLock

**Problem:** Implement a thread-safe in-memory cache where reads are very frequent and writes are rare. Maximize read concurrency.

**What concept:** ReadWriteLock — allow concurrent reads, exclusive writes.

<details>
<summary>Solution</summary>

```java
public class ReadWriteCache<K, V> {
    private final Map<K, V> map = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public V get(K key) {
        lock.readLock().lock();           // multiple threads can hold this simultaneously
        try {
            return map.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void put(K key, V value) {
        lock.writeLock().lock();          // exclusive — blocks all readers and writers
        try {
            map.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Read-then-write: must upgrade to write lock
    // ReadWriteLock does NOT support lock upgrade — must release read lock first
    public V getOrLoad(K key, Supplier<V> loader) {
        // Step 1: check with read lock
        lock.readLock().lock();
        try {
            V value = map.get(key);
            if (value != null) return value;  // cache hit — no write needed
        } finally {
            lock.readLock().unlock();
        }

        // Step 2: must re-check after acquiring write lock (another thread may have loaded)
        lock.writeLock().lock();
        try {
            V value = map.get(key);           // double-check
            if (value == null) {
                value = loader.get();
                map.put(key, value);
            }
            return value;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
```

**Key gotcha:** You cannot upgrade from read lock to write lock in `ReentrantReadWriteLock`. You must release the read lock first, then acquire the write lock, then re-check the condition.

</details>

---

### P12: Async Service Call with Timeout and Fallback

**Problem:** Call a remote service asynchronously. If it doesn't respond within 2 seconds, return a cached/default value.

**What concept:** CompletableFuture with timeout — the real-world async pattern.

<details>
<summary>Solution</summary>

```java
public class ServiceWithFallback {
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public CompletableFuture<String> getData(String id) {
        CompletableFuture<String> remote = CompletableFuture
            .supplyAsync(() -> remoteService.fetch(id), pool);

        // Java 9+: built-in timeout
        return remote.orTimeout(2, TimeUnit.SECONDS)
                     .exceptionally(ex -> {
                         if (ex instanceof TimeoutException) {
                             return cache.get(id);      // return stale cached value
                         }
                         return "default";              // other errors
                     });
    }

    // Java 8 compatible: manual timeout with completeOnTimeout
    public CompletableFuture<String> getDataJava8(String id) {
        CompletableFuture<String> remote = CompletableFuture
            .supplyAsync(() -> remoteService.fetch(id), pool);

        // Schedule timeout on separate thread
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> remote.completeExceptionally(
            new TimeoutException()), 2, TimeUnit.SECONDS);

        return remote.exceptionally(ex -> cache.get(id));
    }
}
```

</details>

---

## How to Practice

### Study sequence
1. Read `Concurrency.md` — understand each concept and *why* it exists
2. For each problem here: close the solutions, write the code yourself
3. Explain your code out loud as you write — this is what the interview feels like
4. Common mistakes to watch for:
   - Forgetting `unlock()` in `finally` → deadlock
   - Using `if` instead of `while` before `await()` → spurious wakeup bug
   - Not restoring interrupt flag after catching `InterruptedException`
   - Not using a custom executor for I/O in CompletableFuture
   - Using `new Thread()` instead of ExecutorService

### What interviewers are testing
- Can you identify *which* concurrency problem this is (visibility, atomicity, coordination)?
- Do you reach for the right tool (volatile, AtomicXxx, synchronized, BlockingQueue, CF)?
- Do you know the failure modes (what breaks if you use the wrong tool)?
- Can you explain *why* your solution is correct, not just that it works?

### Quick reference — which tool for which problem
| Problem | Tool |
|---|---|
| One writer, many readers of a flag | `volatile` |
| Counter incremented by many threads | `AtomicInteger` |
| Protecting a critical section | `synchronized` or `ReentrantLock` |
| Need try-lock / fairness / multiple conditions | `ReentrantLock` |
| Producer-consumer with backpressure | `BlockingQueue` |
| Parallel async calls, collect all results | `CompletableFuture.allOf` |
| First result wins (race multiple sources) | `CompletableFuture.anyOf` |
| Main thread waits for N workers | `CountDownLatch` |
| N threads sync between phases | `CyclicBarrier` |
| Limit N concurrent access to a resource | `Semaphore` |
| Read-heavy shared state | `ReadWriteLock` |
| Per-request context in web framework | `ThreadLocal` (with `remove()`) |
| Lazy init, thread-safe singleton | Double-checked locking + `volatile` |
