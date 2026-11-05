package inventory;

import product.Product;
import users.Clerk;
import users.Manager;
import users.Transaction;
import users.User;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Inventory implements Serializable {

  // use a hashmap so we can have product reference and their stock as integer
  private final HashMap<Product, Integer> inventory = new HashMap<>();

  public Product addProduct(Product product, int quantity, User user) {
    // call to check if manager
    requireManager(user);
    if (inventory.containsKey(product)) {
      throw new IllegalArgumentException("product already exists in inventory");
    }
    inventory.put(product, quantity);
    return product;
  }

  public void restockProduct(Product product, int newStock, User user) {
    requireManager(user);
    if (newStock < 0) {
      throw new IllegalArgumentException("stock must not be negative");
    }
    // map.put adds a new product if doesnt exist, else it just edits
    // we dont want the first case, so i call an exception if it isnt in the map in
    // the first place
    if (!inventory.containsKey(product)) {
      throw new IllegalArgumentException("product does not exist in inventory");
    }
    inventory.put(product, newStock);
  }

  public int getStock(Product product) {
    Integer stock = inventory.get(product);
    if (stock == null) {
      throw new IllegalArgumentException("product does not exist in inventory");
    }
    return stock;
  }

  public Map<Product, Integer> getInventory() {
    return new HashMap<>(inventory);
  }

  public Product findProduct(String productName) {
    Objects.requireNonNull(productName, "productName");
    Product match = null;
    // linear search product
    for (Product product : inventory.keySet()) {
      if (product.getName().equals(productName)) {
        if (match != null) {
          throw new IllegalStateException("multiple products have that name");
        }
        match = product;
      }
    }
    if (match == null) {
      throw new IllegalArgumentException("product does not exist in inventory");
    }
    return match;
  }

  public Transaction transaction(Product product, int quantity, User user) {
    User transactionUser = user;
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    int stock = getStock(product);
    if (quantity > stock) {
      throw new IllegalStateException("not enough stock");
    }
    int amount = product.getPrice() * quantity;

    // record in transaction object
    Transaction transaction = new Transaction(user, product, amount, quantity);
    // record new stock
    inventory.put(product, stock - quantity);
    user.addTransaction(transaction);
    return transaction;
  }

  // check if user is a manager
  private static void requireManager(User user) {
    if (!(Objects.requireNonNull(user, "user") instanceof Manager)) {
      throw new SecurityException("only managers can manage products");
    }
  }
}
