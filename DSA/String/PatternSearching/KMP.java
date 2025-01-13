package String.PatternSearching;

import java.util.ArrayList;

public class KMP {
    static int[] lpsCreation(String pattern) {
        int n = pattern.length();
        int lps[] = new int[n];
        
        lps[0] = 0; // as no proper prefix as suffix for len 1

        int len = 0, i=1;
        while(i<n) {
            // If characters match, increment the size of lps
            if(pattern.charAt(i) == pattern.charAt(len)) { 
                len++;
                lps[i] = len;
                i++;
            } else { // If there is a mismatch
                if(len!=0) {
                    len = lps[len-1]; // Update len to the previous lps value to avoid redundant comparisons
                } else {
                    lps[i] = 0; // If no matching prefix found, set lps[i] to 0
                    i++;
                }
            }
        }

        return lps;
    }

    static ArrayList<Integer> kmp(String pattern, String text) {
        int[] lps = lpsCreation(pattern);
        int i=0, j=0;
        ArrayList<Integer> res = new ArrayList<>();
        while(i<text.length()) {
            // If characters match, move both pointers forward
            if(text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                // If the entire pattern is matched store the start index in result
                if(j == pattern.length()) {
                    res.add(i-j);
                    // Use LPS of previous index to skip unnecessary comparisons
                    j=lps[j-1];
                }
            } else {
                if(j!=0) {
                    j = lps[j-1];
                } else {
                    i++;
                }
            }
        }

        return res;
    }

    public static void main(String[] args) {
        String text = "aabaacaadaabaaba";
        String pattern = "aaba";

        System.out.println(kmp(pattern, text));
    }
}
