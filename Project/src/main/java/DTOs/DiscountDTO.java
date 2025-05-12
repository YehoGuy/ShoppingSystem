package DTOs;

import DomainLayer.Item.ItemCategory;

public class DiscountDTO {
    private int percentage;
    private boolean isDouble;
    private ItemCategory itemCategory;
    private Integer itemId;

    public DiscountDTO(int percentage, boolean isDouble,
            ItemCategory itemCategory, Integer itemId) {
        this.percentage = percentage;
        this.isDouble = isDouble;
        this.itemCategory = itemCategory;
        this.itemId = itemId;
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

    public Integer getItemId() {
        return itemId;
    }

    public String toString() {
        if (itemId != null) {
            return "Discount of " + percentage + "% on item with ID " + itemId;
        } else if (itemCategory != null) {
            return "Discount of " + percentage + "% on items in category " + itemCategory;
        } else {
            return "Discount of " + percentage + "% on he entire shop";
        }
    }
}
