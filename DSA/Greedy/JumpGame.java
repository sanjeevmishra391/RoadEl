// https://leetcode.com/problems/jump-game/description/

package Greedy;

public class JumpGame {
    static boolean canJump(int[] nums) {
        boolean res[] = new boolean[nums.length];
        res[nums.length-1] = true;
        for(int i=nums.length-2; i>=0; i--) {
            int max = nums[i], j = 1;
            boolean possible = false;
            while(j <= max) { 
                if(i+j < nums.length && res[i + j]) {
                    possible = true;
                    break;
                }
                j++;
            }

            if(possible)
                res[i] = true;
        }

        return res[0];
    }

    static boolean canJump2(int[] nums) {
        int max = nums[0];
        for(int i=1; i<nums.length; i++) {
            if(max == 0)
                return false;

            max = Math.max(max-1, nums[i]);
        }

        return true;
    }

    public static void main(String[] args) {
        int nums[] = {2,0,0};
        System.out.println(canJump(nums));
        System.out.println(canJump2(nums));
    }
}
