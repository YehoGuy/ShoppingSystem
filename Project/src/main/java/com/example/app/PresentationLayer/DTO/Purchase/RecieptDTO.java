package com.example.app.PresentationLayer.DTO.Purchase;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Read-model DTO returned by GET /api/purchases/… endpoints.
 * For creation you usually POST a CheckoutRequest, not a raw RecieptDTO.
 */
    public record RecieptDTO(
        @Positive int purchaseId,
        @Positive int userId,
        @Positive int storeId,
        @NotNull AddressDTO shippingAddress,
        @NotNull Map<Integer, Integer> items,
        boolean completed,
        LocalDateTime timeOfCompletion,
        double price,
        LocalDateTime timestampOfReceiptGeneration
    ) {
    /** Domain → DTO */
    public static RecieptDTO fromDomain(com.example.app.DomainLayer.Purchase.Reciept r) {
        return new RecieptDTO(
            r.getPurchaseId(),
            r.getUserId(),
            r.getStoreId(),
            AddressDTO.fromDomain(r.getShippingAddress()),
            r.getItems(),
            r.isCompleted(),
            r.getTimeOfCompletion(),
            r.getPrice(),
            r.getTimestampOfRecieptGeneration()
        );
    }
}
