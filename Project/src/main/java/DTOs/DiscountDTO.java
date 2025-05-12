package DTOs;

import DomainLayer.Item.ItemCategory;

public class DiscountDTO {
    private int percentage;
    private boolean isDouble;
    private ItemCategory itemCategory;
    private String itemName;

    public DiscountDTO(int percentage, boolean isDouble,
            ItemCategory itemCategory, String itemName) {
        this.percentage = percentage;
        this.isDouble = isDouble;
        this.itemCategory = itemCategory;
        this.itemName = itemName;
    }

    public int getPercentage() {
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
