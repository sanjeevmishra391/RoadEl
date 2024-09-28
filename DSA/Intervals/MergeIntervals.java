package Intervals;

import java.util.ArrayList;
import java.util.Arrays;

public class MergeIntervals {

    static void printIntervals(int[][] intervals) {
        for(int a[] : intervals) {
            int startSpace = a[0] == 0 ? 1 : 2*a[0];
            int betweenDots = (a[1] - a[0]);
            System.out.printf(" %" + startSpace + "d", a[0]);
            for (int i = 0; i < betweenDots - 1; i++)
                System.out.print(" .");
            System.out.println(" " + a[1] + " ");
        }
    }

    public static int[][] merge(int[][] intervals) {
        ArrayList<int[]> arr = new ArrayList<>();
        Arrays.sort(intervals, (a, b) -> a[0] - b[0]);

        printIntervals(intervals);

        for(int i=0; i<intervals.length;) {
            int j = i+1;
            while(j<intervals.length && intervals[i][1] >= intervals[j][0]) {
                intervals[i][1] = Math.max(intervals[i][1], intervals[j][1]);
                j++;
            }

            arr.add(new int[]{intervals[i][0], intervals[i][1]});
            i = j;
        }

        int[][] res = new int[arr.size()][2];
        int i=0;
        for(int[] a: arr)
            res[i++] = a;

        return res;
    }

    public static void main(String[] args) {
        int[][] intervals = {{2, 6}, {12, 15}, {6, 7}, {8, 9}, {1, 10}};
        intervals = merge(intervals);
        System.out.println("After merging:");
        printIntervals(intervals);
    }
}