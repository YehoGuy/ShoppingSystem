package com.example.app.ApplicationLayer.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.LoggerService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Item.IItemRepository;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Item.ItemReview;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
@Service
public class ItemService {

    // ===== dependencies =====
    private final IItemRepository itemRepository;
    private final AuthTokenService authTokenService;
    private final UserService     userService;

    // ===== constructor DI =====
    public ItemService(IItemRepository itemRepository,
                       AuthTokenService authTokenService,
                       UserService userService) {
        this.itemRepository   = itemRepository;
        this.authTokenService = authTokenService;
        this.userService      = userService;
    }

    /**
     * Creates a new item with the specified parameters.
     *
     * @param name        the item name.
     * @param description the item description.
     * @return the newly created Item.
     */
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
                throw new OurArg("Item category cannot be null");
            }
            if (shopId < 0) {
                throw new OurArg("Shop ID cannot be negative");
            }
            Integer userId = authTokenService.ValidateToken(token);
            if(!userService.hasPermission(userId,PermissionsEnum.manageItems,shopId)){
                throw new OurRuntime("User does not have permission to add item to shop " + shopId);
            }
            Integer returnItemId = itemRepository.createItem(name, description, category);
            LoggerService.logMethodExecutionEnd("createItem", itemRepository.getItem(returnItemId));
            return returnItemId;
        } catch (OurArg e) {
            LoggerService.logDebug("createItem", e);
            throw new OurArg("createItem: " + e.getMessage(), e);
        }
        catch (OurRuntime e) {
            LoggerService.logDebug("createItem", e);
            throw new OurRuntime("createItem: " + e.getMessage(), e);
        }
        catch (Exception e) {
            LoggerService.logError("createItem", e, name, description, category);
            throw new OurRuntime("createItem: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves an item by its identifier.
     *
     * @param itemId the item id.
     * @return the Item instance.
     */
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
            throw new OurArg("getItem " + itemId + ": " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getItem", e);
            throw new OurRuntime("getItem " + itemId + ": " + e.getMessage(), e);
        } 
        catch (Exception e) {
            LoggerService.logError("getItem", e, itemId);
            throw new OurRuntime("getItem " + itemId + ": " + e.getMessage(), e);
        }
    }

    /** 
     * Retrieves a map of item IDs to their corresponding categories.
     * 
     * * @param itemIds the list of item IDs to fetch categories for, <itemId, quantity (not used)> 
     * @return a map where the keys are item IDs and the values are ItemCategory instances.
     */
    public Map<Integer,ItemCategory> getItemdId2Cat(Map<Integer, Integer> itemIds){ 
        try {
            Map<Integer, ItemCategory> itemId2Cat = new HashMap<>();
            for (Integer itemId : itemIds.keySet()) {
                Item item = itemRepository.getItem(itemId);
                if (item != null) {
                    itemId2Cat.put(itemId, item.getCategory());
                }
            }
            return itemId2Cat;
        } catch (OurArg e) {
            LoggerService.logDebug("getItemdId2Cat", e);
            throw new OurArg("getItemdId2Cat: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getItemdId2Cat", e);
            throw new OurRuntime("getItemdId2Cat: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getItemdId2Cat", e, itemIds);
            throw new OurRuntime("getItemdId2Cat: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a list of all items.
     *
     * @return an unmodifiable list of Item instances.
     */
    public List<Item> getAllItems(String token) {
        try {
            LoggerService.logMethodExecution("getAllItems");
            authTokenService.ValidateToken(token);
            List<Item> returnItems = itemRepository.getAllItems();
            LoggerService.logMethodExecutionEnd("getAllItems", returnItems);
            return returnItems;
        } catch (OurArg e) {
            LoggerService.logDebug("getAllItems", e);
            throw new OurArg("getAllItems: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getAllItems", e);
            throw new OurRuntime("getAllItems: " + e.getMessage(), e);
        }
        catch (Exception e) {
            LoggerService.logError("getAllItems", e);
            throw new OurRuntime("getAllItems: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a review to the specified item.
     *
     * @param itemId     the item id.
     * @param rating     the review rating.
     * @param reviewText the review text.
     */
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
            throw new OurArg("addReviewToItem " + itemId + ": " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("addReviewToItem", e);
            throw new OurRuntime("addReviewToItem " + itemId + ": " + e.getMessage(), e);
        } 
        catch (Exception e) {
            LoggerService.logError("addReviewToItem", e, itemId, rating, reviewText);
            throw new OurRuntime("addReviewToItem " + itemId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the reviews for the specified item.
     *
     * @param itemId the item id.
     * @return a list of ItemReview instances.
     */
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
            throw new OurArg("getItemReviews " + itemId + ": " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getItemReviews", e);
            throw new OurRuntime("getItemReviews " + itemId + ": " + e.getMessage(), e);
        }
        catch (Exception e) {
            LoggerService.logError("getItemReviews", e, itemId);
            throw new OurRuntime("getItemReviews " + itemId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the average rating for the specified item.
     *
     * @param itemId the item id.
     * @return the average rating, or -1.0 if no reviews.
     */
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
            throw new OurArg("getItemAverageRating " + itemId + ": " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getItemAverageRating", e);
            throw new OurRuntime("getItemAverageRating " + itemId + ": " + e.getMessage(), e);
        }
        catch (Exception e) {
            LoggerService.logError("getItemAverageRating", e, itemId);
            throw new OurRuntime("getItemAverageRating " + itemId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a list of Item objects for the given list of item IDs.
     *
     * @param itemIds the list of item IDs to fetch
     * @return an unmodifiable list of corresponding Item instances
     */
    public List<Item> getItemsByIds(List<Integer> itemIds , String token) {
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
            throw new OurArg("getItemsByIds: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getItemsByIds", e);
            throw new OurRuntime("getItemsByIds: " + e.getMessage(), e);
        }
        catch (Exception e) {
            LoggerService.logError("getItemsByIds", e, itemIds);
            throw new OurRuntime("getItemsByIds: " + e.getMessage(), e);
        }
    }

    public List<Integer> getItemsByCategory(ItemCategory category, String token) {
        try {
            LoggerService.logMethodExecution("getItemsByCategory", category);
            if (category == null) {
                throw new OurArg("Item category cannot be null");
            }
            authTokenService.ValidateToken(token);
            List<Integer> returnItems = itemRepository.getItemsByCategory(category);
            LoggerService.logMethodExecutionEnd("getItemsByCategory", returnItems);
            return returnItems;
        } catch (OurArg e) {
            LoggerService.logDebug("getItemsByCategory", e);
            throw new OurArg("getItemsByCategory " + category + ": " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getItemsByCategory", e);
            throw new OurRuntime("getItemsByCategory " + category + ": " + e.getMessage(), e);
        }
        catch (Exception e) {
            LoggerService.logError("getItemsByCategory", e, category);
            throw new OurRuntime("getItemsByCategory " + category + ": " + e.getMessage(), e);
        }
    }

}