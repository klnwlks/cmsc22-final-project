package users;

import product.Product;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

// serializable is required for persistence
public class Transaction implements Serializable {

  private final User user;
  private final Product product;
  private final int amount;
  private final int quantity;
  private final LocalDateTime date;

  public Transaction(User user, Product product, int amount, int quantity) {
    this.user = user;
    this.product = product;
    this.amount = amount;
    this.quantity = quantity;
    this.date = LocalDateTime.now();
  }

  public String getTransac() {
    return product.getName() + " x " + quantity + " = " + amount;
  }

  public LocalDateTime getDate() {
    return date;
  }

  public User getUser() {
    return user;
  }

  public Product getProduct() {
    return product;
  }

  public int getAmount() {
    return amount;
  }

  public int getQuantity() {
    return quantity;
  }
}
