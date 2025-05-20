package com.example.app.PresentationLayer.DTO.Purchase;

import java.time.LocalDateTime;
import java.util.Map;

public record PurchaseDTO(
    int purchaseId,
    int userId,
    int storeId,
    Map<Integer, Integer> items,
    AddressDTO shippingAddress,
    boolean isCompleted,
    LocalDateTime timeOfCompletion,
    double price
) {
    /* -------- Domain → DTO -------- */
    public static PurchaseDTO fromDomain(com.example.app.DomainLayer.Purchase.Purchase p) {
        return new PurchaseDTO(
            p.getPurchaseId(),
            p.getUserId(),
            p.getStoreId(),
            p.getItems(),
            AddressDTO.fromDomain(p.getShippingAddress()),
            p.isCompleted(),
            p.getTimeOfCompletion(),
            p.getPrice()
        );
    }
}