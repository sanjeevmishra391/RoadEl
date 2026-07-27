## Two Sum Problems

1. **Problem**: [Subarray Sum Equals K](https://leetcode.com/problems/subarray-sums-divisible-by-k/)  
**Pattern used**: Prefix sum and HashMap: Can be reduced to two-sum problem  
**Why this pattern (trigger)**: Sum of subarray in a range
Key insight / trick: sum[i, j] = sum[j] - sum[i-1] = k . This is like two-sum problem  
**Time complexity**: O(n), for a single iteration  
**Space complexity**: O(n), for hashmap  
**Variations this pattern handles**: Subarray sum divisble by k
2. [Subarray Sums Divisible by K](https://leetcode.com/problems/subarray-sums-divisible-by-k/description/)
3. [Range Sum Query 2D - Immutable](https://leetcode.com/problems/range-sum-query-2d-immutable/description/)
4. [Find Pivot Index](https://leetcode.com/problems/find-pivot-index/description/)

---

5. [Subarray with the largest sum](https://leetcode.com/problems/maximum-subarray/description/)