package Java.Synchronization;

class TableSync {
    synchronized void printTable(int n) { // synchronized
        for(int i=1; i<5; i++) {
            try {
                System.out.println(n*i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

class MyThreadSync extends Thread {
    TableSync t;

    MyThreadSync(TableSync t) {
        this.t = t;
    }

    public void run() {
        t.printTable(5);
    }
}

class MyThreadAnother extends Thread {
    TableSync t;

    MyThreadAnother(TableSync t) {
        this.t = t;
    }

    public void run() {
        t.printTable(100);
    }
}

public class Synchronization {
    public static void main(String[] args) {
        TableSync obj = new TableSync();
        MyThreadSync t1 = new MyThreadSync(obj);
        MyThreadAnother t2 = new MyThreadAnother(obj);

        t1.start();
        t2.start();  
    }
}
