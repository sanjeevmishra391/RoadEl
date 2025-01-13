package Searching;

import java.util.Arrays;

public class PainterPartition {

    static boolean canPaint(int boards[], int paintTime, int painters) {
        int currPainters = 1, time = 0;
        for(int i=0; i<boards.length; i++) {
            if(boards[i] > paintTime)
                return false;
            time += boards[i];
            if(time > paintTime) {
                currPainters++;
                time = boards[i];
                if(currPainters > painters)
                    return false;
            }
        }
        return true;
    }

    static int minTime(int boards[], int k) {
        int low = Arrays.stream(boards).max().getAsInt();
        int high = Arrays.stream(boards).sum();

        while(low < high) {
            int mid = low + (high-low)/2;
            if(canPaint(boards, mid, k))
                high = mid;
            else
                low = mid+1;
        }

        return low;
    }
    public static void main(String[] args) {
        int boards[] = {10, 20, 30, 40};
        System.out.println(minTime(boards, 2));
    }
}
