# BookMyShow — LLD Case Study

---

## 1. Problem Statement

> "Design an online movie ticket booking system where users can search for shows, select seats, and book tickets."

The interviewer is testing concurrent seat reservation (the hardest part), pricing strategies across seat categories, and how you structure a multi-step booking flow end-to-end.

---

## 2. Clarifying Questions

| # | Question | Expected Answer |
|---|----------|-----------------|
| 1 | How many seat categories should we support? | Three: Gold, Silver, General — each with different pricing. |
| 2 | What is the maximum time a user can hold seats before payment? | 10-minute reservation timeout (seats released back if unpaid). |
| 3 | Can the same user book multiple seats in one transaction? | Yes — group booking up to a configurable max (e.g., 10). |
| 4 | Do we need to handle partial booking (some seats taken mid-checkout)? | Yes — atomically confirm all-or-nothing for a booking request. |
| 5 | Should cancellation be supported? | Yes — with refund rules (e.g., full refund > 24h before show). |
| 6 | Do we need real payment processing or mock it? | Mock for LLD; abstract behind a `PaymentService`. |
| 7 | Is this a single-city or multi-city system? | Multi-city; Theatre belongs to a City. |
| 8 | Do we need seat recommendation (e.g., best available)? | Out of scope for core design; mention as extension. |

---

## 3. Requirements

### Functional
- Search movies by city, date, language.
- View all shows for a selected movie + city + date.
- View seat layout for a show with availability.
- Select seats and create a booking (with 10-minute hold).
- Apply coupon codes for discount.
- Complete payment to confirm booking.
- Cancel booking and receive refund based on cancellation policy.
- Receive booking confirmation notification (email/SMS).

### Non-Functional
- Concurrent seat selection: two users trying to book the same seat simultaneously must not both succeed.
- Idempotent booking: duplicate payment confirmation must not double-book.
- Seat availability is read-heavy; booking writes are infrequent by comparison — design accordingly.
- Booking confirmation is eventual (notification can be async).

---

## 4. Entities & Responsibilities

| Class / Interface | Single Responsibility |
|---|---|
| `City` | Groups theatres by geographic location. |
| `Movie` | Metadata only: title, language, duration, genre. No booking logic. |
| `Theatre` | Physical venue — contains multiple screens. |
| `Screen` | One auditorium within a theatre — has a fixed seat layout. |
| `Seat` | One physical seat: row, column, category. Immutable once configured. |
| `Show` | A specific screening: Movie + Screen + start time. Owns the per-show seat inventory. |
| `ShowSeat` | Mutable join between Show and Seat: tracks reservation status, reserved-by, and expiry. |
| `SeatCategory` (enum) | GOLD, SILVER, GENERAL — used by pricing strategy. |
| `SeatStatus` (enum) | AVAILABLE, RESERVED (held, not paid), BOOKED (paid), BLOCKED (maintenance). |
| `PricingStrategy` (interface) | Computes price for one seat in a show. Implementations: `BasePricing`, `PremiumPricing`, `WeekendSurcharge`. |
| `User` | Account holder. Has name, email, phone. |
| `Coupon` | Discount code with percentage/flat reduction and validity period. |
| `Booking` | Aggregate root for one transaction: list of ShowSeats, total amount, payment status. Implements `Command` for undo (cancel). |
| `Payment` | Record of a payment attempt: amount, gateway reference, status. |
| `BookingService` | Facade: orchestrates seat reservation, payment, and notification — the single entry point for the booking flow. |
| `NotificationService` | Observer: sends email/SMS when booking is confirmed or cancelled. |
| `BookingCommand` (interface) | Command pattern: `execute()` and `undo()` on a Booking. |

---

## 5. Class Diagram

```
+----------+        +-------------+      +----------+
|  City    |1------*|   Theatre   |1----*|  Screen  |
|----------|        |-------------|      |----------|
| - name   |        | - name      |      | - name   |
+----------+        | - address   |      | - seats  |
                    +-------------+      | - totalCapacity |
                                         +----------+
                                              |1
                                              | has (1..*)
                                              |*
                                         +----------+
                                         |   Seat   |
                                         |----------|
                                         | - row: int      |
                                         | - col: int      |
                                         | - category:     |
                                         |   SeatCategory  |
                                         +----------+

+----------+        +-------------+      +----------+
|  Movie   |        |    Show     |      | ShowSeat |
|----------|        |-------------|      |----------|
| - title  |1------*| - movie     |1----*| - seat   |
| - lang   |        | - screen    |      | - status |
| - dur    |        | - startTime |      | - reservedBy |
+----------+        | - basePrice |      | - expiresAt  |
                    +-------------+      +----------+

+----------+        +-------------------+
|   User   |1------*|     Booking       |
|----------|        |-------------------|
| - name   |        | - id: UUID        |
| - email  |        | - user            |
| - phone  |        | - show            |
+----------+        | - showSeats: List |
                    | - coupon          |
                    | - totalAmount     |
                    | - status          |
                    | - payment         |
                    +-------------------+
                    | + execute()       |   <<Command>>
                    | + undo()          |
                    +-------------------+

+----------------------+
|   <<interface>>      |
|   PricingStrategy    |
|----------------------|
| + calculate(ShowSeat,|
|   Show): double      |
+----------------------+
     ^         ^
     |         |
BasePricing  WeekendSurcharge

+----------------------+       +-----------------------+
|   BookingService     |       | NotificationService   |
|  (Facade)            |       |  (Observer)           |
|----------------------|       |-----------------------|
| + searchShows(...)   |       | + onBookingConfirmed()|
| + getShowSeats(show) |       | + onBookingCancelled()|
| + reserveSeats(...)  |       +-----------------------+
| + confirmBooking(...)| -------> PaymentService
| + cancelBooking(...) |          (abstracted)
+----------------------+
```

---

## 6. Design Patterns Used

### Strategy Pattern — Seat Pricing
**Trigger:** Gold seats cost more than Silver, which cost more than General. Weekend shows have a surcharge. If you hard-code `if (category == GOLD) price = base * 1.5` inside `Show` or `Seat`, every new pricing rule (dynamic pricing, student discount, matinee discount) forces you to modify the core booking class.

```java
public interface PricingStrategy {
    double calculate(Seat seat, Show show);
}

public class CategoryBasedPricing implements PricingStrategy {
    private static final Map<SeatCategory, Double> MULTIPLIER = Map.of(
        SeatCategory.GOLD,    1.5,
        SeatCategory.SILVER,  1.2,
        SeatCategory.GENERAL, 1.0
    );

    @Override
    public double calculate(Seat seat, Show show) {
        return show.getBasePrice() * MULTIPLIER.get(seat.getCategory());
    }
}

public class WeekendSurchargePricing implements PricingStrategy {
    private final PricingStrategy base;
    private static final double SURCHARGE = 1.15;

    public WeekendSurchargePricing(PricingStrategy base) { this.base = base; }

    @Override
    public double calculate(Seat seat, Show show) {
        double price = base.calculate(seat, show);
        DayOfWeek day = show.getStartTime().getDayOfWeek();
        return (day == SATURDAY || day == SUNDAY) ? price * SURCHARGE : price;
    }
}
```

`BookingService` injects the right strategy at runtime — switching from weekday to weekend pricing requires zero changes to Show, Seat, or Booking.

### Observer Pattern — Booking Notifications
**Trigger:** When a booking is confirmed, multiple downstream actions need to happen: send email, send SMS, update loyalty points. If `BookingService.confirmBooking()` directly calls `EmailService.send()` and `SMSService.send()`, it becomes tightly coupled and adding a new notification channel (push notification) requires modifying `BookingService`.

```java
public interface BookingObserver {
    void onBookingConfirmed(Booking booking);
    void onBookingCancelled(Booking booking);
}

public class EmailNotificationObserver implements BookingObserver {
    @Override
    public void onBookingConfirmed(Booking booking) {
        // send confirmation email to booking.getUser().getEmail()
    }
    @Override
    public void onBookingCancelled(Booking booking) {
        // send cancellation + refund email
    }
}

// BookingService is the publisher:
public class BookingService {
    private final List<BookingObserver> observers = new ArrayList<>();

    public void addObserver(BookingObserver o) { observers.add(o); }

    private void notifyConfirmed(Booking b) {
        observers.forEach(o -> o.onBookingConfirmed(b));
    }
}
```

### Facade Pattern — BookingService
**Trigger:** The booking flow involves: (1) check seat availability, (2) reserve seats with timeout, (3) calculate price with coupon, (4) charge payment, (5) confirm seats as booked, (6) notify user. That is 6 subsystems. If the controller/UI calls all 6 directly, it is tightly coupled to all of them. A Facade provides a single `confirmBooking(user, show, seats, coupon, paymentMethod)` call.

```java
public class BookingService {
    private final ShowSeatRepository seatRepo;
    private final PaymentService paymentService;
    private final CouponService couponService;
    private final List<BookingObserver> observers;
    private final PricingStrategy pricingStrategy;

    public Booking confirmBooking(User user, Show show,
                                   List<Seat> requestedSeats,
                                   Coupon coupon,
                                   PaymentMethod paymentMethod) {
        // Step 1: reserve all-or-nothing
        List<ShowSeat> reserved = reserveSeats(show, requestedSeats, user);

        // Step 2: calculate price
        double total = calculateTotal(reserved, show, coupon);

        // Step 3: charge
        Payment payment = paymentService.charge(paymentMethod, total);
        if (!payment.isSuccessful()) {
            releaseSeats(reserved);
            throw new PaymentFailedException();
        }

        // Step 4: confirm
        Booking booking = createBooking(user, show, reserved, payment, total);
        markSeatsBooked(reserved);

        // Step 5: notify
        notifyConfirmed(booking);

        return booking;
    }
}
```

### Command Pattern — Booking as Undoable Command
**Trigger:** Cancellation is the "undo" of a booking. Without the Command pattern, cancellation logic is scattered — you need to reverse payment, release seats, and send notifications in different places. Encapsulating a `Booking` as a command gives you a single `undo()` that handles the full reversal.

```java
public interface BookingCommand {
    Booking execute();
    void undo();  // cancellation
}

public class CreateBookingCommand implements BookingCommand {
    private final BookingService service;
    private final User user;
    private final Show show;
    private final List<Seat> seats;
    private Booking createdBooking;

    @Override
    public Booking execute() {
        createdBooking = service.confirmBooking(user, show, seats, null, null);
        return createdBooking;
    }

    @Override
    public void undo() {
        if (createdBooking != null) {
            service.cancelBooking(createdBooking);  // reverses payment, releases seats, notifies
        }
    }
}
```

---

## 7. Core Implementation

### The Race Condition — Concurrent Seat Booking

This is the most important part of the entire LLD.

**The Problem:**

```
User A selects Seat G7 | User B selects Seat G7
Thread A: reads ShowSeat G7 → AVAILABLE         ← CHECK
Thread B: reads ShowSeat G7 → AVAILABLE         ← CHECK
Thread A: sets ShowSeat G7 → RESERVED by A      ← USE
Thread B: sets ShowSeat G7 → RESERVED by B      ← USE  ← DOUBLE BOOKING!
```

**Fix 1 — Optimistic Locking (preferred for web systems)**

Add a `version` field to `ShowSeat`. The database UPDATE fails if the version has changed since you read it:

```java
public class ShowSeat {
    private Long id;
    private SeatStatus status;
    private String reservedByUserId;
    private LocalDateTime expiresAt;
    private int version;  // optimistic lock version
}

// In BookingService — runs inside a transaction:
public ShowSeat reserveSeat(Long showSeatId, String userId) {
    ShowSeat seat = seatRepo.findById(showSeatId);

    if (seat.getStatus() != SeatStatus.AVAILABLE) {
        throw new SeatUnavailableException("Seat already taken");
    }

    // The UPDATE will affect 0 rows if version was incremented by another thread
    int rowsUpdated = seatRepo.reserveWithVersion(
        showSeatId,
        userId,
        LocalDateTime.now().plusMinutes(10),
        seat.getVersion()   // pass the version we read
    );

    if (rowsUpdated == 0) {
        throw new SeatUnavailableException("Seat was just taken — please retry");
    }

    seat.setStatus(SeatStatus.RESERVED);
    return seat;
}

// SQL equivalent:
// UPDATE show_seat
// SET status='RESERVED', reserved_by=?, expires_at=?, version=version+1
// WHERE id=? AND version=? AND status='AVAILABLE'
// → if 0 rows updated, another thread won the race
```

**Fix 2 — Pessimistic Locking (simpler, lower throughput)**

```java
// SELECT * FROM show_seat WHERE id=? FOR UPDATE
// Acquires a row-level lock; other threads block until this transaction commits.
// Safe but serialises all seat selections for the same seat.
ShowSeat seat = seatRepo.findByIdWithLock(showSeatId);  // FOR UPDATE
```

**Fix 3 — Seat Reservation with Timeout (in-memory for LLD)**

```java
public class ShowSeat {
    private final AtomicReference<String> reservedBy = new AtomicReference<>(null);
    private volatile LocalDateTime expiresAt;

    // Returns true if this thread successfully reserved the seat
    public boolean tryReserve(String userId, Duration holdDuration) {
        if (reservedBy.compareAndSet(null, userId)) {   // atomic
            expiresAt = LocalDateTime.now().plus(holdDuration);
            return true;
        }
        // Seat is held — check if it has expired
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            // Expired hold — attempt to CAS it back and retry
            String holder = reservedBy.get();
            if (reservedBy.compareAndSet(holder, userId)) {
                expiresAt = LocalDateTime.now().plus(holdDuration);
                return true;
            }
        }
        return false;
    }

    public void release() {
        reservedBy.set(null);
        expiresAt = null;
    }
}
```

### All-or-Nothing Seat Reservation

When a user books 4 seats, either all 4 are reserved or none are. Partial success leaves orphaned reservations:

```java
public List<ShowSeat> reserveSeats(Show show, List<Seat> requested, User user) {
    List<ShowSeat> reserved = new ArrayList<>();
    try {
        for (Seat seat : requested) {
            ShowSeat ss = seatRepo.findByShowAndSeat(show, seat);
            if (!ss.tryReserve(user.getId(), Duration.ofMinutes(10))) {
                throw new SeatUnavailableException("Seat " + seat + " is unavailable");
            }
            reserved.add(ss);
        }
        return reserved;
    } catch (SeatUnavailableException e) {
        // Roll back any seats we reserved so far
        reserved.forEach(ShowSeat::release);
        throw e;
    }
}
```

### Coupon Application

```java
public class CouponService {
    public double applyDiscount(Coupon coupon, double originalAmount) {
        if (coupon == null) return originalAmount;
        if (coupon.getExpiryDate().isBefore(LocalDate.now())) {
            throw new CouponExpiredException(coupon.getCode());
        }
        return switch (coupon.getDiscountType()) {
            case PERCENTAGE -> originalAmount * (1 - coupon.getDiscountValue() / 100.0);
            case FLAT       -> Math.max(0, originalAmount - coupon.getDiscountValue());
        };
    }
}
```

---

## 8. Edge Cases & Tricky Parts

| Scenario | What goes wrong | Fix |
|---|---|---|
| Two users book seat G7 simultaneously | Double booking without locking | Optimistic lock (version field) or `AtomicReference.compareAndSet` |
| User holds seats but never pays | Seats blocked forever | Expiry timer: background job releases RESERVED seats past `expiresAt` |
| Payment succeeds but DB write fails | User charged, no booking record | Write booking record first (status=PENDING), then charge, then update to CONFIRMED. On crash, reconcile PENDING records. |
| Show is cancelled after booking | Users have active bookings | Trigger cancellation event; `BookingObserver` handles mass refund + notification |
| Coupon used by two users concurrently (one-time coupon) | Both see coupon as valid, both apply discount | Atomic `UPDATE coupon SET used=true WHERE code=? AND used=false` — 0 rows = already used |
| User cancels after show starts | Refund should be 0% | Cancellation policy: check `show.getStartTime()` before computing refund |
| Booking ID collision | Two bookings get the same UUID | Use `UUID.randomUUID()` — 2^122 possibilities; collision probability is astronomically low. For DB: use SERIAL primary key instead. |

---

## 9. Extension Points

### Add Waitlist

```java
// When a show is sold out:
public WaitlistEntry joinWaitlist(User user, Show show, List<SeatCategory> preferred) {
    return waitlistRepo.save(new WaitlistEntry(user, show, preferred, LocalDateTime.now()));
}

// When a booking is cancelled:
// BookingService.cancelBooking() calls WaitlistService.notifyNext(show, releasedSeats)
// WaitlistService finds the earliest entry that wants those categories and creates a booking
```

### Add Group Booking Discount

```java
// Decorator on top of PricingStrategy:
public class GroupDiscountPricing implements PricingStrategy {
    private final PricingStrategy base;
    private static final int GROUP_MIN = 5;
    private static final double DISCOUNT = 0.1; // 10% off

    @Override
    public double calculate(Seat seat, Show show) {
        return base.calculate(seat, show); // discount applied at booking level
    }

    public double applyGroupDiscount(double total, int seatCount) {
        return seatCount >= GROUP_MIN ? total * (1 - DISCOUNT) : total;
    }
}
```

### Add Seat Recommendation

```java
public interface SeatRecommender {
    List<Seat> recommend(Show show, int count, SeatPreference preference);
}

public class CentreRowRecommender implements SeatRecommender {
    // Scores seats by distance from the centre of the screen
    // Returns the N highest-scoring available seats
}
// Inject into BookingService; UI calls getRecommendations() before user manually selects
```

---

## 10. Interview Follow-Up Questions

1. **"You mentioned a 10-minute reservation timeout. How do you implement seat expiry without polling the entire database every second?"**
   Answer: Use a `ScheduledExecutorService` or a database job that runs every 30 seconds: `UPDATE show_seat SET status='AVAILABLE', reserved_by=NULL WHERE status='RESERVED' AND expires_at < NOW()`. In production, a message queue (SQS delayed message or Redis TTL key expiry event) triggers per-seat cleanup without full-table scans.

2. **"How does your design handle the case where payment succeeds but the post-payment DB write fails?"**
   Answer: This is the classic distributed transaction problem. Fix: use the Saga pattern. First write `Booking(status=PAYMENT_INITIATED)` — if payment succeeds, update to `CONFIRMED`. If the update fails, a reconciliation job detects `PAYMENT_INITIATED` bookings older than N minutes with a successful payment gateway reference and re-confirms them. Never charge before writing the intent.

3. **"Your `BookingService` does a lot — seat reservation, pricing, payment, notifications. Isn't that a violation of SRP?"**
   Answer: `BookingService` is a Facade — its single responsibility is *orchestrating* the booking flow. It does not contain reservation logic (that's `ShowSeat.tryReserve`), pricing logic (that's `PricingStrategy`), payment logic (that's `PaymentService`), or notification logic (that's `BookingObserver`). The Facade's job is coordination, not computation.

4. **"How would you handle 10,000 users all trying to book the same show the moment tickets go on sale?"**
   Answer: (1) Cache seat availability in Redis — reads hit cache, not DB. (2) Rate-limit the booking endpoint per user. (3) Use a queue: instead of hitting the DB directly, enqueue a `BookSeatRequest`; a pool of workers dequeues and processes serially per seat (or per row). (4) Use database row-level locking — each row only blocks threads competing for *that specific seat*, not the whole table.

5. **"The Command pattern's `undo()` is your cancellation. What if cancellation itself fails halfway (refund API is down)?"**
   Answer: Model cancellation as a multi-step Saga with compensation steps. Write `Booking(status=CANCELLATION_IN_PROGRESS)` first. Then call refund API. If it fails, retry with exponential backoff. Until refund succeeds, the booking stays `CANCELLATION_IN_PROGRESS` — a background job retries it. Only after confirmed refund do you mark seats `AVAILABLE` and booking `CANCELLED`.
