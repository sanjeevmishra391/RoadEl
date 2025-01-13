package String;

public class MinimumDistanceBetweenWords {
    public static void main(String[] args) {
        String s[] =  {"geeks", "for", "geeks", "contribute",  "practice"};
        String word1 = "geeks";
        String word2 = "practice";

        int d1 = -1, d2 = -1, min = Integer.MAX_VALUE;
        for(int i=0; i<s.length; i++) {
            String word = s[i];
            if(word.equals(word1)) {
                d1 = i;
            }

            if(word.equals(word2)) {
                d2 = i;
            }

            if(d1!=-1 && d2!=-1) {
                min = Math.min(min, Math.abs(d2-d1));
            }
        }
        System.out.println(min);
    }
}