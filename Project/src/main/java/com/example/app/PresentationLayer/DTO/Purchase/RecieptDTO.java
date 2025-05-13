package com.example.app.PresentationLayer.DTO.Purchase;


import java.time.LocalDateTime;
import java.util.Map;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;

/**
 
Read‑model DTO returned by GET /receipts/… endpoints.
For creation you usually post a CheckoutRequest, not a raw RecieptDTO.*/
public record RecieptDTO(
        @Positive int purchaseId,
        @Positive int userId,
        @Positive int storeId,
        @NotNull AddressDTO shippingAddress,
        @NotNull Map<Integer, Integer> items,
        boolean completed,
        LocalDateTime timeOfCompletion,
        double price) {

    /* Domain → DTO */
    public static RecieptDTO fromDomain(com.example.app.DomainLayer.Purchase.Reciept r) {
        return new RecieptDTO(
                r.getPurchaseId(),
                r.getUserId(),
                r.getStoreId(),
                AddressDTO.fromDomain(r.getShippingAddress()),
                r.getItems(),
                r.isCompleted(),
                r.getTimeOfCompletion(),
                r.getPrice());
    }

    /* DTO → Domain (rare—used mainly in tests or admin tools) */
    public com.example.app.DomainLayer.Purchase.Reciept toDomain() {
        if (completed) {
            return new com.example.app.DomainLayer.Purchase.Reciept(
                    purchaseId, userId, storeId, items,
                    shippingAddress.toDomain(), timeOfCompletion, price);
        }
        return new com.example.app.DomainLayer.Purchase.Reciept(
                purchaseId, userId, storeId, items,
                shippingAddress.toDomain(), price);
    }
}