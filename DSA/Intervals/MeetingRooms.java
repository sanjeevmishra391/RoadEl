package Intervals;

import java.util.Arrays;

public class MeetingRooms {

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
    
    static boolean canAttendMeetings(int[][] intervals) {
        Arrays.sort(intervals, (a, b) -> a[1] - b[1]);
        printIntervals(intervals);
        int end = intervals[0][1];
        for(int i=1; i<intervals.length; i++) {
            if(intervals[i][0] < end) {
                return false;
            }

            end = intervals[i][1];
        }

        return true;
    }

    public static void main(String[] args) {
        int[][] intervals = {{0, 3}, {5, 10}, {15, 20}};
        boolean ans = canAttendMeetings(intervals);
        System.out.println(ans);
    }
}
