package com.example.app.ApplicationLayer.Purchase;

public interface PaymentMethod {
    /**
     * Checks if the payment service is available.
     *
     * @return true if the payment service is available, false otherwise.
     * @throws RuntimeException if the payment service is not available.
     */
    public boolean isPaymentServiceAvailable();

    /**
     * Processes the payment with the provided details.
     *
     * @param amount                The amount to be paid.
     * @param currency              The currency of the payment.
     * @param cardNumber            The credit card number.
     * @param expirationDateMonth   The expiration month of the credit card.
     * @param expirationDateYear    The expiration year of the credit card.
     * @param cardHolderName        The name of the card holder.
     * @param cvv                   The CVV of the credit card.
     * @param id                    An identifier for the transaction.
     * @return an integer representing the transaction ID or status.
     * @throws IllegalArgumentException if any of the required details are missing or invalid.
     */
    public int processPayment(double amount, String currency, String cardNumber, String expirationDateMonth, String expirationDateYear, String cardHolderName, String cvv, String id);

    /**
     * Cancels a payment with the given transaction ID.
     *
     * @param transactionId The ID of the transaction to cancel.
     * @return true if the cancellation was successful, false otherwise.
     * @throws IllegalArgumentException if the transaction ID is invalid or does not exist.
     */
    public boolean cancelPayment(int transactionId);
    
   
}
