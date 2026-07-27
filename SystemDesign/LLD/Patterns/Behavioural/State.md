# State Pattern

## Intent

Allow an object to alter its behavior when its internal state changes; the object will appear to change its class.

---

## Problem

Without the State pattern, all state-specific behavior lives in a single class as a tangled web of `if-else` or `switch` blocks. Every new state requires modifying every method that switches on state, and the guarded logic for each state is scattered across dozens of conditionals. This violates the Open/Closed Principle and Single Responsibility Principle simultaneously.

```java
// Anti-pattern: state machine baked into a single class
public class Order {
    private String status; // "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED"

    public void confirm() {
        if (status.equals("PENDING")) {
            status = "CONFIRMED";
        } else if (status.equals("CONFIRMED")) {
            throw new IllegalStateException("Already confirmed");
        } else if (status.equals("SHIPPED")) {
            throw new IllegalStateException("Cannot confirm shipped order");
        }
        // Adding CANCELLED means adding another else-if to every method
    }

    public void ship() {
        if (status.equals("CONFIRMED")) {
            status = "SHIPPED";
        } else {
            throw new IllegalStateException("Cannot ship from state: " + status);
        }
    }
    // ... and so on for every transition x every method
}
```

---

## Structure

```
+---------------------+         +---------------------+
|      Context        |-------->|  <<interface>>      |
|  (Order)            |         |      OrderState     |
|---------------------|         |---------------------|
| - currentState      |         | + confirm(ctx)      |
|---------------------|         | + ship(ctx)         |
| + confirm()         |         | + deliver(ctx)      |
| + ship()            |         | + cancel(ctx)       |
| + deliver()         |         +---------------------+
| + cancel()          |                   ^
| + setState(state)   |          +--------+---------+----------+
+---------------------+          |        |         |          |
                        +--------+--+ +---+------+ ++--------+ +----------+
                        | Pending   | |Confirmed | |Shipped  | |Delivered |
                        |State      | |State     | |State    | |State     |
                        |-----------| |----------| |---------| |----------|
                        |+confirm() | |+confirm()| |+confirm | |+confirm()|
                        |+ship()    | |+ship()   | |+ship()  | |+ship()   |
                        |+cancel()  | |+cancel() | |+cancel()| |+cancel() |
                        +-----------+ +----------+ +---------+ +----------+
```

---

## Implementation

### Domain: Order Status State Machine

**State transitions:**
```
PENDING --[confirm]--> CONFIRMED --[ship]--> SHIPPED --[deliver]--> DELIVERED
PENDING --[cancel]--> CANCELLED
CONFIRMED --[cancel]--> CANCELLED
```

```java
// ---------------------------------------------------------------
// Context — Order
// Delegates all state-specific behavior to the current state object.
// State objects set the next state by calling order.setState().
// ---------------------------------------------------------------
public class Order {

    private OrderState currentState;
    private final String orderId;

    public Order(String orderId) {
        this.orderId = orderId;
        // Initial state
        this.currentState = new PendingState();
        System.out.println("Order " + orderId + " created in state: PENDING");
    }

    // Package-private so state classes (in same package) can transition
    void setState(OrderState newState) {
        System.out.println("  Transition: " + currentState.getStateName()
                + " -> " + newState.getStateName());
        this.currentState = newState;
    }

    // Public API — delegates to current state
    public void confirm()  { currentState.confirm(this); }
    public void ship()     { currentState.ship(this); }
    public void deliver()  { currentState.deliver(this); }
    public void cancel()   { currentState.cancel(this); }

    public String getStatus()  { return currentState.getStateName(); }
    public String getOrderId() { return orderId; }
}

// ---------------------------------------------------------------
// State interface
// Each method receives the context so it can trigger transitions.
// ---------------------------------------------------------------
public interface OrderState {
    void confirm(Order order);
    void ship(Order order);
    void deliver(Order order);
    void cancel(Order order);
    String getStateName();
}

// ---------------------------------------------------------------
// Concrete State: PENDING
// ---------------------------------------------------------------
public class PendingState implements OrderState {

    @Override
    public void confirm(Order order) {
        System.out.println("  Payment verified. Confirming order " + order.getOrderId());
        order.setState(new ConfirmedState());
    }

    @Override
    public void ship(Order order) {
        throw new IllegalStateException(
                "Cannot ship order " + order.getOrderId() + ": not yet confirmed.");
    }

    @Override
    public void deliver(Order order) {
        throw new IllegalStateException(
                "Cannot deliver order " + order.getOrderId() + ": not yet shipped.");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("  Cancelling pending order " + order.getOrderId());
        order.setState(new CancelledState());
    }

    @Override
    public String getStateName() { return "PENDING"; }
}

// ---------------------------------------------------------------
// Concrete State: CONFIRMED
// ---------------------------------------------------------------
public class ConfirmedState implements OrderState {

    @Override
    public void confirm(Order order) {
        System.out.println("  Order " + order.getOrderId() + " is already confirmed.");
    }

    @Override
    public void ship(Order order) {
        System.out.println("  Dispatching order " + order.getOrderId() + " to carrier.");
        order.setState(new ShippedState());
    }

    @Override
    public void deliver(Order order) {
        throw new IllegalStateException(
                "Cannot deliver order " + order.getOrderId() + ": not yet shipped.");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("  Refunding payment for order " + order.getOrderId());
        order.setState(new CancelledState());
    }

    @Override
    public String getStateName() { return "CONFIRMED"; }
}

// ---------------------------------------------------------------
// Concrete State: SHIPPED
// ---------------------------------------------------------------
public class ShippedState implements OrderState {

    @Override
    public void confirm(Order order) {
        throw new IllegalStateException(
                "Order " + order.getOrderId() + " is already shipped.");
    }

    @Override
    public void ship(Order order) {
        System.out.println("  Order " + order.getOrderId() + " is already in transit.");
    }

    @Override
    public void deliver(Order order) {
        System.out.println("  Marking order " + order.getOrderId() + " as delivered.");
        order.setState(new DeliveredState());
    }

    @Override
    public void cancel(Order order) {
        // Cancellation after shipping requires a return process
        throw new IllegalStateException(
                "Cannot cancel order " + order.getOrderId()
                        + ": already shipped. Initiate a return instead.");
    }

    @Override
    public String getStateName() { return "SHIPPED"; }
}

// ---------------------------------------------------------------
// Concrete State: DELIVERED (terminal state)
// ---------------------------------------------------------------
public class DeliveredState implements OrderState {

    @Override
    public void confirm(Order order) {
        throw new IllegalStateException("Order " + order.getOrderId() + " already delivered.");
    }

    @Override
    public void ship(Order order) {
        throw new IllegalStateException("Order " + order.getOrderId() + " already delivered.");
    }

    @Override
    public void deliver(Order order) {
        System.out.println("  Order " + order.getOrderId() + " was already delivered.");
    }

    @Override
    public void cancel(Order order) {
        throw new IllegalStateException(
                "Cannot cancel order " + order.getOrderId() + ": already delivered.");
    }

    @Override
    public String getStateName() { return "DELIVERED"; }
}

// ---------------------------------------------------------------
// Concrete State: CANCELLED (terminal state)
// ---------------------------------------------------------------
public class CancelledState implements OrderState {

    @Override
    public void confirm(Order order) {
        throw new IllegalStateException("Order " + order.getOrderId() + " was cancelled.");
    }

    @Override
    public void ship(Order order) {
        throw new IllegalStateException("Order " + order.getOrderId() + " was cancelled.");
    }

    @Override
    public void deliver(Order order) {
        throw new IllegalStateException("Order " + order.getOrderId() + " was cancelled.");
    }

    @Override
    public void cancel(Order order) {
        System.out.println("  Order " + order.getOrderId() + " is already cancelled.");
    }

    @Override
    public String getStateName() { return "CANCELLED"; }
}

// ---------------------------------------------------------------
// Alternative: Enum + Switch state machine
// Simpler for small, stable state machines. Trade-offs discussed below.
// ---------------------------------------------------------------
public class OrderEnumStateMachine {

    public enum Status { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }

    private Status status = Status.PENDING;
    private final String orderId;

    public OrderEnumStateMachine(String orderId) {
        this.orderId = orderId;
    }

    public void confirm() {
        switch (status) {
            case PENDING:
                status = Status.CONFIRMED;
                System.out.println(orderId + ": PENDING -> CONFIRMED");
                break;
            case CONFIRMED:
                System.out.println(orderId + ": already CONFIRMED");
                break;
            default:
                throw new IllegalStateException("Cannot confirm from " + status);
        }
    }

    public void ship() {
        if (status != Status.CONFIRMED) {
            throw new IllegalStateException("Cannot ship from " + status);
        }
        status = Status.SHIPPED;
        System.out.println(orderId + ": CONFIRMED -> SHIPPED");
    }

    public void cancel() {
        switch (status) {
            case PENDING:
            case CONFIRMED:
                status = Status.CANCELLED;
                System.out.println(orderId + ": " + status + " -> CANCELLED");
                break;
            default:
                throw new IllegalStateException("Cannot cancel from " + status);
        }
    }

    public Status getStatus() { return status; }
}

// ---------------------------------------------------------------
// Client / Demo
// ---------------------------------------------------------------
public class StateDemo {

    public static void main(String[] args) {
        System.out.println("=== Happy Path: PENDING -> CONFIRMED -> SHIPPED -> DELIVERED ===\n");

        Order order = new Order("ORD-001");
        order.confirm();
        order.ship();
        order.deliver();
        System.out.println("Final status: " + order.getStatus());

        System.out.println("\n=== Cancellation Path: PENDING -> CANCELLED ===\n");

        Order order2 = new Order("ORD-002");
        order2.cancel();
        System.out.println("Final status: " + order2.getStatus());

        System.out.println("\n=== Invalid Transition ===\n");

        Order order3 = new Order("ORD-003");
        order3.confirm();
        try {
            order3.deliver(); // Cannot deliver before shipping
        } catch (IllegalStateException e) {
            System.out.println("Caught expected exception: " + e.getMessage());
        }
    }
}
```

**Expected output:**
```
=== Happy Path: PENDING -> CONFIRMED -> SHIPPED -> DELIVERED ===

Order ORD-001 created in state: PENDING
  Payment verified. Confirming order ORD-001
  Transition: PENDING -> CONFIRMED
  Dispatching order ORD-001 to carrier.
  Transition: CONFIRMED -> SHIPPED
  Marking order ORD-001 as delivered.
  Transition: SHIPPED -> DELIVERED
Final status: DELIVERED

=== Cancellation Path: PENDING -> CANCELLED ===

Order ORD-002 created in state: PENDING
  Cancelling pending order ORD-002
  Transition: PENDING -> CANCELLED
Final status: CANCELLED

=== Invalid Transition ===

Order ORD-003 created in state: PENDING
  Payment verified. Confirming order ORD-003
  Transition: PENDING -> CONFIRMED
Caught expected exception: Cannot deliver order ORD-003: not yet shipped.
```

---

### Class-per-State vs Enum + Switch: Trade-offs

| Concern | Class-per-State (State Pattern) | Enum + Switch |
|---|---|---|
| OCP compliance | Adding a new state = new class, no existing code changed | Adding a new state = modifying every switch in the context |
| Readability | Behavior for each state is co-located in one class | Behavior is scattered across many switch blocks |
| Complexity | High: N state classes + 1 interface + context | Low: one class, one enum |
| Transition logic | Each state decides its own successor | Centralized in switch blocks (easier to see whole machine) |
| Testing | Each state class tested in isolation | Easier to see all transitions from one place |
| Best fit | Large, complex, evolving state machines | Small (2-4 states), stable state machines |

---

## When to Use

- When an object's behavior depends on its state and it must change behavior at runtime based on that state.
- When operations have large multipart conditionals that depend on the object's state — extract each branch into a state class.
- When state transitions are complex and involve side effects (logging, notifications, validation) that belong with the transition, not scattered in the context.
- When you need to enforce valid transitions (preventing illegal state changes) and want the compiler to help via the type system.

## When NOT to Use

- When the state machine is small (2-3 states) and unlikely to grow — enum + switch is far simpler.
- When state transitions are trivially a single boolean flag (`active`/`inactive`) — a state pattern is overkill.
- When states have no per-state behavior differences — only data changes between states.

---

## Variants

### 1. Enum State Machine

Described above. Best for small, stable machines where readability and simplicity outweigh OCP compliance.

### 2. State Transition Table

Define transitions as a data structure:

```java
Map<OrderState, Map<Event, OrderState>> transitions = new HashMap<>();
transitions.put(PENDING,   Map.of(CONFIRM, CONFIRMED, CANCEL, CANCELLED));
transitions.put(CONFIRMED, Map.of(SHIP, SHIPPED,     CANCEL, CANCELLED));
transitions.put(SHIPPED,   Map.of(DELIVER, DELIVERED));

public void transition(Event event) {
    OrderState next = transitions
            .getOrDefault(currentState, Map.of())
            .get(event);
    if (next == null) throw new IllegalStateException(...);
    this.currentState = next;
}
```

Declarative, easy to visualize, but harder to attach per-transition side effects.

### 3. Spring Statemachine

A full-featured library providing a declarative DSL for defining states, transitions, guards, and actions — suitable for workflow engines.

---

## Real-World Usage

| Example | Notes |
|---|---|
| TCP Connection states | `CLOSED -> LISTEN -> SYN_RECEIVED -> ESTABLISHED -> FIN_WAIT -> TIME_WAIT -> CLOSED`. Classic State pattern example from GoF. |
| `java.lang.Thread.State` | The JVM thread state machine: `NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED`. The JVM transitions threads; the states are an enum. |
| Spring Statemachine | Full Spring framework module for building hierarchical, parallel, and distributed state machines declaratively. Used in order management, payment workflows, CI/CD pipelines. |
| Workflow engines (Camunda, Activiti) | Process instances move through defined states (tasks, gateways, events). Each state type has its own execution behavior — classic State pattern at framework level. |

---

## Interview Questions

**Q: What is the key difference between State and Strategy?**

They share the same structural diagram but differ in intent and who controls transitions:

- **Strategy** encapsulates **interchangeable algorithms**. The **client** sets the strategy externally. Strategies are **stateless** and know nothing about each other. The context does not change strategy on its own.
- **State** encapsulates **behavior that varies by internal state**. Transitions are triggered **internally** — by the state objects themselves or by the context in response to events. State objects often **know about sibling states** (they instantiate the next state).

One-liner: Strategy is externally swapped; State transitions itself.

**Q: How do you prevent invalid state transitions?**

In the class-per-state approach, illegal transitions simply throw `IllegalStateException` from the state object handling the event — no special infrastructure needed. In the transition table approach, a lookup miss means no valid transition exists and you throw an exception. Both approaches make invalid transitions explicit at the state definition level rather than scattered across the context.

**Q: How would you persist a state machine (e.g., order status in a database)?**

Store the state name (string or enum value) in the database. On load, reconstruct the `Order` context with the appropriate `OrderState` instance using a factory or enum mapping. The state objects themselves are typically stateless (they hold no instance data), so they can be singletons or created fresh on demand.
