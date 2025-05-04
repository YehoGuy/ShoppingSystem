package DomainLayer.Shop.Discount;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import DomainLayer.Item.ItemCategory;

/**
 * Represents a discount strategy tied to either an entire shop or a specific item within a shop.
 */
public interface Discount {

    /**
     * Applies this discount to a purchase.
     * @param items a map of item IDs to quantities in the purchase
     * @param totalPrice the total price before discount
     * @param itemsCategory a map of item IDs to their category
     * @return the new total price after applying this discount
     */
    Map<Integer, Double> applyDiscounts(Map<Integer, Integer> items, Map<Integer, AtomicInteger> prices, Map<Integer, Double> itemsDiscountedPrices, Map<Integer, ItemCategory> itemsCategory);

    boolean checkPolicies(Map<Integer,Integer> items, Map<Integer,Double> prices, Map<Integer,ItemCategory> itemsCategory);

    boolean isDouble();
}
