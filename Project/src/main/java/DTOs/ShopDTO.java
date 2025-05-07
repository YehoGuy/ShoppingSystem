package DTOs;

import java.util.Map;

public class ShopDTO {

    private String name;
    private Map<ItemDTO, Integer> items; // ItemDTO and quantity
    private Map<ItemDTO, Integer> prices; // ItemDTO and price

    public ShopDTO(String name, Map<ItemDTO, Integer> items, Map<ItemDTO, Integer> prices) {
        this.name = name;
        this.items = items;
        this.prices = prices;
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

}
