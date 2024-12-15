/*
    Given two strings S1 and S2 of equal length, the task is to determine if S2 is a scrambled form of S1.
    Scrambled string: 
    Given string str, we can represent it as a binary tree by partitioning it into two non-empty substrings recursively.
 */

package Dynamic;

import java.util.Arrays;
import java.util.HashMap;

import roadel.LabeledData;
import roadel.Utility;
import roadel.Utility.Delimiter;
import roadel.Utility.Methods;

public class ScrambledStrings {
    boolean result;
    Methods method;
    HashMap<String, Boolean> map;

    ScrambledStrings(String s1, String s2, Methods method, boolean use) {
        this.method = method;

        if (method == Methods.RECURSION && use) {
            result = recursive(s1, s2);
        } else if (method == Methods.TABULATION && use) {
            result = tabulation(s1, s2);
        } else if (method == Methods.MEMOIZATION && use) {
            map = new HashMap<String, Boolean>();
            result = memoization(s1, s2);
        }
        
        if(use)
            Utility.printRes(this.method, 
                Delimiter.INPUT, 
                    new LabeledData<>("S1", s1),
                    new LabeledData<>("S2", s2),
                Delimiter.OUTPUT, 
                    new LabeledData<>("op", result));
    }

    boolean isAnagram(String s1, String s2) {
        char[] chArr1 = s1.toCharArray();
        char[] chArr2 = s2.toCharArray();

        Arrays.sort(chArr1);
        Arrays.sort(chArr2);

        String copyS1 = new String(chArr1);
        String copyS2 = new String(chArr2);

        return copyS1.equals(copyS2);
    }

    boolean recursive(String s1, String s2) {
        int n = s1.length();

        if(n != s2.length()) // length is different
            return false;

        if(n==0) // both strings empty
            return true;

        if(s1.equals(s2)) // same strings
            return true;

        // check if anagrams
        if(!isAnagram(s1, s2))
            return false;


        for(int i=1; i<n; i++) {
            // Check if S2[0...i] is a scrambled string of S1[0...i] and if S2[i+1...n] is a scrambled string of S1[i+1...n]
            if(recursive(s1.substring(0, i), s2.substring(0, i)) 
                && recursive(s1.substring(i, n), s2.substring(i, n)))
                return true;

            // Check if S2[0...i] is a scrambled string of S1[n-i...n] and S2[i+1...n] is a scrambled string of S1[0...n-i-1]
            if(recursive(s1.substring(0, n-i), s2.substring(i, n)) 
                && recursive(s1.substring(n-i, n), s2.substring(0, i)))
                return true;
        }

        return false;
    }

    boolean tabulation(String s1, String s2) {
        return false;
    }

    boolean memoization(String s1, String s2) {
        int n = s1.length();

        if(n != s2.length()) // length is different
            return false;

        if(n==0) // both strings empty
            return true;

        if(s1.equals(s2)) // same strings
            return true;

        // check if anagrams
        if(!isAnagram(s1, s2))
            return false;

        String key = (s1 + " " + s2);
        boolean flag = false;

        if (map.containsKey(key)) {
            return map.get(key);
        }

        for(int i=1; i<n; i++) {
            // Check if S2[0...i] is a scrambled string of S1[0...i] and if S2[i+1...n] is a scrambled string of S1[i+1...n]
            if(memoization(s1.substring(0, i), s2.substring(0, i)) 
                && memoization(s1.substring(i, n), s2.substring(i, n))) {
                flag = true;
                map.put(key, flag);
                return true;
            }

            // Check if S2[0...i] is a scrambled string of S1[n-i...n] and S2[i+1...n] is a scrambled string of S1[0...n-i-1]
            if(memoization(s1.substring(0, n-i), s2.substring(i, n)) 
                && memoization(s1.substring(n-i, n), s2.substring(0, i))) {
                flag = true;
                map.put(key, flag);
                return true;
            }
        }

        map.put(key, flag);
        return flag;
    }

    public static void main(String[] args) {
        String s1 = "coder", s2 = "ocred";
        
        new ScrambledStrings(s1, s2, Methods.RECURSION, true);
        // new ScrambledStrings(s1, s2, Methods.TABULATION, true);
        new ScrambledStrings(s1, s2, Methods.MEMOIZATION, true);

    }
}
