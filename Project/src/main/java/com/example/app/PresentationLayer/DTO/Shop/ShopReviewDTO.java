package com.example.app.PresentationLayer.DTO.Shop;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Immutable review view sent to / received from the API. **/
public record ShopReviewDTO(
        @Positive int userId,
        @Min(1) @Max(5) int rating,
        @NotBlank String reviewText,
        @NotBlank String shopName) {

    /*  ---------------- Domain ➜ DTO ---------------- */
    public static ShopReviewDTO fromDomain(com.example.app.DomainLayer.Shop.ShopReview r, String shopName) {
        return new ShopReviewDTO(r.getUserId(), r.getRating(), r.getReviewText(), shopName);
    }

    /*  ---------------- DTO ➜ Domain ---------------- */
    public com.example.app.DomainLayer.Shop.ShopReview toDomain() {
        return new com.example.app.DomainLayer.Shop.ShopReview(userId, rating, reviewText);
    }
}