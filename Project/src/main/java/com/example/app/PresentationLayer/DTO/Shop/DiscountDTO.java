package com.example.app.PresentationLayer.DTO.Shop;

import com.example.app.DomainLayer.Item.ItemCategory;

public class DiscountDTO {
    private Integer percentage;
    private boolean isDouble;
    private ItemCategory itemCategory;
    private int itemId;

    // no-args ctor for Jackson / other serializers
    public DiscountDTO() {
    }

    public DiscountDTO(Integer percentage,
                       boolean isDouble,
                       ItemCategory itemCategory,
                       int itemId) {
        this.percentage = percentage;
        this.isDouble = isDouble;
        this.itemCategory = itemCategory;
        this.itemId = itemId;
    }

    public Integer getPercentage() {
        return percentage;
    }

    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }

    public boolean isDouble() {
        return isDouble;
    }

    public void setDouble(boolean isDouble) {
        this.isDouble = isDouble;
    }

    public ItemCategory getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(ItemCategory itemCategory) {
        this.itemCategory = itemCategory;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    @Override
    public String toString() {
        if (itemId != 0) {
            return "Discount of " + percentage + "% on item with ID " + itemId;
        } else if (itemCategory != null) {
            return "Discount of " + percentage + "% on category " + itemCategory;
        } else {
            return "Discount of " + percentage + "% on the entire shop";
        }
    }
}
