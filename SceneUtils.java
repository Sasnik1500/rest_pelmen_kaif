package service.rest21.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import service.rest21.HelloApplication;

import java.io.IOException;

public class SceneUtils {

    public static void switchScene(Stage stage, String fxml, String title) {
        try {
            boolean maximized = stage.isMaximized();
            double width = stage.getWidth();
            double height = stage.getHeight();

            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxml));
            Parent root = loader.load();

            Scene newScene = new Scene(root);
            stage.setTitle(title);
            stage.setScene(newScene);

            if (maximized) {
                stage.setMaximized(true);
            } else {
                stage.setWidth(width);
                stage.setHeight(height);
            }

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}