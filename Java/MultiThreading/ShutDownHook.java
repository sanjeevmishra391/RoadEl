package Java.MultiThreading;

public class ShutDownHook {
    public static void main(String[] args) {
        Runtime r = Runtime.getRuntime();

        r.addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Shutdown hook task completed");
            }
        });

        System.out.println("Main thread is sleeping, press Ctrl-C to exit.");
        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
