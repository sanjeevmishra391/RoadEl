// https://www.geeksforgeeks.org/program-nth-catalan-number/#
/*  
    1. Count the number of expressions containing n pairs of parentheses that are correctly matched. For n = 3, possible expressions are ((())), ()(()), ()()(), (())(), (()()).
    2. Count the number of possible Binary Search Trees with n keys.
    3. Count the number of full binary trees (A rooted binary tree is full if every vertex has either two children or no children) with n+1 leaves.
    4. Given a number n, return the number of ways you can draw n chords in a circle with 2 x n points such that no 2 chords intersect.
    Catalan numbers satisfy the following recursive formula: C_0=C_1=1 \ and \ C_{n}=\sum_{i=0}^{n-1}C_iC_{n-i-1} \ for \ n\geq 2
 */

package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Methods;
import roadel.Utility.Delimiter;


public class Catalan {
    long result;
    Methods method;
    long dp[];

    Catalan(int ip, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(ip);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(ip);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new long[ip+1];
            Arrays.fill(dp, -1);
            result = memoization(ip);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("ip", ip),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    long recursive(int ip) {
        if(ip<=1) return 1;

        long res = 0;
        for(int i=0; i<ip; i++) {
            res += recursive(i) * recursive(ip - i - 1);
        }

        return res;
    }

    long tabulation(int ip) {
        long dp[] = new long[ip+1];
        dp[0] = 1;
        dp[1] = 1;

        for(int i=2; i<=ip; i++) {
            long res = 0;
            for(int j=0; j<i; j++) {
                res += dp[j] * dp[i-j-1];
            }

            dp[i] = res;
        }

        return dp[ip];
    }

    long memoization(int ip) {
        if(ip<=1) return 1;

        if(dp[ip] != -1) return dp[ip];

        long res = 0;
        for(int i=0; i<ip; i++) {
            res += memoization(i) * memoization(ip - i - 1);
        }

        dp[ip] = res;

        return res;
    }

    public static void main(String[] args) {
        int ip = 8;
        
        new Catalan(ip, Methods.RECURSION, true);
        new Catalan(ip, Methods.TABULATION, true);
        new Catalan(ip, Methods.MEMOIZATION, true);

    }
}
