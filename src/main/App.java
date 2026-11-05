package main;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import system.System;
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
  // default path of save file, on close it saves
  private static final Path DATA_FILE = Path.of("data", "cowlendar.dat");
  private System system;
  private ProductInventoryController inventoryController;
  private TransactionLogsController transactionController;
  private Scene inventoryScene;
  private Scene transactionScene;

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

    // Reuse a saved user instead of creating another Manager on every launch.
    ArrayList<User> users = system.getUsers();
    User currentUser;
    if (users.isEmpty()) {
      currentUser = system.newUser("Manager", "", "manager");
    } else {
      currentUser = users.getFirst();
    }
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
    stage.setOnCloseRequest(event -> {
      try {
        system.save(DATA_FILE);
      } catch (IOException exception) {
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
