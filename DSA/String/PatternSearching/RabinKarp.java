package String.PatternSearching;

/*
    Formula for calculating hash
    hash(txt[s+1 ... s+m]) =    (d * 
                                    (hash(txt[s ... m-1]) - txt[s] * h) 
                                    + txt[s+m]
                                ) % q

    here, d = number of characters (usually 256)
        q = prime number
        h = Math.pow(d, m-1) % q
        m = length of window
 */

public class RabinKarp {

    public final static int d = 256;

    static void search(String txt, String pattern, int q) {
        // 1. calculate the hash of pattern and txt window of size of pattern
        // 2. for each window on text, if hash matches then check if all the characters matches
        // else calculate hash for next window by removing current window start character and adding next character to window.
        int m = pattern.length();
        int p = 0; // will store the hash of pattern
        int t = 0; // will store the hash of text
        int h = 1;

        if(txt.length() < m)
            return;

        // i less than m-1
        for(int i=0; i<m-1; i++) {
            h = (h*d)%q;
        }

        for(int i=0; i<m; i++) {
            t = (d * t + txt.charAt(i) ) % q;
            p = (d * p + pattern.charAt(i) ) % q;
        }

        for(int i=0; i+m<=txt.length(); i++) {
            if(t == p) {
                int j = 0;
                for(; j<m; j++) {
                    if(txt.charAt(i+j) != pattern.charAt(j))
                        break;
                }

                if(j==m)
                    System.out.println("Found at " + i);
            }

            if(i + m < txt.length()) {
                t = (d*(t - txt.charAt(i)*h) + txt.charAt(i+m))%q;

                if(t<0)
                    t = t + q;
            }
        }
    }

    public static void main(String[] args) {
        String txt = "GEEKS FOR GEEKS";
        String pattern = "GEEK";

        int q = 101;
        search(txt, pattern, q);
    }    
}
