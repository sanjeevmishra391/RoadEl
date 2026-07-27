# Facade Pattern

## Intent

Provide a unified interface to a set of interfaces in a subsystem; Facade defines a higher-level interface that makes the subsystem easier to use.

---

## Problem: Tight Coupling to Complex Subsystems

An e-commerce checkout flow requires coordinating inventory reservation, payment processing, shipping label creation, and customer notification. Without a facade, every client — the REST controller, the mobile API handler, the batch reorder job — must know about all four subsystems, call them in the correct order, handle partial failures, and manage rollback logic.

The result is duplicated orchestration code scattered across clients, and any change in one subsystem requires touching every caller.

A Facade consolidates this coordination into one place.

---

## Structure

```
           Client
              |
              v
  +-------------------------+
  |  OrderFulfillmentFacade |
  |    +placeOrder()        |
  +----+--------------------+
       |
       +-----------> InventoryService
       |               +reserve(sku, qty)
       |               +release(sku, qty)
       |
       +-----------> PaymentService
       |               +charge(amount, currency, paymentToken)
       |               +refund(transactionId, amount)
       |
       +-----------> ShippingService
       |               +createShipment(address, items)
       |               +cancelShipment(shipmentId)
       |
       +-----------> NotificationService
                       +sendOrderConfirmation(customerId, orderId)
                       +sendOrderFailure(customerId, reason)
```

The facade owns the orchestration. Subsystems are independent and have no knowledge of each other or the facade.

---

## Implementation

### Subsystem Classes

```java
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// ─── InventoryService ────────────────────────────────────────────

public class InventoryService {

    public boolean reserve(String sku, int quantity) {
        System.out.printf("[InventoryService] Reserving %d units of %s%n", quantity, sku);
        // Returns false if stock is insufficient
        return true; // simulated success
    }

    public void release(String sku, int quantity) {
        System.out.printf("[InventoryService] Releasing %d units of %s%n", quantity, sku);
    }

    public boolean hasSufficientStock(String sku, int quantity) {
        System.out.printf("[InventoryService] Checking stock for %s (qty=%d)%n", sku, quantity);
        return true; // simulated
    }
}

// ─── PaymentService ───────────────────────────────────────────────

public class PaymentResult {
    private final boolean success;
    private final String  transactionId;
    private final String  failureReason;

    private PaymentResult(boolean success, String transactionId, String failureReason) {
        this.success       = success;
        this.transactionId = transactionId;
        this.failureReason = failureReason;
    }

    public static PaymentResult success(String txnId) {
        return new PaymentResult(true, txnId, null);
    }

    public static PaymentResult failure(String reason) {
        return new PaymentResult(false, null, reason);
    }

    public boolean isSuccess()       { return success; }
    public String  getTransactionId() { return transactionId; }
    public String  getFailureReason() { return failureReason; }
}

public class PaymentService {

    public PaymentResult charge(BigDecimal amount, String currency, String paymentToken) {
        System.out.printf("[PaymentService] Charging %s %s with token %s%n",
            amount, currency, paymentToken);
        // Simulate a successful charge
        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentResult.success(txnId);
    }

    public void refund(String transactionId, BigDecimal amount) {
        System.out.printf("[PaymentService] Refunding %s for txn %s%n", amount, transactionId);
    }
}

// ─── ShippingService ──────────────────────────────────────────────

public class ShipmentDetails {
    private final String shipmentId;
    private final String trackingNumber;

    public ShipmentDetails(String shipmentId, String trackingNumber) {
        this.shipmentId     = shipmentId;
        this.trackingNumber = trackingNumber;
    }

    public String getShipmentId()     { return shipmentId; }
    public String getTrackingNumber() { return trackingNumber; }

    @Override
    public String toString() {
        return "ShipmentDetails{id='" + shipmentId + "', tracking='" + trackingNumber + "'}";
    }
}

public class ShippingService {

    public ShipmentDetails createShipment(String address, List<String> skus) {
        System.out.printf("[ShippingService] Creating shipment to '%s' for SKUs: %s%n",
            address, skus);
        String shipmentId = "SHIP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String tracking   = "1Z999AA1" + (int)(Math.random() * 100000000);
        return new ShipmentDetails(shipmentId, tracking);
    }

    public void cancelShipment(String shipmentId) {
        System.out.printf("[ShippingService] Cancelling shipment %s%n", shipmentId);
    }
}

// ─── NotificationService ──────────────────────────────────────────

public class NotificationService {

    public void sendOrderConfirmation(String customerId, String orderId, String trackingNumber) {
        System.out.printf("[NotificationService] Sending confirmation to customer %s: " +
            "order=%s, tracking=%s%n", customerId, orderId, trackingNumber);
    }

    public void sendOrderFailure(String customerId, String orderId, String reason) {
        System.out.printf("[NotificationService] Sending failure notice to customer %s: " +
            "order=%s, reason=%s%n", customerId, orderId, reason);
    }
}
```

### Domain Objects

```java
import java.math.BigDecimal;
import java.util.List;

public class OrderItem {
    private final String sku;
    private final int    quantity;
    private final BigDecimal unitPrice;

    public OrderItem(String sku, int quantity, BigDecimal unitPrice) {
        this.sku       = sku;
        this.quantity  = quantity;
        this.unitPrice = unitPrice;
    }

    public String     getSku()       { return sku; }
    public int        getQuantity()  { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}

public class OrderRequest {
    private final String          orderId;
    private final String          customerId;
    private final List<OrderItem> items;
    private final String          shippingAddress;
    private final String          paymentToken;
    private final String          currency;

    public OrderRequest(String orderId, String customerId, List<OrderItem> items,
                        String shippingAddress, String paymentToken, String currency) {
        this.orderId         = orderId;
        this.customerId      = customerId;
        this.items           = items;
        this.shippingAddress = shippingAddress;
        this.paymentToken    = paymentToken;
        this.currency        = currency;
    }

    public String          getOrderId()         { return orderId; }
    public String          getCustomerId()      { return customerId; }
    public List<OrderItem> getItems()           { return items; }
    public String          getShippingAddress() { return shippingAddress; }
    public String          getPaymentToken()    { return paymentToken; }
    public String          getCurrency()        { return currency; }

    public BigDecimal getTotal() {
        return items.stream()
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

public class OrderResult {
    private final boolean success;
    private final String  orderId;
    private final String  trackingNumber;
    private final String  failureReason;

    private OrderResult(boolean success, String orderId, String trackingNumber, String failureReason) {
        this.success       = success;
        this.orderId       = orderId;
        this.trackingNumber = trackingNumber;
        this.failureReason = failureReason;
    }

    public static OrderResult success(String orderId, String trackingNumber) {
        return new OrderResult(true, orderId, trackingNumber, null);
    }

    public static OrderResult failure(String orderId, String reason) {
        return new OrderResult(false, orderId, null, reason);
    }

    public boolean isSuccess()        { return success; }
    public String  getOrderId()       { return orderId; }
    public String  getTrackingNumber() { return trackingNumber; }
    public String  getFailureReason() { return failureReason; }

    @Override
    public String toString() {
        if (success) {
            return "OrderResult{SUCCESS, orderId='" + orderId +
                   "', tracking='" + trackingNumber + "'}";
        }
        return "OrderResult{FAILED, orderId='" + orderId +
               "', reason='" + failureReason + "'}";
    }
}
```

### The Facade

```java
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OrderFulfillmentFacade is the single entry point for placing an order.
 *
 * It coordinates InventoryService, PaymentService, ShippingService, and
 * NotificationService — hiding all orchestration, error handling, and
 * compensation logic from the client.
 *
 * Clients call one method: placeOrder().
 */
public class OrderFulfillmentFacade {

    private final InventoryService    inventoryService;
    private final PaymentService      paymentService;
    private final ShippingService     shippingService;
    private final NotificationService notificationService;

    // Dependencies injected — the facade does not create subsystems itself
    public OrderFulfillmentFacade(InventoryService    inventoryService,
                                   PaymentService      paymentService,
                                   ShippingService     shippingService,
                                   NotificationService notificationService) {
        this.inventoryService    = inventoryService;
        this.paymentService      = paymentService;
        this.shippingService     = shippingService;
        this.notificationService = notificationService;
    }

    /**
     * Places an order by orchestrating all subsystems.
     * Handles partial failures with compensating actions (rollback).
     */
    public OrderResult placeOrder(OrderRequest request) {
        System.out.println("=== Starting order fulfillment: " + request.getOrderId() + " ===");

        // Step 1: Verify and reserve inventory for all line items
        for (OrderItem item : request.getItems()) {
            boolean reserved = inventoryService.reserve(item.getSku(), item.getQuantity());
            if (!reserved) {
                String reason = "Insufficient stock for SKU: " + item.getSku();
                notificationService.sendOrderFailure(
                    request.getCustomerId(), request.getOrderId(), reason);
                return OrderResult.failure(request.getOrderId(), reason);
            }
        }

        // Step 2: Charge payment
        PaymentResult payment = paymentService.charge(
            request.getTotal(),
            request.getCurrency(),
            request.getPaymentToken()
        );

        if (!payment.isSuccess()) {
            // Compensate: release all reserved inventory
            request.getItems().forEach(item ->
                inventoryService.release(item.getSku(), item.getQuantity()));

            String reason = "Payment declined: " + payment.getFailureReason();
            notificationService.sendOrderFailure(
                request.getCustomerId(), request.getOrderId(), reason);
            return OrderResult.failure(request.getOrderId(), reason);
        }

        // Step 3: Create shipment
        List<String> skus = request.getItems().stream()
                                   .map(OrderItem::getSku)
                                   .collect(Collectors.toList());

        ShipmentDetails shipment = shippingService.createShipment(
            request.getShippingAddress(), skus);

        // Step 4: Send confirmation to customer
        notificationService.sendOrderConfirmation(
            request.getCustomerId(),
            request.getOrderId(),
            shipment.getTrackingNumber()
        );

        System.out.println("=== Order fulfillment complete: " + request.getOrderId() + " ===");
        return OrderResult.success(request.getOrderId(), shipment.getTrackingNumber());
    }
}
```

### Client Code

```java
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * The REST controller (or any other client) has zero knowledge of
 * InventoryService, PaymentService, ShippingService, or NotificationService.
 * It simply calls the facade.
 */
public class OrderController {

    private final OrderFulfillmentFacade facade;

    public OrderController(OrderFulfillmentFacade facade) {
        this.facade = facade;
    }

    public OrderResult handleCheckout(String customerId, String paymentToken) {
        List<OrderItem> items = Arrays.asList(
            new OrderItem("SKU-LAPTOP",  1, new BigDecimal("999.00")),
            new OrderItem("SKU-MOUSE",   2, new BigDecimal("29.99"))
        );

        OrderRequest request = new OrderRequest(
            "ORD-20240715-001",
            customerId,
            items,
            "123 Main St, San Francisco, CA 94102",
            paymentToken,
            "USD"
        );

        return facade.placeOrder(request);
    }

    public static void main(String[] args) {
        // Wire up subsystems (in production: Spring/Guice/Dagger does this)
        OrderFulfillmentFacade facade = new OrderFulfillmentFacade(
            new InventoryService(),
            new PaymentService(),
            new ShippingService(),
            new NotificationService()
        );

        OrderController controller = new OrderController(facade);
        OrderResult result = controller.handleCheckout("CUST-007", "tok_visa_4242");

        System.out.println("\nResult: " + result);
    }
}
```

**Output:**
```
=== Starting order fulfillment: ORD-20240715-001 ===
[InventoryService] Reserving 1 units of SKU-LAPTOP
[InventoryService] Reserving 2 units of SKU-MOUSE
[PaymentService] Charging 1058.98 USD with token tok_visa_4242
[ShippingService] Creating shipment to '123 Main St...' for SKUs: [SKU-LAPTOP, SKU-MOUSE]
[NotificationService] Sending confirmation to customer CUST-007: order=ORD-20240715-001, tracking=1Z999AA1...
=== Order fulfillment complete: ORD-20240715-001 ===

Result: OrderResult{SUCCESS, orderId='ORD-20240715-001', tracking='1Z999AA1...'}
```

---

## When to Use

- You want to provide a simple interface to a complex subsystem for the most common use cases.
- There are many dependencies between clients and implementation details of a subsystem. Introduce a facade to decouple clients from subsystem classes.
- You want to layer your subsystems: each layer exposes a facade that is the only entry point to that layer for layers above it.
- When introducing a legacy system into a new architecture — the facade isolates new code from old interfaces.

## When NOT to Use

- **When the facade becomes a god object.** If every feature of the system funnels through one class with hundreds of methods, the facade has become an anti-pattern. Split it into multiple, cohesive facades.
- When your clients actually need fine-grained access to subsystem internals — forcing everything through the facade wastes the flexibility the subsystems provide.
- When the facade introduces an artificial layer with no real simplification, only pass-through calls. This adds indirection without benefit.
- When you need to expose the subsystem to external consumers (e.g., a public API) where the subsystem's richness is a feature, not a burden.

---

## Variants

### Layered Facades
Large systems often have facades at multiple levels. An application layer facade calls domain layer facades, which in turn call infrastructure layer facades. Each layer is shielded from changes in the layer below.

```
  ApplicationFacade
    └─> OrderFulfillmentFacade  (domain layer)
          └─> PaymentGatewayFacade  (infrastructure layer)
                └─> raw HTTP client
```

### Facade vs Service Layer
In DDD and Clean Architecture the "service layer" serves the same purpose as a facade: it orchestrates domain objects and infrastructure to fulfill use cases. The distinction is mainly vocabulary. A service layer typically also owns transaction boundaries and domain events; a pure facade may not.

```
// Service layer equivalent — same orchestration role, richer context
@Service
@Transactional
public class OrderApplicationService {
    public OrderResult placeOrder(PlaceOrderCommand command) { ... }
}
```

---

## Real-World Examples

| Context | Facade | Hides |
|---|---|---|
| Spring `@Service` | `OrderService.placeOrder()` | Repository calls, domain logic, event publishing |
| SLF4J | `LoggerFactory.getLogger()` | Log4j, Logback, JUL implementations |
| `javax.faces` / JSF | Managed bean methods | JSF lifecycle, EL, view state |
| `java.net.URL.openStream()` | Single call | DNS resolution, socket connection, HTTP negotiation |
| AWS SDK high-level clients | `S3Client.putObject()` | HTTP signing, retry, multipart negotiation |

SLF4J is a canonical example: your code calls `logger.info(...)` on the SLF4J `Logger` interface. Whether the underlying implementation is Logback, Log4j2, or `java.util.logging` is completely hidden.

---

## Interview Questions

**Q: What is the difference between Facade and Mediator?**

Both patterns centralize logic and reduce direct dependencies, but for different purposes:

| Dimension | Facade | Mediator |
|---|---|---|
| Direction | Clients -> Facade -> Subsystems | Colleagues <-> Mediator <-> Colleagues |
| Subsystem awareness | Subsystems have no knowledge of the facade | Colleagues know about the mediator (send messages to it) |
| Communication | One-way: client calls facade, subsystems do not call back through facade | Two-way: colleagues communicate through the mediator |
| Primary purpose | Simplify a complex interface | Reduce chaotic many-to-many coupling between peers |
| Example | `OrderFulfillmentFacade` | Chat room: users send messages to mediator, mediator distributes |

Use Facade when you want to simplify access to a subsystem from above. Use Mediator when you want to untangle many-to-many communication between objects at the same level.

**Q: Can subsystems call the facade?**

They should not. The facade should be a one-way simplification layer. If subsystems start calling each other through the facade, it has become a Mediator (which is a different pattern with different tradeoffs). Keep subsystems independent and have them communicate only when the facade explicitly coordinates them.

**Q: How do you prevent a facade from becoming a god object?**

- Keep each facade responsible for one cohesive use-case domain (order fulfillment, not "everything e-commerce").
- If a facade's method count exceeds roughly 10-15, consider splitting it by subdomain.
- Facades should delegate, not implement. No business logic should live in the facade itself.
- Use multiple, narrower facades rather than one wide one: `OrderFacade`, `CatalogFacade`, `CustomerFacade`.
