package com.example.app.DomainLayer.Shop;

import java.util.Map;

/**
 * The {@code PurchasePolicy} interface defines the contract for implementing purchase policies
 * in the shopping system. A purchase policy determines whether a purchase is valid based on
 * specific criteria such as the items being purchased and their total price.
 * 
 * <p>Implementations of this interface should provide the logic for validating purchases
 * according to the business rules of the system.
 */
public interface PurchasePolicy {

    /**
     * Validates whether a purchase is allowed based on the provided items and total price.
     * 
     * @param items A map where the key is the item ID (as {@code Integer}) and the value is the quantity of the item.
     * @param totalPrice The total price of the purchase (as {@code int}).
     * @return {@code true} if the purchase is valid according to the policy, {@code false} otherwise.
     */
    boolean isValidPurchase(Map<Integer, Integer> items, int totalPrice);

    /**
     * Returns the type of the purchase policy as a string.
     * 
     * @return The purchase policy type (as {@code String}).
     */
    void setPolicy(Map<Integer, Integer> items, int policy);

}