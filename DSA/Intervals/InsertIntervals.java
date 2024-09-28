package Intervals;

import java.util.ArrayList;

public class InsertIntervals {

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

    static int[][] insert(int[][] intervals, int[] newInterval) {

        ArrayList<int[]> arr = new ArrayList<>();

        printIntervals(intervals);

        int i = 0;
        while(i<intervals.length && intervals[i][1]<newInterval[0]) {
            arr.add(new int[]{intervals[i][0], intervals[i][1]});
            i++;
        }
        
        // if new interval is last;
        if(i==intervals.length) {
            arr.add(new int[]{newInterval[0], newInterval[1]});
        } else {
            int start = Math.min(intervals[i][0], newInterval[0]);
            int end = Math.max(intervals[i][1], newInterval[1]);

            // if new interval is not overlapping with any interval
            if(newInterval[0]<intervals[i][0] && newInterval[1]<intervals[i][0]) {
                start = newInterval[0];
                end = newInterval[1];
            } else {
                int j=i;
                // if overlapping, find start and end with all the overlapping intervals
                while(j<intervals.length && intervals[i][1] >= intervals[j][0]) {
                    intervals[i][1] = Math.max(intervals[i][1], intervals[j][1]);
                    j++;
                }
    
                arr.add(new int[]{intervals[i][0], intervals[i][1]});
                i = j;
            }
            arr.add(new int[]{start, end});

            // add all the remaning intervals
            while(i<intervals.length) { 
                arr.add(new int[]{intervals[i][0], intervals[i][1]});
                i++;
            }
        }
        
        int[][] res = new int[arr.size()][2];
        int k=0;
        for(int[] a: arr)
            res[k++] = a;

        return res;
    }

    public static void main(String[] args) {
        int intervals[][] = {{3,5},{6,7},{8,10},{13,16}};
        int newInterval[] = {1,2};
        intervals = insert(intervals, newInterval);

        printIntervals(intervals);
    }
}
