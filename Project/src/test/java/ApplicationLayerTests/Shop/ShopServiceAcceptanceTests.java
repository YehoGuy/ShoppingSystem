package ApplicationLayerTests.Shop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import DomainLayer.Item.Item;
import DomainLayer.Item.ItemCategory;
import DomainLayer.Shop.Shop;
import DomainLayer.Shop.IShopRepository;
import DomainLayer.Roles.PermissionsEnum;
import ApplicationLayer.AuthTokenService;
import ApplicationLayer.Item.ItemService;
import ApplicationLayer.Shop.ShopService;
import ApplicationLayer.User.UserService;

class ShopServiceAcceptanceTests {

    @Mock
    private IShopRepository shopRepository;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private ItemService itemService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ShopService shopService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        // initialize @Mock fields
        mocks = MockitoAnnotations.openMocks(this);

        // manually construct with your two-arg constructor
        shopService = new ShopService(shopRepository);

        // wire in the other services
        shopService.setServices(authTokenService, itemService, userService);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
        clearInvocations(shopRepository, authTokenService, itemService, userService);
    }

    // UC5 – Search Products in Market
    @Test
    void testSearchItemsInShop_SuccessfulSearch() throws Exception {
        String token = "valid-token";
        int shopId = 1;
        Item item = new Item(42, "Widget", "A fine widget", 0);

        when(authTokenService.ValidateToken(token)).thenReturn(10);
        when(shopRepository.getShop(shopId)).thenReturn(new Shop(shopId, "ShopA", "ANY", 0));
        when(shopRepository.getItemsByShop(shopId)).thenReturn(Arrays.asList(item.getId()));
        when(itemService.getItemsByIds(Arrays.asList(item.getId()),token))
            .thenReturn(Arrays.asList(item));

        List<Item> results = shopService.searchItemsInShop(shopId, "wid", ItemCategory.ELECTRONICS, null, 0, 1000000000, null, token);
        assertEquals(1, results.size());
        assertEquals(item, results.get(0));
    }

    // UC5 – Search Products in Market (no matches)
    @Test
    void testSearchItemsInShop_NoMatches() throws Exception {
        String token = "valid-token";
        int shopId = 1;

        when(authTokenService.ValidateToken(token)).thenReturn(10);
        when(shopRepository.getShop(shopId)).thenReturn(new Shop(shopId, "ShopA", "ANY", 0));
        when(shopRepository.getItemsByShop(shopId)).thenReturn(Collections.emptyList());

        List<Item> results = shopService.searchItemsInShop(((Integer)shopId), "nonexistent", ItemCategory.AUTOMOTIVE, new ArrayList<>(), 0, 1000000000, 0.0, token);
        assertTrue(results.isEmpty());
    }

    // UC10 – Create Shop
    @Test
    void testCreateShop_Success() throws Exception {
        String token = "t";
        Shop newShop = new Shop(5, "MyShop", "ALL", 10);

        when(authTokenService.ValidateToken(token)).thenReturn(1);
        doNothing().when(userService).validateMemberId(1);
        when(shopRepository.createShop("MyShop", "ALL", 10)).thenReturn(newShop);

        Shop created = shopService.createShop("MyShop", "ALL", 10, token);
        assertEquals(newShop, created);
    }

    // UC10 – Create Shop (name taken)
    @Test
    void testCreateShop_NameAlreadyTaken() throws Exception {
        String token = "t";

        when(authTokenService.ValidateToken(token)).thenReturn(1);
        doNothing().when(userService).validateMemberId(1);
        when(shopRepository.createShop(any(), any(), anyInt()))
            .thenThrow(new RuntimeException("Shop name is already taken"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.createShop("dup", "P", 0, token));
        assertTrue(ex.getMessage().contains("Shop name is already taken"));
    }

    // UC10 – Create Shop (missing details)
    @Test
    void testCreateShop_MissingDetails() throws Exception {
        String token       = "t";
        int userId         = 1;
        String emptyName   = "";
        String policy      = "ALL";
        int discount       = 10;

        // stub token validation and member check
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        doNothing().when(userService).validateMemberId(userId);

        // simulate repository rejecting missing name
        when(shopRepository.createShop(emptyName, policy, discount))
            .thenThrow(new IllegalArgumentException("Shop name is required"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            shopService.createShop(emptyName, policy, discount, token)
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
    void testAddReviewToShop_InvalidToken() throws Exception {
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

        shopService.addItemToShop(shopId, "item1", "no description", qty, price, token);
        verify(shopRepository).addItemToShop(shopId, itemId, qty, price);
    }

    // UC16 – Add Product to Shop (invalid quantity)
    @Test
    void testAddItemToShop_InvalidQuantity() throws Exception {
        String token = "tok";
        int shopId = 3, itemId = 0;

        when(authTokenService.ValidateToken(token)).thenReturn(8);
        when(userService.hasPermission(8, PermissionsEnum.manageItems, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Quantity must be positive"))
            .when(shopRepository).addItemToShop(shopId, itemId, -1, 5);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addItemToShop(shopId, "item1", "no description", -1, 5, token));
        assertTrue(ex.getMessage().contains("Error adding item"));
    }

    // UC16 – Add Product to Shop (no permission)
    @Test
    void testAddItemToShop_NoPermission() throws Exception {
        String token = "tok";
        int shopId = 3;

        when(authTokenService.ValidateToken(token)).thenReturn(8);
        when(userService.hasPermission(8, PermissionsEnum.manageItems, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addItemToShop(shopId, "item1", "no description", 5, 100, token));
        assertTrue(ex.getMessage().contains("does not have permission"));
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
    void testRemoveItemFromShop_ItemNotFound() throws Exception {
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
    void testRemoveItemFromShop_NoPermission() throws Exception {
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
    void testUpdateItemPriceInShop_NegativePrice() throws Exception {
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
    void testUpdateItemPriceInShop_NoPermission() throws Exception {
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

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addSupplyToItem(shopId, 1, 1, token));
        assertTrue(ex.getMessage().contains("Error adding supply"));
    }

    // UC19 – Define Purchase/Discount Policy
    @Test
    void testUpdatePurchasePolicy_Success() throws Exception {
        String token = "t";
        int shopId = 7;
        String policy = "ALL";

        when(authTokenService.ValidateToken(token)).thenReturn(13);
        when(userService.hasPermission(13, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).updatePurchasePolicy(shopId, policy);

        shopService.updatePurchasePolicy(shopId, policy, token);
        verify(shopRepository).updatePurchasePolicy(shopId, policy);
    }

    // UC19 – Define Purchase/Discount Policy (invalid policy)
    @Test
    void testUpdatePurchasePolicy_InvalidPolicy() throws Exception {
        String token = "t";
        int shopId = 7;

        when(authTokenService.ValidateToken(token)).thenReturn(13);
        when(userService.hasPermission(13, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Invalid policy"))
            .when(shopRepository).updatePurchasePolicy(shopId, "bad");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.updatePurchasePolicy(shopId, "bad", token));
        assertTrue(ex.getMessage().contains("Error updating purchase policy"));
    }

    // UC19 – Define Purchase/Discount Policy (no permission)
    @Test
    void testUpdatePurchasePolicy_NoPermission() throws Exception {
        String token = "t";
        int shopId = 7;

        when(authTokenService.ValidateToken(token)).thenReturn(13);
        when(userService.hasPermission(13, PermissionsEnum.setPolicy, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.updatePurchasePolicy(shopId, "ANY", token));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // UC19 – Define Discount  
    @Test
    void testSetGlobalDiscount_Success() throws Exception {
        String token = "t";
        int shopId = 8, discount = 25;

        when(authTokenService.ValidateToken(token)).thenReturn(14);
        when(userService.hasPermission(14, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doNothing().when(shopRepository).setGlobalDiscount(shopId, discount);

        shopService.setGlobalDiscount(shopId, discount, token);
        verify(shopRepository).setGlobalDiscount(shopId, discount);
    }

    // UC19 – Define Discount (invalid discount)
    @Test
    void testSetGlobalDiscount_InvalidDiscount() throws Exception {
        String token = "t";
        int shopId = 8;

        when(authTokenService.ValidateToken(token)).thenReturn(14);
        when(userService.hasPermission(14, PermissionsEnum.setPolicy, shopId)).thenReturn(true);
        doThrow(new IllegalArgumentException("Discount must be <=100"))
            .when(shopRepository).setGlobalDiscount(shopId, 200);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.setGlobalDiscount(shopId, 200, token));
        assertTrue(ex.getMessage().contains("Error setting global discount"));
    }

    // UC19 – Define Discount (no permission)
    @Test
    void testSetGlobalDiscount_NoPermission() throws Exception {
        String token = "t";
        int shopId = 8;

        when(authTokenService.ValidateToken(token)).thenReturn(14);
        when(userService.hasPermission(14, PermissionsEnum.setPolicy, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.setGlobalDiscount(shopId, 10, token));
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
    void testCloseShop_AlreadyClosed() throws Exception {
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
    void testCloseShop_NoPermission() throws Exception {
        String token = "t";
        int shopId = 9;

        when(authTokenService.ValidateToken(token)).thenReturn(15);
        when(userService.hasPermission(15, PermissionsEnum.closeShop, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.closeShop(shopId, token));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }

    // UC28 – Enforce Actions by Permission (sample)
    @Test
    void testActionByPermission_Denied() throws Exception {
        String token = "t";
        int shopId = 10;

        when(authTokenService.ValidateToken(token)).thenReturn(16);
        when(userService.hasPermission(16, PermissionsEnum.manageItems, shopId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> shopService.addItemToShop(shopId, "item1", "no description", 5, 100, token));
        assertTrue(ex.getMessage().contains("does not have permission"));
    }
}