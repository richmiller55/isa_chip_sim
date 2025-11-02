package javafx_test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class SimpleJavaFXWindow extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Set the title of the window (the Stage)
        primaryStage.setTitle("My JavaFX Window");

        // 2. Create a Button
        Button button = new Button("Click Me!");

        // 3. Add an action to the button (event handler)
        button.setOnAction(event -> {
            System.out.println("Button was clicked!");
        });

        // 4. Create a layout pane (StackPane centers its children)
        StackPane root = new StackPane();
        root.getChildren().add(button);

        // 5. Create a Scene, which holds all the visual content
        // Specify the width and height (e.g., 300x250 pixels)
        Scene scene = new Scene(root, 300, 250);

        // 6. Add the Scene to the Stage
        primaryStage.setScene(scene);

        // 7. Display the window
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Launch the JavaFX application
        launch(args);
    }
}
