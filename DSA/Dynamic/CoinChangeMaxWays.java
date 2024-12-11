package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class CoinChangeMaxWays {
    int result;
    Methods method;
    static int dp[][];

    CoinChangeMaxWays(int[] coins, int amount, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(coins, amount, 0);
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
                    new LabeledData<>("Max Ways", result)
            );
    }

    int recursive(int[] coins, int amount, int idx) {
        // given coins i can use them multiple times and current amount should not be greater than amount
        if(idx >= coins.length || amount < 0)
            return 0;

        if(amount == 0)
            return 1;
        
        // either use this coin
        int a = recursive(coins, amount - coins[idx], idx);
        int b = recursive(coins, amount, idx + 1);
        return a+b;
    }

    int tabulation(int[] coins, int amount) {
        int n = coins.length;
        int[][] dp = new int[n+1][amount+1];
        for(int r = 1; r <= n; r++) {
            for(int c = 1; c <= amount; c++) {
                dp[r][c] = dp[r-1][c];
                if(c >= coins[r-1]) {
                    dp[r][c] = Math.max(dp[r][c], dp[r][c-coins[r-1]]+1);
                }
            }
        }

        return dp[n][amount];
    }

    int memoization(int[] coins, int amount, int idx) {
        if(idx >= coins.length || amount < 0) return 0;

        if(amount == 0) return 1;

        if(dp[idx][amount] != -1) return dp[idx][amount];

        int a = memoization(coins, amount-coins[idx], idx);
        int b = memoization(coins, amount, idx+1);
        dp[idx][amount] = a+b;
        
        return dp[idx][amount];
    }

    public static void main(String[] args) {
        int[] coins = {1, 2, 3};
        int amount = 4;
        
        new CoinChangeMaxWays(coins, amount, Methods.RECURSION, true);
        new CoinChangeMaxWays(coins, amount, Methods.TABULATION, true);
        new CoinChangeMaxWays(coins, amount, Methods.MEMOIZATION, true);

    }
}
