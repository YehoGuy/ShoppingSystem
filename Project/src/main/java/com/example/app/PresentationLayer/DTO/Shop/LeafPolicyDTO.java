package com.example.app.PresentationLayer.DTO.Shop;

// Update the import path to the correct package for ItemCategory
import com.example.app.DomainLayer.Item.ItemCategory;
import jakarta.validation.constraints.Positive;

public class LeafPolicyDTO {
    @Positive
    private Integer      threshold;
    private Integer      itemId;
    private ItemCategory itemCategory;
    @Positive
    private Double       basketValue;

    public LeafPolicyDTO() { }

    public LeafPolicyDTO(Integer threshold,
                         Integer itemId,
                         ItemCategory itemCategory,
                         Double basketValue) {
        this.threshold    = threshold;
        this.itemId     = itemId;
        this.itemCategory = itemCategory;
        this.basketValue  = basketValue;
    }

    public Integer getThreshold() { return threshold; }
    public void setThreshold(Integer threshold) { this.threshold = threshold; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public ItemCategory getItemCategory() { return itemCategory; }
    public void setItemCategory(ItemCategory itemCategory) { this.itemCategory = itemCategory; }

    public Double getBasketValue() { return basketValue; }
    public void setBasketValue(Double basketValue) { this.basketValue = basketValue; }

    @Override
    public String toString() {
        if (threshold != null && itemCategory == null) {
            return "Min quantity of item " + itemId + " = " + threshold;
        } else if (threshold != null) {
            return "Min quantity of category " + itemCategory + " = " + threshold;
        } else {
            return "Min basket value = " + basketValue;
        }
    }
}
