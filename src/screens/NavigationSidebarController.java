//BASTA SIDEBAR CONTROLLER TO FOR REUSABILITY V2 TAKE 2 ni greg
package screens;

import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import users.Manager;
import users.User;

public class NavigationSidebarController {
  //instead of seperate
  public enum Page {
    SALES,
    INVENTORY,
    TRANSACTIONS
  }

  @FXML
  private Button salesButton, inventoryButton, transactionButton, logoutButton;
  @FXML
  private Label userLabel, userInitial;

  public void configure(User currentUser, String salesButtonText, Runnable onSales, Runnable onInventory,
      Runnable onTransactions, Runnable onLogout) {
    Objects.requireNonNull(onSales);
    Objects.requireNonNull(onInventory);
    Objects.requireNonNull(onTransactions);
    Objects.requireNonNull(onLogout);
    //checks if non of the sidebars are missing
    
    // sets the user label and initial based on the current user type, pill to sa baba ng sidebar
    userLabel.setText(currentUser instanceof Manager ? "Manager" : "Employee");
    userInitial.setText(userLabel.getText().substring(0, 1));
    salesButton.setText(salesButtonText);
    salesButton.setOnAction(event -> onSales.run());
    inventoryButton.setOnAction(event -> onInventory.run());
    transactionButton.setOnAction(event -> onTransactions.run());
    logoutButton.setOnAction(event -> onLogout.run());
  }

  // updates current active page
  public void setActivePage(Page page) {
    setActive(salesButton, page == Page.SALES);
    setActive(inventoryButton, page == Page.INVENTORY);
    setActive(transactionButton, page == Page.TRANSACTIONS);
  }

  // updates anung button is highlighted based on current active page
  private static void setActive(Button button, boolean active) {
    button.getStyleClass().setAll("button", active ? "side-bar-pressed-btn" : "side-bar-btn");
  }
}
