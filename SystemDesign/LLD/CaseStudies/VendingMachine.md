# Vending Machine — LLD Case Study

---

## 1. Problem Statement

> "Design a vending machine that accepts coins and notes, lets a user select a product, dispenses it, and returns change."

The interviewer is testing whether you can model a state machine cleanly, handle partial payment accumulation, and deal with concurrent inventory access.

---

## 2. Clarifying Questions

| # | Question | Expected Answer |
|---|----------|-----------------|
| 1 | What denominations of coins and notes should be supported? | Coins: 1, 2, 5, 10. Notes: 10, 20, 50, 100, 500, 2000. |
| 2 | What if the machine cannot make exact change? | For MVP, assume it always can. Mention this as an extension. |
| 3 | Can the user cancel after inserting money? | Yes — return all inserted money. |
| 4 | Can multiple users interact simultaneously? | Handle concurrent inventory access safely. |
| 5 | How does restocking work — by admin or automated? | Admin calls a restock method; out of scope for the user-facing flow. |
| 6 | Should the machine track its own coin float? | Yes, for the exact-change problem — mention it as an extension. |
| 7 | What if a product runs out mid-selection? | Block dispense; return payment; go back to Idle. |
| 8 | Should we support card payments? | Not now, but design must allow it as an extension. |

---

## 3. Requirements

### Functional
- User selects a product from the inventory.
- User inserts coins/notes incrementally; machine accumulates payment.
- Once payment >= product price, machine transitions to dispense-ready.
- Machine dispenses the product and decrements inventory.
- Machine calculates and returns change.
- Admin can restock products and check inventory.

### Non-Functional
- Thread-safe inventory updates — two concurrent purchases must not over-decrement.
- Clear, illegal-operation feedback for every invalid action (e.g., inserting a coin before selecting a product).
- Single instance of the machine (Singleton).
- State transitions must be exhaustive — no undefined behaviour for any (state, action) pair.

---

## 4. Entities & Responsibilities

| Class / Interface | Single Responsibility |
|---|---|
| `VendingMachineState` (interface) | Declares the five operations any state must handle: `selectProduct`, `insertCoin`, `insertNote`, `dispenseProduct`, `returnChange`. |
| `IdleState` | Handles the "no product selected" state. Only `selectProduct` does real work; all others print guidance. |
| `ReadyState` | Handles the "product selected, awaiting payment" state. Accepts coins/notes, accumulates payment, auto-advances to DispenseState when paid. |
| `DispenseState` | Handles the "sufficient payment received" state. Only `dispenseProduct` does real work; decrements inventory, advances to ReturnChangeState. |
| `ReturnChangeState` | Handles the "product dispensed, change pending" state. Calculates and returns change, resets machine, returns to IdleState. |
| `VendingMachine` | Context class: owns current state, holds payment total, holds selected product, delegates all actions to current state. Also the Singleton. |
| `Inventory` | Thread-safe product→quantity map. Only class that mutates stock counts. |
| `Product` | Immutable value object: name + price. |
| `Coin` (enum) | Denomination + value for coins. |
| `Note` (enum) | Denomination + value for notes. |

---

## 5. Class Diagram

```
+---------------------------+
|  <<interface>>            |
|  VendingMachineState      |
|---------------------------|
| + selectProduct(Product)  |
| + insertCoin(Coin)        |
| + insertNote(Note)        |
| + dispenseProduct()       |
| + returnChange()          |
+---------------------------+
       ^    ^    ^    ^
       |    |    |    |
  +----+ +--+ +--+ +------+
  |      |    |    |      |
  |   IdleState   ReadyState
  |      |    |    |
DispenseState  ReturnChangeState

(All four implement VendingMachineState)

                    +---------------------------------+
                    |        VendingMachine           |
                    |---------------------------------|
                    | - instance: VendingMachine      |  <<Singleton>>
                    | - currentState: VMState         |
                    | - selectedProduct: Product      |
                    | - totalPayment: double          |
                    | - inventory: Inventory          |
                    | - idleState: VMState            |
                    | - readyState: VMState           |
                    | - dispenseState: VMState        |
                    | - returnChangeState: VMState    |
                    +---------------------------------+
                    | + getInstance(): VendingMachine |
                    | + selectProduct(Product)        |  delegates to currentState
                    | + insertCoin(Coin)              |  delegates to currentState
                    | + insertNote(Note)              |  delegates to currentState
                    | + dispenseProduct()             |  delegates to currentState
                    | + returnChange()                |  delegates to currentState
                    | + setState(VMState)             |  called BY states to advance
                    | + addCoin(Coin)                 |  accumulates payment
                    | + addNote(Note)                 |  accumulates payment
                    +---------------------------------+
                           |               |
                    composes               composes
                           |               |
               +-----------+    +----------+
               | Inventory |    |  Product |
               |-----------|    |----------|
               | - products:    | - name   |
               |  CHMap<P,Int>  | - price  |
               |-----------|    |----------|
               | isAvailable()  | getName()|
               | addProduct()   | getPrice()|
               | updateQty()    +----------+
               +-----------+

+------------+          +-----------+
|  <<enum>>  |          |  <<enum>> |
|   Coin     |          |   Note    |
|------------|          |-----------|
| ONE(1)     |          | TEN(10)   |
| TWO(2)     |          | TWENTY(20)|
| FIVE(5)    |          | FIFTY(50) |
| TEN(10)    |          | HUNDRED   |
|------------|          | FIVE_HUNDRED|
| getValue() |          | TWO_THOUSAND|
+------------+          +-----------+
```

**State transition diagram:**

```
  [Idle] ──selectProduct(available)──> [Ready]
  [Ready] ──payment >= price──────────> [Dispense]
  [Dispense] ──dispenseProduct()──────> [ReturnChange]
  [ReturnChange] ──returnChange()─────> [Idle]

  [Any state] ──invalid action──> print guidance message, stay in same state
```

---

## 6. Design Patterns Used

### State Pattern — the entire machine
**Trigger:** The vending machine's valid operations change completely depending on its current phase. Without State, every method on `VendingMachine` would be a long `switch(currentState)` block. Adding a new state (e.g., `MaintenanceState`) would require modifying every method.

**How it works here:**
- `VendingMachine` is the **Context** — it holds a reference to the current state and routes every call through it.
- Each concrete state (IdleState, ReadyState, etc.) is a **State** — it knows which operations are valid *right now* and which state to transition to next.
- States call back into the Context (`vendingMachine.setState(...)`) to trigger transitions. This keeps the Context dumb about transition logic.

```java
// Context delegates — no if/switch needed:
public void insertCoin(Coin coin) {
    currentState.insertCoin(coin);  // ReadyState accumulates; IdleState says "select first"
}

// State drives the transition:
// Inside ReadyState.insertCoin():
private void checkPaymentStatus() {
    if (vendingMachine.getTotalPayment() >= vendingMachine.getSelectedProduct().getPrice()) {
        vendingMachine.setState(vendingMachine.getDispenseState());  // advance
    }
}
```

**Why not just a big if/else?** With 4 states and 5 operations, that's 20 branches. With 6 states and 7 operations (after extensions), it's 42. The State pattern keeps each state's logic in one class and makes it impossible to forget a case.

### Singleton Pattern — VendingMachine instance
**Trigger:** There is exactly one physical vending machine. If multiple code paths create separate instances, they each have their own inventory and payment state — silently inconsistent.

```java
public static synchronized VendingMachine getInstance() {
    if (instance == null)
        instance = new VendingMachine();
    return instance;
}
```

The `synchronized` keyword prevents two threads from both seeing `instance == null` and both constructing a new machine. For production, prefer the **initialization-on-demand holder idiom** (no synchronisation on every call):

```java
private static class Holder {
    private static final VendingMachine INSTANCE = new VendingMachine();
}
public static VendingMachine getInstance() {
    return Holder.INSTANCE;  // JVM class loading guarantees single initialisation
}
```

### Factory Pattern — Product creation
**Trigger:** `VendingMachineDemo` constructs `Product` objects directly with `new`. If products gain subtypes (PerishableProduct with expiry, WeightedProduct by gram), all callers must change.

```java
public class ProductFactory {
    public static Product create(String name, double price) {
        return new Product(name, price);
    }

    public static Product createPerishable(String name, double price, LocalDate expiry) {
        return new PerishableProduct(name, price, expiry);
    }
}
```

---

## 7. Core Implementation

### State interface and IdleState

```java
public interface VendingMachineState {
    void selectProduct(Product product);
    void insertCoin(Coin coin);
    void insertNote(Note note);
    void dispenseProduct();
    void returnChange();
}

public class IdleState implements VendingMachineState {
    private final VendingMachine vm;

    public IdleState(VendingMachine vm) { this.vm = vm; }

    @Override
    public void selectProduct(Product product) {
        if (vm.inventory.isAvailable(product)) {
            vm.setSelectedProduct(product);
            vm.setState(vm.getReadyState());
            System.out.println("Selected: " + product.getName() + " | Price: Rs." + product.getPrice());
        } else {
            System.out.println("Out of stock: " + product.getName());
        }
    }

    @Override public void insertCoin(Coin coin)   { System.out.println("Select a product first."); }
    @Override public void insertNote(Note note)   { System.out.println("Select a product first."); }
    @Override public void dispenseProduct()       { System.out.println("Select a product and pay."); }
    @Override public void returnChange()          { System.out.println("No change to return."); }
}
```

### ReadyState — payment accumulation

```java
public class ReadyState implements VendingMachineState {
    private final VendingMachine vm;

    public ReadyState(VendingMachine vm) { this.vm = vm; }

    @Override public void selectProduct(Product p) { System.out.println("Product already selected."); }

    @Override
    public void insertCoin(Coin coin) {
        vm.addCoin(coin);
        System.out.printf("Inserted: %s | Total: Rs.%.0f / Rs.%.0f%n",
            coin, vm.getTotalPayment(), vm.getSelectedProduct().getPrice());
        checkPaymentStatus();
    }

    @Override
    public void insertNote(Note note) {
        vm.addNote(note);
        System.out.printf("Inserted: %s | Total: Rs.%.0f / Rs.%.0f%n",
            note, vm.getTotalPayment(), vm.getSelectedProduct().getPrice());
        checkPaymentStatus();
    }

    @Override public void dispenseProduct() { System.out.println("Complete payment first."); }
    @Override public void returnChange()    { System.out.println("Complete payment first."); }

    private void checkPaymentStatus() {
        if (vm.getTotalPayment() >= vm.getSelectedProduct().getPrice()) {
            vm.setState(vm.getDispenseState());
            System.out.println("Payment complete. Please collect your item.");
        }
    }
}
```

### DispenseState — inventory decrement

```java
public class DispenseState implements VendingMachineState {
    private final VendingMachine vm;

    public DispenseState(VendingMachine vm) { this.vm = vm; }

    @Override
    public void dispenseProduct() {
        Product p = vm.getSelectedProduct();
        int remaining = vm.inventory.getQuantity(p) - 1;
        vm.inventory.updateQuantity(p, remaining);  // NOTE: see thread-safety discussion
        System.out.println("Dispensed: " + p.getName() + " | Remaining: " + remaining);
        vm.setState(vm.getReturnChangeState());
    }

    @Override public void selectProduct(Product p) { System.out.println("Collect dispensed item first."); }
    @Override public void insertCoin(Coin c)       { System.out.println("Payment done. Collect item."); }
    @Override public void insertNote(Note n)       { System.out.println("Payment done. Collect item."); }
    @Override public void returnChange()           { System.out.println("Collect item before change."); }
}
```

### ReturnChangeState — change calculation

```java
public class ReturnChangeState implements VendingMachineState {
    private final VendingMachine vm;

    public ReturnChangeState(VendingMachine vm) { this.vm = vm; }

    @Override
    public void returnChange() {
        double change = vm.getTotalPayment() - vm.getSelectedProduct().getPrice();
        if (change > 0) {
            System.out.printf("Change returned: Rs.%.0f%n", change);
        }
        // reset machine
        vm.resetPayment();
        vm.resetSelectedProduct();
        vm.setState(vm.getIdleState());
    }

    @Override public void selectProduct(Product p) { System.out.println("Collect change first."); }
    @Override public void insertCoin(Coin c)       { System.out.println("Collect change first."); }
    @Override public void insertNote(Note n)       { System.out.println("Collect change first."); }
    @Override public void dispenseProduct()        { System.out.println("Product already dispensed."); }
}
```

### Thread-safe inventory decrement (the real fix)

```java
// UNSAFE — existing DispenseState code:
int remaining = vm.inventory.getQuantity(p) - 1;   // READ
vm.inventory.updateQuantity(p, remaining);           // WRITE — not atomic with READ

// SAFE — use ConcurrentHashMap.compute for an atomic read-modify-write:
public class Inventory {
    private final ConcurrentHashMap<Product, Integer> products = new ConcurrentHashMap<>();

    public boolean tryDispense(Product product) {
        // compute() is atomic on ConcurrentHashMap:
        int[] result = {0};
        products.compute(product, (k, qty) -> {
            if (qty == null || qty <= 0) {
                result[0] = -1;   // signal failure
                return 0;
            }
            result[0] = qty - 1;
            return qty - 1;
        });
        return result[0] >= 0;
    }
}

// DispenseState calls:
if (!vm.inventory.tryDispense(product)) {
    System.out.println("Out of stock — returning payment.");
    vm.setState(vm.getReturnChangeState());
    vm.returnChange();
}
```

---

## 8. Edge Cases & Tricky Parts

### Partial payment then cancellation

The existing code has no `cancel()` operation. If a user inserts money but decides not to buy, they cannot get it back. The fix requires:

1. Add `cancel()` to the `VendingMachineState` interface.
2. In `ReadyState.cancel()`: return `totalPayment` to the user, reset payment, reset product, transition to `IdleState`.
3. In other states: `IdleState` says "nothing to cancel"; `DispenseState` says "product already dispensing".

```java
// Extension to VendingMachineState interface:
default void cancel() {
    System.out.println("Cannot cancel in current state.");
}

// ReadyState override:
@Override
public void cancel() {
    System.out.printf("Cancelled. Returning Rs.%.0f%n", vm.getTotalPayment());
    vm.resetPayment();
    vm.resetSelectedProduct();
    vm.setState(vm.getIdleState());
}
```

### Exact change problem

The current implementation returns a `double` representing the change amount but does not specify *which coins/notes* to return — it assumes the machine always has the right denominations. Real machines maintain a **coin float** (count per denomination) and run a greedy or DP algorithm:

```java
public List<Coin> makeChange(double amount, Map<Coin, Integer> coinFloat) {
    List<Coin> result = new ArrayList<>();
    // Sort coins descending by value: TEN, FIVE, TWO, ONE
    for (Coin coin : Coin.values()) {  // assumes enum declared high-to-low
        while (amount >= coin.getValue() && coinFloat.getOrDefault(coin, 0) > 0) {
            result.add(coin);
            amount -= coin.getValue();
            coinFloat.merge(coin, -1, Integer::sum);
        }
    }
    if (amount > 0.001) throw new InsufficientChangeException("Cannot make exact change");
    return result;
}
```

### Concurrent access — two users, same product, qty=1

```
Thread A: inventory.getQuantity(Pepsi) = 1  ← READ
Thread B: inventory.getQuantity(Pepsi) = 1  ← READ (before A's write)
Thread A: updateQuantity(Pepsi, 0)           ← WRITE
Thread B: updateQuantity(Pepsi, 0)           ← WRITE — both dispensed, qty should be -1!
```

Fix: use `tryDispense()` with `ConcurrentHashMap.compute()` as shown above. The `compute` lambda runs under a per-bucket lock inside `ConcurrentHashMap`, making the read-modify-write atomic.

| Bug | Symptom | Root Cause | Fix |
|---|---|---|---|
| Double dispense | Two users get the last item | Non-atomic read-decrement | `ConcurrentHashMap.compute` |
| Payment lost on crash | User pays, power fails, no product | No durable payment record | Write payment to DB before dispensing |
| Floating point change | Change = 7.000000001 due to double arithmetic | IEEE 754 rounding | Use `long` (paise/cents) everywhere |
| State machine stuck | No transition back to Idle if `dispenseProduct()` is never called | Missing timeout | Add a session timeout that calls `cancel()` after N seconds |

---

## 9. Extension Points

### Add Card Payment

```java
// 1. Add a new state: CardPaymentState
// It handles: insertCard(), enterPIN(), confirmPayment() 
// Transitions to DispenseState on success; back to IdleState on failure.

// 2. Add CardPaymentState to VendingMachine context
// 3. ReadyState gets a new method: initiateCardPayment()
//    -> transitions to CardPaymentState

// Zero changes to Inventory, Product, DispenseState, ReturnChangeState.
```

### Add Remote Monitoring / Restocking

```java
// Observer Pattern — Inventory publishes events
public interface InventoryObserver {
    void onLowStock(Product product, int remaining);
    void onOutOfStock(Product product);
}

public class Inventory {
    private final List<InventoryObserver> observers = new ArrayList<>();

    public void addObserver(InventoryObserver o) { observers.add(o); }

    public boolean tryDispense(Product product) {
        products.compute(product, (k, qty) -> {
            // ...
            if (newQty == 0) notifyOutOfStock(k);
            else if (newQty <= LOW_STOCK_THRESHOLD) notifyLowStock(k, newQty);
            return newQty;
        });
    }
}

// RemoteMonitoringService implements InventoryObserver — sends alerts, no other change needed
```

### Add Product Categories / Different Machine Types

```java
// Make VendingMachine generic — HotDrinkMachine, SnackMachine differ only in valid products
// Use Template Method: base class defines the state flow, subclasses restrict valid products
```

---

## 10. Interview Follow-Up Questions

1. **"What happens if `dispenseProduct()` is called but the physical actuator fails — the product doesn't actually come out?"**
   Answer: You've debited inventory and the user gets nothing. Fix: debit inventory only *after* the physical dispense succeeds. If the actuator fails, stay in DispenseState, log the error, alert maintenance. The inventory count stays correct.

2. **"Why does VendingMachine hold references to all four state objects instead of creating them on transition?"**
   Answer: Creating states lazily would work too, but pre-constructing avoids allocation on every transition and allows states to be stateless singletons. More importantly, each state needs a reference to the `VendingMachine` context — passing it at construction once is cleaner than passing it on every `setState` call.

3. **"The `totalPayment` field is a `double`. What's the problem with that?"**
   Answer: Floating-point arithmetic: `0.1 + 0.2 != 0.3` in IEEE 754. A machine could calculate change of `7.000000001` and fail an equality check. Fix: store all monetary values in the smallest unit (paise/cents) as `long`, convert to display units only for printing.

4. **"How would you add a `cancel` operation without touching the four existing state classes?"**
   Answer: Add a `default void cancel()` method to the `VendingMachineState` interface (Java 8+) with a default body that says "cannot cancel here." Only `ReadyState` overrides it. Existing state classes don't change at all.

5. **"The Singleton is created with `synchronized getInstance()`. What's the performance implication and how do you fix it?"**
   Answer: Every call to `getInstance()` acquires a lock, even after the instance is created — unnecessary contention. Fix with double-checked locking (`volatile instance` + two null checks) or the initialization-on-demand holder idiom (a private static inner class whose field is initialized by the JVM class loader, which is thread-safe by spec with zero locking overhead after first load).
