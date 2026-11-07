package Java.MultiThreading;

public class DaemonThread extends Thread {
    public void run() {
        if(Thread.currentThread().isDaemon()) {
            System.out.println("Daemon thread");
            try {
                System.out.println("Making it sleep");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Done...");
        } else {
            System.out.println("User thread");
        }
    }

    public static void main(String[] args) {
        DaemonThread t1 = new DaemonThread();
        DaemonThread t2 = new DaemonThread();
        DaemonThread t3 = new DaemonThread();

        t1.setDaemon(true);

        t1.start();
        t2.start();
        t3.start();

    }
}
