package DTOs;

import java.util.Map;

public class ShoppingCartDTO {
    private final Map<String, Map<ItemDTO, Integer>> cartItems; // Map<shopId, Map<itemId, quantity>>
    private final double totalPrice;
    private final Map<String, Double> shopPrices; // Map<shopId, totalPrice>

    public ShoppingCartDTO(Map<String, Map<ItemDTO, Integer>> cartItems, double totalPrice, Map<String, Double> shopPrices) {
        this.cartItems = cartItems;
        this.totalPrice = totalPrice;
        this.shopPrices = shopPrices;
    }

    public Map<String, Map<ItemDTO, Integer>> getCartItems() {
        return cartItems;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public Map<String, Double> getShopPrices() {
        return shopPrices;
    }
}
