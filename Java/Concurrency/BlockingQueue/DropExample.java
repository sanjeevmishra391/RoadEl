package Java.Concurrency.BlockingQueue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DropExample {

    private static final BlockingQueue<String> analyticsQueue = new ArrayBlockingQueue<>(3);

    public static void trackEvent(String event) {
        boolean accepted = analyticsQueue.offer(event);
        if (!accepted) {
            // Don't block
            System.out.println("Analytics queue full. Dropping: "+ event);
            return;
        }

        System.out.println("Analytics event accepted: " + event);
    }
}