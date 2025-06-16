package com.example.app.DBLayer.Shop;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.ShippingMethod;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.IShopRepository;
import com.example.app.DomainLayer.Shop.Operator;
import com.example.app.DomainLayer.Shop.PurchasePolicy;
import com.example.app.DomainLayer.Shop.Shop;
import com.example.app.DomainLayer.Shop.Discount.Discount;
import com.example.app.DomainLayer.Shop.Discount.Policy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class ShopRepositoryDBImpl implements IShopRepository {

    private ShopRepositoryDB jpaRepo;
    private AtomicInteger shopIdCounter = new AtomicInteger(1);

    @PersistenceContext
    private EntityManager entityManager;

    public ShopRepositoryDBImpl(@Lazy @Autowired ShopRepositoryDB jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Shop createShop(String name, PurchasePolicy purchasePolicy, ShippingMethod shippingMethod) {
        Shop shop = new Shop(shopIdCounter.getAndIncrement(), name, shippingMethod);
        Shop saved = jpaRepo.save(shop);
        if (saved == null) {
            throw new RuntimeException("Failed to save shop: " + shop);
        }
        return saved;
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

    @Override
    public void updatePurchasePolicy(int shopId, PurchasePolicy newPolicy) {
    }

    @Override
    public void setGlobalDiscount(int shopId, int discount, boolean isDouble) {
        try {
            Shop shop = getShop(shopId);
            shop.setGlobalDiscount(discount, isDouble);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void removeGlobalDiscount(int shopId) {
        try {
            Shop shop = getShop(shopId);
            shop.removeGlobalDiscount();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void setDiscountForItem(int shopId, int itemId, int discount, boolean isDouble) {
        try {
            Shop shop = getShop(shopId);
            shop.setDiscountForItem(itemId, discount, isDouble);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void setCategoryDiscount(int shopId, ItemCategory category, int percentage, boolean isDouble) {
        try {
            Shop shop = getShop(shopId);
            shop.setCategoryDiscount(category, percentage, isDouble);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void removeCategoryDiscount(int shopId, ItemCategory category) {
        try {
            Shop shop = getShop(shopId);
            shop.removeCategoryDiscount(category);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void removeDiscountForItem(int shopId, int itemId) {
        try {
            Shop shop = getShop(shopId);
            shop.removeDiscountForItem(itemId);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void addReviewToShop(int shopId, int userId, int rating, String reviewText) {
        try {
            Shop shop = getShop(shopId);
            shop.addReview(userId, rating, reviewText);
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
    public void addItemToShop(int shopId, int itemId, int quantity, int price) {
        try {
            Shop shop = getShop(shopId);
            shop.addItem(itemId, quantity);
            shop.updateItemPrice(itemId, price);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void addSupplyToItem(int shopId, int itemId, int quantity) {
        try {
            Shop shop = getShop(shopId);
            shop.addItem(itemId, quantity);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void updateItemPriceInShop(int shopId, int itemId, int price) {
        try {
            Shop shop = getShop(shopId);
            shop.updateItemPrice(itemId, price);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void removeItemFromShop(int shopId, int itemId) {
        try {
            Shop shop = getShop(shopId);
            shop.removeItemFromShop(itemId);
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
    public void closeShop(Integer shopId) {
        try {
            Shop shop = getShop(shopId);
            shop.setClosed(true);
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
    public void removeSupply(Integer shopId, Integer itemId, Integer supply) {
        try {
            Shop shop = getShop(shopId);
            int currentSupply = shop.getItemQuantity(itemId);
            if (currentSupply < supply) {
                throw new OurRuntime("Not enough supply to remove: " + supply + " from item: " + itemId);
            }
            shop.removeItemQuantity(itemId, currentSupply);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public boolean checkPolicy(HashMap<Integer, HashMap<Integer, Integer>> cart, String token) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkPolicy'");
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
    public void addSupply(Integer shopId, Integer itemId, Integer supply) {
        try {
            Shop shop = getShop(shopId);
            shop.addItem(itemId, supply);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public double purchaseItems(Map<Integer, Integer> purchaseLists, Map<Integer, ItemCategory> itemsCategory,
            Integer shopdId) {
        try {
            Shop shop = getShop(shopdId);
            return shop.purchaseItems(purchaseLists, itemsCategory);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void rollBackPurchase(Map<Integer, Integer> purchaseLists, Integer shopId) {
        try {
            Shop shop = getShop(shopId);
            shop.rollBackPurchase(purchaseLists);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public boolean shipPurchase(String name, int shopId, String country, String city, String street,
            String postalCode) {
        try {
            Shop shop = getShop(shopId);
            return shop.getShippingMethod().processShipping(name, street, city, country, postalCode) != -1;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public List<Discount> getDiscounts(int shopId) {
        try {
            Shop shop = getShop(shopId);
            return shop.getDiscounts();
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    public void setDiscountPolicy(int shopId, Policy policy) {
        try {
            Shop shop = getShop(shopId);
            shop.setDiscountPolicy(policy);
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

}
