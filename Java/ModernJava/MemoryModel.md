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

```java
void example() {
    int x = 5;               // x is on the stack
    String s = new String("hi"); // s (reference) on stack, object on heap
}
// when method returns: x and s reference are popped from stack
// the String object remains on heap until GC runs
```

## Garbage Collection

### How GC Works (Generational Hypothesis)
Most objects die young — so the heap is split into generations:

```
Young Gen (Eden → S0 → S1) ──── promotion ────> Old Gen (Tenured)
   ↑ new objects born here                            ↑ long-lived objects
   ↑ Minor GC (fast, frequent)                        ↑ Major/Full GC (slow, rare)
```

1. New objects → **Eden**
2. Eden fills → **Minor GC** runs, survivors copied to **Survivor (S0/S1)**
3. After N GC cycles (default 15) → promoted to **Old Gen**
4. Old Gen fills → **Major GC** (stop-the-world pause)

### GC Algorithms (know these names)
| GC | When to Use | Key Trait |
|---|---|---|
| Serial GC | Single-threaded apps, small heaps | Simple, stop-the-world |
| Parallel GC | Throughput-focused (default Java 8) | Multiple GC threads |
| G1 GC | Low-latency, large heaps (default Java 9+) | Region-based, predictable pauses |
| ZGC / Shenandoah | Ultra-low latency | Sub-millisecond pauses |

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
