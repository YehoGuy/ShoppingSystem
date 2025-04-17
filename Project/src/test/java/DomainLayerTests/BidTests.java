package DomainLayerTests;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import DomainLayer.Purchase.Bid;

class BidTests {

    private Bid bid;

    @BeforeEach
    void setUp() {
        // purchaseId=2, userId=100, storeId=200, sample items
        bid = new Bid(2, 100, 200, Map.of(501, 1, 502, 2));
    }

    @Test
    void testInheritance() {
        // Bid extends Purchase, so we verify basic Purchase fields
        assertEquals(2, bid.getPurchaseId());
        assertEquals(100, bid.getUserId());
        assertEquals(200, bid.getStoreId());
        assertEquals(2, bid.getItems().size());
        assertFalse(bid.isCompleted());
    }

    @Test
    void testAddBidding() {
        bid.addBidding(888, 99.99);
        assertEquals(99.99, bid.getBidding(888));
    }

    @Test
    void testAddBiddingNegativeShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> bid.addBidding(777, -10.0));
    }

    @Test
    void testGetBiddingDefault() {
        assertEquals(-1.0, bid.getBidding(999), "Should return -1.0 if user hasn’t placed a bid");
    }

    @Test
    void testGetMaxBidding() {
        bid.addBidding(888, 50.0);
        bid.addBidding(999, 75.0);
        bid.addBidding(1000, 20.0);
        assertEquals(75.0, bid.getMaxBidding());
    }

    @Test
    void testCompletePurchaseReturnsHighestBidder() {
        bid.addBidding(888, 50.0);
        bid.addBidding(999, 75.0);
        bid.addBidding(1000, 20.0);

        int winner = bid.completePurchase();
        assertTrue(bid.isCompleted());
        assertNotNull(bid.getTimeOfCompletion());
        assertEquals(999, winner, "Highest bidder is user 999 with 75.0");
    }
}
