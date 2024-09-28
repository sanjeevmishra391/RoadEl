package TwoPointer;

import java.util.List;
import java.util.ArrayList;

public class KSum {
    static ArrayList<List<Integer>> kSum(int[] nums, long target, int k, int index) {
        ArrayList<List<Integer>> res = new ArrayList<List<Integer>>();

        if(index >= nums.length)
            return res;

        if(k == 2) { // reduced to two sum problem
            int i = index, j = nums.length - 1;
            while(i<j) {
                if(target - nums[i] == nums[j]) {
                    List<Integer> temp = new ArrayList<>();
                    temp.add(nums[i]);
                    temp.add((int)target-nums[i]);
                    res.add(temp);
                    //skip duplication
                    while(i<j && nums[i]==nums[i+1]) i++;
                    while(i<j && nums[j-1]==nums[j]) j--;
                    i++;
                    j--;
                //move left bound
                } else if (target - nums[i] > nums[j]) {
                    i++;
                //move right bound
                } else {
                    j--;
                }
            }
        } else {
            for(int i=index; i<nums.length - k + 1; i++) {
                ArrayList<List<Integer>> temp = kSum(nums, target - nums[i], k-1, i+1);
                if(temp != null) {
                    for(List<Integer> t : temp) {
                        t.add(0, nums[i]);
                    }
                    res.addAll(temp);
                }

                while(i < nums.length-1 && nums[i] == nums[i+1])
                    i++;
            }
        }

        return res;
    }
}
