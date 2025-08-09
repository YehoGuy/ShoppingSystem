package com.example.app.PresentationLayer.DTO.User;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.app.PresentationLayer.DTO.Item.ItemDTO;

import io.micrometer.common.lang.NonNull;
import jakarta.validation.constraints.NotNull;

/**
 
Serialises the nested map <shopId → (itemId → quantity)>.*/
public record ShoppingCartDTO(
        @NotNull Map<Integer, List<Integer>> shopItems,
        @NotNull Map<Integer, Map<Integer, Double>> prices,
        @NotNull Map<Integer, Map<Integer, Integer>> quantity,
        @NonNull List<ItemDTO> items) {

    /* -------- Domain ➜ DTO (for responses) -------- */
    public static ShoppingCartDTO fromDomain(com.example.app.DomainLayer.ShoppingCart cart,
    Map<Integer, Map<Integer, Double>> prices,
    List<ItemDTO> items) {
        Map<Integer, Map<Integer, Integer>> cartMap = new HashMap<>();
        Map<Integer, List<Integer>> shopItems = new HashMap<>();
        cart.getCart().forEach((shopId, basket) ->       // basket is Map<Integer,Integer>
            {
                cartMap.put(shopId, new HashMap<>(basket));
                List<Integer> itemIds = basket.keySet().stream().toList();
                shopItems.put(shopId, itemIds);
            });
        
    
        return new ShoppingCartDTO(
                shopItems,
                prices,
                cartMap,
                items);
        
    }

    /* -------- DTO ➜ Domain (for create / merge) -------- */
    public com.example.app.DomainLayer.ShoppingCart toDomain() {
        com.example.app.DomainLayer.ShoppingCart cart = new com.example.app.DomainLayer.ShoppingCart();
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : quantity.entrySet()) {
            int shopId = entry.getKey();
            Map<Integer, Integer> basket = entry.getValue();
            cart.addBasket(shopId);
            for (Map.Entry<Integer, Integer> itemEntry : basket.entrySet()) {
                int itemId = itemEntry.getKey();
                int quantity = itemEntry.getValue();
                cart.addItem(shopId, itemId, quantity);
            }
        }
        return cart;
    }
}