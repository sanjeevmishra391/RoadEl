package Dynamic;

import java.util.*;

class Main {
  
  static int tabulation(int[] ip, int k) {
    int dp[] = new int[ip.length];

    for(int i=1; i<ip.length; i++) {
        int res = Integer.MAX_VALUE;
        for(int j=1; j<=k; j++) {
            res = Math.min(res, i-j>=0 ? dp[i-j] + Math.abs(ip[i] - ip[i-j]) : Integer.MAX_VALUE);
        }
        dp[i] = res;
    }
    
    return dp[ip.length-1];
}
  
  public static void main(String args[]) {
    try (Scanner sc = new Scanner(System.in)) {
        int len = sc.nextInt();
        int k = sc.nextInt();
        
        int arr[] = new int[len];
        while(len>0) {
          arr[arr.length - len] = sc.nextInt();
          len--;
        }
        
        System.out.println(tabulation(arr, k));
    }
  }
}
