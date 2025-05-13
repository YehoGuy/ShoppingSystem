package com.example.app.PresentationLayer.DTO.Item;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ItemReviewDTO(
        @Min(1) @Max(5) int rating,
        @NotBlank String reviewText) {

    /* Domain → DTO */
    public static ItemReviewDTO fromDomain(com.example.app.DomainLayer.Item.ItemReview r) {
        return new ItemReviewDTO(r.getRating(), r.getReviewText());
    }

    /* DTO → Domain */
    public com.example.app.DomainLayer.Item.ItemReview toDomain() {
        return new com.example.app.DomainLayer.Item.ItemReview(rating, reviewText);
    }
}