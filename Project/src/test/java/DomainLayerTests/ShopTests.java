package DomainLayerTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ApplicationLayer.Purchase.ShippingMethod;
import DomainLayer.Shop.Shop;
import DomainLayer.Shop.ShopReview;

public class ShopTests {

    private Shop shop;

    @Mock
    private ShippingMethod shippingMethod;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        // id=1, name="TestShop", shippingMethod = mock
        shop = new Shop(1, "TestShop", shippingMethod);
    }

    @Test
    public void testImmutableFields() {
        assertEquals(1, shop.getId());
        assertEquals("TestShop", shop.getName());
        // shippingMethod should be exactly the mock we injected
        assertSame(shippingMethod, shop.getShippingMethod());
    }

    @Test
    public void testShippingMethodSetter() {
        ShippingMethod other = mock(ShippingMethod.class);
        shop.setShippingMethod(other);
        assertSame(other, shop.getShippingMethod());
    }

    @Test
    public void testReviewsAndAverageRating() {
        assertTrue(shop.getReviews().isEmpty());
        assertEquals(0.0, shop.getAverageRating());

        shop.addReview(1, 5, "Great");
        shop.addReview(new ShopReview(1, 3, "Okay"));

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
        shop.removeItemQuantity(200, 2);
        assertEquals(3, shop.getItemQuantity(200));

        // remove remaining via removeItemQuantity
        shop.removeItemQuantity(200, 3);
        assertEquals(0, shop.getItemQuantity(200));

        // add again then remove completely via removeItemFromShop
        shop.addItem(200, 7);
        shop.removeItemFromShop(200);
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
        shop.removeItemFromShop(300);
        assertEquals(0, shop.getItemQuantity(300));
        assertEquals(0, shop.getItemPrice(300));
    }

    @Test
    public void testInvalidPriceThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> shop.updateItemPrice(5, -10));
    }

    @Test
    public void testInvalidRemoveQuantityThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> shop.removeItemQuantity(5, 0));
        assertThrows(IllegalArgumentException.class,
            () -> shop.removeItemQuantity(5, -2)); // negative other than full removal
    }

    @Test
    public void testPurchaseItems_NoDiscount() {
        shop.addItem(100, 2);
        shop.updateItemPrice(100, 50);

        Map<Integer,Integer> list = Map.of(100, 2);
        // no discounts added, so total = 2 * 50 = 100.0
        assertEquals(100.0, shop.purchaseItems(list));
    }

    @Test
    public void testGlobalDiscount() {
        shop.addItem(101, 3);
        shop.updateItemPrice(101, 20);
        // 3 * 20 = 60
        shop.setGlobalDiscount(50); // 50% off
        Map<Integer,Integer> list = Map.of(101, 3);
        assertEquals(30.0, shop.purchaseItems(list));
    }

    @Test
    public void testItemSpecificDiscount() {
        shop.addItem(102, 5);
        shop.updateItemPrice(102, 10);
        // 5 * 10 = 50
        shop.setDiscountForItem(102, 40); // 40% off this item
        Map<Integer,Integer> list = Map.of(102, 5);
        // pay 60% of 50 = 30.0
        assertEquals(30.0, shop.purchaseItems(list));
    }
}
