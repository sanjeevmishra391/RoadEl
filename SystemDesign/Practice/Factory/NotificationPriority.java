package SystemDesign.Practice.Factory;

public enum NotificationPriority {
    LOW(1),
    DEFAULT(5),
    HIGH(10);

    private int value;

    NotificationPriority(int v) {
        this.value = v;
    }

    public int getValue() {
        return value;
    }
}
