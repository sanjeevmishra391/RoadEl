package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class PalindromePartitioning {
    int result;
    Methods method;
    int dp[][];

    PalindromePartitioning(String s, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(s, 0, s.length()-1);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(s);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[s.length() + 1][s.length() + 1];
            for(int row[] : dp)
                Arrays.fill(row, -1);
            result = memoization(s, 0, s.length()-1, dp);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("String", s),
                Delimiter.OUTPUT, 
                    new LabeledData<>("Minimum number of paritions required", result));
    }

    boolean isPalindrome(String s, int i, int j) {
        while(i<j) {
            if(s.charAt(i) != s.charAt(j))
                return false;

            i++;
            j--;
        }

        return true;
    }

    int recursive(String s, int i, int j) {
        if(i + 1 == j || isPalindrome(s, i, j))
            return 0;

        int ans = Integer.MAX_VALUE;

        for(int k=i+1; k<j; k++) {
            int curr = recursive(s, i, k) + recursive(s, k, j) + 1;
            ans = Math.min(ans, curr);
        }

        return ans;
    }

    int tabulation(String s) {
        int n = s.length();

        // Step 1: Precompute palindrome table
        boolean[][] isPalindrome = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            isPalindrome[i][i] = true; // Single character is always a palindrome
        }
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                if (s.charAt(i) == s.charAt(j)) {
                    isPalindrome[i][j] = len == 2 || isPalindrome[i + 1][j - 1];
                }
            }
        }

        // Step 2: DP to find minimum cuts
        int[] dp = new int[n];
        Arrays.fill(dp, Integer.MAX_VALUE);

        for (int i = 0; i < n; i++) {
            if (isPalindrome[0][i]) {
                dp[i] = 0; // No cuts needed for a palindrome substring
            } else {
                for (int j = 0; j < i; j++) {
                    if (isPalindrome[j + 1][i]) {
                        dp[i] = Math.min(dp[i], dp[j] + 1);
                    }
                }
            }
        }

        return dp[n - 1];
    }

    int memoization(String s, int i, int j, int dp[][]) {
        if(i + 1 == j || isPalindrome(s, i, j))
            return 0;

        if(dp[i][j] != -1)
            return dp[i][j];

        int res = Integer.MAX_VALUE;

        for(int k=i+1; k<j; k++) {
            int curr = memoization(s, i, k, dp) + memoization(s, k, j, dp) + 1;
            res = Math.min(curr, res);
        }

        return dp[i][j] = res;
    }

    public static void main(String[] args) {
        String s = "ababbbabbababa";
        
        new PalindromePartitioning(s, Methods.RECURSION, true);
        new PalindromePartitioning(s, Methods.TABULATION, true);
        new PalindromePartitioning(s, Methods.MEMOIZATION, true);

    }
}
