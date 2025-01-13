/*
151. Reverse Words in a String

Given an input string s, reverse the order of the words.
A word is defined as a sequence of non-space characters. The words in s will be separated by at least one space.
Return a string of the words in reverse order concatenated by a single space.
Note that s may contain leading or trailing spaces or multiple spaces between two words. The returned string should only have a single space separating the words. Do not include any extra spaces.

 */

package String;

import java.util.Stack;

public class ReverseWordsInString {

    static String reverseWords(String s) {
        // insert words in stack
        StringBuilder sb = new StringBuilder();
        Stack<String> stack = new Stack<>();

        for(int i=0; i<s.length(); i++) {
            // if current character is dot and some word is found then add to stack
            if(s.charAt(i) == '.' && !sb.isEmpty()) {
                stack.push(sb.toString());
                sb.setLength(0);
            }

            // builds word
            if(s.charAt(i) != '.') {
                sb.append(s.charAt(i));
            }
        }

        // for the last word
        if(sb.length()>0) {
            stack.push(sb.toString());
        }

        sb.setLength(0);

        while(!stack.isEmpty()) {
            // remove the word from stack
            sb.append(stack.pop());
            // if it's not last word then add .
            if(!stack.isEmpty()) {
                sb.append(".");
            }
        }

        return sb.toString();
    }
    public static void main(String[] args) {
        String s = "...i.like.this..program.very.much...";
        System.out.println(reverseWords(s));
    }
}
