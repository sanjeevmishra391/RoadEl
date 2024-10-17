package Java.Synchronization;

public class Deadlock {
    public static void main(String[] args) {
        final String resource1 = "Hello";
        final String resource2 = "Hello";

        Thread t1 = new Thread() {
            public void run() {
                synchronized(resource1) {
                    System.out.println("Thread 1 : locked resource1");
                
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
    
                    synchronized(resource2) {
                        System.out.println("THread 1 : locked resource2");
                    }
                }
            }
        };

        Thread t2 = new Thread() {
            public void run() {
                synchronized(resource2) {
                    System.out.println("Thread 2 : locked resource2");
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
    
                    synchronized(resource1) {
                        System.out.println("THread 2 : locked resource1");
                    }
                }
            }
        };

        t1.start();
        t2.start();
    }
}