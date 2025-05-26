package com.example.app.DomainLayer.Shop.Discount;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.app.DomainLayer.Item.ItemCategory;

/**
 * Single implementation of {@link Discount} for both global and item-specific discounts.
 */
public class SingleDiscount implements Discount {
    private final Integer itemId; // null for global
    private int percentage;
    private Policy policyHead;
    private boolean isDouble;

    /**
     * Constructor for a global discount.
     *
     * @param itemId   null for global discount
     * @param percentage the discount percentage (0-100)
     */
    public SingleDiscount(Integer itemId, int percentage, Policy policyHead, boolean isDouble) {
        validatePercentage(percentage);
        this.itemId = itemId;
        this.percentage = percentage;
        this.policyHead = policyHead;
        this.isDouble = isDouble;
    }


    @Override
    public Map<Integer, Double> applyDiscounts(Map<Integer, Integer> items, Map<Integer, AtomicInteger> prices, Map<Integer, Double> itemsDiscountedPrices, Map<Integer, ItemCategory> itemsCategory) {
        // item-specific: calculate discount only on that item
        Integer qty = items.get(itemId);
        if (qty == null || qty <= 0) {
            return itemsDiscountedPrices;
        }

        // check policies   
        if (!(checkPolicies(items, itemsDiscountedPrices, itemsCategory))) {
            return itemsDiscountedPrices;
        }

        if(!isDouble){
            double itemPrice = prices.get(itemId).get();
            double discountedPrice = itemPrice * (100 - percentage) / 100;
            itemsDiscountedPrices.put(itemId,Math.min(itemsDiscountedPrices.get(itemId), discountedPrice));
        }else{
            double itemPrice = itemsDiscountedPrices.get(itemId);
            double discountedPrice = itemPrice * (100 - percentage) / 100;
            itemsDiscountedPrices.put(itemId, discountedPrice);
        }
          
        return itemsDiscountedPrices;
    }

    @Override
    public boolean checkPolicies(Map<Integer,Integer> items, Map<Integer,Double> prices, Map<Integer,ItemCategory> itemsCategory) {
        if(policyHead == null) {
            return true; // No policies to check
        }
        return policyHead.test(items, prices, itemsCategory);
    }

    public Integer getItemId() {
        return itemId;
    }

    @Override
    public boolean isDouble() {
        return isDouble;
    }

    private void validatePercentage(int p) {
        if (p < 0 || p > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
    }

    @Override
    public Integer getPercentage() {
        return percentage;
    }
}