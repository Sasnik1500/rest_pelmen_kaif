package service.rest21.controllers;

import javafx.animation.ScaleTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;
import service.rest21.HelloApplication;
import service.rest21.services.UserService;

import java.io.IOException;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;
    @FXML private Button backButton;

    private final UserService userService = UserService.getInstance();

    @FXML
    private void initialize() {
        errorLabel.setVisible(false);

        animateButton(registerButton);
        animateButton(backButton);
    }

    @FXML
    private void handleRegister() {
        errorLabel.setVisible(false);

        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            showError("Заполните все обязательные поля!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Пароли не совпадают!");
            return;
        }

        if (password.length() < 6) {
            showError("Пароль должен быть не менее 6 символов!");
            return;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        registerButton.setDisable(true);
        registerButton.setText("Регистрация...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return userService.register(username, hashedPassword, email, phone);
            }
        };

        task.setOnSucceeded(e -> {
            registerButton.setDisable(false);
            registerButton.setText("Создать аккаунт");

            if (task.getValue()) {
                showSuccess("Регистрация успешна!");
                goBack();
            } else {
                showError("Ошибка регистрации");
            }
        });

        task.setOnFailed(e -> {
            registerButton.setDisable(false);
            registerButton.setText("Создать аккаунт");
            showError("Ошибка соединения с сервером");
        });

        new Thread(task).start();
    }

    @FXML
    private void goBack() {
        openScene("login-view.fxml", "Пельмени с кайфом - Вход");
    }

    private void openScene(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxml));
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();

            boolean maximized = stage.isMaximized();
            boolean fullScreen = stage.isFullScreen();
            double width = stage.getWidth();
            double height = stage.getHeight();

            Scene scene = new Scene(root);
            stage.setTitle(title);
            stage.setScene(scene);

            if (maximized) {
                stage.setMaximized(true);
            } else {
                stage.setWidth(width);
                stage.setHeight(height);
            }

            if (fullScreen) {
                stage.setFullScreen(true);
            }

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Ошибка загрузки страницы");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успех");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void animateButton(Button button) {
        if (button == null) return;

        button.setOnMouseEntered(e -> scale(button, 1.05));
        button.setOnMouseExited(e -> scale(button, 1.0));
        button.setOnMousePressed(e -> scale(button, 0.97));
        button.setOnMouseReleased(e -> scale(button, 1.05));
    }

    private void scale(Button button, double value) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
        st.setToX(value);
        st.setToY(value);
        st.play();
    }
}