# Java Stream API — Advanced

> Streams are a pipeline of operations on a data source. Lazy evaluated — intermediate ops don't run until a terminal op is called.

---

## Stream Pipeline Structure

```
Source → [Intermediate ops (lazy)] → Terminal op (triggers execution)

List.of(1,2,3,4,5)
    .stream()              // source
    .filter(n -> n > 2)    // intermediate — lazy
    .map(n -> n * n)       // intermediate — lazy
    .collect(toList());    // terminal — triggers everything
```

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

## Common Interview Questions

**Q: What's the difference between map() and flatMap()?**
A: `map()` applies a function to each element (1-to-1). `flatMap()` applies a function that returns a stream per element, then flattens all streams into one (1-to-many).

**Q: Are streams reusable?**
A: No. Once a terminal operation is called, the stream is consumed and cannot be reused. Create a new stream from the source.

**Q: What's the difference between findFirst() and findAny()?**
A: `findFirst()` returns the first element in encounter order (deterministic). `findAny()` may return any element — faster in parallel streams because it doesn't need to maintain order.

**Q: When does stream processing actually start?**
A: When a terminal operation is called. Intermediate operations are lazy — they describe the pipeline but don't execute until needed.

**Q: What is a Collector?**
A: A reduction operation that folds stream elements into a mutable result container (List, Map, String, etc.). `Collectors.toList()`, `groupingBy()`, `joining()` are all Collectors.
