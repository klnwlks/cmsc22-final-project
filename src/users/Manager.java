package users;

import inventory.Inventory;
import product.Product;
import product.Category;
import system.System;

import java.util.ArrayList;
import java.util.Objects;

public class Manager extends User {
  private final Inventory inventory;
  private final System system;

  // inventory is a reference to the inventory held by sysstem
  public Manager(String name, String password, Inventory inventory, System system) {
    super(name, password);
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    this.system = Objects.requireNonNull(system, "system");
  }

  // call inventory methods for this, we pass this param to indicate we are a
  // manageer
  public Product addProduct(Product product, int quantity) {
    return inventory.addProduct(product, quantity, this);
  }

  // restock calls a hashmap.put() method
  public void addStock(Product product, int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    inventory.restockProduct(product, inventory.getStock(product) + quantity, this);
  }

  public Product searchProduct(String productName) {
    return inventory.findProduct(productName);
  }

  // manager can edit product
  public Product editProduct(String productName, String name, String description, int price,
      String imageString, Category category) {
    if (price < 0) {
      throw new IllegalArgumentException("price must not be negative");
    }
    Product product = inventory.findProduct(productName);
    product.setName(name);
    product.setDescription(description);
    product.setPrice(price);
    product.setImageString(imageString);
    product.setCategory(category);
    return product;
  }

  public Transaction transaction(Product product, int quantity) {
    return inventory.transaction(product, quantity, this);
  }

  public ArrayList<Transaction> getTransactionLog() {
    return system.getTransactions();
  }
}
