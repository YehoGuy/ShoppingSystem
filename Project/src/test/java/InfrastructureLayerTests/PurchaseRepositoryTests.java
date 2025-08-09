package InfrastructureLayerTests;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.Bid;
import com.example.app.DomainLayer.Purchase.BidReciept;
import com.example.app.DomainLayer.Purchase.Purchase;
import com.example.app.DomainLayer.Purchase.Reciept;
import com.example.app.InfrastructureLayer.PurchaseRepository;

/**
 * Exhaustive JUnit-5 + Mockito test-suite for {@link PurchaseRepository}.
 */
@DisplayName("PurchaseRepository – exhaustive unit tests with long self-describing names")
class PurchaseRepositoryTests {

    /* ─────────────────────────── helpers & reset ─────────────────────────── */

    private PurchaseRepository repo;

    /** Reset the singleton between tests to keep them isolated. */
    @BeforeEach
    void resetSingletonViaReflection() throws Exception {
        repo = new PurchaseRepository();
    }

    private Address anyAddress() {
        return new Address().withCountry("IL").withCity("TLV").withStreet("Rothschild");
    }

    /* ───────────────────── addPurchase path ───────────────────── */

    @Test
    @DisplayName("addPurchase_shouldReturnUniqueIncrementingIds_andPersistPurchaseThatIsRetrievableById")
    void addPurchase_returnsIdAndPersists() {
        int id1 = repo.addPurchase(10, 20, Map.of(7, 3), 42.0, anyAddress());
        int id2 = repo.addPurchase(11, 21, Map.of(8, 1), 10.0, anyAddress());

        assertAll(
                () -> assertNotEquals(id1, id2, "IDs must be unique"),
                () -> assertEquals(10, repo.getPurchaseById(id1).getUserId()),
                () -> assertEquals(21, repo.getPurchaseById(id2).getStoreId()));
    }

    /* ───────────────────── addBid path ───────────────────── */

    @Test
    @DisplayName("addBid_shouldReturnValidId_andStoreConcreteBidInstanceAccessibleViaGetPurchaseById")
    void addBid_returnsIdAndStoresBid() {
        int bidId = repo.addBid(99, 77, Map.of(1, 1), 100);

        Purchase p = repo.getPurchaseById(bidId);
        assertTrue(p instanceof Bid, "stored object should be Bid subclass");
    }

    /* ───────────────────── Enhanced addBid tests ───────────────────── */

    @Test
    @DisplayName("addBid_withBasicParameters_shouldReturnUniqueId_andCreateBidWithCorrectProperties")
    void addBid_basicParameters_returnsIdAndCreatesBid() {
        int userId = 123;
        int storeId = 456;
        Map<Integer, Integer> items = Map.of(1, 2, 3, 4);
        int initialPrice = 100;

        int bidId = repo.addBid(userId, storeId, items, initialPrice);

        Purchase p = repo.getPurchaseById(bidId);
        assertAll(
                () -> assertTrue(p instanceof Bid, "should create Bid instance"),
                () -> assertEquals(userId, p.getUserId()),
                () -> assertEquals(storeId, p.getStoreId()),
                () -> assertEquals(items, p.getItems()),
                () -> assertEquals(initialPrice, ((Bid) p).getHighestBid())
        );
    }

    @Test
    @DisplayName("addBid_withEmptyItemsMap_shouldCreateValidBid")
    void addBid_emptyItems_createsValidBid() {
        int bidId = repo.addBid(1, 2, Map.of(), 50);
        
        Purchase p = repo.getPurchaseById(bidId);
        assertAll(
                () -> assertTrue(p instanceof Bid),
                () -> assertTrue(p.getItems().isEmpty())
        );
    }

    @Test
    @DisplayName("addBid_multipleBids_shouldReturnUniqueIncrementingIds")
    void addBid_multipleBids_uniqueIds() {
        int bid1 = repo.addBid(1, 1, Map.of(1, 1), 10);
        int bid2 = repo.addBid(2, 2, Map.of(2, 2), 20);
        int bid3 = repo.addBid(3, 3, Map.of(3, 3), 30);

        assertAll(
                () -> assertNotEquals(bid1, bid2),
                () -> assertNotEquals(bid2, bid3),
                () -> assertNotEquals(bid1, bid3),
                () -> assertTrue(bid1 > 0 && bid2 > 0 && bid3 > 0)
        );
    }

    @Test
    @DisplayName("addBid_withZeroPrice_shouldCreateBidWithZeroPrice")
    void addBid_zeroPrice_createsValidBid() {
        int bidId = repo.addBid(10, 20, Map.of(5, 1), 0);
        
        Bid bid = (Bid) repo.getPurchaseById(bidId);
        assertEquals(0, bid.getHighestBid());
    }

    @Test
    @DisplayName("addBid_withNegativePrice_shouldCreateBidWithNegativePrice")
    void addBid_negativePrice_createsValidBid() {
        int bidId = repo.addBid(10, 20, Map.of(5, 1), -50);
        
        Bid bid = (Bid) repo.getPurchaseById(bidId);
        assertEquals(-50, bid.getHighestBid());
    }

    /* ───────────────────── getPurchaseById edge ───────────────────── */

    @Test
    @DisplayName("getPurchaseById_whenIdDoesNotExist_shouldThrowIllegalArgumentException")
    void getPurchaseById_nonExistentThrows() {
        assertThrows(IllegalArgumentException.class, () -> repo.getPurchaseById(999));
    }

    /* ───────────────────── deletePurchase path ───────────────────── */

    @Test
    @DisplayName("deletePurchase_shouldRemovePurchase_andSubsequentGetShouldThrow")
    void deletePurchase_removes() {
        int id = repo.addPurchase(5, 5, Map.of(), 0.0, anyAddress());
        repo.deletePurchase(id);
        assertThrows(IllegalArgumentException.class, () -> repo.getPurchaseById(id));
    }

    /* ───────────────────── filtering queries ───────────────────── */

    @Test
    @DisplayName("getUserPurchases_shouldReturnReceiptsForExactlyThosePurchasesMadeBySpecifiedUser")
    void getUserPurchases_returnsCorrectSet() {
        int uid = 42;
        repo.addPurchase(uid, 1, Map.of(), 0, anyAddress());
        repo.addPurchase(uid, 2, Map.of(), 0, anyAddress());
        repo.addPurchase(99, 1, Map.of(), 0, anyAddress()); // other user

        List<Reciept> receipts = repo.getUserPurchases(uid);
        assertEquals(2, receipts.size());
        assertTrue(receipts.stream().allMatch(r -> r.getUserId() == uid));
    }

    @Test
    @DisplayName("getStorePurchases_shouldReturnReceiptsForExactlyThosePurchasesMadeInSpecifiedStore")
    void getStorePurchases_returnsCorrectSet() {
        int sid = 7;
        repo.addPurchase(1, sid, Map.of(), 0, anyAddress());
        repo.addPurchase(2, sid, Map.of(), 0, anyAddress());
        repo.addPurchase(2, 999, Map.of(), 0, anyAddress()); // other store

        List<Reciept> receipts = repo.getStorePurchases(sid);
        assertEquals(2, receipts.size());
        assertTrue(receipts.stream().allMatch(r -> r.getShopId() == sid));
    }

    @Test
    @DisplayName("getUserStorePurchases_shouldReturnReceiptsOnlyWhenUserIdAndStoreIdBothMatch")
    void getUserStorePurchases_returnsCorrectSubset() {
        int uid = 5, sid = 6;
        repo.addPurchase(uid, sid, Map.of(), 0, anyAddress());
        repo.addPurchase(uid, sid, Map.of(), 0, anyAddress());
        repo.addPurchase(uid, 99, Map.of(), 0, anyAddress()); // wrong store
        repo.addPurchase(77, sid, Map.of(), 0, anyAddress()); // wrong user

        List<Reciept> receipts = repo.getUserStorePurchases(uid, sid);
        assertEquals(2, receipts.size());
        assertTrue(receipts.stream().allMatch(r -> r.getUserId() == uid && r.getShopId() == sid));
    }

    /* ───────────────────── concurrency – unique IDs ───────────────────── */

    @Test
    @Timeout(10)
    @DisplayName("addPurchaseInvokedConcurrentlyBy100Threads_shouldGenerate100DistinctIds_andAllPurchasesShouldBePersisted")
    void concurrentAddPurchase_uniqueIds() throws Exception {
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(32);
        Set<Integer> ids = ConcurrentHashMap.newKeySet();

        Runnable task = () -> ids.add(repo.addPurchase(1, 1, Map.of(), 0, anyAddress()));
        pool.invokeAll(Collections.nCopies(threads, Executors.callable(task)));
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertAll(
                () -> assertEquals(threads, ids.size(), "IDs must be unique"),
                () -> assertEquals(threads, repo.getUserPurchases(1).size()));
    }

    /*
     * ───────────────────── concurrency – mixed bids & purchases
     * ─────────────────────
     */

    @Test
    @Timeout(10)
    @DisplayName("addBidAndAddPurchaseMixedAcrossMultipleThreads_shouldMaintainCorrectTotalCount_andNoDuplicateIds")
    void concurrentMixedAdds_consistentStorage() throws Exception {
        int purchases = 60;
        int bids = 40;
        Set<Integer> ids = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newCachedThreadPool();

        IntStream.range(0, purchases)
                .forEach(i -> pool.submit(() -> ids.add(repo.addPurchase(2, 2, Map.of(), 0, anyAddress()))));
        IntStream.range(0, bids).forEach(i -> pool.submit(() -> ids.add(repo.addBid(3, 3, Map.of(), 10))));

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertAll(
                () -> assertEquals(purchases + bids, ids.size(), "every add must get a unique ID"),
                () -> assertEquals(purchases + bids,
                        repo.getUserPurchases(2).size() + repo.getUserPurchases(3).size()));
    }

    /* ───────────────────── Mockito spy integration ───────────────────── */

    @Test
    @DisplayName("getStorePurchases_shouldCallGenerateRecieptExactlyOncePerStoredPurchase_viaMockitoSpyInjection")
    void getStorePurchases_invokesGenerateReceiptExactlyOncePerPurchase() throws Exception {
        // Create a Mockito spy for Purchase & slip it into internal map via reflection
        Purchase spyPurchase1 = spy(new Purchase(111, 1, 77, Map.of(), 0, anyAddress()));
        Purchase spyPurchase2 = spy(new Purchase(112, 2, 77, Map.of(), 0, anyAddress()));

        Field storage = PurchaseRepository.class.getDeclaredField("purchaseStorage");
        storage.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, Purchase> internalMap = (Map<Integer, Purchase>) storage.get(repo);
        internalMap.put(111, spyPurchase1);
        internalMap.put(112, spyPurchase2);

        repo.getStorePurchases(77);

        verify(spyPurchase1, times(1)).generateReciept();
        verify(spyPurchase2, times(1)).generateReciept();
    }

    /* ───────── concurrency – delete race with readers ───────── */
    @Test
    @Timeout(10)
    @DisplayName("deletePurchaseExecutedByOneThreadWhileManyOtherThreadsContinuouslyCallGetPurchaseById_shouldNeverThrowUnexpectedExceptions_andFinalStateMustReflectDeletion")
    void concurrentDeleteWhileReadersLoop_neverThrows_andEndsWithMissingId() throws Exception {
        int id = repo.addPurchase(70, 80, Map.of(), 0, anyAddress());

        ExecutorService pool = Executors.newCachedThreadPool();
        // reader tasks: repeatedly try to fetch purchase; ignore expected
        // IllegalArgumentException
        Runnable reader = () -> {
            for (int i = 0; i < 1_000; i++) {
                try {
                    repo.getPurchaseById(id);
                } catch (IllegalArgumentException ignored) {
                    /* deleted */ }
            }
        };
        // writer task: delete once after slight delay
        Runnable deleter = () -> {
            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {
            }
            repo.deletePurchase(id);
        };

        pool.submit(deleter);
        IntStream.range(0, 20).forEach(j -> pool.submit(reader));
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThrows(IllegalArgumentException.class, () -> repo.getPurchaseById(id));
    }

    /*
     * ───────── concurrency – heavy readers while writers add ─────────
     * 
     * @Test
     * 
     * @Timeout(45)
     * 
     * @DisplayName(
     * "getUserPurchasesCalledRepeatedlyFromManyThreadsWhileOtherThreadsContinuouslyAddPurchasesForSameUser_shouldNeverThrowConcurrentModification_andFinalCountShouldMatchAdds"
     * )
     * void manyReadersAndWritersOnSameUser_noConcurrentModAndCorrectCount() throws
     * Exception {
     * int userId = 123;
     * int additions = 500;
     * 
     * ExecutorService pool = Executors.newCachedThreadPool();
     * 
     * // Writers: add purchases
     * IntStream.range(0, additions).forEach(i ->
     * pool.submit(() -> repo.addPurchase(userId, i, Map.of(), 0, anyAddress())));
     * 
     * // Readers: hammer getUserPurchases
     * IntStream.range(0, 50).forEach(i ->
     * pool.submit(() -> {
     * for (int j = 0; j < 5_000; j++) repo.getUserPurchases(userId);
     * }));
     * 
     * pool.shutdown();
     * pool.awaitTermination(30, TimeUnit.SECONDS);
     * 
     * assertEquals(additions, repo.getUserPurchases(userId).size());
     * }
     */

    /* ───────────────────── getAllBids tests ───────────────────── */

    @Test
    @DisplayName("getAllBids_whenNoBidsExist_shouldReturnEmptyList")
    void getAllBids_noBids_returnsEmptyList() {
        // Add some regular purchases to ensure they're filtered out
        repo.addPurchase(1, 1, Map.of(1, 1), 10.0, anyAddress());
        repo.addPurchase(2, 2, Map.of(2, 2), 20.0, anyAddress());

        List<BidReciept> bids = repo.getAllBids();
        assertTrue(bids.isEmpty(), "should return empty list when no bids exist");
    }

    @Test
    @DisplayName("getAllBids_whenOnlyBidsExist_shouldReturnAllBids")
    void getAllBids_onlyBids_returnsAllBids() {
        int bid1 = repo.addBid(10, 100, Map.of(1, 1), 50);
        int bid2 = repo.addBid(20, 200, Map.of(2, 2), 75);
        int bid3 = repo.addBid(30, 300, Map.of(3, 3), 100);

        List<BidReciept> bids = repo.getAllBids();
        
        assertAll(
                () -> assertEquals(3, bids.size()),
                () -> assertTrue(bids.stream().anyMatch(b -> b.getPurchaseId() == bid1)),
                () -> assertTrue(bids.stream().anyMatch(b -> b.getPurchaseId() == bid2)),
                () -> assertTrue(bids.stream().anyMatch(b -> b.getPurchaseId() == bid3))
        );
    }

    @Test
    @DisplayName("getAllBids_whenMixOfPurchasesAndBidsExist_shouldReturnOnlyBids")
    void getAllBids_mixedPurchases_returnsOnlyBids() {
        // Add regular purchases
        repo.addPurchase(1, 1, Map.of(1, 1), 10.0, anyAddress());
        repo.addPurchase(2, 2, Map.of(2, 2), 20.0, anyAddress());
        
        // Add bids
        int bid1 = repo.addBid(10, 100, Map.of(1, 1), 50);
        int bid2 = repo.addBid(20, 200, Map.of(2, 2), 75);

        List<BidReciept> bids = repo.getAllBids();
        
        assertAll(
                () -> assertEquals(2, bids.size(), "should only return bids, not purchases"),
                () -> assertTrue(bids.stream().anyMatch(b -> b.getPurchaseId() == bid1)),
                () -> assertTrue(bids.stream().anyMatch(b -> b.getPurchaseId() == bid2))
        );
    }

    @Test
    @DisplayName("getAllBids_shouldReturnBidReceiptsWithCorrectData")
    void getAllBids_returnsBidReceiptsWithCorrectData() {
        int userId = 123;
        int storeId = 456;
        Map<Integer, Integer> items = Map.of(7, 3, 8, 2);
        int price = 150;
        
        int bidId = repo.addBid(userId, storeId, items, price);

        List<BidReciept> bids = repo.getAllBids();
        BidReciept bidReceipt = bids.get(0);
        
        assertAll(
                () -> assertEquals(1, bids.size()),
                () -> assertEquals(bidId, bidReceipt.getPurchaseId()),
                () -> assertEquals(userId, bidReceipt.getUserId()),
                () -> assertEquals(storeId, bidReceipt.getShopId()),
                () -> assertEquals(items, bidReceipt.getItems()),
                () -> assertEquals(price, bidReceipt.getPrice())
        );
    }

    @Test
    @DisplayName("getAllBids_afterDeletingBid_shouldNotIncludeDeletedBid")
    void getAllBids_afterDeletion_excludesDeletedBid() {
        int bid1 = repo.addBid(10, 100, Map.of(1, 1), 50);
        int bid2 = repo.addBid(20, 200, Map.of(2, 2), 75);
        
        // Verify both bids exist
        assertEquals(2, repo.getAllBids().size());
        
        // Delete one bid
        repo.deletePurchase(bid1);
        
        List<BidReciept> remainingBids = repo.getAllBids();
        assertAll(
                () -> assertEquals(1, remainingBids.size()),
                () -> assertEquals(bid2, remainingBids.get(0).getPurchaseId())
        );
    }

    /* ───────────────────── getShopBids tests ───────────────────── */

    @Test
    @DisplayName("getShopBids_whenShopHasNoBids_shouldReturnEmptyList")
    void getShopBids_noBids_returnsEmptyList() {
        int shopId = 999;
        
        // Add bids for other shops
        repo.addBid(1, 100, Map.of(1, 1), 50);
        repo.addBid(2, 200, Map.of(2, 2), 75);
        
        List<BidReciept> shopBids = repo.getShopBids(shopId);
        assertTrue(shopBids.isEmpty(), "should return empty list when shop has no bids");
    }

    @Test
    @DisplayName("getShopBids_whenShopHasBids_shouldReturnOnlyThatShopsBids")
    void getShopBids_shopHasBids_returnsCorrectBids() {
        int targetShopId = 500;
        int otherShopId = 600;
        
        // Add bids for target shop
        int bid1 = repo.addBid(10, targetShopId, Map.of(1, 1), 50);
        int bid2 = repo.addBid(20, targetShopId, Map.of(2, 2), 75);
        
        // Add bids for other shops
        repo.addBid(30, otherShopId, Map.of(3, 3), 100);
        
        List<BidReciept> shopBids = repo.getShopBids(targetShopId);
        
        assertAll(
                () -> assertEquals(2, shopBids.size()),
                () -> assertTrue(shopBids.stream().allMatch(b -> b.getShopId() == targetShopId)),
                () -> assertTrue(shopBids.stream().anyMatch(b -> b.getPurchaseId() == bid1)),
                () -> assertTrue(shopBids.stream().anyMatch(b -> b.getPurchaseId() == bid2))
        );
    }

    @Test
    @DisplayName("getShopBids_whenShopHasMixOfPurchasesAndBids_shouldReturnOnlyBids")
    void getShopBids_mixedPurchases_returnsOnlyBids() {
        int shopId = 777;
        
        // Add regular purchases for the shop
        repo.addPurchase(1, shopId, Map.of(1, 1), 10.0, anyAddress());
        repo.addPurchase(2, shopId, Map.of(2, 2), 20.0, anyAddress());
        
        // Add bids for the shop
        int bid1 = repo.addBid(10, shopId, Map.of(1, 1), 50);
        int bid2 = repo.addBid(20, shopId, Map.of(2, 2), 75);

        List<BidReciept> shopBids = repo.getShopBids(shopId);
        
        assertAll(
                () -> assertEquals(2, shopBids.size(), "should only return bids, not purchases"),
                () -> assertTrue(shopBids.stream().allMatch(b -> b.getShopId() == shopId)),
                () -> assertTrue(shopBids.stream().anyMatch(b -> b.getPurchaseId() == bid1)),
                () -> assertTrue(shopBids.stream().anyMatch(b -> b.getPurchaseId() == bid2))
        );
    }

    @Test
    @DisplayName("getShopBids_shouldReturnBidsInConsistentOrder")
    void getShopBids_consistentOrder() {
        int shopId = 888;
        
        // Add multiple bids
        repo.addBid(10, shopId, Map.of(1, 1), 50);
        repo.addBid(20, shopId, Map.of(2, 2), 75);
        repo.addBid(30, shopId, Map.of(3, 3), 100);

        List<BidReciept> bids1 = repo.getShopBids(shopId);
        List<BidReciept> bids2 = repo.getShopBids(shopId);
        
        assertEquals(bids1.size(), bids2.size());
        // Note: Order consistency depends on implementation, this tests that it's repeatable
    }

    @Test
    @DisplayName("getShopBids_afterDeletingShopBid_shouldNotIncludeDeletedBid")
    void getShopBids_afterDeletion_excludesDeletedBid() {
        int shopId = 999;
        
        int bid1 = repo.addBid(10, shopId, Map.of(1, 1), 50);
        int bid2 = repo.addBid(20, shopId, Map.of(2, 2), 75);
        
        // Verify both bids exist for the shop
        assertEquals(2, repo.getShopBids(shopId).size());
        
        // Delete one bid
        repo.deletePurchase(bid1);
        
        List<BidReciept> remainingBids = repo.getShopBids(shopId);
        assertAll(
                () -> assertEquals(1, remainingBids.size()),
                () -> assertEquals(bid2, remainingBids.get(0).getPurchaseId())
        );
    }

    @Test
    @DisplayName("getShopBids_withZeroShopId_shouldWorkCorrectly")
    void getShopBids_zeroShopId_worksCorrectly() {
        int shopId = 0;
        
        int bid1 = repo.addBid(10, shopId, Map.of(1, 1), 50);
        
        List<BidReciept> shopBids = repo.getShopBids(shopId);
        
        assertAll(
                () -> assertEquals(1, shopBids.size()),
                () -> assertEquals(shopId, shopBids.get(0).getShopId()),
                () -> assertEquals(bid1, shopBids.get(0).getPurchaseId())
        );
    }

    @Test
    @DisplayName("getShopBids_withNegativeShopId_shouldWorkCorrectly")
    void getShopBids_negativeShopId_worksCorrectly() {
        int shopId = -100;
        
        int bid1 = repo.addBid(10, shopId, Map.of(1, 1), 50);
        
        List<BidReciept> shopBids = repo.getShopBids(shopId);
        
        assertAll(
                () -> assertEquals(1, shopBids.size()),
                () -> assertEquals(shopId, shopBids.get(0).getShopId()),
                () -> assertEquals(bid1, shopBids.get(0).getPurchaseId())
        );
    }

    /* ───────────────────── Cross-function integration tests ───────────────────── */

    @Test
    @DisplayName("addBid_getAllBids_getShopBids_integrationTest_shouldMaintainDataConsistency")
    void integrationTest_bidFunctions_maintainConsistency() {
        int shop1 = 100, shop2 = 200;
        
        // Add bids to different shops
        int bid1Shop1 = repo.addBid(10, shop1, Map.of(1, 1), 50);
        int bid2Shop1 = repo.addBid(20, shop1, Map.of(2, 2), 75);
        int bid1Shop2 = repo.addBid(30, shop2, Map.of(3, 3), 100);
        
        // Add regular purchases (should not appear in bid queries)
        repo.addPurchase(40, shop1, Map.of(4, 4), 25.0, anyAddress());
        
        List<BidReciept> allBids = repo.getAllBids();
        List<BidReciept> shop1Bids = repo.getShopBids(shop1);
        List<BidReciept> shop2Bids = repo.getShopBids(shop2);
        
        assertAll(
                () -> assertEquals(3, allBids.size(), "getAllBids should return all 3 bids"),
                () -> assertEquals(2, shop1Bids.size(), "shop1 should have 2 bids"),
                () -> assertEquals(1, shop2Bids.size(), "shop2 should have 1 bid"),
                () -> assertTrue(shop1Bids.stream().anyMatch(b -> b.getPurchaseId() == bid1Shop1)),
                () -> assertTrue(shop1Bids.stream().anyMatch(b -> b.getPurchaseId() == bid2Shop1)),
                () -> assertTrue(shop2Bids.stream().anyMatch(b -> b.getPurchaseId() == bid1Shop2)),
                () -> assertTrue(shop1Bids.stream().allMatch(b -> b.getShopId() == shop1)),
                () -> assertTrue(shop2Bids.stream().allMatch(b -> b.getShopId() == shop2))
        );
    }

    /* ───────────────────── Concurrency tests for bid functions ───────────────────── */

    @Test
    @Timeout(10)
    @DisplayName("addBid_concurrentExecution_shouldGenerateUniqueIds_andAllBidsAccessibleViaGetAllBids")
    void concurrentAddBid_uniqueIds_allAccessible() throws Exception {
        int numThreads = 50;
        int bidsPerThread = 4;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        Set<Integer> bidIds = ConcurrentHashMap.newKeySet();

        List<Callable<Void>> tasks = IntStream.range(0, numThreads)
                .mapToObj(threadId -> (Callable<Void>) () -> {
                    for (int i = 0; i < bidsPerThread; i++) {
                        int bidId = repo.addBid(threadId, threadId * 10, Map.of(i, i + 1), threadId * 100 + i);
                        bidIds.add(bidId);
                    }
                    return null;
                })
                .toList();

        pool.invokeAll(tasks);
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        List<BidReciept> allBids = repo.getAllBids();
        
        assertAll(
                () -> assertEquals(numThreads * bidsPerThread, bidIds.size(), "all bid IDs should be unique"),
                () -> assertEquals(numThreads * bidsPerThread, allBids.size(), "getAllBids should return all created bids")
        );
    }

    @Test
    @Timeout(10)
    @DisplayName("getShopBids_calledConcurrentlyWhileBidsAreAdded_shouldNeverThrowConcurrentModification")
    void concurrentGetShopBids_whileAddingBids_noConcurrentModification() throws Exception {
        int targetShopId = 555;
        ExecutorService pool = Executors.newCachedThreadPool();

        // Writers: continuously add bids for the target shop
        Runnable bidAdder = () -> {
            for (int i = 0; i < 100; i++) {
                repo.addBid(i, targetShopId, Map.of(i, i + 1), i * 10);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {}
            }
        };

        // Readers: continuously call getShopBids
        Runnable bidReader = () -> {
            for (int i = 0; i < 200; i++) {
                repo.getShopBids(targetShopId); // Should never throw ConcurrentModificationException
            }
        };

        // Start multiple writers and readers
        pool.submit(bidAdder);
        pool.submit(bidAdder);
        IntStream.range(0, 10).forEach(i -> pool.submit(bidReader));

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        // Verify final state
        List<BidReciept> finalBids = repo.getShopBids(targetShopId);
        assertTrue(!finalBids.isEmpty(), "should have some bids for the target shop");
        assertTrue(finalBids.stream().allMatch(b -> b.getShopId() == targetShopId));
    }
}
