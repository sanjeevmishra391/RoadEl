package Java.OperatorsAndStatements;

public class Operators {
    public static void main(String[] args) {
        int x = 3;
        int y = ++x * 5 / x-- + --x;
        System.out.println("x is " + x);
        System.out.println("y is " + y);

        Integer a = 73;
        System.out.println(a instanceof Integer);
        
        long q = 5;
        long w = (q=3);
        System.out.println(q); // Outputs 3
        System.out.println(w); // Also, outputs 3
    }
}
