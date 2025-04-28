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
import DomainLayer.Purchase.Address;
import DomainLayer.Purchase.Bid;
import DomainLayer.Purchase.IPurchaseRepository;
import DomainLayer.Purchase.Purchase;

public class PurchaseService {

    private final IPurchaseRepository purchaseRepository;
    private AuthTokenService authTokenService;
    private UserService userService;
    private ItemService itemService;
    private ShopService shopService;
    private MessageService messageService;

    /**
     * Constructs a PurchaseService with the specified purchase repository.
     * 
     * @param purchaseRepository The repository for managing purchase data.
     */
    public PurchaseService(IPurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    /**
     * Sets the required services for the PurchaseService.
     * 
     * @param authTokenService The service for validating authentication tokens.
     * @param userService The service for managing user-related operations.
     * @param itemService The service for managing item-related operations.
     * @param shopService The service for managing shop-related operations.
     * @param messageService The service for sending messages to users.
     */
    public void setServices(AuthTokenService authTokenService, UserService userService, ItemService itemService, ShopService shopService, MessageService messageService) {
        this.authTokenService = authTokenService;
        this.userService = userService;
        this.itemService = itemService;
        this.shopService = shopService;
        this.messageService = messageService;
    }
    

    /**
     * Handles the checkout process for a user's shopping cart.
     * 
     * @param authToken The authentication token of the user.
     * @param userId The ID of the user.
     * @param shippingAddress The shipping address for the purchase.
     * @return A list of purchase IDs created during the checkout process.
     * @throws Exception If an error occurs during the checkout process.
     */
    public List<Integer> checkoutCart(String authToken, int userId, Address shippingAddress) throws Exception {
        LoggerService.logMethodExecution("checkoutCart", userId, shippingAddress);
        HashMap<Integer, HashMap<Integer, Integer>> aqcuired = new HashMap<>(); // shopId -> (itemId -> quantity)
        HashMap<Integer, HashMap<Integer, Integer>> cartBackup = null; // shopId -> (itemId -> quantity)
        HashMap<Integer,Double> totalPrices = new HashMap<>(); // shopId -> total price
        HashMap<Integer, Integer> purchaseIds = new HashMap<>(); // purchaseId -> shopId
        try {
            // 1. Validate the authToken and userId
            if(authTokenService.ValidateToken(authToken)==userId){
                // 2. retrieve user's cart
                HashMap<Integer, HashMap<Integer, Integer>> cart = userService.getUserShoppingCartItems(userId);
                cartBackup = cart; // backup the cart (cart is a deep copy of the original cart)
                // 3. create a purchase for each store (Repo creates)
                for(Integer shopId : cart.keySet()){
                    double totalPrice = shopService.purchaseItems(cart.get(shopId), shopId);
                    totalPrices.put(shopId, totalPrice);
                    aqcuired.put(shopId, cart.get(shopId));
                    int pid = purchaseRepository.addPurchase(userId, shopId, aqcuired.get(shopId), totalPrice, shippingAddress);
                    purchaseIds.put(pid,shopId);
                    // 4. handle payment
                    userService.pay(authToken, shopId, totalPrice);
                }
                // 6. remove items from the cart (backup before)
                userService.clearUserShoppingCart(userId);
                // 5. handle shipping
                ////for(Integer purchaseId : purchaseIds.keySet())
                ////    shopService.shipPurchase(purchaseId, purchaseIds.get(purchaseId), shippingAddress);
                // 7. LOG the purchase
                LoggerService.logMethodExecutionEnd("checkoutCart", purchaseIds);
                // 8. return purchase ID's
                return purchaseIds.keySet().stream().toList();
            } else{
                throw new IllegalArgumentException("Invalid authToken or userId");
            }
            
        } catch (Exception e) {
            // return Items to shop
            for(Integer shopId : aqcuired.keySet()){
                shopService.rollBackPurchase(aqcuired.get(shopId), shopId);
            }
            // restore the cart
            if(cartBackup != null){
                userService.restoreUserShoppingCart(userId, cartBackup);
            }
            // restore payment
            for(Integer shopId : totalPrices.keySet()){
                ///userService.refundPayment(authToken, shopId, totalPrices.get(shopId));
            }
            throw e;
        }
    }


    /**
     * Creates a bid for a store with the specified items.
     * 
     * @param authToken The authentication token of the user.
     * @param userId The ID of the user.
     * @param storeId The ID of the store.
     * @param items A map of item IDs to quantities for the bid.
     * @return The ID of the created bid.
     * @throws Exception If an error occurs during bid creation.
     */
    public int createBid(String authToken, int userId, int storeId, Map<Integer, Integer> items, int initialPrice) throws Exception{
        LoggerService.logMethodExecution("createBid", userId, storeId, items);
        Map<Integer, Integer> acquired = new HashMap<>();
        try {
            // 1. Validate the authToken & userId & userRole
            if(authTokenService.ValidateToken(authToken)==userId){
                // 2. check that all items exist in the store and acquire them
                shopService.purchaseItems(items, storeId);
                // 3. create a bid for the store (Repo creates)
                int purchaseId = purchaseRepository.addBid(userId, storeId, items);
                // 4. LOG the bid
                LoggerService.logMethodExecutionEnd("createBid", purchaseId);
                // 5. return purchase ID
                return purchaseId;
            } else{
                throw new IllegalArgumentException("Invalid authToken or userId");
            }
        } catch (Exception e) {
            // return items to shop
            shopService.rollBackPurchase(items, storeId);
            throw e;
        }
    }


    /**
     * Posts a bidding on an existing bid.
     * 
     * @param authToken The authentication token of the user.
     * @param userId The ID of the user.
     * @param purchaseId The ID of the bid to post the bidding on.
     * @param bidAmount The amount of the bid.
     * @throws Exception If an error occurs during the bidding process.
     */
    public void postBidding(String authToken, int userId, int purchaseId, double bidAmount) throws Exception{
        try {
            // 1. Validate the userId
            if(authTokenService.ValidateToken(authToken)==userId){
                // 2. check that the purchase is a bid
                Purchase purchase = purchaseRepository.getPurchaseById(purchaseId);
                if (!(purchase instanceof Bid)) 
                    throw new RuntimeException("Purchase "+purchaseId+" is not a bid");
                // 3. check that the user is not the owner of the bid
                if(purchase.getUserId() == userId)
                    throw new RuntimeException("User "+userId+" is the owner of the bid "+purchaseId+". thus Cannot bid on it");
                // 4. add the bidding to the purchase (Repo)
                ((Bid)purchase).addBidding(userId, bidAmount);
                // 5. LOG the bidding
                LoggerService.logMethodExecutionEnd("postBidding", purchaseId);
            } else{
                throw new IllegalArgumentException("Invalid authToken or userId");
            }
        } catch (Exception e) {
            throw e;
        }
    }


    /**
     * Finalizes a bid and determines the highest bidder.
     * 
     * @param authToken The authentication token of the user.
     * @param userId The ID of the user.
     * @param purchaseId The ID of the bid to finalize.
     * @return The ID of the highest bidder.
     * @throws Exception If an error occurs during bid finalization.
     */
    public int finalizeBid(String authToken, int userId, int purchaseId) throws Exception {
        try {
            LoggerService.logMethodExecution("finalizeBid", userId, purchaseId);
            // 1. Validate the userId & authToken
            if(authTokenService.ValidateToken(authToken)==userId){
                // 2. check that the purchase is a bid
                Purchase purchase = purchaseRepository.getPurchaseById(purchaseId);
                if (!(purchase instanceof Bid)) 
                    throw new RuntimeException("Purchase "+purchaseId+" is not a bid");
                // 3. check that the user is the owner of the bid
                if(purchase.getUserId() != userId)
                    throw new RuntimeException("User "+userId+" is not the owner of the bid "+purchaseId);
                // 4. finalize the bid (Repo)
                int highestBidderId = purchase.completePurchase();
                // 5. handle payment
                /// userService.getUserPaymentMethod(highestBidderId).processPayment(calcedPrice);
                // 6. handle shipping
                /// userService.getUserShippingMethod(highestBidderId).processShipping(purchase.getShippingAddress());
                // 7. notify the bidders
                List<Integer> bidders = ((Bid)purchase).getBiddersIds();
                for(Integer uid : bidders){
                    if(uid != highestBidderId)
                        messageService.sendMessageToUser(authToken, uid, "Bid "+purchaseId+" has been finalized. you did'nt win", 0);
                    else
                        messageService.sendMessageToUser(authToken, uid, "Congratulations! You have won the bid "+purchaseId+"!", 0);
                }
                // 8. LOG the purchase
                LoggerService.logMethodExecutionEnd("finalizeBid", highestBidderId);
                // 9. return highestBidder ID
                return highestBidderId;
            } else{
                throw new IllegalArgumentException("Invalid authToken or userId");
            }
        } catch (Exception e) {
            throw e;
        }
        
    }


    /**
     * Retrieves a purchase by its ID.
     * 
     * @param purchaseId The ID of the purchase to retrieve.
     * @return The Purchase object corresponding to the specified ID.
     * @throws IllegalArgumentException If the purchase ID is invalid.
     */
    public Purchase getPurchaseById(int purchaseId) {
        try {
            return purchaseRepository.getPurchaseById(purchaseId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid purchaseId");
        }
    }

    /**
     * Retrieves all purchases made by a user.
     * 
     * @param authToken The authentication token of the user.
     * @param userId The ID of the user.
     * @return A list of Purchase objects made by the user.
     * @throws RuntimeException If an error occurs while retrieving purchases.
     */
    public List<Purchase> getUserPurchases(String authToken, int userId) {
        try {
            // 1. Validate the userId & authToken
            if(authTokenService.ValidateToken(authToken)==userId){
                // 2. retrieve & return the purchases from the repository
                return purchaseRepository.getUserPurchases(userId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving user purchases: " + e.getMessage(), e);
        }
        return null;
    }

}