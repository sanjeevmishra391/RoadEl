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

