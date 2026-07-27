package Arrays;

import java.util.*;

public class TwoSum {

    // 1. Sort and find
    // 2. Two pass in HashMap

    static int[] twoSum(int arr[], int target) {
        Map<Integer, Integer> map = new HashMap<>();

        for(int i=0; i<arr.length; i++) {
            // b = target - a;
            if (map.containsKey(target - arr[i])) {
                return new int[]{map.get(target - arr[i]), i};
            }

            map.put(arr[i], i);
        }

        return new int[]{-1, -1};
    }

    public static void main(String[] args) {
        int arr[] = {3,4,5,6};
        int ans[] = twoSum(arr, 11);
        System.out.println(ans[0] + " " + ans[1]);
    }
}
