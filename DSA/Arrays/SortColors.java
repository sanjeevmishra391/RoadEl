package Arrays;

import java.util.Arrays;

public class SortColors {

    // 1. Count number of 0s, 1s and 2s and overwrite array with that number of numbers in the order
    // 2. Maintain 3 pointers > low, mid, and high: O(n)
    static void swap(int nums[], int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    static void dutchFlag(int nums[]) {
        int low = 0, mid = 0, high = nums.length - 1;

        while(mid <= high) {
            if(nums[mid] == 0) {
                swap(nums, low, mid);
                low++;
                mid++;
            } else if (nums[mid] == 1) {
                mid++;
            } else {
                swap(nums, high, mid);
                high--;
                // mid++; don't do this, recheck for swapped element
            }
        }
    }

    public static void main(String[] args) {
        int colors[] = {2,0,2,1,1,0};
        dutchFlag(colors);
        System.out.println(Arrays.toString(colors));
    }
}
