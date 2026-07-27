# Proxy Pattern

## Intent

Provide a surrogate or placeholder for another object to control access to it.

---

## Problem: Controlling Object Access

You have a client that needs to interact with a service, but:
- The service is expensive to create (virtual proxy)
- The client might not have permission to use the service (protection proxy)
- The service lives on another machine (remote proxy)
- You want to intercept all calls for logging/metrics without changing client or service code (dynamic proxy)

Changing the client to handle all these concerns violates the Single Responsibility Principle and creates tight coupling. The proxy sits between the client and the real subject, acting as a transparent intermediary.

---

## Structure

```
      <<interface>>
        Subject
       +request()
           ^
           |
    +------+------+
    |              |
RealSubject       Proxy
+request()        -realSubject: RealSubject  (created lazily or injected)
                  +request()
                    └─> (pre-processing)
                    └─> realSubject.request()
                    └─> (post-processing)
```

The client holds a reference to `Subject`. It never knows whether it is talking to `RealSubject` or `Proxy`.

---

## Implementation

### (1) Virtual Proxy: Lazy-Loading a Heavy ReportGenerator

The real `ReportGenerator` runs expensive database queries. We defer creation until the first actual use.

```java
import java.util.List;
import java.util.ArrayList;

// ─── Domain ──────────────────────────────────────────────────────

public class Report {
    private final String title;
    private final List<String> rows;

    public Report(String title, List<String> rows) {
        this.title = title;
        this.rows  = rows;
    }

    public String getTitle() { return title; }
    public List<String> getRows() { return rows; }

    @Override
    public String toString() {
        return "Report{title='" + title + "', rows=" + rows.size() + "}";
    }
}

// ─── Subject interface ────────────────────────────────────────────

public interface ReportGenerator {
    Report generateSalesReport(String quarter);
    Report generateInventoryReport();
}

// ─── RealSubject: expensive to construct ─────────────────────────

public class DatabaseReportGenerator implements ReportGenerator {

    public DatabaseReportGenerator() {
        // Simulates expensive initialization: DB connection pool, schema introspection
        System.out.println("[DatabaseReportGenerator] Initializing DB connections... (expensive)");
        simulateSlowInit();
    }

    private void simulateSlowInit() {
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Override
    public Report generateSalesReport(String quarter) {
        System.out.println("[DatabaseReportGenerator] Querying sales for " + quarter);
        List<String> rows = new ArrayList<>();
        rows.add("Product A: $12,000");
        rows.add("Product B: $8,500");
        rows.add("Product C: $21,300");
        return new Report("Sales Report " + quarter, rows);
    }

    @Override
    public Report generateInventoryReport() {
        System.out.println("[DatabaseReportGenerator] Querying inventory levels");
        List<String> rows = new ArrayList<>();
        rows.add("SKU-001: 450 units");
        rows.add("SKU-002: 12 units (LOW)");
        return new Report("Inventory Report", rows);
    }
}

// ─── Virtual Proxy: defers creation of DatabaseReportGenerator ───

public class LazyReportGeneratorProxy implements ReportGenerator {

    // Starts null; created only when first method is called
    private DatabaseReportGenerator realGenerator;

    private DatabaseReportGenerator getRealGenerator() {
        if (realGenerator == null) {
            System.out.println("[LazyReportGeneratorProxy] First access — creating real generator");
            realGenerator = new DatabaseReportGenerator();
        }
        return realGenerator;
    }

    @Override
    public Report generateSalesReport(String quarter) {
        return getRealGenerator().generateSalesReport(quarter);
    }

    @Override
    public Report generateInventoryReport() {
        return getRealGenerator().generateInventoryReport();
    }
}

// ─── Client ──────────────────────────────────────────────────────

class VirtualProxyDemo {
    public static void main(String[] args) {
        // Proxy is cheap to create — no DB connection yet
        ReportGenerator generator = new LazyReportGeneratorProxy();
        System.out.println("Proxy created — DB not yet initialized.");

        // First call triggers real initialization
        Report sales = generator.generateSalesReport("Q3-2024");
        System.out.println("Got: " + sales);

        // Subsequent calls reuse the already-initialized real object
        Report inventory = generator.generateInventoryReport();
        System.out.println("Got: " + inventory);
    }
}
```

---

### (2) Protection Proxy: Role-Based Access to InventoryService

```java
import java.util.Set;
import java.util.EnumSet;

// ─── Domain ──────────────────────────────────────────────────────

public enum Role { VIEWER, OPERATOR, ADMIN }

public class User {
    private final String name;
    private final Role   role;

    public User(String name, Role role) {
        this.name = name;
        this.role = role;
    }

    public String getName() { return name; }
    public Role   getRole() { return role; }
}

// ─── Subject interface ────────────────────────────────────────────

public interface InventoryService {
    int  getStockLevel(String sku);
    void updateStockLevel(String sku, int quantity);
    void deleteProduct(String sku);
}

// ─── RealSubject ─────────────────────────────────────────────────

public class InventoryServiceImpl implements InventoryService {

    private final java.util.Map<String, Integer> stock = new java.util.HashMap<>();

    public InventoryServiceImpl() {
        stock.put("SKU-001", 200);
        stock.put("SKU-002", 15);
    }

    @Override
    public int getStockLevel(String sku) {
        int level = stock.getOrDefault(sku, 0);
        System.out.printf("[InventoryService] Stock for %s: %d%n", sku, level);
        return level;
    }

    @Override
    public void updateStockLevel(String sku, int quantity) {
        stock.put(sku, quantity);
        System.out.printf("[InventoryService] Updated %s -> %d%n", sku, quantity);
    }

    @Override
    public void deleteProduct(String sku) {
        stock.remove(sku);
        System.out.printf("[InventoryService] Deleted %s%n", sku);
    }
}

// ─── Protection Proxy ─────────────────────────────────────────────

public class AuthorizationProxy implements InventoryService {

    private final InventoryService realService;
    private final User             currentUser;

    public AuthorizationProxy(InventoryService realService, User currentUser) {
        this.realService = realService;
        this.currentUser = currentUser;
    }

    private void requireRole(Role minimum) {
        // Simple ordinal comparison; in production use a permission matrix
        if (currentUser.getRole().ordinal() < minimum.ordinal()) {
            throw new SecurityException(
                String.format("User '%s' with role %s is not authorized. Required: %s",
                    currentUser.getName(), currentUser.getRole(), minimum));
        }
    }

    @Override
    public int getStockLevel(String sku) {
        requireRole(Role.VIEWER);  // All roles may read
        return realService.getStockLevel(sku);
    }

    @Override
    public void updateStockLevel(String sku, int quantity) {
        requireRole(Role.OPERATOR);  // Only operators and admins may write
        realService.updateStockLevel(sku, quantity);
    }

    @Override
    public void deleteProduct(String sku) {
        requireRole(Role.ADMIN);  // Only admins may delete
        realService.deleteProduct(sku);
    }
}

// ─── Client ──────────────────────────────────────────────────────

class ProtectionProxyDemo {
    public static void main(String[] args) {
        InventoryService realService = new InventoryServiceImpl();

        User viewer   = new User("alice", Role.VIEWER);
        User operator = new User("bob",   Role.OPERATOR);
        User admin    = new User("carol", Role.ADMIN);

        InventoryService viewerProxy   = new AuthorizationProxy(realService, viewer);
        InventoryService operatorProxy = new AuthorizationProxy(realService, operator);
        InventoryService adminProxy    = new AuthorizationProxy(realService, admin);

        // VIEWER: can read, cannot write
        viewerProxy.getStockLevel("SKU-001");          // OK
        try {
            viewerProxy.updateStockLevel("SKU-001", 300);  // Throws
        } catch (SecurityException e) {
            System.out.println("Blocked: " + e.getMessage());
        }

        // OPERATOR: can read and write, cannot delete
        operatorProxy.updateStockLevel("SKU-002", 50); // OK
        try {
            operatorProxy.deleteProduct("SKU-002");        // Throws
        } catch (SecurityException e) {
            System.out.println("Blocked: " + e.getMessage());
        }

        // ADMIN: full access
        adminProxy.deleteProduct("SKU-002");           // OK
    }
}
```

---

### (3) Remote Proxy: Stub for a Remote OrderService

In a microservices architecture the client should not know whether `OrderService` is local or remote. The remote proxy encapsulates the network call.

```java
import java.util.Optional;

// ─── Subject interface (shared between client and server) ─────────

public interface OrderService {
    Optional<Order> findById(String orderId);
    void cancelOrder(String orderId);
}

// ─── Domain ──────────────────────────────────────────────────────

public class Order {
    private final String id;
    private final String status;

    public Order(String id, String status) {
        this.id     = id;
        this.status = status;
    }

    public String getId()     { return id; }
    public String getStatus() { return status; }

    @Override
    public String toString() {
        return "Order{id='" + id + "', status='" + status + "'}";
    }
}

// ─── Remote Proxy (stub) ─────────────────────────────────────────

public class OrderServiceRemoteProxy implements OrderService {

    private final String baseUrl;
    // In production: inject an HttpClient or a generated gRPC stub

    public OrderServiceRemoteProxy(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public Optional<Order> findById(String orderId) {
        // Simulates: GET {baseUrl}/orders/{orderId}
        System.out.printf("[RemoteProxy] GET %s/orders/%s%n", baseUrl, orderId);
        // Deserialize JSON response to Order object (omitted for brevity)
        return Optional.of(new Order(orderId, "PROCESSING"));
    }

    @Override
    public void cancelOrder(String orderId) {
        // Simulates: DELETE {baseUrl}/orders/{orderId}
        System.out.printf("[RemoteProxy] DELETE %s/orders/%s%n", baseUrl, orderId);
        // Handle HTTP errors, timeouts, retries here
    }
}

// ─── Client ──────────────────────────────────────────────────────

class RemoteProxyDemo {
    public static void main(String[] args) {
        OrderService orders = new OrderServiceRemoteProxy("https://orders.internal.example.com");

        orders.findById("ORD-789").ifPresent(o -> System.out.println("Found: " + o));
        orders.cancelOrder("ORD-789");
    }
}
```

---

### (4) Dynamic Proxy: Logging All Method Calls via Reflection

Java's `java.lang.reflect.Proxy` creates a proxy class at runtime for any interface. This is the mechanism underlying Spring AOP, Hibernate lazy-loading, Mockito mocks, and many frameworks.

```java
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// ─── Subject interface ────────────────────────────────────────────

public interface UserRepository {
    User findById(long id);
    void save(User user);
    void delete(long id);
}

// ─── RealSubject ─────────────────────────────────────────────────

public class UserRepositoryImpl implements UserRepository {
    @Override
    public User findById(long id) {
        System.out.println("[UserRepositoryImpl] Querying user " + id);
        return new User("user-" + id, Role.VIEWER);
    }

    @Override
    public void save(User user) {
        System.out.println("[UserRepositoryImpl] Saving " + user.getName());
    }

    @Override
    public void delete(long id) {
        System.out.println("[UserRepositoryImpl] Deleting user " + id);
    }
}

// ─── InvocationHandler: intercepts every method call ─────────────

public class LoggingInvocationHandler implements InvocationHandler {

    private final Object target;

    public LoggingInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long start = System.currentTimeMillis();
        System.out.printf("[DynamicProxy] --> %s.%s(%s)%n",
            target.getClass().getSimpleName(),
            method.getName(),
            formatArgs(args));

        Object result = method.invoke(target, args);

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("[DynamicProxy] <-- %s returned in %d ms%n",
            method.getName(), elapsed);

        return result;
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(args[i]);
        }
        return sb.toString();
    }
}

// ─── Factory helper ───────────────────────────────────────────────

public class LoggingProxyFactory {

    @SuppressWarnings("unchecked")
    public static <T> T createLoggingProxy(T target, Class<T> interfaceType) {
        return (T) Proxy.newProxyInstance(
            interfaceType.getClassLoader(),
            new Class<?>[]{ interfaceType },
            new LoggingInvocationHandler(target)
        );
    }
}

// ─── Client ──────────────────────────────────────────────────────

class DynamicProxyDemo {
    public static void main(String[] args) {
        UserRepository real  = new UserRepositoryImpl();
        UserRepository proxy = LoggingProxyFactory.createLoggingProxy(real, UserRepository.class);

        // Client code unchanged — all method calls go through the proxy
        proxy.findById(42);
        proxy.save(new User("newuser", Role.OPERATOR));
        proxy.delete(7);
    }
}
```

**Key insight:** The dynamic proxy is interface-based. Spring AOP uses CGLIB bytecode generation when no interface is available to proxy concrete classes as well.

---

## When to Use

- You need lazy initialization of an expensive object (virtual proxy).
- You need to control access based on caller identity or permissions (protection proxy).
- You need a local representative for an object in a different address space (remote proxy).
- You need to transparently add cross-cutting concerns — logging, caching, metrics, transactions — without modifying the target class (dynamic proxy / AOP).

## When NOT to Use

- When the overhead of the proxy layer is measurable and unacceptable on a hot code path.
- When the interface is large and changes frequently — every interface change requires updating the proxy.
- When the indirection makes debugging significantly harder and there is a simpler alternative (e.g., the behavior can be added directly to the class).
- When you need to add behavior to only some methods, not all — consider a more targeted approach rather than a blanket proxy.

---

## Variants

### Virtual Proxy
Defers creation of the real subject. Useful for resources that are expensive to initialize (DB connections, file handles, external service clients).

### Protection Proxy
Checks caller permissions before delegating. Acts as a security gate without embedding authorization logic in the real subject.

### Remote Proxy
Hides network communication. The client code is location-transparent; network details (serialization, timeouts, retries) live in the proxy.

### Dynamic / Reflective Proxy
Generated at runtime using reflection (`java.lang.reflect.Proxy`) or bytecode manipulation (CGLIB, Byte Buddy). Powers frameworks like Spring AOP, Hibernate, and Mockito.

### Cache Proxy
Intercepts read operations and returns cached results to avoid re-computing or re-fetching. A specific variant of virtual proxy.

---

## Real-World Examples

| Context | Proxy type | Example |
|---|---|---|
| Spring AOP | Dynamic | `@Transactional`, `@Cacheable`, `@Async` method interceptors |
| Hibernate | Virtual | Lazy-loaded `@OneToMany` collections return proxy objects |
| `java.lang.reflect.Proxy` | Dynamic | Mockito mocks, JDK dynamic proxies |
| gRPC / RMI stubs | Remote | Generated stubs hide network transport |
| Spring Security | Protection | Method security via `@PreAuthorize` AOP proxy |

---

## Interview Questions

**Q: What is the difference between Proxy, Decorator, and Facade?**

| Dimension | Proxy | Decorator | Facade |
|---|---|---|---|
| Primary intent | Control access | Add behavior | Simplify interface |
| Interface | Same as subject | Same as component | New, simpler interface |
| Object lifecycle | Proxy may create/manage the real object | Client provides the wrapped object | Client interacts through facade; subsystem hidden |
| Transparency | Fully transparent to client | Fully transparent to client | Client knows about facade, not subsystem |
| Typical concern | Access control, lazy init, remoting | Stacking optional behaviors | Hiding complexity |

In practice, a protection proxy and a decorator look structurally identical. The distinction is **why** the wrapper exists: to control access (proxy) or to enrich behavior (decorator).

**Q: How does Spring's @Transactional use the Proxy pattern?**

When you annotate a bean method with `@Transactional`, Spring wraps the bean in a proxy (JDK dynamic proxy if it implements an interface; CGLIB subclass proxy otherwise). Every call to that method goes through `TransactionInterceptor`, which begins a transaction before the method, commits on success, and rolls back on exception — all without any transaction management code in your service class.

**Q: What is the difference between a virtual proxy and a lazy singleton?**

A lazy singleton defers creation of a single global instance using `synchronized` or double-checked locking. A virtual proxy defers creation of a per-client or per-call subject and implements the same interface as that subject, so the client code does not change. The singleton is about instance uniqueness; the virtual proxy is about transparent deferred loading.
