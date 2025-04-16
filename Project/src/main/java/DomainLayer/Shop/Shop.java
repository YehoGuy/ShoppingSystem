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

    // Mutable fields: declared volatile for visibility across threads.
    private volatile String purchasePolicy;

    // A thread-safe mapping for discounts.
    // The key represents the item ID.
    // When the key is 0, it acts as a global discount, ensuring that all items in the shop will have at least this discount.
    private final ConcurrentHashMap<Integer, Integer> discounts = new ConcurrentHashMap<>();

    // Thread-safe collections for other shop properties.
    private final List<ShopReview> reviews = new CopyOnWriteArrayList<>();

    // Items: Using a ConcurrentHashMap mapping an item ID to an AtomicInteger quantity.
    private final ConcurrentHashMap<Integer, AtomicInteger> items = new ConcurrentHashMap<>();

    /**
     * Constructor to initialize the shop.
     * The provided discount value will be used as a global discount (key 0).
     *
     * @param id             the shop identifier.
     * @param name           the name of the shop.
     * @param purchasePolicy the shop's purchase policy.
     * @param discount       the global discount for all items in the shop (applied using key 0).
     */
    public Shop(int id, String name, String purchasePolicy, int discount) {
        this.id = id;
        this.name = name;
        this.purchasePolicy = purchasePolicy;
        // Set the global discount using key 0.
        discounts.put(0, discount);
    }

    // ======= Immutable Field Getters =======
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // ======= Getters and Setters for Mutable Fields =======

    public String getPurchasePolicy() {
        return purchasePolicy;
    }

    public synchronized void setPurchasePolicy(String purchasePolicy) {
        this.purchasePolicy = purchasePolicy;
    }

    // ======= Discount Mapping Methods =======

    /**
     * Returns the global discount that is applied to all items.
     * This is the discount mapped to key 0.
     *
     * @return the global discount value; returns 0 if not set.
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
     * For any non-zero itemId, if its specific discount is less than the global discount,
     * the effective discount will be the global discount.
     *
     * @param itemId   the identifier of the item (use 0 for global discount).
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
     * If an item-specific discount exists, the effective discount will be the maximum of that discount
     * and the global discount (key 0). Otherwise, it returns the global discount.
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
        // Ensure that the item gets at least the global discount.
        return Math.max(itemDiscount, globalDiscount);
    }

    // ======= Methods for Reviews =======

    /**
     * Adds a new review to the shop.
     *
     * @param review a Review instance.
     */
    public void addReview(ShopReview review) {
        reviews.add(review);
    }

    /**
     * Convenience method to add a review by providing a rating and review text.
     *
     * @param rating     the rating for the review.
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
     * Computes and returns the average rating of all reviews.
     *
     * @return the average rating; returns 0 if no reviews exist.
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

    // ======= Methods for Items =======

    /**
     * Adds a given quantity of an item.
     * If the item already exists, it increments the quantity atomically.
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
     * If the remaining quantity is zero or below, it removes the item from the shop.
     *
     * @param itemId   the item identifier.
     * @param quantity the quantity to remove.
     */
    public void removeItem(int itemId, int quantity) {
        if(quantity == -1) {
            items.remove(itemId);
            return;
        }
        if(quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        items.computeIfPresent(itemId, (key, existing) -> {
            int newQuantity = existing.addAndGet(-quantity);
            return newQuantity > 0 ? existing : null;
        });
    }

    /**
     * Returns the current quantity of the specified item.
     *
     * @param itemId the item identifier.
     * @return the current quantity; returns 0 if the item is not present.
     */
    public int getItemQuantity(int itemId) {
        AtomicInteger quantity = items.get(itemId);
        return quantity != null ? quantity.get() : 0;
    }

    public List<Integer> getItemIds() {
        return new ArrayList<>(items.keySet());
    }
}
