# Java 11–17 Modern Features

> Java 8 is what most interviews test. Java 11-17 features are asked as "what's new in modern Java?" — know them conceptually with one code example each.

---

## Java 10 — var (Local Variable Type Inference)

```java
// Before
HashMap<String, List<Integer>> map = new HashMap<String, List<Integer>>();

// After — type inferred by compiler (NOT dynamic typing)
var map = new HashMap<String, List<Integer>>();
var list = new ArrayList<String>();
var name = "Java";   // inferred as String

// Rules:
// - Only for LOCAL variables with initializer
// - Cannot use for method params, return types, fields
// - Cannot be null (compiler needs type to infer)
var x = null;  // DOES NOT COMPILE
```

---

## Java 11 — String New Methods

```java
String s = "  hello  ";

s.isBlank();          // true if empty or only whitespace (vs isEmpty() which only checks length)
s.strip();            // like trim() but handles Unicode whitespace
s.stripLeading();     // remove leading whitespace only
s.stripTrailing();    // remove trailing whitespace only

"line1\nline2\nline3".lines()   // Stream<String> of lines
    .forEach(System.out::println);

"abc".repeat(3);      // "abcabcabc"
```

---

## Java 14 — Records (Finalized in Java 16)

```java
// Before: DTO/POJO with boilerplate
class Point {
    private final int x;
    private final int y;
    public Point(int x, int y) { this.x = x; this.y = y; }
    public int x() { return x; }
    public int y() { return y; }
    // equals, hashCode, toString...
}

// After: Record — immutable data carrier
record Point(int x, int y) {}

// Automatically generated: constructor, accessors, equals, hashCode, toString
Point p = new Point(3, 4);
p.x();          // 3
p.toString();   // Point[x=3, y=4]

// Can add custom methods and compact constructor
record Range(int start, int end) {
    Range {  // compact constructor — validation
        if (start > end) throw new IllegalArgumentException();
    }
    int length() { return end - start; }
}
```

**Key points:**
- Implicitly `final` — cannot extend or be extended
- Fields are implicitly `private final`
- Great for DTOs, value objects, map keys

---

## Java 14 — Pattern Matching for instanceof (Finalized in Java 16)

```java
// Before
if (obj instanceof String) {
    String s = (String) obj;  // explicit cast
    System.out.println(s.length());
}

// After — binding variable
if (obj instanceof String s) {
    System.out.println(s.length());  // s is in scope here
}

// Works in expressions
String result = (obj instanceof Integer i) ? "Number: " + i : "Not a number";
```

---

## Java 17 — Sealed Classes

```java
// Sealed class: restricts which classes can extend it
public sealed class Shape permits Circle, Rectangle, Triangle {}

public final class Circle extends Shape { double radius; }
public final class Rectangle extends Shape { double width, height; }
public non-sealed class Triangle extends Shape {}  // open for further extension

// Why: exhaustiveness checking in switch (Java 21 pattern switch)
// Compiler knows exactly which subtypes exist
```

---

## Java 14 — Switch Expressions (Finalized in Java 14)

```java
// Old switch statement (fall-through, no return value)
int day = 3;
String name;
switch (day) {
    case 1: name = "Mon"; break;
    case 2: name = "Tue"; break;
    default: name = "Other";
}

// New switch expression (no fall-through, returns value)
String name = switch (day) {
    case 1 -> "Mon";
    case 2 -> "Tue";
    default -> "Other";
};

// Multi-line case with yield
String name = switch (day) {
    case 1 -> "Mon";
    case 6, 7 -> {
        System.out.println("Weekend!");
        yield "Weekend";   // yield = return from switch block
    }
    default -> "Weekday";
};
```

---

## Java 15 — Text Blocks

```java
// Before: ugly string concatenation
String json = "{\n" +
    "  \"name\": \"John\",\n" +
    "  \"age\": 30\n" +
    "}";

// After: Text block (triple quotes)
String json = """
    {
      "name": "John",
      "age": 30
    }
    """;

// Useful for: SQL, JSON, HTML in code
String sql = """
    SELECT id, name
    FROM users
    WHERE active = true
    ORDER BY name
    """;
```

---

## Java 9 — Interface Private Methods

```java
interface Validator {
    boolean validate(String input);

    default boolean validateWithLog(String input) {
        boolean result = validate(input);
        logResult(input, result);   // call private method
        return result;
    }

    private void logResult(String input, boolean result) {
        System.out.println(input + " → " + result);
    }
}
```

---

## Quick Reference — Feature by Version

| Version | Feature |
|---|---|
| Java 8 | Streams, Lambdas, Optional, Method References, Default methods |
| Java 9 | Modules, Interface private methods, `List.of()` / `Map.of()` |
| Java 10 | `var` |
| Java 11 | String methods (isBlank, strip, lines, repeat), `HttpClient` |
| Java 14 | `switch` expressions, `instanceof` pattern matching (preview) |
| Java 15 | Text blocks |
| Java 16 | Records (final), `instanceof` pattern matching (final) |
| Java 17 | Sealed classes, Random generators |

---

## Common Interview Questions

**Q: What's the difference between `var` and dynamic typing?**
A: `var` is still statically typed — the compiler infers the type at compile time. The type cannot change at runtime. Dynamic typing (like Python) determines types at runtime.

**Q: When would you use a Record over a regular class?**
A: For immutable data carriers (DTOs, value objects, return types with multiple values). Not suitable when you need mutable fields, inheritance, or custom serialization.

**Q: What problem do Sealed classes solve?**
A: They let you model closed hierarchies where you know all subtypes upfront. Enables exhaustive pattern matching and documents intent that the hierarchy is not meant to be extended externally.

**Q: Is `var` allowed in lambda parameters?**
A: Yes, since Java 11 — allows adding annotations: `(@NotNull var x) -> x.toUpperCase()`
