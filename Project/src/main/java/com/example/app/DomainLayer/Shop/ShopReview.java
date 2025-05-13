package com.example.app.DomainLayer.Shop;


/**
 * A domain-specific class representing a review with a rating and a review text.
 */
public class ShopReview {
    private final int rating;      // Rating as an integer.
    private final String reviewText; // The review text.
    private final int userId;     // The ID of the user who wrote the review.

    public ShopReview(int userId, int rating, String reviewText) {
        this.rating = rating;
        this.reviewText = reviewText;
        this.userId = userId;
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