package Java.Synchronization;

public class InterruptingThread extends Thread {
    public void run() {
        try {
            Thread.sleep(1000);
            System.out.println("Task");
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread interrupted... " + e);  
        }
    }
    public static void main(String[] args) {
        InterruptingThread t1 = new InterruptingThread();

        t1.start();

        try {
            t1.interrupt();
        } catch (Exception e) {
            System.out.println("Execption handled: " + e);
        }
    }
}
