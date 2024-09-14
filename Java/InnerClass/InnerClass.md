# Inner Class

- Nested classes represent a particular type of relationship that is it can access all the members (data members and methods) of the outer class, including private.  
- An inner class is a part of a nested class. Non-static nested classes are known as inner classes.

## Types of Nested classes
There are two types of nested classes non-static and static nested classes. The non-static nested classes are also known as inner classes.

- Non-static nested class (inner class)
    - **Member inner class** : A class created within class and outside method.
    - **Anonymous inner class** : A class created for implementing an interface or extending class. The java compiler decides its name.
    - **Local inner class** : A class was created within the method.
- Static nested class : A static class was created within the class.
- Nested Interface : An interface created within class or interface.

## Member Inner Class [⤴︎](./MemberInnerClass.java)

The general form of syntax to create an object of the member inner class is as follows:  

**Syntax**
```
OuterClassReference.new MemberInnerClassConstructor();
```

**Example**
```
obj.new Inner();
```


### Internal working of Java member inner class

The java compiler creates two class files in the case of the inner class. The class file name of the inner class is "Outer$Inner". If you want to instantiate the inner class, you must have to create the instance of the outer class. In such a case, an instance of inner class is created inside the instance of the outer class.

## Java Anonymous Inner Class
[Using Class](./AnonymousInner.java) | [Using Interface](./AnonymousInnerInterface.java)

Java anonymous inner class is an inner class without a name and for which only a single object is created. It should be used if you have to override a method of class or interface. Java Anonymous inner class can be created in two ways:

1. Class (may be abstract or concrete).
2. Interface

## Local Inner Class [⤴︎](./LocalInnerClass.java)

A class that is created inside a method, is called local inner class in java. Local Inner Classes are the inner classes that are defined inside a block. They can be marked as final or abstract.

- Local inner class cannot be invoked from outside the method.
- Local inner class cannot access non-final local variable till JDK 1.7. Since JDK 1.8, it is possible to access the non-final local variable in the local inner class.

## Static Nested Class [⤴︎](./StaticNestedClass.java)

Static class defined inside another class. It cannot access non-static data members and methods. It can be accessed by outer class name.

- It can access static data members of the outer class, including private.
- The static nested class cannot access non-static (instance) data members or methods

## Java Nested Interface
[Inside Interface](./InterfaceNestedInterface.java) | [Inside Class](./InterfaceNestedInsideClass.java)

An interface that is declared within another interface or class, is known as a nested interface. The nested interfaces are used to group related interfaces so that they can be easy to maintain. The nested interface must be referred to by the outer interface or class. It can't be accessed directly.

- The nested interface must be public if it is declared inside the interface, but it can have any access modifier if declared within the class.
- Nested interfaces are declared static.
- In the collection framework, the sun microsystem has provided a nested interface Entry. Entry is the subinterface of Map, i.e., accessed by Map.Entry.

### Can we define a class inside the interface?
Yes, if we define a class inside the interface, the Java compiler creates a static nested class. Let's see how can we define a class within the interface:

```java
interface M {  
  class A { }  
}
```