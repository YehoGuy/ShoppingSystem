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

    /**
     * Constructor for ShopService.
     *
     * @param shopRepository an instance of IShopRepository that handles storage of Shop instances.
     */
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
            throw new OurArg("createShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("createShop", e);
            throw new OurRuntime("createShop" + e.getMessage());
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
            throw new OurArg("getShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getShop", e);
            throw new OurRuntime("getShop" + e.getMessage());
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
            throw new OurArg("getAllShops" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getAllShops", e);
            throw new OurRuntime("getAllShops" + e.getMessage());
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
            throw new OurArg("updatePurchasePolicy" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("updatePurchasePolicy", e);
            throw new OurRuntime("updatePurchasePolicy" + e.getMessage());
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
    public void setGlobalDiscount(int shopId, int discount, boolean isDouble, String token) {
        try {
            LoggerService.logMethodExecution("setGlobalDiscount", shopId, discount);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                throw new OurArg("User does not have permission to update discount for shop " + shopId);
            }
            shopRepository.setGlobalDiscount(shopId, discount, isDouble);
            LoggerService.logMethodExecutionEndVoid("setGlobalDiscount");
        } catch (OurArg e) {
            LoggerService.logDebug("setGlobalDiscount", e);
            throw new OurArg("setGlobalDiscount" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("setGlobalDiscount", e);
            throw new OurRuntime("setGlobalDiscount" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("setGlobalDiscount", e, shopId, discount);
            throw new OurRuntime("Error setting global discount for shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Removes the global discount for the specified shop.
     *
     * @param shopId the shop id.
     */
    public void removeGlobalDiscount(int shopId, String token) {
        try {
            LoggerService.logMethodExecution("removeGlobalDiscount", shopId);
            Integer userId = authTokenService.ValidateToken(token);
            if(!userService.hasPermission(userId,PermissionsEnum.setPolicy,shopId)){
                throw new RuntimeException("User does not have permission to remove discount for shop " + shopId);
            }
            shopRepository.removeGlobalDiscount(shopId);
            LoggerService.logMethodExecutionEndVoid("removeGlobalDiscount");
        } catch (Exception e) {
            LoggerService.logError("removeGlobalDiscount", e, shopId);
            throw new RuntimeException("Error removing global discount for shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Sets a discount for a specific item in the specified shop.
     *
     * @param shopId   the shop id.
     * @param itemId   the item id.
     * @param discount the discount value.
     */
    public void setDiscountForItem(int shopId, int itemId, int discount, boolean isDouble, String token) {
        try {
            LoggerService.logMethodExecution("setDiscountForItem", shopId, itemId, discount);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                throw new OurArg("User does not have permission to update item discount for shop " + shopId);
            }
            shopRepository.setDiscountForItem(shopId, itemId, discount, isDouble);
            LoggerService.logMethodExecutionEndVoid("setDiscountForItem");
        } catch (OurArg e) {
            LoggerService.logDebug("setDiscountForItem", e);
            throw new OurArg("setDiscountForItem" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("setDiscountForItem", e);
            throw new OurRuntime("setDiscountForItem" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("setDiscountForItem", e, shopId, itemId, discount);
            throw new OurRuntime("Error setting discount for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Removes the discount for a specific item in the specified shop.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     */
    public void removeDiscountForItem(int shopId, int itemId, String token) {
        try {
            LoggerService.logMethodExecution("removeDiscountForItem", shopId, itemId);
            Integer userId = authTokenService.ValidateToken(token);
            if(!userService.hasPermission(userId,PermissionsEnum.setPolicy,shopId)){
                throw new RuntimeException("User does not have permission to remove discount for item " + itemId + " in shop " + shopId);
            }
            shopRepository.removeDiscountForItem(shopId, itemId);
            LoggerService.logMethodExecutionEndVoid("removeDiscountForItem");
        } catch (Exception e) {
            LoggerService.logError("removeDiscountForItem", e, shopId, itemId);
            throw new RuntimeException("Error removing discount for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * set a discount for category in the specified shop.
     * 
     * @param shopId   the shop id.
     * @param category the item category.
     * @param discount the discount value.
     * @param isDouble whether to apply the discount as a double discount.
     */
    public void setCategoryDiscount(int shopId, ItemCategory category, int discount, boolean isDouble, String token) {
        try {
            LoggerService.logMethodExecution("setCategoryDiscount", shopId, category, discount);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                throw new OurArg("User does not have permission to add bundle discount to shop " + shopId);
            }
            shopRepository.setCategoryDiscount(shopId, category, discount, isDouble);
            LoggerService.logMethodExecutionEndVoid("setCategoryDiscount");
        } catch (Exception e) {
            LoggerService.logError("setCategoryDiscount", e, shopId, category, discount);
            throw new RuntimeException("Error setting category discount for item " + category + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Removes the discount for a specific item category in the specified shop.
     *
     * @param shopId   the shop id.
     * @param category the item category.
     */
    public void removeCategoryDiscount(int shopId, ItemCategory category, String token) {
        try {
            LoggerService.logMethodExecution("removeCategoryDiscount", shopId, category);
            Integer userId = authTokenService.ValidateToken(token);
            if(!userService.hasPermission(userId,PermissionsEnum.setPolicy,shopId)){
                throw new RuntimeException("User does not have permission to remove discount for item " + category + " in shop " + shopId);
            }
            shopRepository.removeCategoryDiscount(shopId, category);
            LoggerService.logMethodExecutionEndVoid("removeCategoryDiscount");
        } catch (Exception e) {
            LoggerService.logError("removeCategoryDiscount", e, shopId, category);
            throw new RuntimeException("Error removing category discount for item " + category + " in shop " + shopId + ": " + e.getMessage(), e);
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
            throw new OurArg("addReviewToShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("addReviewToShop", e);
            throw new OurRuntime("addReviewToShop" + e.getMessage());
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
            throw new OurArg("getShopAverageRating" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getShopAverageRating", e);
            throw new OurRuntime("getShopAverageRating" + e.getMessage());
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
            throw new OurArg("addItemToShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("addItemToShop", e);
            throw new OurRuntime("addItemToShop" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("addItemToShop", e, shopId, name, quantity, price);
            throw new OurRuntime("Error adding item to shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    
    public void addSupplyToItem(int shopId, int itemId, int quantity, String token) {
        try {
            LoggerService.logMethodExecution("addSupplyToItem", shopId, itemId, quantity);
            authTokenService.ValidateToken(token);
            shopRepository.addSupplyToItem(shopId, itemId, quantity);
            LoggerService.logMethodExecutionEndVoid("addSupplyToItem");
        } catch (OurArg e) {
            LoggerService.logDebug("addSupplyToItem", e);
            throw new OurArg("addSupplyToItem" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("addSupplyToItem", e);
            throw new OurRuntime("addSupplyToItem" + e.getMessage());
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
                throw new OurRuntime("User does not have permission to update price for item " + itemId + " in shop " + shopId);
            }
            shopRepository.updateItemPriceInShop(shopId, itemId, price);
            LoggerService.logMethodExecutionEndVoid("updateItemPriceInShop");
        } catch (OurArg e) {
            LoggerService.logDebug("updateItemPriceInShop", e);
            throw new OurArg("updateItemPriceInShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("updateItemPriceInShop", e);
            throw new OurRuntime("updateItemPriceInShop" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("updateItemPriceInShop", e, shopId, itemId, price);
            throw new OurRuntime("Error updating price for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    
    public void removeItemFromShop(int shopId, int itemId, String token) {
        try {
            LoggerService.logMethodExecution("removeItemFromShop", shopId, itemId);
            int userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)) {
                throw new OurRuntime("User does not have permission to remove item " + itemId + " from shop " + shopId);
            }
            shopRepository.removeItemFromShop(shopId, itemId);
            LoggerService.logMethodExecutionEndVoid("removeItemFromShop");
        } catch (OurArg e) {
            LoggerService.logDebug("removeItemFromShop", e);
            throw new OurArg("removeItemFromShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeItemFromShop", e);
            throw new OurRuntime("removeItemFromShop" + e.getMessage());
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
            throw new OurArg("getItemQuantityFromShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getItemQuantityFromShop", e);
            throw new OurRuntime("getItemQuantityFromShop" + e.getMessage());
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
                throw new OurRuntime("User does not have permission to close shop " + shopId);
            }
            shopRepository.closeShop(shopId);
            LoggerService.logMethodExecutionEndVoid("closeShop");
        } catch (OurArg e) {
            LoggerService.logDebug("closeShop", e);
            throw new OurArg("closeShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("closeShop", e);
            throw new OurRuntime("closeShop" + e.getMessage());
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
            throw new OurArg("checkSupplyAvailability" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("checkSupplyAvailability", e);
            throw new OurRuntime("checkSupplyAvailability" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("checkSupplyAvailability", e, shopId, itemId);
            throw new OurRuntime("Error checking supply availability: " + e.getMessage(), e);
        }
    }
    
    public Double purchaseItems(Map<Integer, Integer> purchaseLists, Integer shopId) {
        try {
            LoggerService.logMethodExecution("purchaseItems", purchaseLists, shopId);
            Map<Integer, ItemCategory> itemsCategory = itemService.getItemdId2Cat(purchaseLists);
            double totalPrice = shopRepository.purchaseItems(purchaseLists,itemsCategory, shopId);
            LoggerService.logMethodExecutionEnd("purchaseItems", totalPrice);
            return totalPrice;
        } catch (OurArg e) {
            LoggerService.logDebug("purchaseItems", e);
            throw new OurArg("purchaseItems" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("purchaseItems", e);
            throw new OurRuntime("purchaseItems" + e.getMessage());
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
            throw new OurArg("rollBackPurchase" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("rollBackPurchase", e);
            throw new OurRuntime("rollBackPurchase" + e.getMessage());
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
            throw new OurArg("checkSupplyAvailabilityAndAcquire" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("checkSupplyAvailabilityAndAcquire", e);
            throw new OurRuntime("checkSupplyAvailabilityAndAcquire" + e.getMessage());
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
            throw new OurArg("addSupply" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("addSupply", e);
            throw new OurRuntime("addSupply" + e.getMessage());
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
            throw new OurArg("removeSupply" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeSupply", e);
            throw new OurRuntime("removeSupply" + e.getMessage());
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
            throw new OurArg("checkPolicy" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("checkPolicy", e);
            throw new OurRuntime("checkPolicy" + e.getMessage());
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
            throw new OurArg("getItemsByShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getItemsByShop", e);
            throw new OurRuntime("getItemsByShop" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("getItemsByShop", e, shopId);
            throw new OurRuntime("Error retrieving items for shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    
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
            throw new OurArg("getItems" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getItems", e);
            throw new OurRuntime("getItems" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("getItems", e);
            throw new OurRuntime("Error retrieving all items: " + e.getMessage(), e);
        }
    }
    
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
            throw new OurArg("searchItems" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("searchItems", e);
            throw new OurRuntime("searchItems" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("searchItems", e, name, category, keywords, minPrice, maxPrice, minProductRating, minShopRating);
            throw new OurRuntime("Error searching items: " + e.getMessage(), e);
        }
    }
    
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
            throw new OurArg("searchItemsInShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("searchItemsInShop", e);
            throw new OurRuntime("searchItemsInShop" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("searchItemsInShop", e, shopId, name, category, keywords, minPrice, maxPrice, minProductRating);
            throw new OurRuntime("Error searching items in shop: " + e.getMessage(), e);
        }
    }
    
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
            throw new OurArg("filterItemsInShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("filterItemsInShop", e);
            throw new OurRuntime("filterItemsInShop" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("filterItemsInShop", e, shop, name, category, keywords, minPrice, maxPrice, minProductRating);
            throw new OurRuntime("Error filtering items in shop " + shop.getId() + ": " + e.getMessage(), e);
        }
    }

    public void shipPurchase(String token, int purchaseId, int shopId, String country, String city, String street, String postalCode) {
        try {
            LoggerService.logMethodExecution("shipPurchase", purchaseId, country, city, street, postalCode);
            authTokenService.ValidateToken(token);
            shopRepository.shipPurchase(purchaseId, shopId,  country, city, street, postalCode);
            LoggerService.logMethodExecutionEndVoid("shipPurchase");
        } catch (Exception e) {
            LoggerService.logError("shipPurchase", e, purchaseId, country, city, street, postalCode);
            throw new RuntimeException("Error shipping purchase " + purchaseId + ": " + e.getMessage(), e);
        }
    }
}

