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

### How HashMap works — Step by Step

HashMap is backed by an **array of buckets** (Node[] table, default size 16). Each bucket can hold a chain of entries that hashed to the same index.

```
put("Alice", 30):

1. hash = "Alice".hashCode() ^ (hash >>> 16)   // bit-spread to reduce collisions
2. index = hash & (capacity - 1)               // e.g. hash & 15 → index 3
3. bucket[3] is empty → insert Node("Alice", 30) there

put("Bob", 25):                                // suppose also hashes to index 3

4. bucket[3] is not empty — collision!
5. traverse linked list: is any existing key .equals("Bob")? No.
6. append Node("Bob", 25) to the list

                bucket[3]
                ┌───────────────┐     ┌───────────────┐
                │ "Alice" → 30  │────▶│ "Bob"  → 25   │──▶ null
                └───────────────┘     └───────────────┘

get("Bob"):
1. compute hash → index 3
2. traverse bucket[3]: "Alice".equals("Bob")? No. "Bob".equals("Bob")? Yes.
3. return 25
```

**Java 8 optimization:** when a bucket's linked list exceeds **8 entries**, it converts to a **Red-Black Tree** → O(log n) lookup instead of O(n). Reverts to list if size drops below 6.

### Load Factor and Resizing
- Default load factor: **0.75** (resize when 75% full)
- On resize: capacity doubles, **all entries rehashed** into new array
- Resize is O(n) — expensive. Avoid by pre-sizing when you know the count:

```java
// Pre-size to avoid resizing for 100 entries
// formula: expectedSize / loadFactor → 100 / 0.75 ≈ 134 → next power of 2 = 256? → 128 is fine
Map<String, Integer> map = new HashMap<>(128);
```

### Why capacity must be a power of 2
```java
// Index computed as: hash & (capacity - 1)
// capacity=16: capacity-1 = 0b00001111
// This is a fast bitwise AND — only works correctly when capacity is power of 2
// If capacity were 10: capacity-1 = 0b00001001 → skips indices 2,4,6,8 → uneven distribution
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

## Map Utility Methods — Frequently Used in Practice

These come up constantly in coding rounds. Know them fluently.

```java
Map<String, Integer> wordCount = new HashMap<>();

// getOrDefault — avoid null check
int count = wordCount.getOrDefault("hello", 0);   // 0 if absent, no NPE

// putIfAbsent — insert only if key not present
wordCount.putIfAbsent("hello", 1);   // does nothing if "hello" already exists

// computeIfAbsent — compute and insert if absent (great for grouped structures)
Map<String, List<String>> groups = new HashMap<>();
groups.computeIfAbsent("fruits", k -> new ArrayList<>()).add("apple");
groups.computeIfAbsent("fruits", k -> new ArrayList<>()).add("banana");
// groups = {"fruits": ["apple", "banana"]}

// merge — combine existing value with new value
Map<String, Integer> freq = new HashMap<>();
for (String word : words) {
    freq.merge(word, 1, Integer::sum);   // if absent: put 1; if present: add 1 to existing
}
// equivalent to: freq.put(word, freq.getOrDefault(word, 0) + 1) — but cleaner

// compute — always update based on current value (even if absent)
map.compute("key", (k, v) -> v == null ? 1 : v + 1);
```

**Choosing the right method:**
| Situation | Method |
|---|---|
| Read with fallback default | `getOrDefault` |
| Insert only if not present | `putIfAbsent` |
| Initialize a nested structure | `computeIfAbsent` |
| Frequency counting / accumulation | `merge` |
| Any conditional update | `compute` |

## TreeMap — Sorted Map and Range Queries

```java
TreeMap<Integer, String> map = new TreeMap<>();
map.put(3, "three");
map.put(1, "one");
map.put(4, "four");
map.put(2, "two");

// Sorted iteration — always ascending by key
map.forEach((k, v) -> System.out.println(k + "=" + v));  // 1,2,3,4

// Navigation
map.firstKey();              // 1  (smallest)
map.lastKey();               // 4  (largest)
map.floorKey(3);             // 3  (≤ 3)
map.ceilingKey(3);           // 3  (≥ 3)
map.lowerKey(3);             // 2  (strictly < 3)
map.higherKey(3);            // 4  (strictly > 3)

// Range views — subMap, headMap, tailMap
map.subMap(2, true, 4, false);   // keys [2, 4) → {2, 3}
map.headMap(3);                  // keys < 3 → {1, 2}
map.tailMap(3);                  // keys ≥ 3 → {3, 4}
```

**When to choose TreeMap over HashMap:**
- Need keys in sorted order
- Need range queries (`subMap`, `headMap`, `tailMap`)
- Need floor/ceiling/nearest-key lookups
- Trade-off: O(log n) per operation vs O(1) for HashMap

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

## Iterator — Fail-Fast vs Fail-Safe

### Fail-Fast Iterators (ArrayList, HashMap, HashSet)
Throw `ConcurrentModificationException` if the collection is structurally modified during iteration.

```java
List<String> list = new ArrayList<>(List.of("a", "b", "c"));

// Wrong — throws ConcurrentModificationException
for (String s : list) {
    if (s.equals("b")) list.remove(s);   // modifying while iterating
}

// Correct — use Iterator.remove()
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("b")) it.remove();   // safe removal via iterator
}

// Also correct — Java 8+
list.removeIf(s -> s.equals("b"));
```

### Fail-Safe Iterators (ConcurrentHashMap, CopyOnWriteArrayList)
Iterate on a **snapshot** — no `ConcurrentModificationException`, but may see stale data.

```java
// CopyOnWriteArrayList — safe for concurrent iteration + occasional writes
List<String> list = new CopyOnWriteArrayList<>(List.of("a", "b", "c"));
for (String s : list) {
    list.add("d");   // no exception — iterates the original snapshot
}
// Use when: reads >> writes (event listener lists, observer lists)

// ConcurrentHashMap — iteration is weakly consistent
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
// Iterator reflects some-but-not-all concurrent updates — no CME
```

| | Fail-Fast | Fail-Safe |
|---|---|---|
| Examples | ArrayList, HashMap, HashSet | ConcurrentHashMap, CopyOnWriteArrayList |
| On modification | Throws ConcurrentModificationException | No exception |
| Data freshness | Real data | Snapshot (may be stale) |
| Performance | Better | Copy overhead (COW) or weaker guarantees |

## Immutable vs Unmodifiable Collections

```java
// Collections.unmodifiableList — wrapper, original list can still change
List<String> mutable = new ArrayList<>(List.of("a", "b"));
List<String> unmodifiable = Collections.unmodifiableList(mutable);
mutable.add("c");          // allowed — modifies original
unmodifiable.get(2);       // "c" — reflects the change!
unmodifiable.add("d");     // throws UnsupportedOperationException

// List.of() (Java 9+) — truly immutable, no backing mutable list
List<String> immutable = List.of("a", "b", "c");
immutable.add("d");        // throws UnsupportedOperationException
immutable.set(0, "x");     // throws UnsupportedOperationException
// Note: List.of() also rejects null elements

// Map.of() / Set.of() — same pattern
Map<String, Integer> map = Map.of("a", 1, "b", 2);
Set<String> set = Set.of("x", "y", "z");
```

| | `Collections.unmodifiableList()` | `List.of()` |
|---|---|---|
| Truly immutable | No (original can change) | Yes |
| Null elements | Allowed | Not allowed |
| Use when | Expose internal list read-only | Create a fixed, final list |

## Common Interview Questions

**Q: Walk me through what happens when you call `map.put("Alice", 30)` on a HashMap.**
A: (1) Compute `hash = "Alice".hashCode() ^ (hash >>> 16)` to spread bits. (2) Compute `index = hash & (capacity - 1)` to find the bucket. (3) If bucket is empty, insert a new Node there. (4) If bucket has entries (collision), traverse the linked list comparing keys with `equals()`. If a matching key is found, update the value. Otherwise, append a new Node. (5) If the linked list in that bucket exceeds 8 nodes, convert it to a Red-Black Tree for O(log n) lookup.

**Q: Why is HashMap not thread-safe?**
A: Two threads resizing simultaneously can corrupt the internal array — in Java 7 this caused an infinite loop (cyclic linked list); in Java 8, it causes lost updates. Even without resize, two threads can overwrite each other's puts. Use `ConcurrentHashMap` for thread-safe access.

**Q: What happens if two keys have the same hashCode?**
A: They land in the same bucket — a **collision**. HashMap chains them in a linked list (or tree). On `get`, it traverses the bucket using `equals()` to find the right key. Performance degrades from O(1) to O(n) with many collisions — O(log n) after Java 8 treeification.

**Q: What is the load factor and why is it 0.75 by default?**
A: Load factor = entries / capacity. When this ratio exceeds 0.75, the map resizes (doubles capacity, rehashes all entries). 0.75 is a balance: lower load factor means fewer collisions but more memory; higher means more collisions. 0.75 gives good time-space trade-off empirically.

**Q: Why must HashMap capacity be a power of 2?**
A: Bucket index is computed as `hash & (capacity - 1)`. This bitwise AND is a fast modulo operation — but it only distributes correctly when capacity is a power of 2 (capacity-1 is all 1-bits). With non-power-of-2 capacity, many indices would never be used, causing uneven distribution.

**Q: How would you implement a frequency counter for a list of words?**
A: `freq.merge(word, 1, Integer::sum)` or `freq.put(word, freq.getOrDefault(word, 0) + 1)`. The `merge` approach is cleaner and idiomatic.

**Q: When would you use `computeIfAbsent`?**
A: When building a map of lists/sets (grouping). `map.computeIfAbsent(key, k -> new ArrayList<>()).add(value)` — creates the list on first use and adds to it in one call, avoiding a null check.

**Q: TreeMap vs HashMap — when do you choose TreeMap?**
A: When you need keys in sorted order, range queries (`subMap`, `headMap`, `tailMap`), or floor/ceiling/nearest-key navigation. Trade-off: O(log n) per operation vs O(1) average for HashMap.

**Q: ArrayList vs LinkedList — which should you use?**
A: Default to ArrayList. It has O(1) random access and better cache locality. LinkedList only wins for frequent insertions/deletions at the head — but even then ArrayDeque is usually better. LinkedList uses ~3× more memory per element (object pointer + two node pointers).

**Q: What is the difference between fail-fast and fail-safe iterators?**
A: Fail-fast (ArrayList, HashMap) throw `ConcurrentModificationException` if the collection is modified during iteration — detected via a `modCount`. Fail-safe (ConcurrentHashMap, CopyOnWriteArrayList) iterate on a snapshot — no exception, but may see stale data. Fix for fail-fast: use `iterator.remove()` or `removeIf()` instead of direct `collection.remove()`.
