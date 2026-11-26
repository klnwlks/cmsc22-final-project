package screens;

import java.io.File;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import product.Category;
import product.Product;
import users.Clerk;

public class SalesController {
  private record Line(Product product, int quantity) {
  }

  @FXML
  private FlowPane products;
  @FXML
  private FlowPane categoryFilters;
  @FXML
  private TextField searchField;
  @FXML
  private TableView<Line> cart;
  @FXML
  private TableColumn<Line, String> productColumn, priceColumn, totalColumn;
  @FXML
  private TableColumn<Line, Integer> quantityColumn;
  @FXML
  private TableColumn<Line, Void> removeColumn;
  @FXML
  private Label totalLabel, message, currentMonth, title;
  @FXML
  private VBox salePanel;

  private final Clerk clerk;
  private final boolean readOnly;
  private final Map<Product, Integer> quantities = new LinkedHashMap<>();
  private final ObservableList<Line> cartItems = FXCollections.observableArrayList();
  private final ToggleGroup categories = new ToggleGroup();
  private Category selectedCategory;

  public SalesController(Clerk clerk, boolean readOnly) {
    this.clerk = clerk;
    this.readOnly = readOnly;
  }

  @FXML
  private void initialize() {
    if (readOnly) {
      title.setText("Clerk dashboard: " + clerk.getName());
      salePanel.setVisible(false);
      salePanel.setManaged(false);
    }
    YearMonth month = YearMonth.now();
    currentMonth.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        + ", " + month.getYear());
    productColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().product().getName()));
    quantityColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().quantity()));
    priceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(money(cell.getValue().product().getPrice())));
    totalColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
        money((double) cell.getValue().product().getPrice() * cell.getValue().quantity())));
    quantityColumn.setCellFactory(column -> new TableCell<>() {
      private final Button decrease = new Button("−");
      private final Label count = new Label();
      private final Button increase = new Button("+");
      private final HBox controls = new HBox(4, decrease, count, increase);
      {
        controls.setAlignment(Pos.CENTER);
        decrease.getStyleClass().add("quantity-button");
        increase.getStyleClass().add("quantity-button");
        count.getStyleClass().add("quantity-count");
        decrease.setOnAction(event -> {
          Line line = getTableRow().getItem();
          if (line != null) {
            changeQuantity(line.product(), -1);
          }
        });
        increase.setOnAction(event -> {
          Line line = getTableRow().getItem();
          if (line != null) {
            changeQuantity(line.product(), 1);
          }
        });
      }

      @Override
      protected void updateItem(Integer quantity, boolean empty) {
        super.updateItem(quantity, empty);
        count.setText(empty || quantity == null ? "" : Integer.toString(quantity));
        setGraphic(empty ? null : controls);
      }
    });
    removeColumn.setCellFactory(column -> new TableCell<>() {
      private final Button remove = new Button("×");
      {
        remove.getStyleClass().add("remove-button");
        remove.setOnAction(event -> {
          Line line = getTableRow().getItem();
          if (line != null) {
            quantities.remove(line.product());
            updateCart();
          }
        });
      }

      @Override
      protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : remove);
      }
    });
    cart.setItems(cartItems);
    addCategoryFilter(null);
    for (Category category : Category.values()) {
      addCategoryFilter(category);
    }
    categories.getToggles().getFirst().setSelected(true);
    searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());
    updateCart();
  }

  public void refresh() {
    // rebuild the cards and category counts from the current inventory.
    products.getChildren().clear();
    Map<Product, Integer> inventory = clerk.getInventory();
    Map<Category, Integer> categoryCounts = new java.util.EnumMap<>(Category.class);
    inventory.keySet().forEach(product -> categoryCounts.merge(product.getCategory(), 1, Integer::sum));
    for (Toggle toggle : categories.getToggles()) {
      Category category = (Category) toggle.getUserData();
      String label = category == null ? "All" : categoryLabel(category);
      int count = category == null ? inventory.size() : categoryCounts.getOrDefault(category, 0);
      ((ToggleButton) toggle).setText(label + "  " + count);
    }

    // apply the selected category and case-insensitive search before sorting.
    String query = searchField.getText().strip().toLowerCase(Locale.ROOT);
    inventory.entrySet().stream()
        .filter(entry -> selectedCategory == null || entry.getKey().getCategory() == selectedCategory)
        .filter(entry -> query.isEmpty()
            || entry.getKey().getName().toLowerCase(Locale.ROOT).contains(query)
            || (entry.getKey().getDescription() != null
                && entry.getKey().getDescription().toLowerCase(Locale.ROOT).contains(query)))
        .sorted(Comparator.comparing(entry -> entry.getKey().getName(), String.CASE_INSENSITIVE_ORDER))
        .forEach(entry -> {
          Product product = entry.getKey();
          ImageView image = new ImageView();
          image.setFitWidth(100);
          image.setFitHeight(68);
          image.setPreserveRatio(true);
          // ay basta image urls ung image natin
          String source = product.getImageString();
          if (source != null && !source.isBlank()) {
            try {
              image.setImage(new Image(new File(source).toURI().toString(), true));
            } catch (IllegalArgumentException ignored) {
              image.setImage(null);
            }
          }
          image.getStyleClass().add("product-image");
          StackPane imageFrame = new StackPane(image);
          imageFrame.setMinSize(100, 68);
          imageFrame.setPrefSize(100, 68);
          imageFrame.setMaxSize(100, 68);
          // reusable cards to show products na pwede ibenta
          // aalam mo dapat component to pero watevs
          Label name = new Label(product.getName());
          name.getStyleClass().add("product-name");
          name.setWrapText(true);
          Label description = new Label(product.getDescription() == null ? "" : product.getDescription());
          description.getStyleClass().add("product-description");
          description.setWrapText(true);
          Label price = new Label(money(product.getPrice()));
          price.getStyleClass().add("product-price");
          Label stock = new Label(entry.getValue() > 0 ? entry.getValue() + " in stock" : "Out of stock");
          stock.getStyleClass().add(entry.getValue() > 0 ? "product-stock" : "product-stock-empty");
          VBox content = new VBox(4, imageFrame, name, description, price, stock);
          content.setMinWidth(100);
          content.setPrefWidth(100);
          content.setMaxWidth(100);
          content.setAlignment(Pos.CENTER);
          content.getStyleClass().add("product-card-content");
          Button card = new Button();
          card.setGraphic(content);
          card.setDisable(readOnly || entry.getValue() <= 0);
          card.getStyleClass().add("product-card");
          card.setOnAction(event -> add(product));
          products.getChildren().add(card);
        });
  }

  private void addCategoryFilter(Category category) {
    ToggleButton button = new ToggleButton();
    button.setUserData(category);
    button.setToggleGroup(categories);
    button.setOnAction(event -> {
      button.setSelected(true);
      selectedCategory = category;
      refresh();
    });
    categoryFilters.getChildren().add(button);
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

  private void changeQuantity(Product product, int amount) {
    int quantity = quantities.getOrDefault(product, 0);
    if (amount > 0) {
      int stock = clerk.getInventory().getOrDefault(product, 0);
      if (quantity >= stock) {
        message.setText("No more stock for " + product.getName());
        return;
      }
      quantities.put(product, quantity + 1);
    } else if (quantity <= 1) {
      quantities.remove(product);
    } else {
      quantities.put(product, quantity - 1);
    }
    message.setText("");
    updateCart();
  }

  @FXML
  private void clear() {
    quantities.clear();
    message.setText("");
    updateCart();
  }

  @FXML
  private void completeSale() {
    if (readOnly)
      return;
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
    cartItems.setAll(quantities.entrySet().stream()
        .map(entry -> new Line(entry.getKey(), entry.getValue())).toList());
    double total = quantities.entrySet().stream()
        .mapToDouble(entry -> (double) entry.getKey().getPrice() * entry.getValue()).sum();
    totalLabel.setText(money(total));
  }

  private static String money(double amount) {
    return String.format("₱%,.2f", amount);
  }

  private static String categoryLabel(Category category) {
    return switch (category) {
      case ICECREAM -> "Ice cream";
      default -> category.name().charAt(0) + category.name().substring(1).toLowerCase(Locale.ROOT);
    };
  }
}
