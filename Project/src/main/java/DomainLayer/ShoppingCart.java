package DomainLayer;

import java.util.Dictionary;
import java.util.Hashtable;

public class ShoppingCart {
    final private Dictionary<Integer, Dictionary<Integer,Integer>> items; // shopID, (productID, quantity)
    
    public ShoppingCart() {
        this.items = new Hashtable<>();
    }

    public void clearCart() {
        ((Hashtable<Integer, Dictionary<Integer, Integer>>) items).clear();
    }
    
    public void addBasket(int shopId) {
        if (!((Hashtable<Integer, Dictionary<Integer, Integer>>) items).containsKey(shopId)) {
            items.put(shopId, new Hashtable<>());
        }
    }
    
    public void removeBasket(int shopId) {
        if (((Hashtable<Integer, Dictionary<Integer, Integer>>) items).containsKey(shopId)) {
            items.remove(shopId);
        }
    }

    public Dictionary<Integer, Integer> getBasket(int shopId) {
        return items.get(shopId);
    }

    public void addItem(int shopId, int productId, int quantity) {
        if (!((Hashtable<Integer, Dictionary<Integer, Integer>>) items).containsKey(shopId)) {
            items.put(shopId, new Hashtable<>());
        }
        Dictionary<Integer, Integer> shopItems = items.get(shopId);
        if (((Hashtable<Integer, Integer>) shopItems).containsKey(productId)) {
            shopItems.put(productId, shopItems.get(productId) + quantity);
        } else {
            shopItems.put(productId, quantity);
        }
    }

    public void removeItem(int shopId, int productId) {
        if (((Hashtable<Integer, Dictionary<Integer, Integer>>) items).containsKey(shopId)) {
            Dictionary<Integer, Integer> shopItems = items.get(shopId);
            if (((Hashtable<Integer, Integer>) shopItems).containsKey(productId)) {
                shopItems.remove(productId);
            }
        }
    }

    public void setBasket(int shopId, Dictionary<Integer, Integer> basket) {
        items.put(shopId, basket);
    }

    public void updateProduct(int shopId, int productId, int quantity) {
        if (!((Hashtable<Integer, Dictionary<Integer, Integer>>) items).containsKey(shopId)) {
            return;
        }
        Dictionary<Integer, Integer> shopItems = items.get(shopId);
        shopItems.put(productId, quantity);
    }

    public void mergeCart(ShoppingCart otherCart) {
        Hashtable<Integer, Dictionary<Integer, Integer>> otherItems = 
            (Hashtable<Integer, Dictionary<Integer, Integer>>) otherCart.items;

        for (Integer shopId : otherItems.keySet()) {
            if (!items.containsKey(shopId)) {
                items.put(shopId, otherItems.get(shopId));
            } else {
                Dictionary<Integer, Integer> currentBasket = items.get(shopId);
                Dictionary<Integer, Integer> otherBasket = otherItems.get(shopId);

                for (Integer productId : ((Hashtable<Integer, Integer>) otherBasket).keySet()) {
                    int quantity = otherBasket.get(productId);
                    if (currentBasket.get(productId) != null) {
                        quantity += currentBasket.get(productId);
                    }
                    currentBasket.put(productId, quantity);
                }
            }
        }
    }

}
