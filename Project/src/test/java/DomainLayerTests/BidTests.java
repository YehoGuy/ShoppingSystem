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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.example.app.DomainLayer.Purchase.Bid;
import com.example.app.DomainLayer.Purchase.BidReciept;

/**
 * Solid JUnit-5 test-suite for {@link Bid} with exhaustive coverage and dead-lock-proof
 * concurrency checks.  Every method name is verbose and self-explaining.
 *
 * ⚠  Production-code pre-req: in Bid.addBidding replace  
 *       biddersIds.put(userId, null);  
 *   with  
 *       biddersIds.put(userId, Boolean.TRUE);
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
    @DisplayName(
        "constructor_givenInitialPriceAndItems_shouldStartIncomplete_highestBidEqualsInitial_andBidderIdIsMinusOne"
    )
    void constructor_setsBaselineStateCorrectly() throws Exception {
        Bid bid = new Bid(1, 10, 20, Map.of(5, 2), 50);

        assertAll(
            () -> assertEquals(50, bid.getMaxBidding()),
            () -> assertFalse(bid.isCompleted()),
            () -> assertEquals(-1,
                    ((AtomicInteger) pry(bid, "highestBidderId")).get()),
            () -> assertTrue(bid.getBiddersIds().isEmpty()),
            () -> assertEquals(Map.of(5, 2), bid.getItems()));
    }

    /* ───────── addBidding rules (low / equal) ───────── */
    @Test
    @DisplayName(
        "addBidding_whenBidNotHigherThanCurrent_shouldLeaveEverythingUnchanged"
    )
    void addBidding_lowerOrEqualDoesNothing() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);

        bid.addBidding(11, 90);   // lower
        bid.addBidding(12, 100);  // equal

        assertAll(
            () -> assertEquals(100, bid.getMaxBidding()),
            () -> assertFalse(bid.isCompleted()),
            () -> assertTrue(bid.getBiddersIds().isEmpty()));
    }

    /* ───────── addBidding rules (higher) ───────── */
    @Test
    @DisplayName(
        "addBidding_whenBidIsHigher_shouldUpdateHighestBid_setWinner_andMarkCompletedExactlyOnce"
    )
    void addBidding_higherBidWinsAndCompletes() throws Exception {
        Bid bid = new Bid(1, 10, 20, Map.of(), 100);

        LocalDateTime before = LocalDateTime.now();
        bid.addBidding(77, 150);
        LocalDateTime after = LocalDateTime.now();

        assertAll(
            () -> assertTrue(bid.isCompleted()),
            () -> assertEquals(150, bid.getMaxBidding()),
            () -> assertEquals(77,
                    ((AtomicInteger) pry(bid, "highestBidderId")).get()),
            () -> assertEquals(1, bid.getBiddersIds().size()),
            () -> assertTrue(!bid.getTimeOfCompletion().isBefore(before)
                          && !bid.getTimeOfCompletion().isAfter(after.plusSeconds(1))));
    }

    /* ───────── completePurchase scenarios ───────── */
    @Test
    @DisplayName(
        "completePurchase_onFreshBid_shouldMarkCompleted_setTimestamp_andReturnReceiptWithSentinelValues"
    )
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
    @DisplayName(
        "completePurchase_afterBidAlreadyCompleted_shouldThrowIllegalStateException"
    )
    void completePurchase_secondCallThrows() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 90);
        bid.completePurchase();
        assertThrows(IllegalStateException.class, bid::completePurchase);
    }

    /* ───────── concurrency – race for highest bid ───────── */
    @Test
    @Timeout(10)   // seconds safeguard
    @DisplayName(
        "addBidding_200ConcurrentThreadsEachRaisingBid_shouldYieldExactlyOneWinner_andExactlyOneBidderRecorded"
    )
    void addBidding_concurrentRaceProducesSingleWinner() throws Exception {
        int basePrice = 100;
        int threads   = 200;
        Bid bid = new Bid(1, 10, 20, Map.of(), basePrice);

        ExecutorService pool = Executors.newCachedThreadPool();
        List<CompletableFuture<Void>> futures =
            IntStream.range(0, threads)
                     .mapToObj(i -> CompletableFuture.runAsync(
                         () -> bid.addBidding(i, basePrice + 1 + i), pool))
                     .toList();

        futures.forEach(CompletableFuture::join);
        pool.shutdownNow();

        assertAll(
            () -> assertTrue(bid.isCompleted()),
            () -> assertEquals(1, bid.getBiddersIds().size()),
            () -> assertTrue(bid.getMaxBidding() > basePrice));
    }

    /* ───────── concurrency – mixed operations ───────── */
    @Test
    @Timeout(10)
    @DisplayName(
        "completePurchaseAndAddBiddingInvokedConcurrently_shouldFinishWithConsistentCompletedStateAndNoThreadLeakage"
    )
    void completePurchase_and_addBidding_concurrentInvocationsStayConsistent() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 90);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CompletableFuture.allOf(
            CompletableFuture.runAsync(() -> { try { bid.completePurchase(); } catch (IllegalStateException ignored) {} }, pool),
            CompletableFuture.runAsync(() -> bid.addBidding(42, 120), pool),
            CompletableFuture.runAsync(() -> { try { bid.completePurchase(); } catch (IllegalStateException ignored) {} }, pool),
            CompletableFuture.runAsync(() -> bid.addBidding(43, 130), pool)
        ).join();
        pool.shutdownNow();

        assertTrue(bid.isCompleted());
        assertNotNull(bid.getTimeOfCompletion());
    }

    /* ───────── concurrency – lower bids AFTER the winner ───────── */
    @Test
    @Timeout(10)
    @DisplayName(
        "lowerConcurrentBidsSubmittedAfterAWinningBid_mustNotAlterHighestBidOrWinner_andMustNotRecordAdditionalBidders"
    )
    void lowerConcurrentBidsSubmittedAfterAWinningBid_mustNotAlterHighestBidOrWinner_andMustNotRecordAdditionalBidders() throws Exception {
        int base = 100;
        Bid bid  = new Bid(1, 10, 20, Map.of(), base);

        // Thread that wins first
        CompletableFuture.runAsync(() -> bid.addBidding(999, 150)).join();

        int threads = 100;
        ExecutorService pool = Executors.newCachedThreadPool();
        List<CompletableFuture<Void>> lowers =
            IntStream.range(0, threads)
                    .mapToObj(i -> CompletableFuture.runAsync(
                        () -> bid.addBidding(i, base + i), pool)) // all < 150
                    .toList();

        lowers.forEach(CompletableFuture::join);
        pool.shutdownNow();

        assertAll(
            () -> assertTrue(bid.isCompleted()),
            () -> assertEquals(150, bid.getMaxBidding()),
            () -> assertEquals(1, bid.getBiddersIds().size()),   // only winner recorded
            () -> assertTrue(bid.getBiddersIds().contains(999)));
    }

    /* ───────── concurrency – readers vs. writers ───────── */
    @Test
    @Timeout(10)
    @DisplayName(
        "getBiddersIdsInvokedConcurrentlyWhileMultipleBidsArrive_shouldNeverThrowConcurrentModification_andShouldEventuallyReportExactlyOneBidder"
    )
    void getBiddersIdsInvokedConcurrentlyWhileMultipleBidsArrive_shouldNeverThrowConcurrentModification_andShouldEventuallyReportExactlyOneBidder() throws Exception {
        int base = 100;
        Bid bid  = new Bid(1, 10, 20, Map.of(), base);

        ExecutorService pool = Executors.newCachedThreadPool();

        // Writer: one high bid plus many lower bids
        CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
            bid.addBidding(55, 200);           // winning bid
            IntStream.range(0, 50).forEach(i -> bid.addBidding(i, base + i));
        }, pool);

        // Readers continuously snapshot bidder IDs
        CompletableFuture<Void> readers = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 1_000; i++) {
                bid.getBiddersIds();
            }
        }, pool);

        CompletableFuture.allOf(writer, readers).join();
        pool.shutdownNow();

        assertEquals(1, bid.getBiddersIds().size());
    }

    /* ───────── concurrency – many completePurchase() calls ───────── */
    @Test
    @Timeout(10)
    @DisplayName(
        "completePurchaseInvokedConcurrentlyByManyThreads_shouldSucceedExactlyOnce_andAllOtherThreadsMustReceiveIllegalStateException"
    )
    void completePurchaseInvokedConcurrentlyByManyThreads_shouldSucceedExactlyOnce_andAllOtherThreadsMustReceiveIllegalStateException() {
        Bid bid = new Bid(1, 10, 20, Map.of(), 120);

        int threads = 30;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<CompletableFuture<Boolean>> results =
            IntStream.range(0, threads)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        try { bid.completePurchase(); return Boolean.TRUE; }
                        catch (IllegalStateException e) { return Boolean.FALSE; }
                    }, pool))
                    .toList();

        long successes = results.stream().map(CompletableFuture::join).filter(b -> b).count();
        pool.shutdownNow();

        assertAll(
            () -> assertEquals(1, successes, "exactly one thread should succeed"),
            () -> assertTrue(bid.isCompleted()),
            () -> assertNotNull(bid.getTimeOfCompletion()));
    }
}
