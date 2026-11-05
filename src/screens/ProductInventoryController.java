package screens;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import inventory.Inventory;
import users.Manager;
import users.User;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import product.Category;
import product.Product;

public class ProductInventoryController {
  private record Entry(Product product, int stock) {
  }

  @FXML
  private TextField searchField;
  @FXML
  private Button addProductButton;
  @FXML
  private Button editProductButton;
  @FXML
  private Label userLabel;
  @FXML
  private HBox categoryFilters;
  @FXML
  private Label totalProducts, categorySummary, productsInStock;
  @FXML
  private Label currentInStock, currentSummary, resultSummary;
  @FXML
  private TableView<Entry> productTable;
  @FXML
  private TableColumn<Entry, String> productColumn, sizeColumn;
  @FXML
  private TableColumn<Entry, Category> categoryColumn;
  @FXML
  private TableColumn<Entry, String> priceColumn, statusColumn;
  @FXML
  private TableColumn<Entry, Void> deleteColumn;

  private final ObservableList<Entry> visibleProducts = FXCollections.observableArrayList();
  private final ToggleGroup categories = new ToggleGroup();
  private Category selectedCategory;
  private final Inventory inventory;
  private final User currentUser;
  private final Runnable onShowTransactions;

  public ProductInventoryController(system.System system, User currentUser, Runnable onShowTransactions) {
    this.inventory = system.getInventory();
    this.currentUser = currentUser;
    this.onShowTransactions = onShowTransactions;
  }

  @FXML
  private void initialize() {
    userLabel.setText(currentUser.getName());
    // disable if not manager
    addProductButton.setDisable(!(currentUser instanceof Manager));

    // else if manager, enable button
    if (currentUser instanceof Manager) {
      editProductButton.disableProperty().bind(
          productTable.getSelectionModel().selectedItemProperty().isNull());
    } else {
      editProductButton.setDisable(true);
    }

    productColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().product().getName()));
    categoryColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().product().getCategory()));
    sizeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().product().getDescription()));
    priceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper("₱" + cell.getValue().product().getPrice()));
    statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
        cell.getValue().stock() > 0 ? "Available" : "Out of Stock"));
    deleteColumn.setCellFactory(column -> new TableCell<>() {
      private final Button delete = new Button("Delete");
      {
        delete.setDisable(!(currentUser instanceof Manager));
        delete.setOnAction(event -> {
          Entry entry = getTableRow().getItem();
          if (entry == null)
            return;
          // show confirmation dialog
          Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
              "Delete " + entry.product().getName() + "?", ButtonType.OK, ButtonType.CANCEL);
          confirmation.initOwner(productTable.getScene().getWindow());
          confirmation.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> {
            try {
              inventory.removeProduct(entry.product(), currentUser);
              refresh();
            } catch (IllegalArgumentException | SecurityException exception) {
              showError(exception.getMessage());
            }
          });
        });
      }

      @Override
      protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : delete);
      }
    });

    // init categories
    for (Category category : Category.values())
      addCategory(category);
    categories.getToggles().getFirst().setSelected(true);
    searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());
    productTable.setItems(visibleProducts);
    refresh();
  }

  // reusable function to create sortable categories
  private void addCategory(Category category) {
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

  // tldr get from systm the inventory, get the proper values
  // update table, the summary, etc
  // show filtered in query, by cateogry, new numnber of products
  public void refresh() {
    List<Entry> products = new ArrayList<>();
    Map<Category, Integer> categoryCounts = new HashMap<>();
    int inStockCount = 0;
    int currentInStockCount = 0;

    for (Map.Entry<Product, Integer> item : inventory.getInventory().entrySet()) {
      Product product = item.getKey();
      int stock = item.getValue();
      products.add(new Entry(product, stock));

      Category category = product.getCategory();
      int count = categoryCounts.getOrDefault(category, 0);
      categoryCounts.put(category, count + 1);

      if (stock > 0) {
        inStockCount++;
        if (selectedCategory == null || category == selectedCategory) {
          currentInStockCount++;
        }
      }
    }

    String query = searchField.getText().strip().toLowerCase();
    List<Entry> matches = new ArrayList<>();
    for (Entry entry : products) {
      if (selectedCategory != null && entry.product().getCategory() != selectedCategory) {
        continue;
      }
      if (!entry.product().getName().toLowerCase().contains(query)) {
        continue;
      }
      matches.add(entry);
    }
    visibleProducts.setAll(matches);

    totalProducts.setText(Integer.toString(products.size()));
    categorySummary.setText("Across " + categoryCounts.size() + " categories");
    productsInStock.setText(Integer.toString(inStockCount));
    currentInStock.setText(Integer.toString(currentInStockCount));
    currentSummary.setText(selectedCategory == null ? "All categories" : selectedCategory.name());
    resultSummary.setText("Showing " + visibleProducts.size() + " of " + products.size() + " products");

    for (Toggle toggle : categories.getToggles()) {
      Category category = (Category) toggle.getUserData();
      String label;
      int count;
      if (category == null) {
        label = "All";
        count = products.size();
      } else {
        label = category.name();
        count = categoryCounts.getOrDefault(category, 0);
      }
      ((ToggleButton) toggle).setText(label + " " + count);
    }
  }

  // change scene
  @FXML
  private void showTransactions() {
    onShowTransactions.run();
  }

  // open dialog
  @FXML
  private void addProduct() {
    showProductDialog(null);
  }

  @FXML
  private void editProduct() {
    Entry selected = productTable.getSelectionModel().getSelectedItem();
    if (selected != null && currentUser instanceof Manager) {
      showProductDialog(selected);
    }
  }

  // add and edit modals
  private void showProductDialog(Entry existing) {
    ProductFormDialog dialog;
    try {
      dialog = new ProductFormDialog(productTable.getScene().getWindow(),
          existing == null ? null : existing.product(), existing == null ? 0 : existing.stock());
    } catch (IOException exception) {
      showError("Could not open product form: " + exception.getMessage());
      return;
    }
    dialog.showAndWait().ifPresent(entry -> {
      try {
        if (existing == null) {
          inventory.addProduct(entry.product(), entry.stock(), currentUser);
        } else if (currentUser instanceof Manager manager) {
          Product edited = entry.product();
          manager.editProduct(existing.product().getName(), edited.getName(),
              edited.getDescription(), edited.getPrice(), existing.product().getImageString(),
              edited.getCategory());
        } else {
          throw new SecurityException("only managers can manage products");
        }
        refresh();
      } catch (IllegalArgumentException | IllegalStateException | SecurityException exception) {
        showError(exception.getMessage());
      }
    });
  }

  private void showError(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR, message);
    alert.initOwner(productTable.getScene().getWindow());
    alert.showAndWait();
  }

}
