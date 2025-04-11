package DomainLayer.Purchase;

import java.util.HashMap;
import java.util.Map;

public class Bid extends Purchase{

    private final Map<Integer, double> bids; // user (bidder) ID -> bid amount


    /**
     * Constructs a new {@code Bid} with the specified user ID, store ID, and items.
     *
     * @param userId the ID of the user initiating the bid.
     * @param storeId the ID of the store where the bid is made.
     * @param items a map of item IDs to their quantities.
     */
    public Bid(int userId, int storeId, Map<Integer, Integer> items) {
        super(userId, storeId, items, null);
        this.bids = new HashMap<>();
    }

    /**
     * Adds a user's bid
     *
     * @param userId the ID of the bidder;
     * @param bidAmount the amount of the bid.
     */
    public void addBid(int userId, double bidAmount) {
        if (bidAmount <= 0) {
            throw new IllegalArgumentException("Bid amount must be positive");
        }   
        bids.put(userId, bidAmount);
    }

    /**
     * Returns the bid amount for a specific user.
     * 
     * @param userId the ID of the user whose bid amount is to be retrieved.
     * @return the bid amount for the specified user, or -1 if the user has not placed a bid.
     */
    public double getBid(int userId) {
        return bids.getOrDefault(userId,-1);
    }


}
