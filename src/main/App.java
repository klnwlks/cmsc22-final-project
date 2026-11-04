package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(new Label("Hello, JavaFX!"), 640, 400));
        stage.setTitle("JavaFX App");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
