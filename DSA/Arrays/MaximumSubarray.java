package Arrays;

// Given an integer array nums, find the subarray with the largest sum, and return its sum.

public class MaximumSubarray {

    static int bruteForce(int nums[]) {
        int maxSum = nums[0];
        // find all the subarrays and for each subarray calculate sum and compare with maxSum
        for(int i=0; i<nums.length; i++) {
            int subSum = nums[i];
            for(int j=i; j<nums.length; j++) {
                subSum = j==i ? nums[j] : subSum + nums[j];
                maxSum = Math.max(maxSum, subSum);
            }
        }

        return maxSum;
    }

    // use kadane's algo
    static int optimised(int nums[]) {
        // Every index has an optimal choice to make
        // - Extend the running sum from a previous elements
        // - Start fresh sum from current index
        // If running sum becomes -ve then start fresh sum, as -ve sum is taxing for maximum sum

        int maxSoFar = nums[0];
        int currMax = 0;
        for(int n: nums) {
            currMax += n;
            maxSoFar = Math.max(maxSoFar, currMax);
            if(currMax < 0) {
                currMax = 0;
            }
        }

        return maxSoFar;
    }

    public static void main(String[] args) {
        int nums[] = {-2,1,-3,4,-1,2,1,-5,4};
        System.out.println(bruteForce(nums));
        System.out.println(optimised(nums));
    }
}
