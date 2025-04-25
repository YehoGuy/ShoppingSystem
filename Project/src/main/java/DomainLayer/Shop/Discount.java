package DomainLayer.Shop;

import java.util.Map;

/**
 * The {@code Discount} interface defines the contract for implementing discount strategies
 * in the shopping system. A discount strategy calculates the discount amount based on the 
 * items in the purchase.
 * 
 * <p>Implementations of this interface should provide the logic for calculating discounts
 * according to specific business rules, such as percentage-based discounts, quantity-based 
 * discounts, or promotional offers.
 */
public interface Discount {

    /**
     * Calculates the discount amount for a given set of items.
     * 
     * @param items A map where the key is the item ID (as {@code Integer}) and the value is the quantity of the item.
     * @return The discount amount (as {@code int}) to be applied to the purchase.
     */
    int getDiscount(Map<Integer, Integer> items);

}