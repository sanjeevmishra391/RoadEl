package Java.MultiThreading;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class MyCallable implements Callable<Integer> {
    public Integer call() {
        System.out.println(Thread.currentThread().getName() + " is calculating");
        return 42;  // Return a result
    }
}

public class ThreadPoolUsingCallable  {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(new MyCallable());

        // Check if the task is done
        if (!future.isDone()) {
            System.out.println("Task is still in progress...");
        }
        
        Integer result = future.get();  // Block and get the result
        System.out.println("Result from thread: " + result);
        
        executor.shutdown();
    }
}
