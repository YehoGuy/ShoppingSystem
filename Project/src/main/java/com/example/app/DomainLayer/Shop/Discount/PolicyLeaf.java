package com.example.app.DomainLayer.Shop.Discount;

import java.util.Map;
import com.example.app.DomainLayer.Item.ItemCategory;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("leaf")
public class PolicyLeaf extends Policy {

    private Integer threshold;
    private Integer itemId;
    private ItemCategory itemCategory;
    private Double basketValue;

    // NEW ctor capturing metadata + predicate
    public PolicyLeaf(Integer threshold, Integer itemId, ItemCategory itemCategory, Double basketValue) {
        this.threshold = threshold;
        this.itemId = itemId;
        this.itemCategory = itemCategory;
        this.basketValue = basketValue;
    }

    public PolicyLeaf() {
        this(null, null, null, null); // Default constructor for JPA
    }

    @Override
    public boolean test(Map<Integer, Integer> items, Map<Integer, Double> prices,
            Map<Integer, ItemCategory> itemsCategory) {
        if (this.getThreshold() != null && this.getItemId() != null)
            return items.getOrDefault(this.getItemId(), 0) >= this.getThreshold();
        else if (this.getThreshold() != null && this.getCategory() != null)
            return items.entrySet().stream().filter(e -> itemsCategory.get(e.getKey()) == this.getCategory())
                    .mapToInt(Map.Entry::getValue).sum() >= this.getThreshold();
        else if (this.getBasketValue() != null)
            return items.entrySet().stream()
                    .mapToDouble(e -> prices.getOrDefault(e.getKey(), 0.0) * e.getValue())
                    .sum() >= this.getBasketValue();
        else
            return true; // Default case, no specific policy applied
    }

    // NEW getters so we can reverse-map to DTO:
    public Integer getThreshold() {
        return threshold;
    }

    public Integer getItemId() {
        return itemId;
    }

    public ItemCategory getCategory() {
        return itemCategory;
    }

    public Double getBasketValue() {
        return basketValue;
    }
}
