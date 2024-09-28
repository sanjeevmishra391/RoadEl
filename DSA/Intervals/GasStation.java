// https://leetcode.com/problems/gas-station/

package Intervals;


public class GasStation {

    static int canCompleteCircuit(int[] gas, int[] cost) {
        int n = gas.length;
        for(int i = 0; i < n; i++){
            int totalFuel = 0;
            int stopCount = 0, j = i;
            while(stopCount < n){
                totalFuel += gas[j % n] - cost[j % n];
                if(totalFuel < 0) break; // whenever we reach -ve
                stopCount++;
                j++;
            }
            if(stopCount == n && totalFuel >= 0) return i; // cover all the stops & our fuel left is 0 or more than that
        }
        return -1;
    }

    static int canCompleteCircuit2(int[] gas, int[] cost) {
        int total_sum = 0, surplus = 0, start = 0;
        for(int i=0; i<gas.length; i++) {
            total_sum += gas[i] - cost[i];
            surplus += gas[i] - cost[i];

            if(surplus < 0) {
                surplus = 0;
                start = i+1;
            }
        }

        return total_sum < 0 ? -1 : start;
    }

    public static void main(String[] args) {
        int[] gas = {5,5,1,3,4};
        int[] cost = {8,1,7,1,1};
        System.out.println(canCompleteCircuit2(gas, cost));
    }
}
