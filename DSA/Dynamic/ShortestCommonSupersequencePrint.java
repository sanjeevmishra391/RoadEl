package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class ShortestCommonSupersequencePrint {
    int result;
    String scs;
    Methods method;
    int dp[][];

    ShortestCommonSupersequencePrint(String s1, String s2, Methods method, boolean use) {
        this.method = method;
        this.scs = "";

        if (method == Methods.RECURSION && use) {
            result = recursive(s1, s2, s1.length()-1, s2.length()-1);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(s1, s2);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[s1.length() + 1][s2.length()+1];
            for(int row[] : dp)
                Arrays.fill(row, -1);
            result = memoization(s1, s2, s1.length(), s2.length(), dp);
            printSCS(s1, s2, dp);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("S1", s1),
                    new LabeledData<>("S2", s2),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result),
                    new LabeledData<>("SCS", scs));
    }

    int recursive(String s1, String s2, int m, int n) {
        if(m < 0 || n < 0)
            return 0;

        if(s1.charAt(m) == s2.charAt(n)) {
            return recursive(s1, s2, m-1, n-1) + 1;
        }

        return Math.max(recursive(s1, s2, m-1, n), recursive(s1, s2, m, n-1));
    }

    void printSCS(String s1, String s2, int dp[][]) {
        int i = s1.length(), j = s2.length();
        while(i>0 && j>0) {
            if(s1.charAt(i-1) == s2.charAt(j-1)) {
                scs = s1.charAt(i -1) + scs;
                i--;
                j--;
            } else if(dp[i-1][j] > dp[i][j-1]) {
                scs = s1.charAt(i-1) + scs;
                i--;
            } else {
                scs = s2.charAt(j-1) + scs;
                j--;
            }
        }

        while(i>0) {
            scs = s1.charAt(i-1) + scs;
            i--;
        }

        while(j>0) {
            scs = s2.charAt(j-1) + scs;
            j--;
        }
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

        printSCS(s1, s2, dp);

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
        String s1 = "clap", s2 = "slap";
        
        new ShortestCommonSupersequencePrint(s1, s2, Methods.RECURSION, true);
        new ShortestCommonSupersequencePrint(s1, s2, Methods.TABULATION, true);
        new ShortestCommonSupersequencePrint(s1, s2, Methods.MEMOIZATION, true);

    }
}
