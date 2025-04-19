package DomainLayer.Shop;


/**
 * A domain-specific class representing a review with a rating and a review text.
 */
public class ShopReview {
    private final int rating;      // Rating as an integer.
    private final String reviewText; // The review text.

    public ShopReview(int rating, String reviewText) {
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
        return "Review{rating=" + rating + ", reviewText='" + reviewText + "'}";
    }
}