/*
 * Given a rod of length n inches and an array price[]. 
 * price[i] denotes the value of a piece of length i. 
 * 
 * The task is to determine the maximum value obtainable by cutting up the rod and selling the pieces.
 * Input: price[] =  [1, 5, 8, 9, 10, 17, 17, 20]
 * Output: 22
 */

package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class RodCutting {
    int result;
    Methods method;
    int dp[][];

    RodCutting(int[] price, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(price, 1, 0);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(price);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[price.length + 1][price.length+1];
            for(int row[]:dp)
                Arrays.fill(row, -1);
            result = memoization(price, 1, 0, dp);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("price", price),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    int recursive(int[] price, int idx, int currLen) {
        if(idx>=price.length || currLen >= price.length)
            return 0;

        if((price.length - currLen) >= idx) {
            return Math.max(recursive(price, idx+1, currLen), recursive(price, idx, currLen + idx) + price[idx-1]);
        } 

        return recursive(price, idx+1, currLen);
    }

    int tabulation(int[] price) {
        int n = price.length;
        int dp[] = new int[n+1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= i; j++) {
                dp[i] = Math.max(dp[i], price[j - 1] + dp[i - j]);
            }
        }
        
        return dp[n];
    }

    int memoization(int[] price, int idx, int currLen, int[][] dp) {
        if(idx>price.length || currLen >= price.length)
            return 0;

        if(dp[idx-1][currLen]!=-1)
            return dp[idx-1][currLen];

        dp[idx-1][currLen] = memoization(price, idx+1, currLen, dp);

        if((price.length - currLen) >= idx) {
            dp[idx-1][currLen] = Math.max(dp[idx-1][currLen], memoization(price, idx, currLen + idx, dp) + price[idx-1]);
        }
        return dp[idx-1][currLen];
    }

    public static void main(String[] args) {
        int[] price = {1, 5, 8, 9, 10, 17, 17, 20};
        
        new RodCutting(price, Methods.RECURSION, true);
        new RodCutting(price, Methods.TABULATION, true);
        new RodCutting(price, Methods.MEMOIZATION, true);

    }

}
