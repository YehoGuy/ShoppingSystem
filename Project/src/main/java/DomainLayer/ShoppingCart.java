import java.util.Dictionary;
import java.util.Hashtable;

public class ShoppingCart {
    final private int cartId;
    final private Dictionary<Integer, Dictionary<Integer,Integer>> items; // shopID, (productID, quantity)
    
    public ShoppingCart(int cartId) {
        this.cartId = cartId;
        this.items = new Hashtable<>();
    }
    
    public int getCartId() {
        return cartId;
    }

    public void clearCart() {
        ((Hashtable)items).clear();
    }
    
    public void addBasket(int shopId) {
        if (!((Hashtable)items).containsKey(shopId)) {
            items.put(shopId, new Hashtable<>());
        }
    }
    
    public void removeBasket(int shopId) {
        if (((Hashtable)items).containsKey(shopId)) {
            items.remove(shopId);
        }
    }

    public Dictionary<Integer, Integer> getBasket(int shopId) {
        return items.get(shopId);
    }

    public void addItem(int shopId, int productId, int quantity) {
        if (!((Hashtable)items).containsKey(shopId)) {
            items.put(shopId, new Hashtable<>());
        }
        Dictionary<Integer, Integer> shopItems = items.get(shopId);
        if (((Hashtable) shopItems).containsKey(productId)) {
            shopItems.put(productId, shopItems.get(productId) + quantity);
        } else {
            shopItems.put(productId, quantity);
        }
    }

    public void removeItem(int shopId, int productId) {
        if (((Hashtable)items).containsKey(shopId)) {
            Dictionary<Integer, Integer> shopItems = items.get(shopId);
            if (((Hashtable)shopItems).containsKey(productId)) {
                shopItems.remove(productId);
            }
        }
    }

    public void setBasket(int shopId, Dictionary<Integer, Integer> basket) {
        items.put(shopId, basket);
    }

    public void updateProduct(int shopId, int productId, int quantity) {
        if (!((Hashtable)items).containsKey(shopId)) {
            return;
        }
        Dictionary<Integer, Integer> shopItems = items.get(shopId);
        shopItems.put(productId, quantity);
    }

}
