package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class KnapSack {

    int result;
    Methods method;
    int dp[][];

    KnapSack(int[] weight, int[] value, int W, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(weight, value, W, 0);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(weight, value, W);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[weight.length][W+1];
            for (int[] row : dp) {
                Arrays.fill(row, -1);
            }
            result = memoization(weight, value, W, weight.length-1, dp);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("Weight", weight),
                    new LabeledData<>("Value", value),
                    new LabeledData<>("Bag Capacity", W),
                Delimiter.OUTPUT, 
                    new LabeledData<>("Total Profit", result));
    }

    int recursive(int[] weight, int[] value, int W, int idx) {
        if(W<=0 || idx >= weight.length)
            return 0;
        
        // 1. if current weight is less than or equal to capacity then we have choice to either pick it or not.
        if(W >= weight[idx])
            return Math.max(recursive(weight, value, W, idx+1), 
                            recursive(weight, value, W-weight[idx], idx+1) + value[idx]);
        
        // 2. if current weight is more than capacity then we don't pick it.
        return recursive(weight, value, W, idx+1);
    }

    int tabulation(int[] weight, int[] value, int W) {
        int dp[][] = new int[weight.length+1][W+1];

        // initialization : if capacity is 0 or #items is 0 then profit will be 0;
        for(int n=1; n <= weight.length; n++) {
            for(int w = 1; w<=W; w++) {
                if(weight[n-1] <= w)
                   dp[n][w] = Math.max(dp[n-1][w - weight[n-1]] + value[n-1], dp[n-1][w]);
                else    
                    dp[n][w] = dp[n-1][w];
            }
        }

        return dp[weight.length][W];
    }

    int memoization(int[] weight, int[] value, int W, int idx, int[][] dp) {
        if(idx < 0)
            return 0;
        
        if(dp[idx][W] != -1)
            return dp[idx][W];

        // 1. if current weight is less than or equal to capacity then we have choice to either pick it or not.
        if(W >= weight[idx])
            dp[idx][W] = Math.max(memoization(weight, value, W, idx-1, dp), 
            memoization(weight, value, W-weight[idx], idx-1, dp) + value[idx]);
        else
            // 2. if current weight is more than capacity then we don't pick it.
            dp[idx][W] = memoization(weight, value, W, idx-1, dp);

        return dp[idx][W];
    }

    public static void main(String[] args) {
        int[] weight = {10, 20, 30};
        int[] value = {60, 100, 120};
        int W = 50;
        
        new KnapSack(weight, value, W, Methods.RECURSION, true);
        new KnapSack(weight, value, W, Methods.TABULATION, true);
        new KnapSack(weight, value, W, Methods.MEMOIZATION, true);

    }
}
