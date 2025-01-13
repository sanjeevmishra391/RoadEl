package SystemDesign.Examples.VendingMachine.VendingMachineStates;

import SystemDesign.Examples.VendingMachine.Coin;
import SystemDesign.Examples.VendingMachine.Note;
import SystemDesign.Examples.VendingMachine.Product;
import SystemDesign.Examples.VendingMachine.VendingMachine;

public class ReadyState implements VendingMachineState {
    private final VendingMachine vendingMachine;

    public ReadyState(VendingMachine vendingMachine) {
        this.vendingMachine = vendingMachine;
    }

    @Override
    public void selectProduct(Product product) {
        System.out.println("Product already selected. Make the payment");
    }

    @Override
    public void insertCoin(Coin coin) {
        vendingMachine.addCoin(coin);
        System.out.println("Coin inserted: "+ coin);
        checkPaymentStatus();
    }

    @Override
    public void insertNote(Note note) {
        vendingMachine.addNote(note);
        System.out.println("Note inserted: "+ note);
        checkPaymentStatus();
    }

    @Override
    public void dispenseProduct() {
        System.out.println("Please make payment first.");
    }

    @Override
    public void returnChange() {
        System.out.println("Please make payment first.");
    }
    
    private void checkPaymentStatus() {
        if(vendingMachine.getTotalPayment() >= vendingMachine.getSelectedProduct().getPrice()) {
            vendingMachine.setState(vendingMachine.getDispenseState());
        }
    }
}
