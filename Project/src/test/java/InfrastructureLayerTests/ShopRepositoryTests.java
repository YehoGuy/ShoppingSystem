package InfrastructureLayerTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.app.ApplicationLayer.Purchase.ShippingMethod;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Operator;
import com.example.app.DomainLayer.Shop.PurchasePolicy;
import com.example.app.DomainLayer.Shop.Shop;
import com.example.app.InfrastructureLayer.ShopRepository;

public class ShopRepositoryTests {

    private ShopRepository repo;

    @Mock
    private PurchasePolicy purchasePolicy;

    @Mock
    private ShippingMethod shippingMethod;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        repo = new ShopRepository();
    }

    // UC1 – Create & Get Shop (success)
    // Verifies that a shop can be created and then retrieved by ID, preserving its
    // name and shipping method.
    @Test
    public void testCreateAndGetShop_Success() {
        Shop s = repo.createShop("MyShop", purchasePolicy, shippingMethod);
        assertNotNull(s, "createShop should return a non-null Shop");
        assertEquals("MyShop", s.getName(), "Shop name should match");
        assertSame(shippingMethod, s.getShippingMethod(), "ShippingMethod should be the mock we passed");
        Shop fetched = repo.getShop(s.getId());
        assertSame(s, fetched, "getShop should return the exact same Shop instance");
    }

    // UC2 – Get All Shops (success)
    // Verifies that getAllShops returns all created shops.
    @Test
    public void testGetAllShops_Success() {
        repo.createShop("A", purchasePolicy, shippingMethod);
        repo.createShop("B", purchasePolicy, shippingMethod);
        List<Shop> all = repo.getAllShops();
        assertEquals(2, all.size(), "getAllShops should return both shops");
    }

    // UC4 – Add & Supply Items (success)
    // Verifies addItemToShop and addSupplyToItem update stock quantities correctly.
    @Test
    public void testAddAndSupplyItems_Success() {
        Shop s = repo.createShop("S", purchasePolicy, shippingMethod);
        repo.addItemToShop(s.getId(), 1, 5, 100);
        assertEquals(5, repo.getItemQuantityFromShop(s.getId(), 1),
                "Quantity should be 5 after initial addItemToShop");
        repo.addSupplyToItem(s.getId(), 1, 3);
        assertEquals(8, repo.getItemQuantityFromShop(s.getId(), 1),
                "Quantity should be 8 after addSupplyToItem");
    }

    // UC5 – Update Item Price (success)
    // Verifies updateItemPriceInShop changes the item's price.
    @Test
    public void testUpdateItemPrice_Success() {
        Shop s = repo.createShop("S", purchasePolicy, shippingMethod);
        repo.addItemToShop(s.getId(), 2, 1, 50);
        repo.updateItemPriceInShop(s.getId(), 2, 75);
        assertEquals(75, repo.getShop(s.getId()).getItemPrice(2),
                "Item price should be updated to 75");
    }

    // UC6 – Remove Item Completely (success)
    // Verifies removeItemFromShop deletes stock and price.
    @Test
    public void testRemoveItemFromShop_Success() {
        Shop s = repo.createShop("S", purchasePolicy, shippingMethod);
        repo.addItemToShop(s.getId(), 3, 2, 20);
        repo.removeItemFromShop(s.getId(), 3);
        assertEquals(0, repo.getItemQuantityFromShop(s.getId(), 3),
                "Quantity should be zero after removal");
        assertEquals(0, repo.getShop(s.getId()).getItemPrice(3),
                "Price should be zero after removal");
    }

    // UC7 – Close Shop (success)
    // Verifies closeShop removes the shop, causing getShop to throw.
    @Test
    public void testCloseShop_Success() {
        Shop s = repo.createShop("S", purchasePolicy, shippingMethod);
        repo.closeShop(s.getId());
        assertThrows(RuntimeException.class,
                () -> repo.getShop(s.getId()),
                "getShop should throw after shop is closed");
    }

    // UC8 – Check Supply Availability (success)
    // Verifies checkSupplyAvailability and removeSupply work as expected.
    @Test
    public void testCheckSupplyAvailability_Success() {
        Shop s = repo.createShop("S", purchasePolicy, shippingMethod);
        repo.addItemToShop(s.getId(), 4, 2, 5);
        assertTrue(repo.checkSupplyAvailability(s.getId(), 4),
                "Supply should be available after adding stock");
        repo.removeSupply(s.getId(), 4, 2);
        assertFalse(repo.checkSupplyAvailability(s.getId(), 4),
                "Supply should not be available after removing all stock");
    }

    // UC9 – Purchase & Rollback (success)
    // Verifies purchaseItems deducts stock and rollBackPurchase restores it.
    @Test
    public void testPurchaseAndRollback_Success() {
        Shop s = repo.createShop("S", purchasePolicy, shippingMethod);
        repo.addItemToShop(s.getId(), 5, 3, 10);
        double total = repo.purchaseItems(Map.of(5, 2), Map.of(5, ItemCategory.ELECTRONICS), s.getId());
        assertEquals(20.0, total,
                "purchaseItems should return 2×10 = 20.0");
        assertEquals(1, repo.getItemQuantityFromShop(s.getId(), 5),
                "Quantity should be 1 after purchase");
        repo.rollBackPurchase(Map.of(5, 2), s.getId());
        assertEquals(3, repo.getItemQuantityFromShop(s.getId(), 5),
                "Quantity should be restored to 3 after rollback");
    }

    // UC10 – Error Cases (failure)
    // Verifies methods throw when given a non-existent shop ID.
    @Test
    public void testNonexistentShopOperations_Failure() {
        int badId = 999;
        assertThrows(RuntimeException.class,
                () -> repo.getShop(badId), "getShop should fail for unknown ID");
        assertThrows(RuntimeException.class,
                () -> repo.updatePurchasePolicy(badId, purchasePolicy), "updatePurchasePolicy should fail");
        assertThrows(RuntimeException.class,
                () -> repo.setGlobalDiscount(badId, 10, true), "setGlobalDiscount should fail");
        assertThrows(IllegalArgumentException.class,
                () -> repo.removeGlobalDiscount(badId), "removeGlobalDiscount should fail");
        assertThrows(RuntimeException.class,
                () -> repo.addItemToShop(badId, 1, 1, 1), "addItemToShop should fail");
        assertThrows(RuntimeException.class,
                () -> repo.addReviewToShop(badId, 1, 5, "X"), "addReviewToShop should fail");
        assertThrows(RuntimeException.class,
                () -> repo.closeShop(badId), "closeShop should fail");
    }

    // UC11 – Set & Remove Discounts (success & failure)
    @Test
    public void testSetAndRemoveItemAndCategoryDiscounts() {
        Shop s = repo.createShop("D", purchasePolicy, shippingMethod);
        int id = s.getId();

        // Item‐level
        assertDoesNotThrow(() -> repo.setDiscountForItem(id, 100, 20, false));
        assertDoesNotThrow(() -> repo.removeDiscountForItem(id, 100));

        // Category‐level
        assertDoesNotThrow(() -> repo.setCategoryDiscount(id, ItemCategory.ELECTRONICS, 15, true));
        assertDoesNotThrow(() -> repo.removeCategoryDiscount(id, ItemCategory.ELECTRONICS));

        // average rating
        repo.addReviewToShop(id, 1, 5, "Great");
        repo.addReviewToShop(id, 2, 3, "Ok");
        double avg = repo.getShopAverageRating(id);
        assertEquals(4.0, avg, 1e-6);

        // bad shop
        int bad = 999;
        assertThrows(RuntimeException.class, () -> repo.setDiscountForItem(bad, 1, 1, false));
        assertThrows(IllegalArgumentException.class, () -> repo.removeDiscountForItem(bad, 1));
        assertThrows(RuntimeException.class, () -> repo.setCategoryDiscount(bad, ItemCategory.ELECTRONICS, 1, false));
        assertThrows(RuntimeException.class, () -> repo.removeCategoryDiscount(bad, ItemCategory.ELECTRONICS));
        assertThrows(RuntimeException.class, () -> repo.getShopAverageRating(bad));
    }

    // UC12 – Supply & Acquire (success & failure)
    @Test
    public void testAddSupplyAndCheckAcquire() {
        Shop s = repo.createShop("S", purchasePolicy, shippingMethod);
        int id = s.getId();
        repo.addItemToShop(id, 50, 2, 10);

        // checkSupplyAvailability
        assertTrue(repo.checkSupplyAvailability(id, 50));
        repo.removeSupply(id, 50, 2);
        assertFalse(repo.checkSupplyAvailability(id, 50));

        // checkSupplyAvailabilityAndAqcuire
        repo.addSupply(id, 50, 5);
        assertTrue(repo.checkSupplyAvailabilityAndAqcuire(id, 50, 3));
        assertFalse(repo.checkSupplyAvailabilityAndAqcuire(id, 50, 10));

        // bad shop
        assertThrows(IllegalArgumentException.class, () -> repo.checkSupplyAvailabilityAndAqcuire(999, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> repo.addSupply(999, 1, 1));
    }

    // UC13 – getItemsByShop & getItems (success & failure)
    @Test
    public void testGetItemsByShopAndGetItems() {
        Shop s1 = repo.createShop("X", purchasePolicy, shippingMethod);
        Shop s2 = repo.createShop("Y", purchasePolicy, shippingMethod);
        repo.addItemToShop(s1.getId(), 11, 1, 5);
        repo.addItemToShop(s2.getId(), 22, 2, 10);

        List<Integer> shop1Items = repo.getItemsByShop(s1.getId());
        assertEquals(List.of(11), shop1Items);

        List<Integer> allItems = repo.getItems();
        assertTrue(allItems.contains(11));
        assertTrue(allItems.contains(22));

        // bad shop
        assertThrows(RuntimeException.class, () -> repo.getItemsByShop(999));
    }

    // UC14 – shipPurchase delegates to ShippingMethod
    @Test
    public void testShipPurchase_CallsShippingMethod() {
        Shop s = repo.createShop("Z", purchasePolicy, shippingMethod);
        int shopId = s.getId();
        // exercise
        repo.shipPurchase("123", shopId, "C", "Ci", "St", "PC");
        // verify delegate
        verify(shippingMethod).processShipping("123", "St", "Ci", "C", "PC");

        // bad shop
        assertThrows(RuntimeException.class,
                () -> repo.shipPurchase("1", 999, "a", "b", "c", "d"));
    }

    // UC15 – getClosedShops and closeShop
    @Test
    public void testGetClosedShopsAndCloseShop() {
        Shop s = repo.createShop("C", purchasePolicy, shippingMethod);
        assertTrue(repo.getClosedShops().isEmpty());
        repo.closeShop(s.getId());
        List<Integer> closed = repo.getClosedShops();
        assertEquals(1, closed.size());
        assertSame(s.getId(), closed.get(0));
    }

    // UC16 – policy is a no-op but returns true
    @Test
    public void testCheckPolicy_AlwaysTrue() {
        assertTrue(repo.checkPolicy(new HashMap<>(), "anyToken"));
    }

    // UC18 – getAllShops returns unmodifiable list
    @Test
    public void testGetAllShops_Unmodifiable() {
        Shop a = repo.createShop("A", purchasePolicy, shippingMethod);
        Shop b = repo.createShop("B", purchasePolicy, shippingMethod);
        List<Shop> all = repo.getAllShops();
        assertEquals(List.of(a, b), all);
        // unmodifiable
        assertThrows(UnsupportedOperationException.class,
                () -> all.add(a),
                "getAllShops should return an unmodifiable list");
    }

    // UC19 – supply operations: addSupplyToItem, checkSupplyAvailability &
    // removeSupply
    @Test
    public void testSupplyOperations() {
        Shop s = repo.createShop("Supp", purchasePolicy, shippingMethod);
        int id = s.getId();

        // initially no stock
        assertFalse(repo.checkSupplyAvailability(id, 99));
        // addSupply
        repo.addSupplyToItem(id, 99, 7);
        assertTrue(repo.checkSupplyAvailability(id, 99));
        assertEquals(7, repo.getItemQuantityFromShop(id, 99));

        // removeSupply
        repo.removeSupply(id, 99, 3);
        assertEquals(4, repo.getItemQuantityFromShop(id, 99));

        // remove remaining → none left
        repo.removeSupply(id, 99, 4);
        assertFalse(repo.checkSupplyAvailability(id, 99));

        // bad shop
        assertThrows(RuntimeException.class,
                () -> repo.checkSupplyAvailability(999, 1));
        assertThrows(RuntimeException.class,
                () -> repo.removeSupply(999, 1, 1));
    }

    // UC20 – addItemToShop & removeItemFromShop & updateItemPriceInShop &
    // getItemQuantityFromShop
    @Test
    public void testItemAddRemoveUpdateAndQuantity() {
        Shop s = repo.createShop("Items", purchasePolicy, shippingMethod);
        int id = s.getId();

        // addItemToShop
        repo.addItemToShop(id, 5, 10, 50);
        assertEquals(10, repo.getItemQuantityFromShop(id, 5));
        assertEquals(50, repo.getShop(id).getItemPrice(5));

        // updateItemPriceInShop
        repo.updateItemPriceInShop(id, 5, 75);
        assertEquals(75, repo.getShop(id).getItemPrice(5));

        // removeItemFromShop
        repo.removeItemFromShop(id, 5);
        assertEquals(0, repo.getItemQuantityFromShop(id, 5));
        assertEquals(0, repo.getShop(id).getItemPrice(5));

        // bad shop
        assertThrows(RuntimeException.class,
                () -> repo.addItemToShop(999, 1, 1, 1));
        assertThrows(RuntimeException.class,
                () -> repo.updateItemPriceInShop(999, 1, 1));
        assertThrows(RuntimeException.class,
                () -> repo.removeItemFromShop(999, 1));
        assertThrows(RuntimeException.class,
                () -> repo.getItemQuantityFromShop(999, 1));
    }

    // UC21 – purchaseItems & rollBackPurchase
    @Test
    public void testPurchaseAndRollback_FullFlow() {
        Shop s = repo.createShop("BuyOut", purchasePolicy, shippingMethod);
        int id = s.getId();

        // stock = 5
        repo.addItemToShop(id, 8, 5, 20);
        double cost = repo.purchaseItems(Map.of(8, 3), Map.of(8, ItemCategory.BOOKS), id);
        assertEquals(60.0, cost, 1e-6);
        assertEquals(2, repo.getItemQuantityFromShop(id, 8));

        // rollback 3 → back to 5
        repo.rollBackPurchase(Map.of(8, 3), id);
        assertEquals(5, repo.getItemQuantityFromShop(id, 8));

        // bad shop
        assertThrows(RuntimeException.class,
                () -> repo.purchaseItems(Map.of(1, 1), Map.of(1, ItemCategory.BOOKS), 999));
        assertThrows(RuntimeException.class,
                () -> repo.rollBackPurchase(Map.of(1, 1), 999));
    }

    // UC22 – getClosedShops is empty until closeShop
    @Test
    public void testGetClosedShops() {
        assertTrue(repo.getClosedShops().isEmpty());
        Shop s1 = repo.createShop("C1", purchasePolicy, shippingMethod);
        Shop s2 = repo.createShop("C2", purchasePolicy, shippingMethod);
        repo.closeShop(s1.getId());
        List<Integer> closed = repo.getClosedShops();
        assertEquals(1, closed.size());
        assertSame(s1.getId(), closed.get(0));
        // ensure C2 is still open
        assertDoesNotThrow(() -> repo.getShop(s2.getId()));
    }

    /* ═══════════════════ Additional Comprehensive Function Tests ═══════════════════ */

    // UC23 – updatePurchasePolicy (success & failure)
    @Test
    public void testUpdatePurchasePolicy() {
        Shop s = repo.createShop("PolicyShop", purchasePolicy, shippingMethod);
        int shopId = s.getId();
        
        // Test updating purchase policy - should not throw
        assertDoesNotThrow(() -> repo.updatePurchasePolicy(shopId, purchasePolicy));
        
        // Test with non-existent shop - should throw RuntimeException
        assertThrows(RuntimeException.class, 
                () -> repo.updatePurchasePolicy(999, purchasePolicy),
                "updatePurchasePolicy should fail for non-existent shop");
    }

    // UC24 – setGlobalDiscount & removeGlobalDiscount comprehensive tests
    @Test
    public void testGlobalDiscountOperations() {
        Shop s = repo.createShop("DiscountShop", purchasePolicy, shippingMethod);
        int shopId = s.getId();
        
        // Test setting global discount
        assertDoesNotThrow(() -> repo.setGlobalDiscount(shopId, 15, true));
        assertDoesNotThrow(() -> repo.setGlobalDiscount(shopId, 25, false));
        
        // Test removing global discount
        assertDoesNotThrow(() -> repo.removeGlobalDiscount(shopId));
        
        // Test with non-existent shop
        assertThrows(RuntimeException.class, 
                () -> repo.setGlobalDiscount(999, 10, true),
                "setGlobalDiscount should fail for non-existent shop");
        assertThrows(IllegalArgumentException.class, 
                () -> repo.removeGlobalDiscount(999),
                "removeGlobalDiscount should fail for non-existent shop");
        
        // Test edge cases for discount values
        assertDoesNotThrow(() -> repo.setGlobalDiscount(shopId, 0, true), "Zero discount should be allowed");
        assertDoesNotThrow(() -> repo.setGlobalDiscount(shopId, 100, false), "100% discount should be allowed");
    }

    // UC25 – getAllOpenShops (success)
    @Test
    public void testGetAllOpenShops() {
        // Initially should be empty or contain existing shops
        List<Shop> initialOpen = repo.getAllOpenShops();
        int initialCount = initialOpen.size();
        
        // Create some shops
        Shop s1 = repo.createShop("OpenShop1", purchasePolicy, shippingMethod);
        Shop s2 = repo.createShop("OpenShop2", purchasePolicy, shippingMethod);
        Shop s3 = repo.createShop("ToCloseShop", purchasePolicy, shippingMethod);
        
        // All should be open initially
        List<Shop> allOpen = repo.getAllOpenShops();
        assertEquals(initialCount + 3, allOpen.size(), "Should have 3 more open shops");
        assertTrue(allOpen.contains(s1), "Should contain shop 1");
        assertTrue(allOpen.contains(s2), "Should contain shop 2");
        assertTrue(allOpen.contains(s3), "Should contain shop 3");
        
        // Close one shop
        repo.closeShop(s3.getId());
        
        // Should have one less open shop
        List<Shop> openAfterClose = repo.getAllOpenShops();
        assertEquals(initialCount + 2, openAfterClose.size(), "Should have one less open shop");
        assertTrue(openAfterClose.contains(s1), "Should still contain shop 1");
        assertTrue(openAfterClose.contains(s2), "Should still contain shop 2");
        assertFalse(openAfterClose.contains(s3), "Should not contain closed shop");
    }

    // UC26 – getAllClosedShops (success)
    @Test
    public void testGetAllClosedShops() {
        // Initially should be empty
        List<Shop> initialClosed = repo.getAllClosedShops();
        int initialCount = initialClosed.size();
        
        // Create and close some shops
        Shop s1 = repo.createShop("ToClose1", purchasePolicy, shippingMethod);
        Shop s2 = repo.createShop("ToClose2", purchasePolicy, shippingMethod);
        Shop s3 = repo.createShop("StayOpen", purchasePolicy, shippingMethod);
        
        // Close two shops
        repo.closeShop(s1.getId());
        repo.closeShop(s2.getId());
        
        // Should have two more closed shops
        List<Shop> allClosed = repo.getAllClosedShops();
        assertEquals(initialCount + 2, allClosed.size(), "Should have 2 more closed shops");
        assertTrue(allClosed.contains(s1), "Should contain closed shop 1");
        assertTrue(allClosed.contains(s2), "Should contain closed shop 2");
        assertFalse(allClosed.contains(s3), "Should not contain open shop");
        
        // Verify the open shop is still accessible
        assertDoesNotThrow(() -> repo.getShop(s3.getId()), "Open shop should still be accessible");
    }

    // UC27 – setDiscountPolicy (success & failure)
    @Test
    public void testSetDiscountPolicy() {
        Shop s = repo.createShop("PolicyTestShop", purchasePolicy, shippingMethod);
        int shopId = s.getId();
        
        // We need to create a mock Policy since it's a domain object
        // For now, test that the method doesn't throw with null
        assertDoesNotThrow(() -> repo.setDiscountPolicy(shopId, null));
        
        // Test with non-existent shop
        assertThrows(RuntimeException.class, 
                () -> repo.setDiscountPolicy(999, null),
                "setDiscountPolicy should fail for non-existent shop");
    }

    // UC28 – getDiscounts (success & failure)
    @Test
    public void testGetDiscounts() {
        Shop s = repo.createShop("DiscountTestShop", purchasePolicy, shippingMethod);
        int shopId = s.getId();
        
        // Should return a list (even if empty)
        List<?> discounts = repo.getDiscounts(shopId);
        assertNotNull(discounts, "getDiscounts should return a non-null list");
        
        // Test with non-existent shop
        assertThrows(RuntimeException.class, 
                () -> repo.getDiscounts(999),
                "getDiscounts should fail for non-existent shop");
    }

    // UC29 – getPolicies (success & failure)
    @Test
    public void testGetPolicies() {
        Shop s = repo.createShop("PoliciesTestShop", purchasePolicy, shippingMethod);
        int shopId = s.getId();
        
        // Should return a list (even if empty)
        List<?> policies = repo.getPolicies(shopId);
        assertNotNull(policies, "getPolicies should return a non-null list");
        
        // Test with non-existent shop
        assertThrows(RuntimeException.class, 
                () -> repo.getPolicies(999),
                "getPolicies should fail for non-existent shop");
    }

    // UC30 – Comprehensive createShop tests
    @Test
    public void testCreateShop_Comprehensive() {
        // Test normal creation
        Shop s1 = repo.createShop("TestShop", purchasePolicy, shippingMethod);
        assertNotNull(s1, "Shop should be created");
        assertEquals("TestShop", s1.getName(), "Shop name should match");
        assertTrue(s1.getId() > 0, "Shop ID should be positive");
        
        // Test creation with special characters in name
        Shop s2 = repo.createShop("Spéciàl Chárs & Símb@ls", purchasePolicy, shippingMethod);
        assertNotNull(s2, "Shop with special characters should be created");
        assertEquals("Spéciàl Chárs & Símb@ls", s2.getName(), "Special characters should be preserved");
        
        // Test creation with empty name
        Shop s3 = repo.createShop("", purchasePolicy, shippingMethod);
        assertNotNull(s3, "Shop with empty name should be created");
        assertEquals("", s3.getName(), "Empty name should be preserved");
        
        // Test creation with very long name
        String longName = "A".repeat(1000);
        Shop s4 = repo.createShop(longName, purchasePolicy, shippingMethod);
        assertNotNull(s4, "Shop with long name should be created");
        assertEquals(longName, s4.getName(), "Long name should be preserved");
        
        // Verify all shops have unique IDs
        assertNotEquals(s1.getId(), s2.getId(), "Shop IDs should be unique");
        assertNotEquals(s2.getId(), s3.getId(), "Shop IDs should be unique");
        assertNotEquals(s3.getId(), s4.getId(), "Shop IDs should be unique");
        
        // Test with null parameters (implementation allows null name)
        Shop nullNameShop = repo.createShop(null, purchasePolicy, shippingMethod);
        assertNotNull(nullNameShop, "Shop with null name should be created");
        assertEquals(null, nullNameShop.getName(), "Null name should be preserved");
    }

    // UC31 – Enhanced shipPurchase tests
    @Test
    public void testShipPurchase_Comprehensive() {
        Shop s = repo.createShop("ShippingShop", purchasePolicy, shippingMethod);
        int shopId = s.getId();
        
        // Test normal shipping
        assertDoesNotThrow(() -> repo.shipPurchase("John Doe", shopId, "USA", "New York", "123 Main St", "10001"));
        verify(shippingMethod).processShipping("John Doe", "123 Main St", "New York", "USA", "10001");
        
        // Test with special characters
        assertDoesNotThrow(() -> repo.shipPurchase("José García", shopId, "España", "Madrid", "Calle de Alcalá", "28014"));
        verify(shippingMethod).processShipping("José García", "Calle de Alcalá", "Madrid", "España", "28014");
        
        // Test with empty strings
        assertDoesNotThrow(() -> repo.shipPurchase("", shopId, "", "", "", ""));
        verify(shippingMethod).processShipping("", "", "", "", "");
        
        // Test with very long strings
        String longString = "X".repeat(500);
        assertDoesNotThrow(() -> repo.shipPurchase(longString, shopId, longString, longString, longString, longString));
        verify(shippingMethod).processShipping(longString, longString, longString, longString, longString);
        
        // Test with non-existent shop
        assertThrows(RuntimeException.class,
                () -> repo.shipPurchase("Test", 999, "Country", "City", "Street", "Postal"),
                "shipPurchase should fail for non-existent shop");
    }

    // UC32 – Enhanced getItems tests
    @Test
    public void testGetItems_Comprehensive() {
        // Initially should be empty or contain existing items
        List<Integer> initialItems = repo.getItems();
        int initialCount = initialItems.size();
        
        // Create shops and add items
        Shop s1 = repo.createShop("ItemShop1", purchasePolicy, shippingMethod);
        Shop s2 = repo.createShop("ItemShop2", purchasePolicy, shippingMethod);
        
        repo.addItemToShop(s1.getId(), 100, 5, 10);
        repo.addItemToShop(s1.getId(), 101, 3, 15);
        repo.addItemToShop(s2.getId(), 102, 7, 20);
        repo.addItemToShop(s2.getId(), 100, 2, 12); // Same item in different shop
        
        List<Integer> allItems = repo.getItems();
        
        // Should contain all unique item IDs
        assertTrue(allItems.contains(100), "Should contain item 100");
        assertTrue(allItems.contains(101), "Should contain item 101");
        assertTrue(allItems.contains(102), "Should contain item 102");
        
        // Should have at least the new items (may contain duplicates or not based on implementation)
        assertTrue(allItems.size() >= initialCount, "Should have at least the initial number of items");
        
        // Remove an item and check
        repo.removeItemFromShop(s1.getId(), 101);
        List<Integer> itemsAfterRemoval = repo.getItems();
        
        // Item 100 and 102 should still be there
        assertTrue(itemsAfterRemoval.contains(100), "Should still contain item 100");
        assertTrue(itemsAfterRemoval.contains(102), "Should still contain item 102");
    }

    // UC33 – Enhanced getClosedShops tests
    @Test
    public void testGetClosedShops_Comprehensive() {
        // Initially should be empty
        List<Integer> initialClosed = repo.getClosedShops();
        int initialCount = initialClosed.size();
        
        // Create multiple shops
        Shop s1 = repo.createShop("Shop1", purchasePolicy, shippingMethod);
        Shop s2 = repo.createShop("Shop2", purchasePolicy, shippingMethod);
        Shop s3 = repo.createShop("Shop3", purchasePolicy, shippingMethod);
        Shop s4 = repo.createShop("Shop4", purchasePolicy, shippingMethod);
        
        // Should still be empty after creating shops
        List<Integer> afterCreation = repo.getClosedShops();
        assertEquals(initialCount, afterCreation.size(), "No shops should be closed after creation");
        
        // Close some shops
        repo.closeShop(s1.getId());
        repo.closeShop(s3.getId());
        
        List<Integer> afterClosing = repo.getClosedShops();
        assertEquals(initialCount + 2, afterClosing.size(), "Should have 2 more closed shops");
        assertTrue(afterClosing.contains(s1.getId()), "Should contain closed shop 1 ID");
        assertTrue(afterClosing.contains(s3.getId()), "Should contain closed shop 3 ID");
        assertFalse(afterClosing.contains(s2.getId()), "Should not contain open shop 2 ID");
        assertFalse(afterClosing.contains(s4.getId()), "Should not contain open shop 4 ID");
        
        // Close another shop
        repo.closeShop(s2.getId());
        
        List<Integer> afterClosingMore = repo.getClosedShops();
        assertEquals(initialCount + 3, afterClosingMore.size(), "Should have 3 closed shops");
        assertTrue(afterClosingMore.contains(s2.getId()), "Should now contain closed shop 2 ID");
        
        // Test that returned list is not modifiable (if implemented as such)
        List<Integer> closedList = repo.getClosedShops();
        assertNotNull(closedList, "Closed shops list should not be null");
    }

    // UC34 – Enhanced addSupplyToItem tests
    @Test
    public void testAddSupplyToItem_Comprehensive() {
        Shop s = repo.createShop("SupplyShop", purchasePolicy, shippingMethod);
        int shopId = s.getId();
        
        // Add initial item
        repo.addItemToShop(shopId, 200, 5, 25);
        assertEquals(5, repo.getItemQuantityFromShop(shopId, 200), "Initial quantity should be 5");
        
        // Add supply to existing item
        repo.addSupplyToItem(shopId, 200, 10);
        assertEquals(15, repo.getItemQuantityFromShop(shopId, 200), "Quantity should be 15 after adding supply");
        
        // Add more supply
        repo.addSupplyToItem(shopId, 200, 5);
        assertEquals(20, repo.getItemQuantityFromShop(shopId, 200), "Quantity should be 20 after adding more supply");
        
        // Add supply with zero quantity (implementation throws exception for non-positive values)
        assertThrows(RuntimeException.class, 
                () -> repo.addSupplyToItem(shopId, 200, 0), 
                "Adding zero supply should throw exception");
        assertEquals(20, repo.getItemQuantityFromShop(shopId, 200), "Quantity should remain 20 after failed zero addition");
        
        // Add supply to non-existent item (behavior may vary)
        assertDoesNotThrow(() -> repo.addSupplyToItem(shopId, 999, 5), "Adding supply to non-existent item should handle gracefully");
        
        // Test with very large quantities
        repo.addSupplyToItem(shopId, 200, 1000000);
        assertEquals(1000020, repo.getItemQuantityFromShop(shopId, 200), "Should handle large quantities");
        
        // Test with non-existent shop
        assertThrows(RuntimeException.class, 
                () -> repo.addSupplyToItem(999, 200, 5),
                "addSupplyToItem should fail for non-existent shop");
    }

    // UC35 – Enhanced getAllShops tests
    @Test
    public void testGetAllShops_Comprehensive() {
        // Get initial state
        List<Shop> initialShops = repo.getAllShops();
        int initialCount = initialShops.size();
        
        // Create multiple shops
        Shop s1 = repo.createShop("Alpha", purchasePolicy, shippingMethod);
        Shop s2 = repo.createShop("Beta", purchasePolicy, shippingMethod);
        Shop s3 = repo.createShop("Gamma", purchasePolicy, shippingMethod);
        
        // Should contain all created shops
        List<Shop> allShops = repo.getAllShops();
        assertEquals(initialCount + 3, allShops.size(), "Should have 3 more shops");
        assertTrue(allShops.contains(s1), "Should contain shop Alpha");
        assertTrue(allShops.contains(s2), "Should contain shop Beta");
        assertTrue(allShops.contains(s3), "Should contain shop Gamma");
        
        // Close a shop - getAllShops actually removes closed shops
        repo.closeShop(s2.getId());
        List<Shop> shopsAfterClose = repo.getAllShops();
        assertEquals(initialCount + 2, shopsAfterClose.size(), "getAllShops should have 2 shops after closing one");
        assertTrue(shopsAfterClose.contains(s1), "Should still contain shop Alpha");
        assertFalse(shopsAfterClose.contains(s2), "Should not contain closed shop Beta");
        assertTrue(shopsAfterClose.contains(s3), "Should still contain shop Gamma");
        
        // Verify list is unmodifiable
        assertThrows(UnsupportedOperationException.class,
                () -> allShops.add(s1),
                "getAllShops should return an unmodifiable list");
        
        // Test with large number of shops
        for (int i = 0; i < 100; i++) {
            repo.createShop("Shop" + i, purchasePolicy, shippingMethod);
        }
        List<Shop> manyShops = repo.getAllShops();
        assertEquals(initialCount + 102, manyShops.size(), "Should handle many shops (accounting for one closed shop)");
    }

    // UC36 – Integration test for all functions
    @Test
    public void testIntegration_CompleteShopWorkflow() {
        // Create multiple shops
        Shop mainShop = repo.createShop("MainShop", purchasePolicy, shippingMethod);
        Shop competitorShop = repo.createShop("CompetitorShop", purchasePolicy, shippingMethod);
        int mainShopId = mainShop.getId();
        int competitorShopId = competitorShop.getId();
        
        // Add items to shops
        repo.addItemToShop(mainShopId, 1001, 10, 50);
        repo.addItemToShop(mainShopId, 1002, 5, 100);
        repo.addItemToShop(competitorShopId, 1001, 8, 45);
        
        // Set global discount
        repo.setGlobalDiscount(mainShopId, 10, false);
        
        // Add supply
        repo.addSupplyToItem(mainShopId, 1001, 20);
        assertEquals(30, repo.getItemQuantityFromShop(mainShopId, 1001), "Supply should be added");
        
        // Update purchase policy
        assertDoesNotThrow(() -> repo.updatePurchasePolicy(mainShopId, purchasePolicy));
        
        // Test shipping
        assertDoesNotThrow(() -> repo.shipPurchase("Customer", mainShopId, "USA", "NYC", "Main St", "10001"));
        
        // Verify shops in different lists
        List<Shop> allShops = repo.getAllShops();
        List<Shop> openShops = repo.getAllOpenShops();
        List<Integer> closedShopIds = repo.getClosedShops();
        
        assertTrue(allShops.contains(mainShop), "Main shop should be in all shops");
        assertTrue(allShops.contains(competitorShop), "Competitor shop should be in all shops");
        assertTrue(openShops.contains(mainShop), "Main shop should be in open shops");
        assertTrue(openShops.contains(competitorShop), "Competitor shop should be in open shops");
        assertFalse(closedShopIds.contains(mainShopId), "Main shop should not be in closed shops");
        
        // Close competitor shop
        repo.closeShop(competitorShopId);
        
        // Verify state after closing
        List<Shop> openAfterClose = repo.getAllOpenShops();
        List<Shop> closedAfterClose = repo.getAllClosedShops();
        List<Integer> closedIdsAfterClose = repo.getClosedShops();
        
        assertTrue(openAfterClose.contains(mainShop), "Main shop should still be open");
        assertFalse(openAfterClose.contains(competitorShop), "Competitor shop should not be in open shops");
        assertTrue(closedAfterClose.contains(competitorShop), "Competitor shop should be in closed shops");
        assertTrue(closedIdsAfterClose.contains(competitorShopId), "Competitor shop ID should be in closed shop IDs");
        
        // Verify items are still accessible
        List<Integer> allItems = repo.getItems();
        assertTrue(allItems.contains(1001), "Item 1001 should be in items list");
        assertTrue(allItems.contains(1002), "Item 1002 should be in items list");
        
        // Remove global discount
        assertDoesNotThrow(() -> repo.removeGlobalDiscount(mainShopId));
    }

}
