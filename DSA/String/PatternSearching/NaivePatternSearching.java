package String.PatternSearching;

public class NaivePatternSearching {

    static void search(String text, String pattern) {
        int textLen = text.length(), patLen = pattern.length();
        for(int i=0; i<=textLen-patLen; i++) {
            int j=0;
            for(; j<patLen; j++) {
                if(text.charAt(i+j) != pattern.charAt(j))
                    break;
            }

            if(j == patLen)
                System.out.println("Found at index: " + i);
        }
    }
    public static void main(String[] args) {
        String text = "AABAACAADAABAAABA";
        String pattern = "AABAA";
        search(text, pattern);
    }
}
