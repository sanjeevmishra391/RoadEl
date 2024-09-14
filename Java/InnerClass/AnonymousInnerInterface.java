package Java.InnerClass;

interface Eatable {
    void eat();
}

public class AnonymousInnerInterface {
    public static void main(String[] args) {
        Eatable e = new Eatable() {
            public void eat() {
                System.out.println("Person is eating.");
            }
        };

        e.eat();
    }
}
