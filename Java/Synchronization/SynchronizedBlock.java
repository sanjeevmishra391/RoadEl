package Java.Synchronization;

class TableBlock {
    void printTable(int n) {
        synchronized(this) {
            for(int i=1; i<n; i++) {
                System.out.println(n*i);
                try {
                    Thread.sleep(400);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class MyThreadBlock extends Thread {
    TableBlock t;

    MyThreadBlock(TableBlock t) {
        this.t = t;
    }

    public void run() {
        t.printTable(5);
    }
}

class MyThreadAnotherBlock extends Thread {
    TableBlock t;

    MyThreadAnotherBlock(TableBlock t) {
        this.t = t;
    }

    public void run() {
        t.printTable(100);
    }
}

public class SynchronizedBlock {
    public static void main(String[] args) {
        TableBlock obj = new TableBlock();
        MyThreadBlock t1 = new MyThreadBlock(obj);
        MyThreadAnotherBlock t2 = new MyThreadAnotherBlock(obj);

        t1.start();
        t2.start();  
    }
}
