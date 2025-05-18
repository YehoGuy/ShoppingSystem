package DTOs;

import Domain.ItemCategory;

public class ItemDTO {
    private int id;
    private double price;
    private String name;
    private String description;
    private ItemCategory category;

    // Jackson needs this
    public ItemDTO() { }

    public ItemDTO(int id, String name, String description, double price, ItemCategory category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
    }

    // Getters
    public int getId() { return id; }
    public double getPrice() { return price; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategoryString() { return category.toString(); }
    public ItemCategory getCategory() { return category; }

    // Setters (required by Jackson)
    public void setId(int id) { this.id = id; }
    public void setPrice(double price) { this.price = price; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(ItemCategory category) { this.category = category; }
}
