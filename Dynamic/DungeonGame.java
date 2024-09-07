package Dynamic;
/* 
 * The demons had captured the princess and imprisoned her in the bottom-right corner of a dungeon. 
 * The dungeon consists of m x n rooms laid out in a 2D grid. Our valiant knight was initially positioned in the top-left room and must fight his way through dungeon to rescue the princess.
 * The knight has an initial health point represented by a positive integer. If at any point his health point drops to 0 or below, he dies immediately.
 * Some of the rooms are guarded by demons (represented by negative integers), so the knight loses health upon entering these rooms; other rooms are either empty (represented as 0) or contain magic orbs that increase the knight's health (represented by positive integers).
 * To reach the princess as quickly as possible, the knight decides to move only rightward or downward in each step.
 * Return the knight's minimum initial health so that he can rescue the princess.
 * Note that any room can contain threats or power-ups, even the first room the knight enters and the bottom-right room where the princess is imprisoned.
 */

import java.util.Arrays;

public class DungeonGame {
    int result;
    int dp[][];

    DungeonGame(int[][] dungeon, String method) {
        switch (method) {
            case "recursive":
                result = recursive(dungeon, 0, 0, 0, Integer.MAX_VALUE);
                result = result < 0 ? -result + 1 : 1;
                break;
            case "recursive2":
                result = recursive2(dungeon, 0, 0);
                break;
            case "tabulationDP":
                result = tabulationDP(dungeon);
                break;
            case "memoizationDP":
                dp = new int[dungeon.length][dungeon[0].length];
                for(int i=0; i<dungeon.length; i++)    
                    Arrays.fill(dp[i], -1);
                result = memoizationDP(dungeon);
                break;
            default:
                break;
        }

        printRes(dungeon, result, method);
    }

    int recursive(int[][] dungeon, int r, int c, int pathSum, int minSoFar) {
        if(r>=dungeon.length || c>=dungeon[0].length)
            return Integer.MIN_VALUE;
        
        pathSum += dungeon[r][c];
        minSoFar = Math.min(minSoFar, pathSum);
        if(r == dungeon.length-1 && c == dungeon[0].length-1)
            return minSoFar;

        
        return Math.max(recursive(dungeon, r+1, c, pathSum, minSoFar), recursive(dungeon, r, c+1, pathSum, minSoFar));
    }

    int recursive2(int[][] dungeon, int r, int c) {
        if(r>=dungeon.length || c>=dungeon[0].length)
            return Integer.MAX_VALUE;
        
        if(r == dungeon.length-1 && c == dungeon[0].length-1)
            return dungeon[r][c] <= 0 ? -dungeon[r][c] + 1 : 1;

        
        
        int minSo = Math.min(recursive2(dungeon, r+1, c), recursive2(dungeon, r, c+1)) - dungeon[r][c];

        return minSo <= 0 ? 1 : minSo;
    }

    int tabulationDP(int[][] dungeon) {
        int r = dungeon.length, c = dungeon[0].length;
        int dp[][] = new int[r+1][c+1];
        
        for(int row[] : dp) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }
        
        dp[r][c-1] = 1;
        dp[r-1][c] = 1;

        for(int i=r-1; i>=0; i--) {
            for(int j=c-1; j>=0; j--) {
                int need = Math.min(dp[i+1][j], dp[i][j+1]) - dungeon[i][j];
                dp[i][j] = need <= 0 ? 1 : need;
            }
        }
        return dp[0][0];
    }

    // INCOMPLETE
    int memoizationDP(int[][] dungeon) {
        return 0;
    }

    void printRes(int[][] grid, int res, String method) {
        System.out.println("Method called : " + method);
        System.out.println("Input:");
        for(int[] r: grid) {
            for(int c : r) {
                System.out.printf("%4d ", c);
            }
            System.out.print("\n");
        }

        System.out.println("\n" + "Output: " + result + "\n");
    }

    public static void main(String[] args) {
        int[][] dungeon = {{-2,-3,3},{-5,-10,1},{10,30,-5}};
        new DungeonGame(dungeon, "recursive");
        new DungeonGame(dungeon, "recursive2");
        new DungeonGame(dungeon, "tabulationDP");
        // new DungeonGame(dungeon, "memoizationDP");
    }
}
