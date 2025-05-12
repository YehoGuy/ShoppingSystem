package DTOs;

import main.java.Domain.ItemCategory;

public class LeafPolicyDTO {
    private Integer threshold;
    private String itemName;
    private ItemCategory itemCategory;
    private Double basketValue;

    public LeafPolicyDTO(Integer threshold, String itemName, ItemCategory itemCategory, double basketValue) {
        this.threshold = threshold;
        this.itemName = itemName;
        this.itemCategory = itemCategory;
        this.basketValue = basketValue;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public String getItemName() {
        return itemName;
    }

    public ItemCategory getItemCategory() {
        return itemCategory;
    }

    public double getBasketValue() {
        return basketValue;
    }

    public String toString() {
        if (threshold != null && itemCategory == null)
            return "Minimum quantity of item " + itemName + " is " + threshold;
        else if (threshold != null && itemCategory != null)
            return "Minimum quantity of category " + itemCategory + " is " + threshold;
        else
            return "Minimum basket value of is " + basketValue;
    }
}