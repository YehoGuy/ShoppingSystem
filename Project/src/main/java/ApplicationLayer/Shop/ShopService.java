package ApplicationLayer.Shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.Item.ItemService;
import ApplicationLayer.LoggerService;
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

    /**
     * Creates a new shop with the specified parameters.
     *
     * @param name           the shop name.
     * @param purchasePolicy the shop purchase policy.
     * @param globalDiscount the global discount for all items in the shop.
     * @return the newly created Shop.
     */
    public Shop createShop(String name, PurchasePolicy purchasePolicy, ShippingMethod shippingMethod,  String token) {
        try {
            LoggerService.logMethodExecution("createShop", name, purchasePolicy);
            Integer userId = authTokenService.ValidateToken(token);
            userService.validateMemberId(userId);
            Shop returnShop = shopRepository.createShop(name, purchasePolicy, shippingMethod);
            Role founderRole = new Role(userId, returnShop.getId(), null);
            founderRole.setFoundersPermissions();
            userService.addRole(userId, founderRole);
            LoggerService.logMethodExecutionEnd("createShop", returnShop);
            return returnShop;
        } catch (Exception e) 
        {
            LoggerService.logError("createShop", e, name, purchasePolicy);
            throw new RuntimeException("Error creating shop: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a shop by its identifier.
     *
     * @param shopId the shop id.
     * @return the Shop instance.
     */
    public Shop getShop(int shopId, String token) {
        try {
            LoggerService.logMethodExecution("getShop", shopId);
            authTokenService.ValidateToken(token);
            Shop returnShop = shopRepository.getShop(shopId);
            LoggerService.logMethodExecutionEnd("getShop", returnShop);
            return returnShop;

        } catch (Exception e) {
            LoggerService.logError("getShop", e, shopId);
            throw new RuntimeException("Error retrieving shop with id " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns a list of all shops.
     *
     * @return an unmodifiable list of Shop instances.
     */
    public List<Shop> getAllShops(String token) {
        try {
            LoggerService.logMethodExecution("getAllShops");
            authTokenService.ValidateToken(token);
            List<Shop> returnShops = shopRepository.getAllShops();
            LoggerService.logMethodExecutionEnd("getAllShops", returnShops);
            return returnShops;
        } catch (Exception e) {
            LoggerService.logError("getAllShops", e);
            throw new RuntimeException("Error retrieving all shops: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the purchase policy for the specified shop.
     *
     * @param shopId    the shop id.
     * @param newPolicy the new purchase policy.
     */
    public void updatePurchasePolicy(int shopId, PurchasePolicy newPolicy, String token) {
        try {
            LoggerService.logMethodExecution("updatePurchasePolicy", shopId, newPolicy);
            Integer userId = authTokenService.ValidateToken(token);
            if(!userService.hasPermission(userId,PermissionsEnum.setPolicy,shopId)){
                throw new RuntimeException("User does not have permission to update purchase policy for shop " + shopId);
            }
            shopRepository.updatePurchasePolicy(shopId, newPolicy);
            LoggerService.logMethodExecutionEndVoid("updatePurchasePolicy");
        } catch (Exception e) {
            LoggerService.logError("updatePurchasePolicy", e, shopId, newPolicy);
            throw new RuntimeException("Error updating purchase policy for shop " + shopId + ": " + e.getMessage(), e);
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
            if(!userService.hasPermission(userId,PermissionsEnum.setPolicy,shopId)){
                throw new RuntimeException("User does not have permission to update discount for shop " + shopId);
            }
            shopRepository.setGlobalDiscount(shopId, discount, isDouble);
            LoggerService.logMethodExecutionEndVoid("setGlobalDiscount");
        } catch (Exception e) {
            LoggerService.logError("setGlobalDiscount", e, shopId, discount);
            throw new RuntimeException("Error setting global discount for shop " + shopId + ": " + e.getMessage(), e);
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
            if(!userService.hasPermission(userId,PermissionsEnum.setPolicy,shopId)){
                throw new RuntimeException("User does not have permission to update discount for shop " + shopId);
            }
            shopRepository.setDiscountForItem(shopId, itemId, discount, isDouble);
            LoggerService.logMethodExecutionEndVoid("setDiscountForItem");
        } catch (Exception e) {
            LoggerService.logError("setDiscountForItem", e, shopId, itemId, discount);
            throw new RuntimeException("Error setting discount for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
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
            if(!userService.hasPermission(userId,PermissionsEnum.setPolicy,shopId)){
                throw new RuntimeException("User does not have permission to update discount for shop " + shopId);
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

    /**
     * Adds a review to the specified shop.
     *
     * @param shopId     the shop id.
     * @param rating     the review rating.
     * @param reviewText the review text.
     */
    public void addReviewToShop(int shopId, int rating, String reviewText, String token) {
        try {
            LoggerService.logMethodExecution("addReviewToShop", shopId, rating, reviewText);
            Integer userId = authTokenService.ValidateToken(token);
            userService.validateMemberId(userId);
            shopRepository.addReviewToShop(shopId, userId, rating, reviewText);
            LoggerService.logMethodExecutionEndVoid("addReviewToShop");
        } catch (Exception e) {
            LoggerService.logError("addReviewToShop", e, shopId, rating, reviewText);
            throw new RuntimeException("Error adding review to shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the average rating of the specified shop.
     *
     * @param shopId the shop id.
     * @return the average rating.
     */
    public double getShopAverageRating(int shopId, String token) {
        try {
            LoggerService.logMethodExecution("getShopAverageRating", shopId);
            authTokenService.ValidateToken(token);
            double returnDouble = shopRepository.getShopAverageRating(shopId);
            LoggerService.logMethodExecutionEnd("getShopAverageRating", returnDouble);
            return returnDouble;
        } catch (Exception e) {
            LoggerService.logError("getShopAverageRating", e, shopId);
            throw new RuntimeException("Error retrieving average rating for shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Adds a given quantity of an item to the specified shop and sets its price.
     *
     * @param shopId   the shop id.
     * @param itemId   the item id.
     * @param quantity the quantity to add.
     * @param price    the price for the item (must be non-negative).
     */
    public void addItemToShop(int shopId, String name, String desc, int quantity, int price, String token) {
        try {
            LoggerService.logMethodExecution("addItemToShop", shopId, quantity, price);
            Integer userId = authTokenService.ValidateToken(token);
            if(!userService.hasPermission(userId,PermissionsEnum.manageItems,shopId)){
                throw new RuntimeException("User does not have permission to add item to shop " + shopId);
            }
            Integer itemId = itemService.createItem(shopId, name, desc, userId, token);
            shopRepository.addItemToShop(shopId, itemId, quantity, price);
            LoggerService.logMethodExecutionEndVoid("addItemToShop");
        } catch (Exception e) {
            LoggerService.logError("addItemToShop", e, shopId, quantity, price);
            throw new RuntimeException("Error adding item to shop " + shopId + ": " + e.getMessage(), e);
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
        } catch (Exception e) {
            LoggerService.logError("addSupplyToItem", e, shopId, itemId, quantity);
            throw new RuntimeException("Error adding supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Updates the price for an existing item in the specified shop.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param price  the new price (must be non-negative).
     */
    public void updateItemPriceInShop(int shopId, int itemId, int price, String token) {
        try {
            LoggerService.logMethodExecution("updateItemPriceInShop", shopId, itemId, price);
            Integer userId = authTokenService.ValidateToken(token);
            if(!userService.hasPermission(userId,PermissionsEnum.manageItems,shopId)){
                throw new RuntimeException("User does not have permission to update price for item " + itemId + " in shop " + shopId);
            }
            shopRepository.updateItemPriceInShop(shopId, itemId, price);
            LoggerService.logMethodExecutionEndVoid("updateItemPriceInShop");
        } catch (Exception e) {
            LoggerService.logError("updateItemPriceInShop", e, shopId, itemId, price);
            throw new RuntimeException("Error updating price for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
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
            Integer userId = authTokenService.ValidateToken(token);
            if(!userService.hasPermission(userId,PermissionsEnum.manageItems,shopId)){
                throw new RuntimeException("User does not have permission to remove item " + itemId + " from shop " + shopId);
            }
            shopRepository.removeItemFromShop(shopId, itemId);
            LoggerService.logMethodExecutionEndVoid("removeItemFromShop");
        } catch (Exception e) {
            LoggerService.logError("removeItemFromShop", e, shopId, itemId);
            throw new RuntimeException("Error removing item " + itemId + " from shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the current quantity of an item from the specified shop.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @return the quantity.
     */
    public int getItemQuantityFromShop(int shopId, int itemId, String token) {
        try {
            LoggerService.logMethodExecution("getItemQuantityFromShop", shopId, itemId);
            authTokenService.ValidateToken(token);
            int returnInt = shopRepository.getItemQuantityFromShop(shopId, itemId);
            LoggerService.logMethodExecutionEnd("getItemQuantityFromShop", returnInt);
            return returnInt;
        } catch (Exception e) {
            LoggerService.logError("getItemQuantityFromShop", e, shopId, itemId);
            throw new RuntimeException("Error retrieving quantity for item " + itemId + " from shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Closes the shop identified by shopId.
     * This removes the shop from the repository.
     *
     * @param shopId the shop id.
     */
    public void closeShop(Integer shopId, String token) {
        try {
            LoggerService.logMethodExecution("closeShop", shopId);
            Integer userId = authTokenService.ValidateToken(token);
            if(!userService.hasPermission(userId,PermissionsEnum.closeShop,shopId)){
                throw new RuntimeException("User does not have permission to close shop " + shopId);
            }
            shopRepository.closeShop(shopId);
            LoggerService.logMethodExecutionEndVoid("closeShop");
        } catch (Exception e) {
            LoggerService.logError("closeShop", e, shopId);
            throw new RuntimeException("Error closing shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Checks if the supply is available for the given item in the specified shop.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @return true if available, false otherwise.
     */
    public boolean checkSupplyAvailability(Integer shopId, Integer itemId, String token) {
        try {
            LoggerService.logMethodExecution("checkSupplyAvailability", shopId, itemId);
            authTokenService.ValidateToken(token);
            boolean returnBoolean = shopRepository.checkSupplyAvailability(shopId, itemId);
            LoggerService.logMethodExecutionEnd("checkSupplyAvailability", returnBoolean);
            return returnBoolean;
        } catch (Exception e) {
            LoggerService.logError("checkSupplyAvailability", e, shopId, itemId);
            throw new RuntimeException("Error checking supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public double purchaseItems(Map<Integer, Integer> purchaseLists, Integer shopId) {
        try {
            LoggerService.logMethodExecution("purchaseItems", purchaseLists, shopId);
            Map<Integer, ItemCategory> itemsCategory = itemService.getItemdId2Cat(purchaseLists);
            double totalPrice = shopRepository.purchaseItems(purchaseLists,itemsCategory, shopId);
            LoggerService.logMethodExecutionEnd("purchaseItems", totalPrice);
            return totalPrice;
        } catch (Exception e) {
            LoggerService.logError("purchaseItems", e, purchaseLists, shopId);
            throw new RuntimeException("Error purchasing items from shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public void rollBackPurchase(Map<Integer, Integer> purchaseLists, Integer shopId) {
        try{
            LoggerService.logMethodExecution("rollBackPurchase", purchaseLists, shopId);
            shopRepository.rollBackPurchase(purchaseLists, shopId);
            LoggerService.logMethodExecutionEndVoid("rollBackPurchase");
        } catch (Exception e) {
            LoggerService.logError("rollBackPurchase", e, purchaseLists, shopId);
            throw new RuntimeException("Error rolling back purchase from shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Checks if the supply is available for the given item in the specified shop
     * and acquires it if available.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param supply the supply to acquire.
     * @return true if available, false otherwise.
     */
    public boolean checkSupplyAvailabilityAndAcquire(Integer shopId, Integer itemId, Integer supply) {
        try {
            LoggerService.logMethodExecution("checkSupplyAvailabilityAndAqcuire", shopId, itemId, supply);
            boolean returnBoolean = shopRepository.checkSupplyAvailabilityAndAqcuire(shopId, itemId, supply);
            LoggerService.logMethodExecutionEnd("checkSupplyAvailabilityAndAqcuire", returnBoolean);
            return returnBoolean;
        } catch (Exception e) {
            LoggerService.logError("checkSupplyAvailability", e, shopId, itemId);
            throw new RuntimeException("Error checking supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }
    

    /**
     * Increases the supply count for the given item in the shop by the specified amount.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param supply the supply to add.
     */
    public void addSupply(Integer shopId, Integer itemId, Integer supply) {
        try {
            LoggerService.logMethodExecution("addSupply", shopId, itemId, supply);
            shopRepository.addSupply(shopId, itemId, supply);
            LoggerService.logMethodExecutionEndVoid("addSupply");
        } catch (Exception e) {
            LoggerService.logError("addSupply", e, shopId, itemId, supply);
            throw new RuntimeException("Error adding supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Decreases the supply count for the given item in the shop by the specified amount.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param supply the supply to remove.
     */
    public void removeSupply(Integer shopId, Integer itemId, Integer supply, String token) {
        try {
            LoggerService.logMethodExecution("removeSupply", shopId, itemId, supply);
            authTokenService.ValidateToken(token);
            shopRepository.removeSupply(shopId, itemId, supply);
            LoggerService.logMethodExecutionEndVoid("removeSupply");
        } catch (Exception e) {
            LoggerService.logError("removeSupply", e, shopId, itemId, supply);
            throw new RuntimeException("Error removing supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public boolean checkPolicy(HashMap<Integer, HashMap<Integer,Integer>> cart, String token) {
        try{
            LoggerService.logMethodExecution("checkPolicy", cart);
            authTokenService.ValidateToken(token);
            boolean returnBoolean = shopRepository.checkPolicy(cart, token);
            LoggerService.logMethodExecutionEnd("checkPolicy", returnBoolean);
            return returnBoolean;
        } catch (Exception e) {
            LoggerService.logError("checkPolicy", e, cart);
            throw new RuntimeException("Error checking policy: " + e.getMessage(), e);
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
            LoggerService.logMethodExecution("getItems", shopId);
            authTokenService.ValidateToken(token);
            List<Integer> returnItems = shopRepository.getItemsByShop(shopId);
            LoggerService.logMethodExecutionEnd("getItems", returnItems);
            List<Item> items = itemService.getItemsByIds(returnItems, token);
            LoggerService.logMethodExecutionEnd("getItems", items);
            return items;
        } catch (Exception e) {
            LoggerService.logError("getItems", e, shopId);
            throw new RuntimeException("Error retrieving items for shop " + shopId + ": " + e.getMessage(), e);
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
            List<Integer> returnItemsIds = shopRepository.getItems();
            LoggerService.logMethodExecutionEnd("getItems", returnItemsIds);
            List<Item> returnItems = itemService.getItemsByIds(returnItemsIds, token);
            return returnItems;
        } catch (Exception e) {
            LoggerService.logError("getItems", e);
            throw new RuntimeException("Error retrieving all items: " + e.getMessage(), e);
        }
    }
    

    /**
     * Search across all shops (no single‑shop focus) for items matching any
     * combination of these optional criteria.
     */
    public List<Item> searchItems(
            String name,
            ItemCategory category,
            List<String> keywords,
            Integer minPrice,
            Integer maxPrice,
            Double minProductRating,
            Double minShopRating, String token) 
        {
        try{
            LoggerService.logMethodExecution(
                "searchItems", name, category, keywords,
                minPrice, maxPrice, minProductRating, minShopRating
            );
            authTokenService.ValidateToken(token);
            List<Item> results = new ArrayList<>();
            for (Shop shop : getAllShops(token)) {
                if (minShopRating != null && shop.getAverageRating() < minShopRating) {
                    continue;
                }
                // delegate per‑shop filtering
                results.addAll(filterItemsInShop(
                    shop, name, category, keywords,
                    minPrice, maxPrice, minProductRating, token
                ));
            }

            LoggerService.logMethodExecutionEnd("searchItems", results);
            return results;
        }
        catch(Exception e){
            LoggerService.logError("searchItems", e, name, category, keywords,
                minPrice, maxPrice, minProductRating, minShopRating);
            throw new RuntimeException("Error searching items: " + e.getMessage(), e);
        }
    }

    /**
     * Search within a specific shop for items matching any combination of these optional criteria.
     */
    public List<Item> searchItemsInShop(
            Integer shopId,
            String name,
            ItemCategory category,
            List<String> keywords,
            Integer minPrice,
            Integer maxPrice,
            Double minProductRating, String token) 
    {
        try{
            LoggerService.logMethodExecution(
                "searchItemsInShop", shopId, name, category, keywords,
                minPrice, maxPrice, minProductRating
            );
            authTokenService.ValidateToken(token);
            Shop shop = getShop(shopId, token);
            List<Item> results = filterItemsInShop(
                shop, name, category, keywords,
                minPrice, maxPrice, minProductRating, token
            );
    
            LoggerService.logMethodExecutionEnd("searchItemsInShop", results);
            return results;
        }
        catch(Exception e){
            LoggerService.logError("searchItemsInShop", e, shopId, name, category, keywords,
                minPrice, maxPrice, minProductRating);
            throw new RuntimeException("Error searching items in shop: " + e.getMessage(), e);
        }
    }

    /**
     * Private helper: apply all item‑level filters within one shop.
     * if a filter is null, it is ignored.
     */
    private List<Item> filterItemsInShop(
            Shop shop,
            String name,
            ItemCategory category,
            List<String> keywords,
            Integer minPrice,
            Integer maxPrice,
            Double minProductRating, String token) 
    {
        try{
            List<Item> results = new ArrayList<>();
            List<Item> shopItems = getItemsByShop(shop.getId(), token);
            for (Item item : shopItems) {
                // name
                if (name != null &&
                    !item.getName().toLowerCase().contains(name.toLowerCase())) {
                    continue;
                }
                // category
                if (category != null && item.getCategory() != category) {
                    continue;
                }
                // keywords
                if (keywords != null && !keywords.isEmpty()) {
                    String ln = item.getName().toLowerCase();
                    String ld = item.getDescription().toLowerCase();
                    boolean match = keywords.stream()
                        .map(String::toLowerCase)
                        .anyMatch(kw -> ln.contains(kw) || ld.contains(kw));
                    if (!match) continue;
                }
                // price
                int price = shop.getItemPrice(item.getId());
                if (minPrice != null && price < minPrice) continue;
                if (maxPrice != null && price > maxPrice) continue;
                // product rating
                double pr = item.getAverageRating();
                if (minProductRating != null && pr < minProductRating) continue;
                results.add(item);
            }
            return results;
        }
        catch(Exception e){
            LoggerService.logError("filterItemsInShop", e, shop, name, category, keywords,
                minPrice, maxPrice, minProductRating);
            throw new RuntimeException("Error filtering items in shop: " + e.getMessage(), e);
        }
    }
}

