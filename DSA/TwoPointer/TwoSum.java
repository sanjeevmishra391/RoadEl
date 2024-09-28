package TwoPointer;

import java.util.HashMap;
import java.util.Arrays;


public class TwoSum {

    static int[] twoSum(int[] nums, int target) {
        // 1. use nested loop O(n2)
        // 2. use hashmap, Time : O(n), Space : O(n)

        HashMap<Integer, Integer> map = new HashMap<>();
        for(int i=0; i<nums.length; i++) {
            if(map.containsKey(nums[i])) {
                return new int[]{map.get(nums[i]), i};
            }

            map.put(target - nums[i], i);
        }

        return new int[]{-1, -1};
    }
    public static void main(String[] args) {
        int nums[] = {7,11,15,2};
        int target = 9;

        System.out.println(Arrays.toString(twoSum(nums, target)));
    }
}
