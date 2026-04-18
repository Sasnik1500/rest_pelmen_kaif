package service.rest21.controllers;

import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.rest21.HelloApplication;
import service.rest21.models.CartItem;
import service.rest21.models.Order;
import service.rest21.models.Product;
import service.rest21.services.LogService;
import service.rest21.services.OrderService;
import service.rest21.services.ProductService;
import service.rest21.utils.Session;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MainMenuController {

    @FXML private Label welcomeLabel;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private GridPane productsGrid;
    @FXML private Label cartCountLabel;
    @FXML private Label cartTotalLabel;
    @FXML private Button cartButton;
    @FXML private Button historyButton;
    @FXML private Button logoutButton;
    @FXML private TextField searchField;

    private final ProductService productService = ProductService.getInstance();
    private final OrderService orderService = OrderService.getInstance();
    private final Session session = Session.getInstance();
    private final LogService logService = LogService.getInstance();

    private String currentCategory = "Все";

    @FXML
    private void initialize() {
        if (session.isGuest()) {
            welcomeLabel.setText("Добро пожаловать, гость!");
        } else if (session.getCurrentUser() != null) {
            welcomeLabel.setText("Добро пожаловать, " + session.getCurrentUser().getUsername() + "!");
        } else {
            welcomeLabel.setText("Добро пожаловать!");
        }

        categoryComboBox.getItems().clear();
        categoryComboBox.getItems().add("Все");
        categoryComboBox.getItems().addAll(productService.getCategories());
        categoryComboBox.setValue("Все");
        categoryComboBox.setOnAction(e -> filterProducts());

        animateButton(cartButton);
        animateButton(historyButton);
        animateButton(logoutButton);

        if (historyButton != null) {
            historyButton.setDisable(session.isGuest());
            historyButton.setOpacity(session.isGuest() ? 0.6 : 1.0);
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> loadProducts());
        }

        updateCartInfo();
        loadProducts();
    }

    private void loadProducts() {
        productsGrid.getChildren().clear();
        productsGrid.setHgap(22);
        productsGrid.setVgap(22);
        productsGrid.setPadding(new Insets(18));

        List<Product> products = "Все".equals(currentCategory)
                ? productService.getAllProducts()
                : productService.getProductsByCategory(currentCategory);

        String query = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        int column = 0;
        int row = 0;

        for (Product product : products) {
            boolean matches = query.isEmpty()
                    || product.getName().toLowerCase().contains(query)
                    || product.getCategory().toLowerCase().contains(query)
                    || product.getDescription().toLowerCase().contains(query);

            if (!matches) {
                continue;
            }

            VBox card = createProductCard(product);
            productsGrid.add(card, column, row);

            column++;
            if (column > 2) {
                column = 0;
                row++;
            }
        }
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #fffafb; -fx-padding: 16; -fx-background-radius: 18; -fx-border-color: #ead7df; -fx-border-radius: 18;");
        card.setPrefWidth(240);
        card.setPrefHeight(340);

        ImageView imageView = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream(product.getImagePath()));
            imageView.setImage(image);
            imageView.setFitWidth(210);
            imageView.setFitHeight(140);
            imageView.setPreserveRatio(true);
        } catch (Exception e) {
            imageView = null;
        }

        Label name = new Label(product.getName());
        name.setWrapText(true);
        name.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #5a3f4d;");

        Label shortDescription = new Label(product.getDescription());
        shortDescription.setWrapText(true);
        shortDescription.setStyle("-fx-text-fill: #9c7a88; -fx-font-size: 12;");
        shortDescription.setMinHeight(40);
        shortDescription.setPrefHeight(40);
        shortDescription.setMaxHeight(40);

        Label price = new Label(String.format("%.0f ₽", product.getPrice()));
        price.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #d16e92;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Spinner<Integer> quantitySpinner = new Spinner<>(1, 20, 1);
        quantitySpinner.setPrefWidth(78);
        quantitySpinner.setEditable(true);

        Button addBtn = new Button("В корзину");
        addBtn.setStyle("-fx-background-color: #e8a6b9; -fx-text-fill: white; -fx-background-radius: 12; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> handleAddToCart(product, quantitySpinner.getValue()));

        Button descBtn = new Button("Описание");
        descBtn.setStyle("-fx-background-color: #f4d9e2; -fx-text-fill: #7a4d61; -fx-background-radius: 12; -fx-font-weight: bold;");
        descBtn.setOnAction(e -> showProductDetails(product));

        animateButton(addBtn);
        animateButton(descBtn);

        HBox controls = new HBox(8, quantitySpinner, addBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        HBox buttons = new HBox(descBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox bottomBox = new VBox(6, price, controls, buttons);
        bottomBox.setAlignment(Pos.BOTTOM_LEFT);

        if (imageView != null) {
            card.getChildren().addAll(imageView, name, shortDescription, spacer, bottomBox);
        } else {
            Label noImage = new Label("📷");
            noImage.setStyle("-fx-font-size: 42;");
            noImage.setMinHeight(140);
            noImage.setAlignment(Pos.CENTER);
            card.getChildren().addAll(noImage, name, shortDescription, spacer, bottomBox);
        }

        return card;
    }

    private void handleAddToCart(Product product, int quantity) {
        if (isGuest()) {
            showLoginRequiredDialog();
            return;
        }

        session.addToCart(new CartItem(product, quantity));
        updateCartInfo();
        showAlert("Добавлено", product.getName() + " добавлен в корзину");
    }

    private boolean isGuest() {
        return session.isGuest();
    }

    private void showLoginRequiredDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Требуется вход");
        alert.setHeaderText("Гостевой режим");
        alert.setContentText("Чтобы добавить товар в корзину или оформить заказ, необходимо авторизоваться.\nПерейти к окну входа?");

        ButtonType loginButtonType = new ButtonType("Войти");
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(loginButtonType, cancelButtonType);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == loginButtonType) {
            openLoginScene();
        }
    }

    private void openLoginScene() {
        openScene("login-view.fxml", "Пельмени с кайфом - Вход");
    }

    private void showProductDetails(Product product) {
        showProductDescription(product);
    }

    private void showProductDescription(Product product) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(product.getName());

        VBox root = new VBox(16);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #fff9fb, #f8eef3);");

        ImageView imageView = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream(product.getImagePath()));
            imageView.setImage(image);
            imageView.setFitWidth(340);
            imageView.setFitHeight(220);
            imageView.setPreserveRatio(true);
        } catch (Exception ignored) {
        }

        Label title = new Label(product.getName());
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #5e4251;");
        title.setWrapText(true);

        Label price = new Label(String.format("%.0f ₽", product.getPrice()));
        price.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #dd6f92;");

        Label shortText = new Label(product.getDescription());
        shortText.setWrapText(true);
        shortText.setStyle("-fx-font-size: 14; -fx-text-fill: #8f6f7c;");

        Label descriptionTitle = new Label("Подробное описание");
        descriptionTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #5e4251;");

        Label description = new Label(product.getDetailedDescription());
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 14; -fx-text-fill: #7c5a69;");

        Button closeButton = new Button("Закрыть");
        closeButton.setStyle("-fx-background-color: #f3a6ba; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 14; -fx-cursor: hand;");
        closeButton.setOnAction(e -> dialog.close());
        animateButton(closeButton);

        if (imageView.getImage() != null) {
            root.getChildren().add(imageView);
        }

        root.getChildren().addAll(title, price, shortText, descriptionTitle, description, closeButton);

        Scene scene = new Scene(root, 420, 560);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    @FXML
    private void filterProducts() {
        currentCategory = categoryComboBox.getValue();
        loadProducts();
    }

    @FXML
    private void onSearch() {
        loadProducts();
    }

    @FXML
    private void openCart() {
        if (isGuest()) {
            showLoginRequiredDialog();
            return;
        }

        openScene("cart-view.fxml", "Корзина");
    }

    @FXML
    private void openOrderHistory() {
        if (isGuest()) {
            showLoginRequiredDialog();
            return;
        }

        String username = session.getCurrentUser().getUsername();
        List<Order> orders = orderService.getOrdersByUsername(username);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("История заказов");
        dialog.setHeaderText("Заказы пользователя: " + username);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<Order> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Order, Number> idCol = new TableColumn<>("№");
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));

        TableColumn<Order, String> dateCol = new TableColumn<>("Дата");
        dateCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getOrderTime() != null
                                ? data.getValue().getOrderTime().toLocalDate().toString()
                                : ""
                )
        );

        TableColumn<Order, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        TableColumn<Order, String> totalCol = new TableColumn<>("Сумма");
        totalCol.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.0f ₽", data.getValue().getTotalAmount()))
        );

        table.getColumns().addAll(idCol, dateCol, statusCol, totalCol);
        table.getItems().addAll(orders);

        VBox content = new VBox(12);
        content.setPadding(new Insets(10));

        if (orders.isEmpty()) {
            Label emptyLabel = new Label("У вас пока нет заказов.");
            emptyLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #7c5a69;");
            content.getChildren().add(emptyLabel);
        } else {
            content.getChildren().add(table);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(650);
        dialog.getDialogPane().setPrefHeight(420);
        dialog.showAndWait();
    }

    @FXML
    private void logout() {
        String username = session.getCurrentUser() != null ? session.getCurrentUser().getUsername() : "Неизвестно";
        logService.addLog(username, "Выход из системы", "Пользователи", "Logged out: " + username);
        session.logout();

        openScene("login-view.fxml", "Пельмени с кайфом - Вход");
    }

    public void updateCartInfo() {
        cartCountLabel.setText("Товаров: " + session.getCartItemsCount());
        cartTotalLabel.setText(String.format("Сумма: %.0f ₽", session.getCartTotal()));
    }

    private Stage currentStage() {
        if (cartButton != null && cartButton.getScene() != null) {
            return (Stage) cartButton.getScene().getWindow();
        }
        if (logoutButton != null && logoutButton.getScene() != null) {
            return (Stage) logoutButton.getScene().getWindow();
        }
        if (historyButton != null && historyButton.getScene() != null) {
            return (Stage) historyButton.getScene().getWindow();
        }
        throw new IllegalStateException("Stage is not available");
    }

    private void openScene(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxml));
            Parent root = loader.load();

            Stage stage = currentStage();

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
            showAlert("Ошибка", "Не удалось открыть окно: " + fxml);
        }
    }

    private void showAlert(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private void animateButton(Button button) {
        if (button == null) return;
        button.setOnMouseEntered(e -> scaleButton(button, 1.04));
        button.setOnMouseExited(e -> scaleButton(button, 1.0));
        button.setOnMousePressed(e -> scaleButton(button, 0.96));
        button.setOnMouseReleased(e -> scaleButton(button, 1.04));
    }

    private void scaleButton(Button button, double scale) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
        st.setToX(scale);
        st.setToY(scale);
        st.play();
    }
}