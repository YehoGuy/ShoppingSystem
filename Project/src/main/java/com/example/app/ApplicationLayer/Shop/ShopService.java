package com.example.app.ApplicationLayer.Shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.Item.ItemService;
import com.example.app.ApplicationLayer.LoggerService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.ShippingMethod;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.DomainLayer.Shop.IShopRepository;
import com.example.app.DomainLayer.Shop.Operator;
import com.example.app.DomainLayer.Shop.PurchasePolicy;
import com.example.app.DomainLayer.Shop.Shop;
import com.example.app.DomainLayer.Shop.Discount.Discount;
import com.example.app.DomainLayer.User;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.Shop.Discount.Policy;
import com.example.app.DomainLayer.Shop.Discount.PolicyComposite;
import com.example.app.DomainLayer.Shop.Discount.PolicyLeaf;
import com.example.app.PresentationLayer.DTO.Shop.CompositePolicyDTO;
import com.example.app.PresentationLayer.DTO.Shop.LeafPolicyDTO;
import com.example.app.PresentationLayer.DTO.Shop.PoliciesDTO;

@Service
public class ShopService {
    private final IShopRepository shopRepository;
    private final AuthTokenService authTokenService;
    private final UserService userService;
    private final ItemService itemService;

    public ShopService(IShopRepository shopRepository,
            AuthTokenService authTokenService,
            UserService userService,
            ItemService itemService) {
        this.shopRepository = shopRepository;
        this.authTokenService = authTokenService;
        this.userService = userService;
        this.itemService = itemService;
    }

    public Shop createShop(String name, PurchasePolicy purchasePolicy, ShippingMethod shippingMethod, String token) {
        try {
            LoggerService.logMethodExecution("createShop", name, purchasePolicy);
            Integer userId = authTokenService.ValidateToken(token);
            userService.validateMemberId(userId);
            Shop returnShop = shopRepository.createShop(name, purchasePolicy, shippingMethod);
            Role founderRole = new Role(userId, returnShop.getId(), null);
            founderRole.setFoundersPermissions();
            userService.addFounderRole(userId, founderRole, returnShop.getId());
            LoggerService.logMethodExecutionEnd("createShop", returnShop);
            return returnShop;
        } catch (OurArg e) {
            LoggerService.logDebug("createShop", e);
            throw new OurArg("createShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("createShop", e);
            throw new OurRuntime("createShop" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("createShop", e, name, purchasePolicy);
            throw new OurRuntime("Error creating shop: " + e.getMessage(), e);
        }
    }

    public Shop getShop(int shopId, String token) {
        try {
            LoggerService.logMethodExecution("getShop", shopId);
            authTokenService.ValidateToken(token);
            Shop returnShop = shopRepository.getShop(shopId);
            LoggerService.logMethodExecutionEnd("getShop", returnShop);
            return returnShop;
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
            LoggerService.logMethodExecution("getAllShops", token);
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
            LoggerService.logError("getAllShops", e, token);
            throw new OurRuntime("Error retrieving all shops: " + e.getMessage(), e);
        }
    }

    public void updatePurchasePolicy(int shopId, PurchasePolicy newPolicy, String token) {
        try {
            LoggerService.logMethodExecution("updatePurchasePolicy", shopId, newPolicy);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                OurRuntime e = new OurRuntime(
                        "User does not have permission to update purchase policy for shop " + shopId);
                LoggerService.logDebug("updatePurchasePolicy", e);
                throw e;
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

    public void setGlobalDiscount(int shopId, int discount, boolean isDouble, String token) {
        try {
            LoggerService.logMethodExecution("setGlobalDiscount", shopId, discount);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                OurRuntime e = new OurRuntime("User does not have permission to update discount for shop " + shopId);
                LoggerService.logDebug("setGlobalDiscount", e);
                throw e;
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

    public void removeGlobalDiscount(int shopId, String token) {
        try {
            LoggerService.logMethodExecution("removeGlobalDiscount", shopId);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                OurRuntime e = new OurRuntime("User does not have permission to remove discount for shop " + shopId);
                LoggerService.logDebug("removeGlobalDiscount", e);
                throw e;
            }
            shopRepository.removeGlobalDiscount(shopId);
            LoggerService.logMethodExecutionEndVoid("removeGlobalDiscount");
        } catch (OurArg e) {
            LoggerService.logDebug("removeGlobalDiscount", e);
            throw new OurArg("removeGlobalDiscount" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeGlobalDiscount", e);
            throw new OurRuntime("removeGlobalDiscount" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("removeGlobalDiscount", e, shopId);
            throw new OurRuntime("Error removing global discount for shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public void setDiscountForItem(int shopId, int itemId, int discount, boolean isDouble, String token) {
        try {
            LoggerService.logMethodExecution("setDiscountForItem", shopId, itemId, discount);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                OurRuntime e = new OurRuntime("User does not have permission to update discount for shop " + shopId);
                LoggerService.logDebug("setDiscountForItem", e);
                throw e;
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
            throw new OurRuntime(
                    "Error setting discount for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public void removeDiscountForItem(int shopId, int itemId, String token) {
        try {
            LoggerService.logMethodExecution("removeDiscountForItem", shopId, itemId);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                OurRuntime e = new OurRuntime(
                        "User does not have permission to remove discount for item " + itemId + " in shop " + shopId);
                LoggerService.logDebug("removeDiscountForItem", e);
                throw e;
            }
            shopRepository.removeDiscountForItem(shopId, itemId);
            LoggerService.logMethodExecutionEndVoid("removeDiscountForItem");
        } catch (OurArg e) {
            LoggerService.logDebug("removeDiscountForItem", e);
            throw new OurArg("removeDiscountForItem" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeDiscountForItem", e);
            throw new OurRuntime("removeDiscountForItem" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("removeDiscountForItem", e, shopId, itemId);
            throw new OurRuntime(
                    "Error removing discount for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public void setCategoryDiscount(int shopId, ItemCategory category, int discount, boolean isDouble, String token) {
        try {
            LoggerService.logMethodExecution("setCategoryDiscount", shopId, category, discount);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                OurRuntime e = new OurRuntime("User does not have permission to update discount for shop " + shopId);
                LoggerService.logDebug("setCategoryDiscount", e);
                throw e;
            }
            shopRepository.setCategoryDiscount(shopId, category, discount, isDouble);
            LoggerService.logMethodExecutionEndVoid("setCategoryDiscount");
        } catch (OurArg e) {
            LoggerService.logDebug("setCategoryDiscount", e);
            throw new OurArg("setCategoryDiscount" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("setCategoryDiscount", e);
            throw new OurRuntime("setCategoryDiscount" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("setCategoryDiscount", e, shopId, category, discount);
            throw new OurRuntime(
                    "Error setting category discount for " + category + " in shop " + shopId + ": " + e.getMessage(),
                    e);
        }
    }

    public void removeCategoryDiscount(int shopId, ItemCategory category, String token) {
        try {
            LoggerService.logMethodExecution("removeCategoryDiscount", shopId, category);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                OurRuntime e = new OurRuntime(
                        "User does not have permission to remove discount for item " + category + " in shop " + shopId);
                LoggerService.logDebug("removeCategoryDiscount", e);
                throw e;
            }
            shopRepository.removeCategoryDiscount(shopId, category);
            LoggerService.logMethodExecutionEndVoid("removeCategoryDiscount");
        } catch (OurArg e) {
            LoggerService.logDebug("removeCategoryDiscount", e);
            throw new OurArg("removeCategoryDiscount" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeCategoryDiscount", e);
            throw new OurRuntime("removeCategoryDiscount" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("removeCategoryDiscount", e, shopId, category);
            throw new OurRuntime(
                    "Error removing category discount for " + category + " in shop " + shopId + ": " + e.getMessage(),
                    e);
        }
    }

    public void addReviewToShop(int shopId, int rating, String reviewText, String token) {
        try {
            LoggerService.logMethodExecution("addReviewToShop", shopId, rating, reviewText);
            Integer userId = authTokenService.ValidateToken(token);
            userService.validateMemberId(userId);
            if (userService.isSuspended(userId)) {
                OurRuntime e = new OurRuntime("User is suspended and cannot add a review.");
                LoggerService.logDebug("addReviewToShop", e);
                throw e;
            }
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

    public double getShopAverageRating(int shopId, String token) {
        try {
            LoggerService.logMethodExecution("getShopAverageRating", shopId);
            authTokenService.ValidateToken(token);
            double returnDouble = shopRepository.getShopAverageRating(shopId);
            LoggerService.logMethodExecutionEnd("getShopAverageRating", returnDouble);
            return returnDouble;
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

    public void addItemToShop(int shopId, String name, String description, int quantity, ItemCategory category,
            int price, String token) {
        try {
            LoggerService.logMethodExecution("addItemToShop", shopId, quantity, price);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)) {
                OurRuntime e = new OurRuntime("User does not have permission to add item to shop " + shopId);
                LoggerService.logDebug("addItemToShop", e);
                throw e;
            }
            Integer itemId = itemService.createItem(shopId, name, description, category, token);
            shopRepository.addItemToShop(shopId, itemId, quantity, price);
            LoggerService.logMethodExecutionEndVoid("addItemToShop");
        } catch (OurArg e) {
            LoggerService.logDebug("addItemToShop", e);
            throw new OurArg("addItemToShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("addItemToShop", e);
            throw new OurRuntime("addItemToShop" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("addItemToShop", e, shopId, quantity, price);
            throw new OurRuntime("Error adding item to shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public void addSupplyToItem(int shopId, int itemId, int quantity, String token) {
        try {
            LoggerService.logMethodExecution("addSupplyToItem", shopId, itemId, quantity);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)) {
                OurRuntime e = new OurRuntime(
                        "User does not have permission to add supply for item " + itemId + " in shop " + shopId);
                LoggerService.logDebug("addSupplyToItem", e);
                throw e;
            }
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
            throw new OurRuntime(
                    "Error adding supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public void updateItemPriceInShop(int shopId, int itemId, int price, String token) {
        try {
            LoggerService.logMethodExecution("updateItemPriceInShop", shopId, itemId, price);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)) {
                OurRuntime e = new OurRuntime(
                        "User does not have permission to update price for item " + itemId + " in shop " + shopId);
                LoggerService.logDebug("updateItemPriceInShop", e);
                throw e;
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
            throw new OurRuntime(
                    "Error updating price for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public void removeItemFromShop(int shopId, int itemId, String token) {
        try {
            LoggerService.logMethodExecution("removeItemFromShop", shopId, itemId);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)) {
                OurRuntime e = new OurRuntime(
                        "User does not have permission to remove item " + itemId + " from shop " + shopId);
                LoggerService.logDebug("removeItemFromShop", e);
                throw e;
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
            throw new OurRuntime("Error removing item " + itemId + " from shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public int getItemQuantityFromShop(int shopId, int itemId, String token) {
        try {
            LoggerService.logMethodExecution("getItemQuantityFromShop", shopId, itemId);
            authTokenService.ValidateToken(token);
            int returnInt = shopRepository.getItemQuantityFromShop(shopId, itemId);
            LoggerService.logMethodExecutionEnd("getItemQuantityFromShop", returnInt);
            return returnInt;
        } catch (OurArg e) {
            LoggerService.logDebug("getItemQuantityFromShop", e);
            throw new OurArg("getItemQuantityFromShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getItemQuantityFromShop", e);
            throw new OurRuntime("getItemQuantityFromShop" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("getItemQuantityFromShop", e, shopId, itemId);
            throw new OurRuntime(
                    "Error retrieving quantity for item " + itemId + " from shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public void closeShop(Integer shopId, String token) {
        try {
            LoggerService.logMethodExecution("closeShop", shopId);
            Integer userId = authTokenService.ValidateToken(token);
            if ((!userService.isAdmin(userId))
                    && (!userService.hasPermission(userId, PermissionsEnum.closeShop, shopId))) {
                OurRuntime e = new OurRuntime("User does not have permission to close shop " + shopId);
                LoggerService.logDebug("closeShop", e);
                throw e;
            }
            shopRepository.closeShop(shopId);
            userService.closeShopNotification(shopId);
            userService.removeOwnerFromStore(token, userId, shopId);
            LoggerService.logMethodExecutionEndVoid("closeShop");
        } catch (OurArg e) {
            LoggerService.logDebug("closeShop", e);
            throw new OurArg("closeShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("closeShop", e);
            throw new OurRuntime("closeShop" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("closeShop", e, shopId);
            throw new OurRuntime("Error closing shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public boolean checkSupplyAvailability(Integer shopId, Integer itemId, String token) {
        try {
            LoggerService.logMethodExecution("checkSupplyAvailability", shopId, itemId);
            authTokenService.ValidateToken(token);
            boolean returnBoolean = shopRepository.checkSupplyAvailability(shopId, itemId);
            LoggerService.logMethodExecutionEnd("checkSupplyAvailability", returnBoolean);
            return returnBoolean;
        } catch (OurArg e) {
            LoggerService.logDebug("checkSupplyAvailability", e);
            throw new OurArg("checkSupplyAvailability" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("checkSupplyAvailability", e);
            throw new OurRuntime("checkSupplyAvailability" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("checkSupplyAvailability", e, shopId, itemId);
            throw new OurRuntime(
                    "Error checking supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public double purchaseItems(Map<Integer, Integer> purchaseLists, Integer shopId, String token) {
        try {
            LoggerService.logMethodExecution("purchaseItems", purchaseLists, shopId);
            Integer userId = authTokenService.ValidateToken(token);
            if (userService.isSuspended(userId)) {
                OurRuntime e = new OurRuntime("User is suspended and cannot purchase items.");
                LoggerService.logDebug("purchaseItems", e);
                throw e;
            }
            Map<Integer, ItemCategory> itemsCategory = itemService.getItemdId2Cat(purchaseLists);
            double totalPrice = shopRepository.purchaseItems(purchaseLists, itemsCategory, shopId);
            LoggerService.logMethodExecutionEnd("purchaseItems", totalPrice);
            return totalPrice;
        } catch (OurArg e) {
            LoggerService.logDebug("purchaseItems", e);
            throw new OurArg("purchaseItems" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("purchaseItems", e);
            throw new OurRuntime("purchaseItems" + e.getMessage());
        }

        catch (Exception e) {
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
            LoggerService.logMethodExecution("checkSupplyAvailabilityAndAqcuire", shopId, itemId, supply);
            boolean returnBoolean = shopRepository.checkSupplyAvailabilityAndAqcuire(shopId, itemId, supply);
            LoggerService.logMethodExecutionEnd("checkSupplyAvailabilityAndAqcuire", returnBoolean);
            return returnBoolean;
        } catch (OurArg e) {
            LoggerService.logDebug("checkSupplyAvailabilityAndAqcuire", e);
            throw new OurArg("checkSupplyAvailabilityAndAqcuire" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("checkSupplyAvailabilityAndAqcuire", e);
            throw new OurRuntime("checkSupplyAvailabilityAndAqcuire" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("checkSupplyAvailability", e, shopId, itemId);
            throw new OurRuntime(
                    "Error checking supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public void addSupply(Integer shopId, Integer itemId, Integer supply, String token) {
        try {
            LoggerService.logMethodExecution("addSupply", shopId, itemId, supply);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)) {
                OurRuntime e = new OurRuntime(
                        "User does not have permission to add supply for item " + itemId + " in shop " + shopId);
                LoggerService.logDebug("addSupply", e);
                throw e;
            }
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
            throw new OurRuntime(
                    "Error adding supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public void removeSupply(Integer shopId, Integer itemId, Integer supply, String token) {
        try {
            LoggerService.logMethodExecution("removeSupply", shopId, itemId, supply);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)) {
                throw new OurRuntime(
                        "User does not have permission to remove supply for item " + itemId + " in shop " + shopId);
            }
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
            throw new OurRuntime(
                    "Error removing supply for item " + itemId + " in shop " + shopId + ": " + e.getMessage(), e);
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

    public List<Item> getItemsByShop(Integer shopId, String token) {
        try {
            LoggerService.logMethodExecution("getItems", shopId);
            authTokenService.ValidateToken(token);
            List<Integer> returnItems = shopRepository.getItemsByShop(shopId);
            LoggerService.logMethodExecutionEnd("getItems", returnItems);
            List<Item> items = itemService.getItemsByIds(returnItems, token);
            LoggerService.logMethodExecutionEnd("getItems", items);
            return items;
        } catch (OurArg e) {
            LoggerService.logDebug("getItems", e);
            throw new OurArg("getItems" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getItems", e);
            throw new OurRuntime("getItems" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("getItems", e, shopId);
            throw new OurRuntime("Error retrieving items for shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public List<Item> getItems(String token) {
        try {
            LoggerService.logMethodExecution("getItems");
            authTokenService.ValidateToken(token);
            List<Integer> returnItemsIds = shopRepository.getItems();
            LoggerService.logMethodExecutionEnd("getItems", returnItemsIds);
            return itemService.getItemsByIds(returnItemsIds, token);
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

    public List<Item> searchItems(String name, ItemCategory category, List<String> keywords, Integer minPrice,
            Integer maxPrice, Double minProductRating, Double minShopRating, String token) {
        try {
            LoggerService.logMethodExecution("searchItems", name, category, keywords, minPrice, maxPrice,
                    minProductRating, minShopRating);
            authTokenService.ValidateToken(token);
            List<Item> results = new ArrayList<>();
            for (Shop shop : getAllShops(token)) {
                if (minShopRating != null && shop.getAverageRating() < minShopRating)
                    continue;
                results.addAll(
                        filterItemsInShop(shop, name, category, keywords, minPrice, maxPrice, minProductRating, token));
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
            LoggerService.logError("searchItems", e, name, category, keywords, minPrice, maxPrice, minProductRating,
                    minShopRating);
            throw new OurRuntime("Error searching items: " + e.getMessage(), e);
        }
    }

    public List<Item> searchItemsInShop(Integer shopId, String name, ItemCategory category, List<String> keywords,
            Integer minPrice, Integer maxPrice, Double minProductRating, String token) {
        try {
            LoggerService.logMethodExecution("searchItemsInShop", shopId, name, category, keywords, minPrice, maxPrice,
                    minProductRating);
            authTokenService.ValidateToken(token);
            Shop shop = getShop(shopId, token);
            List<Item> results = filterItemsInShop(shop, name, category, keywords, minPrice, maxPrice, minProductRating,
                    token);
            LoggerService.logMethodExecutionEnd("searchItemsInShop", results);
            return results;
        } catch (OurArg e) {
            LoggerService.logDebug("searchItemsInShop", e);
            throw new OurArg("searchItemsInShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("searchItemsInShop", e);
            throw new OurRuntime("searchItemsInShop" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("searchItemsInShop", e, shopId, name, category, keywords, minPrice, maxPrice,
                    minProductRating);
            throw new OurRuntime("Error searching items in shop: " + e.getMessage(), e);
        }
    }

    private List<Item> filterItemsInShop(Shop shop, String name, ItemCategory category, List<String> keywords,
            Integer minPrice, Integer maxPrice, Double minProductRating, String token) {
        try {
            List<Item> results = new ArrayList<>();
            List<Item> shopItems = getItemsByShop(shop.getId(), token);
            for (Item item : shopItems) {
                if (name != null && !item.getName().toLowerCase().contains(name.toLowerCase()))
                    continue;
                if (category != null && item.getCategory() != category)
                    continue;
                if (keywords != null && !keywords.isEmpty()) {
                    String ln = item.getName().toLowerCase();
                    String ld = item.getDescription().toLowerCase();
                    boolean match = keywords.stream().map(String::toLowerCase)
                            .anyMatch(kw -> ln.contains(kw) || ld.contains(kw));
                    if (!match)
                        continue;
                }
                int price = shop.getItemPrice(item.getId());
                if (minPrice != null && price < minPrice)
                    continue;
                if (maxPrice != null && price > maxPrice)
                    continue;
                if (minProductRating != null && item.getAverageRating() < minProductRating)
                    continue;
                results.add(item);
            }
            return results;
        } catch (OurArg e) {
            LoggerService.logDebug("filterItemsInShop", e);
            throw new OurArg("filterItemsInShop" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("filterItemsInShop", e);
            throw new OurRuntime("filterItemsInShop" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("filterItemsInShop", e, shop, name, category, keywords, minPrice, maxPrice,
                    minProductRating);
            throw new OurRuntime("Error filtering items in shop: " + e.getMessage(), e);
        }
    }

    public void shipPurchase(String token, int purchaseId, int shopId, String country, String city, String street,
            String postalCode) {
        try {
            LoggerService.logMethodExecution("shipPurchase", purchaseId, country, city, street, postalCode);
            Integer userId = authTokenService.ValidateToken(token);
            User user = userService.getUserById(userId);
            String userName;
            if (user instanceof Member) {
                userName = ((Member) user).getUsername();
            } else {
                userName = "guest";
            }
            boolean b = shopRepository.shipPurchase(userName, shopId, country, city, street, postalCode);
            if (!b) {
                OurRuntime e = new OurRuntime("Failed to ship purchase " + purchaseId + " from shop " + shopId);
                LoggerService.logDebug("shipPurchase", e);
                throw e;
            }
            LoggerService.logMethodExecutionEndVoid("shipPurchase");
        } catch (OurArg e) {
            LoggerService.logDebug("shipPurchase", e);
            throw new OurArg("shipPurchase" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("shipPurchase", e);
            throw new OurRuntime("shipPurchase" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("shipPurchase", e, purchaseId, country, city, street, postalCode);
            throw new OurRuntime("Error shipping purchase " + purchaseId + ": " + e.getMessage(), e);
        }
    }

    public List<Shop> getShopsByWorker(int workerId, String token) {
        try {
            LoggerService.logMethodExecution("getShopsByWorker", workerId);
            List<Integer> shopIds = userService.getShopIdsByWorkerId(workerId);
            List<Shop> returnShops = new ArrayList<>();
            for (Integer shopId : shopIds) {
                Shop shop = getShop(shopId, token);
                returnShops.add(shop);
            }
            LoggerService.logMethodExecutionEnd("getShopsByWorker", returnShops);
            return returnShops;
        } catch (OurArg e) {
            LoggerService.logDebug("getShopsByWorker", e);
            throw new OurArg("getShopsByWorker" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getShopsByWorker", e);
            throw new OurRuntime("getShopsByWorker" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("getShopsByWorker", e, workerId);
            throw new OurRuntime("Error retrieving shops by worker " + workerId + ": " + e.getMessage(), e);
        }
    }

    public void setDiscountPolicy(int shopId, CompositePolicyDTO dto, String token) {
        try {
            LoggerService.logMethodExecution("setDiscountPolicy", shopId, dto);
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)) {
                throw new OurRuntime("No permission to set policy for shop " + shopId);
            }
            // convert DTO → domain Policy
            Policy policy = mapPolicyDTO(shopId, dto);
            shopRepository.setDiscountPolicy(shopId, policy);
            LoggerService.logMethodExecutionEndVoid("setDiscountPolicy");
        } catch (OurArg e) {
            LoggerService.logDebug("setDiscountPolicy", e);
            throw e;
        } catch (OurRuntime e) {
            LoggerService.logDebug("setDiscountPolicy", e);
            throw e;
        } catch (Exception e) {
            LoggerService.logError("setDiscountPolicy", e, shopId, dto);
            throw new OurRuntime("Error setting discount policy: " + e.getMessage(), e);
        }
    }

    /**
     * Recursively map CompositePolicyDTO to a Policy tree.
     */
    private Policy mapPolicyDTO(int shopId, CompositePolicyDTO dto) {
        // map left side
        Policy left;
        if (dto.getCompoPolicy1() != null) {
            left = mapPolicyDTO(shopId, dto.getCompoPolicy1());
        } else if (dto.getLeafPolicy1() != null) {
            left = mapLeafPolicy(dto.getLeafPolicy1(), shopId);
        } else {
            left = new PolicyLeaf(null, null, null, 0.0); // no requirement
        }
        // map right side
        Policy right;
        if (dto.getCompoPolicy2() != null) {
            right = mapPolicyDTO(shopId, dto.getCompoPolicy2());
        } else if (dto.getLeafPolicy2() != null) {
            right = mapLeafPolicy(dto.getLeafPolicy2(), shopId);
        } else {
            right = null;
        }
        // combine
        Operator op = dto.getOperator();
        if (right == null) {
            return new PolicyComposite(left, op);
        } else {
            return new PolicyComposite(left, right, op);
        }
    }

    /**
     * Map a single LeafPolicyDTO to a Policy using a TriPredicate.
     */
    private Policy mapLeafPolicy(LeafPolicyDTO leaf, int shopId) {
        // item-level
        if (leaf.getThreshold() != null && leaf.getItemId() != null) {
            int threshold = leaf.getThreshold();
            int itemId = leaf.getItemId();
            return new PolicyLeaf(threshold, itemId, null, 0.0);
        }
        // category-level
        if (leaf.getThreshold() != null && leaf.getItemCategory() != null) {
            int threshold = leaf.getThreshold();
            ItemCategory cat = leaf.getItemCategory();
            return new PolicyLeaf(threshold, null, cat, 0.0);
        }
        // basket-value
        double minValue = leaf.getBasketValue();
        return new PolicyLeaf(null, null, null, minValue);
    }

    public List<Discount> getDiscounts(int shopId, String token) {
        try {
            LoggerService.logMethodExecution("getDiscounts", shopId);
            authTokenService.ValidateToken(token);
            List<Discount> discounts = shopRepository.getDiscounts(shopId);
            LoggerService.logMethodExecutionEnd("getDiscounts", discounts);
            return discounts;
        } catch (OurArg e) {
            LoggerService.logDebug("getDiscounts", e);
            throw new OurArg("getDiscounts" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getDiscounts", e);
            throw new OurRuntime("getDiscounts" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("getDiscounts", e, shopId);
            throw new OurRuntime("Error retrieving discounts for shop " + shopId + ": " + e.getMessage(), e);
        }
    }

    public List<Policy> getPolicies(int shopId, String token) {
        try {
            Integer userId = authTokenService.ValidateToken(token);
            if (!userService.hasPermission(userId, PermissionsEnum.viewPolicy, shopId)) {
                throw new OurRuntime("No permission to view policies for shop " + shopId);
            }
            // println("getPolicies called for shop " + shopId);
            for (Policy p : shopRepository.getPolicies(shopId)) {
                if (p == null) {
                    //System.out.println("sdlkcsl;dkcsdkcsdds;k");
                } else {
                    //System.out.println("Policy4672829: " + p);

                }
            }
            return shopRepository.getPolicies(shopId);
        } catch (OurArg e) {
            LoggerService.logDebug("getPolicies", e);
            throw new OurArg("getPolicies" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getPolicies", e);
            throw new OurRuntime("getPolicies" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("getPolicies", e, shopId, token);
            throw new OurRuntime("Error retrieving policies for shop " + shopId + ": " + e.getMessage(), e);
        }
    }
}
