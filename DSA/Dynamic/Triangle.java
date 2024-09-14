package Dynamic;
/* 
 * Given a triangle array, return the minimum path sum from top to bottom.
 * For each step, you may move to an adjacent number of the row below. 
 * More formally, if you are on index i on the current row, you may move to either index i or index i + 1 on the next row.
 */

import java.util.Arrays;

public class Triangle {
    int result;
    int dp[][];

    Triangle(int[][] tr, String method) {
        switch (method) {
            case "recursive":
                result = recursive(tr, 0, 0, 0);
                break;
            case "tabulationDP":
                result = tabulationDP(tr);
                break;
            case "memoizationDP":
                dp = new int[tr.length][];
                for(int i=0; i<tr.length; i++) { 
                    dp[i] = new int[tr[i].length];
                    Arrays.fill(dp[i], -1);
                }
                result = memoizationDP(tr, 0, 0);
                break;
            default:
                break;
        }

        printRes(tr, result, method);
    }

    int recursive(int tr[][], int r, int c, int pathSum) {
        // out of range
        if(r>=tr.length || c>=tr[r].length)
            return Integer.MAX_VALUE;

        // base case
        if(r==tr.length-1) {
            System.out.println("L "+ r +" "+c+" "+ pathSum);
            return pathSum + tr[r][c];
        }

        // subproblem
        pathSum += tr[r][c];
        return Math.min(recursive(tr, r+1, c, pathSum), recursive(tr, r+1, c+1, pathSum));
    }

    int tabulationDP(int[][] tr) {
        int rows = tr.length;
        int dp[][] = new int[rows][];

        dp[0] = new int[1];
        dp[0][0] = tr[0][0];

        for(int r = 1; r < rows; r++) {
            dp[r] = new int[tr[r].length];
            dp[r][0] = dp[r-1][0] + tr[r][0];
        }

        for(int r=1; r<rows; r++) {
            for(int c=1; c<tr[r].length; c++) {
                dp[r][c] = Math.min(c < tr[r-1].length  ? dp[r-1][c] : Integer.MAX_VALUE, dp[r-1][c-1]) + tr[r][c];
            }
        }

        int min = Integer.MAX_VALUE;
        for(int i=0; i<tr[rows-1].length; i++) {
            min = Math.min(min, dp[rows-1][i]);
        }

        return min;
    }

    int memoizationDP(int[][] tr, int r, int c) {
        if(r>=tr.length || c>=tr[r].length)
            return Integer.MAX_VALUE;

        if(r == tr.length-1) {
            return tr[r][c];
        }

        if(dp[r][c] != -1) return dp[r][c];

        dp[r][c] = Math.min(memoizationDP(tr, r+1, c), memoizationDP(tr, r+1, c+1)) + tr[r][c];

        return dp[r][c];
    }

    void printRes(int[][] grid, int res, String method) {
        System.out.println("Method called : " + method);
        System.out.print("Input:  [");
        for(int[] r: grid) { 
                System.out.print("[");
            for(int c : r) {
                System.out.print(c + ", ");
            }
            System.out.print("],");
        }
        System.out.print("]");

        System.out.println("\n" + "Output : " + result + "\n");
    }

    public static void main(String[] args) {
        int[][] tr = {{2},{3,4},{6,5,7},{4,1,8,3}};
        // new Triangle(tr, "recursive");
        // new Triangle(tr, "tabulationDP");
        new Triangle(tr, "memoizationDP");
    }
}
