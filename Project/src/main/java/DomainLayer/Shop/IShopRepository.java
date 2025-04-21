package DomainLayer.Shop;

import java.util.List;

public interface IShopRepository {

    /**
     * Creates a new shop with the specified parameters.
     *
     * @param name           the shop name.
     * @param purchasePolicy the shop purchase policy.
     * @param globalDiscount the global discount for all items in the shop.
     * @return the newly created Shop object with an auto-allocated id.
     */
    Shop createShop(String name, String purchasePolicy, int globalDiscount);

    /**
     * Retrieves a shop by its id.
     *
     * @param id the shop id.
     * @return the Shop object.
     */
    Shop getShop(int id);

    /**
     * Returns an unmodifiable list of all shops.
     *
     * @return a list containing all registered shops.
     */
    List<Shop> getAllShops();

    /**
     * Updates the purchase policy for the specified shop.
     *
     * @param shopId    the shop id.
     * @param newPolicy the new purchase policy.
     */
    void updatePurchasePolicy(int shopId, String newPolicy);

    /**
     * Sets the global discount for the specified shop.
     *
     * @param shopId   the shop id.
     * @param discount the global discount value.
     */
    void setGlobalDiscount(int shopId, int discount);

    /**
     * Sets a discount for a specific item in the specified shop.
     *
     * @param shopId   the shop id.
     * @param itemId   the item id.
     * @param discount the discount value.
     */
    void setDiscountForItem(int shopId, int itemId, int discount);

    /**
     * Adds a review to the specified shop.
     *
     * @param shopId     the shop id.
     * @param rating     the review rating.
     * @param reviewText the review text.
     */
    void addReviewToShop(int shopId,int userId, int rating, String reviewText);

    /**
     * Retrieves the average rating of the specified shop.
     *
     * @param shopId the shop id.
     * @return the average rating.
     */
    double getShopAverageRating(int shopId);

    /**
     * Adds a given quantity of an item to the specified shop, and sets its price.
     *
     * @param shopId   the shop id.
     * @param itemId   the item id.
     * @param quantity the quantity to add.
     * @param price    the price for the item (must be non-negative).
     */
    void addItemToShop(int shopId, int itemId, int quantity, int price);

    /**
     * Adds a given quantity of an item to the specified shop.
     *
     * @param shopId   the shop id.
     * @param itemId   the item id.
     * @param quantity the quantity to add.
     */
    void addSupplyToItem(int shopId, int itemId, int quantity);

    /**
     * Updates the price for an existing item in the specified shop.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param price  the new price (must be non-negative).
     */
    void updateItemPriceInShop(int shopId, int itemId, int price);

    /**
     * Removes an item from the specified shop.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     */
    void removeItemFromShop(int shopId, int itemId);

    /**
     * Retrieves the current quantity of an item from the specified shop.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @return the quantity.
     */
    int getItemQuantityFromShop(int shopId, int itemId);

    /**
     * Closes the shop identified by shopId.
     * This removes the shop from the registry.
     *
     * @param shopId the shop id.
     */
    void closeShop(Integer shopId);

    /**
     * Checks if the supply is available for the given item in the specified shop.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @return true if the supply count is greater than zero, false otherwise.
     */
    boolean checkSupplyAvailability(Integer shopId, Integer itemId);

    /**
     * checks if the supply is available for the given item in the specified shop
     * and acquires the supply if available.
     * 
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param supply the supply to acquire.
     * @return true if the supply was successfully acquired, false otherwise.
     */
    boolean checkSupplyAvailabilityAndAqcuire(Integer shopId, Integer itemId, Integer supply);

    /**
     * Decreases the supply count for the given item in the shop by the specified supply value.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param supply the supply to remove.
     */
    void removeSupply(Integer shopId, Integer itemId, Integer supply);

    /**
     * Retrieves a list of item IDs that belong to the shop identified by shopId.
     *
     * @param shopId the shop id.
     * @return a list of item IDs.
     */
    List<Integer> getItemsByShop(Integer shopId);

    /**
     * Retrieves a list of item IDs that belong to the shop identified by shopId.
     *
     * @return a list of item IDs.
     */
    List<Integer> getItems();

    /**
     * Retrieves a list of all shops.
     *
     * @return a list of all shops.
     */
    void addSupply(Integer shopId, Integer itemId, Integer supply);
}
