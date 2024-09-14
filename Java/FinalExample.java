package Java;

// can not extend final class
final class NonExtendableClass {
    void run() {
        System.out.println("In the NonExtendableClass");
        System.out.println("Running method");
    }
}

// The type ExtendClass cannot subclass the final class NonExtendableClas
// class ExtendClass extends NonExtendableClass {
//     void run() {
//         System.out.println("In the ExtendClass");
//     }
// }

class Animal {
    // can not override final method
    final void makeSound() {
        System.out.println("Animal making sound");
    }
}

public class FinalExample extends Animal {
    // Cannot override the final method from Animal
    // void makeSound() {
    //     System.out.println("Another sound");
    // }

    public static void main(String[] args) {
        // can not change value of final variable
        final int a = 5;
        System.out.println(a);
        // The final local variable a cannot be assigned. It must be blank and not using a compound assignment
        // a = 10;
    }
}
