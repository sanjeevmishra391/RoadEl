package Java.Exception;

public class UserDefinedException extends Exception {
    public UserDefinedException(String str) {  
        // Calling constructor of parent Exception  
        super(str);  
        System.out.println("This is user defined exception.");
    }
}
