# Design Patterns — Interview Guide

> 23 patterns exist. You need to know 12 deeply. The rest you can name and define in one sentence.

---

## What is a Design Pattern?

A reusable solution to a recurring design problem. Not a finished design — a template you adapt.

Three groups:
- **Creational** — how objects are created
- **Structural** — how objects are composed
- **Behavioural** — how objects communicate

---

## The 12 You Must Know Cold

These appear in LLD interviews, system design discussions, and code review conversations.

### Creational

| Pattern | One-line purpose | Deep Dive |
|---|---|---|
| **Singleton** | Exactly one instance, global access point | [Singleton.md](./Creational/Singleton.md) |
| **Builder** | Construct complex objects step by step | [Builder.md](./Creational/Builder.md) |
| **Factory Method** | Subclass decides which object to create | [Factory.md](./Creational/Factory.md) |
| **Abstract Factory** | Create families of related objects | [AbstractFactory.md](./Creational/AbstractFactory.md) |

### Structural

| Pattern | One-line purpose | Deep Dive |
|---|---|---|
| **Decorator** | Add behavior without subclassing | [Decorator.md](./Structural/Decorator.md) |
| **Adapter** | Make incompatible interfaces work together | [Adapter.md](./Structural/Adapter.md) |
| **Proxy** | Control access to an object | [Proxy.md](./Structural/Proxy.md) |
| **Facade** | Simplify a complex subsystem | [Facade.md](./Structural/Facade.md) |

### Behavioural

| Pattern | One-line purpose | Deep Dive |
|---|---|---|
| **Observer** | Notify many objects when state changes | [Observer.md](./Behavioural/Observer.md) |
| **Strategy** | Swap algorithms at runtime | [Strategy.md](./Behavioural/Strategy.md) |
| **Command** | Encapsulate a request as an object (undo/redo) | [Command.md](./Behavioural/Command.md) |
| **State** | Change behavior when internal state changes | [State.md](./Behavioural/State.md) |
| **Template Method** | Algorithm skeleton with overridable steps | [Template.md](./Behavioural/Template.md) |

---

## The Other 10 — Know the Name + One Line

You won't be asked to implement these in a 45-minute LLD round. But if they come up in a follow-up, know what they are.

| Pattern | One-line purpose |
|---|---|
| Prototype | Clone an existing object instead of creating from scratch |
| Composite | Treat individual objects and groups uniformly (tree structure) |
| Bridge | Separate abstraction from implementation so both can vary independently |
| Flyweight | Share fine-grained objects to reduce memory when many similar objects exist |
| Chain of Responsibility | Pass a request through a chain of handlers until one handles it |
| Iterator | Access elements of a collection without exposing its structure |
| Mediator | Centralize complex communication between many objects |
| Memento | Capture and restore an object's state (snapshot/undo) |
| Visitor | Add operations to objects without modifying their classes |
| Interpreter | Define a grammar and interpret sentences in a language |

---

## Pattern vs Pattern — Common Interview Confusions

| Pair | Key Distinction |
|---|---|
| **Decorator vs Proxy** | Decorator adds behavior; Proxy controls access. Same structure, different intent. |
| **Decorator vs Inheritance** | Decorator composes at runtime; inheritance is fixed at compile time. |
| **Factory vs Abstract Factory** | Factory creates one product type; Abstract Factory creates a *family* of related products. |
| **Strategy vs State** | Strategy is externally swapped (algorithm choice); State transitions itself (behavior changes with internal state). |
| **Observer vs Pub-Sub** | Observer is direct (subject holds observer references); Pub-Sub has a broker between publisher and subscriber. |
| **Facade vs Mediator** | Facade simplifies a subsystem for the caller; Mediator coordinates between sibling components. |
| **Proxy vs Adapter** | Proxy wraps same interface; Adapter converts one interface to another. |
| **Command vs Strategy** | Command encapsulates a *request* (what to do + when + undo); Strategy encapsulates an *algorithm* (how to do it). |
| **Template vs Strategy** | Template uses inheritance (override steps); Strategy uses composition (inject algorithm). |

---

## When to Apply Each Pattern — Trigger Table

| If the interviewer says... | Reach for... |
|---|---|
| "Only one instance of this service should exist" | Singleton |
| "This object has many optional configuration fields" | Builder |
| "Create objects without knowing the exact class" | Factory Method |
| "Support multiple themes / platforms / families" | Abstract Factory |
| "Add logging / auth / retry without touching the class" | Decorator |
| "Wrap a legacy API behind a new interface" | Adapter |
| "Lazy-load a heavy resource / add auth check" | Proxy |
| "Hide a complex subsystem behind a simple call" | Facade |
| "Many components need to react when X changes" | Observer |
| "Support multiple payment methods / sorting algorithms" | Strategy |
| "Need undo/redo / request queuing" | Command |
| "Object behavior changes based on its current state" | State |
| "Algorithm skeleton with some steps that vary" | Template Method |

---

## Study Order

For interview prep, go in this sequence:

1. **SOLID first** — [../SOLID.md](../SOLID.md) — patterns are SOLID applied
2. **State** — appears in almost every LLD case study
3. **Strategy** — most interviewable, most common in real code
4. **Observer** — event-driven systems everywhere
5. **Factory / Builder** — creational basics
6. **Decorator / Proxy / Facade** — structural trio, easy to confuse
7. **Command** — for undo/redo and task queuing systems
8. **Template Method** — JdbcTemplate, every framework uses this
9. **Singleton** — know the thread-safe variants cold
10. **Abstract Factory / Adapter** — supporting cast
