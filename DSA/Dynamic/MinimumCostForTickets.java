/*
    You have planned some train traveling one year in advance. The days of the year in which you will travel are given as an integer array days. Each day is an integer from 1 to 365.

    Train tickets are sold in three different ways:

    a 1-day pass is sold for costs[0] dollars,
    a 7-day pass is sold for costs[1] dollars, and
    a 30-day pass is sold for costs[2] dollars.
    The passes allow that many days of consecutive travel.

    For example, if we get a 7-day pass on day 2, then we can travel for 7 days: 2, 3, 4, 5, 6, 7, and 8.
    Return the minimum number of dollars you need to travel every day in the given list of days.
 */

package Dynamic;

import java.util.HashSet;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Methods;
import roadel.Utility.Delimiter;

public class MinimumCostForTickets {
    int result;
    Methods method;
    static int dp[];

    MinimumCostForTickets(int[] days, int[] costs, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(days, costs, 0, 0, 0);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation2(days, costs);
        } else if (method == Methods.MEMOIZATION && use) {
            dp = new int[days.length + 1];
            result = memoization(days, costs, 0);
        }
        
        if(use)
        Utility.printRes(this.method, 
            Delimiter.INPUT, 
                new LabeledData<>("Days", days),
                new LabeledData<>("Costs", costs), 
            Delimiter.OUTPUT, 
                new LabeledData<>("Total cost", result));
    }

    int recursive(int[] days, int[] costs, int d, int totalCost, int lastDayLimit) {
        if (d >= days.length)
            return Integer.MAX_VALUE;

        if (d == days.length - 1) {
            if (days[d] > lastDayLimit)
                totalCost = totalCost + Math.min(costs[0], Math.min(costs[1], costs[2]));

            return totalCost;
        }
        int cost;
        // if today > lastDayLimit buy new pass
        if (days[d] > lastDayLimit) {
            int oneDayPass = recursive(days, costs, d + 1, totalCost + costs[0], days[d]);
            int weekPass = recursive(days, costs, d + 1, totalCost + costs[1], days[d] + 6);
            int monthPass = recursive(days, costs, d + 1, totalCost + costs[2], days[d] + 29);
            cost = Math.min(Math.min(oneDayPass, weekPass), monthPass);
        }
        // else you can travel
        else {
            cost = recursive(days, costs, d + 1, totalCost, lastDayLimit);
        }

        // return cost
        return cost;
    }

    int tabulation(int[] days, int[] costs) {
        HashSet<Integer> travelDays = new HashSet<>();
        for (int day : days)
            travelDays.add(day);

        int dp[] = new int[366];
        for (int i = 1; i < 366; i++) {
            // For non-travel days, the cost stays the same as for the previous day.
            // On the days we travel, it's a minimum of yesterday's cost plus single-day
            // ticket, or cost for 8 days ago plus 7-day pass, or cost 31 days ago plus
            // 30-day pass.
            if (!travelDays.contains(i))
                dp[i] = dp[i - 1];
            else {
                dp[i] = Math.min(
                        dp[i - 1] + costs[0],
                        Math.min(
                                dp[Math.max(0, i - 7)] + costs[1],
                                dp[Math.max(0, i - 30)] + costs[2]));
            }
        }

        return dp[365];
    }

    // accounting only those days which we travel and we only look 30 days back,
    // store the cost for last 30 days in a rolling array.
    int tabulation2(int[] days, int[] costs) {
        HashSet<Integer> travelDays = new HashSet<>();
        for (int day : days)
            travelDays.add(day);

        int dp[] = new int[30];
        for (int i = days[0]; i <= days[days.length - 1]; i++) {
            // For non-travel days, the cost stays the same as for the previous day.
            // On the days we travel, it's a minimum of yesterday's cost plus single-day
            // ticket, or cost for 8 days ago plus 7-day pass, or cost 31 days ago plus
            // 30-day pass.
            if (!travelDays.contains(i))
                dp[i % 30] = dp[(i - 1) % 30];
            else {
                dp[i % 30] = Math.min(
                        dp[(i - 1) % 30] + costs[0],
                        Math.min(
                                dp[(Math.max(0, i - 7)) % 30] + costs[1],
                                dp[(Math.max(0, i - 30)) % 30] + costs[2]));
            }
        }

        return dp[days[days.length - 1] % 30];
    }

    int memoization(int[] days, int[] costs, int d) {
        if (d >= days.length)
            return 0;

        if (dp[d] != 0)
            return dp[d];

        int a = costs[0] + memoization(days, costs, d + 1);
        int i;

        for (i = d; i < days.length && days[i] < days[d] + 7; i++)
            ;

        int b = costs[1] + memoization(days, costs, i);

        for (i = d; i < days.length && days[i] < days[d] + 30; i++)
            ;

        int c = costs[2] + memoization(days, costs, i);

        dp[d] = Math.min(a, Math.min(b, c));

        return dp[d];
    }

    public static void main(String[] args) {
        int[] days = { 1, 4, 6, 7, 8, 365 };
        int[] costs = { 2, 7, 15 };
        // expected = 11

        new MinimumCostForTickets(days, costs, Methods.RECURSION, true);
        new MinimumCostForTickets(days, costs, Methods.TABULATION, false);
        new MinimumCostForTickets(days, costs, Methods.MEMOIZATION, false);

    }
}
