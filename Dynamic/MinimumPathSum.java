package Dynamic;
/* 
 * Given a m x n grid filled with non-negative numbers, find a path from top left to bottom right, which minimizes the sum of all numbers along its path.
 * Note: You can only move either down or right at any point in time.
 * 
 * Input: grid = [[1,3,1],[1,5,1],[4,2,1]]
 * Output: 7
 */

import java.io.Serializable;
import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.*;

class MinimumPathSum {
    int result;
    Methods method;
    int dp[][];

    MinimumPathSum(int[][] grid, Methods method, boolean use) {
        this.method = method;

        if(method == Methods.RECURSION && use) {
            result = recursive(grid, 0, 0, 0);
        } else if(method == Methods.TABULATION && use) {
            result = tabulationDP(grid);
        } else if(method == Methods.MEMOIZATION && use) {
            dp = new int[grid.length][grid[0].length];
            for(int i=0; i<grid.length; i++)    
                Arrays.fill(dp[i], -1);
            result = memoizationDP(grid, 0, 0);
        }

        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("Grid", (Serializable) grid),
                Delimiter.OUTPUT, 
                    new LabeledData<>("Path Sum", result));
    }

    int recursive(int[][] grid, int pathSum, int r, int c) {
        // rows and cols out of range
        if(r>=grid.length || c>=grid[0].length)
            return Integer.MAX_VALUE;

        // reached the bottom right cell
        if(r == grid.length-1 && c == grid[0].length-1)
            return pathSum + grid[r][c];

        pathSum += grid[r][c];
        return Math.min(recursive(grid, pathSum, r+1, c), recursive(grid, pathSum, r, c+1));
    }

    int tabulationDP(int[][] grid) {
        int r = grid.length, c = grid[0].length;
        int dp[][] = new int[r][c];
        dp[0][0] = grid[0][0];
        for(int i=1; i<r; i++) {
            dp[i][0] = dp[i-1][0] + grid[i][0];
        }

        for(int j=1; j<c; j++) 
            dp[0][j] = dp[0][j-1] + grid[0][j];
        
        for(int i=1; i<r; i++) {
            for(int j=1; j<c; j++) {
                dp[i][j] = Math.min(dp[(i-1)][j], dp[i][(j-1)]) + grid[i][j];
            }
        }
        return dp[r-1][c-1];
    }

    int memoizationDP(int[][] grid, int r, int c) { 
        // rows and cols out of range
        if(r>=grid.length || c>=grid[0].length)
            return Integer.MAX_VALUE;

        // reached the bottom right cell
        if(r == grid.length-1 && c == grid[0].length-1) {
            dp[r][c] = grid[r][c];
            return dp[r][c];
        }

        if(dp[r][c] != -1)  return dp[r][c];
        
        dp[r][c] = Math.min(memoizationDP(grid, r+1, c), memoizationDP(grid, r, c+1)) + grid[r][c];
  
        return dp[r][c];
    }
    public static void main(String[] args) {
        int[][] grid = {{1,3,1},{1,5,1},{4,2,1}};
        // int[][] grid = {{1,2,3},{4,5,6}};
        new MinimumPathSum(grid, Methods.RECURSION, true);
        new MinimumPathSum(grid, Methods.TABULATION, false);
        new MinimumPathSum(grid, Methods.MEMOIZATION, false);
    }
}