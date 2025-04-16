package DomainLayerTests;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import DomainLayer.Purchase.Address;
import DomainLayer.Purchase.Purchase;

class PurchaseTests {

    private Purchase purchase;
    private Address address;

    @BeforeEach
    void setUp() {
        // Suppose purchaseId=1, userId=10, storeId=20
        address = new Address().withCountry("USA").withCity("New York");
        purchase = new Purchase(1, 10, 20, Map.of(101, 2, 102, 5), address);
    }

    @Test
    void testInitialState() {
        assertFalse(purchase.isCompleted(), "Purchase should initially be incomplete");
        assertEquals(10, purchase.getUserId());
        assertEquals(20, purchase.getStoreId());
        assertEquals(2, purchase.getItems().size(), "Should have 2 item entries");
        assertNotNull(purchase.getShippingAddress());
    }

    @Test
    void testAddItem() {
        purchase.addItem(103, 3);
        assertEquals(3, purchase.getItems().size());
        assertTrue(purchase.getItems().containsKey(103));
    }

    @Test
    void testAddItemExisting() {
        purchase.addItem(101, 3); // item 101 already in the map with quantity=2
        assertEquals(2, purchase.getItems().size());   // still 2 distinct item IDs
        assertEquals(5, purchase.getItems().get(101)); // 2 + 3 = 5
    }

    @Test
    void testRemoveItem() {
        purchase.removeItem(102, 2); // item 102 was 5 initially
        assertEquals(3, purchase.getItems().get(102));
    }

    @Test
    void testRemoveItemAllQuantity() {
        purchase.removeItem(101, 2); // removing entire quantity of 101
        assertFalse(purchase.getItems().containsKey(101));
    }

    @Test
    void testCompletePurchase() {
        int result = purchase.completePurchase();
        assertTrue(purchase.isCompleted());
        assertNotNull(purchase.getTimeOfCompletion());
        assertEquals(1, result, "Default completePurchase returns '1' as a success indicator");
    }

    @Test
    void testCancelPurchase() {
        purchase.completePurchase();
        purchase.cancelPurchase();
        assertFalse(purchase.isCompleted());
        assertNull(purchase.getTimeOfCompletion());
    }
}
