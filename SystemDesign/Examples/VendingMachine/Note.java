package SystemDesign.Examples.VendingMachine;

public enum Note {
    TEN(10),
    TWENTY(20),
    FIFTY(50),
    HUNDRED(100),
    FIVE_HUNDRED(500),
    TWO_THOUSAND(2000);

    private final double value;

    Note(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
