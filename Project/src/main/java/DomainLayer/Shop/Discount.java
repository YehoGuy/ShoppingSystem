// package DomainLayer.Shop;

// import java.util.Map;

// /**
//  * The {@code Discount} interface defines the contract for implementing discount strategies
//  * in the shopping system. A discount strategy calculates the discount amount based on the 
//  * items in the purchase.
//  * 
//  * <p>Implementations of this interface should provide the logic for calculating discounts
//  * according to specific business rules, such as percentage-based discounts, quantity-based 
//  * discounts, or promotional offers.
//  */
// public interface Discount {

//     /**
//      * Applies the discount to the given items and calculates the total price after discount.
//      * 
//      * @param items A map where the key is the item ID (as {@code Integer}) and the value is the quantity of the item.
//      * @param totalPrice The total price of the purchase (as {@code int}).
//      * @return The total price after applying the discount (as {@code int}).
//      */
//     int applyDiscounts(Map<Integer, Integer> items, int totalPrice);

//     /**
//      * Returns the discount type as a string.
//      * 
//      * @return The discount type (as {@code String}).
//      */
//     void setDiscount(Map<Integer, Integer> items, int discount);

// }

// DomainLayer/Shop/Discount.java
package DomainLayer.Shop;

import java.util.Map;

/**
 * Represents a discount strategy tied to either an entire shop or a specific item within a shop.
 */
public interface Discount {

    /**
     * The ID of the shop this discount applies to.
     * @return the shop ID
     */
    int getShopId();

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
    int applyDiscounts(Map<Integer, Integer> items, int totalPrice);

    /**
     * Updates the discount percentage.
     * @param percentage the new discount percentage (0–100)
     */
    void setPercentage(int percentage);
}
