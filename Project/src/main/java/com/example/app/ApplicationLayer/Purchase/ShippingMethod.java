package com.example.app.ApplicationLayer.Purchase;

/**
 * Represents a shipping method for processing shipments and retrieving shipping details.
 * Implementations of this interface should define specific behaviors for different types of shipping,
 * such as standard shipping, express shipping, or international shipping.
 */
public interface ShippingMethod {

    /**
     * Checks if the shipping service is available.
     *
     * @return true if the shipping service is available, false otherwise.
     * @throws RuntimeException if the shipping service is not available.
     */
    public boolean isShippingServiceAvailable();

    /**
     * Processes the shipping with the provided details.
     *
     * @param name      The name of the recipient.
     * @param address   The address for shipping.
     * @param city      The city for shipping.
     * @param country   The country for shipping.
     * @param zipCode   The zip code for shipping.
     * @return an integer representing the shipping ID or status.
     * @throws IllegalArgumentException if any of the required details are missing or invalid.
     */
    public int processShipping(String name, String address, String city, String country, String zipCode);

    /**
     * Retrieves the shipping details for a given shipping ID.
     *
     * @param shippingId The ID of the shipping to retrieve details for.
     * @return a string containing the shipping details.
     */
    public boolean cancelShipping(int shippingId);
}