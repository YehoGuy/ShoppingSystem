package DomainLayer.Shop;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A discount that applies a percentage off the entire purchase
 * if all specified bundle item IDs are present in the cart.
 */
public class BundleDiscount implements Discount {
    private final int shopId;
    private final Map<Integer, Integer> bundleItems; // itemId -> threshold
    private int percentage;

    /**
     * @param shopId       the shop this discount belongs to
     * @param bundleItems  the set of item IDs that must all be in the cart
     * @param percentage   the discount percentage (0–100)
     */
    public BundleDiscount(int shopId, Map<Integer, Integer> bundleItems, int percentage) {
        if (bundleItems == null || bundleItems.isEmpty()) {
            throw new IllegalArgumentException("Bundle items must not be null or empty");
        }
        validatePercentage(percentage);
        this.shopId       = shopId;
        this.bundleItems  = Map.copyOf(bundleItems);
        this.percentage   = percentage;
    }

    @Override
    public int getShopId() {
        return shopId;
    }

    @Override
    public Integer getItemId() {
        return null;  // whole‐cart discount when bundle is present
    }

    @Override
    public int getPercentage() {
        return percentage;
    }

    @Override
    public void setPercentage(int percentage) {
        validatePercentage(percentage);
        this.percentage = percentage;
    }

    @Override
    public Map<Integer, Integer> applyDiscounts(Map<Integer, Integer> items, Map<Integer, AtomicInteger> prices, Map<Integer, Integer> itemsDiscountedPrices) {
        // 1) Verify bundle completeness
        for (Map.Entry<Integer, Integer> req : bundleItems.entrySet()) {
            int itemId      = req.getKey();
            int requiredQty = req.getValue();
            Integer actualQty = items.get(itemId);
            if (actualQty == null || actualQty < requiredQty) {
                // missing a required bundle item → no discount
                return itemsDiscountedPrices;
            }
        }

        // 2) Apply discount
        for (Map.Entry<Integer, Integer> req : bundleItems.entrySet()) {
            int itemId      = req.getKey();
            int requiredQty = req.getValue();
            Integer actualQty = items.get(itemId);
            if (actualQty != null && actualQty >= requiredQty) {
                // apply discount to the item
                AtomicInteger price = prices.get(itemId);
                if (price != null) {
                    int discountedPrice = price.get() * (100 - percentage) / 100;
                    if(itemsDiscountedPrices.get(itemId) > discountedPrice) {
                        itemsDiscountedPrices.put(itemId, discountedPrice);
                    }
                }
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
