package DTOs;

import DomainLayer.Item.ItemCategory;

public class LeafPolicyDTO {
    private Integer threshold;
    private Integer itemId;
    private ItemCategory itemCategory;
    private Double basketValue;

    public LeafPolicyDTO(Integer threshold, Integer itemId, ItemCategory itemCategory, double basketValue) {
        this.threshold = threshold;
        this.itemId = itemId;
        this.itemCategory = itemCategory;
        this.basketValue = basketValue;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public Integer getItemId() {
        return itemId;
    }

    public ItemCategory getItemCategory() {
        return itemCategory;
    }

    public double getBasketValue() {
        return basketValue;
    }

    public String toString() {
        if (threshold != null && itemCategory == null)
            return "Minimum quantity of item " + itemId + " is " + threshold;
        else if (threshold != null && itemCategory != null)
            return "Minimum quantity of category " + itemCategory + " is " + threshold;
        else
            return "Minimum basket value of is " + basketValue;
    }
}