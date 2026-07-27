# Template Method Pattern

## Intent

Define the skeleton of an algorithm in a base class, deferring some steps to subclasses so that subclasses can redefine certain steps of an algorithm without changing the algorithm's overall structure.

---

## Problem

Without the Template Method pattern, teams copy-paste the same algorithm skeleton across multiple classes and diverge over time. A data migration pipeline always involves the same high-level steps (read, transform, write, clean up), but the implementation of each step differs by source/target. Without a shared skeleton, every migration class re-implements retry logic, logging, and error handling independently — a maintenance nightmare.

```java
// Anti-pattern: duplicated skeleton
public class CsvToDbMigration {
    public void migrate() {
        // Step 1 - read (same boilerplate every time)
        log.info("Starting migration");
        List<Record> data = readFromCsv(); // unique
        // Step 2 - transform (unique)
        List<DbRow> rows = transformCsv(data);
        // Step 3 - write (unique)
        writeToDb(rows);
        log.info("Migration complete");
    }
}

public class ApiToDbMigration {
    public void migrate() {
        // Exact same boilerplate, different implementations
        log.info("Starting migration");   // copy-pasted
        List<Record> data = callApi();
        List<DbRow> rows = transformApiResponse(data);
        writeToDb(rows);
        log.info("Migration complete");   // copy-pasted
    }
}
```

---

## Structure

```
+-----------------------------------------------+
|           AbstractDataMigration               |
|-----------------------------------------------|
| + migrate()           <<template method>>     |
| # readData()          <<abstract>>            |
| # transformData()     <<abstract>>            |
| # writeData()         <<abstract>>            |
| # onMigrationComplete() <<hook, empty default>>|
+-----------------------------------------------+
                  ^               ^
                  |               |
    +-------------+--+    +-------+---------+
    | CsvToDbMigration|    | ApiToDbMigration|
    |-----------------|    |-----------------|
    | # readData()    |    | # readData()    |
    | # transformData |    | # transformData |
    | # writeData()   |    | # writeData()   |
    | # onMigration.. |    +-----------------+
    |   Complete()    |
    +-----------------+
```

---

## Implementation

### Domain: Data Migration Framework

```java
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

// ---------------------------------------------------------------
// Abstract record types (domain model)
// ---------------------------------------------------------------
public class SourceRecord {
    private final Map<String, String> fields;

    public SourceRecord(Map<String, String> fields) {
        this.fields = fields;
    }

    public String get(String key) { return fields.getOrDefault(key, ""); }

    @Override
    public String toString() { return fields.toString(); }
}

public class TargetRecord {
    private final String id;
    private final String name;
    private final String email;

    public TargetRecord(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    @Override
    public String toString() {
        return "TargetRecord{id='" + id + "', name='" + name + "', email='" + email + "'}";
    }
}

// ---------------------------------------------------------------
// AbstractClass — DataMigrationTemplate
//
// The Hollywood Principle: "Don't call us, we'll call you."
// Subclasses do NOT call migrate() — migrate() calls them.
// Subclasses provide the primitive steps; the template method
// controls when and in what order they are invoked.
// ---------------------------------------------------------------
public abstract class DataMigrationTemplate {

    // TEMPLATE METHOD — final prevents subclasses from changing the skeleton
    public final void migrate() {
        System.out.println("[" + getClass().getSimpleName() + "] Starting migration...");
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Abstract — each subclass implements its own reader
            List<SourceRecord> rawData = readData();
            System.out.println("  Read " + rawData.size() + " source records.");

            // Step 2: Abstract — each subclass transforms differently
            List<TargetRecord> transformedData = transformData(rawData);
            System.out.println("  Transformed to " + transformedData.size() + " target records.");

            // Step 3: Abstract — each subclass writes to its own sink
            writeData(transformedData);
            System.out.println("  Write complete.");

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("  Duration: " + elapsed + "ms");

            // Step 4: Hook — optional post-migration logic (default: no-op)
            onMigrationComplete(transformedData.size());

        } catch (Exception e) {
            System.err.println("  Migration failed: " + e.getMessage());
            onMigrationFailed(e);
            throw new RuntimeException("Migration failed", e);
        }

        System.out.println("[" + getClass().getSimpleName() + "] Migration finished.\n");
    }

    // ---------------------------------------------------------------
    // Primitive operations — subclasses MUST implement these
    // ---------------------------------------------------------------

    /** Read raw data from the source system. */
    protected abstract List<SourceRecord> readData();

    /** Transform raw source records into target records. */
    protected abstract List<TargetRecord> transformData(List<SourceRecord> rawData);

    /** Write target records to the destination system. */
    protected abstract void writeData(List<TargetRecord> records);

    // ---------------------------------------------------------------
    // Hook methods — subclasses MAY override (default implementations
    // are intentionally empty, hence "hooks").
    //
    // Hook methods embody the Hollywood Principle: the framework
    // calls the hook; subclasses provide behavior without controlling
    // when the hook is invoked.
    // ---------------------------------------------------------------

    /**
     * Called after a successful migration.
     * Default: no-op. Override to send notifications, update audit tables, etc.
     */
    protected void onMigrationComplete(int recordCount) {
        // Hook: intentionally empty — override to add post-migration behavior
    }

    /**
     * Called when migration throws an exception.
     * Default: no-op. Override to send alerts or record failure metrics.
     */
    protected void onMigrationFailed(Exception e) {
        // Hook: intentionally empty
    }
}

// ---------------------------------------------------------------
// ConcreteClass 1 — CsvToDbMigration
// Reads from a CSV file, maps columns to DB fields, writes to DB.
// Overrides the onMigrationComplete hook to send a Slack alert.
// ---------------------------------------------------------------
public class CsvToDbMigration extends DataMigrationTemplate {

    private final String csvFilePath;
    private final String dbTableName;

    public CsvToDbMigration(String csvFilePath, String dbTableName) {
        this.csvFilePath = csvFilePath;
        this.dbTableName = dbTableName;
    }

    @Override
    protected List<SourceRecord> readData() {
        System.out.println("  CsvToDb: Reading from " + csvFilePath);
        // Simulated: in production, use OpenCSV or Jackson CSV
        List<SourceRecord> records = new ArrayList<>();
        records.add(new SourceRecord(Map.of("user_id", "1", "full_name", "Alice Smith", "email_addr", "alice@example.com")));
        records.add(new SourceRecord(Map.of("user_id", "2", "full_name", "Bob Jones",  "email_addr", "bob@example.com")));
        records.add(new SourceRecord(Map.of("user_id", "3", "full_name", "Carol White", "email_addr", "carol@example.com")));
        return records;
    }

    @Override
    protected List<TargetRecord> transformData(List<SourceRecord> rawData) {
        System.out.println("  CsvToDb: Mapping CSV columns to DB schema");
        List<TargetRecord> result = new ArrayList<>();
        for (SourceRecord r : rawData) {
            // CSV uses different field names than DB schema — map them here
            result.add(new TargetRecord(
                    r.get("user_id"),
                    r.get("full_name"),
                    r.get("email_addr")
            ));
        }
        return result;
    }

    @Override
    protected void writeData(List<TargetRecord> records) {
        System.out.println("  CsvToDb: Inserting " + records.size()
                + " rows into table '" + dbTableName + "'");
        records.forEach(r -> System.out.println("    INSERT: " + r));
    }

    // Override the hook — add custom post-migration behavior
    @Override
    protected void onMigrationComplete(int recordCount) {
        System.out.println("  CsvToDb Hook: Sending Slack notification — "
                + recordCount + " records migrated from " + csvFilePath);
    }
}

// ---------------------------------------------------------------
// ConcreteClass 2 — ApiToDbMigration
// Reads from a REST API, normalizes the response, writes to DB.
// Does NOT override onMigrationComplete (uses the no-op default).
// ---------------------------------------------------------------
public class ApiToDbMigration extends DataMigrationTemplate {

    private final String apiEndpoint;
    private final String authToken;

    public ApiToDbMigration(String apiEndpoint, String authToken) {
        this.apiEndpoint = apiEndpoint;
        this.authToken = authToken;
    }

    @Override
    protected List<SourceRecord> readData() {
        System.out.println("  ApiToDb: Calling endpoint " + apiEndpoint
                + " with auth token [REDACTED]");
        // Simulated HTTP GET + JSON parse
        List<SourceRecord> records = new ArrayList<>();
        records.add(new SourceRecord(Map.of("id", "101", "name", "Dave Brown", "contact", "dave@api.io")));
        records.add(new SourceRecord(Map.of("id", "102", "name", "Eve Davis",  "contact", "eve@api.io")));
        return records;
    }

    @Override
    protected List<TargetRecord> transformData(List<SourceRecord> rawData) {
        System.out.println("  ApiToDb: Normalizing API response fields");
        List<TargetRecord> result = new ArrayList<>();
        for (SourceRecord r : rawData) {
            result.add(new TargetRecord(
                    r.get("id"),
                    r.get("name"),
                    r.get("contact") // API uses "contact" instead of "email"
            ));
        }
        return result;
    }

    @Override
    protected void writeData(List<TargetRecord> records) {
        System.out.println("  ApiToDb: Bulk inserting " + records.size() + " records via JDBC batch");
        records.forEach(r -> System.out.println("    BATCH INSERT: " + r));
    }

    // No override of onMigrationComplete — accepts the no-op default hook
}

// ---------------------------------------------------------------
// Client / Demo
// ---------------------------------------------------------------
public class TemplateMethodDemo {

    public static void main(String[] args) {
        // Hollywood Principle in action:
        // We call migrate() — migrate() calls readData(), transformData(),
        // writeData(), and onMigrationComplete() on our behalf.
        // We never call those methods directly.

        DataMigrationTemplate csvMigration =
                new CsvToDbMigration("/data/users_export.csv", "users");
        csvMigration.migrate();

        DataMigrationTemplate apiMigration =
                new ApiToDbMigration("https://api.legacy.com/users", "Bearer secret-token");
        apiMigration.migrate();
    }
}
```

**Expected output:**
```
[CsvToDbMigration] Starting migration...
  CsvToDb: Reading from /data/users_export.csv
  Read 3 source records.
  CsvToDb: Mapping CSV columns to DB schema
  Transformed to 3 target records.
  CsvToDb: Inserting 3 rows into table 'users'
    INSERT: TargetRecord{id='1', name='Alice Smith', email='alice@example.com'}
    INSERT: TargetRecord{id='2', name='Bob Jones', email='bob@example.com'}
    INSERT: TargetRecord{id='3', name='Carol White', email='carol@example.com'}
  Write complete.
  Duration: 1ms
  CsvToDb Hook: Sending Slack notification — 3 records migrated from /data/users_export.csv
[CsvToDbMigration] Migration finished.

[ApiToDbMigration] Starting migration...
  ApiToDb: Calling endpoint https://api.legacy.com/users with auth token [REDACTED]
  Read 2 source records.
  ApiToDb: Normalizing API response fields
  Transformed to 2 target records.
  ApiToDb: Bulk inserting 2 records via JDBC batch
    BATCH INSERT: TargetRecord{id='101', name='Dave Brown', email='dave@api.io'}
    BATCH INSERT: TargetRecord{id='102', name='Eve Davis', email='eve@api.io'}
  Write complete.
  Duration: 1ms
[ApiToDbMigration] Migration finished.
```

---

### Template Method with Strategy (Replace Inheritance with Composition)

A limitation of Template Method is that changing behavior requires subclassing. You can replace inheritance with composition by injecting strategy objects for each step:

```java
@FunctionalInterface interface DataReader  { List<SourceRecord> read(); }
@FunctionalInterface interface DataTransformer { List<TargetRecord> transform(List<SourceRecord> r); }
@FunctionalInterface interface DataWriter  { void write(List<TargetRecord> records); }

public class ComposableMigration {

    private final DataReader reader;
    private final DataTransformer transformer;
    private final DataWriter writer;

    public ComposableMigration(DataReader reader,
                                DataTransformer transformer,
                                DataWriter writer) {
        this.reader = reader;
        this.transformer = transformer;
        this.writer = writer;
    }

    // The skeleton is now in a concrete class, not an abstract one
    public final void migrate() {
        List<SourceRecord> raw = reader.read();
        List<TargetRecord> transformed = transformer.transform(raw);
        writer.write(transformed);
    }
}

// Usage: inject lambdas at construction time
ComposableMigration migration = new ComposableMigration(
    () -> readFromCsv("/path/to/file.csv"),
    records -> records.stream().map(r -> new TargetRecord(r.get("id"), r.get("name"), r.get("email"))).toList(),
    rows -> rows.forEach(row -> System.out.println("Writing: " + row))
);
migration.migrate();
```

This is the **Strategy-based alternative** to Template Method. It avoids the combinatorial explosion of subclasses when multiple steps vary independently.

---

## The Hollywood Principle

"Don't call us, we'll call you."

The abstract class's `migrate()` method calls `readData()`, `transformData()`, and `writeData()` — these abstract methods are registered callbacks, not methods the client invokes directly. The subclass provides implementations but surrenders control of when they run to the framework (the abstract class). This inversion of control is the defining characteristic of the Template Method pattern and frameworks in general.

---

## When to Use

- When you have several classes with nearly identical algorithms that differ only in specific steps — extract the skeleton into an abstract class.
- When you want to control which parts of an algorithm subclasses can override (abstract for must-override, hook for may-override).
- When a framework needs to define the processing pipeline but leave implementation details to application code (the Hollywood Principle).
- When you need to enforce invariants around an algorithm (e.g., always log, always close resources) regardless of how subclasses implement the steps.

## When NOT to Use

- When the number of abstract steps that vary independently is large — subclass explosion makes this unmanageable. Prefer composition (Strategy) instead.
- When you need runtime algorithm selection — Template Method binds the algorithm skeleton at compile time through inheritance. Use Strategy for runtime flexibility.
- When subclasses differ so much that they share almost no common skeleton — inheritance is the wrong tool.
- When the Liskov Substitution Principle would be violated — if subclasses override hooks in ways that break the invariants the template method relies on.

---

## Variants

### 1. Hook Methods

Optional override points within the template. Declared with an empty (no-op) default implementation in the abstract class. Subclasses selectively override only the hooks they need. Examples: `onMigrationComplete()`, `beforeWrite()`, `afterRead()`.

### 2. Template Method with Strategy (Composition over Inheritance)

Replace abstract methods with injected `@FunctionalInterface` strategy objects. The skeleton moves from an abstract class to a concrete class with constructor-injected steps. Eliminates the need for subclassing and enables runtime reconfiguration.

---

## Real-World Usage

| API | Notes |
|---|---|
| `javax.servlet.HttpServlet` | `service()` is the template method. It dispatches to `doGet()`, `doPost()`, `doPut()`, etc., which you override. You never call `service()` directly — the servlet container calls it. |
| `org.springframework.jdbc.core.JdbcTemplate` | `query()`, `update()`, and `execute()` are template methods. They handle connection acquisition, statement preparation, exception translation, and resource cleanup. You provide only the SQL and a `RowMapper` (strategy). |
| `Spring AbstractController` | `handleRequest()` is the template method. `handleRequestInternal()` is the abstract step you override. Spring's MVC infrastructure calls `handleRequest()`. |
| `Spring Batch AbstractItemCountingItemStreamItemReader` | `doRead()` is the abstract step. The base class handles open/close/restart/skip count management via the template method `read()`. |

---

## Interview Questions

**Q: What is the difference between Template Method and Strategy? When do you prefer one over the other?**

Both patterns define an algorithm skeleton with interchangeable steps. The difference is the binding mechanism:

- **Template Method** uses **inheritance** (compile-time). The skeleton lives in an abstract base class. Steps are varied by subclassing. Changing behavior requires a new subclass.
- **Strategy** uses **composition** (runtime). The skeleton lives in a concrete context class. Steps are varied by injecting different strategy objects. Behavior can change at runtime without subclassing.

Prefer **Template Method** when: the skeleton is complex and benefits from being in a single class; steps share significant common state through `this`; you are building a framework where the calling convention must be enforced.

Prefer **Strategy** when: you need runtime algorithm switching; the number of step combinations would cause subclass explosion; you want to keep classes small and avoid inheritance hierarchies; you are in a language/style that prefers composition.

**Q: What is the Hollywood Principle and how does it relate to Template Method?**

"Don't call us, we'll call you." In the Template Method pattern, the subclass never calls the template method — the template method (in the abstract class) calls the subclass's overridden methods. The subclass registers implementations by overriding, and the framework decides when to invoke them. This is the foundation of all callback-based and IoC frameworks.

**Q: Why is the template method typically declared `final`?**

Declaring `migrate()` (or equivalent) `final` prevents subclasses from overriding the skeleton itself — only the abstract steps and hooks should be overridable. If a subclass could override the template method, it could bypass invariants (logging, error handling, resource cleanup) that the base class guarantees. `final` enforces the invariant.
