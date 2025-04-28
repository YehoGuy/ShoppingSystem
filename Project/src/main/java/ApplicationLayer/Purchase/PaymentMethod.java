package ApplicationLayer.Purchase;

public interface PaymentMethod {
    // This interface will define the methods for different payment methods
    // For example, credit card, PayPal, etc.

    /**
     * Processes a payment with the specified amount.
     * 
     * <p>Implementations of this method should handle the logic for completing a payment
     * transaction, including any necessary validations or interactions with external systems.
     * 
     * @param amount the amount to be processed for payment. Must be a positive value.
     * @throws IllegalArgumentException if the amount is negative or zero.
     */
    void processPayment(double amount, int shopId);

    /**
     * Retrieves the details of the payment method.
     * 
     * <p>This method should return a string representation of the payment method's details,
     * such as the type of payment method (e.g., "Credit Card") or specific account information.
     * 
     * @return a string containing the payment method's details.
     */
    String getDetails();

    /**
     * refunds a payment with the specified amount.
     * <p>Implementations of this method should handle the logic for processing a refund
     * transaction, including any necessary validations or interactions with external systems.
     * 
     * @param amount the amount to be refunded. Must be a positive value.
     * @param shopId the ID of the shop where the payment was made.
     * @throws IllegalArgumentException if the amount is negative or zero.
     * @throws IllegalStateException if the refund cannot be processed (e.g., payment not found).
     */
    void refundPayment(double amount, int shopId);
   
}
