/*
 * Given a string str of length N which consists of only 0, 1 or 2s, count the number of substring which have equal number of 0s, 1s and 2s.
 */
package String;

import java.util.Arrays;
import java.util.HashMap;

public class Equal012 {

    static long getSubstringWithEqual012(String str) { 
        int count[] = new int[3];
        HashMap<String, Integer> map = new HashMap<>();
        map.put("0#0", 1);
        
        long ans = 0l;
        
        for(char ch : str.toCharArray()) {
            count[ch - '0']++;
            
            String key = (count[1] - count[0]) + "#" + (count[2] - count[1]);
            if(map.containsKey(key)) {
                ans = ans + (long) map.get(key);
                map.put(key, map.get(key) + 1);
            } else {
                map.put(key, 1);
            }
        }        
        
        return ans;
    }

    static boolean allEqual(int[] count) {
        if(count[0] == count[1] && count[1] == count[2])
            return true;

        return false;
    }
    public static void main(String[] args) {
        String s = "0102010";
        int n = s.length(), ans = 0;
        int count[] = new int[3];
        for(int i=0; i<n; i++) {
            for(int j=i; j<n; j++) {
                int ch = s.charAt(j) - '0';
                count[ch]++;

                if(allEqual(count)) {
                    ans++;
                }
            }
            Arrays.fill(count, 0);
        }

        System.out.println(ans);
    }
}
