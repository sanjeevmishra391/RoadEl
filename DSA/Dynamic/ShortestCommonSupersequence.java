/* 
Given two strings str1 and str2, find the shortest string that has both str1 and str2 as subsequences.
Examples:

Input:   str1 = "geek",  str2 = "eke"
Output: "geeke"
 */

package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class ShortestCommonSupersequence {
    int result;
    Methods method;
    int dp[][];

    ShortestCommonSupersequence(String s1, String s2, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(s1, s2, s1.length()-1, s2.length()-1);
            // len(s1) + len(s2) - len(lcs(s1, s2))
            result = s1.length() + s2.length() - result; 
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(s1, s2);
            result = s1.length() + s2.length() - result;
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[s1.length() + 1][s2.length()+1];
            for(int row[] : dp)
                Arrays.fill(row, -1);
            result = memoization(s1, s2, s1.length(), s2.length(), dp);
            result = s1.length() + s2.length() - result;
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("S1", s1),
                    new LabeledData<>("S2", s2),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    int recursive(String s1, String s2, int m, int n) {
        if(m < 0 || n < 0)
            return 0;

        if(s1.charAt(m) == s2.charAt(n)) {
            return recursive(s1, s2, m-1, n-1) + 1;
        }

        return Math.max(recursive(s1, s2, m-1, n), recursive(s1, s2, m, n-1));
    }

    int tabulation(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int dp[][] = new int[m+1][n+1];

        for(int i=1; i<=m; i++) {
            for(int j=1; j<=n; j++) {
                if(s1.charAt(i-1) == s2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
                }

            }
        }

        return dp[m][n];

    }

    int memoization(String s1, String s2, int m, int n, int dp[][]) {
        if(m <= 0 || n <= 0)
            return 0;

        if(dp[m][n]!=-1)
            return dp[m][n];

        if(s1.charAt(m-1) == s2.charAt(n-1)) {
            return dp[m][n] =  memoization(s1, s2, m-1, n-1, dp) + 1;
        }

        return dp[m][n] = Math.max(memoization(s1, s2, m-1, n, dp), memoization(s1, s2, m, n-1, dp));
    }

    public static void main(String[] args) {
        String s1 = "ABC", s2 = "ACB"; // 1 > C, 2 > B 
        
        new ShortestCommonSupersequence(s1, s2, Methods.RECURSION, true);
        new ShortestCommonSupersequence(s1, s2, Methods.TABULATION, true);
        new ShortestCommonSupersequence(s1, s2, Methods.MEMOIZATION, true);

    }
}
