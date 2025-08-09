package com.example.app.DomainLayer.Purchase;

import java.time.LocalDateTime;
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
    int addPurchase(int userId, int storeId, Map<Integer, Integer> items, double price, Address shippingAddresse);

    /**
     * Adds a bid to the repository.
     *
     * @param userId The ID of the user making the bid.
     * @param storeId The ID of the store where the bid is made.
     * @param items A map of item IDs to their quantities.
     * @return The ID of the newly created bid.
     * @throws IllegalArgumentException if the purchase ID already exists.
     */
    int addBid(int userId, int storeId, Map<Integer, Integer> items, int initialPrice);

    /**
     * Adds a bid to the repository with auction start and end times.
     *
     * @param userId The ID of the user making the bid.
     * @param storeId The ID of the store where the bid is made.
     * @param items A map of item IDs to their quantities.
     * @param initialPrice The initial price for the bid.
     * @param auctionStart The start time of the auction.
     * @param auctionEnd The end time of the auction.
     * @return The ID of the newly created bid.
     * @throws IllegalArgumentException if the purchase ID already exists.
     */
    int addBid(int userId, int storeId, Map<Integer, Integer> items, int initialPrice, LocalDateTime auctionStart, LocalDateTime auctionEnd);

    /**
     * Retrieves a purchase by its ID.
     *
     * @param purchaseId The ID of the purchase to retrieve.
     * @return Reciept Data Object for purchase with the specified ID.
     * @throws IllegalArgumentException if the purchase ID does not exist.
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

    /**
     * Retrieves all bids.
     *
     * @return A list of Bid data objects (BidReciept) for the available bids.
     */
    List<BidReciept> getAllBids();

    /**
     * Retrieves all bids offered by a specific shop.
     *
     * @param shopId The ID of the shop whose bids to retrieve.
     * @return A list of Bid data objects (BidReciept) for the bids offered by the specified shop.
     */
    List<BidReciept> getShopBids(int shopId);

    public void postBidding(Bid bid, int userId, int bidPrice);

    public void postBiddingAuction(Bid bid, int userId, int bidPrice);

    public void addReciept(Reciept reciept);

}