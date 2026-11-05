package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import screens.ProductInventoryController;
import screens.TransactionLogsController;
import users.User;

public class App extends Application {
  // system is a reserved keyword sa java but since spec demands we use system as
  // a class ughhhhh
  private final system.System system = new system.System();
  private ProductInventoryController inventoryController;
  private TransactionLogsController transactionController;
  private Scene inventoryScene;
  private Scene transactionScene;

  @Override
  public void start(Stage stage) throws java.io.IOException {
    User currentUser = system.newUser("Manager", "", "manager");
    inventoryController = new ProductInventoryController(system, currentUser, () -> showTransactions(stage));
    transactionController = new TransactionLogsController(system, currentUser, () -> showInventory(stage));

    // load both scenes with the same model and current user.
    FXMLLoader inventoryLoader = new FXMLLoader(App.class.getResource("/screens/ProductInventory.fxml"));
    inventoryLoader.setController(inventoryController);
    inventoryScene = new Scene(inventoryLoader.load(), 1000, 700);

    FXMLLoader transactionLoader = new FXMLLoader(App.class.getResource("/screens/TransactionLogs.fxml"));
    transactionLoader.setController(transactionController);
    transactionScene = new Scene(transactionLoader.load(), 1000, 700);

    showInventory(stage);
    stage.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
      if (isFocused) {
        if (stage.getScene() == transactionScene)
          transactionController.refresh();
        else
          inventoryController.refresh();
      }
    });
    stage.show();
  }

  private void showInventory(Stage stage) {
    inventoryController.refresh();
    stage.setScene(inventoryScene);
    stage.setTitle("Cowlendar - password123");
  }

  private void showTransactions(Stage stage) {
    transactionController.refresh();
    stage.setScene(transactionScene);
    stage.setTitle("Cowlendar - password123");
  }

  public static void main(String[] args) {
    launch(args);
  }
}
