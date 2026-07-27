# Low-Level Design (LLD) Interview Guide

> For Senior and Staff-level engineering interviews at FAANG and FAANG-adjacent companies.

---

## LLD vs HLD — What Is Actually Being Tested

High-Level Design (HLD) and Low-Level Design (LLD) interviews look similar on the surface — both ask you to design a system — but they test entirely different cognitive skills.

| Dimension | HLD | LLD |
|---|---|---|
| **Scope** | Distributed systems, services, infrastructure | A single service or module's internals |
| **Primary skill** | Capacity estimation, scalability, trade-offs | Object modeling, abstraction, design patterns |
| **Artifacts** | System topology diagrams, data flow, API contracts | Class diagrams, sequence diagrams, interface definitions |
| **Interviewer concern** | "Can this handle 10M requests/day?" | "Is this extensible without breaking existing code?" |
| **Time horizon** | How does this scale over 2 years? | How does the next engineer change this safely? |
| **Right answer feel** | Numbers, trade-offs, alternatives | Clean interfaces, SOLID reasoning, pattern awareness |

In an LLD round the interviewer is asking: **"Do you know how to write code that other engineers can maintain, extend, and trust?"** HLD knowledge does not substitute for this. Engineers who default to talking about databases and load balancers in LLD rounds signal that they haven't done the preparation.

---

## The 4-Step Approach to Any LLD Question

### Step 1 — Clarify Requirements (5 minutes)

Do not start modeling before you understand what the system actually needs to do. Ask:

- **Functional scope**: What are the core use cases? What is explicitly out of scope?
- **Scale hints**: Is this a single-machine simulation, or will it be embedded in a distributed service? (This affects whether you need thread safety.)
- **Assumptions**: Am I modeling the storage layer, or is persistence out of scope for this session?
- **Constraints**: Any language preference? Are we optimizing for readability, extensibility, or performance?

A useful framing: "Before I start modeling, let me confirm I understand what we're building and then I'll call out what I'm treating as in-scope vs out-of-scope." This immediately signals seniority.

### Step 2 — Identify Entities and Classes (8 minutes)

Extract the nouns from your requirements. Every noun that carries state or behavior is a candidate class. Every noun that is just data is a candidate value object or enum.

Mechanics:
1. Write the use cases in plain English: "A user books a seat for a show."
2. Underline the nouns: **User**, **Seat**, **Show**, **Booking**.
3. Mark which nouns have behavior (methods) vs which are data containers.
4. Identify actors (things that initiate actions) vs resources (things acted upon).

Ask yourself: "If I delete this class, what breaks?" If nothing breaks, the class might not belong.

### Step 3 — Define Relationships and Responsibilities (10 minutes)

For each pair of classes, decide:

- **Association**: Does A hold a reference to B? (a `Booking` has a `Seat`)
- **Composition**: Does A own B's lifecycle? (a `Cinema` owns its `Screens`)
- **Aggregation**: Does A reference B without owning it? (a `Show` references `Actors`)
- **Inheritance**: Is A a specialization of B? (use sparingly — prefer composition)
- **Dependency**: Does A use B only in a method parameter? (a `NotificationService` uses an `EmailClient`)

For each class, write one sentence: "This class is responsible for \_\_\_." If that sentence has an "and" in it, you may have a Single Responsibility violation. SRP is the most common thing LLD interviewers look for.

### Step 4 — Apply Patterns (10 minutes)

Once relationships are clear, identify where standard patterns reduce complexity. Do not force patterns. The question is: "Is there a known structural solution to this recurring problem?" If yes, name it and explain why it fits.

See the pattern trigger table below.

---

## How to Draw a Class Diagram Verbally

Many interviews happen on a whiteboard, Google Docs, or a plain text editor. You need to communicate class structure without drawing tool support. Use this script:

**Class declaration**: "I have a class called `OrderProcessor`. It has two private fields: `paymentGateway` of type `PaymentGateway` — that's an interface — and `orderRepository` of type `OrderRepository`."

**Method signature**: "It exposes one public method, `processOrder`, which takes an `Order` object and returns a `ProcessingResult`. Internally it calls `paymentGateway.charge` and `orderRepository.save`."

**Relationship**: "The `OrderProcessor` depends on the `PaymentGateway` interface, not any concrete implementation. It holds a reference — not ownership — so this is a dependency, not composition."

**Polymorphism**: "I'll have `StripeGateway` and `PayPalGateway` both implement `PaymentGateway`. The `OrderProcessor` doesn't know which one it gets — that's resolved at construction time."

This verbal style is more impressive than silent drawing because it shows you can reason about structure out loud — which is what senior engineers do in real design sessions.

---

## ASCII Class Diagram Notation

Used throughout these docs:

```
+---------------------------+
|      ClassName            |  <-- class name in header
+---------------------------+
| - privateField: Type      |  <-- fields (- private, + public, # protected)
| + publicField: Type       |
+---------------------------+
| + publicMethod(): Return  |  <-- methods
| - privateMethod(): void   |
+---------------------------+

<<interface>>                  <-- interface marker
<<abstract>>                   <-- abstract class marker

A -----> B                     <-- A depends on B (uses)
A ------o B                    <-- A has a reference to B (association)
A <>----o B                    <-- A aggregates B (B lifecycle independent)
A <>==== B                     <-- A composes B (A owns B's lifecycle)
A =====> B                     <-- A inherits from / implements B
```

---

## Pattern Trigger Table

When you hear the interviewer describe a problem, map it to a pattern before reaching for code.

| If the interviewer says... | Think... | Why |
|---|---|---|
| "We need to support multiple payment types / notification channels / storage backends and add more later" | **Strategy** | Interchangeable algorithms behind a common interface |
| "We need to create objects but don't want the caller to know which concrete class gets created" | **Factory / Abstract Factory** | Decouples construction from use |
| "There should only be one instance of this across the whole system" | **Singleton** | Controlled instantiation — also ask whether you actually need it |
| "We need to undo operations / support command history" | **Command** | Encapsulates a request as an object |
| "Objects need to notify other objects when their state changes, without tight coupling" | **Observer** | Publisher/subscriber decoupling |
| "We need to add behavior to objects without changing their class" | **Decorator** | Wraps existing behavior, open for extension |
| "Building a complex object requires many steps that vary" | **Builder** | Separates construction from representation |
| "We need a simplified interface to a complex subsystem" | **Facade** | Single entry point to a library |
| "We need to treat individual items and groups of items uniformly" | **Composite** | Tree structures, recursive containment |
| "We need to walk through a collection without exposing its internals" | **Iterator** | Traversal abstraction |
| "We need to change an object's behavior based on its internal state" | **State** | Replaces conditional chains with state objects |
| "An object needs to be constructed from a family of related objects that must match" | **Abstract Factory** | Ensures consistent product families |
| "We need to adapt an existing class to an interface we don't control" | **Adapter** | Interface translation without modifying source |
| "We want to defer expensive object creation and control access" | **Proxy** | Controls access to real subject |

**Key discipline**: Name the pattern out loud and explain the fit in one sentence before you start writing any code. "This is a Strategy pattern because we want the discount calculation to be swappable without touching the order processing logic."

---

## Common LLD Interview Questions

### Beginner–Intermediate (appear in mid-level rounds, but expected to be solved faster at senior level)

- **Parking Lot** — multi-level, vehicle types, ticketing, fees. Tests: Inheritance hierarchy, State pattern for spots, Factory for ticket types.
- **Vending Machine** — item selection, payment, change. Tests: State pattern (idle → selected → paid → dispensing), inventory management.
- **ATM** — card insertion, PIN, withdrawal, deposit. Tests: State pattern, Transaction modeling, security boundary thinking.
- **Library Management** — books, members, borrowing, fines. Tests: SRP, composition, date/deadline logic.

### Core Senior Level

- **BookMyShow / Movie Ticket Booking** — theaters, shows, seats, bookings, concurrent seat reservation. Tests: Concurrency awareness, Observer for seat status, Strategy for pricing.
- **Elevator System** — multiple elevators, floor requests, scheduling. Tests: State pattern (IDLE/MOVING/DOOR_OPEN), scheduling algorithm choice.
- **Hotel Booking System** — rooms, reservations, check-in/out, billing. Tests: Builder for reservations, Strategy for pricing, Observer for notifications.
- **Chess** — pieces, board, move validation, game state. Tests: deep object modeling, Composite for board, polymorphic move validation.
- **LRU Cache** — get/put with O(1), eviction. Tests: Data structure knowledge, whether you model it as a class with clean interface.
- **Rate Limiter** — token bucket, sliding window. Tests: Strategy pattern, thread safety thinking.

### Staff / System-Design-Adjacent LLD

- **Notification Service** — email, SMS, push; templates, retry, priority queues. Tests: Strategy, Observer, Builder for notification construction.
- **Food Delivery System (Swiggy/DoorDash)** — order lifecycle, driver assignment, tracking. Tests: State machine, Observer, Factory for order types.
- **Ride Sharing (Uber)** — matching, pricing, driver state. Tests: Strategy for pricing surge, Observer for real-time updates, State for driver/rider.
- **Logging / Monitoring Framework** — log levels, sinks, formatters. Tests: Chain of Responsibility, Decorator, Singleton (Logger instance).
- **Payment Gateway** — multi-provider, retry, idempotency, audit trail. Tests: Strategy, Command (for retry), Observer (for events).

---

## 45-Minute LLD Round Breakdown

| Phase | Time | What you should be doing |
|---|---|---|
| Clarify requirements | 0–5 min | Ask questions, confirm scope, write use cases |
| Identify entities | 5–13 min | List classes, mark actors vs resources, mark interfaces |
| Define relationships | 13–23 min | Draw or describe class diagram, name associations |
| Apply patterns | 23–30 min | Identify 1–2 patterns, justify them verbally |
| Write core code | 30–42 min | Implement the most interesting 2–3 classes; skip boilerplate |
| Review and discuss | 42–45 min | Discuss extensibility, what would change with new requirements |

**The most common failure mode at senior level**: spending 30 minutes writing verbose Java boilerplate (getters, setters, constructors) and never getting to the interesting design decisions. Interviewers at senior level want to see the decisions, not the typing.

---

## What NOT To Do

**Do not jump to code before design.** Writing code in the first 5 minutes signals that you don't know how to design. Senior engineers design before they code. Spend at least 15–20 minutes on paper before any implementation.

**Do not over-engineer.** Adding every pattern you know to one design is a red flag. A Parking Lot system does not need an Abstract Factory, a Proxy, and an Observer all at once. Show restraint — apply patterns where there is a concrete problem they solve.

**Do not design for unlimited scale unless asked.** If the interviewer asks for a Parking Lot, they want object modeling. They do not want a discussion of Kafka topics and microservices. Read the room.

**Do not skip interface definitions.** Senior engineers define interfaces first. Concrete classes are implementation details. "I'd make this an interface because the caller should never depend on a specific implementation" is exactly the phrase interviewers want to hear.

**Do not treat all state as mutable.** Think about which objects are value objects (immutable, identity-less) vs entities (mutable, identity-bearing). A `Money` type should be immutable. A `Booking` has identity and mutable state.

**Do not model inheritance as your default relationship.** Composition over inheritance is a real principle. Every `extends` should be justified. When you hear yourself saying "X is a type of Y," ask: is it really, or does it just have some shared behavior?

**Do not ignore error handling in the design phase.** "What happens if the payment fails?" is an LLD question, not just an implementation concern. The design should show where exceptions propagate and which layer handles them.

---

## How These Docs Are Organized

```
LLD/
├── README.md              (this file — approach and strategy)
├── SOLID.md               (SOLID principles + DRY/KISS/YAGNI with Java examples)
├── Patterns/              (one file per design pattern)
│   ├── Strategy.md
│   ├── Observer.md
│   ├── ...
└── CaseStudies/           (end-to-end worked designs)
    ├── ParkingLot.md
    ├── BookMyShow.md
    ├── ...
```

Start with `SOLID.md` before the case studies. Every LLD case study is ultimately a demonstration of SOLID principles applied through design patterns. The principles are the foundation; the patterns are the vocabulary.
