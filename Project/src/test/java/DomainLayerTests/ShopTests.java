package DomainLayerTests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.Item.ItemService;
import com.example.app.ApplicationLayer.Purchase.ShippingMethod;
import com.example.app.ApplicationLayer.Shop.ShopService;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Shop.IShopRepository;
import com.example.app.DomainLayer.Shop.Operator;
import com.example.app.DomainLayer.Shop.PurchasePolicy;
import com.example.app.DomainLayer.Shop.Shop;
import com.example.app.DomainLayer.Shop.ShopReview;
import com.example.app.DomainLayer.Shop.Discount.Policy;

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
    public void testShopCreation_sanityCheck_Success() {
        assertEquals(1, shop.getId());
        assertEquals("TestShop", shop.getName());
        // shippingMethod should be exactly the mock we injected
        assertSame(shippingMethod, shop.getShippingMethod());
    }

    @Test
    public void testShippingMethodSetter_Success() {
        ShippingMethod other = mock(ShippingMethod.class);
        shop.setShippingMethod(other);
        assertSame(other, shop.getShippingMethod());
    }

    @Test
    public void testAddReviewToShop_Success() {
        assertTrue(shop.getReviews().isEmpty());

        ShopReview review = new ShopReview(1, 5, "Great");
        shop.addReview(review);
        assertEquals(1, shop.getReviews().size());
        assertSame(review, shop.getReviews().get(0));

        shop.addReview(1, 3, "Okay");
        assertEquals(2, shop.getReviews().size());
    }

    @Test
    public void testAddReviewToShop_invaildRating_Failure() { 
        assertThrows(IllegalArgumentException.class,
            () -> shop.addReview(1, 0, "Bad")); // rating 0 is invalid
        assertThrows(IllegalArgumentException.class,
            () -> shop.addReview(1, 6, "Bad")); // rating 6 is invalid
    }

    @Test
    public void testAddReviewsToShopAndGetAverageRating_sanityCheck_Success() {
        assertTrue(shop.getReviews().isEmpty());
        assertEquals(-1.0, shop.getAverageRating());

        shop.addReview(1, 5, "Great");
        shop.addReview(new ShopReview(1, 3, "Okay"));

        List<ShopReview> reviews = shop.getReviews();
        assertEquals(2, reviews.size());
        assertEquals(4.0, shop.getAverageRating());
    }

    @Test
    public void testGetAverageRating_noReviews_Success() {
        assertTrue(shop.getReviews().isEmpty());
        assertEquals(-1.0, shop.getAverageRating());
    }

    @Test   
    public void testAddItemToShop_Success() {
        assertEquals(0, shop.getItemQuantity(100));
        shop.addItem(100, 2);
        assertEquals(2, shop.getItemQuantity(100));
    }   

    @Test
    public void testAddItemToShop_AlreadyExists_Success() {
        assertEquals(0, shop.getItemQuantity(100));
        shop.addItem(100, 2);
        assertEquals(2, shop.getItemQuantity(100));
        shop.addItem(100, 3);  // adding more to existing item
        assertEquals(5, shop.getItemQuantity(100));
    }

    @Test
    public void testAddItemToShop_NegativeQuantity_Failure() {
        shop.addItem(100, 2);
        assertThrows(IllegalArgumentException.class,
            () -> shop.addItem(100, -1)); // negative quantity
    }

    @Test
    public void testRemoveItemFromShop_Success() {
        shop.addItem(100, 2);
        assertEquals(2, shop.getItemQuantity(100));
        shop.removeItemFromShop(100);
        assertEquals(0, shop.getItemQuantity(100));
    }

    @Test
    public void testRemoveItemFromShop_Failure() {
        assertThrows(IllegalArgumentException.class,
            () -> shop.removeItemFromShop(100)); // item not in shop
    }

    @Test
    public void testGetItemIds_sanityCheck_Success() {
        shop.addItem(1, 1);
        shop.addItem(2, 1);
        List<Integer> ids = shop.getItemIds();
        assertTrue(ids.contains(1));
        assertTrue(ids.contains(2));
    }

    @Test
    public void testPriceUpdateAndRetrievl_Success() {
        shop.addItem(10, 2);
        assertEquals(0, shop.getItemPrice(10));
        shop.updateItemPrice(10, 50);
        assertEquals(50, shop.getItemPrice(10));
    }

    @Test 
    public void testNegativePriceUpdate_Failure() {
        assertThrows(IllegalArgumentException.class,
            () -> shop.updateItemPrice(10, -50)); // negative price
    }

    @Test 
    public void testPriceUpdateForNonExistingItem_Failure() {
        assertThrows(IllegalArgumentException.class,
            () -> shop.updateItemPrice(10, 50)); // item not in shop
    }

    @Test
    public void testPriceAndQuantityRemovedWhenItemRemoved_Success() {
        shop.addItem(300, 2);
        shop.updateItemPrice(300, 20);
        assertEquals(20, shop.getItemPrice(300));

        // remove completely
        shop.removeItemFromShop(300);
        assertEquals(0, shop.getItemQuantity(300));
        assertEquals(0, shop.getItemPrice(300));
    }

    @Test
    public void testPurchaseItems_NoDiscount_Success() {
        shop.addItem(100, 2);
        shop.updateItemPrice(100, 50);

        Map<Integer,Integer> list = Map.of(100, 2);
        // no discounts added, so total = 2 * 50 = 100.0
        double total = shop.purchaseItems(list, Collections.emptyMap());
        assertEquals(100.0, total);
    }

    @Test
    public void testGlobalDiscount_Success() {
        shop.addItem(101, 3);
        shop.updateItemPrice(101, 20);
        // 3 * 20 = 60
        // apply a 50% global discount (not double)
        shop.setGlobalDiscount(50, false);
        Map<Integer,Integer> list = Map.of(101, 3);
        double total = shop.purchaseItems(list, Collections.emptyMap());
        assertEquals(30.0, total);
    }

    // SingleDiscount tests
@Test
public void testSingleDiscount_Success() {
    // Arrange: one item, price 20, quantity 3 → total 60
    shop.addItem(1, 3);
    shop.updateItemPrice(1, 20);
    // Act: apply 50% discount to item 1 (not double)
    shop.setDiscountForItem(1, 50, false);
    double total = shop.purchaseItems(
        Map.of(1, 3),
        Collections.emptyMap()
    );
    // Assert: pay 50% of 60 = 30.0
    assertEquals(30.0, total, 1e-6);
}

@Test
public void testSingleDiscount_InvalidPercentageLow_Failure() {
    // Arrange: item exists so that setDiscountForItem reaches validation
    shop.addItem(2, 1);
    shop.updateItemPrice(2, 10);
    // Act & Assert: negative percentage should throw
    assertThrows(IllegalArgumentException.class, () ->
        shop.setDiscountForItem(2, -10, false)
    );
}

@Test
public void testSingleDiscount_InvalidPercentageHigh_Failure() {
    // Arrange: item exists so that setDiscountForItem reaches validation
    shop.addItem(3, 1);
    shop.updateItemPrice(3, 10);
    // Act & Assert: percentage >100 should throw
    assertThrows(IllegalArgumentException.class, () ->
        shop.setDiscountForItem(3, 150, false)
    );
}


    @Test
    public void testItemSpecificDiscount_Success() {
        shop.addItem(102, 5);
        shop.updateItemPrice(102, 10);
        // 5 * 10 = 50
        // apply a 40% discount to item 102 (not double)
        shop.setDiscountForItem(102, 40, false);
        Map<Integer,Integer> list = Map.of(102, 5);
        double total = shop.purchaseItems(list, Collections.emptyMap());
        // pay 60% of 50 = 30.0
        assertEquals(30.0, total);
    }

    @Test
    public void testItemSpecificDiscountAndGlobalDiscount_Success() {
        shop.addItem(103, 4);
        shop.updateItemPrice(103, 25);
        // 4 * 25 = 100
        // apply a 20% discount to item 103 (not double)
        shop.setDiscountForItem(103, 20, false);
        // then a 10% global discount (not double)
        shop.setGlobalDiscount(10, false);
        Map<Integer,Integer> list = Map.of(103, 4);
        double total = shop.purchaseItems(list, Collections.emptyMap());
        // best effective discount: 20% on item overrides 10% global => pay 80.0
        assertEquals(80.0, total);
    }
    

    /// Thread Safety Tests ///

    /**
     * UC—Concurrent Purchases: spawn 10 threads all calling purchaseItems(...)
     * and then verify that all succeed and the total revenue matches the expected value.
     */
    @Test
    public void testPurchaseItems_ConcurrentThreads_Success() throws Exception {
        // prepare shop: item #1, 100 units @ $5 each
        shop.addItem(1, 100);
        shop.updateItemPrice(1, 5);

        int threadCount = 10;
        // each thread will buy 10 units
        Map<Integer,Integer> purchaseBatch = Map.of(1, 10);

        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        List<Future<Double>> results = new ArrayList<>();

        // fire off N concurrent purchase tasks (no categories)
        for (int i = 0; i < threadCount; i++) {
            results.add(exec.submit(() ->
                shop.purchaseItems(purchaseBatch, Collections.emptyMap())
            ));
        }

        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.MINUTES);

        // collect successes and total revenue
        int successCount = 0;
        double totalRevenue = 0;
        for (Future<Double> f : results) {
            try {
                totalRevenue += f.get();
                successCount++;
            } catch (ExecutionException ee) {
                fail("Purchase threw unexpectedly: " + ee.getCause());
            }
        }

        // all threads should succeed
        assertEquals(threadCount, successCount);
        // each bought 10 * $5 = $50
        assertEquals(50 * threadCount, totalRevenue, 0.0001);
        // inventory should be zero
        assertEquals(0, shop.getItemQuantity(1));
    }


        /**
     * UC—Concurrent Reviews: spawn 10 threads all calling addReview(...)
     * and then verify that exactly 10 reviews were recorded and the
     * computed average matches the expected value.
     */
    @Test
    public void testAddReviews_ConcurrentThreads_Success() throws Exception {
        int threadCount = 10;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        try {
            // Launch 10 writer threads, each adding one review with a known rating pattern
            for (int i = 0; i < threadCount; i++) {
                final int rating = (i % 5) + 1;
                exec.submit(() -> {
                    // userId can be anything; use thread hash
                    shop.addReview(ThreadLocalRandom.current().nextInt(), rating, "Review " + rating);
                });
            }
        } finally {
            exec.shutdown();
            exec.awaitTermination(1, TimeUnit.MINUTES);
        }

        // Exactly 10 reviews must have been recorded
        assertEquals(threadCount, shop.getReviews().size(),
            "Expected exactly 10 reviews added concurrently");

        // Compute expected average = average of ratings (i % 5) + 1 for i = 0..9
        double expectedAvg = IntStream.range(0, threadCount)
                                    .map(i -> (i % 5) + 1)
                                    .average()
                                    .orElse(-1.0);
        assertEquals(expectedAvg, shop.getAverageRating(),
            1e-6, "Average rating should match the expected value");
    }



    @Test
    public void testAddReviews_ConcurrentAdding10Threads_Success() throws Exception {
        // each thread will add one review with rating ∈ [1..5]
        ExecutorService exec = Executors.newFixedThreadPool(10);
        try {
            for (int i = 0; i < 10; i++) {
                final int rating = (i % 5) + 1;
                exec.submit(() -> shop.addReview(ThreadLocalRandom.current().nextInt(), rating, "Review " + rating));
            }
        } finally {
            exec.shutdown();
            exec.awaitTermination(1, TimeUnit.MINUTES);
        }

        // exactly 10 reviews must have been added
        assertEquals(10, shop.getReviews().size());

        // compute expected average
        double expectedAvg = IntStream.range(0, 10)
                                    .map(i -> (i % 5) + 1)
                                    .average()
                                    .orElse(-1.0);
        assertEquals(expectedAvg, shop.getAverageRating());
    }

    /**
     * UC—Concurrent Read/Write: while 5 threads are adding reviews,
     * one thread continuously reads getAverageRating() to ensure no
     * exceptions and that intermediate values remain in a valid range.
     */
    @Test
    public void testGetAverageRating_ConcurrentReadsWhileAddingReviews_Success() throws Exception {
        ExecutorService exec = Executors.newFixedThreadPool(6);
        CountDownLatch startLatch = new CountDownLatch(1);

        // reader task: once started, repeatedly call getAverageRating()
        Future<?> reader = exec.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 100; i++) {
                    double avg = shop.getAverageRating();
                    // either no reviews yet (-1.0) or between 1 and 5
                    assertTrue(avg == -1.0 || (avg >= 1.0 && avg <= 5.0));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 5 writers
        for (int i = 1; i <= 5; i++) {
            final int rating = i;
            exec.submit(() -> {
                try {
                    startLatch.await();
                    shop.addReview(ThreadLocalRandom.current().nextInt(), rating, "R" + rating);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // start all tasks
        startLatch.countDown();

        // wait for completion
        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.MINUTES);
        reader.get();  // propagate assertion errors

        // after all, there must be exactly 5 reviews,
        // average = (1+2+3+4+5)/5 = 3.0
        assertEquals(5, shop.getReviews().size());
        assertEquals(3.0, shop.getAverageRating());
    }

    /**
     * UC—Concurrent Purchase vs. Removal:
     * Two threads try to purchase 3 units each of item 200 (initial stock 5)
     * while a third thread removes the item entirely. We confirm that
     * at most one purchase succeeds and the final quantity is 0.
     */
    @Test
    public void testPurchaseItems_ConcurrentPurchaseAndRemoval_Success() throws Exception {
        // prepare shop with item 200, qty 5 @ price 10
        shop.addItem(200, 5);
        shop.updateItemPrice(200, 10);

        ExecutorService exec = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);
        List<Exception> errors = new CopyOnWriteArrayList<>();

        // buyer task: try to buy 3 units
        Runnable buyer = () -> {
            try {
                startLatch.await();
                // updated signature: pass an empty category map
                shop.purchaseItems(Map.of(200, 3), Collections.emptyMap());
                successes.incrementAndGet();
            } catch (Exception e) {
                errors.add(e);
            }
        };

        // remover task: remove the item completely
        Runnable remover = () -> {
            try {
                startLatch.await();
                shop.removeItemFromShop(200);
            } catch (Exception e) {
                // shouldn't happen
                errors.add(e);
            }
        };

        // submit two buyers and one remover
        exec.submit(buyer);
        exec.submit(buyer);
        exec.submit(remover);

        // start them all
        startLatch.countDown();
        exec.shutdown();
        assertTrue(exec.awaitTermination(1, TimeUnit.MINUTES), "threads did not finish in time");

        // at most one buyer can succeed (stock was 5, each buyer wants 3)
        assertTrue(successes.get() <= 1,
            "Expected at most one successful purchase, but got " + successes.get());

        // after removal, item should be gone
        assertEquals(0, shop.getItemQuantity(200),
            "Expected item 200 to be completely removed");

        // at least one purchaser should have failed
        assertTrue(errors.size() >= 1,
            "Expected at least one buyer to see an error");
    }


    // UC1 – Concurrent restock & purchase (success)
    @Test
    public void testConcurrentRestockAndPurchase_Success() throws Exception {
        // prepare item 600 with price 10, initial stock 5
        shop.addItem(600, 5);
        shop.updateItemPrice(600, 10);
        Map<Integer,Integer> purchase = Map.of(600, 5);

        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Exception> purchaser = exec.submit(() -> {
            ready.countDown();
            start.await();
            try {
                // now pass empty category map
                shop.purchaseItems(purchase, Collections.emptyMap());
                return null;
            } catch (Exception e) {
                return e;
            }
        });

        Future<?> restocker = exec.submit(() -> {
            ready.countDown();
            start.await();
            shop.addItem(600, 5);
            return null;
        });

        ready.await();
        start.countDown();

        Exception purchaseEx = purchaser.get();
        restocker.get();
        exec.shutdown();

        assertNull(purchaseEx, "Purchase should succeed with sufficient stock");
        // final stock = 5 + 5 - 5 = 5
        assertEquals(5, shop.getItemQuantity(600),
            "Final stock should be 5 after concurrent restock and purchase");
    }

    // UC2 – Concurrent price update & purchase (success)
    @Test
    public void testConcurrentPriceUpdateAndPurchase_Success() throws Exception {
        shop.addItem(700, 1);
        shop.updateItemPrice(700, 20);
        Map<Integer,Integer> purchase = Map.of(700, 1);

        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Double> purchaser = exec.submit(() -> {
            ready.countDown();
            start.await();
            return shop.purchaseItems(purchase, Collections.emptyMap());
        });
        Future<?> priceUpdater = exec.submit(() -> {
            ready.countDown();
            start.await();
            shop.updateItemPrice(700, 30);
            return null;
        });

        ready.await();
        start.countDown();

        double total = purchaser.get();
        priceUpdater.get();
        exec.shutdown();

        assertTrue(total == 20.0 || total == 30.0,
            "Total should be either old or new price");
    }

    // UC3 – Category & multiple discounts interaction (success)
// Verifies that the best discount among item‐specific, global, and category is applied.
@Test
public void testCategoryAndMultipleDiscounts_Success() {
    // prepare two items
    shop.addItem(800, 2);
    shop.updateItemPrice(800, 100);
    shop.addItem(801, 1);
    shop.updateItemPrice(801, 200);

    // 20% off item 800
    shop.setDiscountForItem(800, 20, false);
    // 10% off entire shop
    shop.setGlobalDiscount(10, false);
    // 30% off everything in a given category
    ItemCategory cat = ItemCategory.ELECTRONICS;
    shop.setCategoryDiscount(cat, 30, false);

    // map each item to its category
    Map<Integer, ItemCategory> categories = Map.of(
        800, cat,
        801, cat
    );

    // perform purchase with category information
    double total = shop.purchaseItems(
        Map.of(800, 2, 801, 1),
        categories
    );

    // (2×100 + 1×200) = 400 → 30% off = 280
    assertEquals(280.0, total, 0.0001);
}

// UC4 – Rollback on discount failure (success)
// Ensures stock is restored if a discount throws during purchase.
@Test
public void testRollbackOnDiscountFailure_Success() throws Exception {
    shop.addItem(900, 5);
    shop.updateItemPrice(900, 50);

    // reflectively grab the private discounts list
    var f = Shop.class.getDeclaredField("discounts");
    f.setAccessible(true);
    @SuppressWarnings("unchecked")
    CopyOnWriteArrayList<com.example.app.DomainLayer.Shop.Discount.Discount> discounts =
        (CopyOnWriteArrayList<com.example.app.DomainLayer.Shop.Discount.Discount>) f.get(shop);

    // inject a “faulty” discount that always throws
    discounts.add(new com.example.app.DomainLayer.Shop.Discount.Discount() {
        @Override
        public Map<Integer, Double> applyDiscounts(
            Map<Integer, Integer> items,
            Map<Integer, AtomicInteger> prices,
            Map<Integer, Double> current,
            Map<Integer, ItemCategory> categories) {
            throw new RuntimeException("Bad discount");
        }
        @Override public boolean isDouble() { return false; }
        @Override
        public boolean checkPolicies(
            Map<Integer, Integer> items,
            Map<Integer, Double> prices,
            Map<Integer, ItemCategory> categories) {
            return true;
        }
        @Override
        public Integer getPercentage() {
            return 0;
        }
        @Override
        public Policy getPolicy() {
            return null; // no policy for this faulty discount
        }
        @Override
        public ItemCategory getItemCategory() {
            return null; // no category for this faulty discount
        }
        @Override
        public Integer getItemId() {
            return 900; // this discount applies to item 900
        }
    });

    // a purchase of 2 should throw, and rollback to 5
    assertThrows(RuntimeException.class,
        () -> shop.purchaseItems(Map.of(900,2), Collections.emptyMap()));
    assertEquals(5, shop.getItemQuantity(900),
        "Stock should be restored after discount failure");
}




    // UC5 – Concurrent reviews vs. average rating (success)
    // Starts multiple threads adding reviews while one reads average.
    @Test
    public void testConcurrentReviewsAndAverageRating_Success() throws Exception {
        int writers = 5;
        ExecutorService exec = Executors.newFixedThreadPool(writers + 1);
        CountDownLatch ready = new CountDownLatch(writers + 1);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();
        // writer threads
        for (int i = 0; i < writers; i++) {
            futures.add(exec.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                shop.addReview(1, 5, "Great");
                return null;
            }));
        }
        // reader thread
        futures.add(exec.submit(() -> {
            ready.countDown();
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            double avg = shop.getAverageRating();
            // allow initial -1.0 (no reviews yet) through 5.0
            assertTrue(avg >= -1.0 && avg <= 5.0,
                "Average should be between -1.0 and 5.0, got " + avg);
            return null;
        }));

        // kick them all off
        ready.await();
        start.countDown();

        // wait for completion
        for (Future<?> f : futures) {
            f.get();
        }
        exec.shutdown();

        // after all five 5-star reviews
        assertEquals(5.0, shop.getAverageRating(),
            "All five 5-star reviews should average to 5.0");
    }


    // UC6 – Permission/auth failures in ShopService (failure)
    // Mocks AuthTokenService and UserService to deny access.
    @Test
    public void testShopServicePermissionFailures_Failure() throws Exception {
        IShopRepository repo = mock(IShopRepository.class);
        AuthTokenService ats = mock(AuthTokenService.class);
        ItemService is = mock(ItemService.class);
        UserService us = mock(UserService.class);
        ShopService ss = new ShopService(repo, ats, us, is);

        // invalid token should be rejected
        when(ats.ValidateToken("bad")).thenThrow(new RuntimeException("Invalid token"));
        assertThrows(RuntimeException.class,
            () -> ss.createShop("X", mock(PurchasePolicy.class), mock(ShippingMethod.class), "bad"));

        // valid token but no permission to set policy → failure
        when(ats.ValidateToken("tok")).thenReturn(1);
        when(us.hasPermission(1, PermissionsEnum.setPolicy, 2)).thenReturn(false);
        assertThrows(RuntimeException.class,
            () -> ss.setGlobalDiscount(2, 10, true, "tok"));
    }


    // UC8 – Bulk purchase of multiple items (success)
    // Purchases more than one SKU in one call and checks stock adjustment.
    @Test
    public void testBulkPurchaseOfMultipleItems_Success() {
        shop.addItem(1000, 3);
        shop.updateItemPrice(1000, 10);
        shop.addItem(1001, 2);
        shop.updateItemPrice(1001, 20);

        // no per-item categories, so pass empty map
        double total = shop.purchaseItems(
            Map.of(1000, 2, 1001, 1),
            Collections.emptyMap()
        );
        assertEquals(2 * 10 + 1 * 20, total);
        assertEquals(1, shop.getItemQuantity(1000));
        assertEquals(1, shop.getItemQuantity(1001));
    }

    /**
    * Verifies that a per‐item discount only applies when its purchase policy is satisfied.
    * Here we create a policy requiring at least 3 units of item 42 in the cart,
    * then attach a 50% discount to item 42 under that policy.
    * The test checks that the discount is not applied when the policy is not met.
    */
    @Test
    public void testPolicyDrivenSingleItemDiscount_NoDiscount() {
        // Arrange: add item 42, price 10
        shop.addItem(42, 5);
        shop.updateItemPrice(42, 10);

        // 1) Define a policy: require at least 3 of item 42
        // threshold = 3, itemId = 42, no category, basketValue unused, for first no opearator
        shop.addPolicy(3, 42, null, 0.0, null);

        // 2) Create a 50% discount on item 42 under that policy (not double)
        shop.setDiscountForItem(42, 50, false);

        // Act & Assert #1: buy only 2 units → policy not met → no discount
        double total2 = shop.purchaseItems(
            Map.of(42, 2),
            Collections.emptyMap()
        );
        // pay full price 2×10 = 20.0
        assertEquals(20.0, total2, 1e-6, "Policy not met → no discount should apply");
    }

     /**
    * Verifies that a per‐item discount only applies when its purchase policy is satisfied.
    * Here we create a policy requiring at least 3 units of item 42 in the cart,
    * then attach a 50% discount to item 42 under that policy.
    * The test checks that the discount is applied when the policy is met.
    */
    @Test
    public void testPolicyDrivenSingleItemDiscount_Success() {
        // Arrange: add item 42, price 10
        shop.addItem(42, 5);
        shop.updateItemPrice(42, 10);

        // 1) Define a policy: require at least 3 of item 42
        // threshold = 3, itemId = 42, no category, basketValue unused, for first no opearator
        shop.addPolicy(3, 42, null, 0.0, null);

        // 2) Create a 50% discount on item 42 under that policy (not double)
        shop.setDiscountForItem(42, 50, false);

        // Act & Assert #2: buy 3 units → policy met → 50% discount applies
        double total3 = shop.purchaseItems(
            Map.of(42, 3),
            Collections.emptyMap()
        );
        // pay 50% of 3×10 = 15.0
        assertEquals(15.0, total3, 1e-6, "Policy met → 50% discount should apply");
    }


     /**
     * Verifies that the same SingleDiscount does NOT apply when one of the
     * AND‐combined policies is not satisfied.
     * Here the basket threshold is raised so that basketValue=40 fails.
     */
    @Test
    public void testCompositePolicySingleDiscount_AND_Failure() {
        // Arrange: item 42, price 10
        shop.addItem(42, 5);
        shop.updateItemPrice(42, 10);

        // Policy1: require ≥2 units of item 42
        shop.addPolicy(2, 42, null, 0.0, null);
        // Policy2: require basket total ≥40 (3×10=30 < 40)
        shop.addPolicy(null, null, null, 40.0, Operator.AND);

        // Attach 50% discount under the composite policy
        shop.setDiscountForItem(42, 50, false);

        // Act: purchase 3 units → first policy OK, second fails
        double total = shop.purchaseItems(
            Map.of(42, 3),
            Collections.emptyMap()
        );

        // Assert: no discount → pay full 3×10 = 30.0
        assertEquals(30.0, total, 1e-6,
            "Second policy not met → discount should NOT apply");
    }

    /**
     * Verifies that a SingleDiscount with two policies combined by OR
     * applies if at least one policy is satisfied, and is skipped only if both fail.
     */
    @Test
    public void testCompositePolicySingleDiscount_OR_Success() {
        // Arrange: item 42, price 10
        shop.addItem(42, 5);
        shop.updateItemPrice(42, 10);

        // Policy1: require ≥4 units of item 42
        shop.addPolicy(4, 42, null, 0.0, null);
        // Policy2: require basket total ≥20
        shop.addPolicy(null, null, null, 20.0, Operator.OR);

        // Attach 25% discount under the composite (OR) policy
        shop.setDiscountForItem(42, 25, false);

        // Act & Assert #1: purchase 2 units → total=20, second policy met → discount applies
        double total2 = shop.purchaseItems(
            Map.of(42, 2),
            Collections.emptyMap()
        );
        assertEquals(15.0, total2, 1e-6,
            "BasketValue policy met (OR) → discount should apply (2×10=20→25% off=15)");

        // Act & Assert #2: purchase 1 unit → total=10, both policies fail → no discount
        double total1 = shop.purchaseItems(
            Map.of(42, 1),
            Collections.emptyMap()
        );
        assertEquals(10.0, total1, 1e-6,
            "Neither policy met → discount should NOT apply");
    }

    /**
     * Verifies that a SingleDiscount with two policies combined by XOR
     * applies if exactly one policy is satisfied, and is skipped if both are met or both fail.
     */
    @Test
    public void testCompositePolicySingleDiscount_XOR_Success() {
        // Arrange: item 42, price 10
        shop.addItem(42, 5);
        shop.updateItemPrice(42, 10);

        // Policy1: require ≥4 units of item 42
        shop.addPolicy(4, 42, null, 0.0, null);
        // Policy2: require basket total ≥20
        shop.addPolicy(null, null, null, 20.0, Operator.XOR);

        // Attach 25% discount under the composite (XOR) policy
        shop.setDiscountForItem(42, 25, false);

        // Act & Assert #1: purchase 2 units → total=20, second policy met → discount applies
        double total2 = shop.purchaseItems(
            Map.of(42, 2),
            Collections.emptyMap()
        );
        assertEquals(15.0, total2, 1e-6,
            "BasketValue policy met (XOR) → discount should apply (2×10=20→25% off=15)");

        // Act & Assert #2: purchase 1 unit → total=10, both policies fail → no discount
        double total1 = shop.purchaseItems(
            Map.of(42, 1),
            Collections.emptyMap()
        );
        assertEquals(10.0, total1, 1e-6,
            "Neither policy met → discount should NOT apply");
    }

    @Test
    public void testConcurrentAddPolicy_OneSucceedsOneFails() throws Exception {
        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        AtomicReference<Exception> exA = new AtomicReference<>();
        AtomicReference<Exception> exB = new AtomicReference<>();

        // Thread A: valid operator → should succeed
        Future<?> fa = exec.submit(() -> {
            ready.countDown();
            try {
                go.await();
                shop.addPolicy(3, 42, null, 0.0, null);
            } catch (Exception e) {
                exA.set(e);
            }
        });

        // Thread B: null operator → should throw immediately
        Future<?> fb = exec.submit(() -> {
            ready.countDown();
            try {
                go.await();
                Thread.sleep(100); // ensure A runs first
                shop.addPolicy(2, 43, null, 0.0, null);
            } catch (Exception e) {
                exB.set(e);
            }
        });

        // start both
        ready.await();
        go.countDown();

        // wait for both to finish
        fa.get();
        fb.get();
        exec.shutdown();

        // Thread A: no exception
        assertNull(exA.get(), "Thread A should succeed adding its policy");

        // Thread B: IllegalArgumentException due to null operator
        assertNotNull(exB.get(), "Thread B should have thrown");
        assertTrue(exB.get() instanceof IllegalArgumentException);
        assertTrue(
            exB.get().getMessage().contains("Operator cannot be null"),
            "Expected message to indicate null-operator, but was: " + exB.get().getMessage()
        );
    }

     @Test
    void testGetPurchasePolicies_ReturnsNull() {
        assertNull(shop.getPurchasePolicies());
    }

    @Test
    void testGetReviews_Unmodifiable() {
        shop.addReview(1, 5, "Great");
        assertThrows(UnsupportedOperationException.class,
            () -> shop.getReviews().add(new ShopReview(2, 3, "Oops")));
    }

    @Test
    void testGetItemQuantity_AbsentAndPresent() {
        assertEquals(0, shop.getItemQuantity(123));
        shop.addItem(123, 4);
        assertEquals(4, shop.getItemQuantity(123));
    }

    @Test
    void testRemoveItemQuantity_SuccessAndRemoval() {
        shop.addItem(10, 5);
        shop.removeItemQuantity(10, 3);
        assertEquals(2, shop.getItemQuantity(10));
        // remove the rest
        shop.removeItemQuantity(10, 2);
        assertEquals(0, shop.getItemQuantity(10));
    }

    @Test
    void testRemoveItemQuantity_Invalid() {
        assertThrows(IllegalArgumentException.class,
            () -> shop.removeItemQuantity(1, -1));
        assertThrows(IllegalArgumentException.class,
            () -> shop.removeItemQuantity(99, 1));
    }

    @Test
    void testGetItemIds_Sanity() {
        shop.addItem(1,1);
        shop.addItem(2,1);
        assertTrue(shop.getItemIds().containsAll(java.util.List.of(1,2)));
    }

    @Test
    void testUpdateItemPriceAndGetPrice() {
        shop.addItem(50, 2);
        assertEquals(0, shop.getItemPrice(50));
        shop.updateItemPrice(50, 30);
        assertEquals(30, shop.getItemPrice(50));
    }

    @Test
    void testUpdateItemPrice_Invalid() {
        assertThrows(IllegalArgumentException.class,
            () -> shop.updateItemPrice(5, -10));
        assertThrows(IllegalArgumentException.class,
            () -> shop.updateItemPrice(999, 20));
    }

    @Test
    void testGetTotalPrice_MissingAndPresent() {
        shop.addItem(100, 2);
        shop.updateItemPrice(100, 5);   // total = 10
        shop.addItem(101, 3);           // no price set = 0
        Map<Integer,Integer> cart = Map.of(100,2, 101,3);
        assertEquals(10, shop.getTotalPrice(cart));
    }

    @Test
    void testRemoveGlobalDiscount_NoOp() {
        // no exception if none set
        shop.removeGlobalDiscount();
    }

    @Test
    void testRemoveDiscountForItem_NoOp() {
        shop.removeDiscountForItem(42);
    }

    @Test
    void testRemoveCategoryDiscount_NoOp() {
        shop.removeCategoryDiscount(ItemCategory.BOOKS);
    }

    @Test
    void testAddPolicy_FirstAndCompositeBranches() {
        // first call: threshold-only
        assertDoesNotThrow(() ->
            shop.addPolicy(2, 10, null, 0.0, null)
        );
        // second without operator should fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            shop.addPolicy(1, 10, null, 0.0, null)
        );
        assertTrue(ex.getMessage().contains("Operator cannot be null"));

        // second with operator works
        Shop s2 = new Shop(2, "X", shippingMethod);
        assertDoesNotThrow(() -> {
            s2.addPolicy(2, 20, null, 0.0, null);
            s2.addPolicy(3, 20, null, 0.0, Operator.AND);
        });

        // third without operator on same shop should now fail on "policyComposite not null"
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
            s2.addPolicy(4, 20, null, 0.0, null)
        );
        assertTrue(ex2.getMessage().contains("adding discount+policy in progress"));

        // third with operator OK
        assertDoesNotThrow(() ->
            s2.addPolicy(5, 20, null, 0.0, Operator.OR)
        );
    }

    @Test
    void testShopReview_toString() {
        ShopReview r = new ShopReview(7, 4, "Nice");
        String s = r.toString();
        assertTrue(s.contains("rating=4"));
        assertTrue(s.contains("reviewText='Nice'"));
    }

    @Test
    void testOperatorEnum() {
        assertEquals(Operator.AND, Operator.valueOf("AND"));
        assertEquals(3, Operator.values().length);
    }

    @Test
    void testPurchaseItems_InsufficientStock_Rollback() {
        shop.addItem(200, 1);
        Map<Integer,Integer> cart = Map.of(200, 2);
        assertThrows(IllegalArgumentException.class, () ->
            shop.purchaseItems(cart, Collections.emptyMap())
        );
        // original stock should remain
        assertEquals(1, shop.getItemQuantity(200));
    }

    @Test
    void testRollBackPurchase() {
        shop.addItem(300, 5);
        // simulate rollback of 3 units
        shop.rollBackPurchase(Map.of(300, 3));
        assertEquals(8, shop.getItemQuantity(300));
    }
}       
