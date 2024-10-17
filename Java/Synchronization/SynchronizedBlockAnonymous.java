package Java.Synchronization;

class Sender {
    public void senderMessage(String msg) {
        System.out.println("Sending message: \"" + msg + "\"");

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Thread interrupted");
        }

        System.out.println("Message sent.\n");
    }
}

class SenderWThreads extends Thread {
    private String msg;
    Sender sd;

    SenderWThreads(String m, Sender obj) {
        msg = m;
        sd = obj;
    }

    public void run() {
        synchronized(sd) {
            sd.senderMessage(msg);
        }
    }

}

public class SynchronizedBlockAnonymous {
    public static void main(String[] args) {
        Sender sender = new Sender();
        SenderWThreads sender1 = new SenderWThreads( "Hola" , sender);
        SenderWThreads sender2 =  new SenderWThreads( "Welcome to club", sender);

        sender1.start();
        sender2.start();

        try {
            sender1.join();
            sender2.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
