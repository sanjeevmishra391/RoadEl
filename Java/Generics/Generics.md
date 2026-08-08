# Java Generics

> Generics enable type-safe code by parameterizing classes, interfaces, and methods with types. They eliminate casts and catch type errors at compile time.

---

## Why Generics

```java
// Without generics — runtime ClassCastException risk
List list = new ArrayList();
list.add("hello");
list.add(42);          // compiles — no type check
String s = (String) list.get(1);   // ClassCastException at runtime!

// With generics — compile-time safety
List<String> list = new ArrayList<>();
list.add("hello");
list.add(42);          // COMPILE ERROR — caught early
String s = list.get(0);   // no cast needed
```

---

## Generic Classes and Methods

```java
// Generic class
public class Pair<A, B> {
    private final A first;
    private final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() { return first; }
    public B getSecond() { return second; }
}

Pair<String, Integer> nameAge = new Pair<>("Alice", 30);

// Generic method — T is scoped to this method only
public <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
}

// Can be called with any Comparable type
max("apple", "banana");   // "banana"
max(3, 7);                // 7
```

---

## Type Erasure — The Most Important Concept

**At runtime, generic type information is erased.** `List<String>` and `List<Integer>` are both `List` at runtime.

```java
// At runtime these are identical — both just "List"
List<String> strings = new ArrayList<>();
List<Integer> ints = new ArrayList<>();

strings.getClass() == ints.getClass();   // true — both are ArrayList.class

// Consequence: cannot do instanceof with generic type
if (obj instanceof List<String>) { }    // COMPILE ERROR — can't check at runtime

// Can check the raw type
if (obj instanceof List<?>) { }         // OK
```

**What erasure prevents:**
```java
// Cannot create generic arrays
T[] arr = new T[10];           // COMPILE ERROR — type not known at runtime
T obj = new T();               // COMPILE ERROR — can't instantiate type parameter

// Workaround for array
Object[] arr = new Object[10];
T result = (T) arr[0];   // unchecked cast — unavoidable
```

---

## Wildcards — PECS Rule

### Upper Bound `<? extends T>` — Producer (read from)
```java
// Accepts List<Number>, List<Integer>, List<Double>
public double sum(List<? extends Number> list) {
    double total = 0;
    for (Number n : list) total += n.doubleValue();  // can READ as Number
    return total;
}

sum(new ArrayList<Integer>());   // works
sum(new ArrayList<Double>());    // works

// Cannot add to ? extends — compiler doesn't know the exact type
list.add(1.5);   // COMPILE ERROR — could be List<Integer>, adding Double would break it
```

### Lower Bound `<? super T>` — Consumer (write to)
```java
// Accepts List<Integer>, List<Number>, List<Object>
public void addNumbers(List<? super Integer> list) {
    list.add(1);    // can WRITE Integer (or subtype) — safe
    list.add(2);
}

addNumbers(new ArrayList<Integer>());   // works
addNumbers(new ArrayList<Number>());    // works
addNumbers(new ArrayList<Object>());    // works

// Cannot read specific type from ? super
Integer i = list.get(0);   // COMPILE ERROR — might be List<Object>, contains Object
Object o = list.get(0);    // OK — Object is always safe
```

### PECS — Producer Extends, Consumer Super

```
If a parameterized type produces T values → use <? extends T>
If a parameterized type consumes T values → use <? super T>
If both → use explicit <T>
If neither (just passing around) → use <?>
```

```java
// Classic example: Collections.copy
public static <T> void copy(List<? super T> dest,    // consumer — writes T
                             List<? extends T> src) { // producer — reads T
    for (T t : src) dest.add(t);
}
```

---

## Why `List<Dog>` is NOT a `List<Animal>`

```java
// Arrays are covariant — this compiles but can fail at runtime
Animal[] animals = new Dog[3];
animals[0] = new Cat();   // ArrayStoreException at runtime!

// Generics are INVARIANT — this is caught at compile time
List<Animal> animals = new ArrayList<Dog>();   // COMPILE ERROR — safer!

// Why? If this were allowed:
List<Dog> dogs = new ArrayList<>();
List<Animal> animals = dogs;   // hypothetically allowed
animals.add(new Cat());        // Cat in a List<Dog> → disaster
Dog d = dogs.get(0);          // ClassCastException
```

**Fix with wildcards:**
```java
List<? extends Animal> animals = new ArrayList<Dog>();   // OK — read-only view
```

---

## Bounded Type Parameters

```java
// T must implement Comparable<T>
public <T extends Comparable<T>> T min(List<T> list) {
    return list.stream().min(Comparator.naturalOrder()).orElseThrow();
}

// Multiple bounds — class first, then interfaces
public <T extends Animal & Serializable & Cloneable> void process(T t) { }

// T must extend a class AND implement an interface
public <T extends AbstractCache & Closeable> void flushAndClose(T cache) {
    cache.flush();
    cache.close();
}
```

---

## Common Interview Questions

**Q: What is type erasure and what does it prevent?**
A: At compile time, generic type parameters are erased and replaced with their bounds (or `Object`). This means `List<String>` and `List<Integer>` are the same `List` at runtime. Consequences: cannot do `instanceof List<String>`, cannot create `new T[]` or `new T()`, cannot have overloaded methods that differ only in generic type.

**Q: Why is `List<Dog>` not a `List<Animal>` even though `Dog extends Animal`?**
A: Generic types are invariant. If `List<Dog>` were assignable to `List<Animal>`, you could add a `Cat` to what is actually a `List<Dog>`. The compiler prevents this. Use `List<? extends Animal>` for a read-only covariant view.

**Q: What is PECS?**
A: Producer Extends, Consumer Super. Use `<? extends T>` when reading T values from the collection. Use `<? super T>` when writing T values into the collection.

**Q: Can you create a generic array like `T[] arr = new T[10]`?**
A: No — type erasure means `T` is unknown at runtime, so the JVM can't allocate a typed array. Workarounds: use `Object[]` with unchecked cast, or pass a `Class<T>` and use `Array.newInstance(clazz, size)`.

**Q: What's the difference between `List<?>` and `List<Object>`?**
A: `List<Object>` only accepts a `List<Object>` — not a `List<String>`. `List<?>` accepts any `List` of any type, but you can only read `Object` from it and cannot add anything (except `null`).

**Q: Can you overload a method that differs only by generic type, like `void process(List<String>)` and `void process(List<Integer>)`?**
A: No — type erasure makes both signatures identical at runtime (`void process(List)`). The compiler rejects this as a duplicate method. Workaround: use different method names, or add another distinguishing parameter.

**Q: Why does Java use erasure instead of reified generics (like C#)?**
A: Backward compatibility. Java 5 introduced generics into a language and JVM that already had millions of raw-type users. Erasure let generics be a compile-time feature with zero bytecode changes, so old code and new code could interoperate. The cost is the loss of runtime type information (`instanceof List<String>` impossible, no generic arrays).
