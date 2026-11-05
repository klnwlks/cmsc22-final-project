package screens;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import users.Transaction;
import users.User;

/**
 * Read-only view of System.getTransactions(), aggregating every user's history.
 */
public class TransactionLogsController {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final system.System system;
  private final User currentUser;
  private final Runnable onShowInventory;

  @FXML
  private Label userLabel;
  @FXML
  private Label transactionSummary;
  @FXML
  private TableView<Transaction> transactionTable;
  @FXML
  private TableColumn<Transaction, LocalDateTime> dateColumn;
  @FXML
  private TableColumn<Transaction, String> userColumn;
  @FXML
  private TableColumn<Transaction, String> productColumn;
  @FXML
  private TableColumn<Transaction, Number> quantityColumn;
  @FXML
  private TableColumn<Transaction, Number> amountColumn;

  public TransactionLogsController(system.System system, User currentUser, Runnable onShowInventory) {
    this.system = system;
    this.currentUser = currentUser;
    this.onShowInventory = onShowInventory;
  }

  @FXML
  private void initialize() {
    // gets na yan
    userLabel.setText(currentUser.getName());
    dateColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getDate()));
    // set date format to be readable
    dateColumn.setCellFactory(column -> new TableCell<>() {
      @Override
      protected void updateItem(LocalDateTime date, boolean empty) {
        super.updateItem(date, empty);
        setText(empty || date == null ? null : DATE_FORMAT.format(date));
      }
    });
    userColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUser().getName()));
    productColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getProduct().getName()));
    quantityColumn.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().getQuantity()));
    amountColumn.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().getAmount()));
    transactionTable.setItems(FXCollections.observableArrayList());
    refresh();
  }

  @FXML
  public void refresh() {
    transactionTable.getItems().setAll(system.getTransactions());
    transactionTable.sort();
    transactionSummary.setText("Showing " + transactionTable.getItems().size() + " transactions from all users");
  }

  @FXML
  private void showInventory() {
    onShowInventory.run();
  }
}
