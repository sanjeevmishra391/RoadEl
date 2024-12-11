package Dynamic;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class NumberOfSubsetsWithDiff {
    int result;
    Methods method;
    int dp[];

    NumberOfSubsetsWithDiff(int[] ip, int diff, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(ip, diff);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(ip, diff);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[ip.length + 1];
            result = memoization(ip);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("Set", ip),
                    new LabeledData<>("Difference", diff),

                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    int helper(int ip[]) {
        int sum = 0;
        for(int a : ip)
            sum += a;

        return sum;
    }

    int recursive(int[] ip, int diff) {
        return 0;
    }

    int tabulation(int[] ip, int diff) {
        // s1 + s2 = sum  -- 1
        // s1 - s2 = diff -- 2
        // adding 1 and 2
        // 2*s1 = sum + diff -- reduced to subset sum problem

        int n = ip.length, sum = helper(ip);
        sum = diff + sum;
        if(sum%2!=0)
            return 0;
        sum = sum/2;
        int dp[][] = new int[n+1][sum+1];

        // If sum is 0, then answer is true
        for (int i = 0; i <= n; i++)
            dp[i][0] = 1;

        for(int i=1; i<=n; i++) {
            for(int j=1; j<=sum; j++) {
                dp[i][j] = dp[i-1][j];
                if(j>=ip[i-1]) {
                    dp[i][j] = dp[i][j] + dp[i-1][j-ip[i-1]];
                }
            }
        }

        return dp[n][sum];
    }

    int memoization(int[] ip) {
        return 0;
    }

    public static void main(String[] args) {
        int[] ip = {1,1,2,3};
        int diff = 1;
        
        new NumberOfSubsetsWithDiff(ip, diff, Methods.RECURSION, false);
        new NumberOfSubsetsWithDiff(ip, diff, Methods.TABULATION, true);
        new NumberOfSubsetsWithDiff(ip, diff, Methods.MEMOIZATION, false);

    }

}
