package InfrastructureLayerTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ApplicationLayer.Purchase.ShippingMethod;
import DomainLayer.Shop.PurchasePolicy;
import DomainLayer.Shop.Shop;
import InfrastructureLayer.ShopRepository;

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
    // Verifies that a shop can be created and then retrieved by ID, preserving its name and shipping method.
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

    // UC3 – Update Purchase Policy (success)
    // Verifies that updatePurchasePolicy adds the given policy to the shop.
    @Test
    public void testUpdatePurchasePolicy_Success() {
        Shop s = repo.createShop("S", purchasePolicy, shippingMethod);
        repo.updatePurchasePolicy(s.getId(), purchasePolicy);
        assertTrue(repo.getShop(s.getId()).getPurchasePolicies().contains(purchasePolicy),
            "The shop's purchasePolicies list should contain the added policy");
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
        double total = repo.purchaseItems(Map.of(5, 2), s.getId());
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
            () -> repo.setGlobalDiscount(badId, 10), "setGlobalDiscount should fail");
        assertThrows(IllegalArgumentException.class,
            () -> repo.removeGlobalDiscount(badId), "removeGlobalDiscount should fail");
        assertThrows(RuntimeException.class,
            () -> repo.addItemToShop(badId, 1, 1, 1), "addItemToShop should fail");
        assertThrows(RuntimeException.class,
            () -> repo.addReviewToShop(badId, 1, 5, "X"), "addReviewToShop should fail");
        assertThrows(RuntimeException.class,
            () -> repo.closeShop(badId), "closeShop should fail");
    }
}
