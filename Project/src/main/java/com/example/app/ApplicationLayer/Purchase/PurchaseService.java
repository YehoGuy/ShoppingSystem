package com.example.app.ApplicationLayer.Purchase;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.ZoneId;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.Item.ItemService;
import com.example.app.ApplicationLayer.LoggerService;
import com.example.app.ApplicationLayer.NotificationService;
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

@Service
public class PurchaseService {
    
    private final IPurchaseRepository purchaseRepository;
    private AuthTokenService authTokenService;
    private UserService userService;
    private ItemService itemService;
    private ShopService shopService;
    private MessageService messageService;
    private NotificationService notificationService; 
    // private NotificationService notificationService; 
    private TaskScheduler taskscheduler;

    public PurchaseService(IPurchaseRepository purchaseRepository,
            AuthTokenService authTokenService,
            UserService userService,
            ShopService shopService,
            ItemService itemService,
            MessageService messageService,
            NotificationService notificationService,
            TaskScheduler taskscheduler) {
        this.purchaseRepository = purchaseRepository;
        this.authTokenService = authTokenService;
        this.userService = userService;
        this.shopService = shopService;
        this.itemService = itemService;
        this.messageService = messageService;
        this.notificationService = notificationService;
        this.taskscheduler = taskscheduler;
    }

    public void setServices(AuthTokenService authTokenService, UserService userService, ItemService itemService,
            ShopService shopService, MessageService messageService) {
        this.authTokenService = authTokenService;
        this.userService = userService;
        this.itemService = itemService;
        this.shopService = shopService;
        this.messageService = messageService;
    }

    public List<Integer> checkoutCart(String authToken, Address shippingAddress, String currency, String cardNumber, String expirationDateMonth, String expirationDateYear, String cardHolderName, String cvv, String id) {
        LoggerService.logMethodExecution("checkoutCart", authToken, shippingAddress);
        HashMap<Integer, HashMap<Integer, Integer>> aqcuired = new HashMap<>();
        HashMap<Integer, HashMap<Integer, Integer>> cartBackup = null;
        HashMap<Integer, Double> totalPrices = new HashMap<>();
        HashMap<Integer, Integer> purchaseIds = new HashMap<>();
        List<Integer> paymentIds = new ArrayList<>();
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
                int payid = userService.pay(authToken, shopId, totalPrice, currency, cardNumber,
                        expirationDateMonth, expirationDateYear, cardHolderName, cvv, id);
                paymentIds.add(payid);
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
            for (Integer pid : paymentIds) {
                userService.refundPaymentAuto(authToken, pid);
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

    public BidReciept getBid(String authToken, int purchaseId) {
        LoggerService.logMethodExecution("getBid", authToken, purchaseId);
        try {
            authTokenService.ValidateToken(authToken);
            Purchase purchase = purchaseRepository.getPurchaseById(purchaseId);
            if (!(purchase instanceof Bid)) {
                throw new OurRuntime("Purchase " + purchaseId + " is not a bid");
            }
            LoggerService.logMethodExecutionEnd("getBid", purchaseId);
            return ((Bid) purchase).generateReciept();
        } catch (OurArg e) {
            LoggerService.logDebug("getBid", e);
            throw new OurArg("getBid: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getBid", e);
            throw new OurRuntime("getBid: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getBid", e, authToken, purchaseId);
            throw new OurRuntime("getBid: " + e.getMessage(), e);
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
            highestBidderId = ((Bid) purchase).getHighestBidderId();
            finalPrice = ((Bid) purchase).getMaxBidding();
            shopId = purchase.getStoreId();
            if (highestBidderId == -1) {
                throw new OurRuntime("No bids were placed on purchase " + purchaseId);
            }
            if (finalPrice == -1) {
                throw new OurRuntime("No final price was set for purchase " + purchaseId);
            }
            if (shopId == -1) {
                throw new OurRuntime("No shop ID was set for purchase " + purchaseId);
            }
            Map<Integer, Integer> items = purchase.getItems();
            userService.addBidToUserShoppingCart(highestBidderId, shopId, items);
            try {
                List<Integer> bidders = ((Bid) purchase).getBiddersIds();
                Reciept receipt = purchase.generateReciept();
                for (Integer uid : bidders) {
                    String msg = (uid != highestBidderId)
                            ? "Bid " + purchaseId + " has been finalized. You didn't win.\n" + receipt
                            : "Congratulations! You have won the bid " + purchaseId + "!\n" + receipt;
                    messageService.sendMessageToUser(authToken, uid, msg, 0);
                }
                purchase.completePurchase();
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

    public List<BidReciept> getAllBids(String authToken){
        try {
            LoggerService.logMethodExecution("getAllBids", authToken);
            authTokenService.ValidateToken(authToken);
            List<BidReciept> bids = purchaseRepository.getAllBids();
            LoggerService.logMethodExecutionEnd("getAllBids", bids);
            return bids;
        } catch (OurArg e) {
            LoggerService.logDebug("getAllBids", e);
            throw new OurArg("getAllBids: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getAllBids", e);
            throw new OurRuntime("getAllBids: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getAllBids", e, authToken);
            throw new OurRuntime("Error retrieving all bids: " + e.getMessage(), e);
        }
    }

    public List<BidReciept> getShopBids(String authToken, int shopId) {
        try {
            LoggerService.logMethodExecution("getShopBids", authToken, shopId);
            authTokenService.ValidateToken(authToken);
            List<BidReciept> bids = purchaseRepository.getShopBids(shopId);
            LoggerService.logMethodExecutionEnd("getShopBids", bids);
            return bids;
        } catch (OurArg e) {
            LoggerService.logDebug("getShopBids", e);
            throw new OurArg("getShopBids: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getShopBids", e);
            throw new OurRuntime("getShopBids: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getShopBids", e, authToken, shopId);
            throw new OurRuntime("Error retrieving shop bids: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("deprecation")
    public int startAuction(String authToken, int storeId, Map<Integer, Integer> items, int initialPrice, LocalDateTime auctionEndTime) {
        LoggerService.logMethodExecution("startAuction", authToken, storeId, items, initialPrice, auctionEndTime);
        try {
            int userId = authTokenService.ValidateToken(authToken);
            shopService.purchaseItems(items, storeId, authToken);
            int auctionId = purchaseRepository.addBid(userId, storeId, items, initialPrice, LocalDateTime.now(), auctionEndTime);
            taskscheduler.schedule(
                () -> finalizeAuction(auctionId, authToken),
                Date.from(auctionEndTime.atZone(ZoneId.systemDefault()).toInstant())
            );
            LoggerService.logMethodExecutionEnd("startAuction", auctionId);
            return auctionId;
        } catch (OurArg e) {
            LoggerService.logDebug("startAuction", e);
            throw new OurArg("startAuction: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("startAuction", e);
            throw new OurRuntime("startAuction: " + e.getMessage(), e);
        } catch (Exception e) {
            shopService.rollBackPurchase(items, storeId);
            LoggerService.logError("startAuction", e, authToken, storeId, items);
            throw new OurRuntime("startAuction: " + e.getMessage(), e);
        }
    }


    public void postBiddingAuction(String authToken, int auctionId, int bidAmount) {
        LoggerService.logMethodExecution("postBiddingAuction", authToken, auctionId);
        try {
            int userId = authTokenService.ValidateToken(authToken);
            Purchase purchase = purchaseRepository.getPurchaseById(auctionId);
            if (!(purchase instanceof Bid)) {
                throw new OurRuntime("Purchase " + auctionId + " is not a bid");
            }
            if (purchase.getUserId() == userId) {
                throw new OurRuntime(
                        "User " + userId + " is the owner of the bid " + auctionId + " and cannot bid on it");
            }
            ((Bid) purchase).addBidding(userId,bidAmount);
            
            postBidding(authToken, auctionId, bidAmount); ///?
            LoggerService.logMethodExecutionEndVoid("postBiddingAuction");
        } catch (OurArg e) {
            LoggerService.logDebug("postBiddingAuction", e);
            throw new OurArg("postBiddingAuction: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("postBiddingAuction", e);
            throw new OurRuntime("postBiddingAuction: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("postBiddingAuction", e, authToken, auctionId, 1);
            throw new OurRuntime("postBiddingAuction: " + e.getMessage(), e);
        }
    }

    private void finalizeAuction(int auctionId, String authToken) {
        int initiatingUserId = -1;
        int winnerId = -1;
        int finalPrice = -1;
        int shopId = -1;
        try {
            LoggerService.logMethodExecution("finalizeAuction", authToken, auctionId);
            Purchase purchase = purchaseRepository.getPurchaseById(auctionId);
            if (!(purchase instanceof Bid)) {
                throw new OurRuntime("Purchase " + auctionId + " is not a bid");
            }
            Bid bid = (Bid) purchase;
            bid.completePurchase();
            try {
                initiatingUserId = authTokenService.ValidateToken(authToken);
            } catch (Exception e) {
                LoggerService.logError("finalizeAuction", e, authToken, auctionId);
                throw new OurRuntime("finalizeAuction: " + e.getMessage(), e);
            }
            if (initiatingUserId != bid.getUserId()) {
                throw new OurRuntime("User " + initiatingUserId + " is not the owner of the bid");
            }
            winnerId = bid.getHighestBidderId();
            finalPrice = bid.getMaxBidding();
            shopId = bid.getStoreId();
            if (winnerId == -1) {
                throw new OurRuntime("No bids were placed on auction " + auctionId);
            }
            if (finalPrice == -1) {
                throw new OurRuntime("No final price was set for auction " + auctionId);
            }
            if (shopId == -1) {
                throw new OurRuntime("No shop was set for auction " + auctionId);
            }
            for (int pid : bid.getBiddersIds()) {
                notificationService.sendToUser(
                    pid,
                    "Auction ended",
                    (pid == winnerId ? " You won with a bid of " + finalPrice + "." : " You lost. The winning bid was " + finalPrice + ".")
                );
            }

            userService.addAuctionWinBidToUserShoppingCart(winnerId, bid);

            // save message to user that are not logged in
            for (int pid : bid.getBiddersIds()) {
                String msg = (pid != winnerId)
                    ? "Auction " + auctionId + " has ended. You didn't win.\n"
                    : "Congratulations! You have won the auction!\n" ;
                messageService.sendMessageToUser(authToken, pid, msg, -1);
            }
        } catch (OurArg e) {
            LoggerService.logDebug("finalizeAuction", e);
            throw new OurArg("finalizeAuction: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("finalizeAuction", e);
            throw new OurRuntime("finalizeAuction: " + e.getMessage(), e);
        } 
    }
 
   
}
