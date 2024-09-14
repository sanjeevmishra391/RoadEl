/*
You are given an array of binary strings strs and two integers m and n.
Return the size of the largest subset of strs such that there are at most m 0's and n 1's in the subset.
A set x is a subset of a set y if all elements of x are also elements of y.

strs = ["10","0001","111001","1","0"], m = 5, n = 3
Output = 4
 */

package Dynamic;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Methods;
import roadel.Utility.Delimiter;

public class OnesAndZeroes {
    int result;
    Methods method;
    int dp[];

    OnesAndZeroes(String[] strs, int m, int n, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            int totalZero = 0, totalOne = 0;

            for(String s : strs) {
                int arr[] = countZeroesAndOnes(s);
                totalZero += arr[0];
                totalOne += arr[1];
            }

            result = recursive(strs, m, n, totalZero, totalOne, 0, strs.length);
        } else if (method == Methods.TABULATION && use) {
            // result = tabulation(strs);
        } else if (method == Methods.MEMOIZATION && use) {
            // dp = new int[ip.length + 1];
            // result = memoization(ip);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("strs", strs),
                    new LabeledData<>("m", m),
                    new LabeledData<>("n", n),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    int[] countZeroesAndOnes(String str) {
        int totalZero = 0, totalOne = 0;
        for(char c : str.toCharArray()) { 
            if(c == '0') {
                totalZero++;
            } else {
                totalOne++;
            }
        }

        return new int[]{totalZero, totalOne};
    }

    int recursive(String strs[], int m, int n, int zero, int one, int idx, int count) {
        if(idx >= strs.length) return 0;
        if(zero<=m && one<=n) return count;

        int[] arr = countZeroesAndOnes(strs[idx]);

        System.out.printf("%2d %2d\n", zero, one);
        // remove
        int ans = recursive(strs, m, n, zero, one, idx+1, count);
        // don't remove it
        ans  = Math.max(ans, recursive(strs, m, n, zero - arr[0], one - arr[1], idx+1, count-1));


        return ans;
    }

    int tabulation(int[] ip) {
        return 0;
    }

    int memoization(int[] ip) {
        return 0;
    }

    public static void main(String[] args) {
        // String[] strs = {"10","0001","111001","1","0"};
        String[] strs = {"11111","100","1101","1101","11000"};
        int m = 5;
        int n = 7;
        
        new OnesAndZeroes(strs, m, n, Methods.RECURSION, true);
        new OnesAndZeroes(strs, m, n, Methods.TABULATION, false);
        new OnesAndZeroes(strs, m, n, Methods.MEMOIZATION, false);

    }
}
