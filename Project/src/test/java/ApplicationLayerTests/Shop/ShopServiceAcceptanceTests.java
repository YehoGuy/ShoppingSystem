package ApplicationLayerTests.Shop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.Item.ItemService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.ShippingMethod;
import com.example.app.ApplicationLayer.Shop.ShopService;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Item.Item;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Item.ItemReview;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Shop.IShopRepository;
import com.example.app.DomainLayer.Shop.Operator;
import com.example.app.DomainLayer.Shop.PurchasePolicy;
import com.example.app.DomainLayer.Shop.Shop;
import com.example.app.DomainLayer.Shop.ShopReview;
import com.example.app.InfrastructureLayer.ShopRepository;

class ShopServiceAcceptanceTests {

    @Mock
    private IShopRepository shopRepository;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private ItemService itemService;

    @Mock
    private UserService userService;

    @Mock
    private ShippingMethod shippingMethod;

    @Mock
    private PurchasePolicy purchasePolicy;


    @InjectMocks
    private ShopService shopService;


    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        // initialize @Mock fields
        mocks = MockitoAnnotations.openMocks(this);

        // manually construct with your two-arg constructor
        shopService = new ShopService(shopRepository, authTokenService, userService, itemService);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
        clearInvocations(shopRepository, authTokenService, itemService, userService);
    }

    // UC5 – Search Products in Market
    @Test
    void testSearchItemsInShop_FullSearch_Success() throws Exception {
        String token = "valid-token";
        int shopId = 1;
        Item item = new Item(42, "Widget", "A fine widget", 0);

        when(authTokenService.ValidateToken(token)).thenReturn(10);
        when(shopRepository.getShop(shopId)).thenReturn(new Shop(shopId, "ShopA", shippingMethod));
        when(shopRepository.getItemsByShop(shopId)).thenReturn(Arrays.asList(item.getId()));
        when(itemService.getItemsByIds(Arrays.asList(item.getId()),token))
            .thenReturn(Arrays.asList(item));

        List<Item> results = shopService.searchItemsInShop(shopId, "wid", ItemCategory.ELECTRONICS, null, 0, 1000000000, null, token);
        assertEquals(1, results.size());
        assertEquals(item, results.get(0));
    }

    // UC5 – Search Products in Market (no matches)
    @Test
    void testSearchItemsInShop_NoMatches_Failure() throws Exception {
        String token = "valid-token";
        int shopId = 1;

        when(authTokenService.ValidateToken(token)).thenReturn(10);
        when(shopRepository.getShop(shopId)).thenReturn(new Shop(shopId, "ShopA", shippingMethod));
        when(shopRepository.getItemsByShop(shopId)).thenReturn(Collections.emptyList());

        List<Item> results = shopService.searchItemsInShop(((Integer)shopId), "nonexistent", ItemCategory.AUTOMOTIVE, new ArrayList<>(), 0, 1000000000, 0.0, token);
        assertTrue(results.isEmpty());
    }

    // UC10 – Create Shop
    @Test
    void testCreateShop_Success() throws Exception {
        String token = "t";
        Shop newShop = new Shop(0, "MyShop", shippingMethod);

        when(authTokenService.ValidateToken(token)).thenReturn(1);
        doNothing().when(userService).validateMemberId(1);
        when(shopRepository.createShop("MyShop", purchasePolicy, shippingMethod)).thenReturn(newShop);

        Shop created = shopService.createShop("MyShop",purchasePolicy, shippingMethod, token);
        assertEquals(newShop, created);
    }

    // UC10 – Create Shop (name taken)
    @Test
    void testCreateShop_NameAlreadyTaken_Failure() throws Exception {
        String token = "t";

        when(authTokenService.ValidateToken(token)).thenReturn(1);
        doNothing().when(userService).validateMemberId(1);
        when(shopRepository.createShop(any(), any(), any()))
            .thenThrow(new RuntimeException("Shop name is already taken"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.createShop("dup", purchasePolicy, shippingMethod, token));
        assertTrue(ex.getMessage().contains("Shop name is already taken"));
    }

    // UC10 – Create Shop (missing details)
    @Test
    void testCreateShop_MissingDetails_Failure() throws Exception {
        String token       = "t";
        int userId         = 1;
        String emptyName   = "";

        // stub token validation and member check
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        doNothing().when(userService).validateMemberId(userId);

        // simulate repository rejecting missing name
        when(shopRepository.createShop(emptyName, purchasePolicy, shippingMethod))
            .thenThrow(new IllegalArgumentException("Shop name is required"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            shopService.createShop(emptyName, purchasePolicy, shippingMethod, token)
        );
        assertTrue(ex.getMessage().contains("Shop name is required"));
    }


    // UC11 – Rate Shop
    @Test
    void testAddReviewToShop_Success() throws Exception {
        String token = "t";
        int shopId = 2;
        int rating = 4;
        String text = "Great!";

        when(authTokenService.ValidateToken(token)).thenReturn(7);
        doNothing().when(userService).validateMemberId(7);
        doNothing().when(shopRepository).addReviewToShop(shopId, 7, rating, text);

        shopService.addReviewToShop(shopId, rating, text, token);
        verify(shopRepository).addReviewToShop(shopId, 7, rating, text);
    }

    // UC11 – Rate Shop (invalid rating)
    @Test
    void testAddReviewToShop_InvalidToken_Failure() throws Exception {
        String token = "bad";

        when(authTokenService.ValidateToken(token)).thenThrow(new RuntimeException("Invalid token"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addReviewToShop(1, 5, "X", token));
        assertTrue(ex.getMessage().contains("Invalid token"));
    }

    // UC16 – Add Product to Shop
    @Test
    void testAddItemToShop_Success() throws Exception {
        String token = "tok";
        int shopId = 3, itemId = 0, qty = 5, price = 100;

        when(authTokenService.ValidateToken(token)).thenReturn(8);
        when(userService.hasPermission(8, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        doNothing().when(shopRepository).addItemToShop(shopId, itemId, qty, price);

        shopService.addItemToShop(shopId, "item1", "no description", qty,ItemCategory.ELECTRONICS, price, token);
        verify(shopRepository).addItemToShop(shopId, itemId, qty, price);
    }

    // UC16 – Add Product to Shop (invalid quantity)
    @Test
    void testAddItemToShop_InvalidQuantity_Failure() throws Exception {
        String token = "tok";
        int shopId = 3, itemId = 0;

        when(authTokenService.ValidateToken(token)).thenReturn(8);
        when(userService.hasPermission(8, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Quantity must be positive"))
            .when(shopRepository).addItemToShop(shopId, itemId, -1, 5);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addItemToShop(shopId, "item1", "no description", -1,ItemCategory.ELECTRONICS, 5, token));
        assertTrue(ex.getMessage().contains("Error adding item"));
    }

    // UC16 – Add Product to Shop (no permission)
    @Test
    void testAddItemToShop_NoPermission_Failure() throws Exception {
        String token = "tok";
        int shopId = 3;

        when(authTokenService.ValidateToken(token)).thenReturn(8);
        when(userService.hasPermission(8, PermissionsEnum.manageItems, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addItemToShop(shopId, "item1", "no description", 5,ItemCategory.ELECTRONICS, 100, token));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // UC16 – Add Product to Shop (negative price)
    @Test
    void testAddItemToShop_NegativePrice_Failure() throws Exception {
        String token = "tok";
        int shopId = 3, itemId = 0;

        when(authTokenService.ValidateToken(token)).thenReturn(8);
        when(userService.hasPermission(8, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Price must be non-negative"))
            .when(shopRepository).addItemToShop(shopId, itemId, 5, -100);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addItemToShop(shopId, "item1", "no description", 5,ItemCategory.ELECTRONICS, -100, token));
        assertTrue(ex.getMessage().contains("Error adding item"));
    }

    // UC17 – Remove Product from Shop
    @Test
    void testRemoveItemFromShop_Success() throws Exception {
        String token = "tk";
        int shopId = 4, itemId = 20;

        when(authTokenService.ValidateToken(token)).thenReturn(9);
        when(userService.hasPermission(9, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        doNothing().when(shopRepository).removeItemFromShop(shopId, itemId);

        shopService.removeItemFromShop(shopId, itemId, token);
        verify(shopRepository).removeItemFromShop(shopId, itemId);
    }

    // UC17 – Remove Product from Shop (item not found)
    @Test
    void testRemoveItemFromShop_ItemNotFound_Failure() throws Exception {
        String token = "tk";
        int shopId = 4, itemId = 20;

        when(authTokenService.ValidateToken(token)).thenReturn(9);
        when(userService.hasPermission(9, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Item does not exist"))
            .when(shopRepository).removeItemFromShop(shopId, itemId);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.removeItemFromShop(shopId, itemId, token));
        assertTrue(ex.getMessage().contains("Error removing item"));
    }

    // UC17 – Remove Product from Shop (no permission)
    @Test
    void testRemoveItemFromShop_NoPermission_Failure() throws Exception {
        String token = "tk";
        int shopId = 4;

        when(authTokenService.ValidateToken(token)).thenReturn(9);
        when(userService.hasPermission(9, PermissionsEnum.manageItems, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.removeItemFromShop(shopId, 1, token));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // UC18 – Update Product Details
    @Test
    void testUpdateItemPriceInShop_Success() throws Exception {
        String token = "t";
        int shopId = 5, itemId = 30, price = 200;

        when(authTokenService.ValidateToken(token)).thenReturn(11);
        when(userService.hasPermission(11, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        doNothing().when(shopRepository).updateItemPriceInShop(shopId, itemId, price);

        shopService.updateItemPriceInShop(shopId, itemId, price, token);
        verify(shopRepository).updateItemPriceInShop(shopId, itemId, price);
    }

    // UC18 – Update Product Details (invalid price)
    @Test
    void testUpdateItemPriceInShop_NegativePrice_Failure() throws Exception {
        String token = "t";
        int shopId = 5;

        when(authTokenService.ValidateToken(token)).thenReturn(11);
        when(userService.hasPermission(11, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Price must be non-negative"))
            .when(shopRepository).updateItemPriceInShop(shopId, 1, -10);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.updateItemPriceInShop(shopId, 1, -10, token));
        assertTrue(ex.getMessage().contains("Error updating price"));
    }

    // UC18 – Update Product Details (no permission)
    @Test
    void testUpdateItemPriceInShop_NoPermission_Failure() throws Exception {
        String token = "t";
        int shopId = 5;

        when(authTokenService.ValidateToken(token)).thenReturn(11);
        when(userService.hasPermission(11, PermissionsEnum.manageItems, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.updateItemPriceInShop(shopId, 1, 10, token));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    @Test
    void testAddSupplyToItem_Success() throws Exception {
        String token = "t";
        int shopId = 6, itemId = 40, qty = 5;

        when(authTokenService.ValidateToken(token)).thenReturn(12);
        when(userService.hasPermission(anyInt(), eq(PermissionsEnum.manageItems), eq(shopId))).thenReturn(true);        
        doNothing().when(shopRepository).addSupplyToItem(shopId, itemId, qty);
        shopService.addSupplyToItem(shopId, itemId, qty, token);
        verify(shopRepository).addSupplyToItem(shopId, itemId, qty);
    }

    @Test
    void testAddSupplyToItem_Failure() throws Exception {
        String token = "t";
        int shopId = 6;

        when(authTokenService.ValidateToken(token)).thenReturn(12);
        doThrow(new RuntimeException("Supply failed"))
            .when(shopRepository).addSupplyToItem(shopId, 1, 1);

        assertThrows(RuntimeException.class,
            () -> shopService.addSupplyToItem(shopId, 1, 1, token));

    }

    // UC19 – Define Purchase/Discount Policy
    @Test
    void testUpdatePurchasePolicy_Success() throws Exception {
        String token = "t";
        int shopId = 7;
        when(authTokenService.ValidateToken(token)).thenReturn(13);
        when(userService.hasPermission(13, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).updatePurchasePolicy(shopId, purchasePolicy);

        shopService.updatePurchasePolicy(shopId, purchasePolicy, token);
        verify(shopRepository).updatePurchasePolicy(shopId, purchasePolicy);
    }

    // UC19 – Define Purchase/Discount Policy (invalid policy)
    @Test
    void testUpdatePurchasePolicy_InvalidPolicy_Failure() throws Exception {
        String token = "t";
        int shopId = 7;

        when(authTokenService.ValidateToken(token)).thenReturn(13);
        when(userService.hasPermission(13, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Invalid policy"))
            .when(shopRepository).updatePurchasePolicy(shopId, purchasePolicy);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.updatePurchasePolicy(shopId, purchasePolicy, token));
        assertTrue(ex.getMessage().contains("Error updating purchase policy"));
    }

    // UC19 – Define Purchase/Discount Policy (no permission)
    @Test
    void testUpdatePurchasePolicy_NoPermission_Failure() throws Exception {
        String token = "t";
        int shopId = 7;

        when(authTokenService.ValidateToken(token)).thenReturn(13);
        when(userService.hasPermission(13, PermissionsEnum.setPolicy, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.updatePurchasePolicy(shopId, purchasePolicy, token));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // UC19 – Define Discount  
    @Test
    void testSetGlobalDiscount_Success() throws Exception {
        String token = "t";
        int shopId = 8, discount = 25;

        when(authTokenService.ValidateToken(token)).thenReturn(14);
        when(userService.hasPermission(14, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).setGlobalDiscount(shopId, discount, true);

        shopService.setGlobalDiscount(shopId, discount, true, token);
        verify(shopRepository).setGlobalDiscount(shopId, discount, true);
    }

    // UC19 – Define Discount (invalid discount)
    @Test
    void testSetGlobalDiscount_InvalidDiscount_Failure() throws Exception {
        String token = "t";
        int shopId = 8;

        when(authTokenService.ValidateToken(token)).thenReturn(14);
        when(userService.hasPermission(14, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Discount must be <=100"))
            .when(shopRepository).setGlobalDiscount(shopId, 200, true);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.setGlobalDiscount(shopId, 200, true, token));
        assertTrue(ex.getMessage().contains("Error setting global discount"));
    }

    // UC19 – Define Discount (no permission)
    @Test
    void testSetGlobalDiscount_NoPermission_Failure() throws Exception {
        String token = "t";
        int shopId = 8;

        when(authTokenService.ValidateToken(token)).thenReturn(14);
        when(userService.hasPermission(14, PermissionsEnum.setPolicy, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.setGlobalDiscount(shopId, 10, true, token));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // UC19 - Define single discount
    @Test
    void testSetDiscountForItem_Success() throws Exception {
        String token = "t";
        int shopId = 8, itemId = 1, discount = 20;

        when(authTokenService.ValidateToken(token)).thenReturn(14);
        when(userService.hasPermission(14, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).setDiscountForItem(shopId, itemId, discount, true);

        shopService.setDiscountForItem(shopId, itemId, discount, true, token);
        verify(shopRepository).setDiscountForItem(shopId, itemId, discount, true);
    }

    // UC19 - Define single discount (invalid discount)
    @Test
    void testSetDiscountForItem_InvalidDiscount_Failure() throws Exception {
        String token = "t";
        int shopId = 8, itemId = 1;

        when(authTokenService.ValidateToken(token)).thenReturn(14);
        when(userService.hasPermission(14, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Discount must be <=100"))
            .when(shopRepository).setDiscountForItem(shopId, itemId, 200, true);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.setDiscountForItem(shopId, itemId, 200, true, token));
        assertTrue(ex.getMessage().contains("Error setting discount for item"));
    }

    // UC19 - Define single discount (no permission)
    @Test
    void testSetDiscountForItem_NoPermission_Failure() throws Exception {
        String token = "t";
        int shopId = 8, itemId = 1;

        when(authTokenService.ValidateToken(token)).thenReturn(14);
        when(userService.hasPermission(14, PermissionsEnum.setPolicy, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.setDiscountForItem(shopId, itemId, 10, true, token));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // UC24 – Close Shop
    @Test
    void testCloseShop_Success() throws Exception {
        String token = "t";
        int shopId = 9;

        when(authTokenService.ValidateToken(token)).thenReturn(15);
        when(userService.hasPermission(15, PermissionsEnum.closeShop, shopId)).thenReturn(true);
        doNothing().when(shopRepository).closeShop(shopId);

        shopService.closeShop(shopId, token);
        verify(shopRepository).closeShop(shopId);
    }

    // UC24 – Close Shop (already closed)
    @Test
    void testCloseShop_AlreadyClosed_Failure() throws Exception {
        String token = "t";
        int shopId = 9;

        when(authTokenService.ValidateToken(token)).thenReturn(15);
        when(userService.hasPermission(15, PermissionsEnum.closeShop, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Shop already closed"))
            .when(shopRepository).closeShop(shopId);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.closeShop(shopId, token));
        assertTrue(ex.getMessage().contains("Error closing shop"));
    }

    // UC24 – Close Shop (no permission)
    @Test
    void testCloseShop_NoPermission_Failure() throws Exception {
        String token = "t";
        int shopId = 9;

        when(authTokenService.ValidateToken(token)).thenReturn(15);
        when(userService.hasPermission(15, PermissionsEnum.closeShop, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.closeShop(shopId, token));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // UC28 – Enforce Actions by Permission
    void testActionByPermission_Succeess() throws Exception{
        //TODO
    }

    // UC28 – Enforce Actions by Permission (sample)
    @Test
    void testActionByPermission_Denied_Failure() throws Exception {
        String token = "t";
        int shopId = 10;

        when(authTokenService.ValidateToken(token)).thenReturn(16);
        when(userService.hasPermission(16, PermissionsEnum.manageItems, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addItemToShop(shopId, "item1", "no description", 5,ItemCategory.ELECTRONICS, 100, token));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // Search/filter edge cases in ShopService (success)
    // Verifies search with no filters and with price filter yields correct results.
    @Test
    void testSearchFilterEdgeCasesInShopService_Success() throws Exception {
        String token = "t";
        when(authTokenService.ValidateToken(token)).thenReturn(1);

        // Prepare a single shop with one item
        ShippingMethod sm = mock(ShippingMethod.class);
        Shop shopObj = new Shop(1, "S", sm);
        when(shopRepository.getAllShops()).thenReturn(List.of(shopObj));
        when(shopRepository.getShop(1)).thenReturn(shopObj);
        when(shopRepository.getItemsByShop(1)).thenReturn(List.of(10));

        // Stub item service to return one item priced at 0
        Item it = new Item(10, "Foo", "Bar", 0);
        when(itemService.getItemsByIds(List.of(10), token)).thenReturn(List.of(it));

        // no filters → one result
        List<Item> r1 = shopService.searchItemsInShop(1, null, null, null, null, null, null, token);
        assertEquals(1, r1.size(), "Should return the single item when no filters are applied");

        // price too high → empty
        List<Item> r2 = shopService.searchItemsInShop(1, null, null, null, 200, null, null, token);
        assertTrue(r2.isEmpty(), "Should return no items when minPrice > item price");
    }

    // Closing & reopening shops (success)
    // Closes a shop and verifies it cannot be retrieved, then “reopens” by creating another.
    @Test
    void testClosingAndReopeningShopsInRepo_Success() {
        ShopRepository repo = new ShopRepository();
        PurchasePolicy pp = mock(PurchasePolicy.class);
        ShippingMethod sm = mock(ShippingMethod.class);

        // Create, close, and verify removal
        Shop a = repo.createShop("A", pp, sm);
        repo.closeShop(a.getId());
        assertThrows(RuntimeException.class,
            () -> repo.getShop(a.getId()),
            "Closed shop should no longer be retrievable");

        // “Reopen” = create a new shop under a different name; ID must differ
        Shop b = repo.createShop("B", pp, sm);
        assertNotEquals(a.getId(), b.getId(),
            "Reopened shop should get a new, unique ID");
    }

    // Concurrent shop creation (success)
    // Spawns multiple threads creating shops to ensure unique IDs.
    @Test
    void testConcurrentShopCreationInRepo_Success() throws Exception {
        ShopRepository repo = new ShopRepository();
        PurchasePolicy pp = mock(PurchasePolicy.class);
        ShippingMethod sm = mock(ShippingMethod.class);

        int threads = 10;
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        List<Future<Shop>> futures = new ArrayList<>();

        // Each thread creates a shop with a unique name
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(exec.submit(() -> repo.createShop("S" + idx, pp, sm)));
        }

        // Collect all returned IDs
        Set<Integer> ids = new HashSet<>();
        for (Future<Shop> f : futures) {
            try {
                ids.add(f.get().getId());
            } catch (ExecutionException e) {
                fail("Shop creation failed in thread: " + e.getCause());
            }
        }
        exec.shutdown();

        // Expect exactly `threads` distinct IDs
        assertEquals(threads, ids.size(),
            "Each thread must create a shop with a unique ID");
    }


    @Test
    void testRemoveGlobalDiscount_NoPerm() throws Exception {
        String tok = "x";
        when(authTokenService.ValidateToken(tok)).thenReturn(3);
        when(userService.hasPermission(anyInt(), any(), anyInt())).thenReturn(false);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.removeGlobalDiscount(9, tok));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    @Test
    void testRemoveDiscountForItem_Error() throws Exception {
        String t = "tk";
        when(authTokenService.ValidateToken(t)).thenReturn(4);
        when(userService.hasPermission(4, PermissionsEnum.setPolicy, 5)).thenReturn(true);
        doThrow(new IllegalArgumentException("oops")).when(shopRepository).removeDiscountForItem(5, 99);
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.removeDiscountForItem(5, 99, t));
        assertTrue(ex.getMessage().contains("Error removing discount for item"));
    }

    // getShopAverageRating
    @Test
    void testGetShopAverageRating_Success() throws Exception {
        String tok = "rat";
        when(authTokenService.ValidateToken(tok)).thenReturn(7);
        when(shopRepository.getShopAverageRating(20)).thenReturn(4.5);
        double r = shopService.getShopAverageRating(20, tok);
        assertEquals(4.5, r);
    }

    // getItemQuantityFromShop
    @Test
    void testGetItemQuantityFromShop_Success() throws Exception {
        String tok = "q";
        when(authTokenService.ValidateToken(tok)).thenReturn(8);
        when(shopRepository.getItemQuantityFromShop(2, 99)).thenReturn(42);
        int q = shopService.getItemQuantityFromShop(2, 99, tok);
        assertEquals(42, q);
    }

    // checkSupplyAvailability
    @Test
    void testCheckSupplyAvailability_Success() throws Exception {
        String tok = "a";
        when(authTokenService.ValidateToken(tok)).thenReturn(9);
        when(shopRepository.checkSupplyAvailability(3, 4)).thenReturn(true);
        assertTrue(shopService.checkSupplyAvailability(3, 4, tok));
    }

    // purchaseItems
    @Test
    void testPurchaseItems_Success() throws Exception {
        String t = "x"; int uid = 10, sid = 5;
        Map<Integer,Integer> cart = Map.of(1,2);
        when(authTokenService.ValidateToken(t)).thenReturn(uid);
        when(userService.isSuspended(uid)).thenReturn(false);
        Map<Integer,ItemCategory> catMap = Map.of(1, ItemCategory.ELECTRONICS);
        when(itemService.getItemdId2Cat(cart)).thenReturn(catMap);
        when(shopRepository.purchaseItems(cart, catMap, sid)).thenReturn(123.0);
        double tot = shopService.purchaseItems(cart, sid, t);
        assertEquals(123.0, tot);
    }

    @Test
    void testPurchaseItems_Suspended() throws Exception {
        String t = "x";
        when(authTokenService.ValidateToken(t)).thenReturn(11);
        when(userService.isSuspended(11)).thenReturn(true);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.purchaseItems(Map.of(), 1, t));
        assertTrue(ex.getMessage().contains("cannot purchase items"));
    }

    // rollBackPurchase
    @Test
    void testRollBackPurchase_Success() throws Exception {
        Map<Integer,Integer> cart = Map.of(2,3);
        doNothing().when(shopRepository).rollBackPurchase(cart, 6);
        shopService.rollBackPurchase(cart, 6);
        verify(shopRepository).rollBackPurchase(cart, 6);
    }

    // checkSupplyAvailabilityAndAcquire
    @Test
    void testCheckSupplyAvailabilityAndAcquire_Success() {
        when(shopRepository.checkSupplyAvailabilityAndAqcuire(1,2,5)).thenReturn(false);
        assertFalse(shopService.checkSupplyAvailabilityAndAcquire(1,2,5));
    }

    // addSupply
    @Test
    void testAddSupply_Success() throws Exception {
        String t = "t";
        when(authTokenService.ValidateToken(t)).thenReturn(12);
        when(userService.hasPermission(12, PermissionsEnum.manageItems, 7)).thenReturn(true);
        doNothing().when(shopRepository).addSupply(7,8,9);
        shopService.addSupply(7,8,9,t);
        verify(shopRepository).addSupply(7,8,9);
    }

    // removeSupply
    @Test
    void testRemoveSupply_Success() throws Exception{
        String t = "t";
        when(authTokenService.ValidateToken(t)).thenReturn(13);
        when(userService.hasPermission(13, PermissionsEnum.manageItems, 7)).thenReturn(true);
        doNothing().when(shopRepository).removeSupply(7,8,9);
        shopService.removeSupply(7,8,9,t);
        verify(shopRepository).removeSupply(7,8,9);
    }

    // getItemsByShop
    @Test
    void testGetItemsByShop_Success() throws Exception{
        String t = "t";
        List<Integer> ids = List.of(100);
        when(authTokenService.ValidateToken(t)).thenReturn(15);
        when(shopRepository.getItemsByShop(3)).thenReturn(ids);
        Item it = new Item(100, "Name", "Desc", 0);
        when(itemService.getItemsByIds(ids, t)).thenReturn(List.of(it));
        List<Item> out = shopService.getItemsByShop(3, t);
        assertEquals(1, out.size());
        assertEquals(it, out.get(0));
    }

    // getAllShops
    @Test
    void testGetAllShops_Success() throws Exception{
        String tok = "tok";
        List<Shop> list = List.of(new Shop(1, "A", shippingMethod));
        when(authTokenService.ValidateToken(tok)).thenReturn(2);
        when(shopRepository.getAllShops()).thenReturn(list);
        List<Shop> result = shopService.getAllShops(tok);
        assertEquals(list, result);
    }

    @Test
    void testGetAllShops_Error() throws Exception{
        String tok = "tok";
        when(authTokenService.ValidateToken(tok)).thenReturn(2);
        when(shopRepository.getAllShops()).thenThrow(new RuntimeException("fail"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getAllShops(tok));
        assertTrue(ex.getMessage().contains("Error retrieving all shops"));
    }

    // getItems (global)
    @Test
    void testGetItems_Success() throws Exception{
        String tok = "t";
        List<Integer> ids = List.of(200);
        when(authTokenService.ValidateToken(tok)).thenReturn(3);
        when(shopRepository.getItems()).thenReturn(ids);
        Item itm = new Item(200, "Name", "Desc", 0);
        when(itemService.getItemsByIds(ids, tok)).thenReturn(List.of(itm));
        List<Item> out = shopService.getItems(tok);
        assertEquals(1, out.size());
        assertEquals(itm, out.get(0));
    }

    @Test
    void testGetItems_Error() throws Exception{
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(4);
        when(shopRepository.getItems()).thenThrow(new RuntimeException("err"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getItems(tok));
        assertTrue(ex.getMessage().contains("Error retrieving all items"));
    }

    // searchItems (global)
    @Test
    void testSearchItems_Global_Success() throws Exception{
        String tok = "x";
        when(authTokenService.ValidateToken(tok)).thenReturn(5);
        Shop shop1 = new Shop(1, "S1", shippingMethod);
        when(shopRepository.getAllShops()).thenReturn(List.of(shop1));
        // underlying filter: get item IDs and details
        when(shopRepository.getItemsByShop(1)).thenReturn(List.of(1, 2));
        Item found = new Item(1, "FindMe", "desc", 0);
        Item other = new Item(2, "IgnoreMe", "desc", 0);
        when(itemService.getItemsByIds(List.of(1, 2), tok)).thenReturn(List.of(found, other));
        // apply name filter "find"
        List<Item> res = shopService.searchItems("find", null, null, null, null, null, null, tok);
        assertEquals(1, res.size());
        assertEquals(found, res.get(0));
    }

    @Test
    void testSearchItems_Global_Error() throws Exception{
        String tok = "x";
        when(authTokenService.ValidateToken(tok)).thenReturn(6);
        when(shopRepository.getAllShops()).thenThrow(new RuntimeException("no shops"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.searchItems(null, null, null, null, null, null, null, tok));
        // Should wrap and prefix with "Error searching items: "
        assertTrue(ex.getMessage().contains("Error searching items:") || ex.getMessage().contains("searchItems"));
    }

    // shipPurchase
    @Test
    void testShipPurchase_Success() throws Exception{
        String tok = "tk";
        when(authTokenService.ValidateToken(tok)).thenReturn(7);
        doNothing().when(shopRepository).shipPurchase(1,2,"C","City","St","PC");
        shopService.shipPurchase(tok,1,2,"C","City","St","PC");
        verify(shopRepository).shipPurchase(1,2,"C","City","St","PC");
    }

    @Test
    void testShipPurchase_Error() throws Exception{
        String tok = "tk";
        when(authTokenService.ValidateToken(tok)).thenReturn(7);
        doThrow(new RuntimeException("fail ship")).when(shopRepository).shipPurchase(anyInt(), anyInt(), any(), any(), any(), any());
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.shipPurchase(tok,3,4,"C","C","C","C"));
        assertTrue(ex.getMessage().contains("Error shipping purchase"));
    }

    // removeGlobalDiscount
    @Test
    void testRemoveGlobalDiscount_Success() throws Exception {
        String tok = "x"; int shopId = 10;
        when(authTokenService.ValidateToken(tok)).thenReturn(3);
        when(userService.hasPermission(3, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).removeGlobalDiscount(shopId);
        shopService.removeGlobalDiscount(shopId, tok);
        verify(shopRepository).removeGlobalDiscount(shopId);
    }

    @Test
    void testRemoveGlobalDiscount_NoPermission_Failure() throws Exception {
        String tok = "x"; int shopId = 9;
        when(authTokenService.ValidateToken(tok)).thenReturn(3);
        when(userService.hasPermission(anyInt(), any(), anyInt())).thenReturn(false);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.removeGlobalDiscount(shopId, tok)
        );
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // removeDiscountForItem
    @Test
    void testRemoveDiscountForItem_Success() throws Exception {
        String t = "tk"; int shopId = 5, itemId = 99;
        when(authTokenService.ValidateToken(t)).thenReturn(4);
        when(userService.hasPermission(4, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).removeDiscountForItem(shopId, itemId);
        shopService.removeDiscountForItem(shopId, itemId, t);
        verify(shopRepository).removeDiscountForItem(shopId, itemId);
    }

    @Test
    void testRemoveDiscountForItem_Error_Failure() throws Exception {
        String t = "tk"; int shopId = 5, itemId = 99;
        when(authTokenService.ValidateToken(t)).thenReturn(4);
        when(userService.hasPermission(4, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("oops"))
            .when(shopRepository).removeDiscountForItem(shopId, itemId);
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.removeDiscountForItem(shopId, itemId, t)
        );
        assertTrue(ex.getMessage().contains("Error removing discount for item"));
    }

    // category discounts
    @Test
    void testSetCategoryDiscount_Success() throws Exception {
        String t = "t"; int shopId = 11;
        when(authTokenService.ValidateToken(t)).thenReturn(5);
        when(userService.hasPermission(5, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).setCategoryDiscount(shopId, ItemCategory.BOOKS, 30, false);
        shopService.setCategoryDiscount(shopId, ItemCategory.BOOKS, 30, false, t);
        verify(shopRepository).setCategoryDiscount(shopId, ItemCategory.BOOKS, 30, false);
    }

    @Test
    void testSetCategoryDiscount_InvalidDiscount_Failure() throws Exception {
        String t = "t"; int shopId = 11;
        when(authTokenService.ValidateToken(t)).thenReturn(5);
        when(userService.hasPermission(5, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Invalid discount"))
            .when(shopRepository).setCategoryDiscount(shopId, ItemCategory.CLOTHING, 150, false);
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.setCategoryDiscount(shopId, ItemCategory.CLOTHING, 150, false, t)
        );
        assertTrue(ex.getMessage().contains("Error setting category discount"));
    }

    @Test
    void testSetCategoryDiscount_NoPermission_Failure() throws Exception {
        String t = "t"; int shopId = 11;
        when(authTokenService.ValidateToken(t)).thenReturn(5);
        when(userService.hasPermission(5, PermissionsEnum.setPolicy, shopId)).thenReturn(false);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.setCategoryDiscount(shopId, ItemCategory.CLOTHING, 20, false, t)
        );
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // removeCategoryDiscount
    @Test
    void testRemoveCategoryDiscount_Success() throws Exception {
        String t = "t"; int shopId = 12;
        when(authTokenService.ValidateToken(t)).thenReturn(6);
        when(userService.hasPermission(6, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).removeCategoryDiscount(shopId, ItemCategory.CLOTHING);
        shopService.removeCategoryDiscount(shopId, ItemCategory.CLOTHING, t);
        verify(shopRepository).removeCategoryDiscount(shopId, ItemCategory.CLOTHING);
    }

    @Test
    void testRemoveCategoryDiscount_Error_Failure() throws Exception {
        String t = "t"; int shopId = 12;
        when(authTokenService.ValidateToken(t)).thenReturn(6);
        when(userService.hasPermission(6, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalStateException("fail"))
            .when(shopRepository).removeCategoryDiscount(shopId, ItemCategory.ELECTRONICS);
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.removeCategoryDiscount(shopId, ItemCategory.ELECTRONICS, t)
        );
        assertTrue(ex.getMessage().contains("Error removing category discount"));
    }

    @Test
    void testRemoveCategoryDiscount_NoPermission_Failure() throws Exception {
        String t = "t"; int shopId = 12;
        when(authTokenService.ValidateToken(t)).thenReturn(6);
        when(userService.hasPermission(6, PermissionsEnum.setPolicy, shopId)).thenReturn(false);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.removeCategoryDiscount(shopId, ItemCategory.ELECTRONICS, t)
        );
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // addReviewToShop (suspended user)
    @Test
    void testAddReviewToShop_Suspended_Failure() throws Exception {
        String token = "t"; int shopId = 2;
        when(authTokenService.ValidateToken(token)).thenReturn(7);
        doNothing().when(userService).validateMemberId(7);
        when(userService.isSuspended(7)).thenReturn(true);
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.addReviewToShop(shopId, 5, "text", token)
        );
        assertTrue(ex.getMessage().contains("cannot add a review"));
    }

    // addItemToShop (item service error)
    @Test
    void testAddItemToShop_ItemServiceError_Failure() throws Exception {
        String token = "tok"; int shopId = 3;
        when(authTokenService.ValidateToken(token)).thenReturn(8);
        when(userService.hasPermission(8, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        when(itemService.createItem(eq(shopId), any(), any(), eq(ItemCategory.AUTOMOTIVE), eq(token)))
            .thenThrow(new RuntimeException("create failed"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addItemToShop(shopId, "n", "d", 1,ItemCategory.AUTOMOTIVE, 1, token)
        );
        assertTrue(ex.getMessage().contains("Error adding item to shop"));
    }
    
    // getShop throws on invalid token
    @Test
    void testGetShop_InvalidToken() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new RuntimeException("no auth"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getShop(1, "bad")
        );
        assertTrue(ex.getMessage().contains("no auth"));
    }

    // searchItems filters by minShopRating
    @Test
    void testSearchItems_MinShopRating() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        Shop low = new Shop(1, "Low", shippingMethod);
        Shop high = new Shop(2, "High", shippingMethod);
        low.addReview(new ShopReview(0, 1, tok));
        high.addReview(new ShopReview(0, 5, tok));
        when(shopRepository.getAllShops()).thenReturn(Arrays.asList(low, high));
        // both shops have one item each
        when(shopRepository.getItemsByShop(anyInt())).thenReturn(Arrays.asList(10));
        Item it = new Item(10, "X", "Y", 0);
        when(itemService.getItemsByIds(eq(Arrays.asList(10)), eq(tok))).thenReturn(Arrays.asList(it));

        // only high-rated shop passes minShopRating=4.0
        List<Item> res = shopService.searchItems(null, null, null, null, null, null, 4.0, tok);
        assertEquals(1, res.size());
    }

    // searchItems global category filter
    @Test
    void testSearchItems_CategoryFilter() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        Shop s = new Shop(1, "S", shippingMethod);
        when(shopRepository.getAllShops()).thenReturn(List.of(s));
        when(shopRepository.getItemsByShop(1)).thenReturn(List.of(1,2));
        Item a = new Item(1, "A", "desc", 0);
        Item b = new Item(2, "B", "desc", 2);
        when(itemService.getItemsByIds(List.of(1,2), tok)).thenReturn(List.of(a,b));
        List<Item> found = shopService.searchItems(null, ItemCategory.ELECTRONICS, null, null, null, null, null, tok);
        assertEquals(1, found.size()); assertEquals(a, found.get(0));
    }

    // searchItems keywords filter on description
    @Test
    void testSearchItems_KeywordsFilter() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        Shop s = new Shop(1, "S", shippingMethod);
        when(shopRepository.getAllShops()).thenReturn(List.of(s));
        when(shopRepository.getItemsByShop(1)).thenReturn(List.of(1));
        Item it = new Item(1, "Name", "specialDescription", 0);
        when(itemService.getItemsByIds(List.of(1), tok)).thenReturn(List.of(it));
        List<Item> res = shopService.searchItems(null, null, List.of("special"), null, null, null, null, tok);
        assertEquals(1, res.size());
    }

    // searchItems minProductRating filter
    @Test
    void testSearchItems_MinProductRatingFilter() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        Shop s = new Shop(1, "S", shippingMethod);
        when(shopRepository.getAllShops()).thenReturn(List.of(s));
        when(shopRepository.getItemsByShop(1)).thenReturn(List.of(1));
        Item it = new Item(1, "N", "D", 5);
        it.addReview(new ItemReview(5,""));
        when(itemService.getItemsByIds(List.of(1), tok)).thenReturn(List.of(it));
        List<Item> res1 = shopService.searchItems(null, null, null, null, null, 4.0, null, tok);
        assertEquals(1, res1.size());
        List<Item> res2 = shopService.searchItems(null, null, null, null, null, 6.0, null, tok);
        assertTrue(res2.isEmpty());
    }

    // purchaseItems repository exception
    @Test
    void testPurchaseItems_RepoError() throws Exception {
        String tok = "t"; int uid=1, sid=1;
        Map<Integer,Integer> cart = Map.of(1,1);
        when(authTokenService.ValidateToken(tok)).thenReturn(uid);
        when(userService.isSuspended(uid)).thenReturn(false);
        when(itemService.getItemdId2Cat(cart)).thenReturn(Collections.emptyMap());
        when(shopRepository.purchaseItems(cart, Collections.emptyMap(), sid))
            .thenThrow(new RuntimeException("fail"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.purchaseItems(cart, sid, tok)
        );
        assertTrue(ex.getMessage().contains("Error purchasing items"));
    }

    // checkPolicy success and error
    @Test
    void testCheckPolicy_SuccessAndError() throws Exception {
        String tok = "t";
        HashMap<Integer,HashMap<Integer,Integer>> cart = new HashMap<>();
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.checkPolicy(cart, tok)).thenReturn(true);
        assertTrue(shopService.checkPolicy(cart, tok));
        when(shopRepository.checkPolicy(cart, tok)).thenThrow(new RuntimeException("oops"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.checkPolicy(cart, tok)
        );
        assertTrue(ex.getMessage().contains("Error checking policy"));
    }

    // rollBackPurchase exception
    @Test
    void testRollBackPurchase_Error() throws Exception {
        Map<Integer,Integer> cart = Map.of(1,1);
        doThrow(new RuntimeException("rbfail")).when(shopRepository).rollBackPurchase(cart, 1);
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.rollBackPurchase(cart, 1)
        );
        assertTrue(ex.getMessage().contains("Error rolling back purchase"));
    }

    // getItemsByShop error branch
    @Test
    void testGetItemsByShop_Error() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.getItemsByShop(1)).thenThrow(new RuntimeException("gone"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getItemsByShop(1, tok)
        );
        assertTrue(ex.getMessage().contains("Error retrieving items for shop"));
    }

    // shipPurchase invalid token
    @Test
    void testShipPurchase_InvalidToken() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new RuntimeException("noAuth"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.shipPurchase("bad",1,1,"C","C","C","C")
        );
        assertTrue(ex.getMessage().contains("noAuth"));
    }

    // getShop success
    @Test
    void testGetShop_Success() throws Exception {
        String tok = "abc";
        int shopId = 5;
        Shop expected = new Shop(shopId, "X", shippingMethod);

        when(authTokenService.ValidateToken(tok)).thenReturn(2);
        when(shopRepository.getShop(shopId)).thenReturn(expected);

        Shop actual = shopService.getShop(shopId, tok);
        assertEquals(expected, actual);
    }

    // getAllShops invalid token
    @Test
    void testGetAllShops_InvalidToken() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new RuntimeException("no auth"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getAllShops("bad"));
        assertTrue(ex.getMessage().contains("no auth"));
    }

    // searchItemsInShop error path
    @Test
    void testSearchItemsInShop_Error() throws Exception {
        String tok = "t";
        int shopId = 1;

        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.getShop(shopId)).thenThrow(new RuntimeException("fail getShop"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.searchItemsInShop(shopId, null, null, null, null, null, null, tok));
        assertTrue(ex.getMessage().contains("Error searching items:") || ex.getMessage().contains("searchItems"));
    }

    // removeSupply — no permission
    @Test
    void testRemoveSupply_NoPermission_Failure() throws Exception {
        String tok = "t";
        int shopId = 1, itemId = 2, qty = 3;

        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(userService.hasPermission(1, PermissionsEnum.manageItems, shopId)).thenReturn(false);

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.removeSupply(shopId, itemId, qty, tok));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // removeSupply — repo error
    @Test
    void testRemoveSupply_RepoError() throws Exception {
        String tok = "t";
        int shopId = 1, itemId = 2, qty = 3;

        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(userService.hasPermission(1, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        doThrow(new RuntimeException("err"))
            .when(shopRepository).removeSupply(shopId, itemId, qty);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.removeSupply(shopId, itemId, qty, tok));
        assertTrue(ex.getMessage().contains("Error removing supply"));
    }

    // checkSupplyAvailabilityAndAcquire — error path
    @Test
    void testCheckSupplyAvailabilityAndAcquire_Error() {
        when(shopRepository.checkSupplyAvailabilityAndAqcuire(1, 2, 3))
            .thenThrow(new RuntimeException("fail"));

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.checkSupplyAvailabilityAndAcquire(1, 2, 3));
        assertTrue(ex.getMessage().contains("Error checking supply"));
    }

    // createShop — invalid token
    @Test
    void testCreateShop_InvalidToken() throws Exception {
        String tok = "bad";
        when(authTokenService.ValidateToken(tok)).thenThrow(new RuntimeException("no auth"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.createShop("Name", purchasePolicy, shippingMethod, tok));
        assertTrue(ex.getMessage().contains("no auth"));
    }

    // updatePurchasePolicy — invalid token
    @Test
    void testUpdatePurchasePolicy_InvalidToken() throws Exception {
        String tok = "bad";
        when(authTokenService.ValidateToken(tok)).thenThrow(new RuntimeException("no auth"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.updatePurchasePolicy(1, purchasePolicy, tok));
        assertTrue(ex.getMessage().contains("Error updating purchase policy"));
    }
    
        // getItemsByShop – invalid token
    @Test
    void testGetItemsByShop_InvalidToken() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new RuntimeException("auth fail"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getItemsByShop(10, "bad"));
        assertTrue(ex.getMessage().contains("auth fail"));
    }

    // getItemsByShop – repository error
    @Test
    void testGetItemsByShop_RepoError() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.getItemsByShop(2)).thenThrow(new RuntimeException("db error"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getItemsByShop(2, tok));
        assertTrue(ex.getMessage().contains("Error retrieving items for shop"));
    }

    // getItems (global) – invalid token
    @Test
    void testGetItems_InvalidToken() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new RuntimeException("no auth"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getItems("bad"));
        assertTrue(ex.getMessage().contains("no auth"));
    }

    // setGlobalDiscount – invalid token
    @Test
    void testSetGlobalDiscount_InvalidToken() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new RuntimeException("no auth"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.setGlobalDiscount(1, 10, true, "bad"));
        assertTrue(ex.getMessage().contains("no auth"));
    }

    // setDiscountForItem – invalid token
    @Test
    void testSetDiscountForItem_InvalidToken() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new RuntimeException("no auth"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.setDiscountForItem(1, 1, 5, true, "bad"));
        assertTrue(ex.getMessage().contains("no auth"));
    }

    // setCategoryDiscount – invalid token
    @Test
    void testSetCategoryDiscount_InvalidToken() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new RuntimeException("no auth"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.setCategoryDiscount(1, ItemCategory.BOOKS, 5, false, "bad"));
        assertTrue(ex.getMessage().contains("no auth"));
    }

    // removeCategoryDiscount – invalid token
    @Test
    void testRemoveCategoryDiscount_InvalidToken() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new RuntimeException("no auth"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.removeCategoryDiscount(1, ItemCategory.CLOTHING, "bad"));
        assertTrue(ex.getMessage().contains("no auth"));
    }

    // getShopAverageRating – repo error
    @Test
    void testGetShopAverageRating_RepoError() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.getShopAverageRating(3)).thenThrow(new RuntimeException("db fail"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getShopAverageRating(3, tok));
        assertTrue(ex.getMessage().contains("Error retrieving average rating"));
    }

    // checkSupplyAvailability – repo error
    @Test
    void testCheckSupplyAvailability_RepoError() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.checkSupplyAvailability(5, 6)).thenThrow(new RuntimeException("db fail"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.checkSupplyAvailability(5, 6, tok));
        assertTrue(ex.getMessage().contains("Error checking supply"));
    }

    // purchaseItems – itemService throws OurArg
    @Test
    void testPurchaseItems_ItemServiceError() throws Exception {
        String tok = "t";
        int uid = 2, sid = 3;
        Map<Integer,Integer> cart = Map.of(1,1);
        when(authTokenService.ValidateToken(tok)).thenReturn(uid);
        when(userService.isSuspended(uid)).thenReturn(false);
        when(itemService.getItemdId2Cat(cart)).thenThrow(new OurArg("bad arg"));

        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.purchaseItems(cart, sid, tok));
        assertTrue(ex.getMessage().contains("purchaseItems"));
    }

    // searchItemsInShop – price filter edge
    @Test
    void testSearchItemsInShop_PriceFilter() throws Exception {
        String tok = "t";
        int shopId = 1;
        Item it = new Item(1, "X", "Y", 0);

        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.getShop(shopId)).thenReturn(new Shop(shopId,"S",shippingMethod));
        when(shopRepository.getItemsByShop(shopId)).thenReturn(List.of(1));
        when(itemService.getItemsByIds(List.of(1), tok)).thenReturn(List.of(it));

        // spy shop so getItemPrice(1) == 5
        Shop spyShop = spy(new Shop(shopId,"S",shippingMethod));
        doReturn(5).when(spyShop).getItemPrice(1);
        when(shopRepository.getShop(shopId)).thenReturn(spyShop);

        // ask for minPrice=6 → no results
        List<Item> res = shopService.searchItemsInShop(shopId, null, null, null, 6, null, null, tok);
        assertTrue(res.isEmpty());
    }

    // checkPolicy – invalid token (OurArg path)
    @Test
    void testCheckPolicy_InvalidToken() throws Exception {
        HashMap<Integer, HashMap<Integer, Integer>> cart = new HashMap<>();
        when(authTokenService.ValidateToken("bad")).thenThrow(new OurArg("bad token"));
        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.checkPolicy(cart, "bad"));
        assertTrue(ex.getMessage().contains("checkPolicy"));
    }

    // addSupply – invalid token → OurArg branch
    @Test
    void testAddSupply_InvalidToken() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new OurArg("no auth"));
        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.addSupply(1, 2, 3, "bad"));
        assertTrue(ex.getMessage().contains("addSupply"));
    }

    // setGlobalDiscount – repo error → catches Exception and throws OurRuntime
    @Test
    void testSetGlobalDiscount_RepoError() throws Exception {
        String tok = "t"; int shopId = 2;
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(userService.hasPermission(1, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalStateException("db fail"))
            .when(shopRepository).setGlobalDiscount(shopId, 50, false);

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.setGlobalDiscount(shopId, 50, false, tok));
        assertTrue(ex.getMessage().contains("Error setting global discount for shop"));
    }

    // removeGlobalDiscount – repo error
    @Test
    void testRemoveGlobalDiscount_RepoError() throws Exception {
        String tok = "t"; int shopId = 3;
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(userService.hasPermission(1, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("db fail"))
            .when(shopRepository).removeGlobalDiscount(shopId);

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.removeGlobalDiscount(shopId, tok));
        assertTrue(ex.getMessage().contains("Error removing global discount for shop"));
    }

    // ----- add multiple discount policies in a single shop -----
    @Test
    void testAddThreeDiscountPolicies_Success() throws Exception {
        String token = "t";
        int shopId = 20;
        int userId = 5;

        // Arrange: valid token and permission
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        when(userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)).thenReturn(true);

        // Act: add three distinct discount policies to the same shop
        shopService.addDiscountPolicy(token, /*discountId=*/1, /*priority=*/10, ItemCategory.BOOKS, 10.0, Operator.AND, shopId);
        shopService.addDiscountPolicy(token, /*discountId=*/2, /*priority=*/20, ItemCategory.ELECTRONICS, 15.0, Operator.OR, shopId);
        shopService.addDiscountPolicy(token, /*discountId=*/3, /*priority=*/30, ItemCategory.CLOTHING, 20.0, Operator.AND, shopId);

        // Assert: verify that each call was forwarded to the repository correctly
        verify(shopRepository).addDiscountPolicy(
            eq(1), eq(10), eq(ItemCategory.BOOKS), eq(10.0), eq(Operator.AND), eq(shopId)
        );
        verify(shopRepository).addDiscountPolicy(
            eq(2), eq(20), eq(ItemCategory.ELECTRONICS), eq(15.0), eq(Operator.OR), eq(shopId)
        );
        verify(shopRepository).addDiscountPolicy(
            eq(3), eq(30), eq(ItemCategory.CLOTHING), eq(20.0), eq(Operator.AND), eq(shopId)
        );
    }


    // getShopAverageRating – invalid token (OurRuntime path)
    @Test
    void testGetShopAverageRating_InvalidToken() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new OurRuntime("no auth"));
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.getShopAverageRating(5, "bad"));
        assertTrue(ex.getMessage().contains("getShopAverageRating"));
    }
    
    // createShop – userService.validateMemberId throws OurRuntime → should wrap into createShop… branch
    @Test
    void testCreateShop_UserServiceOurRuntimeFailure() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        doThrow(new OurRuntime("member fail"))
            .when(userService).validateMemberId(1);

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.createShop("Name", purchasePolicy, shippingMethod, tok));
        assertTrue(ex.getMessage().contains("createShop"));
    }

    // searchItemsInShop – shop.getItemPrice throws → filterItemsInShop → caught and rethrown by searchItemsInShop
    @Test
    void testSearchItemsInShop_FilterError() throws Exception {
        String tok = "t";
        int shopId = 1;
        Item it = new Item(1, "X", "Y", 0);

        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        Shop spyShop = spy(new Shop(shopId, "S", shippingMethod));
        when(shopRepository.getShop(shopId)).thenReturn(spyShop);
        when(shopRepository.getItemsByShop(shopId)).thenReturn(List.of(1));
        when(itemService.getItemsByIds(List.of(1), tok)).thenReturn(List.of(it));
        doThrow(new RuntimeException("price fail"))
            .when(spyShop).getItemPrice(1);

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.searchItemsInShop(shopId, null, null, null, null, null, null, tok));
        assertTrue(ex.getMessage().contains("searchItemsInShop"));
    }

    // checkPolicy – ValidateToken throws OurArg → should propagate as OurArg("checkPolicy"+…)
    @Test
    void testCheckPolicy_InvalidTokenOurArg() throws Exception {
        HashMap<Integer,HashMap<Integer,Integer>> cart = new HashMap<>();
        when(authTokenService.ValidateToken("bad")).thenThrow(new OurArg("bad token"));

        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.checkPolicy(cart, "bad"));
        assertTrue(ex.getMessage().contains("checkPolicy"));
    }

    // addSupply – ValidateToken throws OurArg → should propagate as OurArg("addSupply"+…)
    @Test
    void testAddSupply_InvalidTokenOurArg() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new OurArg("auth fail"));

        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.addSupply(1, 2, 3, "bad"));
        assertTrue(ex.getMessage().contains("addSupply"));
    }

    // searchItems – ValidateToken throws OurArg → should propagate as OurArg("searchItems"+…)
    @Test
    void testSearchItems_InvalidTokenOurArg() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new OurArg("no auth"));

        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.searchItems(null, null, null, null, null, null, null, "bad"));
        assertTrue(ex.getMessage().contains("searchItems"));
    }

    // searchItems – itemService.getItemsByIds throws OurArg inside filterItemsInShop → bubbles up to searchItems
    @Test
    void testSearchItems_ItemServiceOurArg() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        Shop s = new Shop(1, "S", shippingMethod);
        when(shopRepository.getAllShops()).thenReturn(List.of(s));
        when(shopRepository.getItemsByShop(1)).thenReturn(List.of(1));
        when(itemService.getItemsByIds(List.of(1), tok)).thenThrow(new OurArg("item bad"));

        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.searchItems(null, null, null, null, null, null, null, tok));
        assertTrue(ex.getMessage().contains("searchItems"));
        assertTrue(ex.getMessage().contains("filterItemsInShop"));
        assertTrue(ex.getMessage().contains("item bad"));
    }

    // closeShop – userService.closeShopNotification throws → should wrap into "Error closing shop…"
    @Test
    void testCloseShop_NotificationError() throws Exception {
        String tok = "t";
        int shopId = 1;
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(userService.hasPermission(1, PermissionsEnum.closeShop, shopId)).thenReturn(true);
        doNothing().when(shopRepository).closeShop(shopId);
        doThrow(new RuntimeException("notify fail"))
            .when(userService).closeShopNotification(shopId);

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.closeShop(shopId, tok));
        assertTrue(ex.getMessage().contains("Error closing shop"));
    }

    // getAllShops – ValidateToken throws OurArg → should propagate as OurArg("getAllShops"+…)
    @Test
    void testGetAllShops_OurArg() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new OurArg("no auth"));

        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.getAllShops("bad"));
        assertTrue(ex.getMessage().contains("getAllShops"));
    }

    // rollBackPurchase – repository throws OurArg → should wrap into rollBackPurchase… branch
    @Test
    void testRollBackPurchase_OurArg() throws Exception {
        Map<Integer,Integer> cart = Map.of(1,1);
        doThrow(new OurArg("rb fail"))
            .when(shopRepository).rollBackPurchase(cart, 1);

        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.rollBackPurchase(cart, 1));
        assertTrue(ex.getMessage().contains("rollBackPurchase"));
    }

    // shipPurchase – repository throws OurRuntime → should wrap into shipPurchase… branch
    @Test
    void testShipPurchase_RepoOurRuntime() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        doThrow(new OurRuntime("ship fail"))
            .when(shopRepository).shipPurchase(anyInt(), anyInt(), any(), any(), any(), any());

        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.shipPurchase(tok, 1, 2, "C", "C", "C", "C"));
        assertTrue(ex.getMessage().contains("shipPurchase"));
        assertTrue(ex.getMessage().contains("ship fail"));
    }
    
    // getItemQuantityFromShop → authToken throws OurArg
    @Test
    void testGetItemQuantityFromShop_InvalidTokenOurArg() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new OurArg("no auth"));
        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.getItemQuantityFromShop(1, 2, "bad"));
        assertTrue(ex.getMessage().contains("getItemQuantityFromShop"));
    }

    // getItemQuantityFromShop → repo throws RuntimeException
    @Test
    void testGetItemQuantityFromShop_RepoError() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.getItemQuantityFromShop(1, 2))
            .thenThrow(new RuntimeException("db fail"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getItemQuantityFromShop(1, 2, tok));
        assertTrue(ex.getMessage().contains("Error retrieving quantity"));
    }

    // searchItemsInShop → shop.getItemPrice throws OurArg inside filterItemsInShop
    @Test
    void testSearchItemsInShop_ItemPriceOurArg_Failure() throws Exception {
        String tok = "t";
        int shopId = 1;
        Item it = new Item(1, "X", "Y", 0);

        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        Shop spyShop = spy(new Shop(shopId, "S", shippingMethod));
        when(shopRepository.getShop(shopId)).thenReturn(spyShop);
        when(shopRepository.getItemsByShop(shopId)).thenReturn(List.of(1));
        when(itemService.getItemsByIds(List.of(1), tok)).thenReturn(List.of(it));
        doThrow(new OurArg("price error"))
            .when(spyShop).getItemPrice(1);

        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.searchItemsInShop(shopId, null, null, null, null, null, null, tok));
        assertTrue(ex.getMessage().contains("filterItemsInShop"));
    }

    // checkSupplyAvailability → authToken throws OurArg
    @Test
    void testCheckSupplyAvailability_InvalidTokenOurArg() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new OurArg("no auth"));
        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.checkSupplyAvailability(1, 2, "bad"));
        assertTrue(ex.getMessage().contains("checkSupplyAvailability"));
    }

    // checkSupplyAvailability → repo throws RuntimeException
    @Test
    void testCheckSupplyAvailability_RepoErrorOurRuntime() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.checkSupplyAvailability(1, 2))
            .thenThrow(new IllegalStateException("db fail"));
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.checkSupplyAvailability(1, 2, tok));
        assertTrue(ex.getMessage().contains("Error checking supply"));
    }

    // checkSupplyAvailabilityAndAcquire → repo throws OurArg
    @Test
    void testCheckSupplyAvailabilityAndAcquire_RepoOurArg() {
        when(shopRepository.checkSupplyAvailabilityAndAqcuire(1, 2, 3))
            .thenThrow(new OurArg("fail"));
        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.checkSupplyAvailabilityAndAcquire(1, 2, 3));
        assertTrue(ex.getMessage().contains("checkSupplyAvailabilityAndAqcuire"));
    }

    // checkSupplyAvailabilityAndAcquire → repo throws RuntimeException
    @Test
    void testCheckSupplyAvailabilityAndAcquire_RepoOurRuntime() {
        when(shopRepository.checkSupplyAvailabilityAndAqcuire(1, 2, 3))
            .thenThrow(new RuntimeException("fail"));
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.checkSupplyAvailabilityAndAcquire(1, 2, 3));
        assertTrue(ex.getMessage().contains("Error checking supply"));
    }

    // getItemsByShop → authToken throws OurArg
    @Test
    void testGetItemsByShop_InvalidTokenOurArg() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new OurArg("no auth"));
        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.getItemsByShop(1, "bad"));
        assertTrue(ex.getMessage().contains("getItems"));
    }

    // getItemsByShop → repo throws RuntimeException
    @Test
    void testGetItemsByShop_RepoErrorOurRuntime() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.getItemsByShop(2))
            .thenThrow(new IllegalStateException("db error"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getItemsByShop(2, tok));
        assertTrue(ex.getMessage().contains("Error retrieving items"));
    }

    // getItems (global) → authToken throws OurArg
    @Test
    void testGetItems_InvalidTokenOurArg() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new OurArg("no auth"));
        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.getItems("bad"));
        assertTrue(ex.getMessage().contains("getItems"));
    }

    // getItems (global) → repo throws RuntimeException
    @Test
    void testGetItems_RepoOurRuntime() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.getItems())
            .thenThrow(new IllegalStateException("db fail"));
        OurRuntime ex = assertThrows(OurRuntime.class,
            () -> shopService.getItems(tok));
        assertTrue(ex.getMessage().contains("Error retrieving all items"));
    }

    // getItems (global) → itemService throws OurArg
    @Test
    void testGetItems_ItemServiceOurArg() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.getItems()).thenReturn(List.of(1));
        when(itemService.getItemsByIds(List.of(1), tok))
            .thenThrow(new OurArg("item error"));
        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.getItems(tok));
        assertTrue(ex.getMessage().contains("getItems"));
        assertTrue(ex.getMessage().contains("item error"));
    }

    // searchItems → filterItemsInShop throws RuntimeException, bubbles up as OurRuntime
    @Test
    void testSearchItems_FilterItemsError() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        Shop s = new Shop(1, "S", shippingMethod);
        when(shopRepository.getAllShops()).thenReturn(List.of(s));
        when(shopRepository.getItemsByShop(1)).thenReturn(List.of(1));
        when(itemService.getItemsByIds(List.of(1), tok))
            .thenThrow(new RuntimeException("item fail"));
        
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.searchItems(null, null, null, null, null, null, null, tok));
        assertTrue(ex.getMessage().contains("searchItems"));
    }


    @Test
    void testGetShop_Success2() throws Exception {
        String token = "tok";
        int shopId = 42;
        Shop expected = new Shop(shopId, "MyShop", shippingMethod);

        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShop(shopId)).thenReturn(expected);

        Shop actual = shopService.getShop(shopId, token);
        assertEquals(expected, actual);
    }

    @Test
    void testGetShop_InvalidToken_Failure() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new RuntimeException("no auth"));
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getShop(1, "bad"));
        assertTrue(ex.getMessage().contains("no auth"));
    }

    @Test
    void testGetShop_RepoError_Failure() throws Exception {
        String token = "tok";
        int shopId = 7;

        when(authTokenService.ValidateToken(token)).thenReturn(1);
        when(shopRepository.getShop(shopId)).thenThrow(new RuntimeException("db fail"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.getShop(shopId, token));
        assertTrue(ex.getMessage().contains("Error retrieving shop"));
    }


    //
    // ----- searchItems(...)
    //

    @Test
    void testSearchItems_Global_Success2() throws Exception {
        String token = "tok";
        when(authTokenService.ValidateToken(token)).thenReturn(1);

        // one shop that passes minShopRating
        Shop s = new Shop(1, "S", shippingMethod);
        s.addReview(1,5,"r");
        when(shopRepository.getAllShops()).thenReturn(List.of(s));

        // underlying in-shop search
        when(shopRepository.getShop(1)).thenReturn(s);
        when(shopRepository.getItemsByShop(1)).thenReturn(List.of(10));
        Item it = new Item(10,"Find","desc",0);
        when(itemService.getItemsByIds(List.of(10), token)).thenReturn(List.of(it));

        // only "find" matches
        var results = shopService.searchItems("find", null, null, null, null, null, 4.0, token);
        assertEquals(1, results.size());
        assertEquals(it, results.get(0));
    }

    @Test
    void testSearchItems_InvalidToken_OurArg() throws Exception{
        when(authTokenService.ValidateToken("bad")).thenThrow(new OurArg("bad token"));
        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.searchItems(null, null, null, null, null, null, null, "bad"));
        assertTrue(ex.getMessage().contains("searchItems"));
    }


    //
    // ----- searchItemsInShop(...)
    //

    @Test
    void testSearchItemsInShop_PriceAndRatingFilters() throws Exception {
        String token = "tok";
        int shopId = 5;
        when(authTokenService.ValidateToken(token)).thenReturn(1);

        Shop spyShop = spy(new Shop(shopId, "S", shippingMethod));
        doReturn(20).when(spyShop).getItemPrice(10);
        when(shopRepository.getShop(shopId)).thenReturn(spyShop);
        when(shopRepository.getItemsByShop(shopId)).thenReturn(List.of(10));

        Item it = new Item(10, "Name", "Desc", 0);
        it.addReview(new ItemReview(5, "r"));
        when(itemService.getItemsByIds(List.of(10), token)).thenReturn(List.of(it));

        // minPrice > price → no hits
        assertTrue(shopService.searchItemsInShop(shopId, null, null, null, 30, 50, null, token).isEmpty());
        // minProductRating > avg → no hits
        assertTrue(shopService.searchItemsInShop(shopId, null, null, null, null, null, 6.0, token).isEmpty());
    }

    @Test
    void testSearchItemsInShop_ShopFetchError() throws Exception {
        String token = "tok";
        int shopId = 5;
        when(authTokenService.ValidateToken(token)).thenReturn(1);
        when(shopRepository.getShop(shopId)).thenThrow(new RuntimeException("no shop"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.searchItemsInShop(shopId, null, null, null, null, null, null, token));
        assertTrue(ex.getMessage().contains("searchItemsInShop"));
    }


    //
    // ----- addSupplyToItem(...)
    //

    @Test
    void testAddSupplyToItem_Success2() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(userService.hasPermission(1, PermissionsEnum.manageItems, 2)).thenReturn(true);

        doNothing().when(shopRepository).addSupplyToItem(2, 3, 4);
        shopService.addSupplyToItem(2, 3, 4, tok);
        verify(shopRepository).addSupplyToItem(2, 3, 4);
    }

    @Test
    void testAddSupplyToItem_NoPermission() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(userService.hasPermission(1, PermissionsEnum.manageItems, 2)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addSupplyToItem(2,3,4,tok));
        assertTrue(ex.getMessage().contains("permission"));
    }

    @Test
    void testAddSupplyToItem_RepoError() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(userService.hasPermission(1, PermissionsEnum.manageItems, 2)).thenReturn(true);
        doThrow(new RuntimeException("fail")).when(shopRepository).addSupplyToItem(2,3,4);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addSupplyToItem(2,3,4,tok));
        assertTrue(ex.getMessage().contains("Error adding supply"));
    }


    //
    // ----- removeDiscountForItem(...)
    //

    @Test
    void testRemoveDiscountForItem_Success2() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(userService.hasPermission(1, PermissionsEnum.setPolicy, 7)).thenReturn(true);

        doNothing().when(shopRepository).removeDiscountForItem(7, 99);
        shopService.removeDiscountForItem(7, 99, tok);
        verify(shopRepository).removeDiscountForItem(7, 99);
    }

    @Test
    void testRemoveDiscountForItem_NoPermission() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(userService.hasPermission(1, PermissionsEnum.setPolicy, 7)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.removeDiscountForItem(7,99,tok));
        assertTrue(ex.getMessage().contains("permission"));
    }

    @Test
    void testRemoveDiscountForItem_RepoError() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(userService.hasPermission(1, PermissionsEnum.setPolicy, 7)).thenReturn(true);
        doThrow(new IllegalArgumentException("oops"))
            .when(shopRepository).removeDiscountForItem(7, 99);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.removeDiscountForItem(7,99,tok));
        assertTrue(ex.getMessage().contains("Error removing discount for item"));
    }


    //
    // ----- addSupply(...)
    //

    @Test
    void testAddSupply_Success2() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(2);
        when(userService.hasPermission(2, PermissionsEnum.manageItems, 5)).thenReturn(true);

        doNothing().when(shopRepository).addSupply(5, 6, 7);
        shopService.addSupply(5,6,7,tok);
        verify(shopRepository).addSupply(5,6,7);
    }

    @Test
    void testAddSupply_NoPermission() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(2);
        when(userService.hasPermission(2, PermissionsEnum.manageItems, 5)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addSupply(5,6,7,tok));
        assertTrue(ex.getMessage().contains("permission"));
    }

    @Test
    void testAddSupply_RepoError() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(2);
        when(userService.hasPermission(2, PermissionsEnum.manageItems, 5)).thenReturn(true);
        doThrow(new RuntimeException("fail")).when(shopRepository).addSupply(5,6,7);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addSupply(5,6,7,tok));
        assertTrue(ex.getMessage().contains("Error adding supply"));
    }


    //
    // ----- addDiscountPolicy(...)
    //

    @Test
    void testAddDiscountPolicy_Success() throws Exception {
        String tok = "t";
        int shopId = 8;
        when(authTokenService.ValidateToken(tok)).thenReturn(3);
        when(userService.hasPermission(3, PermissionsEnum.setPolicy, shopId)).thenReturn(true);

        doNothing().when(shopRepository)
            .addDiscountPolicy(2, 10, ItemCategory.BOOKS, 50.0, Operator.AND, shopId);

        shopService.addDiscountPolicy(tok, 2, 10, ItemCategory.BOOKS, 50.0, Operator.AND, shopId);
        verify(shopRepository).addDiscountPolicy(2,10,ItemCategory.BOOKS,50.0,Operator.AND,shopId);
    }

    @Test
    void testAddDiscountPolicy_NoPermission() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(3);
        when(userService.hasPermission(3, PermissionsEnum.setPolicy, 8)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addDiscountPolicy(tok,2,10,ItemCategory.BOOKS,10,Operator.OR,8));
        assertTrue(ex.getMessage().contains("permission"));
    }

    @Test
    void testAddDiscountPolicy_InvalidToken_OurArg() {
        assertThrows(OurRuntime.class, () ->
            shopService.addDiscountPolicy("bad",1,2,null,0.0,Operator.OR,5)
        );
    }


    //
    // ----- getShopsByWorker(...)
    //

    @Test
    void testGetShopsByWorker_Success() throws Exception {
        String token = "tok";
        int workerId = 99;
        // userService.getShopIdsByWorkerId does not use token
        when(userService.getShopIdsByWorkerId(workerId)).thenReturn(List.of(1,2));

        Shop a = new Shop(1,"A",shippingMethod);
        Shop b = new Shop(2,"B",shippingMethod);
        // getShop is called with token
        when(authTokenService.ValidateToken(token)).thenReturn(10);
        when(shopRepository.getShop(1)).thenReturn(a);
        when(shopRepository.getShop(2)).thenReturn(b);

        var out = shopService.getShopsByWorker(workerId, token);
        assertEquals(2, out.size());
        assertTrue(out.containsAll(List.of(a,b)));
    }

    @Test
    void testGetShopsByWorker_GetShopFails() throws Exception {
        String tok = "tok";
        int workerId = 5;
        when(userService.getShopIdsByWorkerId(workerId)).thenReturn(List.of(42));
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.getShop(42)).thenThrow(new OurArg("nope"));

        OurArg ex = assertThrows(OurArg.class,
            () -> shopService.getShopsByWorker(workerId, tok));
        assertTrue(ex.getMessage().contains("getShopsByWorker"));
    }

}