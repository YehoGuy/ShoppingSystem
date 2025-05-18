package DTOs;

import java.util.List;
import java.util.Map;

public class ShopDTO {

    private int shopId;
    private String name;
    private Map<ItemDTO, Integer> items;   // ItemDTO → quantity
    private Map<ItemDTO, Integer> prices;  // ItemDTO → price
    private List<ShopReviewDTO> reviews;

    // Jackson needs this
    public ShopDTO() { }

    public ShopDTO(int shopId, String name, Map<ItemDTO, Integer> items, Map<ItemDTO, Integer> prices, List<ShopReviewDTO> reviews) {
        this.shopId = shopId;
        this.name = name;
        this.items = items;
        this.prices = prices;
        this.reviews = reviews;
    }

    // Getters
    public int getShopId() { return shopId; }
    public String getName() { return name; }
    public Map<ItemDTO, Integer> getItems() { return items; }
    public Map<ItemDTO, Integer> getPrices() { return prices; }
    public List<ShopReviewDTO> getReviews() { return reviews; }

    // Setter methods for Jackson
    public void setShopId(int shopId) { this.shopId = shopId; }
    public void setName(String name) { this.name = name; }
    public void setItems(Map<ItemDTO, Integer> items) { this.items = items; }
    public void setPrices(Map<ItemDTO, Integer> prices) { this.prices = prices; }
    public void setReviews(List<ShopReviewDTO> reviews) { this.reviews = reviews; }

}
