package com.example.app.DomainLayer.Purchase;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.print.attribute.HashAttributeSet;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "purchases")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "purchase_type")
@DiscriminatorValue("regular")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected int purchaseId; // purchase ID
    protected int userId; // initiating user ID
    protected int storeId; // store ID

    @ElementCollection
    @CollectionTable(name = "purchase_items", joinColumns = @JoinColumn(name = "purchase_id"))
    @MapKeyColumn(name = "item_id")
    @Column(name = "quantity")
    private Map<Integer, Integer> persistedItems = new HashMap<>();

    @Transient
    protected ConcurrentHashMap<Integer, Integer> items; // itemId -> quantity

    @Embedded
    protected Address shippingAddress; // shipping address
    protected boolean isCompleted; // purchase status
    protected LocalDateTime timeOfCompletion; // time of purchase completion

    protected double price = 0; // total price of the purchase

    /**
     * Constructs a new {@code Purchase} with the specified user ID, store ID, and
     * items.
     * 
     * @param userId  the ID of the user initiating the purchase.
     * @param storeId the ID of the store where the purchase is made.
     * @param items   a map of item IDs to their quantities.
     */
    public Purchase(int purchaseId, int userId, int storeId, Map<Integer, Integer> items, double price,
            Address shippingAddress) {
        this.purchaseId = purchaseId;
        this.userId = userId;
        this.storeId = storeId;
        this.items = new ConcurrentHashMap<>(items);
        this.shippingAddress = shippingAddress;
        this.isCompleted = false;
        this.price = price;
        prePersist();
    }

    /**
     * Constructs a new {@code Purchase} with the specified user ID and store ID.
     * 
     * <p>
     * The items list is initialized as empty, and the purchase is marked as
     * incomplete.
     * 
     * @param userId  the ID of the user initiating the purchase.
     * @param storeId the ID of the store where the purchase is made.
     */
    public Purchase(int purchaseId, int userId, int storeId, Address shippingAddress) {
        this.purchaseId = purchaseId;
        this.userId = userId;
        this.storeId = storeId;
        this.items = new ConcurrentHashMap<>();
        this.shippingAddress = shippingAddress;
        this.isCompleted = false;
        this.price = -1;
    }

    public Purchase() {
        this.purchaseId = 0;
        this.userId = 0;
        this.isCompleted = false;
        this.items = new ConcurrentHashMap<>();
        this.storeId = 0;
        this.shippingAddress = new Address();
        this.price = -1;
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
     * Returns the total price of the purchase.
     * 
     * @return the total price.
     */
    public Address getAddress() {
        return shippingAddress;
    }

    /**
     * Sets the shipping address for the purchase.
     * 
     * @param address the new shipping address.
     */
    public void setAddress(Address address) {
        this.shippingAddress = address;
    }

    /**
     * Returns a map of item IDs to their quantities in the purchase.
     * 
     * @return a {@code HashMap} containing the items and their quantities.
     */
    public Map<Integer, Integer> getItems() {
        return Map.copyOf(items);
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
     * Checks whether the purchase is completed.
     * 
     * @return {@code true} if the purchase is completed, {@code false} otherwise.
     */
    public boolean isCompleted() {
        return isCompleted;
    }

    /**
     * Returns the time of purchase completion.
     * 
     * @return the time of completion, or {@code null} if the purchase is not
     *         completed.
     */
    public LocalDateTime getTimeOfCompletion() {
        return timeOfCompletion;
    }

    /**
     * Adds an item to the purchase or updates its quantity if it already exists.
     * 
     * @param itemId   the ID of the item to add.
     * @param quantity the quantity of the item to add.
     */
    public void addItem(int itemId, int quantity) {
        // merge() ensures the add is atomic and no increments are lost
        items.merge(itemId, quantity, Integer::sum);
    }

    /**
     * Removes a specified quantity of an item from the purchase.
     * 
     * <p>
     * If the quantity to remove is greater than or equal to the current quantity,
     * the item is removed entirely from the purchase.
     * 
     * @param itemId   the ID of the item to remove.
     * @param quantity the quantity of the item to remove.
     */
    public void removeItem(int itemId, int quantity) {
        // computeIfPresent() runs atomically and removes the entry if quantity <= 0
        items.computeIfPresent(itemId, (key, oldVal) -> {
            int newVal = oldVal - quantity;
            return (newVal > 0) ? newVal : null; // if 0 or below, remove
        });
    }

    /**
     * Marks the purchase as completed.
     */
    public Reciept completePurchase() {
        this.isCompleted = true;
        this.timeOfCompletion = LocalDateTime.now();
        return generateReciept();
    }

    /**
     * Cancels the purchase by marking it as incomplete.
     */
    public Reciept cancelPurchase() {
        this.isCompleted = false;
        this.timeOfCompletion = null;
        return generateReciept();
    }

    /**
     * Returns a string representation of the purchase.
     * 
     * @return a string containing the purchase details.
     */
    public Reciept generateReciept() {
        if (!isCompleted)
            return new Reciept(purchaseId, userId, storeId, items, shippingAddress, null, this.price);
        else
            return new Reciept(purchaseId, userId, storeId, items, shippingAddress, timeOfCompletion, this.price);
    }

    @PostLoad
    protected void postLoad() {
        items = new ConcurrentHashMap<>();
        for (Map.Entry<Integer, Integer> entry : persistedItems.entrySet()) {
            items.put(entry.getKey(), entry.getValue());
        }
    }


    @PrePersist
    @PreUpdate
    private void prePersist() {
        // Sync items to persistedItems
        persistedItems.clear();
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            persistedItems.put(entry.getKey(), entry.getValue());
        }
    }
}