package com.example.app.ApplicationLayer.Purchase;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.Bid;
import com.example.app.DomainLayer.Purchase.BidReciept;
import com.example.app.DomainLayer.Purchase.IPurchaseRepository;
import com.example.app.DomainLayer.Purchase.Purchase;
import com.example.app.DomainLayer.Purchase.Reciept;
import com.example.app.DomainLayer.Roles.PermissionsEnum;

import jakarta.validation.constraints.Min;
import java.util.HashSet;

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

    public List<Integer> checkoutCart(String authToken, Address shippingAddress, String currency, String cardNumber,
            String expirationDateMonth, String expirationDateYear, String cardHolderName, String cvv, String id) {
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

                int payid = userService.pay(authToken, shopId, totalPrice, currency, cardNumber,
                        expirationDateMonth, expirationDateYear, cardHolderName, cvv, id);
                paymentIds.add(payid);
                int pid = purchaseRepository.addPurchase(userId, shopId, aqcuired.get(shopId), totalPrice,
                        shippingAddress);
                purchaseIds.put(pid, shopId);
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
            // } catch (OurArg e) {
            // LoggerService.logDebug("checkoutCart", e);
            // throw new OurArg("checkoutCart: " + e.getMessage(), e);
            // } catch (OurRuntime e) {
            // for (Integer shopId : aqcuired.keySet()) {
            // shopService.rollBackPurchase(aqcuired.get(shopId), shopId);
            // }
            // if (cartBackup != null) {
            // userService.restoreUserShoppingCart(userId, cartBackup);
            // }
            // for (Integer pid : paymentIds) {
            // userService.refundPaymentAuto(authToken, pid);
            // }
            // LoggerService.logError("checkoutCart", e, authToken, shippingAddress);
            // throw new OurRuntime("checkoutCart: " + e.getMessage(), e);
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

    public List<Integer> partialCheckoutCart(String authToken, Address shippingAddress, String currency,
            String cardNumber, String expirationDateMonth, String expirationDateYear, String cardHolderName, String cvv,
            String id, int shopIdToBuy) {
        LoggerService.logMethodExecution("partialCheckoutCart", authToken, shippingAddress);
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
                if (shopId != shopIdToBuy) {
                    continue; // Skip shops that are not the one we want to buy from
                }
                aqcuired.put(shopId, cart.get(shopId));
                double totalPrice = shopService.purchaseItems(cart.get(shopId), shopId, authToken);
                totalPrices.put(shopId, totalPrice);

                int payid = userService.pay(authToken, shopId, totalPrice, currency, cardNumber,
                        expirationDateMonth, expirationDateYear, cardHolderName, cvv, id);
                paymentIds.add(payid);
                int pid = purchaseRepository.addPurchase(userId, shopId, aqcuired.get(shopId), totalPrice,
                        shippingAddress);
                purchaseIds.put(pid, shopId);
            }
            userService.clearUserShoppingCartByShopId(userId, shopIdToBuy);
            for (Integer purchaseId : purchaseIds.keySet()) {
                shopService.shipPurchase(authToken, purchaseId, purchaseIds.get(purchaseId),
                        shippingAddress.getCountry(),
                        shippingAddress.getCity(), shippingAddress.getStreet(), shippingAddress.getZipCode());
            }
            LoggerService.logMethodExecutionEnd("partialCheckoutCart", purchaseIds);
            userService.purchaseNotification(cart);
            return purchaseIds.keySet().stream().toList();
            // } catch (OurArg e) {
            // LoggerService.logDebug("checkoutCart", e);
            // throw new OurArg("checkoutCart: " + e.getMessage(), e);
            // } catch (OurRuntime e) {
            // for (Integer shopId : aqcuired.keySet()) {
            // shopService.rollBackPurchase(aqcuired.get(shopId), shopId);
            // }
            // if (cartBackup != null) {
            // userService.restoreUserShoppingCart(userId, cartBackup);
            // }
            // for (Integer pid : paymentIds) {
            // userService.refundPaymentAuto(authToken, pid);
            // }
            // LoggerService.logError("checkoutCart", e, authToken, shippingAddress);
            // throw new OurRuntime("checkoutCart: " + e.getMessage(), e);
        } catch (Exception e) {
            for (Integer shopId : aqcuired.keySet()) {
                shopService.rollBackPurchase(aqcuired.get(shopId), shopId);
            }
            if (cartBackup != null) {
                userService.restoreUserShoppingCartByShopId(userId, cartBackup, shopIdToBuy);
            }
            for (Integer pid : paymentIds) {
                userService.refundPaymentAuto(authToken, pid);
            }
            LoggerService.logError("partialCheckoutCart", e, authToken, shippingAddress);
            throw new OurRuntime("partialCheckoutCart: " + e.getMessage(), e);
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

    public void postBidding(String authToken, int purchaseId, int bidPrice) {
        LoggerService.logMethodExecution("postBidding", authToken, purchaseId, bidPrice);
        try {
            int userId = authTokenService.ValidateToken(authToken);
            Purchase purchase = purchaseRepository.getPurchaseById(purchaseId);
            if (!(purchase instanceof Bid)) {
                throw new OurRuntime("Purchase " + purchaseId + " is not a bid");
            }

            ((Bid) purchase).addBidding(userId, bidPrice, true);
            LoggerService.logMethodExecutionEndVoid("postBidding");
        } catch (OurArg e) {
            LoggerService.logDebug("postBidding", e);
            throw new OurArg("postBidding: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("postBidding", e);
            throw new OurRuntime("postBidding: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("postBidding", e, authToken, purchaseId, bidPrice);
            throw new OurRuntime("postBidding: " + e.getMessage(), e);
        }
    }

    public int finalizeBid(String authToken, int purchaseId, boolean fromAcceptBid) {
        int initiatingUserId = -1;
        boolean payed = false;
        int finalPrice = -1;
        int shopId = -1;
        try {
            LoggerService.logMethodExecution("finalizeBid", authToken, purchaseId);
            Purchase purchase = purchaseRepository.getPurchaseById(purchaseId);
            if (!(purchase instanceof Bid)) {
                throw new OurRuntime("Purchase " + purchaseId + " is not a bid");
            }
            initiatingUserId = authTokenService.ValidateToken(authToken);
            if (!fromAcceptBid) {
                if (initiatingUserId == purchase.getUserId()) {
                    throw new OurRuntime("You need to be shop's owner to finalize bid");
                }
            }
            shopId = purchase.getStoreId();
            int ownerId = userService.getShopOwner(shopId);
            if (!fromAcceptBid) {
                if (initiatingUserId != ownerId) {
                    throw new OurRuntime(
                            "User " + initiatingUserId +
                                    " is not the owner of shop " + shopId);
                }
            }
            finalPrice = ((Bid) purchase).getMaxBidding();
            if (finalPrice == -1) {
                throw new OurRuntime("No final price was set for purchase " + purchaseId);
            }
            if (shopId == -1) {
                throw new OurRuntime("No shop ID was set for purchase " + purchaseId);
            }
            Map<Integer, Integer> items = purchase.getItems();
            userService.addBidToUserShoppingCart(initiatingUserId, shopId, items);
            String msg = "The bid is finalized #"
                    + purchaseId
                    + ".\nIt has been added to your bids list.\n\n";
            notificationService.sendToUser(initiatingUserId, "The bid is over ", msg);
            purchase.completePurchase();
            userService.addBidToUserShoppingCart(initiatingUserId, shopId, items);
            LoggerService.logMethodExecutionEnd("finalizeBid", initiatingUserId);
            return initiatingUserId;
        } catch (OurArg e) {
            LoggerService.logDebug("finalizeBid", e);
            throw new OurArg("finalizeBid: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("finalizeBid", e);
            throw new OurRuntime("finalizeBid: " + e.getMessage(), e);
        } catch (Exception e) {
            if (payed) {
                userService.refundPaymentByStoreEmployee(authToken, initiatingUserId, shopId, finalPrice);
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
            for (PermissionsEnum permission : permissions) {
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

    public List<BidReciept> getAllBids(String authToken, boolean fromBid) {
        try {
            LoggerService.logMethodExecution("getAllBids", authToken);
            int userId = authTokenService.ValidateToken(authToken);
            // 1. fetch everything...
            List<BidReciept> bids = new ArrayList<>(purchaseRepository.getAllBids());

            if (fromBid) {
                // find shop owner of each bid - for owner only has to see all bids of his shops
                for (BidReciept bid : bids) {
                    int shopId = bid.getShopId();
                    int shopOwnerId = userService.getShopOwner(shopId);
                    if (shopOwnerId != userId) { // if the shop is closed the shopOwnerId = -1
                        // 2. filter out other users’ bids
                        bids = bids.stream()
                                .filter(b -> b.getUserId() == userId)
                                .collect(Collectors.toList());

                        // 3.Distinguish between the bids of the user to auctions he has participated in
                        // - present only the bids he has made
                        bids = bids.stream().filter(b -> b.getUserId() == userId
                                && b.getEndTime() == null).collect(Collectors.toList());

                        LoggerService.logMethodExecutionEnd("getAllBids", bids);
                    } else {
                        // 3.Distinguish between the bids of the user to auctions he has participated in
                        // - present only the bids he has made
                        bids = bids.stream().filter(b -> b.getEndTime() == null).collect(Collectors.toList());
                    }
                }
            } else {
                bids = bids.stream().filter(b -> b.getEndTime() != null).collect(Collectors.toList());

                LoggerService.logMethodExecutionEnd("getAllBids", bids);
            }
            // Sort the list finishedBids so it will return only the bids that the shop is
            // not close
            List<Integer> closedShopsIds = shopService.getclosedShops(authToken);
            bids.removeIf(b -> closedShopsIds.contains(b.getShopId()));

            // drop bids whose item no longer exists
            Set<Integer> validItemIds = new HashSet<>();
            for (BidReciept bid : bids) {
                Set<Integer> shopItemIds = shopService.searchItemsInShop(bid.getShopId(), null, null, Collections.emptyList(), null, null, null, authToken).stream()
                        .map(Item::getId)
                        .collect(Collectors.toSet());
                validItemIds.addAll(shopItemIds);
            }
            bids.removeIf(b -> b.getItems().keySet().stream()
                    .anyMatch(itemId -> !validItemIds.contains(itemId)));
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
    public int startAuction(String authToken, int storeId, Map<Integer, Integer> items, int initialPrice,
            LocalDateTime auctionEndTime) {
        LoggerService.logMethodExecution("startAuction", authToken, storeId, items, initialPrice, auctionEndTime);
        try {
            int userId = authTokenService.ValidateToken(authToken);
            shopService.purchaseItems(items, storeId, authToken);
            int auctionId = purchaseRepository.addBid(userId, storeId, items, initialPrice, LocalDateTime.now(),
                    auctionEndTime);
            taskscheduler.schedule(
                    () -> finalizeAuction(auctionId),
                    Date.from(auctionEndTime.atZone(ZoneId.systemDefault()).toInstant()));
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

    public void postBiddingAuction(String authToken, int auctionId, int bidPrice) {
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
            purchaseRepository.postBiddingAuction((Bid) purchase, userId, bidPrice);
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

    private void finalizeAuction(int auctionId) {
        int winnerId = -1;
        int finalPrice = -1;
        int shopId = -1;
        try {
            LoggerService.logMethodExecution("finalizeAuction", auctionId);
            Purchase purchase = purchaseRepository.getPurchaseById(auctionId);
            if (!(purchase instanceof Bid)) {
                throw new OurRuntime("Purchase " + auctionId + " is not a bid");
            }
            Bid bid = (Bid) purchase;
            bid.completePurchase();
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
                        (pid == winnerId ? " You won with a bid of " + finalPrice + "."
                                : " You lost. The winning bid was " + finalPrice + "."));
            }

            userService.addAuctionWinBidToUserShoppingCart(winnerId, bid);
        } catch (OurArg e) {
            LoggerService.logDebug("finalizeAuction", e);
            throw new OurArg("finalizeAuction: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("finalizeAuction", e);
            throw new OurRuntime("finalizeAuction: " + e.getMessage(), e);
        }
    }

    public void acceptBid(String authToken, int bidId) {
        try {
            LoggerService.logMethodExecution("acceptBid", bidId);
            Bid bid = (Bid) purchaseRepository.getPurchaseById(bidId);
            if (bid == null) {
                throw new OurRuntime("Bid " + bidId + " does not exist");
            }
            int userId;
            try {
                userId = authTokenService.ValidateToken(authToken);
            } catch (Exception e) {
                LoggerService.logError("acceptBid", e, authToken, bidId);
                throw new OurRuntime("acceptBid: " + e.getMessage(), e);
            }
            int shopOwnerId = userService.getShopOwner(bid.getStoreId());
            String msg = "User " + ((Member) userService.getUserById(userId)).getUsername()
                    + " accepted bid with price " + bid.getHighestBid();
            notificationService.sendToUser(shopOwnerId, authToken, msg);
            finalizeBid(authToken, bidId, true); // do it automatically
        } catch (OurArg e) {
            LoggerService.logDebug("acceptBid", e);
            throw new OurArg("acceptBid: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("acceptBid", e);
            throw new OurRuntime("acceptBid: " + e.getMessage(), e);
        }
    }

    public List<BidReciept> getFinishedBidsList(String authToken) {
        List<BidReciept> allBidsOfUser = getAllBids(authToken, true);
        int userId;
        try {
            userId = authTokenService.ValidateToken(authToken);
        } catch (Exception e) {
            LoggerService.logError("getFinishedBidsList", e, authToken);
            throw new OurRuntime("getFinishedBidsList: " + e.getMessage(), e);
        }
        // Sort the list so it will return only the bids that were finished
        List<BidReciept> finishedBids = new ArrayList<>();
        for (BidReciept bid : allBidsOfUser) {
            if (bid.isCompleted() == true && bid.getUserId() == userId) {
                finishedBids.add(bid);
            }
        }
        return finishedBids;
    }

    public List<BidReciept> getAuctionsWinList(String authToken) {
        try {
            int userId = authTokenService.ValidateToken(authToken);
            LoggerService.logMethodExecution("getAuctionsWinList", userId);
            List<BidReciept> auctionsWinList = userService.getAuctionsWinList(userId);
            // Sort auctionsWinList so it will return only the wins that the shop is not
            // close
            List<Integer> closedShopsIds = shopService.getclosedShops(authToken);
            auctionsWinList.removeIf(b -> closedShopsIds.contains(b.getShopId()));

            // drop aucntions whose item no longer exists
            Set<Integer> validItemIds = itemService.getAllItems(authToken).stream()
                    .map(Item::getId)
                    .collect(Collectors.toSet());
            auctionsWinList.removeIf(b -> b.getItems().keySet().stream()
                    .anyMatch(itemId -> !validItemIds.contains(itemId)));

            LoggerService.logMethodExecutionEnd("getAuctionsWinList", auctionsWinList);
            return auctionsWinList;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getAuctionsWinList", e);
            throw new OurRuntime("getAuctionsWinList: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("getAuctionsWinList", e);
            throw new OurArg("getAuctionsWinList: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getAuctionsWinList", e, authToken);
            throw new OurRuntime("getAuctionsWinList: " + e.getMessage(), e);
        }
    }

}
