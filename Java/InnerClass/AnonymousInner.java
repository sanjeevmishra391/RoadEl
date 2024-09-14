package Java.InnerClass;

abstract class Person {
    abstract void eat();
}

public class AnonymousInner {
    public static void main(String[] args) {
        Person p = new Person() {
            void eat() {
                System.out.println("Person is eating.");
            }
        };

        p.eat();
    }
}
