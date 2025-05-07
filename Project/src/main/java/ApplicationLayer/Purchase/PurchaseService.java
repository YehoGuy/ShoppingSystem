package ApplicationLayer.Purchase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.Item.ItemService;
import ApplicationLayer.LoggerService;
import ApplicationLayer.OurArg;
import ApplicationLayer.Message.MessageService;
import ApplicationLayer.OurRuntime;
import ApplicationLayer.Shop.ShopService;
import ApplicationLayer.User.UserService;
import DomainLayer.Purchase.Address;
import DomainLayer.Purchase.Bid;
import DomainLayer.Purchase.BidReciept;
import DomainLayer.Purchase.IPurchaseRepository;
import DomainLayer.Purchase.Purchase;
import DomainLayer.Purchase.Reciept;

public class PurchaseService {

    private final IPurchaseRepository purchaseRepository;
    private AuthTokenService authTokenService;
    private UserService userService;
    private ItemService itemService;
    private ShopService shopService;
    private MessageService messageService;

    public PurchaseService(IPurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public void setServices(AuthTokenService authTokenService, UserService userService, ItemService itemService, ShopService shopService, MessageService messageService) {
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
                int pid = purchaseRepository.addPurchase(userId, shopId, aqcuired.get(shopId), totalPrice, shippingAddress);
                purchaseIds.put(pid, shopId);
                userService.pay(authToken, shopId, totalPrice);
            }
            userService.clearUserShoppingCart(userId);
            for (Integer purchaseId : purchaseIds.keySet()) {
                shopService.shipPurchase(authToken, purchaseId, purchaseIds.get(purchaseId), shippingAddress.getCountry(),
                        shippingAddress.getCity(), shippingAddress.getStreet(), shippingAddress.getZipCode());
            }
            LoggerService.logMethodExecutionEnd("checkoutCart", purchaseIds);
            userService.purchaseNotification(cart);
            return purchaseIds.keySet().stream().toList();
        } catch (OurArg e) {
            LoggerService.logDebug("checkoutCart", e);
            throw e;
        } catch (OurRuntime e) {
            LoggerService.logDebug("checkoutCart", e);
            throw e;
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
            throw new OurRuntime("Error during checkout: " + e.getMessage(), e);
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
            throw e;
        } catch (OurRuntime e) {
            LoggerService.logDebug("createBid", e);
            throw e;
        } catch (Exception e) {
            shopService.rollBackPurchase(items, storeId);
            LoggerService.logError("createBid", e, authToken, storeId, items);
            throw new OurRuntime("Error creating bid: " + e.getMessage(), e);
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
                throw new OurRuntime("User " + userId + " is the owner of the bid " + purchaseId + " and cannot bid on it");
            }
            ((Bid) purchase).addBidding(userId, bidAmount);
            LoggerService.logMethodExecutionEndVoid("postBidding");
        } catch (OurArg e) {
            LoggerService.logDebug("postBidding", e);
            throw e;
        } catch (OurRuntime e) {
            LoggerService.logDebug("postBidding", e);
            throw e;
        } catch (Exception e) {
            LoggerService.logError("postBidding", e, authToken, purchaseId, bidAmount);
            throw new OurRuntime("Error posting bidding: " + e.getMessage(), e);
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
            shopService.shipPurchase(authToken, purchaseId, shopId, shippingAddress.getCountry(), shippingAddress.getCity(), shippingAddress.getStreet(), shippingAddress.getZipCode());
            try {
                List<Integer> bidders = ((Bid) purchase).getBiddersIds();
                for (Integer uid : bidders) {
                    String msg = (uid != highestBidderId)
                            ? "Bid " + purchaseId + " has been finalized. You didn't win.\n" + receipt
                            : "Congratulations! You have won the bid " + purchaseId + "!\n" + receipt;
                    messageService.sendMessageToUser(authToken, uid, msg, 0);
                }
            } catch (Exception ignored) {}
            LoggerService.logMethodExecutionEnd("finalizeBid", highestBidderId);
            return highestBidderId;
        } catch (OurArg e) {
            LoggerService.logDebug("finalizeBid", e);
            throw e;
        } catch (OurRuntime e) {
            LoggerService.logDebug("finalizeBid", e);
            throw e;
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
            throw e;
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
            throw e;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getUserPurchases", e);
            throw e;
        } catch (Exception e) {
            LoggerService.logError("getUserPurchases", e, authToken, userId);
            throw new OurRuntime("Error retrieving user purchases: " + e.getMessage(), e);
        }
    }
}
