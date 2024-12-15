/*
    Given boolean expression with T and F and three operators (&, |, ^),
    count the number of ways we can parenthesize the expression so that the value of expression evaluates to true. 
 */

package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class EvaluationExpressionToTrue {
    int result;
    Methods method;
    int dp[][][];

    EvaluationExpressionToTrue(String s, Methods method, boolean use) {
        this.method = method;

        int N = s.length();

        if (method == Methods.RECURSION && use) {
            result = recursive(s, 0, N-1);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(s);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[N + 1][N + 1][2];
            for(int row[][] : dp)
                for(int col[] : row)
                    Arrays.fill(col, -1);
            result = memoization(s, 0, N-1, 1, dp);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("String", s),
                Delimiter.OUTPUT, 
                    new LabeledData<>("Op", result));
    }

    int recursive(String s, int i, int j) {
        return -1;
    }

    int tabulation(String s) {
        return 0;
    }

    int memoization(String s, int i, int j, int isTrue, int dp[][][]) {
        if(i > j)
            return 0;

        if(i == j) {
            if(isTrue == 1) {
                return (s.charAt(i) == 'T') ? 1 : 0;
            } else {
                return (s.charAt(i) == 'F') ? 1 : 0;
            }
        }

        if(dp[i][j][isTrue] != -1)
            return dp[i][j][isTrue];

        int temp = 0;
        int leftTrue, rightTrue, leftFalse, rightFalse;

        for (int k = i + 1; k <= j - 1; k = k + 2) {
 
            if (dp[i][k - 1][1] != -1)
                leftTrue = dp[i][k - 1][1];
            else {
                // Count number of True in left Partition
                leftTrue = memoization(s, i, k - 1, 1, dp);
            }

            if (dp[i][k - 1][0] != -1)
                leftFalse = dp[i][k - 1][0];
            else {
                // Count number of False in left Partition
                leftFalse = memoization(s, i, k - 1, 0, dp);
            }

            if (dp[k + 1][j][1] != -1)
                rightTrue = dp[k + 1][j][1];
            else {
                // Count number of True in right Partition
                rightTrue = memoization(s, k + 1, j, 1, dp);
            }

            if (dp[k + 1][j][0] != -1)
                rightFalse = dp[k + 1][j][0];
            else {
                // Count number of False in right Partition
                rightFalse = memoization(s, k + 1, j, 0, dp);
            }
 
            // Evaluate AND operation
            if (s.charAt(k) == '&') {
                if (isTrue == 1) {
                    temp = temp + leftTrue * rightTrue;
                } else {
                    temp = temp
                               + leftTrue * rightFalse
                               + leftFalse * rightTrue
                               + leftFalse * rightFalse;
                }
            }
            // Evaluate OR operation
            else if (s.charAt(k) == '|') {
                if (isTrue == 1) {
                    temp = temp
                               + leftTrue * rightTrue
                               + leftTrue * rightFalse
                               + leftFalse * rightTrue;
                } else {
                    temp
                        = temp + leftFalse * rightFalse;
                }
            }
           
            // Evaluate XOR operation
            else if (s.charAt(k) == '^') {
                if (isTrue == 1) {
                    temp = temp
                               + leftTrue * rightFalse
                               + leftFalse * rightTrue;
                } else {
                    temp = temp
                               + leftTrue * rightTrue
                               + leftFalse * rightFalse;
                }
            }
            dp[i][j][isTrue] = temp;
        }

        return temp;
    }

    public static void main(String[] args) {
        String s = "T|T&F^T";
        
        new EvaluationExpressionToTrue(s, Methods.RECURSION, true);
        // new EvaluationExpressionToTrue(s, Methods.TABULATION, true);
        new EvaluationExpressionToTrue(s, Methods.MEMOIZATION, true);

    }
}
