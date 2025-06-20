package com.example.app.DomainLayer.Purchase;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;

@Entity
@DiscriminatorValue("bid")
public class Bid extends Purchase {

    private final Integer initialPrice;

    @ElementCollection
    @CollectionTable(name = "bid_ids", joinColumns = @JoinColumn(name = "purchase_id"))
    @MapKeyColumn(name = "member_id")
    @Column(name = "quantity")
    private Map<Integer, Boolean> persistedBiddersIds = new HashMap<>();

    @Transient
    private final ConcurrentHashMap<Integer, Boolean> biddersIds = new ConcurrentHashMap<>();
    private Integer highestBid; // initialPrice if no Bidder
    private Integer highestBidderId; // -1 if no Bidder
    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;

    /**
     * Constructs a new {@code Bid} with the specified user ID, store ID, and items.
     *
     * @param userId  the ID of the user initiating the bid.
     * @param storeId the ID of the store where the bid is made.
     * @param items   a map of item IDs to their quantities.
     */
    public Bid(int purchaseId, int userId, int storeId, Map<Integer, Integer> items, int initialPrice) {
        super(purchaseId, userId, storeId, items, initialPrice, null);
        this.initialPrice = initialPrice;
        highestBid = initialPrice;
        highestBidderId = -1;
        this.auctionStartTime = null;
        this.auctionEndTime = null;
    }

    /**
     * Constructs a new {@code Bid} with the specified user ID, store ID, items,
     * auction start time, and auction end time.
     *
     * @param purchaseId       the ID of the purchase.
     * @param userId           the ID of the user initiating the bid.
     * @param storeId          the ID of the store where the bid is made.
     * @param items            a map of item IDs to their quantities.
     * @param initialPrice     the initial price of the bid.
     * @param auctionStartTime the start time of the auction.
     * @param auctionEndTime   the end time of the auction.
     */
    public Bid(int purchaseId, int userId, int storeId, Map<Integer, Integer> items, int initialPrice,
            LocalDateTime auctionStartTime, LocalDateTime auctionEndTime) {
        this(purchaseId, userId, storeId, items, initialPrice);
        this.auctionStartTime = auctionStartTime;
        this.auctionEndTime = auctionEndTime;
    }

    public Bid() {
        this(-1, -1, -1, new HashMap<>(), -1, LocalDateTime.MIN, LocalDateTime.MAX);
    }

    /**
     * Adds a user's bid
     *
     * @param userId    the ID of the bidder;
     * @param bidAmount the amount of the bid.
     */
    public synchronized void addBidding(int userId, int bidPrice) {
        LocalDateTime now = LocalDateTime.now();
        if(auctionStartTime != null) {
            if (now.isBefore(auctionStartTime)) {
                throw new IllegalStateException(
                        "Auction has not started yet. Current time: " + now + ", Auction start time: " + auctionStartTime);
            }
            if (now.isAfter(auctionEndTime) && isCompleted) {
                throw new IllegalStateException("Auction has already ended or is completed. Current time: " + now
                        + ", Auction end time: " + auctionEndTime);
            }
        }
        if (bidPrice > highestBid) {
            if (!isCompleted) {
                highestBid = bidPrice;
                highestBidderId = userId;
                biddersIds.put(userId, Boolean.TRUE); // ← ALWAYS non-null, thread-safe marker
            }
        }
        else{
            // throw new IllegalStateException("Bid price is lower than the current highest bid");
            return;
        }
    }

    /**
     * Returns the maximum bid amount among all bidders.
     * 
     * @return the maximum bid amount, or -1 if no bids have been placed.
     */
    public int getMaxBidding() {
        return highestBid;
    }

    @Override
    /**
     * Completes the purchase and returns the Reciept of the user with the highest
     * bid.
     */
    public synchronized BidReciept completePurchase() {
        if (!this.isCompleted()) {
            this.isCompleted = true;
            this.timeOfCompletion = LocalDateTime.now();
            return new BidReciept(this.purchaseId, this.userId, this.storeId, this.items, this.shippingAddress,
                    this.highestBid, this.highestBidderId, this.initialPrice, this.highestBid,
                    this.highestBidderId, this.isCompleted, this.auctionEndTime);
        } else {
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
        return new BidReciept(this.purchaseId, this.userId, this.storeId, this.items, this.shippingAddress,
                this.initialPrice, this.highestBidderId, this.initialPrice, this.highestBid,
                this.highestBidderId, this.isCompleted, this.auctionEndTime);
    }

    public int getInitialPrice() {
        return initialPrice;
    }

    public int getHighestBid() {
        return highestBid;
    }

    public int getHighestBidderId() {
        return highestBidderId;
    }

    public LocalDateTime getAuctionStartTime() {
        return auctionStartTime;
    }

    public LocalDateTime getAuctionEndTime() {
        return auctionEndTime;
    }

    public void setAuctionStartTime(LocalDateTime minusMinutes) {
        this.auctionStartTime = minusMinutes;
    }

    public void setAuctionEndTime(LocalDateTime plusMinutes) {
        this.auctionEndTime = plusMinutes;
    }

    @PostLoad
    private void postLoad() {
        // Sync persistedItems to items
        for (Map.Entry<Integer, Boolean> entry : persistedBiddersIds.entrySet()) {
            biddersIds.put(entry.getKey(), entry.getValue());
        }
    }

    @PrePersist
    @PreUpdate
    private void prePersist() {
        // Sync items to persistedItems
        persistedBiddersIds.clear();
        for (Map.Entry<Integer, Boolean> entry : biddersIds.entrySet()) {
            persistedBiddersIds.put(entry.getKey(), entry.getValue());
        }
    }
}