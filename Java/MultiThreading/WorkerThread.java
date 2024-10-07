package Java.MultiThreading;

public class WorkerThread implements Runnable {
    private String message;

    WorkerThread(String s) {
        message = s;
    }

    public void run() {
        System.out.println(Thread.currentThread().getName() + " (Start) message = "+ message);
        processMessage();
        System.out.println(Thread.currentThread().getName() + " (End) message = "+ message);
    }

    private void processMessage() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
