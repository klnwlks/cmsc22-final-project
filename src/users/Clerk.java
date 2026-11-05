package users;

import inventory.Inventory;
import product.Product;

import java.util.Map;
import java.util.Objects;

public class Clerk extends User {

  private final Inventory inventory;

  public Clerk(String name, String password, Inventory inventory) {
    super(name, password);
    this.inventory = Objects.requireNonNull(inventory, "inventory");
  }

  public Transaction transaction(Product product, int quantity) {
    return inventory.transaction(product, quantity, this);
  }

  public Map<Product, Integer> getInventory() {
    return inventory.getInventory();
  }
}
