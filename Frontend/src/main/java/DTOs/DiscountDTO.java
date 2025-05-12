package DTOs;

import main.java.Domain.ItemCategory;

public class DiscountDTO {
    private Integer percentage;
    private boolean isDouble;
    private ItemCategory itemCategory;
    private String itemName;

    public DiscountDTO(Integer percentage, boolean isDouble,
            ItemCategory itemCategory, String itemName) {
        this.percentage = percentage;
        this.isDouble = isDouble;
        this.itemCategory = itemCategory;
        this.itemName = itemName;
    }

    public Integer getPercentage() {
        return percentage;
    }

    public boolean isDouble() {
        return isDouble;
    }

    public ItemCategory getItemCategory() {
        return itemCategory;
    }

    public String getItemName() {
        return itemName;
    }

    public String toString() {
        if (itemName != null) {
            return "Discount of " + percentage + "% on item with ID " + itemName;
        } else if (itemCategory != null) {
            return "Discount of " + percentage + "% on items in category " + itemCategory;
        } else {
            return "Discount of " + percentage + "% on he entire shop";
        }
    }
}
