package service.rest21.controllers;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.rest21.HelloApplication;
import service.rest21.models.Order;
import service.rest21.models.Product;
import service.rest21.services.LogService;
import service.rest21.services.OrderService;
import service.rest21.services.ProductService;
import service.rest21.services.PromoCode;
import service.rest21.services.UserService;
import service.rest21.utils.Session;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class AdminController {

    @FXML private StackPane contentPane;

    @FXML private Button usersNavButton;
    @FXML private Button productsNavButton;
    @FXML private Button ordersNavButton;
    @FXML private Button promoNavButton;
    @FXML private Button logsNavButton;

    @FXML private Button logoutButton;
    @FXML private Button addProductButton;
    @FXML private Button addPromoButton;
    @FXML private Button applyStatusButton;
    @FXML private Button refreshUsersButton;
    @FXML private Button refreshLogsButton;

    @FXML private TextField userSearchField;
    @FXML private TableView<AdminUserRow> usersTable;

    @FXML private TextField productSearchField;
    @FXML private TableView<ProductRow> productsTable;

    @FXML private ComboBox<String> orderFilterCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TableView<Order> ordersTable;

    @FXML private TableView<PromoRow> promoTable;
    @FXML private TableView<LogRow> logsTable;

    @FXML private ScrollPane usersPane;
    @FXML private ScrollPane productsPane;
    @FXML private ScrollPane ordersPane;
    @FXML private ScrollPane promoPane;
    @FXML private ScrollPane logsPane;

    private final Session sessionManager = Session.getInstance();
    private final ProductService productService = ProductService.getInstance();
    private final OrderService orderService = OrderService.getInstance();
    private final UserService userService = UserService.getInstance();
    private final LogService logService = LogService.getInstance();
    private final PromoCode promoCodeService = PromoCode.getInstance();

    private final ObservableList<AdminUserRow> userRows = FXCollections.observableArrayList();
    private final ObservableList<ProductRow> productRows = FXCollections.observableArrayList();
    private final ObservableList<Order> orderRows = FXCollections.observableArrayList();
    private final ObservableList<PromoRow> promoRows = FXCollections.observableArrayList();
    private final ObservableList<LogRow> logRows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (sessionManager.getCurrentUser() == null ||
                !"admin".equalsIgnoreCase(sessionManager.getCurrentUser().getUsername())) {
            Platform.runLater(this::goToCatalog);
            return;
        }

        setupUsersTable();
        setupProductsTable();
        setupOrdersTable();
        setupPromoTable();
        setupLogsTable();

        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        promoTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        logsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        if (userSearchField != null) {
            userSearchField.textProperty().addListener((obs, oldVal, newVal) -> loadUsers());
        }

        if (productSearchField != null) {
            productSearchField.textProperty().addListener((obs, oldVal, newVal) -> loadProducts());
        }

        orderFilterCombo.getItems().addAll("Все", "Принят", "Готовится", "Завершён");
        orderFilterCombo.setValue("Все");
        orderFilterCombo.setOnAction(e -> loadOrders());

        statusCombo.getItems().addAll("Принят", "Готовится", "Завершён");
        statusCombo.setValue("Готовится");

        loadUsers();
        loadProducts();
        loadOrders();
        loadPromos();
        loadLogs();
        showUsers();

        animateButton(usersNavButton);
        animateButton(productsNavButton);
        animateButton(ordersNavButton);
        animateButton(promoNavButton);
        animateButton(logsNavButton);
        animateButton(logoutButton);
        animateButton(addProductButton);
        animateButton(addPromoButton);
        animateButton(applyStatusButton);
        animateButton(refreshUsersButton);
        animateButton(refreshLogsButton);
    }

    private void animateButton(Button button) {
        if (button == null) return;
        button.setOnMouseEntered(e -> playScale(button, 1.04, 1.04, 120));
        button.setOnMouseExited(e -> playScale(button, 1.0, 1.0, 120));
        button.setOnMousePressed(e -> playScale(button, 0.96, 0.96, 80));
        button.setOnMouseReleased(e -> playScale(button, 1.04, 1.04, 80));
    }

    private void playScale(Button button, double x, double y, int millis) {
        ScaleTransition st = new ScaleTransition(Duration.millis(millis), button);
        st.setToX(x);
        st.setToY(y);
        st.play();
    }

    private void setupUsersTable() {
        TableColumn<AdminUserRow, Number> numCol = new TableColumn<>("#");
        numCol.setCellValueFactory(data -> data.getValue().numberProperty());

        TableColumn<AdminUserRow, String> usernameCol = new TableColumn<>("Логин");
        usernameCol.setCellValueFactory(data -> data.getValue().usernameProperty());

        TableColumn<AdminUserRow, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data -> data.getValue().emailProperty());

        TableColumn<AdminUserRow, String> roleCol = new TableColumn<>("Роль");
        roleCol.setCellValueFactory(data -> data.getValue().roleProperty());

        TableColumn<AdminUserRow, String> blockedCol = new TableColumn<>("Заблокирован");
        blockedCol.setCellValueFactory(data -> data.getValue().blockedTextProperty());

        TableColumn<AdminUserRow, String> createdAtCol = new TableColumn<>("Создан");
        createdAtCol.setCellValueFactory(data -> data.getValue().createdAtProperty());

        TableColumn<AdminUserRow, Void> actionsCol = new TableColumn<>("Действия");
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button unblockBtn = new Button("Разблокировать");
            private final Button blockBtn = new Button("Заблокировать");
            private final HBox box = new HBox(8, unblockBtn, blockBtn);

            {
                unblockBtn.setStyle("-fx-background-color: #215a32; -fx-text-fill: #7ef0a2; -fx-background-radius: 8; -fx-cursor: hand;");
                blockBtn.setStyle("-fx-background-color: #4a1f25; -fx-text-fill: #ff6d7f; -fx-background-radius: 8; -fx-cursor: hand;");

                animateButton(unblockBtn);
                animateButton(blockBtn);

                unblockBtn.setOnAction(e -> {
                    AdminUserRow row = getTableView().getItems().get(getIndex());
                    boolean ok = userService.updateBlockedStatus(row.getUsername(), false);
                    if (ok) {
                        row.setBlocked(false);
                        row.setBlockedText("Нет");
                        usersTable.refresh();
                        logService.addLog(
                                sessionManager.getCurrentUser().getUsername(),
                                "Разблокировка",
                                "Пользователи",
                                "Unblocked: " + row.getUsername()
                        );
                        loadUsers();
                        loadLogs();
                        showInfo("Готово", "Пользователь разблокирован.");
                    } else {
                        showInfo("Ошибка", "Не удалось разблокировать пользователя.");
                    }
                });

                blockBtn.setOnAction(e -> {
                    AdminUserRow row = getTableView().getItems().get(getIndex());

                    if ("admin".equalsIgnoreCase(row.getUsername())) {
                        showInfo("Запрещено", "Нельзя заблокировать admin.");
                        return;
                    }

                    boolean ok = userService.updateBlockedStatus(row.getUsername(), true);
                    if (ok) {
                        row.setBlocked(true);
                        row.setBlockedText("Да");
                        usersTable.refresh();
                        logService.addLog(
                                sessionManager.getCurrentUser().getUsername(),
                                "Блокировка пользователя",
                                "Пользователи",
                                "Blocked: " + row.getUsername()
                        );
                        loadUsers();
                        loadLogs();
                        showInfo("Готово", "Пользователь заблокирован.");
                    } else {
                        showInfo("Ошибка", "Не удалось заблокировать пользователя.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                AdminUserRow row = getTableView().getItems().get(getIndex());
                unblockBtn.setVisible(row.isBlocked());
                unblockBtn.setManaged(row.isBlocked());
                blockBtn.setVisible(!row.isBlocked());
                blockBtn.setManaged(!row.isBlocked());

                setGraphic(box);
            }
        });

        usersTable.getColumns().clear();
        usersTable.getColumns().addAll(numCol, usernameCol, emailCol, roleCol, blockedCol, createdAtCol, actionsCol);
        usersTable.setItems(userRows);
    }

    private void setupProductsTable() {
        TableColumn<ProductRow, String> nameCol = new TableColumn<>("Название");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(220);

        TableColumn<ProductRow, String> categoryCol = new TableColumn<>("Категория");
        categoryCol.setCellValueFactory(data -> data.getValue().categoryProperty());
        categoryCol.setPrefWidth(150);

        TableColumn<ProductRow, String> priceCol = new TableColumn<>("Цена");
        priceCol.setCellValueFactory(data -> data.getValue().priceProperty());
        priceCol.setPrefWidth(110);

        TableColumn<ProductRow, String> descriptionCol = new TableColumn<>("Описание");
        descriptionCol.setCellValueFactory(data -> data.getValue().descriptionProperty());
        descriptionCol.setPrefWidth(320);

        TableColumn<ProductRow, Void> actionsCol = new TableColumn<>("Действия");
        actionsCol.setPrefWidth(260);
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Изменить");
            private final Button hideBtn = new Button("Скрыть");
            private final Button deleteBtn = new Button("Удалить");
            private final HBox box = new HBox(8, editBtn, hideBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #21435a; -fx-text-fill: #8fd6ff; -fx-background-radius: 8; -fx-cursor: hand;");
                hideBtn.setStyle("-fx-background-color: #4b4b1f; -fx-text-fill: #ffe28f; -fx-background-radius: 8; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #4a1f25; -fx-text-fill: #ff8a9c; -fx-background-radius: 8; -fx-cursor: hand;");

                animateButton(editBtn);
                animateButton(hideBtn);
                animateButton(deleteBtn);

                editBtn.setOnAction(e -> {
                    ProductRow row = getTableView().getItems().get(getIndex());
                    showEditProductDialog(row);
                });

                hideBtn.setOnAction(e -> {
                    ProductRow row = getTableView().getItems().get(getIndex());
                    boolean newStatus = !row.isActive();

                    productService.toggleProduct(row.getId(), newStatus);

                    logService.addLog(
                            sessionManager.getCurrentUser().getUsername(),
                            newStatus ? "Показ товара" : "Скрытие товара",
                            "Товары",
                            row.getName()
                    );

                    loadProducts();
                    loadLogs();
                });

                deleteBtn.setOnAction(e -> {
                    ProductRow row = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Удаление товара");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Удалить товар \"" + row.getName() + "\"?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        productService.deleteProduct(row.getId());
                        logService.addLog(
                                sessionManager.getCurrentUser().getUsername(),
                                "Удаление товара",
                                "Товары",
                                row.getName()
                        );
                        loadProducts();
                        loadLogs();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    ProductRow row = getTableView().getItems().get(getIndex());
                    hideBtn.setText(row.isActive() ? "Скрыть" : "Показать");
                    setGraphic(box);
                }
            }
        });

        productsTable.getColumns().clear();
        productsTable.getColumns().addAll(nameCol, categoryCol, priceCol, descriptionCol, actionsCol);
        productsTable.setItems(productRows);
    }

    private void setupOrdersTable() {
        TableColumn<Order, Number> idCol = new TableColumn<>("#");
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));

        TableColumn<Order, String> clientCol = new TableColumn<>("Клиент");
        clientCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));

        TableColumn<Order, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        TableColumn<Order, String> totalCol = new TableColumn<>("Сумма");
        totalCol.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.0f ₽", data.getValue().getTotalAmount()))
        );

        TableColumn<Order, String> dateCol = new TableColumn<>("Дата");
        dateCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getOrderTime().toLocalDate().toString())
        );

        ordersTable.getColumns().clear();
        ordersTable.getColumns().addAll(idCol, clientCol, statusCol, totalCol, dateCol);
        ordersTable.setItems(orderRows);
    }

    private void setupPromoTable() {
        TableColumn<PromoRow, String> codeCol = new TableColumn<>("Код");
        codeCol.setCellValueFactory(data -> data.getValue().codeProperty());

        TableColumn<PromoRow, String> discountCol = new TableColumn<>("Скидка");
        discountCol.setCellValueFactory(data -> data.getValue().discountProperty());

        TableColumn<PromoRow, Number> usedCol = new TableColumn<>("Использован");
        usedCol.setCellValueFactory(data -> data.getValue().usedProperty());

        TableColumn<PromoRow, Number> limitCol = new TableColumn<>("Лимит");
        limitCol.setCellValueFactory(data -> data.getValue().limitProperty());

        TableColumn<PromoRow, String> expireCol = new TableColumn<>("Истекает");
        expireCol.setCellValueFactory(data -> data.getValue().expiresProperty());

        TableColumn<PromoRow, String> activeCol = new TableColumn<>("Активен");
        activeCol.setCellValueFactory(data -> data.getValue().activeProperty());

        TableColumn<PromoRow, Void> actionsCol = new TableColumn<>("Действия");
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Изменить");
            private final Button toggleBtn = new Button();
            private final Button deleteBtn = new Button("Удалить");
            private final HBox box = new HBox(8, editBtn, toggleBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #21435a; -fx-text-fill: #8fd6ff; -fx-background-radius: 8; -fx-cursor: hand;");
                toggleBtn.setStyle("-fx-background-color: #4b4b1f; -fx-text-fill: #ffe28f; -fx-background-radius: 8; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #4a1f25; -fx-text-fill: #ff8a9c; -fx-background-radius: 8; -fx-cursor: hand;");

                animateButton(editBtn);
                animateButton(toggleBtn);
                animateButton(deleteBtn);

                editBtn.setOnAction(e -> {
                    PromoRow row = getTableView().getItems().get(getIndex());
                    showEditPromoDialog(row);
                });

                toggleBtn.setOnAction(e -> {
                    PromoRow row = getTableView().getItems().get(getIndex());
                    boolean newActive = !row.isActive();

                    boolean ok = promoCodeService.setPromoActive(row.getId(), newActive);
                    if (ok) {
                        row.setActive(newActive);
                        row.setActiveText(newActive ? "Да" : "Нет");
                        promoTable.refresh();
                        loadPromos();

                        logService.addLog(
                                sessionManager.getCurrentUser().getUsername(),
                                newActive ? "Включение промокода" : "Выключение промокода",
                                "Промокоды",
                                row.getCode()
                        );
                        loadLogs();
                    } else {
                        showInfo("Ошибка", "Не удалось изменить статус промокода.");
                    }
                });

                deleteBtn.setOnAction(e -> {
                    PromoRow row = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Удаление");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Удалить промокод " + row.getCode() + "?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        boolean ok = promoCodeService.deletePromoCode(row.getId());
                        if (ok) {
                            logService.addLog(
                                    sessionManager.getCurrentUser().getUsername(),
                                    "Удаление промокода",
                                    "Промокоды",
                                    row.getCode()
                            );
                            loadPromos();
                            loadLogs();
                        } else {
                            showInfo("Ошибка", "Не удалось удалить промокод.");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                PromoRow row = getTableView().getItems().get(getIndex());
                toggleBtn.setText(row.isActive() ? "Выключить" : "Включить");
                setGraphic(box);
            }
        });

        promoTable.getColumns().clear();
        promoTable.getColumns().addAll(codeCol, discountCol, usedCol, limitCol, expireCol, activeCol, actionsCol);
        promoTable.setItems(promoRows);
    }

    private void setupLogsTable() {
        TableColumn<LogRow, Number> idCol = new TableColumn<>("#");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());

        TableColumn<LogRow, String> timeCol = new TableColumn<>("Время");
        timeCol.setCellValueFactory(data -> data.getValue().createdAtProperty());

        TableColumn<LogRow, String> userCol = new TableColumn<>("Пользователь");
        userCol.setCellValueFactory(data -> data.getValue().usernameProperty());

        TableColumn<LogRow, String> actionCol = new TableColumn<>("Действие");
        actionCol.setCellValueFactory(data -> data.getValue().actionProperty());

        TableColumn<LogRow, String> tableCol = new TableColumn<>("Таблица");
        tableCol.setCellValueFactory(data -> data.getValue().tableNameProperty());

        TableColumn<LogRow, String> detailsCol = new TableColumn<>("Детали");
        detailsCol.setCellValueFactory(data -> data.getValue().detailsProperty());

        logsTable.getColumns().clear();
        logsTable.getColumns().addAll(idCol, timeCol, userCol, actionCol, tableCol, detailsCol);
        logsTable.setItems(logRows);
    }

    private void loadUsers() {
        userRows.clear();

        String query = userSearchField == null || userSearchField.getText() == null
                ? ""
                : userSearchField.getText().trim().toLowerCase();

        for (UserService.AdminUserRowData u : userService.getAllUsersAdmin()) {
            boolean matches = query.isEmpty()
                    || u.getUsername().toLowerCase().contains(query)
                    || u.getEmail().toLowerCase().contains(query)
                    || u.getRole().toLowerCase().contains(query);

            if (matches) {
                userRows.add(new AdminUserRow(
                        u.getId(),
                        u.getNumber(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getRole(),
                        u.isBlocked(),
                        u.getCreatedAt()
                ));
            }
        }

        usersTable.refresh();
    }

    private void loadProducts() {
        productRows.clear();

        String query = productSearchField == null || productSearchField.getText() == null
                ? ""
                : productSearchField.getText().trim().toLowerCase();

        List<Product> products = productService.getAllProductsForAdmin();

        for (Product product : products) {
            boolean matches = query.isEmpty()
                    || product.getName().toLowerCase().contains(query)
                    || product.getCategory().toLowerCase().contains(query)
                    || product.getDescription().toLowerCase().contains(query);

            if (matches) {
                productRows.add(new ProductRow(product));
            }
        }

        productsTable.refresh();
    }

    private void loadOrders() {
        orderRows.clear();
        String filter = orderFilterCombo.getValue();

        List<Order> allOrders = orderService.getAllOrders();

        for (Order order : allOrders) {
            if (filter == null || "Все".equals(filter) || filter.equals(order.getStatus())) {
                orderRows.add(order);
            }
        }

        ordersTable.refresh();
    }

    private void loadPromos() {
        promoRows.clear();

        for (PromoCode.PromoCodeData promo : promoCodeService.getAllPromoCodes()) {
            promoRows.add(new PromoRow(
                    promo.getId(),
                    promo.getCode(),
                    "-" + promo.getDiscount() + "%",
                    promo.getUsedCount(),
                    promo.getUsageLimit(),
                    promo.getExpiresAt(),
                    promo.isActive() ? "Да" : "Нет",
                    promo.isActive()
            ));
        }

        promoTable.refresh();
    }

    private void loadLogs() {
        logRows.clear();

        for (LogService.LogRowData log : logService.getAllLogs()) {
            logRows.add(new LogRow(
                    log.getId(),
                    log.getCreatedAt(),
                    log.getUsername(),
                    log.getAction(),
                    log.getTableName(),
                    log.getDetails()
            ));
        }

        logsTable.refresh();
    }

    @FXML
    private void refreshUsers() {
        loadUsers();
    }

    @FXML
    private void refreshLogs() {
        loadLogs();
    }

    @FXML
    private void addProduct() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить товар");
        dialog.setHeaderText("Создание нового товара");

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        TextField categoryField = new TextField();
        TextField priceField = new TextField();
        TextField imageField = new TextField("/service/rest21/images/no-image.png");
        TextArea descField = new TextArea();
        TextArea detailedField = new TextArea();

        descField.setPrefRowCount(2);
        detailedField.setPrefRowCount(4);

        VBox box = new VBox(10,
                new Label("Название"), nameField,
                new Label("Категория"), categoryField,
                new Label("Цена"), priceField,
                new Label("Путь к фото"), imageField,
                new Label("Краткое описание"), descField,
                new Label("Подробное описание"), detailedField
        );
        box.setStyle("-fx-padding: 10;");
        dialog.getDialogPane().setContent(box);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == saveButtonType) {
            try {
                String name = nameField.getText().trim();
                String category = categoryField.getText().trim();
                String priceText = priceField.getText().trim().replace(",", ".");
                String image = imageField.getText().trim();
                String description = descField.getText().trim();
                String detailed = detailedField.getText().trim();

                if (name.isEmpty() || category.isEmpty() || priceText.isEmpty()) {
                    showInfo("Ошибка", "Заполните название, категорию и цену.");
                    return;
                }

                double price = Double.parseDouble(priceText);

                Product newProduct = new Product(
                        0,
                        name,
                        category,
                        price,
                        description,
                        image,
                        detailed,
                        true
                );

                boolean ok = productService.addProduct(newProduct);

                if (ok) {
                    logService.addLog(
                            sessionManager.getCurrentUser().getUsername(),
                            "Добавление товара",
                            "Товары",
                            newProduct.getName()
                    );

                    loadProducts();
                    loadLogs();
                    showProducts();
                    showInfo("Готово", "Товар успешно добавлен.");
                } else {
                    showInfo("Ошибка", "Не удалось добавить товар в базу данных.");
                }

            } catch (NumberFormatException e) {
                showInfo("Ошибка", "Цена должна быть числом.");
            } catch (Exception e) {
                e.printStackTrace();
                showInfo("Ошибка", "Проверь данные товара.");
            }
        }
    }

    @FXML
    private void addPromo() {
        showCreatePromoDialog();
    }

    private void showCreatePromoDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить промокод");
        dialog.setHeaderText("Создание нового промокода");

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField codeField = new TextField();
        codeField.setPromptText("Код");

        TextField discountField = new TextField();
        discountField.setPromptText("Скидка (%)");

        TextField limitField = new TextField();
        limitField.setPromptText("Максимум использований");

        TextField expiresField = new TextField();
        expiresField.setPromptText("Дата YYYY-MM-DD или пусто");

        CheckBox activeCheck = new CheckBox("Активен");
        activeCheck.setSelected(true);

        VBox box = new VBox(12,
                new Label("Код"), codeField,
                new Label("Скидка (%)"), discountField,
                new Label("Максимум использований"), limitField,
                new Label("Дата истечения"), expiresField,
                activeCheck
        );
        box.setStyle("-fx-padding: 10;");
        dialog.getDialogPane().setContent(box);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            try {
                String code = codeField.getText().trim();
                int discount = Integer.parseInt(discountField.getText().trim());
                int limit = Integer.parseInt(limitField.getText().trim());
                String expires = expiresField.getText().trim();
                boolean active = activeCheck.isSelected();

                if (code.isBlank()) {
                    showInfo("Ошибка", "Введите код промокода.");
                    return;
                }

                boolean ok = promoCodeService.addPromoCode(code, discount, limit, expires, active);
                if (ok) {
                    logService.addLog(
                            sessionManager.getCurrentUser().getUsername(),
                            "Добавление промокода",
                            "Промокоды",
                            code
                    );
                    loadPromos();
                    loadLogs();
                    showInfo("Готово", "Промокод добавлен.");
                } else {
                    showInfo("Ошибка", "Не удалось добавить промокод.");
                }
            } catch (Exception e) {
                showInfo("Ошибка", "Проверьте корректность введённых данных.");
            }
        }
    }

    private void showEditPromoDialog(PromoRow row) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Редактировать промокод");
        dialog.setHeaderText("Изменение промокода");

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField codeField = new TextField(row.getCode());
        TextField discountField = new TextField(row.getDiscount().replace("-", "").replace("%", ""));
        TextField limitField = new TextField(String.valueOf(row.getLimitValue()));
        TextField expiresField = new TextField("Бессрочно".equals(row.getExpires()) ? "" : row.getExpires());

        CheckBox activeCheck = new CheckBox("Активен");
        activeCheck.setSelected(row.isActive());

        VBox box = new VBox(12,
                new Label("Код"), codeField,
                new Label("Скидка (%)"), discountField,
                new Label("Максимум использований"), limitField,
                new Label("Дата истечения"), expiresField,
                activeCheck
        );
        box.setStyle("-fx-padding: 10;");
        dialog.getDialogPane().setContent(box);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            try {
                String code = codeField.getText().trim();
                int discount = Integer.parseInt(discountField.getText().trim());
                int limit = Integer.parseInt(limitField.getText().trim());
                String expires = expiresField.getText().trim();
                boolean active = activeCheck.isSelected();

                boolean ok = promoCodeService.updatePromoCode(row.getId(), code, discount, limit, expires, active);
                if (ok) {
                    logService.addLog(
                            sessionManager.getCurrentUser().getUsername(),
                            "Редактирование промокода",
                            "Промокоды",
                            code
                    );
                    loadPromos();
                    loadLogs();
                    showInfo("Готово", "Промокод обновлён.");
                } else {
                    showInfo("Ошибка", "Не удалось обновить промокод.");
                }
            } catch (Exception e) {
                showInfo("Ошибка", "Проверьте корректность введённых данных.");
            }
        }
    }

    @FXML
    private void showUsers() {
        usersPane.setVisible(true);
        usersPane.setManaged(true);

        productsPane.setVisible(false);
        productsPane.setManaged(false);

        ordersPane.setVisible(false);
        ordersPane.setManaged(false);

        promoPane.setVisible(false);
        promoPane.setManaged(false);

        logsPane.setVisible(false);
        logsPane.setManaged(false);

        setActiveNav(usersNavButton);
    }

    @FXML
    private void showProducts() {
        usersPane.setVisible(false);
        usersPane.setManaged(false);

        productsPane.setVisible(true);
        productsPane.setManaged(true);

        ordersPane.setVisible(false);
        ordersPane.setManaged(false);

        promoPane.setVisible(false);
        promoPane.setManaged(false);

        logsPane.setVisible(false);
        logsPane.setManaged(false);

        setActiveNav(productsNavButton);
    }

    @FXML
    private void showOrders() {
        usersPane.setVisible(false);
        usersPane.setManaged(false);

        productsPane.setVisible(false);
        productsPane.setManaged(false);

        ordersPane.setVisible(true);
        ordersPane.setManaged(true);

        promoPane.setVisible(false);
        promoPane.setManaged(false);

        logsPane.setVisible(false);
        logsPane.setManaged(false);

        setActiveNav(ordersNavButton);
    }

    @FXML
    private void showPromos() {
        usersPane.setVisible(false);
        usersPane.setManaged(false);

        productsPane.setVisible(false);
        productsPane.setManaged(false);

        ordersPane.setVisible(false);
        ordersPane.setManaged(false);

        promoPane.setVisible(true);
        promoPane.setManaged(true);

        logsPane.setVisible(false);
        logsPane.setManaged(false);

        loadPromos();
        setActiveNav(promoNavButton);
    }

    @FXML
    private void showLogs() {
        usersPane.setVisible(false);
        usersPane.setManaged(false);

        productsPane.setVisible(false);
        productsPane.setManaged(false);

        ordersPane.setVisible(false);
        ordersPane.setManaged(false);

        promoPane.setVisible(false);
        promoPane.setManaged(false);

        logsPane.setVisible(true);
        logsPane.setManaged(true);

        loadLogs();
        setActiveNav(logsNavButton);
    }

    private void setActiveNav(Button activeButton) {
        String normal = "-fx-background-color: transparent; -fx-text-fill: #f3d7e1; -fx-alignment: CENTER_LEFT; -fx-font-size: 16; -fx-padding: 14 20;";
        String active = "-fx-background-color: #6d3146; -fx-text-fill: #fff5f8; -fx-alignment: CENTER_LEFT; -fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 14 20; -fx-background-radius: 10;";

        usersNavButton.setStyle(normal);
        productsNavButton.setStyle(normal);
        ordersNavButton.setStyle(normal);
        promoNavButton.setStyle(normal);
        logsNavButton.setStyle(normal);

        activeButton.setStyle(active);
    }

    @FXML
    private void applyStatus() {
        Order selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
        String selectedStatus = statusCombo.getValue();

        if (selectedOrder == null) {
            showInfo("Выберите заказ", "Сначала выберите заказ в таблице.");
            return;
        }

        if (selectedStatus == null || selectedStatus.isBlank()) {
            showInfo("Выберите статус", "Сначала выберите новый статус.");
            return;
        }

        boolean ok = orderService.updateOrderStatus(selectedOrder.getId(), selectedStatus);

        if (ok) {
            loadOrders();
            loadLogs();
            showInfo("Готово", "Статус заказа обновлён.");
        } else {
            showInfo("Ошибка", "Не удалось обновить статус заказа.");
        }
    }

    private void goToCatalog() {
        openScene("main-menu-view.fxml", "Пельмени с кайфом - Главное меню");
    }

    @FXML
    private void logout() {
        String username = sessionManager.getCurrentUser() != null
                ? sessionManager.getCurrentUser().getUsername()
                : "Неизвестно";

        logService.addLog(username, "Выход из системы", "Пользователи", "Logged out: " + username);
        sessionManager.logout();

        openScene("login-view.fxml", "Пельмени с кайфом - Вход");
    }


    private Stage currentStage() {
        if (contentPane != null && contentPane.getScene() != null) {
            return (Stage) contentPane.getScene().getWindow();
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
            showInfo("Ошибка", "Не удалось открыть окно: " + fxml);
        }
    }

    private void showInfo(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    public static class AdminUserRow {
        private final int id;
        private final SimpleIntegerProperty number;
        private final SimpleStringProperty username;
        private final SimpleStringProperty email;
        private final SimpleStringProperty role;
        private final SimpleBooleanProperty blocked;
        private final SimpleStringProperty blockedText;
        private final SimpleStringProperty createdAt;

        public AdminUserRow(int id, int number, String username, String email, String role, boolean blocked, String createdAt) {
            this.id = id;
            this.number = new SimpleIntegerProperty(number);
            this.username = new SimpleStringProperty(username);
            this.email = new SimpleStringProperty(email);
            this.role = new SimpleStringProperty(role);
            this.blocked = new SimpleBooleanProperty(blocked);
            this.blockedText = new SimpleStringProperty(blocked ? "Да" : "Нет");
            this.createdAt = new SimpleStringProperty(createdAt);
        }


        public int getId() { return id; }
        public SimpleIntegerProperty numberProperty() { return number; }
        public SimpleStringProperty usernameProperty() { return username; }
        public SimpleStringProperty emailProperty() { return email; }
        public SimpleStringProperty roleProperty() { return role; }
        public SimpleBooleanProperty blockedProperty() { return blocked; }
        public SimpleStringProperty blockedTextProperty() { return blockedText; }
        public SimpleStringProperty createdAtProperty() { return createdAt; }

        public String getUsername() { return username.get(); }
        public boolean isBlocked() { return blocked.get(); }

        public void setBlocked(boolean value) { blocked.set(value); }
        public void setBlockedText(String text) { blockedText.set(text); }
    }

    public static class ProductRow {
        private final int id;
        private final boolean active;
        private final Product source;

        private final SimpleStringProperty name;
        private final SimpleStringProperty category;
        private final SimpleStringProperty price;
        private final SimpleStringProperty description;

        public ProductRow(Product product) {
            this.id = product.getId();
            this.source = product;
            this.active = product.isActive();
            this.name = new SimpleStringProperty(product.getName());
            this.category = new SimpleStringProperty(product.getCategory());
            this.price = new SimpleStringProperty(String.format("%.0f руб.", product.getPrice()));
            this.description = new SimpleStringProperty(product.getDescription());
        }

        public int getId() { return id; }
        public String getName() { return name.get(); }
        public boolean isActive() { return active; }
        public Product getSource() { return source; }

        public SimpleStringProperty nameProperty() { return name; }
        public SimpleStringProperty categoryProperty() { return category; }
        public SimpleStringProperty priceProperty() { return price; }
        public SimpleStringProperty descriptionProperty() { return description; }
    }

    public static class PromoRow {
        private final long id;
        private final SimpleStringProperty code;
        private final SimpleStringProperty discount;
        private final SimpleIntegerProperty used;
        private final SimpleIntegerProperty limit;
        private final SimpleStringProperty expires;
        private final SimpleStringProperty active;
        private final SimpleBooleanProperty activeFlag;

        public PromoRow(long id, String code, String discount, int used, int limit, String expires, String active, boolean activeFlag) {
            this.id = id;
            this.code = new SimpleStringProperty(code);
            this.discount = new SimpleStringProperty(discount);
            this.used = new SimpleIntegerProperty(used);
            this.limit = new SimpleIntegerProperty(limit);
            this.expires = new SimpleStringProperty(expires);
            this.active = new SimpleStringProperty(active);
            this.activeFlag = new SimpleBooleanProperty(activeFlag);
        }

        public long getId() { return id; }
        public String getCode() { return code.get(); }
        public String getDiscount() { return discount.get(); }
        public int getLimitValue() { return limit.get(); }
        public String getExpires() { return expires.get(); }
        public boolean isActive() { return activeFlag.get(); }

        public void setActive(boolean value) { activeFlag.set(value); }
        public void setActiveText(String text) { active.set(text); }

        public SimpleStringProperty codeProperty() { return code; }
        public SimpleStringProperty discountProperty() { return discount; }
        public SimpleIntegerProperty usedProperty() { return used; }
        public SimpleIntegerProperty limitProperty() { return limit; }
        public SimpleStringProperty expiresProperty() { return expires; }
        public SimpleStringProperty activeProperty() { return active; }
    }

    public static class LogRow {
        private final SimpleLongProperty id;
        private final SimpleStringProperty createdAt;
        private final SimpleStringProperty username;
        private final SimpleStringProperty action;
        private final SimpleStringProperty tableName;
        private final SimpleStringProperty details;

        public LogRow(long id, String createdAt, String username, String action, String tableName, String details) {
            this.id = new SimpleLongProperty(id);
            this.createdAt = new SimpleStringProperty(createdAt);
            this.username = new SimpleStringProperty(username);
            this.action = new SimpleStringProperty(action);
            this.tableName = new SimpleStringProperty(tableName);
            this.details = new SimpleStringProperty(details);
        }

        public SimpleLongProperty idProperty() { return id; }
        public SimpleStringProperty createdAtProperty() { return createdAt; }
        public SimpleStringProperty usernameProperty() { return username; }
        public SimpleStringProperty actionProperty() { return action; }
        public SimpleStringProperty tableNameProperty() { return tableName; }
        public SimpleStringProperty detailsProperty() { return details; }
    }

    private void showEditProductDialog(ProductRow row) {
        Product product = row.getSource();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Изменить товар");
        dialog.setHeaderText("Редактирование товара");

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField(product.getName());
        TextField categoryField = new TextField(product.getCategory());
        TextField priceField = new TextField(String.valueOf(product.getPrice()));
        TextField imageField = new TextField(product.getImagePath());
        TextArea descField = new TextArea(product.getDescription());
        TextArea detailedField = new TextArea(product.getDetailedDescription());

        descField.setPrefRowCount(2);
        detailedField.setPrefRowCount(4);

        VBox box = new VBox(10,
                new Label("Название"), nameField,
                new Label("Категория"), categoryField,
                new Label("Цена"), priceField,
                new Label("Путь к фото"), imageField,
                new Label("Краткое описание"), descField,
                new Label("Подробное описание"), detailedField
        );
        box.setStyle("-fx-padding: 10;");
        dialog.getDialogPane().setContent(box);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            try {
                Product updated = new Product(
                        product.getId(),
                        nameField.getText().trim(),
                        categoryField.getText().trim(),
                        Double.parseDouble(priceField.getText().trim().replace(",", ".")),
                        descField.getText().trim(),
                        imageField.getText().trim(),
                        detailedField.getText().trim(),
                        product.isActive()
                );

                productService.updateProduct(updated);

                logService.addLog(
                        sessionManager.getCurrentUser().getUsername(),
                        "Изменение товара",
                        "Товары",
                        updated.getName()
                );

                loadProducts();
                loadLogs();
                showInfo("Готово", "Товар обновлён.");
            } catch (Exception e) {
                showInfo("Ошибка", "Проверь данные товара.");
            }
        }
    }
}