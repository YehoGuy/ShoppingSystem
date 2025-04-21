package InfrastructureLayerTests;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import DomainLayer.Shop.Shop;
import InfrastructureLayer.ShopRepository;

public class ShopRepositoryTests {

    private ShopRepository repo;

    @BeforeEach
    public void setup() {
        repo = new ShopRepository();
    }

    @Test
    public void testCreateAndGetShop() {
        Shop s = repo.createShop("MyShop", "No returns", 5);
        assertNotNull(s);
        assertEquals("MyShop", s.getName());
        assertEquals("No returns", s.getPurchasePolicy());
        assertEquals(5, s.getGlobalDiscount());

        Shop fetched = repo.getShop(s.getId());
        assertSame(s, fetched);
    }

    @Test
    public void testGetAllShops() {
        repo.createShop("A", "P1", 1);
        repo.createShop("B", "P2", 2);
        List<Shop> all = repo.getAllShops();
        assertEquals(2, all.size());
    }

    @Test
    public void testUpdatePurchasePolicy() {
        Shop s = repo.createShop("S", "Policy1", 0);
        repo.updatePurchasePolicy(s.getId(), "Policy2");
        assertEquals("Policy2", repo.getShop(s.getId()).getPurchasePolicy());
    }

    @Test
    public void testDiscounts() {
        Shop s = repo.createShop("S", "P", 10);
        // global only
        assertEquals(10, s.getGlobalDiscount());
        assertEquals(10, s.getDiscountForItem(1));

        repo.setGlobalDiscount(s.getId(), 20);
        assertEquals(20, s.getGlobalDiscount());

        repo.setDiscountForItem(s.getId(), 42, 5);
        // item specific less than global => global applies
        assertEquals(20, s.getDiscountForItem(42));
        // item specific greater
        repo.setDiscountForItem(s.getId(), 42, 25);
        assertEquals(25, s.getDiscountForItem(42));
    }

    @Test
    public void testReviewsAndAverage() {
        Shop s = repo.createShop("S", "P", 0);
        assertEquals(0.0, repo.getShopAverageRating(s.getId()));
        repo.addReviewToShop(s.getId(),1, 4, "Good");
        repo.addReviewToShop(s.getId(),1, 2, "Bad");
        assertEquals(3.0, repo.getShopAverageRating(s.getId()));
    }

    @Test
    public void testAddItemAndQuantity() {
        Shop s = repo.createShop("S", "P", 0);
        repo.addItemToShop(s.getId(), 7, 3, 100);
        assertEquals(3, repo.getItemQuantityFromShop(s.getId(), 7));
        assertEquals(100, s.getItemPrice(7));

        // add more
        repo.addItemToShop(s.getId(), 7, 2, 150);
        assertEquals(5, repo.getItemQuantityFromShop(s.getId(), 7));
        assertEquals(150, s.getItemPrice(7));
    }

    @Test
    public void testUpdateItemPrice() {
        Shop s = repo.createShop("S", "P", 0);
        repo.addItemToShop(s.getId(), 8, 1, 50);
        repo.updateItemPriceInShop(s.getId(), 8, 75);
        assertEquals(75, s.getItemPrice(8));
    }

    @Test
    public void testRemoveItemCompletely() {
        Shop s = repo.createShop("S", "P", 0);
        repo.addItemToShop(s.getId(), 9, 4, 20);
        repo.removeItemFromShop(s.getId(), 9);
        assertEquals(0, repo.getItemQuantityFromShop(s.getId(), 9));
        assertEquals(0, s.getItemPrice(9));
    }

    @Test
    public void testCheckSupplyAndRemoveSupply() {
        Shop s = repo.createShop("S", "P", 0);
        repo.addItemToShop(s.getId(), 10, 5, 30);
        assertTrue(repo.checkSupplyAvailability(s.getId(), 10));
        repo.removeSupply(s.getId(), 10, 3);
        assertEquals(2, repo.getItemQuantityFromShop(s.getId(), 10));
        repo.removeSupply(s.getId(), 10, 2);
        assertFalse(repo.checkSupplyAvailability(s.getId(), 10));
    }

    @Test
    public void testGetItemsList() {
        Shop s = repo.createShop("S", "P", 0);
        repo.addItemToShop(s.getId(), 1, 1, 10);
        repo.addItemToShop(s.getId(), 2, 1, 20);
        List<Integer> ids = repo.getItemsByShop(s.getId());
        assertTrue(ids.contains(1));
        assertTrue(ids.contains(2));
    }

    @Test
    public void testCloseShop() {
        Shop s = repo.createShop("S", "P", 0);
        repo.closeShop(s.getId());
        assertThrows(Exception.class, () -> repo.getShop(s.getId()).getId());
    }

    @Test
    public void testNonexistentShopThrows() {
        assertThrows(IllegalArgumentException.class, () -> repo.updatePurchasePolicy(999, "X"));
        assertThrows(IllegalArgumentException.class, () -> repo.setGlobalDiscount(999, 1));
        assertThrows(IllegalArgumentException.class, () -> repo.addItemToShop(999, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> repo.addReviewToShop(999, 1, 5, ""));
        assertThrows(IllegalArgumentException.class, () -> repo.removeSupply(999, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> repo.closeShop(999));
    }
}