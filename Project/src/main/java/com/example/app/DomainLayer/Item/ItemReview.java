package com.example.app.DomainLayer.Item;

/**
 * Represents a review for an item.
 */
public class ItemReview {
    private final int rating;          // The rating as an integer.
    private final String reviewText;   // The review text.

    public ItemReview(int rating, String reviewText) {
        this.rating = rating;
        this.reviewText = reviewText;
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