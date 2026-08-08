## Operators

A Java operator is a special symbol that can be applied to a set of variables, values, or literals—referred to as operands—and that returns a result.
> Unary, Binary, and Ternary operators

### 1. Binary Operators

Evaluated from right to left but follows precendence

- Arithmetic Operators  
    Operators : + (addition), - (substraction), * (multiplication), / (division), % (modulus)  

    The multiplicative operators (*, /, %) have a higher order of precedence than the additive operators (+, -).

    > The modulus operation is not limited to positive integer values in Java and may also be applied to negative integers and floating-point integers

- Compound Assignment Operators

    Example:
    ```java
    int x = 2, z = 3;
    x = x * z;  // Simple assignment operator
    x *= z;  // Compound assignment operator
    ```

    ```java
    long x = 10;
    int y = 5;
    y = y * x;  // DOES NOT COMPILE, because long can not be casted to int
    // using compound assignment operator will resolve this
    y *= x;
    ```

    The assignment operator is that the result of the assignment is an expression in and of itself, equal to the value of the assignment.
    ```java
    long x = 5;
    long y = (x=3);
    System.out.println(x); // Outputs 3
    System.out.println(y); // Also, outputs 3
    ```

- Relational Operators
Compare two expressions and return a boolean value.

    | Operator | Definition |
    | -------- | ---------- |
    | <	   | Strictly less than |
    | <=   | Less than or equal to |
    | >	   | Strictly greater than |
    | >=   | Greater than or equal to |
    | instanceof | Determines whether an arbitrary object is a member of a particular class or interface |

- Logical Operators
    | Operator  | Definition         | Value |
    | --------- | ------------------ | ----- |
    | &&        | And                | True, if both operands are true |
    | \|\|      | Or                 | False, if both operands are false |
    | !         | Not                | True > False, False > True |
    | ^         | XOR (Exclusive OR) | If operands are same then True, else False |

    && and || are often referred to as **short-circuit operators**

    ```java
    int x = 6;
    boolean y = (x >= 6) || (++x <= 7);
    System.out.println(x); // x will not be incremented because the right-hand side of the expression is never evaluated due to short circuit
    ```

- Equality Operators
    | Operator  | Definition         | 
    | --------- | ------------------ |
    | ==        | Equals             |
    | !=        | Not Equals         | 

    - If the numeric values are of different data types, the values are automatically promoted. ```5 == 5.00``` returns ``true`` since the left side is promoted to a ``double``.

    ```java
    boolean x = true == 3;  // DOES NOT COMPILE
    boolean y = false != "Giraffe";  // DOES NOT COMPILE
    boolean z = 3 == "Kangaroo";  // DOES NOT COMPILE
    ```

    ```java
    boolean y = false;
    boolean x = (y = true);
    System.out.println(x);  // Outputs true
    ```

    For object comparison, the equality operator is applied to the references to the objects, not the objects they point to. Two references are equal if and only if they point to the same object, or both point to null.
    ```java
    File x = new File("myFile.txt");
    File y = new File("myFile.txt");
    File z = x;
    System.out.println(x == y);  // Outputs false
    System.out.println(x == z);  // Outputs true
    ```

### Numeric Promotion Rules

- If two values have different data types, Java will automatically promote one of the values to the larger of the two data types.
- If one of the values is integral and the other is floating-point, Java will automatically promote the integral value to the floating-point value's data type.
- Smaller data types, namely byte, short, and char, are first promoted to int any time they're used with a Java binary arithmetic operator, even if neither of the operands is int.
- After all promotion has occurred and the operands have the same data type, the resulting value will have the same data type as its promoted operands.

```java
short x = 10;
short y = 3;
short z = x * y;  // DOES NOT COMPILE.
// cast to make it run
```

### 2. Unary Operators

Operators : ++, --, -, !  
++x : pre increment, first increases the value by 1 then used.  
x++ : post increment, first use the value then increment the value.  

### 3. Ternary Operator

boolean_statement ? statement_to_execute_when_true : statement_to_execute_when_false;

Example:
```java
int a = x > 100 ? x*2 : x/2;
```

```java
System.out.println((y > 5) ? 21 : "Zebra");
int animal = (y < 91) ? 9 : "Horse";  // DOES NOT COMPILE
```

The ``System.out.println()`` does not care that the statements are completely different types, because it can convert both to String.
On the other hand, the compiler does know that "Horse" is of the wrong data type and cannot be assigned to an int; therefore, it will not allow the code to be compiled.

As of Java 7, only one of the right-hand expressions of the ternary operator will be evaluated at runtime. In a manner similar to the short-circuit operators, if one of the two right-hand expressions in a ternary operator performs a side effect, then it may not be applied at runtime. Let's illustrate this principle with the following example:

```java
int y = 1;
int z = 1;
final int x = y<10 ? y++ : z++;
System.out.println(y+","+z); // Outputs 2,1
```
## Statements

### The ``if-else`` statement

-
    ```java
    if(hourOfDay < 11)
        System.out.println("Good Morning");
        morningGreetingCount++;
    ```

    Based on the indentation, you might be inclined to think the variable morningGreetingCount is only going to be incremented if the hourOfDay is less than 11, but that's not what this code does. It will execute the print statement only if the condition is met, but it will always execute the increment operation.

    --- 
- 
    ```java
    if(hourOfDay < 15) {
        System.out.println("Good Afternoon");
    } else if(hourOfDay < 11) {
        System.out.println("Good Morning");  // UNREACHABLE CODE
    } else {
        System.out.println("Good Evening");
    }
    ```

    ---
- 
    ```java
    int x = 1;
    if(x) {  // DOES NOT COMPILE
        …
    }
    ```

    ---
-
    ```java
    int x = 1;
    if(x = 5) {  // DOES NOT COMPILE
    …
    }
    ```

### The ```switch``` statement

> **The case statement value must also be a literal, enum constant, or final constant variable.**

**Psudo**
```java
switch(condition) {
    case constantExpression1:
        // statements
        break;
    case constantExpression2:
        // statements
        break;
    default:
        // statements
}
```

**How not to write switch statements**
```java
int dayOfWeek = 5;
switch(dayOfWeek) {
  case 0:
    System.out.print("Sunday ");
  default:
    System.out.print("Weekday ");
  case 6:
    System.out.println("Saturday");
  break;
}
```

The output will be ```Weekday Saturday``` since the break statement is missing.

```java
private int getSortOrder(String firstName, final String lastName) {
    String middleName = "Patricia";
    final String suffix = "JR";
    int id = 0;
    switch(firstName) {
        case "Test":
        return 52;
    case middleName:       // DOES NOT COMPILE
        id = 5;
        break;
    case suffix:
        id = 0;
        break;
    case lastName:  // DOES NOT COMPILE : Despite lastName being final, it is not constant as it is passed to the function
        id = 8;
        break;
    case 5:  // DOES NOT COMPILE
        id = 7;
        break;
    case 'J':  // DOES NOT COMPILE
        id = 10;
        break;
    case java.time.DayOfWeek.SUNDAY:  // DOES NOT COMPILE: it is enum
        id=15;
        break;
    }
    return id;
}
```

### The ```while``` statement

Infinite Loop:
```java
int x = 2;
int y = 5;
while(x < 10)
  y++;
```

### The ```do-while``` statement
It guarantees that the statement or block will be executed at least once.

### The ```for``` statement

**Psudo**
```java
for(initialization; booleanExpression; updateStatement) {
    // statements
}
```

Mutiple Terms
```java
int x = 0;
for(long y = 0, z = 4; x < 5 && y < 10; x++, y++) {
  System.out.print(y + " ");
}
System.out.print(x);
```
--- 
Redeclaring a Variable in the Initialization Block
```java
int x = 0;
for(long y = 0, x = 4; x < 5 && y < 10; x++, y++) {   // DOES NOT COMPILE
  System.out.print(x + " ");
}
```
--- 
Using Incompatible Data Types in the Initialization Block
```java
for(long y = 0, int x = 4; x < 5 && y<10; x++, y++) {   // DOES NOT COMPILE
  System.out.print(x + " ");
}
```
---
Using Loop Variables Outside the Loop
```java
for(long y = 0, x = 4; x < 5 && y < 10; x++, y++) {
  System.out.print(y + " ");
}
System.out.print(x);  // DOES NOT COMPILE
```
--- 

**``for-each`` statement**

Psudo
```java
for(datetype instance : collection) {
    // statement
}
```

The right-hand side of the for-each loop statement must be a built-in Java array or an object whose class implements ``java.lang.Iterable``.

--- 

```java
String names = "Lisa";
for(String name : names) {   // DOES NOT COMPILE
  System.out.print(name + " ");
}
```
The String ``names`` is not an array, nor does it implement ``java.lang.Iterable``, so the compiler will throw an exception since it does not know how to iterate over the String.

--- 
```java
String[] names = new String[3];
for(int name : names) {  // DOES NOT COMPILE
  System.out.print(name + " ");
}
```
This code will fail to compile because the left-hand side of the for-each statement does not define an instance of String.

### ```label```

A label is an optional pointer to the head of a statement that allows the application flow to jump to it or break from it.

```java
int[][] myComplexArray = {{5,2,1,3},{3,9,8,9},{5,7,12,7}};
OUTER_LOOP:  for(int[] mySimpleArray : myComplexArray) {
  INNER_LOOP:  for(int i=0; i<mySimpleArray.length; i++) {
    System.out.print(mySimpleArray[i]+"\t");
  }
    System.out.println();
}
```

### The ```break``` Statement
A ``break`` statement transfers the flow of control out to the enclosing statement.

```java
int[][] list = {{1,13,5},{1,2,5},{2,7,2}};
int searchValue = 2;
int positionX = -1;
int positionY = -1;
PARENT_LOOP: for(int i=0; i<list.length; i++) {
    for(int j=0; j<list[i].length; j++) {
    if(list[i][j]==searchValue) {
        positionX = i;
        positionY = j;
        break PARENT_LOOP;
    }
    }
}
if(positionX==-1 || positionY==-1) {
    System.out.println("Value "+searchValue+" not found");
} else {
    System.out.println("Value "+searchValue+" found at: " + "("+positionX+","+positionY+")");
}
```

When executed, this code will output:
```bash
Value 2 found at: (1,1)
```

### The ``continue`` Statement

The ``continue`` statement transfers control to the boolean expression that determines if the loop should continue.

```java
FIRST_CHAR_LOOP: for (int a = 1; a <= 4; a++) {
    for (char x = 'a'; x <= 'c'; x++) {
        if (a == 2 || x == 'b')
            continue FIRST_CHAR_LOOP;
        System.out.print(" " + a + x);
    }
}
```

The output looks like this:
```bash
1a 3a 4a
```

## Autoboxing and Unboxing

Java automatically converts between primitives and their wrapper classes.

```java
// Autoboxing — primitive → wrapper
Integer a = 5;          // compiler: Integer.valueOf(5)
List<Integer> list = new ArrayList<>();
list.add(10);           // autoboxed to Integer

// Unboxing — wrapper → primitive
int x = a;              // compiler: a.intValue()
int sum = list.get(0) + 1;  // unboxed for arithmetic
```

**Integer cache trap — the most common interview trick:**
```java
Integer a = 127;
Integer b = 127;
System.out.println(a == b);   // true  — cached range: -128 to 127

Integer c = 128;
Integer d = 128;
System.out.println(c == d);   // false — outside cache, new objects
System.out.println(c.equals(d));  // true  — always use equals() for wrappers
```

**NullPointerException from unboxing:**
```java
Integer value = null;
int x = value;   // NullPointerException — unboxing null throws NPE
```

## Common Interview Questions

**Q: What is the difference between `==` and `equals()` for Integer objects?**
A: `==` compares object references. `equals()` compares values. For `Integer` values between -128 and 127, `==` may return `true` due to the Integer cache, but above 127 it returns `false` even for equal values. Always use `equals()` when comparing wrapper types.

**Q: What is short-circuit evaluation? Why does it matter?**
A: With `&&`, if the left side is `false`, the right side is never evaluated. With `||`, if the left side is `true`, the right side is skipped. This matters for null checks: `if (obj != null && obj.getValue() > 0)` is safe because the second part only runs when obj is non-null. The non-short-circuit operators `&` and `|` always evaluate both sides.

**Q: What is numeric promotion and when does it happen?**
A: When `byte`, `short`, or `char` are used in arithmetic, they're promoted to `int` before the operation. This is why `short a = 1; short b = 2; short c = a + b;` does not compile — the sum is an `int`. You must cast: `short c = (short)(a + b)`.

**Q: What's the output of `System.out.println(1 + 2 + "3")`?**
A: `"33"`. Evaluated left to right: `1 + 2 = 3` (integer addition), then `3 + "3" = "33"` (string concatenation). Compare with `"1" + 2 + 3` → `"123"`.

**Q: Can you use a `String` in a switch statement?**
A: Yes, since Java 7. Valid switch types: `int`/`Integer`, `byte`/`Byte`, `short`/`Short`, `char`/`Character`, `String`, and `enum`. Note: `long`, `float`, `double`, and `boolean` are not allowed.