## Decorators

In Java, decorators are a **structural design pattern** used to dynamically add or modify behavior of objects without altering their code. The Decorator Pattern achieves this by wrapping objects with other objects that provide additional functionality.

### Key Concepts of Decorators:
- **Component Interface**: Defines the common functionality for both the original object and the decorators.
- **Concrete Component**: The core object whose behavior you want to enhance.
- **Decorator**: An abstract class or interface that wraps the component and adds behavior.
- **Concrete Decorators**: Classes that extend the decorator and provide the specific enhancements.

### Example: Coffee Shop Scenario
Imagine you’re building a system for a coffee shop where customers can customize their drinks with toppings.

**Step 1**: Define the Component Interface
[Coffee](./Coffee.java)

**Step 2**: Create the Concrete Component
[Simple Coffee](./SimpleCoffee.java)

**Step 3**: Create the Abstract Decorator
[Coffee Decorator](./CoffeeDecorator.java)

**Step 4**: Create Concrete Decorators and Use the Decorators
[Driver](./Driver.java)


### Benefits of Decorators:
- **Open-Closed Principle**: New behaviors can be added without modifying existing code.
- **Dynamic Composition**: Combine behaviors at runtime by layering decorators.
- **Flexible Design**: Avoids large, monolithic classes with too many responsibilities.

### When to Use:
- When you need to add or modify object behavior without altering the object itself.
- When subclassing would lead to a complex hierarchy with too many variations.
