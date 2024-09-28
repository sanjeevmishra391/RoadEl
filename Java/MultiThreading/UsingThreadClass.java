package Java.MultiThreading;

public class UsingThreadClass extends Thread {
    static UsingThreadClass obj;
    public void run() {
        System.out.println("In the thread now");
        System.out.println("Thread state while running: " + obj.getState());
        System.out.println("Thread id: " + obj.getId());
        System.out.println("Thread: " + obj.toString());
        System.out.println("Thread group: " + obj.getThreadGroup());
    }

    public static void main(String[] args) {
        obj = new UsingThreadClass();
        // is thread alive
        System.out.println("Is thread alive : " + obj.isAlive());
        // set the name of thread;
        obj.setName("Child Thread");
        // get the priority of thread.
        System.out.println("Priority of thread : " + obj.getPriority());
        // thread status
        System.out.println("Thread status before start : " + obj.getState());
        // start the thread
        obj.start();
        // get the thread name
        System.out.println("Thread name: "+ obj.getName());
        // is thread alive
        System.out.println("Is thread alive : " + obj.isAlive());
        // thread status
        System.out.println("Thread status after running finished : " + obj.getState());
    }
}
