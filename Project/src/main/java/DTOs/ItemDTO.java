package DTOs;

import DomainLayer.Item.ItemCategory;

public class ItemDTO {
    private int id;
    private String name;
    private String description;
    private ItemCategory category;

    public ItemDTO(int id, String name, String description, ItemCategory category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public int getId() {
        return id;
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
