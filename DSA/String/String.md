## Pattern Searching

### 1. Naive Pattern Searching algorithm
Naive pattern searching is the simplest method among other pattern-searching algorithms. It checks for all characters of the main string to the pattern. This algorithm is helpful for smaller texts. It does not need any pre-processing phases. We can find the substring by checking once for the string. It also does not occupy extra space to perform the operation.

[Code](./PatternSearching/NaivePatternSearching.java)

Time Complexity: O(N*M)  
Auxiliary Space: O(1)

### 2. Knuth-Morris-Pratt (KMP) algorithm

KMP uses the structure of the pattern to avoid redundant comparisons. It preprocesses the pattern string and creates an array called the Longest Prefix Suffix (lps) array which indicates how much of the pattern can be reused after a mismatch.

**LPS Array**
- LPS is the Longest Proper Prefix which is also a Suffix. A proper prefix is a prefix that doesn’t include whole string. For example, prefixes of "abc" are "" , "a", "ab" and "abc" but proper prefixes are "" , "a" and "ab" only. Suffixes of the string are "" , "c", "bc", and "abc".
- Each value, ```lps[i]``` is the length of longest proper prefix of ```pat[0..i]``` which is also a suffix of ```pat[0..i]```.

```
pat [A B A B C A B A B]
idx [0 1 2 3 4 5 6 7 8]
lps [0 0 1 2 0 1 2 3 4]

create lps arr;
len = 0
lps[0] = 0 as no proper prefix exists for a single character string.

i=1, len=0  
    pat[i] != pat[len] and len == 0
    lps[i] = 0
    i = 1
    [0 0]  
i=2, len=0  
    pat[i] == pat[len] (pat[2] == pat[0])
    len = 1
    lps[2] = 1 (len)
    i = 3
    [0 0 1]  
i=3, len=1  
    pat[i] == pat[len] (pat[3] == pat[1])
    len = 2
    lps[3] = 2
    i = 4
    [0 0 1 2]  
i=4, len=2  
    pat[i] != pat[len] (pat[4] != pat[2]) and len != 0
    len = lps[len-1] = 0
i=4, len=0  
    pat[i] != pat[len] (pat[4] != pat[0]) and len == 0
    lps[i] = 0
    i = 5  
i=5, len=0  
i=6, len=1
i=7, len=2
i=8, len=3
i=9, len=4
[0, 0, 1, 2, 0, 1, 2, 3, 4]
```

[Code](./PatternSearching/KMP.java)

Time Complexity: O(n + m), where n is the length of the text and m is the length of the pattern. This is because creating the LPS (Longest Prefix Suffix) array takes O(m) time, and the search through the text takes O(n) time.

Auxiliary Space: O(m), as we need to store the LPS array of size m.

### 3. Rabin-Karp Algorithm
Rabin-Karp algorithm is an algorithm used for searching/matching patterns in the text using a hash function. Unlike Naive string matching algorithm, it does not travel through every character in the initial phase rather it filters the characters that do not match and then performs the comparison.

> A sequence of characters is taken and checked for the possibility of the presence of the required string. If the possibility is found then, character matching is performed.

**Limitations of Rabin-Karp Algorithm**  
*Spurious Hit*  
When the hash value of the pattern matches with the hash value of a window of the text but the window is not the actual pattern then it is called a spurious hit.

Spurious hit increases the time complexity of the algorithm. In order to minimize spurious hit, we use modulus. It greatly reduces the spurious hit.

[Code](./PatternSearching/RabinKarp.java)

**Rabin-Karp Algorithm Complexity**  
The average case and best case complexity of Rabin-Karp algorithm is O(m + n) and the worst case complexity is O(mn).  
The worst-case complexity occurs when spurious hits occur a number for all the windows.


