package Java.MultiThreading;

import java.util.concurrent.ExecutorService;  
import java.util.concurrent.Executors;  

public class ThreadPool {
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for(int i=0; i<10; i++) {
            Runnable t = new WorkerThread("" + i);
            executor.execute(t);
        }

        executor.shutdown();

        while(!executor.isTerminated()) { }

        System.out.println("Finished all threads");
    }
}
