package com.example.app.DomainLayer.Item;

import jakarta.persistence.Embeddable;

/**
 * Represents a review for an item.
 */
@Embeddable
public class ItemReview {
    private final int rating; // The rating as an integer.
    private final String reviewText; // The review text.

    public ItemReview(int rating, String reviewText) {
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public ItemReview() {
        this.rating = 0; // Default rating
        this.reviewText = ""; // Default review text
    }

    public int getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    @Override
    public String toString() {
        return "ItemReview{rating=" + rating + ", reviewText='" + reviewText + "'}";
    }
}