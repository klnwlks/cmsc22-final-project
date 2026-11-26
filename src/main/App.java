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
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.BorderPane;
import screens.LoginController;
import screens.NavigationSidebarController;
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
  private User signedInUser;
  private ProductInventoryController inventoryController;
  private TransactionLogsController transactionController;
  private LoginController loginController;
  private Scene loginScene;
  private Scene dashboardScene;
  private BorderPane inventoryPage, transactionPage, salesPage;
  private NavigationSidebarController inventorySidebar, transactionSidebar, salesSidebar;
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

    // ABOVE COMMENT FIXED - OKAY NA LUKAS nag gawa na ako login system
    FXMLLoader loginLoader = new FXMLLoader(App.class.getResource("/screens/Login.fxml"));

    // set the controller for the login screen para maayos log out rin instead of
    // just closing the app goes back to log in screen
    loginController = new LoginController(system, user -> {
      try {
        showUserScreens(stage, user);
        system.save(DATA_FILE);
      } catch (IOException exception) {
        new Alert(Alert.AlertType.ERROR, "Could not open the account: " + exception.getMessage()).showAndWait();
      }
    });
    // logout button now works mga syug
    loginLoader.setController(loginController);
    loginScene = new Scene(loginLoader.load());
    stage.setScene(loginScene);
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
    signedInUser = currentUser;
    inventoryController = new ProductInventoryController(system, currentUser);
    transactionController = new TransactionLogsController(system, currentUser);

    FXMLLoader inventoryLoader = new FXMLLoader(App.class.getResource("/screens/ProductInventory.fxml"));
    inventoryLoader.setController(inventoryController);
    inventoryPage = inventoryLoader.load();
    inventorySidebar = (NavigationSidebarController) inventoryLoader.getNamespace().get("sidebarController");
    configureSidebar(inventorySidebar, currentUser, stage);

    FXMLLoader transactionLoader = new FXMLLoader(App.class.getResource("/screens/TransactionLogs.fxml"));
    transactionLoader.setController(transactionController);
    transactionPage = transactionLoader.load();
    transactionSidebar = (NavigationSidebarController) transactionLoader.getNamespace().get("sidebarController");
    configureSidebar(transactionSidebar, currentUser, stage);

    // tapos pag maanger is readonly dapat ung dashboard ng clerk nila
    if (currentUser instanceof Clerk clerk) {
      loadSales(clerk, false, stage);
      showSales(stage);
      return;
    }
    showInventory(stage);
  }

  private void configureSidebar(NavigationSidebarController sidebar, User currentUser, Stage stage) {
    String salesButtonText = currentUser instanceof Clerk ? "Sales" : "Clerk dashboards";
    Runnable salesAction = currentUser instanceof Clerk ? () -> showSales(stage) : () -> openClerkDashboard(stage);
    sidebar.configure(currentUser, salesButtonText, salesAction, () -> showInventory(stage),
        () -> showTransactions(stage), () -> logout(stage));
  }

  private void loadSales(Clerk clerk, boolean readOnly, Stage stage) throws IOException {
    var loader = new FXMLLoader(App.class.getResource("/screens/Sales.fxml"));
    salesController = new SalesController(clerk, readOnly);
    loader.setController(salesController);
    salesPage = loader.load();
    salesSidebar = (NavigationSidebarController) loader.getNamespace().get("sidebarController");
    configureSidebar(salesSidebar, signedInUser, stage); // replaced for the reusable sidebar
  }

  private void openClerkDashboard(Stage stage) {
    var names = system.getUsers().stream().filter(Clerk.class::isInstance)
        .map(User::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    // oa an error handling
    if (names.isEmpty()) {
      new Alert(Alert.AlertType.INFORMATION, "There are no clerk accounts yet.").showAndWait();
      return;
    }
    var picker = new ChoiceDialog<>(names.getFirst(), names);
    picker.initOwner(stage);
    picker.setTitle("Clerk dashboards");
    picker.setHeaderText("Choose a clerk to view");
    picker.showAndWait().ifPresent(name -> {
      try {
        Clerk clerk = system.getUsers().stream().filter(Clerk.class::isInstance).map(Clerk.class::cast)
            .filter(user -> user.getName().equals(name)).findFirst().orElseThrow();
        loadSales(clerk, true, stage);
        showSales(stage);
      } catch (IOException exception) {
        new Alert(Alert.AlertType.ERROR, "Could not open clerk dashboard: " + exception.getMessage()).showAndWait();
      }
    });
  }

  private void logout(Stage stage) {
    try {
      system.save(DATA_FILE);
      // sidebar
      dashboardScene = null;
      loginController.showLoginMode();
      stage.setScene(loginScene);
      stage.setTitle("Cowlendar");
    } catch (IOException exception) {
      new Alert(Alert.AlertType.ERROR, "Could not save data before logging out: " + exception.getMessage())
          .showAndWait();
    }
  }

  private void showSales(Stage stage) {
    salesController.refresh();
    showPage(stage, salesPage, salesSidebar, NavigationSidebarController.Page.SALES);
  }

  private void showInventory(Stage stage) {
    inventoryController.refresh();
    showPage(stage, inventoryPage, inventorySidebar, NavigationSidebarController.Page.INVENTORY);
  }

  private void showTransactions(Stage stage) {
    transactionController.refresh();
    showPage(stage, transactionPage, transactionSidebar, NavigationSidebarController.Page.TRANSACTIONS);
  }

  private void showPage(Stage stage, BorderPane page, NavigationSidebarController sidebar,
      NavigationSidebarController.Page activePage) {
    sidebar.setActivePage(activePage);
    if (dashboardScene == null) {
      dashboardScene = new Scene(page);
      stage.setScene(dashboardScene);
    } else if (dashboardScene.getRoot() != page) {
      dashboardScene.setRoot(page);
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
