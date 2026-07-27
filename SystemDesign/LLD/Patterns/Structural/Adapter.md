# Adapter Pattern

## Intent

Convert the interface of a class into another interface that clients expect; Adapter lets classes work together that could not otherwise because of incompatible interfaces.

---

## Problem: Incompatible Interfaces

You have a working legacy `OldPaymentGateway` that talks to an external bank over a proprietary protocol. You cannot modify it — it is a third-party JAR, or it is shared by fifty other services. Your new codebase expects a clean `PaymentProcessor` interface. You need a translator layer that bridges the gap without touching either side.

This is the classic Adapter scenario: existing code that works but speaks the wrong dialect.

---

## Structure

### Object Adapter (preferred in Java — uses composition)

```
   <<interface>>
   PaymentProcessor           OldPaymentGateway
   +processPayment(...)            +makePayment(...)
          ^                        +refundTransaction(...)
          |
   PaymentAdapter
   -gateway: OldPaymentGateway   (composition)
   +processPayment(...)
     └─> translates to gateway.makePayment(...)
```

### Class Adapter (uses multiple inheritance / Java interfaces + delegation)

```
   <<interface>>          <<class>>
   PaymentProcessor    OldPaymentGateway
         ^                   ^
         |                   |
   PaymentClassAdapter
   (implements Target, extends Adaptee — only possible in Java
    when Adaptee is an interface or when you own the Adaptee class)
   +processPayment(...)
     └─> this.makePayment(...)   (inherited from OldPaymentGateway)
```

In Java, true multiple inheritance of implementation is impossible, so the class adapter pattern is approximated by implementing the target interface and extending the adaptee. When the adaptee is a concrete class you do not own, the object adapter (composition) is always the correct choice.

---

## Implementation

### Legacy System (the Adaptee — cannot be changed)

```java
import java.math.BigDecimal;

/**
 * Legacy payment gateway — third-party code, cannot be modified.
 * Uses a different naming convention and parameter structure.
 */
public class OldPaymentGateway {

    /**
     * @param merchantId  legacy merchant identifier
     * @param amountCents amount in cents as a long
     * @param currencyISO three-letter ISO currency code
     * @return legacy result code: "00" means success
     */
    public String makePayment(String merchantId, long amountCents, String currencyISO) {
        System.out.printf("[OldPaymentGateway] Charging %d %s cents for merchant %s%n",
            amountCents, currencyISO, merchantId);
        return "00"; // simulated success
    }

    /**
     * @param transactionRef legacy transaction reference
     * @param amountCents    amount to refund in cents
     * @return true if the refund was accepted
     */
    public boolean refundTransaction(String transactionRef, long amountCents) {
        System.out.printf("[OldPaymentGateway] Refunding %d cents, ref=%s%n",
            amountCents, transactionRef);
        return true;
    }
}
```

### New Target Interface

```java
import java.math.BigDecimal;
import java.util.Currency;

/**
 * The interface the rest of the application depends on.
 * All new payment integrations must implement this contract.
 */
public interface PaymentProcessor {

    /**
     * @param amount   payment amount (e.g., 19.99)
     * @param currency ISO 4217 currency
     * @param orderId  internal order identifier
     * @return PaymentResult with status and transaction reference
     */
    PaymentResult charge(BigDecimal amount, Currency currency, String orderId);

    /**
     * @param transactionId the transaction reference returned by charge()
     * @param amount        amount to refund (may be partial)
     * @return true if the refund was accepted
     */
    boolean refund(String transactionId, BigDecimal amount);
}
```

### Result DTO

```java
public class PaymentResult {
    private final boolean success;
    private final String  transactionId;
    private final String  rawCode;

    public PaymentResult(boolean success, String transactionId, String rawCode) {
        this.success       = success;
        this.transactionId = transactionId;
        this.rawCode       = rawCode;
    }

    public boolean isSuccess()       { return success; }
    public String  getTransactionId() { return transactionId; }
    public String  getRawCode()       { return rawCode; }

    @Override
    public String toString() {
        return "PaymentResult{success=" + success +
               ", transactionId='" + transactionId + "', rawCode='" + rawCode + "'}";
    }
}
```

---

### Object Adapter (recommended)

```java
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

/**
 * Object Adapter: wraps OldPaymentGateway via composition.
 * The client only ever sees the PaymentProcessor interface.
 */
public class PaymentAdapter implements PaymentProcessor {

    private static final String MERCHANT_ID  = "MERCHANT_XYZ_001";
    private static final String SUCCESS_CODE = "00";

    // Adaptee held by composition — could be injected for testability
    private final OldPaymentGateway gateway;

    public PaymentAdapter(OldPaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public PaymentResult charge(BigDecimal amount, Currency currency, String orderId) {
        // Translate: BigDecimal dollars -> long cents
        long amountCents = amount.multiply(BigDecimal.valueOf(100))
                                 .longValueExact();

        String resultCode = gateway.makePayment(MERCHANT_ID, amountCents, currency.getCurrencyCode());

        boolean success = SUCCESS_CODE.equals(resultCode);
        // Generate an internal transaction ID; in production this might come from the gateway
        String txnId = success ? "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                               : null;

        System.out.printf("[PaymentAdapter] Translated charge: order=%s -> legacy code=%s%n",
            orderId, resultCode);

        return new PaymentResult(success, txnId, resultCode);
    }

    @Override
    public boolean refund(String transactionId, BigDecimal amount) {
        long amountCents = amount.multiply(BigDecimal.valueOf(100))
                                 .longValueExact();
        boolean accepted = gateway.refundTransaction(transactionId, amountCents);
        System.out.printf("[PaymentAdapter] Refund for txn=%s accepted=%b%n",
            transactionId, accepted);
        return accepted;
    }
}
```

### Class Adapter Variant

Use only when you own or can extend the adaptee as an abstract class / interface:

```java
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

/**
 * Class Adapter: extends OldPaymentGateway AND implements PaymentProcessor.
 * Only viable when OldPaymentGateway is not final and you can subclass it.
 * Generally less flexible than the object adapter.
 */
public class PaymentClassAdapter extends OldPaymentGateway implements PaymentProcessor {

    private static final String MERCHANT_ID  = "MERCHANT_XYZ_001";
    private static final String SUCCESS_CODE = "00";

    @Override
    public PaymentResult charge(BigDecimal amount, Currency currency, String orderId) {
        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
        // Calls inherited method directly — no separate instance needed
        String resultCode = this.makePayment(MERCHANT_ID, amountCents, currency.getCurrencyCode());
        boolean success = SUCCESS_CODE.equals(resultCode);
        String txnId = success ? "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                               : null;
        return new PaymentResult(success, txnId, resultCode);
    }

    @Override
    public boolean refund(String transactionId, BigDecimal amount) {
        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
        return this.refundTransaction(transactionId, amountCents);
    }
}
```

**Limitations of class adapter:**
- Cannot adapt multiple adaptees simultaneously.
- Exposes adaptee's methods publicly (clients could call `makePayment` directly).
- Breaks if `OldPaymentGateway` is `final` (common with third-party code).

---

### Client Code

```java
import java.math.BigDecimal;
import java.util.Currency;

public class CheckoutService {

    private final PaymentProcessor paymentProcessor;

    // Constructor injection — accepts any PaymentProcessor implementation
    public CheckoutService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    public void checkout(String orderId, BigDecimal total) {
        Currency usd = Currency.getInstance("USD");

        System.out.println("Processing order: " + orderId);
        PaymentResult result = paymentProcessor.charge(total, usd, orderId);

        if (result.isSuccess()) {
            System.out.println("Payment accepted. Transaction: " + result.getTransactionId());
        } else {
            System.out.println("Payment declined. Code: " + result.getRawCode());
        }
    }

    public static void main(String[] args) {
        // Wire the adapter — CheckoutService has no idea OldPaymentGateway exists
        OldPaymentGateway legacy = new OldPaymentGateway();
        PaymentProcessor  adapter = new PaymentAdapter(legacy);

        CheckoutService service = new CheckoutService(adapter);
        service.checkout("ORDER-001", new BigDecimal("49.99"));
    }
}
```

---

## When to Use

- You want to use an existing class but its interface does not match what your code expects.
- You are integrating a third-party library you cannot or should not modify.
- You are wrapping a legacy component to fit a new, cleaner architecture without rewriting it.
- You need several disparate implementations to satisfy a common interface (a normalizing adapter layer).

## When NOT to Use

- When you own both interfaces and can simply change one of them — adapters are a workaround, not a first-class design.
- When the conceptual mismatch between the two interfaces is so large that adaptation is misleading or error-prone. A new implementation may be safer.
- When it is tempting to pile business logic into the adapter — adapters should do interface translation only, not orchestration.

---

## Variants

### Two-Way Adapter
Implements both the target and the adaptee interface, allowing objects to be used in both contexts. Rare; signals that the two interfaces are very similar and a refactor might be cleaner.

### Object Adapter vs Class Adapter

| Dimension | Object Adapter | Class Adapter |
|---|---|---|
| Mechanism | Composition | Inheritance |
| Overriding adaptee behavior | Requires subclass of adaptee | Easy (just override) |
| Adapting multiple adaptees | Possible (hold multiple fields) | Not possible |
| Adaptee visibility | Adaptee methods hidden | Adaptee methods exposed |
| Java suitability | Preferred | Limited (no multiple inheritance) |

---

## Real-World Examples

| Class | Adapts | To |
|---|---|---|
| `java.io.InputStreamReader` | `InputStream` (byte-oriented) | `Reader` (character-oriented) |
| `Arrays.asList(T... a)` | Array | `List<T>` |
| `Collections.enumeration(Collection)` | `Collection.iterator()` | `Enumeration` |
| Spring's `HandlerAdapter` | Various controller types | Uniform `handle()` contract |
| JDBC `ResultSet` wrappers | Raw JDBC `ResultSet` | ORM entity representation |

`new InputStreamReader(new FileInputStream("data.txt"), StandardCharsets.UTF_8)` is a real-world object adapter you use every day.

---

## Interview Questions

**Q: What is the difference between Adapter and Facade?**

- **Adapter** translates one interface into another that already exists; it serves an existing contract. The target interface is defined by the client's needs.
- **Facade** invents a new, simpler interface over a subsystem; it defines a new, convenient surface. No pre-existing target interface is being matched.

You use Adapter when you cannot change an existing interface. You use Facade when you want to hide complexity behind a new surface.

**Q: Object adapter vs class adapter — which is better in Java and why?**

Object adapter is almost always better in Java because:
1. Java does not support multiple inheritance of implementation. The class adapter requires extending the adaptee, which locks you out of other inheritance hierarchies.
2. Object adapter works even when the adaptee is `final` or a third-party class.
3. Object adapter lets you adapt multiple adaptee instances or swap the adaptee at runtime.
4. The class adapter leaks the adaptee's public API to clients.

**Q: Can an adapter change the behavior of the adaptee?**

It should not. An adapter is a structural translation layer, not a behavioral modifier. If you need to add behavior, combine Adapter with Decorator: first adapt the interface, then decorate the adapter.
