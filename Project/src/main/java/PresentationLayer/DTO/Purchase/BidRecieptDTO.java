package PresentationLayer.DTO.Purchase;

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
        /* ——— bid‑specific fields ——— */
        @Positive int thisBidderId,
        @Positive int initialPrice,
        int highestBid,
        int highestBidderId) {

    /* Domain → DTO */
    public static BidRecieptDTO fromDomain(DomainLayer.Purchase.BidReciept b) {
        return new BidRecieptDTO(
                b.getPurchaseId(),
                b.getUserId(),
                b.getStoreId(),
                AddressDTO.fromDomain(b.getShippingAddress()),
                b.getItems(),
                b.isCompleted(),
                b.getTimeOfCompletion(),
                b.getPrice(),
                b.getThisBidderId(),
                b.getInitialPrice(),
                b.getHighestBid(),
                b.getHighestBidderId());
    }
}