# Singleton Pattern

## Intent

Ensure a class has only one instance and provide a global point of access to it.

---

## Problem: What Breaks Without Singleton

Consider a `ConfigurationManager` that reads a config file on construction. Without Singleton:

```java
// Caller A
ConfigurationManager config1 = new ConfigurationManager(); // reads file, allocates memory

// Caller B (different class, different thread)
ConfigurationManager config2 = new ConfigurationManager(); // reads file AGAIN, different object

// Mutations on config1 are invisible to config2 — state is split
config1.set("timeout", "30");
System.out.println(config2.get("timeout")); // null — inconsistent state
```

Concrete failure modes:
- Multiple DB connection pools exhaust available connections
- Config changes in one part of the app are invisible elsewhere
- Expensive initialization (file I/O, network) repeated unnecessarily
- Shared mutable state with no single owner leads to race conditions

---

## Structure

```
+--------------------------------------------------+
|              ConfigurationManager                |
+--------------------------------------------------+
| - instance: ConfigurationManager  {static}      |
| - properties: Map<String, String>                |
+--------------------------------------------------+
| - ConfigurationManager()                         |
| + getInstance(): ConfigurationManager  {static}  |
| + get(key: String): String                       |
| + set(key: String, value: String): void          |
+--------------------------------------------------+
         |
         | (self-reference — holds own single instance)
         |
         +---> [single instance lives here]

Client ------> ConfigurationManager.getInstance() ----> [same object every time]
```

---

## Implementation

### Variant 1: Naive Singleton — BROKEN in Multithreading

```java
public class ConfigurationManager {

    private static ConfigurationManager instance; // not volatile — BROKEN
    private final Map<String, String> properties = new HashMap<>();

    private ConfigurationManager() {
        loadFromFile(); // expensive initialization
    }

    // Thread-unsafe: two threads can both see instance == null simultaneously
    // and each create their own instance. The second overwrites the first,
    // orphaning any state the first thread already wrote.
    public static ConfigurationManager getInstance() {
        if (instance == null) {              // Thread A and Thread B can both pass this check
            instance = new ConfigurationManager(); // Both create separate instances
        }
        return instance;
    }

    public String get(String key) { return properties.get(key); }
    public void set(String key, String value) { properties.put(key, value); }

    private void loadFromFile() {
        // simulate loading from config.properties
        properties.put("db.url", "jdbc:postgresql://localhost:5432/prod");
        properties.put("timeout", "5000");
    }
}
```

**Why it breaks:** Without synchronization or `volatile`, the JVM can reorder instructions.
Thread A might partially construct the object (allocate memory, assign reference) before
the constructor body finishes. Thread B sees a non-null reference and returns a
half-initialized object.

---

### Variant 2: Synchronized getInstance — CORRECT but SLOW

```java
public class ConfigurationManager {

    private static ConfigurationManager instance;
    private final Map<String, String> properties = new HashMap<>();

    private ConfigurationManager() {
        loadFromFile();
    }

    // synchronized guarantees only one thread enters at a time.
    // Correct, but every single call to getInstance() acquires a lock —
    // even after the instance is already created. Under high contention
    // this becomes a serialization bottleneck.
    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }

    public String get(String key) { return properties.get(key); }
    public void set(String key, String value) { properties.put(key, value); }

    private void loadFromFile() {
        properties.put("db.url", "jdbc:postgresql://localhost:5432/prod");
        properties.put("timeout", "5000");
    }
}
```

**Performance cost:** `synchronized` on a static method locks the `Class` object.
In a heavily-threaded application with millions of `getInstance()` calls, this is a
measurable bottleneck even though the critical section (object creation) executes
exactly once.

---

### Variant 3: Double-Checked Locking with volatile — CORRECT and FAST

```java
public class ConfigurationManager {

    // volatile is MANDATORY here. Without it, the JVM and CPU are allowed to
    // reorder the instructions inside the constructor relative to the assignment
    // of 'instance'. A thread could observe 'instance != null' but read from
    // an object whose fields are not yet fully initialized (partial construction).
    //
    // volatile enforces a happens-before relationship:
    //   - All writes inside the constructor complete BEFORE 'instance' is written.
    //   - Any thread that reads a non-null 'instance' sees a fully constructed object.
    //
    // Instruction reordering risk (without volatile):
    //   Normal new ConfigurationManager() compiles roughly to:
    //     1. Allocate memory for the object
    //     2. Assign reference to 'instance'   <-- CPU may do this BEFORE step 3
    //     3. Execute constructor body
    //   Thread B checks 'instance != null' after step 2 but before step 3:
    //   it gets a reference to an uninitialized object. volatile prevents this reordering.
    private static volatile ConfigurationManager instance;

    private final Map<String, String> properties = new HashMap<>();

    private ConfigurationManager() {
        loadFromFile();
    }

    public static ConfigurationManager getInstance() {
        if (instance == null) {                          // First check — no lock (fast path)
            synchronized (ConfigurationManager.class) { // Lock only when instance might be null
                if (instance == null) {                  // Second check — inside lock (safe)
                    instance = new ConfigurationManager();
                }
            }
        }
        return instance; // volatile read — guaranteed to see fully constructed object
    }

    public String get(String key) {
        return properties.get(key);
    }

    public void set(String key, String value) {
        properties.put(key, value);
    }

    private void loadFromFile() {
        properties.put("db.url", "jdbc:postgresql://localhost:5432/prod");
        properties.put("timeout", "5000");
        properties.put("max.connections", "20");
    }
}
```

**Why the double check?**  
The outer `if (instance == null)` avoids acquiring the lock on every call — the common
case after startup. The inner `if (instance == null)` handles the race where two threads
both pass the outer check before either acquires the lock: the second thread would
otherwise create a second instance.

---

### Variant 4: Enum Singleton — BEST (prefer this)

```java
public enum ConfigurationManager {

    INSTANCE; // JVM guarantees exactly one instance, created lazily on first access

    private final Map<String, String> properties = new HashMap<>();

    // Instance initializer block — runs once when the enum constant is first accessed
    {
        loadFromFile();
    }

    public String get(String key) {
        return properties.get(key);
    }

    public void set(String key, String value) {
        properties.put(key, value);
    }

    private void loadFromFile() {
        properties.put("db.url", "jdbc:postgresql://localhost:5432/prod");
        properties.put("timeout", "5000");
        properties.put("max.connections", "20");
    }
}

// Usage — no getInstance() needed, no nullability, no DCL boilerplate
class Application {
    public static void main(String[] args) {
        ConfigurationManager.INSTANCE.set("feature.flag", "enabled");
        System.out.println(ConfigurationManager.INSTANCE.get("db.url"));
    }
}
```

**Why enum is the best approach:**

1. **Thread safety is free.** The JVM class-loading mechanism guarantees that static
   initializers (including enum constants) are executed atomically. No `synchronized`,
   no `volatile` needed.

2. **Reflection-proof.** `java.lang.reflect.Constructor.newInstance()` throws
   `IllegalArgumentException` for enum types. Variants 1-3 can be broken via reflection
   by calling the private constructor.

3. **Serialization-proof.** If a class implements `Serializable`, Java's default
   deserialization creates a *new* object, breaking the singleton guarantee. Enums are
   automatically handled by the serialization spec — `readResolve()` is implicit and
   returns the existing enum constant.

4. **Minimal boilerplate.** No `getInstance()`, no `private static` field, no null checks.

**Limitation:** Enum classes cannot extend another class (they can implement interfaces).
If your singleton must inherit from a base class, use DCL (Variant 3) instead.

---

## When to Use

- Exactly one instance is required by contract: a single database connection pool,
  a single logger, a single event bus.
- The instance manages a shared resource where multiple instances would produce
  incorrect behavior (e.g., conflicting writes, double-initialization).
- The initialization is expensive (file I/O, network) and must happen exactly once.
- Global configuration state that must be consistent across the entire JVM process.

## When NOT to Use

- **As a substitute for dependency injection.** Singletons injected via DI containers
  (Spring `@Singleton`, Guice `@Singleton`) are testable. Hard-coded `getInstance()`
  calls create hidden dependencies that cannot be swapped in tests.
- **Shared mutable state across threads without explicit synchronization.** A singleton
  instance does not make its internal state thread-safe. The methods themselves must
  use concurrent data structures or locking.
- **Unit-tested code paths.** Classes that call `getInstance()` directly are impossible
  to unit-test in isolation without reflection hacks. This is the most common misuse.
- **Microservices or distributed systems.** A JVM-singleton is local to one process.
  Across services, you need a distributed coordination mechanism (Redis, ZooKeeper).
- **Any class whose behavior should vary per context** (e.g., per-request, per-tenant).

---

## Variants Summary

| Variant | Thread-safe | Lazy | Reflection-proof | Serialization-proof | Recommended |
|---|---|---|---|---|---|
| Naive | No | Yes | No | No | Never |
| Synchronized | Yes | Yes | No | No | Only for simplicity |
| DCL + volatile | Yes | Yes | No | No | Acceptable |
| Enum | Yes | Yes | Yes | Yes | Preferred |

---

## Real-World Examples

- **`java.lang.Runtime.getRuntime()`** — the JVM runtime environment is a classic
  singleton. Internally uses eager initialization (initialized at class load time).
- **`java.util.logging.Logger` hierarchy** — loggers are singletons per name,
  managed by `LogManager`.
- **Spring `ApplicationContext`** — in a Spring Boot app, beans declared as
  `@Scope("singleton")` (the default) follow the Singleton pattern within one
  application context.
- **HikariCP connection pool** — a single `HikariDataSource` is typically shared
  across the application. Creating multiple pools for the same database exhausts
  connections.
- **`System.in`, `System.out`, `System.err`** — the standard streams are effectively
  global singletons in a JVM process.

---

## Interview Questions

**Q1: Why is `volatile` necessary in double-checked locking? What goes wrong without it?**

Without `volatile`, the JVM's memory model permits out-of-order writes. The CPU or
JIT compiler may assign the reference to `instance` before the constructor body
has fully executed. A second thread can observe `instance != null` and return a
partially-initialized object. `volatile` introduces a happens-before relationship,
guaranteeing that all writes inside the constructor are visible before the reference
assignment becomes visible to other threads.

**Q2: You have a Singleton but your unit tests are failing because they cannot mock
the dependency. How do you fix this?**

This is the testability trap. The fix is to stop calling `getInstance()` directly
inside business logic. Instead, accept the dependency through a constructor or method
parameter. Register the singleton in a DI container. In tests, inject a mock or a
test-double. The singleton lifecycle is managed by the container, not hardcoded in
client code. The pattern stays; the tight coupling is eliminated.

**Q3: How does the enum singleton handle serialization differently from a regular
class-based singleton?**

For a regular class that implements `Serializable`, Java deserializes a fresh object
by bypassing the constructor, producing a second instance and breaking the singleton
guarantee unless `readResolve()` is manually implemented. For enum types, the Java
specification explicitly mandates that deserialization return the existing enum
constant with the same name. No `readResolve()` override is needed.
