package main;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import system.System;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import screens.LoginController;
import screens.SalesController;
import users.Clerk;
import screens.ProductInventoryController;
import screens.TransactionLogsController;
import users.User;

public class App extends Application {
  // system is a reserved keyword sa java but since spec demands we use system as
  // a class ughhhhh
  // default path of save file, on close it saves
  private static final Path DATA_FILE = Path.of("data", "cowlendar.dat");
  private System system;
  private ProductInventoryController inventoryController;
  private TransactionLogsController transactionController;
  private Scene inventoryScene;
  private Scene transactionScene;
  private Scene salesScene;
  private SalesController salesController;

  @Override
  public void start(Stage stage) throws java.io.IOException {
    try {
      system = System.load(DATA_FILE);
    } catch (NoSuchFileException exception) {
      system = new system.System();
    } catch (IOException | ClassNotFoundException exception) {
      Platform.exit();
      return;
    }

    // default is now ung user login
    // by default nagcrecreate sha ng user rather than actually checking if meron
    // katamad magmemorize password
    FXMLLoader loginLoader = new FXMLLoader(App.class.getResource("/screens/Login.fxml"));
    loginLoader.setController(new LoginController(system, user -> {
      try {
        showUserScreens(stage, user);
        system.save(DATA_FILE);
      } catch (IOException exception) {
        new Alert(Alert.AlertType.ERROR, "Could not open the account: " + exception.getMessage()).showAndWait();
      }
    }));
    stage.setScene(new Scene(loginLoader.load()));
    stage.setTitle("Cowlendar ");

    // save on close of root
    stage.setOnCloseRequest(event -> {
      try {
        system.save(DATA_FILE);
      } catch (IOException exception) {
        new Alert(Alert.AlertType.ERROR, "Could not save data: " + exception.getMessage()).showAndWait();
      }
    });
    stage.show();
  }

  private void showUserScreens(Stage stage, User currentUser) throws IOException {
    inventoryController = new ProductInventoryController(system, currentUser, () -> showTransactions(stage));
    transactionController = new TransactionLogsController(system, currentUser, () -> showInventory(stage));

    FXMLLoader inventoryLoader = new FXMLLoader(App.class.getResource("/screens/ProductInventory.fxml"));
    inventoryLoader.setController(inventoryController);
    inventoryScene = new Scene(inventoryLoader.load(), 1000, 700);

    FXMLLoader transactionLoader = new FXMLLoader(App.class.getResource("/screens/TransactionLogs.fxml"));
    transactionLoader.setController(transactionController);
    transactionScene = new Scene(transactionLoader.load(), 1000, 700);

    // dont show sales screen to maanger
    Button inventorySales = (Button) inventoryLoader.getNamespace().get("salesButton");
    Button transactionSales = (Button) transactionLoader.getNamespace().get("salesButton");
    boolean isClerk = currentUser instanceof Clerk;
    inventorySales.setVisible(isClerk);
    inventorySales.setManaged(isClerk);
    transactionSales.setVisible(isClerk);
    transactionSales.setManaged(isClerk);
    if (currentUser instanceof Clerk clerk) {
      // lambda funcs para may reference to change screeens
      salesController = new SalesController(clerk, () -> showInventory(stage), () -> showTransactions(stage));
      FXMLLoader salesLoader = new FXMLLoader(App.class.getResource("/screens/Sales.fxml"));
      salesLoader.setController(salesController);
      salesScene = new Scene(salesLoader.load(), 1000, 700);
      inventorySales.setOnAction(event -> showSales(stage));
      transactionSales.setOnAction(event -> showSales(stage));
      showSales(stage);
      return;
    }
    showInventory(stage);
  }

  private void showSales(Stage stage) {
    salesController.refresh();
    stage.setScene(salesScene);
    stage.setTitle("Cowlendar - password123");
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
