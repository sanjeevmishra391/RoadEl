package Java.MultiThreading.OrderProcessing;

import java.util.concurrent.*;

public class OrderProcessing {
    static final int MAX_TH = 3;
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        String orderId = "ORD1234";

        ExecutorService executor = Executors.newFixedThreadPool(MAX_TH);

        PaymentValidation paymentTask = new PaymentValidation(orderId);
        InventoryUpdate inventoryTask = new InventoryUpdate(orderId);
        SendMail mailTask = new SendMail(orderId);

        // Submit the tasks and get futures for each
        Future<String> paymentFuture = executor.submit(paymentTask);
        Future<String> inventoryFuture = executor.submit(inventoryTask);
        Future<String> mailFuture = executor.submit(mailTask);

        System.out.println(paymentFuture.get());
        System.out.println(inventoryFuture.get());
        System.out.println(mailFuture.get());

        System.out.println("Order Placed Successfully");

        executor.shutdown();
    }
}
