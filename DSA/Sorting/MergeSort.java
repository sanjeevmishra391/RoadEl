package Sorting;

import java.util.Arrays;

public class MergeSort {

    static void merge(int arr[], int left, int mid, int right) {
        int len1 = mid - left + 1;
        int len2 = right - mid;

        int L[] = new int[len1];
        int R[] = new int[len2];

        for(int i=0; i<len1; i++)
            L[i] = arr[left+i];

        for(int j=0; j<len2; j++)
            R[j] = arr[mid+j+1];

        int i=0, j=0, k = left;
        while(i<len1 && j<len2) {
            if(L[i] <= R[j]) {
                arr[k] = L[i];
                i++;
            } else {
                arr[k] = R[j];
                j++;
            }

            k++;
        }

        while(i<len1) {
            arr[k] = L[i];
            k++;
            i++;
        }

        while(j<len2) {
            arr[k] = R[j];
            k++;
            j++;
        }

    }

    static void sort(int arr[], int left, int right) {
        if(left >= right)
            return;

        int mid = left + (right-left)/2;

        sort(arr, left, mid);
        sort(arr, mid+1, right);

        merge(arr, left, mid, right);
    }

    public static void main(String[] args) {
        int arr[] = {63,9,3,8,2};
        sort(arr, 0, arr.length-1);
        System.out.println(Arrays.toString(arr));
    }
}
