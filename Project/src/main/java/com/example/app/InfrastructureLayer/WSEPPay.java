package com.example.app.InfrastructureLayer;

import java.util.Map;


import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.example.app.ApplicationLayer.Purchase.PaymentMethod;


public class WSEPPay implements PaymentMethod {

    private static final String PAYMENT_URL = "https://damp-lynna-wsep-1984852e.koyeb.app/"; // Replace with actual URL
    private final RestTemplate restTemplate;

    public WSEPPay() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean isPaymentServiceAvailable() {
        Map<String, String> postContent = Map.of(
            "action_type", "handshake"
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.setAll(postContent);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
        String body = restTemplate.postForObject(PAYMENT_URL, request, String.class);

        if (body == null) {
            throw new RuntimeException("Payment service is not available");
        }

        return body.equals("OK");
    }
    
    @Override
    public int processPayment(double amount, String currency, String cardNumber, String expirationDateMonth, String expirationDateYear, String cardHolderName, String cvv, String id) {
        if (amount <= 0 || currency == null || cardNumber == null || expirationDateMonth == null || expirationDateYear == null || cardHolderName == null || cvv == null || id == null) {
            throw new IllegalArgumentException("All payment details must be provided and valid");
        }
        
        // Payment
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        Map<String, String> postContentPayment = Map.of(
            "action_type", "pay",
            "amount", String.valueOf(amount),
            "currency", currency,
            "card_number", cardNumber,
            "month", expirationDateMonth,
            "year", expirationDateYear,
            "holder", cardHolderName,
            "cvv", cvv,
            "id", id
        );
        MultiValueMap<String, String> formDataPayment = new LinkedMultiValueMap<>();
        formDataPayment.setAll(postContentPayment);
        HttpEntity<MultiValueMap<String, String>> requestPayment = new HttpEntity<>(formDataPayment, headers);
        String responseBody = restTemplate.postForObject(PAYMENT_URL, requestPayment, String.class);

        try {
            int paymentId = Integer.parseInt(responseBody);
            return paymentId;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public boolean cancelPayment(int paymentId) {
        if (paymentId <= 10000 || paymentId >= 100000) {
            throw new IllegalArgumentException("Payment ID must be a positive integer");
        }
        // Cancel payment
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        Map<String, String> postContentCancel = Map.of(
            "action_type", "cancel_pay",
            "transaction_id", String.valueOf(paymentId)
        );
        MultiValueMap<String, String> formDataCancel = new LinkedMultiValueMap<>();
        formDataCancel.setAll(postContentCancel);
        HttpEntity<MultiValueMap<String, String>> requestCancel = new HttpEntity<>(formDataCancel, headers);
        String responseBody = restTemplate.postForObject(PAYMENT_URL, requestCancel, String.class);

        return responseBody.equals("1");
    }
    
}

