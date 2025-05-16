package DTOs;

import java.util.Map;

public class ShoppingCartDTO {
    private final Map<String, Map<ItemDTO, Integer>> cartItems; // Map<shopName, Map<item, quantity>>

    public ShoppingCartDTO(Map<String, Map<ItemDTO, Integer>> cartItems) {
        this.cartItems = cartItems;
    }

    public Map<String, Map<ItemDTO, Integer>> getCartItems() {
        return cartItems;
    }
}
