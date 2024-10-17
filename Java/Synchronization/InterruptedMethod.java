package Java.Synchronization;

public class InterruptedMethod extends Thread {

    public void run() {
        for(int i=1; i<3; i++) {
            if(Thread.interrupted()) {
                System.out.println("Code of interrupted thread");
            } else {
                System.out.println("Normal Thread: " + Thread.currentThread().getName());
            }
        }
    }
    public static void main(String[] args) {
        InterruptedMethod t1 = new InterruptedMethod();
        InterruptedMethod t2 = new InterruptedMethod();
        t1.start();
        t1.interrupt();
        t2.start();
    }
}
