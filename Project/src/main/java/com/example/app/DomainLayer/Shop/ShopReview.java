package com.example.app.DomainLayer.Shop;

import jakarta.persistence.Embeddable;

/**
 * A domain-specific class representing a review with a rating and a review
 * text.
 */
@Embeddable
public class ShopReview {
    private int rating; // Rating as an integer.
    private String reviewText; // The review text.
    private int userId; // The ID of the user who wrote the review.

    public ShopReview(int userId, int rating, String reviewText) {
        this.rating = rating;
        this.reviewText = reviewText;
        this.userId = userId;
    }

    public ShopReview() {
        this.rating = 0; // Default rating
        this.reviewText = ""; // Default review text
        this.userId = 0; // Default user ID
    }

    public int getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "Review{rating=" + rating + ", reviewText='" + reviewText + "'}";
    }
}