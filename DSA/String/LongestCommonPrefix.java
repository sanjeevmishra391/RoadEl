/*
14. Longest Common Prefix
Write a function to find the longest common prefix string amongst an array of strings.
If there is no common prefix, return an empty string "".
 */

package String;

public class LongestCommonPrefix {

    static String commonPrefix(String words[]) {
        int j=0;

        while(j<words[0].length()) {
            boolean allSame = true;
            char c = words[0].charAt(j);
            for(int i=1; i<words.length; i++) {
                if(words[i].length() <= j || words[i].charAt(j) != c)
                    allSame = false;
            }

            if(!allSame)
                break;

            j++;
        }

        return words[0].substring(0, j);
    }
    public static void main(String[] args) {
        String words[] = {"geeksforgeeks", "geeks", "geek", "geezer", ""};
        System.out.println(commonPrefix(words));
    }
}
