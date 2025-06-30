package com.example.app.DBLayer.Shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.ShippingMethod;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Discount.Discount;
import com.example.app.DomainLayer.Shop.Discount.Policy;
import com.example.app.DomainLayer.Shop.IShopRepository;
import com.example.app.DomainLayer.Shop.PurchasePolicy;
import com.example.app.DomainLayer.Shop.Shop;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
@Profile("!no-db & !test")
public class ShopRepositoryDBImpl implements IShopRepository {

    private ShopRepositoryDB jpaRepo;

    @PersistenceContext
    private EntityManager entityManager;

    public ShopRepositoryDBImpl(@Lazy @Autowired ShopRepositoryDB jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Shop createShop(String name, PurchasePolicy purchasePolicy, ShippingMethod shippingMethod) {


        if(getAllShops().stream().anyMatch(shop -> shop.getName().equals(name))) {
            throw new OurRuntime("Shop with name " + name + " already exists.");
        }   
        

        int highestShopId = jpaRepo.findAll().stream()
            .mapToInt(Shop::getId)
            .max()
            .orElse(0) + 1;

        Shop shop = new Shop(highestShopId, name, shippingMethod);

        try {
            Shop saved = jpaRepo.save(shop);
            return saved;
        } catch (Exception e) {
            throw new OurRuntime("Failed to create shop: " + name, e);
        }
    }

    @Override
    public Shop getShop(int id) {
        return jpaRepo.findById(id)
                .orElseThrow(() -> new OurRuntime("Shop not found: " + id));
    }

    @Override
    public List<Shop> getAllShops() {
        return jpaRepo.findAll();
    }

    public List<Shop> getAllOpenShops() {
        return jpaRepo.findAll().stream()
                .filter(shop -> !shop.isClosed())
                .collect(Collectors.toList());
    }

    public List<Shop> getAllClosedShops() {
        return jpaRepo.findAll().stream()
                .filter(Shop::isClosed)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updatePurchasePolicy(int shopId, PurchasePolicy newPolicy) {
        return;
    }

    @Override
    @Transactional
    public void setGlobalDiscount(int shopId, int discount, boolean isDouble) {
        try {
            Shop shop = getShop(shopId);
            shop.setGlobalDiscount(discount, isDouble);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void removeGlobalDiscount(int shopId) {
        try {
            Shop shop = getShop(shopId);
            shop.removeGlobalDiscount();
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void setDiscountForItem(int shopId, int itemId, int discount, boolean isDouble) {
        try {
            Shop shop = getShop(shopId);
            shop.setDiscountForItem(itemId, discount, isDouble);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void setCategoryDiscount(int shopId, ItemCategory category, int percentage, boolean isDouble) {
        try {
            Shop shop = getShop(shopId);
            shop.setCategoryDiscount(category, percentage, isDouble);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void removeCategoryDiscount(int shopId, ItemCategory category) {
        try {
            Shop shop = getShop(shopId);
            shop.removeCategoryDiscount(category);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void removeDiscountForItem(int shopId, int itemId) {
        try {
            Shop shop = getShop(shopId);
            shop.removeDiscountForItem(itemId);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void addReviewToShop(int shopId, int userId, int rating, String reviewText) {
        try {
            Shop shop = getShop(shopId);
            shop.addReview(userId, rating, reviewText);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public double getShopAverageRating(int shopId) {
        try {
            Shop shop = getShop(shopId);
            return shop.getAverageRating();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void addItemToShop(int shopId, int itemId, int quantity, int price) {
        try {
            Shop shop = getShop(shopId);
            shop.addItem(itemId, quantity);
            shop.updateItemPrice(itemId, price);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    

    @Override
    @Transactional
    public void addSupplyToItem(int shopId, int itemId, int quantity) {
        try {
            Shop shop = getShop(shopId);
            shop.addItem(itemId, quantity);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void updateItemPriceInShop(int shopId, int itemId, int price) {
        try {
            Shop shop = getShop(shopId);
            shop.updateItemPrice(itemId, price);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void removeItemFromShop(int shopId, int itemId) {
        try {
            Shop shop = getShop(shopId);
            shop.removeItemFromShop(itemId);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public int getItemQuantityFromShop(int shopId, int itemId) {
        try {
            Shop shop = getShop(shopId);
            return shop.getItemQuantity(itemId);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void closeShop(Integer shopId) {
        try {
            Shop shop = getShop(shopId);
            shop.setClosed(true);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public boolean checkSupplyAvailability(Integer shopId, Integer itemId) {
        try {
            Shop shop = getShop(shopId);
            return shop.getItemQuantity(itemId) > 0;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    
    public boolean checkSupplyAvailabilityAndAqcuire(Integer shopId, Integer itemId, Integer supply) {
        try {
            Shop shop = getShop(shopId);
            int currentSupply = shop.getItemQuantity(itemId);
            if (currentSupply >= supply) {
                return true;
            }
            return false;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void removeSupply(Integer shopId, Integer itemId, Integer supply) {
        try {
            Shop shop = getShop(shopId);
            int currentSupply = shop.getItemQuantity(itemId);
            if (currentSupply < supply) {
                throw new OurRuntime("Not enough supply to remove: " + supply + " from item: " + itemId);
            }
            shop.removeItemQuantity(itemId, currentSupply);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public boolean checkPolicy(HashMap<Integer, HashMap<Integer, Integer>> cart, String token) {
        return true; // Placeholder for policy check logic
    }

    @Override
    public List<Integer> getItemsByShop(Integer shopId) {
        try {
            Shop shop = getShop(shopId);
            return shop.getItemIds();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public List<Integer> getItems() {
        try {
            List<Shop> shops = getAllShops();
            List<Integer> lst = shops.stream()
                    .flatMap(shop -> shop.getItemIds().stream())
                    .collect((Collectors.toList()));
            return lst == null ? Collections.emptyList() : lst;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void addSupply(Integer shopId, Integer itemId, Integer supply) {
        try {
            Shop shop = getShop(shopId);
            shop.addItem(itemId, supply);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public double purchaseItems(Map<Integer, Integer> purchaseLists, Map<Integer, ItemCategory> itemsCategory,
            Integer shopdId) {
        try {
            Shop shop = getShop(shopdId);
            double ret =shop.purchaseItems(purchaseLists, itemsCategory);
            updateShop(shop);
            return ret;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void rollBackPurchase(Map<Integer, Integer> purchaseLists, Integer shopId) {
        try {
            Shop shop = getShop(shopId);
            shop.rollBackPurchase(purchaseLists);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public boolean shipPurchase(String name, int shopId, String country, String city, String street,
            String postalCode) {
        try {
            Shop shop = getShop(shopId);
            boolean ret = shop.getShippingMethod().processShipping(name, street, city, country, postalCode) != -1;
            updateShop(shop);
            return ret;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public List<Discount> getDiscounts(int shopId) {
        try {
            Shop shop = getShop(shopId);
            return shop.getDiscounts();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void setDiscountPolicy(int shopId, Policy policy) {
        try {
            Shop shop = getShop(shopId);
            shop.setDiscountPolicy(policy);
            updateShop(shop);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public List<Policy> getPolicies(int shopId) {
        try {
            Shop shop = getShop(shopId);
            return shop.getPolicies();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    public void deleteAll() {
        try {
            jpaRepo.deleteAll();
        } catch (Exception e) {
            throw new OurRuntime("Failed to delete all shops", e);
        }
    }

    private void updateShop(Shop updatedShop) {
        if (updatedShop == null) {
            throw new IllegalArgumentException("Shop cannot be null.");
        }

        updatedShop.prePersist();
        jpaRepo.save(updatedShop);
        

    }

    @Override
    public List<Integer> getClosedShops() {
        try {
            return Collections.unmodifiableList(
                    jpaRepo.findAll().stream()
                            .filter(Shop::isClosed)
                            .map(Shop::getId)
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving closed shops: " + e.getMessage(), e);
        }
    }

    @Override
    public double applyDiscount(Map<Integer, Integer> items, Map<Integer, ItemCategory> itemsCat, int shopId) {
        try {
            Shop shop = getShop(shopId);
            return shop.applyDiscount(items, itemsCat);
        } catch (RuntimeException e) {
            throw e;
        }
    }
}