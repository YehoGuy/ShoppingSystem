package com.example.app.InfrastructureLayer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.Bid;
import com.example.app.DomainLayer.Purchase.BidReciept;
import com.example.app.DomainLayer.Purchase.IPurchaseRepository;
import com.example.app.DomainLayer.Purchase.Purchase;
import com.example.app.DomainLayer.Purchase.Reciept;

@Repository
@Profile("no-db | test")
public class PurchaseRepository implements IPurchaseRepository {
    /**
     * This class handles the purchases data storage and retrieval.
     * It implement's the IPurchaseRepository interface to provide
     * methods for adding, retrieving, and deleting purchases.
     */

    // purchase_id --> Purchase
    private final ConcurrentHashMap<Integer, Purchase> purchaseStorage;
    // Counter for generating unique purchase IDs
    private final AtomicInteger purchaseIdCounter = new AtomicInteger(1);

    public PurchaseRepository() {
        this.purchaseStorage = new ConcurrentHashMap<>();
    }

    /**
     * Generates a new unique purchase ID.
     *
     * @return A new unique purchase ID.
     */
    // no need to be synchronized, as AtomicInteger is thread-safe
    private int getNewPurchaseId() {
        return purchaseIdCounter.getAndIncrement();
    }

    @Override
    /**
     * Adds a purchase to the repository.
     *
     * @param userId           The ID of the user making the purchase.
     * @param storeId          The ID of the store where the purchase is made.
     * @param items            A map of item IDs to their quantities.
     * @param shippingAddresse The shipping address for the purchase.
     * @return The ID of the newly created purchase.
     */
    public int addPurchase(int userId, int storeId, Map<Integer, Integer> items, double price,
            Address shippingAddresse) {
        int id = getNewPurchaseId();
        Purchase purchase = new Purchase(id, userId, storeId, items, price, shippingAddresse);
        purchaseStorage.put(id, purchase);
        return id;
    }

    @Override
    /**
     * Adds a bid to the repository.
     *
     * @param userId  The ID of the user making the bid.
     * @param storeId The ID of the store where the bid is made.
     * @param items   A map of item IDs to their quantities.
     * @return The ID of the newly created bid.
     * @throws UnsupportedOperationException if bids are not supported in this
     *                                       repository.
     */
    public int addBid(int userId, int storeId, Map<Integer, Integer> items, int initialPrice) {
        int id = getNewPurchaseId();
        Bid bid = new Bid(id, userId, storeId, items, initialPrice);
        purchaseStorage.put(id, bid);
        return id;
    }

    @Override
    /**
     * Adds a bid to the repository with auction start and end times.
     *
     * @param userId       The ID of the user making the bid.
     * @param storeId      The ID of the store where the bid is made.
     * @param items        A map of item IDs to their quantities.
     * @param initialPrice The initial price for the bid.
     * @param auctionStart The start time of the auction.
     * @param auctionEnd   The end time of the auction.
     * @return The ID of the newly created bid.
     */
    public int addBid(int userId, int storeId, Map<Integer, Integer> items, int initialPrice,
            LocalDateTime auctionStart, LocalDateTime auctionEnd) {
        int id = getNewPurchaseId();
        Bid bid = new Bid(id, userId, storeId, items, initialPrice, auctionStart, auctionEnd);
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
    public List<Reciept> getUserPurchases(int userId) {
        return purchaseStorage.values().stream()
                .filter(purchase -> purchase.getUserId() == userId)
                .map(Purchase::generateReciept)
                .toList();
    }

    @Override
    /**
     * Retrieves all purchases made in a specific store.
     *
     * @param storeId The ID of the store whose purchases to retrieve.
     * @return A list of purchases made in the specified store.
     */
    public List<Reciept> getStorePurchases(int storeId) {
        return purchaseStorage.values().stream()
                .filter(purchase -> purchase.getStoreId() == storeId)
                .map(Purchase::generateReciept)
                .toList();
    }

    @Override
    /**
     * Retrieves all purchases made by a specific user in a specific store.
     *
     * @param userId  The ID of the user whose purchases to retrieve.
     * @param storeId The ID of the store whose purchases to retrieve.
     * @return A list of purchases made by the specified user in the specified
     *         store.
     */
    public List<Reciept> getUserStorePurchases(int userId, int storeId) {
        return purchaseStorage.values().stream()
                .filter(purchase -> purchase.getUserId() == userId && purchase.getStoreId() == storeId)
                .map(Purchase::generateReciept)
                .toList();
    }

    @Override
    /**
     * Retrieves all bids.
     *
     * @return A list of bids available in the repository.
     */
    public List<BidReciept> getAllBids() {
        return purchaseStorage.values().stream()
                .filter(purchase -> purchase instanceof Bid)
                .map(purchase -> ((Bid) purchase).generateReciept())
                .toList();
    }

    @Override
    /**
     * Retrieves all bids offered by a specific shop.
     *
     * @param shopId The ID of the shop whose bids to retrieve.
     * @return A list of bids offered by the specified shop.
     */
    public List<BidReciept> getShopBids(int shopId) {
        return purchaseStorage.values().stream()
                .filter(purchase -> purchase instanceof Bid && purchase.getStoreId() == shopId)
                .map(purchase -> ((Bid) purchase).generateReciept())
                .toList();
    }


    /**
     * Posts a bid to an auction.
     * This method adds a bidding to an existing bid.
     * @param bid      The bid to which the bidding is added.
     * @param userId   The ID of the user making the bid.
     * @param bidPrice The price of the bid.
     */
   @Override
    public void postBiddingAuction(Bid bid, int userId, int bidPrice) {
        bid.addBidding(userId, bidPrice, false);
    }

    /**
     * Posts a bid to a non-auction purchase.
     * This method adds a bidding to an existing bid.
     * @param bid      The bid to which the bidding is added.
     * @param userId   The ID of the user making the bid.
     * @param bidPrice The price of the bid.
     */
    @Override
    public void postBidding(Bid bid, int userId, int bidPrice) {
        bid.addBidding(userId, bidPrice, false);
    }

    @Override
    public void addReciept(Reciept reciept) {
    }

}