// 465. Optimal Account Balancing

package Backtracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptimalAccountBalancing {

    static int dfs(List<Integer> list, int idx) {
       // Skip settled accounts
        while (idx < list.size() && list.get(idx) == 0) {
            idx++;
        }

        if (idx == list.size()) {
            return 0; // All accounts are settled
        }
        
        int min = Integer.MAX_VALUE;
        // Try settling `list[idx]` with subsequent accounts
        for (int i = idx + 1; i < list.size(); i++) {
            if (list.get(idx) * list.get(i) < 0) { // Opposite signs indicate potential settlement
                // Temporarily settle
                list.set(i, list.get(i) + list.get(idx));

                // Recursively settle remaining balances
                min = Math.min(min, 1 + dfs(list, idx + 1));

                // Backtrack
                list.set(i, list.get(i) - list.get(idx));
            }
        }

        return min;
    }

    static int minimumTransactions(int[][] transactions) {
        Map<Integer, Integer> map = new HashMap<>();
        for(int tr[] : transactions) {
            map.put(tr[0], map.getOrDefault(tr[0], 0) - tr[2]);
            map.put(tr[1], map.getOrDefault(tr[1], 0) + tr[2]);
        }

        List<Integer> list = new ArrayList<>();
        for(int balance : map.values()) {
            if(balance!=0)
                list.add(balance);
        }

        System.out.println(list);

        return dfs(list, 0);
    }

    public static void main(String[] args) {
        int transactions[][]  = {{0,1,10}, {1,0,1}, {1,2,5}, {2,0,5}};
        System.out.println(minimumTransactions(transactions));
    }
}

// x gave z$ to y.
// in-degree and out-degree