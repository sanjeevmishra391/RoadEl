package Arrays;

import java.util.HashSet;
import java.util.Set;

public class Duplicate {

    static boolean containsDuplicate(int[] nums) {
        int n = nums.length;
        for(int i=0; i<n; i++) {
            for(int j=i+1; j<n; j++) {
                if(nums[i] == nums[j])
                    return true;
            }
        }

        return false;
    }

    static boolean containsDuplicateUsingSet(int[] nums) {
        Set<Integer> set = new HashSet<>();
        for(int a: nums) {
            if(set.contains(a)) {
                return true;
            }

            set.add(a);
        }
        return false;
    }

    public static void main(String[] args) {
        int arr[] = {1,2,3,4};
        System.out.println(containsDuplicate(arr));
        System.out.println(containsDuplicateUsingSet(arr));
    }
}
