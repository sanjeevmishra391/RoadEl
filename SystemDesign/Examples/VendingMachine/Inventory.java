package SystemDesign.Examples.VendingMachine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Inventory {
    private final Map<Product, Integer> products;

    Inventory() {
        products = new ConcurrentHashMap<>();
    }

    public void addProduct(Product product, int quantity) {
        products.put(product, products.getOrDefault(product, 0) + quantity);
    }

    public void removeProduct(Product product) {
        products.remove(product);
    }

    public void updateQuantity(Product product, int quantity) {
        products.put(product, quantity);
    }

    public int getQuantity(Product product) {
        return products.getOrDefault(product, 0);
    }

    public boolean isAvailable(Product product) {
        return products.containsKey(product) && products.get(product) > 0;
    }

    public void displayProducts() {
        for(Map.Entry<Product, Integer> e : products.entrySet()) {
            System.out.println(e.getKey() + ", Quantity: " + e.getValue());
        }
    }

}
