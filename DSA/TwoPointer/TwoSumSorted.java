package TwoPointer;

import java.util.Arrays;

public class TwoSumSorted {

    static int binarySearch(int[] numbers, int start, int end, int key) {
        int mid = start + (end - start)/2;

        while(start<end) {
            mid = start + (end - start)/2;
            if(numbers[mid] == key) {
                return mid;
            } else if(numbers[mid] > key) {
                end = mid;
            } else {
                start = mid;
            }
        }

        return -1;
    }

    static int[] twoSum(int[] numbers, int target) {
        // use binary search
        for(int i=0; i<numbers.length; i++) {
            int b = binarySearch(numbers, 0, i, target - numbers[i]);

            if(b != -1)
                return new int[]{b, i};
        }

        return new int[] {-1, -1};
    }

    static int[] twoSum2(int[] numbers, int target) {
        // use two pointer
        int start = 0, end = numbers.length-1;
        while(start < end) {
            if(numbers[start] + numbers[end] == target) {
                return new int[]{start, end};
            } else if (numbers[start] + numbers[end] > target) {
                end--;
            } else {
                start++;
            }
        }

        return new int[]{-1, -1};
    }

    public static void main(String[] args) {
        int nums[] = {2,7,11,15};
        int target = 9;

        System.out.println(Arrays.toString(twoSum(nums, target)));
        System.out.println(Arrays.toString(twoSum2(nums, target)));

    }
}
