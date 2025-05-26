package com.example.app.PresentationLayer.DTO.Shop;


import java.util.List;
import java.util.Map;

import com.example.app.PresentationLayer.DTO.Item.ItemDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

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
        List<ShopReviewDTO> reviews = s.getReviews().stream()
                .map(r -> ShopReviewDTO.fromDomain(r, s.getName()))
                .toList();
        return new ShopDTO(
                s.getId(),
                s.getName(),
                items,
                s.getItemQuantities(),
                s.getItemPrices(),
                reviews);
    }

    public int getShopId() {
        return id;
    }

}