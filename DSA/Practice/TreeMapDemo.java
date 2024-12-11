package Practice;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class TreeMapDemo {
    public static void main(String[] args) {
        TreeMap<Integer, Integer> tm = new TreeMap<>();
        int items[][] = {{193,732},{781,962},{864,954},{749,627},{136,746},{478,548},{640,908},{210,799},{567,715},{914,388},{487,853},{533,554},{247,919},{958,150},{193,523},{176,656},{395,469},{763,821},{542,946},{701,676}};
        Arrays.sort(items, (a, b) -> a[0]-b[0]);
        for(int a[] : items) {
            int v = a[1];
            if(tm.floorEntry(a[0]) != null) {
                v = Math.max(tm.floorEntry(a[0]).getValue(), a[1]);
            }
            tm.put(a[0], v);
        }

        for (Map.Entry<Integer, Integer> e : tm.entrySet()) {
            System.out.print("["+e.getKey() + " " + e.getValue() + "], ");
        }
        System.out.println();

        int queries[] = {885,1445,1580,1309,205,1788,1214,1404,572,1170,989,265,153,151,1479,1180,875,276,1584};

        for(int q : queries) {
            Map.Entry<Integer, Integer> e = tm.floorEntry(q);
            if(e == null) {
                System.out.println(0 + ", ");
            } else
                System.out.print(e.getValue() + ", ");
        }
    }
}
