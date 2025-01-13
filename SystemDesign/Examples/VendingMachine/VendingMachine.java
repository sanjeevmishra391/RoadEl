package SystemDesign.Examples.VendingMachine;

import SystemDesign.Examples.VendingMachine.VendingMachineStates.IdleState;
import SystemDesign.Examples.VendingMachine.VendingMachineStates.ReadyState;
import SystemDesign.Examples.VendingMachine.VendingMachineStates.DispenseState;
import SystemDesign.Examples.VendingMachine.VendingMachineStates.ReturnChangeState;
import SystemDesign.Examples.VendingMachine.VendingMachineStates.VendingMachineState;

/*
 * Requirements
The vending machine should support multiple products with different prices and quantities.
The machine should accept coins and notes of different denominations.
The machine should dispense the selected product and return change if necessary.
The machine should keep track of the available products and their quantities.
The machine should handle multiple transactions concurrently and ensure data consistency.
The machine should provide an interface for restocking products and collecting money.
The machine should handle exceptional scenarios, such as insufficient funds or out-of-stock 
 */

public class VendingMachine {

    private static VendingMachine instance;
    public Inventory inventory;
    private final VendingMachineState idleState;
    private final VendingMachineState readyState;
    private final VendingMachineState dispenseState;
    private final VendingMachineState returnChangeState;
    private VendingMachineState currentState;
    private Product selectedProduct;
    private double totalPayment;


    private VendingMachine() { // singleton design pattern
        inventory = new Inventory();
        idleState = new IdleState(this);
        readyState = new ReadyState(this);
        dispenseState = new DispenseState(this);
        returnChangeState = new ReturnChangeState(this);
        currentState = idleState;
        selectedProduct = null;
        totalPayment  = 0.0;
    }
    
    // singleton design pattern
    public static synchronized VendingMachine getInstance() {
        if(instance == null)
            instance = new VendingMachine();
        return instance;
    }

    // state methods
    public VendingMachineState getIdleState() {
        return idleState;
    }

    public VendingMachineState getReadyState() {
        return readyState;
    }

    public VendingMachineState getDispenseState() {
        return dispenseState;
    }

    public VendingMachineState getReturnChangeState() {
        return returnChangeState;
    }

    public void setState(VendingMachineState state) {
        this.currentState = state;
    }

    // product methods
    public void selectProduct(Product product) {
        this.currentState.selectProduct(product);
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product selectedProduct) {
        this.selectedProduct = selectedProduct;
    }

    public void dispenseProduct() {
        this.currentState.dispenseProduct();
    }

    public void resetSelectedProduct() {
        this.selectedProduct = null;
    }

    // payment methods
    public void insertCoin(Coin coin) {
        currentState.insertCoin(coin);
    }

    public void insertNote(Note note) {
        currentState.insertNote(note);
    }

    public void addCoin(Coin coin) {
        totalPayment += coin.getValue();
    }

    public void addNote(Note note) {
        totalPayment += note.getValue();
    }

    public double getTotalPayment() {
        return totalPayment;
    }

    public void returnChange() {
        this.currentState.returnChange();
    }

    public void resetPayment() {
        this.totalPayment = 0;
    }

}
