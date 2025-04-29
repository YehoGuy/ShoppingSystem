package DomainLayer.Item;

import java.util.List;


public interface IItemRepository {

    /**
     * Creates a new item with the given name and description.
     * The repository auto‐allocates a unique id for the item.
     *
     * @param name        the item name
     * @param description the item description
     * @return the newly created Item id
     */
    Integer createItem(String name, String description, Integer category);

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

    /**
     * Retrieves a list of Item objects for the given list of item IDs.
     * If an ID is not found, it is skipped.
     *
     * @param itemIds the list of item IDs to fetch
     * @return an unmodifiable list of corresponding Item instances
     */
    List<Item> getItemsByIds(List<Integer> itemIds);

    /**
     * Retrieves a list of all items in the specified category.
     *
     * @param categoryId the category ID
     * @return an unmodifiable list of Item instances in the category
     */
    List<Item> getItemsByCategory(ItemCategory category);
}