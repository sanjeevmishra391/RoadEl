package Java.InnerClass;

public class StaticNestedClass {
    static int data = 30;
    static class Inner {
        void msg() {
            System.out.println("Data is " + data);
        }

        static void print() {
            System.out.println("Printing something");
        }
    }

    public static void main(String[] args) {
        StaticNestedClass.Inner obj = new StaticNestedClass.Inner();
        obj.msg();

        StaticNestedClass.Inner.print();
    }
}
