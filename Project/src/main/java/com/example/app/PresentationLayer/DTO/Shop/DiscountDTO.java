package com.example.app.PresentationLayer.DTO.Shop;

import com.example.app.DomainLayer.Item.ItemCategory;

public class DiscountDTO {
    private Integer percentage;
    private boolean isDouble;
    private ItemCategory itemCategory;
    private int itemId;
    private CompositePolicyDTO policy;

    // no-args ctor for Jackson / other serializers
    public DiscountDTO() {
    }

    public DiscountDTO(Integer percentage,boolean isDouble, ItemCategory itemCategory, int itemId) {
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

    public CompositePolicyDTO getPolicy() {
        return policy;
    }

    public void setPolicy(CompositePolicyDTO policy) {
        this.policy = policy;
    }

    @Override
    public String toString() {
        String output = "";
        if (itemId != 0) {
            output = "Discount of " + percentage + "% on item with ID " + itemId;
        } else if (itemCategory != null) {
            output = "Discount of " + percentage + "% on category " + itemCategory;
        } else {
            output = "Discount of " + percentage + "% on the entire shop";
        }
        return output + (policy != null ? " | policy: " + policy.toString() : "");
    }

    public static DiscountDTO fromDomain(com.example.app.DomainLayer.Shop.Discount.Discount d) {
        DiscountDTO dto = new DiscountDTO();
        dto.setPercentage(d.getPercentage());
        dto.setDouble(d.isDouble());
        // item- vs category- vs global
        if (d instanceof com.example.app.DomainLayer.Shop.Discount.CategoryDiscount cd) {
            dto.setItemCategory(cd.getCategory());
        }
        if (d instanceof com.example.app.DomainLayer.Shop.Discount.SingleDiscount sd) {
            dto.setItemId(sd.getItemId());
        }
        dto.setPolicy(d.getPolicy() == null ? null : CompositePolicyDTO.fromDomain(d.getPolicy()));
        return dto;
    }
}
