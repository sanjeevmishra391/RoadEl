package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class PerfectSquares {
    int result;
    Methods method;
    static int dp[];

    PerfectSquares(int n, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(n, 0);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(n);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[n+1];
            Arrays.fill(dp, -1);
            result = memoization(n, 0);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("N", n),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    int recursive(int n, int count) {
        if(n<0) return Integer.MAX_VALUE;
        if(n == 0) return count;
        
        if(Math.ceil(Math.sqrt(n)) == Math.floor(Math.sqrt(n)))
            return count + 1;

        int ans = Integer.MAX_VALUE;
        for(int i=1; i*i<=n; i++)
            ans = Math.min(ans, recursive(n-i*i, count+1));

        return ans;
    }

    int tabulation(int n) {
        int dp[] = new int[n+1];
        dp[1] = 1;
        for(int i=1; i<=n; i++) {
            int ans = Integer.MAX_VALUE;
            for(int j=1; j*j<=i; j++)
                ans = Math.min(ans, dp[i-j*j]);
            dp[i] = ans+1;
        }
        return dp[n];
    }

    int memoization(int n, int count) {
        return 0;
    }

    public static void main(String[] args) {
        int n = 20;
        
        new PerfectSquares(n, Methods.RECURSION, true);
        new PerfectSquares(n, Methods.TABULATION, true);
        new PerfectSquares(n, Methods.MEMOIZATION, false);

    }
}