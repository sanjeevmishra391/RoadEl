package roadel;

public class LabeledData<T> {
    private final String label;
    private final T data;

    public LabeledData(String label, T data) {
        this.label = label;
        this.data = data;
    }

    public String getLabel() {
        return label;
    }

    public T getData() {
        return data;
    }
}
