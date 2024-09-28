package Greedy;

public class JumpGame2 {

    static int jump(int[] nums) {
        int count = 0, currEnd = 0, currFarthest = 0;

        for(int i=0; i<nums.length-1; i++) {
            currFarthest = Math.max(currFarthest, nums[i] + i);
            if(i == currEnd) {
                count++;
                currEnd = currFarthest;
            }
        }

        return count;
    }
    public static void main(String[] args) {
        int[] nums = {2,0,2,4,6,0,0,3};
        System.out.println(jump(nums));
    }
}
