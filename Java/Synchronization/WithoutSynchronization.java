package Java.Synchronization;

class TableWithout {
    void printTable(int n) {
        for(int i=1; i<5; i++) {
            try {
                System.out.println(n*i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

class MyThreadWithout extends Thread {
    TableWithout t;

    MyThreadWithout(TableWithout t) {
        this.t = t;
    }

    public void run() {
        t.printTable(5);
    }
}

class MyThreadAnotherWithout extends Thread {
    TableWithout t;

    MyThreadAnotherWithout(TableWithout t) {
        this.t = t;
    }

    public void run() {
        t.printTable(100);
    }
}

public class WithoutSynchronization {
    public static void main(String[] args) {
        TableWithout obj = new TableWithout();
        MyThreadWithout t1 = new MyThreadWithout(obj);
        MyThreadAnotherWithout t2 = new MyThreadAnotherWithout(obj);

        t1.start();
        t2.start();  
    }
}