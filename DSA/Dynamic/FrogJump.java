// https://atcoder.jp/contests/dp/tasks/dp_a

package Dynamic;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Methods;
import roadel.Utility.Delimiter;

public class FrogJump {
    int result;
    Methods method;
    int dp[];

    FrogJump(int[] ip, int k, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(ip, k, 0, 0, 0);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(ip, k);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[ip.length + 1];
            result = memoization(ip);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("ip", ip),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    int recursive(int arr[], int k, int idx, int sum, int lastIdx) {
        if(idx>=arr.length) return Integer.MAX_VALUE;
        
        sum += Math.abs(arr[lastIdx] - arr[idx]);
        if(idx == arr.length-1) {
          return sum;
        }
        
        int res = Integer.MAX_VALUE;
        for(int i=1; i<=k; i++){
            res = Math.min(res, recursive(arr, k, idx+i, sum, idx));
        }
        return res;
    }

    int tabulation(int[] ip, int k) {
        int dp[] = new int[ip.length];

        for(int i=1; i<ip.length; i++) {
            int res = Integer.MAX_VALUE;
            for(int j=1; j<=k; j++) {
                res = Math.min(res, i-j>=0 ? dp[i-j] + Math.abs(ip[i] - ip[i-j]) : Integer.MAX_VALUE);
            }
            dp[i] = res;
        }
        
        return dp[ip.length-1];
    }

    int memoization(int[] ip) {
        return 0;
    }

    public static void main(String[] args) {
        int[] ip = {40, 10, 20, 70, 80, 10, 20, 70, 80, 60};
        int k = 4;
        
        new FrogJump(ip, k, Methods.RECURSION, true);
        new FrogJump(ip, k, Methods.TABULATION, true);
        new FrogJump(ip, k, Methods.MEMOIZATION, false);

    }


}
