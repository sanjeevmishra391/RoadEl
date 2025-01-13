package String;

public class EditDistance {

    static int editDistance(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int dp[][] = new int[m+1][n+1];

        // transforming word1[0...i-1] into an empty string requires i deletions.
        for(int i=0; i<=m; i++)
            dp[i][0] = i;

        // transforming an empty string into word2[0...j-1] requires j insertions.
        for(int j=0; j<=n; j++)
            dp[0][j] = j;

        for(int i=1; i<=m; i++) {
            for(int j=1; j<=n; j++) {
                if(s1.charAt(i-1) == s2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1]; // no operation required
                } else {
                    // dp[i-1][j-1] + 1: replace the character at position i-1 in word1 with the character at position j-1 in word2
                    // dp[i-1][j] + 1: delete the character at position i-1 in word1.
                    // dp[i][j-1] + 1: insert the character at position j-1 in word2 into word1 at position i
                    dp[i][j] = Math.min(Math.min(dp[i-1][j], dp[i][j-1]), dp[i-1][j-1]) + 1;
                }
            }
        }

        return dp[m][n];
    }
    public static void main(String[] args) {
        String s1 = "intention", s2 = "execution";
        System.out.println(editDistance(s1, s2));
    }
}
