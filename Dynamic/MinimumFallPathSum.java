
package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

/**
 * MinimumFallPathSum
 */
public class MinimumFallPathSum {
    int result;
    Methods method;
    static int dp[][];

    MinimumFallPathSum(int[][] matrix, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = Integer.MAX_VALUE;
            for(int i=0; i<matrix[0].length; i++)
                result = Math.min(result, recursive(matrix, 0, i, 0));
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(matrix);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[matrix.length][matrix[0].length];

            for(int i=1; i<matrix.length; i++)
                Arrays.fill(dp[i], 20000);

            result= Integer.MAX_VALUE;
            for(int i=0; i<matrix[0].length; i++)
                result = Math.min(result, memoization(matrix, matrix.length-1, i));
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("Matrix", matrix),
                Delimiter.OUTPUT, 
                    new LabeledData<>("Sum", result));
    }

    int recursive(int[][] matrix, int r, int c, int sum) {
        if(r>=matrix.length || c>=matrix[0].length || c<0)
            return Integer.MAX_VALUE;

        if(r == matrix.length - 1)
            return sum + matrix[r][c];

        sum += matrix[r][c];
        int x = recursive(matrix, r + 1, c - 1, sum);
        int y = recursive(matrix, r + 1, c, sum);
        int z = recursive(matrix, r + 1, c + 1, sum);

        return Math.min(x, Math.min(y, z));
    }

    int tabulation(int[][] matrix) {
        int rows = matrix.length, cols = matrix[0].length;
        int dp[][] = new int[rows][cols];

        for(int i=0; i<cols; i++) {
            dp[0][i] = matrix[0][i];
        }

        for(int i=1; i<rows; i++) {
            for(int j=0; j<cols; j++) {
                int a = j > 0 ? dp[i-1][j-1] : Integer.MAX_VALUE; 
                int b = j == cols - 1 ? Integer.MAX_VALUE : dp[i-1][j+1];
                dp[i][j] = Math.min(a, Math.min(dp[i-1][j], b)) + matrix[i][j];
            }
        }

        int res = dp[rows-1][0];
        for(int i=0; i<cols; i++)
            res = Math.min(res, dp[rows-1][i]);

        return res;
    }

    int memoization(int[][] matrix, int r, int c) {
        if(c>=matrix[0].length || c<0 || r<0)
            return Integer.MAX_VALUE;

        if(r == 0) {
            dp[r][c] = matrix[r][c];
            return dp[r][c];
        }

        if(dp[r][c] != 20000)
            return dp[r][c];

        int x = memoization(matrix, r - 1, c - 1);
        int y = memoization(matrix, r - 1, c);
        int z = memoization(matrix, r - 1, c + 1);

        dp[r][c] = Math.min(x, Math.min(y, z)) + matrix[r][c];

        return dp[r][c];
    }

    public static void main(String[] args) {
        int[][] matrix = {{2,1,3}, {6,5,4}, {7,8,9}};
        
        new MinimumFallPathSum(matrix, Methods.RECURSION, true);
        new MinimumFallPathSum(matrix, Methods.TABULATION, true);
        new MinimumFallPathSum(matrix, Methods.MEMOIZATION, true);

        for(int i=0; i<matrix.length; i++) {
            for(int j=0; j<dp[0].length; j++)
                System.out.printf("%3d ", dp[i][j]);

            System.out.println();
        }

    }
        
}