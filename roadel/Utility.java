package roadel;

public class Utility {

    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    public enum Methods {
        RECURSION,
        TABULATION,
        MEMOIZATION,
        OTHER
    }

    public enum Delimiter {
        INPUT,
        OUTPUT
    }

    // Generic method to handle printing of different types
    public static <T> void print(T param) {
        if (param instanceof Integer || param instanceof String || param instanceof Float || param instanceof Double || param instanceof Long || param instanceof Boolean) {
            System.out.print("   " + param);
        } else if (param instanceof int[]) {
            for (int i : (int[]) param) {
                System.out.printf("%3d ", i);
            }
        } else if (param instanceof int[][]) {
            for (int[] r : (int[][]) param) {
                for (int c : r) {
                    System.out.printf("%3d ", c);
                }
                System.out.println();
            }
        } else if (param instanceof String[]) {
            for (String r : (String[]) param) {
                System.out.printf("\"%s\"  ", r);
            }
        } else {
            System.out.print("Unknown input type");
        }
        System.out.println();  // To ensure a newline after printing
    }

    // Print method name with formatting
    public static void printMethod(Methods method) {
        System.out.println("-------------------------");
        System.out.println("Method: " + CYAN + method + RESET);
        System.out.println("-------------------------");
    }

    // Print results with method, multiple inputs, and output
    public static <T> void printRes(Methods method, Object... params) {
        printMethod(method);
    
        boolean isInputSection = false;
        boolean isOutputSection = false;
    
        for (Object param : params) {
            if (param instanceof Delimiter) {
                if (param == Delimiter.INPUT) {
                    isInputSection = true;
                    isOutputSection = false;
                    // System.out.println(GREEN + "Input:\n" + RESET);
                    System.out.println(GREEN);
                } else if (param == Delimiter.OUTPUT) {
                    isInputSection = false;
                    isOutputSection = true;
                    // System.out.println("\n" + YELLOW + "Output:\n" + RESET);
                    System.out.println(RESET + YELLOW);
                }
            } else if (param instanceof LabeledData<?>) {
                LabeledData<?> labeledData = (LabeledData<?>) param;
                System.out.println(labeledData.getLabel() + ": ");
                if(isInputSection)
                    print(labeledData.getData());
                if(isOutputSection)
                    print(labeledData.getData());
            }
        }

        System.out.println(RESET);
    }
}
