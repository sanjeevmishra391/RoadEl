/*
    Given a string of size ‘n’. The task is to remove or delete minimum number of characters from the string so that the resultant string is palindrome.
    Examples :

    Input : aebcbda
    Output : 2
    Remove characters 'e' and 'd'
    Resultant string will be 'abcba'
    which is a palindromic string
 */

package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class MinimumDeletionToMakePalindrome {
    int result;
    Methods method;
    int dp[][];

    MinimumDeletionToMakePalindrome(String s1, String s2, Methods method, boolean use) {
        this.method = method;
        int lps = 0;
        if (method == Methods.RECURSION && use) {
            lps = recursive(s1, s2, s1.length()-1, s2.length()-1);
            result = s1.length() - lps;  
        } else if (method == Methods.TABULATION && use) {
            lps = tabulation(s1, s2);
            result = s1.length() - lps;
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[s1.length() + 1][s2.length()+1];
            for(int row[] : dp)
                Arrays.fill(row, -1);
            lps = memoization(s1, s2, s1.length(), s2.length(), dp);
            result = s1.length() - lps;
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("S1", s1),
                    new LabeledData<>("S2", s2),
                Delimiter.OUTPUT, 
                    new LabeledData<>("Number of deletions to make string palindromic", result));
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
        String s1 = "aebcbda";
        String s2 = new StringBuilder(s1).reverse().toString();
        
        new MinimumDeletionToMakePalindrome(s1, s2, Methods.RECURSION, true);
        new MinimumDeletionToMakePalindrome(s1, s2, Methods.TABULATION, true);
        new MinimumDeletionToMakePalindrome(s1, s2, Methods.MEMOIZATION, true);

    }
}
