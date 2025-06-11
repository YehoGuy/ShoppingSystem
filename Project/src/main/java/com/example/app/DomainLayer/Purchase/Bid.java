package com.example.app.DomainLayer.Purchase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Bid extends Purchase{

    private final AtomicInteger initialPrice;
    private final ConcurrentHashMap<Integer,Object> biddersIds = new ConcurrentHashMap<>();
    private final AtomicInteger highestBid; // initialPrice if no Bidder
    private final AtomicInteger highestBidderId; // -1 if no Bidder
    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;
    /**
     * Constructs a new {@code Bid} with the specified user ID, store ID, and items.
     *
     * @param userId the ID of the user initiating the bid.
     * @param storeId the ID of the store where the bid is made.
     * @param items a map of item IDs to their quantities.
     */
    public Bid(int purchaseId, int userId, int storeId, Map<Integer, Integer> items, int initialPrice) {
        super(purchaseId, userId, storeId, items, initialPrice, null);
        this.initialPrice = new AtomicInteger(initialPrice);
        highestBid = new AtomicInteger(initialPrice);
        highestBidderId = new AtomicInteger(-1);
        this.auctionStartTime = null;
        this.auctionEndTime = null;
    }

    /**
     * Constructs a new {@code Bid} with the specified user ID, store ID, items, auction start time, and auction end time.
     *
     * @param purchaseId the ID of the purchase.
     * @param userId the ID of the user initiating the bid.
     * @param storeId the ID of the store where the bid is made.
     * @param items a map of item IDs to their quantities.
     * @param initialPrice the initial price of the bid.
     * @param auctionStartTime the start time of the auction.
     * @param auctionEndTime the end time of the auction.
     */
    public Bid(int purchaseId, int userId, int storeId, Map<Integer, Integer> items, int initialPrice, LocalDateTime auctionStartTime, LocalDateTime auctionEndTime) {
        this(purchaseId, userId, storeId, items, initialPrice);
        this.auctionStartTime = auctionStartTime;
        this.auctionEndTime = auctionEndTime;
    }

    /**
     * Adds a user's bid
     *
     * @param userId the ID of the bidder;
     * @param bidAmount the amount of the bid.
     */
    public synchronized void addBidding(int userId, int bidAmount) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auctionStartTime)) {
            throw new IllegalStateException("Auction has not started yet. Current time: " + now + ", Auction start time: " + auctionStartTime);
        }
        if (now.isAfter(auctionEndTime) || isCompleted) {
            throw new IllegalStateException("Auction has already ended or is completed. Current time: " + now + ", Auction end time: " + auctionEndTime);
        }
        if(bidAmount > highestBid.get()) {
            if(!isCompleted){
                highestBid.set(bidAmount);
                highestBidderId.set(userId);
                biddersIds.put(userId, Boolean.TRUE);   // ← ALWAYS non-null, thread-safe marker
            }
        }
    }



    /**
     * Returns the maximum bid amount among all bidders.
     * 
     * @return the maximum bid amount, or -1 if no bids have been placed.
     */
    public int getMaxBidding() {
        return highestBid.get();
    }

    @Override
    /**
     * Completes the purchase and returns the Reciept of the user with the highest bid.
     */
    public synchronized BidReciept completePurchase(){
        if(!this.isCompleted()){
            this.isCompleted = true;
            this.timeOfCompletion = LocalDateTime.now();
            return new BidReciept(this.purchaseId, this.userId, this.storeId, this.items, this.shippingAddress, this.highestBid.get(), this.highestBidderId.get(), this.initialPrice.get(), this.highestBid.get(), this.highestBidderId.get(), this.isCompleted, this.auctionEndTime);
        }
        else{
            throw new IllegalStateException("Purchase is already completed");
        }
    }

    /**
     * Returns a list of all bidders' IDs.
     * 
     * @return a list of all bidders' IDs.
     */
    public synchronized List<Integer> getBiddersIds() {
        return this.biddersIds.keySet().stream().toList();
    }

    @Override
    public BidReciept generateReciept() {
        return new BidReciept(this.purchaseId, this.userId, this.storeId, this.items, this.shippingAddress, this.initialPrice.get(), this.highestBidderId.get(), this.initialPrice.get(), this.highestBid.get(), this.highestBidderId.get(), this.isCompleted, this.auctionEndTime);
    }

    public int getInitialPrice() {
        return initialPrice.get();
    }
    public int getHighestBid() {
        return highestBid.get();
    }
    public int getHighestBidderId() {
        return highestBidderId.get();
    }

    public LocalDateTime getAuctionStartTime() {
        return auctionStartTime;
    }
    public LocalDateTime getAuctionEndTime() {
        return auctionEndTime;
    }
}