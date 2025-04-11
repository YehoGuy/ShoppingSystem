package DomainLayer.Purchase;

import java.util.ArrayList;

public interface IPurchaseRepository {

    /**
     * add the given purchase to the repository.
     *
     * @param purchase The purchase to add.
     */
    void addPurchase(Purchase purchase);

    /**
     * Retrieves a purchase by its ID.
     *
     * @param purchaseId The ID of the purchase to retrieve.
     * @return The purchase with the specified ID, or null if not found.
     */
    Purchase getPurchaseById(int purchaseId);

    /**
     * Deletes a purchase by its ID.
     *
     * @param purchaseId The ID of the purchase to delete.
     */
    void deletePurchase(int purchaseId);

    /**
     * Retrieves all purchases made by a specific user.
     *
     * @param userId The ID of the user whose purchases to retrieve.
     * @return A list of purchases made by the specified user.
     */
    ArrayList<Purchase> getUserPurchases(int userId);

    /**
     * Retrieves all purchases made in a specific store.
     *
     * @param storeId The ID of the store whose purchases to retrieve.
     * @return A list of purchases made in the specified store.
     */
    ArrayList<Purchase> getStorePurchases(int storeId);

}
