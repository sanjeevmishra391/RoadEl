/*
Given two strings ‘str1’ and ‘str2’ of size m and n respectively. The task is to remove/delete and insert minimum number of characters from/in str1 so as to transform it into str2. It could be possible that the same character needs to be removed/deleted from one point of str1 and inserted to some another point.
Example:
Input : str1 = "geeksforgeeks", str2 = "geeks"
Output : Minimum Deletion = 8
         Minimum Insertion = 0 
 */

package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class MinimumInsertionAndDeletion {
    int result, insertions, deletions;
    Methods method;
    int dp[][];

    MinimumInsertionAndDeletion(String s1, String s2, Methods method, boolean use) {
        this.method = method;
        // taking the route from s1 to lcs to s2
        // given len(s1) = m, len(s2) = n
        // len(lcs(s1, s2)) = x
        // # of deletions  = m-x
        // # of insertions = n-x

        if (method == Methods.RECURSION && use) {
            result = recursive(s1, s2, s1.length()-1, s2.length()-1);
            deletions = s1.length() - result;
            insertions = s2.length() - result;
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(s1, s2);
            deletions = s1.length() - result;
            insertions = s2.length() - result;
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[s1.length() + 1][s2.length()+1];
            for(int row[] : dp)
                Arrays.fill(row, -1);
            result = memoization(s1, s2, s1.length(), s2.length(), dp);
            deletions = s1.length() - result;
            insertions = s2.length() - result;
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("S1", s1),
                    new LabeledData<>("S2", s2),
                Delimiter.OUTPUT, 
                    new LabeledData<>("Length of lcs", result),
                    new LabeledData<>("Number of insertions", insertions),
                    new LabeledData<>("Number of deletions", deletions));
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
        String s1 = "heap", s2 = "pea ";
        new MinimumInsertionAndDeletion(s1, s2, Methods.RECURSION, true);
        new MinimumInsertionAndDeletion(s1, s2, Methods.TABULATION, true);
        new MinimumInsertionAndDeletion(s1, s2, Methods.MEMOIZATION, true);

    }
}
