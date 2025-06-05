package com.example.app.DomainLayer.Purchase;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This Data Object class represents a receipt for a purchase.
 * It contains information about the items purchased, their prices, the purchase status etc...
 */

public class Reciept {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected final int purchaseId;                     // purchase ID
    
    @Column(name = "user_id", nullable = false)
    protected final int userId;                        // initiating user ID
    
    @Column(name = "store_id", nullable = false)
    protected final int storeId;                      // store ID
    
    @Transient
    protected final Map<Integer, Integer> items; // itemId -> quantity
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "zip_code")
    protected final Address shippingAddress;              // shipping address
    
    @Column(name = "is_completed", nullable = false)
    protected final AtomicBoolean isCompleted;                 // purchase status   
    
    @Column(name = "time_of_completion", nullable = false)
    protected final LocalDateTime timeOfCompletion;     // time of purchase completion
    
    @Column(name = "price", nullable = false)
    protected final double price; // total price of the purchase

    // very important to make sure a reciept is concurrent עכשווית
    @Column(name = "timestamp_of_reciept_generation", nullable = false)
    protected final LocalDateTime timestampOfRecieptGeneration; // time of receipt generation!!

    /**
     * Constructs a new {@code Reciept} with the specified user ID, store ID, and items.
     * this constructor is meant for UNCOMPLETED purchases
     *
     * @param purchaseId the ID of the purchase.
     * @param userId the ID of the user initiating the purchase.
     * @param storeId the ID of the store where the purchase is made.
     * @param items a map of item IDs to their quantities.
     * @param shippingAddress the shipping address for the purchase.
     */
    public Reciept(int purchaseId, int userId, int storeId, Map<Integer, Integer> items, Address shippingAddress, double price, boolean isCompleted) {
        this.purchaseId = purchaseId;
        this.userId = userId;
        this.storeId = storeId;
        this.items = Map.copyOf(items);
        this.shippingAddress = shippingAddress;
        this.isCompleted = new AtomicBoolean(isCompleted);
        this.timeOfCompletion = null;
        this.timestampOfRecieptGeneration = LocalDateTime.now();
        this.price = price;
    }

    
    /**
     * Constructs a new {@code Reciept} with the specified user ID, store ID, and items.
     * this constructor is meant for UNCOMPLETED purchases
     *
     * @param purchaseId the ID of the purchase.
     * @param userId the ID of the user initiating the purchase.
     * @param storeId the ID of the store where the purchase is made.
     * @param items a map of item IDs to their quantities.
     * @param shippingAddress the shipping address for the purchase.
     */
    public Reciept(int purchaseId, int userId, int storeId, Map<Integer, Integer> items, Address shippingAddress, LocalDateTime timeOfCompletion, double price) {
        this.purchaseId = purchaseId;
        this.userId = userId;
        this.storeId = storeId;
        this.items = Map.copyOf(items);
        this.shippingAddress = shippingAddress;
        this.isCompleted = new AtomicBoolean(true);
        this.timeOfCompletion = timeOfCompletion;
        this.timestampOfRecieptGeneration = LocalDateTime.now();
        this.price = price;
    }

    /**
     * Returns the ID of the purchase.
     *
     * @return the purchase ID.
     */
    public int getPurchaseId() {
        return purchaseId;
    }
    /**
     * Returns the ID of the user who initiated the purchase.
     *
     * @return the user ID.
     */
    public int getUserId() {
        return userId;
    }
    /**
     * Returns the ID of the store where the purchase was made.
     *
     * @return the store ID.
     */
    public int getStoreId() {
        return storeId;
    }
    /**
     * Returns a map of item IDs to their quantities in the purchase.
     *
     * @return a {@code HashMap} containing the items and their quantities.
     */
    public Map<Integer, Integer> getItems() {
        // items is immutable, so we can return it directly
        return items;
    }

    /**
     * Returns the shipping address for the purchase.
     *
     * @return the shipping address as string.
     */
    public String getShippingAddressString() {
        return shippingAddress.toString();
    }

    /**
     * Returns the shipping address for the purchase.
     *
     * @return the shipping address.
     */
    public Address getShippingAddress() {
        return shippingAddress;
    }

    /**
     * Returns the price in the reciept.
     *
     * @return the price.
     */
    public double getPrice() {
        return price;
    }

    /**
     * Returns whether the purchase is completed.
     *
     * @return {@code true} if the purchase is completed, {@code false} otherwise.
     */
    public boolean isCompleted() {
        return isCompleted.get();
    }

    /**
     * Returns the time of purchase completion.
     *
     * @return the time of purchase completion.
     */
    public LocalDateTime getTimeOfCompletion() {
        return timeOfCompletion;
    }

    /**
     * Returns the time of receipt generation.
     *
     * @return the time of receipt generation.
     */
    public LocalDateTime getTimestampOfRecieptGeneration() {
        return timestampOfRecieptGeneration;
    }

    /**
     * Returns a string representation of the receipt.
     *
     * @return a string representation of the receipt.
     */
    @Override
    public String toString() {
        return "Reciept{" +
                "purchaseId=" + purchaseId +
                ", userId=" + userId +
                ", storeId=" + storeId +
                ", items=" + items +
                ", shippingAddress=" + shippingAddress +
                ", isCompleted=" + isCompleted +
                ", timeOfCompletion=" + timeOfCompletion +
                ", timestampOfRecieptGeneration=" + timestampOfRecieptGeneration +
                '}';
    }
    


}
