/*
Given a string s, the task is to find the length of the longest repeating subsequence, such that the two subsequences don’t have the same string character at the same position, i.e. any ith character in the two subsequences shouldn’t have the same index in the original string. 

Examples:
Input: s= “abc”
Output: 0
Explanation: There is no repeating subsequence

Input: s= “aab”
Output: 1
 */

package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class LongestRepeatingSubsequence {
    int result;
    Methods method;
    int dp[][];

    LongestRepeatingSubsequence(String s1, Methods method, boolean use) {
        this.method = method;

        String s2 = s1;

        if (method == Methods.RECURSION && use) {
            result = recursive(s1, s2, s1.length()-1, s2.length()-1);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(s1, s2);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[s1.length() + 1][s2.length()+1];
            for(int row[] : dp)
                Arrays.fill(row, -1);
            result = memoization(s1, s2, s1.length(), s2.length(), dp);
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

        if(s1.charAt(m) == s2.charAt(n) && m != n) { 
            return recursive(s1, s2, m-1, n-1) + 1;
        }

        return Math.max(recursive(s1, s2, m-1, n), recursive(s1, s2, m, n-1));
    }

    int tabulation(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int dp[][] = new int[m+1][n+1];

        for(int i=1; i<=m; i++) {
            for(int j=1; j<=n; j++) {
                if(s1.charAt(i-1) == s2.charAt(j-1) && i != j) {
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

        if(s1.charAt(m-1) == s2.charAt(n-1) && m != n) {
            return dp[m][n] =  memoization(s1, s2, m-1, n-1, dp) + 1;
        }

        return dp[m][n] = Math.max(memoization(s1, s2, m-1, n, dp), memoization(s1, s2, m, n-1, dp));
    }

    public static void main(String[] args) {
        String s1 = "AABEBCDD";
        
        new LongestRepeatingSubsequence(s1, Methods.RECURSION, true);
        new LongestRepeatingSubsequence(s1, Methods.TABULATION, true);
        new LongestRepeatingSubsequence(s1, Methods.MEMOIZATION, true);

    }
}
