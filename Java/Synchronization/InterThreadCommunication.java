package Java.Synchronization;

class Customer {
    int amount = 10000;

    synchronized void withdraw(int amount) {
        System.out.println("Withdrawing...");
        if(this.amount < amount) {
            System.out.println("Less balance; waiting for deposit...");
            try{
                wait();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        this.amount -= amount;
        System.out.println("Withdraw completed...");
    }

    synchronized void deposit(int amount) {
        System.out.println("Depositing...");    
        this.amount += amount;    
        System.out.println("Deposit completed... ");    
        notify();  
    }
}

public class InterThreadCommunication {
    public static void main(String[] args) {
        final Customer c = new Customer();

        new Thread() {
            public void run(){
                c.withdraw(15000);
            }    
        }.start();

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Thread() {    
            public void run() { 
                c.deposit(10000);
            }    
        }.start();    
    }
}
