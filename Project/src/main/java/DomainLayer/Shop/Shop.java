package DomainLayer.Shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import ApplicationLayer.Purchase.ShippingMethod;

/**
 * The Shop class representing a shop entity in your system.
 * It supports safe concurrent modifications and multi-threaded access.
 */
public class Shop {

    // Immutable fields (set once at construction).
    private final int id;
    private final String name;

    private final List<PurchasePolicy> purchasePolicys;

    private final List<Discount> discounts;

    // A thread-safe list to manage shop reviews.
    private final List<ShopReview> reviews;

    // Items: mapping from item ID to its quantity.
    private final ConcurrentHashMap<Integer, Integer> items;

    // Prices: mapping from item ID to its price.
    private final ConcurrentHashMap<Integer, Integer> itemsPrices;

    private final ConcurrentHashMap<Integer, Object> itemAcquireLocks;

    private ShippingMethod shippingMethod;

    /**
     * Constructor to initialize the shop.
     * The provided discount value will be used as a global discount (mapped with key 0).
     *
     * @param id             the shop identifier.
     * @param name           the name of the shop.
     * @param purchasePolicy the shop's purchase policy.
     * @param discount       the global discount for all items in the shop.
     */
    public Shop(int id, String name, ShippingMethod shippingMethod) {
        this.id = id;
        this.name = name;
        this.purchasePolicys = new ArrayList<>();
        this.discounts = new ArrayList<>();
        this.reviews = new CopyOnWriteArrayList<>();
        this.items = new ConcurrentHashMap<>();
        this.itemsPrices = new ConcurrentHashMap<>();
        this.shippingMethod = shippingMethod;
    }

    // ===== Immutable Field Getters =====

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // ===== setters =====

    public void setShippingMethod(ShippingMethod shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public void addDiscount(Discount discount) {
        discounts.add(discount);
    }
    public void addPurchasePolicy(PurchasePolicy purchasePolicy) {
        purchasePolicys.add(purchasePolicy);
    }
    public void removePurchasePolicy(PurchasePolicy purchasePolicy) {
        purchasePolicys.remove(purchasePolicy);
    }
    public void removeDiscount(Discount discount) {
        discounts.remove(discount);
    }

    

    // ===== Methods for Reviews =====

    /**
     * Adds a review to the shop.
     *
     * @param review a ShopReview instance.
     */
    public void addReview(ShopReview review) {
        reviews.add(review);
    }

    /**
     * Convenience method to add a review by providing a rating and review text.
     *
     * @param rating     the review rating.
     * @param reviewText the review text.
     */
    public void addReview(int userId, int rating, String reviewText) {
        reviews.add(new ShopReview(userId, rating, reviewText));
    }

    /**
     * Returns an unmodifiable view of the shop reviews.
     *
     * @return the list of reviews.
     */
    public List<ShopReview> getReviews() {
        return Collections.unmodifiableList(reviews);
    }

    /**
     * Computes and returns the average rating from all reviews.
     *
     * @return the average rating, or 0.0 if no reviews exist.
     */
    public double getAverageRating() {
        int count = reviews.size();
        if (count == 0) {
            return 0.0;
        }
        int total = 0;
        for (ShopReview r : reviews) {
            total += r.getRating();
        }
        return (double) total / count;
    }

    // ===== Methods for Items =====

    /**
     * Adds a given quantity of an item.
     * If the item exists, increments its quantity atomically.
     *
     * @param itemId   the item identifier.
     * @param quantity the quantity to add.
     */
    public void addItem(int itemId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        items.merge(itemId, new AtomicInteger(quantity), (existing, value) -> {
            existing.addAndGet(quantity);
            return existing;
        });
    }

    /**
     * Removes a given quantity of an item.
     * If quantity is -1, or if the quantity reaches zero or below,
     * the item is completely removed from both the quantity map and the price map.
     *
     * @param itemId   the item identifier.
     * @param quantity the quantity to remove (use -1 to remove completely).
     */
    public void removeItem(int itemId, int quantity) {
        if (quantity == -1) {
            // Remove the item completely from both maps.
            items.remove(itemId);
            itemsPrices.remove(itemId);
            return;
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        items.computeIfPresent(itemId, (key, existing) -> {
            int newQuantity = existing.addAndGet(-quantity);
            if (newQuantity <= 0) {
                // Remove price information as well, since the item is fully removed.
                itemsPrices.remove(itemId);
                return null;
            }
            return existing;
        });
    }

    /**
     * Returns the current quantity of the specified item.
     *
     * @param itemId the item identifier.
     * @return the quantity, or 0 if the item does not exist.
     */
    public int getItemQuantity(int itemId) {
        AtomicInteger quantity = items.get(itemId);
        return quantity != null ? quantity.get() : 0;
    }

    /**
     * Returns a list of all item IDs in the shop.
     *
     * @return a list of item IDs.
     */
    public List<Integer> getItemIds() {
        return new ArrayList<>(items.keySet());
    }

    // ===== Methods for Item Prices =====

    /**
     * Updates the price for a given item.
     * If no price exists for the item, a new price entry is added.
     *
     * @param itemId the item identifier.
     * @param price  the new price (must be non-negative).
     */
    public void updateItemPrice(int itemId, int price) {
        if (price < 0) {
            throw new IllegalArgumentException("Price must be non-negative");
        }
        itemsPrices.compute(itemId, (key, existing) -> {
            if (existing == null) {
                return new AtomicInteger(price);
            } else {
                existing.set(price);
                return existing;
            }
        });
    }

    public ShippingMethod getShippingMethod() {
        return shippingMethod;
    }

    /**
     * Retrieves the current price for the specified item.
     *
     * @param itemId the item identifier.
     * @return the price, or 0 if no price is set.
     */
    public int getItemPrice(int itemId) {
        return itemsPrices.getOrDefault(itemId, 0);
    }

    /**
     * Returns the total price for a given set of items.
     * The total price is calculated as the sum of the prices of each item multiplied by its quantity.
     *
     * @param items a map where the key is the item ID and the value is the quantity.
     * @return the total price.
     */
    public int getTotalPrice(Map<Integer, Integer> items) {
        int total = 0;
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            int itemId = entry.getKey();
            int quantity = entry.getValue();
            int price = getItemPrice(itemId);
            total += price * quantity;
        }
        return total;
    }

    /**
     * Checks if the purchase is valid according to the shop's purchase policies.
     * This method iterates through all purchase policies and checks if the purchase is valid.
     *
     * @param items a map where the key is the item ID and the value is the quantity.
     * @return {@code true} if the purchase is valid, {@code false} otherwise.
     */
    private boolean checkPolicys(Map<Integer,Integer> items){
        for (PurchasePolicy purchasePolicy : purchasePolicys) {
            if (!purchasePolicy.isValidPurchase(items, getTotalPrice(items))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the purchase is valid according to the shop's discounts.
     * This method iterates through all discounts and checks if the purchase is valid.
     *
     * @param items a map where the key is the item ID and the value is the quantity.
     * @return {@code true} if the purchase is valid, {@code false} otherwise.
     */
    private boolean checkDiscounts(Map<Integer,Integer> items){
        for (Discount discount : discounts) {
            if (!discount.isSupportedInMultiDiscounts()) {
                return false;
            }
        }
        return true;
    }

    // ===== Purchase Method =====

    /**
     * Processes a purchase of items.
     * This method checks the purchase policy and applies any applicable discounts.
     *
     * @param items a map where the key is the item ID and the value is the quantity.
     */
    public void PurchaseItems(Map<Integer, Integer> items){
        
    }
}
