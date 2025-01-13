package SystemDesign.Examples.VendingMachine.VendingMachineStates;

import SystemDesign.Examples.VendingMachine.Coin;
import SystemDesign.Examples.VendingMachine.Note;
import SystemDesign.Examples.VendingMachine.Product;

public interface VendingMachineState {
    void selectProduct(Product product);
    void insertCoin(Coin coin);
    void insertNote(Note note);
    void dispenseProduct();
    void returnChange();
}
