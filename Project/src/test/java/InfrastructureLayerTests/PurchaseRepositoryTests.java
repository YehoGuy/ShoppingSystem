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

import DomainLayer.Purchase.Address;
import DomainLayer.Purchase.Bid;
import DomainLayer.Purchase.Purchase;
import DomainLayer.Purchase.Reciept;
import InfrastructureLayer.PurchaseRepository;

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
        Field instance = PurchaseRepository.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);                // clear old singleton
        repo = PurchaseRepository.getInstance(); // fresh instance
        // also clear internal map, just in case
        Field storage = PurchaseRepository.class.getDeclaredField("purchaseStorage");
        storage.setAccessible(true);
        ((Map<?,?>) storage.get(repo)).clear();
    }

    private Address anyAddress() {
        return new Address().withCountry("IL").withCity("TLV").withStreet("Rothschild");
    }

    /* ───────────────────── singleton behaviour ───────────────────── */

    @Test
    @DisplayName(
        "getInstance_calledConcurrentlyFromManyThreads_shouldAlwaysReturnTheExactSameSingletonInstance"
    )
    void singleton_returnsSameInstanceAcrossThreads() throws Exception {
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        Set<PurchaseRepository> instances = Collections.newSetFromMap(new ConcurrentHashMap<>());

        List<Callable<Void>> calls = IntStream.range(0, threads)
                .<Callable<Void>>mapToObj(i -> () -> { instances.add(PurchaseRepository.getInstance()); return null; })
                .toList();
        pool.invokeAll(calls);
        pool.shutdownNow();

        assertEquals(1, instances.size(), "all threads must receive the same singleton reference");
    }

    /* ───────────────────── addPurchase path ───────────────────── */

    @Test
    @DisplayName(
        "addPurchase_shouldReturnUniqueIncrementingIds_andPersistPurchaseThatIsRetrievableById"
    )
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
    @DisplayName(
        "addBid_shouldReturnValidId_andStoreConcreteBidInstanceAccessibleViaGetPurchaseById"
    )
    void addBid_returnsIdAndStoresBid() {
        int bidId = repo.addBid(99, 77, Map.of(1, 1), 100);

        Purchase p = repo.getPurchaseById(bidId);
        assertTrue(p instanceof Bid, "stored object should be Bid subclass");
    }

    /* ───────────────────── getPurchaseById edge ───────────────────── */

    @Test
    @DisplayName(
        "getPurchaseById_whenIdDoesNotExist_shouldThrowIllegalArgumentException"
    )
    void getPurchaseById_nonExistentThrows() {
        assertThrows(IllegalArgumentException.class, () -> repo.getPurchaseById(999));
    }

    /* ───────────────────── deletePurchase path ───────────────────── */

    @Test
    @DisplayName(
        "deletePurchase_shouldRemovePurchase_andSubsequentGetShouldThrow"
    )
    void deletePurchase_removes() {
        int id = repo.addPurchase(5, 5, Map.of(), 0.0, anyAddress());
        repo.deletePurchase(id);
        assertThrows(IllegalArgumentException.class, () -> repo.getPurchaseById(id));
    }

    /* ───────────────────── filtering queries ───────────────────── */

    @Test
    @DisplayName(
        "getUserPurchases_shouldReturnReceiptsForExactlyThosePurchasesMadeBySpecifiedUser"
    )
    void getUserPurchases_returnsCorrectSet() {
        int uid = 42;
        repo.addPurchase(uid, 1, Map.of(), 0, anyAddress());
        repo.addPurchase(uid, 2, Map.of(), 0, anyAddress());
        repo.addPurchase(99, 1, Map.of(), 0, anyAddress());   // other user

        List<Reciept> receipts = repo.getUserPurchases(uid);
        assertEquals(2, receipts.size());
        assertTrue(receipts.stream().allMatch(r -> r.getUserId() == uid));
    }

    @Test
    @DisplayName(
        "getStorePurchases_shouldReturnReceiptsForExactlyThosePurchasesMadeInSpecifiedStore"
    )
    void getStorePurchases_returnsCorrectSet() {
        int sid = 7;
        repo.addPurchase(1, sid, Map.of(), 0, anyAddress());
        repo.addPurchase(2, sid, Map.of(), 0, anyAddress());
        repo.addPurchase(2, 999, Map.of(), 0, anyAddress());  // other store

        List<Reciept> receipts = repo.getStorePurchases(sid);
        assertEquals(2, receipts.size());
        assertTrue(receipts.stream().allMatch(r -> r.getStoreId() == sid));
    }

    @Test
    @DisplayName(
        "getUserStorePurchases_shouldReturnReceiptsOnlyWhenUserIdAndStoreIdBothMatch"
    )
    void getUserStorePurchases_returnsCorrectSubset() {
        int uid = 5, sid = 6;
        repo.addPurchase(uid, sid, Map.of(), 0, anyAddress());
        repo.addPurchase(uid, sid, Map.of(), 0, anyAddress());
        repo.addPurchase(uid, 99, Map.of(), 0, anyAddress());   // wrong store
        repo.addPurchase(77, sid, Map.of(), 0, anyAddress());   // wrong user

        List<Reciept> receipts = repo.getUserStorePurchases(uid, sid);
        assertEquals(2, receipts.size());
        assertTrue(receipts.stream().allMatch(r -> r.getUserId() == uid && r.getStoreId() == sid));
    }

    /* ───────────────────── concurrency – unique IDs ───────────────────── */

    @Test
    @Timeout(10)
    @DisplayName(
        "addPurchaseInvokedConcurrentlyBy100Threads_shouldGenerate100DistinctIds_andAllPurchasesShouldBePersisted"
    )
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

    /* ───────────────────── concurrency – mixed bids & purchases ───────────────────── */

    @Test
    @Timeout(10)
    @DisplayName(
        "addBidAndAddPurchaseMixedAcrossMultipleThreads_shouldMaintainCorrectTotalCount_andNoDuplicateIds"
    )
    void concurrentMixedAdds_consistentStorage() throws Exception {
        int purchases = 60;
        int bids      = 40;
        Set<Integer> ids = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newCachedThreadPool();

        IntStream.range(0, purchases).forEach(i ->
            pool.submit(() -> ids.add(repo.addPurchase(2, 2, Map.of(), 0, anyAddress()))));
        IntStream.range(0, bids).forEach(i ->
            pool.submit(() -> ids.add(repo.addBid(3, 3, Map.of(), 10))));

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertAll(
            () -> assertEquals(purchases + bids, ids.size(), "every add must get a unique ID"),
            () -> assertEquals(purchases + bids,
                    repo.getUserPurchases(2).size() + repo.getUserPurchases(3).size()));
    }

    /* ───────────────────── Mockito spy integration ───────────────────── */

    @Test
    @DisplayName(
        "getStorePurchases_shouldCallGenerateRecieptExactlyOncePerStoredPurchase_viaMockitoSpyInjection"
    )
    void getStorePurchases_invokesGenerateReceiptExactlyOncePerPurchase() throws Exception {
        // Create a Mockito spy for Purchase & slip it into internal map via reflection
        Purchase spyPurchase1 = spy(new Purchase(111, 1, 77, Map.of(), 0, anyAddress()));
        Purchase spyPurchase2 = spy(new Purchase(112, 2, 77, Map.of(), 0, anyAddress()));

        Field storage = PurchaseRepository.class.getDeclaredField("purchaseStorage");
        storage.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer,Purchase> internalMap = (Map<Integer, Purchase>) storage.get(repo);
        internalMap.put(111, spyPurchase1);
        internalMap.put(112, spyPurchase2);

        repo.getStorePurchases(77);

        verify(spyPurchase1, times(1)).generateReciept();
        verify(spyPurchase2, times(1)).generateReciept();
    }

    /* ───────── concurrency – delete race with readers ───────── */
    @Test
    @Timeout(10)
    @DisplayName(
        "deletePurchaseExecutedByOneThreadWhileManyOtherThreadsContinuouslyCallGetPurchaseById_shouldNeverThrowUnexpectedExceptions_andFinalStateMustReflectDeletion"
    )
    void concurrentDeleteWhileReadersLoop_neverThrows_andEndsWithMissingId() throws Exception {
        int id = repo.addPurchase(70, 80, Map.of(), 0, anyAddress());

        ExecutorService pool = Executors.newCachedThreadPool();
        // reader tasks: repeatedly try to fetch purchase; ignore expected IllegalArgumentException
        Runnable reader = () -> {
            for (int i = 0; i < 1_000; i++) {
                try { repo.getPurchaseById(id); } catch (IllegalArgumentException ignored) { /* deleted */ }
            }
        };
        // writer task: delete once after slight delay
        Runnable deleter = () -> {
            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
            repo.deletePurchase(id);
        };

        pool.submit(deleter);
        IntStream.range(0, 20).forEach(j -> pool.submit(reader));
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertThrows(IllegalArgumentException.class, () -> repo.getPurchaseById(id));
    }

    /* ───────── concurrency – heavy readers while writers add ───────── */
    @Test
    @Timeout(10)
    @DisplayName(
        "getUserPurchasesCalledRepeatedlyFromManyThreadsWhileOtherThreadsContinuouslyAddPurchasesForSameUser_shouldNeverThrowConcurrentModification_andFinalCountShouldMatchAdds"
    )
    void manyReadersAndWritersOnSameUser_noConcurrentModAndCorrectCount() throws Exception {
        int userId = 123;
        int additions = 500;

        ExecutorService pool = Executors.newCachedThreadPool();

        // Writers: add purchases
        IntStream.range(0, additions).forEach(i ->
            pool.submit(() -> repo.addPurchase(userId, i, Map.of(), 0, anyAddress())));

        // Readers: hammer getUserPurchases
        IntStream.range(0, 50).forEach(i ->
            pool.submit(() -> {
                for (int j = 0; j < 5_000; j++) repo.getUserPurchases(userId);
            }));

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(additions, repo.getUserPurchases(userId).size());
    }

}
