package ApplicationLayer.Purchase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.Item.ItemService;
import ApplicationLayer.LoggerService;
import ApplicationLayer.Shop.ShopService;
import ApplicationLayer.User.UserService;
import DomainLayer.Purchase.Address;
import DomainLayer.Purchase.IPurchaseRepository;
import DomainLayer.Purchase.Purchase;

public class PurchaseService {

    private final IPurchaseRepository purchaseRepository;
    private AuthTokenService authTokenService;
    private UserService userService;
    private ItemService itemService;
    private ShopService shopService;

    public PurchaseService(IPurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    public void setServices(AuthTokenService authTokenService, UserService userService, ItemService itemService, ShopService shopService) {
        this.authTokenService = authTokenService;
        this.userService = userService;
        this.itemService = itemService;
        this.shopService = shopService;
    }
    
    //API Methods
    
    public List<Integer> checkoutCart(String authToken, int userId, Address shippingAddress) {
        LoggerService.logMethodExecution("checkoutCart", userId, shippingAddress);
        List<Integer> purchaseIds = new ArrayList<>();
        HashMap<Integer, HashMap<Integer, Integer>> aqcuired = new HashMap<>();
        HashMap<Integer, HashMap<Integer, Integer>> cartBackup = null;
        try {
            // 1. Validate the authToken and userId
            if(authTokenService.ValidateToken(authToken)==userId){
                // 2. retrieve user's cart
                HashMap<Integer, HashMap<Integer, Integer>> cart = userService.getUserShoppingCartItems(userId);
                // 3. check that all items are available and save them
                for(Integer shopId : cart.keySet()){
                    aqcuired.put(shopId, new HashMap<>());
                    for(Integer itemId : cart.get(shopId).keySet()){
                        boolean aqcSuccess = shopService.checkSupplyAvailabilityAndAcquire(shopId, itemId, cart.get(shopId).get(itemId));
                        if(aqcSuccess)
                            aqcuired.put(shopId, cart.get(shopId));
                        else
                            throw new RuntimeException("Item "+itemId+" not available in shop "+shopId+" in quantity "+cart.get(shopId).get(itemId));
                    }
                }
                // 4. create a purchase for each store (Repo creates)
                for(Integer shopId : aqcuired.keySet()){
                    int pid = purchaseRepository.addPurchase(userId, shopId, aqcuired.get(shopId), shippingAddress);
                    purchaseIds.add(pid);
                    // 5. handle payment
                    ///userService.getUserPaymentMethod(userId).processPayment(calcedPrice);
                    // 6. handle shipping
                    ///userService.getUserShippingMethod(userId).processShipping(shippingAddress);
                    // 7. remove items from the cart (backup before)
                    cartBackup = cart; // cart is a deep copy of the original cart
                    userService.clearUserShoppingCart(userId);
                    // 8. LOG the purchase
                    LoggerService.logMethodExecutionEnd("checkoutCart", purchaseIds);
                    // 9. return purchase ID's
                    return purchaseIds;
                }
            } else{
                throw new IllegalArgumentException("Invalid authToken or userId");
            }
            
        } catch (Exception e) {
            // return Items to shop
            for(Integer shopId : aqcuired.keySet()){
                for(Integer itemId : aqcuired.get(shopId).keySet()){
                    shopService.addSupply(shopId, itemId, aqcuired.get(shopId).get(itemId));
                }
            }
            if(cartBackup != null){
                // restore the cart
                userService.restoreUserShoppingCart(userId, cartBackup);
            }
            throw new RuntimeException("Error during checkout: " + e.getMessage(), e);
        }
        return null; 
    }

    public int createBid(String authToken, int userId, int storeId, Map<Integer, Integer> items) {
        LoggerService.logMethodExecution("createBid", userId, storeId, items);
        try {
            // 1. Validate the authToken & userId & userRole
            if(authTokenService.ValidateToken(authToken)==userId){
                // 2. check that all items exist in the store and acquire them
                for(Integer itemId : items.keySet()){
                    if(!shopService.checkSupplyAvailabilityAndAcquire(storeId, itemId, items.get(itemId))){
                        throw new RuntimeException("Item "+itemId+" not available in shop "+storeId);
                    }
                }
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
            for(Integer itemId : items.keySet()){
                shopService.addSupply(storeId, itemId, items.get(itemId));
            }
            throw new RuntimeException("Error during bid creation: " + e.getMessage(), e);
        }
    }

    public void postBidding(String authToke, int userId, int purchaseId, double bidAmount) {
        try {
            // 1. Validate the userId
            // 2. check that the purchase is a bid
            // 3. check that the user is not the owner of the bid
            // 4. add the bidding to the purchase (Repo)
            // 5. LOG the bidding
            // 6. return purchase ID
        } catch (Exception e) {
            //  return propre error
        }
        
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public int finalizeBid(String authToken, int userId, int purchaseId) {
        try {
            // 1. Validate the userId & authToken
            if(authTokenService.ValidateToken(authToken)==userId){
                // 2. check that the purchase is a bid
                // 3. check that the user is not the owner of the bid
                // 4. finalize the bid (Repo)
                // 5. handle payment
                // 6. notify the bidders
                // 7. LOG the purchase
                // 8. return purchase ID
            } else{
                throw new IllegalArgumentException("Invalid authToken or userId");
            }
        } catch (Exception e) {
            //  return propre error
        }
        
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Purchase getPurchaseById(int purchaseId) {
        try {
            // 1. Validate the purchaseId
            // 2. retrieve & return the purchase from the repository
            return purchaseRepository.getPurchaseById(purchaseId);
        } catch (Exception e) {
            //  return propre error
        }
        throw new UnsupportedOperationException("Not implemented yet");
    }

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
