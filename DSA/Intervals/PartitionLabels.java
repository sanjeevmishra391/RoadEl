package Intervals;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class PartitionLabels {

    static List<Integer> partitionLabels(String s) {
        HashSet<Character> set = new HashSet<>();
        HashMap<Character, Integer> map = new HashMap<>();
        int n = s.length();
        List<Integer> arr = new ArrayList<>();

        for(int i=0; i<n; i++)
            map.put(s.charAt(i), map.getOrDefault(s.charAt(i), 0) + 1);

        int start = n;

        for(int i=n-1; i>=0; i--) {
            char key = s.charAt(i);
            int value = map.get(key);

            if(value == 1) {
                map.remove(key);
                set.remove(key);
            } else {
                set.add(key);
                map.put(key, value-1);
            }

            if(set.isEmpty()) {
                arr.add(0, start - i); 
                start = i;
            }
        }

        return arr;
    }

    public static void main(String[] args) {
        List<Integer> ans = partitionLabels("ababcbacadefegdehijhklij");

        for(int a:  ans)
            System.out.print(a + " ");
    }
    
}
