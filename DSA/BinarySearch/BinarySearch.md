# Binary Search

> Pattern triggers → [`DSA/HowToPrepareDSA.md`](../HowToPrepareDSA.md)

## When to Use Binary Search
- Sorted array + "find target / position" → **classic binary search**
- "Minimum X that satisfies condition Y" → **binary search on answer space**
- "Find boundary between true/false" → **binary search on predicate**
- Input space is **monotonic** (feasibility only flips once as the value increases)

## Two Templates — Know Both

### Template 1: Classic (find exact value)
```java
int binarySearch(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    while (left <= right) {              // <= because right = n-1 (inclusive)
        int mid = left + (right - left) / 2;   // avoids overflow vs (l+r)/2
        if (nums[mid] == target) return mid;
        if (nums[mid] < target)  left = mid + 1;
        else                     right = mid - 1;
    }
    return -1;  // not found
}
// Time: O(log n)  Space: O(1)
```

### Template 2: Find first/last position (boundary search)
```java
// Find LEFTMOST position where condition is true
int findLeft(int[] nums, int target) {
    int left = 0, right = nums.length - 1, result = -1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] == target) {
            result = mid;        // record, but keep searching LEFT
            right = mid - 1;
        } else if (nums[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return result;
}

// Find RIGHTMOST position
int findRight(int[] nums, int target) {
    int left = 0, right = nums.length - 1, result = -1;
    while (left <= right) {
        int mid = left + (right - left) / 2;
        if (nums[mid] == target) {
            result = mid;        // record, but keep searching RIGHT
            left = mid + 1;
        } else if (nums[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return result;
}
```

### Template 3: Binary Search on Answer Space
```java
// "Find minimum X such that condition(X) is true"
// Works when: as X increases, condition goes from false → true (monotonic)
int findMinimum(int[] nums) {
    int left = minPossibleAnswer;
    int right = maxPossibleAnswer;
    while (left < right) {              // < not <=  (we want the boundary)
        int mid = left + (right - left) / 2;
        if (feasible(mid, nums)) right = mid;    // mid might be the answer
        else                     left = mid + 1; // too small, go right
    }
    return left;  // left == right == answer
}
```

---

## The 9 Binary Search Problems (from DSA.md)

### 1. Koko Eating Bananas
```
Pattern: Binary Search on answer space
Search space: [1, max(piles)]
Feasible(speed): can Koko eat all piles in h hours at this speed?
  → hours needed = sum(ceil(pile / speed))
  → feasible if hours <= h
Direction: minimize speed → find leftmost feasible
```
```java
// Time: O(n log(max(piles)))  Space: O(1)
public int minEatingSpeed(int[] piles, int h) {
    int left = 1, right = Arrays.stream(piles).max().getAsInt();
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (canFinish(piles, mid, h)) right = mid;
        else left = mid + 1;
    }
    return left;
}
boolean canFinish(int[] piles, int speed, int h) {
    int hours = 0;
    for (int pile : piles) hours += (pile + speed - 1) / speed;
    return hours <= h;
}
```

### 2. Capacity To Ship Packages In N Days
```
Pattern: Binary Search on answer space
Search space: [max(weights), sum(weights)]
  - min: must carry at least heaviest package
  - max: carry all in one day
Feasible(capacity): can we ship all packages in <= days days?
Direction: minimize capacity
```
```java
// Time: O(n log(sum))  Space: O(1)
public int shipWithinDays(int[] weights, int days) {
    int left = Arrays.stream(weights).max().getAsInt();
    int right = Arrays.stream(weights).sum();
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (canShip(weights, mid, days)) right = mid;
        else left = mid + 1;
    }
    return left;
}
boolean canShip(int[] weights, int cap, int days) {
    int daysNeeded = 1, current = 0;
    for (int w : weights) {
        if (current + w > cap) { daysNeeded++; current = 0; }
        current += w;
    }
    return daysNeeded <= days;
}
```

### 3. Minimum Number of Days to Make m Bouquets
```
Pattern: Binary Search on answer (number of days)
Search space: [1, max(bloomDay)]
Feasible(day): with this many days, can we make m bouquets of k flowers?
  → count consecutive bloomed flowers, form bouquets
Direction: minimize days
```
```java
// Time: O(n log(max))  Space: O(1)
public int minDays(int[] bloomDay, int m, int k) {
    if ((long) m * k > bloomDay.length) return -1;
    int left = 1, right = Arrays.stream(bloomDay).max().getAsInt();
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (canMake(bloomDay, mid, m, k)) right = mid;
        else left = mid + 1;
    }
    return left;
}
boolean canMake(int[] bloomDay, int day, int m, int k) {
    int bouquets = 0, flowers = 0;
    for (int d : bloomDay) {
        if (d <= day) { flowers++; if (flowers == k) { bouquets++; flowers = 0; } }
        else flowers = 0;
    }
    return bouquets >= m;
}
```

### 4. Kth Missing Positive Number
```
Pattern: Binary Search on index
Key insight: if no numbers were missing, arr[i] == i+1
  → missing count at index i = arr[i] - (i+1)
Find first index where missing count >= k
```
```java
// Time: O(log n)  Space: O(1)
public int findKthPositive(int[] arr, int k) {
    int left = 0, right = arr.length;
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (arr[mid] - (mid + 1) >= k) right = mid;
        else left = mid + 1;
    }
    return left + k;  // left = index, k missing before it, answer = left + k
}
```

### 5. Find the Smallest Divisor Given a Threshold
```
Pattern: Binary Search on answer (divisor)
Search space: [1, max(nums)]
Feasible(d): sum of ceil(num/d) for all nums <= threshold
Direction: minimize divisor
```
```java
// Time: O(n log(max))  Space: O(1)
public int smallestDivisor(int[] nums, int threshold) {
    int left = 1, right = Arrays.stream(nums).max().getAsInt();
    while (left < right) {
        int mid = left + (right - left) / 2;
        int sum = 0;
        for (int n : nums) sum += (n + mid - 1) / mid;
        if (sum <= threshold) right = mid;
        else left = mid + 1;
    }
    return left;
}
```

### 6. Split Array Largest Sum
```
Pattern: Binary Search on answer (max subarray sum)
Search space: [max(nums), sum(nums)]
Feasible(maxSum): can we split into <= k subarrays each with sum <= maxSum?
Direction: minimize maxSum
```
```java
// Time: O(n log(sum))  Space: O(1)
public int splitArray(int[] nums, int k) {
    int left = Arrays.stream(nums).max().getAsInt();
    int right = Arrays.stream(nums).sum();
    while (left < right) {
        int mid = left + (right - left) / 2;
        if (canSplit(nums, mid, k)) right = mid;
        else left = mid + 1;
    }
    return left;
}
boolean canSplit(int[] nums, int maxSum, int k) {
    int parts = 1, current = 0;
    for (int n : nums) {
        if (current + n > maxSum) { parts++; current = 0; }
        current += n;
    }
    return parts <= k;
}
```

### 7. Maximum Value at a Given Index in a Bounded Array
```
Pattern: Binary Search on answer (value at index)
Search space: [1, maxSum]
Feasible(val at index): can we build array with given constraints summing to <= maxSum?
Key: optimal array is a mountain — peak at index, decreasing both sides
```
```java
// Time: O(log(maxSum))  Space: O(1)
public int maxValue(int n, int index, int maxSum) {
    int left = 1, right = maxSum;
    while (left < right) {
        int mid = left + (right - left + 1) / 2;
        if (canBuild(n, index, maxSum, mid)) left = mid;
        else right = mid - 1;
    }
    return left;
}
long sumOfRange(long peak, long count) {
    if (peak >= count) return (peak + peak - count + 1) * count / 2;
    return (1 + peak) * peak / 2 + (count - peak);
}
boolean canBuild(int n, int index, int maxSum, int val) {
    long sum = sumOfRange(val, index + 1) + sumOfRange(val, n - index) - val;
    return sum <= maxSum;
}
```

### 8. Minimize Max Distance to Gas Station
```
Pattern: Binary Search on answer (max distance between stations)
Search space: [0, max gap between existing stations]
Feasible(dist): can we add <= k stations so no gap exceeds dist?
  → stations needed per gap = ceil(gap/dist) - 1
```
```java
// Time: O(n log(1/precision))  Space: O(1)  [floating point BS]
public double minmaxGasDist(int[] stations, int k) {
    double left = 0, right = stations[stations.length - 1] - stations[0];
    for (int i = 0; i < 100; i++) {   // 100 iterations → precision ~1e-15
        double mid = (left + right) / 2;
        if (canPlace(stations, k, mid)) right = mid;
        else left = mid;
    }
    return left;
}
boolean canPlace(int[] stations, int k, double dist) {
    int count = 0;
    for (int i = 1; i < stations.length; i++)
        count += (int)((stations[i] - stations[i-1]) / dist);
    return count <= k;
}
```

### 9. Divide Chocolate
```
Pattern: Binary Search on answer (minimum sweetness)
Search space: [min(sweetness), sum/k]
Feasible(minSweet): can we cut into >= k+1 pieces each with sum >= minSweet?
Direction: maximize minimum sweetness (binary search, feasibility goes other way)
```
```java
// Time: O(n log(sum))  Space: O(1)
public int maximizeSweetness(int[] sweetness, int k) {
    int left = Arrays.stream(sweetness).min().getAsInt();
    int right = Arrays.stream(sweetness).sum() / (k + 1);
    while (left < right) {
        int mid = left + (right - left + 1) / 2;  // upper mid for maximize
        if (canCut(sweetness, mid, k + 1)) left = mid;
        else right = mid - 1;
    }
    return left;
}
boolean canCut(int[] s, int minSweet, int pieces) {
    int count = 0, current = 0;
    for (int sw : s) {
        current += sw;
        if (current >= minSweet) { count++; current = 0; }
    }
    return count >= pieces;
}
```

---

## Pattern Debrief — Binary Search on Answer

All 9 problems share the same structure:
```
1. Identify what to binary search ON (the answer itself)
2. Define the search space [left, right]
3. Write a feasibility function: feasible(mid) → boolean
4. Decide direction:
   - "minimize X" → find leftmost true → right = mid when feasible
   - "maximize X" → find rightmost true → left = mid when feasible (use upper mid)
```

The feasibility function is always O(n). Binary search runs O(log(range)). Total: **O(n log range)**.

---

## Common Mistakes
- Integer overflow: use `left + (right - left) / 2` not `(left + right) / 2`
- Off-by-one: `left <= right` for exact search, `left < right` for boundary search
- Wrong mid for maximize problems: use `mid = left + (right - left + 1) / 2` (upper mid) to avoid infinite loop
- Forgetting to verify the search space bounds make sense before coding
