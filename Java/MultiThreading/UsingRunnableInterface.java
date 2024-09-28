package Java.MultiThreading;

public class UsingRunnableInterface implements Runnable {
    static UsingRunnableInterface obj;
    public void run() {
        System.out.println("Thread is running");
        try {
            for(int i=0; i<10; i++) {
                Thread.sleep(1000);
                System.out.println(i);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        obj = new UsingRunnableInterface();
        Thread t = new Thread(obj, "Child Thread");
        t.start();
    }
}
