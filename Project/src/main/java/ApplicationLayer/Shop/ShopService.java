package ApplicationLayer.Shop;

import java.util.ArrayList;
import java.util.List;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.Item.ItemService;
import ApplicationLayer.LoggerService;
import DomainLayer.Item.Item;
import DomainLayer.Item.ItemCategory;
import ApplicationLayer.User.UserService;
import DomainLayer.Shop.IShopRepository;
import DomainLayer.Shop.Shop;

public class ShopService {

    // Use the interface type for the repository dependency.
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
    public Shop createShop(String name, String purchasePolicy, int globalDiscount) {
        try { 
           LoggerService.logMethodExecution("createShop", name, purchasePolicy, globalDiscount);
            Shop returnShop = shopRepository.createShop(name, purchasePolicy, globalDiscount);
            LoggerService.logMethodExecutionEnd("createShop", returnShop);
            return returnShop;
        } catch (Exception e) 
        {
            LoggerService.logError("createShop", e, name, purchasePolicy, globalDiscount);
            throw new RuntimeException("Error creating shop: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a shop by its identifier.
     *
     * @param shopId the shop id.
     * @return the Shop instance.
     */
    public Shop getShop(int shopId) {
        try {
            LoggerService.logMethodExecution("getShop", shopId);
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
    public List<Shop> getAllShops() {
        try {
            LoggerService.logMethodExecution("getAllShops");
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
    public void updatePurchasePolicy(int shopId, String newPolicy) {
        try {
            LoggerService.logMethodExecution("updatePurchasePolicy", shopId, newPolicy);
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
    public void setGlobalDiscount(int shopId, int discount) {
        try {
            LoggerService.logMethodExecution("setGlobalDiscount", shopId, discount);
            shopRepository.setGlobalDiscount(shopId, discount);
            LoggerService.logMethodExecutionEndVoid("setGlobalDiscount");
        } catch (Exception e) {
            LoggerService.logError("setGlobalDiscount", e, shopId, discount);
            throw new RuntimeException("Error setting global discount for shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Sets a discount for a specific item in the specified shop.
     *
     * @param shopId   the shop id.
     * @param itemId   the item id.
     * @param discount the discount value.
     */
    public void setDiscountForItem(int shopId, int itemId, int discount) {
        try {
            LoggerService.logMethodExecution("setDiscountForItem", shopId, itemId, discount);
            shopRepository.setDiscountForItem(shopId, itemId, discount);
            LoggerService.logMethodExecutionEndVoid("setDiscountForItem");
        } catch (Exception e) {
            LoggerService.logError("setDiscountForItem", e, shopId, itemId, discount);
            throw new RuntimeException("Error setting discount for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Adds a review to the specified shop.
     *
     * @param shopId     the shop id.
     * @param rating     the review rating.
     * @param reviewText the review text.
     */
    public void addReviewToShop(int shopId, int rating, String reviewText) {
        try {
            LoggerService.logMethodExecution("addReviewToShop", shopId, rating, reviewText);
            shopRepository.addReviewToShop(shopId, rating, reviewText);
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
    public double getShopAverageRating(int shopId) {
        try {
            LoggerService.logMethodExecution("getShopAverageRating", shopId);
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
    public void addItemToShop(int shopId, int itemId, int quantity, int price) {
        try {
            LoggerService.logMethodExecution("addItemToShop", shopId, itemId, quantity, price);
            shopRepository.addItemToShop(shopId, itemId, quantity, price);
            LoggerService.logMethodExecutionEndVoid("addItemToShop");
        } catch (Exception e) {
            LoggerService.logError("addItemToShop", e, shopId, itemId, quantity, price);
            throw new RuntimeException("Error adding item " + itemId + " to shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Updates the price for an existing item in the specified shop.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param price  the new price (must be non-negative).
     */
    public void updateItemPriceInShop(int shopId, int itemId, int price) {
        try {
            LoggerService.logMethodExecution("updateItemPriceInShop", shopId, itemId, price);
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
    public void removeItemFromShop(int shopId, int itemId) {
        try {
            LoggerService.logMethodExecution("removeItemFromShop", shopId, itemId);
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
    public int getItemQuantityFromShop(int shopId, int itemId) {
        try {
            LoggerService.logMethodExecution("getItemQuantityFromShop", shopId, itemId);
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
    public void closeShop(Integer shopId) {
        try {
            LoggerService.logMethodExecution("closeShop", shopId);
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
    public boolean checkSupplyAvailability(Integer shopId, Integer itemId) {
        try {
            LoggerService.logMethodExecution("checkSupplyAvailability", shopId, itemId);
            boolean returnBoolean = shopRepository.checkSupplyAvailability(shopId, itemId);
            LoggerService.logMethodExecutionEnd("checkSupplyAvailability", returnBoolean);
            return returnBoolean;
        } catch (Exception e) {
            LoggerService.logError("checkSupplyAvailability", e, shopId, itemId);
            throw new RuntimeException("Error checking supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Decreases the supply count for the given item in the shop by the specified amount.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param supply the supply to remove.
     */
    public void removeSupply(Integer shopId, Integer itemId, Integer supply) {
        try {
            LoggerService.logMethodExecution("removeSupply", shopId, itemId, supply);
            shopRepository.removeSupply(shopId, itemId, supply);
            LoggerService.logMethodExecutionEndVoid("removeSupply");
        } catch (Exception e) {
            LoggerService.logError("removeSupply", e, shopId, itemId, supply);
            throw new RuntimeException("Error removing supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a list of items available in the specified shop.
     *
     * @param shopId the shop id.
     * @return a list of Item instances.
     */
    public List<Item> getItemsByShop(Integer shopId) {
        try {
            LoggerService.logMethodExecution("getItems", shopId);
            List<Integer> returnItems = shopRepository.getItemsByShop(shopId);
            LoggerService.logMethodExecutionEnd("getItems", returnItems);
            List<Item> items = itemService.getItemsByIds(returnItems);
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
    public List<Item> getItems() {
        try {
            LoggerService.logMethodExecution("getItems");
            List<Integer> returnItemsIds = shopRepository.getItems();
            LoggerService.logMethodExecutionEnd("getItems", returnItemsIds);
            List<Item> returnItems = itemService.getItemsByIds(returnItemsIds);
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
            Double minShopRating) 
        {
        try{
            LoggerService.logMethodExecution(
                "searchItems", name, category, keywords,
                minPrice, maxPrice, minProductRating, minShopRating
            );

            List<Item> results = new ArrayList<>();
            for (Shop shop : getAllShops()) {
                if (minShopRating != null && shop.getAverageRating() < minShopRating) {
                    continue;
                }
                // delegate per‑shop filtering
                results.addAll(filterItemsInShop(
                    shop, name, category, keywords,
                    minPrice, maxPrice, minProductRating
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
            Double minProductRating) 
        {
        try{
            LoggerService.logMethodExecution(
                "searchItemsInShop", shopId, name, category, keywords,
                minPrice, maxPrice, minProductRating
            );
    
            Shop shop = getShop(shopId);
            List<Item> results = filterItemsInShop(
                shop, name, category, keywords,
                minPrice, maxPrice, minProductRating
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
            Double minProductRating) 
    {
        try{
            List<Item> results = new ArrayList<>();
            List<Item> shopItems = getItemsByShop(shop.getId());
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
