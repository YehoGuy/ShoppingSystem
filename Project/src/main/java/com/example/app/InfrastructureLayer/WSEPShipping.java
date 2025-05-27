package com.example.app.InfrastructureLayer;

import java.util.Map;


import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class WSEPShipping {

    private static final String PAYMENT_URL = "https://damp-lynna-wsep-1984852e.koyeb.app/"; // Replace with actual URL
    private final RestTemplate restTemplate;

    public WSEPShipping() {
        this.restTemplate = new RestTemplate();
    }

    public boolean isShippingServiceAvailable() {
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
            throw new RuntimeException("Shipping service is not available");
        }

        return body.equals("OK");
    }

    public int processShipping(String name, String address, String city, String country, String zipCode) {
        if (name == null || address == null || city == null || country == null || zipCode == null) {
            throw new IllegalArgumentException("All shipping details must be provided");
        }
        // Shipping
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        Map<String, String> postContentShipping = Map.of(
            "action_type", "supply",
            "name", name,
            "address", address,
            "city", city,
            "country", country,
            "zip", zipCode
        );
        MultiValueMap<String, String> formDataShipping = new LinkedMultiValueMap<>();
        formDataShipping.setAll(postContentShipping);
        HttpEntity<MultiValueMap<String, String>> requestShipping = new HttpEntity<>(formDataShipping, headers);
        String responseBody = restTemplate.postForObject(PAYMENT_URL, requestShipping, String.class);

        if (responseBody == null) {
            throw new RuntimeException("Failed to process shipping");
        }

        try {
            return Integer.parseInt(responseBody);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid response from shipping service: " + responseBody, e);
        }
    }

    public boolean cancelShipping(int shippingId) {
        if (shippingId <= 0) {
            throw new IllegalArgumentException("Invalid shipping ID");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        Map<String, String> postContentCancel = Map.of(
            "action_type", "cancel_supply",
            "transaction_id", String.valueOf(shippingId)
        );
        MultiValueMap<String, String> formDataCancel = new LinkedMultiValueMap<>();
        formDataCancel.setAll(postContentCancel);
        HttpEntity<MultiValueMap<String, String>> requestCancel = new HttpEntity<>(formDataCancel, headers);
        String responseBody = restTemplate.postForObject(PAYMENT_URL, requestCancel, String.class);

        return responseBody != null && responseBody.equals("1");
    }
    
}
