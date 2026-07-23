# Sliding Window

## Pattern Recognition Triggers
Use Sliding Window when you see:
- "subarray / substring of size k"
- "longest / shortest subarray satisfying a condition"
- "maximum / minimum sum in a window"
- Input is a string or array, and you need a **contiguous** segment

## Two Types

### 1. Fixed-Size Window
Window size `k` is given. Slide one step at a time.
```
Use when: "find max sum of subarray of size k"
Template:
  - Build first window of size k
  - Slide: add nums[right], remove nums[right - k]
  - Track result at each step
```

### 2. Variable-Size Window (Expand/Shrink)
Window size changes based on a condition.
```
Use when: "longest substring with at most k distinct chars"
Template:
  left = 0
  for right in 0..n:
      add arr[right] to window
      while window violates condition:
          remove arr[left] from window
          left++
      update result (right - left + 1)
```

## Complexity
- Time: **O(n)** — each element enters and exits the window at most once
- Space: **O(k)** or **O(1)** depending on what you track in the window

> Full solutions with code → [`DSA/SlidingWindow/SlidingWindow.md`](../SlidingWindow/SlidingWindow.md)

### Resources
https://leetcode.com/discuss/interview-question/3722472/mastering-sliding-window-technique-a-comprehensive-guide
https://leetcode.com/discuss/study-guide/1688903/Solved-all-two-pointers-problems-in-100-days