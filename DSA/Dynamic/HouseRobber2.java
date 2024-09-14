/*
    You are a professional robber planning to rob houses along a street. 
    Each house has a certain amount of money stashed. All houses at this place are arranged in a circle. 
    That means the first house is the neighbor of the last one. Meanwhile, adjacent houses have a security system connected, 
    and it will automatically contact the police if two adjacent houses were broken into on the same night.

    Given an integer array nums representing the amount of money of each house, 
    return the maximum amount of money you can rob tonight without alerting the police.
 */

package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Methods;
import roadel.Utility.Delimiter;

public class HouseRobber2 {
    int result;
    Methods method;
    static int dp[];

    HouseRobber2(int[] nums, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = Math.max(recursion(nums, nums.length-3, true) + nums[nums.length-1], recursion(nums, nums.length-2, false));
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(nums);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[nums.length+1];
            Arrays.fill(dp, -1);
            int ans = memoization(nums, nums.length-3, true) + nums[nums.length-1];
            Arrays.fill(dp, -1);
            result = Math.max(ans, memoization(nums, nums.length-2, false));
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("ip", nums),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    int recursion(int[] nums, int idx, boolean last) {
        if(last && idx == 0) return 0;
        else if(idx<0) return 0;

        return Math.max(recursion(nums, idx-2, last) + nums[idx], recursion(nums, idx-1, last));
    }

    int tabulation(int[] ip) {
        return 0;
    }

    int memoization(int[] nums, int idx, boolean last) {
        if(last && idx == 0) return 0;
        else if(idx<0) return 0;

        if(dp[idx] != -1) return dp[idx];

        dp[idx] = Math.max(memoization(nums, idx-2, last) + nums[idx], memoization(nums, idx-1, last));
        return dp[idx];
    } 

    public static void main(String[] args) {
        int[] ip = {2,1,2,6,1,8,10,10};
        
        new HouseRobber2(ip, Methods.RECURSION, true);
        new HouseRobber2(ip, Methods.TABULATION, true);
        new HouseRobber2(ip, Methods.MEMOIZATION, true);
    }
}
