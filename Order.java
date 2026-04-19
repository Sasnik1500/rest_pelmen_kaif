package service.rest21.models;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int id;
    private String username;
    private List<CartItem> items;
    private double totalAmount;
    private String status;
    private LocalDateTime orderTime;

    public Order(String username) {
        this.username = username;
        this.items = new ArrayList<>();
        this.status = "Принят";
        this.orderTime = LocalDateTime.now();
    }
    // ===== GET =====
    public int getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public List<CartItem> getItems() {
        return items;
    }
    public double getTotalAmount() {
        return totalAmount;
    }
    public String getStatus() {
        return status;
    }
    public LocalDateTime getOrderTime() {
        return orderTime;
    }
    // ===== SET =====
    public void setId(int id) {
        this.id = id;
    }
    public void setItems(List<CartItem> items) {
        this.items = items;
    }
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }
    // ===== UTILS =====
    public void addItem(CartItem item) {
        this.items.add(item);
    }
    public void calculateTotal() {
        totalAmount = items.stream()
                .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity())
                .sum();
    }
}