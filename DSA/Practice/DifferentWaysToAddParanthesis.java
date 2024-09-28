package Practice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DifferentWaysToAddParanthesis {

    static Map<String, List<Integer>> map = new HashMap<>();

    static List<Integer> diffWaysToCompute(String expression) {
        List<Integer> res = new ArrayList<>();
        for(int i=0; i<expression.length(); i++) {
            char c = expression.charAt(i);
            if(c == '+' || c == '-' || c =='*') {
                String left = expression.substring(0, i);
                String right = expression.substring(i+1);
                List<Integer> l = map.getOrDefault(left, diffWaysToCompute(left));
                List<Integer> r = map.getOrDefault(right, diffWaysToCompute(right));

                for(Integer j : l) {
                    for(Integer k : r) {
                        switch(c) {
                            case '+':
                                res.add(j + k);
                                break;
                            case '-':
                                res.add(j - k);
                                break;
                            case '*':
                                res.add(j * k);
                                break;
                        }
                    }
                }

            }
        }

        if(res.size() == 0) {
            res.add(Integer.valueOf(expression));
        }

        map.put(expression, res);

        return res;
    }

    public static void main(String[] args) {
        String expression = "2*3-4*5";
        System.out.println(diffWaysToCompute(expression).toString());
    }
}
