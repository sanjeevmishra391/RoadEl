# Java Memory Model

## JVM Memory Areas

```
┌─────────────────────────────────────────────────┐
│                     JVM                          │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │  Stack   │  │   Heap   │  │ Metaspace     │  │
│  │(per thread)│ │(shared) │  │(class metadata│  │
│  └──────────┘  └──────────┘  └───────────────┘  │
│  ┌──────────────────────────────────────────┐   │
│  │         PC Register (per thread)         │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

### Stack (per thread)
- Stores: local variables, method call frames, references (not objects)
- Each method call creates a new **stack frame**
- LIFO — frame is pushed on call, popped on return
- **StackOverflowError** = too many nested calls (infinite recursion)
- Fast access — no GC involvement

### Heap (shared across threads)
- Stores: all objects (`new SomeClass()`), instance variables, arrays
- Managed by **Garbage Collector**
- Divided into: **Young Generation** (Eden + Survivor) + **Old Generation** (Tenured)
- **OutOfMemoryError** = heap is full, GC cannot reclaim enough

### Metaspace (Java 8+ — replaced PermGen)
- Stores: class metadata, method bytecode, static variables
- Grows dynamically (no fixed size like old PermGen)
- Can still throw OutOfMemoryError if unconstrained

## Stack vs Heap — Quick Reference

| | Stack | Heap |
|---|---|---|
| Stores | Primitives, references | Objects, arrays |
| Lifetime | Until method returns | Until GC collects |
| Thread safety | Thread-local (safe) | Shared (needs sync) |
| Speed | Faster | Slower |
| Size | Small (~512KB–1MB) | Large (configured by -Xmx) |

### Trace Through Memory — What Lives Where

```java
void example() {
    int x = 5;                    // x → stack (primitive)
    String s = "hello";           // s (reference) → stack, "hello" object → String Pool (Metaspace)
    User u = new User("Alice");   // u (reference) → stack, User object → heap
    u.age = 30;                   // age field lives inside the User object on heap
}
// method returns: x, s, u are all popped off the stack
// "hello" stays in String Pool (long-lived)
// User("Alice") is now unreachable → eligible for GC
```

```java
class Counter {
    static int count = 0;    // count → Metaspace (class-level)
    int id;                  // id field → heap (inside each Counter instance)
}

Counter c1 = new Counter();  // c1 reference → stack, Counter object → heap
Counter c2 = new Counter();  // c2 reference → stack, another Counter object → heap
// Counter.count is shared — one copy in Metaspace, visible to all instances
```

## Memory Leaks in Java

Java has GC, but memory leaks still happen — they occur when objects are **reachable but no longer needed**.

### Common leak patterns

**1. Static collections that grow forever**
```java
class Cache {
    static Map<String, byte[]> store = new HashMap<>();  // static = lives as long as class

    public static void cache(String key, byte[] data) {
        store.put(key, data);  // grows forever — nothing ever removes entries
    }
}
```

**2. Listeners / callbacks never deregistered**
```java
button.addActionListener(myListener);  // holds reference to myListener
// if you never call removeActionListener, myListener (and everything it references) can't be GC'd
// Fix: use WeakReference, or explicitly remove when done
```

**3. ThreadLocal not cleaned up in thread pools**
```java
ThreadLocal<byte[]> buffer = new ThreadLocal<>();
// In a thread pool, threads are reused
// If you set a value and never call remove(), it stays for the thread's lifetime
// Fix: always call buffer.remove() in a finally block
```

**4. Inner class holds outer class reference**
```java
class Outer {
    byte[] bigData = new byte[1024 * 1024];

    class Inner implements Runnable {  // non-static inner class
        public void run() { /* uses bigData */ }
    }
}
// If you submit new Outer().new Inner() to an ExecutorService,
// Inner holds a reference to Outer, so bigData can't be GC'd until the task finishes
// Fix: make Inner static (loses Outer reference), or extract it
```

**How to detect:** heap dump analysis with tools like VisualVM, Eclipse MAT, or `-XX:+HeapDumpOnOutOfMemoryError`.

## Garbage Collection

### Generational Hypothesis

Most objects die young — allocated, used briefly, then abandoned. GC exploits this by splitting the heap:

```
┌──────────────────────────────────────────────────────────┐
│                         HEAP                             │
│  ┌─────────────────────────────┐   ┌──────────────────┐ │
│  │       Young Generation      │   │  Old Generation  │ │
│  │  ┌────────┐ ┌────┐ ┌────┐  │   │   (Tenured)      │ │
│  │  │  Eden  │ │ S0 │ │ S1 │  │   │  long-lived objs │ │
│  │  └────────┘ └────┘ └────┘  │   └──────────────────┘ │
│  └─────────────────────────────┘                        │
└──────────────────────────────────────────────────────────┘
```

### Object Lifecycle

**Step 1 — Born in Eden:**
New objects (`new MyObj(...)`) are allocated in Eden. Eden fills up fast.

**Step 2 — Minor GC (Young Gen collection):**
When Eden is full, a Minor GC runs:
- Live objects from Eden are copied to the active Survivor space (say S0)
- Dead objects in Eden are discarded (no copying — just mark and discard)
- Minor GC is fast (milliseconds) because only Young Gen is scanned

**Step 3 — S0 ↔ S1 (why objects bounce between survivor spaces):**
Java uses a **copying collector** for Young Gen. Only one Survivor space is "active" at a time; the other is always empty.

- Minor GC: copy live objects from Eden + S0 → S1. Wipe S0 entirely.
- Next Minor GC: copy live objects from Eden + S1 → S0. Wipe S1.
- They alternate every cycle.

**Why copy rather than mark-in-place?**
After a copy, all live objects are contiguous — zero fragmentation. New allocation is just incrementing a pointer (extremely fast). Mark-in-place leaves holes that require a free-list, causing fragmentation and slower allocation over time.

Each object tracks its **age** (number of GC cycles survived).

**Step 4 — Promotion to Old Gen:**
When age ≥ threshold (default 15 cycles) → promoted to Old Generation. Also promoted immediately if Survivor spaces are too small to hold it.

**Step 5 — Major/Full GC:**
When Old Gen fills up:
- Scans the entire heap including Old Gen
- Stop-the-world pause — all application threads halt
- Much slower (seconds for large heaps)

```
new obj → Eden → [Minor GC] → S0 → [Minor GC] → S1 → ... → Old Gen
                   fast, ms                              slow, secs if Old fills
```

### GC Algorithms

| Algorithm | Default | Pause | Best For |
|---|---|---|---|
| Serial GC | Single-threaded apps, small heaps | Stop-the-world | Tiny heaps |
| Parallel GC | Java 8 default | Stop-the-world (parallel threads) | Batch/throughput workloads |
| G1 GC | Java 9+ default | Short, predictable | Large heaps, balanced latency |
| ZGC | Java 15+ (production) | Sub-millisecond | Ultra-low latency |
| Shenandoah | Alternative to ZGC | Sub-millisecond | Ultra-low latency |

### G1 GC — How It Works

G1 ("Garbage First") divides the entire heap into equal-sized **regions** (typically 1–32 MB each) instead of a fixed Young/Old layout:

```
┌────┬────┬────┬────┬────┬────┬────┬────┐
│ E  │ S  │ O  │ O  │ E  │ O  │ S  │ E  │
└────┴────┴────┴────┴────┴────┴────┴────┘
 E=Eden  S=Survivor  O=Old   (assigned dynamically)
```

- G1 tracks how much garbage is in each region (via a Remembered Set)
- Collects regions with **most garbage first** → hence "Garbage First"
- You set a pause target (`-XX:MaxGCPauseMillis=200`); G1 picks how many regions to collect per cycle to hit it
- Concurrent marking runs in the background while the app runs
- **Mixed collections**: G1 can collect Young + selected Old regions in one cycle
- Avoids Full GC in normal operation (unlike Parallel GC)

**Why better for large heaps:** predictable pause times, no single stop-the-world Old Gen sweep, background concurrent work.

### How to trigger GC (suggestion only — JVM decides)
```java
System.gc();           // suggests GC, not guaranteed
Runtime.getRuntime().gc();  // same
```

## Reference Types — The 4 Levels

```java
// Strong Reference — default, object never GC'd while reachable
Object strong = new Object();

// Soft Reference — GC'd only when memory is low (good for caches)
SoftReference<Object> soft = new SoftReference<>(new Object());

// Weak Reference — GC'd at next GC cycle (good for listeners, canonicalized maps)
WeakReference<Object> weak = new WeakReference<>(new Object());

// Phantom Reference — GC'd, but notification sent before memory reclaimed (cleanup)
PhantomReference<Object> phantom = new PhantomReference<>(new Object(), referenceQueue);
```

| Type | GC'd When | Use Case |
|---|---|---|
| Strong | Never (while reachable) | Default |
| Soft | Low memory | In-memory caches |
| Weak | Next GC cycle | WeakHashMap, listeners |
| Phantom | After finalization | Cleanup actions |

## GC Tuning — Reducing Request Latency

GC pauses directly cause latency spikes. In order of impact:

**1. Reduce allocation rate (most impactful — fix the source)**
```java
// Avoid creating objects in hot paths
// Bad: new object per request
String key = "user-" + id;   // allocates in every request

// Better: pre-format, reuse, or use StringBuilder with a pool
```

**2. Right-size the heap**
```bash
-Xms4g -Xmx4g    # set min=max — prevents resize-triggered GC and heap thrashing
# Too small → GC runs too often
# Too large → when Full GC does run, it takes longer
```

**3. Tune Young Gen size**
```bash
-XX:NewRatio=2          # Old:Young = 2:1 → Young is 1/3 of heap (default)
-XX:NewSize=2g          # explicit size
# Larger Young Gen → objects die in Minor GC before being promoted
# → less pressure on Old Gen → fewer Full GCs
```

**4. Switch GC algorithm**
```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100    # G1 targets 100ms max pause

-XX:+UseZGC                 # ZGC: sub-millisecond pauses, Java 15+
                            # best for latency-critical services
```

**5. Diagnose before tuning**
```bash
-Xlog:gc*                   # Java 9+ unified GC logging
# Look at: GC frequency, pause duration, heap usage after GC
# If Minor GC is too frequent → increase Young Gen
# If Full GC is frequent → find what's being promoted (object lifetime leak)
```

## Common Interview Questions

**Q: Where are static variables stored?**
A: In **Metaspace** (class-level data), not heap. But objects they reference are on the heap.

**Q: Can stack memory cause memory leaks?**
A: No — stack frames are automatically cleaned up when methods return. Memory leaks come from heap objects with lingering strong references.

**Q: What causes OutOfMemoryError: GC overhead limit exceeded?**
A: JVM is spending more than 98% of time doing GC and recovering less than 2% of heap. Usually means objects are accumulating faster than GC can reclaim.

**Q: What's the difference between Minor GC and Full GC?**
A: Minor GC collects only Young Generation (fast, milliseconds). Full GC collects entire heap including Old Gen (slow, seconds, stop-the-world).

**Q: How does GC know an object is unreachable?**
A: **Reachability analysis** — starting from GC roots (stack frames, static variables, JNI references), trace all reachable objects. Anything not reachable is eligible for collection. Java does NOT use reference counting (Python does).

**Q: Why do objects bounce between S0 and S1 instead of staying in one Survivor space?**
A: GC uses a copying collector. After each Minor GC, live objects are copied from Eden + the active Survivor into the empty Survivor, then the active one is wiped entirely. This keeps one Survivor always empty, enabling pointer-bump allocation (extremely fast) and eliminates fragmentation. The two spaces alternate roles each cycle.

**Q: What is G1 GC and why is it preferred over Parallel GC?**
A: G1 splits the heap into equal-sized regions instead of fixed generations. It collects regions with the most garbage first (predictable pause duration, configurable via `-XX:MaxGCPauseMillis`). Parallel GC is throughput-optimized but causes unpredictable stop-the-world Full GC pauses on large heaps. G1 avoids Full GC in normal operation.

**Q: How would you reduce GC-related request latency in a Java service?**
A: In order: (1) Reduce object allocation rate in hot paths — less garbage = less GC. (2) Set `-Xms == -Xmx` to avoid resize pauses. (3) Increase Young Gen size to let objects die before promotion. (4) Switch to G1 with a pause target, or ZGC for sub-millisecond pauses. (5) Profile with GC logs before tuning — identify whether the problem is Minor GC frequency or Full GC.

**Q: Does GC collect objects in the String Pool?**
A: Rarely. String pool entries are referenced by internal JVM structures tied to the classloader. They survive as long as the classloader is alive — effectively for the lifetime of the application. This is why over-using `intern()` can cause a Metaspace memory leak.

**Q: Can Java have memory leaks even with garbage collection?**
A: Yes — when objects are still reachable but no longer needed. Common causes: (1) Static collections that grow without bound. (2) Event listeners never deregistered — the listener (and everything it references) stays alive. (3) ThreadLocal values not removed in thread pools — threads are reused, so the value persists indefinitely. (4) Non-static inner classes holding an implicit reference to the outer class. Detect with heap dump analysis (`-XX:+HeapDumpOnOutOfMemoryError`).

**Q: What's the difference between PermGen and Metaspace?**
A: PermGen (pre-Java 8) was a fixed-size heap region storing class metadata. It caused `OutOfMemoryError: PermGen space` if too many classes were loaded. Metaspace (Java 8+) stores the same data but in native memory (off-heap), grows dynamically, and is only bounded by available system memory. You can still cap it with `-XX:MaxMetaspaceSize`.
