package com.example.app.ApplicationLayer.Purchase;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.Item.ItemService;
import com.example.app.ApplicationLayer.LoggerService;
import com.example.app.ApplicationLayer.Message.MessageService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Shop.ShopService;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.Bid;
import com.example.app.DomainLayer.Purchase.BidReciept;
import com.example.app.DomainLayer.Purchase.IPurchaseRepository;
import com.example.app.DomainLayer.Purchase.Purchase;
import com.example.app.DomainLayer.Purchase.Reciept;

import com.example.app.DomainLayer.Roles.PermissionsEnum;

import jakarta.validation.constraints.Min;

@Service
public class PurchaseService {

    private final IPurchaseRepository purchaseRepository;
    private AuthTokenService authTokenService;
    private UserService userService;
    private ItemService itemService;
    private ShopService shopService;
    private MessageService messageService;

    public PurchaseService(IPurchaseRepository purchaseRepository,
            AuthTokenService authTokenService,
            UserService userService,
            ShopService shopService,
            ItemService itemService,
            MessageService messageService) {
        this.messageService = messageService;
        this.purchaseRepository = purchaseRepository;
        this.authTokenService = authTokenService;
        this.userService = userService;
        this.shopService = shopService;
        this.itemService = itemService;
    }

    public void setServices(AuthTokenService authTokenService, UserService userService, ItemService itemService,
            ShopService shopService, MessageService messageService) {
        this.authTokenService = authTokenService;
        this.userService = userService;
        this.itemService = itemService;
        this.shopService = shopService;
        this.messageService = messageService;
    }

    public List<Integer> checkoutCart(String authToken, Address shippingAddress) {
        LoggerService.logMethodExecution("checkoutCart", authToken, shippingAddress);
        HashMap<Integer, HashMap<Integer, Integer>> aqcuired = new HashMap<>();
        HashMap<Integer, HashMap<Integer, Integer>> cartBackup = null;
        HashMap<Integer, Double> totalPrices = new HashMap<>();
        HashMap<Integer, Integer> purchaseIds = new HashMap<>();
        int userId = -1;
        try {
            userId = authTokenService.ValidateToken(authToken);
            HashMap<Integer, HashMap<Integer, Integer>> cart = userService.getUserShoppingCartItems(userId);
            cartBackup = cart;
            for (Integer shopId : cart.keySet()) {
                double totalPrice = shopService.purchaseItems(cart.get(shopId), shopId, authToken);
                totalPrices.put(shopId, totalPrice);
                aqcuired.put(shopId, cart.get(shopId));
                int pid = purchaseRepository.addPurchase(userId, shopId, aqcuired.get(shopId), totalPrice,
                        shippingAddress);
                purchaseIds.put(pid, shopId);
                userService.pay(authToken, shopId, totalPrice);
            }
            userService.clearUserShoppingCart(userId);
            for (Integer purchaseId : purchaseIds.keySet()) {
                shopService.shipPurchase(authToken, purchaseId, purchaseIds.get(purchaseId),
                        shippingAddress.getCountry(),
                        shippingAddress.getCity(), shippingAddress.getStreet(), shippingAddress.getZipCode());
            }
            LoggerService.logMethodExecutionEnd("checkoutCart", purchaseIds);
            userService.purchaseNotification(cart);
            return purchaseIds.keySet().stream().toList();
        } catch (OurArg e) {
            LoggerService.logDebug("checkoutCart", e);
            throw new OurArg("checkoutCart: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("checkoutCart", e);
            throw new OurRuntime("checkoutCart: " + e.getMessage(), e);
        } catch (Exception e) {
            for (Integer shopId : aqcuired.keySet()) {
                shopService.rollBackPurchase(aqcuired.get(shopId), shopId);
            }
            if (cartBackup != null) {
                userService.restoreUserShoppingCart(userId, cartBackup);
            }
            for (Integer shopId : totalPrices.keySet()) {
                userService.refundPaymentAuto(authToken, shopId, totalPrices.get(shopId));
            }
            LoggerService.logError("checkoutCart", e, authToken, shippingAddress);
            throw new OurRuntime("checkoutCart: " + e.getMessage(), e);
        }
    }

    public int createBid(String authToken, int storeId, Map<Integer, Integer> items, int initialPrice) {
        LoggerService.logMethodExecution("createBid", authToken, storeId, items);
        try {
            int userId = authTokenService.ValidateToken(authToken);
            shopService.purchaseItems(items, storeId, authToken);
            int purchaseId = purchaseRepository.addBid(userId, storeId, items, initialPrice);
            LoggerService.logMethodExecutionEnd("createBid", purchaseId);
            return purchaseId;
        } catch (OurArg e) {
            LoggerService.logDebug("createBid", e);
            throw new OurArg("createBid: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("createBid", e);
            throw new OurRuntime("createBid: " + e.getMessage(), e);
        } catch (Exception e) {
            shopService.rollBackPurchase(items, storeId);
            LoggerService.logError("createBid", e, authToken, storeId, items);
            throw new OurRuntime("createBid: " + e.getMessage(), e);
        }
    }

    public void postBidding(String authToken, int purchaseId, int bidAmount) {
        LoggerService.logMethodExecution("postBidding", authToken, purchaseId, bidAmount);
        try {
            int userId = authTokenService.ValidateToken(authToken);
            Purchase purchase = purchaseRepository.getPurchaseById(purchaseId);
            if (!(purchase instanceof Bid)) {
                throw new OurRuntime("Purchase " + purchaseId + " is not a bid");
            }
            if (purchase.getUserId() == userId) {
                throw new OurRuntime(
                        "User " + userId + " is the owner of the bid " + purchaseId + " and cannot bid on it");
            }
            ((Bid) purchase).addBidding(userId, bidAmount);
            LoggerService.logMethodExecutionEndVoid("postBidding");
        } catch (OurArg e) {
            LoggerService.logDebug("postBidding", e);
            throw new OurArg("postBidding: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("postBidding", e);
            throw new OurRuntime("postBidding: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("postBidding", e, authToken, purchaseId, bidAmount);
            throw new OurRuntime("postBidding: " + e.getMessage(), e);
        }
    }

    public int finalizeBid(String authToken, int purchaseId) {
        int initiatingUserId = -1;
        boolean payed = false;
        int highestBidderId = -1;
        int finalPrice = -1;
        int shopId = -1;
        try {
            LoggerService.logMethodExecution("finalizeBid", authToken, purchaseId);
            Purchase purchase = purchaseRepository.getPurchaseById(purchaseId);
            if (!(purchase instanceof Bid)) {
                throw new OurRuntime("Purchase " + purchaseId + " is not a bid");
            }
            initiatingUserId = authTokenService.ValidateToken(authToken);
            if (initiatingUserId != purchase.getUserId()) {
                throw new OurRuntime("User " + initiatingUserId + " is not the owner of the bid " + purchaseId);
            }
            Reciept receipt = purchase.completePurchase();
            highestBidderId = ((BidReciept) receipt).getHighestBidderId();
            finalPrice = ((Bid) purchase).getMaxBidding();
            shopId = purchase.getStoreId();
            userService.pay(authToken, shopId, finalPrice);
            payed = true;
            Address shippingAddress = userService.getUserShippingAddress(initiatingUserId);
            purchase.setAddress(shippingAddress);
            shopService.shipPurchase(authToken, purchaseId, shopId, shippingAddress.getCountry(),
                    shippingAddress.getCity(), shippingAddress.getStreet(), shippingAddress.getZipCode());
            try {
                List<Integer> bidders = ((Bid) purchase).getBiddersIds();
                for (Integer uid : bidders) {
                    String msg = (uid != highestBidderId)
                            ? "Bid " + purchaseId + " has been finalized. You didn't win.\n" + receipt
                            : "Congratulations! You have won the bid " + purchaseId + "!\n" + receipt;
                    messageService.sendMessageToUser(authToken, uid, msg, 0);
                }
            } catch (Exception ignored) {
            }
            LoggerService.logMethodExecutionEnd("finalizeBid", highestBidderId);
            return highestBidderId;
        } catch (OurArg e) {
            LoggerService.logDebug("finalizeBid", e);
            throw new OurArg("finalizeBid: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("finalizeBid", e);
            throw new OurRuntime("finalizeBid: " + e.getMessage(), e);
        } catch (Exception e) {
            if (payed) {
                userService.refundPaymentByStoreEmployee(authToken, highestBidderId, shopId, finalPrice);
            }
            LoggerService.logError("finalizeBid", e, authToken, purchaseId);
            throw new OurRuntime("Error finalizing bid " + purchaseId + ": " + e.getMessage(), e);
        }
    }

    public Purchase getPurchaseById(int purchaseId) {
        try {
            LoggerService.logMethodExecution("getPurchaseById", purchaseId);
            Purchase p = purchaseRepository.getPurchaseById(purchaseId);
            LoggerService.logMethodExecutionEnd("getPurchaseById", p);
            return p;
        } catch (OurArg e) {
            LoggerService.logDebug("getPurchaseById", e);
            throw new OurArg("getPurchaseById: " + purchaseId, e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getPurchaseById", e);
            throw new OurRuntime("getPurchaseById: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getPurchaseById", e, purchaseId);
            throw new OurArg("Invalid purchaseId: " + purchaseId, e);
        }
    }

    public List<Reciept> getUserPurchases(String authToken, int userId) {
        try {
            LoggerService.logMethodExecution("getUserPurchases", authToken, userId);
            if (authTokenService.ValidateToken(authToken) == userId) {
                List<Reciept> list = purchaseRepository.getUserPurchases(userId);
                LoggerService.logMethodExecutionEnd("getUserPurchases", list);
                return list;
            } else {
                throw new OurRuntime("Token does not match user ID.");
            }
        } catch (OurArg e) {
            LoggerService.logDebug("getUserPurchases", e);
            throw new OurArg("getUserPurchases: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getUserPurchases", e);
            throw new OurRuntime("getUserPurchases: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getUserPurchases", e, authToken, userId);
            throw new OurRuntime("Error retrieving user purchases: " + e.getMessage(), e);
        }
    }

    public List<Reciept> getReciept(int purchaseId) {
        try {
            LoggerService.logMethodExecution("getReciept", purchaseId);
            Purchase p = purchaseRepository.getPurchaseById(purchaseId);
            if (p == null) {
                throw new OurRuntime("Purchase " + purchaseId + " does not exist");
            }
            Reciept r = p.generateReciept();
            LoggerService.logMethodExecutionEnd("getReciept", r);
            return List.of(r);
        } catch (OurArg e) {
            LoggerService.logDebug("getReciept", e);
            throw new OurArg("getReciept: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getReciept", e);
            throw new OurRuntime("getReciept: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getReciept", e, purchaseId);
            throw new OurRuntime("Error retrieving reciept: " + e.getMessage(), e);
        }
    }

    public List<Reciept> getStorePurchases(String authToken, int shopId) {   
        try {
            LoggerService.logMethodExecution("getStorePurchases", authToken, shopId);
            int userId = authTokenService.ValidateToken(authToken);
            PermissionsEnum[] permissions = userService.getPermitionsByShop(authToken, shopId).get(userId);
            for(PermissionsEnum permission : permissions) {
                if (permission == PermissionsEnum.getHistory) {
                    LoggerService.logMethodExecutionEnd("getStorePurchases", null);
                    return purchaseRepository.getStorePurchases(shopId);
                }
            }
            List<Reciept> list = purchaseRepository.getStorePurchases(shopId);
            LoggerService.logMethodExecutionEnd("getStorePurchases", list);
            return list;
        } catch (OurArg e) {
            LoggerService.logDebug("getStorePurchases", e);
            throw new OurArg("getStorePurchases: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getStorePurchases", e);
            throw new OurRuntime("getStorePurchases: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getStorePurchases", e, authToken, shopId);
            throw new OurRuntime("Error retrieving store purchases: " + e.getMessage(), e);
        }
    }
    

}
