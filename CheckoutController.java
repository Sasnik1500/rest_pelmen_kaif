package service.rest21.controllers;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.rest21.HelloApplication;
import service.rest21.models.CartItem;
import service.rest21.services.OrderService;
import service.rest21.services.PromoCode;
import service.rest21.utils.Session;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class CheckoutController {

    @FXML private TextField promoField;
    @FXML private Label promoStatusLabel;
    @FXML private Label sumLabel;
    @FXML private Label discountLabel;
    @FXML private Label totalLabel;
    @FXML private ComboBox<String> paymentCombo;
    @FXML private TextArea commentField;
    @FXML private Button applyPromoButton;
    @FXML private Button addCardButton;
    @FXML private Button placeOrderButton;
    @FXML private Button backButton;

    private final Session session = Session.getInstance();
    private final PromoCode promoCodeService = PromoCode.getInstance();
    private final OrderService orderService = OrderService.getInstance();

    private double discountPercent = 0.0;
    private List<CartItem> cartItems;
    private double cartTotal = 0.0;

    @FXML
    private void initialize() {
        paymentCombo.getItems().clear();
        paymentCombo.getItems().add("Наличные при получении");
        paymentCombo.setValue("Наличные при получении");

        promoStatusLabel.setText("");

        animateButton(applyPromoButton);
        animateButton(addCardButton);
        animateButton(placeOrderButton);
        animateButton(backButton);

        cartItems = session.getCart();
        cartTotal = session.getCartTotal();
        updateTotals();
    }

    public void setCartData(List<CartItem> items, double total) {
        this.cartItems = items;
        this.cartTotal = total;
        updateTotals();
    }

    @FXML
    private void applyPromo() {
        String code = promoField.getText() == null ? "" : promoField.getText().trim();

        if (code.isEmpty()) {
            promoStatusLabel.setText("Введите промокод");
            promoStatusLabel.setStyle("-fx-text-fill: #cc4f74;");
            return;
        }

        try {
            PromoCode.PromoCodeData promo = promoCodeService.findActivePromoCode(code);

            if (promo == null) {
                discountPercent = 0;
                promoStatusLabel.setText("Промокод не найден или выключен");
                promoStatusLabel.setStyle("-fx-text-fill: #cc4f74;");
                updateTotals();
                return;
            }

            discountPercent = promo.getDiscount();
            promoStatusLabel.setText("Промокод применён: -" + promo.getDiscount() + "%");
            promoStatusLabel.setStyle("-fx-text-fill: #7dbb8b;");
            updateTotals();

        } catch (Exception e) {
            e.printStackTrace();
            promoStatusLabel.setText("Ошибка проверки промокода");
            promoStatusLabel.setStyle("-fx-text-fill: #cc4f74;");
        }
    }

    @FXML
    private void addCard() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить карту");
        dialog.setHeaderText("Введите данные карты");

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("Номер карты");

        TextField expiryField = new TextField();
        expiryField.setPromptText("MM/YY");

        PasswordField pinField = new PasswordField();
        pinField.setPromptText("PIN-код");

        VBox content = new VBox(12,
                new Label("Номер карты"), cardNumberField,
                new Label("Срок действия"), expiryField,
                new Label("PIN-код"), pinField
        );
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == saveButtonType) {
            String cardNumber = cardNumberField.getText().trim();
            String expiry = expiryField.getText().trim();
            String pin = pinField.getText().trim();

            if (cardNumber.isEmpty() || expiry.isEmpty() || pin.isEmpty()) {
                showAlert("Ошибка", "Заполните все поля карты");
                return;
            }

            String masked;
            if (cardNumber.length() >= 4) {
                masked = "Карта **** " + cardNumber.substring(cardNumber.length() - 4);
            } else {
                masked = "Карта";
            }

            if (!paymentCombo.getItems().contains(masked)) {
                paymentCombo.getItems().add(masked);
            }
            paymentCombo.setValue(masked);
        }
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
                openScene("login-view.fxml", "Пельмени с кайфом - Вход");
            }
            return;
        }

        if (session.getCartItemsCount() == 0) {
            showAlert("Ошибка", "Корзина пуста");
            return;
        }

        try {
            String username = session.getCurrentUser() != null
                    ? session.getCurrentUser().getUsername()
                    : "Гость";

            double discountAmount = cartTotal * discountPercent / 100.0;
            double finalTotal = cartTotal - discountAmount;

            List<CartItem> itemsToSave = cartItems != null ? cartItems : session.getCart();

            boolean ok = orderService.createOrder(username, itemsToSave, finalTotal);

            if (!ok) {
                showAlert("Ошибка", "Не удалось сохранить заказ в базе данных");
                return;
            }

            showAlert("Успех", "Заказ оформлен!");
            session.clearCart();

            openScene("main-menu-view.fxml", "Пельмени с кайфом - Главное меню");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось оформить заказ");
        }
    }

    @FXML
    private void goBack() {
        openScene("cart-view.fxml", "Корзина");
    }

    private void updateTotals() {
        double discountAmount = cartTotal * discountPercent / 100.0;
        double finalTotal = cartTotal - discountAmount;

        sumLabel.setText(String.format("%.0f ₽", cartTotal));
        discountLabel.setText(String.format("-%.0f ₽", discountAmount));
        totalLabel.setText(String.format("%.0f ₽", finalTotal));
    }

    private void openScene(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxml));
            Parent root = loader.load();

            Stage stage = (Stage) placeOrderButton.getScene().getWindow();

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
            showAlert("Ошибка", "Не удалось открыть окно");
        }
    }

    private void animateButton(Button button) {
        if (button == null) return;

        button.setOnMouseEntered(e -> scale(button, 1.05));
        button.setOnMouseExited(e -> scale(button, 1.0));
        button.setOnMousePressed(e -> scale(button, 0.95));
        button.setOnMouseReleased(e -> scale(button, 1.05));
    }

    private void scale(Button button, double value) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
        st.setToX(value);
        st.setToY(value);
        st.play();
    }

    private void showAlert(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}