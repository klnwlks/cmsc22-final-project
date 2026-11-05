package product;

import java.io.Serializable;
import java.util.Objects;

public class Product implements Serializable {
  private String name;
  private String description;
  private int price;
  private String imageString;
  private Category category;

  public Product(String name, String description, int price, Category category) {
    this(name, description, price, null, category);
  }

  public Product(String name, String description, int price, String imageString, Category category) {
    this.name = name;
    this.description = description;
    if (price < 0) {
      throw new IllegalArgumentException("price must not be negative");
    }
    this.price = price;
    this.imageString = imageString;
    this.category = category;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public int getPrice() {
    return price;
  }

  public String getImageString() {
    return imageString;
  }

  public Category getCategory() {
    return category;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setPrice(int price) {
    if (price < 0) {
      throw new IllegalArgumentException("price must not be negative");
    }
    this.price = price;
  }

  public void setImageString(String imageString) {
    this.imageString = imageString;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public void setName(String name) {
    this.name = name;
  }
}
