# String

```java
String name = "Fluffy";
String name = new String("Fluffy");
```

### String Concatenation
Java combines the two String objects.  
``"1" + "2"`` is ``"12"``

Expression  is evaluated left to right.
```java
System.out.println(1 + 2);           // 3
System.out.println("a" + "b");       // ab
System.out.println("a" + "b" + 3);   // ab3
System.out.println(1 + 2 + "c");     // 3c
```

### Immutability
Once a String object is created, it is not allowed to change. It cannot be made larger or smaller, and you cannot change one of the characters inside it.

### String Pool
The string pool, also known as the intern pool, is a location in the Java virtual machine (JVM) that collects all the strings in program.

The string pool contains literal values that appear in your program. For example, ``"name"`` is a literal and therefore goes into the string pool. ``myObject.toString()`` is a string but not a literal, so it does not go into the string pool. Strings not in the string pool are garbage collected just like any other object.

```java
String name = "Fluffy"; // uses string pool
String name = new String("Fluffy"); // does not use string pool. creates a new object
```

### Important String Methods

- ``int length()`` - gets the length of string
- ``char charAt(int index)`` - gets the character at the specified index.
- ``int indexOf(char ch)`` - ooks at the characters in the string and finds the first index that matches the desired value. Returns index if found else returns -1. Other overloaded methods are : ``int indexOf(char ch, index fromIndex)``, ``int indexOf(String str)``, ``int indexOf(String str, index fromIndex)``
- ``int substring(int beginIndex)`` -  The first parameter is the index to start with for the returned string. ``int substring(int beginIndex, int endIndex)`` : endIndex is not included.
    ```java
    // string is of length 8 with index from 0 to 7.
    System.out.println(string.substring(3, 3)); // empty string
    System.out.println(string.substring(3, 2));  // throws exception
    System.out.println(string.substring(3, 8)); // throws exception
    ```
- ``String toLowerCase(String str)`` and ``String toUpperCase(String str)`` - convert all the characters into specified case.
- ``boolean equals(String str)`` and ``boolean equalsIgnoreCase(String str)`` - checks whether two String objects contain exactly the same characters in the same order.
- ``boolean startsWith(String prefix)`` and ``boolean endsWith(String suffix)`` - methods look at whether the provided value matches part of the String.
- ``boolean contains(String str)`` - the match can be anywhere in the String.
- ``String replace(char oldChar, char newChar)`` and ``String replace(CharSequence oldChar, CharSequence newChar)`` - method does a simple search and replace on the string.
- ``public String trim()`` - it removes whitespace from the beginning and end of a String.

## ``StringBuilder`` Class

```java
StringBuilder alpha = new StringBuilder();
for(char current = 'a'; current <= 'z'; current++)
    alpha.append(current);
System.out.println(alpha);
```

``charAt()``, ``indexOf()``, ``length()``, and ``substring()``
```java
StringBuilder sb = new StringBuilder("animals");
String sub = sb.substring(sb.indexOf("a"), sb.indexOf("al"));
int len = sb.length();
char ch = sb.charAt(6);
System.out.println(sub + " " + len + " " + ch);
```

- ``StringBuilder append(String str)`` - adds to the end to string. append() is called directly after the constructor.
- ``StringBuilder insert(int offset, String str)`` - method adds characters to the StringBuilder at the requested index and returns a reference to the current StringBuilder
- ``StringBuilder delete(int start, int end)`` and ``StringBuilder deleteCharAt(int index)`` - end is not inclusive.
- ``StringBuilder reverse()``
- ``String toString()``

### ``StringBuilder`` vs ``StringBuffer``
``StringBuffer`` is slower than ``StringBuilder`` as it is thread safe.

## Understanding Equality

```java
StringBuilder one = new StringBuilder();
StringBuilder two = new StringBuilder();
StringBuilder three = one.append("a");
System.out.println(one == two); // false
System.out.println(one == three); // true
```

- ``==`` checks for reference equality. ``equals()`` checks for value equality.
- When string is built without new keyword then string is shared on string pool. So ``==`` will true if same value.
    ```java
    String x = new String("Hello World");
    String y = "Hello World";
    System.out.println(x == y); // false
    ```
- String comparison is done using compile time values.
    ```java
    String x = "Hello World";
    String z = " Hello World".trim();
    System.out.println(x == z); // false
    ```
- If ``equals()`` is not implemented then it returns ``false``.
    ```java
    public class Tiger {
       String name;
       public static void main(String[] args) {
         Tiger t1 = new Tiger();
         Tiger t2 = new Tiger();
         Tiger t3 = t1;
         System.out.println(t1 == t1); // true
         System.out.println(t1 == t2); // false
         System.out.println(t1.equals(t2)); // false
      } 
    }
    ```