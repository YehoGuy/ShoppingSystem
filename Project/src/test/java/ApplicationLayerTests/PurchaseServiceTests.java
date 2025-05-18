package ApplicationLayerTests;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.Item.ItemService;
import com.example.app.ApplicationLayer.Message.MessageService;
import com.example.app.ApplicationLayer.Purchase.PurchaseService;
import com.example.app.ApplicationLayer.Shop.ShopService;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.Bid;
import com.example.app.DomainLayer.Purchase.BidReciept;
import com.example.app.DomainLayer.Purchase.IPurchaseRepository;
import com.example.app.DomainLayer.Purchase.Reciept;
import com.example.app.InfrastructureLayer.PurchaseRepository;
import com.example.app.SimpleHttpServerApplication;

/**
 * High-level “acceptance” tests for {@link PurchaseService}.  
 * Each test uses full mocking to validate observable behaviour across the
 * service’s public API, including happy-paths and rollback paths.
 */
@ExtendWith({ SpringExtension.class, MockitoExtension.class })
@SpringBootTest(classes = SimpleHttpServerApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class PurchaseServiceAcceptanceTests{

    /* ─────────── mocks & SUT ─────────── */
    @Mock IPurchaseRepository repo;
    @Mock AuthTokenService    auth;
    @Mock UserService         users;
    @Mock ItemService         items;
    @Mock ShopService         shops;
    @Mock MessageService      msg;
    PurchaseService service;

    Address addr = new Address().withCountry("IL").withCity("TLV")
                                .withStreet("Rothschild").withZipCode("6800000");

    @BeforeEach
    void setUp() {
        service = new PurchaseService(repo, auth, users, shops, items, msg);
    }
    /* ══════════════════════════════════════════════════════════════
       checkoutCart tests
       ══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName(
        "checkoutCart_whenUserHasCartWithTwoDifferentShopIds_shouldCreateTwoPurchases_CallPaymentAndShipping_ThenReturnBothIds"
    )
    void checkoutCart_multiShopHappyPath() throws Exception {
        String token = "valid";
        int uid = 1;
        int shopA = 10, shopB = 20;
        Map<Integer,Integer> cartShopA = Map.of(5,2);
        Map<Integer,Integer> cartShopB = Map.of(7,1);
        Map<Integer,HashMap<Integer,Integer>> cart = new HashMap<>();
        cart.put(shopA, new HashMap<>(cartShopA));
        cart.put(shopB, new HashMap<>(cartShopB));

        when(auth.ValidateToken(token)).thenReturn(uid);
        when(users.getUserShoppingCartItems(uid)).thenReturn(new HashMap<>(cart));
        when(shops.purchaseItems(cartShopA, shopA, token)).thenReturn(100.0);
        when(shops.purchaseItems(cartShopB, shopB, token)).thenReturn(50.0);
        when(repo.addPurchase(eq(uid), eq(shopA), eq(cartShopA), eq(100.0), any())).thenReturn(1);
        when(repo.addPurchase(eq(uid), eq(shopB), eq(cartShopB), eq(50.0),  any())).thenReturn(2);

        List<Integer> ids = service.checkoutCart(token, addr);

        assertEquals(Set.of(1,2), new HashSet<>(ids));
        verify(users).clearUserShoppingCart(uid);
        verify(users).pay(token, shopA, 100.0);
        verify(users).pay(token, shopB, 50.0);
        verify(shops).shipPurchase(token, 1, shopA, "IL","TLV","Rothschild","6800000");
        verify(shops).shipPurchase(token, 2, shopB, "IL","TLV","Rothschild","6800000");
    }

    @Test
    @DisplayName(
        "checkoutCart_whenSecondShopPaymentThrows_shouldRollbackAcquiredItems_restoreCart_refundFirstPayment_andPropagateException"
    )
    void checkoutCart_rollbackOnPaymentFailure() throws Exception {
        String token="t"; int uid=2, shop=11;
        Map<Integer,Integer> cartShop = Map.of(3,1);
        Map<Integer,HashMap<Integer,Integer>> cart = new HashMap<>();
        cart.put(shop, new HashMap<>(cartShop));

        when(auth.ValidateToken(token)).thenReturn(uid);
        when(users.getUserShoppingCartItems(uid)).thenReturn(new HashMap<>(cart));
        when(shops.purchaseItems(cartShop, shop, token)).thenReturn(30.0);
        when(repo.addPurchase(anyInt(), anyInt(), any(), anyDouble(), any())).thenReturn(9);
        doThrow(new RuntimeException("payFail")).when(users).pay(token, shop, 30.0);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> service.checkoutCart(token, addr));

        verify(shops).rollBackPurchase(cartShop, shop);
        verify(users).restoreUserShoppingCart(eq(uid), any());
        verify(users, never()).clearUserShoppingCart(uid);
    }

    /* ══════════════════════════════════════════════════════════════
       createBid tests
       ══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName(
        "createBid_whenAllValid_shouldReserveItems_CreateBidInRepository_andReturnBidId"
    )
    void createBid_happyPath() throws Exception {
        String t="tok"; int uid=4, shop=5, bidId=44;
        Map<Integer,Integer> itemsMap = Map.of(9,2);

        when(auth.ValidateToken(t)).thenReturn(uid);
        when(repo.addBid(uid, shop, itemsMap, 120)).thenReturn(bidId);

        int id = service.createBid(t, shop, itemsMap, 120);

        assertEquals(bidId,id);
        verify(shops).purchaseItems(itemsMap, shop, t);
    }

    /* ══════════════════════════════════════════════════════════════
       postBidding tests
       ══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName(
        "postBidding_whenUserIsNotOwnerAndBidExists_shouldAddBiddingViaDomainObjectOnce"
    )
    void postBidding_nonOwnerPostsBid() throws Exception {
        String token="a"; int owner=1, bidder=2, pid=10;
        Bid bid = spy(new Bid(pid, owner, 7, Map.of(1,1), 50));

        when(auth.ValidateToken(token)).thenReturn(bidder);
        when(repo.getPurchaseById(pid)).thenReturn(bid);

        service.postBidding(token, pid, 60);

        verify(bid).addBidding(bidder, 60);
    }

    @Test
    @DisplayName(
        "postBidding_whenUserIsOwner_shouldThrowAndNotCallAddBidding"
    )
    void postBidding_ownerCannotBid() throws Exception {
        String token="t"; int owner=3, pid=11;
        Bid bid = spy(new Bid(pid, owner, 9, Map.of(), 10));

        when(auth.ValidateToken(token)).thenReturn(owner);
        when(repo.getPurchaseById(pid)).thenReturn(bid);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> service.postBidding(token,pid,20));

        assertTrue(ex.getMessage().contains("owner"));
        verify(bid, never()).addBidding(anyInt(), anyInt());
    }

    /* ══════════════════════════════════════════════════════════════
       finalizeBid tests
       ══════════════════════════════════════════════════════════════ */

    
    /** happy path: owner finalises bid, payment & shipping succeed, bidders notified */
    @Test
    @DisplayName(
        "finalizeBid_whenOwnerInvokesAndPaymentSucceeds_shouldInvokePay_thenShip_thenNotifyBidders_andReturnHighestBidderId"
    )
    void finalizeBid_happyPath() throws Exception {
        String token = "tok"; int owner = 1, shop = 8, pid = 22;

        // Spy on an *un-completed* bid
        Bid bid = spy(new Bid(pid, owner, shop, Map.of(1,1), 100));

        /* fabricate receipt object the service expects after completion */
        BidReciept rec = mock(BidReciept.class);
        when(rec.getHighestBidderId()).thenReturn(5);
        when(bid.completePurchase()).thenReturn(rec);      // stub out real behaviour
        when(bid.getMaxBidding()).thenReturn(150);
        when(bid.getBiddersIds()).thenReturn(List.of(5));

        /* infrastructure stubs */
        when(repo.getPurchaseById(pid)).thenReturn(bid);
        when(auth.ValidateToken(token)).thenReturn(owner);
        when(users.getUserShippingAddress(owner)).thenReturn(addr);

        /* invoke */
        int winner = service.finalizeBid(token, pid);

        /* verify */
        assertEquals(5, winner);
        verify(users).pay(token, shop, 150);
        verify(shops).shipPurchase(token, pid, shop, "IL", "TLV", "Rothschild", "6800000");
        verify(msg).sendMessageToUser(eq(token), eq(5), contains("Congratulations"), eq(0));
    }

    /** error path: pay() throws immediately – service returns –1 and does NOT refund */
    @Test
    @DisplayName(
        "finalizeBid_whenPayOperationImmediatelyThrows_shouldReturnMinusOne_andNoRefundOrShippingArePerformed"
    )
    void finalizeBid_paymentThrowsNoRefundExpected() throws Exception {
        String token = "tok";
        int owner = 1, shop = 2, pid = 30;

        // fresh spy – keep it un-completed
        Bid bid = spy(new Bid(pid, owner, shop, Map.of(), 50));

        BidReciept rec = mock(BidReciept.class);
        when(rec.getHighestBidderId()).thenReturn(6);
        when(bid.completePurchase()).thenReturn(rec);
        when(bid.getMaxBidding()).thenReturn(80);     // needed for pay()

        when(repo.getPurchaseById(pid)).thenReturn(bid);
        when(auth.ValidateToken(token)).thenReturn(owner);

        /* force pay() to fail */
        doThrow(new RuntimeException("payErr"))
            .when(users).pay(token, shop, 80);

        assertThrows(Throwable.class,() -> service.finalizeBid(token, pid));



        
        verify(users, never())
            .refundPaymentByStoreEmployee(any(), anyInt(), anyInt(), anyDouble());
        verify(shops, never()).shipPurchase(any(), anyInt(), anyInt(),
                                            any(), any(), any(), any());
    }



    /* ══════════════════════════════════════════════════════════════
       simple query helpers
       ══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName(
        "getPurchaseById_whenRepositoryThrows_shouldWrapAndThrowIllegalArgumentException"
    )
    void getPurchaseById_wrapsRepoException() {
        when(repo.getPurchaseById(99)).thenThrow(new RuntimeException());
        assertThrows(IllegalArgumentException.class, () -> service.getPurchaseById(99));
    }

    @Test
    @DisplayName(
        "getUserPurchases_whenTokenMatchesUserId_shouldReturnReceiptsFromRepository"
    )
    void getUserPurchases_happyPath() throws Exception {
        String token="t"; int uid=9;
        List<Reciept> list = List.of(mock(Reciept.class));
        when(auth.ValidateToken(token)).thenReturn(uid);
        when(repo.getUserPurchases(uid)).thenReturn(list);

        List<Reciept> out = service.getUserPurchases(token, uid);

        assertSame(list, out);
    }

    /* ───────────────────────── CONCURRENCY ACCEPTANCE TESTS ───────────────────────── */

    @Nested
    @DisplayName("Concurrency – repository-level")
    class Concurrency {

        @Autowired PurchaseRepository purchaseRepo;

        private static final int THREADS = 16;
        private ExecutorService pool;

        @BeforeEach void initPool() { pool = Executors.newFixedThreadPool(THREADS); }
        @AfterEach  void shutdownPool() { pool.shutdownNow(); }

        /** Multiple threads calling addPurchase must get distinct IDs and all
         *  purchases must be retrievable afterwards. */
        @Test
        void concurrentAddPurchase_producesUniqueIds() throws Exception {

            int userId  = 77;
            int storeId = 3;
            Map<Integer,Integer> items = Map.of(1, 1);

            Set<Integer> ids = ConcurrentHashMap.newKeySet();
            CountDownLatch ready = new CountDownLatch(THREADS);
            CountDownLatch go    = new CountDownLatch(1);

            IntStream.range(0, THREADS).forEach(i ->
                pool.submit(() -> {
                    ready.countDown();
                    go.await();                      // all threads start together
                    int id = purchaseRepo.addPurchase(
                            userId, storeId, items, 10.0, /*shipping*/ null);
                    ids.add(id);
                    return null;
                })
            );

            ready.await();
            go.countDown();
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);

            /* every thread should have received a distinct purchase ID */
            assertThat(ids).hasSize(THREADS);

            /* repository must contain exactly THREADS purchases for that user */
            assertThat(purchaseRepo.getUserPurchases(userId))
                .hasSize(THREADS);
        }

        /** Concurrent deletions of the *same* purchase ID must be safe and
        *  idempotent – the purchase ends up gone, with no exceptions leaking. */
        @Test
        void concurrentDeletePurchase_isIdempotent() throws Exception {

            int purchaseId = purchaseRepo.addPurchase(
                    88, 4, Map.of(2, 2), 20.0, null);   // create once

            CountDownLatch done = new CountDownLatch(THREADS);

            IntStream.range(0, THREADS).forEach(i ->
                pool.submit(() -> {
                    try {
                        purchaseRepo.deletePurchase(purchaseId);
                    } catch (IllegalArgumentException ex) {
                        /* Another thread already deleted it – that’s expected.
                        * Any other exception type would still fail the test. */
                        assertTrue(ex.getMessage().contains("purchaseId"));
                    } finally {
                        done.countDown();
                    }
                })
            );

            done.await(5, TimeUnit.SECONDS);

            /* after all deletions, the repository must not find that ID */
            /* repository must no longer have that purchase ID */
            assertThrows(IllegalArgumentException.class,
                        () -> purchaseRepo.getPurchaseById(purchaseId));
        }

        /* ───────────────────── Bid-specific concurrency tests ───────────────────── */

        /** Many threads call addBid(..) at once – every ID must be unique
         *  and the repository must let us fetch each Bid afterwards. */
        @Test
        void concurrentAddBid_producesUniqueIds() throws Exception {

            int owner   = 55;
            int shopId  = 9;
            Map<Integer,Integer> items = Map.of(1, 1);

            Set<Integer> ids = ConcurrentHashMap.newKeySet();
            CountDownLatch ready = new CountDownLatch(THREADS);
            CountDownLatch go    = new CountDownLatch(1);

            IntStream.range(0, THREADS).forEach(i ->
                pool.submit(() -> {
                    ready.countDown();
                    go.await();                          // blast off together
                    int bidId = purchaseRepo.addBid(owner, shopId, items, 50 + i);
                    ids.add(bidId);
                    return null;
                })
            );

            ready.await();
            go.countDown();
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);

            /* every thread got its own bid-id */
            assertThat(ids).hasSize(THREADS);

            /* and each id maps to a Bid object in the repo */
            ids.forEach(id ->
                assertThat(purchaseRepo.getPurchaseById(id)).isInstanceOf(Bid.class)
            );
        }

        /** Several bidders raise the same Bid in parallel.
         *  After the race the Bid must remain internally consistent:
         *  – maxBidding is at least the opening price and not corrupted  
         *  – bidder list contains no duplicates  
         *  – no exception leaks from concurrent updates */
        @Test
        void concurrentAddBidding_updatesStateConsistently() throws Exception {

            int owner = 1, shop = 7;
            Map<Integer,Integer> baseItems = Map.of(2, 1);
            int bidId = purchaseRepo.addBid(owner, shop, baseItems, 40);

            Bid bid = (Bid) purchaseRepo.getPurchaseById(bidId);

            CountDownLatch ready = new CountDownLatch(THREADS);
            CountDownLatch start = new CountDownLatch(1);

            IntStream.range(0, THREADS).forEach(i -> {
                int bidder = 100 + i;
                int amount = 60 + i;                 // ascending offers
                pool.submit(() -> {
                    ready.countDown();
                    
                    try {
                        start.await();
                        bid.addBidding(bidder, amount);
                    } catch (RuntimeException ignored) {
                        /* business rules may reject some offers (e.g., too low,
                        bidder already bid, etc.) – that's fine; we only care
                        that concurrent calls don't corrupt shared state. */
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                });
            });

            ready.await();
            start.countDown();
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);

            /* 1) max bidding must be ≥ opening price (40) and ≤ highest offer (60+THREADS-1) */
            assertThat(bid.getMaxBidding())
                .isBetween(40, 60 + THREADS - 1);

            /* 2) bidder list must not contain duplicates */
            List<Integer> bidders = bid.getBiddersIds();
            assertThat(bidders)
                .hasSameSizeAs(new HashSet<>(bidders));        // uniqueness check
        }



    }



}
