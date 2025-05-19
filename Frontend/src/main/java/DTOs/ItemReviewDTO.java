package DTOs;

public class ItemReviewDTO {

    private int rating;
    private String reviewText;

    // Jackson needs this
    public ItemReviewDTO() {
    }

    public ItemReviewDTO(int rating, String reviewText) {
        this.rating = rating;
        this.reviewText = reviewText;
    }

    // Getters
    public int getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    // Setters (required by Jackson)
    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

}
