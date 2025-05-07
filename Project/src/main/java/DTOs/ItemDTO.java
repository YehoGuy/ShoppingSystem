package DTOs;

import DomainLayer.Item.ItemCategory;

public class ItemDTO {
    private String name;
    private String description;
    private ItemCategory category;

    public ItemDTO(String name, String description, double price) {
        this.name = name;
        this.description = description;
        this.category = category;
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

}
