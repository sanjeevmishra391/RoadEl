package Java.Concurrency.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class TimeoutExample {

    private static final BlockingQueue<String> queue = new ArrayBlockingQueue<>(3); // bounded

    public static boolean submitRequest(String task) {
        try {
            boolean accepted = queue.offer(task,500,TimeUnit.MILLISECONDS);

            if (!accepted) {
                // Queue remained full
                System.out.println("Queue full. Rejecting request.");

                // In real application:
                // return HTTP 503
                return false;
            }

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}