package DomainLayer;

import java.util.HashMap;

public class ShoppingCart {
    final private HashMap<Integer, HashMap<Integer,Integer>> items; // shopID, (productID, quantity)
    
    public ShoppingCart() {
        this.items = new HashMap<>();
    }

    public void clearCart() {
        items.clear();
    }
    
    public void addBasket(int shopId) {
        if (!((HashMap<Integer, HashMap<Integer, Integer>>) items).containsKey(shopId)) {
            items.put(shopId, new HashMap<>());
        }
    }
    
    public void removeBasket(int shopId) {
        if (((HashMap<Integer, HashMap<Integer, Integer>>) items).containsKey(shopId)) {
            items.remove(shopId);
        }
    }

    public HashMap<Integer, Integer> getBasket(int shopId) {
        return items.get(shopId);
    }

    public void addItem(int shopId, int productId, int quantity) {
        if (!((HashMap<Integer, HashMap<Integer, Integer>>) items).containsKey(shopId)) {
            items.put(shopId, new HashMap<>());
        }
        HashMap<Integer, Integer> shopItems = items.get(shopId);
        if (((HashMap<Integer, Integer>) shopItems).containsKey(productId)) {
            shopItems.put(productId, shopItems.get(productId) + quantity);
        } else {
            shopItems.put(productId, quantity);
        }
    }

    public void removeItem(int shopId, int productId) {
        if (((HashMap<Integer, HashMap<Integer, Integer>>) items).containsKey(shopId)) {
            HashMap<Integer, Integer> shopItems = items.get(shopId);
            if (((HashMap<Integer, Integer>) shopItems).containsKey(productId)) {
                shopItems.remove(productId);
            }
        }
    }

    public void setBasket(int shopId, HashMap<Integer, Integer> basket) {
        items.put(shopId, basket);
    }

    public void updateProduct(int shopId, int productId, int quantity) {
        if (!((HashMap<Integer, HashMap<Integer, Integer>>) items).containsKey(shopId)) {
            return;
        }
        HashMap<Integer, Integer> shopItems = items.get(shopId);
        shopItems.put(productId, quantity);
    }

    public void mergeCart(ShoppingCart otherCart) {
        HashMap<Integer, HashMap<Integer, Integer>> otherItems = 
            (HashMap<Integer, HashMap<Integer, Integer>>) otherCart.items;

        for (Integer shopId : otherItems.keySet()) {
            if (!items.containsKey(shopId)) {
                items.put(shopId, otherItems.get(shopId));
            } else {
                HashMap<Integer, Integer> currentBasket = items.get(shopId);
                HashMap<Integer, Integer> otherBasket = otherItems.get(shopId);

                for (Integer productId : ((HashMap<Integer, Integer>) otherBasket).keySet()) {
                    int quantity = otherBasket.get(productId);
                    if (currentBasket.get(productId) != null) {
                        quantity += currentBasket.get(productId);
                    }
                    currentBasket.put(productId, quantity);
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
        for (Integer shopId : items.keySet()) {
            HashMap<Integer, Integer> shopItems = items.get(shopId);
            HashMap<Integer, Integer> copyShopItems = new HashMap<>(shopItems);
            copy.put(shopId, copyShopItems);
        }
        return copy;
    }

    /**
    * Restores the shopping cart with the given items.
    * @param items A HashMap containing the items to restore in the shopping cart. shopId -> <itemId -> quantity>
    */
    public void restoreCart(HashMap<Integer, HashMap<Integer, Integer>> items) {
        // Logic to restore the shopping cart with the provided items
        for (Integer shopId : items.keySet()) {
            if (!this.items.containsKey(shopId)) {
                this.items.put(shopId, new HashMap<>());
            }
            HashMap<Integer, Integer> shopItems = this.items.get(shopId);
            HashMap<Integer, Integer> newShopItems = items.get(shopId);
            for (Integer productId : newShopItems.keySet()) {
                int quantity = newShopItems.get(productId);
                if (shopItems.containsKey(productId)) {
                    quantity += shopItems.get(productId);
                }
                shopItems.put(productId, quantity);
            }
        }
    }

}
