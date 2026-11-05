package system;

import inventory.Inventory;
import users.Clerk;
import users.Manager;
import users.User;
import users.Transaction;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class System implements Serializable {

  // system manages users and inventory, along with persistence
  private final Inventory inventory = new Inventory();
  private final ArrayList<User> users = new ArrayList<>();

  public Inventory getInventory() {
    return inventory;
  }

  public ArrayList<User> getUsers() {
    return new ArrayList<>(users);
  }

  public ArrayList<Transaction> getTransactions() {
    ArrayList<Transaction> transactions = new ArrayList<>();
    for (User user : users) {
      transactions.addAll(user.getTransactions());
    }
    return transactions;
  }

  public User newUser(String user, String pass, String type) {
    String normalizedType = type.trim().toLowerCase(Locale.ROOT);
    User newUser = switch (normalizedType) {
      case "manager" -> new Manager(user, pass, inventory, this);
      case "clerk" -> new Clerk(user, pass, inventory);
      default -> throw new IllegalArgumentException("type must be manager or clerk");
    };
    users.add(newUser);
    return newUser;
  }

  public void save(Path path) throws IOException {
    Path target = Objects.requireNonNull(path, "path").toAbsolutePath();
    Path parent = target.getParent();
    Files.createDirectories(parent);
    try {
      try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(path))) {
        output.writeObject(this);
      }
    } finally {
      Files.deleteIfExists(target);
    }
  }

  public static System load(Path path) throws IOException, ClassNotFoundException {
    try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(path))) {
      Object saved = input.readObject();
      if (saved instanceof System system) {
        return system;
      }
      throw new InvalidObjectException("file does not contain a system snapshot");
    }
  }
}
