package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class LongestCommonSubstring {
    int result;
    Methods method;
    int dp[][];

    LongestCommonSubstring(String s1, String s2, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = helper(s1, s2, s1.length()-1, s2.length()-1, true);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(s1, s2);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[s1.length() + 1][s2.length()+1];
            for(int row[] : dp)
                Arrays.fill(row, -1);
            result = helper(s1, s2, s1.length()-1, s2.length()-1, false);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("S1", s1),
                    new LabeledData<>("S2", s2),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    int helper(String s1, String s2, int m, int n, boolean isRecursion) {
        int ans = 0;

        for(int i=1; i<=m; i++) {
            for(int j=1; j<=m; j++) {
                ans = Math.max(ans, isRecursion ? recursive(s1, s2, i, j) : memoization(s1, s2, i, j));
            }
        }

        return ans;
    }

    int recursive(String s1, String s2, int m, int n) {
        if(m < 0 || n < 0 || s1.charAt(m) != s2.charAt(n))
            return 0;

        return recursive(s1, s2, m-1, n-1) + 1;
    }

    int tabulation(String s1, String s2) {
        int m = s1.length(), n = s2.length(), res = 0;
        int dp[][] = new int[m+1][n+1];

        for(int i=1; i<=m; i++) {
            for(int j=1; j<=n; j++) {
                if(s1.charAt(i-1) == s2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1] + 1;
                    res =  Math.max(res, dp[i][j]);
                } else {
                    dp[i][j] = 0;
                }
            }
        }

        return res;

    }

    int memoization(String s1, String s2, int m, int n) {
        if(m < 0 || n < 0 || s1.charAt(m) != s2.charAt(n))
            return 0;

        if(dp[m][n]!=-1)
            return dp[m][n];

        return dp[m][n] =  memoization(s1, s2, m-1, n-1) + 1;
    }

    public static void main(String[] args) {
        String s1 = "ABDC", s2 = "ABCB";
        
        new LongestCommonSubstring(s1, s2, Methods.RECURSION, true);
        new LongestCommonSubstring(s1, s2, Methods.TABULATION, true);
        new LongestCommonSubstring(s1, s2, Methods.MEMOIZATION, true);
    }
} 
