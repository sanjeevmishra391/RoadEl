package Arrays;

import java.util.*;

public class Anagrams {

    static boolean areAnagrams(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        char arr[] = a.toCharArray();
        char brr[] = b.toCharArray();

        Arrays.sort(arr);
        Arrays.sort(brr);

        for(int i=0; i<arr.length; i++) {
            if (arr[i] != brr[i]) { 
                return false;
            }
        }

        return true;
    }

    static boolean areAnagramsUsingMap(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        Map<Character, Integer> map = new HashMap<>();

        for(char c: a.toCharArray()) {
            int value = map.getOrDefault(c, 0);
            map.put(c, value+1);
        }

        for(char c: b.toCharArray()) {
            if (!map.containsKey(c)) {
                return false;
            }

            int v = map.get(c);

            if(v==1) {
                map.remove(c);
                continue;
            }

            map.put(c, v-1);
        }

        if (map.isEmpty()) {
            return true;
        }

        return false;
    }

    public static void main(String[] args) {
        String a = "racecar", b = "caerace";
        System.out.println(areAnagrams(a, b));
        System.out.println(areAnagramsUsingMap(a, b));
    }
    
}
