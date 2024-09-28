package TwoPointer;

import java.util.List;
import java.util.Arrays;

public class FourSum {

    static List<List<Integer>> fourSum(int[] nums, int target) {
        Arrays.sort(nums);
        return KSum.kSum(nums, target, 4, 0);
    }

    public static void main(String[] args) {
        int[] nums = {1,0,-1,0,-2,2};
        int target = 0;

        List<List<Integer>> ans = fourSum(nums, target);
        for(List<Integer> a : ans) {
            System.out.println(a.toString());
        }
    }
}
