package Backtracking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NQueens {

    static List<List<String>> res;

    static boolean solve(char[][] board, int row) {
        if(row == board.length) {
            res.add(new ArrayList<>());
            for(int i=0; i<board.length; i++) {
                StringBuilder sb = new StringBuilder();
                for(int j=0; j<board.length; j++) {
                    sb.append(board[i][j]);
                }
                res.get(res.size()-1).add(sb.toString());
            }
            return true;
        }

        for(int i=0; i<board[0].length; i++) {
            if(isSafe(board, row, i)) {
                board[row][i] = 'Q';

                solve(board, row+1);

                board[row][i] = '.';
            }
        }

        return false;
    }

    static void solveNQueens(int n) {
        char board[][] = new char[n][n];
        for(char row[] : board) {
            Arrays.fill(row, '.');
        }

        res = new ArrayList<>();

        solve(board, 0);
    }

    static boolean isSafe(char[][] board, int row, int col) {
        // vertical
        for(int i=0; i<board.length; i++) {
            if(board[i][col]!='.')
                return false;
        }

        // horizontal
        for(int i=0; i<board[0].length; i++) {
            if(board[row][col]!='.')
                return false;
        }

        // diagonal back - up
        int i = row, j = col;
        while(i>=0 && j>=0) {
            if(board[i][j]!='.')
                return false;
            i--;
            j--;
        }

            
        i=row;
        j=col;
        while(i<board.length && j<board.length) {
            if(board[i][j]!='.')
                return false;
            i++;
            j++;
        }
        
        i=row;
        j=col;
        while(i>=0 && j<board.length) {
            if(board[i][j]!='.')
                return false;
            
            i--;
            j++;
        }

        i=row;
        j=col;
        while(i<board.length && j>=0) {
            if(board[i][j]!='.')
                return false;

            i++;
            j--;
        }

        return true;
    }

    public static void main(String[] args) {
        int n = 4;
        solveNQueens(n);

        for(List<String> a : res) {
            System.out.println(a);
        }
    }
}
