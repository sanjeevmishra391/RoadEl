package SystemDesign.SolidPrinciples;

/*
```
class Bird {
    void fly() {
        System.out.println("This bird can fly.");
    }
}

class Sparrow extends Bird {
    @Override
    void fly() {
        System.out.println("Sparrow is flying.");
    }
}

class Penguin extends Bird {
    @Override
    void fly() {
        // Penguins cannot fly, but they are forced to implement fly() from Bird.
        throw new UnsupportedOperationException("Penguins cannot fly!");
    }
}
```

Now consider a method that expects any Bird to fly:
```
void letBirdFly(Bird bird) {
    bird.fly();
}
```

Problem:

- Passing a Penguin to letBirdFly breaks the program because penguins cannot fly.
- This violates LSP because the Penguin subclass does not behave like a proper replacement for Bird.

Fixing the Example (Adhering to LSP):

To follow LSP, the design must ensure that the behavior of the parent class applies to all subclasses. 
In this case, we can split the behavior into separate classes or interface.

 */

interface Flyable {
    void fly();
}

class Bird {
    void eat() {
        System.out.println("This bird is eating.");
    }
}

class Sparrow extends Bird implements Flyable {
    @Override
    public void fly() {
        System.out.println("Sparrow is flying.");
    }
}

class Penguin extends Bird {
    // Penguins don't implement Flyable since they can't fly.
}


public class LiskovSubstitution {

    static void letBirdFly(Flyable bird) {
        bird.fly();
    }
    
    public static void main(String[] args) {
        Sparrow sp = new Sparrow();
        letBirdFly(sp);

        Penguin pg = new Penguin();
        pg.eat();
        // letBirdFly(pg); // Error : penguine do not implement flyable as they don't fly.
    }
}
