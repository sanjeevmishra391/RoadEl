# Factory Method Pattern

## Intent

Define an interface for creating an object, but let subclasses decide which class
to instantiate — Factory Method lets a class defer instantiation to subclasses.

---

## Problem: The `new` Operator Scattered Everywhere

Without a factory, every caller that needs a `Notification` must know the concrete
class and `new` it directly:

```java
// Caller A — in OrderService
if (userPreference.equals("EMAIL")) {
    Notification n = new EmailNotification(user.getEmail(), subject, body);
    n.send();
} else if (userPreference.equals("SMS")) {
    Notification n = new SmsNotification(user.getPhoneNumber(), body);
    n.send();
} else if (userPreference.equals("PUSH")) {
    Notification n = new PushNotification(user.getDeviceToken(), title, body);
    n.send();
}

// Caller B — same logic duplicated in ShipmentService
// Caller C — same logic duplicated in PromotionService
```

**Concrete problems:**

1. **Open/Closed Principle (OCP) violation.** Adding a new channel (e.g., WhatsApp)
   requires editing every `if-else` block in every caller. Classes that should be
   closed for modification are opened.

2. **Dependency on concrete classes.** `OrderService` now imports and depends on
   `EmailNotification`, `SmsNotification`, and `PushNotification`. These are
   implementation details that business logic should not know about.

3. **Duplication of construction logic.** If `EmailNotification` needs an SMTP client
   injected, that wiring is duplicated at every call site.

4. **Testing is harder.** Tests for `OrderService` must also construct real notification
   objects (or manually mock every concrete class).

---

## Structure

```
+-----------------------------+
|    NotificationCreator      |   <<abstract>>
|    (Creator)                |
+-----------------------------+
| + sendNotification(         |
|     channel, recipient,     |
|     message): void          |
| # createNotification(       |   <-- Factory Method (abstract or with default)
|     channel): Notification  |
+-----------------------------+
          ^            ^
          |            |
+-----------------+  +-------------------+
| EmailNotif-     |  | SmsNotification-  |
| icationCreator  |  | Creator           |
| (ConcreteCreator)|  |(ConcreteCreator)  |
+-----------------+  +-------------------+
| # createNotif() |  | # createNotif()   |
|   -> EmailNotif |  |   -> SmsNotif     |
+-----------------+  +-------------------+

+-----------------------------+
|     <<interface>>           |
|       Notification          |
|    (AbstractProduct)        |
+-----------------------------+
| + send(): void              |
| + getChannel(): String      |
+-----------------------------+
          ^        ^       ^
          |        |       |
+-----------+ +--------+ +---------------+
| EmailNotif| | SmsNotif| | PushNotif    |
|(Concrete  | |(Concrete| |(Concrete     |
| Product)  | | Product)| | Product)     |
+-----------+ +--------+ +---------------+
| - email   | | - phone | | - deviceToken|
| - subject | | - body  | | - title      |
| + send()  | | + send()| | + send()     |
+-----------+ +--------+ +---------------+

Parameterized factory (static variant — also shown in Implementation):

+------------------------------+
|   NotificationFactory        |   <<static utility>>
+------------------------------+
| + create(channel, ...):      |
|   Notification               |
+------------------------------+
        |
        +-- creates --> EmailNotification | SmsNotification | PushNotification
```

---

## Implementation

### Step 1: Product interface

```java
public interface Notification {
    void send();
    String getChannel();
}
```

### Step 2: Concrete Products

```java
public class EmailNotification implements Notification {

    private final String toAddress;
    private final String subject;
    private final String body;

    public EmailNotification(String toAddress, String subject, String body) {
        if (toAddress == null || !toAddress.contains("@"))
            throw new IllegalArgumentException("Invalid email address: " + toAddress);
        this.toAddress = toAddress;
        this.subject   = subject;
        this.body      = body;
    }

    @Override
    public void send() {
        // In production: use JavaMailSender, SendGrid client, etc.
        System.out.printf("[EMAIL] To: %s | Subject: %s | Body: %s%n",
                toAddress, subject, body);
    }

    @Override
    public String getChannel() { return "EMAIL"; }
}

public class SmsNotification implements Notification {

    private final String phoneNumber;
    private final String message;

    public SmsNotification(String phoneNumber, String message) {
        if (phoneNumber == null || !phoneNumber.matches("\\+?[0-9]{7,15}"))
            throw new IllegalArgumentException("Invalid phone number: " + phoneNumber);
        this.phoneNumber = phoneNumber;
        this.message     = message;
    }

    @Override
    public void send() {
        // In production: use Twilio, SNS, etc.
        System.out.printf("[SMS] To: %s | Message: %s%n", phoneNumber, message);
    }

    @Override
    public String getChannel() { return "SMS"; }
}

public class PushNotification implements Notification {

    private final String deviceToken;
    private final String title;
    private final String body;

    public PushNotification(String deviceToken, String title, String body) {
        if (deviceToken == null || deviceToken.isBlank())
            throw new IllegalArgumentException("Device token must not be blank");
        this.deviceToken = deviceToken;
        this.title       = title;
        this.body        = body;
    }

    @Override
    public void send() {
        // In production: use Firebase Cloud Messaging (FCM), APNS, etc.
        System.out.printf("[PUSH] Token: %s | Title: %s | Body: %s%n",
                deviceToken, title, body);
    }

    @Override
    public String getChannel() { return "PUSH"; }
}
```

### Step 3a: Classic Factory Method — Creator + ConcreteCreator

```java
// Abstract Creator — defines the factory method and the high-level algorithm
public abstract class NotificationCreator {

    // Factory Method: subclasses override to produce the appropriate product
    // This is the "virtual constructor" — the creator defers instantiation to subclasses
    protected abstract Notification createNotification(String recipient, String message);

    // Template method — uses the factory method internally
    public void sendNotification(String recipient, String message) {
        Notification notification = createNotification(recipient, message);
        // Common pre/post logic lives here, decoupled from the concrete type
        System.out.println("Preparing to send via " + notification.getChannel());
        notification.send();
        System.out.println("Notification dispatched.");
    }
}

// Concrete Creators — each knows which concrete product to instantiate
public class EmailNotificationCreator extends NotificationCreator {

    private final String subject;

    public EmailNotificationCreator(String subject) {
        this.subject = subject;
    }

    @Override
    protected Notification createNotification(String recipient, String message) {
        return new EmailNotification(recipient, subject, message);
    }
}

public class SmsNotificationCreator extends NotificationCreator {

    @Override
    protected Notification createNotification(String recipient, String message) {
        return new SmsNotification(recipient, message);
    }
}

public class PushNotificationCreator extends NotificationCreator {

    private final String title;

    public PushNotificationCreator(String title) {
        this.title = title;
    }

    @Override
    protected Notification createNotification(String recipient, String message) {
        return new PushNotification(recipient, title, message);
    }
}
```

### Step 3b: Parameterized / Static Factory — simpler variant, very common in practice

```java
// Static factory — no subclassing required; channel drives type selection
// This is how NotificationFactory typically looks in production codebases
public final class NotificationFactory {

    // OCP compliance: adding a new channel adds a new case here (and a new class),
    // but NO existing callers need to change.
    public static Notification create(String channel, String recipient,
                                      String subject, String message) {
        return switch (channel.toUpperCase()) {
            case "EMAIL" -> new EmailNotification(recipient, subject, message);
            case "SMS"   -> new SmsNotification(recipient, message);
            case "PUSH"  -> new PushNotification(recipient, subject, message);
            default      -> throw new IllegalArgumentException(
                                "Unsupported notification channel: " + channel);
        };
    }

    // Private constructor — utility class, not meant to be instantiated
    private NotificationFactory() {}
}
```

### Step 4: Client code — fully decoupled from concrete types

```java
// Client depends only on Notification (interface) and NotificationFactory —
// never on EmailNotification, SmsNotification, or PushNotification directly.
public class OrderService {

    public void notifyUser(User user, String orderStatus) {
        String message = "Your order is now: " + orderStatus;

        // Client does not use 'new' on any concrete notification class
        Notification notification = NotificationFactory.create(
                user.getPreferredChannel(),
                user.getContactAddress(),
                "Order Update",
                message
        );

        notification.send();
    }
}

// Adding WhatsApp later: create WhatsAppNotification implements Notification,
// add one case to the factory — OrderService, ShipmentService, PromotionService
// all pick it up without modification. That is OCP in practice.
```

### Step 5: Distinguishing Factory Method from Static Factory Methods

```java
// Static Factory Method (Effective Java, Item 1) — a static method that returns
// an instance; does NOT require inheritance or subclassing.
// These are plain convenience methods, not the GOF Factory Method pattern.
public final class HttpClient {
    public static HttpClient newDefaultClient() { return new HttpClient(/* defaults */); }
    public static HttpClient newClientWithTimeout(int ms) { return new HttpClient(ms); }
    private HttpClient(int timeoutMs) { /* ... */ }
    private HttpClient() { this(5000); }
}

// GOF Factory Method — an abstract method in a Creator class, overridden in
// ConcreteCreators to control what type is produced. Requires an inheritance hierarchy.
// See NotificationCreator above.

// Key distinction:
// Static factory  -> single class, no subclassing, selects pre-defined types
// GOF Factory Method -> inheritance, each subclass decides the product type
```

**The "virtual constructor" concept:**  
A factory method acts like a constructor from the caller's perspective (it produces
an object), but behaves like a virtual method — the actual construction logic is
deferred to the subclass that overrides the factory method. The Creator does not
know which concrete class will be produced; it only knows the product interface.

---

## When to Use

- You need to decouple client code from the concrete classes it creates — clients
  depend only on the product interface, never on `new ConcreteProduct()`.
- The set of products is expected to grow over time. Each new product is added by
  creating a new class and extending the factory, not by editing caller code (OCP).
- A class cannot anticipate the type of objects it needs to create — the type is
  determined by subclass behavior or runtime input.
- You want to centralize and standardize object creation, including validation,
  dependency injection, and caching, in one place.

## When NOT to Use

- **Only one concrete type will ever exist.** A factory for a single product is
  pointless indirection. Use a constructor or a simple static factory method.
- **The product hierarchy is stable and simple.** If you have two types and they
  will never grow, the pattern adds ceremony without payoff.
- **Performance-critical tight loops.** The virtual dispatch and potential allocation
  of a Creator add overhead. In microsecond-sensitive code, profile first.

---

## Variants

### Static Factory Method
A static method on the factory or product class that returns instances. Simpler than
the GOF pattern — no subclassing. Examples: `Collections.unmodifiableList()`,
`Optional.of()`, `LocalDate.of()`. Prefer this when type selection logic is simple
and inheritance is not needed.

### Parameterized Factory
The factory method accepts a parameter (channel type, string key, enum) to select
the product. This is the `NotificationFactory.create(channel, ...)` form shown above.
Common in practice; trades the subclassing of the classic pattern for a selection
parameter.

### Registry-based Factory
A `Map<String, Supplier<Notification>>` replaces the `switch`. New product types
self-register (useful in plugin architectures):

```java
public final class NotificationRegistry {
    private static final Map<String, BiFunction<String, String, Notification>> registry
            = new HashMap<>();

    public static void register(String channel,
                                BiFunction<String, String, Notification> factory) {
        registry.put(channel.toUpperCase(), factory);
    }

    public static Notification create(String channel, String recipient, String message) {
        BiFunction<String, String, Notification> factory = registry.get(channel.toUpperCase());
        if (factory == null) throw new IllegalArgumentException("Unknown channel: " + channel);
        return factory.apply(recipient, message);
    }
}

// Registration (e.g., at startup or in a module initializer)
// NotificationRegistry.register("EMAIL", (r, m) -> new EmailNotification(r, "No Subject", m));
```

---

## Real-World Examples

- **`java.util.Calendar.getInstance()`** — returns a locale-appropriate `Calendar`
  subclass (e.g., `GregorianCalendar`). Callers depend on `Calendar`, not the
  concrete type. Classic parameterized static factory.
- **`java.sql.DriverManager.getConnection(url)`** — JDBC selects the correct `Driver`
  implementation based on the URL prefix and returns a `Connection`. Callers never
  reference the vendor-specific driver class.
- **Spring `BeanFactory` / `ApplicationContext`** — Spring's core container is a
  factory for managed beans. `context.getBean(NotificationService.class)` returns
  the appropriate implementation without the caller knowing the concrete class.
- **`java.nio.file.FileSystems.getDefault()`** — returns the platform-appropriate
  `FileSystem` implementation.
- **`javax.xml.parsers.DocumentBuilderFactory.newInstance()`** — creates a
  platform-specific XML parser without binding callers to a vendor implementation.

---

## Interview Questions

**Q1: How does Factory Method enforce the Open/Closed Principle?**

Adding a new product (e.g., `WhatsAppNotification`) requires: (1) creating the new
class implementing `Notification`, and (2) adding one case to the factory or one
new `ConcreteCreator` subclass. No existing caller (OrderService, ShipmentService)
changes — they depend only on the `Notification` interface and the factory, both of
which are stable. The system is open to extension and closed to modification.

**Q2: What is the difference between a Factory Method and a static factory method
(as in Effective Java)?**

The GOF Factory Method is an *instance method* (often abstract) on a Creator class
that subclasses override to produce different product types. It relies on polymorphism
and inheritance. A static factory method (Effective Java, Item 1) is simply a static
method that returns an instance — there is no inheritance, no Creator hierarchy, and
no subclass involvement. The name collision is unfortunate; they are different concepts.

**Q3: When should you use a registry-based factory instead of a switch statement?**

Use a registry when: new product types can be introduced by third-party code (plugin
or extension point architecture); you want to avoid recompiling the factory when new
channels are added; or the number of types is very large and the switch becomes a
maintenance problem. The registry trades compile-time exhaustiveness checking for
runtime flexibility. For a fixed, internal set of types, a switch or if-else chain is
simpler and the compiler can enforce that all enum cases are handled.
