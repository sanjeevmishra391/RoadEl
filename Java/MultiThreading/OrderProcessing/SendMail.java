package Java.MultiThreading.OrderProcessing;

import java.util.concurrent.Callable;

public class SendMail implements Callable<String> {
    private String orderId;

    public SendMail(String orderId) {
        this.orderId = orderId;
    }

    @Override
    public String call() throws Exception {
        // Simulating email sending logic
        Thread.sleep(1500);  // Simulating time-consuming task
        return "Confirmation email sent for order: " + orderId;
    }
}
