Operations with bits are used in Data compression (data is compressed by converting it from one representation to another, to reduce the space), Exclusive-Or Encryption (an algorithm to encrypt the data for safety issues).

We all know that 1 byte comprises of 8 bits and any integer or character can be represented using bits in computers, which we call its binary form(contains only 1 or 0) or in its base 2 form.
Example:

> 14 = {1110}<sub>2</sub>
= 1 * 2<sup>3</sup> + 1 * 2<sup>2</sup> + 1 * 2<sup>1</sup> + 0 * 2<sup>0</sup>
= 14.

> 20 = {10100}<sub>2</sub>
= 1 * 2<sup>4</sup> + 0 * 2<sup>3</sup> + 1 * 2<sup>2</sup> + 0 * 2<sup>1</sup> + 0 * 2<sup>0</sup>
= 20.

**Bitwise Operators:**

- **NOT ( ~ )** :  Unary operator that flips the bits of the number i.e., if the ith bit is 0, it will change it to 1 and vice versa.

    > N = 5 = (101)<sub>2</sub>  
    > ~N = ~5 = ~(101)<sub>2</sub> = (010)<sub>2</sub> = 2

- **AND ( & )** : Bitwise AND is a binary operator that operates on two equal-length bit patterns. If both bits in the compared position of the bit patterns are 1, the bit in the resulting bit pattern is 1, otherwise 0.
    > A = 5 = (101)<sub>2</sub> , B = 3 = (011)<sub>2</sub>  
    > A & B = (101)<sub>2</sub> & (011)<sub>2</sub> = (001)<sub>2</sub> = 1

- **OR ( | )** : Bitwise OR is also a binary operator that operates on two equal-length bit patterns, similar to bitwise AND. If both bits in the compared position of the bit patterns are 0, the bit in the resulting bit pattern is 0, otherwise 1.
    > A = 5 = (101)<sub>2</sub> , B = 3 = (011)<sub>2</sub>  
    > A | B = (101)<sub>2</sub> | (011)<sub>2</sub> = (111)<sub>2</sub> = 7

- **XOR ( ^ )** : Bitwise XOR also takes two equal-length bit patterns. If both bits in the compared position of the bit patterns are 0 or 1, the bit in the resulting bit pattern is 0, otherwise 1. If bits are same then 0, else 1.
    > A = 5 = (101)<sub>2</sub> , B = 3 = (011)<sub>2</sub>  
    > A ^ B = (101)<sub>2</sub> ^ (011)<sub>2</sub> = (110)<sub>2</sub> = 6

- **Left Shift ( << )** : Left shift operator is a binary operator which shift the some number of bits, in the given bit pattern, to the left and append 0 at the end. Left shift is equivalent to multiplying the bit pattern with 2<sup>k</sup> ( if we are shifting k bits ).
    > 1 << 1 = 2 = 2<sup>1</sup>  
    > 1 << 2 = 4 = 2<sup>2</sup>  
    > 1 << 3 = 8 = 2<sup>3</sup>  
    > 1 << 4 = 16 = 2<sup>4</sup>  
    > …  
    > 1 << n = 2<sup>n</sup>

- **Right Shift ( >> )** : Right shift operator is a binary operator which shift the some number of bits, in the given bit pattern, to the right and append 1 at the end. Right shift is equivalent to dividing the bit pattern with 2<sup>k</sup> ( if we are shifting k bits ).
    > 4 >> 1 = 2  
    > 6 >> 1 = 3  
    > 5 >> 1 = 2  
    > 16 >> 4 = 1

<hr>

**- If all bits are 1 in N then (N+1) is power of 2.**  
**- Xor (^) of n with itself is 0. i.e, N^N == 0**  
**- The binary representation of (x-1) will have all the bits same as x, except for the rightmost 1 in x and all the bits to the right of the rightmost 1.**  
**- x & (x-1) will have all the bits equal to the x except for the rightmost 1 in x.**  
**- x ^ ( x & (x-1)) : Returns the rightmost 1 in binary representation of x.**

<hr>

### How to check if a given number is a power of 2?
```java
boolean isPowerOfTwo(int x) {
    if(x == 0)
        return false;
    else {
        while(x % 2 == 0) x /= 2;
        return (x == 1);
    }    
}
```
The problem can be solved using bit manipulation. Consider a number x that we need to check for being a power for 2. Now think about the binary representation of (x-1). (x-1) will have all the bits same as x, except for the rightmost 1 in x and all the bits to the right of the rightmost 1.  
Let, x = 4 = (100)<sub>2</sub>  
x - 1 = 3 = (011)<sub>2</sub>  
Let, x = 6 = (110)<sub>2</sub>  
x - 1 = 5 = (101)<sub>2</sub>

It might not seem obvious with these examples, but binary representation of (x-1) can be obtained by simply flipping all the bits to the right of rightmost 1 in x and also including the rightmost 1.

Now think about x & (x-1). x & (x-1) will have all the bits equal to the x except for the rightmost 1 in x.  
Let, x = 4 = (100)<sub>2</sub>  
x - 1 = 3 = (011)<sub>2</sub>  
x & (x-1) = 4 & 3 = (100)<sub>2</sub> & (011)<sub>2</sub> = (000)<sub>2</sub>  
Let, x = 6 = (110)<sub>2</sub>  
x - 1 = 5 = (101)<sub>2</sub>  
x & (x-1) = 6 & 5 = (110)<sub>2</sub> & (101)<sub>2</sub> = (100)<sub>2</sub>

Properties for numbers which are powers of 2, is that they have one and only one bit set in their binary representation. If the number is neither zero nor a power of two, it will have 1 in more than one place. So if x is a power of 2 then x & (x-1) will be 0.

```java
boolean isPowerOfTwo(int x) {
    // x will check if x == 0 and !(x & (x - 1)) will check if x is a power of 2 or not
    return (x != 0 & (x & (x - 1)) == 0) ? true : false;
}
```

<hr>

### Count the number of ones in the binary representation of the given number.

The basic approach to evaluate the binary form of a number is to traverse on it and count the number of ones. But this approach takes log<sub>2</sub>N of time in every case.

Why log<sub>2</sub>N ?
As to get a number in its binary form, we have to divide it by 2, until it gets 0, which will take log<sub>2</sub>N of time.

With bitwise operations, we can use an algorithm whose running time depends on the number of ones present in the binary form of the given number. This algorithm is much better, as it will reach to logN, only in its worst case.

```java
int countOneBits(int n) {
    int count = 0;
    while(n!=0) {
        n = n&(n-1);
        count++;
    }
    return count;
}
```

**Why this algorithm works?**  
As explained in the previous algorithm, the relationship between the bits of x and x-1. So as in x-1, the rightmost 1 and bits right to it are flipped, then by performing x&(x-1), and storing it in x, will reduce x to a number containing number of ones(in its binary form) less than the previous state of x, thus increasing the value of count in each iteration.

Example:
n = 23 = {10111}<sub>2</sub> .
1. Initially, count = 0.
2. Now, n will change to n&(n-1). As n-1 = 22 = {10110}<sub>2</sub> , then n&(n-1) will be {10111}<sub>2</sub> & {10110}<sub>2</sub>, which will be {10110}<sub>2</sub> which is equal to 22. Therefore n will change to 22 and count to 1.
3. As n-1 = 21 = {10101}<sub>2</sub> , then n&(n-1) will be {10110}2 & {10101}<sub>2</sub>, which will be {10100}<sub>2</sub> which is equal to 20. Therefore n will change to 20 and count to 2.
4. As n-1 = 19 = {10011}<sub>2</sub>, then n&(n-1) will be {10100}<sub>2</sub> & {10011}<sub>2</sub>, which will be {10000}<sub>2</sub> which is equal to 16. Therefore n will change to 16 and count to 3.
5. As n-1 = 15 = {01111}<sub>2</sub>, then n&(n-1) will be {10000}<sub>2</sub> & {01111}<sub>2</sub>, which will be {00000}<sub>2</sub> which is equal to 0. Therefore n will change to 0 and count to 4.
6. As n = 0, the the loop will terminate and gives the result as 4.

Complexity: O(K), where K is the number of ones present in the binary form of the given number.

```java
int countOneBits(int n) {
    int limit = (int) Math.log(n)/Math.log(2);
    for(int i=0; i<=limit; i++) {
        if((n & (1<<i)) != 0) count++;
    }

    return count;
}
```

<hr>

### Check if the ith bit is set in the binary form of the given number.

To check if the i<sup>th</sup> bit is set or not (1 or not), we can use AND operator. How?

Let’s say we have a number N, and to check whether it’s ith bit is set or not, we can AND it with the number 2<sup>i</sup> . The binary form of 2<sup>i</sup> contains only ith bit as set (or 1), else every bit is 0 there. When we will AND it with N, and if the ith bit of N is set, then it will return a non zero number (2<sup>i</sup> to be specific), else 0 will be returned.

Using Left shift operator, we can write 2i as 1 << i . Therefore:

```java
boolean check(int n) {
    if((n & (1 << i))!= 0)
        return true;
    else
        return false;
}
```

*Example*:  
Let’s say N = 20 = {10100}<sub>2</sub>. Now let’s check if it’s 2nd bit is set or not(starting from 0). For that, we have to AND it with 22 = 1<<2 = {100}<sub>2</sub>.
{10100} & {100} = {100}<sub>2</sub> = 22 = 4 (non-zero number), which means it’s 2nd bit is set.

<hr>

### How to generate all the possible subsets of a set?
A big advantage of bit manipulation is that it can help to iterate over all the subsets of an N-element set. As we all know there are 2N possible subsets of any given set with N elements. What if we represent each element in a subset with a bit. A bit can be either 0 or 1, thus we can use this to denote whether the corresponding element belongs to this given subset or not. So each bit pattern will represent a subset.

Consider a set A of 3 elements.
A = {a, b, c}

Now, we need 3 bits, one bit for each element. 1 represent that the corresponding element is present in the subset, whereas 0 represent the corresponding element is not in the subset. Let’s write all the possible combination of these 3 bits.

0 = (000)<sub>2</sub> = {}  
1 = (001)<sub>2</sub> = {c}  
2 = (010)<sub>2</sub> = {b}  
3 = (011)<sub>2</sub> = {b, c}  
4 = (100)<sub>2</sub> = {a}  
5 = (101)<sub>2</sub> = {a, c}  
6 = (110)<sub>2</sub> = {a, b}  
7 = (111)<sub>2</sub> = {a, b, c}  

Pseudo Code:
```
possibleSubsets(A, N):
    for i = 0 to 2^N:
        for j = 0 to N:
            if jth bit is set in i:
                print A[j]
        print ‘\n’
```
Implementation:

```java
int n = 3;
char[] c = {'a', 'b', 'c'};

for(int i=0; i<(1<<n); i++) {
    for(int j=0; j<n; j++) {
        if((i & (1 << j)) == 0)
            System.out.print(c[j] + " ");
    }
    System.out.println();
}
```

## Tricks with Bits:

1) **x ^ ( x & (x-1)) : Returns the rightmost 1 in binary representation of x.**

    As explained above, (x & (x - 1)) will have all the bits equal to the x except for the rightmost 1 in x. So if we do bitwise XOR of x and (x & (x-1)), it will simply return the rightmost 1. Let’s see an example.
    x = 10 = (1010)<sub>2</sub> ` x & (x-1) = (1010)<sub>2</sub> & (1001)<sub>2</sub> = (1000)<sub>2</sub>
    x ^ (x & (x-1)) = (1010)<sub>2</sub> ^ (1000)<sub>2</sub> = (0010)<sub>2</sub>

2) **x & (-x) : Returns the rightmost 1 in binary representation of x**

    (-x) is the two’s complement of x. (-x) will be equal to one’s complement of x plus 1.
    Therefore (-x) will have all the bits flipped that are on the left of the rightmost 1 in x. So x & (-x) will return rightmost 1.

    x = 10 = (1010)<sub>2</sub>
    (-x) = -10 = (0110)<sub>2</sub>
    x & (-x) = (1010)<sub>2</sub> & (0110)<sub>2</sub> = (0010)<sub>2</sub>

3) **x | (1 << n) : Returns the number x with the nth bit set.**

    (1 << n) will return a number with only nth bit set. So if we OR it with x it will set the nth bit of x.
    x = 10 = (1010)<sub>2</sub> n = 2
    1 << n = (0100)<sub>2</sub>
    x | (1 << n) = (1010)<sub>2</sub> | (0100)<sub>2</sub> = (1110)<sub>2</sub>

### Applications of bit operations:

1) They are widely used in areas of graphics, specially XOR(Exclusive OR) operations.
2) They are widely used in the embedded systems, in situations, where we need to set/clear/toggle just one single bit of a specific register without modifying the other contents. We can do OR/AND/XOR operations with the appropriate mask for the bit position.
3) Data structure like n-bit map can be used to allocate n-size resource pool to represent the current status.
4) Bits are used in networking, framing the packets of numerous bits which is sent to another system generally through any type of serial interface.

## References:
- https://www.hackerearth.com/practice/basic-programming/bit-manipulation/basics-of-bit-manipulation/tutorial/