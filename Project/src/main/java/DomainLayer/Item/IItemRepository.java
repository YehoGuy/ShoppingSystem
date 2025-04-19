package DomainLayer.Item;

import java.util.List;
import DomainLayer.Item.Item;
import DomainLayer.Item.ItemReview;

public interface IItemRepository {

    /**
     * Creates a new item with the given name and description.
     * The repository auto‐allocates a unique id for the item.
     *
     * @param name        the item name
     * @param description the item description
     * @return the newly created Item
     */
    Item createItem(String name, String description);

    /**
     * Retrieves an item by its id.
     *
     * @param itemId the item id
     * @return the Item, or null if not found
     */
    Item getItem(int itemId);

    /** for tests **
     * Returns an unmodifiable list of all items.
     *
     * @return list of all Items
     */
    List<Item> getAllItems();

    /**
     * Adds a review to the specified item.
     *
     * @param itemId     the item id
     * @param rating     the review rating
     * @param reviewText the review text
     */
    void addReviewToItem(int itemId, int rating, String reviewText);

    /**
     * Retrieves an unmodifiable list of reviews for the specified item.
     *
     * @param itemId the item id
     * @return list of ItemReview
     */
    List<ItemReview> getItemReviews(int itemId);

    /**
     * Returns the average rating for the specified item.
     *
     * @param itemId the item id
     * @return the average rating, or -1.0 if no reviews
     */
    double getItemAverageRating(int itemId);

    /**
     * Deletes an item and all its reviews.
     *
     * @param itemId the item id
     */
    void deleteItem(int itemId);
}