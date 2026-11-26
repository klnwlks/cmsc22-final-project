package inventory;

import product.Category;
import product.Product;
import users.Manager;
import users.Transaction;
import users.User;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Inventory implements Serializable {

  // use a hashmap so we can have product reference and their stock as integer
  private final HashMap<Product, Integer> inventory = new HashMap<>();

  // ung starter products ng tri
  public Inventory() {
    starter("Fresh Milk (1 L)", "1 L of fresh, locally produced carabao milk", 140, Category.MILK);
    starter("Fresh Milk (500 mL)", "500 mL of fresh, locally produced carabao milk", 75, Category.MILK);
    starter("Choco Milk (1 L)", "1 L of rich, sweet chocolate milk", 165, Category.MILK);
    starter("Choco Milk (500 mL)", "500 mL of rich, sweet chocolate milk", 85, Category.MILK);
    starter("Coffee Milk (1 L)", "1 L of mellow coffee milk", 175, Category.MILK);
    starter("Coffee Milk (500 mL)", "500 mL of mellow coffee milk", 90, Category.MILK);
    starter("Pandan Milk (1 L)", "1 L of creamy, refreshing pandan milk", 165, Category.MILK);
    starter("Pandan Milk (500 mL)", "500 mL of creamy, refreshing pandan milk", 85, Category.MILK);
    starter("Melon Milk (1 L)", "1 L of sweet melon milk", 165, Category.MILK);
    starter("Melon Milk (500 mL)", "500 mL of sweet melon milk", 85, Category.MILK);
    starter("Yogurt Plain (1 L)", "1 L of refreshing plain yogurt", 160, Category.YOGURT);
    starter("Yogurt Plain (500 mL)", "500 mL of refreshing plain yogurt", 80, Category.YOGURT);
    starter("Yogurt Strawberry (1 L)", "1 L of refreshing strawberry yogurt", 250, Category.YOGURT);
    starter("Yogurt Strawberry (500 mL)", "500 mL of refreshing strawberry yogurt", 125, Category.YOGURT);
    starter("Kesong Puti (200 g)", "200 g of mild, fresh kesong puti", 90, Category.CHEESE);
    starter("Butter", "A rich, smooth stick of butter", 160, Category.BUTTER);
    starter("Cheddar (200 g)", "200 g of smooth, flavorful cheddar cheese", 200, Category.CHEESE);
    starter("Mozzarella (1 kg)", "1 kg of savory, stretchy mozzarella cheese", 800, Category.CHEESE);
    starter("Pastillas de Leche (Plain, 18 pcs)", "A pack of soft, milky pastillas de leche", 130, Category.SWEETS);
    starter("Ice Cream (8 oz)", "8 oz of rich, creamy ice cream", 65, Category.ICECREAM);
    starter("Ice Cream (12 oz)", "12 oz of rich, creamy ice cream", 90, Category.ICECREAM);
  }

  // tldr ilagay ung stuff, default 20 stocks tayo
  private void starter(String name, String description, int price, Category category) {
    // also defualt iamges
    String image = switch (name) {
      case "Fresh Milk (1 L)" -> "Fresh-milk-1L.jpg";
      case "Fresh Milk (500 mL)" -> "Fresh-milk-500ml.jpg";
      case "Choco Milk (1 L)" -> "Choco-milk-1L.jpg";
      case "Choco Milk (500 mL)" -> "Choco-milk-500ml.jpg";
      case "Coffee Milk (1 L)" -> "Coffee-milk-1L.jpg";
      case "Coffee Milk (500 mL)" -> "Coffee-milk-500ml.jpg";
      case "Yogurt Plain (1 L)" -> "Yogurt-1L.jpg";
      case "Yogurt Plain (500 mL)" -> "Yogurt-500ml.jpg";
      case "Kesong Puti (200 g)" -> "Kesong-Puti.jpg";
      case "Mozzarella (1 kg)" -> "Mozzarella.jpg";
      case "Pastillas de Leche (Plain, 18 pcs)" -> "Pastillas-Plain.jpg";
      default -> null;
    };
    inventory.put(new Product(name, description, price, image == null ? null : "src/images/" + image, category), 20);
  }

  public Product addProduct(Product product, int quantity, User user) {
    // call to check if manager
    requireManager(user);
    if (inventory.containsKey(product)) {
      throw new IllegalArgumentException("product already exists in inventory");
    }
    inventory.put(product, quantity);
    return product;
  }

  public void removeProduct(Product product, User user) {
    requireManager(user);
    if (inventory.remove(product) == null) {
      throw new IllegalArgumentException("product does not exist in inventory");
    }
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

  public void voidTransaction(Transaction sale, User user) {
    if (sale == null || sale.isVoided() || !sale.getUser().getTransactions().contains(sale)) {
      throw new IllegalArgumentException("Select a recorded sale that has not been voided.");
    }
    // return sold product stock once voided
    Product product = sale.getProduct();
    Integer stock = inventory.get(product);
    if (stock != null) {
      inventory.put(product, Math.addExact(stock, sale.getQuantity()));
    }
    sale.markVoided();
  }

  // check if user is a manager
  private static void requireManager(User user) {
    if (!(user instanceof Manager)) {
      throw new SecurityException("only managers can manage products");
    }
  }
}
