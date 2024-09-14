/*
    You are given an integer array coins representing coins of different denominations and an integer amount representing a total amount of money.

    Return the fewest number of coins that you need to make up that amount. If that amount of money cannot be made up by any combination of the coins, return -1.

    You may assume that you have an infinite number of each kind of coin.
 */

package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Methods;
import roadel.Utility.Delimiter;

public class CoinChange {
    int result;
    Methods method;
    static int dp[][];

    CoinChange(int[] coins, int amount, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(coins, amount, 0, 0);
            result = result == Integer.MAX_VALUE ? -1 : result;
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(coins, amount);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[coins.length + 1][amount+1];

            for(int row = 0; row <= coins.length ; row++)
                Arrays.fill(dp[row], -1);
            
            result = memoization(coins, amount, 0);
            result = result == Integer.MAX_VALUE ? -1 : result;
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("Coins", coins),
                    new LabeledData<>("Amount", amount),
                Delimiter.OUTPUT, 
                    new LabeledData<>("Coins Needed", result)
            );
    }

    int recursive(int[] coins, int amount, int idx, int count) {
        // given coins i can use them multiple times and current amount should not be greater than amount
        if(idx >= coins.length || amount < 0)
            return Integer.MAX_VALUE;

        if(amount == 0) return count;

        // either use this coin
        int a = recursive(coins, amount - coins[idx], idx, count+1);
        int b = recursive(coins, amount, idx + 1, count);
        return Math.min(a, b);
    }

    int tabulation(int[] coins, int amount) {
        int[][] dp = new int[coins.length+1][amount+1];

        for(int col = 1; col <= amount ; col++) {
            dp[0][col] = Integer.MAX_VALUE;
        }

        for(int r = 1; r < dp.length; r++) {
            for(int c = 1; c < dp[0].length; c++) {
                dp[r][c] = Math.min(dp[r-1][c], 
                                    c-coins[r-1] >= 0 ? (dp[r][c - coins[r-1]] == Integer.MAX_VALUE ? dp[r][c - coins[r-1]] : dp[r][c - coins[r-1]] + 1) : Integer.MAX_VALUE);
            }
        }

        return dp[coins.length][amount] == Integer.MAX_VALUE ? -1 : dp[coins.length][amount];
    }

    int memoization(int[] coins, int amount, int idx) {
        if(idx >= coins.length || amount < 0) return Integer.MAX_VALUE;

        if(amount == 0) return dp[idx][amount] + 1;

        if(dp[idx][amount] != -1) return dp[idx][amount];

        int a = memoization(coins, amount-coins[idx], idx);
        a = a == Integer.MAX_VALUE ? Integer.MAX_VALUE : a+1;
        int b = memoization(coins, amount, idx+1);

        dp[idx][amount] = Math.min(a, b);

        return dp[idx][amount];
    }

    public static void main(String[] args) {
        int[] coins = {2, 3, 5};
        int amount = 6;
        
        new CoinChange(coins, amount, Methods.RECURSION, false);
        new CoinChange(coins, amount, Methods.TABULATION, true);
        new CoinChange(coins, amount, Methods.MEMOIZATION, true);

    }
}
