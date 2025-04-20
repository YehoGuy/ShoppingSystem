package main.DomainLayer.Shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Shop class representing a shop entity in your system.
 * It supports safe concurrent modifications and multi-threaded access.
 */
public class Shop {

    // Immutable fields (set once at construction).
    private final int id;
    private final String name;

    // Mutable field: declared volatile for visibility across threads.
    private volatile String purchasePolicy;

    // A thread-safe mapping for discounts.
    // Key 0 acts as the global discount.
    private final ConcurrentHashMap<Integer, Integer> discounts = new ConcurrentHashMap<>();

    // A thread-safe list to manage shop reviews.
    private final List<ShopReview> reviews = new CopyOnWriteArrayList<>();

    // Items: mapping from item ID to its quantity.
    private final ConcurrentHashMap<Integer, AtomicInteger> items = new ConcurrentHashMap<>();

    // Prices: mapping from item ID to its price.
    private final ConcurrentHashMap<Integer, AtomicInteger> itemsPrices = new ConcurrentHashMap<>();

    /**
     * Constructor to initialize the shop.
     * The provided discount value will be used as a global discount (mapped with key 0).
     *
     * @param id             the shop identifier.
     * @param name           the name of the shop.
     * @param purchasePolicy the shop's purchase policy.
     * @param discount       the global discount for all items in the shop.
     */
    public Shop(int id, String name, String purchasePolicy, int discount) {
        this.id = id;
        this.name = name;
        this.purchasePolicy = purchasePolicy;
        // Set the global discount using key 0.
        discounts.put(0, discount);
    }

    // ===== Immutable Field Getters =====

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // ===== Getters and Setters for Mutable Fields =====

    public String getPurchasePolicy() {
        return purchasePolicy;
    }

    public synchronized void setPurchasePolicy(String purchasePolicy) {
        this.purchasePolicy = purchasePolicy;
    }

    // ===== Discount Mapping Methods =====

    /**
     * Returns the global discount (key 0) applied to all items.
     *
     * @return the global discount, or 0 if not set.
     */
    public int getGlobalDiscount() {
        return discounts.getOrDefault(0, 0);
    }

    /**
     * Sets the global discount for the shop (applied to all items).
     *
     * @param discount the global discount value.
     */
    public synchronized void setGlobalDiscount(int discount) {
        discounts.put(0, discount);
    }

    /**
     * Sets a discount for a specific item.
     * For non-zero itemId, the discount is applied directly.
     * Use itemId 0 to update the global discount.
     *
     * @param itemId   the identifier of the item (0 for global discount).
     * @param discount the discount value for the item.
     */
    public synchronized void setDiscountForItem(int itemId, int discount) {
        if (itemId == 0) {
            setGlobalDiscount(discount);
        } else {
            discounts.put(itemId, discount);
        }
    }

    /**
     * Retrieves the effective discount for a given item.
     * Returns the maximum of the item-specific discount and the global discount.
     *
     * @param itemId the identifier of the item.
     * @return the effective discount for the item.
     */
    public int getDiscountForItem(int itemId) {
        int globalDiscount = discounts.getOrDefault(0, 0);
        if (itemId == 0) {
            return globalDiscount;
        }
        int itemDiscount = discounts.getOrDefault(itemId, globalDiscount);
        return Math.max(itemDiscount, globalDiscount);
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
    public void addReview(int rating, String reviewText) {
        reviews.add(new ShopReview(rating, reviewText));
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

    /**
     * Retrieves the current price for the specified item.
     *
     * @param itemId the item identifier.
     * @return the price, or 0 if no price is set.
     */
    public int getItemPrice(int itemId) {
        AtomicInteger price = itemsPrices.get(itemId);
        return price != null ? price.get() : 0;
    }

    @Override
    public String toString() {
        return "Shop{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", purchasePolicy='" + purchasePolicy + '\'' +
                ", globalDiscount=" + getGlobalDiscount() +
                ", averageRating=" + getAverageRating() +
                ", items=" + items +
                ", itemsPrices=" + itemsPrices +
                '}';
    }
}
