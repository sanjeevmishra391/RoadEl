package Dynamic;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Methods;
import roadel.Utility.Delimiter;

public class SubsetSum {
    boolean result;
    Methods method;
    int dp[];

    SubsetSum(int[] ip, int sum, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(ip, 0, sum, 0);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(ip, sum, ip.length);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[ip.length + 1];
            result = memoization(ip, sum);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("ip", ip),
                    new LabeledData<>("sum", sum),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    boolean recursive(int[] ip, int idx, int sum, int totalSum) {
        if(idx>=ip.length || totalSum > sum) return false;
        if(sum == totalSum) return true;
        return recursive(ip, idx+1, sum, totalSum + ip[idx]) || recursive(ip, idx+1, sum, totalSum);
    }

    boolean tabulation(int[] set, int sum, int n) {
        // The value of subset[i][j] will be
        // true if there is a subset of
        // set[0..j-1] with sum equal to i
        boolean subset[][] = new boolean[sum + 1][n + 1];

        // If sum is 0, then answer is true
        for (int i = 0; i <= n; i++)
            subset[0][i] = true;

        // If sum is not 0 and set is empty,
        // then answer is false
        for (int i = 1; i <= sum; i++)
            subset[i][0] = false;

        // Fill the subset table in bottom
        // up manner
        for (int i = 1; i <= sum; i++) {
            for (int j = 1; j <= n; j++) {
                subset[i][j] = subset[i][j - 1];
                if (i >= set[j - 1])
                    subset[i][j]
                        = subset[i][j]
                          || subset[i - set[j - 1]][j - 1];
            }
        }

        return subset[sum][n];
    }

    boolean memoization(int[] ip, int sum) {
        return false;
    }

    public static void main(String[] args) {
        int[] ip = {3, 34, 4, 12, 5, 2};
        int sum = 9;
        
        new SubsetSum(ip, sum, Methods.RECURSION, true);
        new SubsetSum(ip, sum, Methods.TABULATION, true);
        new SubsetSum(ip, sum, Methods.MEMOIZATION, false);

    }
}
