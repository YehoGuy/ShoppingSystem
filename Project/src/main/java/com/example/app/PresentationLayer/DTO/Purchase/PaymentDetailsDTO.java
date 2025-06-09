package com.example.app.PresentationLayer.DTO.Purchase;

public class PaymentDetailsDTO {
    private String currency;
    private String cardNumber;
    private String expirationDateMonth;
    private String expirationDateYear;
    private String cardHolderName;
    private String cvv;
    private String id;

    // Default constructor for Jackson
    public PaymentDetailsDTO() {
    }

    public PaymentDetailsDTO(String currency, String cardNumber, String expirationDateMonth, String expirationDateYear, String cardHolderName, String cvv, String id) {
        this.currency = currency;
        this.cardNumber = cardNumber;
        this.expirationDateMonth = expirationDateMonth;
        this.expirationDateYear = expirationDateYear;
        this.cardHolderName = cardHolderName;
        this.cvv = cvv;
        this.id = id;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpirationDateMonth() {
        return expirationDateMonth;
    }

    public void setExpirationDateMonth(String expirationDateMonth) {
        this.expirationDateMonth = expirationDateMonth;
    }

    public String getExpirationDateYear() {
        return expirationDateYear;
    }

    public void setExpirationDateYear(String expirationDateYear) {
        this.expirationDateYear = expirationDateYear;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
