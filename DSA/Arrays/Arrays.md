# Arrays

## Why Arrays Are the Most Important DSA Topic

Arrays appear in almost every interview problem — either directly or as the underlying structure for strings, matrices, and sliding window problems. They are the foundation that all other patterns are applied on top of.

---

## Core Concepts

### Memory Layout
```
int[] arr = {10, 20, 30, 40, 50}
Index:        0   1   2   3   4

- Contiguous memory block
- Random access in O(1): arr[i] = base_address + i * element_size
- Fixed size in Java (use ArrayList for dynamic)
```

### Complexity Reference
| Operation | Array | ArrayList |
|---|---|---|
| Access by index | O(1) | O(1) |
| Search (unsorted) | O(n) | O(n) |
| Search (sorted) | O(log n) | O(log n) |
| Insert at end | O(1) | O(1) amortized |
| Insert at middle | O(n) | O(n) |
| Delete at middle | O(n) | O(n) |
| Space | O(n) | O(n) |

---

## Essential Array Techniques

### 1. Prefix Sum — Range Queries in O(1)
```
Problem: "sum of subarray from index l to r" asked multiple times
Brute force: O(n) per query
Prefix sum: O(n) build, O(1) per query

prefix[i] = arr[0] + arr[1] + ... + arr[i]
sum(l, r) = prefix[r] - prefix[l-1]
```
```java
// Build prefix sum
int[] prefix = new int[n + 1];  // prefix[0] = 0 (sentinel)
for (int i = 0; i < n; i++)
    prefix[i + 1] = prefix[i] + arr[i];

// Query sum from index l to r (0-indexed, inclusive)
int rangeSum = prefix[r + 1] - prefix[l];
```

**Key problems using prefix sum:**
- Subarray Sum Equals K → count subarrays with sum = k using HashMap
- Range Sum Query 2D → 2D prefix sum
- Find Pivot Index → total sum - prefix[i] - arr[i] == prefix[i]

```java
// Subarray Sum Equals K — O(n) with prefix sum + HashMap
public int subarraySum(int[] nums, int k) {
    Map<Integer, Integer> prefixCount = new HashMap<>();
    prefixCount.put(0, 1);  // empty subarray has sum 0
    int sum = 0, count = 0;
    for (int n : nums) {
        sum += n;
        count += prefixCount.getOrDefault(sum - k, 0);
        prefixCount.merge(sum, 1, Integer::sum);
    }
    return count;
}
// Time: O(n)  Space: O(n)
```

---

### 2. Kadane's Algorithm — Maximum Subarray Sum
```
Key insight: if running sum becomes negative, reset it — it only hurts future sums
```
```java
// Time: O(n)  Space: O(1)
public int maxSubArray(int[] nums) {
    int maxSum = nums[0], curSum = 0;
    for (int n : nums) {
        curSum = Math.max(curSum, 0);  // reset if negative
        curSum += n;
        maxSum = Math.max(maxSum, curSum);
    }
    return maxSum;
}
```

---

### 3. Dutch National Flag (3-way partition)
```
Problem: sort array of 0s, 1s, 2s in-place in O(n)
Three pointers: low (next 0 position), mid (current), high (next 2 position)
```
```java
// Time: O(n)  Space: O(1)
public void sortColors(int[] nums) {
    int low = 0, mid = 0, high = nums.length - 1;
    while (mid <= high) {
        if      (nums[mid] == 0) swap(nums, low++, mid++);
        else if (nums[mid] == 1) mid++;
        else                     swap(nums, mid, high--);
        // don't increment mid when swapping with high — recheck swapped element
    }
}
```

---

### 4. Cyclic Sort — When Values Are 1 to N
```
Problem: array contains values 1 to n, find missing/duplicate
Key insight: value v should be at index v-1 → swap until every element is at right index
```
```java
// Time: O(n)  Space: O(1)
// After cyclic sort, index where nums[i] != i+1 → that index+1 is missing
void cyclicSort(int[] nums) {
    int i = 0;
    while (i < nums.length) {
        int correct = nums[i] - 1;
        if (nums[i] != nums[correct]) swap(nums, i, correct);
        else i++;
    }
}
```

---

### 5. Boyer-Moore Voting — Majority Element
```
Problem: find element appearing > n/2 times
Key insight: majority element "survives" vote cancellation with any other element
```
```java
// Time: O(n)  Space: O(1)
public int majorityElement(int[] nums) {
    int candidate = nums[0], votes = 1;
    for (int i = 1; i < nums.length; i++) {
        if (votes == 0) { candidate = nums[i]; votes = 1; }
        else if (nums[i] == candidate) votes++;
        else votes--;
    }
    return candidate;  // problem guarantees majority exists
}
```

---

### 6. Product Array Without Division
```
Problem: output[i] = product of all elements except nums[i], no division
Key: left pass computes prefix product, right pass multiplies suffix product
```
```java
// Time: O(n)  Space: O(1) extra (output array doesn't count)
public int[] productExceptSelf(int[] nums) {
    int n = nums.length;
    int[] output = new int[n];
    output[0] = 1;
    for (int i = 1; i < n; i++)
        output[i] = output[i - 1] * nums[i - 1];  // prefix products

    int right = 1;
    for (int i = n - 1; i >= 0; i--) {
        output[i] *= right;                         // multiply suffix product
        right *= nums[i];
    }
    return output;
}
```

---

### 7. Matrix Rotation / Spiral Traversal

**Rotate 90° clockwise (in-place):**
```java
// Step 1: Transpose (swap across diagonal)
// Step 2: Reverse each row
// Time: O(n²)  Space: O(1)
public void rotate(int[][] matrix) {
    int n = matrix.length;
    // Transpose
    for (int i = 0; i < n; i++)
        for (int j = i + 1; j < n; j++) {
            int tmp = matrix[i][j];
            matrix[i][j] = matrix[j][i];
            matrix[j][i] = tmp;
        }
    // Reverse each row
    for (int[] row : matrix) {
        int l = 0, r = row.length - 1;
        while (l < r) { int tmp = row[l]; row[l++] = row[r]; row[r--] = tmp; }
    }
}
```

**Spiral traversal:**
```java
// Time: O(m×n)  Space: O(1)
public List<Integer> spiralOrder(int[][] matrix) {
    List<Integer> result = new ArrayList<>();
    int top = 0, bottom = matrix.length - 1;
    int left = 0, right = matrix[0].length - 1;
    while (top <= bottom && left <= right) {
        for (int i = left;  i <= right;  i++) result.add(matrix[top][i]);   top++;
        for (int i = top;   i <= bottom; i++) result.add(matrix[i][right]); right--;
        if (top <= bottom)
            for (int i = right; i >= left;  i--) result.add(matrix[bottom][i]); bottom--;
        if (left <= right)
            for (int i = bottom; i >= top;  i--) result.add(matrix[i][left]);  left++;
    }
    return result;
}
```

---

## Key Interview Problems

| Problem | Technique | Time | Space |
|---|---|---|---|
| Two Sum (unsorted) | HashMap | O(n) | O(n) |
| Best Time to Buy/Sell Stock | Track min so far | O(n) | O(1) |
| Maximum Subarray | Kadane's | O(n) | O(1) |
| Product of Array Except Self | Prefix × suffix | O(n) | O(1) |
| Find Missing Number | XOR or math sum | O(n) | O(1) |
| Find Duplicate Number | Floyd's cycle detection | O(n) | O(1) |
| Sort Colors (0,1,2) | Dutch National Flag | O(n) | O(1) |
| Majority Element | Boyer-Moore voting | O(n) | O(1) |
| Subarray Sum Equals K | Prefix sum + HashMap | O(n) | O(n) |
| Rotate Matrix | Transpose + reverse rows | O(n²) | O(1) |
| Spiral Matrix | 4-boundary traversal | O(m×n) | O(1) |
| Merge Intervals | Sort by start, compare | O(n log n) | O(n) |
| Jump Game | Greedy max reach | O(n) | O(1) |
| Trapping Rain Water | Two pointers / prefix max | O(n) | O(1) |
| Container With Most Water | Two pointers | O(n) | O(1) |

---

## Common Interview Q&A

**Q: Two Sum — sorted vs unsorted?**
A: Unsorted → HashMap in O(n). Sorted → Two Pointers in O(n) and O(1) space.

**Q: When does prefix sum beat brute force?**
A: When the same range-sum query is asked multiple times, or when you need to count subarrays with a target sum.

**Q: How to find a duplicate in array of 1 to n without extra space?**
A: Floyd's cycle detection — treat array values as "next pointer" indices. Or cyclic sort.

**Q: Difference between in-place and O(1) space?**
A: In-place means modifying the input directly. O(1) space means using a fixed number of extra variables — both mean no extra array proportional to input size.

---

> Trapping Rain Water and Container With Most Water → full solutions in [`TwoPointer/TwoPointers.md`](../TwoPointer/TwoPointers.md)

## Problems With Full Solutions

### Best Time to Buy and Sell Stock
```java
// Time: O(n)  Space: O(1)
// Key: track min price seen so far, update max profit at each step
public int maxProfit(int[] prices) {
    int minPrice = Integer.MAX_VALUE, maxProfit = 0;
    for (int price : prices) {
        minPrice = Math.min(minPrice, price);
        maxProfit = Math.max(maxProfit, price - minPrice);
    }
    return maxProfit;
}
```

### Find Missing Number (XOR approach)
```java
// Time: O(n)  Space: O(1)
// XOR all indices 0..n with all values — pairs cancel, missing survives
public int missingNumber(int[] nums) {
    int xor = nums.length;
    for (int i = 0; i < nums.length; i++) xor ^= i ^ nums[i];
    return xor;
}
```

### Jump Game
```java
// Time: O(n)  Space: O(1)
// Key: track furthest index reachable, if current > max reach → stuck
public boolean canJump(int[] nums) {
    int maxReach = 0;
    for (int i = 0; i < nums.length; i++) {
        if (i > maxReach) return false;
        maxReach = Math.max(maxReach, i + nums[i]);
    }
    return true;
}
```

---

## Common Mistakes

- **Two Sum**: using `arr[i] == target - arr[i]` without checking `i != j` (same index)
- **Prefix sum**: off-by-one — use `prefix[i+1] = prefix[i] + arr[i]` with a 0-sentinel at index 0
- **Merge intervals**: forgetting to sort by start time first
- **Dutch Flag**: incrementing `mid` when swapping with `high` — must recheck swapped element
- **Rotate matrix**: transposing then reversing columns (wrong) vs reversing rows (correct for 90° clockwise)
