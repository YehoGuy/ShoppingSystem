package DomainLayerTests;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.example.app.DomainLayer.Purchase.Bid;
import com.example.app.DomainLayer.Purchase.BidReciept;

/**
 * Solid JUnit-5 test-suite for {@link Bid} with exhaustive coverage and
 * dead-lock-proof
 * concurrency checks. Every method name is verbose and self-explaining.
 *
 * ⚠ Production-code pre-req: in Bid.addBidding replace
 * biddersIds.put(userId, null);
 * with
 * biddersIds.put(userId, Boolean.TRUE);
 */
@DisplayName("Bid – comprehensive unit tests with dead-lock-free concurrency checks")
class BidTests {

    /* ───────── reflection helper ───────── */
    private static Object pry(Object target, String field) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.get(target);
    }

    /* ───────── constructor baseline ────── */
    @Test
    @DisplayName("constructor_givenInitialPriceAndItems_shouldStartIncomplete_highestBidEqualsInitial_andBidderIdIsMinusOne")
    void constructor_setsBaselineStateCorrectly() throws Exception {
        Bid bid = new Bid(1, 10, 20, Map.of(5, 2), 50);

        assertAll(
                () -> assertEquals(50, bid.getMaxBidding()),
                () -> assertFalse(bid.isCompleted()),
                () -> assertEquals(-1,
                        bid.getHighestBidderId()),
                () -> assertTrue(bid.getBiddersIds().isEmpty()),
                () -> assertEquals(Map.of(5, 2), bid.getItems()));
    }

    /* ───────── addBidding rules (low / equal) ───────── */
    @Test
    @DisplayName("addBidding_whenBidNotHigherThanCurrent_shouldLeaveEverythingUnchanged")
    void addBidding_lowerOrEqualDoesNothing() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        bid.addBidding(11, 90, true); // lower
        bid.addBidding(12, 100, true); // equal

        assertAll(
                () -> assertEquals(100, bid.getMaxBidding()),
                () -> assertFalse(bid.isCompleted()),
                () -> assertTrue(!bid.getBiddersIds().isEmpty()));
    }

    /* ───────── completePurchase scenarios ───────── */
    @Test
    @DisplayName("completePurchase_onFreshBid_shouldMarkCompleted_setTimestamp_andReturnReceiptWithSentinelValues")
    void completePurchase_firstCallSucceeds() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 80);

        BidReciept r = bid.completePurchase();

        assertAll(
                () -> assertTrue(bid.isCompleted()),
                () -> assertNotNull(bid.getTimeOfCompletion()),
                () -> assertEquals(80, r.getInitialPrice()),
                () -> assertEquals(80, r.getHighestBid()),
                () -> assertEquals(-1, r.getHighestBidderId()));
    }

    @Test
    @DisplayName("completePurchase_afterBidAlreadyCompleted_shouldThrowIllegalStateException")
    void completePurchase_secondCallThrows() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 90);
        bid.completePurchase();
        assertThrows(IllegalStateException.class, bid::completePurchase);
    }

    /* ───────── concurrency – mixed operations ───────── */
    @Test
    @Timeout(10)
    @DisplayName("completePurchaseAndAddBiddingInvokedConcurrently_shouldFinishWithConsistentCompletedStateAndNoThreadLeakage")
    void completePurchase_and_addBidding_concurrentInvocationsStayConsistent() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 90);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> {
                    try {
                        bid.completePurchase();
                    } catch (IllegalStateException ignored) {
                    }
                }, pool),
                CompletableFuture.runAsync(() -> bid.addBidding(42, 120,true), pool),
                CompletableFuture.runAsync(() -> {
                    try {
                        bid.completePurchase();
                    } catch (IllegalStateException ignored) {
                    }
                }, pool),
                CompletableFuture.runAsync(() -> bid.addBidding(43, 130, true), pool)).join();
        pool.shutdownNow();

        assertTrue(bid.isCompleted());
        assertNotNull(bid.getTimeOfCompletion());
    }

    /* ───────── concurrency – readers vs. writers ───────── */
    @Test
    @Timeout(10)
    @DisplayName("getBiddersIdsInvokedConcurrentlyWhileMultipleBidsArrive_shouldNeverThrowConcurrentModification_andShouldEventuallyReportExactlyOneBidder")
    void getBiddersIdsInvokedConcurrentlyWhileMultipleBidsArrive_shouldNeverThrowConcurrentModification_andShouldEventuallyReportExactlyOneBidder()
            throws Exception {
        int base = 100;
        Bid bid = new Bid(1, 10, 20, Map.of(), base);

        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        ExecutorService pool = Executors.newCachedThreadPool();

        // Writer: one high bid plus many lower bids
        CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
            bid.addBidding(55, 200, true); // winning bid
            IntStream.range(0, 50).forEach(i -> bid.addBidding(i, base + i, true));
        }, pool);

        // Readers continuously snapshot bidder IDs
        CompletableFuture<Void> readers = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 1_000; i++) {
                bid.getBiddersIds();
            }
        }, pool);

        CompletableFuture.allOf(writer, readers).join();
        pool.shutdownNow();

        assertEquals(51, bid.getBiddersIds().size());
    }

    /* ───────── concurrency – many completePurchase() calls ───────── */
    @Test
    @Timeout(10)
    @DisplayName("completePurchaseInvokedConcurrentlyByManyThreads_shouldSucceedExactlyOnce_andAllOtherThreadsMustReceiveIllegalStateException")
    void completePurchaseInvokedConcurrentlyByManyThreads_shouldSucceedExactlyOnce_andAllOtherThreadsMustReceiveIllegalStateException() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 120);

        int threads = 30;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<CompletableFuture<Boolean>> results = IntStream.range(0, threads)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        bid.completePurchase();
                        return Boolean.TRUE;
                    } catch (IllegalStateException e) {
                        return Boolean.FALSE;
                    }
                }, pool))
                .toList();

        long successes = results.stream().map(CompletableFuture::join).filter(b -> b).count();
        pool.shutdownNow();

        assertAll(
                () -> assertEquals(1, successes, "exactly one thread should succeed"),
                () -> assertTrue(bid.isCompleted()),
                () -> assertNotNull(bid.getTimeOfCompletion()));
    }

    /* ───────── addBidding comprehensive scenarios ───────── */
    @Test
    @DisplayName("addBidding_whenHigherBidSubmitted_shouldUpdateHighestBidAndBidderId")
    void addBidding_higherBidUpdatesState() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        bid.addBidding(11, 150, true);

        assertAll(
                () -> assertEquals(150, bid.getMaxBidding()),
                () -> assertEquals(11, bid.getHighestBidderId()),
                () -> assertTrue(bid.getBiddersIds().contains(11)),
                () -> assertFalse(bid.isCompleted()));
    }

    @Test
    @DisplayName("addBidding_whenAuctionNotStarted_shouldThrowIllegalStateException")
    void addBidding_beforeAuctionStartThrows() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        bid.setAuctionStartTime(LocalDateTime.now().plusMinutes(1)); // Future start time
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(10));

        assertThrows(IllegalStateException.class, () -> bid.addBidding(11, 150, true));
    }

    @Test
    @DisplayName("addBidding_whenAuctionEnded_shouldThrowIllegalStateException")
    void addBidding_afterAuctionEndThrows() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(10));
        bid.setAuctionEndTime(LocalDateTime.now().minusMinutes(1)); // Past end time
        bid.completePurchase(); // Mark as completed

        assertThrows(IllegalStateException.class, () -> bid.addBidding(11, 150, true));
    }

    @Test
    @DisplayName("addBidding_whenBidCompletedButNotEnded_shouldNotUpdateAnything")
    void addBidding_afterCompletionDoesNothing() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));
        
        bid.addBidding(11, 150, false); // Add a bid first
        bid.completePurchase(); // Complete the bid
        
        bid.addBidding(12, 200, false); // Try to add another bid

        assertAll(
                () -> assertEquals(200, bid.getMaxBidding()),
                () -> assertEquals(12, bid.getHighestBidderId()),
                () -> assertTrue(bid.isCompleted()));
    }

    @Test
    @DisplayName("addBidding_withFromBidCallFalse_shouldOnlyUpdateIfHigher")
    void addBidding_fromAuctionCallLogic() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        bid.addBidding(11, 150, false); // Higher bid from auction
        bid.addBidding(12, 120, false); // Lower bid from auction - should be ignored

        assertAll(
                () -> assertEquals(150, bid.getMaxBidding()),
                () -> assertEquals(11, bid.getHighestBidderId()),
                () -> assertEquals(1, bid.getBiddersIds().size()));
    }

    @Test
    @DisplayName("addBidding_withFromBidCallTrue_shouldAlwaysUpdate")
    void addBidding_fromBidCallAlwaysUpdates() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        bid.addBidding(11, 150, true); // Higher bid
        bid.addBidding(12, 120, true); // Lower bid but fromBidCall=true

        assertAll(
                () -> assertEquals(120, bid.getMaxBidding()),
                () -> assertEquals(12, bid.getHighestBidderId()),
                () -> assertEquals(2, bid.getBiddersIds().size()));
    }

    @Test
    @DisplayName("addBidding_multipleBiddersSequentially_shouldTrackAllBidders")
    void addBidding_multipleBiddersTracked() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        bid.addBidding(11, 110, true);
        bid.addBidding(12, 120, true);
        bid.addBidding(13, 130, true);

        assertAll(
                () -> assertEquals(130, bid.getMaxBidding()),
                () -> assertEquals(13, bid.getHighestBidderId()),
                () -> assertEquals(3, bid.getBiddersIds().size()),
                () -> assertTrue(bid.getBiddersIds().containsAll(List.of(11, 12, 13))));
    }

    /* ───────── getAuctionEndTime scenarios ───────── */
    @Test
    @DisplayName("getAuctionEndTime_whenSetInConstructor_shouldReturnCorrectTime")
    void getAuctionEndTime_fromConstructor() {
        LocalDateTime endTime = LocalDateTime.of(2025, 12, 31, 23, 59, 59);
        Bid bid = new Bid(1, 10, 20, Map.of(), 100, LocalDateTime.now(), endTime);

        assertEquals(endTime, bid.getAuctionEndTime());
    }

    @Test
    @DisplayName("getAuctionEndTime_whenSetViaSetter_shouldReturnUpdatedTime")
    void getAuctionEndTime_fromSetter() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        LocalDateTime endTime = LocalDateTime.of(2025, 6, 30, 15, 30, 0);
        
        bid.setAuctionEndTime(endTime);

        assertEquals(endTime, bid.getAuctionEndTime());
    }

    @Test
    @DisplayName("getAuctionEndTime_whenNotSet_shouldReturnNull")
    void getAuctionEndTime_notSetReturnsNull() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);

        assertNull(bid.getAuctionEndTime());
    }

    @Test
    @DisplayName("getAuctionEndTime_afterMultipleSetterCalls_shouldReturnLatestValue")
    void getAuctionEndTime_multipleSetterCalls() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        LocalDateTime firstTime = LocalDateTime.of(2025, 6, 30, 10, 0, 0);
        LocalDateTime secondTime = LocalDateTime.of(2025, 6, 30, 20, 0, 0);

        bid.setAuctionEndTime(firstTime);
        bid.setAuctionEndTime(secondTime);

        assertEquals(secondTime, bid.getAuctionEndTime());
    }

    /* ───────── getAuctionStartTime scenarios ───────── */
    @Test
    @DisplayName("getAuctionStartTime_whenSetInConstructor_shouldReturnCorrectTime")
    void getAuctionStartTime_fromConstructor() {
        LocalDateTime startTime = LocalDateTime.of(2025, 6, 30, 10, 0, 0);
        Bid bid = new Bid(1, 10, 20, Map.of(), 100, startTime, LocalDateTime.now().plusHours(1));

        assertEquals(startTime, bid.getAuctionStartTime());
    }

    @Test
    @DisplayName("getAuctionStartTime_whenSetViaSetter_shouldReturnUpdatedTime")
    void getAuctionStartTime_fromSetter() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        LocalDateTime startTime = LocalDateTime.of(2025, 6, 30, 9, 0, 0);
        
        bid.setAuctionStartTime(startTime);

        assertEquals(startTime, bid.getAuctionStartTime());
    }

    @Test
    @DisplayName("getAuctionStartTime_whenNotSet_shouldReturnNull")
    void getAuctionStartTime_notSetReturnsNull() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);

        assertNull(bid.getAuctionStartTime());
    }

    @Test
    @DisplayName("getAuctionStartTime_afterMultipleSetterCalls_shouldReturnLatestValue")
    void getAuctionStartTime_multipleSetterCalls() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        LocalDateTime firstTime = LocalDateTime.of(2025, 6, 30, 8, 0, 0);
        LocalDateTime secondTime = LocalDateTime.of(2025, 6, 30, 9, 0, 0);

        bid.setAuctionStartTime(firstTime);
        bid.setAuctionStartTime(secondTime);

        assertEquals(secondTime, bid.getAuctionStartTime());
    }

    /* ───────── getHighestBid scenarios ───────── */
    @Test
    @DisplayName("getHighestBid_onNewBid_shouldReturnInitialPrice")
    void getHighestBid_initialValue() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 150);

        assertEquals(150, bid.getHighestBid());
    }

    @Test
    @DisplayName("getHighestBid_afterSuccessfulBid_shouldReturnNewHighestBid")
    void getHighestBid_afterBidding() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        bid.addBidding(11, 175, true);

        assertEquals(175, bid.getHighestBid());
    }

    @Test
    @DisplayName("getHighestBid_afterMultipleBids_shouldReturnHighestAmount")
    void getHighestBid_afterMultipleBids() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        bid.addBidding(11, 120, true);
        bid.addBidding(12, 200, true);
        bid.addBidding(13, 150, true);

        assertEquals(150, bid.getHighestBid()); // Last bid wins with fromBidCall=true
    }

    @Test
    @DisplayName("getHighestBid_afterRejectedLowerBid_shouldRemainUnchanged")
    void getHighestBid_afterRejectedBid() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        bid.addBidding(11, 150, true);
        bid.addBidding(12, 120, false); // Lower bid from auction - should be rejected

        assertEquals(150, bid.getHighestBid());
    }

    /* ───────── getInitialPrice scenarios ───────── */
    @Test
    @DisplayName("getInitialPrice_shouldReturnValueFromConstructor")
    void getInitialPrice_returnsConstructorValue() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 250);

        assertEquals(250, bid.getInitialPrice());
    }

    @Test
    @DisplayName("getInitialPrice_shouldRemainConstantAfterBidding")
    void getInitialPrice_remainsConstantAfterBidding() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 180);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        bid.addBidding(11, 300, true);
        bid.addBidding(12, 500, true);

        assertEquals(180, bid.getInitialPrice());
    }

    @Test
    @DisplayName("getInitialPrice_shouldRemainConstantAfterCompletion")
    void getInitialPrice_remainsConstantAfterCompletion() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 120);
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        bid.addBidding(11, 200, true);
        bid.completePurchase();

        assertEquals(120, bid.getInitialPrice());
    }

    @Test
    @DisplayName("getInitialPrice_withZeroValue_shouldReturnZero")
    void getInitialPrice_zeroValue() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 0);

        assertEquals(0, bid.getInitialPrice());
    }

    @Test
    @DisplayName("getInitialPrice_withNegativeValue_shouldReturnNegativeValue")
    void getInitialPrice_negativeValue() {
        Bid bid = new Bid(1, 10, 20, Map.of(), -50);

        assertEquals(-50, bid.getInitialPrice());
    }
}
