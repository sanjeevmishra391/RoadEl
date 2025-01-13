package String;

import java.util.HashMap;

public class RomanToIntegerConversion {
    public static void main(String[] args) {
        String num = "IX";
        // Start from left to right.
        // Convert each roman number sequentially.
        // Pick a letter, if it's value is less than the next one then get their value and find the difference.
        // else add the current letter's value.

        HashMap<Character, Integer> romanMap = new HashMap<>();
        romanMap.put('I', 1);
        romanMap.put('V', 5);
        romanMap.put('X', 10);
        romanMap.put('L', 50);
        romanMap.put('C', 100);
        romanMap.put('D', 500);
        romanMap.put('M', 1000);

        int j = 0, res = 0;
        while(j<num.length()) {
            char c0 = num.charAt(j);
            char c1 = j < num.length()-1 ? num.charAt(j+1) : ' ';
            if(romanMap.get(c0) >= romanMap.getOrDefault(c1, 0)) {
                res = res + romanMap.get(c0);
                j++;
            } else {
                res += (romanMap.get(c1) - romanMap.get(c0));
                j=j+2;
            }
        }

        System.out.println(res);
    }
}