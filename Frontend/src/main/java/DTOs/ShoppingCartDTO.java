package DTOs;

import java.util.List;
import java.util.Map;

public class ShoppingCartDTO {

    // shopId → list of cart entries
    private Map<String, List<CartEntryDTO>> cartItems;
    private double totalPrice;
    private Map<String, Double> shopPrices;

    public ShoppingCartDTO() {}

    public ShoppingCartDTO(Map<String, List<CartEntryDTO>> cartItems, double totalPrice, Map<String, Double> shopPrices) {
        this.cartItems = cartItems;
        this.totalPrice = totalPrice;
        this.shopPrices = shopPrices;
    }

    public Map<String, List<CartEntryDTO>> getCartItems() {
        return cartItems;
    }

    public void setCartItems(Map<String, List<CartEntryDTO>> cartItems) {
        this.cartItems = cartItems;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Map<String, Double> getShopPrices() {
        return shopPrices;
    }

    public void setShopPrices(Map<String, Double> shopPrices) {
        this.shopPrices = shopPrices;
    }
}
