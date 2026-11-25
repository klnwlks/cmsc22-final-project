package screens;

import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import users.User;

public class LoginController {
  @FXML private TextField usernameField;
  @FXML private PasswordField passwordField;
  @FXML private ComboBox<String> roleField;
  @FXML private Label errorLabel, titleLabel, subtitleLabel;
  @FXML private Button submitButton, switchModeButton;

  private final system.System system;
  private final Consumer<User> onUserCreated;
  private boolean loginMode;

  public LoginController(system.System system, Consumer<User> onUserCreated) {
    this.system = system;
    this.onUserCreated = onUserCreated;
  }

  @FXML
  private void initialize() {
    roleField.getItems().setAll("Clerk", "Manager");
    roleField.getSelectionModel().selectFirst();
    setLoginMode(!system.getUsers().isEmpty());
  }

  @FXML
  private void handleSubmit() {
    if (loginMode) {
      handleLogin();
    } else {
      handleCreateAccount();
    }
  }

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

    User user = system.newUser(username, password, role.toLowerCase(java.util.Locale.ROOT));
    onUserCreated.accept(user);
  }

  private void handleLogin() {
    User user = system.authenticate(usernameField.getText().trim(), passwordField.getText());
    if (user == null) {
      errorLabel.setText("Incorrect username or password.");
      return;
    }
    onUserCreated.accept(user);
  }

  @FXML
  private void switchMode() {
    setLoginMode(!loginMode);
  }

  public void showLoginMode() {
    usernameField.clear();
    passwordField.clear();
    setLoginMode(true);
  }

  private void setLoginMode(boolean enabled) {
    loginMode = enabled;
    titleLabel.setText(enabled ? "Welcome back" : "Cowlendar");
    subtitleLabel.setText(enabled ? "Log in to your account" : "Create a clerk or manager account");
    roleField.setVisible(!enabled);
    roleField.setManaged(!enabled);
    submitButton.setText(enabled ? "Log in" : "Create account");
    switchModeButton.setText(enabled ? "New here? Create an account" : "Already have an account? Log in");
    errorLabel.setText("");
  }
}
