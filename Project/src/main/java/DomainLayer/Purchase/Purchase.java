package DomainLayer.Purchase;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Purchase {

    protected int userId;                           // initiating user ID
    protected int storeId;                         // store ID
    protected HashMap<Integer, Integer> items;    // itemId -> quantity
    protected Address shippingAddress;           // shipping address
    protected boolean isCompleted;              // purchase status   
    protected LocalDateTime timeOfCompletion;  // time of purchase completion
      

    /**
     * Constructs a new {@code Purchase} with the specified user ID, store ID, and items.
     * 
     * @param userId the ID of the user initiating the purchase.
     * @param storeId the ID of the store where the purchase is made.
     * @param items a map of item IDs to their quantities.
     */
    public Purchase(int userId, int storeId, Map<Integer, Integer> items, Address shippingAddress) {
        this.userId = userId;
        this.storeId = storeId;
        this.items = new HashMap<>(items);
        this.shippingAddress = shippingAddress;
        this.isCompleted = false;
    }

    /**
     * Constructs a new {@code Purchase} with the specified user ID and store ID.
     * 
     * <p>The items list is initialized as empty, and the purchase is marked as incomplete.
     * 
     * @param userId the ID of the user initiating the purchase.
     * @param storeId the ID of the store where the purchase is made.
     */
    public Purchase(int userId, int storeId, Address shippingAddress) {
        this.userId = userId;
        this.storeId = storeId;
        this.items = new HashMap<>();
        this.shippingAddress = shippingAddress;
        this.isCompleted = false;
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
    public HashMap<Integer, Integer> getItems() {
        return items;
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
     * @return the time of completion, or {@code null} if the purchase is not completed.
     */
    public LocalDateTime getTimeOfCompletion() {
        return timeOfCompletion;
    }

    /**
     * Adds an item to the purchase or updates its quantity if it already exists.
     * 
     * @param itemId the ID of the item to add.
     * @param quantity the quantity of the item to add.
     */
    public void addItem(int itemId, int quantity) {
        if (items.containsKey(itemId)) {
            items.put(itemId, items.get(itemId) + quantity);
        } else {
            items.put(itemId, quantity);
        }
    }

    /**
     * Removes a specified quantity of an item from the purchase.
     * 
     * <p>If the quantity to remove is greater than or equal to the current quantity,
     * the item is removed entirely from the purchase.
     * 
     * @param itemId the ID of the item to remove.
     * @param quantity the quantity of the item to remove.
     */
    public void removeItem(int itemId, int quantity) {
        if (items.containsKey(itemId)) {
            int currentQuantity = items.get(itemId);
            if (currentQuantity > quantity) {
                items.put(itemId, currentQuantity - quantity);
            } else {
                items.remove(itemId);
            }
        }
    }

    /**
     * Marks the purchase as completed.
     */
    public void completePurchase() {
        this.isCompleted = true;
        this.timeOfCompletion = LocalDateTime.now();
    }

    /**
     * Cancels the purchase by marking it as incomplete.
     */
    public void cancelPurchase() {
        this.isCompleted = false;
        this.timeOfCompletion = null;
    }
}