package com.example.app.ApplicationLayer.Purchase;

/**
 * Represents a shipping method for processing shipments and retrieving shipping details.
 * Implementations of this interface should define specific behaviors for different types of shipping,
 * such as standard shipping, express shipping, or international shipping.
 */
public interface ShippingMethod {

    /**
     * Processes a shipment to the specified address.
     *
     * @param purchaseId The ID of the purchase associated with the shipment.
     * @param country    The country where the shipment is to be delivered.
     * @param city       The city where the shipment is to be delivered.
     * @param street     The street address where the shipment is to be delivered.
     * @param postalCode The postal code of the delivery address.
     * 
     * @throws IllegalArgumentException if any of the address parameters are null or invalid.
     */
    void processShipment(int purchaseId, String country, String city, String street, String postalCode);

    /**
     * Retrieves the details of the shipping method.
     * This may include information such as the type of shipping (e.g., standard, express),
     * estimated delivery time, and cost.
     *
     * @return A string containing the details of the shipping method.
     */
    String getDetails();

}