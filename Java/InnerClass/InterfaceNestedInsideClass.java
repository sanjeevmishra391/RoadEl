package Java.InnerClass;

class A {
    interface Showable {
        void msg();
    }
}

public class InterfaceNestedInsideClass implements A.Showable {
    public void msg() {
        System.out.println("Interface inside class");
    }
    public static void main(String[] args) {
        A.Showable obj = new InterfaceNestedInsideClass();
        obj.msg();
    }
}
