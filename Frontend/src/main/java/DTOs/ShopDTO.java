package DTOs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopDTO {

    private int shopId;
    private String name;

    private List<ItemDTO> items;                 // List of items in the shop
    private Map<Integer, Integer> itemQuantities; // itemId → quantity
    private Map<Integer, Double> itemPrices;     // itemId → price

    private List<ShopReviewDTO> reviews;

    public ShopDTO() { }

    public ShopDTO(int shopId, String name,
                   List<ItemDTO> items,
                   Map<Integer, Integer> itemQuantities,
                   Map<Integer, Double> itemPrices,
                   List<ShopReviewDTO> reviews) {
        this.shopId = shopId;
        this.name = name;
        this.items = items;
        this.itemQuantities = itemQuantities;
        this.itemPrices = itemPrices;
        this.reviews = reviews;
    }

    public int getShopId() { return shopId; }
    public void setShopId(int shopId) { this.shopId = shopId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<ItemDTO> getItems() { return items; }
    public void setItems(List<ItemDTO> items) { this.items = items; }

    public Map<Integer, Integer> getItemQuantities() { return itemQuantities; }
    public void setItemQuantities(Map<Integer, Integer> itemQuantities) { this.itemQuantities = itemQuantities; }

    public Map<Integer, Double> getItemPrices() { return itemPrices; }
    public void setItemPrices(Map<Integer, Double> itemPrices) { this.itemPrices = itemPrices; }

    public List<ShopReviewDTO> getReviews() { return reviews; }
    public void setReviews(List<ShopReviewDTO> reviews) { this.reviews = reviews; }
    
    public static Map<ItemDTO, Integer> itemQuantitiesToMapConverter(List<ItemDTO> items, Map<Integer, Integer> itemQuantities) {
        Map<ItemDTO, Integer> itemQuantitiesMap = new HashMap<>();
        for (ItemDTO item : items) {
            int quantity = itemQuantities.get(item.getId());
            itemQuantitiesMap.put(item, quantity);
        }
        return itemQuantitiesMap;
    }

    public static Map<ItemDTO, Double> itemPricesToMapConverter(List<ItemDTO> items, Map<Integer, Double> itemPrices) {
        Map<ItemDTO, Double> itemPricesMap = new HashMap<>();
        for (ItemDTO item : items) {
            Double price = itemPrices.get(item.getId());
            itemPricesMap.put(item, price);
        }
        return itemPricesMap;
    }
}
