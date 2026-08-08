# Java Stream API — Advanced

> Streams are a pipeline of operations on a data source. Lazy evaluated — intermediate ops don't run until a terminal op is called.

---

## Stream Pipeline Structure

```
Source → [Intermediate ops (lazy)] → Terminal op (triggers execution)

List.of(1,2,3,4,5)
    .stream()              // source
    .filter(n -> n > 2)    // intermediate — lazy, nothing runs yet
    .map(n -> n * n)       // intermediate — lazy, nothing runs yet
    .collect(toList());    // terminal — NOW everything runs
```

### How lazy evaluation works — trace through

```java
List.of(1, 2, 3, 4, 5)
    .stream()
    .filter(n -> { System.out.println("filter: " + n); return n > 2; })
    .map(n -> { System.out.println("map: " + n); return n * n; })
    .findFirst();   // terminal — stops as soon as one result found

// Output:
// filter: 1    ← filtered out
// filter: 2    ← filtered out
// filter: 3    ← passes filter
// map: 3       ← mapped immediately
// (stops — findFirst() has its result, 4 and 5 never processed)
```

This is why streams can be more efficient than for-loops for large datasets with `findFirst`/`anyMatch` — they short-circuit. With `collect(toList())`, all elements are processed.

## Creating Streams

```java
// From collection
list.stream()
list.parallelStream()      // multi-threaded processing

// From array
Arrays.stream(arr)
Stream.of(1, 2, 3)

// Infinite streams (must limit!)
Stream.iterate(0, n -> n + 2).limit(10)     // 0,2,4,6,...
Stream.generate(Math::random).limit(5)

// Range (IntStream, LongStream)
IntStream.range(0, 5)          // 0,1,2,3,4
IntStream.rangeClosed(1, 5)    // 1,2,3,4,5
```

---

## Intermediate Operations (Lazy)

```java
.filter(predicate)          // keep elements matching condition
.map(function)              // transform each element
.mapToInt/Long/Double()     // convert to primitive stream (avoids boxing)
.flatMap(function)          // flatten Stream<Stream<T>> to Stream<T>
.distinct()                 // remove duplicates (uses equals)
.sorted()                   // natural order
.sorted(comparator)         // custom order
.limit(n)                   // take first n elements
.skip(n)                    // skip first n elements
.peek(consumer)             // debug/side effect without consuming
```

### flatMap — Most Commonly Misunderstood

```java
// map gives Stream<Stream<String>> — wrong
List<List<String>> nested = List.of(List.of("a","b"), List.of("c","d"));
nested.stream().map(List::stream)       // Stream<Stream<String>>

// flatMap flattens to Stream<String> — correct
nested.stream().flatMap(List::stream)   // Stream<String>: a, b, c, d

// Real example: split sentences into words
List<String> sentences = List.of("hello world", "foo bar");
sentences.stream()
    .flatMap(s -> Arrays.stream(s.split(" ")))
    .collect(toList());  // [hello, world, foo, bar]
```

---

## Terminal Operations (Trigger Execution)

```java
.collect(collector)         // accumulate into collection or value
.forEach(consumer)          // iterate (no return)
.count()                    // number of elements
.findFirst()                // Optional<T> — first element
.findAny()                  // Optional<T> — any element (faster in parallel)
.anyMatch(predicate)        // boolean — any element matches
.allMatch(predicate)        // boolean — all elements match
.noneMatch(predicate)       // boolean — no elements match
.min(comparator)            // Optional<T>
.max(comparator)            // Optional<T>
.reduce(identity, accumulator)   // fold to single value
.toArray()                  // Object[]
```

---

## Collectors — The Power of collect()

```java
import static java.util.stream.Collectors.*;

// Basic
.collect(toList())
.collect(toSet())
.collect(toUnmodifiableList())

// Joining strings
.collect(joining(", "))              // "a, b, c"
.collect(joining(", ", "[", "]"))    // "[a, b, c]"

// groupingBy — most asked in interviews
Map<String, List<Employee>> byDept = employees.stream()
    .collect(groupingBy(Employee::getDepartment));

// groupingBy with downstream collector
Map<String, Long> countByDept = employees.stream()
    .collect(groupingBy(Employee::getDepartment, counting()));

Map<String, Double> avgSalaryByDept = employees.stream()
    .collect(groupingBy(Employee::getDepartment,
             averagingDouble(Employee::getSalary)));

// partitioningBy — split into true/false map
Map<Boolean, List<Integer>> partitioned = numbers.stream()
    .collect(partitioningBy(n -> n % 2 == 0));
// {true=[2,4,6], false=[1,3,5]}

// toMap
Map<String, Integer> nameToAge = people.stream()
    .collect(toMap(Person::getName, Person::getAge));

// toMap with merge function (handle duplicate keys)
Map<String, Integer> map = people.stream()
    .collect(toMap(Person::getName, Person::getAge,
             (existing, replacement) -> existing));  // keep first

// Counting, summing, averaging
.collect(counting())
.collect(summingInt(Employee::getSalary))
.collect(averagingInt(Employee::getSalary))
.collect(summarizingInt(Employee::getSalary))  // IntSummaryStatistics
```

---

## reduce — Fold to Single Value

```java
// sum
int sum = numbers.stream().reduce(0, Integer::sum);

// product
int product = numbers.stream().reduce(1, (a, b) -> a * b);

// max string length
Optional<String> longest = words.stream()
    .reduce((a, b) -> a.length() >= b.length() ? a : b);
```

---

## Parallel Streams

```java
list.parallelStream()
    .filter(...)
    .map(...)
    .collect(toList());

// Or convert mid-pipeline
list.stream()
    .filter(...)
    .parallel()
    .map(...)
    .collect(toList());
```

**When to use parallel:**
- Large data sets (>10k elements typically)
- CPU-bound operations (not I/O-bound)
- Operations are stateless and independent

**When NOT to use parallel:**
- Small collections (overhead of thread coordination > benefit)
- Ordered operations (parallel breaks ordering unless you use `forEachOrdered`)
- Shared mutable state (race conditions)

---

## Common Patterns

```java
// Top 3 highest salaries
employees.stream()
    .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
    .limit(3)
    .collect(toList());

// Distinct departments, sorted
employees.stream()
    .map(Employee::getDepartment)
    .distinct()
    .sorted()
    .collect(toList());

// Map of name → employee (fast lookup)
Map<String, Employee> byName = employees.stream()
    .collect(toMap(Employee::getName, Function.identity()));

// Flat list of all skills across all employees
employees.stream()
    .flatMap(e -> e.getSkills().stream())
    .distinct()
    .sorted()
    .collect(toList());
```

---

## Optional — Avoiding NullPointerException

`Optional<T>` is a container that either holds a value or is empty. Forces callers to explicitly handle the absent case.

```java
// Creating
Optional<String> present = Optional.of("hello");         // value must be non-null
Optional<String> empty   = Optional.empty();
Optional<String> nullable = Optional.ofNullable(getName()); // null → empty Optional

// Checking and extracting
optional.isPresent()          // true if value exists
optional.isEmpty()            // true if empty (Java 11+)
optional.get()                // returns value, throws NoSuchElementException if empty — avoid bare get()
optional.orElse("default")    // value or fallback
optional.orElseGet(() -> compute())  // value or lazily computed fallback
optional.orElseThrow(() -> new NotFoundException("not found"))

// Transforming (like stream operations)
Optional<Integer> len = optional.map(String::length);            // transform if present
Optional<String>  lower = optional.filter(s -> s.length() > 3); // empty if predicate fails
Optional<String>  flat = optional.flatMap(s -> findSomething(s)); // when your function returns Optional

// The right pattern: chain, don't unwrap
String result = findUser(id)
    .map(User::getEmail)
    .filter(email -> email.endsWith("@company.com"))
    .orElse("unknown");
```

**What NOT to do with Optional:**
```java
// Don't use Optional as a method parameter — use overloads instead
void process(Optional<String> name) { }  // bad — callers pass null to Optional param

// Don't use Optional for fields — use null or a sentinel value
class User { Optional<String> email; }  // bad — not serializable, not idiomatic

// Don't use bare get() — defeats the purpose
optional.get()  // if empty: NoSuchElementException — same as NPE, worse

// Do use it as a return type to signal possible absence
Optional<User> findById(int id) { ... }
```

**Optional in streams:**
```java
// Stream of Optionals → unwrap present ones
List<Optional<String>> opts = List.of(Optional.of("a"), Optional.empty(), Optional.of("b"));
List<String> values = opts.stream()
    .filter(Optional::isPresent)
    .map(Optional::get)
    .collect(toList());   // ["a", "b"]

// Java 9+: Optional.stream() for cleaner flatMap
List<String> values = opts.stream()
    .flatMap(Optional::stream)   // empty optionals contribute 0 elements
    .collect(toList());
```

---

## Common Interview Questions

**Q: Explain how lazy evaluation works in streams.**
A: Intermediate operations (`filter`, `map`, `sorted`) don't execute when called — they build a pipeline description. Execution only starts when a terminal operation (`collect`, `findFirst`, `count`) is invoked. Each element flows through the entire pipeline before the next element starts. This enables short-circuiting: `findFirst()` stops as soon as one element passes all filters, so elements after it are never processed.

**Q: What's the difference between map() and flatMap()?**
A: `map()` is 1-to-1: each element produces exactly one output element. `flatMap()` is 1-to-many: each element produces a stream, and all those streams are merged into one. Use `flatMap` when your transform returns a collection or stream (e.g., splitting sentences into words, unwrapping nested lists).

**Q: Are streams reusable?**
A: No. A stream can only be consumed once. After a terminal operation, the stream is closed — calling any operation on it throws `IllegalStateException`. Always create a new stream from the source.

**Q: What's the difference between findFirst() and findAny()?**
A: Both return `Optional<T>`. `findFirst()` always returns the first element in encounter order (deterministic). `findAny()` returns any element — in parallel streams it's faster because threads don't need to coordinate order. In sequential streams they behave identically.

**Q: When would you use reduce() vs collect()?**
A: `reduce()` folds a stream into a single immutable value (sum, max, concatenation). `collect()` accumulates into a mutable container (List, Map, String via joining). For building collections, always use `collect` — using `reduce` to build a list would create a new list copy per element (O(n²)).

**Q: How does groupingBy work?**
A: `Collectors.groupingBy(classifier)` builds a `Map<K, List<T>>` where keys are the result of the classifier function. Each value is a list of elements that produced that key. Combine with a downstream collector to aggregate: `groupingBy(f, counting())` gives `Map<K, Long>`, `groupingBy(f, averagingInt(g))` gives `Map<K, Double>`.

**Q: When should you NOT use parallel streams?**
A: (1) Small collections — thread coordination overhead outweighs benefit. (2) I/O-bound operations — threads block waiting; more threads don't help. (3) Operations with shared mutable state — race conditions. (4) Operations that need ordered output — parallel breaks encounter order unless you add `forEachOrdered` (which eliminates the performance gain). Default to sequential; switch to parallel only after profiling shows it helps.

**Q: What is Optional and when should you use it?**
A: `Optional<T>` is a container that either holds a value or is empty. Use it as a return type to make potential absence explicit — the caller is forced to handle it. Don't use it as a field type (not serializable), method parameter (pass null or use overloads), or with `get()` without checking (defeats the purpose). Correct use: chain with `map`, `filter`, `orElse`, `orElseThrow`.
