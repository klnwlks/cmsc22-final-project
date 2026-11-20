package screens;

import java.util.Locale;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import users.User;

public class LoginController {
  @FXML private TextField usernameField;
  @FXML private PasswordField passwordField;
  @FXML private ComboBox<String> roleField;
  @FXML private Label errorLabel;

  private system.System system;
  private Consumer<User> onUserCreated;

  public LoginController(system.System system, Consumer<User> onUserCreated) {
    this.system = system;
    this.onUserCreated = onUserCreated;
  }

  @FXML
  private void initialize() {
    roleField.getItems().setAll("Clerk", "Manager");
    roleField.getSelectionModel().selectFirst();
  }

  @FXML
  private void handleCreateAccount() {
    String username = usernameField.getText().trim();
    String password = passwordField.getText();
    String role = roleField.getValue();

    if (username.isEmpty() || password.isEmpty() || role == null) {
      errorLabel.setText("Enter a username, password, and role.");
      return;
    }

    boolean exists = system.getUsers().stream()
        .anyMatch(user -> user.getName().equalsIgnoreCase(username));
    if (exists) {
      errorLabel.setText("That username is already in use.");
      return;
    }

    User user = system.newUser(username, password, role.toLowerCase(Locale.ROOT));
    onUserCreated.accept(user);
  }
}
