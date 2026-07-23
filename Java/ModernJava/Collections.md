# Java Collections Internals

## Collections Hierarchy (Interview View)

```
Iterable
└── Collection
    ├── List       → ArrayList, LinkedList, Vector
    ├── Set        → HashSet, LinkedHashSet, TreeSet
    └── Queue      → PriorityQueue, ArrayDeque, LinkedList

Map (not Collection)
    → HashMap, LinkedHashMap, TreeMap, ConcurrentHashMap, Hashtable
```

## HashMap Internals — Most Asked

### How HashMap works
- Backed by an **array of buckets** (default 16)
- Key → `hashCode()` → index in array
- Each bucket holds a **linked list** of entries (for collisions)
- Java 8+: when a bucket's linked list exceeds **8 entries**, it converts to a **Red-Black Tree** → O(log n) instead of O(n)

```java
// Simplified internals
put(key, value):
  1. hash = key.hashCode() ^ (hash >>> 16)   // spread bits
  2. index = hash & (capacity - 1)            // fast modulo
  3. if bucket[index] is empty → insert
  4. if collision → traverse list, compare key equality
  5. if list length > 8 → convert to TreeNode (Red-Black Tree)

get(key):
  1. compute hash → index
  2. traverse bucket (list or tree)
  3. compare with .equals()
```

### Load Factor and Resizing
- Default load factor: **0.75** (resize when 75% full)
- On resize: capacity doubles, all entries **rehashed**
- Resize is expensive O(n) — avoid by setting initial capacity if size is known

```java
// Pre-size to avoid resizing for 100 entries
Map<String, Integer> map = new HashMap<>(128); // 100 / 0.75 ≈ 134 → round to power of 2
```

### HashMap vs Variants

| | HashMap | LinkedHashMap | TreeMap | ConcurrentHashMap | Hashtable |
|---|---|---|---|---|---|
| Order | None | Insertion order | Sorted (natural/comparator) | None | None |
| Null keys | 1 allowed | 1 allowed | Not allowed | Not allowed | Not allowed |
| Thread safe | No | No | No | Yes (segment locks) | Yes (full lock) |
| Performance | O(1) avg | O(1) avg | O(log n) | O(1) avg | O(1) but slow |
| Use when | Default | Need insertion order | Need sorted keys | Concurrent access | Legacy — avoid |

```java
// LinkedHashMap as LRU Cache
LinkedHashMap<Integer, Integer> lru = new LinkedHashMap<>(capacity, 0.75f, true) {
    protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
        return size() > capacity;
    }
};
// accessOrder=true means get() also counts as access → LRU eviction
```

## ArrayList vs LinkedList

| | ArrayList | LinkedList |
|---|---|---|
| Backed by | Dynamic array | Doubly linked list |
| get(i) | O(1) | O(n) |
| add at end | O(1) amortized | O(1) |
| add at middle | O(n) (shift) | O(1) if node known, O(n) to find |
| Memory | Less (contiguous) | More (node + 2 pointers) |
| Cache friendly | Yes | No |
| Use when | Random access, iteration | Frequent insert/delete at head/tail |

> **In practice:** ArrayList outperforms LinkedList in almost all cases due to cache locality. Default to ArrayList.

## HashSet, LinkedHashSet, TreeSet

All backed by their Map counterpart (value = dummy object):

```java
HashSet       → backed by HashMap        // O(1), no order
LinkedHashSet → backed by LinkedHashMap  // O(1), insertion order
TreeSet       → backed by TreeMap        // O(log n), sorted
```

## PriorityQueue — Min/Max Heap

```java
// Min-heap (default)
PriorityQueue<Integer> minHeap = new PriorityQueue<>();

// Max-heap
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());

// Custom comparator
PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]); // sort by first element

// Operations
pq.offer(val);   // insert  O(log n)
pq.poll();       // remove min/max  O(log n)
pq.peek();       // view min/max  O(1)
```

## ArrayDeque — Use Instead of Stack

```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(1);    // addFirst
stack.pop();      // removeFirst
stack.peek();     // peekFirst

Deque<Integer> queue = new ArrayDeque<>();
queue.offer(1);   // addLast
queue.poll();     // removeFirst
queue.peek();     // peekFirst
```

> **Never use `Stack<>` class** — it's legacy and synchronized. Use `ArrayDeque` instead.

## ConcurrentHashMap vs Collections.synchronizedMap

```java
// synchronizedMap: wraps entire map with one lock — whole map locked per operation
Map<K,V> synced = Collections.synchronizedMap(new HashMap<>());

// ConcurrentHashMap: segment-level locking (Java 7) → CAS operations (Java 8)
// Multiple threads can read/write different segments simultaneously
Map<K,V> concurrent = new ConcurrentHashMap<>();
```

| | synchronizedMap | ConcurrentHashMap |
|---|---|---|
| Locking | Entire map per op | Bucket/segment level |
| Read performance | Blocks all | Non-blocking reads |
| Null keys/values | Allowed | Not allowed |
| Iterator | Fail-fast | Weakly consistent (no ConcurrentModificationException) |
| Use when | Rarely used | High-concurrency read/write |

## Common Interview Questions

**Q: Why is HashMap not thread-safe?**
A: Two threads can simultaneously resize the map causing an infinite loop (Java 7) or lost updates (Java 8). Use ConcurrentHashMap in multithreaded code.

**Q: What happens if two keys have the same hashCode?**
A: They go into the same bucket — this is a **collision**. HashMap uses `equals()` to distinguish them in the bucket's linked list/tree.

**Q: What's the contract between hashCode() and equals()?**
A: If `a.equals(b)` is true, then `a.hashCode() == b.hashCode()` MUST be true. The reverse is not required (hash collisions are allowed).

**Q: Why initial capacity as power of 2?**
A: Index is computed as `hash & (capacity - 1)` — this is a fast bitwise operation that only works correctly when capacity is a power of 2.

**Q: TreeMap vs HashMap — when do you choose TreeMap?**
A: When you need keys in sorted order, range queries (`subMap`, `headMap`, `tailMap`), or floor/ceiling key lookups. HashMap is faster for everything else.
