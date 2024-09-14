package Java.Exception;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class JavaException {
    // ArithmeticException
    static void divide(int a, int b) {
        try {
            // if b is zero then exception will be thrown
            System.out.println(a/b);
        } catch (ArithmeticException e) {
            System.out.println("Division by zero is not allowed");
            System.out.println(e);
        } finally {
            System.out.println("End of function divide\n");
        }
    }

    // NullPointerException
    @SuppressWarnings("null")
    static void nullException() {
        try {
            String s = null;
            System.out.println(s.length());
        } catch (NullPointerException e) {
            System.out.println("Cannot perform this action on null data");
            System.out.println(e);
        } finally {
            System.out.println("End of function nullException\n");
        }
    }

    // NumberFormatException
    static void numberFormat() {
        try {
            String s = "abc";
            @SuppressWarnings("unused")
            int n = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            System.out.println("There is number format exception");
            System.out.println(e);
        } finally {
            System.out.println("End of function numberFormat\n");
        }
    }

    // ArrayIndexOutOfBoundsException
    static void arrayException() {
        try {
            int arr[] = new int[1];
            int a = arr[2];
            System.out.println(a);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Unable to access given index in array");
            System.out.println(e);
        } finally {
            System.out.println("End of function arrayException\n");
        }
    }

    // Unhandled Exception
    static void unhandledException() {
        int a = Integer.parseInt("asv");
        System.out.println(a+a);
    }

    static void unhandledException2() {
        int a = Integer.parseInt("asv");
        System.out.println(a+a);
    }

    // Multiple Exceptions
    static void multipleExceptions() {
        try {
            String str = "4";
            int a = Integer.parseInt(str);
            int b = 3;
            int res = a/b;
            System.out.println("Result = " + res);
            int arr[] = new int[1];
            int i = arr[3];
            System.out.println(i);
        } catch (ArithmeticException e) {
            System.out.println("Divison by zero is not allowed");
        } catch (NumberFormatException e) {
            System.out.println("Unable to convert String to Integer");
        } catch(Exception e) {
            System.out.println("Some error occured");
            System.out.println(e);
        } finally {
            System.out.println("End of function multipleExceptions\n");
        }
    }

    // the commented code will cause error
    static void unorderException() {
        try {    
            int a[]=new int[5];    
            a[5]=30/0;    
        } catch(Exception e) {
            System.out.println("common task completed");
        } 
        /* 
          catch(ArithmeticException e) {
             System.out.println("task1 is completed");
         } catch(ArrayIndexOutOfBoundsException e) { 
             System.out.println("task 2 completed");
         }  
         */  
        
        System.out.println("rest of the code...");    
    }

    // nested try
    static void nestedTry() {
        //outer try block   
        try {    
            //inner try block 1  
            try {    
                System.out.println("Dividing by 0");    
                int b =39/0;    
                System.out.println(b);
            }  
            //catch block of inner try block 1  
            catch(ArithmeticException e) {  
                System.out.println(e);  
            }    
                
            
            //inner try block 2  
            try {    
                int a[]=new int[5];    
                //assigning the value out of array bounds  
                a[5]=4;    
            }
            //catch block of inner try block 2  
            catch(ArrayIndexOutOfBoundsException e) {  
                System.out.println(e);  
            }    
            
                
            System.out.println("Other statement");    
        }  
        //catch block of outer try block  
        catch(Exception e) {  
            System.out.println("Handled the exception (outer catch)");  
        }    
        
        System.out.println("Normal flow\n");    
    }

    //function to check if person is eligible to vote or not   
    static void validate(int age) {  
        if(age<18) {  
            //throw Arithmetic exception if not eligible to vote  
            throw new ArithmeticException("Person is not eligible to vote");    
        } else {  
            System.out.println("Person is eligible to vote!!");  
        }  
    }

    // Throwing Checked Exception
    static void readFile() throws FileNotFoundException, IOException {
        FileReader file = new FileReader("./Java/Exception/abc.txt");  
        BufferedReader fileInput = new BufferedReader(file);
        String s = fileInput.readLine();
        System.out.println(s);
        fileInput.close();
        throw new FileNotFoundException();  
    }

    static void throwUserException() throws UserDefinedException {
        throw new UserDefinedException("Some error occured");
    }

    public static void main(String[] args) {
        // ArithmeticException
        divide(5, 0);

        // NullPointerException
        nullException();

        // NumberFormatException
        numberFormat();

        // ArrayIndexOutOfBoundsException
        arrayException();

        // handle unhandled exception
        try {
            unhandledException();
        } catch (Exception e) {
            System.out.println("Please resolve the below issue");
            System.out.println(e + "\n");
        }

        // calling unhandledException2
        // unhandledException2();

        // non callable
        // multipleExceptions
        multipleExceptions();

        // nested try
        nestedTry();

        // throw
        // validate(5);

        // throws
        try {
            readFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println();
        }

        // user defined exception
        try {
            throwUserException();
        } catch (UserDefinedException e) {
            System.out.println(e);
        }
    }
}
