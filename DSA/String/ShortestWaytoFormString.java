// 1055. Shortest Way to Form String
package String;

public class ShortestWaytoFormString {
    
    static int shortestWay(String s, String t) {
        int iS = 0, iT = 0, ans = 0;
        boolean found = false;
        while(iS<s.length() && iT<t.length()) {
            if(s.charAt(iS) == t.charAt(iT)) {
                iT++;
                found = true;
            }

            if(iS == s.length()-1) {
                if(!found)
                    return -1;
                
                ans = ans + 1;
                found = false;
            }

            iS = (iS + 1)%s.length();
        }

        return ans;
    }
    
    public static void main(String[] args) {
        System.out.println(shortestWay("abcd", "abc"));
    }
}
