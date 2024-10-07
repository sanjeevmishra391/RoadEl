package Java.MultiThreading.OrderProcessing;

import java.util.concurrent.Callable;

public class InventoryUpdate implements Callable<String> {
    private String orderId;

    InventoryUpdate(String orderId) {
        this.orderId = orderId;
    }
    
    @Override
    public String call() throws Exception {
        // Simulating payment validation logic
        Thread.sleep(2000);  // Simulating time-consuming task
        return "Inventory updated for order: " + orderId;
    }
}
