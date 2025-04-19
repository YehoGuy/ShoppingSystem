package InfrastructureLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import DomainLayer.Shop.IShopRepository;
import DomainLayer.Shop.Shop;

public class ShopRepository implements IShopRepository {

    // A thread-safe map to manage Shop instances.
    private final ConcurrentHashMap<Integer, Shop> shops = new ConcurrentHashMap<>();

    // Atomic counter to allocate unique shop ids.
    private final AtomicInteger shopIdCounter = new AtomicInteger(1);

    /**
     * Creates a new shop with the specified parameters.
     * The ShopService auto-allocates a unique id for the shop.
     *
     * @param name           the shop name.
     * @param purchasePolicy the shop purchase policy.
     * @param globalDiscount the global discount for all items in the shop.
     * @return the newly created Shop object with an auto-allocated id.
     */
    public Shop createShop(String name, String purchasePolicy, int globalDiscount) {
        int id = shopIdCounter.getAndIncrement();
        Shop shop = new Shop(id, name, purchasePolicy, globalDiscount);
        Shop previous = shops.putIfAbsent(id, shop);
        if (previous != null) {
            throw new IllegalStateException("Shop with id " + id + " already exists.");
        }
        return shop;
    }

    /** For tests **/
    public Shop getShop(int id) {
        return shops.get(id);
    }

    /** For tests **/
    public List<Shop> getAllShops() {
        return Collections.unmodifiableList(
                shops.values().stream().collect(Collectors.toList())
        );
    }

    public void updatePurchasePolicy(int shopId, String newPolicy) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            shop.setPurchasePolicy(newPolicy);
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    public void setGlobalDiscount(int shopId, int discount) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            shop.setGlobalDiscount(discount);
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    public void setDiscountForItem(int shopId, int itemId, int discount) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            shop.setDiscountForItem(itemId, discount);
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    public void addReviewToShop(int shopId, int rating, String reviewText) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            shop.addReview(rating, reviewText);
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    public double getShopAverageRating(int shopId) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            return shop.getAverageRating();
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    /**
     * Adds a given quantity of an item to the specified shop,
     * and also sets/updates the item’s price.
     *
     * @param shopId   the shop id.
     * @param itemId   the item id.
     * @param quantity the quantity to add.
     * @param price    the price for the item (must be non-negative).
     */
    public void addItemToShop(int shopId, int itemId, int quantity, int price) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            shop.addItem(itemId, quantity);
            // Immediately update the price for the item after adding it.
            shop.updateItemPrice(itemId, price);
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    /**
     * Updates the price of an existing item in a shop.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param price  the new price (must be non-negative).
     */
    public void updateItemPriceInShop(int shopId, int itemId, int price) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            shop.updateItemPrice(itemId, price);
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    public void removeItemFromShop(int shopId, int itemId) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            shop.removeItem(itemId, -1);
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    public int getItemQuantityFromShop(int shopId, int itemId) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            return shop.getItemQuantity(itemId);
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    /**
     * Closes the shop identified by shopId.
     * This removes the shop from the registry.
     *
     * @param shopId the shop id.
     */
    public void closeShop(Integer shopId) {
        Shop removed = shops.remove(shopId);
        if (removed == null) {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    /**
     * Checks if the supply is available for the given item in the specified shop.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @return true if the supply count is greater than zero, false otherwise.
     */
    public boolean checkSupplyAvailability(Integer shopId, Integer itemId) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            return shop.getItemQuantity(itemId) > 0;
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    /**
     * Decreases the supply count for the given item in the shop by the specified supply value.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param supply the supply to remove.
     */
    public void removeSupply(Integer shopId, Integer itemId, Integer supply) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            shop.removeItem(itemId, supply);
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    /**
     * Returns a list of item IDs that belong to the shop identified by shopId.
     *
     * @param shopId the shop id.
     * @return a list of item IDs.
     */
    public List<Integer> getItems(Integer shopId) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            return shop.getItemIds();
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }
}
