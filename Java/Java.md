## Building Blocks

### Comments

```java
// single line comment

/*
* Multiple line comment
*/

/**
 * 
 * Java doc comment, used for generating documentation
 * @see https://www.baeldung.com/javadoc
 * 
 * /
```
**How to generate javadoc**
```
javadoc -d JavaDoc Calculator.java
```
[Example for Javadoc](./Calculator.java)  

### Defining Class and Files

- At most one public class can be in a java file.
- If there is any public class then it should be same as file name

### Writing main() method

- Program stars from ```public static void main(String[] args)``` method call.
- After compilation generates ```.class``` file.
- The variable name ```args``` hints that this list contains values that were read in (arguments) when the JVM started.

### Packages and Imports
- To run the java file use below commands
    ```bash
    javac filename.java
    java filename {arguments}

    // other ways of compiling
    javac packagea/ClassA.java packageb/ClassB.java 
    ```
- Java puts classes in packages.
- Packages are used to avoid naming conflicts
- You have to import packages in order to use the files in the package.
    ```java
    import java.util.*; // all the files in the package
    import java.util.Random; // only a single java class
    ```
- If you explicitly import a class name, it takes precedence over any wildcards present. 
- In case of conflict you could in this way
    ```java
    import java.util.Date;
    public class Conflicts {
        Date date;
        java.sql.Date sqlDate;
    }
    ```
- ```java.lang``` package is imported by default. It contains System, Wrappper, Object, Runtime, Thread, Compiler classes.
- If no package is defined then it's called **default package**.
- To defined a package use below format.  
    ```package dbms;```

### Creating Objects
- Use ```new``` keyword to create the object.
    ```java
    Animal dog = new Animal();
    ```
- ```Animal()``` is the constructor which is used to initialize the object.
- The name of the constructor matches the name of the class, and there's no return type.
    ```java
    public void Chick() { } // NOT A CONSTRUCTON
    ```
- If you do not create any constructor then compiler will provide you a **default constructor** which do not accept any arguments. This constructor is used to initiase primitive variables with default values.

- The constructor runs after all fields and instance initializer blocks have run. Order matters.
    ```java
    { System.out.println(name); }  // DOES NOT COMPILE
    private String name = "Fluffy";
    ```

### Primitive Types

Java has eight built-in data types, referred to as the Java primitive types.

| Type | Description | Default | Size | Example | Range of values |
| ---- | ----------- | ------- | ---- | ------- | --------------- |
| boolean  |	true or false |	false |	8 bits 	| true, false 	| true, false |
| byte |	twos-complement integer |	0 | 	8 bits | 	(none) 	| -128 to 127 |
| char | 	Unicode character |	\u0000 |	16 bits |	‘a’, ‘\u0041’, ‘\101’, ‘\\’, ‘\’, ‘\n’, ‘β’ 	| characters representation of ASCII values 0 to 255 |
| short | 	twos-complement integer | 	0 | 	16 bits | 	(none) | 	-32,768 to 32,767 |
| int |	twos-complement intger |	0 	| 32 bits |	-2,-1,0,1,2 |	-2,147,483,648 to 2,147,483,647 |
| long |	twos-complement integer | 	0 |	64 bits |	-2L,-1L,0L,1L,2L |	-9,223,372,036,854,775,808 to 9,223,372,036,854,775,807 |
| float |	IEEE 754 floating point |	0.0 |	32 bits |	1.23e100f , -1.23e-100f , .3f ,3.14F |	upto 7 decimal digits |
| double |	IEEE 754 floating point |	0.0 |	64 bits |	1.23456e300d , -123456e-300d , 1e1d |	upto 16 decimal digits |

- Use L/l at the end of literal to mark it as long and use f/F as the end of literal to mark it as float type.

### Variables and Identifiers
- The name must begin with a letter or the symbol $ or _.
- Local variables must be initialized before use. 
    ```java
    int q;
    System.out.println(q); // compile time error
    ```

    ```java
    public void findAnswer(boolean check) {
        int answer;
        int onlyOneBranch;
        if (check) {
            onlyOneBranch = 1;
            answer = 1;
        } else {
            answer = 2;
        }
        System.out.println(answer);
        System.out.println(onlyOneBranch); // DOES NOT COMPILE
    }
    ```
- Instance and class variables do not require you to initialize them.
- Scope
    - Local variables—in scope from declaration to end of block
    - Instance variables—in scope from declaration until object garbage collected
    - Class variables—in scope from declaration until program ends


## OOP Principles

### 1. Encapsulation

**What it is:** Keeping an object's data private and only exposing controlled operations on it.

**Without encapsulation - anything can go wrong:**
```java
class BankAccount {
    double balance;   // public — anyone can write directly
}

account.balance = -500;   // valid! No check. Invariant broken.
```

**With encapsulation - the class owns its invariants:**
```java
class BankAccount {
    private double balance;   // nobody writes this directly

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Must be positive");
        balance += amount;
    }

    public void withdraw(double amount) {
        if (amount > balance) throw new IllegalStateException("Insufficient funds");
        balance -= amount;
    }

    public double getBalance() { return balance; }
}
```

Now `balance` can never go negative unless a bug is inside `BankAccount` itself - easy to find and fix.

**The other benefit:** you can change the internal implementation (e.g., store balance in cents as `long` for precision) without touching any caller. The public interface stays the same.

**Access modifiers:**
| Modifier | Same class | Same package | Subclass | Everywhere |
|---|---|---|---|---|
| `private` | ✓ | — | — | — |
| (package) | ✓ | ✓ | — | — |
| `protected` | ✓ | ✓ | ✓ | — |
| `public` | ✓ | ✓ | ✓ | ✓ |

---

### 2. Inheritance

**What it is:** A class acquires the fields and methods of a parent class. Models an "is-a" relationship.

```java
class Animal {
    String name;
    void breathe() { System.out.println("breathing"); }
}

class Dog extends Animal {
    void fetch() { System.out.println(name + " fetches!"); }  // inherits name
}

Dog d = new Dog();
d.breathe();   // inherited from Animal
d.fetch();     // Dog's own method
```

**What you can do in a subclass:**
- Use all non-private fields and methods from parent
- Override methods to change behavior (`@Override`)
- Add new fields and methods
- Call parent via `super.method()` or `super()`

**Rules for overriding:**
```java
class Animal {
    protected Animal makeChild() { return new Animal(); }  // covariant return
}

class Dog extends Animal {
    @Override
    public Dog makeChild() { return new Dog(); }  // OK: more accessible, subtype return
    // Cannot narrow access (e.g. private would fail)
    // Cannot throw new/broader checked exceptions
}
```

**When NOT to use inheritance:**
If you can't say "a Dog IS-A Animal" truthfully in every context, don't extend. Classic mistake:
```java
class Stack extends ArrayList { }  // Stack IS-A ArrayList? No.
// Now Stack has add(), remove(), get() exposed — violates stack contract.
// Should be: Stack HAS-A list internally (composition)
```

---

### 3. Composition

**What it is:** Building behavior by combining objects rather than inheriting it.

```java
// Instead of: class FlyingDog extends Dog, Bird  (Java doesn't allow this)
// Use composition:
class FlyingDog {
    private Dog dog = new Dog();      // HAS-A dog
    private Wings wings = new Wings(); // HAS-A wings

    void fetch()  { dog.fetch(); }
    void fly()    { wings.flap(); }
}
```

**Why composition wins over inheritance:**

```java
// Problem with inheritance: locked at compile time
class Logger {
    void log(String msg) { System.out.println(msg); }
}
class AuditLogger extends Logger {
    @Override void log(String msg) { super.log("[AUDIT] " + msg); }
}
// AuditLogger is forever tied to Logger's internals.
// If Logger.log() changes behavior, AuditLogger silently changes too.

// With composition: behavior is explicit and swappable
class AuditLogger {
    private final Logger delegate;  // injected - could be any Logger

    AuditLogger(Logger delegate) { this.delegate = delegate; }

    void log(String msg) { delegate.log("[AUDIT] " + msg); }
}
// Swap the delegate at runtime. No inheritance coupling.
```

**Rule: Favor composition over inheritance.** Use inheritance only when:
- The relationship is truly "is-a" in every context
- You control the parent class (extending third-party classes is risky)

---

### 4. Polymorphism

**What it is:** One interface, many implementations. The same code works with different types.

#### Compile-time polymorphism - Overloading
Same method name, different parameter lists. Resolved by the compiler based on argument types.

```java
class Printer {
    void print(String s)         { System.out.println("String: " + s); }
    void print(int n)            { System.out.println("Int: " + n); }
    void print(String s, int n)  { System.out.println(s + " x " + n); }
}

Printer p = new Printer();
p.print("hi");      // → print(String)
p.print(42);        // → print(int)
p.print("hi", 3);   // → print(String, int)
```

Note: return type alone does NOT distinguish overloads - the compiler uses parameter types only.

#### Runtime polymorphism - Overriding
Subclass provides its own implementation. The JVM decides which one to call at runtime based on the actual object type - not the reference type.

```java
class Animal {
    void sound() { System.out.println("..."); }
}
class Dog extends Animal {
    @Override void sound() { System.out.println("Woof"); }
}
class Cat extends Animal {
    @Override void sound() { System.out.println("Meow"); }
}

// Same code, different behavior depending on actual type:
Animal[] animals = { new Dog(), new Cat(), new Dog() };
for (Animal a : animals) {
    a.sound();    // Woof, Meow, Woof - JVM dispatches to actual type
}
```

**Why it matters:** You can write code against the `Animal` interface and it works for every subtype - including ones written in the future. This is the foundation of the Open/Closed Principle (open for extension, closed for modification).

**Variable and static method shadowing (not overriding):**
```java
class Parent {
    String name = "Parent";
    static void greet() { System.out.println("Hello from Parent"); }
}
class Child extends Parent {
    String name = "Child";     // shadows, not overrides
    static void greet() { System.out.println("Hello from Child"); }
}

Parent p = new Child();
System.out.println(p.name);  // "Parent" — reference type wins for fields
p.greet();                   // "Hello from Parent" — reference type wins for static
// Only instance methods are overridden (runtime dispatch)
```

---

### 5. Abstraction

**What it is:** Exposing *what* something does, hiding *how* it does it. You interact with a simplified interface, not the internal complexity.

Two mechanisms in Java:

**Abstract class** - partially implemented, forces subclasses to complete it:
```java
abstract class Shape {
    String color;
    Shape(String color) { this.color = color; }

    abstract double area();     // "what" — every shape must implement this
    abstract double perimeter();

    void describe() {           // "how" — shared logic reused by all shapes
        System.out.printf("%s: area=%.2f%n", color, area());
    }
}

class Circle extends Shape {
    double radius;
    Circle(String color, double radius) { super(color); this.radius = radius; }

    @Override double area()      { return Math.PI * radius * radius; }
    @Override double perimeter() { return 2 * Math.PI * radius; }
}
```

**Interface** - pure contract, no implementation (pre-Java 8):
```java
interface Drawable {
    void draw();         // every implementor must provide this
}
interface Resizable {
    void resize(double factor);
}

// A class can fulfill multiple contracts
class Circle extends Shape implements Drawable, Resizable {
    public void draw()               { /* render to screen */ }
    public void resize(double f)     { radius *= f; }
}
```

---

### Interface vs Abstract Class - The Key Interview Question

| | Interface | Abstract Class |
|---|---|---|
| Multiple inheritance | Yes - a class can implement many | No - single `extends` only |
| Constructor | No | Yes |
| Fields | `public static final` only | Any modifier |
| Methods (pre-Java 8) | Abstract only | Abstract + concrete |
| Methods (Java 8+) | Abstract + `default` + `static` | Abstract + concrete |
| Instance state | No | Yes |
| Use when | Define a **capability/contract** across unrelated classes | Share **implementation** among related classes |

```java
// Interface — Bird and Airplane are unrelated, but both can fly
interface Flyable { void fly(); }
class Bird    implements Flyable { public void fly() { /* flap */ } }
class Airplane implements Flyable { public void fly() { /* engine */ } }

// Abstract class — Circle and Rectangle are related (both shapes)
// Share state (color) and behavior (describe())
abstract class Shape {
    protected String color;
    Shape(String color)     { this.color = color; }
    abstract double area();
    void describe()         { System.out.println(color + ": " + area()); }
}
class Circle    extends Shape { /* ... */ }
class Rectangle extends Shape { /* ... */ }
```

**Java 8+ blurred the line** with `default` methods on interfaces. Still choose abstract class when you need: instance state, a constructor, or `protected`/package-private access.

### equals() and hashCode() Contract - Critical

**Contract:** if `a.equals(b)` is `true`, then `a.hashCode() == b.hashCode()` MUST be true.
(The reverse is not required - hash collisions are allowed.)

**Why both must be overridden together:**
```java
class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }

    // ONLY overriding equals — hashCode still uses object identity
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point p)) return false;
        return x == p.x && y == p.y;
    }
}

Point p1 = new Point(1, 2);
Point p2 = new Point(1, 2);
p1.equals(p2);  // true — equals says same

Set<Point> set = new HashSet<>();
set.add(p1);
set.contains(p2);  // FALSE! — different hashCode → different bucket → never found
```

**Correct implementation:**
```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Point p)) return false;
    return x == p.x && y == p.y;
}

@Override
public int hashCode() {
    return Objects.hash(x, y);   // consistent with equals
}
```

**Interview trap:** "What happens if you only override `equals`?" → HashMap and HashSet break silently.

### Comparable vs Comparator

```java
// Comparable — natural ordering, class modifies itself (one sort order)
class Employee implements Comparable<Employee> {
    String name;
    int salary;

    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.salary, other.salary);  // ascending salary
    }
}

// Comparator — external ordering, doesn't touch the class (multiple sort orders)
Comparator<Employee> byName = Comparator.comparing(Employee::getName);
Comparator<Employee> bySalary = Comparator.comparingInt(Employee::getSalary).reversed();
Comparator<Employee> byNameThenSalary = byName.thenComparing(bySalary);

employees.sort(byNameThenSalary);
Collections.sort(employees);  // uses Comparable (natural order)
```

| | Comparable | Comparator |
|---|---|---|
| Location | Inside the class | External / anonymous / lambda |
| Method | `compareTo(T o)` | `compare(T o1, T o2)` |
| Sort orders | One (natural) | Multiple |
| Use when | Class has a clear natural order | Need custom/multiple orderings |

### Difference between final, finally and finalize

| Sr. no. | 	Key |	final |	finally	| finalize |
| ------- | ------- | ------- | ------- | -------- |
| 1. |	Definition |	final is the keyword and access modifier which is used to apply restrictions on a class, method or variable. |	finally is the block in Java Exception Handling to execute the important code whether the exception occurs or not.	|finalize is the method in Java which is used to perform clean up processing just before object is garbage collected. |
| 2. |	Applicable to |	Final keyword is used with the classes, methods and variables. |	Finally block is always related to the try and catch block in exception handling. |	finalize() method is used with the objects. |
| 3. |	Functionality |	<span style="color:#E19898">(1) Once declared, final variable becomes constant and cannot be modified. (2) final method cannot be overridden by sub class. (3) final class cannot be inherited.</span> | <span style="color:#E19898">(1) finally block runs the important code even if exception occurs or not. (2) finally block cleans up all the resources used in try block. </span> | <span style="color:#E19898">finalize method performs the cleaning activities with respect to the object before its destruction.</span>
| 4. |	Execution |	Final method is executed only when we call it. | Finally block is executed as soon as the try-catch block is executed. It's execution is not dependant on the exception. | finalize method is executed just before the object is destroyed.

### Difference between ```equals()``` and ==

```java
String a = "Hey";
String b = new String("Hey");

if(a == b)
    System.err.println("Equal");
else
    System.out.println("Not"); // this executes
```

### Constructor Definition Rules:

- The first statement of every constructor is a call to another constructor within the class using this(), or a call to a constructor in the direct parent class using super().
- The super() call may not be used after the first statement of the constructor.
- If no super() call is declared in a constructor, Java will insert a no-argument super() as the first statement of the constructor.
- If the parent doesn't have a no-argument constructor and the child doesn't define any constructors, the compiler will throw an error and try to insert a default no-argument constructor into the child class.
- If the parent doesn't have a no-argument constructor, the compiler requires an explicit call to a parent constructor in each child constructor.

### Overriding a method is not without limitations, though. The compiler performs the following checks when you override a nonprivate method:

- The method in the child class must have the same signature as the method in the parent class.
- The method in the child class must be at least as accessible or more accessible than the method in the parent class.
- The method in the child class may not throw a checked exception that is new or broader than the class of any exception thrown in the parent class method.
- If the method returns a value, it must be the same or a subclass of the method in the parent class, known as covariant return types


### Important Points

- Variables and static methods are shadowed but not overridden.
- A class or interface type T will be initialized immediately before the first occurrence of any one of the following:

    > T is a class and an instance of T is created.  
    > T is a class and a static method declared by T is invoked.  
    > A static field declared by T is assigned.  
    > A static field declared by T is used and the field is not a constant variable.  
    > T is a top-level class, and an assert statement lexically nested within T is executed. 
- hashCode() method returns the hash code for the Method class object.

## Functional Interfaces (java.util.function)

The 4 core functional interfaces - know these cold:

```java
// Predicate<T> — boolean test on T
Predicate<String> isLong = s -> s.length() > 5;
isLong.test("Hello");       // false
isLong.test("Hello World"); // true

// Composition
Predicate<String> startsWithH = s -> s.startsWith("H");
Predicate<String> longAndH = isLong.and(startsWithH);   // both must match
Predicate<String> longOrH  = isLong.or(startsWithH);    // either
Predicate<String> notLong  = isLong.negate();

// Function<T, R> — maps T to R
Function<String, Integer> strLen = String::length;
strLen.apply("hello");  // 5

// Chaining
Function<String, String> upper = String::toUpperCase;
Function<String, Integer> upperLen = upper.andThen(strLen); // apply upper, then strLen
Function<String, Integer> lenFirst = strLen.compose(upper); // apply upper first, then strLen

// Consumer<T> — accepts T, returns nothing (side effect)
Consumer<String> print = System.out::println;
print.accept("hello");  // prints hello

// Supplier<T> — no input, produces T
Supplier<List<String>> listFactory = ArrayList::new;
List<String> list = listFactory.get();  // new ArrayList each time
```

**BiXxx variants for two arguments:**
```java
BiFunction<String, Integer, String>  // (String, Integer) → String
BiPredicate<String, String>          // (String, String) → boolean
BiConsumer<String, Integer>          // (String, Integer) → void
```

**Comparator.comparing() - most used in practice:**
```java
// Chain comparators fluently
employees.sort(
    Comparator.comparing(Employee::getDepartment)
              .thenComparingInt(Employee::getSalary)
              .reversed()
);
```

- Features Introduced in Java 8 :
    - Stream API
    - Lambda Expressions : A lambda expression in Java is a concise way to represent an anonymous function—a function without a name. Introduced in Java 8, it enables functional programming features and is widely used with the Stream API and functional interfaces like Runnable, Callable, and Comparator
    - Functional Interface : Interface with a single abstract method which can be implemented using lambda expression.
    - Date/Time API
    - Comparable and Comparator
    - Interface Default and Static Methods : interfaces can have static and default methods that, despite being declared in an interface, have a defined behavior.
    - Method References
        - Reference to a Static Method ```anyMatch(User::isRealUser)```
        - Reference to an Instance Method ```anyMatch(user::isLegalName)```
        - Reference to an Instance Method of an Object of a Particular Type ```filter(String::isEmpty)```
        - Reference to a Constructor ```map(User::new)```
    - Optional<T> : Optional<T> class can help to handle situations where there is a possibility of getting the NPE. It works as a container for the object of type T. It can return a value of this object if this value is not a null. When the value inside this container is null, it allows doing some predefined actions instead of throwing NPE.
- In Java, the transient and volatile keywords are used to improve the reliability and efficiency of applications. They are used to: 
    - The transient keyword prevents sensitive data from being serialized, ensuring it remains private
    - The volatile keyword ensures that all threads have access to the most up-to-date value
    - The transient and volatile keywords help to write robust and thread-safe code

    - Transient: Used with instance variables to exclude them from serialization
    - Volatile: Used with variables to indicate that the JVM and compiler always read its value from main memory
    
    Differences between transient and volatile
    - transient cannot be used with the static keyword, but volatile can 
    - transient variables are initialized with default values during de-serialization 
    - volatile ensures visibility and atomicity for simple read and write operations 


### Stream API

The Stream API in Java, introduced in Java 8, is a powerful tool for working with sequences of elements. It allows you to perform operations like filtering, mapping, and reducing on collections or arrays in a declarative, functional programming style. Streams enable efficient processing of data by supporting lazy evaluation, parallel execution, and concise syntax.

```java
import java.util.*;
import java.util.stream.*;

public class StreamExample {
    public static void main(String[] args) {
        // Create a list of numbers
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // Filter even numbers, square them, and collect into a list
        List<Integer> evenSquares = numbers.stream()
                                           .filter(n -> n % 2 == 0) // Filter even numbers
                                           .map(n -> n * n)         // Square them
                                           .collect(Collectors.toList()); // Collect results

        System.out.println("Even squares: " + evenSquares);
    }
}
```

![Maps](image.png)

### Resources

- [JDK vs JRE vs JVM](https://www.geeksforgeeks.org/differences-jdk-jre-jvm/)
- [Internal Working of Java HashMap](https://www.javatpoint.com/working-of-hashmap-in-java)
- [Integer vs int](https://www.theserverside.com/blog/Coffee-Talk-Java-News-Stories-and-Opinions/int-vs-Integer-java-difference-comparison-primitive-object-types#:~:text=The%20key%20difference%20between%20the,included%20in%20the%20Java%20API.)
- [Comparable vs Comparator](https://www.javatpoint.com/difference-between-comparable-and-comparator)
- [Double-Checked Locking](https://refactoring.guru/java-dcl-issue)