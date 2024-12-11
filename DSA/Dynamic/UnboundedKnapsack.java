package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class UnboundedKnapsack {
    int result;
    Methods method;
    int dp[][];

    UnboundedKnapsack(int[] weight, int[] value, int W, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(weight, value, W, weight.length-1);
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
                    new LabeledData<>("Profit", result));
    }

    int recursive(int[] weight, int[] value, int W, int idx) {
        if(idx<0 || W<0)
            return 0;

        if(W>=weight[idx]) {
            return Math.max(recursive(weight, value, W - weight[idx], idx) + value[idx], recursive(weight, value, W, idx-1));
        } else {
            return recursive(weight, value, W, idx-1);
        }
    }

    int tabulation(int[] weight, int[] value, int W) {
        int n = weight.length;
        int dp[][] = new int[n+1][W+1];

        for(int i=1; i<=n; i++) {
            for(int j=1; j<=W; j++) {
                dp[i][j] = dp[i-1][j];
                if(j>=weight[i-1]) {
                    dp[i][j] = Math.max(dp[i][j-weight[i-1]] + value[i-1], dp[i][j]);
                }
            }
        }

        return dp[n][W];
    }

    int memoization(int[] weight, int[] value, int W, int idx, int[][] dp) {
        if(idx<0 || W<0)
            return 0;

        if(dp[idx][W]!=-1)
            return dp[idx][W];

        dp[idx][W] = recursive(weight, value, W, idx-1);

        if(W>=weight[idx])
            dp[idx][W] = Math.max(dp[idx][W], recursive(weight, value, W-weight[idx], idx)+value[idx]);
        
        return dp[idx][W];
    }

    public static void main(String[] args) {
        int[] weight = {10, 20, 30};
        int[] value = {60, 100, 120};
        int W = 50;
        
        new UnboundedKnapsack(weight, value, W, Methods.RECURSION, true);
        new UnboundedKnapsack(weight, value, W, Methods.TABULATION, true);
        new UnboundedKnapsack(weight, value, W, Methods.MEMOIZATION, true);

    }

}