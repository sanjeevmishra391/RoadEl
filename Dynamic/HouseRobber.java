/*
    You are a professional robber planning to rob houses along a street. 
    Each house has a certain amount of money stashed, the only constraint stopping you from robbing each of them 
    is that adjacent houses have security systems connected and it will automatically contact 
    the police if two adjacent houses were broken into on the same night.

    Given an integer array nums representing the amount of money of each house, 
    return the maximum amount of money you can rob tonight without alerting the police.
 */

package Dynamic;

import java.util.Arrays;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class HouseRobber {
    int result;
    Methods method;
    static int dp[];

    HouseRobber(int[] ip, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(ip, ip.length-1);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation2(ip);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[ip.length + 1];
            Arrays.fill(dp, -1);
            result = memoization(ip, ip.length-1);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("ip", ip),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    int recursive(int[] ip, int idx) {
        if(idx < 0) return 0;

        return Math.max(recursive(ip, idx-2) + ip[idx], recursive(ip, idx-1));
    }

    int tabulation(int[] ip) {
        int dp[] = new int[ip.length];

        dp[0] = ip[0];
        dp[1] = Math.max(ip[0], ip[1]);

        for(int i=2; i<ip.length; i++) {
            dp[i] = Math.max(dp[i-1], dp[i-2] + ip[i]);
        }

        return Math.max(dp[ip.length-1], dp[ip.length-2]);
    }

    int tabulation2(int[] ip) {
        int past = ip[0]; // dp[i-2]
        int prev = Math.max(past, ip[1]); // dp[i-1];

        int res = 0;

        for(int i=2; i<ip.length; i++) {
            res = Math.max(prev, past + ip[i]);
            past = prev;
            prev = res;
        }

        return Math.max(res, prev);
    }

    int memoization(int[] ip, int idx) {
        if(idx<0) return 0;

        if(dp[idx] != -1) return dp[idx];

        dp[idx] = Math.max(memoization(ip, idx-2) + ip[idx], memoization(ip, idx-1));

        return dp[idx];
    } 

    public static void main(String[] args) {
        int[] ip = {2,1,2,6,1,8,10,10};
        
        new HouseRobber(ip, Methods.RECURSION, true);
        new HouseRobber(ip, Methods.TABULATION, true);
        new HouseRobber(ip, Methods.MEMOIZATION, true);
    }
}
