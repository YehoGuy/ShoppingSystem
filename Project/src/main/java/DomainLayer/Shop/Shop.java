package DomainLayer.Shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import ApplicationLayer.OurArg;
import ApplicationLayer.Purchase.ShippingMethod;

/**
 * The Shop class representing a shop entity in your system.
 * It supports safe concurrent modifications and multi-threaded access.
 */
public class Shop {

    // Immutable fields (set once at construction).
    private final int id;
    private final String name;

    private final List<PurchasePolicy> purchasePolicies;

    /**
     * Thread-safe list of discounts. CopyOnWriteArrayList allows
     * lock-free iteration even while another thread is adding/removing discounts.
     */
    private final CopyOnWriteArrayList<Discount> discounts;

    // A thread-safe list to manage shop reviews.
    private final List<ShopReview> reviews;

    // Items: mapping from item ID to its quantity.
    private final ConcurrentHashMap<Integer, AtomicInteger> items;

    // Prices: mapping from item ID to its price.
    private final ConcurrentHashMap<Integer, AtomicInteger> itemsPrices;

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
        this.purchasePolicies = new ArrayList<>();
        this.discounts = new CopyOnWriteArrayList<>();
        this.reviews = new CopyOnWriteArrayList<>();
        this.items = new ConcurrentHashMap<>();
        this.itemsPrices = new ConcurrentHashMap<>();
        this.itemAcquireLocks = new ConcurrentHashMap<>();
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

    public void addPurchasePolicy(PurchasePolicy purchasePolicy) {
        purchasePolicies.add(purchasePolicy);
    }

    public void removePurchasePolicy(PurchasePolicy purchasePolicy) {
        purchasePolicies.remove(purchasePolicy);
    }

    // ===== Getters =====
    /**
     * Returns an unmodifiable view of the purchase policies.
     * Currently for tests
     * @return the list of purchase policies.
     */
    public List<PurchasePolicy> getPurchasePolicies() {
        return Collections.unmodifiableList(purchasePolicies);
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
        if(rating < 1 || rating > 5) {
            throw new OurArg("Rating must be between 1 and 5");
        }
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
            return -1.0;
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
            throw new OurArg("Quantity must be positive");
        }
        items.merge(itemId, new AtomicInteger(quantity), (existing, value) -> {
            existing.addAndGet(quantity);
            return existing;
        });
    }

    /**
     * Completely removes an item (and its price) from the shop,
     * using a per‐item lock to avoid races, and then
     * removes that lock from the lock‐map.
     *
     * @param itemId the item identifier to remove
     */
    public void removeItemFromShop(int itemId) {
        // 1) Ensure item exists
        if (!items.containsKey(itemId)) {
            throw new OurArg("Item not found: " + itemId);
        }

        // 2) Grab (or create) the per‐item lock object
        Object lock = itemAcquireLocks.computeIfAbsent(itemId, k -> new Object());

        // 3) Under that lock, remove both quantity and price
        synchronized (lock) {
            items.remove(itemId);
            itemsPrices.remove(itemId);
        }

        // 4) Finally, drop the lock entry itself
        itemAcquireLocks.remove(itemId);
    }

    public void removeItemQuantity(int itemId, int quantity) {
        if (quantity <= 0) {
            throw new OurArg("Quantity must be positive");
        }
        AtomicInteger currentQty = items.get(itemId);
        if (currentQty != null) {
            currentQty.addAndGet(-quantity);
            if (currentQty.get() <= 0) {
                items.remove(itemId);
            }
        } else {
            throw new OurArg("Item not found: " + itemId);
        }
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
            throw new OurArg("Price must be non-negative");
        }
        if (!items.containsKey(itemId)) {
            throw new OurArg("Item not found: " + itemId);
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
        AtomicInteger price = itemsPrices.get(itemId);
        return price != null ? price.get() : 0;
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
    public boolean checkPolicys(Map<Integer,Integer> items){
        for (PurchasePolicy purchasePolicy : purchasePolicies) {
            if (!purchasePolicy.isValidPurchase(items, getTotalPrice(items))) {
                return false;
            }
        }
        return true;
    }


    private double applyDiscount(Map<Integer,Integer> items){
        Map<Integer, Integer> itemsDiscountedPrices = new HashMap<>();
        for(Map.Entry<Integer, Integer> entry : items.entrySet()){
            int itemId = entry.getKey();
            int price = getItemPrice(itemId);
            itemsDiscountedPrices.put(itemId, price);
        }

        for(Discount discount : discounts){
            itemsDiscountedPrices = discount.applyDiscounts(items, itemsPrices, itemsDiscountedPrices);
        }
        
        // calculate the total price after applying discounts
        double totalPrice = 0;
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            int itemId = entry.getKey();
            int quantity = entry.getValue();
            int discountedPrice = itemsDiscountedPrices.get(itemId);
            totalPrice += discountedPrice * quantity;
        }

        return totalPrice;
    }

    /**
     * Applies a global percentage discount to the total price of any purchase.
     *
     * @param percentage the discount percentage (0–100)
     */
    public void setGlobalDiscount(int percentage) {
        // try to update existing
        for (Discount d : discounts) {
            if (d.getItemId() == null) {
                d.setPercentage(percentage);
                return;
            }
        }
        // otherwise add new
        discounts.add(new SingleDiscount(this.id, null,/* price unused */0, percentage));
    }

    /**
     * Removes a global discount from the shop.
     * This method iterates through all discounts and removes the one that matches the given percentage.
     *
     * @param percentage the discount percentage to remove (0–100).
     */ 
    public void removeGlobalDiscount() {
        discounts.removeIf(d ->
            d.getItemId() == null
        );
    }
    
        /**
     * Applies a percentage discount to a single item.
     *
     * @param itemId     the ID of the item to discount.
     * @param percentage the discount percentage (0–100).
     */
    public void setDiscountForItem(int itemId, int percentage) {
        for (Discount d : discounts) {
            if (d.getItemId() != null
             && d.getItemId().equals(itemId)) {
                d.setPercentage(percentage);
                return;
            }
        }
        discounts.add(new SingleDiscount(this.id, itemId, this.getItemPrice(itemId), percentage));
    }

    /**
     * Removes a discount for a single item.
     *
     * @param itemId the ID of the item to remove the discount from.
     */
    public void removeDiscountForItem(int itemId) {
        discounts.removeIf(d ->
            d.getItemId() != null &&
            d.getItemId().equals(itemId)
        );
    }

    /**
     * Adds a bundle discount to the shop.
     * A bundle discount applies a percentage off the entire purchase
     * if all specified bundle item IDs are present in the cart.
     *
     * @param bundleItems  a map of item IDs to quantities required for the discount.
     * @param percentage   the discount percentage (0–100).
     */
    public void addBundleDiscount(Map<Integer, Integer> bundleItems, int percentage){  // bundleItems is a map of itemId to quantity
        discounts.add(new BundleDiscount(bundleItems, percentage));
    }

    /**
     * Removes a bundle discount from the shop.
     * This method iterates through all discounts and removes the one that matches the given bundle items.
     *
     * @param bundleItems a map of item IDs to quantities required for the discount.
     */
    public void removeBundleDiscount(Map<Integer, Integer> bundleItems, int percentage) {
        //TODO-V2: Implement this method to remove the bundle discount from the shop.
    }
    
    // ===== Purchase Method =====
     
    /**
     * Processes a purchase of items:
     *   1) Checks purchase policies up front
     *   2) Locks each item & deducts stock
     *   3) Computes total, applies discounts, and returns the final amount
     * If anything fails, rolls back any stock modifications.
     *
     * @param purchaseList map of itemId→quantity to purchase
     * @return the total price after discounts
     */
    public double purchaseItems(Map<Integer, Integer> purchaseList) {
        // 1) policy check up-front
        // if (!checkPolicys(purchaseList)) {
        //     throw new IllegalStateException("Purchase policy violation");
        // }

        // remember for rollback
        Map<Integer, Integer> originalStock = new HashMap<>();

        try {
            // 2) lock & deduct each item
            for (Map.Entry<Integer, Integer> e : purchaseList.entrySet()) {
                int itemId = e.getKey();
                int qty    = e.getValue();

                Object lock = itemAcquireLocks.computeIfAbsent(itemId, k -> new Object());
                synchronized (lock) {
                    AtomicInteger availAtom = items.get(itemId);
                    int avail = availAtom != null ? availAtom.get() : 0;
                    if (avail < qty) {
                        throw new OurArg("Insufficient stock for item " + itemId);
                    }
                    originalStock.put(itemId, avail);
                    availAtom.addAndGet(-qty);
                }
            }

            // 3) compute total + apply discounts
            double finalTotal = applyDiscount(purchaseList);

            return finalTotal;

        } catch (RuntimeException ex) {
            // rollback only those we already touched
            for (Map.Entry<Integer, Integer> r : originalStock.entrySet()) {
                int itemId  = r.getKey();
                int prevQty = r.getValue();
                Object lock = itemAcquireLocks.get(itemId);
                synchronized (lock) {
                    items.get(itemId).set(prevQty);
                }
            }
            throw ex;
        }
    }

    public void rollBackPurchase(Map<Integer, Integer> purchaseList) {
        for (Map.Entry<Integer, Integer> e : purchaseList.entrySet()) {
            int itemId = e.getKey();
            int qty    = e.getValue();

            Object lock = itemAcquireLocks.computeIfAbsent(itemId, k -> new Object());
            synchronized (lock) {
                AtomicInteger availAtom = items.get(itemId);
                if (availAtom != null) {
                    availAtom.addAndGet(qty);
                }
            }
        }
    }
}
