package com.example.app.DomainLayer.Shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.app.ApplicationLayer.Purchase.ShippingMethod;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Discount.CategoryDiscount;
import com.example.app.DomainLayer.Shop.Discount.Discount;
import com.example.app.DomainLayer.Shop.Discount.GlobalDiscount;
import com.example.app.DomainLayer.Shop.Discount.Policy;
import com.example.app.DomainLayer.Shop.Discount.PolicyComposite;
import com.example.app.DomainLayer.Shop.Discount.SingleDiscount;
import com.example.app.InfrastructureLayer.WSEPShipping;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * The Shop class representing a shop entity in your system.
 * It supports safe concurrent modifications and multi-threaded access.
 */
@Entity
@Table(name = "shops")
public class Shop {

    // Immutable fields (set once at construction).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shop_id")
    private List<Discount> discounts;

    // A thread-safe list to manage shop reviews.
    @ElementCollection
    private List<ShopReview> reviews;

    // Items: mapping from item ID to its quantity.
    @ElementCollection
    @CollectionTable(name = "shop_items", joinColumns = @JoinColumn(name = "shop_id"))
    @MapKeyColumn(name = "item_id")
    @Column(name = "quantity")
    private Map<Integer, Integer> persistedItems = new HashMap<>();

    @Transient
    private Map<Integer, AtomicInteger> items;

    // Prices: mapping from item ID to its price.
    @ElementCollection
    @CollectionTable(name = "shop_item_prices", joinColumns = @JoinColumn(name = "shop_id"))
    @MapKeyColumn(name = "item_id")
    @Column(name = "price")
    private Map<Integer, Integer> persistedItemsPrices = new HashMap<>();

    @Transient
    private Map<Integer, AtomicInteger> itemsPrices;

    @Transient
    private Map<Integer, Object> itemAcquireLocks;

    @Transient
    private ShippingMethod shippingMethod;

    private String shippingMethodName;

    // ===== Fields for Purchase Policy =====
    @Transient
    private Policy policytemp;

    @Transient
    private PolicyComposite policyComposite;

    private boolean isClosed = false;

    /**
     * Constructor to initialize the shop.
     * The provided discount value will be used as a global discount (mapped with
     * key 0).
     *
     * @param id             the shop identifier.
     * @param name           the name of the shop.
     * @param purchasePolicy the shop's purchase policy.
     */
    public Shop(int id, String name, ShippingMethod shippingMethod) {
        this.id = id;
        this.name = name;
        this.discounts = new CopyOnWriteArrayList<>();
        this.reviews = new CopyOnWriteArrayList<>();
        this.items = new ConcurrentHashMap<>();
        this.itemsPrices = new ConcurrentHashMap<>();
        this.itemAcquireLocks = new ConcurrentHashMap<>();
        this.shippingMethod = shippingMethod;
        this.shippingMethodName = shippingMethod == null ? "" : shippingMethod.getClass().getSimpleName();
        this.policytemp = null;
        this.policyComposite = null;
        this.persistedItems = new HashMap<>();
        this.persistedItemsPrices = new HashMap<>();
    }

    public Shop() {
        // Default constructor for JPA
        this.id = 0; // This will be set by the database
        this.name = "";
        this.discounts = new CopyOnWriteArrayList<>();
        this.reviews = new CopyOnWriteArrayList<>();
        this.items = new ConcurrentHashMap<>();
        this.itemsPrices = new ConcurrentHashMap<>();
        this.itemAcquireLocks = new ConcurrentHashMap<>();
        this.shippingMethod = new WSEPShipping();
        this.shippingMethodName = "";
        this.policytemp = null;
        this.policyComposite = null;
        this.persistedItems = new HashMap<>();
        this.persistedItemsPrices = new HashMap<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setShippingMethod(ShippingMethod shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    // ===== Getters =====
    /**
     * Returns an unmodifiable view of the purchase policies.
     * Currently for tests
     * 
     * @return the list of purchase policies.
     */
    public List<PurchasePolicy> getPurchasePolicies() {
        return null;
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
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
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
            throw new IllegalArgumentException("Quantity must be positive");
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
            throw new IllegalArgumentException("Item not found: " + itemId);
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
            throw new IllegalArgumentException("Quantity must be positive");
        }
        AtomicInteger currentQty = items.get(itemId);
        if (currentQty != null) {
            currentQty.addAndGet(-quantity);
            if (currentQty.get() <= 0) {
                items.remove(itemId);
            }
        } else {
            throw new IllegalArgumentException("Item not found: " + itemId);
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
            throw new IllegalArgumentException("Price must be non-negative");
        }
        if (!items.containsKey(itemId)) {
            throw new IllegalArgumentException("Item not found: " + itemId);
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
     * The total price is calculated as the sum of the prices of each item
     * multiplied by its quantity.
     *
     * @param items a map where the key is the item ID and the value is the
     *              quantity.
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

    private double applyDiscount(Map<Integer, Integer> items, Map<Integer, ItemCategory> itemsCat) {
        Map<Integer, Double> itemsDiscountedPrices = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            int itemId = entry.getKey();
            double price = itemsPrices.get(itemId).get();
            itemsDiscountedPrices.put(itemId, price);
        }

        for (Discount discount : discounts) {
            if (discount.isDouble()) {
                itemsDiscountedPrices = discount.applyDiscounts(items, itemsPrices, itemsDiscountedPrices, itemsCat);
            }
        }

        // is not double discount
        for (Discount discount : discounts) {
            if (!discount.isDouble()) {
                itemsDiscountedPrices = discount.applyDiscounts(items, itemsPrices, itemsDiscountedPrices, itemsCat);
            }
        }

        // calculate the total price after applying discounts
        double totalPrice = 0;
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            int itemId = entry.getKey();
            int quantity = entry.getValue();
            double discountedPrice = itemsDiscountedPrices.get(itemId);
            totalPrice += discountedPrice * quantity;
        }

        return totalPrice;
    }

    /**
     * Applies a global percentage discount to the total price of any purchase.
     *
     * @param percentage the discount percentage (0–100)
     */
    public synchronized void setGlobalDiscount(int percentage, boolean isDouble) {
        GlobalDiscount discount;
        if (policyComposite == null)
            discount = new GlobalDiscount(percentage, policytemp, isDouble);
        else
            discount = new GlobalDiscount(percentage, policyComposite, isDouble);

        this.policyComposite = null;
        this.policytemp = null;
        // delete previous global discount - if exists
        removeGlobalDiscount();
        discounts.add(discount);
    }

    /**
     * Removes a global discount from the shop.
     * This method iterates through all discounts and removes the one that matches
     * the given percentage.
     *
     * @param percentage the discount percentage to remove (0–100).
     */
    public void removeGlobalDiscount() {
        for (int i = 0; i < discounts.size(); i++) {
            if (discounts.get(i) instanceof GlobalDiscount) {
                discounts.remove(i);
                return;
            }
        }
    }

    /**
     * Applies a percentage discount to a single item.
     *
     * @param itemId     the ID of the item to discount.
     * @param percentage the discount percentage (0–100).
     */
    public synchronized void setDiscountForItem(int itemId, int percentage, boolean isDouble) {
        SingleDiscount discount;
        if (policyComposite == null)
            discount = new SingleDiscount(itemId, percentage, policytemp, isDouble);
        else
            discount = new SingleDiscount(itemId, percentage, policyComposite, isDouble);

        this.policyComposite = null;
        this.policytemp = null;
        // delete previous discount for this item - if exists
        removeDiscountForItem(itemId);
        discounts.add(discount);
    }

    /**
     * Removes a discount for a single item.
     *
     * @param itemId the ID of the item to remove the discount from.
     */
    public void removeDiscountForItem(int itemId) {
        for (int i = 0; i < discounts.size(); i++) {
            if (discounts.get(i) instanceof SingleDiscount
                    && ((SingleDiscount) discounts.get(i)).getItemId() == itemId) {
                discounts.remove(i);
                return;
            }
        }
    }

    public synchronized void setCategoryDiscount(ItemCategory category, int percentage, boolean isDouble) {
        CategoryDiscount discount;
        if (policyComposite == null)
            discount = new CategoryDiscount(category, percentage, policytemp, isDouble);
        else
            discount = new CategoryDiscount(category, percentage, policyComposite, isDouble);

        this.policyComposite = null;
        this.policytemp = null;
        // delete previous discount for this category - if exists
        removeCategoryDiscount(category);
        discounts.add(discount);
    }

    public void removeCategoryDiscount(ItemCategory category) {
        for (int i = 0; i < discounts.size(); i++) {
            if (discounts.get(i) instanceof CategoryDiscount
                    && ((CategoryDiscount) discounts.get(i)).getCategory() == category) {
                discounts.remove(i);
                return;
            }
        }
    }

    // ===== Purchase Method =====

    /**
     * Processes a purchase of items:
     * 1) Checks purchase policies up front
     * 2) Locks each item & deducts stock
     * 3) Computes total, applies discounts, and returns the final amount
     * If anything fails, rolls back any stock modifications.
     *
     * @param purchaseList map of itemId→quantity to purchase
     * @return the total price after discounts
     */
    public double purchaseItems(Map<Integer, Integer> purchaseList, Map<Integer, ItemCategory> itemsCategory) {
        // 1) policy check up-front
        // if (!checkPolicys(purchaseList)) {
        // throw new IllegalStateException("Purchase policy violation");
        // }

        // remember for rollback
        Map<Integer, Integer> originalStock = new HashMap<>();

        try {
            // 2) lock & deduct each item
            for (Map.Entry<Integer, Integer> e : purchaseList.entrySet()) {
                int itemId = e.getKey();
                int qty = e.getValue();

                Object lock = itemAcquireLocks.computeIfAbsent(itemId, k -> new Object());
                synchronized (lock) {
                    AtomicInteger availAtom = items.get(itemId);
                    int avail = availAtom != null ? availAtom.get() : 0;
                    if (avail < qty) {
                        throw new IllegalArgumentException("Insufficient stock for item " + itemId);
                    }
                    originalStock.put(itemId, avail);
                    availAtom.addAndGet(-qty);
                }
            }

            // 3) compute total + apply discounts
            double finalTotal = applyDiscount(purchaseList, itemsCategory);

            return finalTotal;

        } catch (RuntimeException ex) {
            // rollback only those we already touched
            for (Map.Entry<Integer, Integer> r : originalStock.entrySet()) {
                int itemId = r.getKey();
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
            int qty = e.getValue();

            Object lock = itemAcquireLocks.computeIfAbsent(itemId, k -> new Object());
            synchronized (lock) {
                AtomicInteger availAtom = items.get(itemId);
                if (availAtom != null) {
                    availAtom.addAndGet(qty);
                }
            }
        }
    }

    public synchronized void setDiscountPolicy(Policy policy) {
        // clear any previous-in-progress policy
        this.policytemp = null;
        this.policyComposite = null;

        // attach either as a single leaf or a composite
        if (policy instanceof PolicyComposite) {
            this.policyComposite = (PolicyComposite) policy;
        } else {
            this.policytemp = policy;
        }
    }

    public Map<Integer, Double> getItemPrices() {
        Map<Integer, Double> prices = new HashMap<>();
        for (Map.Entry<Integer, AtomicInteger> entry : itemsPrices.entrySet()) {
            int itemId = entry.getKey();
            double price = entry.getValue().get();
            prices.put(itemId, price);
        }
        return prices;
    }

    public Map<Integer, Integer> getItemQuantities() {
        Map<Integer, Integer> quantities = new HashMap<>();
        for (Map.Entry<Integer, AtomicInteger> entry : items.entrySet()) {
            int itemId = entry.getKey();
            int quantity = entry.getValue().get();
            quantities.put(itemId, quantity);
        }
        return quantities;
    }

    public List<Integer> getItems() {
        List<Integer> itemsList = new ArrayList<>();
        for (Map.Entry<Integer, AtomicInteger> entry : items.entrySet()) {
            int itemId = entry.getKey();
            itemsList.add(itemId);
        }
        return itemsList;
    }

    public List<Discount> getDiscounts() {
        return Collections.unmodifiableList(discounts);
    }

    public List<Policy> getPolicies() {
        List<Policy> policies = new ArrayList<>();
        if (policyComposite != null) {
            policies.add(policyComposite);
        }
        if (policytemp != null) {
            policies.add(policytemp);
        }
        return policies;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    public boolean isClosed() {
        return isClosed;
    }

    @PostLoad
    private void postLoad() {
        // Sync persistedItems to items
        for (Map.Entry<Integer, Integer> entry : persistedItems.entrySet()) {
            items.put(entry.getKey(), new AtomicInteger(entry.getValue()));
        }
        // Sync persistedPrices to itemsPrices
        for (Map.Entry<Integer, Integer> entry : persistedItemsPrices.entrySet()) {
            itemsPrices.put(entry.getKey(), new AtomicInteger(entry.getValue()));
        }
    }

    @PrePersist
    @PreUpdate
    private void prePersist() {
        // Sync items to persistedItems
        persistedItems.clear();
        for (Map.Entry<Integer, AtomicInteger> entry : items.entrySet()) {
            persistedItems.put(entry.getKey(), entry.getValue().get());
        }
        // Sync itemsPrices to persistedPrices
        persistedItemsPrices.clear();
        for (Map.Entry<Integer, AtomicInteger> entry : itemsPrices.entrySet()) {
            persistedItemsPrices.put(entry.getKey(), entry.getValue().get());
        }
    }

}
