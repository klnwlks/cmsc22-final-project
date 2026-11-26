package users;

import java.io.Serializable;
import java.util.ArrayList;

// abstract since only clerk and manager could be users
public abstract class User implements Serializable {

  private final String name;
  // should hold transactions per user
  private final ArrayList<Transaction> transactions = new ArrayList<>();
  private final String password;

  protected User(String name, String password) {
    this.name = name;
    this.password = password;
  }

  public String getName() {
    return name;
  }

  public boolean matchesPassword(String password) {
    return this.password.equals(password);
  } // for login purposes

  public ArrayList<Transaction> getTransactions() {
    return new ArrayList<>(transactions);
  }

  public void addTransaction(Transaction transaction) {
    transactions.add(transaction);
  }
}
