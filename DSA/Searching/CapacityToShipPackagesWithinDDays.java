package Searching;

import java.util.Arrays;

public class CapacityToShipPackagesWithinDDays {

    static boolean canShip(int weights[], int capacity, int days) {
        int daysNeeded = 1, currWeights = 0;
        for(int i=0; i< weights.length; i++) {
            currWeights += weights[i];
            if(currWeights > capacity) {
                daysNeeded++;
                currWeights = weights[i];
                if(daysNeeded > days)
                    return false;
            }
        }

        return true;
    }

    static int shipWithinDays(int[] weights, int days) {
        int low = Arrays.stream(weights).max().getAsInt();
        int high = Arrays.stream(weights).sum();

        while(low < high) {
            int mid = low + (high-low)/2;
            if(canShip(weights, mid, days))
                high = mid;
            else
                low = mid+1;
        }

        return low;
    }
    public static void main(String[] args) {
        int[] weights = {1,2,3,4,5,6,7,8,9,10};
        System.out.println(shipWithinDays(weights, 5));
    }
}
