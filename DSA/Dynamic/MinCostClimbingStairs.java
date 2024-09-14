package Dynamic;
/* 
 * You are given an integer array cost where cost[i] is the cost of ith step on a staircase. Once you pay the cost, you can either climb one or two steps.
 * You can either start from the step with index 0, or the step with index 1.
 * Return the minimum cost to reach the top of the floor.
 * 
 * Input: cost = [10,15,20]
 * Output: 15
 * 
 * 
 * Input: cost = [1,100,1,1,1,100,1,1,100,1]
 * Output: 6
 */

import java.util.Arrays;

public class MinCostClimbingStairs {
    int result;
    int dp[];

    MinCostClimbingStairs(int cost[], String method) {
        switch (method) {
            case "recursive":
                result = Math.min(recursive(cost, 0), recursive(cost, 1));
                break;
            case "tabulationDP":
                result = tabulationDP(cost);
                break;
            case "memoizationDP":
                int n = cost.length;
                dp = new int[n+1];
                Arrays.fill(dp, -1);
                result = Math.min(memoizationDP(cost, n-2), memoizationDP(cost, n-1));
                break;
            default:
                break;
        }

        printRes(cost, result, method);
    }

    int recursive(int[] cost, int stair) {
        if(stair>=cost.length) return 0;

        return Math.min(recursive(cost, stair+1), recursive(cost, stair+2)) + cost[stair];
    }

    int tabulationDP(int[] cost) {
        int n = cost.length;
        int dp[] = new int[n+1];

        dp[0] = cost[0];
        dp[1] = cost[1];

        for(int i=2; i<n; i++) {
            dp[i] = Math.min(dp[i-1], dp[i-2]) + cost[i];
        }

        return Math.min(dp[n-2], dp[n-1]);
    }

    int memoizationDP(int[] cost, int stair) {
        if(stair==0 || stair ==1)
            return cost[stair];
        
        if(dp[stair]!=-1)
            return dp[stair];
        
        dp[stair] = Math.min(memoizationDP(cost,stair-1), memoizationDP(cost, stair-2)) + cost[stair];
        return dp[stair];
    }

    void printRes(int[] cost, int res, String method) {
        System.out.println("Method called : " + method);
        System.out.print("Input : ");
        for(int i : cost) 
            System.out.print(i + ",");

        System.out.println("\n" + "Output : " + result + "\n");
    }

    public static void main(String[] args) {
        int[] cost1 = {1,100,1,1,1,100,1,1,100,1};
        int[] cost2 = {10,15,20};

        new MinCostClimbingStairs(cost1, "recursive");
        new MinCostClimbingStairs(cost2, "recursive");

        new MinCostClimbingStairs(cost1, "tabulationDP");
        new MinCostClimbingStairs(cost2, "tabulationDP");

        new MinCostClimbingStairs(cost1, "memoizationDP");
        new MinCostClimbingStairs(cost2, "memoizationDP");

    }
}
