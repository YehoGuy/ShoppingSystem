package com.example.app.DomainLayer.Shop.Discount;

import java.util.Map;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Discount.Policy;
import com.example.app.DomainLayer.Shop.Discount.TriPredicate;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("leaf")
public class PolicyLeaf extends Policy {

    private final Integer threshold;
    private final Integer itemId;
    private final ItemCategory itemCategory;
    private final Double basketValue;

    // your predicate logic
    @Embedded
    private final TriPredicate<Map<Integer, Integer>, Map<Integer, Double>, Map<Integer, ItemCategory>> predicate;

    // NEW ctor capturing metadata + predicate
    public PolicyLeaf(Integer threshold, Integer itemId, ItemCategory itemCategory, Double basketValue,
            TriPredicate<Map<Integer, Integer>, Map<Integer, Double>, Map<Integer, ItemCategory>> predicate) {
        this.threshold = threshold;
        this.itemId = itemId;
        this.itemCategory = itemCategory;
        this.basketValue = basketValue;
        this.predicate = predicate;
    }

    // old ctor—delegate to new one with null metadata
    public PolicyLeaf(TriPredicate<Map<Integer, Integer>, Map<Integer, Double>, Map<Integer, ItemCategory>> predicate) {
        this(null, null, null, null, predicate);
    }

    @Override
    public boolean test(Map<Integer, Integer> items, Map<Integer, Double> prices,
            Map<Integer, ItemCategory> itemsCategory) {
        return predicate.test(items, prices, itemsCategory);
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
