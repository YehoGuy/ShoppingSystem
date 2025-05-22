package DTOs;

import java.util.List;
import java.util.Map;

public class ShoppingCartDTO {

    // shopId -> Item Id
    private Map<Integer, List<Integer>> shopItems;
    // shopId -> Map<Item Id, Price>
    private Map<Integer, Map<Integer,Double>> shopItemPrices;
    // shopId -> Map<Item Id, Quantity>
    private Map<Integer, Map<Integer,Integer>> shopItemQuantities;
    private List<ItemDTO> items;



    public ShoppingCartDTO() {}

    public ShoppingCartDTO(Map<Integer, List<Integer>> shopItems,
                        Map<Integer, Map<Integer, Double>> shopItemPrices,
                        Map<Integer, Map<Integer, Integer>> shopItemQuantities,
                        double totalPrice,  List<ItemDTO> items) {
        this.shopItems = shopItems;
        this.shopItemPrices = shopItemPrices;
        this.shopItemQuantities = shopItemQuantities;
    }

    public Map<Integer, List<Integer>> getShopItems() {
        return shopItems;
    }

    public void setShopItems(Map<Integer, List<Integer>> shopItems) {
        this.shopItems = shopItems;
    }

    public Map<Integer, Map<Integer, Double>> getShopItemPrices() {
        return shopItemPrices;
    }

    public void setShopItemPrices(Map<Integer, Map<Integer, Double>> shopItemPrices) {
        this.shopItemPrices = shopItemPrices;
    }

    public Map<Integer, Map<Integer, Integer>> getShopItemQuantities() {
        return shopItemQuantities;
    }

    public void setShopItemQuantities(Map<Integer, Map<Integer, Integer>> shopItemQuantities) {
        this.shopItemQuantities = shopItemQuantities;
    }

    public Double getTotalPrice() {
        return shopItemPrices.values().stream()
                .mapToDouble(map -> map.values().stream().mapToDouble(Double::doubleValue).sum())
                .sum();
    }

    public List<ItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ItemDTO> items) {
        this.items = items;
    }
}