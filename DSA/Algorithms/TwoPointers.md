# Two Pointers

## Pattern Recognition Triggers
Use Two Pointers when you see:
- **Sorted array** + pair/triplet with target sum
- "Remove duplicates in-place"
- "Reverse / palindrome check"
- Two sequences being compared simultaneously (merge, compare)

## Complexity
- Time: **O(n)** — each pointer moves at most n steps
- Space: **O(1)** — no extra data structures

## Three Sub-Patterns

### 1. Opposite Ends → Move to Center
Both pointers start at ends, converge inward.
```
Use when: sorted array, find pair summing to target
left = 0, right = n-1
while left < right:
    sum = arr[left] + arr[right]
    if sum == target: found
    if sum < target:  left++
    if sum > target:  right--
```

### 2. Same Direction (Fast & Slow)
Both pointers start at left, one moves faster.
```
Use when: remove duplicates, move zeros, partition
slow = 0
for fast in 0..n:
    if condition on arr[fast]:
        arr[slow] = arr[fast]
        slow++
```

### 3. Two Separate Arrays
One pointer per array, advance the smaller one.
```
Use when: merge two sorted arrays, compare sequences
i = 0, j = 0
while i < n and j < m:
    process arr1[i] vs arr2[j]
    advance whichever is smaller
```

> Full solutions with code → [`DSA/TwoPointer/TwoPointers.md`](../TwoPointer/TwoPointers.md)

---

### Patterns

1. Running from both ends of an array

    The first type of problems are, having two pointers at left and right end of array, then moving them to the center while processing something with them.

- 2 Sum problem  
    - [2Sum Sorted Array](https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/)
    - [3Sum](https://leetcode.com/problems/3sum/)
    - [4Sum](https://leetcode.com/problems/4sum/)
    - [Number of Subsequences That Satisfy the Given Sum Condition](https://leetcode.com/problems/number-of-subsequences-that-satisfy-the-given-sum-condition/)
    - [Two Sum IV - Input is a BST](https://leetcode.com/problems/two-sum-iv-input-is-a-bst/)
    - [Sum of Square Numbers](https://leetcode.com/problems/sum-of-square-numbers/)
    - [Boats to Save People](https://leetcode.com/problems/boats-to-save-people/)
    - [Minimize Maximum Pair Sum in Array](https://leetcode.com/problems/minimize-maximum-pair-sum-in-array/)
    - [3Sum With Multiplicity](https://leetcode.com/problems/3sum-with-multiplicity/)