package main.ApplicationLayer.Item;

import java.util.List;

import main.ApplicationLayer.LoggerService;
import main.DomainLayer.Item.IItemRepository;
import main.DomainLayer.Item.Item;
import main.DomainLayer.Item.ItemReview;
import DomainLayer.Item.ItemRepository;

public class ItemService {

    private static volatile ItemService instance;

    private final IItemRepository itemRepository;

    /**
     * Private constructor to prevent direct instantiation.
     *
     * @param itemRepository an instance of IItemRepository that handles storage of Item instances.
     */
    private ItemService(IItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /**
     * Returns the singleton instance, initializing with the default ItemRepository if necessary.
     *
     * @return the singleton ItemService
     */
    public static ItemService getInstance() {
        if (instance == null) {
            synchronized (ItemService.class) {
                if (instance == null) {
                    instance = new ItemService(new ItemRepository());
                }
            }
        }
        return instance;
    }

    /**
     * Returns the singleton instance, initializing with the provided repository if necessary.
     * Subsequent calls ignore the repository parameter once initialized.
     *
     * @param repo a custom IItemRepository implementation
     * @return the singleton ItemService
     */
    public static ItemService getInstance(IItemRepository repo) {
        if (instance == null) {
            synchronized (ItemService.class) {
                if (instance == null) {
                    instance = new ItemService(repo);
                }
            }
        }
        return instance;
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
            LoggerService.logMethodExecution("createItem", name, description);
            Item returnItem = itemRepository.createItem(name, description);
            LoggerService.logMethodExecutionEnd("createItem", returnItem);
            return returnItem;
        } catch (Exception e) {
            LoggerService.logError("createItem", e, name, description);
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
            LoggerService.logMethodExecution("getItem", itemId);
            Item returnItem = itemRepository.getItem(itemId);
            LoggerService.logMethodExecutionEnd("getItem", returnItem);
            return returnItem;
        } catch (Exception e) {
            LoggerService.logError("getItem", e, itemId);
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
            LoggerService.logMethodExecution("getAllItems");
            List<Item> returnItems = itemRepository.getAllItems();
            LoggerService.logMethodExecutionEnd("getAllItems", returnItems);
            return returnItems;
        } catch (Exception e) {
            LoggerService.logError("getAllItems", e);
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
            LoggerService.logMethodExecution("addReviewToItem", itemId, rating, reviewText);
            itemRepository.addReviewToItem(itemId, rating, reviewText);
            LoggerService.logMethodExecutionEndVoid("addReviewToItem");
        } catch (Exception e) {
            LoggerService.logError("addReviewToItem", e, itemId, rating, reviewText);
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
            LoggerService.logMethodExecution("getItemReviews", itemId);
            List<ItemReview> returnItems = itemRepository.getItemReviews(itemId);
            LoggerService.logMethodExecutionEnd("getItemReviews", returnItems);
            return returnItems;
        } catch (Exception e) {
            LoggerService.logError("getItemReviews", e, itemId);
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
            LoggerService.logMethodExecution("getItemAverageRating", itemId);
            double returnDouble = itemRepository.getItemAverageRating(itemId);
            LoggerService.logMethodExecutionEnd("getItemAverageRating", returnDouble);
            return returnDouble;
        } catch (Exception e) {
            LoggerService.logError("getItemAverageRating", e, itemId);
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
            LoggerService.logMethodExecution("deleteItem", itemId);
            itemRepository.deleteItem(itemId);
            LoggerService.logMethodExecutionEndVoid("deleteItem");
        } catch (Exception e) {
            LoggerService.logError("deleteItem", e, itemId);
            throw new RuntimeException("Error deleting item " + itemId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a list of Item objects for the given list of item IDs.
     *
     * @param itemIds the list of item IDs to fetch
     * @return an unmodifiable list of corresponding Item instances
     */
    public List<Item> getItemsByIds(List<Integer> itemIds) {
        try {
            LoggerService.logMethodExecution("getItemsByIds", itemIds);
            List<Item> result = itemRepository.getItemsByIds(itemIds);
            LoggerService.logMethodExecutionEnd("getItemsByIds", result);
            return result;
        } catch (Exception e) {
            LoggerService.logError("getItemsByIds", e, itemIds);
            throw new RuntimeException("Error fetching items: " + e.getMessage(), e);
        }
    }
}