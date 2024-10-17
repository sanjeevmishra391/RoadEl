package Backtracking;

import java.util.Arrays;

// use swapping of current with the next elements.

public class Permutations {

    void recursion(int[] arr, int i) {
        if(i==arr.length) {
            System.out.println(Arrays.toString(arr));
            return;
        }

        for(int j=i; j<arr.length; j++) {
            int temp = arr[j];
            arr[j] = arr[i];
            arr[i] = temp;

            recursion(arr, i+1);

            temp = arr[j];
            arr[j] = arr[i];
            arr[i] = temp;
        }
    }

    public static void main(String[] args) {
        int arr[] = {1,2,3};
        Permutations p = new Permutations();
        p.recursion(arr, 0);
    }
}
