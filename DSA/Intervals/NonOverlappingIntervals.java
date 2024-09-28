package Intervals;

import java.util.Arrays;

public class NonOverlappingIntervals {

    static void printIntervals(int[][] intervals) {
        for(int a[] : intervals) {
            int startSpace = a[0] == 0 ? 1 : a[0];
            int betweenDots = (a[1] - a[0]);
            System.out.printf("%" + startSpace + "d", a[0]);
            for (int i = 0; i < betweenDots - 1; i++)
                System.out.print(".");
            System.out.println(a[1]);
        }
    }

    static int eraseOverlapIntervals(int[][] intervals) {
        if(intervals.length == 0) return 0;

        Arrays.sort(intervals, (a, b) -> a[1] - b[1]);

        printIntervals(intervals);

        int end = intervals[0][1], c = 0;
        for(int i=1; i<intervals.length; i++) {
            if(intervals[i][0] < end) {
                c++;
            } else {
                end = intervals[i][1];
            }
        }

        return c;
    }
    public static void main(String[] args) {
        int intervals[][] = {{1,2},{2,3},{3,4},{1,3}};
        
        int ans = eraseOverlapIntervals(intervals);

        System.out.println(ans);
    }
}
