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
        List<Shop> closed = repo.getClosedShops();
        assertEquals(1, closed.size());
        assertSame(s, closed.get(0));
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
        List<Shop> closed = repo.getClosedShops();
        assertEquals(1, closed.size());
        assertSame(s1, closed.get(0));
        // ensure C2 is still open
        assertDoesNotThrow(() -> repo.getShop(s2.getId()));
    }

}
