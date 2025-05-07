package DTOs;

import java.util.List;
import java.util.Map;

public class ShopDTO {

    private String name;
    private Map<ItemDTO, Integer> items; // ItemDTO and quantity
    private Map<ItemDTO, Integer> prices; // ItemDTO and price
    private List<ShopReviewDTO> reviews; // List of reviews for the shop

    public ShopDTO(String name, Map<ItemDTO, Integer> items, Map<ItemDTO, Integer> prices,
            List<ShopReviewDTO> reviews) {
        this.name = name;
        this.items = items;
        this.prices = prices;
        this.reviews = reviews;
    }

    public String getName() {
        return name;
    }

    public Map<ItemDTO, Integer> getItems() {
        return items;
    }

    public Map<ItemDTO, Integer> getPrices() {
        return prices;
    }

    public List<ShopReviewDTO> getReviews() {
        return reviews;
    }

}
