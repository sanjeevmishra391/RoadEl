# Strategy Pattern

## Intent

Define a family of algorithms, encapsulate each one, and make them interchangeable so that the algorithm can vary independently from the clients that use it.

---

## Problem

Without the Strategy pattern, algorithm variants live inside a single class as branching `if-else` or `switch` blocks. Every time a new payment method is added, the `PaymentProcessor` class must be modified — violating the Open/Closed Principle. The class grows in size, becomes harder to test (one large unit), and changes to one branch can inadvertently break others.

```java
// Anti-pattern: algorithm variants baked into the context
public class PaymentProcessor {
    public void processPayment(String type, double amount) {
        if (type.equals("CREDIT_CARD")) {
            // 40 lines of credit card logic
        } else if (type.equals("PAYPAL")) {
            // 30 lines of PayPal logic
        } else if (type.equals("CRYPTO")) {
            // 50 lines of crypto logic
        }
        // Adding APPLE_PAY means touching this file again
    }
}
```

---

## Structure

```
+--------------------+          +----------------------+
|      Context       |--------->|  <<interface>>       |
|--------------------|          |      Strategy        |
| - strategy         |          |----------------------|
|--------------------|          | + execute(context)   |
| + setStrategy(s)   |          +----------------------+
| + executeStrategy()|                     ^
+--------------------+          +----------+-----------+
                                |          |           |
                    +-----------+--+ +-----+------+ +--+----------+
                    | ConcreteA    | | ConcreteB  | | ConcreteC   |
                    |--------------| |------------| |-------------|
                    | + execute()  | | + execute()| | + execute() |
                    +--------------+ +------------+ +-------------+
```

---

## Implementation

### Domain: Payment Processing

```java
// ---------------------------------------------------------------
// Strategy interface
// ---------------------------------------------------------------
public interface PaymentStrategy {
    void pay(double amount);
    String getName();
}

// ---------------------------------------------------------------
// ConcreteStrategy A — Credit Card
// ---------------------------------------------------------------
public class CreditCardStrategy implements PaymentStrategy {

    private final String cardNumber;
    private final String cvv;
    private final String expiryDate;

    public CreditCardStrategy(String cardNumber, String cvv, String expiryDate) {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expiryDate = expiryDate;
    }

    @Override
    public void pay(double amount) {
        System.out.printf("CreditCard: Charging $%.2f to card ending in %s%n",
                amount, cardNumber.substring(cardNumber.length() - 4));
    }

    @Override
    public String getName() { return "CREDIT_CARD"; }
}

// ---------------------------------------------------------------
// ConcreteStrategy B — PayPal
// ---------------------------------------------------------------
public class PayPalStrategy implements PaymentStrategy {

    private final String email;
    private final String password;

    public PayPalStrategy(String email, String password) {
        this.email = email;
        this.password = password;
    }

    @Override
    public void pay(double amount) {
        System.out.printf("PayPal: Debiting $%.2f from account %s%n", amount, email);
    }

    @Override
    public String getName() { return "PAYPAL"; }
}

// ---------------------------------------------------------------
// ConcreteStrategy C — Cryptocurrency
// ---------------------------------------------------------------
public class CryptoStrategy implements PaymentStrategy {

    private final String walletAddress;
    private final String coin;

    public CryptoStrategy(String walletAddress, String coin) {
        this.walletAddress = walletAddress;
        this.coin = coin;
    }

    @Override
    public void pay(double amount) {
        System.out.printf("Crypto: Transferring %.6f %s to wallet %s%n",
                amount / 40000.0, coin, walletAddress);
    }

    @Override
    public String getName() { return coin.toUpperCase(); }
}

// ---------------------------------------------------------------
// Context — ShoppingCart
// Holds a reference to the current strategy and delegates to it.
// The strategy can be swapped at runtime with setPaymentStrategy().
// ---------------------------------------------------------------
public class ShoppingCart {

    private PaymentStrategy paymentStrategy;
    private double totalAmount;

    public ShoppingCart(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    // Runtime switching — user changes payment method at checkout
    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        System.out.println("Switching payment method to: " + paymentStrategy.getName());
        this.paymentStrategy = paymentStrategy;
    }

    public void addItem(String name, double price) {
        System.out.println("Added: " + name + " ($" + price + ")");
        totalAmount += price;
    }

    public void checkout() {
        if (paymentStrategy == null) {
            throw new IllegalStateException("No payment strategy set");
        }
        System.out.printf("Checking out. Total: $%.2f%n", totalAmount);
        paymentStrategy.pay(totalAmount);
        totalAmount = 0;
    }
}

// ---------------------------------------------------------------
// Strategy Registry — maps strategy name to instance
// Eliminates the need for if-else at the selection site
// ---------------------------------------------------------------
import java.util.HashMap;
import java.util.Map;

public class PaymentStrategyRegistry {

    private final Map<String, PaymentStrategy> registry = new HashMap<>();

    public void register(PaymentStrategy strategy) {
        registry.put(strategy.getName(), strategy);
    }

    public PaymentStrategy get(String name) {
        PaymentStrategy strategy = registry.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown payment strategy: " + name);
        }
        return strategy;
    }
}

// ---------------------------------------------------------------
// Java 8 Functional Approach
// PaymentStrategy has a single abstract method -> @FunctionalInterface
// ---------------------------------------------------------------
@FunctionalInterface
public interface PaymentStrategy {
    void pay(double amount);
    // getName() removed for pure functional interface
}

public class FunctionalStrategyDemo {

    public static void main(String[] args) {
        // Lambda as strategy — no class declaration needed
        PaymentStrategy creditCard = amount ->
                System.out.printf("Lambda CreditCard: Charging $%.2f%n", amount);

        PaymentStrategy paypal = amount ->
                System.out.printf("Lambda PayPal: Debiting $%.2f%n", amount);

        // Method reference as strategy
        PaymentStrategy crypto = FunctionalStrategyDemo::payCrypto;

        ShoppingCart cart = new ShoppingCart(creditCard);
        cart.addItem("Laptop", 999.99);
        cart.checkout();

        // Switch to PayPal at runtime
        cart.setPaymentStrategy(paypal);
        cart.addItem("Mouse", 29.99);
        cart.checkout();
    }

    private static void payCrypto(double amount) {
        System.out.printf("Crypto method ref: $%.2f%n", amount);
    }
}

// ---------------------------------------------------------------
// Comparator as a classic Strategy example
// java.util.Comparator IS a strategy: it encapsulates a comparison
// algorithm and is passed into Collections.sort / List.sort.
// ---------------------------------------------------------------
import java.util.*;

public class ComparatorStrategyDemo {

    public static void main(String[] args) {
        List<String> names = Arrays.asList("Charlie", "Alice", "Bob", "Dave");

        // Strategy 1: natural order
        names.sort(Comparator.naturalOrder());
        System.out.println("Natural: " + names);

        // Strategy 2: reverse order
        names.sort(Comparator.reverseOrder());
        System.out.println("Reverse: " + names);

        // Strategy 3: by length, then alphabetically
        names.sort(Comparator.comparingInt(String::length)
                              .thenComparing(Comparator.naturalOrder()));
        System.out.println("By length then alpha: " + names);

        // Strategy 4: custom lambda
        names.sort((a, b) -> b.compareToIgnoreCase(a));
        System.out.println("Reverse case-insensitive: " + names);
    }
}

// ---------------------------------------------------------------
// Client / Full Demo
// ---------------------------------------------------------------
public class StrategyDemo {

    public static void main(String[] args) {
        PaymentStrategy cc = new CreditCardStrategy("4111111111111234", "123", "12/28");
        PaymentStrategy pp = new PayPalStrategy("user@example.com", "secret");
        PaymentStrategy crypto = new CryptoStrategy("0xABCDEF1234567890", "BTC");

        ShoppingCart cart = new ShoppingCart(cc);
        cart.addItem("Keyboard", 79.99);
        cart.addItem("Monitor", 299.99);
        cart.checkout();

        System.out.println();
        cart.setPaymentStrategy(pp);
        cart.addItem("USB Hub", 29.99);
        cart.checkout();

        System.out.println();
        cart.setPaymentStrategy(crypto);
        cart.addItem("Graphics Card", 499.99);
        cart.checkout();
    }
}
```

**Expected output:**
```
Added: Keyboard ($79.99)
Added: Monitor ($299.99)
Checking out. Total: $379.98
CreditCard: Charging $379.98 to card ending in 1234

Switching payment method to: PAYPAL
Added: USB Hub ($29.99)
Checking out. Total: $29.99
PayPal: Debiting $29.99 from account user@example.com

Switching payment method to: BTC
Added: Graphics Card ($499.99)
Checking out. Total: $499.99
Crypto: Transferring 0.012500 BTC to wallet 0xABCDEF1234567890
```

---

## When to Use

- When you need to define a class that will have one behavior that is similar to other behaviors in a list, and you want to switch between these behaviors algorithmically at runtime.
- When you have a class doing one specific thing in many different ways and you want to eliminate `if-else` / `switch` sprawl.
- When different variants of an algorithm require different data structures or external dependencies that should not leak into the context class.
- When algorithm selection should be configurable (e.g., via configuration, feature flags, or user preference) without recompiling the context.

## When NOT to Use

- When you only have two or three strategies and they never change — the added interface and indirection cost outweigh the benefit.
- When all strategies share so little logic that each is a one-liner; a plain lambda passed directly is cleaner.
- When clients never need to switch strategies; just pick the right implementation at construction time and consider Template Method instead.
- When strategies need to be composed or chained — the Decorator or Chain of Responsibility pattern may be more appropriate.

---

## Variants

### 1. Functional Strategy (Java 8+)

When the strategy interface has exactly one abstract method, annotate it with `@FunctionalInterface` and pass lambdas or method references. No concrete strategy classes needed. This is the dominant modern form.

### 2. Strategy Registry / Map

Pre-register strategies by key in a `Map<String, Strategy>`. The context performs a map lookup at runtime. Eliminates the caller-side `if-else` for strategy selection.

```java
registry.register(new CreditCardStrategy(...));
registry.register(new PayPalStrategy(...));
PaymentStrategy chosen = registry.get(userSelectedMethod);
cart.setPaymentStrategy(chosen);
```

### 3. Strategy with Default

Provide a default strategy in the context constructor; let callers override. Common in frameworks where sensible defaults prevent null checks.

---

## Real-World Usage

| API | Notes |
|---|---|
| `java.util.Comparator` | The canonical Java strategy example. Passed into `List.sort()`, `TreeMap`, `Arrays.sort()` to vary comparison behavior without changing the container. |
| `Comparator.comparing()` | Factory method that creates a strategy from a key extractor. Composable with `thenComparing()`. |
| `Collections.sort(list, comparator)` | The sort algorithm is fixed; the ordering strategy (comparator) is plugged in. |
| `Spring ResourceLoader` | Different `ResourceLoader` implementations (classpath, filesystem, URL) are strategies for loading resources. The context (application context) delegates to whichever is configured. |

---

## Interview Questions

**Q: What is the key difference between Strategy and State?**

Strategy and State share the same UML structure (a context holding a reference to an interface with concrete implementations), but their intent and transition model differ fundamentally.

- **Strategy** encapsulates **algorithms** that are **interchangeable**. The context does not decide which strategy to use — the **client** sets it externally (`setStrategy()`). The strategies are **stateless** and unaware of each other.
- **State** encapsulates **behavior that changes based on internal state**. Transitions are triggered from **within** the concrete state classes or the context itself — the **client does not call `setState()`**. States are often **aware of other states** (they decide what the next state is).

In short: Strategy is **externally swapped**, State **transitions itself internally**.

**Q: How does the Strategy pattern relate to the Open/Closed Principle?**

The context class is closed for modification — adding a new algorithm means adding a new strategy class without touching the context. This is the canonical demonstration of OCP in behavioral patterns.

**Q: Strategy vs Template Method: when do you prefer each?**

Template Method uses **inheritance** (compile-time binding): the skeleton is in an abstract class, subclasses fill in steps. Strategy uses **composition** (runtime binding): the algorithm is in a separate object injected into the context. Prefer Strategy when you need runtime switching, when you want to avoid the inheritance hierarchy explosion of Template Method, or when you are using a language/framework that discourages deep inheritance. Prefer Template Method when the algorithm skeleton is complex, shared setup/teardown is substantial, and subclassing is acceptable.
