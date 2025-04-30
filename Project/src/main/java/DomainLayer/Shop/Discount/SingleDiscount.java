package DomainLayer.Shop.Discount;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import DomainLayer.Item.ItemCategory;

/**
 * Single implementation of {@link Discount} for both global and item-specific discounts.
 */
public class SingleDiscount implements Discount {
    private final Integer itemId; // null for global
    private int percentage;

    /**
     * Constructor for a global discount.
     *
     * @param itemId   null for global discount
     * @param percentage the discount percentage (0-100)
     */
    public SingleDiscount(Integer itemId, int percentage) {
        validatePercentage(percentage);
        this.itemId = itemId;
        this.percentage = percentage;
    }


    @Override
    public Map<Integer, Integer> applyDiscounts(Map<Integer, Integer> items, Map<Integer, Integer> prices, Map<Integer, ItemCategory> itemsCategory) {
        Map<Integer, Integer> itemsDiscountedPrices = new java.util.HashMap<>();
        // item-specific: calculate discount only on that item
        Integer qty = items.get(itemId);
        if (qty == null || qty <= 0) {
            return itemsDiscountedPrices;
        }

        int itemPrice = prices.get(itemId);
        int discountedPrice = itemPrice * (100 - percentage) / 100;
        itemsDiscountedPrices.put(itemId, discountedPrice);
          
        return itemsDiscountedPrices;
    }

    private void validatePercentage(int p) {
        if (p < 0 || p > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
    }
}