package DTOs;

public class ShopReviewDTO {

    private final int rating; // Rating as an integer.
    private final String reviewText; // The review text.
    private final int userId; // The ID of the user who wrote the review.
    private final String shopName; // The name of the shop being reviewed.

    public ShopReviewDTO(int userId, int rating, String reviewText, String shopName) {
        this.shopName = shopName;
        this.rating = rating;
        this.reviewText = reviewText;
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public String getShopName() {
        return shopName;
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
