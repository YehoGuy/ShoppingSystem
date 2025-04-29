package DomainLayer.Shop;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single implementation of {@link Discount} for both global and item-specific discounts.
 */
public class SingleDiscount implements Discount {
    private final int shopId;
    private final Integer itemId; // null for global
    private int percentage;

    /**
     * Constructor for a global discount.
     *
     * @param shopId    the shop ID
     * @param itemId   null for global discount
     * @param priceItem the price of the item (for item-specific discounts)
     * @param percentage the discount percentage (0-100)
     */
    public SingleDiscount(int shopId, Integer itemId, int priceItem, int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new OurArg("Discount percentage must be between 0 and 100");
        }
        this.shopId = shopId;
        this.itemId = itemId;
        this.percentage = percentage;
    }

    @Override
    public Integer getItemId() {
        return itemId;
    }

    @Override
    public int getPercentage() {
        return percentage;
    }

    @Override
    public void setPercentage(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new OurArg("Discount percentage must be between 0 and 100");
        }
        this.percentage = percentage;
    }

    @Override
    public Map<Integer, Integer> applyDiscounts(Map<Integer, Integer> items, Map<Integer, AtomicInteger> prices, Map<Integer, Integer> itemsDiscountedPrices) {
        if (itemId == null) {
            // global: apply to entire cart
            for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
                Integer itemId = entry.getKey();
                Integer qty = entry.getValue();
                if (qty != null && qty > 0) {
                    // determine full price of that item (requires external price lookup)
                    int itemPrice = prices.get(itemId).get();
                    int discountedPrice = itemPrice * (100 - percentage) / 100;
                    if(itemsDiscountedPrices.get(itemId) > discountedPrice) {
                        itemsDiscountedPrices.put(itemId, discountedPrice);
                    }
                }
            }
            return itemsDiscountedPrices;
        }
        // item-specific: calculate discount only on that item
        Integer qty = items.get(itemId);
        if (qty == null || qty <= 0) {
            return itemsDiscountedPrices;
        }

        int itemPrice = prices.get(itemId).get();
        int discountedPrice = itemPrice * (100 - percentage) / 100;
        if(itemsDiscountedPrices.get(itemId) > discountedPrice) {
            itemsDiscountedPrices.put(itemId, discountedPrice);
        }
          
        return itemsDiscountedPrices;
    }
}