package com.example.app.DomainLayer.Shop.Discount;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.app.DomainLayer.Item.ItemCategory;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * Represents a discount strategy tied to either an entire shop or a specific
 * item within a shop.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "discount_type")
public abstract class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    /**
     * Applies this discount to a purchase.
     * 
     * @param items         a map of item IDs to quantities in the purchase
     * @param totalPrice    the total price before discount
     * @param itemsCategory a map of item IDs to their category
     * @return the new total price after applying this discount
     */
    public abstract Map<Integer, Double> applyDiscounts(Map<Integer, Integer> items, Map<Integer, AtomicInteger> prices,
            Map<Integer, Double> itemsDiscountedPrices, Map<Integer, ItemCategory> itemsCategory);

    public abstract boolean checkPolicies(Map<Integer, Integer> items, Map<Integer, Double> prices,
            Map<Integer, ItemCategory> itemsCategory);

    public abstract boolean isDouble();

    public abstract Integer getPercentage();

    public abstract Policy getPolicy();

    public abstract ItemCategory getItemCategory();

    public abstract Integer getItemId();

}
