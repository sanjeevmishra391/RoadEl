package String;

public class MinimumWIndowSubstring {
    static String minWindow(String s, String t) {
        if(s.length() < t.length())
            return "";

        int mapT[] = new int[256];
        // store the freq of characters in t
        for(char c : t.toCharArray()) {
            mapT[c]++;
        }
        // this counter will help to find if all characters of t are have been found in the window of s
        int counter = t.length();
        int start = 0, end = 0, minLen = Integer.MAX_VALUE, startIndex = 0;
        while(end<s.length()) {
            // if current character is in s then decrement the counter as we found one character.
            if(mapT[s.charAt(end)] > 0) {
                counter--;
            }

            mapT[s.charAt(end)]--;
            end++;

            // counter will be 0 when all the characters have been found and are present in current window
            while(counter == 0) {
                if(end - start < minLen) {
                    minLen = end-start;
                    startIndex = start;
                }

                mapT[s.charAt(start)]++;

                if(mapT[s.charAt(start)] > 0) {
                    counter++;
                }

                start++;
            }
        }

        if(minLen == Integer.MAX_VALUE)
            return "";

        return s.substring(startIndex, startIndex+minLen);

    }

    public static void main(String[] args) {
        String s = "ADOBECODEBANC", t= "ABC";
        System.out.println(minWindow(s, t));
    }
}
