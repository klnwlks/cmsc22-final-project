package screens;

import java.io.IOException;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Window;
import product.Category;
import product.Product;

public class ProductFormDialog extends Dialog<ProductFormDialog.Result> {
  // i use a record to easily return both product and stock
  public record Result(Product product, int stock) {
  }

  @FXML
  private TextField name;
  @FXML
  private TextField description;
  @FXML
  private TextField price;
  @FXML
  private TextField stock;
  @FXML
  private ComboBox<Category> category;
  @FXML
  private Label error;

  // gets na yan
  // be flexible and reuse the modal for add and edit product
  public ProductFormDialog(Window owner, Product existing, int quantity) throws IOException {
    initOwner(owner);
    initModality(Modality.WINDOW_MODAL);
    setTitle(existing == null ? "Add product" : "Edit product");

    FXMLLoader loader = new FXMLLoader(ProductFormDialog.class.getResource("ProductFormDialog.fxml"));
    loader.setController(this);

    setDialogPane(loader.load());
    category.setItems(FXCollections.observableArrayList(Category.values()));
    category.setValue(existing == null ? Category.MILK : existing.getCategory());
    stock.setText(Integer.toString(quantity));
    if (existing != null) {
      name.setText(existing.getName());
      description.setText(existing.getDescription());
      price.setText(Integer.toString(existing.getPrice()));
      stock.setEditable(false);
    }
    getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
      try {
        if (name.getText().isBlank())
          throw new IllegalArgumentException("Enter a product name.");
        if (Integer.parseInt(price.getText()) < 0 || Integer.parseInt(stock.getText()) < 0)
          throw new IllegalArgumentException("Price and stock must not be negative.");
      } catch (NumberFormatException exception) {
        error.setText("Enter whole numbers for price and stock.");
        event.consume();
      } catch (IllegalArgumentException exception) {
        error.setText(exception.getMessage());
        event.consume();
      }
    });

    // return data to the table, remove whitespacea nd etc
    setResultConverter(button -> button == ButtonType.OK
        ? new Result(new Product(name.getText().strip(), description.getText().strip(),
            Integer.parseInt(price.getText()), category.getValue()), Integer.parseInt(stock.getText()))
        : null);
  }
}
