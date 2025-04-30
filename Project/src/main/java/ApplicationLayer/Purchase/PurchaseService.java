package ApplicationLayer.Purchase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.Item.ItemService;
import ApplicationLayer.LoggerService;
import ApplicationLayer.Message.MessageService;
import ApplicationLayer.Shop.ShopService;
import ApplicationLayer.User.UserService;
import ApplicationLayer.OurArg;
import ApplicationLayer.OurRuntime;
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
    private ShopService shopService;
    private MessageService messageService;

    public PurchaseService(IPurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public void setServices(AuthTokenService authTokenService, UserService userService, ShopService shopService, MessageService messageService) {
        this.authTokenService = authTokenService;
        this.userService = userService;
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
                double totalPrice = shopService.purchaseItems(cart.get(shopId), shopId);
                totalPrices.put(shopId, totalPrice);
                aqcuired.put(shopId, cart.get(shopId));
                int pid = purchaseRepository.addPurchase(userId, shopId, aqcuired.get(shopId), totalPrice, shippingAddress);
                purchaseIds.put(pid, shopId);
                userService.pay(authToken, shopId, totalPrice);
            }
            userService.clearUserShoppingCart(userId);
            LoggerService.logMethodExecutionEnd("checkoutCart", purchaseIds);
            return purchaseIds.keySet().stream().toList();
        } catch (OurArg e) {
            LoggerService.logDebug("checkoutCart", e);
            throw new OurArg("checkoutCart" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("checkoutCart", e);
            throw new OurRuntime("checkoutCart" + e.getMessage());
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

    public Integer createBid(String authToken, int storeId, Map<Integer, Integer> items, int initialPrice) {
        LoggerService.logMethodExecution("createBid", authToken, storeId, items, initialPrice);
        try {
            int userId = authTokenService.ValidateToken(authToken);
            shopService.purchaseItems(items, storeId);
            int purchaseId = purchaseRepository.addBid(userId, storeId, items, initialPrice);
            LoggerService.logMethodExecutionEnd("createBid", purchaseId);
            return purchaseId;
        } catch (OurArg e) {
            LoggerService.logDebug("createBid", e);
            throw new OurArg("createBid" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("createBid", e);
            throw new OurRuntime("createBid" + e.getMessage());
        } catch (Exception e) {
            shopService.rollBackPurchase(items, storeId);
            LoggerService.logError("createBid", e, authToken, storeId, items, initialPrice);
            throw new OurRuntime("Error creating bid: " + e.getMessage(), e);
        }
    }

    public void postBidding(String authToken, int purchaseId, int bidAmount) {
        LoggerService.logMethodExecution("postBidding", authToken, purchaseId, bidAmount);
        try {
            int userId = authTokenService.ValidateToken(authToken);
            Purchase purchase = purchaseRepository.getPurchaseById(purchaseId);
            if (!(purchase instanceof Bid)) {
                throw new OurArg("Purchase " + purchaseId + " is not a bid");
            }
            if (purchase.getUserId() == userId) {
                throw new OurArg("User " + userId + " cannot bid on own bid " + purchaseId);
            }
            ((Bid) purchase).addBidding(userId, bidAmount);
            LoggerService.logMethodExecutionEndVoid("postBidding");
        } catch (OurArg e) {
            LoggerService.logDebug("postBidding", e);
            throw new OurArg("postBidding" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("postBidding", e);
            throw new OurRuntime("postBidding" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("postBidding", e, authToken, purchaseId, bidAmount);
            throw new OurRuntime("Error posting bidding: " + e.getMessage(), e);
        }
    }

    public Integer finalizeBid(String authToken, int purchaseId) {
        LoggerService.logMethodExecution("finalizeBid", authToken, purchaseId);
        int initiatingUserId = -1;
        boolean payed = false;
        int highestBidderId = -1;
        int finalPrice = -1;
        int shopId = -1;
        try {
            Purchase purchase = purchaseRepository.getPurchaseById(purchaseId);
            if (!(purchase instanceof Bid)) {
                throw new OurArg("Purchase " + purchaseId + " is not a bid");
            }
            initiatingUserId = authTokenService.ValidateToken(authToken);
            if (initiatingUserId != purchase.getUserId()) {
                throw new OurArg("User " + initiatingUserId + " not owner of bid " + purchaseId);
            }
            Reciept receipt = purchase.completePurchase();
            highestBidderId = ((BidReciept) receipt).getHighestBidderId();
            finalPrice = ((Bid) purchase).getMaxBidding();
            shopId = purchase.getStoreId();
            userService.pay(authToken, shopId, finalPrice);
            payed = true;
            List<Integer> bidders = ((Bid) purchase).getBiddersIds();
            for (Integer uid : bidders) {
                try {
                    String messageContent = (uid != highestBidderId) ?
                            "Bid " + purchaseId + " finalized. You didn't win." :
                            "Congratulations! You won bid " + purchaseId + "!";
                    messageService.sendMessageToUser(authToken, uid, messageContent, 0);
                } catch (Exception ignored) {}
            }
            LoggerService.logMethodExecutionEnd("finalizeBid", highestBidderId);
            return highestBidderId;
        } catch (OurArg e) {
            LoggerService.logDebug("finalizeBid", e);
            throw new OurArg("finalizeBid" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("finalizeBid", e);
            throw new OurRuntime("finalizeBid" + e.getMessage());
        } catch (Exception e) {
            if (payed) {
                userService.refundPaymentByStoreEmployee(authToken, highestBidderId, shopId, finalPrice);
            }
            LoggerService.logError("finalizeBid", e, authToken, purchaseId);
            throw new OurRuntime("Error finalizing bid: " + e.getMessage(), e);
        }
    }

    public Purchase getPurchaseById(int purchaseId) {
        LoggerService.logMethodExecution("getPurchaseById", purchaseId);
        try {
            Purchase purchase = purchaseRepository.getPurchaseById(purchaseId);
            LoggerService.logMethodExecutionEnd("getPurchaseById", purchase);
            return purchase;
        } catch (OurArg e) {
            LoggerService.logDebug("getPurchaseById", e);
            throw new OurArg("getPurchaseById" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getPurchaseById", e);
            throw new OurRuntime("getPurchaseById" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("getPurchaseById", e, purchaseId);
            throw new OurRuntime("Invalid purchase ID: " + e.getMessage(), e);
        }
    }

    public List<Reciept> getUserPurchases(String authToken, int userId) {
        LoggerService.logMethodExecution("getUserPurchases", authToken, userId);
        try {
            if (authTokenService.ValidateToken(authToken) == userId) {
                List<Reciept> purchases = purchaseRepository.getUserPurchases(userId);
                LoggerService.logMethodExecutionEnd("getUserPurchases", purchases);
                return purchases;
            } else {
                throw new OurRuntime("Unauthorized access");
            }
        } catch (OurArg e) {
            LoggerService.logDebug("getUserPurchases", e);
            throw new OurArg("getUserPurchases" + e.getMessage());
        } catch (OurRuntime e) {
            LoggerService.logDebug("getUserPurchases", e);
            throw new OurRuntime("getUserPurchases" + e.getMessage());
        } catch (Exception e) {
            LoggerService.logError("getUserPurchases", e, authToken, userId);
            throw new OurRuntime("Error retrieving user purchases: " + e.getMessage(), e);
        }
    }
}
