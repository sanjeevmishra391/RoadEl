package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class MinimumSubsetSumDiff {
    int result;
    Methods method;
    int dp[][];

    MinimumSubsetSumDiff(int[] ip, Methods method, boolean use) {
        this.method = method;

        int sum = helper(ip);

        if (method == Methods.RECURSION && use) {
            result = recursive(ip, sum, 0, ip.length-1);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(ip, sum);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[ip.length + 1][sum+1];
            for(int row[] : dp)
                Arrays.fill(row, -1);
            result = memoization(ip, sum, 0, ip.length-1, dp);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("ip", ip),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    int helper(int set[]) {
        int sum = 0;
        for(int a : set)
            sum += a;

        return sum;
    }

    int recursive(int[] set, int sum, int currSum, int idx) {
        if(idx == 0)
            return Math.abs(sum - 2*currSum);
        int diff = recursive(set, sum, currSum + set[idx], idx-1);
        diff = Math.min(diff, recursive(set, sum, currSum, idx-1));
        return diff;
    }

    int tabulation(int[] ip, int sum) {
        int n = ip.length;
        boolean[][] dp = new boolean[n+1][sum+1];

        for(int i = 0; i <=n; i++)
            dp[i][0] = true;

        for(int i=1; i<=n; i++) {
            for(int j=1; j<=sum; j++) {
                if(j>=ip[i-1])
                    dp[i][j] = dp[i-1][j - ip[i-1]] || dp[i-1][j];
                else
                    dp[i][j] = dp[i-1][j];
            }
        }

        int minDiff = Integer.MAX_VALUE;
        for(int i=0; i<=sum/2; i++) {
            if(dp[n][i])
                minDiff = Math.min(minDiff, sum - 2*i);
        }
        return minDiff;
    }

    int memoization(int[] set, int sum , int currSum, int idx, int dp[][]) {
        if(idx == 0)
            return Math.abs(sum - 2*currSum);

        if(dp[idx][currSum] != -1)
            return dp[idx][currSum];

        
        dp[idx][currSum] = memoization(set, sum, currSum + set[idx], idx-1, dp);
        dp[idx][currSum] = Math.min(dp[idx][currSum], memoization(set, sum, currSum, idx-1, dp));
        return dp[idx][currSum];
    }

    public static void main(String[] args) {
        int[] ip = {1, 6, 11, 14};
        new MinimumSubsetSumDiff(ip, Methods.RECURSION, true);
        new MinimumSubsetSumDiff(ip, Methods.TABULATION, true);
        new MinimumSubsetSumDiff(ip, Methods.MEMOIZATION, true);
    }
}
