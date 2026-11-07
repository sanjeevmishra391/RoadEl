package Java.Concurrency;

import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyPractice implements Runnable {

    AtomicInteger counter = new AtomicInteger(0);
    public void run() {
        for(int i=0; i<1000; i++) {
            this.counter.incrementAndGet();
        }
    }

    public static void main(String[] args) {

        ConcurrencyPractice ob = new ConcurrencyPractice();
        Thread a = new Thread(ob);
        Thread b = new Thread(ob);

        a.start();
        b.start();

        try {
            a.join();
            b.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(ob.counter.get());
    }
}
