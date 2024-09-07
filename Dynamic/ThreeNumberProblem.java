package Dynamic;
/* 
Given 3 numbers {1, 3, 5}, the task is to tell the total number of ways we can form a number N using the sum of the given three numbers. 
(allowing repetitions and different arrangements).
 */

import java.util.Arrays;

public class ThreeNumberProblem {

    int result;
    int dp[];
    ThreeNumberProblem(int n, String method) {
        switch (method) {
            case "recursive":
                result = recursive(n);
                break;
            case "tabulationDP":
                result = tabulationDP(n);
                break;
            case "memoizationDP":
                dp = new int[n+1];
                Arrays.fill(dp, -1);
                result = memoizationDP(n);
                break;
            default:
                break;
        }

        printRes(n, result, method);
    }

    int recursive(int n) {
        if(n<0) return 0;
        if(n==0) return 1;

        return recursive(n-1) + recursive(n-3) + recursive(n-5);
    }

    int tabulationDP(int n) {
        int dp[] = new int[n+1];
        dp[0] = 1;
        dp[1] = 1;

        for(int i=2; i<=n; i++) {
            dp[i] = dp[i-1] + (i>=3 ? dp[i-3] : 0) + (i>=5 ? dp[i-5] : 0);
        }

        return dp[n];
    }

    int memoizationDP(int n) {
        if(n < 0)
            return 0;

        if(n == 0)
            return 1;
        
        if(dp[n] != -1)
            return dp[n];

        dp[n] = memoizationDP(n-1) + memoizationDP(n-3) + memoizationDP(n-5);
        return dp[n];
    }

    void printRes(int n, int res, String method) {
        System.out.println("Method called : " + method);
        System.out.println("Input : " + n + "\n" + "Output : " + result + "\n");
    }

    public static void main(String[] args) {
        int n = 7;
        new ThreeNumberProblem(n, "recursive");
        new ThreeNumberProblem(n, "tabulationDP");
        new ThreeNumberProblem(n, "memoizationDP");
    }
}
