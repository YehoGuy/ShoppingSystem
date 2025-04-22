package InfrastructureLayer;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import DomainLayer.Purchase.Address;
import DomainLayer.Purchase.Bid;
import DomainLayer.Purchase.IPurchaseRepository;
import DomainLayer.Purchase.Purchase;

public class PurchaseRepository implements IPurchaseRepository {
    /**
    * This class handles the purchases data storage and retrieval.
    * It implement's the IPurchaseRepository interface to provide
    * methods for adding, retrieving, and deleting purchases.
    */

    // purchase_id --> Purchase
    private final ConcurrentHashMap<Integer, Purchase> purchaseStorage;
    // Singleton instance of PurchaseRepository.
    private static PurchaseRepository instance = null;
     // Counter for generating unique purchase IDs
    private int purchaseIdCounter = 0;

    /**
     * Returns the singleton instance of PurchaseRepository.
     *
     * @return The singleton instance of PurchaseRepository.
     */
    public static synchronized  PurchaseRepository getInstance() {
        if (instance == null) {
            instance = new PurchaseRepository();
        }
        return instance;
    }

    private PurchaseRepository() {
        this.purchaseStorage = new ConcurrentHashMap<>();
    }

    /**
     * Generates a new unique purchase ID.
     * -- a Note about concurrency --
     *  This method is synchronized to ensure that only one thread can access it at a time.
     *  This is important to ensure unique pruchase IDs.
     * @return A new unique purchase ID.
     */
    private synchronized int getNewPurchaseId() {
        return purchaseIdCounter++;
    }

    
    @Override
    /**
     * Adds a purchase to the repository.
     *
     * @param userId The ID of the user making the purchase.
     * @param storeId The ID of the store where the purchase is made.
     * @param items A map of item IDs to their quantities.
     * @param shippingAddresse The shipping address for the purchase.
     * @return The ID of the newly created purchase.
     */
    public int addPurchase(int userId, int storeId, Map<Integer, Integer> items, Address shippingAddresse) {
        int id = getNewPurchaseId();
        Purchase purchase = new Purchase(id, userId, storeId, items, shippingAddresse);
        purchaseStorage.put(id, purchase);
        return id;
    }

    @Override
    /**
     * Adds a bid to the repository.
     *
     * @param userId The ID of the user making the bid.
     * @param storeId The ID of the store where the bid is made.
     * @param items A map of item IDs to their quantities.
     * @return The ID of the newly created bid.
     * @throws UnsupportedOperationException if bids are not supported in this repository.
     */
    public int addBid(int userId, int storeId, Map<Integer, Integer> items) {
        int id = getNewPurchaseId();
        Bid bid = new Bid(id, userId, storeId, items);
        purchaseStorage.put(id, bid);
        return id;
    }

    @Override
    /**
     * Retrieves a purchase by its ID.
     *
     * @param purchaseId The ID of the purchase to retrieve.
     * @return The purchase with the specified ID.
     * @throws IllegalArgumentException if the purchase ID does not exist.
     */
    public Purchase getPurchaseById(int purchaseId) {
        Purchase p = purchaseStorage.get(purchaseId);
        if (p == null) {
            throw new IllegalArgumentException("Purchase not found, purchaseId: " + purchaseId);
        }
        return p;
    }

    @Override
    /**
     * Deletes a purchase by its ID.
     *
     * @param purchaseId The ID of the purchase to delete.
     */
    public void deletePurchase(int purchaseId) {
        purchaseStorage.remove(purchaseId);
    }

    @Override
    /**
     * Retrieves all purchases made by a specific user.
     *
     * @param userId The ID of the user whose purchases to retrieve.
     * @return A list of purchases made by the specified user.
     */
    public ArrayList<Purchase> getUserPurchases(int userId) {
        ArrayList<Purchase> userPurchases = new ArrayList<>();
        for (Purchase purchase : purchaseStorage.values()) {
            if (purchase.getUserId() == userId) {
                userPurchases.add(purchase);
            }
        }
        return userPurchases;
    }

    @Override
    /**
     * Retrieves all purchases made in a specific store.
     *
     * @param storeId The ID of the store whose purchases to retrieve.
     * @return A list of purchases made in the specified store.
     */
    public ArrayList<Purchase> getStorePurchases(int storeId) {
        ArrayList<Purchase> storePurchases = new ArrayList<>();
        for (Purchase purchase : purchaseStorage.values()) {
            if (purchase.getStoreId() == storeId) {
                storePurchases.add(purchase);
            }
        }
        return storePurchases;
    }

    @Override
    /**
     * Retrieves all purchases made by a specific user in a specific store.
     *
     * @param userId The ID of the user whose purchases to retrieve.
     * @param storeId The ID of the store whose purchases to retrieve.
     * @return A list of purchases made by the specified user in the specified store.
     */
    public ArrayList<Purchase> getUserStorePurchases(int userId, int storeId) {
        ArrayList<Purchase> userStorePurchases = new ArrayList<>();
        for (Purchase purchase : purchaseStorage.values()) {
            if (purchase.getUserId() == userId && purchase.getStoreId() == storeId) {
                userStorePurchases.add(purchase);
            }
        }
        return userStorePurchases;
    }   
    

}