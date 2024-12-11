package Dynamic;

public class CountSubsetSum {

    static int tabulation(int[] nums, int target) {
        int n = nums.length;
        int dp[][] = new int[n+1][target+1];
        
        for(int i=0; i<=n; i++)
            dp[i][0] = 1;
        
        for(int i=1; i<=n; i++) {
            for(int j=0; j<=target; j++) {
                dp[i][j] = dp[i-1][j];
                if(j >= nums[i-1]) {
                    dp[i][j] += dp[i-1][j-nums[i-1]];
                }
            }
        }
        
        return dp[n][target];
    }

    public static void main(String[] args) {
        int[] ip = {5, 2, 3, 10, 6, 8};
        int target = 10;
        System.out.println(tabulation(ip, target));
    }
}
