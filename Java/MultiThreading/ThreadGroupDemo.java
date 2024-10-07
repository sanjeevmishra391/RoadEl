package Java.MultiThreading;

public class ThreadGroupDemo implements Runnable {

    public void run() {
        System.out.println("Running thread : " + Thread.currentThread().getName());
    }
    public static void main(String[] args) {
        ThreadGroupDemo target = new ThreadGroupDemo();
        ThreadGroup tg = new ThreadGroup("Parent");

        Thread t1 = new Thread(tg, target, "Thread 1");
        Thread t2 = new Thread(tg, target, "Thread 2");
        Thread t3 = new Thread(tg, target, "Thread 3");
        
        t1.start();
        t2.start();
        t3.start();
        tg.list();

        System.out.println("Active bugs: " + tg.activeCount());

    }
}
