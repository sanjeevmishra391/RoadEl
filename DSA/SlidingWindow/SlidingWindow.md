# Sliding Window — Problems

> See pattern notes: `DSA/Algorithms/SlidingWindow.md`

## Problem List

| # | Problem | Type | Difficulty |
|---|---|---|---|
| 1 | Longest Substring Without Repeating Characters | Variable | Medium |
| 2 | Minimum Window Substring | Variable | Hard |
| 3 | Longest Substring with At Most K Distinct Characters | Variable | Medium |
| 4 | Fruits Into Baskets | Variable | Medium |
| 5 | Max Consecutive Ones III | Variable | Medium |
| 6 | Sliding Window Maximum | Fixed | Hard |
| 7 | Permutation in String | Fixed | Medium |

---

## Problem 1: Longest Substring Without Repeating Characters
```
Input:  s = "abcabcbb"
Output: 3  ("abc")

Pattern: Variable sliding window
Window condition: all characters unique
Shrink when: duplicate found
Track: max window size
```
```java
// Time: O(n)  Space: O(min(n, 26)) for charset
public int lengthOfLongestSubstring(String s) {
    Set<Character> window = new HashSet<>();
    int left = 0, maxLen = 0;
    for (int right = 0; right < s.length(); right++) {
        while (window.contains(s.charAt(right)))
            window.remove(s.charAt(left++));
        window.add(s.charAt(right));
        maxLen = Math.max(maxLen, right - left + 1);
    }
    return maxLen;
}
```

---

## Problem 2: Minimum Window Substring
```
Input:  s = "ADOBECODEBANC", t = "ABC"
Output: "BANC"

Pattern: Variable sliding window
Window condition: contains all chars of t
Shrink: when window is valid, try to shrink from left
Track: minimum valid window (start index + length)

Key: use 'have' and 'need' counters to avoid scanning whole map each step
```
```java
// Time: O(n + m)  Space: O(n + m)
public String minWindow(String s, String t) {
    if (s.isEmpty() || t.isEmpty()) return "";
    Map<Character, Integer> need = new HashMap<>();
    for (char c : t.toCharArray()) need.merge(c, 1, Integer::sum);

    Map<Character, Integer> have = new HashMap<>();
    int formed = 0, required = need.size();
    int left = 0, minLen = Integer.MAX_VALUE, minStart = 0;

    for (int right = 0; right < s.length(); right++) {
        char c = s.charAt(right);
        have.merge(c, 1, Integer::sum);
        if (need.containsKey(c) && have.get(c).equals(need.get(c))) formed++;

        while (formed == required) {
            if (right - left + 1 < minLen) { minLen = right - left + 1; minStart = left; }
            char lc = s.charAt(left++);
            have.merge(lc, -1, Integer::sum);
            if (need.containsKey(lc) && have.get(lc) < need.get(lc)) formed--;
        }
    }
    return minLen == Integer.MAX_VALUE ? "" : s.substring(minStart, minStart + minLen);
}
```

---

## Problem 3: Longest Substring with At Most K Distinct Characters
```
Input:  s = "eceba", k = 2
Output: 3  ("ece")

Pattern: Variable sliding window
Window condition: distinct chars <= k
Shrink: when distinct > k
```
```java
// Time: O(n)  Space: O(k)
public int lengthOfLongestSubstringKDistinct(String s, int k) {
    Map<Character, Integer> freq = new HashMap<>();
    int left = 0, maxLen = 0;
    for (int right = 0; right < s.length(); right++) {
        freq.merge(s.charAt(right), 1, Integer::sum);
        while (freq.size() > k) {
            char lc = s.charAt(left++);
            freq.merge(lc, -1, Integer::sum);
            if (freq.get(lc) == 0) freq.remove(lc);
        }
        maxLen = Math.max(maxLen, right - left + 1);
    }
    return maxLen;
}
```

---

## Problem 4: Fruits Into Baskets
```
Input:  fruits = [1,2,1,2,3]
Output: 4  (pick [1,2,1,2] — at most 2 types)

Pattern: Variable sliding window
Key insight: EXACTLY the same as "longest subarray with at most 2 distinct values"
Window condition: distinct fruit types <= 2
```
```java
// Time: O(n)  Space: O(1) — at most 2 keys
public int totalFruit(int[] fruits) {
    Map<Integer, Integer> basket = new HashMap<>();
    int left = 0, maxPicked = 0;
    for (int right = 0; right < fruits.length; right++) {
        basket.merge(fruits[right], 1, Integer::sum);
        while (basket.size() > 2) {
            int lf = fruits[left++];
            basket.merge(lf, -1, Integer::sum);
            if (basket.get(lf) == 0) basket.remove(lf);
        }
        maxPicked = Math.max(maxPicked, right - left + 1);
    }
    return maxPicked;
}
```

---

## Problem 5: Max Consecutive Ones III
```
Input:  nums = [1,1,1,0,0,0,1,1,1,1,0], k = 2
Output: 6  (flip positions 5 and 10)

Pattern: Variable sliding window
Window condition: at most k zeros in window
Shrink: when zero count > k
Key insight: "flip k zeros" = "window with at most k zeros"
```
```java
// Time: O(n)  Space: O(1)
public int longestOnes(int[] nums, int k) {
    int left = 0, zeros = 0, maxLen = 0;
    for (int right = 0; right < nums.length; right++) {
        if (nums[right] == 0) zeros++;
        while (zeros > k) if (nums[left++] == 0) zeros--;
        maxLen = Math.max(maxLen, right - left + 1);
    }
    return maxLen;
}
```

---

## Problem 6: Sliding Window Maximum
```
Input:  nums = [1,3,-1,-3,5,3,6,7], k = 3
Output: [3,3,5,5,6,7]

Pattern: Fixed sliding window + Monotonic Deque
Key insight: maintain a deque of indices in DECREASING order of values
  - front of deque = index of max in current window
  - remove indices that are out of window (too old)
  - remove values from back that are smaller than current (they'll never be max)
```
```java
// Time: O(n)  Space: O(k)
public int[] maxSlidingWindow(int[] nums, int k) {
    int n = nums.length;
    int[] result = new int[n - k + 1];
    Deque<Integer> deque = new ArrayDeque<>(); // stores indices

    for (int i = 0; i < n; i++) {
        // remove indices outside window
        while (!deque.isEmpty() && deque.peekFirst() < i - k + 1)
            deque.pollFirst();
        // remove smaller values from back (they'll never be the max)
        while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i])
            deque.pollLast();
        deque.offerLast(i);
        // window is full
        if (i >= k - 1) result[i - k + 1] = nums[deque.peekFirst()];
    }
    return result;
}
```

---

## Problem 7: Permutation in String
```
Input:  s1 = "ab", s2 = "eidbaooo"
Output: true  ("ba" is permutation of "ab")

Pattern: Fixed sliding window (size = s1.length)
Key: permutation = same char frequencies
Use int[26] frequency arrays, compare them
```
```java
// Time: O(n)  Space: O(1) — fixed size arrays
public boolean checkInclusion(String s1, String s2) {
    if (s1.length() > s2.length()) return false;
    int[] need = new int[26], have = new int[26];
    for (char c : s1.toCharArray()) need[c - 'a']++;
    int k = s1.length();

    for (int i = 0; i < s2.length(); i++) {
        have[s2.charAt(i) - 'a']++;
        if (i >= k) have[s2.charAt(i - k) - 'a']--;
        if (Arrays.equals(have, need)) return true;
    }
    return false;
}
```

---

## Pattern Debrief — Sliding Window

All 7 problems follow one of two templates:

**Fixed window:** build first window, then slide (add right, remove left)
**Variable window:** expand right always, shrink left when condition violated

The **window condition** is the only thing that changes:
- Unique chars only → HashSet
- At most K distinct → HashMap with size check
- At most K zeros → count zeros
- All chars of t present → `have == required` counter
- Maximum in window → Monotonic Deque
