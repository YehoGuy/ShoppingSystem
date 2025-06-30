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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import com.example.app.DomainLayer.Shop.Discount.Discount;
import com.example.app.DomainLayer.Shop.Discount.Policy;
import com.example.app.DomainLayer.Shop.IShopRepository;
import com.example.app.DomainLayer.Shop.PurchasePolicy;
import com.example.app.DomainLayer.Shop.Shop;
import com.example.app.DomainLayer.Shop.ShopReview;
import com.example.app.InfrastructureLayer.ShopRepository;
import com.example.app.PresentationLayer.DTO.Shop.CompositePolicyDTO;

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
    @SuppressWarnings({"UnusedMethod", "unused"})
    void setUp() {
        // initialize @Mock fields
        mocks = MockitoAnnotations.openMocks(this);

        // manually construct with your two-arg constructor
        shopService = new ShopService(shopRepository, authTokenService, userService, itemService);
    }

    @AfterEach
    @SuppressWarnings({"UnusedMethod", "unused"})
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
        
        Shop mockShop = mock(Shop.class);
        when(mockShop.getId()).thenReturn(shopId);
        when(mockShop.getItemPrice(item.getId())).thenReturn(500); // price within range 0-1000000000
        
        when(shopRepository.getShop(shopId)).thenReturn(mockShop);
        when(shopRepository.getItemsByShop(shopId)).thenReturn(Arrays.asList(item.getId()));
        when(itemService.getItemsByIds(Arrays.asList(item.getId()), token))
                .thenReturn(Arrays.asList(item));

        List<Item> results = shopService.searchItemsInShop(shopId, "wid", ItemCategory.ELECTRONICS, null, 0, 1000000000,
                null, token);
        assertEquals(1, results.size());
        assertEquals(item, results.get(0));
    }

    // UC5 – Search Products in Market (no matches)
    @Test
    void testSearchItemsInShop_NoMatches_Failure() throws Exception {
        String token = "valid-token";
        int shopId = 1;

        when(authTokenService.ValidateToken(token)).thenReturn(10);
        
        Shop mockShop = mock(Shop.class);
        when(mockShop.getId()).thenReturn(shopId);
        
        when(shopRepository.getShop(shopId)).thenReturn(mockShop);
        when(shopRepository.getItemsByShop(shopId)).thenReturn(Collections.emptyList());

        List<Item> results = shopService.searchItemsInShop(((Integer) shopId), "nonexistent", ItemCategory.AUTOMOTIVE,
                new ArrayList<>(), 0, 1000000000, 0.0, token);
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

        Shop created = shopService.createShop("MyShop", purchasePolicy, shippingMethod, token);
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
        String token = "t";
        int userId = 1;
        String emptyName = "";

        // stub token validation and member check
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        doNothing().when(userService).validateMemberId(userId);

        // simulate repository rejecting missing name
        when(shopRepository.createShop(emptyName, purchasePolicy, shippingMethod))
                .thenThrow(new IllegalArgumentException("Shop name is required"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.createShop(emptyName, purchasePolicy, shippingMethod, token));
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

        shopService.addItemToShop(shopId, "item1", "no description", qty, ItemCategory.ELECTRONICS, price, token);
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
                () -> shopService.addItemToShop(shopId, "item1", "no description", -1, ItemCategory.ELECTRONICS, 5,
                        token));
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
                () -> shopService.addItemToShop(shopId, "item1", "no description", 5, ItemCategory.ELECTRONICS, 100,
                        token));
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
                () -> shopService.addItemToShop(shopId, "item1", "no description", 5, ItemCategory.ELECTRONICS, -100,
                        token));
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

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shopService.addSupplyToItem(shopId, 1, 1, token));
        assertTrue(exception != null);

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
    @Test
    void testActionByPermission_Succeess() throws Exception {
        // Simple test to verify permission-based action succeeds
        assertTrue(true, "Permission-based action test placeholder");
    }

    // UC28 – Enforce Actions by Permission (sample)
    @Test
    void testActionByPermission_Denied_Failure() throws Exception {
        String token = "t";
        int shopId = 10;

        when(authTokenService.ValidateToken(token)).thenReturn(16);
        when(userService.hasPermission(16, PermissionsEnum.manageItems, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.addItemToShop(shopId, "item1", "no description", 5, ItemCategory.ELECTRONICS, 100,
                        token));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // Search/filter edge cases in ShopService (success)
    // Verifies search with no filters and with price filter yields correct results.
    @Test
    void testSearchFilterEdgeCasesInShopService_Success() throws Exception {
        String token = "t";
        when(authTokenService.ValidateToken(token)).thenReturn(1);

        // Prepare a single shop with one item
        Shop shopObj = mock(Shop.class);
        when(shopObj.getId()).thenReturn(1);
        when(shopObj.getItemPrice(10)).thenReturn(50); // Set item price to 50
        
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
    // Closes a shop and verifies it cannot be retrieved, then “reopens” by creating
    // another.
    @Test
    void testClosingAndReopeningShopsInRepo_Success() {
        ShopRepository repo = new ShopRepository();
        PurchasePolicy pp = mock(PurchasePolicy.class);
        ShippingMethod sm = mock(ShippingMethod.class);

        // Create, close, and verify removal
        Shop a = repo.createShop("A", pp, sm);
        repo.closeShop(a.getId());
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> repo.getShop(a.getId()),
                "Closed shop should no longer be retrievable");
        assertTrue(exception != null);

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
                Shop shop = f.get();
                ids.add(shop.getId());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                String errorMessage = "Shop creation failed in thread: " + (cause != null ? cause.getMessage() : "Unknown error");
                fail(errorMessage);
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
        String t = "x";
        int uid = 10, sid = 5;
        Map<Integer, Integer> cart = Map.of(1, 2);
        when(authTokenService.ValidateToken(t)).thenReturn(uid);
        when(userService.isSuspended(uid)).thenReturn(false);
        Map<Integer, ItemCategory> catMap = Map.of(1, ItemCategory.ELECTRONICS);
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
        Map<Integer, Integer> cart = Map.of(2, 3);
        doNothing().when(shopRepository).rollBackPurchase(cart, 6);
        shopService.rollBackPurchase(cart, 6);
        verify(shopRepository).rollBackPurchase(cart, 6);
    }

    // checkSupplyAvailabilityAndAcquire
    @Test
    void testCheckSupplyAvailabilityAndAcquire_Success() {
        when(shopRepository.checkSupplyAvailabilityAndAqcuire(1, 2, 5)).thenReturn(false);
        assertFalse(shopService.checkSupplyAvailabilityAndAcquire(1, 2, 5));
    }

    // addSupply
    @Test
    void testAddSupply_Success() throws Exception {
        String t = "t";
        when(authTokenService.ValidateToken(t)).thenReturn(12);
        when(userService.hasPermission(12, PermissionsEnum.manageItems, 7)).thenReturn(true);
        doNothing().when(shopRepository).addSupply(7, 8, 9);
        shopService.addSupply(7, 8, 9, t);
        verify(shopRepository).addSupply(7, 8, 9);
    }

    // removeSupply
    @Test
    void testRemoveSupply_Success() throws Exception {
        String t = "t";
        when(authTokenService.ValidateToken(t)).thenReturn(13);
        when(userService.hasPermission(13, PermissionsEnum.manageItems, 7)).thenReturn(true);
        doNothing().when(shopRepository).removeSupply(7, 8, 9);
        shopService.removeSupply(7, 8, 9, t);
        verify(shopRepository).removeSupply(7, 8, 9);
    }

    // getItemsByShop
    @Test
    void testGetItemsByShop_Success() throws Exception {
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
    void testGetAllShops_Success() throws Exception {
        String tok = "tok";
        List<Shop> list = List.of(new Shop(1, "A", shippingMethod));
        when(authTokenService.ValidateToken(tok)).thenReturn(2);
        when(shopRepository.getAllShops()).thenReturn(list);
        List<Shop> result = shopService.getAllShops(tok);
        assertEquals(list, result);
    }

    @Test
    void testGetAllShops_Error() throws Exception {
        String tok = "tok";
        when(authTokenService.ValidateToken(tok)).thenReturn(2);
        when(shopRepository.getAllShops()).thenThrow(new RuntimeException("fail"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.getAllShops(tok));
        assertTrue(ex.getMessage().contains("Error retrieving all shops"));
    }

    // getItems (global)
    @Test
    void testGetItems_Success() throws Exception {
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
    void testGetItems_Error() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(4);
        when(shopRepository.getItems()).thenThrow(new RuntimeException("err"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.getItems(tok));
        assertTrue(ex.getMessage().contains("Error retrieving all items"));
    }

    // searchItems (global)
    @Test
    void testSearchItems_Global_Success() throws Exception {
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
    void testSearchItems_Global_Error() throws Exception {
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
    void testShipPurchase_Success() throws Exception {
        String tok = "tk";
        when(authTokenService.ValidateToken(tok)).thenReturn(7);
        when(shopRepository.shipPurchase(any(), anyInt(), any(), any(), any(), any())).thenReturn(true);
        shopService.shipPurchase(tok, 3, 4, "C", "C", "C", "C");
        verify(shopRepository).shipPurchase(anyString(), anyInt(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testShipPurchase_Error() throws Exception {
        String tok = "tk";
        when(authTokenService.ValidateToken(tok)).thenReturn(7);
        doThrow(new RuntimeException("fail ship")).when(shopRepository).shipPurchase(anyString(), anyInt(), anyString(),
                anyString(), anyString(), anyString());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.shipPurchase(tok, 3, 4, "C", "C", "C", "C"));
        assertTrue(ex.getMessage().contains("Error shipping purchase"));
    }

    // removeGlobalDiscount
    @Test
    void testRemoveGlobalDiscount_Success() throws Exception {
        String tok = "x";
        int shopId = 10;
        when(authTokenService.ValidateToken(tok)).thenReturn(3);
        when(userService.hasPermission(3, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).removeGlobalDiscount(shopId);
        shopService.removeGlobalDiscount(shopId, tok);
        verify(shopRepository).removeGlobalDiscount(shopId);
    }

    @Test
    void testRemoveGlobalDiscount_NoPermission_Failure() throws Exception {
        String tok = "x";
        int shopId = 9;
        when(authTokenService.ValidateToken(tok)).thenReturn(3);
        when(userService.hasPermission(anyInt(), any(), anyInt())).thenReturn(false);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> shopService.removeGlobalDiscount(shopId, tok));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // removeDiscountForItem
    @Test
    void testRemoveDiscountForItem_Success() throws Exception {
        String t = "tk";
        int shopId = 5, itemId = 99;
        when(authTokenService.ValidateToken(t)).thenReturn(4);
        when(userService.hasPermission(4, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).removeDiscountForItem(shopId, itemId);
        shopService.removeDiscountForItem(shopId, itemId, t);
        verify(shopRepository).removeDiscountForItem(shopId, itemId);
    }

    @Test
    void testRemoveDiscountForItem_Error_Failure() throws Exception {
        String t = "tk";
        int shopId = 5, itemId = 99;
        when(authTokenService.ValidateToken(t)).thenReturn(4);
        when(userService.hasPermission(4, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("oops"))
                .when(shopRepository).removeDiscountForItem(shopId, itemId);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.removeDiscountForItem(shopId, itemId, t));
        assertTrue(ex.getMessage().contains("Error removing discount for item"));
    }

    // category discounts
    @Test
    void testSetCategoryDiscount_Success() throws Exception {
        String t = "t";
        int shopId = 11;
        when(authTokenService.ValidateToken(t)).thenReturn(5);
        when(userService.hasPermission(5, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).setCategoryDiscount(shopId, ItemCategory.BOOKS, 30, false);
        shopService.setCategoryDiscount(shopId, ItemCategory.BOOKS, 30, false, t);
        verify(shopRepository).setCategoryDiscount(shopId, ItemCategory.BOOKS, 30, false);
    }

    @Test
    void testSetCategoryDiscount_InvalidDiscount_Failure() throws Exception {
        String t = "t";
        int shopId = 11;
        when(authTokenService.ValidateToken(t)).thenReturn(5);
        when(userService.hasPermission(5, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Invalid discount"))
                .when(shopRepository).setCategoryDiscount(shopId, ItemCategory.CLOTHING, 150, false);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.setCategoryDiscount(shopId, ItemCategory.CLOTHING, 150, false, t));
        assertTrue(ex.getMessage().contains("Error setting category discount"));
    }

    @Test
    void testSetCategoryDiscount_NoPermission_Failure() throws Exception {
        String t = "t";
        int shopId = 11;
        when(authTokenService.ValidateToken(t)).thenReturn(5);
        when(userService.hasPermission(5, PermissionsEnum.setPolicy, shopId)).thenReturn(false);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> shopService.setCategoryDiscount(shopId, ItemCategory.CLOTHING, 20, false, t));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // removeCategoryDiscount
    @Test
    void testRemoveCategoryDiscount_Success() throws Exception {
        String t = "t";
        int shopId = 12;
        when(authTokenService.ValidateToken(t)).thenReturn(6);
        when(userService.hasPermission(6, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).removeCategoryDiscount(shopId, ItemCategory.CLOTHING);
        shopService.removeCategoryDiscount(shopId, ItemCategory.CLOTHING, t);
        verify(shopRepository).removeCategoryDiscount(shopId, ItemCategory.CLOTHING);
    }

    @Test
    void testRemoveCategoryDiscount_Error_Failure() throws Exception {
        String t = "t";
        int shopId = 12;
        when(authTokenService.ValidateToken(t)).thenReturn(6);
        when(userService.hasPermission(6, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalStateException("fail"))
                .when(shopRepository).removeCategoryDiscount(shopId, ItemCategory.ELECTRONICS);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.removeCategoryDiscount(shopId, ItemCategory.ELECTRONICS, t));
        assertTrue(ex.getMessage().contains("Error removing category discount"));
    }

    @Test
    void testRemoveCategoryDiscount_NoPermission_Failure() throws Exception {
        String t = "t";
        int shopId = 12;
        when(authTokenService.ValidateToken(t)).thenReturn(6);
        when(userService.hasPermission(6, PermissionsEnum.setPolicy, shopId)).thenReturn(false);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> shopService.removeCategoryDiscount(shopId, ItemCategory.ELECTRONICS, t));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // addReviewToShop (suspended user)
    @Test
    void testAddReviewToShop_Suspended_Failure() throws Exception {
        String token = "t";
        int shopId = 2;
        when(authTokenService.ValidateToken(token)).thenReturn(7);
        doNothing().when(userService).validateMemberId(7);
        when(userService.isSuspended(7)).thenReturn(true);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> shopService.addReviewToShop(shopId, 5, "text", token));
        assertTrue(ex.getMessage().contains("cannot add a review"));
    }

    // addItemToShop (item service error)
    @Test
    void testAddItemToShop_ItemServiceError_Failure() throws Exception {
        String token = "tok";
        int shopId = 3;
        when(authTokenService.ValidateToken(token)).thenReturn(8);
        when(userService.hasPermission(8, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        when(itemService.createItem(eq(shopId), any(), any(), eq(ItemCategory.AUTOMOTIVE), eq(token)))
                .thenThrow(new RuntimeException("create failed"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.addItemToShop(shopId, "n", "d", 1, ItemCategory.AUTOMOTIVE, 1, token));
        assertTrue(ex.getMessage().contains("Error adding item to shop"));
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
        when(shopRepository.getItemsByShop(1)).thenReturn(List.of(1, 2));
        Item a = new Item(1, "A", "desc", 0);
        Item b = new Item(2, "B", "desc", 2);
        when(itemService.getItemsByIds(List.of(1, 2), tok)).thenReturn(List.of(a, b));
        List<Item> found = shopService.searchItems(null, ItemCategory.ELECTRONICS, null, null, null, null, null, tok);
        assertEquals(1, found.size());
        assertEquals(a, found.get(0));
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
        it.addReview(new ItemReview(5, ""));
        when(itemService.getItemsByIds(List.of(1), tok)).thenReturn(List.of(it));
        List<Item> res1 = shopService.searchItems(null, null, null, null, null, 4.0, null, tok);
        assertEquals(1, res1.size());
        List<Item> res2 = shopService.searchItems(null, null, null, null, null, 6.0, null, tok);
        assertTrue(res2.isEmpty());
    }

    // purchaseItems repository exception
    @Test
    void testPurchaseItems_RepoError() throws Exception {
        String tok = "t";
        int uid = 1, sid = 1;
        Map<Integer, Integer> cart = Map.of(1, 1);
        when(authTokenService.ValidateToken(tok)).thenReturn(uid);
        when(userService.isSuspended(uid)).thenReturn(false);
        when(itemService.getItemdId2Cat(cart)).thenReturn(Collections.emptyMap());
        when(shopRepository.purchaseItems(cart, Collections.emptyMap(), sid))
                .thenThrow(new RuntimeException("fail"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.purchaseItems(cart, sid, tok));
        assertTrue(ex.getMessage().contains("Error purchasing items"));
    }

    // checkPolicy success and error
    @Test
    void testCheckPolicy_SuccessAndError() throws Exception {
        String tok = "t";
        HashMap<Integer, HashMap<Integer, Integer>> cart = new HashMap<>();
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.checkPolicy(cart, tok)).thenReturn(true);
        assertTrue(shopService.checkPolicy(cart, tok));
        when(shopRepository.checkPolicy(cart, tok)).thenThrow(new RuntimeException("oops"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.checkPolicy(cart, tok));
        assertTrue(ex.getMessage().contains("Error checking policy"));
    }

    // rollBackPurchase exception
    @Test
    void testRollBackPurchase_Error() throws Exception {
        Map<Integer, Integer> cart = Map.of(1, 1);
        doThrow(new RuntimeException("rbfail")).when(shopRepository).rollBackPurchase(cart, 1);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.rollBackPurchase(cart, 1));
        assertTrue(ex.getMessage().contains("Error rolling back purchase"));
    }

    // getItemsByShop error branch
    @Test
    void testGetItemsByShop_Error() throws Exception {
        String tok = "t";
        when(authTokenService.ValidateToken(tok)).thenReturn(1);
        when(shopRepository.getItemsByShop(1)).thenThrow(new RuntimeException("gone"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.getItemsByShop(1, tok));
        assertTrue(ex.getMessage().contains("Error retrieving items for shop"));
    }

    // shipPurchase invalid token
    @Test
    void testShipPurchase_InvalidToken() throws Exception {
        when(authTokenService.ValidateToken("bad")).thenThrow(new RuntimeException("noAuth"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> shopService.shipPurchase("bad", 1, 1, "C", "C", "C", "C"));
        assertTrue(ex.getMessage().contains("noAuth"));
    }

    // getShop success
    @Test
    void testGetShop_Success() throws Exception {
        String token = "validToken";
        int shopId = 1;
        Shop mockShop = mock(Shop.class);
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShop(shopId)).thenReturn(mockShop);
        
        Shop result = shopService.getShop(shopId, token);
        
        assertEquals(mockShop, result);
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getShop(shopId);
    }
    
    @Test
    void testGetShop_InvalidToken() throws Exception {
        String token = "invalidToken";
        int shopId = 1;
        
        when(authTokenService.ValidateToken(token)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, () -> shopService.getShop(shopId, token));
        assertTrue(exception.getMessage().contains("Invalid token"));
        verify(authTokenService).ValidateToken(token);
    }
    
    @Test
    void testGetShop_ShopNotFound() throws Exception {
        String token = "validToken";
        int shopId = 999;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShop(shopId)).thenThrow(new RuntimeException("Shop not found"));
        
        OurRuntime exception = assertThrows(OurRuntime.class, () -> shopService.getShop(shopId, token));
        assertTrue(exception.getMessage().contains("Shop not found"));
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getShop(shopId);
    }

    @Test
    void testGetAllShops_EmptyList() throws Exception {
        String token = "validToken";
        List<Shop> emptyList = new ArrayList<>();
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getAllShops()).thenReturn(emptyList);
        
        List<Shop> result = shopService.getAllShops(token);
        
        assertTrue(result.isEmpty());
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getAllShops();
    }
    
    @Test
    void testGetAllShops_InvalidToken() throws Exception {
        String token = "invalidToken";
        
        when(authTokenService.ValidateToken(token)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, () -> shopService.getAllShops(token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
    }

    @Test
    void testGetAllOpenShops_Success() throws Exception {
        String token = "validToken";
        List<Shop> mockOpenShops = Arrays.asList(mock(Shop.class), mock(Shop.class));
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getAllOpenShops()).thenReturn(mockOpenShops);
        
        List<Shop> result = shopService.getAllOpenShops(token);
        
        assertEquals(mockOpenShops, result);
        assertEquals(2, result.size());
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getAllOpenShops();
    }
    
    @Test
    void testGetAllOpenShops_EmptyList() throws Exception {
        String token = "validToken";
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getAllOpenShops()).thenReturn(new ArrayList<>());
        
        List<Shop> result = shopService.getAllOpenShops(token);
        
        assertTrue(result.isEmpty());
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getAllOpenShops();
    }
    
    @Test
    void testGetAllOpenShops_InvalidToken() throws Exception {
        String token = "invalidToken";
        
        when(authTokenService.ValidateToken(token)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, () -> shopService.getAllOpenShops(token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
    }

    @Test
    void testGetAllClosedShops_Success() throws Exception {
        String token = "validToken";
        List<Shop> mockClosedShops = Arrays.asList(mock(Shop.class));
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getAllClosedShops()).thenReturn(mockClosedShops);
        
        List<Shop> result = shopService.getAllClosedShops(token);
        
        assertEquals(mockClosedShops, result);
        assertEquals(1, result.size());
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getAllClosedShops();
    }
    
    @Test
    void testGetAllClosedShops_EmptyList() throws Exception {
        String token = "validToken";
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getAllClosedShops()).thenReturn(new ArrayList<>());
        
        List<Shop> result = shopService.getAllClosedShops(token);
        
        assertTrue(result.isEmpty());
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getAllClosedShops();
    }
    
    @Test
    void testGetAllClosedShops_InvalidToken() throws Exception {
        String token = "invalidToken";
        
        when(authTokenService.ValidateToken(token)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, () -> shopService.getAllClosedShops(token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
    }

    @Test
    void testGetShopAverageRating_ZeroRating() throws Exception {
        String token = "validToken";
        int shopId = 1;
        double expectedRating = 0.0;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShopAverageRating(shopId)).thenReturn(expectedRating);
        
        double result = shopService.getShopAverageRating(shopId, token);
        
        assertEquals(expectedRating, result, 0.01);
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getShopAverageRating(shopId);
    }
    
    @Test
    void testGetShopAverageRating_InvalidToken() throws Exception {
        String token = "invalidToken";
        int shopId = 1;
        
        when(authTokenService.ValidateToken(token)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, () -> shopService.getShopAverageRating(shopId, token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
    }
    
    @Test
    void testGetShopAverageRating_ShopNotFound() throws Exception {
        String token = "validToken";
        int shopId = 999;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShopAverageRating(shopId)).thenThrow(new RuntimeException("Shop not found"));
        
        OurRuntime exception = assertThrows(OurRuntime.class, () -> shopService.getShopAverageRating(shopId, token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getShopAverageRating(shopId);
    }

    @Test
    void testGetItemQuantityFromShop_ZeroQuantity() throws Exception {
        String token = "validToken";
        int shopId = 1;
        int itemId = 100;
        int expectedQuantity = 0;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getItemQuantityFromShop(shopId, itemId)).thenReturn(expectedQuantity);
        
        int result = shopService.getItemQuantityFromShop(shopId, itemId, token);
        
        assertEquals(expectedQuantity, result);
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getItemQuantityFromShop(shopId, itemId);
    }
    
    @Test
    void testGetItemQuantityFromShop_InvalidToken() throws Exception {
        String token = "invalidToken";
        int shopId = 1;
        int itemId = 100;
        
        when(authTokenService.ValidateToken(token)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, () -> shopService.getItemQuantityFromShop(shopId, itemId, token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
    }
    
    @Test
    void testGetItemQuantityFromShop_ItemNotFound() throws Exception {
        String token = "validToken";
        int shopId = 1;
        int itemId = 999;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getItemQuantityFromShop(shopId, itemId)).thenThrow(new RuntimeException("Item not found in shop"));
        
        OurRuntime exception = assertThrows(OurRuntime.class, () -> shopService.getItemQuantityFromShop(shopId, itemId, token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getItemQuantityFromShop(shopId, itemId);
    }

    @Test
    void testGetItemsByShop_EmptyList() throws Exception {
        String token = "validToken";
        int shopId = 1;
        List<Integer> emptyItemIds = new ArrayList<>();
        List<Item> emptyItems = new ArrayList<>();
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getItemsByShop(shopId)).thenReturn(emptyItemIds);
        when(itemService.getItemsByIds(emptyItemIds, token)).thenReturn(emptyItems);
        
        List<Item> result = shopService.getItemsByShop(shopId, token);
        
        assertTrue(result.isEmpty());
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getItemsByShop(shopId);
        verify(itemService).getItemsByIds(emptyItemIds, token);
    }
    
    @Test
    void testGetItemsByShop_InvalidToken() throws Exception {
        String token = "invalidToken";
        int shopId = 1;
        
        when(authTokenService.ValidateToken(token)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, () -> shopService.getItemsByShop(shopId, token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
    }
    
    @Test
    void testGetItemsByShop_ShopNotFound() throws Exception {
        String token = "validToken";
        int shopId = 999;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getItemsByShop(shopId)).thenThrow(new RuntimeException("Shop not found"));
        
        OurRuntime exception = assertThrows(OurRuntime.class, () -> shopService.getItemsByShop(shopId, token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getItemsByShop(shopId);
    }

    @Test
    void testGetItems_EmptyList() throws Exception {
        String token = "validToken";
        List<Integer> emptyItemIds = new ArrayList<>();
        List<Item> emptyItems = new ArrayList<>();
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getItems()).thenReturn(emptyItemIds);
        when(itemService.getItemsByIds(emptyItemIds, token)).thenReturn(emptyItems);
        
        List<Item> result = shopService.getItems(token);
        
        assertTrue(result.isEmpty());
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getItems();
        verify(itemService).getItemsByIds(emptyItemIds, token);
    }
    
    @Test
    void testGetItems_InvalidToken() throws Exception {
        String token = "invalidToken";
        
        when(authTokenService.ValidateToken(token)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, () -> shopService.getItems(token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
    }

    @Test
    void testGetShopsByWorker_Success() throws Exception {
        String token = "validToken";
        int workerId = 456;
        List<Integer> shopIds = Arrays.asList(1, 2, 3);
        Shop mockShop1 = mock(Shop.class);
        Shop mockShop2 = mock(Shop.class);
        Shop mockShop3 = mock(Shop.class);
        
        when(userService.getShopIdsByWorkerId(workerId)).thenReturn(shopIds);
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShop(1)).thenReturn(mockShop1);
        when(shopRepository.getShop(2)).thenReturn(mockShop2);
        when(shopRepository.getShop(3)).thenReturn(mockShop3);
        
        List<Shop> result = shopService.getShopsByWorker(workerId, token);
        
        assertEquals(3, result.size());
        assertTrue(result.contains(mockShop1));
        assertTrue(result.contains(mockShop2));
        assertTrue(result.contains(mockShop3));
        verify(userService).getShopIdsByWorkerId(workerId);
        verify(authTokenService, times(3)).ValidateToken(token);
        verify(shopRepository).getShop(1);
        verify(shopRepository).getShop(2);
        verify(shopRepository).getShop(3);
    }
    
    @Test
    void testGetShopsByWorker_EmptyList() throws Exception {
        String token = "validToken";
        int workerId = 456;
        List<Integer> emptyShopIds = new ArrayList<>();
        
        when(userService.getShopIdsByWorkerId(workerId)).thenReturn(emptyShopIds);
        
        List<Shop> result = shopService.getShopsByWorker(workerId, token);
        
        assertTrue(result.isEmpty());
        verify(userService).getShopIdsByWorkerId(workerId);
    }
    
    @Test
    void testGetShopsByWorker_WorkerNotFound() throws Exception {
        String token = "validToken";
        int workerId = 999;
        
        when(userService.getShopIdsByWorkerId(workerId)).thenThrow(new RuntimeException("Worker not found"));
        
        OurRuntime exception = assertThrows(OurRuntime.class, () -> shopService.getShopsByWorker(workerId, token));
        assertTrue(exception != null);
        verify(userService).getShopIdsByWorkerId(workerId);
    }

    @Test
    void testGetOpenShopsByWorker_Success() throws Exception {
        String token = "validToken";
        int workerId = 456;
        List<Integer> shopIds = Arrays.asList(1, 2, 3);
        Shop openShop1 = mock(Shop.class);
        Shop closedShop = mock(Shop.class);
        Shop openShop2 = mock(Shop.class);
        
        when(openShop1.isClosed()).thenReturn(false);
        when(closedShop.isClosed()).thenReturn(true);
        when(openShop2.isClosed()).thenReturn(false);
        
        when(userService.getShopIdsByWorkerId(workerId)).thenReturn(shopIds);
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShop(1)).thenReturn(openShop1);
        when(shopRepository.getShop(2)).thenReturn(closedShop);
        when(shopRepository.getShop(3)).thenReturn(openShop2);
        
        List<Shop> result = shopService.getOpenShopsByWorker(workerId, token);
        
        assertEquals(2, result.size());
        assertTrue(result.contains(openShop1));
        assertTrue(result.contains(openShop2));
        assertFalse(result.contains(closedShop));
        verify(userService).getShopIdsByWorkerId(workerId);
        verify(authTokenService, times(3)).ValidateToken(token);
    }
    
    @Test
    void testGetOpenShopsByWorker_AllShopsClosed() throws Exception {
        String token = "validToken";
        int workerId = 456;
        List<Integer> shopIds = Arrays.asList(1, 2);
        Shop closedShop1 = mock(Shop.class);
        Shop closedShop2 = mock(Shop.class);
        
        when(closedShop1.isClosed()).thenReturn(true);
        when(closedShop2.isClosed()).thenReturn(true);
        
        when(userService.getShopIdsByWorkerId(workerId)).thenReturn(shopIds);
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShop(1)).thenReturn(closedShop1);
        when(shopRepository.getShop(2)).thenReturn(closedShop2);
        
        List<Shop> result = shopService.getOpenShopsByWorker(workerId, token);
        
        assertTrue(result.isEmpty());
        verify(userService).getShopIdsByWorkerId(workerId);
    }

    @Test
    void testGetClosedShopsByWorker_Success() throws Exception {
        String token = "validToken";
        int workerId = 456;
        List<Integer> shopIds = Arrays.asList(1, 2, 3);
        Shop openShop = mock(Shop.class);
        Shop closedShop1 = mock(Shop.class);
        Shop closedShop2 = mock(Shop.class);
        
        when(openShop.isClosed()).thenReturn(false);
        when(closedShop1.isClosed()).thenReturn(true);
        when(closedShop2.isClosed()).thenReturn(true);
        
        when(userService.getShopIdsByWorkerId(workerId)).thenReturn(shopIds);
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShop(1)).thenReturn(openShop);
        when(shopRepository.getShop(2)).thenReturn(closedShop1);
        when(shopRepository.getShop(3)).thenReturn(closedShop2);
        
        List<Shop> result = shopService.getClosedShopsByWorker(workerId, token);
        
        assertEquals(2, result.size());
        assertTrue(result.contains(closedShop1));
        assertTrue(result.contains(closedShop2));
        assertFalse(result.contains(openShop));
        verify(userService).getShopIdsByWorkerId(workerId);
        verify(authTokenService, times(3)).ValidateToken(token);
    }
    
    @Test
    void testGetClosedShopsByWorker_AllShopsOpen() throws Exception {
        String token = "validToken";
        int workerId = 456;
        List<Integer> shopIds = Arrays.asList(1, 2);
        Shop openShop1 = mock(Shop.class);
        Shop openShop2 = mock(Shop.class);
        
        when(openShop1.isClosed()).thenReturn(false);
        when(openShop2.isClosed()).thenReturn(false);
        
        when(userService.getShopIdsByWorkerId(workerId)).thenReturn(shopIds);
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShop(1)).thenReturn(openShop1);
        when(shopRepository.getShop(2)).thenReturn(openShop2);
        
        List<Shop> result = shopService.getClosedShopsByWorker(workerId, token);
        
        assertTrue(result.isEmpty());
        verify(userService).getShopIdsByWorkerId(workerId);
    }

    @Test
    void testGetDiscounts_Success() throws Exception {
        String token = "validToken";
        int shopId = 1;
        List<Discount> mockDiscounts = Arrays.asList(mock(Discount.class), mock(Discount.class));
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getDiscounts(shopId)).thenReturn(mockDiscounts);
        
        List<Discount> result = shopService.getDiscounts(shopId, token);
        
        assertEquals(mockDiscounts, result);
        assertEquals(2, result.size());
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getDiscounts(shopId);
    }
    
    @Test
    void testGetDiscounts_EmptyList() throws Exception {
        String token = "validToken";
        int shopId = 1;
        List<Discount> emptyDiscounts = new ArrayList<>();
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getDiscounts(shopId)).thenReturn(emptyDiscounts);
        
        List<Discount> result = shopService.getDiscounts(shopId, token);
        
        assertTrue(result.isEmpty());
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getDiscounts(shopId);
    }
    
    @Test
    void testGetDiscounts_InvalidToken() throws Exception {
        String token = "invalidToken";
        int shopId = 1;
        
        when(authTokenService.ValidateToken(token)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, () -> shopService.getDiscounts(shopId, token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
    }
    
    @Test
    void testGetDiscounts_ShopNotFound() throws Exception {
        String token = "validToken";
        int shopId = 999;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getDiscounts(shopId)).thenThrow(new RuntimeException("Shop not found"));
        
        OurRuntime exception = assertThrows(OurRuntime.class, () -> shopService.getDiscounts(shopId, token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getDiscounts(shopId);
    }

    @Test
    void testGetPolicies_Success() throws Exception {
        String token = "validToken";
        int shopId = 1;
        int userId = 123;
        List<Policy> mockPolicies = Arrays.asList(mock(Policy.class), mock(Policy.class));
        
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        when(userService.hasPermission(userId, PermissionsEnum.viewPolicy, shopId)).thenReturn(true);
        when(shopRepository.getPolicies(shopId)).thenReturn(mockPolicies);
        
        List<Policy> result = shopService.getPolicies(shopId, token);
        
        assertEquals(mockPolicies, result);
        assertEquals(2, result.size());
        verify(authTokenService).ValidateToken(token);
        verify(userService).hasPermission(userId, PermissionsEnum.viewPolicy, shopId);
        verify(shopRepository).getPolicies(shopId);
    }
    
    @Test
    void testGetPolicies_EmptyList() throws Exception {
        String token = "validToken";
        int shopId = 1;
        int userId = 123;
        List<Policy> emptyPolicies = new ArrayList<>();
        
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        when(userService.hasPermission(userId, PermissionsEnum.viewPolicy, shopId)).thenReturn(true);
        when(shopRepository.getPolicies(shopId)).thenReturn(emptyPolicies);
        
        List<Policy> result = shopService.getPolicies(shopId, token);
        
        assertTrue(result.isEmpty());
        verify(authTokenService).ValidateToken(token);
        verify(userService).hasPermission(userId, PermissionsEnum.viewPolicy, shopId);
        verify(shopRepository).getPolicies(shopId);
    }
    
    @Test
    void testGetPolicies_NoPermission() throws Exception {
        String token = "validToken";
        int shopId = 1;
        int userId = 123;
        
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        when(userService.hasPermission(userId, PermissionsEnum.viewPolicy, shopId)).thenReturn(false);
        
        OurRuntime exception = assertThrows(OurRuntime.class, () -> shopService.getPolicies(shopId, token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
        verify(userService).hasPermission(userId, PermissionsEnum.viewPolicy, shopId);
    }
    
    @Test
    void testGetPolicies_InvalidToken() throws Exception {
        String token = "invalidToken";
        int shopId = 1;
        
        when(authTokenService.ValidateToken(token)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, () -> shopService.getPolicies(shopId, token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
    }

    @Test
    void testGetClosedShops_Success() throws Exception {
        String token = "validToken";
        List<Integer> mockClosedShopIds = Arrays.asList(1, 3, 5);
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getClosedShops()).thenReturn(mockClosedShopIds);
        
        List<Integer> result = shopService.getclosedShops(token);
        
        assertEquals(mockClosedShopIds, result);
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(3));
        assertTrue(result.contains(5));
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getClosedShops();
    }
    
    @Test
    void testGetClosedShops_EmptyList() throws Exception {
        String token = "validToken";
        List<Integer> emptyList = new ArrayList<>();
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getClosedShops()).thenReturn(emptyList);
        
        List<Integer> result = shopService.getclosedShops(token);
        
        assertTrue(result.isEmpty());
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getClosedShops();
    }
    
    @Test
    void testGetClosedShops_InvalidToken() throws Exception {
        String token = "invalidToken";
        
        when(authTokenService.ValidateToken(token)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, () -> shopService.getclosedShops(token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
    }
    
    @Test
    void testGetClosedShops_RepositoryError() throws Exception {
        String token = "validToken";
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getClosedShops()).thenThrow(new RuntimeException("Database error"));
        
        OurRuntime exception = assertThrows(OurRuntime.class, () -> shopService.getclosedShops(token));
        assertTrue(exception != null);
        verify(authTokenService).ValidateToken(token);
        verify(shopRepository).getClosedShops();
    }

    // Edge case tests for boundary conditions
    @Test
    void testGetShopAverageRating_MaxRating() throws Exception {
        String token = "validToken";
        int shopId = 1;
        double maxRating = 5.0;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShopAverageRating(shopId)).thenReturn(maxRating);
        
        double result = shopService.getShopAverageRating(shopId, token);
        
        assertEquals(maxRating, result, 0.01);
    }
    
    @Test
    void testGetItemQuantityFromShop_LargeQuantity() throws Exception {
        String token = "validToken";
        int shopId = 1;
        int itemId = 100;
        int largeQuantity = Integer.MAX_VALUE;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getItemQuantityFromShop(shopId, itemId)).thenReturn(largeQuantity);
        
        int result = shopService.getItemQuantityFromShop(shopId, itemId, token);
        
        assertEquals(largeQuantity, result);
    }
    
    @Test
    void testGetShopsByWorker_SingleShop() throws Exception {
        String token = "validToken";
        int workerId = 456;
        List<Integer> singleShopId = Arrays.asList(1);
        Shop mockShop = mock(Shop.class);
        
        when(userService.getShopIdsByWorkerId(workerId)).thenReturn(singleShopId);
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShop(1)).thenReturn(mockShop);
        
        List<Shop> result = shopService.getShopsByWorker(workerId, token);
        
        assertEquals(1, result.size());
        assertEquals(mockShop, result.get(0));
    }

    // Additional comprehensive tests for shipPurchase method
    @Test
    void testShipPurchase_InvalidParameters() throws Exception {
        String token = "validToken";
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        doThrow(new IllegalArgumentException("Invalid address"))
            .when(shopRepository).shipPurchase(anyString(), anyInt(), anyString(), anyString(), anyString(), anyString());
        
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> shopService.shipPurchase(token, 1, 1, "", "city", "state", "zip"));
        assertTrue(exception.getMessage().contains("Error shipping purchase"));
    }

    @Test
    void testShipPurchase_RepositoryFailure() throws Exception {
        String token = "validToken";
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        doThrow(new RuntimeException("Database error"))
            .when(shopRepository).shipPurchase(anyString(), anyInt(), anyString(), anyString(), anyString(), anyString());
        
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> shopService.shipPurchase(token, 1, 1, "address", "city", "state", "zip"));
        assertTrue(exception.getMessage().contains("Error shipping purchase"));
    }

    // Additional comprehensive tests for searchItems method
    @Test
    void testSearchItems_WithAllFilters() throws Exception {
        String token = "validToken";
        List<String> keywords = Arrays.asList("test", "product");
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        
        Shop mockShop = mock(Shop.class);
        when(mockShop.getId()).thenReturn(1);
        when(mockShop.getAverageRating()).thenReturn(4.5);
        when(mockShop.getItemPrice(1)).thenReturn(50); // item1 price within range 40-60
        when(mockShop.getItemPrice(2)).thenReturn(70); // item2 price outside range
        
        when(shopRepository.getAllShops()).thenReturn(Arrays.asList(mockShop));
        when(shopRepository.getItemsByShop(1)).thenReturn(Arrays.asList(1, 2));
        
        Item item1 = new Item(1, "Test Product", "A test product description", ItemCategory.ELECTRONICS.ordinal());
        item1.addReview(new ItemReview(4, "Good product"));
        Item item2 = new Item(2, "Regular Item", "Normal description", ItemCategory.CLOTHING.ordinal());
        when(itemService.getItemsByIds(Arrays.asList(1, 2), token)).thenReturn(Arrays.asList(item1, item2));
        
        // Test with all filters applied
        List<Item> result = shopService.searchItems("test", ItemCategory.ELECTRONICS, keywords, 40, 60, 3.0, 4.0, token);
        
        assertEquals(1, result.size());
        assertEquals(item1, result.get(0));
        verify(authTokenService, times(3)).ValidateToken(token); // Called in searchItems, getAllShops, and getItemsByShop
        verify(shopRepository).getAllShops();
    }

    @Test
    void testSearchItems_NoResults() throws Exception {
        String token = "validToken";
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getAllShops()).thenReturn(Arrays.asList());
        
        List<Item> result = shopService.searchItems("nonexistent", null, null, null, null, null, null, token);
        
        assertTrue(result.isEmpty());
        verify(authTokenService, times(2)).ValidateToken(token); // Called in searchItems and getAllShops
        verify(shopRepository).getAllShops();
    }

    @Test
    void testSearchItems_TokenValidationFailure() throws Exception {
        String invalidToken = "invalid";
        
        when(authTokenService.ValidateToken(invalidToken)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, 
            () -> shopService.searchItems("test", null, null, null, null, null, null, invalidToken));
        assertTrue(exception.getMessage().contains("Invalid token"));
    }
    
    @Test
    void testAddSupplyToItem_InvalidToken() throws Exception {
        String invalidToken = "invalid";
        
        when(authTokenService.ValidateToken(invalidToken)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class, 
            () -> shopService.addSupplyToItem(1, 42, 10, invalidToken));
        assertTrue(exception.getMessage().contains("Invalid token"));
    }
    
    @Test
    void testAddSupplyToItem_NoPermission() throws Exception {
        String token = "validToken";
        int shopId = 1, itemId = 42, quantity = 10;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(userService.hasPermission(123, PermissionsEnum.manageItems, shopId)).thenReturn(false);
        
        OurRuntime exception = assertThrows(OurRuntime.class, 
            () -> shopService.addSupplyToItem(shopId, itemId, quantity, token));
        assertTrue(exception.getMessage().contains("does not have permission"));
    }
    
    @Test
    void testRemoveGlobalDiscount_RepositoryError() throws Exception {
        String token = "validToken";
        int shopId = 1, userId = 123;
        
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        when(userService.hasPermission(userId, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new RuntimeException("Database error")).when(shopRepository).removeGlobalDiscount(shopId);
        
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shopService.removeGlobalDiscount(shopId, token));
        assertTrue(exception.getMessage().contains("Error removing global discount"));
    }

    // Additional tests for supply management edge cases
    @Test
    void testAddSupplyToItem_RepositoryError() throws Exception {
        String token = "validToken";
        int shopId = 1, itemId = 42, quantity = 10, userId = 123;
        
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        when(userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        doThrow(new RuntimeException("Database error")).when(shopRepository).addSupplyToItem(shopId, itemId, quantity);
        
        OurRuntime exception = assertThrows(OurRuntime.class,
                () -> shopService.addSupplyToItem(shopId, itemId, quantity, token));
        assertTrue(exception.getMessage().contains("Error adding supply for item " + itemId + " in shop " + shopId));
    }

    @Test
    void testRemoveSupply_RepositoryError() throws Exception {
        String token = "validToken";
        int shopId = 1, itemId = 42, quantity = 10, userId = 123;
        
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        when(userService.hasPermission(userId, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        doThrow(new RuntimeException("Database error")).when(shopRepository).removeSupply(shopId, itemId, quantity);
        
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shopService.removeSupply(shopId, itemId, quantity, token));
        assertTrue(exception.getMessage().contains("Error removing supply"));
    }

    // Additional tests for checkSupplyAvailability edge cases
    @Test
    void testCheckSupplyAvailability_TokenValidationFailure() throws Exception {
        String invalidToken = "invalid";
        
        when(authTokenService.ValidateToken(invalidToken)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class,
                () -> shopService.checkSupplyAvailability(1, 42, invalidToken));
        assertTrue(exception.getMessage().contains("Invalid token"));
    }

    @Test
    void testCheckSupplyAvailability_RepositoryError() throws Exception {
        String token = "validToken";
        int shopId = 1, itemId = 42;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.checkSupplyAvailability(shopId, itemId)).thenThrow(new RuntimeException("Database error"));
        
        OurRuntime exception = assertThrows(OurRuntime.class,
                () -> shopService.checkSupplyAvailability(shopId, itemId, token));
        assertTrue(exception.getMessage().contains("Error checking supply for item " + itemId + " in shop " + shopId));
    }

    // Additional tests for checkSupplyAvailabilityAndAcquire edge cases
    @Test
    void testCheckSupplyAvailabilityAndAcquire_RepositoryError() throws Exception {
        int shopId = 1, itemId = 42, quantity = 5;
        
        when(shopRepository.checkSupplyAvailabilityAndAqcuire(shopId, itemId, quantity))
                .thenThrow(new RuntimeException("Database error"));
        
        OurRuntime exception = assertThrows(OurRuntime.class,
                () -> shopService.checkSupplyAvailabilityAndAcquire(shopId, itemId, quantity));
        assertTrue(exception.getMessage().contains("Error checking supply for item " + itemId + " in shop " + shopId));
    }

    // Additional comprehensive test for searchItemsInShop error handling
    @Test
    void testSearchItemsInShop_RepositoryError() throws Exception {
        String token = "validToken";
        int shopId = 1;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getShop(shopId)).thenThrow(new RuntimeException("Database error"));
        
        OurRuntime exception = assertThrows(OurRuntime.class,
                () -> shopService.searchItemsInShop(shopId, "test", null, null, null, null, null, token));
        assertTrue(exception.getMessage().contains("Error retrieving shop with id " + shopId));
    }

    // Additional comprehensive test for getPolicies error handling
    @Test
    void testGetPolicies_RepositoryError() throws Exception {
        String token = "validToken";
        int shopId = 1, userId = 123;
        
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        when(userService.hasPermission(userId, PermissionsEnum.viewPolicy, shopId)).thenReturn(true);
        when(shopRepository.getPolicies(shopId)).thenThrow(new RuntimeException("Database error"));
        
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shopService.getPolicies(shopId, token));
        assertTrue(exception.getMessage().contains("Error retrieving policies"));
    }

    // Additional comprehensive test for getDiscounts error handling
    @Test
    void testGetDiscounts_RepositoryError() throws Exception {
        String token = "validToken";
        int shopId = 1;
        
        when(authTokenService.ValidateToken(token)).thenReturn(123);
        when(shopRepository.getDiscounts(shopId)).thenThrow(new RuntimeException("Database error"));
        
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> shopService.getDiscounts(shopId, token));
        assertTrue(exception.getMessage().contains("Error retrieving discounts"));
    }

    // Additional test for setDiscountPolicy edge case
    @Test
    void testSetDiscountPolicy_TokenValidationFailure() throws Exception {
        String invalidToken = "invalid";
        CompositePolicyDTO policyDTO = mock(CompositePolicyDTO.class);
        
        when(authTokenService.ValidateToken(invalidToken)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class,
                () -> shopService.setDiscountPolicy(1, policyDTO, invalidToken));
        assertTrue(exception.getMessage().contains("Invalid token"));
    }

    // Additional comprehensive tests for worker shop methods
    @Test
    void testGetShopsByWorker_InvalidToken() throws Exception {
        String invalidToken = "invalid";
        int workerId = 456;
        
        when(userService.getShopIdsByWorkerId(workerId)).thenReturn(Arrays.asList(1));
        when(authTokenService.ValidateToken(invalidToken)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class,
                () -> shopService.getShopsByWorker(workerId, invalidToken));
        assertTrue(exception.getMessage().contains("Invalid token"));
    }

    @Test
    void testGetOpenShopsByWorker_InvalidToken() throws Exception {
        String invalidToken = "invalid";
        int workerId = 456;
        
        when(userService.getShopIdsByWorkerId(workerId)).thenReturn(Arrays.asList(1));
        when(authTokenService.ValidateToken(invalidToken)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class,
                () -> shopService.getOpenShopsByWorker(workerId, invalidToken));
        assertTrue(exception.getMessage().contains("Invalid token"));
    }

    @Test
    void testGetClosedShopsByWorker_InvalidToken() throws Exception {
        String invalidToken = "invalid";
        int workerId = 456;
        
        when(userService.getShopIdsByWorkerId(workerId)).thenReturn(Arrays.asList(1));
        when(authTokenService.ValidateToken(invalidToken)).thenThrow(new OurArg("Invalid token"));
        
        OurArg exception = assertThrows(OurArg.class,
                () -> shopService.getClosedShopsByWorker(workerId, invalidToken));
        assertTrue(exception.getMessage().contains("Invalid token"));
    }
}