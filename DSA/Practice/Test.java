package Practice;

import java.util.Arrays;

public class Test  {
   public static void main(String[] args) {
      int N = 3;
      int arr[][] = {{1,2,5}, {3,1,1}, {3,3,3}};
      int[][] dp = new int[N+1][3];
        for(int i=1; i<=N; i++) {
            for(int j=0; j<3; j++) {
               for(int k=0; k<3; k++) {
                  System.out.println(i + " " + j + " " + k + " ");
                  if(j!=k) {
                     dp[i][j] = Math.max(dp[i-1][k] + arr[i-1][k], dp[i][j]);
                  }
               }
            }
        }

        System.out.println(Arrays.toString(dp[N]));
   }
} 