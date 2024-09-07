/*
    There is only one character 'A' on the screen of a notepad. You can perform one of two operations on this notepad for each step:

    Copy All: You can copy all the characters present on the screen (a partial copy is not allowed).
    Paste: You can paste the characters which are copied last time.
    Given an integer n, return the minimum number of operations to get the character 'A' exactly n times on the screen.
 */

package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class TwoKeysKeyboard {
    int result;
    Methods method;
    int dp[];
    static int MAX = Integer.MAX_VALUE;

    TwoKeysKeyboard(int n, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(n, 1, 1, false, 1);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(n);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[n + 1];
            Arrays.fill(dp, -1);
            dp[0] = 0;
            result = memoization(n);
        } else {
            result = minSteps(n);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("N", n),
                Delimiter.OUTPUT, 
                    new LabeledData<>("Result", result));
    }
    
    int recursive(int n, int copyCount, int curr, boolean lastActionCopy, int res) {
        if(n==1) return 0;

        if(curr > n) return MAX;

        if(curr == n) return res;

        int a = MAX, b = MAX, c = MAX;
        if(lastActionCopy) {
            // paste
            a = recursive(n, copyCount, curr + copyCount, false, res+1);
        } else if(!lastActionCopy && n >= curr + copyCount) {
            // if last action was paste then you can paste again or copy new string
            // copy
            b = recursive(n, curr, curr, true, res+1);
            // paste
            c = recursive(n, copyCount, curr + copyCount, false, res+1);
        }

        return Math.min(a, Math.min(b, c));
    }

    @Deprecated
    int tabulation(int n) {
        return 0;
    }

    @Deprecated
    int memoization(int n) {
        return 0;
    }

    // finding prime factors of the n and adding primes together
    int minSteps(int n) {      
        if(n==1) return 0;

        int factor = 2, ans = 0;
        while(n > 1) {
            while(n % factor == 0) {
                ans += factor;
                n = n / factor;
            }

            factor++;
        }

        return ans; 
    }

    public static void main(String[] args) {
        int n = 10;
        
        new TwoKeysKeyboard(n, Methods.RECURSION, true);
        new TwoKeysKeyboard(n, Methods.TABULATION, true);
        new TwoKeysKeyboard(n, Methods.MEMOIZATION, false);
        new TwoKeysKeyboard(n, Methods.OTHER, true);

    }
}
