package DomainLayerTests;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Test
    void testConcurrentAddItem() throws InterruptedException {
        // Create a single purchase
        Purchase purchase = new Purchase(1, 10, 20, null);

        final int THREAD_COUNT = 20;
        final int INCREMENTS_PER_THREAD = 1000;  // each thread adds item (id=99) 1,000 times

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // Spawn threads that all modify the same Purchase object
        for (int t = 0; t < THREAD_COUNT; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < INCREMENTS_PER_THREAD; i++) {
                        purchase.addItem(99, 1);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();     // wait for all threads to finish
        executor.shutdown();

        // In a perfectly synchronized scenario, item #99's quantity should be 20,000.
        // But since Purchase's addItem() is not thread-safe, this might fail sporadically.
        Integer finalQuantity = purchase.getItems().get(99);

        // Note: This test could intermittently pass or fail if there's a race condition.
        // If concurrency is unsafely handled, you might see a number < 20000.
        // The test is considered "failed" if the final quantity isn't what we expect:
        assertEquals(20_000, finalQuantity,
                "Race condition detected! The final quantity is incorrect due to unsynchronized access.");
    }
}
