package TwoPointer;

import java.util.List;
import java.util.Arrays;

public class ThreeSum {
    // a + b + c = 0
    static List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        return KSum.kSum(nums, 0, 3, 0);
    }

    public static void main(String[] args) {
        int nums[] = {0,0,0,0};

        List<List<Integer>> ans = threeSum(nums);
        for(List<Integer> a : ans) {
            System.out.println(a.toString());
        }
    }
}
