package DomainLayerTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import DomainLayer.Shop.Shop;
import DomainLayer.Shop.ShopReview;

public class ShopTests {

    private Shop shop;

    @BeforeEach
    public void setup() {
        // id=1, name="TestShop", policy="Default", global discount=10
        shop = new Shop(1, "TestShop", "DefaultPolicy", 10);
    }

    @Test
    public void testImmutableFields() {
        assertEquals(1, shop.getId());
        assertEquals("TestShop", shop.getName());
    }

    @Test
    public void testPurchasePolicySetter() {
        assertEquals("DefaultPolicy", shop.getPurchasePolicy());
        shop.setPurchasePolicy("NewPolicy");
        assertEquals("NewPolicy", shop.getPurchasePolicy());
    }

    @Test
    public void testGlobalDiscount() {
        assertEquals(10, shop.getGlobalDiscount());
        assertEquals(10, shop.getDiscountForItem(42)); // no specific => global
        shop.setGlobalDiscount(20);
        assertEquals(20, shop.getGlobalDiscount());
        assertEquals(20, shop.getDiscountForItem(99));
    }

    @Test
    public void testItemSpecificDiscount() {
        shop.setDiscountForItem(5, 15);
        // specific < global => global applies
        shop.setGlobalDiscount(20);
        assertEquals(20, shop.getDiscountForItem(5));
        // specific > global
        shop.setDiscountForItem(5, 25);
        assertEquals(25, shop.getDiscountForItem(5));
    }

    @Test
    public void testReviewsAndAverageRating() {
        assertTrue(shop.getReviews().isEmpty());
        assertEquals(0.0, shop.getAverageRating());
        shop.addReview(1,5, "Great");
        shop.addReview(new ShopReview(1,3, "Okay"));
        List<ShopReview> reviews = shop.getReviews();
        assertEquals(2, reviews.size());
        assertEquals(4.0, shop.getAverageRating());
    }

    @Test
    public void testItemAddAndQuantity() {
        assertEquals(0, shop.getItemQuantity(100));
        shop.addItem(100, 4);
        assertEquals(4, shop.getItemQuantity(100));
        shop.addItem(100, 2);
        assertEquals(6, shop.getItemQuantity(100));
    }

    @Test
    public void testRemoveItemPartialAndFull() {
        shop.addItem(200, 5);
        shop.removeItem(200, 2);
        assertEquals(3, shop.getItemQuantity(200));
        // remove remaining
        shop.removeItem(200, 3);
        assertEquals(0, shop.getItemQuantity(200));
        // add again then remove completely via -1
        shop.addItem(200, 7);
        shop.removeItem(200, -1);
        assertEquals(0, shop.getItemQuantity(200));
    }

    @Test
    public void testGetItemIds() {
        shop.addItem(1, 1);
        shop.addItem(2, 1);
        List<Integer> ids = shop.getItemIds();
        assertTrue(ids.contains(1));
        assertTrue(ids.contains(2));
    }

    @Test
    public void testPriceUpdateAndRetrieval() {
        assertEquals(0, shop.getItemPrice(10));
        shop.updateItemPrice(10, 50);
        assertEquals(50, shop.getItemPrice(10));
        shop.updateItemPrice(10, 75);
        assertEquals(75, shop.getItemPrice(10));
    }

    @Test
    public void testPriceRemovedWhenItemRemoved() {
        shop.addItem(300, 2);
        shop.updateItemPrice(300, 20);
        assertEquals(20, shop.getItemPrice(300));
        // remove completely
        shop.removeItem(300, -1);
        assertEquals(0, shop.getItemQuantity(300));
        assertEquals(0, shop.getItemPrice(300));
    }

    @Test
    public void testInvalidPriceThrows() {
        assertThrows(IllegalArgumentException.class, () -> shop.updateItemPrice(5, -10));
    }

    @Test
    public void testInvalidRemoveQuantityThrows() {
        assertThrows(IllegalArgumentException.class, () -> shop.removeItem(5, 0));
        assertThrows(IllegalArgumentException.class, () -> shop.removeItem(5, -2)); // other than -1
    }
}