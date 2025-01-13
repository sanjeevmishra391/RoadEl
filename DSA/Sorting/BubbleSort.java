package Sorting;

import java.util.Arrays;

public class BubbleSort {

    static void sort(int arr[]) {
        int n = arr.length;
        for(int i=0; i<n-1; i++) {
            boolean allSorted = true;
            for(int j=0; j<n-i-1; j++) {
                if(arr[j] > arr[j+1]) {
                    // swap
                    int temp = arr[j];
                    arr[j] = arr[j+1];
                    arr[j+1] = temp;

                    allSorted = false;
                }
            }
            if(allSorted)
                break;
        }
    }
    public static void main(String[] args) {
        int arr[] = {63,9,3,8,2};
        sort(arr);
        System.out.println(Arrays.toString(arr));
    }
}
