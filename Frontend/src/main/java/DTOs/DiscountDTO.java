package DTOs;

import Domain.ItemCategory;
public class DiscountDTO {
    private Integer percentage;
    private boolean isDouble;
    private ItemCategory itemCategory;
    private int itemId;

    public DiscountDTO(Integer percentage, boolean isDouble,
            ItemCategory itemCategory, int itemId) {
        this.percentage = percentage;
        this.isDouble = isDouble;
        this.itemCategory = itemCategory;
        this.itemId = itemId;
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

    public int getItemId() {
        return itemId;
    }

    public String toString() {
        if (itemId != 0) {
            return "Discount of " + percentage + "% on item with ID " + itemId;
        } else if (itemCategory != null) {
            return "Discount of " + percentage + "% on items in category " + itemCategory;
        } else {
            return "Discount of " + percentage + "% on he entire shop";
        }
    }
}
