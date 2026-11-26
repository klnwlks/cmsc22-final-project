package screens;

import java.time.YearMonth; //changed Local date time to year month
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

//basta better na imports yung pinagnpalit ko
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField; //BAT wala to dito ???
import users.Transaction;
import users.User;


/**
 * Shows sales history and lets clerks and managers void recorded sales.
 */
public class TransactionLogsController {
  private static final int PAGE_SIZE = 8;
  private static final String ALL_MONTHS = "All months";
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a");

  private final system.System system;
  private final User currentUser;
  @FXML

  // added the rest of the labels and buttons for the UI BELOW IS YUNG PILLS SA FIGMA
  private Label transactionSummary, totalSales, totalTransactions, salesSubtitle, transactionsSubtitle, pageLabel;
  @FXML
  private TextField searchField;
  @FXML
  private ComboBox<String> monthFilter;
  // added the rest of the labels and buttons for the UI ABOVE IS YUNG PILLS
  
  //I just seperated everything nalang kasi it kept crashing when i used multiple ones on the same one
  @FXML
  private Button previousPageButton, nextPageButton, voidButton;
  @FXML
  private TableView<Transaction> transactionTable;
  @FXML
  private TableColumn<Transaction, String> dateColumn;
  @FXML
  private TableColumn<Transaction, String> userColumn;
  @FXML
  private TableColumn<Transaction, String> productColumn;
  @FXML
  private TableColumn<Transaction, String> quantityColumn;
  @FXML
  private TableColumn<Transaction, String> amountColumn;
  //

  private List<Transaction> transactions = List.of();
  private int currentPage;
  private boolean updatingMonthOptions;

  public TransactionLogsController(system.System system, User currentUser) {
    this.system = system;
    this.currentUser = currentUser;
  }

  @FXML
  private void initialize() {
    dateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(DATE_FORMAT.format(cell.getValue().getDate()))); //di ko magets yung last code oml masmadali to intindihin
    userColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUser().getName()));
    productColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getProduct().getName()));
    quantityColumn.setCellValueFactory(cell -> {
      int quantity = cell.getValue().getQuantity();
      return new ReadOnlyStringWrapper(quantity + (quantity == 1 ? " item" : " items"));
    });

    // changed the amount column to show ₱ sign and comma formatting sa PILL
    amountColumn.setCellValueFactory(cell -> {
      Transaction sale = cell.getValue();
      return new ReadOnlyStringWrapper(money(sale.getAmount()) + (sale.isVoided() ? " (voided)" : ""));
    });
    transactionTable.setItems(FXCollections.observableArrayList());
    voidButton.disableProperty().bind(transactionTable.getSelectionModel().selectedItemProperty()
        .map(Transaction::isVoided).orElse(true));

    // added the rest of the labels and buttons for the UI BELOW IS YUNG PILLS SA FIGMA
    refresh();
    monthFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (!updatingMonthOptions && newValue != null) {
        currentPage = 0;
        updateSummaryCards();
        updateVisibleTransactions();
      }
    });
    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      currentPage = 0;
      updateVisibleTransactions();
    });
    // mga searchfield, month chuchu, tas basta mismong pills lahat yan
  }

  @FXML
  public void refresh() {
    transactions = new ArrayList<>(system.getTransactions());
    transactions.sort(Comparator.comparing(Transaction::getDate).reversed());
    updateMonthOptions();
    updateSummaryCards();
    updateVisibleTransactions();
  }

  @FXML
  private void voidSale() {
    Transaction sale = transactionTable.getSelectionModel().getSelectedItem();
    if (sale == null || sale.isVoided()) {
      return;
    }
    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
        "Void this sale and return its quantity to stock if the product is still listed?",
        ButtonType.OK, ButtonType.CANCEL);
    confirmation.initOwner(voidButton.getScene().getWindow());
    if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
      try {
        system.getInventory().voidTransaction(sale, currentUser);
        transactionTable.getSelectionModel().clearSelection();
        refresh();
      } catch (IllegalArgumentException | ArithmeticException | SecurityException exception) {
        new Alert(Alert.AlertType.ERROR, "Could not void sale: " + exception.getMessage()).showAndWait();
      }
    }
  }

  private void updateMonthOptions() {
    String selected = monthFilter.getValue();
    Set<YearMonth> months = new TreeSet<>(Comparator.reverseOrder());
    transactions.stream()
        .map(transaction -> YearMonth.from(transaction.getDate()))
        .forEach(months::add);
    months.add(YearMonth.now());

    List<String> options = new ArrayList<>();
    options.add(ALL_MONTHS);
    months.stream().map(TransactionLogsController::monthLabel).forEach(options::add);

    updatingMonthOptions = true;
    monthFilter.setItems(FXCollections.observableArrayList(options));
    if (selected != null && options.contains(selected)) {
      monthFilter.setValue(selected);
    } else if (transactions.isEmpty()) {
      monthFilter.setValue(monthLabel(YearMonth.now()));
    } else {
      monthFilter.setValue(monthLabel(YearMonth.from(transactions.getFirst().getDate())));
    }
    updatingMonthOptions = false;
  }

  private void updateSummaryCards() {
    List<Transaction> periodTransactions = transactionsForSelectedMonth().stream()
        .filter(sale -> !sale.isVoided()).toList();
    long sales = periodTransactions.stream().mapToLong(Transaction::getAmount).sum();
    String period = monthFilter.getValue() == null || ALL_MONTHS.equals(monthFilter.getValue())
        ? "Across all months"
        : "For " + monthFilter.getValue();
    totalSales.setText(money(sales));
    totalTransactions.setText(Integer.toString(periodTransactions.size()));
    salesSubtitle.setText(period);
    transactionsSubtitle.setText(period);
  }

  private void updateVisibleTransactions() {
    List<Transaction> visible = transactionsForSelectedMonth().stream()
        .filter(this::matchesSearch)
        .toList();
    int pageCount = Math.max(1, (visible.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    currentPage = Math.min(currentPage, pageCount - 1);
    int fromIndex = Math.min(currentPage * PAGE_SIZE, visible.size());
    int toIndex = Math.min(fromIndex + PAGE_SIZE, visible.size());

    transactionTable.getItems().setAll(visible.subList(fromIndex, toIndex));
    if (visible.isEmpty()) {
      transactionSummary.setText("Showing 0 transactions");
    } else {
      transactionSummary.setText(
          "Showing " + (fromIndex + 1) + "-" + toIndex + " of " + visible.size() + " transactions");
    }
    pageLabel.setText((currentPage + 1) + " / " + pageCount);
    previousPageButton.setDisable(currentPage == 0);
    nextPageButton.setDisable(currentPage >= pageCount - 1);
  }

  private List<Transaction> transactionsForSelectedMonth() {
    String selectedMonth = monthFilter.getValue();
    if (selectedMonth == null || ALL_MONTHS.equals(selectedMonth)) {
      return transactions;
    }
    YearMonth month = parseMonth(selectedMonth);
    return transactions.stream()
        .filter(transaction -> YearMonth.from(transaction.getDate()).equals(month))
        .toList();
  }

  private boolean matchesSearch(Transaction transaction) {
    String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
    if (query.isEmpty()) {
      return true;
    }
    return transaction.getUser().getName().toLowerCase(Locale.ROOT).contains(query)
        || transaction.getProduct().getName().toLowerCase(Locale.ROOT).contains(query)
        || DATE_FORMAT.format(transaction.getDate()).toLowerCase(Locale.ROOT).contains(query)
        || Integer.toString(transaction.getQuantity()).contains(query)
        || money(transaction.getAmount()).toLowerCase(Locale.ROOT).contains(query);
  }

  @FXML
  private void previousPage() {
    if (currentPage > 0) {
      currentPage--;
      updateVisibleTransactions();
    }
  }

  @FXML
  private void nextPage() {
    currentPage++;
    updateVisibleTransactions();
  }

  private static String monthLabel(YearMonth month) {
    return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ", " + month.getYear();
  }

  private static YearMonth parseMonth(String label) {
    for (int month = 1; month <= 12; month++) {
      YearMonth candidate = YearMonth.of(Integer.parseInt(label.substring(label.lastIndexOf(' ') + 1)), month);
      if (monthLabel(candidate).equals(label)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("Invalid month filter: " + label);
  }

  private static String money(long amount) {
    return String.format(Locale.ROOT, "₱%,d.00", amount);
  }
}
