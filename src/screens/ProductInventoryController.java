package screens;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

  // WAG NA NATIN TO PAG USPAN BASTA SINEPERATE KO LAHAT KASI ANG GULO SOBRA, TAS
  // NAGCRCRASH SYA PAG DI KO TALGA LIKE INIDENTIFY MUNA

  //ignore taas na comment di ko na sineperate para "reusable code", basta lahat to masmaayos pag ganto
  @FXML
  private TextField searchField;
  @FXML
  private Button addProductButton;
  @FXML
  private Button editProductButton, restockButton;
  @FXML
  private HBox categoryFilters;
  @FXML
  private Label totalProducts, categorySummary, productsInStock;
  @FXML
  private Label currentInStock, currentSummary, resultSummary;
  @FXML
  private Button previousPageButton, nextPageButton;
  @FXML
  private HBox pageButtons;
  // GENERAL PILLS NASA ABOVE
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
  // MGA PANG PAAYOS NA VARIABLES ang tass

  private final ObservableList<Entry> visibleProducts = FXCollections.observableArrayList();
  private final ToggleGroup categories = new ToggleGroup();
  private static final int PAGE_SIZE = 10;
  private Category selectedCategory;
  private List<Entry> matchingProducts = List.of();
  private int currentPage;
  private final Inventory inventory;
  private final User currentUser;

  public ProductInventoryController(system.System system, User currentUser) {
    this.inventory = system.getInventory();
    this.currentUser = currentUser;
  }

  @FXML
  private void initialize() {
    // disable if not manager
    addProductButton.setDisable(!(currentUser instanceof Manager));

    // else if manager, enable button
    if (currentUser instanceof Manager) {
      editProductButton.disableProperty().bind(
          productTable.getSelectionModel().selectedItemProperty().isNull());
    } else {
      editProductButton.setDisable(true);
    }
    // haha if edit disabled disable din restock
    restockButton.disableProperty().bind(editProductButton.disableProperty());

    productColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().product().getName()));
    categoryColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().product().getCategory()));
    sizeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().product().getDescription()));
    priceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper("₱" + cell.getValue().product().getPrice()));
    statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
        cell.getValue().stock() > 0 ? cell.getValue().stock() + " in stock" : "Out of Stock"));
    categoryColumn.setCellFactory(column -> new TableCell<>() {
      @Override
      protected void updateItem(Category category, boolean empty) {
        super.updateItem(category, empty);
        if (empty || category == null) {
          setGraphic(null);
          return;
        }
        Label badge = new Label(categoryLabel(category));
        badge.getStyleClass().addAll("category-badge", categoryStyleClass(category));
        setGraphic(badge);
      }
    });
    statusColumn.setCellFactory(column -> new TableCell<>() {
      @Override
      protected void updateItem(String status, boolean empty) {
        super.updateItem(status, empty);
        if (empty || status == null) {
          setGraphic(null);
          return;
        }
        Label badge = new Label(status);
        badge.getStyleClass().addAll("status-badge", status.equals("Out of Stock") ? "out-of-stock" : "available");
        setGraphic(badge);
      }
    });
    deleteColumn.setCellFactory(column -> new TableCell<>() {
      private final Button delete = new Button("×");
      {
        delete.setDisable(!(currentUser instanceof Manager));
        delete.getStyleClass().add("delete-button");
        delete.setTooltip(new Tooltip("Delete product"));
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
    addAllCategory();
    for (Category category : Category.values())
      addCategory(category);
    categories.getToggles().getFirst().setSelected(true);
    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      currentPage = 0;
      refresh();
    });
    productTable.setItems(visibleProducts);
    refresh();
  }

  private void addAllCategory() {
    ToggleButton button = new ToggleButton();
    button.setUserData(null);
    button.setToggleGroup(categories);
    button.setOnAction(event -> {
      button.setSelected(true);
      selectedCategory = null;
      currentPage = 0;
      refresh();
    });
    categoryFilters.getChildren().add(button);
  }

  // reusable function to create sortable categories
  private void addCategory(Category category) {
    ToggleButton button = new ToggleButton();
    button.setUserData(category);
    button.setToggleGroup(categories);
    button.setOnAction(event -> {
      button.setSelected(true);
      selectedCategory = category;
      currentPage = 0;
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
      products.sort(Comparator.comparing(entry -> entry.product().getName(), String.CASE_INSENSITIVE_ORDER));
    }

    String query = searchField.getText().strip().toLowerCase(Locale.ROOT);
    List<Entry> matches = new ArrayList<>();
    for (Entry entry : products) {
      if (selectedCategory != null && entry.product().getCategory() != selectedCategory) {
        continue;
      }
      if (!entry.product().getName().toLowerCase(Locale.ROOT).contains(query)) {
        continue;
      }
      matches.add(entry);
    }
    matchingProducts = matches;

    totalProducts.setText(Integer.toString(products.size()));
    categorySummary.setText("Across " + categoryCounts.size() + " categories");
    productsInStock.setText(Integer.toString(inStockCount));
    currentInStock.setText(Integer.toString(currentInStockCount));
    currentSummary.setText(selectedCategory == null ? "All categories" : categoryLabel(selectedCategory));
    for (Toggle toggle : categories.getToggles()) {
      Category category = (Category) toggle.getUserData();
      String label;
      int count;
      if (category == null) {
        label = "All";
        count = products.size();
      } else {
        label = categoryLabel(category);
        count = categoryCounts.getOrDefault(category, 0);
      }
      ((ToggleButton) toggle).setText(label + " " + count);
    }
    updatePagination();
  }

  private void updatePagination() {
    int pageCount = Math.max(1, (matchingProducts.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    currentPage = Math.min(currentPage, pageCount - 1);
    int fromIndex = Math.min(currentPage * PAGE_SIZE, matchingProducts.size());
    int toIndex = Math.min(fromIndex + PAGE_SIZE, matchingProducts.size());
    visibleProducts.setAll(matchingProducts.subList(fromIndex, toIndex));

    if (matchingProducts.isEmpty()) {
      resultSummary.setText("Showing 0 of 0 products");
    } else {
      resultSummary.setText(
          "Showing " + (fromIndex + 1) + "-" + toIndex + " of " + matchingProducts.size() + " products");
    }

    previousPageButton.setDisable(currentPage == 0);
    nextPageButton.setDisable(currentPage >= pageCount - 1);
    pageButtons.getChildren().clear();
    for (int page = 0; page < pageCount; page++) {
      int pageIndex = page;
      Button pageButton = new Button(Integer.toString(page + 1));
      pageButton.getStyleClass().add("page-number");
      if (page == currentPage) {
        pageButton.getStyleClass().add("current-page");
      }
      pageButton.setOnAction(event -> {
        currentPage = pageIndex;
        updatePagination();
      });
      pageButtons.getChildren().add(pageButton);
    }
  }

  @FXML
  private void previousPage() {
    if (currentPage > 0) {
      currentPage--;
      updatePagination();
    }
  }

  @FXML
  private void nextPage() {
    int pageCount = Math.max(1, (matchingProducts.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    if (currentPage < pageCount - 1) {
      currentPage++;
      updatePagination();
    }
  }

  private static String categoryLabel(Category category) {
    return switch (category) {
      case ICECREAM -> "Ice cream";
      default -> category.name().charAt(0) + category.name().substring(1).toLowerCase(Locale.ROOT);
    };
  }

  private static String categoryStyleClass(Category category) {
    return "category-" + category.name().toLowerCase(Locale.ROOT);
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

  @FXML
  private void restock() {
    Entry selected = productTable.getSelectionModel().getSelectedItem();
    if (selected == null || !(currentUser instanceof Manager manager))
      return;
    TextInputDialog dialog = new TextInputDialog();
    dialog.initOwner(productTable.getScene().getWindow());
    dialog.setTitle("Restock product");
    dialog.setHeaderText(selected.product().getName() + " · " + selected.stock() + " in stock");
    dialog.setContentText("Quantity to add:");
    dialog.showAndWait().ifPresent(value -> {
      try {
        manager.addStock(selected.product(), Integer.parseInt(value.strip()));
        refresh();
      } catch (IllegalArgumentException | ArithmeticException exception) {
        showError("Enter a positive whole quantity that fits in stock.");
      }
    });
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
              edited.getDescription(), edited.getPrice(), edited.getImageString(),
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
