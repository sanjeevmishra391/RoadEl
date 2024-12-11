package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class EqualSumPartition {
    boolean result;
    Methods method;
    int dp[][];

    EqualSumPartition(int[] ip, Methods method, boolean use) {
        this.method = method;
        
        result = false;

        int sum = helper(ip);

        if (method == Methods.RECURSION && use) {
            if(sum % 2 == 0)
                result = recursive(ip, sum/2, ip.length-1);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(ip, sum);
        } else if (method == Methods.MEMOIZATION && use) {
            if(sum % 2 == 0) {
                sum = sum/2;
                dp = new int[ip.length + 1][sum+1];
                for(int row[] : dp)
                    Arrays.fill(row, -1);
                result = memoization(ip, sum, ip.length-1, dp);
            }
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

    boolean recursive(int[] ip, int arrSum, int idx) {
        if(idx < 0)
            return false;

        if(arrSum == 0)
            return true;

        if(ip[idx] > arrSum)
            return recursive(ip, arrSum, idx-1);


        return recursive(ip, arrSum - ip[idx], idx-1) || recursive(ip, arrSum, idx-1);
    }

    boolean tabulation(int[] ip, int sum) {
        if(sum % 2 != 0)
            return false;
        sum = sum/2;
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

        return dp[n][sum];

    }

    boolean memoization(int[] ip, int arrSum, int idx, int dp[][]) {
        if(arrSum == 0)
            return true;

        if(idx < 0 ||  arrSum<0)
            return false;


        if(dp[idx][arrSum]!= -1)
            return dp[idx][arrSum] == 1;

        if(ip[idx] > arrSum) 
            dp[idx][arrSum] = memoization(ip, arrSum, idx-1, dp) ? 1 : 0;
        else
            dp[idx][arrSum] = (memoization(ip, arrSum-ip[idx], idx-1, dp) || 
                                memoization(ip, arrSum, idx-1, dp)) ? 1 : 0;

        return dp[idx][arrSum] == 1;
    }

    public static void main(String[] args) {
        int[] ip = {1, 5, 11, 5};
        // int[] ip = {3, 1, 1, 2, 2, 1};

        
        new EqualSumPartition(ip, Methods.RECURSION, true);
        new EqualSumPartition(ip, Methods.TABULATION, true);
        new EqualSumPartition(ip, Methods.MEMOIZATION, true);

    }
}
