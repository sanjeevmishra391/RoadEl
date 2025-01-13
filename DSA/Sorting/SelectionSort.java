package Sorting;

import java.util.Arrays;

public class SelectionSort {

    static void sort(int arr[]) {
        for(int i=0; i<arr.length-1; i++) {
            int minIndex = i;
            for(int j=i+1; j<arr.length; j++) {
                if(arr[j] < arr[minIndex]) {
                    minIndex = j;
                }
            }

            // swapping
            int temp = arr[i];
            arr[i] = arr[minIndex];
            arr[minIndex] = temp;
        }
    }
    public static void main(String[] args) {
        int arr[] = {63,9,3,8,2};
        sort(arr);
        System.out.println(Arrays.toString(arr));
    }
}
