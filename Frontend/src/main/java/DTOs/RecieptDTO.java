package DTOs;

import java.time.LocalDateTime;
import java.util.Map;

public class RecieptDTO {
    private int purchaseId;
    private int userId;
    private int storeId;
    private Map<Integer, Integer> items; // itemId -> quantity
    private AddressDTO shippingAddress;
    private boolean isCompleted;
    private LocalDateTime timeOfCompletion;
    private double price;
    private LocalDateTime timestampOfRecieptGeneration;

    public RecieptDTO(int purchaseId, int userId, int storeId, Map<Integer, Integer> items,
            AddressDTO shippingAddress, boolean isCompleted, LocalDateTime timeOfCompletion,
            double price, LocalDateTime timestampOfRecieptGeneration) {
        this.purchaseId = purchaseId;
        this.userId = userId;
        this.storeId = storeId;
        this.items = items;
        this.shippingAddress = shippingAddress;
        this.isCompleted = isCompleted;
        this.timeOfCompletion = timeOfCompletion;
        this.price = price;
        this.timestampOfRecieptGeneration = timestampOfRecieptGeneration;
    }

    public RecieptDTO() {
        // Default constructor
    }

    public int getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(int purchaseId) {
        this.purchaseId = purchaseId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getStoreId() {
        return storeId;
    }

    public void setStoreId(int storeId) {
        this.storeId = storeId;
    }

    public Map<Integer, Integer> getItems() {
        return items;
    }

    public void setItems(Map<Integer, Integer> items) {
        this.items = items;
    }

    public AddressDTO getAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(AddressDTO shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public LocalDateTime getTimeOfCompletion() {
        return timeOfCompletion;
    }

    public void setTimeOfCompletion(LocalDateTime timeOfCompletion) {
        this.timeOfCompletion = timeOfCompletion;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public LocalDateTime getTimestampOfRecieptGeneration() {
        return timestampOfRecieptGeneration;
    }

    public void setTimestampOfRecieptGeneration(LocalDateTime timestampOfRecieptGeneration) {
        this.timestampOfRecieptGeneration = timestampOfRecieptGeneration;
    }
}
