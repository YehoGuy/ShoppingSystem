package DomainLayer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShoppingCart {
    final private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer,Integer>> items; // shopID, (productID, quantity)  
                                                                    //every entry in the HashMap is a basket.
    
    public ShoppingCart() {
        this.items = new ConcurrentHashMap<>();
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

    public HashMap<Integer, Integer> getBasket(int shopId) {
        ConcurrentHashMap<Integer, Integer> basket = items.get(shopId);
        return basket != null ? new HashMap<>(basket) : null;
    }

    public void addItem(int shopId, int productId, int quantity) {
        items.putIfAbsent(shopId, new ConcurrentHashMap<>());
        items.get(shopId).merge(productId, quantity, Integer::sum); // Thread-safe add/update
    }

    public void removeItem(int shopId, int productId) {
        ConcurrentHashMap<Integer, Integer> shopItems = items.get(shopId);
        if (shopItems != null) {
            shopItems.remove(productId);
        }
    }

    public void setBasket(int shopId, HashMap<Integer, Integer> basket) {
        ConcurrentHashMap<Integer, Integer> concurrentBasket = new ConcurrentHashMap<>(basket);
        items.put(shopId, concurrentBasket);
    }

    public void updateProduct(int shopId, int productId, int quantity) {
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
     * The copy is a deep copy, meaning that changes to the copy will not affect the original items.
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
    * @param items A HashMap containing the items to restore in the shopping cart. shopId -> <itemId -> quantity>
    */
    public void restoreCart(HashMap<Integer, HashMap<Integer, Integer>> newItems) {
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

}
