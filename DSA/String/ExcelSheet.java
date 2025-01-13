/*
 * Given a positive integer N, return its corresponding column title as it would appear in an Excel sheet.
For N =1 we have column A, for 27 we have AA and so on.
 */

package String;

public class ExcelSheet {

    static String recursion(int n, String curr) {
        if(n<=0)
            return curr;

        int q = (n-1)/26;
        int rem = (n-1)%26;
        if(q!=0) {
            return recursion(q, (char)(65 + rem) + curr);
        }

        return (char) (65 + rem) + curr;
    }
    public static void main(String[] args) {
        int n = 705;
        System.out.println(recursion(n, ""));
    }
}
