package screens;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import product.Product;
import users.Clerk;

public class SalesController {
  private record Line(Product product, int quantity) {
  }

  @FXML
  private FlowPane products;
  @FXML
  private TableView<Line> cart;
  @FXML
  private TableColumn<Line, String> productColumn, priceColumn, totalColumn;
  @FXML
  private TableColumn<Line, Integer> quantityColumn;
  @FXML
  private Label userLabel, totalLabel, message;

  private final Clerk clerk;
  private final Runnable onInventory, onTransactions;
  private final Map<Product, Integer> quantities = new LinkedHashMap<>();

  public SalesController(Clerk clerk, Runnable onInventory, Runnable onTransactions) {
    this.clerk = clerk;
    this.onInventory = onInventory;
    this.onTransactions = onTransactions;
  }

  @FXML
  private void initialize() {
    userLabel.setText(clerk.getName());
    productColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().product().getName()));
    quantityColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().quantity()));
    priceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(money(cell.getValue().product().getPrice())));
    totalColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
        money((long) cell.getValue().product().getPrice() * cell.getValue().quantity())));
    updateCart();
  }

  public void refresh() {
    products.getChildren().clear();
    // map put each product into
    clerk.getInventory().entrySet().stream()
        .sorted(Comparator.comparing(entry -> entry.getKey().getName(), String.CASE_INSENSITIVE_ORDER))
        .forEach(entry -> {
          Product product = entry.getKey();
          // load iamge
          ImageView image = new ImageView();
          image.setFitWidth(100);
          image.setFitHeight(80);
          image.setPreserveRatio(true);
          String source = product.getImageString();
          if (source != null && !source.isBlank()) {
            try {
              // regex to match ung image url dapat
              String url = source.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*") ? source : new File(source).toURI().toString();
              Image loaded = new Image(url, true);
              image.setImage(loaded);
            } catch (IllegalArgumentException ignored) {
              // ignore muna wala images
            }
          }
          Button add = new Button("Add to sale");
          add.setDisable(entry.getValue() <= 0);
          add.setOnAction(event -> add(product));
          products.getChildren().add(new VBox(5, image, new Label(product.getName()),
              new Label(money(product.getPrice()) + " · " + entry.getValue() + " in stock"), add));
        });
  }

  private void add(Product product) {
    int quantity = quantities.getOrDefault(product, 0);
    int stock = clerk.getInventory().getOrDefault(product, 0);

    // dheck if we have stock for taht
    if (quantity >= stock) {
      message.setText("No more stock for " + product.getName());
      return;
    }
    quantities.put(product, quantity + 1);
    message.setText("");
    updateCart();
  }

  @FXML
  private void removeSelected() {
    Line selected = cart.getSelectionModel().getSelectedItem();
    if (selected != null) {
      if (selected.quantity() == 1)
        quantities.remove(selected.product());
      else
        quantities.put(selected.product(), selected.quantity() - 1);
      updateCart();
    }
  }

  @FXML
  private void clear() {
    quantities.clear();
    message.setText("");
    updateCart();
  }

  @FXML
  private void completeSale() {
    if (quantities.isEmpty()) {
      message.setText("Add at least one product to complete the sale.");
      return;
    }

    // record transaction per item
    for (Map.Entry<Product, Integer> entry : quantities.entrySet()) {
      clerk.transaction(entry.getKey(), entry.getValue());
    }
    clear();
    refresh();
    message.setText("Sale completed.");
  }

  private void updateCart() {
    cart.setItems(FXCollections.observableArrayList(quantities.entrySet().stream()
        .map(entry -> new Line(entry.getKey(), entry.getValue())).toList()));
    double total = quantities.entrySet().stream()
        .mapToLong(entry -> entry.getKey().getPrice() * entry.getValue()).sum();
    totalLabel.setText(money(total));
  }

  private static String money(long amount) {
    return String.format("₱%,d", amount);
  }

  @FXML
  private void showInventory() {
    onInventory.run();
  }

  @FXML
  private void showTransactions() {
    onTransactions.run();
  }
}
