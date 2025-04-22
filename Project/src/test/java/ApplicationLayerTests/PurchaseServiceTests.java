package ApplicationLayerTests;

import static org.assertj.core.api.Assertions.*;      // AssertJ
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;      // JUnit assertions

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ApplicationLayer.Purchase.PurchaseService;
import ApplicationLayer.AuthTokenService;
import ApplicationLayer.User.UserService;
import ApplicationLayer.Item.ItemService;
import ApplicationLayer.Shop.ShopService;
import ApplicationLayer.Message.MessageService;
import DomainLayer.Purchase.*;

/**
 * Acceptance‑level tests for {@link PurchaseService} that cover all success & fault
 * scenarios required by the 2025 Market System use‑cases (UC8, UC14, UC8.5 ...).
 *
 * <p>All collaborators are mocked – the tests exercise end‑to‑end behaviour of
 * <strong>PurchaseService</strong> itself while treating surrounding layers as external
 * systems.</p>
 */
@ExtendWith(MockitoExtension.class)
class PurchaseServiceTests {

    // ==== mocks ====        
    @Mock private IPurchaseRepository purchaseRepo;
    @Mock private AuthTokenService auth;
    @Mock private UserService users;
    @Mock private ItemService items;
    @Mock private ShopService shops;
    @Mock private MessageService msg;

    // class under test
    private PurchaseService service;

    // common test data
    private final String token   = "validToken";
    private final int    userId  = 111;
    private final int    shopId  = 10;
    private final int    itemId  = 99;
    private final Address address =
        new Address().withCountry("IL").withCity("BGU");

    @BeforeEach
    void init() {
        service = new PurchaseService(purchaseRepo);
        service.setServices(auth, users, items, shops, msg);
    }

    // ---------------------------------------------------------------------
    // UC‑8  checkout cart
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("checkoutCart()")
    class CheckoutCart {
        @Test
        @DisplayName("happy path – items available, cart cleared, purchase IDs returned")
        void checkout_success() throws Exception {
            Map<Integer,Integer> shopCart = Map.of(itemId, 2);
            Map<Integer,HashMap<Integer,Integer>> cart = new HashMap<>();
            cart.put(shopId, new HashMap<>(shopCart));

            when(auth.ValidateToken(token)).thenReturn(userId);
            when(users.getUserShoppingCartItems(userId))
                .thenReturn(new HashMap<>(cart));
            when(shops.checkSupplyAvailabilityAndAcquire(shopId, itemId, 2))
                .thenReturn(true);
            when(purchaseRepo.addPurchase(eq(userId), eq(shopId), anyMap(), eq(address)))
                .thenReturn(555);

            List<Integer> result = service.checkoutCart(token, userId, address);

            assertEquals(List.of(555), result);
            verify(users).clearUserShoppingCart(userId);
            verify(purchaseRepo).addPurchase(userId, shopId, cart.get(shopId), address);
        }

        @Test
        @DisplayName("failure – item not available => cart restored, exception raised")
        void checkout_itemNotAvailable() throws Exception {
            Map<Integer,Integer> shopCart = Map.of(itemId, 2);
            HashMap<Integer,HashMap<Integer,Integer>> cart = new HashMap<>();
            cart.put(shopId, new HashMap<>(shopCart));

            when(auth.ValidateToken(token)).thenReturn(userId);
            when(users.getUserShoppingCartItems(userId))
                .thenReturn(new HashMap<>(cart));
            when(shops.checkSupplyAvailabilityAndAcquire(shopId, itemId, 2))
                .thenReturn(false);

            RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.checkoutCart(token, userId, address)
            );
            assertTrue(ex.getMessage().contains("not available"));

            verify(users).restoreUserShoppingCart(userId, cart);
            verifyNoInteractions(purchaseRepo);
        }
    }

    // ---------------------------------------------------------------------
    // UC‑14  create bid
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("createBid()")
    class CreateBid {
        @Test
        @DisplayName("happy path – bid created")
        void createBid_success() throws Exception {
            Map<Integer,Integer> itemsMap = Map.of(itemId, 1);
            when(auth.ValidateToken(token)).thenReturn(userId);
            when(shops.checkSupplyAvailabilityAndAcquire(shopId, itemId, 1))
                .thenReturn(true);
            when(purchaseRepo.addBid(userId, shopId, itemsMap)).thenReturn(77);

            int id = service.createBid(token, userId, shopId, itemsMap);
            assertEquals(77, id);
            verify(purchaseRepo).addBid(userId, shopId, itemsMap);
        }

        @Test
        @DisplayName("failure – supply missing => exception raised")
        void createBid_itemMissing() throws Exception {
            Map<Integer,Integer> itemsMap = Map.of(itemId, 1);
            when(auth.ValidateToken(token)).thenReturn(userId);
            when(shops.checkSupplyAvailabilityAndAcquire(shopId, itemId, 1))
                .thenReturn(false);

            RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.createBid(token, userId, shopId, itemsMap)
            );
            assertTrue(ex.getMessage().contains("not available"));
            verifyNoInteractions(purchaseRepo);
        }
    }

    // ---------------------------------------------------------------------
    // postBidding
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("postBidding() – happy path")
    void postBidding_success() throws Exception {
        Bid bid = new Bid(50, userId+1, shopId, Map.of(itemId,1));
        when(auth.ValidateToken(token)).thenReturn(userId);
        when(purchaseRepo.getPurchaseById(50)).thenReturn(bid);

        service.postBidding(token, userId, 50, 120.0);

        assertEquals(120.0, bid.getBidding(userId));
    }

    // ---------------------------------------------------------------------
    // finalizeBid – happy path & edge cases
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("finalizeBid()")
    class FinalizeBid {
        private Bid ownerBid;

        @BeforeEach
        void setUpBid() {
            ownerBid = new Bid(80, userId, shopId, Map.of(itemId,1));
        }

        @Test
        @DisplayName("happy path – highest bidder wins & notifications sent")
        void finalizeBid_success() throws Exception {
            ownerBid.addBidding(222, 50);
            ownerBid.addBidding(333, 75);

            when(auth.ValidateToken(token)).thenReturn(userId);
            when(purchaseRepo.getPurchaseById(80)).thenReturn(ownerBid);

            int winner = service.finalizeBid(token, userId, 80);
            assertEquals(333, winner);

            verify(msg).sendMessageToUser(token, 222,
                "Bid 80 has been finalized. you did'nt win", 0);
            verify(msg).sendMessageToUser(token, 333,
                "Congratulations! You have won the bid 80!", 0);
        }

        @Test
        @DisplayName("failure – no bidders => returns -1")
        void finalizeBid_noBidders() throws Exception {
            when(auth.ValidateToken(token)).thenReturn(userId);
            when(purchaseRepo.getPurchaseById(80)).thenReturn(ownerBid);

            int winner = service.finalizeBid(token, userId, 80);
            assertEquals(-1, winner);
        }
    }

    // ---------------------------------------------------------------------
    // simple pass‑throughs
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("getPurchaseById() delegates to repo")
    void getPurchaseById_delegatesToRepo() {
        Purchase p = mock(Purchase.class);
        when(purchaseRepo.getPurchaseById(1)).thenReturn(p);
        assertSame(p, service.getPurchaseById(1));
    }

    @Test
    @DisplayName("getUserPurchases() with valid token")
    void getUserPurchases_validToken() throws Exception {
        ArrayList<Purchase> list = new ArrayList<>();
        list.add(mock(Purchase.class));

        when(auth.ValidateToken(token)).thenReturn(userId);
        when(purchaseRepo.getUserPurchases(userId)).thenReturn(list);

        assertSame(list, service.getUserPurchases(token, userId));
    }
}