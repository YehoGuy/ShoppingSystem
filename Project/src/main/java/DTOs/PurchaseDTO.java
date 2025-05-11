package DTOs;

import java.time.LocalDateTime;
import java.util.Map;

public class PurchaseDTO {

    private final int purchaseId;
    private final int userId;
    private final int storeId;
    private final Map<Integer, Integer> items; // itemId -> quantity
    private final AddressDTO shippingAddress;
    private final boolean isCompleted;
    private final LocalDateTime timeOfCompletion;
    private final double price;

    public PurchaseDTO(
        int purchaseId,
        int userId,
        int storeId,
        Map<Integer, Integer> items,
        AddressDTO shippingAddress,
        boolean isCompleted,
        LocalDateTime timeOfCompletion,
        double price
    ) {
        this.purchaseId = purchaseId;
        this.userId = userId;
        this.storeId = storeId;
        this.items = items;
        this.shippingAddress = shippingAddress;
        this.isCompleted = isCompleted;
        this.timeOfCompletion = timeOfCompletion;
        this.price = price;
    }

    public int getPurchaseId() {
        return purchaseId;
    }

    public int getUserId() {
        return userId;
    }

    public int getStoreId() {
        return storeId;
    }

    public Map<Integer, Integer> getItems() {
        return items;
    }

    public AddressDTO getShippingAddress() {
        return shippingAddress;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public LocalDateTime getTimeOfCompletion() {
        return timeOfCompletion;
    }

    public double getPrice() {
        return price;
    }
}
