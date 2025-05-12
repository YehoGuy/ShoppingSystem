package DTOs;

import Domain.ItemCategory;
public class ItemDTO {
    private int id;
    private double price;
    private String name;
    private String description;
    private ItemCategory category;

    public ItemDTO(int id, String name, String description, double price, ItemCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category.toString();
    }

    public ItemCategory getCategoryEnum() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public int getId() {
        return id;
    }

}
