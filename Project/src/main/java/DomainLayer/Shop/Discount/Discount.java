package DomainLayer.Shop.Discount;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a discount strategy tied to either an entire shop or a specific item within a shop.
 */
public interface Discount {

    /**
     * The ID of the item this discount applies to, or null if it is a global shop discount.
     * @return the item ID, or null for global discounts
     */
    Integer getItemId();

    /**
     * The discount percentage (0–100).
     * @return the percentage of the discount
     */
    int getPercentage();

    /**
     * Applies this discount to a purchase.
     * @param items a map of item IDs to quantities in the purchase
     * @param totalPrice the total price before discount
     * @return the new total price after applying this discount
     */
    Map<Integer, Integer> applyDiscounts(Map<Integer, Integer> items, Map<Integer, AtomicInteger> prices, Map<Integer, Integer> itemsDiscountedPrices);

    /**
     * Updates the discount percentage.
     * @param percentage the new discount percentage (0–100)
     */
    void setPercentage(int percentage);
}
