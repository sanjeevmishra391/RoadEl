package Searching;

public class BinarySearch {

    static int binarySearch(int arr[], int k) {
        // using iterative algorithm
        int low = 0, high = arr.length-1;
        while(low<=high) {
            int mid = low + (high-low)/2;
            if(arr[mid] == k) {
                return mid;
            } else if(arr[mid] > k) {
                high = mid-1;
            } else {
                low = mid+1;
            }
        }

        System.out.println(low + " " + high);

        return -low;
    }

    public static void main(String[] args) {
        int arr[] = { 2, 3, 4, 10, 40 };
        System.out.println(binarySearch(arr, 3));
    }
}
