package service.rest21.models;

public class Product {
    private int id;
    private String name;
    private String category;
    private double price;
    private String description;
    private String imagePath;
    private String detailedDescription;
    private boolean active;

    public Product(int id, String name, String category, double price,
                   String description, String imagePath,
                   String detailedDescription, boolean active) {

        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.description = description;
        this.imagePath = imagePath;
        this.detailedDescription = detailedDescription;
        this.active = active;
    }

    public Product(int id, String name, String category, double price,
                   String description, String imagePath,
                   String detailedDescription) {

        this(id, name, category, price, description, imagePath, detailedDescription, true);
    }
    // ===== GET / SET =====
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public double getPrice() {
        return price;
    }
    public void setPrice(double price) {
        this.price = price;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    public String getDetailedDescription() {
        return detailedDescription;
    }
    public void setDetailedDescription(String detailedDescription) {
        this.detailedDescription = detailedDescription;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
}