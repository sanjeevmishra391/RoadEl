package Java.String;

public class StringMain {
    public static void main(String[] args) {
        // case 1
        int three = 3;
        String four = "4";
        System.out.println(1 + 2 + three + four);

        // case 2
        String s1 = "1";
        String s2 = s1.concat("2");
        s2.concat("3");
        System.out.println(s2);

        StringBuilder sb = new StringBuilder("animals");
        String sub = sb.substring(sb.indexOf("a"), sb.indexOf("al"));
        int len = sb.length();
        char ch = sb.charAt(6);
        System.out.println(sub + " " + len + " " + ch);

    }
}
