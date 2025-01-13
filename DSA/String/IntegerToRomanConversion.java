package String;

import java.util.HashMap;

public class IntegerToRomanConversion {
    static HashMap<Integer, String> integerMap;
    static String getValue(int num, int multiplier) {
        String res = "";
        if(num>=1 && num<=3) {
            res += integerMap.get(1*multiplier).repeat(num);
        } else if(num == 4 || num == 9) {
            num++;
            res += integerMap.get(1*multiplier) + integerMap.get(num*multiplier);
        } else if(num == 5) {
            res += integerMap.get(num*multiplier);
        } else if(num>=6 && num<=8) {
            res += integerMap.get(5*multiplier) + getValue(num-5, multiplier);
        }
        return res;
    }
    public static void main(String[] args) {
        int num = 495; // CDXCV

        // 1 2 3 (repetition of 1), 4 (5-1), 5, 6 (5+1), 7 (5+2), 8 (5+3), 9 (10-1), 10
        integerMap = new HashMap<>();
        integerMap.put(1, "I");
        integerMap.put(5, "V");
        integerMap.put(10, "X");
        integerMap.put(50, "L");
        integerMap.put(100, "C");
        integerMap.put(500, "D");
        integerMap.put(1000, "M");

        String res = "";
        int multiplier = 1;
        while(num>0) {
            int rem = num%10;
            res = getValue(rem, multiplier) + res;
            num = num/10;
            multiplier *= 10;
        }

        System.out.println(res);
    }
}
