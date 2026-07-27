# Parking Lot — LLD Case Study

---

## 1. Problem Statement

> "Design a parking lot system that can park vehicles of different sizes and track which spots are occupied."

The interviewer wants to see how you model real-world hierarchy (lot → level → spot), handle size compatibility, and deal with concurrent access during spot assignment.

---

## 2. Clarifying Questions

| # | Question | Expected Answer |
|---|----------|-----------------|
| 1 | How many vehicle types do we need to support? | At minimum: Motorcycle/Bike, Car, Truck |
| 2 | Can a larger vehicle park in a smaller spot? | No. Each spot has a fixed allowed type. |
| 3 | Do we need a ticketing/payment system? | Start without; design for extension. |
| 4 | How many levels and spots per level? | Variable — configurable at startup. |
| 5 | Do we need "nearest available" spot assignment or first-fit is fine? | First-fit per level is acceptable for MVP. |
| 6 | Should we support reserved/disabled spots? | No for now, but design should allow it. |
| 7 | Is this single-machine or distributed? | Single machine. Thread safety is still required. |
| 8 | Do we need persistence (re-startup recovery)? | No; in-memory is fine. |

---

## 3. Requirements

### Functional
- Park a vehicle: find the first available spot compatible with the vehicle type.
- Release a vehicle: free its spot when it exits.
- Query available spots by vehicle type.
- Look up which spot a given vehicle occupies.
- Multi-level support: levels are scanned top-down (Level 0 first).

### Non-Functional
- Thread-safe spot assignment — two vehicles must not be given the same spot.
- O(1) lookup for "which spot is my vehicle at?" (HashMap).
- Configurable at startup: number of levels, spots per level, type of each spot.
- Clean separation of concerns: UI (ParkingLot), business logic (ParkingLotManager), data (Level, Spot, Vehicle).

---

## 4. Entities & Responsibilities

| Class / Interface | Single Responsibility |
|---|---|
| `VehicleType` (enum) | Enumerates CAR, BIKE, TRUCK — the canonical set of vehicle categories. |
| `Vehicle` | Value object: holds plate number + type. No business logic. |
| `Spot` | Represents one physical parking space: knows its id, allowed vehicle type, and whether it is currently occupied. |
| `Level` | Owns an ordered list of Spots on one floor. Answers "give me empty spots for type X". |
| `ParkingLotManager` | Business logic: finds the first matching spot across levels, marks it occupied, and exposes release. The only class that mutates Spot state. |
| `ParkingLot` | UI/Driver layer: takes scanner input, delegates to Manager, owns the `vehicleNumber → Spot` lookup map. |

---

## 5. Class Diagram

```
+---------------------+
|    <<enum>>         |
|    VehicleType      |
|---------------------|
|  CAR                |
|  BIKE               |
|  TRUCK              |
+---------------------+
          ^
          | uses
          |
+---------------------+        +---------------------+
|      Vehicle        |        |        Spot         |
|---------------------|        |---------------------|
| - number: String    |        | - id: String        |
| - type: VehicleType |        | - filled: boolean   |
+---------------------+        | - allowedType: VT   |
| + getNumber()       |        +---------------------+
| + getType()         |        | + isFilled()        |
+---------------------+        | + setFilled(bool)   |
                               | + getAllowedType()   |
                               +---------------------+
                                         ^
                                         | contains (1..*)
                                         |
                               +---------------------+
                               |       Level         |
                               |---------------------|
                               | - id: String        |
                               | - spots: List<Spot> |
                               +---------------------+
                               | + addSpot()         |
                               | + getEmptySpots()   |
                               | + getEmSpotsFor(VT) |
                               +---------------------+
                                         ^
                                         | manages (1..*)
                                         |
                               +-------------------------+
                               |   ParkingLotManager     |
                               |-------------------------|
                               | - levels: List<Level>   |
                               +-------------------------+
                               | + getEmptySpots(VT)     |
                               | + bookSpot(Vehicle)     |
                               | + releaseSpot(Spot)     |
                               +-------------------------+
                                         ^
                                         | uses
                                         |
+------------------------------------------------+
|                  ParkingLot                    |
|------------------------------------------------|
| - manager: ParkingLotManager                   |
| - allocatedSpaceMap: HashMap<String, Spot>     |
+------------------------------------------------+
| + start()                                      |
| + parkVehicle()                                |
| + releaseVehicle()                             |
| + checkParkingSpot()                           |
+------------------------------------------------+
```

---

## 6. Design Patterns Used

### State Pattern (implicit) — Spot availability
**Trigger:** A `Spot` can be in one of two states: available or occupied. Operations that depend on that state (`bookSpot`) need to react differently.

The `filled` boolean inside `Spot` is the simplest possible state machine (two states). A full State pattern would give `Spot` an `Available` and `Occupied` state object — but the boolean is the correct trade-off here because there are only two states and no per-state behaviour differences other than eligibility. In an interview, name this explicitly: *"I'm using State implicitly through a boolean — if spots gain more states (reserved, maintenance, EV-only) I'd promote this to a full State enum or object."*

### Strategy Pattern — Pricing (extension point)
**Trigger:** Different spot categories (standard, premium, EV) will have different pricing rules. If you hard-code `price = hours * rate` inside `Spot`, every new pricing model requires modifying existing code.

```java
// Strategy interface — not in the existing code, but where it belongs
public interface PricingStrategy {
    double calculate(long entryEpoch, long exitEpoch, Spot spot);
}

public class HourlyPricing implements PricingStrategy {
    private final Map<VehicleType, Double> ratePerHour;

    public HourlyPricing(Map<VehicleType, Double> rates) {
        this.ratePerHour = rates;
    }

    @Override
    public double calculate(long entryEpoch, long exitEpoch, Spot spot) {
        double hours = Math.ceil((exitEpoch - entryEpoch) / 3600.0);
        return hours * ratePerHour.getOrDefault(spot.getAllowedVehicleType(), 10.0);
    }
}

public class FlatRatePricing implements PricingStrategy {
    private final double flatRate;
    public FlatRatePricing(double flatRate) { this.flatRate = flatRate; }
    @Override
    public double calculate(long entryEpoch, long exitEpoch, Spot spot) {
        return flatRate;
    }
}
```

Inject `PricingStrategy` into `Ticket` or `ParkingLotManager` — switching from hourly to flat-rate requires zero changes to existing classes.

### Factory Pattern — Spot creation
**Trigger:** The `initialise()` method creates spots with different `VehicleType` values based on a configuration array. A Factory centralises the "which subclass/configuration do I instantiate?" decision.

```java
// Current code does this inline:
level.addSpot(new Spot(spotId, vehicleTypeAllowedInSpots[i][s], false));

// A SpotFactory makes this explicit and testable:
public class SpotFactory {
    public static Spot createSpot(String id, VehicleType type) {
        return switch (type) {
            case BIKE  -> new SmallSpot(id);
            case CAR   -> new MediumSpot(id);
            case TRUCK -> new LargeSpot(id);
        };
    }
}
```

---

## 7. Core Implementation

### The tricky part: Thread-safe spot assignment

The existing `bookSpot` has a **TOCTOU (Time-Of-Check-Time-Of-Use) race condition**:

```java
// UNSAFE — two threads can both read the same empty spot, both pass the check,
// and both call setFilled(true) on the SAME spot.
public Pair bookSpot(Vehicle vehicle) throws Exception {
    Map<Level, List<Spot>> availableSpotsMap = getEmptySpots(vehicle.getType()); // CHECK
    for (...) {
        Spot allocatedSpot = entry.getValue().get(0);
        allocatedSpot.setFilled(true);  // USE — not atomic with CHECK
        return new Pair(allocatedLevel, allocatedSpot);
    }
}
```

**Fix 1 — synchronized method (simple, correct for single JVM):**

```java
public synchronized Pair bookSpot(Vehicle vehicle) throws Exception {
    // Now only one thread can execute this at a time
    for (Level level : levels) {
        List<Spot> spots = level.getEmSpotsForSpecificVehicleType(vehicle.getType());
        if (!spots.isEmpty()) {
            Spot spot = spots.get(0);
            spot.setFilled(true);  // safe: we hold the monitor
            return new Pair(level, spot);
        }
    }
    throw new Exception("No spots available for " + vehicle.getType());
}
```

**Fix 2 — CAS-style with AtomicBoolean (higher throughput, no central lock):**

```java
public class Spot {
    private final AtomicBoolean filled = new AtomicBoolean(false);

    // Returns true if this call successfully claimed the spot
    public boolean tryOccupy() {
        return filled.compareAndSet(false, true);  // atomic check-and-set
    }

    public void release() {
        filled.set(false);
    }
}

// In ParkingLotManager — no synchronized needed:
public Pair bookSpot(Vehicle vehicle) throws Exception {
    for (Level level : levels) {
        for (Spot spot : level.getSpotsByType(vehicle.getType())) {
            if (spot.tryOccupy()) {          // atomic — only one thread wins
                return new Pair(level, spot);
            }
        }
    }
    throw new Exception("No spots available");
}
```

Fix 2 allows concurrent parking of vehicles on different spots without serialising all threads through one lock — important in a real multi-gate parking lot.

### Vehicle size vs spot size compatibility matrix

The current design uses `allowedVehicleType` as a 1:1 match. A real lot often allows a Motorcycle to park in a Car spot when Bike spots are full. Model this as a compatibility table:

```java
public class SpotCompatibility {
    // Key = spot type, Value = vehicle types that MAY use it (ordered by preference)
    private static final Map<VehicleType, List<VehicleType>> COMPATIBLE = Map.of(
        VehicleType.BIKE,  List.of(VehicleType.BIKE),
        VehicleType.CAR,   List.of(VehicleType.CAR, VehicleType.BIKE),   // bike can park in car spot
        VehicleType.TRUCK, List.of(VehicleType.TRUCK, VehicleType.CAR, VehicleType.BIKE)
    );

    public static boolean canFit(VehicleType spotType, VehicleType vehicleType) {
        return COMPATIBLE.getOrDefault(spotType, List.of()).contains(vehicleType);
    }
}
```

---

## 8. Edge Cases & Tricky Parts

| Scenario | What goes wrong | Fix |
|---|---|---|
| Two threads book last spot simultaneously | Both see `filled=false`, both set `filled=true`, double-booking | Use `AtomicBoolean.compareAndSet` or `synchronized bookSpot` |
| Vehicle exits without a recorded entry | `allocatedSpaceMap.get(number)` returns null, NPE | Null-check before any release; throw `VehicleNotFoundException` |
| Level iteration order is HashMap-based | `getEmptySpots` returns a `HashMap<Level, ...>` — iteration order is undefined, so "nearest level" semantics are broken | Use `LinkedHashMap` or sort levels by floor number |
| Same vehicle number parked twice | `allocatedSpaceMap.put` silently overwrites the old spot, leaking it | Check for duplicate entry before booking |
| Releasing a spot that's already free | `setFilled(false)` on an already-free spot is a silent no-op, but loses a concurrent booking | Assert `filled==true` before releasing; throw `InvalidStateException` |
| All spots full — stress test | `bookSpot` throws `Exception`; caller must handle it | Propagate as a checked `ParkingFullException` for explicit handling |

---

## 9. Extension Points

### Add EV Charging Spots
```java
// 1. Extend SpotType instead of VehicleType
public enum SpotType { REGULAR, EV_CHARGING, DISABLED }

// 2. Add spotType field to Spot — does not break existing code
public class Spot {
    private final SpotType spotType;  // new field, default REGULAR
    // ...
}

// 3. EVPricingStrategy charges extra per kWh — plugged into Strategy
public class EVPricingStrategy implements PricingStrategy {
    @Override
    public double calculate(long entry, long exit, Spot spot) {
        double hours = (exit - entry) / 3600.0;
        return hours * 15.0 + spot.getKwhConsumed() * 8.0; // parking + charging
    }
}
```

### Add Monthly Passes
```java
public interface ParkingPass {
    boolean isValid(LocalDate date);
    boolean covers(VehicleType type);
}

public class MonthlyPass implements ParkingPass {
    private final String vehicleNumber;
    private final YearMonth validMonth;
    private final VehicleType type;
    // isValid checks if date falls in validMonth
}

// In ParkingLotManager.bookSpot: check if vehicle has a valid pass before charging
```

### Add Different Pricing Per Floor
```java
// Inject a Map<String, PricingStrategy> levelId → strategy into ParkingLotManager
// Ground floor (premium) uses PremiumPricing; upper floors use StandardPricing
// Zero changes to Spot, Level, or Vehicle.
```

**Open-Closed Principle in practice:** All three extensions add new classes. No existing class is modified.

---

## 10. Interview Follow-Up Questions

1. **"Your `bookSpot` iterates a HashMap — can you guarantee Level 0 is always checked first?"**
   Answer: No. HashMap has no ordering. Switch to `List<Level>` (already done in the existing code) or `LinkedHashMap` if you need insertion-order. The existing `List<Level> levels` field is correct — the bug is in `getEmptySpots` which puts results into a `HashMap`.

2. **"How would you find the nearest available spot to the entrance, not just any spot?"**
   Answer: Assign each spot a distance score (e.g., `levelIndex * 100 + spotIndexInLevel`). Keep a `PriorityQueue<Spot>` per vehicle type, ordered by distance. `bookSpot` becomes O(log n) poll. On release, O(log n) offer.

3. **"What happens if the machine crashes after `setFilled(true)` but before the ticket is printed?"**
   Answer: The spot is marked occupied but no vehicle is tracked — a ghost occupation. Fix: write the ticket record first (WAL pattern), then mark the spot. On restart, reconcile spot state against ticket records.

4. **"How would you scale this to a distributed parking system with multiple entry gates?"**
   Answer: Replace the in-memory `AtomicBoolean` with a distributed lock (Redis SETNX or a database row lock). Use optimistic locking with a `version` column: `UPDATE spot SET filled=true, version=version+1 WHERE id=? AND filled=false AND version=?`. Retry on version mismatch.

5. **"The current design has `VehicleType` on both `Spot` (allowed type) and `Vehicle` (actual type). What's the risk?"**
   Answer: They will diverge when you add size-compatibility (a Bike can park in a Car spot). The `Spot` field should be renamed `SpotSize` or `SpotCategory`, and a separate compatibility matrix should own the matching logic — not each individual `Spot` or `Vehicle`.
