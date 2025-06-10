package com.example.app.PresentationLayer.DTO.Purchase;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.validation.constraints.Positive;

/** Extends the basic receipt view with bidding details. */
public record BidRecieptDTO(
        /*  ——— base receipt fields ——— */
        @Positive int purchaseId,
        @Positive int userId,
        @Positive int storeId,
        AddressDTO shippingAddress,
        Map<Integer, Integer> items,
        boolean completed,
        LocalDateTime timeOfCompletion,
        double price,
        LocalDateTime endTime, // End time of the auction, if applicable
        /* ——— bid‑specific fields ——— */
        @Positive int thisBidderId,
        @Positive int initialPrice,
        int highestBid,
        int highestBidderId) {

    /* Domain → DTO */
    public static BidRecieptDTO fromDomain(com.example.app.DomainLayer.Purchase.BidReciept b) {
        return new BidRecieptDTO(
                b.getPurchaseId(),
                b.getUserId(),
                b.getStoreId(),
                new AddressDTO(
                    "xxx","xxx", "xxx", "xxx", "xxx", "xxx" // Placeholder, replace with actual address fields
                ),
                b.getItems(),
                b.isCompleted(),
                b.getTimeOfCompletion(),
                b.getPrice(),
                b.getEndTime(),
                b.getThisBidderId(),
                b.getInitialPrice(),
                b.getHighestBid(),
                b.getHighestBidderId());
    }

}