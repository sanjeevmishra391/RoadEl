package Practice;

public class Practice {

    static int[] window(int nums[]) {
        int res = nums[0], currSum = 0;
        int currStart = 0, resStart = 0, resEnd = 0;

        for(int i=0; i<nums.length; i++) {
            // if currSum is negative then restart
            if(currSum < 0) {
                currSum = 0;
                currStart = i;
            }

            currSum += nums[i];

            if(currSum >= res) {
                res = currSum;
                resStart = currStart;
                resEnd = i;
            }
        }

        System.out.println("Max sum " + res);

        return new int[] {resStart, resEnd};
    }
    
    public static void main(String[] args) {
        int num[] = {5,4,-1,7,8};
        int res[] = window(num);
        System.out.println(res[0] + " " + res[1]);
        System.out.println(num[res[0]] + " " + num[res[1]]);
    }
}
