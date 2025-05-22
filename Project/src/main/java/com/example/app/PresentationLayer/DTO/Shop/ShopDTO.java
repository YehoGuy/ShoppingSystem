package com.example.app.PresentationLayer.DTO.Shop;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

import com.example.app.PresentationLayer.DTO.Item.ItemDTO;

/**
 
A read/write DTO for shop endpoints.
For “list shops” you might return a slimmer “ShopSummaryDTO”, but
this works fine for create / detail views.*/
public record ShopDTO(
        @Positive int id,
        @NotBlank String name,
        List<ItemDTO> items,
        Map<Integer, Integer> itemQuantities,
        Map<Integer, Double> itemPrices,
        List<ShopReviewDTO> reviews) {

    /* ------------- Domain ➜ DTO (for GET endpoints) ------------- */
    public static ShopDTO fromDomain(com.example.app.DomainLayer.Shop.Shop s, List<ItemDTO> items) {
        
        return new ShopDTO(
                s.getId(),
                s.getName(),
                items,
                s.getItemQuantities(),
                s.getItemPrices(),
                s.getReviews().stream().map(ShopReviewDTO::fromDomain).toList());
    }


}