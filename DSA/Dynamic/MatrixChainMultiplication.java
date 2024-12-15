package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class MatrixChainMultiplication {
    int result;
    Methods method;
    int dp[][];

    MatrixChainMultiplication(int arr[], Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(arr, 0, arr.length-1);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(arr);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[arr.length + 1][arr.length + 1];
            for(int row[] : dp)
                Arrays.fill(row, -1);
            result = memoization(arr, 0, arr.length-1, dp);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("Arr", arr),
                Delimiter.OUTPUT, 
                    new LabeledData<>("Op", result));
    }

    int recursive(int arr[], int i, int j) {
        if(i + 1 == j)
            return 0;

        int ans = Integer.MAX_VALUE;

        for(int k=i+1; k<j; k++) {
            int curr = recursive(arr, i, k) + recursive(arr, k, j) + arr[i]*arr[k]*arr[j];
            ans = Math.min(ans, curr);
        }

        return ans;
    }

    int tabulation(int arr[]) {
        return 0;
    }

    int memoization(int arr[], int i, int j, int dp[][]) {
        if(i + 1 == j)
            return 0;

        if(dp[i][j] != -1)
            return dp[i][j];

        int res = Integer.MAX_VALUE;

        for(int k=i+1; k<j; k++) {
            int curr = memoization(arr, i, k, dp) + memoization(arr, k, j, dp) + arr[i]*arr[k]*arr[j];
            res = Math.min(curr, res);
        }

        return dp[i][j] = res;
    }

    public static void main(String[] args) {
        int arr[] = {1, 2, 3, 4, 3};
        
        new MatrixChainMultiplication(arr, Methods.RECURSION, true);
        // new MatrixChainMultiplication(arr, Methods.TABULATION, true);
        new MatrixChainMultiplication(arr, Methods.MEMOIZATION, true);

    }
}
