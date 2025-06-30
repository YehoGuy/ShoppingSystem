package com.example.app.DomainLayer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.ArrayList;
import com.example.app.ApplicationLayer.OurRuntime;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Embeddable
public class ShoppingCart {
    // JPA-compatible collections for persistence
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shopping_cart_items")
    private List<CartItem> persistentItems = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shopping_cart_bids")
    @Column(name = "bid_product_id")
    private List<String> persistentBids = new ArrayList<>(); // Format: "shopId-productId"

    // Transient collections for runtime performance
    @Transient
    final private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> items; // shopID, (productID,
                                                                                         // quantity)
    // every entry in the HashMap is a basket.
    @Transient
    final private ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> bids; // shopID, productID

    public ShoppingCart() {
        this.items = new ConcurrentHashMap<>();
        this.bids = new ConcurrentHashMap<>();
        this.persistentItems = new ArrayList<>();
        this.persistentBids = new ArrayList<>();
        // Load from persistent collections if they exist
        loadFromPersistentCollections();
    }

    /**
     * Load data from persistent collections into transient runtime collections
     */
    public void loadFromPersistentCollections() {
        // Load items
        for (CartItem item : persistentItems) {
            items.putIfAbsent(item.getShopId(), new ConcurrentHashMap<>());
            items.get(item.getShopId()).put(item.getProductId(), item.getQuantity());
        }

        // Load bids
        for (String bidKey : persistentBids) {
            String[] parts = bidKey.split("-");
            if (parts.length == 2) {
                try {
                    Integer shopId = Integer.valueOf(parts[0]);
                    Integer productId = Integer.valueOf(parts[1]);
                    bids.putIfAbsent(shopId, new CopyOnWriteArrayList<>());
                    if (!bids.get(shopId).contains(productId)) {
                        bids.get(shopId).add(productId);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
    }

    /**
     * Sync transient collections to persistent collections for database storage
     */
    public void syncToPersistentCollections() {
        // Clear and rebuild persistent items
        persistentItems.clear();
        for (Map.Entry<Integer, ConcurrentHashMap<Integer, Integer>> shopEntry : items.entrySet()) {
            Integer shopId = shopEntry.getKey();
            for (Map.Entry<Integer, Integer> itemEntry : shopEntry.getValue().entrySet()) {
                persistentItems.add(new CartItem(shopId, itemEntry.getKey(), itemEntry.getValue()));
            }
        }

        // Clear and rebuild persistent bids
        persistentBids.clear();
        for (Map.Entry<Integer, CopyOnWriteArrayList<Integer>> bidEntry : bids.entrySet()) {
            Integer shopId = bidEntry.getKey();
            for (Integer productId : bidEntry.getValue()) {
                persistentBids.add(shopId + "-" + productId);
            }
        }
    }

    public void clearCart() {
        items.clear();
        bids.clear();
        syncToPersistentCollections();
    }

    public void addBasket(int shopId) {
        items.putIfAbsent(shopId, new ConcurrentHashMap<>());
        syncToPersistentCollections();
    }

    public void removeBasket(int shopId) {
        items.remove(shopId);
        bids.remove(shopId);
        syncToPersistentCollections();
    }

    public ConcurrentHashMap<Integer, Integer> getBasket(int shopId) {
        return items.getOrDefault(shopId, new ConcurrentHashMap<>());
    }

    public void addItem(int shopId, int productId, int quantity) {
        items.putIfAbsent(shopId, new ConcurrentHashMap<>());
        items.get(shopId).merge(productId, quantity, Integer::sum); // Thread-safe add/update
        syncToPersistentCollections();
    }

    public void removeItem(int shopId, int productId) {
        if (!hasItemOfShop(shopId, productId))
            throw new OurRuntime("item or shop not in cart. ", shopId, productId);
        ConcurrentHashMap<Integer, Integer> shopItems = items.get(shopId);
        if (shopItems != null) {
            shopItems.remove(productId);
        }
        // Also remove from bids if present
        CopyOnWriteArrayList<Integer> shopBids = bids.get(shopId);
        if (shopBids != null) {
            shopBids.remove(Integer.valueOf(productId));
        }
        syncToPersistentCollections();
    }

    public void setBasket(int shopId, HashMap<Integer, Integer> basket) {
        ConcurrentHashMap<Integer, Integer> concurrentBasket = new ConcurrentHashMap<>(basket);
        items.put(shopId, concurrentBasket);
        syncToPersistentCollections();
    }

    public void updateProduct(int shopId, int productId, int quantity) {
        if (bids.containsKey(shopId)) {
            CopyOnWriteArrayList<Integer> productBids = bids.get(shopId);
            if (productBids.contains(productId)) {
                // If the product is bid on, we do not allow quantity updates
                throw new OurRuntime("Cannot update quantity for a product that has bids.");
            }
        }
        ConcurrentHashMap<Integer, Integer> shopItems = items.get(shopId);
        if (shopItems != null) {
            shopItems.put(productId, quantity);
        }
        syncToPersistentCollections();
    }

    public void mergeCart(ShoppingCart otherCart) {
        HashMap<Integer, HashMap<Integer, Integer>> otherItems = otherCart.getItems();
        for (Integer shopId : otherItems.keySet()) {
            items.putIfAbsent(shopId, new ConcurrentHashMap<>());
            ConcurrentHashMap<Integer, Integer> currentBasket = items.get(shopId);
            HashMap<Integer, Integer> otherBasket = otherItems.get(shopId);

            synchronized (currentBasket) { // Synchronize per basket for compound updates
                for (Integer productId : otherBasket.keySet()) {
                    currentBasket.merge(productId, otherBasket.get(productId), Integer::sum);
                }
            }
        }
        syncToPersistentCollections();
    }

    /**
     * Returns a copy of the items in the shopping cart.
     * The copy is a deep copy, meaning that changes to the copy will not affect the
     * original items.
     * 
     * @return A deep copy of the items in the shopping cart.
     */
    public HashMap<Integer, HashMap<Integer, Integer>> getItems() {
        HashMap<Integer, HashMap<Integer, Integer>> copy = new HashMap<>();
        for (Map.Entry<Integer, ConcurrentHashMap<Integer, Integer>> entry : items.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Restores the shopping cart with the given items.
     * 
     * @param items A HashMap containing the items to restore in the shopping cart.
     *              shopId -> <itemId -> quantity>
     */
    public void restoreCart(HashMap<Integer, HashMap<Integer, Integer>> newItems) {
        if (!items.isEmpty()) {
            return;
        }
        for (Integer shopId : newItems.keySet()) {
            items.putIfAbsent(shopId, new ConcurrentHashMap<>());
            ConcurrentHashMap<Integer, Integer> shopItems = items.get(shopId);
            HashMap<Integer, Integer> newShopItems = newItems.get(shopId);

            synchronized (shopItems) {
                for (Integer productId : newShopItems.keySet()) {
                    shopItems.merge(productId, newShopItems.get(productId), Integer::sum);
                }
            }
        }
        syncToPersistentCollections();
    }

    public Map<Integer, Map<Integer, Integer>> getCart() {
        Map<Integer, Map<Integer, Integer>> cartCopy = new HashMap<>();
        for (Map.Entry<Integer, ConcurrentHashMap<Integer, Integer>> entry : items.entrySet()) {
            cartCopy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return cartCopy;
    }

    public void addBid(int shopId, Map<Integer, Integer> newItems) {
        bids.putIfAbsent(shopId, new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<Integer> productBids = bids.get(shopId);

        for (Map.Entry<Integer, Integer> entry : newItems.entrySet()) {
            Integer productId = entry.getKey();
            if (!productBids.contains(productId)) {
                productBids.add(productId);
            }
        }

        ConcurrentHashMap<Integer, Integer> shopItems = items.get(shopId);
        if (shopItems == null) {
            shopItems = new ConcurrentHashMap<>();
            items.put(shopId, shopItems);
        }
        for (Map.Entry<Integer, Integer> entry : newItems.entrySet()) {
            if (!productBids.contains(entry.getKey())) {
                productBids.add(entry.getKey());
            } else {
                Integer productId = entry.getKey();
                Integer quantity = entry.getValue();
                shopItems.merge(productId, quantity, Integer::sum); // Add to the basket
            }
        }
        syncToPersistentCollections();
    }

    /**
     * JPA lifecycle method called after loading from database
     */
    @PostLoad
    private void onLoad() {
        loadFromPersistentCollections();
    }

    /**
     * JPA lifecycle method called before persisting to database
     */
    @PrePersist
    @PreUpdate
    private void onPersistUpdate() {
        syncToPersistentCollections();
    }

    public boolean hasItemOfShop(int shopID, int itemID) {
        if (items.get(shopID) == null)
            return false;
        return items.get(shopID).get(itemID) != null;
    }

    public void updateProductQuantity(int shopID, int itemID, int addOrRemove) {
        if (!hasItemOfShop(shopID, itemID))
            throw new OurRuntime("item or shop not in cart. ", shopID, itemID, addOrRemove);
        int quantity = items.get(shopID).get(itemID);
        items.get(shopID).put(itemID, quantity + addOrRemove);
        syncToPersistentCollections();
    }

    public void removeItemFromCart(int shopID, int itemID) {
        items.get(shopID).remove(itemID);
        syncToPersistentCollections();
    }

    public List<Integer> getShopIds() {
        List<Integer> shopIds = new ArrayList<>(items.keySet());
        shopIds.addAll(bids.keySet());
        return shopIds;
    }
}
