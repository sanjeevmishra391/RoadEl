package Practice;

import java.util.Arrays;

public class LargestNumber {
    static String largestNumber(int[] nums) {
        String[] st = new String[nums.length];
        int i=0;
        for(int a: nums) {
            st[i++] = a + "";
        }
        
        Arrays.sort(st, (a, b) -> (b+a).compareTo(a+b));
        if(st[0].equals("0")) return "0";

        String res = "";
        for(String a : st) {
            res += a;
        }

        return res;
    }
    public static void main(String[] args) {
        int nums[] = {3,30,9,34,5};
        // int[] nums = {999999991,9};
        System.out.println(largestNumber(nums));
    }
}