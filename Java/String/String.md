# String, Equality, and Memory

---

## How String Storage Works - Pool vs Heap

```java
String n = "name";               // (1) literal — goes into String Pool
String m = new String("name");   // (2) forced heap object
```

### What JVM does for `String n = "name"`
1. Compiler embeds `"name"` as a constant in the `.class` file
2. At class load time, JVM looks in the String Pool for `"name"`
3. If not found → creates one String object in the pool, stores `n` pointing to it
4. If already there → reuses the existing one (no new object)

**Why check pool first?** String Pool is a cache for reuse. Since strings are immutable, sharing the same object between callers is safe - nobody can modify it. This saves memory: 1000 variables all holding `"name"` point to the same object.

### What JVM does for `String m = new String("name")`
1. JVM still finds or creates `"name"` in the String Pool (the literal is compiled in)
2. **Additionally** creates a brand-new String object on the heap copying those characters
3. `m` points to the heap object, not the pool

So `new String("name")` always results in **both** pool and heap having the value. The heap object is the one you're using; the pool one is a side-effect of the literal.

```
String Pool (Metaspace)         Heap
┌────────────┐                  ┌────────────┐
│ "name" ◄───┼──── n            │ "name" ◄───┼──── m
└────────────┘                  └────────────┘
     ▲
     └── also created as side-effect of new String("name")
```

### Why would you ever use `new String("name")`?

Almost never. Historical reasons: old APIs that needed a distinct object identity (not just value equality). In modern Java, always use literals.

---

## intern()

`intern()` bridges the heap object back to the pool.

```java
String m = new String("name");   // heap object
String pooled = m.intern();      // JVM: "name" already in pool? return that reference

System.out.println(pooled == n); // true — same pool object
System.out.println(m == n);      // false — m is still the heap object
```

**When to use:** You have millions of strings loaded from a file/DB with many duplicates, and memory is a concern. `intern()` deduplicates them into the pool. Overusing it pressures Metaspace (where the pool lives).

---

## Why String is Immutable

Once created, a String's character array can never change. Reasons:

1. **String Pool requires it**: multiple variables share the same pooled object. If `n` could change `"name"` to `"game"`, every other variable pointing to that pool entry would be affected silently.

2. **Thread safety**: immutable objects are safe to share across threads with no synchronization. A String can be passed between threads freely.

3. **Security**: Java uses String for class loading, file paths, database URLs, network connections. If you pass a filename to a security check and someone mutates it after validation but before the OS call, you have a vulnerability. Immutability closes that gap.

4. **hashCode caching**: String caches its computed hash in a field (computed once on first call, stored). This is only safe because the value never changes. HashMap/HashSet performance depends on stable hash codes.

```java
// Inside String class (conceptually):
private int hash;  // cached, starts at 0

public int hashCode() {
    int h = hash;
    if (h == 0) {
        h = computeHash(value);
        hash = h;         // safe to cache because value never changes
    }
    return h;
}
```

---

## toString(), hashCode(), and equals() on Objects

Every Java class inherits from `Object`. The defaults:

```java
// Object.toString() default:
getClass().getName() + "@" + Integer.toHexString(hashCode())
// e.g. "MyObj@5acf9800"
```

### Why does `@5acf9800` change every run?

The default `hashCode()` in `Object` returns a value derived from the **object's memory address** (via the JVM's internal identity hash mechanism). Each run, the JVM allocates objects at different addresses, so the hash — and therefore the hex in toString — changes.

It is NOT the raw memory address (Java abstracts that), but it is based on object identity. Two distinct objects always get different values; same object always returns same value within one run.

```java
MyObj ob  = new MyObj(3, 4);
MyObj ob2 = new MyObj(3, 4);

ob.toString();   // MyObj@5acf9800  ← different objects
ob2.toString();  // MyObj@7852e922  ← different addresses

ob2.toString().equals(ob.toString());  // false — different addresses
```

### When to Override hashCode

Override whenever you override `equals`. The contract:

> If `a.equals(b)` is `true`, then `a.hashCode() == b.hashCode()` MUST be `true`.

```java
class MyObj {
    int a, b;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MyObj other)) return false;
        return a == other.a && b == other.b;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);  // consistent with equals
    }
}
```

`Objects.hash(a, b)` combines the hashes of fields using a prime-number formula - same field values always produce the same code.

### What Breaks if You Override Only One

**Override `equals` but NOT `hashCode`:**
```java
MyObj k1 = new MyObj(3, 4);
MyObj k2 = new MyObj(3, 4);
k1.equals(k2);   // true — your custom equals says same

Map<MyObj, String> map = new HashMap<>();
map.put(k1, "hello");
map.get(k2);     // null — k2 hashes to a different bucket than k1
                 // HashMap never even compares equals — wrong bucket
```

HashMap first uses `hashCode()` to find the bucket, then `equals()` to find the key. If `hashCode` is inconsistent, `equals` never gets a chance to run.

**Override `hashCode` but NOT `equals`:**
```java
k1.hashCode() == k2.hashCode();  // true — same bucket
k1.equals(k2);   // false — Object.equals() compares references, not values
map.get(k2);     // null — same bucket but equals() says different
```

Both must be overridden together. IDEs generate them; `Objects.hash()` is the easiest correct pattern.

---

## The Code - Line by Line

```java
String n = "name";               // pool lookup/create, n → pool object
String m = new String("name");   // pool gets "name" (or reuses), + new heap object, m → heap

n == m        // false: different object references (pool vs heap)
n.equals(m)   // true:  same character sequence

MyObj ob  = new MyObj(3, 4);   // heap object at address A
MyObj ob2 = new MyObj(3, 4);   // heap object at address B

ob == ob2          // false: different references (A ≠ B)
ob.equals(ob2)     // true: your equals() compares fields a and b
                   // NOTE: the equals here takes MyObj, not Object
                   // — this is NOT overriding Object.equals, it's overloading it!
                   // (this is a subtle bug — see below)

ob.toString()      // MyObj@<hex of ob's identity hash>
ob2.toString()     // MyObj@<different hex> — different object
ob2.toString().equals(ob.toString())  // false — different hex strings
```

---

## StringBuilder and the Pool

```java
StringBuilder alpha = new StringBuilder();
for (char current = 'a'; current <= 'z'; current++)
    alpha.append(current);
System.out.println(alpha);
```

- `StringBuilder` is a **heap object**. Its internal buffer is a `char[]` on the heap.
- `append()` modifies the buffer in-place — no new String created per iteration.
- `System.out.println(alpha)` internally calls `alpha.toString()` — this creates **one** new String object on the heap with value `"abcdefghijklmnopqrstuvwxyz"`.
- That String is **not** added to the String Pool — it was created at runtime via `toString()`, not as a compile-time literal.

**StringBuffer** works identically for memory — same heap object, same `toString()` → heap String result. The only difference is StringBuffer's methods are `synchronized` (slower, for multi-thread use).

**Does GC run on the String Pool?** Pool objects are held by the JVM's internal references (class loader reference). They are only eligible for GC if the classloader that loaded the string is unloaded — which is rare for application code. In practice, pool entries live for the lifetime of the application. This is why blindly calling `intern()` on every string is dangerous: those strings never get collected.

---

## StringBuilder vs String vs StringBuffer

| | String | StringBuilder | StringBuffer |
|---|---|---|---|
| Mutable | No | Yes | Yes |
| Thread-safe | Yes (immutable) | No | Yes (synchronized) |
| Performance | Slow in loops | Fast | Slower than StringBuilder |
| Pool | Yes (literals) | No | No |
| toString() result | — | New heap String | New heap String |
| Use when | Value doesn't change | Building strings in single thread | Building strings across threads (rare) |

---

## Common Interview Questions

**Q: Why is String immutable in Java?**
A: Four reasons: (1) String pool requires it — pooled strings are shared; mutation would corrupt all references. (2) Thread safety — immutable objects need no synchronization. (3) Security — String is used for class loading, file paths, DB URLs; mutation after validation is a vulnerability. (4) hashCode caching — String caches its hash; safe only if the value never changes.

**Q: `String n = "name"` and `String m = new String("name")` — where does each live? What does `n == m` return?**
A: `n` points to a pool object in Metaspace. `m` points to a new heap object (the literal `"name"` also ends up in the pool as a side-effect, but `m` points to the heap copy). `n == m` is `false` (different objects). `n.equals(m)` is `true`.

**Q: What does `intern()` do? When would you use it?**
A: `intern()` returns the canonical pool reference for that string value. If `"name"` is already in the pool, you get that reference back. Used to deduplicate large sets of repeated strings loaded from external sources (files, DBs). Risk: pool entries are rarely GC'd — overuse causes Metaspace pressure.

**Q: Why does `ob.toString()` give a different value every run?**
A: Default `Object.toString()` is `className + "@" + Integer.toHexString(hashCode())`. The default `hashCode()` is based on the object's identity (derived from its JVM-internal address, which changes each run). Same object → same hash within one run. Different runs → different addresses → different hash.

**Q: When must you override hashCode? What breaks if you only override equals?**
A: Override `hashCode` whenever you override `equals` — they form a contract. If you override only `equals`: two objects that `equals()` considers the same will hash to different buckets in HashMap/HashSet. `get()`, `contains()` will return wrong results because HashMap uses `hashCode` to find the bucket before calling `equals`.

**Q: Does StringBuilder use the String Pool?**
A: No. `StringBuilder` is a heap object. Calling `toString()` on it creates a new String on the heap, not in the pool. The pool only stores compile-time string literals and strings explicitly `intern()`'d.

**Q: Does GC collect objects in the String Pool?**
A: Rarely. Pool objects are referenced by the JVM's internal class-loader structures and survive as long as the classloader is alive — effectively the program lifetime for most code. This is why careless use of `intern()` is a memory leak risk.
