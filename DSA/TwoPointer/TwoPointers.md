# Two Pointers — Problems

> Pattern theory and templates: `DSA/Algorithms/TwoPointers.md`

## Problem List

| # | Problem | Sub-pattern | Difficulty |
|---|---|---|---|
| 1 | Two Sum II (sorted array) | Opposite ends | Easy |
| 2 | Valid Palindrome | Opposite ends | Easy |
| 3 | Container With Most Water | Opposite ends | Medium |
| 4 | 3Sum | Opposite ends | Medium |
| 5 | 4Sum | Opposite ends | Medium |
| 6 | Remove Duplicates from Sorted Array | Fast & Slow | Easy |
| 7 | Move Zeroes | Fast & Slow | Easy |
| 8 | Trapping Rain Water | Opposite ends | Hard |
| 9 | Boats to Save People | Opposite ends | Medium |
| 10 | Sort Colors (Dutch Flag) | Three pointers | Medium |

---

## Sub-Pattern 1: Opposite Ends

Both pointers start at ends, converge inward based on a condition.

### Problem 1: Two Sum II — Input Array Is Sorted
```
Input:  numbers = [2,7,11,15], target = 9
Output: [1,2]  (1-indexed)

Key: sorted array → two pointers. Sum < target → move left right. Sum > target → move right left.
```
```java
// Time: O(n)  Space: O(1)
public int[] twoSum(int[] numbers, int target) {
    int left = 0, right = numbers.length - 1;
    while (left < right) {
        int sum = numbers[left] + numbers[right];
        if      (sum == target) return new int[]{left + 1, right + 1};
        else if (sum < target)  left++;
        else                    right--;
    }
    return new int[]{};
}
```

---

### Problem 2: Valid Palindrome
```
Input:  s = "A man, a plan, a canal: Panama"
Output: true

Key: skip non-alphanumeric, compare case-insensitively from both ends.
```
```java
// Time: O(n)  Space: O(1)
public boolean isPalindrome(String s) {
    int left = 0, right = s.length() - 1;
    while (left < right) {
        while (left < right && !Character.isLetterOrDigit(s.charAt(left)))  left++;
        while (left < right && !Character.isLetterOrDigit(s.charAt(right))) right--;
        if (Character.toLowerCase(s.charAt(left)) !=
            Character.toLowerCase(s.charAt(right))) return false;
        left++; right--;
    }
    return true;
}
```

---

### Problem 3: Container With Most Water
```
Input:  height = [1,8,6,2,5,4,8,3,7]
Output: 49

Key: area = min(height[l], height[r]) * (r - l)
     Always move the SHORTER wall inward — moving the taller one can only decrease or maintain min height
     while also shrinking width → guaranteed smaller area.
```
```java
// Time: O(n)  Space: O(1)
public int maxArea(int[] height) {
    int left = 0, right = height.length - 1, maxWater = 0;
    while (left < right) {
        int water = Math.min(height[left], height[right]) * (right - left);
        maxWater = Math.max(maxWater, water);
        if (height[left] < height[right]) left++;
        else                              right--;
    }
    return maxWater;
}
```

---

### Problem 4: 3Sum
```
Input:  nums = [-1,0,1,2,-1,-4]
Output: [[-1,-1,2],[-1,0,1]]

Key:
  1. Sort first → enables two pointers
  2. Fix one element (nums[i]), find pair summing to -nums[i] in rest
  3. Skip duplicates at every level to avoid duplicate triplets
```
```java
// Time: O(n²)  Space: O(1) excluding output
public List<List<Integer>> threeSum(int[] nums) {
    Arrays.sort(nums);
    List<List<Integer>> result = new ArrayList<>();
    for (int i = 0; i < nums.length - 2; i++) {
        if (i > 0 && nums[i] == nums[i - 1]) continue;  // skip duplicate i
        if (nums[i] > 0) break;                          // no triplet possible
        int left = i + 1, right = nums.length - 1;
        while (left < right) {
            int sum = nums[i] + nums[left] + nums[right];
            if (sum == 0) {
                result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                while (left < right && nums[left]  == nums[left + 1])  left++;  // skip dup
                while (left < right && nums[right] == nums[right - 1]) right--; // skip dup
                left++; right--;
            } else if (sum < 0) left++;
            else                right--;
        }
    }
    return result;
}
```

---

### Problem 5: 4Sum
```
Input:  nums = [1,0,-1,0,-2,2], target = 0
Output: [[-2,-1,1,2],[-2,0,0,2],[-1,0,0,1]]

Key: generalize 3Sum — fix two elements (i, j), two-pointer on the rest.
     Two nested loops + inner two-pointer = O(n³) total.
```
```java
// Time: O(n³)  Space: O(1) excluding output
public List<List<Integer>> fourSum(int[] nums, int target) {
    Arrays.sort(nums);
    List<List<Integer>> result = new ArrayList<>();
    for (int i = 0; i < nums.length - 3; i++) {
        if (i > 0 && nums[i] == nums[i - 1]) continue;
        for (int j = i + 1; j < nums.length - 2; j++) {
            if (j > i + 1 && nums[j] == nums[j - 1]) continue;
            int left = j + 1, right = nums.length - 1;
            while (left < right) {
                long sum = (long)nums[i] + nums[j] + nums[left] + nums[right];
                if (sum == target) {
                    result.add(Arrays.asList(nums[i], nums[j], nums[left], nums[right]));
                    while (left < right && nums[left]  == nums[left + 1])  left++;
                    while (left < right && nums[right] == nums[right - 1]) right--;
                    left++; right--;
                } else if (sum < target) left++;
                else                     right--;
            }
        }
    }
    return result;
}
// Note: use long for sum to avoid integer overflow
```

---

### Problem 8: Trapping Rain Water
```
Input:  height = [0,1,0,2,1,0,1,3,2,1,2,1]
Output: 6

Key: water at bar i = min(maxLeft, maxRight) - height[i]
Two pointer: process from side with SMALLER max (that's the constraining side)
```
```java
// Time: O(n)  Space: O(1)
public int trap(int[] height) {
    int left = 0, right = height.length - 1;
    int maxLeft = 0, maxRight = 0, water = 0;
    while (left < right) {
        if (height[left] <= height[right]) {
            if (height[left] >= maxLeft) maxLeft = height[left];
            else water += maxLeft - height[left];
            left++;
        } else {
            if (height[right] >= maxRight) maxRight = height[right];
            else water += maxRight - height[right];
            right--;
        }
    }
    return water;
}
```

---

### Problem 9: Boats to Save People
```
Input:  people = [3,2,2,1], limit = 3
Output: 3  (boats: [3],[2,1],[2])

Key: greedy with two pointers — pair heaviest with lightest if they fit together.
     Sort first. If lightest + heaviest <= limit → both in one boat.
     Otherwise heaviest goes alone.
```
```java
// Time: O(n log n)  Space: O(1)
public int numRescueBoats(int[] people, int limit) {
    Arrays.sort(people);
    int left = 0, right = people.length - 1, boats = 0;
    while (left <= right) {
        if (people[left] + people[right] <= limit) left++;  // both fit
        right--;  // heaviest always takes a boat
        boats++;
    }
    return boats;
}
```

---

## Sub-Pattern 2: Fast & Slow (Same Direction)

Slow pointer tracks the "write position", fast pointer scans forward.

### Problem 6: Remove Duplicates from Sorted Array
```
Input:  nums = [1,1,2,3,3]
Output: 3  (array becomes [1,2,3,...])

Key: slow = next write position. Write only when nums[fast] != nums[slow-1].
```
```java
// Time: O(n)  Space: O(1)
public int removeDuplicates(int[] nums) {
    int slow = 1;
    for (int fast = 1; fast < nums.length; fast++)
        if (nums[fast] != nums[slow - 1])
            nums[slow++] = nums[fast];
    return slow;
}
```

**Variation: allow at most 2 duplicates (LC 80)**
```java
public int removeDuplicatesII(int[] nums) {
    int slow = 2;
    for (int fast = 2; fast < nums.length; fast++)
        if (nums[fast] != nums[slow - 2])  // compare with 2 positions back
            nums[slow++] = nums[fast];
    return slow;
}
```

---

### Problem 7: Move Zeroes
```
Input:  nums = [0,1,0,3,12]
Output: [1,3,12,0,0]

Key: slow tracks next non-zero position. Swap nums[slow] and nums[fast] when non-zero found.
     Maintains relative order of non-zero elements.
```
```java
// Time: O(n)  Space: O(1)
public void moveZeroes(int[] nums) {
    int slow = 0;
    for (int fast = 0; fast < nums.length; fast++)
        if (nums[fast] != 0)
            swap(nums, slow++, fast);
}
// When fast == slow, swap is no-op (same index) — fine
```

---

## Sub-Pattern 3: Three Pointers

### Problem 10: Sort Colors (Dutch National Flag)
```
Input:  nums = [2,0,2,1,1,0]
Output: [0,0,1,1,2,2]

Key: 3 pointers — low (next 0 spot), mid (current), high (next 2 spot)
     - nums[mid] == 0: swap with low, advance both low and mid
     - nums[mid] == 1: advance mid only
     - nums[mid] == 2: swap with high, advance high only (DON'T advance mid — recheck)
```
```java
// Time: O(n)  Space: O(1)
public void sortColors(int[] nums) {
    int low = 0, mid = 0, high = nums.length - 1;
    while (mid <= high) {
        if      (nums[mid] == 0) { swap(nums, low++, mid++); }
        else if (nums[mid] == 1) { mid++; }
        else                     { swap(nums, mid, high--); }
        // critical: don't mid++ when swapping with high
        // the swapped element at mid hasn't been examined yet
    }
}
private void swap(int[] nums, int i, int j) {
    int tmp = nums[i]; nums[i] = nums[j]; nums[j] = tmp;
}
```

---

## Pattern Debrief — Two Pointers

| Sub-pattern | When | Direction |
|---|---|---|
| Opposite ends | Sorted array, pair/sum, palindrome | Converge inward |
| Fast & Slow | In-place modification, remove/write | Both left-to-right |
| Three pointers | 3-way partition | Mixed |

**The key decision every time:** Which pointer should move, and when?
- Opposite ends: move whichever side is the "losing" side (smaller wall, smaller sum side)
- Fast & slow: fast always moves; slow only moves when a condition is met

**Duplicate handling in 3Sum/4Sum:**
```
After finding a valid triplet:
  while (left < right && nums[left] == nums[left+1]) left++;
  while (left < right && nums[right] == nums[right-1]) right--;
  left++; right--;  // move past the last valid pair
```

---

## Existing Code in This Repo
- [`TwoSum.java`](./TwoSum.java) — unsorted version (HashMap)
- [`TwoSumSorted.java`](./TwoSumSorted.java) — sorted version (two pointers)
- [`ThreeSum.java`](./ThreeSum.java) — 3Sum with deduplication
- [`FourSum.java`](./FourSum.java) — 4Sum with deduplication
- [`KSum.java`](./KSum.java) — generalized K-Sum
