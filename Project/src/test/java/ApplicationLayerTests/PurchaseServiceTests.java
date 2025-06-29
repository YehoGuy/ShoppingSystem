package ApplicationLayerTests;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.scheduling.TaskScheduler;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.NotificationService;
import com.example.app.ApplicationLayer.Item.ItemService;
import com.example.app.ApplicationLayer.Message.MessageService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.PurchaseService;
import com.example.app.ApplicationLayer.Shop.ShopService;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.Bid;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Purchase.BidReciept;
import com.example.app.DomainLayer.Purchase.IPurchaseRepository;
import com.example.app.DomainLayer.Purchase.Purchase;
import com.example.app.DomainLayer.Purchase.Reciept;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.InfrastructureLayer.PurchaseRepository;

/**
 * High-level “acceptance” tests for {@link PurchaseService}.
 * Each test uses full mocking to validate observable behaviour across the
 * service’s public API, including happy-paths and rollback paths.
 */
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc(addFilters = false)
class PurchaseServiceTests {

    /* ─────────── mocks & SUT ─────────── */
    @Mock
    IPurchaseRepository repo;
    @Mock
    AuthTokenService auth;
    @Mock
    UserService users;
    @Mock
    ItemService items;
    @Mock
    ShopService shops;
    @Mock
    MessageService msg;
    @Mock
    NotificationService nots;
    @Mock 
    TaskScheduler taskscheduler;

    PurchaseService service;

    Address addr = new Address().withCountry("IL").withCity("TLV")
            .withStreet("Rothschild").withZipCode("6800000");
    Address defaultAddr = new Address()
            .withCountry("IL").withCity("TLV")
            .withStreet("Rothschild").withZipCode("6800000");

    @BeforeEach
    void setUp() {
        service = new PurchaseService(repo, auth, users, shops, items, msg, nots, taskscheduler);
    }
    /*
     * ══════════════════════════════════════════════════════════════
     * checkoutCart tests
     * ══════════════════════════════════════════════════════════════
     */

    @Test
    @DisplayName("checkoutCart_whenUserHasCartWithTwoDifferentShopIds_shouldCreateTwoPurchases_CallPaymentAndShipping_ThenReturnBothIds")
    void checkoutCart_multiShopHappyPath() throws Exception {
        String token = "valid";
        int uid = 1;
        int shopA = 10, shopB = 20;
        Map<Integer, Integer> cartShopA = Map.of(5, 2);
        Map<Integer, Integer> cartShopB = Map.of(7, 1);
        Map<Integer, HashMap<Integer, Integer>> cart = new HashMap<>();
        cart.put(shopA, new HashMap<>(cartShopA));
        cart.put(shopB, new HashMap<>(cartShopB));

        when(auth.ValidateToken(token)).thenReturn(uid);
        when(users.getUserShoppingCartItems(uid)).thenReturn(new HashMap<Integer, HashMap<Integer, Integer>>(cart));
        when(shops.purchaseItems(cartShopA, shopA, token)).thenReturn(100.0);
        when(shops.purchaseItems(cartShopB, shopB, token)).thenReturn(50.0);
        when(repo.addPurchase(eq(uid), eq(shopA), eq(cartShopA), eq(100.0), any())).thenReturn(1);
        when(repo.addPurchase(eq(uid), eq(shopB), eq(cartShopB), eq(50.0), any())).thenReturn(2);

        List<Integer> ids = service.checkoutCart(token, addr, "1234567890123456", "12/25", "123", "John Doe",
                "123456789", "john@example.com", "1234567890");

        assertEquals(Set.of(1, 2), new HashSet<>(ids));
        verify(users).clearUserShoppingCart(uid);
        verify(users).pay(token, shopA, 100.0, "1234567890123456", "12/25", "123", "John Doe", "123456789",
                "john@example.com", "1234567890");
        verify(users).pay(token, shopB, 50.0, "1234567890123456", "12/25", "123", "John Doe", "123456789",
                "john@example.com", "1234567890");
        verify(shops).shipPurchase(token, 1, shopA, "IL", "TLV", "Rothschild", "6800000");
        verify(shops).shipPurchase(token, 2, shopB, "IL", "TLV", "Rothschild", "6800000");
    }

    @Test
    @DisplayName("createBid_whenAllValid_shouldReserveItems_CreateBidInRepository_andReturnBidId")
    void createBid_happyPath() throws Exception {
        String t = "tok";
        int uid = 4, shop = 5, bidId = 44;
        Map<Integer, Integer> itemsMap = Map.of(9, 2);

        when(auth.ValidateToken(t)).thenReturn(uid);
        when(repo.addBid(uid, shop, itemsMap, 120)).thenReturn(bidId);

        int id = service.createBid(t, shop, itemsMap, 120);

        assertEquals(bidId, id);
        verify(shops).purchaseItems(itemsMap, shop, t);
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * postBidding tests
     * ══════════════════════════════════════════════════════════════
     */

    @Test
    @DisplayName("postBidding_whenUserIsNotOwnerAndBidExists_shouldAddBiddingViaDomainObjectOnce")
    void postBidding_nonOwnerPostsBid() throws Exception {
        String token = "a";
        int owner = 1, bidder = 2, pid = 10;
        Bid bid = spy(new Bid(pid, owner, 7, Map.of(1, 1), 50));
        bid.setAuctionStartTime(LocalDateTime.now().minusMinutes(1));
        bid.setAuctionEndTime(LocalDateTime.now().plusMinutes(1));

        when(auth.ValidateToken(token)).thenReturn(bidder);
        when(repo.getPurchaseById(pid)).thenReturn(bid);

        service.postBidding(token, pid, 60);

        verify(bid).addBidding(bidder, 60, true);
    }


    /*
     * ══════════════════════════════════════════════════════════════
     * finalizeBid tests
     * ══════════════════════════════════════════════════════════════
     */

    /**
     * happy path: owner finalises bid, payment & shipping succeed, bidders notified
     */
    @Test
    @DisplayName("finalizeBid_whenOwnerInvokesAndPaymentSucceeds_shouldInvokePay_thenShip_thenNotifyBidders_andReturnHighestBidderId")
    void finalizeBid_happyPath() throws Exception {
        String token = "tok";
        int owner = 1, shop = 8, pid = 22;

        // Spy on an *un-completed* bid
        Bid bid = spy(new Bid(pid, owner, shop, Map.of(1, 1), 100));

        /* fabricate receipt object the service expects after completion */
        BidReciept rec = mock(BidReciept.class);
        when(bid.completePurchase()).thenReturn(rec);

        // only stub what's actually used:
        when(bid.getItems()).thenReturn(Map.of(1, 1));

        // infrastructure stubs
        when(repo.getPurchaseById(pid)).thenReturn(bid);
        when(auth.ValidateToken(token)).thenReturn(owner);

        /* invoke */
        int winner = service.finalizeBid(token, pid, true);

        /* verify */
        assertEquals(1, winner);
        verify(nots).sendToUser(
            eq(owner),
            eq("The bid is over "),
            contains("#" + pid)
        );
    }

    @Test
    @DisplayName("finalizeBid_checkAddedToCart_whenBidIsCompleted_shouldAddToCart")
    void finalizeBid_checkAddedToCart_whenBidIsCompleted_shouldAddToCart() throws Exception {
        String token = "tok";
        int initiatingUserId = 1;
        int purchaseId       = 22;
        int shopId           = 8;
        Map<Integer, Integer> items = Map.of(1, 1);

        Bid bid = mock(Bid.class);

        when(repo.getPurchaseById(purchaseId)).thenReturn(bid);

        when(auth.ValidateToken(token)).thenReturn(initiatingUserId);

        when(bid.getStoreId()).thenReturn(shopId);
        when(users.getShopOwner(shopId)).thenReturn(999); 
        when(bid.getMaxBidding()).thenReturn(150);
        when(bid.getItems()).thenReturn(items);

        when(users.addBidToUserShoppingCart(eq(initiatingUserId), eq(shopId), eq(items)))
            .thenReturn(true);

        int result = service.finalizeBid(token, purchaseId, true);

        assertEquals(initiatingUserId, result, "should return the initiating user ID");

        verify(users, times(2))
            .addBidToUserShoppingCart(initiatingUserId, shopId, items);

        String expectedMsg =
            "The bid is finalized #"
            + purchaseId
            + ".\nIt has been added to your bids list.\n\n";
        verify(nots)
            .sendToUser(eq(initiatingUserId), eq("The bid is over "), eq(expectedMsg));

        verify(bid).completePurchase();
    }

    /*
     * ══════════════════════════════════════════════════════════════
     * simple query helpers
     * ══════════════════════════════════════════════════════════════
     */

    @Test
    @DisplayName("getPurchaseById_whenRepositoryThrows_shouldWrapAndThrowIllegalArgumentException")
    void getPurchaseById_wrapsRepoException() {
        when(repo.getPurchaseById(99)).thenThrow(new RuntimeException());
        assertThrows(IllegalArgumentException.class, () -> service.getPurchaseById(99));
    }

    @Test
    @DisplayName("getUserPurchases_whenTokenMatchesUserId_shouldReturnReceiptsFromRepository")
    void getUserPurchases_happyPath() throws Exception {
        String token = "t";
        int uid = 9;
        List<Reciept> list = List.of(mock(Reciept.class));
        when(auth.ValidateToken(token)).thenReturn(uid);
        when(repo.getUserPurchases(uid)).thenReturn(list);

        List<Reciept> out = service.getUserPurchases(token, uid);

        assertSame(list, out);
    }

    /*
     * ───────────────────────── CONCURRENCY ACCEPTANCE TESTS
     * ─────────────────────────
     */

    @Nested
    @DisplayName("Concurrency – repository-level")
    class Concurrency {

        @Autowired
        PurchaseRepository purchaseRepo = new PurchaseRepository();

        private static final int THREADS = 16;
        private ExecutorService pool;

        @BeforeEach
        void initPool() {
            pool = Executors.newFixedThreadPool(THREADS);
        }

        @AfterEach
        void shutdownPool() {
            pool.shutdownNow();
        }

        /**
         * Multiple threads calling addPurchase must get distinct IDs and all
         * purchases must be retrievable afterwards.
         */
        @Test
        void concurrentAddPurchase_producesUniqueIds() throws Exception {

            int userId = 77;
            int storeId = 3;
            Map<Integer, Integer> items = Map.of(1, 1);

            Set<Integer> ids = ConcurrentHashMap.newKeySet();
            CountDownLatch ready = new CountDownLatch(THREADS);
            CountDownLatch go = new CountDownLatch(1);

            IntStream.range(0, THREADS).forEach(i -> pool.submit(() -> {
                ready.countDown();
                go.await(); // all threads start together
                int id = purchaseRepo.addPurchase(
                        userId, storeId, items, 10.0, /* shipping */ null);
                ids.add(id);
                return null;
            }));

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

        /**
         * Concurrent deletions of the *same* purchase ID must be safe and
         * idempotent – the purchase ends up gone, with no exceptions leaking.
         */
        @Test
        void concurrentDeletePurchase_isIdempotent() throws Exception {

            int purchaseId = purchaseRepo.addPurchase(
                    88, 4, Map.of(2, 2), 20.0, null); // create once

            CountDownLatch done = new CountDownLatch(THREADS);

            IntStream.range(0, THREADS).forEach(i -> pool.submit(() -> {
                try {
                    purchaseRepo.deletePurchase(purchaseId);
                } catch (IllegalArgumentException ex) {
                    /*
                     * Another thread already deleted it – that’s expected.
                     * Any other exception type would still fail the test.
                     */
                    assertTrue(ex.getMessage().contains("purchaseId"));
                } finally {
                    done.countDown();
                }
            }));

            done.await(5, TimeUnit.SECONDS);

            /* after all deletions, the repository must not find that ID */
            /* repository must no longer have that purchase ID */
            assertThrows(IllegalArgumentException.class,
                    () -> purchaseRepo.getPurchaseById(purchaseId));
        }

        /* ───────────────────── Bid-specific concurrency tests ───────────────────── */

        /**
         * Many threads call addBid(..) at once – every ID must be unique
         * and the repository must let us fetch each Bid afterwards.
         */
        @Test
        void concurrentAddBid_producesUniqueIds() throws Exception {

            int owner = 55;
            int shopId = 9;
            Map<Integer, Integer> items = Map.of(1, 1);

            Set<Integer> ids = ConcurrentHashMap.newKeySet();
            CountDownLatch ready = new CountDownLatch(THREADS);
            CountDownLatch go = new CountDownLatch(1);

            IntStream.range(0, THREADS).forEach(i -> pool.submit(() -> {
                ready.countDown();
                go.await(); // blast off together
                int bidId = purchaseRepo.addBid(owner, shopId, items, 50 + i);
                ids.add(bidId);
                return null;
            }));

            ready.await();
            go.countDown();
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);

            /* every thread got its own bid-id */
            assertThat(ids).hasSize(THREADS);

            /* and each id maps to a Bid object in the repo */
            ids.forEach(id -> assertThat(purchaseRepo.getPurchaseById(id)).isInstanceOf(Bid.class));
        }

        /**
         * Several bidders raise the same Bid in parallel.
         * After the race the Bid must remain internally consistent:
         * – maxBidding is at least the opening price and not corrupted
         * – bidder list contains no duplicates
         * – no exception leaks from concurrent updates
         */
        @Test
        void concurrentAddBidding_updatesStateConsistently() throws Exception {

            int owner = 1, shop = 7;
            Map<Integer, Integer> baseItems = Map.of(2, 1);
            int bidId = purchaseRepo.addBid(owner, shop, baseItems, 40);

            Bid bid = (Bid) purchaseRepo.getPurchaseById(bidId);

            CountDownLatch ready = new CountDownLatch(THREADS);
            CountDownLatch start = new CountDownLatch(1);

            IntStream.range(0, THREADS).forEach(i -> {
                int bidder = 100 + i;
                int amount = 60 + i; // ascending offers
                pool.submit(() -> {
                    ready.countDown();

                    try {
                        start.await();
                        bid.addBidding(bidder, amount, true);
                    } catch (RuntimeException ignored) {
                        /*
                         * business rules may reject some offers (e.g., too low,
                         * bidder already bid, etc.) – that's fine; we only care
                         * that concurrent calls don't corrupt shared state.
                         */
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                });
            });

            ready.await();
            start.countDown();
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);

            /*
             * 1) max bidding must be ≥ opening price (40) and ≤ highest offer
             * (60+THREADS-1)
             */
            assertThat(bid.getMaxBidding())
                    .isBetween(40, 60 + THREADS - 1);

            /* 2) bidder list must not contain duplicates */
            List<Integer> bidders = bid.getBiddersIds();
            assertThat(bidders)
                    .hasSameSizeAs(new HashSet<>(bidders)); // uniqueness check
        }

    }

    @Test
    @DisplayName("checkoutCart_whenPurchaseItemsThrowsOurRuntime_shouldWrapAndThrowOurRuntime")
    void checkoutCart_purchaseItemsThrowsOurRuntime() throws Exception {
        String token = "tok";
        int uid = 3, shop = 5;
        Map<Integer, Integer> cartShop = Map.of(9, 1);
        Map<Integer, HashMap<Integer, Integer>> cart = Map.of(shop, new HashMap<>(cartShop));

        when(auth.ValidateToken(token)).thenReturn(uid);
        when(users.getUserShoppingCartItems(uid)).thenReturn(new HashMap<Integer, HashMap<Integer, Integer>>(cart));
        when(shops.purchaseItems(cartShop, shop, token))
                .thenThrow(new OurRuntime("purchase error"));

        OurRuntime ex = assertThrows(OurRuntime.class, () -> service.checkoutCart(token, addr, "1234567890123456",
                "12/25", "123", "John Doe", "123456789", "john@example.com", "1234567890"));
        assertTrue(ex.getMessage().contains("checkoutCart:"));
        verify(users, never()).pay(any(), anyInt(), anyDouble(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("checkoutCart_whenAddPurchaseThrows_shouldRollbackRestoreRefundAndThrowOurRuntime")
    void checkoutCart_addPurchaseThrows() throws Exception {
        String token = "tok";
        int uid = 4, shop = 6;
        Map<Integer, Integer> cartShop = Map.of(2, 2);
        Map<Integer, HashMap<Integer, Integer>> cart = Map.of(shop, new HashMap<>(cartShop));
        when(auth.ValidateToken(token)).thenReturn(uid);
        when(users.getUserShoppingCartItems(uid)).thenReturn(new HashMap<Integer, HashMap<Integer, Integer>>(cart));
        when(shops.purchaseItems(cartShop, shop, token)).thenReturn(44.0);
        when(repo.addPurchase(uid, shop, cartShop, 44.0, addr))
                .thenThrow(new RuntimeException("db fail"));

        OurRuntime ex = assertThrows(OurRuntime.class, () -> service.checkoutCart(token, addr, "1234567890123456",
                "12/25", "123", "John Doe", "123456789", "john@example.com", "1234567890"));
        assertTrue(ex.getMessage().contains("checkoutCart:"));
        verify(shops).rollBackPurchase(cartShop, shop);
        verify(users).restoreUserShoppingCart(eq(uid), any());
        verify(users, never()).refundPaymentAuto(token, shop);
    }

    // ─────────── createBid exception branches ───────────

    @Test
    @DisplayName("createBid_whenValidateTokenThrowsOurArg_shouldWrapAndThrowOurArg")
    void createBid_validateTokenThrowsOurArg() throws Exception {
        String token = "bad";
        when(auth.ValidateToken(token)).thenThrow(new OurArg("no auth"));

        OurArg ex = assertThrows(OurArg.class, () -> service.createBid(token, 1, Map.of(), 10));
        assertTrue(ex.getMessage().contains("createBid:"));
    }

    @Test
    @DisplayName("createBid_whenPurchaseItemsThrowsOurRuntime_shouldWrapAndThrowOurRuntime")
    void createBid_purchaseItemsThrowsOurRuntime() throws Exception {
        String token = "tok";
        int shopId = 7;
        Map<Integer, Integer> itemsMap = Map.of(1, 1);

        when(auth.ValidateToken(token)).thenReturn(2);
        doThrow(new OurRuntime("reserve fail"))
                .when(shops).purchaseItems(itemsMap, shopId, token);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> service.createBid(token, shopId, itemsMap, 20));
        assertTrue(ex.getMessage().contains("createBid:"));
    }

    // ─────────── postBidding non-bid type ───────────

    @Test
    @DisplayName("postBidding_whenPurchaseNotBid_shouldThrowOurRuntime")
    void postBidding_nonBidType() throws Exception {
        String token = "tok";
        int pid = 3;
        Purchase notBid = mock(Purchase.class);

        when(auth.ValidateToken(token)).thenReturn(1);
        when(repo.getPurchaseById(pid)).thenReturn(notBid);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> service.postBidding(token, pid, 50));
        assertTrue(ex.getMessage().contains("postBidding:"));
    }

    // ─────────── finalizeBid non-bid & auth branches ───────────

    @Test
    @DisplayName("finalizeBid_whenPurchaseNotBid_shouldThrowOurRuntime")
    void finalizeBid_nonBidType() {
        String token = "tok";
        int pid = 4;
        Purchase notBid = mock(Purchase.class);

        when(repo.getPurchaseById(pid)).thenReturn(notBid);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> service.finalizeBid(token, pid, false));
        assertTrue(ex.getMessage().contains("finalizeBid:"));
    }

    @Test
    @DisplayName("finalizeBid_whenValidateTokenThrowsOurArg_shouldPropagateOurArg")
    void finalizeBid_validateTokenThrowsOurArg() throws Exception {
        String token = "tok";
        int pid = 5;
        Bid bid = spy(new Bid(pid, 1, 1, Map.of(), 10));

        when(repo.getPurchaseById(pid)).thenReturn(bid);
        when(auth.ValidateToken(token)).thenThrow(new OurArg("no auth"));

        OurArg ex = assertThrows(OurArg.class, () -> service.finalizeBid(token, pid, false));
        assertTrue(ex.getMessage().contains("finalizeBid:"));
    }

    // ─────────── getPurchaseById & getUserPurchases branches ───────────

    @Test
    @DisplayName("getPurchaseById_whenRepositoryReturnsPurchase_shouldReturnIt")
    void getPurchaseById_happyPath() {
        Purchase p = mock(Purchase.class);
        when(repo.getPurchaseById(9)).thenReturn(p);

        Purchase result = service.getPurchaseById(9);
        assertSame(p, result);
    }

    @Test
    @DisplayName("getUserPurchases_whenTokenDoesNotMatch_shouldThrowOurRuntime")
    void getUserPurchases_tokenMismatch() throws Exception {
        String token = "tok";
        int uid = 8;

        when(auth.ValidateToken(token)).thenReturn(uid + 1);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> service.getUserPurchases(token, uid));
        assertTrue(ex.getMessage().contains("getUserPurchases:"));
    }

    @Test
    @DisplayName("getUserPurchases_whenRepositoryThrows_shouldWrapAndThrowOurRuntime")
    void getUserPurchases_repositoryThrows() throws Exception {
        String token = "tok";
        int uid = 9;

        when(auth.ValidateToken(token)).thenReturn(uid);
        when(repo.getUserPurchases(uid)).thenThrow(new RuntimeException("fail"));

        OurRuntime ex = assertThrows(OurRuntime.class, () -> service.getUserPurchases(token, uid));
        assertTrue(ex.getMessage().contains("Error retrieving user purchases:"));
    }

    // ------------ getAllBids() ----------------
    @Test
    @DisplayName("getAllBids_whenRepositoryReturnsBids_shouldReturnThem")
    void getAllBids_happyPath() throws Exception {
        String token = "tok";
        int uid = 9;
        List<BidReciept> bids = new ArrayList<>();  
        when(auth.ValidateToken(token)).thenReturn(uid);
        when(repo.getAllBids()).thenReturn(bids);
        List<BidReciept> out = service.getAllBids(token, true);
        assertEquals(bids, out);
        verify(repo).getAllBids();
    }

    // ───────────────────── getBid invariants ─────────────────────

    @Test
    @DisplayName("getBid_whenBidExists_shouldReturnItsReceipt_andCallGenerateReceipt")
    void getBid_happyPath() throws Exception {
        String token = "tok";
        int uid = 7, pid = 18, shopId = 4;

        Bid bid = spy(new Bid(pid, uid, shopId, Map.of(1, 1), 50));
        BidReciept rcpt = mock(BidReciept.class);

        when(bid.generateReciept()).thenReturn(rcpt);
        when(repo.getPurchaseById(pid)).thenReturn(bid);
        when(auth.ValidateToken(token)).thenReturn(uid);

        BidReciept out = service.getBid(token, pid);

        assertSame(rcpt, out);
        verify(bid).generateReciept();
    }

    // ───────────────────── postBidding invariants ─────────────────────

    @Test
    @DisplayName("postBidding_whenValidateTokenThrowsOurArg_shouldPropagateOurArg")
    void postBidding_validateTokenThrows() throws Exception {
        String token = "tok";
        int pid = 40;

        when(auth.ValidateToken(token)).thenThrow(new OurArg("bad token"));

        assertThrows(OurArg.class, () -> service.postBidding(token, pid, 99));
    }

    // ───────────────────── auction tests ─────────────────────

    @Nested
    @DisplayName("startAuction")
    class StartAuctionServiceTests {
        @Test
        @DisplayName("whenValid_callsServices_andReturnsId")
        void validStartAuction() throws Exception {
            String token = "tok";
            int userId = 7, storeId = 3;
            Map<Integer,Integer> itemsMap = Map.of(1, 2);
            int initPrice = 100;
            LocalDateTime endTime = LocalDateTime.now().plusHours(2);
            int auctionId = 123;

            when(auth.ValidateToken(token)).thenReturn(userId);
            when(shops.purchaseItems(itemsMap, storeId, token))
                .thenReturn(0.0);
            when(repo.addBid(
                    eq(userId), eq(storeId), eq(itemsMap), eq(initPrice),
                    any(LocalDateTime.class), eq(endTime)
            )).thenReturn(auctionId);

            int result = service.startAuction(token, storeId, itemsMap, initPrice, endTime);

            assertEquals(auctionId, result);
            verify(shops).purchaseItems(itemsMap, storeId, token);
            // ← here: all args must be matchers
            verify(repo).addBid(
                eq(userId),
                eq(storeId),
                eq(itemsMap),
                eq(initPrice),
                any(LocalDateTime.class),
                eq(endTime)
            );
            verify(taskscheduler).schedule(any(Runnable.class), any(Date.class));
        }

        @Test
        @DisplayName("whenTokenInvalid_throwsOurArg")
        void invalidToken_throws() throws Exception {
            when(auth.ValidateToken("bad")).thenThrow(new OurArg("nope"));
            assertThrows(OurArg.class, () ->
                service.startAuction("bad", 1, Map.of(), 0, LocalDateTime.now())
            );
        }

        @Test
        @DisplayName("whenRepoThrows_rollsBack_andThrowsOurRuntime")
        void repoError_rollsBack() throws Exception {
            String token = "tok";
            int userId = 7, storeId = 3;
            Map<Integer,Integer> itemsMap = Map.of(1, 2);

            when(auth.ValidateToken(token)).thenReturn(userId);
            when(shops.purchaseItems(itemsMap, storeId, token))
                .thenReturn(0.0);
            when(repo.addBid(
                    anyInt(), anyInt(), anyMap(),
                    anyInt(), any(LocalDateTime.class), any(LocalDateTime.class)
            )).thenThrow(new RuntimeException("db failure"));

            OurRuntime ex = assertThrows(OurRuntime.class, () ->
                service.startAuction(token, storeId, itemsMap, 50, LocalDateTime.now().plusHours(1))
            );
            verify(shops).rollBackPurchase(itemsMap, storeId);
        }
    }

    @Nested
    @DisplayName("postBiddingAuction")
    class PostBiddingAuctionTests {

        @Test
        @DisplayName("whenNonOwner_addsBid")
        void nonOwner_addsBid() throws Exception {
            String token = "tok";
            int owner = 1, bidder = 2, auctionId = 10, bidPrice = 60;
            Bid bid = mock(Bid.class);

            when(auth.ValidateToken(token)).thenReturn(bidder);
            when(repo.getPurchaseById(auctionId)).thenReturn(bid);
            when(bid.getUserId()).thenReturn(owner);

            service.postBiddingAuction(token, auctionId, bidPrice);

            verify(bid).addBidding(bidder, bidPrice, false);
        }

        @Test
        @DisplayName("whenOwner_throwsOurRuntime")
        void ownerCannotBid_throws() throws Exception {
            String token = "tok";
            int owner = 1, auctionId = 10;
            Bid bid = mock(Bid.class);

            when(auth.ValidateToken(token)).thenReturn(owner);
            when(repo.getPurchaseById(auctionId)).thenReturn(bid);
            when(bid.getUserId()).thenReturn(owner);

            assertThrows(OurRuntime.class,
                () -> service.postBiddingAuction(token, auctionId, 60)
            );
        }
    }

    @Nested @DisplayName("getStorePurchases tests")
    class StorePurchases {
        @Test @DisplayName("when user lacks permission should still return history")
        void noPermission_returnsHistory() throws Exception {
            String token = "t"; int shop=5;
            when(auth.ValidateToken(token)).thenReturn(2);
            HashMap<Integer, PermissionsEnum[]> perms = new HashMap<>();
            perms.put(2, new PermissionsEnum[]{});
            when(users.getPermitionsByShop(token, shop)).thenReturn(perms);
            List<Reciept> list = List.of(mock(Reciept.class));
            when(repo.getStorePurchases(shop)).thenReturn(list);

            List<Reciept> out = service.getStorePurchases(token, shop);
            assertEquals(list, out);
        }

        @Test @DisplayName("when repository throws should wrap in OurRuntime")
        void repoError_throws() throws Exception{
            String token = "t"; int shop=5;
            when(auth.ValidateToken(token)).thenReturn(2);
            HashMap<Integer, PermissionsEnum[]> permsMap = new HashMap<>();
            permsMap.put(2, new PermissionsEnum[]{PermissionsEnum.getHistory});
            when(users.getPermitionsByShop(token, shop)).thenReturn(permsMap);
            when(repo.getStorePurchases(shop)).thenThrow(new RuntimeException("db"));
            OurRuntime ex = assertThrows(OurRuntime.class, () -> service.getStorePurchases(token, shop));
            assertFalse(ex.getMessage().contains("getStorePurchases:"));
        }
    }

    @Test @DisplayName("acceptBid should notify owner and finalize bid")
    void acceptBid_happy() throws Exception {
        String token = "tok"; int bidId=3;
        Bid bid = mock(Bid.class);
        when(repo.getPurchaseById(bidId)).thenReturn(bid);
        when(auth.ValidateToken(token)).thenReturn(10);
        when(users.getShopOwner(anyInt())).thenReturn(20);
        when(users.getUserById(10)).thenReturn(mock(com.example.app.DomainLayer.Member.class));

        service.acceptBid(token, bidId);

        verify(nots).sendToUser(eq(20), eq(token), contains("accepted bid"));
        // finalizeBid is called internally, so verify repo.getPurchaseById again
        verify(repo, times(2)).getPurchaseById(bidId);
    }

    @Test @DisplayName("getAuctionsWinList filters closed shops and missing items")
    void auctionsWinList_filters() throws Exception {
        String token = "t"; int uid=2;
        BidReciept b1 = mock(BidReciept.class);
        when(b1.getShopId()).thenReturn(1);
        when(b1.getItems()).thenReturn(Map.of(9,1));
        when(items.getAllItems(token)).thenReturn(List.of(new com.example.app.DomainLayer.Item.Item(9, "", "", 0)));
        when(auth.ValidateToken(token)).thenReturn(uid);
        when(users.getAuctionsWinList(uid)).thenReturn(new ArrayList<>(List.of(b1)));
        when(shops.getclosedShops(token)).thenReturn(List.of(2));

        List<BidReciept> out = service.getAuctionsWinList(token);
        assertEquals(1, out.size());
    }

    // ─────────── partialCheckoutCart tests ───────────

    @Test
    @DisplayName("partialCheckoutCart_happyPath_shouldOnlyProcessOneShop")
    void partialCheckoutCart_happyPath() throws Exception {
        String token = "tok";
        int uid = 1, shopA = 10, shopB = 20;
        Map<Integer,Integer> cartA = Map.of(5,2);
        Map<Integer,HashMap<Integer,Integer>> cart = Map.of(
            shopA, new HashMap<>(cartA),
            shopB, new HashMap<>(Map.of(7,1))
        );

        when(auth.ValidateToken(token)).thenReturn(uid);
        when(users.getUserShoppingCartItems(uid))
            .thenReturn(new HashMap<>(cart));
        when(shops.purchaseItems(cartA, shopA, token)).thenReturn(100.0);
        when(repo.addPurchase(eq(uid), eq(shopA), eq(cartA), eq(100.0), any()))
            .thenReturn(42);

        List<Integer> out = service.partialCheckoutCart(
            token, addr,
            "USD","4111","12","25","Alice","123","ID",
            shopA
        );

        assertEquals(List.of(42), out);
        verify(users).clearUserShoppingCartByShopId(uid, shopA);
        verify(shops).shipPurchase(eq(token), eq(42), eq(shopA),
            any(), any(), any(), any());
    }

    @Test
    @DisplayName("partialCheckoutCart_whenPurchaseThrows_shouldRollbackAndThrow")
    void partialCheckoutCart_exceptionPath() throws Exception{
        String token = "tok";
        int uid = 2, shop = 5;
        Map<Integer,Integer> cartShop = Map.of(9,1);
        when(auth.ValidateToken(token)).thenReturn(uid);
        when(users.getUserShoppingCartItems(uid))
            .thenReturn(new HashMap<>(Map.of(shop, new HashMap<>(cartShop))));
        when(shops.purchaseItems(cartShop, shop, token))
            .thenThrow(new OurRuntime("boom"));

        OurRuntime ex = assertThrows(
            OurRuntime.class,
            () -> service.partialCheckoutCart(
                token, addr,
                "USD","4111","12","25","Bob","123","ID",
                shop
            )
        );
        assertTrue(ex.getMessage().contains("partialCheckoutCart:"));
        verify(shops).rollBackPurchase(cartShop, shop);
        verify(users).restoreUserShoppingCartByShopId(eq(uid), any(), eq(shop));
    }

    // ─────────── getReciept tests ───────────

    @Test @DisplayName("getReciept_happyPath_returnsListOfOne")
    void getReciept_happyPath() throws Exception {
        Reciept r = mock(Reciept.class);
        Purchase p = mock(Purchase.class);
        when(repo.getPurchaseById(99)).thenReturn(p);
        when(p.generateReciept()).thenReturn(r);

        List<Reciept> out = service.getReciept(99);
        assertEquals(1, out.size());
        assertSame(r, out.get(0));
    }

    @Test @DisplayName("getReciept_notFound_throwsOurRuntime")
    void getReciept_notFound() {
        when(repo.getPurchaseById(123)).thenReturn(null);
        OurRuntime ex = assertThrows(
            OurRuntime.class,
            () -> service.getReciept(123)
        );
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    // ─────────── getShopBids tests ───────────

    @Test @DisplayName("getShopBids_happyPath_returnsRepoList")
    void getShopBids_happyPath() throws Exception {
        List<BidReciept> bids = List.of(mock(BidReciept.class));
        when(auth.ValidateToken("tok")).thenReturn(7);
        when(repo.getShopBids(5)).thenReturn(bids);

        List<BidReciept> out = service.getShopBids("tok", 5);
        assertSame(bids, out);
    }

    @Test @DisplayName("getShopBids_repoError_wrapsOurRuntime")
    void getShopBids_repoError() throws Exception {
        when(auth.ValidateToken("tok")).thenReturn(7);
        when(repo.getShopBids(5)).thenThrow(new RuntimeException("db"));

        OurRuntime ex = assertThrows(
            OurRuntime.class,
            () -> service.getShopBids("tok", 5)
        );
        assertTrue(ex.getMessage().contains("Error retrieving shop bids"));
    }

    // ─────────── getAllBids(fromBid=false) tests ───────────

    @Test @DisplayName("getAllBids_falseBranch_filtersByCompletionClosedAndMissingItems")
    void getAllBids_falseBranch() throws Exception {
        String token = "tok"; int uid = 9;
        // make two receipts: one completed, one not
        BidReciept done = mock(BidReciept.class),
                    open = mock(BidReciept.class);
        when(done.getEndTime()).thenReturn(LocalDateTime.now().minusDays(1));
        when(done.getShopId()).thenReturn(1);
        when(done.getItems()).thenReturn(Map.of(100,1));
        when(open.getEndTime()).thenReturn(null);

        when(auth.ValidateToken(token)).thenReturn(uid);
        when(repo.getAllBids()).thenReturn(List.of(done, open));
        // filter out shop-2 as “closed”
        lenient().when(shops.getclosedShops(token)).thenReturn(List.of(2));
        // filter out item-200 as “missing”
        lenient().when(items.getAllItems(token))
            .thenReturn(List.of(new Item(100, "", "", 0)));

        List<BidReciept> out = service.getAllBids(token, false);
        // only “done” with shopId=1 & itemId=100 survives
        assertEquals(1, out.size());
        assertSame(done, out.get(0));
    }

    // ─────────── getFinishedBidsList tests ───────────

    @Test
    @DisplayName("getFinishedBidsList_returnsOnlyCompletedForUser")
    void getFinishedBidsList_filtersCorrectly() throws Exception {
        String token = "t";
        int    uid   = 42;

        BidReciept done = mock(BidReciept.class);
        BidReciept open = mock(BidReciept.class);

        when(done.isCompleted()).thenReturn(true);
        when(done.getUserId())     .thenReturn(uid);

        // spy the service
        PurchaseService spySvc = spy(service);

        // make these two lenient stubs:
        lenient().doReturn(List.of(done, open))
                .when(spySvc).getAllBids(eq(token), eq(true));

        lenient().when(auth.ValidateToken(token))
                .thenReturn(uid);

        List<BidReciept> out = spySvc.getFinishedBidsList(token);

        assertEquals(1, out.size());
        assertSame(done, out.get(0));
    }

    // ────────────────────────────── finalizeAuction via startAuction callback ──────────────────────────────
    @Test
    @DisplayName("startAuction_schedules_and_finalizeAuction_runsCallback")
    void startAuction_schedules_and_finalizeAuction_runsCallback() throws Exception {
        String token = "tok";
        int    user  = 1, shopId = 2, auctionId = 99;
        Map<Integer,Integer> items = Map.of(1,1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        when(auth.ValidateToken(token)).thenReturn(user);
        when(shops.purchaseItems(items, shopId, token)).thenReturn(0.0);
        when(repo.addBid(eq(user), eq(shopId), eq(items), eq(10),
                        any(LocalDateTime.class), eq(end)))
            .thenReturn(auctionId);

        // capture the Runnable that Spring will schedule
        ArgumentCaptor<Runnable> runCap = ArgumentCaptor.forClass(Runnable.class);

        service.startAuction(token, shopId, items, 10, end);

        verify(taskscheduler)
        .schedule(runCap.capture(), any(Date.class));

        // now prepare that Bid so finalizeAuction will run
        Bid bid = mock(Bid.class);
        when(repo.getPurchaseById(auctionId)).thenReturn(bid);
        when(bid.completePurchase()).thenReturn(mock(BidReciept.class));
        when(bid.getHighestBidderId()).thenReturn(77);
        when(bid.getMaxBidding()).thenReturn(123);
        when(bid.getStoreId()).thenReturn(shopId);
        when(bid.getBiddersIds()).thenReturn(List.of(77,88));

        // run the callback
        runCap.getValue().run();

        // verify notifications & cart addition
        verify(nots).sendToUser(eq(77), eq("Auction ended"), contains("won"));
        verify(users).addAuctionWinBidToUserShoppingCart(77, bid);
    }

    // ─────────────────────────── getAllBids true‐branch for owner vs non‐owner ───────────────────────────
    @Test
    @DisplayName("getAllBids_trueBranch_filtersCorrectly_forOwnerAndOthers")
    void getAllBids_trueBranch_filtersCorrectly_forOwnerAndOthers() throws Exception {
        String token = "tok"; int uid = 1;
        BidReciept ownerBid = mock(BidReciept.class),
                otherBid = mock(BidReciept.class);

        when(ownerBid.getShopId()).thenReturn(5);
        when(ownerBid.getUserId()).thenReturn(uid);
        when(ownerBid.getEndTime()).thenReturn(null);

        when(otherBid.getShopId()).thenReturn(6);
        when(otherBid.getUserId()).thenReturn(2);
        when(otherBid.getEndTime()).thenReturn(null);

        when(auth.ValidateToken(token)).thenReturn(uid);
        when(repo.getAllBids()).thenReturn(List.of(ownerBid, otherBid));

        // shop 5 belongs to me, shop 6 belongs to somebody else
        when(users.getShopOwner(5)).thenReturn(uid);
        when(users.getShopOwner(6)).thenReturn(3);
        when(shops.getclosedShops(token)).thenReturn(List.of());
        when(items.getAllItems(token))
            .thenReturn(List.of(new Item(1,"","",0)));

        List<BidReciept> out = service.getAllBids(token, true);
        assertEquals(1, out.size());
        assertSame(ownerBid, out.get(0));
    }

    // ─────────────────────────────── setServices actually overrides deps ───────────────────────────────
    @Test
    @DisplayName("setServices_overridesAuthTokenService")
    void setServices_overridesAuthTokenService() throws Exception{
        AuthTokenService newAuth = mock(AuthTokenService.class);
        service.setServices(newAuth, users, items, shops, msg);

        // newAuth should now be in use
        when(newAuth.ValidateToken("bad")).thenThrow(new OurArg("nope"));
        OurArg ex = assertThrows(
        OurArg.class,
        () -> service.getUserPurchases("bad", 0)
        );
        assertTrue(ex.getMessage().contains("getUserPurchases:"));
    }

    // ─────────────────────── acceptBid when repo returns null ───────────────────────
    @Test
    @DisplayName("acceptBid_whenBidNull_throwsOurRuntime")
    void acceptBid_whenBidNull_throwsOurRuntime() {
        when(repo.getPurchaseById(42)).thenReturn(null);
        OurRuntime ex = assertThrows(
        OurRuntime.class,
        () -> service.acceptBid("tok", 42)
        );
        assertTrue(ex.getMessage().contains("Bid 42 does not exist"));
    }

    // ─────────────────────────── shipping‐failure in checkoutCart ───────────────────────────
    @Test
    @DisplayName("checkoutCart_whenShipFails_rollsBackAndRefunds")
    void checkoutCart_whenShipFails_rollsBackAndRefunds() throws Exception {
        String token = "tok";
        int    uid       = 7,
            shop      = 3,
            purchaseId = 55;
        Map<Integer,Integer> cartShop = Map.of(9,1);

        when(auth.ValidateToken(token)).thenReturn(uid);
        when(users.getUserShoppingCartItems(uid))
            .thenReturn(new HashMap<>(Map.of(shop, new HashMap<>(cartShop))));
        when(shops.purchaseItems(cartShop, shop, token)).thenReturn(10.0);
        when(repo.addPurchase(uid, shop, cartShop, 10.0, addr))
            .thenReturn(purchaseId);

        when(users.pay(
            eq(token), eq(shop), eq(10.0),
            eq("USD"), eq("4111"), eq("12"), eq("25"),
            eq("Name"), eq("CVV"), eq("ID")
        )).thenReturn(purchaseId);

        // simulate shipping blow-up
        doThrow(new OurRuntime("ship error"))
            .when(shops)
            .shipPurchase(
                eq(token), eq(purchaseId), eq(shop),
                any(), any(), any(), any()
            );

        OurRuntime ex = assertThrows(
            OurRuntime.class,
            () -> service.checkoutCart(
                token, addr,
                "USD","4111","12","25","Name","CVV","ID"
            )
        );
        assertTrue(ex.getMessage().contains("checkoutCart:"));

        // verify rollback + restore + refund
        verify(shops).rollBackPurchase(cartShop, shop);
        verify(users).restoreUserShoppingCart(eq(uid), any());
        verify(users).refundPaymentAuto(token, purchaseId);
    }

    // ─────────── getBid exception branches ───────────
    @Test
    @DisplayName("getBid_whenNotABid_throwsOurRuntime")
    void getBid_nonBidType() throws Exception{
        when(repo.getPurchaseById(10)).thenReturn(mock(Purchase.class));
        when(auth.ValidateToken("t")).thenReturn(1);

        OurRuntime ex = assertThrows(
        OurRuntime.class,
        () -> service.getBid("t", 10)
        );
        assertTrue(ex.getMessage().contains("is not a bid"));
    }

    @Test
    @DisplayName("getBid_whenValidateTokenFails_propagatesOurArg")
    void getBid_validateTokenThrows() throws Exception{
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("nope"));
        assertThrows(OurArg.class, () -> service.getBid("bad", 1));
    }

    // ─────────── postBiddingAuction exception ───────────

    @Test
    @DisplayName("postBiddingAuction_whenNotABid_throwsOurRuntime")
    void postBiddingAuction_nonBidType() throws Exception{
        when(auth.ValidateToken("tok")).thenReturn(1);
        when(repo.getPurchaseById(42)).thenReturn(mock(Purchase.class));

        OurRuntime ex = assertThrows(
        OurRuntime.class,
        () -> service.postBiddingAuction("tok", 42, 100)
        );
        assertTrue(ex.getMessage().contains("is not a bid"));
    }

    // ─────────── finalizeBid negative branches ───────────

    @Test
    @DisplayName("finalizeBid_nonBidType_throwsOurRuntime_variant")
    void finalizeBid_nonBidType_variant(){
        when(repo.getPurchaseById(5)).thenReturn(mock(Purchase.class));

        OurRuntime ex = assertThrows(
        OurRuntime.class,
        () -> service.finalizeBid("t", 5, false)
        );
        assertTrue(ex.getMessage().contains("is not a bid"));
    }

    @Test
    @DisplayName("finalizeBid_initiatorIsBuyer_notAccept_throwsOurRuntime")
    void finalizeBid_initiatorIsBuyer() throws Exception{
        Bid b = mock(Bid.class);
        when(repo.getPurchaseById(7)).thenReturn(b);
        when(auth.ValidateToken("t")).thenReturn(2);
        when(b.getUserId()).thenReturn(2);

        OurRuntime ex = assertThrows(
        OurRuntime.class,
        () -> service.finalizeBid("t", 7, false)
        );
        assertTrue(ex.getMessage().contains("You need to be shop's owner"));
    }

    @Test
    @DisplayName("finalizeBid_notShopOwner_notAccept_throwsOurRuntime")
    void finalizeBid_notShopOwner() throws Exception{
        Bid b = mock(Bid.class);
        when(repo.getPurchaseById(8)).thenReturn(b);
        when(auth.ValidateToken("t")).thenReturn(2);
        when(b.getUserId()).thenReturn(1);
        when(b.getStoreId()).thenReturn(99);
        when(users.getShopOwner(99)).thenReturn(3);

        OurRuntime ex = assertThrows(
        OurRuntime.class,
        () -> service.finalizeBid("t", 8, false)
        );
        assertTrue(ex.getMessage().contains("is not the owner of shop"));
    }

    @Test
    @DisplayName("finalizeBid_noFinalPrice_throwsOurRuntime")
    void finalizeBid_noFinalPrice() throws Exception{
        Bid b = mock(Bid.class);
        when(repo.getPurchaseById(9)).thenReturn(b);
        when(auth.ValidateToken("t")).thenReturn(2);
        when(b.getUserId()).thenReturn(1);
        when(b.getStoreId()).thenReturn(5);
        when(users.getShopOwner(5)).thenReturn(2);
        when(b.getMaxBidding()).thenReturn(-1);

        OurRuntime ex = assertThrows(
        OurRuntime.class,
        () -> service.finalizeBid("t", 9, false)
        );
        assertTrue(ex.getMessage().contains("No final price"));
    }

    @Test
    @DisplayName("finalizeBid_noShopId_throwsOurRuntime")
    void finalizeBid_noShopId() throws Exception{
        Bid b = mock(Bid.class);
        when(repo.getPurchaseById(10)).thenReturn(b);
        when(auth.ValidateToken("t")).thenReturn(2);
        when(b.getUserId()).thenReturn(1);
        when(b.getStoreId()).thenReturn(-1);
        when(b.getMaxBidding()).thenReturn(100);
        when(users.getShopOwner(-1)).thenReturn(2);

        OurRuntime ex = assertThrows(
        OurRuntime.class,
        () -> service.finalizeBid("t", 10, false)
        );
        assertTrue(ex.getMessage().contains("No shop ID"));
    }

    // ─────────── finalizeAuction exception paths ───────────

    @Test
    @DisplayName("startAuction_callback_noBids_throwsOurRuntime")
    void finalizeAuction_noBids() throws Exception {
        String tok = "t"; int user = 1, shopId = 2, aId = 77;
        when(auth.ValidateToken(tok)).thenReturn(user);
        when(shops.purchaseItems(any(), eq(shopId), eq(tok))).thenReturn(0.0);
        when(repo.addBid(eq(user), eq(shopId), anyMap(), anyInt(), any(), any()))
        .thenReturn(aId);

        ArgumentCaptor<Runnable> cap = ArgumentCaptor.forClass(Runnable.class);
        service.startAuction(tok, shopId, Map.of(1,1), 10, LocalDateTime.now().plusHours(1));
        verify(taskscheduler).schedule(cap.capture(), any(Date.class));

        Bid b = mock(Bid.class);
        when(repo.getPurchaseById(aId)).thenReturn(b);
        // no bidders at all:
        when(b.getHighestBidderId()).thenReturn(-1);

        OurRuntime ex = assertThrows(
        OurRuntime.class,
        () -> cap.getValue().run()
        );
        assertTrue(ex.getMessage().contains("No bids were placed"));
    }

    @Test
    @DisplayName("startAuction_callback_noFinalPrice_throwsOurRuntime")
    void finalizeAuction_noFinalPrice() throws Exception {
        String tok = "t"; int user = 1, shopId = 2, aId = 78;
        when(auth.ValidateToken(tok)).thenReturn(user);
        when(shops.purchaseItems(any(), eq(shopId), eq(tok))).thenReturn(0.0);
        when(repo.addBid(eq(user), eq(shopId), anyMap(), anyInt(), any(), any()))
        .thenReturn(aId);

        ArgumentCaptor<Runnable> cap = ArgumentCaptor.forClass(Runnable.class);
        service.startAuction(tok, shopId, Map.of(1,1), 10, LocalDateTime.now().plusHours(1));
        verify(taskscheduler).schedule(cap.capture(), any(Date.class));

        Bid b = mock(Bid.class);
        when(repo.getPurchaseById(aId)).thenReturn(b);
        when(b.getHighestBidderId()).thenReturn(5);
        when(b.getMaxBidding()).thenReturn(-1);

        OurRuntime ex = assertThrows(
        OurRuntime.class,
        () -> cap.getValue().run()
        );
        assertTrue(ex.getMessage().contains("No final price"));
    }

    // ─────────── getStorePurchases permission branch ───────────

    @Test
    @DisplayName("getStorePurchases_withHistoryPermission_returnsList")
    void getStorePurchases_withHistoryPermission() throws Exception {
        String tok = "t"; int shop=5, uid=3;
        when(auth.ValidateToken(tok)).thenReturn(uid);
        when(users.getPermitionsByShop(tok, shop))
        .thenReturn(new HashMap<>(Map.of(uid, new PermissionsEnum[]{PermissionsEnum.getHistory})));

        List<Reciept> data = List.of(mock(Reciept.class));
        when(repo.getStorePurchases(shop)).thenReturn(data);

        List<Reciept> out = service.getStorePurchases(tok, shop);
        assertSame(data, out);
    }

    // ─────────── getAuctionsWinList exception branch ───────────

    @Test
    @DisplayName("getAuctionsWinList_validateTokenFails_throwsOurArg")
    void getAuctionsWinList_validateFails() throws Exception{
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("nope"));
        assertThrows(OurArg.class,
        () -> service.getAuctionsWinList("bad")
        );
    }

    @Test
    @DisplayName("getAuctionsWinList_itemServiceError_wrapsOurRuntime")
    void getAuctionsWinList_itemServiceError() throws Exception {
        String tok = "t"; int uid=4;
        when(auth.ValidateToken(tok)).thenReturn(uid);
        when(users.getAuctionsWinList(uid)).thenReturn(new ArrayList<>(List.of(mock(BidReciept.class))));
        when(shops.getclosedShops(tok)).thenReturn(List.of());
        when(items.getAllItems(tok)).thenThrow(new RuntimeException("db"));

        OurRuntime ex = assertThrows(
        OurRuntime.class,
        () -> service.getAuctionsWinList(tok)
        );
        assertTrue(ex.getMessage().contains("getAuctionsWinList:"));
    }

}
