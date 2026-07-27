# Observer Pattern

## Intent

Define a one-to-many dependency between objects so that when one object changes state, all its dependents are notified and updated automatically.

---

## Problem

Without the Observer pattern, an event source (e.g., `OrderService`) must directly reference every consumer that cares about its state changes. Adding a new consumer requires modifying the source class — a violation of the Open/Closed Principle. The source becomes tightly coupled to its consumers, making the system brittle and hard to extend.

---

## Structure

```
+---------------------+          +------------------+
|     <<interface>>   |          |   <<interface>>  |
|       Subject       |          |     Observer     |
|---------------------|          |------------------|
| + attach(Observer)  |<>------->| + update(Event)  |
| + detach(Observer)  |          +------------------+
| + notifyObservers() |                   ^
+---------------------+                   |
          ^                    +----------+----------+
          |                    |                     |
+---------+----------+  +------+--------+  +---------+--------+
|   ConcreteSubject  |  | ConcreteObs1  |  | ConcreteObs2     |
|--------------------|  |---------------|  |------------------|
| - observers: List  |  | + update(e)   |  | + update(e)      |
| - state            |  +---------------+  +------------------+
| + setState()       |
| + getState()       |
+--------------------+
```

---

## Implementation

### Domain: Order Status Notification System

```java
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// ---------------------------------------------------------------
// Event object (used in push variant)
// ---------------------------------------------------------------
public class OrderEvent {
    public enum Type { CREATED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }

    private final String orderId;
    private final Type type;
    private final String details;

    public OrderEvent(String orderId, Type type, String details) {
        this.orderId = orderId;
        this.type = type;
        this.details = details;
    }

    public String getOrderId()  { return orderId; }
    public Type   getType()     { return type; }
    public String getDetails()  { return details; }

    @Override
    public String toString() {
        return "[OrderEvent orderId=" + orderId + ", type=" + type + "]";
    }
}

// ---------------------------------------------------------------
// Observer interface
// ---------------------------------------------------------------
public interface OrderObserver {
    void onOrderEvent(OrderEvent event);
}

// ---------------------------------------------------------------
// Subject interface
// ---------------------------------------------------------------
public interface OrderSubject {
    void attach(OrderObserver observer);
    void detach(OrderObserver observer);
    void notifyObservers(OrderEvent event);
}

// ---------------------------------------------------------------
// ConcreteSubject — OrderService
//
// Thread safety: CopyOnWriteArrayList ensures that iteration in
// notifyObservers() does not throw ConcurrentModificationException
// if attach/detach is called from another thread during notification.
// ---------------------------------------------------------------
public class OrderService implements OrderSubject {

    // CopyOnWriteArrayList: safe for concurrent reads + rare writes
    private final List<OrderObserver> observers = new CopyOnWriteArrayList<>();
    private String currentStatus;

    @Override
    public void attach(OrderObserver observer) {
        observers.add(observer);
    }

    @Override
    public void detach(OrderObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(OrderEvent event) {
        // Snapshot iteration — thread-safe with CopyOnWriteArrayList
        for (OrderObserver observer : observers) {
            observer.onOrderEvent(event);
        }
    }

    // Business method — changes state and fires notification (push)
    public void placeOrder(String orderId) {
        currentStatus = "CREATED";
        System.out.println("OrderService: Order " + orderId + " placed.");
        notifyObservers(new OrderEvent(orderId, OrderEvent.Type.CREATED,
                "Order placed successfully"));
    }

    public void confirmOrder(String orderId) {
        currentStatus = "CONFIRMED";
        notifyObservers(new OrderEvent(orderId, OrderEvent.Type.CONFIRMED,
                "Payment confirmed"));
    }

    public void shipOrder(String orderId, String trackingNumber) {
        currentStatus = "SHIPPED";
        notifyObservers(new OrderEvent(orderId, OrderEvent.Type.SHIPPED,
                "Tracking: " + trackingNumber));
    }

    // Pull variant: observers can query this getter instead of using event data
    public String getCurrentStatus() {
        return currentStatus;
    }
}

// ---------------------------------------------------------------
// ConcreteObserver 1 — EmailNotificationObserver (push variant)
// Data arrives via the event parameter; no need to call back subject
// ---------------------------------------------------------------
public class EmailNotificationObserver implements OrderObserver {

    private final String emailAddress;

    public EmailNotificationObserver(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public void onOrderEvent(OrderEvent event) {
        System.out.println("EmailObserver -> Sending email to " + emailAddress
                + " | Order " + event.getOrderId()
                + " is now " + event.getType()
                + " | " + event.getDetails());
    }
}

// ---------------------------------------------------------------
// ConcreteObserver 2 — SMSNotificationObserver (push variant)
// ---------------------------------------------------------------
public class SMSNotificationObserver implements OrderObserver {

    private final String phoneNumber;

    public SMSNotificationObserver(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void onOrderEvent(OrderEvent event) {
        // Only send SMS for shipped/delivered; ignore other events
        if (event.getType() == OrderEvent.Type.SHIPPED
                || event.getType() == OrderEvent.Type.DELIVERED) {
            System.out.println("SMSObserver  -> Sending SMS to " + phoneNumber
                    + " | " + event.getDetails());
        }
    }
}

// ---------------------------------------------------------------
// ConcreteObserver 3 — AnalyticsObserver (pull variant)
// Stores reference to the subject and pulls state on notification
// ---------------------------------------------------------------
public class AnalyticsObserver implements OrderObserver {

    private final OrderService orderService; // reference to subject for pull

    public AnalyticsObserver(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void onOrderEvent(OrderEvent event) {
        // Pull: ignores data in event; fetches directly from subject
        String latestStatus = orderService.getCurrentStatus();
        System.out.println("AnalyticsObs -> Recording metric: orderId="
                + event.getOrderId() + " status=" + latestStatus);
    }
}

// ---------------------------------------------------------------
// Client / Demo
// ---------------------------------------------------------------
public class ObserverDemo {

    public static void main(String[] args) {
        OrderService orderService = new OrderService();

        // Attach observers
        orderService.attach(new EmailNotificationObserver("customer@example.com"));
        orderService.attach(new SMSNotificationObserver("+1-555-0100"));
        orderService.attach(new AnalyticsObserver(orderService)); // pull variant

        System.out.println("--- Placing order ---");
        orderService.placeOrder("ORD-001");

        System.out.println("\n--- Confirming order ---");
        orderService.confirmOrder("ORD-001");

        System.out.println("\n--- Shipping order ---");
        orderService.shipOrder("ORD-001", "UPS-9876543210");
    }
}
```

**Expected output:**
```
--- Placing order ---
OrderService: Order ORD-001 placed.
EmailObserver -> Sending email to customer@example.com | Order ORD-001 is now CREATED | Order placed successfully
AnalyticsObs -> Recording metric: orderId=ORD-001 status=CREATED

--- Confirming order ---
EmailObserver -> Sending email to customer@example.com | Order ORD-001 is now CONFIRMED | Payment confirmed
AnalyticsObs -> Recording metric: orderId=ORD-001 status=CONFIRMED

--- Shipping order ---
EmailObserver -> Sending email to customer@example.com | Order ORD-001 is now SHIPPED | Tracking: UPS-9876543210
SMSObserver  -> Sending SMS to +1-555-0100 | Tracking: UPS-9876543210
AnalyticsObs -> Recording metric: orderId=ORD-001 status=SHIPPED
```

---

### Push vs Pull Variant Summary

| Aspect | Push | Pull |
|---|---|---|
| Data delivery | Subject embeds data in event | Observer calls back to subject |
| Coupling | Lower (observer needs no subject ref) | Observer must hold subject reference |
| Efficiency | May send unused data | Observer fetches only what it needs |
| Consistency | Snapshot in event may be stale by the time observer processes it | Observer fetches the very latest state |

---

## When to Use

- When a change in one object requires updating an unknown number of other objects (open-ended consumer list).
- When objects should be able to notify other objects without assumptions about who those objects are.
- When you need to decouple the event source from event consumers so either can vary independently.
- When implementing distributed event-handling systems, MVC architecture (model notifies views), or reactive pipelines.

## When NOT to Use

- When observers are few and fixed — direct calls are simpler and more traceable.
- When observers need a guaranteed delivery order and you cannot control it through registration order.
- When notification chains cause cascading updates and make the call stack hard to reason about.
- When the update frequency is very high and observer iteration becomes a bottleneck (consider batching or reactive streams instead).

---

## Variants

### 1. Push vs Pull (described above)

### 2. Event Bus (decoupled from direct observer)

In a true Event Bus (e.g., Guava `EventBus`, Spring `ApplicationEventPublisher`), producers and consumers have **no direct reference to each other**. Events are published to a central broker. Subscribers register interest by event type.

```java
// Spring ApplicationEventPublisher — producer knows nothing about consumers
@Service
public class OrderService {
    @Autowired
    private ApplicationEventPublisher publisher;

    public void shipOrder(String orderId) {
        publisher.publishEvent(new OrderShippedEvent(this, orderId));
    }
}

@Component
public class EmailListener {
    @EventListener
    public void handle(OrderShippedEvent event) {
        System.out.println("Email sent for " + event.getOrderId());
    }
}
```

### 3. Reactive Streams

RxJava / Project Reactor replace the manual observer list with composable, backpressure-aware streams.

```java
// RxJava 3
Observable<OrderEvent> orderStream = Observable.create(emitter -> {
    emitter.onNext(new OrderEvent("ORD-1", OrderEvent.Type.CREATED, ""));
    emitter.onNext(new OrderEvent("ORD-1", OrderEvent.Type.SHIPPED, "UPS-123"));
    emitter.onComplete();
});

orderStream
    .filter(e -> e.getType() == OrderEvent.Type.SHIPPED)
    .subscribe(e -> System.out.println("Reactive SMS: " + e.getOrderId()));
```

---

## Real-World Usage

| API | Notes |
|---|---|
| `java.util.Observer` / `Observable` | Deprecated since Java 9. Reasons: `Observable` is a class (not interface), forcing inheritance; `setChanged()` is protected, preventing delegation; not thread-safe. |
| `java.beans.PropertyChangeListener` | Still used in Swing/JavaBeans. Supports named properties (`PropertyChangeEvent`). |
| `Spring ApplicationEventPublisher` | Event bus model. Decouples producers from consumers. Supports `@Async` listeners. |
| `RxJava` / `Project Reactor` | Full reactive-streams spec. Handles backpressure, error propagation, and operator chains. Preferred for async pipelines. |

---

## Interview Questions

**Q: What is the key difference between Observer and Pub-Sub?**

In the Observer pattern, the Subject holds a **direct reference** to its observers — there is no intermediary. Adding an observer requires calling `subject.attach(observer)`. In Pub-Sub, a **broker/message bus** sits between producers and consumers. Producers publish to a **topic** and have no knowledge of who subscribes. Consumers subscribe to topics without knowing the producer. This makes Pub-Sub more loosely coupled and suitable for distributed systems (e.g., Kafka, RabbitMQ), while Observer is an in-process, synchronous pattern.

**Q: How do you make Observer thread-safe?**

Use `CopyOnWriteArrayList` for the observer list — reads (iteration during `notifyObservers`) are lock-free, and writes (attach/detach) create a new copy. Alternatively, synchronize `attach`, `detach`, and `notifyObservers` on the same lock, but copy the list before iterating to avoid holding the lock during notification callbacks (which could deadlock if an observer calls back to the subject).

**Q: What happens if an observer throws an exception during notification?**

With a naive `for` loop, one throwing observer breaks notification for all subsequent ones. Solutions: wrap each `observer.onOrderEvent(event)` call in a try-catch, log and continue; or submit each notification to an executor so failures are isolated.

**Q: When would you prefer Reactive Streams (RxJava/Reactor) over classic Observer?**

When you need backpressure (producers faster than consumers), operator composition (filter, map, flatMap on the stream), asynchronous execution, or declarative error handling. Classic Observer is synchronous and single-threaded by default; reactive streams are designed for async, non-blocking pipelines.
