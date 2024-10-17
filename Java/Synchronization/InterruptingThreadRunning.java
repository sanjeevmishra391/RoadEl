package Java.Synchronization;

public class InterruptingThreadRunning extends Thread {
    public void run() {
        try {
            Thread.sleep(1000);
            System.out.println("Task");
        } catch (InterruptedException e) {
            System.out.println("Exception handled :" + e);
        }
        
        System.out.println("Thread is running.");
    }
    public static void main(String[] args) {
        InterruptingThreadRunning t1 = new InterruptingThreadRunning();

        t1.start();
        t1.interrupt();
    }
}
