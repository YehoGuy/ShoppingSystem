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
     * Applies the discount to the given items and calculates the total price after discount.
     * 
     * @param items A map where the key is the item ID (as {@code Integer}) and the value is the quantity of the item.
     * @param totalPrice The total price of the purchase (as {@code int}).
     * @return The total price after applying the discount (as {@code int}).
     */
    int applyDiscounts(Map<Integer, Integer> items, int totalPrice);

    /**
     * Returns the discount type as a string.
     * 
     * @return The discount type (as {@code String}).
     */
    void setDiscount(Map<Integer, Integer> items, int discount);
}