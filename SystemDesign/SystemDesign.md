# System Design

- [Solid Principle](#solid-principles)
- [DRY Principle](#the-dry-principle)


## SOLID Principles 

[With Pictures ↗︎](https://medium.com/backticks-tildes/the-s-o-l-i-d-principles-in-pictures-b34ce2f1e898) | [With Code ↗︎](https://blog.algomaster.io/p/solid-principles-explained-with-code)

1. **S - Single Responsibility**  
A class should have a single responsibility.

2. **O — Open-Closed**  
Classes should be open for extension, but closed for modification.

3. **[L — Liskov Substitution](./SolidPrinciples/LiskovSubstitution.java)**  
    If S is a subtype of T, then objects of type T in a program may be replaced with objects of type S without altering any of the desirable properties of that program.

    It states that a subclass should be replaceable by its parent class without altering the correctness of the program. In simpler terms, if a program works with a parent class, it should also work with any of its subclasses without needing changes.

4. **[I — Interface Segregation](./SolidPrinciples/InterfaceSegregation.java)**  
    Clients should not be forced to depend on methods that they do not use.

    The Interface Segregation Principle (ISP) states that a class should not be forced to implement interfaces it doesn’t use. Instead of having one large interface with many methods, break it into smaller, more specific interfaces that are tailored to the needs of the classes using them.

    - A fat interface (an interface with too many methods) forces classes to implement methods they don’t need.
    - ISP avoids this by creating focused, specific interfaces that provide only what a class actually needs.

5. **[D — Dependency Inversion](./SolidPrinciples//DependencyInversion.java)**  
    - High-level modules should not depend on low-level modules. Both should depend on the abstraction.

    - Abstractions should not depend on details. Details should depend on abstractions.

    It states that high-level modules (the overall logic of your application) should not depend on low-level modules (specific details of implementation). Instead, both should depend on abstractions (interfaces or abstract classes).

    This principle helps make your code more flexible, easier to test, and less prone to changes in one part affecting others.

    Key Idea:
    High-level modules define the overall functionality.
    Low-level modules handle specific tasks like accessing a database, reading files, etc.
    DIP ensures that the high-level module doesn’t directly depend on the low-level module but communicates via an abstraction (e.g., interface).


## The DRY Principle
### "Don't Repeat Yourself"  [↗︎](https://blog.algomaster.io/p/082450d8-0e7b-4447-a8dc-b7308e45f048)

> "Every piece of knowledge must have a single, unambiguous, authoritative representation within a system."

In other words, the DRY principle encourages developers to write modular, reusable code and avoid duplicating the same functionality in multiple places.

It encourages us to minimize redundancy and write code that does one thing well, making our lives (and the lives of those who maintain our code) much easier.

Try to : 
1. Avoide Code Duplication
2. Use Decorators for Cross-cutting Concerns

## Resources
https://github.com/ashishps1/awesome-low-level-design?tab=readme-ov-file