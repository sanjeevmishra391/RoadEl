# Searching

> Binary Search templates and 9 problems with full solutions → [`DSA/BinarySearch/BinarySearch.md`](../BinarySearch/BinarySearch.md)

---

## Searching Algorithms Overview

| Algorithm | Best | Average | Worst | Space | Requires |
|---|---|---|---|---|---|
| Linear Search | O(1) | O(n) | O(n) | O(1) | Nothing |
| Binary Search | O(1) | O(log n) | O(log n) | O(1) | Sorted array |
| Jump Search | O(1) | O(√n) | O(√n) | O(1) | Sorted array |
| Interpolation Search | O(1) | O(log log n) | O(n) | O(1) | Sorted + uniform dist |
| Exponential Search | O(1) | O(log n) | O(log n) | O(1) | Sorted (unbounded) |
| Binary Search on Answer | — | O(n log range) | O(n log range) | O(1) | Monotonic feasibility |

---

## 1. Linear Search

Scan every element one by one. Only option for unsorted data.

```java
// Time: O(n)  Space: O(1)
int linearSearch(int[] arr, int target) {
    for (int i = 0; i < arr.length; i++)
        if (arr[i] == target) return i;
    return -1;
}
```

**When to use:** Unsorted data, very small arrays, or when you only search once (not worth sorting first).

---

## 2. Binary Search [Code](./BinarySearch.java)

Halves the search space each step. Requires a **sorted** array.

```java
// Time: O(log n)  Space: O(1)
int binarySearch(int[] arr, int target) {
    int low = 0, high = arr.length - 1;
    while (low <= high) {
        int mid = low + (high - low) / 2;   // avoids overflow
        if      (arr[mid] == target) return mid;
        else if (arr[mid] < target)  low = mid + 1;
        else                         high = mid - 1;
    }
    return -1;
}
```

### Finding First and Last Occurrence

```java
// First occurrence of target
int firstOccurrence(int[] arr, int target) {
    int low = 0, high = arr.length - 1, result = -1;
    while (low <= high) {
        int mid = low + (high - low) / 2;
        if (arr[mid] == target) { result = mid; high = mid - 1; } // keep going left
        else if (arr[mid] < target) low = mid + 1;
        else high = mid - 1;
    }
    return result;
}

// Last occurrence of target
int lastOccurrence(int[] arr, int target) {
    int low = 0, high = arr.length - 1, result = -1;
    while (low <= high) {
        int mid = low + (high - low) / 2;
        if (arr[mid] == target) { result = mid; low = mid + 1; }  // keep going right
        else if (arr[mid] < target) low = mid + 1;
        else high = mid - 1;
    }
    return result;
}
```

### Count of Occurrences

```java
int countOccurrences(int[] arr, int target) {
    int first = firstOccurrence(arr, target);
    if (first == -1) return 0;
    return lastOccurrence(arr, target) - first + 1;
}
```

---

## 3. Jump Search

Jump in blocks of size √n, then do linear search within the block. Good when backward traversal is costly (e.g., magnetic tape).

```java
// Time: O(√n)  Space: O(1)
int jumpSearch(int[] arr, int target) {
    int n = arr.length;
    int step = (int) Math.sqrt(n);
    int prev = 0;
    // Jump forward until block may contain target
    while (arr[Math.min(step, n) - 1] < target) {
        prev = step;
        step += (int) Math.sqrt(n);
        if (prev >= n) return -1;
    }
    // Linear search within the block
    while (arr[prev] < target) {
        prev++;
        if (prev == Math.min(step, n)) return -1;
    }
    return (arr[prev] == target) ? prev : -1;
}
```

---

## 4. Binary Search on Answer Space

The most important searching technique for interviews at 3 YOE level. Instead of searching in an array, you binary search on the **answer itself**.

### Template

```java
// "Find minimum X such that feasible(X) is true"
int binarySearchOnAnswer(int[] input) {
    int low = minPossibleAnswer;
    int high = maxPossibleAnswer;
    while (low < high) {
        int mid = low + (high - low) / 2;
        if (feasible(mid, input)) high = mid;     // mid could be the answer
        else                      low = mid + 1;  // too small
    }
    return low;
}

boolean feasible(int candidate, int[] input) {
    // Can we achieve the goal with this candidate value?
    // O(n) check — this is where the real work happens
}
```

### When to Recognise This Pattern

Ask: **"Can I binary search on the answer instead of the array?"**

Signs:
- "Find **minimum** capacity / speed / days / value such that ..."
- "Find **maximum** value such that constraint is satisfied"
- The answer space is a range of integers
- For a given candidate value, you can check feasibility in O(n)
- As the candidate increases, feasibility only flips **once** (monotonic)

### Problems in This Repo

| Problem | Search Space | Feasibility Check | File |
|---|---|---|---|
| Capacity to Ship Packages in D Days | [max(weight), sum(weights)] | Can we ship all in ≤ days? | [CapacityToShipPackagesWithinDDays.java](./CapacityToShipPackagesWithinDDays.java) |
| Painter Partition | [max(board), sum(boards)] | Can k painters paint in ≤ time? | [PainterPartition.java](./PainterPartition.java) |
| Koko Eating Bananas | [1, max(piles)] | Can she eat all in ≤ h hours? | [BinarySearch/BinarySearch.md](../BinarySearch/BinarySearch.md) |
| Minimum Days to Make Bouquets | [1, max(bloomDay)] | Can we make m bouquets? | [BinarySearch/BinarySearch.md](../BinarySearch/BinarySearch.md) |
| Split Array Largest Sum | [max(nums), sum(nums)] | Can we split into ≤ k parts? | [BinarySearch/BinarySearch.md](../BinarySearch/BinarySearch.md) |

---

## 5. Searching in Rotated Sorted Array

A sorted array that has been rotated at an unknown pivot. Binary search still works — one half is always sorted.

```java
// Time: O(log n)  Space: O(1)
// Key insight: one of the two halves is always sorted after any split
int searchRotated(int[] nums, int target) {
    int low = 0, high = nums.length - 1;
    while (low <= high) {
        int mid = low + (high - low) / 2;
        if (nums[mid] == target) return mid;
        // Left half is sorted
        if (nums[low] <= nums[mid]) {
            if (target >= nums[low] && target < nums[mid]) high = mid - 1;
            else low = mid + 1;
        }
        // Right half is sorted
        else {
            if (target > nums[mid] && target <= nums[high]) low = mid + 1;
            else high = mid - 1;
        }
    }
    return -1;
}
```

---

## 6. Search in 2D Matrix

Two variants — both solved with binary search:

### Variant 1: Each row sorted, first element of row > last of previous row
```java
// Treat as 1D sorted array of m*n elements
// Time: O(log(m*n))  Space: O(1)
boolean searchMatrix(int[][] matrix, int target) {
    int m = matrix.length, n = matrix[0].length;
    int low = 0, high = m * n - 1;
    while (low <= high) {
        int mid = low + (high - low) / 2;
        int val = matrix[mid / n][mid % n];  // convert 1D index to 2D
        if      (val == target) return true;
        else if (val < target)  low = mid + 1;
        else                    high = mid - 1;
    }
    return false;
}
```

### Variant 2: Each row and column sorted independently
```java
// Staircase search: start at top-right, eliminate row or column each step
// Time: O(m + n)  Space: O(1)
boolean searchMatrixII(int[][] matrix, int target) {
    int row = 0, col = matrix[0].length - 1;
    while (row < matrix.length && col >= 0) {
        if      (matrix[row][col] == target) return true;
        else if (matrix[row][col] > target)  col--;   // too big → go left
        else                                 row++;   // too small → go down
    }
    return false;
}
```

---

## Key Interview Questions

| Problem | Algorithm | Time |
|---|---|---|
| Search in sorted array | Binary Search | O(log n) |
| First / last occurrence | Binary Search (boundary) | O(log n) |
| Count occurrences | Binary Search × 2 | O(log n) |
| Search in rotated sorted array | Modified Binary Search | O(log n) |
| Find minimum in rotated sorted array | Modified Binary Search | O(log n) |
| Search 2D matrix (fully sorted) | 1D Binary Search | O(log mn) |
| Search 2D matrix (row+col sorted) | Staircase Search | O(m + n) |
| Kth largest element | Quick Select | O(n) avg |
| Koko Eating Bananas | Binary Search on Answer | O(n log max) |
| Capacity to Ship Packages | Binary Search on Answer | O(n log sum) |

---

## Common Mistakes

- Using `(low + high) / 2` → integer overflow for large indices. Always use `low + (high - low) / 2`
- `while (low <= high)` for exact search vs `while (low < high)` for boundary search — know when to use each
- In rotated array search: checking `nums[low] <= nums[mid]` (not `<`) — handles duplicates and single-element halves correctly
- In 2D matrix staircase: starting at bottom-left also works (go up or right) — but top-right is more intuitive
- Forgetting that Binary Search on Answer needs a **monotonic** feasibility function — verify this before applying the pattern
