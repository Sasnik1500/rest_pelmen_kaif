package service.rest21.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.rest21.HelloApplication;
import service.rest21.models.User;
import service.rest21.services.LogService;
import service.rest21.services.UserService;
import service.rest21.utils.Session;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button togglePasswordButton;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button guestButton;

    private final UserService userService = UserService.getInstance();
    private final Session session = Session.getInstance();
    private final LogService logService = LogService.getInstance();

    private boolean passwordVisible = false;

    @FXML
    private void initialize() {
        errorLabel.setVisible(false);

        if (visiblePasswordField != null) {
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            visiblePasswordField.setText(passwordField.getText());
        }

        if (passwordField != null && visiblePasswordField != null) {
            passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!visiblePasswordField.getText().equals(newVal)) {
                    visiblePasswordField.setText(newVal);
                }
            });

            visiblePasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!passwordField.getText().equals(newVal)) {
                    passwordField.setText(newVal);
                }
            });
        }

        if (togglePasswordButton != null) {
            togglePasswordButton.setText("👁‍🗨");
            animateButton(togglePasswordButton);
        }

        animateButton(loginButton);
        animateButton(registerButton);
        animateButton(guestButton);
    }

    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            visiblePasswordField.setText(passwordField.getText());
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);

            passwordField.setVisible(false);
            passwordField.setManaged(false);

            togglePasswordButton.setText("👁");
            playFade(visiblePasswordField);
            playPop(togglePasswordButton);
            visiblePasswordField.requestFocus();
            visiblePasswordField.positionCaret(visiblePasswordField.getText().length());
        } else {
            passwordField.setText(visiblePasswordField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);

            togglePasswordButton.setText("👁‍🗨");
            playFade(passwordField);
            playPop(togglePasswordButton);
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordVisible
                ? visiblePasswordField.getText().trim()
                : passwordField.getText().trim();

        errorLabel.setVisible(false);

        if (username.isEmpty() || password.isEmpty()) {
            showError("Заполните все поля!");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Вход...");

        try {
            User user = userService.login(username, password);

            if (user != null) {
                session.setCurrentUser(user);
                logService.addLog(
                        user.getUsername(),
                        "Вход в систему",
                        "Пользователи",
                        "Logged in: " + user.getUsername()
                );

                if ("admin".equalsIgnoreCase(user.getUsername())) {
                    openScene("admin-view.fxml", "Админ панель");
                } else {
                    openScene("main-menu-view.fxml", "Пельмени с кайфом - Главное меню");
                }
            } else {
                showError("Неверный логин или пароль!");
            }
        } catch (UserService.BlockedUserException e) {
            showError("Ваш аккаунт заблокирован!");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Ошибка входа");
        }

        loginButton.setDisable(false);
        loginButton.setText("Войти");
    }

    @FXML
    private void handleGuestLogin() {
        session.loginAsGuest();
        logService.addLog("Гость", "Вход в систему", "Пользователи", "Logged in as guest");
        openScene("main-menu-view.fxml", "Пельмени с кайфом - Главное меню");
    }

    @FXML
    private void handleRegister() {
        openScene("register-view.fxml", "Пельмени с кайфом - Регистрация");
    }

    private void openScene(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxml));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();

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
            showError("Ошибка загрузки окна: " + fxml);
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
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

    private void playFade(javafx.scene.Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(180), node);
        ft.setFromValue(0.4);
        ft.setToValue(1.0);
        ft.play();
    }

    private void playPop(Button button) {
        ScaleTransition st = new ScaleTransition(Duration.millis(140), button);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.12);
        st.setToY(1.12);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }
}