package Practice;

import java.util.PriorityQueue;
import java.util.Arrays;

public class KClosestPoints {
    public static void main(String[] args) {
        int points[][] = {{3,3}, {5,-1}, {-2,4}};
        int k = 2;
        PriorityQueue<int[]> pq = new PriorityQueue<>((b, a) -> ((a[0]*a[0] + a[1]*a[1]) - (b[0]*b[0] + b[1]*b[1])));
    
        for(int[] point : points) {
            pq.add(point);
            if(pq.size() == k) {
                pq.poll();
            }
        }

        System.out.println(Arrays.toString(pq.poll()));
        System.out.println(Arrays.toString(pq.poll()));
    }
}
