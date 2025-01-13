package SystemDesign.Examples.VendingMachine;

public class VendingMachineDemo {
    public static void main(String[] args) {
        VendingMachine vendingMachine = VendingMachine.getInstance();

        // products
        Product pepsi = new Product("Pepsi", 20);
        Product kitkat = new Product("Kitkat", 10);
        Product chips = new Product("Chips", 5);

        // load inventory
        vendingMachine.inventory.addProduct(pepsi, 4);
        vendingMachine.inventory.addProduct(kitkat, 1);
        vendingMachine.inventory.addProduct(chips, 2);

        // display vending machine items
        vendingMachine.inventory.displayProducts();

        // select product : kitkat
        vendingMachine.selectProduct(kitkat);

        // insert coin
        vendingMachine.insertCoin(Coin.TWO);
        vendingMachine.insertCoin(Coin.FIVE);
        // insert note
        vendingMachine.insertNote(Note.TEN);


        // dispense item
        vendingMachine.dispenseProduct();

        // return change
        vendingMachine.returnChange();
    }
}
