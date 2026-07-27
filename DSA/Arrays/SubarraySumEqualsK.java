package Arrays;

import java.util.HashMap;
import java.util.Map;

/* 
https://leetcode.com/problems/subarray-sum-equals-k
Given an array of integers nums and an integer k, return the total number of subarrays whose sum equals to k.
A subarray is a contiguous non-empty sequence of elements within an array.
Example 3, 4, 7, 2, -3, 1, 4, 2 and k = 7

Since arary can have -ve and +ve elements, sliding window will not work

sum[i, j] = sum[j] - sum[i-1] = k
This is like two-sum problem, instead of storing indices, we store frequency for a sum to appear in prefix-sum
Store { sum[j], fre } and check if sum[i]-k exist then add freq to result. Store sum[i] along with freq.

Map = {{0,1}, {3, 1}, {7, 1}, {14, 1}, {16, 1}, {13, 1}, {14, 2}, {18, 1}, {20, 1}
count = 0.     0.      1.      2.       2.       2.       3.       3.       4.

Similar problem: https://leetcode.com/problems/subarray-sums-divisible-by-k/
*/ 

public class SubarraySumEqualsK {

    // Time: O(n^2), Space: O(1)
    static int bruteForce(int nums[], int k) {
        int res = 0, n = nums.length;
        for(int i=0; i<n; i++) {
            int sum = 0;
            for(int j=i; j<n; j++) {
                sum += nums[j];
                if(sum == k) {
                    res++;
                }
            }
        }

        return res;
    }

    // Time: O(n), Space: O(n)
    static int optimised(int nums[], int k) {
        int count = 0, sum = 0;
        Map<Integer, Integer> freqMap = new HashMap<>();
        freqMap.put(0, 1);
        for(int n: nums) {
            sum += n;
            // check if sum - k exist in map and use freq to count result
            count += freqMap.getOrDefault(sum - k, 0);
            // store sum, freq in map
            freqMap.put(sum, freqMap.getOrDefault(sum, 0)+1);
        }

        return count;
    }

    public static void main(String[] args) {
        int nums[] = {3, 4, 7, 2, -3, 1, 4, 2};
        int k = 7;

        System.out.println(bruteForce(nums, k));
        System.out.println(optimised(nums, k));
    }
}
