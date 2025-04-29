package ApplicationLayer.Item;

import java.util.List;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.LoggerService;
import ApplicationLayer.OurArg;
import ApplicationLayer.OurRuntime;
import ApplicationLayer.User.UserService;
import DomainLayer.Item.IItemRepository;
import DomainLayer.Item.Item;
import DomainLayer.Item.ItemCategory;
import DomainLayer.Item.ItemReview;
import DomainLayer.Roles.PermissionsEnum;

public class ItemService {

    private final IItemRepository itemRepository;
    private AuthTokenService authTokenService;
    private UserService userService;

    public ItemService(IItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public void setServices(AuthTokenService authTokenService, UserService userService) {
        this.authTokenService = authTokenService;
        this.userService = userService;
    }

    public Integer createItem(int shopId, String name, String description, Integer category, String token) {
        try {
            LoggerService.logMethodExecution("createItem", name, description, category);

            if (name == null || name.isEmpty()) {
                throw new OurArg("Item name cannot be null or empty");
            }
            if (description == null || description.isEmpty()) {
                throw new OurArg("Item description cannot be null or empty");
            }
            if (category == null || category < 0 || category >= ItemCategory.values().length) {
                throw new OurArg("Item category is invalid");
            }
            if (shopId < 0) {
                throw new OurArg("Shop ID cannot be negative");
            }

            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)) {
                throw new OurArg("User does not have permission to add item to shop " + shopId);
            }

            Integer returnItemId = itemRepository.createItem(name, description, category);
            LoggerService.logMethodExecutionEnd("createItem", itemRepository.getItem(returnItemId));
            return returnItemId;

        } catch (OurArg e) {
            LoggerService.logDebug("createItem", e);
            return null; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("createItem", e, name, description, category);
            throw new OurRuntime("Error creating item: " + e.getMessage(), e);
        }
    }

    public Item getItem(int itemId, String token) {
        try {
            LoggerService.logMethodExecution("getItem", itemId);
            if (itemId < 0) {
                throw new OurArg("Item ID cannot be negative");
            }
            authTokenService.ValidateToken(token);
            Item returnItem = itemRepository.getItem(itemId);
            LoggerService.logMethodExecutionEnd("getItem", returnItem);
            return returnItem;
        } catch (OurArg e) {
            LoggerService.logDebug("getItem", e);
            return null; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("getItem", e, itemId);
            throw new OurRuntime("Error retrieving item with id " + itemId + ": " + e.getMessage(), e);
        }
    }

    public List<Item> getAllItems(String token) {
        try {
            LoggerService.logMethodExecution("getAllItems");
            authTokenService.ValidateToken(token);
            List<Item> returnItems = itemRepository.getAllItems();
            LoggerService.logMethodExecutionEnd("getAllItems", returnItems);
            return returnItems;
        } catch (OurArg e) {
            LoggerService.logDebug("getAllItems", e);
            return null; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("getAllItems", e);
            throw new OurRuntime("Error retrieving all items: " + e.getMessage(), e);
        }
    }

    public void addReviewToItem(int itemId, int rating, String reviewText, String token) {
        try {
            LoggerService.logMethodExecution("addReviewToItem", itemId, rating, reviewText);
            if (itemId < 0) {
                throw new OurArg("Item ID cannot be negative");
            }
            if (rating < 1 || rating > 5) {
                throw new OurArg("Rating must be between 1 and 5");
            }
            if (reviewText == null || reviewText.isEmpty()) {
                throw new OurArg("Review text cannot be null or empty");
            }
            authTokenService.ValidateToken(token);
            itemRepository.addReviewToItem(itemId, rating, reviewText);
            LoggerService.logMethodExecutionEndVoid("addReviewToItem");
        } catch (OurArg e) {
            LoggerService.logDebug("addReviewToItem", e);
            // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("addReviewToItem", e, itemId, rating, reviewText);
            throw new OurRuntime("Error adding review to item " + itemId + ": " + e.getMessage(), e);
        }
    }

    public List<ItemReview> getItemReviews(int itemId, String token) {
        try {
            LoggerService.logMethodExecution("getItemReviews", itemId);
            if (itemId < 0) {
                throw new OurArg("Item ID cannot be negative");
            }
            authTokenService.ValidateToken(token);
            List<ItemReview> returnItems = itemRepository.getItemReviews(itemId);
            LoggerService.logMethodExecutionEnd("getItemReviews", returnItems);
            return returnItems;
        } catch (OurArg e) {
            LoggerService.logDebug("getItemReviews", e);
            return null; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("getItemReviews", e, itemId);
            throw new OurRuntime("Error retrieving reviews for item " + itemId + ": " + e.getMessage(), e);
        }
    }

    public double getItemAverageRating(int itemId, String token) {
        try {
            LoggerService.logMethodExecution("getItemAverageRating", itemId);
            if (itemId < 0) {
                throw new OurArg("Item ID cannot be negative");
            }
            authTokenService.ValidateToken(token);
            double returnDouble = itemRepository.getItemAverageRating(itemId);
            LoggerService.logMethodExecutionEnd("getItemAverageRating", returnDouble);
            return returnDouble;
        } catch (OurArg e) {
            LoggerService.logDebug("getItemAverageRating", e);
            return -1.0; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("getItemAverageRating", e, itemId);
            throw new OurRuntime("Error retrieving average rating for item " + itemId + ": " + e.getMessage(), e);
        }
    }

    public List<Item> getItemsByIds(List<Integer> itemIds, String token) {
        try {
            LoggerService.logMethodExecution("getItemsByIds", itemIds);
            if (itemIds == null || itemIds.isEmpty()) {
                throw new OurArg("Item IDs list cannot be null or empty");
            }
            for (Integer itemId : itemIds) {
                if (itemId < 0) {
                    throw new OurArg("Item ID cannot be negative");
                }
            }
            authTokenService.ValidateToken(token);
            List<Item> result = itemRepository.getItemsByIds(itemIds);
            LoggerService.logMethodExecutionEnd("getItemsByIds", result);
            return result;
        } catch (OurArg e) {
            LoggerService.logDebug("getItemsByIds", e);
            return null; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("getItemsByIds", e, itemIds);
            throw new OurRuntime("Error fetching items: " + e.getMessage(), e);
        }
    }
}
