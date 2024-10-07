package Java.MultiThreading.OrderProcessing;

import java.util.concurrent.Callable;

public class PaymentValidation implements Callable<String> {

    private String orderId;

    PaymentValidation(String orderId) {
        this.orderId = orderId;
    }
    
    @Override
    public String call() throws Exception {
        // Simulating payment validation logic
        Thread.sleep(2000);  // Simulating time-consuming task
        return "Payment validated for order: " + orderId;
    }
}
