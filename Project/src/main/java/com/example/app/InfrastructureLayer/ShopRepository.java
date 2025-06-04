package com.example.app.InfrastructureLayer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.ShippingMethod;
import com.example.app.DataLayer.ShopJpaRepository;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Discount.Discount;
import com.example.app.DomainLayer.Shop.Discount.Policy;
import com.example.app.DomainLayer.Shop.IShopRepository;
import com.example.app.DomainLayer.Shop.Operator;
import com.example.app.DomainLayer.Shop.PurchasePolicy;
import com.example.app.DomainLayer.Shop.Shop;

import jakarta.transaction.Transactional;

@Repository
public class ShopRepository implements IShopRepository {

    private final ConcurrentHashMap<Integer, Shop> shops = new ConcurrentHashMap<>();
    private final AtomicInteger shopIdCounter = new AtomicInteger(1);
    private final List<Shop> closedShops = new CopyOnWriteArrayList<>();

    private final ShopJpaRepository jpaRepo;

    boolean isTestMode;

    @Autowired
    public ShopRepository(ShopJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
        isTestMode = false; 
    }

    // Constructor for testing purposes
    public ShopRepository() {
        this.jpaRepo = null;
        isTestMode = true; 
    }

    @Override
    @Transactional
    public Shop createShop(String name, PurchasePolicy purchasePolicy, ShippingMethod shippingMethod) {
        try {
            int id = shopIdCounter.getAndIncrement();
            Shop shop = new Shop(id, name, shippingMethod);
            Shop previous = shops.putIfAbsent(id, shop);
            if (previous != null) {
                throw new IllegalStateException("Shop with id " + id + " already exists.");
            }
            if(!isTestMode)
                jpaRepo.save(shop);
            return shop;
        } catch (Exception e) {
            throw new OurRuntime("Error creating shop: " + e.getMessage(), e);
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
                    shops.values().stream().collect(Collectors.toList()));
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving all shops: " + e.getMessage(), e);
        }
    }

    ///TODO: probably unused method, should be removed
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
    @Transactional
    public void setGlobalDiscount(int shopId, int discount, boolean isDouble) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.setGlobalDiscount(discount, isDouble);
            if(!isTestMode)
                jpaRepo.save(shop);
        } catch (Exception e) {
            throw new RuntimeException("Error setting global discount: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void removeGlobalDiscount(int shopId) {
        Shop shop = shops.get(shopId);
        if (shop == null) {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
        shop.removeGlobalDiscount();
        if(!isTestMode)
                jpaRepo.save(shop);
    }

    @Override
    @Transactional
    public void setDiscountForItem(int shopId, int itemId, int discount, boolean isDouble) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.setDiscountForItem(itemId, discount, isDouble);
            if(!isTestMode)
                jpaRepo.save(shop);
        } catch (Exception e) {
            throw new RuntimeException("Error setting discount for item: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void removeDiscountForItem(int shopId, int itemId) {
        Shop shop = shops.get(shopId);
        if (shop == null) {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
        shop.removeDiscountForItem(itemId);
        if(!isTestMode)
            jpaRepo.save(shop);
    }

    @Override
    @Transactional
    public void setCategoryDiscount(int shopId, ItemCategory category, int percentage, boolean isDouble) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.setCategoryDiscount(category, percentage, isDouble);
            if(!isTestMode)
                jpaRepo.save(shop);
        } catch (Exception e) {
            throw new RuntimeException("Error setting category discount: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void removeCategoryDiscount(int shopId, ItemCategory category) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.removeCategoryDiscount(category);
            if(!isTestMode)
                jpaRepo.save(shop);
        } catch (Exception e) {
            throw new RuntimeException("Error removing category discount: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void addReviewToShop(int shopId, int userId, int rating, String reviewText) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.addReview(userId, rating, reviewText);
            if(!isTestMode)
                jpaRepo.save(shop);
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
    @Transactional
    public void addItemToShop(int shopId, int itemId, int quantity, int price) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.addItem(itemId, quantity);
            shop.updateItemPrice(itemId, price);
            if(!isTestMode)
                jpaRepo.save(shop);
        } catch (Exception e) {
            throw new RuntimeException("Error adding item to shop: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void addSupplyToItem(int shopId, int itemId, int quantity) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.addItem(itemId, quantity);
            if(!isTestMode)
                jpaRepo.save(shop);
        } catch (Exception e) {
            throw new RuntimeException("Error adding supply to item: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void updateItemPriceInShop(int shopId, int itemId, int price) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.updateItemPrice(itemId, price);
            if(!isTestMode)
                jpaRepo.save(shop);
        } catch (Exception e) {
            throw new RuntimeException("Error updating item price: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void removeItemFromShop(int shopId, int itemId) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.removeItemFromShop(itemId);
            if(!isTestMode)
                jpaRepo.save(shop);
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
    @Transactional //TODO: WTF IS THI
    public void closeShop(Integer shopId) {
        try {
            
            Shop removed = shops.remove(shopId);
            //print the removed shop for debugging purposes
            System.out.println("Closing shop: " + removed);
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
    @Transactional
    public double purchaseItems(Map<Integer, Integer> purchaseLists, Map<Integer, ItemCategory> itemsCategory,
            Integer shopId) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            double price = shop.purchaseItems(purchaseLists, itemsCategory);
            if (!isTestMode) {
                jpaRepo.save(shop); 
            }
            return price;
        } catch (Exception e) {
            throw new RuntimeException("Error purchasing items: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void rollBackPurchase(Map<Integer, Integer> purchaseLists, Integer shopId) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.rollBackPurchase(purchaseLists);
            if (!isTestMode) {
                jpaRepo.save(shop); 
            }
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
                // shop.removeItemQuantity(itemId, supply); // Decrease the supply count -----
                // האם צריך להוריד את הכמות?
                // or just return true and let the caller handle the removal ----------- לבדוק
                // אם זה בסדר
                return true;
            }
            return false;
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    @Override
    @Transactional
    /**
     * adds a given quantity of an item to the specified shop.
     * 
     * @param shopId the shop id.
     * @param itemId the item id.
     * @param supply the quantity to add.
     */
    public void addSupply(Integer shopId, Integer itemId, Integer supply) {
        Shop shop = shops.get(shopId);
        if (shop != null) {
            shop.addItem(itemId, supply);
            if (!isTestMode) {
                jpaRepo.save(shop); 
            }
        } else {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }
    }

    @Override
    @Transactional
    /**
     * Decreases the supply count for the given item in the shop by the specified
     * supply value.
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
            if (!isTestMode) {
                jpaRepo.save(shop); 
            }
        } catch (Exception e) {
            throw new RuntimeException("Error removing supply: " + e.getMessage(), e);
        }
    }

    // Should be change to be curd (too much logic)
    // TODO: what the helly
    @Override
    public boolean checkPolicy(HashMap<Integer, HashMap<Integer, Integer>> cart, String token) {
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
            List<Integer> lst = Collections.unmodifiableList(
                    shops.values().stream()
                            .flatMap(shop -> shop.getItemIds().stream())
                            .collect(Collectors.toList()));
            return lst == null ? Collections.emptyList() : lst;
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving all items: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void shipPurchase(int purchaseId, int shopId, String country, String city, String street,
            String postalCode) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null)
                throw new IllegalArgumentException("Shop not found: " + shopId);

            shop.getShippingMethod().processShipment(purchaseId, country, city, street, postalCode);
            if(!isTestMode) {
                jpaRepo.save(shop); 
            }
        } catch (Exception e) {
            throw new RuntimeException("Error shipping purchase: " + e.getMessage(), e);
        }
    }

    public List<Shop> getClosedShops() {
        try {
            return Collections.unmodifiableList(closedShops);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving closed shops: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void addDiscountPolicy(int threshold, int itemId, ItemCategory category, double basketValue,
            Operator operator, int shopId) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.addPolicy(threshold, itemId, category, basketValue, operator);
            if (!isTestMode) {
                jpaRepo.save(shop); 
            }
        } catch (Exception e) {
            throw new RuntimeException("Error adding discount policy: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void setDiscountPolicy(int shopId, Policy policy) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            shop.setDiscountPolicy(policy);
            if (!isTestMode) {
                jpaRepo.save(shop); 
            }
        } catch (Exception e) {
            throw new RuntimeException("Error setting discount policy: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Discount> getDiscounts(int shopId) {
        try {
            Shop shop = shops.get(shopId);
            if (shop == null) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            return shop.getDiscounts();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving discounts: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Policy> getPolicies(int shopId) {
        // 1) Load the Shop aggregate
        Shop shop = shops.get(shopId);
        if (shop == null) {
            throw new NoSuchElementException("Shop not found: " + shopId);
        }
        return shop.getPolicies();
    }
}
