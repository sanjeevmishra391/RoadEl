package Sorting;

import java.util.Arrays;

public class QuickSort {

    static int partition(int arr[], int left, int right) {
        int pivot = arr[right];

        int i = left-1;

        for(int j=left; j<right; j++) {
            if(arr[j] < pivot) {
                i++;
                swap(arr, i, j);
            }
        }

        swap(arr, i+1, right);
        return i+1;
    }

    static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }


    static void sort(int arr[], int left, int right) {
        if(left >= right)
            return;

        int pi = partition(arr, left, right);

        sort(arr, left, pi-1);
        sort(arr, pi+1, right);
    }
    public static void main(String[] args) {
        int arr[] = {63,9,3,8,2};
        sort(arr, 0, arr.length-1);
        System.out.println(Arrays.toString(arr));
    }
}
