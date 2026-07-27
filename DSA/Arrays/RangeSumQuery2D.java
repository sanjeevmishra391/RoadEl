package Arrays;

// https://leetcode.com/problems/range-sum-query-2d-immutable/description/

class NumMatrix {
    private int prefixSum[][];

    public NumMatrix(int[][] matrix) {
        int rows = matrix.length, cols = matrix[0].length;
        prefixSum = new int[rows][cols];
        prefixSum[0][0] = matrix[0][0];

        // first col
        for(int i=1; i<cols; i++) {
            prefixSum[0][i] = prefixSum[0][i-1] + matrix[0][i];
        }

        // first row
        for(int i=1; i<rows; i++) {
            prefixSum[i][0] = prefixSum[i-1][0] + matrix[i][0];
        }

        // rest of matrix
        for(int i=1; i<rows; i++) {
            for(int j=1; j<cols; j++) {
                prefixSum[i][j] = prefixSum[i-1][j] + prefixSum[i][j-1] + matrix[i][j] - prefixSum[i-1][j-1];
            }
        }
    }
    
    public int sumRegion(int row1, int col1, int row2, int col2) {
        int totalArea = prefixSum[row2][col2];
        int areaToRemove = (col1 > 0 ? prefixSum[row2][col1-1] : 0) + (row1 > 0 ?prefixSum[row1-1][col2] : 0);
        int areaToAddBack = (row1 > 0 && col1 > 0) ? prefixSum[row1-1][col1-1] : 0;

        return totalArea - areaToRemove + areaToAddBack;
    }
}

public class RangeSumQuery2D {
    public static void main(String[] args) {
        int matrix[][] = {  {3, 0, 1, 4, 2}, 
                            {5, 6, 3, 2, 1}, 
                            {1, 2, 0, 1, 5}, 
                            {4, 1, 0, 1, 7}, 
                            {1, 0, 3, 0, 5}};
        NumMatrix obj = new NumMatrix(matrix);
        System.out.println(obj.sumRegion(2, 1, 4, 3));
        System.out.println(obj.sumRegion(1, 1, 2, 2));
        System.out.println(obj.sumRegion(1, 2, 2, 4));
    }
}
