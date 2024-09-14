package Dynamic;
class Fibonacci {
    static int fibRecursive(int n) {
        // base case
        if(n == 0 || n == 1)
            return n;

        return fibRecursive(n-1) + fibRecursive(n-2);
    }

    static int fibDP(int n) {
        if(n == 0)
            return 0;

        int dp[] = new int[n+1];
        dp[1] = 1;

        for(int i=2; i<=n; i++) {
            dp[i] = dp[i-1] + dp[i-2];
        }

        return dp[n];
    }

    static int fibOptimised(int n) {
        int a, b, c;
        a = 0;
        b = 1;

        for(int i=2; i<=n; i++) {
            c = a + b;
            a = b;
            b = c;
        }

        return b;
    }

    public static void main(String[] args) {
        int n = 6;
        int nth;
        // nth = fibRecursive(n);
        // nth = fibDP(n);
        nth = fibOptimised(n);

        System.out.println(n + "th fibonacci = " + nth);
    }
}