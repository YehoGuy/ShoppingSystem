package com.example.app.DomainLayer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.data.util.Pair;
import com.example.app.ApplicationLayer.OurRuntime;
import jakarta.persistence.Embeddable;

@Embeddable
public class ShoppingCart {
    
    final private ItemMapping items; 

    final private CopyOnWriteArrayList<Pair<Integer, Integer>> bids; // shopID, productID
    
    public ShoppingCart() {
        this.items = new ItemMapping();
        this.bids = new CopyOnWriteArrayList<>();
    }

    public void clearCart() {
        items.clear();
    }
    

    public void addBasket(int shopId) {
        items.putIfAbsent(shopId, new ConcurrentHashMap<>());

    }

    public void removeBasket(int shopId) {
        items.remove(shopId);
    }

    public ConcurrentHashMap<Integer, Integer> getBasket(int shopId) {
        return items.getOrDefault(shopId, new ConcurrentHashMap<>());
    }

    public void addItem(int shopId, int productId, int quantity) {
        items.putIfAbsent(shopId, new ConcurrentHashMap<>());
        items.get(shopId).merge(productId, quantity, Integer::sum); // Thread-safe add/update
    }

    public void removeItem(int shopId, int productId) {
        items.removeItem(shopId, productId);

        for (Pair<Integer, Integer> bid : bids) {
            if (bid.getFirst() == shopId && bid.getSecond() == productId) {
                removeBid(shopId, productId); // Remove bid if it exists
                break; // Exit after removing the bid to avoid ConcurrentModificationException
            }
        }
    }

    public void setBasket(int shopId, HashMap<Integer, Integer> basket) {
        ConcurrentHashMap<Integer, Integer> concurrentBasket = new ConcurrentHashMap<>(basket);
        items.put(shopId, concurrentBasket);
    }

    public void updateProduct(int shopId, int productId, int quantity) {
        for (Pair<Integer, Integer> bid : bids) {
            if (bid.getFirst() == shopId && bid.getSecond() == productId) {
                throw new OurRuntime("Cannot update product in a bid");
            }
        }
        ConcurrentHashMap<Integer, Integer> shopItems = items.get(shopId);
        if (shopItems != null) {
            shopItems.put(productId, quantity);
        }
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
        for (Map.Entry<Integer, ConcurrentHashMap<Integer, Integer>> entry : items.getItems().entrySet()) {
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
        if(!items.isEmpty())
        {
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
    }

    public Map<Integer, Map<Integer, Integer>> getCart() {
        Map<Integer, Map<Integer, Integer>> cartCopy = new HashMap<>();
        for (Map.Entry<Integer, ConcurrentHashMap<Integer, Integer>> entry : items.getItems().entrySet()) {
            cartCopy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return cartCopy;
    }

    public void addBid(int shopId, Map<Integer, Integer> newItems) {
        for (Map.Entry<Integer, Integer> entry : newItems.entrySet()) {
            int productId = entry.getKey();
            int quantity = entry.getValue();
            bids.add(Pair.of(shopId, productId));
            addItem(shopId, productId, quantity);
        }
    }

    public CopyOnWriteArrayList<Pair<Integer, Integer>> getBids() {
        return bids;
    }
    
    public void removeBid(int shopId, int productId) {
        bids.removeIf(bid -> bid.getFirst() == shopId && bid.getSecond() == productId);
    }

}
