package ApplicationLayer.Purchase;

import java.util.List;
import java.util.Map;

import DomainLayer.Purchase.Address;
import DomainLayer.Purchase.IPurchaseRepository;
import DomainLayer.Purchase.Purchase;

public class PurchaseService {

    IPurchaseRepository purchaseRepository;

    public PurchaseService(IPurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    //API Methods
    
    public int checkoutCart(String authToken, int userId, Address shippingAddress) {
        try {
            // 1. Validate the authToken and userId
            // 2. retrieve user's cart
            // 3. check that all items are available and save them
            // 4. create a purchase for each store (Repo creates)
            // 5. handle payment
            // 6. remove items from the cart
            // 7. LOG the purchase
            // 8. return purchase ID
        } catch (Exception e) {
            //  return propre error
        }
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public int createBid(String authToken, int userId, int storeId, Map<Integer, Integer> items) {
        try {
            // 1. Validate the authToken & userId & userRole
            // 2. check that all items exist in the store
            // 3. create a bid for the store (Repo creates)
            // 4. LOG the bid
            // 5. return purchase ID
        } catch (Exception e) {
            //  return propre error
        }
        
        throw new UnsupportedOperationException("Not implemented yet");
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
            // 2. retrieve the purchase and check that it is a bid
            Purchase p = getPurchaseById(purchaseId);
            
            // 3. check that the user has matching role in the Shop
            // 4. check that the Items are available in the store and "aqquire" them
            // 4. finalize the bid (Repo)
            // 5. handle payment
            // 6. notify the bidders
            // 7. LOG the purchase
            // 8. return something
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
            // 2. retrieve & return the purchases from the repository
            return purchaseRepository.getUserPurchases(userId);
        } catch (Exception e) {
            //  return propre error
        }
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<Purchase> getStorePurchases(String authToken, int userId, int storeId) {
        try{
            // 1. Validate the storeId & authToken & userId & userRole
            // 2. retrieve & return the purchases from the repository
            return purchaseRepository.getStorePurchases(storeId);
        } catch (Exception e) {
            //  return propre error
        }
        
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<Purchase> getUserPurchasesInStore(String authToken, int userId, int storeId) {
        try {
            // 1. Validate the authToken & userId
            // 2. retrieve & return the purchases from the repository
            return purchaseRepository.getUserStorePurchases(userId, storeId);
        } catch (Exception e) {
            //  return propre error
        }
        
        throw new UnsupportedOperationException("Not implemented yet");
    }
    
}
