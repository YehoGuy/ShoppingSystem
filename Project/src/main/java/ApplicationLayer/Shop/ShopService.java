package ApplicationLayer.Shop;

import java.util.List;

import DomainLayer.Shop.IShopRepository;
import DomainLayer.Shop.Shop;

public class ShopService {

    // Use the interface type for the repository dependency.
    private final IShopRepository shopRepository;

    /**
     * Constructor for ShopService.
     *
     * @param shopRepository an instance of IShopRepository that handles storage of Shop instances.
     */
    public ShopService(IShopRepository shopRepository) {
        this.shopRepository = shopRepository;
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
            return shopRepository.createShop(name, purchasePolicy, globalDiscount);
        } catch (Exception e) {
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
            return shopRepository.getShop(shopId);
        } catch (Exception e) {
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
            return shopRepository.getAllShops();
        } catch (Exception e) {
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
            shopRepository.updatePurchasePolicy(shopId, newPolicy);
        } catch (Exception e) {
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
            shopRepository.setGlobalDiscount(shopId, discount);
        } catch (Exception e) {
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
            shopRepository.setDiscountForItem(shopId, itemId, discount);
        } catch (Exception e) {
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
            shopRepository.addReviewToShop(shopId, rating, reviewText);
        } catch (Exception e) {
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
            return shopRepository.getShopAverageRating(shopId);
        } catch (Exception e) {
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
            shopRepository.addItemToShop(shopId, itemId, quantity, price);
        } catch (Exception e) {
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
            shopRepository.updateItemPriceInShop(shopId, itemId, price);
        } catch (Exception e) {
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
            shopRepository.removeItemFromShop(shopId, itemId);
        } catch (Exception e) {
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
            return shopRepository.getItemQuantityFromShop(shopId, itemId);
        } catch (Exception e) {
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
            shopRepository.closeShop(shopId);
        } catch (Exception e) {
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
            return shopRepository.checkSupplyAvailability(shopId, itemId);
        } catch (Exception e) {
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
            shopRepository.removeSupply(shopId, itemId, supply);
        } catch (Exception e) {
            throw new RuntimeException("Error removing supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns a list of item IDs that belong to the shop identified by shopId.
     *
     * @param shopId the shop id.
     * @return a list of item IDs.
     */
    public List<Integer> getItems(Integer shopId) {
        try {
            return shopRepository.getItems(shopId);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving items for shop " + shopId + ": " + e.getMessage(), e);
        }
    }
}
