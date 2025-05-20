package com.example.app.PresentationLayer.DTO.User;


import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 
Serialises the nested map <shopId → (itemId → quantity)>.*/
public record ShoppingCartDTO(
    @NotNull Map<String, List<CartEntryDTO>> cartItems,
    double totalPrice,
    Map<String, Double> shopPrices
) {
    /* -------- Domain → DTO -------- */
    public static ShoppingCartDTO fromDomain(com.example.app.DomainLayer.ShoppingCart c) {
        // var itemsMap = c.getItems().entrySet().stream()
        //     .collect(Collectors.toMap(
        //         e -> e.getKey().toString(),
        //         e -> e.getValue().stream().map(CartEntryDTO::fromDomain).toList()
        //     ));
        // return new ShoppingCartDTO(
        //     itemsMap,
        //     c.getTotalPrice(),
        //     c.getShopPrices()
        // );
        return null;
        //TODO: Implement the conversion from domain to DTO
    }
}
