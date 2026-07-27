# Java Concurrency Patterns for LLD Interviews

This guide bridges Java concurrency theory and LLD interview questions. When an interviewer asks you to "design a thread-safe LRU cache" or "design a task scheduler," they are asking you to apply these patterns — not to recite `synchronized` syntax.

---

## Part 1: Concurrency in LLD Interviews

### Which LLD Problems Have a Concurrency Sub-Problem

Concurrency is almost always relevant in these commonly asked LLD questions:

| LLD Problem | Concurrency Sub-Problem |
|---|---|
| LRU Cache | Multiple threads reading/writing the cache simultaneously |
| Rate Limiter | Atomic counter updates, refill scheduling |
| Task Scheduler | Priority queue access, delayed execution, cancellation |
| Connection Pool | Bounded capacity, blocking borrow, thread-safe return |
| Parking Lot | Slot allocation under concurrent arrivals |
| Pub/Sub System | Concurrent publishers, concurrent subscribers, queue ordering |
| Bank Account | Atomic balance updates, deadlock risk on transfers |
| Singleton | Double-checked locking (or why to avoid it) |

In an interview, you do not need to implement every concurrency detail. You need to **identify the concurrency sub-problems**, explain which constructs solve them, and implement at least one critical section correctly.

### The Two Failure Modes

**Too much locking — deadlock and performance degradation**

Over-synchronization is the failure mode of cautious engineers. Signs:
- Synchronizing on `this` for every method in a class, including reads
- Acquiring multiple locks in different orders in different code paths (deadlock)
- Using a single coarse-grained lock for a data structure that could support finer-grained locking

**Too little locking — race conditions and data corruption**

Under-synchronization is the failure mode of engineers who think about the happy path. Signs:
- Assuming `++counter` is atomic (it is not: it is read-modify-write, three operations)
- Reading a field updated in another thread without `volatile` or synchronization
- Performing a check-then-act pattern without holding a lock across both operations

```java
// Classic check-then-act race condition
if (!cache.containsKey(key)) {       // Thread A checks: key absent
    // Thread B also checks: key absent
    cache.put(key, computeValue());  // Both threads insert — one is lost
}
```

---

## Part 2: Core Patterns with Java Implementations

---

### 1. Thread-Safe Singleton

Already covered in `Patterns/Singleton.md`. The canonical implementation for interviews is the **initialization-on-demand holder** pattern:

```java
public class Singleton {
    private Singleton() {}

    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }

    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

This is lazy (the Holder class is only loaded when `getInstance()` is first called), thread-safe (class loading is guaranteed thread-safe by the JVM), and requires no synchronization overhead on subsequent calls.

---

### 2. Producer-Consumer with BlockingQueue

**Problem it solves:** Decoupling work generation from work processing. Producers should not be blocked by slow consumers; consumers should not spin-wait when no work is available.

**Java construct:** `BlockingQueue` — specifically `LinkedBlockingQueue` for unbounded or `ArrayBlockingQueue` for bounded capacity.

```java
import java.util.concurrent.*;

public class ProducerConsumerExample {

    private static final int CAPACITY = 100;
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(CAPACITY);

    class Producer implements Runnable {
        private final String name;

        Producer(String name) { this.name = name; }

        @Override
        public void run() {
            try {
                for (int i = 0; i < 20; i++) {
                    String item = name + "-item-" + i;
                    queue.put(item);  // Blocks if queue is full
                    System.out.println("Produced: " + item);
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    class Consumer implements Runnable {
        private final String name;

        Consumer(String name) { this.name = name; }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // poll with timeout avoids indefinite blocking on shutdown
                    String item = queue.poll(1, TimeUnit.SECONDS);
                    if (item == null) break; // Timeout: assume producers done
                    System.out.println(name + " consumed: " + item);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void run() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        executor.submit(new Producer("P1"));
        executor.submit(new Producer("P2"));
        executor.submit(new Consumer("C1"));
        executor.submit(new Consumer("C2"));
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }
}
```

**When to use:** Anywhere you have a pipeline where stages run at different speeds. CI pipelines, log ingestion, event processing.

**Pitfalls:**
- Never use a raw `LinkedList` + `wait()/notify()` unless asked to implement from scratch — `BlockingQueue` is the production answer
- `ArrayBlockingQueue` is bounded and will cause producers to block if consumers can't keep up — this is usually what you want for backpressure
- Prefer `poll(timeout)` over `take()` in consumers so that threads respond to shutdown signals

---

### 3. Read-Write Lock

**Problem it solves:** When reads are far more frequent than writes, a mutual-exclusion lock forces all readers to serialize even though they could run concurrently. `ReentrantReadWriteLock` allows any number of concurrent readers, or exactly one writer.

```java
import java.util.concurrent.locks.*;
import java.util.*;

public class ReadWriteCache<K, V> {

    private final Map<K, V> cache = new HashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    public V get(K key) {
        readLock.lock();
        try {
            return cache.get(key);
        } finally {
            readLock.unlock();  // Always release in finally
        }
    }

    public void put(K key, V value) {
        writeLock.lock();
        try {
            cache.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }

    // Read-to-write lock UPGRADE — not directly supported, requires releasing read lock first
    public V computeIfAbsent(K key, java.util.function.Function<K, V> loader) {
        readLock.lock();
        try {
            V value = cache.get(key);
            if (value != null) return value;
        } finally {
            readLock.unlock();
        }

        // Between releasing read lock and acquiring write lock, another thread
        // may have inserted the value — must re-check inside write lock
        writeLock.lock();
        try {
            V value = cache.get(key);  // Double-check
            if (value == null) {
                value = loader.apply(key);
                cache.put(key, value);
            }
            return value;
        } finally {
            writeLock.unlock();
        }
    }
}
```

**When to use:** Configuration stores, caches, directory lookups — anywhere reads dominate and writes are infrequent.

**The upgrade problem:** Java's `ReentrantReadWriteLock` does **not** support upgrading a read lock to a write lock. If you try to acquire a write lock while holding a read lock, you will deadlock. The correct pattern is to release the read lock, acquire the write lock, and re-check the condition (as shown above).

---

### 4. Thread Pool Pattern

**Problem it solves:** Creating a new thread for every task is expensive. Thread pools reuse a fixed or flexible set of threads to process a queue of submitted tasks.

```java
import java.util.concurrent.*;

public class ThreadPoolExamples {

    public static void main(String[] args) throws InterruptedException {

        // Fixed pool — best for CPU-bound tasks where you know the parallelism
        ExecutorService fixedPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );

        // Cached pool — creates threads on demand, reuses idle ones
        // Good for short-lived async tasks; dangerous for long-running ones (unbounded)
        ExecutorService cachedPool = Executors.newCachedThreadPool();

        // Scheduled pool — for delayed or periodic tasks
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(
            () -> System.out.println("Heartbeat"),
            0, 5, TimeUnit.SECONDS
        );

        // Custom ThreadPoolExecutor — when you need explicit control
        ThreadPoolExecutor customPool = new ThreadPoolExecutor(
            4,                              // corePoolSize
            8,                              // maximumPoolSize
            60L, TimeUnit.SECONDS,          // keepAliveTime for idle threads
            new LinkedBlockingQueue<>(500), // task queue with bounded capacity
            new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
        );
        // CallerRunsPolicy: if pool is saturated, the submitting thread runs the task
        // This provides natural backpressure — slows producers when system is overloaded

        // ForkJoinPool — for recursive divide-and-conquer (merge sort, tree traversal)
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        Long result = forkJoinPool.invoke(new SumTask(new long[1000], 0, 1000));

        fixedPool.shutdown();
        cachedPool.shutdown();
        scheduler.shutdown();
        customPool.shutdown();
        forkJoinPool.shutdown();
    }
}

class SumTask extends RecursiveTask<Long> {
    private static final int THRESHOLD = 100;
    private final long[] array;
    private final int start, end;

    SumTask(long[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            long sum = 0;
            for (int i = start; i < end; i++) sum += array[i];
            return sum;
        }
        int mid = (start + end) / 2;
        SumTask left = new SumTask(array, start, mid);
        SumTask right = new SumTask(array, mid, end);
        left.fork();                      // Run left in another thread
        return right.compute() + left.join(); // Run right here, then join left
    }
}
```

**Rejection policies when pool is saturated:**
- `AbortPolicy` (default): throws `RejectedExecutionException`
- `CallerRunsPolicy`: caller's thread runs the task — provides backpressure
- `DiscardPolicy`: silently drops the task
- `DiscardOldestPolicy`: drops the oldest queued task and retries

---

### 5. Future / CompletableFuture

**Problem it solves:** Asynchronous task execution where you need the result later, or where you want to chain and combine async operations without blocking threads.

```java
import java.util.concurrent.*;

public class CompletableFutureExamples {

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    // Basic async execution
    public CompletableFuture<String> fetchUser(String userId) {
        return CompletableFuture.supplyAsync(
            () -> callUserService(userId),
            executor
        );
    }

    // Chaining: thenApply transforms the result (synchronous transform)
    public CompletableFuture<String> fetchAndFormatUser(String userId) {
        return fetchUser(userId)
            .thenApply(user -> "Formatted: " + user);
    }

    // thenCompose: flattens a Future<Future<T>> — use when the next step is itself async
    public CompletableFuture<String> fetchUserAndOrders(String userId) {
        return fetchUser(userId)
            .thenCompose(user -> fetchOrders(user)); // fetchOrders returns CompletableFuture
    }

    // thenCombine: combines two independent futures when both complete
    public CompletableFuture<String> fetchUserProfile(String userId) {
        CompletableFuture<String> userFuture = fetchUser(userId);
        CompletableFuture<String> preferencesFuture = fetchPreferences(userId);

        return userFuture.thenCombine(
            preferencesFuture,
            (user, prefs) -> user + " | " + prefs
        );
    }

    // Exception handling
    public CompletableFuture<String> fetchUserSafely(String userId) {
        return fetchUser(userId)
            .exceptionally(ex -> {
                System.err.println("Failed to fetch user: " + ex.getMessage());
                return "DEFAULT_USER";
            });
    }

    // Wait for all futures to complete
    public void processAll(List<String> userIds) throws ExecutionException, InterruptedException {
        List<CompletableFuture<String>> futures = userIds.stream()
            .map(this::fetchUser)
            .collect(java.util.stream.Collectors.toList());

        CompletableFuture<Void> allDone = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        allDone.get(); // Blocks until all complete
        futures.forEach(f -> System.out.println(f.join())); // join() won't block — already done
    }

    // Wait for any future to complete (first result wins)
    public CompletableFuture<String> fetchFromFastestSource(String userId) {
        return CompletableFuture.anyOf(
            fetchFromPrimary(userId),
            fetchFromSecondary(userId)
        ).thenApply(result -> (String) result);
    }

    private String callUserService(String userId) { return "User:" + userId; }
    private CompletableFuture<String> fetchOrders(String user) {
        return CompletableFuture.supplyAsync(() -> "Orders for " + user, executor);
    }
    private CompletableFuture<String> fetchPreferences(String userId) {
        return CompletableFuture.supplyAsync(() -> "Prefs:" + userId, executor);
    }
    private CompletableFuture<String> fetchFromPrimary(String userId) {
        return CompletableFuture.supplyAsync(() -> "Primary:" + userId, executor);
    }
    private CompletableFuture<String> fetchFromSecondary(String userId) {
        return CompletableFuture.supplyAsync(() -> "Secondary:" + userId, executor);
    }
}
```

**Key distinction — `thenApply` vs `thenCompose`:**
- `thenApply(f)` where `f: T -> R`: use when the transform is a plain synchronous function
- `thenCompose(f)` where `f: T -> CompletableFuture<R>`: use when the next step is itself async; avoids `CompletableFuture<CompletableFuture<R>>`

---

### 6. Immutable Object Pattern

**Problem it solves:** Mutable shared state is the root of most concurrency bugs. If an object cannot be modified after construction, it can be shared between any number of threads without any synchronization.

```java
public final class ImmutablePoint {
    private final int x;
    private final int y;
    private final List<String> tags;

    public ImmutablePoint(int x, int y, List<String> tags) {
        this.x = x;
        this.y = y;
        // Defensive copy — do not store the caller's mutable list
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public List<String> getTags() { return tags; } // Already unmodifiable

    // "Mutation" returns a new instance instead
    public ImmutablePoint withX(int newX) {
        return new ImmutablePoint(newX, this.y, this.tags);
    }
}
```

**Rules for a properly immutable class:**
1. Class is `final` (prevents subclasses from adding mutable state)
2. All fields are `private final`
3. No setter methods
4. Mutable fields (arrays, collections) are defensively copied in the constructor and wrapped unmodifiable on return

**Why this matters for interviews:** If you make your value objects immutable, you eliminate a whole class of synchronization problems. An interviewer who sees you reach for immutability before reaching for locks will score you well.

---

## Part 3: LLD Problems with Concurrency Sub-Problems

---

### 1. Thread-Safe LRU Cache

**Approach A: ConcurrentHashMap + synchronized doubly linked list**

```java
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class LRUCacheV1<K, V> {

    private final int capacity;
    private final ConcurrentHashMap<K, Node<K, V>> map;
    private final DoublyLinkedList<K, V> list;
    private final ReentrantLock listLock = new ReentrantLock();

    public LRUCacheV1(int capacity) {
        this.capacity = capacity;
        this.map = new ConcurrentHashMap<>(capacity);
        this.list = new DoublyLinkedList<>();
    }

    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) return null;

        listLock.lock();
        try {
            list.moveToFront(node);
        } finally {
            listLock.unlock();
        }
        return node.value;
    }

    public void put(K key, V value) {
        listLock.lock();
        try {
            Node<K, V> existing = map.get(key);
            if (existing != null) {
                existing.value = value;
                list.moveToFront(existing);
                return;
            }
            Node<K, V> newNode = new Node<>(key, value);
            map.put(key, newNode);
            list.addToFront(newNode);
            if (map.size() > capacity) {
                Node<K, V> evicted = list.removeLast();
                map.remove(evicted.key);
            }
        } finally {
            listLock.unlock();
        }
    }

    static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev, next;
        Node(K key, V value) { this.key = key; this.value = value; }
    }

    static class DoublyLinkedList<K, V> {
        private final Node<K, V> head = new Node<>(null, null); // sentinel
        private final Node<K, V> tail = new Node<>(null, null); // sentinel

        DoublyLinkedList() {
            head.next = tail;
            tail.prev = head;
        }

        void addToFront(Node<K, V> node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
        }

        void moveToFront(Node<K, V> node) {
            remove(node);
            addToFront(node);
        }

        Node<K, V> removeLast() {
            if (tail.prev == head) return null;
            Node<K, V> last = tail.prev;
            remove(last);
            return last;
        }

        private void remove(Node<K, V> node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }
    }
}
```

**Approach B: LinkedHashMap with synchronized block** (simpler, same interview score)

```java
import java.util.*;

public class LRUCacheV2<K, V> {

    private final int capacity;
    // LinkedHashMap with access order = true: most recently accessed is last
    private final LinkedHashMap<K, V> cache;

    public LRUCacheV2(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public synchronized V get(K key) {
        return cache.getOrDefault(key, null);
    }

    public synchronized void put(K key, V value) {
        cache.put(key, value);
    }
}
```

**Comparison:**

| | V1 (ConcurrentHashMap + DLL) | V2 (LinkedHashMap + synchronized) |
|---|---|---|
| Reads | Can parallelize map reads | Serialized on synchronized |
| Writes | Always serialized (list lock) | Serialized on synchronized |
| Code complexity | High | Low |
| Interview recommendation | Use when interviewer asks about fine-grained locking | Use as default; upgrade if asked |

For most interviews, **V2 is the right answer to give first**. Mention V1 as an optimization when concurrent read throughput matters.

---

### 2. Token Bucket Rate Limiter (Thread-Safe)

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class TokenBucketRateLimiter {

    private final long maxTokens;
    private final long refillRatePerSecond;
    private final AtomicLong tokens;
    private volatile long lastRefillTimestampNanos;
    private final ScheduledExecutorService scheduler;

    public TokenBucketRateLimiter(long maxTokens, long refillRatePerSecond) {
        this.maxTokens = maxTokens;
        this.refillRatePerSecond = refillRatePerSecond;
        this.tokens = new AtomicLong(maxTokens);
        this.lastRefillTimestampNanos = System.nanoTime();

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-refill");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::refill, 1, 1, TimeUnit.SECONDS);
    }

    // Try to consume one token; returns true if allowed
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    public boolean tryAcquire(int permits) {
        while (true) {
            long current = tokens.get();
            if (current < permits) return false;
            if (tokens.compareAndSet(current, current - permits)) return true;
            // CAS failed — another thread updated tokens; retry
        }
    }

    // Block until a token is available (with timeout)
    public boolean acquire(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (tryAcquire()) return true;
            Thread.sleep(10); // Small backoff; in production use a condition variable
        }
        return false;
    }

    private void refill() {
        tokens.updateAndGet(current -> Math.min(maxTokens, current + refillRatePerSecond));
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
```

**Key design decisions:**
- `AtomicLong` with CAS loop avoids synchronized on the hot path
- `volatile` on `lastRefillTimestampNanos` ensures visibility across threads
- Refill runs on a separate scheduled thread, not inline on each request (avoids thundering herd)

---

### 3. Task Scheduler

```java
import java.util.concurrent.*;

public class TaskScheduler {

    private final ScheduledExecutorService executor;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> taskRegistry;

    public TaskScheduler(int threadCount) {
        this.executor = Executors.newScheduledThreadPool(threadCount);
        this.taskRegistry = new ConcurrentHashMap<>();
    }

    // Execute once after a delay
    public String scheduleOnce(Runnable task, long delayMs) {
        String taskId = java.util.UUID.randomUUID().toString();
        ScheduledFuture<?> future = executor.schedule(
            wrap(task, taskId), delayMs, TimeUnit.MILLISECONDS
        );
        taskRegistry.put(taskId, future);
        return taskId;
    }

    // Execute at a fixed rate regardless of task duration
    public String scheduleAtFixedRate(Runnable task, long initialDelayMs, long periodMs) {
        String taskId = java.util.UUID.randomUUID().toString();
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
            wrap(task, taskId), initialDelayMs, periodMs, TimeUnit.MILLISECONDS
        );
        taskRegistry.put(taskId, future);
        return taskId;
    }

    // Execute with fixed delay BETWEEN executions (delay starts after task completes)
    public String scheduleWithFixedDelay(Runnable task, long initialDelayMs, long delayMs) {
        String taskId = java.util.UUID.randomUUID().toString();
        ScheduledFuture<?> future = executor.scheduleWithFixedDelay(
            wrap(task, taskId), initialDelayMs, delayMs, TimeUnit.MILLISECONDS
        );
        taskRegistry.put(taskId, future);
        return taskId;
    }

    // Cancel by task ID
    public boolean cancel(String taskId) {
        ScheduledFuture<?> future = taskRegistry.remove(taskId);
        if (future == null) return false;
        return future.cancel(false); // false = don't interrupt if running
    }

    private Runnable wrap(Runnable task, String taskId) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                System.err.println("Task " + taskId + " failed: " + e.getMessage());
            }
        };
    }

    public void shutdown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
}
```

**`scheduleAtFixedRate` vs `scheduleWithFixedDelay`:**
- Fixed rate: period is measured from the *start* of the previous run. If the task takes longer than the period, the next run starts immediately after. Use for heartbeats, polling.
- Fixed delay: period is measured from the *end* of the previous run. Guarantees a gap between runs. Use for tasks that should not overlap.

---

### 4. Connection Pool

```java
import java.util.concurrent.*;

public class ConnectionPool {

    private final BlockingQueue<Connection> availableConnections;
    private final Semaphore semaphore;
    private final int maxConnections;

    public ConnectionPool(int maxConnections) {
        this.maxConnections = maxConnections;
        this.semaphore = new Semaphore(maxConnections, true); // fair = FIFO ordering
        this.availableConnections = new LinkedBlockingQueue<>(maxConnections);

        // Pre-create all connections
        for (int i = 0; i < maxConnections; i++) {
            availableConnections.offer(new Connection("conn-" + i));
        }
    }

    // Borrow a connection; blocks up to timeoutMs
    public Connection borrow(long timeoutMs) throws InterruptedException {
        if (!semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Connection pool exhausted; timeout after " + timeoutMs + "ms");
        }
        Connection conn = availableConnections.poll();
        if (conn == null) {
            semaphore.release(); // Should not happen, but safe to release
            throw new IllegalStateException("Semaphore acquired but no connection available");
        }
        return conn;
    }

    // Return a connection to the pool
    public void release(Connection conn) {
        if (conn == null) return;
        if (conn.isValid()) {
            availableConnections.offer(conn);
        } else {
            // Create a replacement if connection is broken
            availableConnections.offer(new Connection("conn-replacement-" + System.nanoTime()));
        }
        semaphore.release();
    }

    // Use pattern with try-finally to guarantee release
    public <T> T withConnection(java.util.function.Function<Connection, T> action) throws InterruptedException {
        Connection conn = borrow(5000);
        try {
            return action.apply(conn);
        } finally {
            release(conn);
        }
    }

    static class Connection {
        private final String id;
        private boolean valid = true;

        Connection(String id) { this.id = id; }
        public boolean isValid() { return valid; }
        public void invalidate() { this.valid = false; }
        public String getId() { return id; }
    }
}
```

**Why `Semaphore` + `BlockingQueue` together?**
- `Semaphore` enforces capacity: no more than `maxConnections` threads can hold a connection at once
- `BlockingQueue` provides the actual connection objects: O(1) `offer` and `poll`, no iteration needed
- This combination is cleaner than using `synchronized` on a list and more predictable than just `BlockingQueue.take()`

---

## Part 4: Common Interview Questions

---

### "What's the difference between `synchronized` and `ReentrantLock`?"

`synchronized` is a language keyword. The JVM handles lock acquisition and release automatically — you cannot forget to unlock. It is non-interruptible (a thread waiting on `synchronized` cannot be interrupted) and there is only one condition variable per object (`wait()`/`notify()`).

`ReentrantLock` is a class from `java.util.concurrent.locks`. It provides:
- **Interruptible lock acquisition** (`lockInterruptibly()`) — useful for deadlock detection and responsive shutdown
- **Timed lock acquisition** (`tryLock(timeout)`) — useful for avoiding deadlock or implementing timeouts
- **Multiple condition variables** (`newCondition()`) — allows waiting on different conditions without the thundering herd of `notifyAll()`
- **Fairness** — optional FIFO ordering of waiting threads

Rule of thumb: use `synchronized` when it is sufficient; reach for `ReentrantLock` when you need interruptibility, timeouts, or multiple conditions.

---

### "When would you use `volatile` vs `AtomicInteger`?"

`volatile` guarantees **visibility**: a write to a `volatile` variable is immediately visible to all threads that subsequently read it. It does **not** guarantee **atomicity** of compound operations.

Use `volatile` for:
- A flag field that one thread writes and others read (`volatile boolean running = true`)
- A reference that is reassigned atomically (the reference write itself is atomic on 64-bit JVMs)

`AtomicInteger` guarantees both visibility and atomic compound operations (read-modify-write). Use it when you need:
- Thread-safe increment: `counter.incrementAndGet()` — this cannot be done safely with `volatile int`
- Compare-and-set operations: `counter.compareAndSet(expected, update)`
- Accumulators with concurrent updates

The key distinction: `volatile int x; x++;` is **not** thread-safe. `AtomicInteger x; x.incrementAndGet();` is.

---

### "How do you prevent deadlock?"

Deadlock requires four conditions simultaneously (Coffman conditions): mutual exclusion, hold-and-wait, no preemption, and circular wait. Eliminating any one prevents deadlock.

In practice:

1. **Lock ordering**: always acquire multiple locks in the same global order. If thread A acquires lock L1 then L2, thread B must also acquire L1 then L2 — never L2 then L1.

2. **Lock timeouts**: use `tryLock(timeout)` instead of `lock()`. If you cannot acquire a lock within the timeout, release what you hold and retry. This breaks the hold-and-wait condition.

3. **Avoid holding locks while calling external code**: if you hold a lock and then call a method on an object you don't control, that method might acquire another lock.

4. **Use higher-level constructs**: `java.util.concurrent` classes are designed to avoid deadlock. Prefer them over hand-rolled synchronization.

5. **Lock-free data structures**: `ConcurrentHashMap`, `AtomicLong`, `CopyOnWriteArrayList` — no locks to deadlock on.

---

### "What is the happens-before relationship?"

The Java Memory Model (JMM) defines which memory actions in one thread are **guaranteed to be visible** to another thread. The happens-before relation is the formal mechanism for this.

Key happens-before rules:
- **Monitor lock**: an unlock of a monitor happens-before every subsequent lock of that monitor
- **Volatile write**: a write to a `volatile` field happens-before every subsequent read of that field
- **Thread start**: a call to `Thread.start()` happens-before any action in the started thread
- **Thread join**: all actions in a thread happen-before `Thread.join()` returns
- **Transitivity**: if A happens-before B and B happens-before C, then A happens-before C

**Practical meaning:** Without a happens-before relationship, the JVM and CPU are free to reorder instructions and cache values in registers. Code that appears to work in testing may fail in production because the JVM reordered a write and a read. `synchronized`, `volatile`, `java.util.concurrent` constructs all establish happens-before relationships that make your code provably correct under the JMM.
