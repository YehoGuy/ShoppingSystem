package InfrastructureLayer;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import DomainLayer.Purchase.Address;
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
     * @throws IllegalArgumentException if the purchase ID already exists.
     */
    public int addPurchase(int userId, int storeId, Map<Integer, Integer> items, Address shippingAddresse) {
        int id = getNewPurchaseId();
        Purchase purchase = new Purchase(id, userId, storeId, items, shippingAddresse);
        purchaseStorage.put(id, purchase);
        return id;
    }

    @Override
    /**
     * Retrieves a purchase by its ID.
     *
     * @param purchaseId The ID of the purchase to retrieve.
     * @return The purchase with the specified ID, or null if not found.
     */
    public Purchase getPurchaseById(int purchaseId) {
        return purchaseStorage.get(purchaseId);
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
        // Logic to retrieve all purchases made by a specific user
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
        // Logic to retrieve all purchases made in a specific store
        ArrayList<Purchase> storePurchases = new ArrayList<>();
        for (Purchase purchase : purchaseStorage.values()) {
            if (purchase.getStoreId() == storeId) {
                storePurchases.add(purchase);
            }
        }
        return storePurchases;
    }

    

}
