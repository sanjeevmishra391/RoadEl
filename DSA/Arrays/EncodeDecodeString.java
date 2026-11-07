package Arrays;

import java.util.ArrayList;
import java.util.List;

class Solution {

    public String encode(List<String> strs) {
        StringBuilder sb = new StringBuilder();

        for(String str: strs) {
            sb.append(str.length() + "#" + str);
        }

        return sb.toString();
    }

    public List<String> decode(String str) {
        List<String> res = new ArrayList<>();
        int i=0;
        while(i<str.length()) {
            int length = 0;
            char ch = str.charAt(i);
            while(Character.isDigit(ch)) {
                length = length *10 + (int) (ch - '0');
                i++;
                ch = str.charAt(i);
            }
            i++;
            String st = str.substring(i, i+length);
            i = i+length;
            res.add(st);
        }

        return res;
    }
}


public class EncodeDecodeString {
    public static void main(String[] args) {
        List<String> strs = List.of("Hello","World");

        Solution sb = new Solution();
        String enc = sb.encode(strs);
        System.out.println();
        System.out.println(enc);

        System.out.println(sb.decode(enc));
    }
}
