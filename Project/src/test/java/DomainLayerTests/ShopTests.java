package DomainLayerTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.stream.IntStream;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.Item.ItemService;
import ApplicationLayer.Purchase.ShippingMethod;
import ApplicationLayer.Shop.ShopService;
import ApplicationLayer.User.UserService;
import DomainLayer.Item.Item;
import DomainLayer.Roles.PermissionsEnum;
import DomainLayer.Shop.Discount;
import DomainLayer.Shop.IShopRepository;
import DomainLayer.Shop.PurchasePolicy;
import DomainLayer.Shop.Shop;
import DomainLayer.Shop.ShopReview;
import InfrastructureLayer.ShopRepository;


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
        assertEquals(100.0, shop.purchaseItems(list));
    }

    @Test
    public void testGlobalDiscount_Success() {
        shop.addItem(101, 3);
        shop.updateItemPrice(101, 20);
        // 3 * 20 = 60
        shop.setGlobalDiscount(50); // 50% off
        Map<Integer,Integer> list = Map.of(101, 3);
        assertEquals(30.0, shop.purchaseItems(list));
    }

    @Test
    public void testItemSpecificDiscount_Success() {
        shop.addItem(102, 5);
        shop.updateItemPrice(102, 10);
        // 5 * 10 = 50
        shop.setDiscountForItem(102, 40); // 40% off this item
        Map<Integer,Integer> list = Map.of(102, 5);
        // pay 60% of 50 = 30.0
        assertEquals(30.0, shop.purchaseItems(list));
    }

    @Test
    public void testItemSpecificDiscountAndGlobalDiscount_Success() {
        shop.addItem(103, 4);
        shop.updateItemPrice(103, 25);
        // 4 * 25 = 100
        shop.setDiscountForItem(103, 20); // 20% off this item
        shop.setGlobalDiscount(10); // 10% off everything
        Map<Integer,Integer> list = Map.of(103, 4);
        assertEquals(80.0, shop.purchaseItems(list)); // take the higher discount
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

        // fire off N concurrent purchase tasks
        for (int i = 0; i < threadCount; i++) {
            results.add(exec.submit(() -> shop.purchaseItems(purchaseBatch)));
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
        // each bought 10*$5 = $50
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
        public void testPurchaseItems_ConcurrentThreads_InsufficientStock_Failure() throws Exception {
            // prepare shop: item #1, only 50 units @ $5 each
            shop.addItem(1, 50);
            shop.updateItemPrice(1, 5);

            int threadCount = 10;
            // each thread will attempt to buy 10 units (total demand = 100)
            Map<Integer,Integer> purchaseBatch = Map.of(1, 10);

            ExecutorService exec = Executors.newFixedThreadPool(threadCount);
            List<Future<Double>> results = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                results.add(exec.submit(() -> shop.purchaseItems(purchaseBatch)));
            }

            exec.shutdown();
            exec.awaitTermination(1, TimeUnit.MINUTES);

            int successCount = 0;
            int failureCount = 0;
            double totalRevenue = 0;

            for (Future<Double> f : results) {
                try {
                    totalRevenue += f.get();
                    successCount++;
                } catch (ExecutionException ee) {
                    // Expect IllegalArgumentException for insufficient stock
                    assertTrue(ee.getCause() instanceof IllegalArgumentException);
                    assertTrue(ee.getCause().getMessage().contains("Insufficient stock for item 1"));
                    failureCount++;
                }
            }

            // Only 5 threads can succeed (5 * 10 = 50 units)
            assertEquals(5, successCount);
            assertEquals(5, failureCount);

            // Each successful purchase yields 10 * $5 = $50
            assertEquals(50 * successCount, totalRevenue, 0.0001);

            // No stock remains
            assertEquals(0, shop.getItemQuantity(1));
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
                    shop.purchaseItems(Map.of(200, 3));
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
        // Simulates one thread restocking item 600 while another thread purchases it.
        @Test
        public void testConcurrentRestockAndPurchase_Success() throws Exception {
            // prepare item 600 with price 10, initial stock 5
            shop.addItem(600, 5);
            shop.updateItemPrice(600, 10);
            Map<Integer,Integer> purchase = Map.of(600, 5);

            ExecutorService exec = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            // purchaser thread will attempt to buy 5 units
            Future<Exception> purchaser = exec.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    shop.purchaseItems(purchase);
                    return null;
                } catch (Exception e) {
                    return e;
                }
            });

            // restocker thread will add 5 more units concurrently
            Future<?> restocker = exec.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException ignore) {
                    Thread.currentThread().interrupt();
                }
                shop.addItem(600, 5);
            });

            // wait for both threads to be ready, then start
            ready.await();
            start.countDown();

            // collect results
            Exception purchaseEx = purchaser.get();
            restocker.get();
            exec.shutdown();

            // purchase must succeed without exception
            assertNull(purchaseEx, "Purchase should succeed with sufficient stock");
            // after purchase and restock, stock = initial+restock-purchased = 5+5-5 = 5
            assertEquals(5, shop.getItemQuantity(600),
                "Final stock should be 5 after concurrent restock and purchase");
        }


        // UC2 – Concurrent price update & purchase (success)
        // Tests that a purchase may see either the old or new price when price is updated concurrently.
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
                return shop.purchaseItems(purchase);
            });
            Future<?> priceUpdater = exec.submit(() -> {
                ready.countDown();
                try { start.await(); } catch (InterruptedException ignore) {}
                shop.updateItemPrice(700, 30);
            });

            ready.await();
            start.countDown();

            double total = purchaser.get();
            priceUpdater.get();
            exec.shutdown();

            assertTrue(total == 20.0 || total == 30.0,
                "Total should be either old or new price");
        }

        // UC3 – Bundle & multiple discounts interaction (success)
        // Verifies that the best discount among item‐specific, global, and bundle is applied.
        @Test
        public void testBundleAndMultipleDiscounts_Success() {
            shop.addItem(800, 2); shop.updateItemPrice(800, 100);
            shop.addItem(801, 1); shop.updateItemPrice(801, 200);

            shop.setDiscountForItem(800, 20); // 20% off item 800
            shop.setGlobalDiscount(10);       // 10% off everything
            Map<Integer,Integer> bundle = Map.of(800,2, 801,1);
            shop.addBundleDiscount(bundle, 30); // 30% off bundle

            double total = shop.purchaseItems(Map.of(800,2, 801,1));
            // 400 total → 30% off = 280
            assertEquals(280.0, total);
        }

        // UC4 – Rollback on discount failure (success)
        // Ensures stock is restored if a discount throws during purchase.
        @Test
        public void testRollbackOnDiscountFailure_Success() throws Exception {
            shop.addItem(900, 5);
            shop.updateItemPrice(900, 50);

            // reflectively grab the private CopyOnWriteArrayList<Discount> discounts
            java.lang.reflect.Field discountsField = Shop.class.getDeclaredField("discounts");
            discountsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            CopyOnWriteArrayList<Discount> discounts =
                (CopyOnWriteArrayList<Discount>) discountsField.get(shop);

            // inject a “faulty” discount that always throws in applyDiscounts
            discounts.add(new Discount() {
                @Override
                public Map<Integer, Integer> applyDiscounts(
                        Map<Integer, Integer> items,
                        Map<Integer, AtomicInteger> prices,
                        Map<Integer, Integer> current) {
                    throw new RuntimeException("Bad discount");
                }
                @Override public Integer getItemId()   { return null; }
                @Override public void setPercentage(int p) { /* no-op */ }
                @Override public int getPercentage()   { return 0; }
            });

            // attempt a purchase of 2 units → discount throws → should roll back
            assertThrows(RuntimeException.class,
                () -> shop.purchaseItems(Map.of(900,2)));

            // after failure, original stock (5) must be untouched
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
            ShopService ss = new ShopService(repo);
            ss.setServices(ats, is, us);

            when(ats.ValidateToken("bad")).thenThrow(new RuntimeException("Invalid token"));
            assertThrows(RuntimeException.class,
                () -> ss.createShop("X", mock(PurchasePolicy.class), mock(ShippingMethod.class), "bad"));

            when(ats.ValidateToken("tok")).thenReturn(1);
            when(us.hasPermission(1, PermissionsEnum.setPolicy, 2)).thenReturn(false);
            assertThrows(RuntimeException.class,
                () -> ss.setGlobalDiscount(2, 10, "tok"));
        }

        // UC8 – Bulk purchase of multiple items (success)
        // Purchases more than one SKU in one call and checks stock adjustment.
        @Test
        public void testBulkPurchaseOfMultipleItems_Success() {
            shop.addItem(1000, 3); shop.updateItemPrice(1000, 10);
            shop.addItem(1001, 2); shop.updateItemPrice(1001, 20);

            double total = shop.purchaseItems(Map.of(1000,2, 1001,1));
            assertEquals(2*10 + 1*20, total);
            assertEquals(1, shop.getItemQuantity(1000));
            assertEquals(1, shop.getItemQuantity(1001));
        }

}
    
