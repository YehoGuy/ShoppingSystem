// ────────────────────────────────────────────────────────────────────────────
// src/main/java/DTOs/BidRecieptDTO.java
// Frontend DTO, mirroring the backend fields exactly
// ────────────────────────────────────────────────────────────────────────────
package DTOs;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Matches com.example.app.PresentationLayer.DTO.Purchase.BidRecieptDTO on the
 * backend.
 */
public class BidRecieptDTO {
    // ─── base receipt fields ───────────────────────────────────────────────────
    private int purchaseId;
    private int userId;
    private int storeId;
    private AddressDTO shippingAddress;
    // id -> quantity
    private Map<Integer, Integer> items;
    private boolean completed;
    private LocalDateTime timeOfCompletion;
    private double price;
    private LocalDateTime endTime; // End time of the auction, if applicable

    // ─── bid-specific fields ──────────────────────────────────────────────────
    private int thisBidderId;
    private int initialPrice;
    private int highestBid;
    private int highestBidderId;

    // Default no-args constructor (for Jackson)
    public BidRecieptDTO() {
    }

    public BidRecieptDTO(int purchaseId,
            int userId,
            int storeId,
            AddressDTO shippingAddress,
            Map<Integer, Integer> items,
            boolean completed,
            LocalDateTime timeOfCompletion,
            double price,
            int thisBidderId,
            int initialPrice,
            int highestBid,
            int highestBidderId,
            LocalDateTime endTime) {
        this.purchaseId = purchaseId;
        this.userId = userId;
        this.storeId = storeId;
        this.shippingAddress = shippingAddress;
        if (items.keySet().size() != 1) {
            throw new IllegalArgumentException("BidRecieptDTO should only contain one store's items");
        }
        this.items = items;
        this.completed = completed;
        this.timeOfCompletion = timeOfCompletion;
        this.price = price;
        this.thisBidderId = thisBidderId;
        this.initialPrice = initialPrice;
        this.highestBid = highestBid;
        this.highestBidderId = highestBidderId;
        this.endTime = endTime;
    }

    // ─── Getters & Setters ──────────────────────────────────────────────────

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

    public AddressDTO getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(AddressDTO shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public Map<Integer, Integer> getItems() {
        return items;
    }

    public void setItems(Map<Integer, Integer> items) {
        this.items = items;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
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

    public int getThisBidderId() {
        return thisBidderId;
    }

    public void setThisBidderId(int thisBidderId) {
        this.thisBidderId = thisBidderId;
    }

    public int getInitialPrice() {
        return initialPrice;
    }

    public void setInitialPrice(int initialPrice) {
        this.initialPrice = initialPrice;
    }

    public int getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(int highestBid) {
        this.highestBid = highestBid;
    }

    public int getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(int highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public ShoppingCartDTO toShopingCartDTO(String api) {
        Map<Integer, List<Integer>> shopItems = new java.util.HashMap<>();
        shopItems.put(storeId, items.keySet().stream().toList());

        Map<Integer, Map<Integer, Double>> shopItemPrices = new java.util.HashMap<>();
        Map<Integer, Double> itemPriceMap = new java.util.HashMap<>();
        Integer itemId = items.keySet().stream().findFirst().orElseThrow();
        itemPriceMap.put(itemId, (double) this.highestBid);
        shopItemPrices.put(storeId, itemPriceMap);
        Map<Integer, Map<Integer, Integer>> shopItemQuantities = new java.util.HashMap<>();

        List<ItemDTO> itemsList = List.of(getItemById(itemId, api)); // Placeholder, implement as needed

        return new ShoppingCartDTO(
                shopItems,
                shopItemPrices,
                shopItemQuantities,
                itemsList);
    }

    private ItemDTO getItemById(int id, String api) {

        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        String url = api + "/items/" + id;
        try {
            return restTemplate.getForObject(url, ItemDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch item with id " + id, e);
        }

    }
}
