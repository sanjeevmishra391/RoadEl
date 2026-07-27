# ATM — LLD Case Study

---

## 1. Problem Statement

> "Design an ATM machine that allows users to insert a card, authenticate with a PIN, and perform transactions like cash withdrawal, balance inquiry, and transfer."

The interviewer is testing your ability to model a clean state machine, handle the atomicity problem (cash dispensed but debit failed), and use Chain of Responsibility for layered transaction validation.

---

## 2. Clarifying Questions

| # | Question | Expected Answer |
|---|----------|-----------------|
| 1 | What transactions should be supported? | Withdrawal, Balance Inquiry, Mini Statement, Transfer (MVP: withdrawal + balance). |
| 2 | How many PIN attempts before card lockout? | 3 failed attempts lock the card. |
| 3 | Is there a daily withdrawal limit? | Yes — configurable per account/card type (e.g., Rs. 20,000/day). |
| 4 | What happens if cash is dispensed but the debit fails? | This is the critical atomicity question — must be handled explicitly. |
| 5 | Does the ATM need to support multiple currency denominations? | Yes — 100, 200, 500, 2000 notes; dispense using minimum notes. |
| 6 | Should we support NFC / cardless ATM (OTP-based)? | Not in scope for core design; mention as extension. |
| 7 | Can multiple transactions happen simultaneously on the same account? | No — one ATM session is serial. But two ATMs on the same account is a real concurrency scenario. |
| 8 | Do we need to print receipts? | Optional; abstract behind a `ReceiptPrinter` interface. |

---

## 3. Requirements

### Functional
- User inserts card → ATM reads card data.
- User enters PIN → ATM authenticates against the bank.
- On authentication, user selects transaction type.
- For withdrawal: validate limits → check balance → dispense cash → debit account.
- Balance inquiry: display account balance.
- After transaction: ask for another transaction or eject card.
- After 3 wrong PINs: lock card and eject.

### Non-Functional
- Atomicity: cash dispense and account debit must be treated as a single unit — no partial failure left uncorrected.
- PIN never stored in ATM — validated via bank over encrypted channel.
- Card is ejected and session cleared on: successful logout, timeout (90s idle), 3 wrong PINs, or power cycle.
- Thread-safe account debit: two concurrent ATMs on same account must not over-draw.

---

## 4. Entities & Responsibilities

| Class / Interface | Single Responsibility |
|---|---|
| `ATMState` (interface) | Declares all ATM operations. Each state handles the operations it supports and rejects the rest. |
| `IdleState` | Waiting for a card. Only `insertCard()` does real work. |
| `HasCardState` | Card inserted; waiting for PIN. Accepts PIN input; handles lockout after 3 failures. |
| `AuthenticatedState` | PIN verified; user can select a transaction or eject. |
| `TransactionState` | A transaction is in progress. Prevents overlapping operations. |
| `ATM` | Context: owns current state, cash dispenser, card reader, and current session data. |
| `Card` | Value object: card number, expiry, network (Visa/Mastercard). No business logic. |
| `Account` | Balance + daily-limit tracking. Lives in the Bank, not the ATM. |
| `Bank` | Authenticates PIN, authorises debit/credit, returns account info. The ATM's only external dependency. |
| `Transaction` | Command: encapsulates one operation (withdraw/balance/transfer), its amount, and a `rollback()`. |
| `CashDispenser` | Manages the physical note cassettes. Implements greedy denomination algorithm. |
| `TransactionValidator` (interface) | Chain of Responsibility handler: `validate(Transaction)` → pass to next handler or reject. |
| `PINValidator` | Checks PIN was correctly verified for this session. |
| `BalanceValidator` | Checks account balance >= requested amount. |
| `DailyLimitValidator` | Checks today's withdrawals + requested amount <= daily limit. |
| `FraudValidator` | Checks for suspicious patterns (velocity, geo-anomaly). |

---

## 5. Class Diagram

```
+---------------------------+
|  <<interface>>            |
|      ATMState             |
|---------------------------|
| + insertCard(Card)        |
| + enterPIN(String)        |
| + selectTransaction(type) |
| + executeTransaction(Txn) |
| + ejectCard()             |
+---------------------------+
       ^   ^   ^   ^
       |   |   |   |
  IdleState  HasCardState
       |           |
  AuthenticatedState  TransactionState


+-------------------------------+
|            ATM                |
|-------------------------------|
| - currentState: ATMState      |
| - currentCard: Card           |
| - currentAccount: Account     |
| - pinAttempts: int            |
| - cashDispenser: CashDispenser|
| - bank: Bank                  |
|-------------------------------|
| + insertCard(Card)            |  delegates to currentState
| + enterPIN(String)            |  delegates to currentState
| + selectTransaction(TxnType)  |  delegates to currentState
| + ejectCard()                 |  delegates to currentState
| + setState(ATMState)          |  called by states
+-------------------------------+

+------------+       +-------------+       +-----------+
|    Card    |       |   Account   |       |   Bank    |
|------------|       |-------------|       |-----------|
| - number   |       | - id        |       | + validatePIN(Card, PIN)|
| - expiry   |       | - balance   |       | + debit(Account, amt)   |
| - network  |       | - dailyLimit|       | + credit(Account, amt)  |
| - pin(enc) |       | - todayDebit|       | + getAccount(Card)      |
+------------+       +-------------+       +-----------+

+---------------------+
|  <<interface>>      |
|  TransactionValidator|
|---------------------|
| - next: TxnValidator|
| + validate(Txn)     |  Chain of Responsibility
| + setNext(TxnValidator)|
+---------------------+
     ^    ^    ^    ^
     |    |    |    |
  PINValidator  BalanceValidator
     |              |
DailyLimitValidator  FraudValidator

+---------------------+       +-------------------+
|    Transaction      |       |   CashDispenser   |
|  (<<Command>>)      |       |-------------------|
|---------------------|       | - cassettes:      |
| - type: TxnType     |       |   Map<Integer,Int>|
| - amount: double    |       |-------------------|
| - account: Account  |       | + canDispense(amt)|
| - status: TxnStatus |       | + dispense(amt)   |
|---------------------|       | + refill(cassette)|
| + execute()         |       +-------------------+
| + rollback()        |
+---------------------+
```

**State Transition Diagram:**

```
[Idle] ──insertCard()──────────────────> [HasCard]
[HasCard] ──enterPIN(correct)──────────> [Authenticated]
[HasCard] ──enterPIN(wrong x3)─────────> [Idle]  (card retained/ejected)
[HasCard] ──ejectCard()────────────────> [Idle]
[Authenticated] ──selectTransaction()──> [Transaction]
[Authenticated] ──ejectCard()──────────> [Idle]
[Transaction] ──executeTransaction()───> [Authenticated]  (ready for next txn)
[Transaction] ──ejectCard()────────────> [Idle]
```

---

## 6. Design Patterns Used

### State Pattern — ATM state machine
**Trigger:** An ATM's valid actions change completely at each phase. Without State, you'd have `enterPIN()` that does nothing unless `currentState == HAS_CARD`, written as a giant `switch`. Adding a new state (e.g., `MaintenanceState`) would require touching every method.

```java
public interface ATMState {
    void insertCard(Card card);
    void enterPIN(String pin);
    void selectTransaction(TransactionType type);
    void executeTransaction(Transaction transaction);
    void ejectCard();
}

public class IdleState implements ATMState {
    private final ATM atm;
    public IdleState(ATM atm) { this.atm = atm; }

    @Override
    public void insertCard(Card card) {
        if (card.isExpired()) {
            System.out.println("Card expired. Please use a valid card.");
            return;
        }
        atm.setCurrentCard(card);
        atm.resetPINAttempts();
        atm.setState(atm.getHasCardState());
        System.out.println("Card accepted. Please enter your PIN.");
    }

    @Override public void enterPIN(String pin)               { System.out.println("Insert card first."); }
    @Override public void selectTransaction(TransactionType t){ System.out.println("Insert card first."); }
    @Override public void executeTransaction(Transaction t)   { System.out.println("Insert card first."); }
    @Override public void ejectCard()                         { System.out.println("No card inserted."); }
}

public class HasCardState implements ATMState {
    private final ATM atm;
    private static final int MAX_ATTEMPTS = 3;

    @Override
    public void enterPIN(String pin) {
        boolean valid = atm.getBank().validatePIN(atm.getCurrentCard(), pin);
        if (valid) {
            atm.setCurrentAccount(atm.getBank().getAccount(atm.getCurrentCard()));
            atm.setState(atm.getAuthenticatedState());
            System.out.println("PIN verified. Select a transaction.");
        } else {
            atm.incrementPINAttempts();
            int remaining = MAX_ATTEMPTS - atm.getPINAttempts();
            if (remaining <= 0) {
                System.out.println("Card locked after 3 failed attempts.");
                atm.getBank().lockCard(atm.getCurrentCard());
                atm.ejectCard();  // → transitions to Idle inside ejectCard()
            } else {
                System.out.println("Wrong PIN. " + remaining + " attempt(s) remaining.");
            }
        }
    }

    @Override public void insertCard(Card c)                  { System.out.println("Card already inserted."); }
    @Override public void selectTransaction(TransactionType t){ System.out.println("Enter PIN first."); }
    @Override public void executeTransaction(Transaction t)   { System.out.println("Enter PIN first."); }
    @Override
    public void ejectCard() {
        atm.clearSession();
        atm.setState(atm.getIdleState());
        System.out.println("Card ejected.");
    }
}
```

**Why State here is better than a big enum switch:**
- 5 states × 5 operations = 25 branches as a switch. With State, each state class has 5 methods, zero conditionals — the "what state am I in?" question is answered entirely by which object `currentState` points to.
- Adding `MaintenanceState` = one new class, zero changes to existing classes.

### Chain of Responsibility — Transaction Validation
**Trigger:** Before dispensing cash, the ATM must check: (1) PIN session is valid, (2) account has sufficient balance, (3) withdrawal is within daily limit, (4) the pattern is not fraudulent. These checks are ordered and independent. Without CoR, they'd all live inside a single `validate()` method. Adding fraud detection (step 4) would require modifying the withdrawal method.

```java
public abstract class TransactionValidator {
    protected TransactionValidator next;

    public TransactionValidator setNext(TransactionValidator next) {
        this.next = next;
        return next;  // fluent chaining
    }

    public abstract void validate(Transaction txn) throws TransactionException;

    protected void passToNext(Transaction txn) throws TransactionException {
        if (next != null) next.validate(txn);
    }
}

public class BalanceValidator extends TransactionValidator {
    @Override
    public void validate(Transaction txn) throws TransactionException {
        Account account = txn.getAccount();
        if (account.getBalance() < txn.getAmount()) {
            throw new InsufficientFundsException(
                "Balance: " + account.getBalance() + ", Requested: " + txn.getAmount()
            );
        }
        passToNext(txn);
    }
}

public class DailyLimitValidator extends TransactionValidator {
    @Override
    public void validate(Transaction txn) throws TransactionException {
        Account account = txn.getAccount();
        double projected = account.getTodayDebitTotal() + txn.getAmount();
        if (projected > account.getDailyLimit()) {
            throw new DailyLimitExceededException(
                "Daily limit: " + account.getDailyLimit() +
                ", Used: " + account.getTodayDebitTotal() +
                ", Requested: " + txn.getAmount()
            );
        }
        passToNext(txn);
    }
}

public class FraudValidator extends TransactionValidator {
    @Override
    public void validate(Transaction txn) throws TransactionException {
        // Check velocity: more than 3 withdrawals in 1 hour
        if (txn.getAccount().getRecentWithdrawalCount(Duration.ofHours(1)) >= 3) {
            throw new FraudSuspectedException("Unusual withdrawal velocity detected.");
        }
        passToNext(txn);
    }
}

// Build the chain in ATM setup:
TransactionValidator chain = new BalanceValidator();
chain.setNext(new DailyLimitValidator())
     .setNext(new FraudValidator());

// Use:
chain.validate(transaction);  // throws on first failure, no if/else chain in caller
```

Adding a new check (e.g., `GeofenceValidator`) = add one class, plug it into the chain. Zero existing classes change.

### Command Pattern — Transaction with Rollback
**Trigger:** The most dangerous scenario in ATM design is: cash physically dispenses (mechanical actuator fires) but the bank debit fails (network timeout). Without rollback, the customer gets cash and the account is not debited — the bank loses money. A `Transaction` as a Command gives you a clean `rollback()` that credits the account back.

```java
public interface TransactionCommand {
    void execute() throws TransactionException;
    void rollback();
}

public class WithdrawalTransaction implements TransactionCommand {
    private final Account account;
    private final CashDispenser dispenser;
    private final Bank bank;
    private final double amount;
    private boolean dispensed = false;
    private boolean debited = false;

    @Override
    public void execute() throws TransactionException {
        // Step 1: Debit first, then dispense
        // This is the CORRECT order (see Section 8)
        bank.debit(account, amount);
        debited = true;

        try {
            dispenser.dispense(amount);
            dispensed = true;
        } catch (DispenserException e) {
            // Dispenser failed after debit — must rollback
            rollback();
            throw new TransactionException("Dispenser failure. Amount refunded.", e);
        }
    }

    @Override
    public void rollback() {
        if (debited && !dispensed) {
            // Reverse the debit
            bank.credit(account, amount);
            System.out.println("Transaction rolled back. Rs." + amount + " credited back.");
        }
        if (dispensed && !debited) {
            // Cash is out but debit never happened — log for manual reconciliation
            // Cannot "take back" physical cash
            System.err.println("CRITICAL: Cash dispensed but debit failed. Logging for reconciliation.");
            bank.logReconciliationNeeded(account, amount);
        }
    }
}
```

---

## 7. Core Implementation

### ATM Context Class

```java
public class ATM {
    private ATMState currentState;
    private final ATMState idleState;
    private final ATMState hasCardState;
    private final ATMState authenticatedState;
    private final ATMState transactionState;

    private Card currentCard;
    private Account currentAccount;
    private int pinAttempts;

    private final CashDispenser cashDispenser;
    private final Bank bank;

    public ATM(Bank bank, Map<Integer, Integer> initialCassettes) {
        this.bank = bank;
        this.cashDispenser = new CashDispenser(initialCassettes);

        // Create all states upfront — they hold a reference to 'this' (Context)
        idleState         = new IdleState(this);
        hasCardState      = new HasCardState(this);
        authenticatedState= new AuthenticatedState(this);
        transactionState  = new TransactionState(this);

        currentState = idleState;  // start in Idle
    }

    // All public operations delegate to the current state
    public void insertCard(Card card)                  { currentState.insertCard(card); }
    public void enterPIN(String pin)                   { currentState.enterPIN(pin); }
    public void selectTransaction(TransactionType type){ currentState.selectTransaction(type); }
    public void executeTransaction(Transaction txn)    { currentState.executeTransaction(txn); }
    public void ejectCard()                            { currentState.ejectCard(); }

    // Session management — called by states
    public void clearSession() {
        currentCard = null;
        currentAccount = null;
        pinAttempts = 0;
    }

    public void incrementPINAttempts() { pinAttempts++; }
    public void resetPINAttempts()     { pinAttempts = 0; }
    public int  getPINAttempts()       { return pinAttempts; }

    // Getters / setters used by states
    public void setState(ATMState state)           { this.currentState = state; }
    public void setCurrentCard(Card card)          { this.currentCard = card; }
    public void setCurrentAccount(Account account) { this.currentAccount = account; }
    public Card getCurrentCard()                   { return currentCard; }
    public Account getCurrentAccount()             { return currentAccount; }
    public Bank getBank()                          { return bank; }
    public CashDispenser getCashDispenser()        { return cashDispenser; }

    // State accessors (states call these to get sibling states)
    public ATMState getIdleState()          { return idleState; }
    public ATMState getHasCardState()       { return hasCardState; }
    public ATMState getAuthenticatedState() { return authenticatedState; }
    public ATMState getTransactionState()   { return transactionState; }
}
```

### CashDispenser — Greedy Denomination Algorithm

```java
public class CashDispenser {
    // Key = denomination, Value = count available
    private final TreeMap<Integer, Integer> cassettes;

    public CashDispenser(Map<Integer, Integer> initial) {
        // TreeMap with reverse order so we process 2000 before 500 before 200 before 100
        this.cassettes = new TreeMap<>(Comparator.reverseOrder());
        this.cassettes.putAll(initial);
    }

    public boolean canDispense(double amount) {
        return tryDispense(amount, false).isPresent();
    }

    public Map<Integer, Integer> dispense(double amount) throws DispenserException {
        Optional<Map<Integer, Integer>> result = tryDispense(amount, true);
        if (result.isEmpty()) {
            throw new DispenserException("Cannot dispense Rs." + amount +
                " with available denominations.");
        }
        return result.get();
    }

    // Core greedy algorithm — if commit=true, actually deduct from cassettes
    private Optional<Map<Integer, Integer>> tryDispense(double requestedAmount, boolean commit) {
        int remaining = (int) requestedAmount;
        Map<Integer, Integer> toDispense = new LinkedHashMap<>();
        Map<Integer, Integer> tempCassettes = new HashMap<>(cassettes);

        for (int denomination : cassettes.keySet()) {
            int available = tempCassettes.get(denomination);
            int needed = remaining / denomination;
            int used = Math.min(needed, available);
            if (used > 0) {
                toDispense.put(denomination, used);
                remaining -= used * denomination;
                tempCassettes.put(denomination, available - used);
            }
        }

        if (remaining != 0) return Optional.empty();  // cannot make exact amount

        if (commit) {
            tempCassettes.forEach(cassettes::put);  // apply deductions
        }

        return Optional.of(toDispense);
    }
}
```

### Concurrent Account Debit (Two ATMs, Same Account)

```java
public class Account {
    private final String id;
    private final AtomicLong balancePaise;  // store in paise to avoid floating point
    private final long dailyLimitPaise;
    private final AtomicLong todayDebitPaise;

    // Returns true if debit succeeded, false if insufficient funds
    public boolean tryDebit(long amountPaise) {
        return balancePaise.updateAndGet(current -> {
            if (current >= amountPaise) return current - amountPaise;
            return current;  // no change — insufficient
        }) == balancePaise.get() - amountPaise
            || /* check if actually decreased: */
            debitWithCAS(amountPaise);
    }

    // Cleaner CAS loop:
    public boolean debitWithCAS(long amountPaise) {
        while (true) {
            long current = balancePaise.get();
            if (current < amountPaise) return false;  // insufficient funds
            if (balancePaise.compareAndSet(current, current - amountPaise)) {
                todayDebitPaise.addAndGet(amountPaise);
                return true;
            }
            // Another thread changed balance — retry
        }
    }

    public void credit(long amountPaise) {
        balancePaise.addAndGet(amountPaise);
    }

    public long getBalancePaise() { return balancePaise.get(); }
}
```

---

## 8. Edge Cases & Tricky Parts

### The Atomicity Problem — Cash vs. Debit

This is the most critical scenario:

```
Option A (Debit then Dispense) — PREFERRED:
  1. Debit account (-Rs.500)   ← if this fails, nothing happened — safe
  2. Dispense cash (+Rs.500)   ← if this fails, rollback the debit (bank.credit)

Option B (Dispense then Debit) — DANGEROUS:
  1. Dispense cash (+Rs.500)   ← cash is now physically out; cannot be recalled
  2. Debit account (-Rs.500)   ← if this fails, bank loses Rs.500, NO ROLLBACK possible
```

**Always debit first, then dispense.** If dispensing fails, you can programmatically reverse a debit. You cannot programmatically take cash back from a customer.

```java
// CORRECT order in WithdrawalTransaction.execute():
bank.debit(account, amount);   // 1. debit first
dispenser.dispense(amount);    // 2. dispense second

// If step 2 fails:
bank.credit(account, amount);  // rollback step 1 — safe and complete
```

In production, the debit is a **two-phase commit**: the bank places a `HOLD` on funds (debit from available but not cleared), then clears it when dispense confirms. If dispense fails, the hold is released. This prevents the edge case where a rollback `credit` itself fails.

### Other Tricky Scenarios

| Scenario | What goes wrong | Fix |
|---|---|---|
| Three ATMs, same account, concurrent withdrawal | All three see sufficient balance; all three debit; account goes negative | CAS loop on `AtomicLong` balance; DB: `UPDATE account SET balance=balance-? WHERE balance>=?` |
| Network timeout between debit and dispense response | Debit happened, ATM doesn't know if dispense happened | ATM retries with idempotency key; bank returns "already debited" — ATM proceeds to dispense; reconciliation job handles inconsistencies |
| Card left in ATM (user walks away) | Session stays open; next user can access account | Idle timeout (90 seconds) triggers `ejectCard()` + `clearSession()` automatically |
| PIN brute force | Attacker tries all 10,000 four-digit PINs | 3-attempt lockout after which `bank.lockCard()` is called; card is physically retained (not ejected) on lockout |
| Double withdrawal via two rapid requests | User submits the same withdrawal twice (retry loop) | Idempotency key per transaction — bank rejects duplicate request with same key |
| Cassette short count | ATM says it can dispense but physical count is off | `canDispense()` runs against software count; periodic hardware reconciliation updates cassette counts |
| Floating point in Rs. 100.50 | `0.1 + 0.2 != 0.3` | Store all amounts in paise (integers). `Rs. 100.50 = 10050 paise`. |

---

## 9. Extension Points

### Add International Cards

```java
// 1. Card gains a currency and network field
public class Card {
    private final Currency currency;  // INR, USD, EUR
    private final CardNetwork network; // VISA, MASTERCARD, AMEX
}

// 2. Bank interface gains a currency conversion method
public interface Bank {
    // existing methods...
    double getExchangeRate(Currency from, Currency to);
    boolean isInternationalAllowed(Card card);
}

// 3. DailyLimitValidator checks both local and international limits
// 4. CashDispenser dispenses in local currency
// Zero changes to ATMState, Transaction, or CashDispenser algorithm
```

### Add Cardless ATM (OTP-Based)

```java
// Replace the card insertion flow with OTP flow:
// 1. Add CardlessState (replaces HasCardState)
// 2. User enters phone number → bank sends OTP
// 3. User enters OTP → transitions to AuthenticatedState
// AuthenticatedState, TransactionState, CashDispenser — UNCHANGED
// Only IdleState gains a new path: enterPhoneNumber() → CardlessState

public class CardlessState implements ATMState {
    @Override
    public void enterOTP(String otp) {
        boolean valid = atm.getBank().validateOTP(atm.getCurrentPhone(), otp);
        if (valid) {
            atm.setCurrentAccount(atm.getBank().getAccountByPhone(atm.getCurrentPhone()));
            atm.setState(atm.getAuthenticatedState());
        }
    }
    // All other methods print "Enter OTP first."
}
```

### Add Cash Deposit

```java
// 1. New TransactionType: DEPOSIT
// 2. DepositTransaction implements TransactionCommand:
//    execute(): verify notes → credit account → update cassette count
//    rollback(): debit account if credit succeeded but note jam occurred
// 3. CashDispenser gains acceptDeposit(Map<Integer, Integer> notes)
//    — validates note authenticity, updates internal count
// 4. New validator in chain: NoteAuthenticityValidator (checks for counterfeits)
// Chain, State, and existing withdrawal flow are unchanged
```

---

## 10. Interview Follow-Up Questions

1. **"What is the exact sequence of operations in a withdrawal, and what happens if each step fails?"**
   Answer: (1) Validate chain passes → (2) `bank.debit()` → (3) `dispenser.dispense()` → (4) print receipt. Step 1 fails: tell user, no state change. Step 2 fails: tell user "bank declined", no state change. Step 3 fails: `bank.credit()` to reverse step 2, print "dispenser error, amount refunded". Step 4 fails: do nothing — transaction is complete, receipt is optional. The critical insight: debit *before* dispense so rollback is always possible.

2. **"You have a `FraudValidator` in the chain. If fraud is detected on step 4, do you throw an exception or return a boolean?"**
   Answer: Throw a checked `FraudSuspectedException` (subclass of `TransactionException`). It is a distinct error type with a specific recovery path: lock the card, alert the bank's fraud team, eject card. A boolean return would require the caller to inspect the return value and know what to do — the exception forces explicit handling and carries context (which rule fired).

3. **"How do you prevent two ATMs from simultaneously withdrawing the last Rs. 1000 from an account with Rs. 1000 balance?"**
   Answer: The balance debit must be a single atomic operation. In Java: `AtomicLong.compareAndSet` in a retry loop (shown above). In a real bank: the core banking system runs the authorisation; the ATM sends an authorisation request and the bank applies a database-level row lock (`SELECT ... FOR UPDATE`) before debiting. The ATM never holds the balance locally — it always asks the bank.

4. **"Your chain validates balance and daily limit separately. Is there a race condition between those two checks and the actual debit?"**
   Answer: Yes — classic TOCTOU. A user could pass both checks (balance is sufficient, limit not exceeded) but another concurrent transaction drains the balance between the check and the debit. Fix: the validation chain should be run inside the same database transaction as the debit, or use a database-level check: `UPDATE account SET balance=balance-? WHERE balance>=? AND today_debit+? <= daily_limit`. If 0 rows updated, the constraint was violated.

5. **"Why do you pre-create all four state objects in the ATM constructor instead of creating them lazily?"**
   Answer: Pre-creation makes the State pattern explicit — the ATM has a known, finite set of states defined at startup, not at runtime. It avoids allocation on every transition and allows all states to be stateless (no fields besides the ATM context reference). It also makes it impossible to end up in an undefined state through a coding error. The cost is four extra object allocations at startup — a perfectly acceptable trade-off for a machine that runs for years.
