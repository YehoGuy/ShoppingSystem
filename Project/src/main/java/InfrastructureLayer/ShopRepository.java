package InfrastructureLayer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import ApplicationLayer.Purchase.ShippingMethod;
import DomainLayer.Item.ItemCategory;
import DomainLayer.Shop.IShopRepository;
import DomainLayer.Shop.PurchasePolicy;
import DomainLayer.Shop.Shop;

public class ShopRepository implements IShopRepository {

    private final ConcurrentHashMap<Integer, Shop> shops = new ConcurrentHashMap<>();
    private final AtomicInteger shopIdCounter = new AtomicInteger(1);
    private final List<Shop> closedShops = new CopyOnWriteArrayList<>();

    
    @Override
    public Shop createShop(String name, PurchasePolicy purchasePolicy, ShippingMethod shippingMethod) {
        try {
            int id = shopIdCounter.getAndIncrement();
            Shop shop = new Shop(id, name, shippingMethod);
            Shop previous = shops.putIfAbsent(id, shop);
            if (previous != null) {
                throw new IllegalStateException("Shop with id " + id + " already exists.");
            }
            return shop;
        } catch (Exception e) {
            throw new RuntimeException("Error creating shop: " + e.getMessage(), e);
        }
    }

    @Override
    public Shop getShop(int id) {
        try {
            Shop shop = shops.get(id);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + id);
            }
            return shop;
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving shop: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Shop> getAllShops() {
        try {
            return Collections.unmodifiableList(
                shops.values().stream().collect(Collectors.toList())
            );
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving all shops: " + e.getMessage(), e);
        }
    }

    @Override
    public void updatePurchasePolicy(int shopId, PurchasePolicy newPolicy) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            //shop.addPurchasePolicy(newPolicy);
        } catch (Exception e) {
            throw new RuntimeException("Error updating purchase policy: " + e.getMessage(), e);
        }
    }

    @Override
    public void setGlobalDiscount(int shopId, int discount, boolean isDouble) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.setGlobalDiscount(discount, isDouble);
        } catch (Exception e) {
            throw new RuntimeException("Error setting global discount: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeGlobalDiscount(int shopId) {
        Shop shop = shops.get(shopId);
        if (shop == null) {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
        shop.removeGlobalDiscount();
    }

    @Override
    public void setDiscountForItem(int shopId, int itemId, int discount, boolean isDouble) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.setDiscountForItem(itemId, discount, isDouble);
        } catch (Exception e) {
            throw new RuntimeException("Error setting discount for item: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeDiscountForItem(int shopId, int itemId) {
        Shop shop = shops.get(shopId);
        if (shop == null) {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
        shop.removeDiscountForItem(itemId);
    }

    @Override
    public void setCategoryDiscount(int shopId, ItemCategory category, int percentage, boolean isDouble) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.setCategoryDiscount(category, percentage, isDouble);
        } catch (Exception e) {
            throw new RuntimeException("Error setting category discount: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeCategoryDiscount(int shopId, ItemCategory category) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.removeCategoryDiscount(category);
        } catch (Exception e) {
            throw new RuntimeException("Error removing category discount: " + e.getMessage(), e);
        }
    }

    @Override
    public void addReviewToShop(int shopId,int userId, int rating, String reviewText) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.addReview(userId, rating, reviewText);
        } catch (Exception e) {
            throw new RuntimeException("Error adding review to shop: " + e.getMessage(), e);
        }
    }

    @Override
    public double getShopAverageRating(int shopId) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            return shop.getAverageRating();
        } catch (Exception e) {
            throw new RuntimeException("Error getting average rating: " + e.getMessage(), e);
        }
    }

    @Override
    public void addItemToShop(int shopId, int itemId, int quantity, int price) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.addItem(itemId, quantity);
            shop.updateItemPrice(itemId, price);
        } catch (Exception e) {
            throw new RuntimeException("Error adding item to shop: " + e.getMessage(), e);
        }
    }

    public void addSupplyToItem(int shopId, int itemId, int quantity) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.addItem(itemId, quantity);
        } catch (Exception e) {
            throw new RuntimeException("Error adding supply to item: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateItemPriceInShop(int shopId, int itemId, int price) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.updateItemPrice(itemId, price);
        } catch (Exception e) {
            throw new RuntimeException("Error updating item price: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeItemFromShop(int shopId, int itemId) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.removeItemFromShop(itemId);
        } catch (Exception e) {
            throw new RuntimeException("Error removing item from shop: " + e.getMessage(), e);
        }
    }

    @Override
    public int getItemQuantityFromShop(int shopId, int itemId) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            return shop.getItemQuantity(itemId);
        } catch (Exception e) {
            throw new RuntimeException("Error getting item quantity: " + e.getMessage(), e);
        }
    }

    @Override
    public void closeShop(Integer shopId) {
        try {
            Shop removed = shops.remove(shopId);
            if (removed == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            closedShops.add(removed);
        } catch (Exception e) {
            throw new RuntimeException("Error closing shop: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean checkSupplyAvailability(Integer shopId, Integer itemId) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            return shop.getItemQuantity(itemId) > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error checking supply availability: " + e.getMessage(), e);
        }
    }

    @Override
    public double purchaseItems(Map<Integer, Integer> purchaseLists, Map<Integer, ItemCategory> itemsCategory, Integer shopId) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            return shop.purchaseItems(purchaseLists, itemsCategory);
        } catch (Exception e) {
            throw new RuntimeException("Error purchasing items: " + e.getMessage(), e);
        }
    }

    @Override
    public void rollBackPurchase(Map<Integer, Integer> purchaseLists, Integer shopId) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.rollBackPurchase(purchaseLists);
        } catch (Exception e) {
            throw new RuntimeException("Error rolling back purchase: " + e.getMessage(), e);
        }
    }


    @Override
    /**
     * Should not be used and be deleted after guy chagnes his code
     */
    public boolean checkSupplyAvailabilityAndAqcuire(Integer shopId, Integer itemId, Integer supply) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            int currentQuantity = shop.getItemQuantity(itemId);
            if (currentQuantity >= supply) {
                // shop.removeItemQuantity(itemId, supply); // Decrease the supply count   ----- האם צריך להוריד את הכמות?
                // or just return true and let the caller handle the removal  ----------- לבדוק אם זה בסדר
                return true;
            }
            return false;
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }
    
    @Override
    /**
     * adds a given quantity of an item to the specified shop.
     * 
     * @param shopId   the shop id.
     * @param itemId   the item id.
     * @param supply   the quantity to add.
     */
    public void addSupply(Integer shopId, Integer itemId, Integer supply) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            shop.addItem(itemId, supply);
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    @Override
    /**
     * Decreases the supply count for the given item in the shop by the specified supply value.
     *
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param supply the supply to remove.
     */
    public void removeSupply(Integer shopId, Integer itemId, Integer supply) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.removeItemQuantity(itemId, supply);
        } catch (Exception e) {
            throw new RuntimeException("Error removing supply: " + e.getMessage(), e);
        }
    }

    // Should be change to be curd (too much logic)
    @Override
    public boolean checkPolicy(HashMap<Integer, HashMap<Integer,Integer>> cart, String token) {
        try {
            // TODO: policies
            // 1. member policy - member items only
            // 2. limit policy - limit quantity of items in cart
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Error checking policy: " + e.getMessage(), e);
        }
    }


    @Override
    public List<Integer> getItemsByShop(Integer shopId) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            return shop.getItemIds();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving items by shop: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Integer> getItems() {
        try {
            return Collections.unmodifiableList(
                    shops.values().stream()
                         .flatMap(shop -> shop.getItemIds().stream())
                         .collect(Collectors.toList())
            );
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving all items: " + e.getMessage(), e);
        }
    }

    public List<Shop> getClosedShops() {
        try {
            return Collections.unmodifiableList(closedShops);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving closed shops: " + e.getMessage(), e);
        }
    }

}
