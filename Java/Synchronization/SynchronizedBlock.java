package Java.Synchronization;

class Table {
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

class MyThread extends Thread {
    Table t;

    MyThread(Table t) {
        this.t = t;
    }

    public void run() {
        t.printTable(5);
    }
}

class MyThreadAnother extends Thread {
    Table t;

    MyThreadAnother(Table t) {
        this.t = t;
    }

    public void run() {
        t.printTable(100);
    }
}

public class SynchronizedBlock {
    public static void main(String[] args) {
        Table obj = new Table();
        MyThread t1 = new MyThread(obj);
        MyThreadAnother t2 = new MyThreadAnother(obj);

        t1.start();
        t2.start();  
    }
}
