import java.util.List;
import DomainLayer.Item.Item;
import DomainLayer.Item.ItemReview;

public class ItemService {

    private final IItemRepository itemRepository;

    /**
     * Constructor for ItemService.
     *
     * @param itemRepository an instance of IItemRepository that handles storage of Item instances.
     */
    public ItemService(IItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /**
     * Creates a new item with the specified parameters.
     *
     * @param name        the item name.
     * @param description the item description.
     * @return the newly created Item.
     */
    public Item createItem(String name, String description) {
        try {
            return itemRepository.createItem(name, description);
        } catch (Exception e) {
            throw new RuntimeException("Error creating item: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves an item by its identifier.
     *
     * @param itemId the item id.
     * @return the Item instance.
     */
    public Item getItem(int itemId) {
        try {
            return itemRepository.getItem(itemId);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving item with id " + itemId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns a list of all items.
     *
     * @return an unmodifiable list of Item instances.
     */
    public List<Item> getAllItems() {
        try {
            return itemRepository.getAllItems();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving all items: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a review to the specified item.
     *
     * @param itemId     the item id.
     * @param rating     the review rating.
     * @param reviewText the review text.
     */
    public void addReviewToItem(int itemId, int rating, String reviewText) {
        try {
            itemRepository.addReviewToItem(itemId, rating, reviewText);
        } catch (Exception e) {
            throw new RuntimeException("Error adding review to item " + itemId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the reviews for the specified item.
     *
     * @param itemId the item id.
     * @return a list of ItemReview instances.
     */
    public List<ItemReview> getItemReviews(int itemId) {
        try {
            return itemRepository.getItemReviews(itemId);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving reviews for item " + itemId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the average rating for the specified item.
     *
     * @param itemId the item id.
     * @return the average rating, or -1.0 if no reviews.
     */
    public double getItemAverageRating(int itemId) {
        try {
            return itemRepository.getItemAverageRating(itemId);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving average rating for item " + itemId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Deletes the specified item.
     *
     * @param itemId the item id.
     */
    public void deleteItem(int itemId) {
        try {
            itemRepository.deleteItem(itemId);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting item " + itemId + ": " + e.getMessage(), e);
        }
    }
}