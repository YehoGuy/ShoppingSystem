package ApplicationLayer.Shop;

import java.util.*;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.Item.ItemService;
import ApplicationLayer.LoggerService;
import ApplicationLayer.OurArg;
import ApplicationLayer.OurRuntime;
import ApplicationLayer.Purchase.ShippingMethod;
import ApplicationLayer.User.UserService;
import DomainLayer.Item.Item;
import DomainLayer.Item.ItemCategory;
import DomainLayer.Roles.PermissionsEnum;
import DomainLayer.Roles.Role;
import DomainLayer.Shop.IShopRepository;
import DomainLayer.Shop.PurchasePolicy;
import DomainLayer.Shop.Shop;

public class ShopService {
    private final IShopRepository shopRepository;
    private AuthTokenService authTokenService;
    private ItemService itemService;
    private UserService userService;

    public ShopService(IShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    public void setServices(AuthTokenService authTokenService, ItemService itemService, UserService userService) {
        this.authTokenService = authTokenService;
        this.itemService = itemService;
        this.userService = userService;
    }

    public Shop createShop(String name, PurchasePolicy policy, ShippingMethod shippingMethod, String token) {
        try {
            LoggerService.logMethodExecution("createShop", name, policy);
            int userId = authTokenService.ValidateToken(token);
            userService.validateMemberId(userId);
            Shop shop = shopRepository.createShop(name, policy, shippingMethod);
            Role founderRole = new Role(userId, shop.getId(), null);
            founderRole.setFoundersPermissions();
            userService.addRole(userId, founderRole);
            LoggerService.logMethodExecutionEnd("createShop", shop);
            return shop;
        } catch (OurArg e) {
            LoggerService.logDebug("createShop", e);
            return null; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("createShop", e, name, policy);
            throw new OurRuntime("Error creating shop: " + e.getMessage(), e);
        }
    }

    public Shop getShop(int shopId, String token) {
        try {
            LoggerService.logMethodExecution("getShop", shopId);
            authTokenService.ValidateToken(token);
            Shop shop = shopRepository.getShop(shopId);
            LoggerService.logMethodExecutionEnd("getShop", shop);
            return shop;
        } catch (OurArg e) {
            LoggerService.logDebug("getShop", e);
            return null; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("getShop", e, shopId);
            throw new OurRuntime("Error retrieving shop with id " + shopId + ": " + e.getMessage(), e);
        }
    }

    public List<Shop> getAllShops(String token) {
        try {
            LoggerService.logMethodExecution("getAllShops");
            authTokenService.ValidateToken(token);
            List<Shop> returnShops = shopRepository.getAllShops();
            LoggerService.logMethodExecutionEnd("getAllShops", returnShops);
            return returnShops;
        } catch (OurArg e) {
            LoggerService.logDebug("getAllShops", e);
            return null; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("getAllShops", e);
            throw new OurRuntime("Error retrieving all shops: " + e.getMessage(), e);
        }
    }

    public void updatePurchasePolicy(int shopId, PurchasePolicy newPolicy, String token) {
        try {
            LoggerService.logMethodExecution("updatePurchasePolicy", shopId, newPolicy);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                throw new OurArg("User does not have permission to update purchase policy for shop " + shopId);
            }
            shopRepository.updatePurchasePolicy(shopId, newPolicy);
            LoggerService.logMethodExecutionEndVoid("updatePurchasePolicy");
        } catch (OurArg e) {
            LoggerService.logDebug("updatePurchasePolicy", e);
        } catch (Exception e) {
            LoggerService.logError("updatePurchasePolicy", e, shopId, newPolicy);
            throw new OurRuntime("Error updating purchase policy for shop " + shopId + ": " + e.getMessage(), e);
        }
    }


    /**
     * Sets the global discount for the specified shop.
     *
     * @param shopId   the shop id.
     * @param discount the global discount value.
     */
    public void setGlobalDiscount(int shopId, int discount, String token) {
        try {
            LoggerService.logMethodExecution("setGlobalDiscount", shopId, discount);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                throw new OurArg("User does not have permission to update discount for shop " + shopId);
            }
            shopRepository.setGlobalDiscount(shopId, discount);
            LoggerService.logMethodExecutionEndVoid("setGlobalDiscount");
        } catch (OurArg e) {
            LoggerService.logDebug("setGlobalDiscount", e);
        } catch (Exception e) {
            LoggerService.logError("setGlobalDiscount", e, shopId, discount);
            throw new OurRuntime("Error setting global discount for shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    
    public void setDiscountForItem(int shopId, int itemId, int discount, String token) {
        try {
            LoggerService.logMethodExecution("setDiscountForItem", shopId, itemId, discount);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                throw new OurArg("User does not have permission to update item discount for shop " + shopId);
            }
            shopRepository.setDiscountForItem(shopId, itemId, discount);
            LoggerService.logMethodExecutionEndVoid("setDiscountForItem");
        } catch (OurArg e) {
            LoggerService.logDebug("setDiscountForItem", e);
        } catch (Exception e) {
            LoggerService.logError("setDiscountForItem", e, shopId, itemId, discount);
            throw new OurRuntime("Error setting discount for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    
    public void addBundleDiscount(int shopId, Map<Integer, Integer> basket, int discount, String token) {
        try {
            LoggerService.logMethodExecution("addBundleDiscount", shopId, basket, discount);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                throw new OurArg("User does not have permission to add bundle discount to shop " + shopId);
            }
            shopRepository.addBundleDiscount(shopId, basket, discount);
            LoggerService.logMethodExecutionEndVoid("addBundleDiscount");
        } catch (OurArg e) {
            LoggerService.logDebug("addBundleDiscount", e);
        } catch (Exception e) {
            LoggerService.logError("addBundleDiscount", e, shopId, basket, discount);
            throw new OurRuntime("Error adding bundle discount to shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    
    public void addReviewToShop(int shopId, int rating, String reviewText, String token) {
        try {
            LoggerService.logMethodExecution("addReviewToShop", shopId, rating, reviewText);
            Integer userId = authTokenService.ValidateToken(token);
            userService.validateMemberId(userId);
            shopRepository.addReviewToShop(shopId, userId, rating, reviewText);
            LoggerService.logMethodExecutionEndVoid("addReviewToShop");
        } catch (OurArg e) {
            LoggerService.logDebug("addReviewToShop", e);
        } catch (Exception e) {
            LoggerService.logError("addReviewToShop", e, shopId, rating, reviewText);
            throw new OurRuntime("Error adding review to shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    
    public Double getShopAverageRating(int shopId, String token) {
        try {
            LoggerService.logMethodExecution("getShopAverageRating", shopId);
            authTokenService.ValidateToken(token);
            double rating = shopRepository.getShopAverageRating(shopId);
            LoggerService.logMethodExecutionEnd("getShopAverageRating", rating);
            return rating;
        } catch (OurArg e) {
            LoggerService.logDebug("getShopAverageRating", e);
            return null; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("getShopAverageRating", e, shopId);
            throw new OurRuntime("Error retrieving average rating for shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    
    public void addItemToShop(int shopId, String name, String desc, int quantity, int price, String token) {
        try {
            LoggerService.logMethodExecution("addItemToShop", shopId, name, quantity, price);
            int userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)) {
                throw new OurArg("User does not have permission to add item to shop " + shopId);
            }
            int itemId = itemService.createItem(shopId, name, desc, userId, token);
            shopRepository.addItemToShop(shopId, itemId, quantity, price);
            LoggerService.logMethodExecutionEndVoid("addItemToShop");
        } catch (OurArg e) {
            LoggerService.logDebug("addItemToShop", e);
        } catch (Exception e) {
            LoggerService.logError("addItemToShop", e, shopId, name, quantity, price);
            throw new OurRuntime("Error adding item to shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    

    /**
     * Adds a given quantity of an item to the specified shop.
     *
     * @param shopId   the shop id.
     * @param itemId   the item id.
     * @param quantity the quantity to add.
     */
    public void addSupplyToItem(int shopId, int itemId, int quantity, String token) {
        try {
            LoggerService.logMethodExecution("addSupplyToItem", shopId, itemId, quantity);
            authTokenService.ValidateToken(token);
            shopRepository.addSupplyToItem(shopId, itemId, quantity);
            LoggerService.logMethodExecutionEndVoid("addSupplyToItem");
        } catch (OurArg e) {
            LoggerService.logDebug("addSupplyToItem", e);
        } catch (Exception e) {
            LoggerService.logError("addSupplyToItem", e, shopId, itemId, quantity);
            throw new OurRuntime("Error adding supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    
    public void updateItemPriceInShop(int shopId, int itemId, int price, String token) {
        try {
            LoggerService.logMethodExecution("updateItemPriceInShop", shopId, itemId, price);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)) {
                throw new OurArg("User does not have permission to update price for item " + itemId + " in shop " + shopId);
            }
            shopRepository.updateItemPriceInShop(shopId, itemId, price);
            LoggerService.logMethodExecutionEndVoid("updateItemPriceInShop");
        } catch (OurArg e) {
            LoggerService.logDebug("updateItemPriceInShop", e);
        } catch (Exception e) {
            LoggerService.logError("updateItemPriceInShop", e, shopId, itemId, price);
            throw new OurRuntime("Error updating price for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    

    /**
     * Removes an item from the specified shop.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     */
    public void removeItemFromShop(int shopId, int itemId, String token) {
        try {
            LoggerService.logMethodExecution("removeItemFromShop", shopId, itemId);
            int userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)) {
                throw new OurArg("User does not have permission to remove item " + itemId + " from shop " + shopId);
            }
            shopRepository.removeItemFromShop(shopId, itemId);
            LoggerService.logMethodExecutionEndVoid("removeItemFromShop");
        } catch (OurArg e) {
            LoggerService.logDebug("removeItemFromShop", e);
        } catch (Exception e) {
            LoggerService.logError("removeItemFromShop", e, shopId, itemId);
            throw new OurRuntime("Error removing item from shop: " + e.getMessage(), e);
        }
    }
    
    public Integer getItemQuantityFromShop(int shopId, int itemId, String token) {
        try {
            LoggerService.logMethodExecution("getItemQuantityFromShop", shopId, itemId);
            authTokenService.ValidateToken(token);
            int quantity = shopRepository.getItemQuantityFromShop(shopId, itemId);
            LoggerService.logMethodExecutionEnd("getItemQuantityFromShop", quantity);
            return quantity;
        } catch (OurArg e) {
            LoggerService.logDebug("getItemQuantityFromShop", e);
            return null; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("getItemQuantityFromShop", e, shopId, itemId);
            throw new OurRuntime("Error retrieving item quantity: " + e.getMessage(), e);
        }
    }
    
    public void closeShop(Integer shopId, String token) {
        try {
            LoggerService.logMethodExecution("closeShop", shopId);
            int userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.closeShop, shopId)) {
                OurArg e = new OurArg("User does not have permission to close shop " + shopId);
                LoggerService.logDebug("closeShop", e);
                throw e;
            }
            shopRepository.closeShop(shopId);
            LoggerService.logMethodExecutionEndVoid("closeShop");
        } catch (OurArg e) {
            LoggerService.logDebug("closeShop", e);
        } catch (Exception e) {
            LoggerService.logError("closeShop", e, shopId);
            throw new OurRuntime("Error closing shop: " + e.getMessage(), e);
        }
    }
    
    public boolean checkSupplyAvailability(Integer shopId, Integer itemId, String token) {
        try {
            LoggerService.logMethodExecution("checkSupplyAvailability", shopId, itemId);
            authTokenService.ValidateToken(token);
            boolean available = shopRepository.checkSupplyAvailability(shopId, itemId);
            LoggerService.logMethodExecutionEnd("checkSupplyAvailability", available);
            return available;
        } catch (OurArg e) {
            LoggerService.logDebug("checkSupplyAvailability", e);
            return false;
        } catch (Exception e) {
            LoggerService.logError("checkSupplyAvailability", e, shopId, itemId);
            throw new OurRuntime("Error checking supply availability: " + e.getMessage(), e);
        }
    }
    
    public Double purchaseItems(Map<Integer, Integer> purchaseLists, Integer shopId) {
        try {
            LoggerService.logMethodExecution("purchaseItems", purchaseLists, shopId);
            double totalPrice = shopRepository.purchaseItems(purchaseLists, shopId);
            LoggerService.logMethodExecutionEnd("purchaseItems", totalPrice);
            return totalPrice;
        } catch (OurArg e) {
            LoggerService.logDebug("purchaseItems", e);
            return null; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("purchaseItems", e, purchaseLists, shopId);
            throw new OurRuntime("Error purchasing items from shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    
    public void rollBackPurchase(Map<Integer, Integer> purchaseLists, Integer shopId) {
        try {
            LoggerService.logMethodExecution("rollBackPurchase", purchaseLists, shopId);
            shopRepository.rollBackPurchase(purchaseLists, shopId);
            LoggerService.logMethodExecutionEndVoid("rollBackPurchase");
        } catch (OurArg e) {
            LoggerService.logDebug("rollBackPurchase", e);
        } catch (Exception e) {
            LoggerService.logError("rollBackPurchase", e, purchaseLists, shopId);
            throw new OurRuntime("Error rolling back purchase from shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    
    public boolean checkSupplyAvailabilityAndAcquire(Integer shopId, Integer itemId, Integer supply) {
        try {
            LoggerService.logMethodExecution("checkSupplyAvailabilityAndAcquire", shopId, itemId, supply);
            boolean available = shopRepository.checkSupplyAvailabilityAndAqcuire(shopId, itemId, supply);
            LoggerService.logMethodExecutionEnd("checkSupplyAvailabilityAndAcquire", available);
            return available;
        } catch (OurArg e) {
            LoggerService.logDebug("checkSupplyAvailabilityAndAcquire", e);
            return false; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("checkSupplyAvailabilityAndAcquire", e, shopId, itemId, supply);
            throw new OurRuntime("Error acquiring supply for item " + itemId + ": " + e.getMessage(), e);
    
        }
    }

    public void addSupply(Integer shopId, Integer itemId, Integer supply) {
        try {
            LoggerService.logMethodExecution("addSupply", shopId, itemId, supply);
            shopRepository.addSupply(shopId, itemId, supply);
            LoggerService.logMethodExecutionEndVoid("addSupply");
        } catch (OurArg e) {
            LoggerService.logDebug("addSupply", e);
        } catch (Exception e) {
            LoggerService.logError("addSupply", e, shopId, itemId, supply);
            throw new OurRuntime("Error adding supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    
    public void removeSupply(Integer shopId, Integer itemId, Integer supply, String token) {
        try {
            LoggerService.logMethodExecution("removeSupply", shopId, itemId, supply);
            authTokenService.ValidateToken(token);
            shopRepository.removeSupply(shopId, itemId, supply);
            LoggerService.logMethodExecutionEndVoid("removeSupply");
        } catch (OurArg e) {
            LoggerService.logDebug("removeSupply", e);
        } catch (Exception e) {
            LoggerService.logError("removeSupply", e, shopId, itemId, supply);
            throw new OurRuntime("Error removing supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    

    public boolean checkPolicy(HashMap<Integer, HashMap<Integer, Integer>> cart, String token) {
        try {
            LoggerService.logMethodExecution("checkPolicy", cart);
            authTokenService.ValidateToken(token);
            boolean result = shopRepository.checkPolicy(cart, token);
            LoggerService.logMethodExecutionEnd("checkPolicy", result);
            return result;
        } catch (OurArg e) {
            LoggerService.logDebug("checkPolicy", e);
            return false;
        } catch (Exception e) {
            LoggerService.logError("checkPolicy", e, cart);
            throw new OurRuntime("Error checking policy: " + e.getMessage(), e);
        }
    }
    

    /**
     * Retrieves a list of items available in the specified shop.
     *
     * @param shopId the shop id.
     * @return a list of Item instances.
     */
    public List<Item> getItemsByShop(Integer shopId, String token) {
        try {
            LoggerService.logMethodExecution("getItemsByShop", shopId);
            authTokenService.ValidateToken(token);
            List<Integer> itemIds = shopRepository.getItemsByShop(shopId);
            LoggerService.logMethodExecutionEnd("getItemsByShop", itemIds);
            List<Item> items = itemService.getItemsByIds(itemIds, token);
            LoggerService.logMethodExecutionEnd("getItemsByShop [fetched items]", items);
            return items;
        } catch (OurArg e) {
            LoggerService.logDebug("getItemsByShop", e);
            return null; // we will change to return DTO with appropriate error message
        } catch (Exception e) {
            LoggerService.logError("getItemsByShop", e, shopId);
            throw new OurRuntime("Error retrieving items for shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    

    /**
     * Retrieves a list of all items available in all shops.
     *
     * @return a list of Item instances.
     */
    public List<Item> getItems(String token) {
        try {
            LoggerService.logMethodExecution("getItems");
            authTokenService.ValidateToken(token);
            List<Integer> itemIds = shopRepository.getItems();
            LoggerService.logMethodExecutionEnd("getItems [IDs]", itemIds);
            List<Item> items = itemService.getItemsByIds(itemIds, token);
            LoggerService.logMethodExecutionEnd("getItems [fetched items]", items);
            return items;
        } catch (OurArg e) {
            LoggerService.logDebug("getItems", e);
            return null;
        } catch (Exception e) {
            LoggerService.logError("getItems", e);
            throw new OurRuntime("Error retrieving all items: " + e.getMessage(), e);
        }
    }
    

    /**
     * Search across all shops (no single‑shop focus) for items matching any
     * combination of these optional criteria.
     */
    public List<Item> searchItems(String name, ItemCategory category, List<String> keywords,
                              Integer minPrice, Integer maxPrice,
                              Double minProductRating, Double minShopRating, String token) {
        try {
            LoggerService.logMethodExecution("searchItems", name, category, keywords, minPrice, maxPrice, minProductRating, minShopRating);
            authTokenService.ValidateToken(token);
            List<Item> results = new ArrayList<>();
            for (Shop shop : getAllShops(token)) {
                if (minShopRating != null && shop.getAverageRating() < minShopRating) continue;
                results.addAll(filterItemsInShop(shop, name, category, keywords, minPrice, maxPrice, minProductRating, token));
            }
            LoggerService.logMethodExecutionEnd("searchItems", results);
            return results;
        } catch (OurArg e) {
            LoggerService.logDebug("searchItems", e);
            return null;
        } catch (Exception e) {
            LoggerService.logError("searchItems", e, name, category, keywords, minPrice, maxPrice, minProductRating, minShopRating);
            throw new OurRuntime("Error searching items: " + e.getMessage(), e);
        }
    }


    /**
     * Search within a specific shop for items matching any combination of these optional criteria.
     */
    public List<Item> searchItemsInShop(Integer shopId, String name, ItemCategory category, List<String> keywords,
                                    Integer minPrice, Integer maxPrice, Double minProductRating, String token) {
    try {
        LoggerService.logMethodExecution("searchItemsInShop", shopId, name, category, keywords, minPrice, maxPrice, minProductRating);
        authTokenService.ValidateToken(token);
        Shop shop = getShop(shopId, token);
        List<Item> results = filterItemsInShop(shop, name, category, keywords, minPrice, maxPrice, minProductRating, token);
        LoggerService.logMethodExecutionEnd("searchItemsInShop", results);
        return results;
    } catch (OurArg e) {
        LoggerService.logDebug("searchItemsInShop", e);
        return null; // we will change to return DTO with appropriate error message
    } catch (Exception e) {
        LoggerService.logError("searchItemsInShop", e, shopId, name, category, keywords, minPrice, maxPrice, minProductRating);
        throw new OurRuntime("Error searching items in shop: " + e.getMessage(), e);
    }
}


    /**
     * Private helper: apply all item‑level filters within one shop.
     * if a filter is null, it is ignored.
     */
    private List<Item> filterItemsInShop(Shop shop, String name, ItemCategory category, List<String> keywords,
                                     Integer minPrice, Integer maxPrice, Double minProductRating, String token) {
    try {
        LoggerService.logMethodExecution("filterItemsInShop", shop, name, category, keywords, minPrice, maxPrice, minProductRating);

        List<Item> results = new ArrayList<>();
        List<Item> shopItems = getItemsByShop(shop.getId(), token);
        for (Item item : shopItems) {
            if (name != null && !item.getName().toLowerCase().contains(name.toLowerCase())) continue;
            if (category != null && item.getCategory() != category) continue;
            if (keywords != null && !keywords.isEmpty()) {
                String ln = item.getName().toLowerCase();
                String ld = item.getDescription().toLowerCase();
                boolean match = keywords.stream().map(String::toLowerCase).anyMatch(kw -> ln.contains(kw) || ld.contains(kw));
                if (!match) continue;
            }
            int price = shop.getItemPrice(item.getId());
            if (minPrice != null && price < minPrice) continue;
            if (maxPrice != null && price > maxPrice) continue;
            double rating = item.getAverageRating();
            if (minProductRating != null && rating < minProductRating) continue;
            results.add(item);
        }
        LoggerService.logMethodExecutionEnd("filterItemsInShop", results);
        return results;
    } catch (OurArg e) {
        LoggerService.logDebug("filterItemsInShop", e);
        return null; // we will change to return DTO with appropriate error message
    } catch (Exception e) {
        LoggerService.logError("filterItemsInShop", e, shop, name, category, keywords, minPrice, maxPrice, minProductRating);
        throw new OurRuntime("Error filtering items in shop " + shop.getId() + ": " + e.getMessage(), e);
    }
}

}

