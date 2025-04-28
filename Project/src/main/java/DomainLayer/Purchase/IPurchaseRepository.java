package DomainLayer.Purchase;

import java.util.List;
import java.util.Map;

public interface IPurchaseRepository {

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
    int addPurchase(int userId, int storeId, Map<Integer, Integer> items, Address shippingAddresse);

    /**
     * Adds a bid to the repository.
     *
     * @param userId The ID of the user making the bid.
     * @param storeId The ID of the store where the bid is made.
     * @param items A map of item IDs to their quantities.
     * @return The ID of the newly created bid.
     * @throws IllegalArgumentException if the purchase ID already exists.
     */
    int addBid(int userId, int storeId, Map<Integer, Integer> items);

    /**
     * Retrieves a purchase by its ID.
     *
     * @param purchaseId The ID of the purchase to retrieve.
     * @return Reciept Data Object for purchase with the specified ID.
     * @throws IllegalArgumentException if the purchase ID does not exist.
     */
    Reciept getPurchaseById(int purchaseId);

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
     * @return A list of Reciept data objects for the purchases made by the specified user.
     */
    List<Reciept> getUserPurchases(int userId);

    /**
     * Retrieves all purchases made in a specific store.
     *
     * @param storeId The ID of the store whose purchases to retrieve.
     * @return A list of Reciept data objects for the purchases made in the specified store.
     */
    List<Reciept> getStorePurchases(int storeId);

    /**
     * Retrieves all purchases made by a specific user in a specific store.
     *
     * @param userId The ID of the user whose purchases to retrieve.
     * @param storeId The ID of the store whose purchases to retrieve.
     * @return A list of Reciept data objects for the purchases made by the specified user in the specified store.
     */
    List<Reciept> getUserStorePurchases(int userId, int storeId);



}