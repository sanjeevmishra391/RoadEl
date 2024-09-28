package Algorithms;

import java.util.Arrays;

public class Kadane {

    static int maximumSumSubarray(int nums[]) {
        if(nums.length == 0)
            return 0;

        int max = nums[0], sum = 0;
        for(int n : nums) {
            sum = Math.max(sum, 0);
            sum += n;
            max = Math.max(max, sum);
        }
        
        return max;
    }

    static int[] maximumSumSubarrayWindow(int nums[]) {
        int max = nums[0], sum = 0, l=0, start = 0, end = 0;
        for(int i=0; i<nums.length; i++) {
            if(sum<0) {
                sum = 0;
                l = i;
            }
            sum += nums[i];
            if(sum > max) {
                max = sum;
                start = l;
                end = i;
            }
        }
        
        return new int[]{start, end};
    }

    public static void main(String[] args) {
        int nums[] = {4, -1, 2, -7, 8, 2};
        System.out.println(maximumSumSubarray(nums));
        System.out.println(Arrays.toString(maximumSumSubarrayWindow(nums)));
    }
}
