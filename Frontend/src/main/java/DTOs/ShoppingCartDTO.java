package DTOs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

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
        // 1. Fetch (or empty) the raw data for this shop:
        List<Integer> itemIds    = shopItems.getOrDefault(shopId, Collections.emptyList());
        Map<Integer, Double> prices     = shopItemPrices
                                            .getOrDefault(shopId, Collections.emptyMap());
        Map<Integer, Integer> quantities = shopItemQuantities
                                            .getOrDefault(shopId, Collections.emptyMap());

        // 2. Build the list of ItemDTOs that belong in this shop-cart:
        List<ItemDTO> filteredItems = items.stream()
            .filter(item -> itemIds.contains(item.getId()))
            .collect(Collectors.toList());

        // 3. Compute totalPrice—only for those itemIds, only when both price & qty exist:
        double totalPrice = 0.0;
        for (Integer itemId : itemIds) {
            Double price = prices.get(itemId);
            Integer qty  = quantities.get(itemId);
            if (price != null && qty != null) {
                totalPrice += price * qty;
            }
        }

        // 4. (If your DTO really needs maps keyed by shopId:) wrap them up again:
        Map<Integer, List<Integer>>        filteredShopItemsMap       =
                Collections.singletonMap(shopId, new ArrayList<>(itemIds));
        Map<Integer, Map<Integer, Double>> filteredPriceMap           =
                Collections.singletonMap(shopId, new HashMap<>(prices));
        Map<Integer, Map<Integer, Integer>> filteredQuantityMap       =
                Collections.singletonMap(shopId, new HashMap<>(quantities));


        // 5. Return it:
        return new ShoppingCartDTO(
            filteredShopItemsMap,
            filteredPriceMap,
            filteredQuantityMap,
            totalPrice,
            filteredItems
        );
        
    }
}                       