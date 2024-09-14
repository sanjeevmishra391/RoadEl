package Java.InnerClass;

interface Showable {
    void show(); // by default it's public and abstract
    interface Message {
        void msg(); // by default it's public and abstract
    }
}

public class InterfaceNestedInterface implements Showable.Message {
    public void msg() {
        System.out.println("Interface defined inside interface");
    }

    public static void main(String[] args) {
        Showable.Message obj = new InterfaceNestedInterface();
        obj.msg();
    }
}
