package DomainLayer.Shop.Discount;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import DomainLayer.Item.ItemCategory;

public class GlobalDiscount implements Discount {
    
    private final int percentage;

    public GlobalDiscount(int percentage) {
        validatePercentage(percentage);
        this.percentage = percentage;
    }

    @Override
    public Map<Integer, Integer> applyDiscounts(Map<Integer, Integer> items, Map<Integer, Integer> prices, Map<Integer, ItemCategory> itemsCategory) {
        Map<Integer, Integer> itemsDiscountedPrices = new java.util.HashMap<>();
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            Integer itemId = entry.getKey();
            Integer qty = entry.getValue();
            if (qty != null && qty > 0) {
                // determine full price of that item (requires external price lookup)
                int itemPrice = prices.get(itemId);
                int discountedPrice = itemPrice * (100 - percentage) / 100;
                itemsDiscountedPrices.put(itemId, discountedPrice);
            }
        }
        return itemsDiscountedPrices;
    }

    private void validatePercentage(int p) {
        if (p < 0 || p > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
    }
}

