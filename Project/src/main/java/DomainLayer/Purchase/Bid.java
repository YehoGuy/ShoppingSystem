package DomainLayer.Purchase;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Bid extends Purchase{

    private final Map<Integer, Double> biddings; // user (bidder) ID -> bid amount;

    /**
     * Constructs a new {@code Bid} with the specified user ID, store ID, and items.
     *
     * @param userId the ID of the user initiating the bid.
     * @param storeId the ID of the store where the bid is made.
     * @param items a map of item IDs to their quantities.
     */
    public Bid(int purchaseId, int userId, int storeId, Map<Integer, Integer> items) {
        super(purchaseId, userId, storeId, items, null);
        this.biddings = new HashMap<>();
    }

    /**
     * Adds a user's bid
     *
     * @param userId the ID of the bidder;
     * @param bidAmount the amount of the bid.
     */
    public void addBidding(int userId, double bidAmount) {
        if (bidAmount <= 0) {
            throw new IllegalArgumentException("Bid amount must be positive");
        }   
        biddings.put(userId, bidAmount);
    }

    /**
     * Returns the bid amount for a specific user.
     * 
     * @param userId the ID of the user whose bid amount is to be retrieved.
     * @return the bid amount for the specified user, or -1 if the user has not placed a bid.
     */
    public double getBidding(int userId) {
        return biddings.getOrDefault(userId,-1.0);
    }

    /**
     * Returns the maximum bid amount among all bidders.
     * 
     * @return the maximum bid amount, or -1 if no bids have been placed.
     */
    public double getMaxBidding() {
        double maxBid = -1;
        for (double bid : biddings.values()) {
            if (bid > maxBid) {
                maxBid = bid;
            }
        }
        return maxBid;
    }

    @Override
    /**
     * Completes the purchase and returns the ID of the user with the highest bid.
     */
    public int completePurchase(){
        this.isCompleted = true;
        this.timeOfCompletion = LocalDateTime.now();
        double maxBidding = 0;
        int maxBidder = -1;
        for(Map.Entry<Integer, Double> e : biddings.entrySet()){
            if(e.getValue() > maxBidding){
                maxBidding = e.getValue();
                maxBidder = e.getKey();
            }
        }
        return maxBidder;
    }




}
