package SystemDesign.SolidPrinciples;

/*
Think of a printer with multiple capabilities:
- Printing
- Scanning
- Faxing

Instead of one large interface like this:
```
interface MultifunctionDevice {
    void print();
    void scan();
    void fax();
}
```

Split it into smaller interfaces.
*/

interface Printable {
    void print();
}

interface Scannable {
    void scan();
}

interface Faxable {
    void fax();
}


class BasicPrinter implements Printable {
    @Override
    public void print() {
        System.out.println("Printing...");
    }
}

class AdvancedPrinter implements Printable, Scannable, Faxable {
    @Override
    public void print() {
        System.out.println("Printing...");
    }

    @Override
    public void scan() {
        System.out.println("Scanning...");
    }

    @Override
    public void fax() {
        System.out.println("Faxing...");
    }
}

public class InterfaceSegregation {
    public static void main(String[] args) {
        BasicPrinter bp = new BasicPrinter();
        bp.print();

        AdvancedPrinter ap  =new AdvancedPrinter();
        ap.print();
        ap.scan();
        ap.fax();
    }
}
