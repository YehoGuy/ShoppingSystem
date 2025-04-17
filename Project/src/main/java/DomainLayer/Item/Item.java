import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an item that contains an id, name, description, and a list of reviews.
 */
public class Item {

    private final int id;
    private final String name;
    private final String description;

    // List to hold reviews.
    private final List<ItemReview> reviews = new ArrayList<>();

    public Item(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns an unmodifiable view of the reviews.
     *
     * @return a list of reviews.
     */
    public List<ItemReview> getReviews() {
        synchronized (reviews) {
            return Collections.unmodifiableList(new ArrayList<>(reviews));
        }
    }

    /**
     * Adds a new review to the item.
     *
     * @param rating     the review rating.
     * @param reviewText the review text.
     */
    public void addReview(int rating, String reviewText) {
        addReview(new ItemReview(rating, reviewText));
    }

    /**
     * Adds an ItemReview to the item.
     *
     * @param review the review to add.
     */
    public void addReview(ItemReview review) {
        synchronized (reviews) {
            reviews.add(review);
        }
    }

    /**
     * Calculates and returns the average rating.
     *
     * @return the average rating, or 0 if there are no reviews.
     */
    public double getAverageRating() {
        List<ItemReview> currentReviews = getReviews();
        if (currentReviews.isEmpty()) {
            return -1.0;
        }
        int sum = 0;
        for (ItemReview review : currentReviews) {
            sum += review.getRating();
        }
        return (double) sum / currentReviews.size();
    }

    @Override
    public String toString() {
        return "Item{id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", averageRating=" + getAverageRating() +
                ", reviews=" + reviews +
                '}';
    }
}
