package DTOs;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShoppingCartDTO {

    // shopId -> List<Item Id>
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
        double total = 0.0;

        // For each shop…
        for (Map.Entry<Integer, Map<Integer, Double>> shopEntry : shopItemPrices.entrySet()) {
            Integer shopId = shopEntry.getKey();
            Map<Integer, Double> prices     = shopEntry.getValue();
            Map<Integer, Integer> quantities = shopItemQuantities.get(shopId);

            if (quantities == null) continue; // no quantities for this shop

            // For each item in the price-map…
            for (Map.Entry<Integer, Double> itemEntry : prices.entrySet()) {
                Integer itemId   = itemEntry.getKey();
                Double  price    = itemEntry.getValue();
                Integer quantity = quantities.get(itemId);

                if (price != null && quantity != null) {
                    total += price * quantity;
                }
            }
        }

        return total;
    }

    
    public List<ItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ItemDTO> items) {
        this.items = items;
    }

    public ShoppingCartDTO getShoppingCartDTOofShop(int shopId) {
        Map<Integer, List<Integer>> filteredShopItems = shopItems.entrySet().stream()
                .filter(entry -> entry.getKey().equals(shopId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<Integer, Map<Integer, Double>> filteredShopItemPrices = shopItemPrices.entrySet().stream()
                .filter(entry -> entry.getKey().equals(shopId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<Integer, Map<Integer, Integer>> filteredShopItemQuantities = shopItemQuantities.entrySet().stream()
                .filter(entry -> entry.getKey().equals(shopId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        List<ItemDTO> filteredItems = items.stream()
                .filter(item -> filteredShopItems.containsKey(shopId) && 
                                filteredShopItems.get(shopId).contains(item.getId()))
                .collect(Collectors.toList());
        
        Double totalPrice = 0.0;
        for (Map.Entry<Integer, Double> entry : filteredShopItemPrices.get(shopId).entrySet()) {
            Integer itemId = entry.getKey();
            Double price = entry.getValue();
            Integer quantity = filteredShopItemQuantities.get(shopId).get(itemId);
            totalPrice += price * quantity;
        }
            
        ShoppingCartDTO cart = new ShoppingCartDTO(
                filteredShopItems,
                filteredShopItemPrices,
                filteredShopItemQuantities,
                totalPrice,
                filteredItems
        );
        return cart;
    }
}                       