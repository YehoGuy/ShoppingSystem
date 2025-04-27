package InfrastructureLayerTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ApplicationLayer.Purchase.ShippingMethod;
import DomainLayer.Shop.PurchasePolicy;
import DomainLayer.Shop.Shop;
import InfrastructureLayer.ShopRepository;

/**
 * Now that ShopRepository#createShop takes a PurchasePolicy and a ShippingMethod,
 * we mock those collaborators here.
 */
public class ShopRepositoryTests {

    private ShopRepository repo;

    @Mock
    private PurchasePolicy purchasePolicy;

    @Mock
    private ShippingMethod shippingMethod;

    @BeforeEach
    public void setup() {
        // Initialize @Mock fields
        MockitoAnnotations.openMocks(this);
        repo = new ShopRepository();
    }

    @Test
    public void testCreateAndGetShop() {
        // 1) Create a new shop
        Shop s = repo.createShop("MyShop", purchasePolicy, shippingMethod);

        // 2) Basic assertions on the returned Shop
        assertNotNull(s);
        assertEquals("MyShop", s.getName());
        // it should have registered the policy and shippingMethod internally
        assertTrue(s.getPurchasePolicies().contains(purchasePolicy));
        assertSame(shippingMethod, s.getShippingMethod());

        // 3) getShop must return the exact same instance
        Shop fetched = repo.getShop(s.getId());
        assertSame(s, fetched);
    }

    @Test
    public void testGetAllShops() {
        repo.createShop("A", purchasePolicy, shippingMethod);
        repo.createShop("B", purchasePolicy, shippingMethod);

        List<Shop> all = repo.getAllShops();
        assertEquals(2, all.size());
    }

    @Test
    public void testUpdatePurchasePolicy() {
        Shop s = repo.createShop("S", purchasePolicy, shippingMethod);
        // swap in a different mock
        PurchasePolicy anotherPolicy = mock(PurchasePolicy.class);

        repo.updatePurchasePolicy(s.getId(), anotherPolicy);
        assertTrue(repo.getShop(s.getId()).getPurchasePolicies().contains(anotherPolicy));
    }

    @Test
    public void testCloseShop() {
        Shop s = repo.createShop("S", purchasePolicy, shippingMethod);
        repo.closeShop(s.getId());
        assertThrows(IllegalArgumentException.class, () -> repo.getShop(s.getId()));
    }

    @Test
    public void testNonexistentShopThrows() {
        assertThrows(IllegalArgumentException.class, () -> repo.updatePurchasePolicy(999, purchasePolicy));
        assertThrows(IllegalArgumentException.class, () -> repo.setGlobalDiscount(999, 10));
        assertThrows(IllegalArgumentException.class, () -> repo.addItemToShop(999, 1, 1, 100));
        assertThrows(IllegalArgumentException.class, () -> repo.addReviewToShop(999, 5, 5, "x"));
        assertThrows(IllegalArgumentException.class, () -> repo.removeSupply(999, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> repo.closeShop(999));
    }

    // ... you can add more tests for discounts, items, supply, etc.
}
