package service.rest21.controllers;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.rest21.HelloApplication;
import service.rest21.models.CartItem;
import service.rest21.utils.Session;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class CartController {

    @FXML private VBox cartItemsContainer;
    @FXML private Label totalLabel;
    @FXML private Button orderButton;
    @FXML private Button backButton;
    @FXML private Label emptyCartLabel;
    @FXML private ScrollPane cartScrollPane;

    private final Session session = Session.getInstance();

    @FXML
    private void initialize() {
        animateButton(orderButton);
        animateButton(backButton);

        if (cartScrollPane != null) {
            cartScrollPane.setFitToWidth(true);
            cartScrollPane.setPannable(true);
            cartScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        }

        loadCartItems();
    }

    private void loadCartItems() {
        cartItemsContainer.getChildren().clear();

        if (session.getCart().isEmpty()) {
            emptyCartLabel.setVisible(true);
            orderButton.setDisable(true);
            totalLabel.setText("Итого: 0 ₽");
            return;
        }

        emptyCartLabel.setVisible(false);
        orderButton.setDisable(false);

        for (CartItem item : session.getCart()) {
            cartItemsContainer.getChildren().add(createCartItemBox(item));
        }

        updateTotal();
    }

    private HBox createCartItemBox(CartItem item) {
        HBox box = new HBox(18);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #fffafb; -fx-background-radius: 18; -fx-border-color: #ead7df; -fx-border-radius: 18;");

        ImageView imageView = new ImageView();
        try {
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(item.getProduct().getImagePath())));
            imageView.setImage(image);
            imageView.setFitWidth(96);
            imageView.setFitHeight(96);
            imageView.setPreserveRatio(true);
        } catch (Exception e) {
            imageView.setFitWidth(96);
            imageView.setFitHeight(96);
        }

        VBox infoBox = new VBox(8);
        infoBox.setPrefWidth(260);

        Label nameLabel = new Label(item.getProduct().getName());
        nameLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #5a3f4d;");

        Label priceLabel = new Label(String.format("%.0f ₽ за шт.", item.getProduct().getPrice()));
        priceLabel.setStyle("-fx-text-fill: #9c7a88;");

        Label itemTotalLabel = new Label(String.format("Сумма: %.0f ₽", item.getTotalPrice()));
        itemTotalLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #d16e92;");

        infoBox.getChildren().addAll(nameLabel, priceLabel, itemTotalLabel);

        VBox quantityBox = new VBox(10);
        quantityBox.setAlignment(javafx.geometry.Pos.CENTER);

        Button minusButton = new Button("-");
        minusButton.setStyle("-fx-background-color: #f4d9e2; -fx-text-fill: #7a4d61; -fx-background-radius: 10; -fx-font-weight: bold;");
        minusButton.setPrefWidth(42);

        Label quantityLabel = new Label(String.valueOf(item.getQuantity()));
        quantityLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #5a3f4d;");

        Button plusButton = new Button("+");
        plusButton.setStyle("-fx-background-color: #f4d9e2; -fx-text-fill: #7a4d61; -fx-background-radius: 10; -fx-font-weight: bold;");
        plusButton.setPrefWidth(42);

        Button removeButton = new Button("Удалить");
        removeButton.setStyle("-fx-background-color: #e8a6b9; -fx-text-fill: white; -fx-background-radius: 12;");

        animateButton(minusButton);
        animateButton(plusButton);
        animateButton(removeButton);

        minusButton.setOnAction(e -> {
            int newQuantity = item.getQuantity() - 1;
            if (newQuantity <= 0) {
                session.removeFromCart(item);
            } else {
                session.updateQuantity(item.getProduct(), newQuantity);
            }
            loadCartItems();
        });

        plusButton.setOnAction(e -> {
            session.updateQuantity(item.getProduct(), item.getQuantity() + 1);
            loadCartItems();
        });

        removeButton.setOnAction(e -> {
            session.removeFromCart(item);
            loadCartItems();
        });

        quantityBox.getChildren().addAll(minusButton, quantityLabel, plusButton, removeButton);

        HBox.setHgrow(infoBox, Priority.ALWAYS);
        box.getChildren().addAll(imageView, infoBox, quantityBox);

        return box;
    }

    private void updateTotal() {
        totalLabel.setText(String.format("Итого: %.0f ₽", session.getCartTotal()));
    }

    @FXML
    private void placeOrder() {
        if (session.isGuest()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Требуется вход");
            alert.setHeaderText("Гостевой режим");
            alert.setContentText("Гость не может оформить заказ.\nПерейти к авторизации?");

            ButtonType loginButtonType = new ButtonType("Войти");
            ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(loginButtonType, cancelButtonType);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == loginButtonType) {
                openLogin();
            }
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("checkout-view.fxml"));
            Parent root = loader.load();

            CheckoutController controller = loader.getController();
            controller.setCartData(session.getCart(), session.getCartTotal());

            Stage stage = (Stage) orderButton.getScene().getWindow();

            boolean maximized = stage.isMaximized();
            boolean fullScreen = stage.isFullScreen();
            double width = stage.getWidth();
            double height = stage.getHeight();

            Scene scene = new Scene(root);
            stage.setTitle("Оформление заказа");
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
        }
    }

    private void openLogin() {
        openScene("login-view.fxml", "Пельмени с кайфом - Вход");
    }

    @FXML
    private void goBack() {
        openScene("main-menu-view.fxml", "Пельмени с кайфом - Главное меню");
    }

    private void openScene(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxml));
            Parent root = loader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();

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
        }
    }

    private void animateButton(Button button) {
        if (button == null) return;

        button.setOnMouseEntered(e -> scale(button, 1.04));
        button.setOnMouseExited(e -> scale(button, 1.0));
        button.setOnMousePressed(e -> scale(button, 0.97));
        button.setOnMouseReleased(e -> scale(button, 1.04));
    }

    private void scale(Button button, double value) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
        st.setToX(value);
        st.setToY(value);
        st.play();
    }
}