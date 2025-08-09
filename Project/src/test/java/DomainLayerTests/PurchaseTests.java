package DomainLayerTests;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.Purchase;
import com.example.app.DomainLayer.Purchase.Reciept;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link Purchase}.
 *
 * Dependencies (pom.xml):
 *   <dependency>
 *       <groupId>org.junit.jupiter</groupId>
 *       <artifactId>junit-jupiter</artifactId>
 *       <version>5.10.2</version>
 *       <scope>test</scope>
 *   </dependency>
 *   <dependency>
 *       <groupId>org.mockito</groupId>
 *       <artifactId>mockito-core</artifactId>
 *       <version>5.11.0</version>
 *       <scope>test</scope>
 *   </dependency>
 */
@DisplayName("Purchase – exhaustive unit tests with descriptive method names")
class PurchaseTests {

    /* ---------------------------------------------------------------------- */
    /*  Helpers                                                               */
    /* ---------------------------------------------------------------------- */
    private Address anyAddress() {
        return new Address()
            .withCountry("IL")
            .withCity("Tel-Aviv")
            .withStreet("Rothschild")
            .withHouseNumber("1")
            .withZipCode("6800000");
    }

    /* ---------------------------------------------------------------------- */
    /*  Constructor scenarios                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName("constructorWithItems_shouldCopySuppliedMapIntoInternalConcurrentHashMap_andLeavePurchaseIncomplete")
    void constructorWithItems_shouldCopySuppliedMapIntoInternalConcurrentHashMap_andLeavePurchaseIncomplete() {

        Map<Integer,Integer> supplied = new HashMap<>();
        supplied.put(7, 3);
        supplied.put(8, 1);

        Purchase p = new Purchase(10, 100, 200, supplied, 42.5, anyAddress());

        /* basic fields */
        assertAll("basic getters reflect constructor arguments",
            () -> assertEquals(10,  p.getPurchaseId()),
            () -> assertEquals(100, p.getUserId()),
            () -> assertEquals(200, p.getStoreId()),
            () -> assertFalse(p.isCompleted(), "should start incomplete"),
            () -> assertEquals(anyAddress(), p.getShippingAddress())
        );

        /* items were copied, not referenced */
        assertEquals(Map.of(7,3, 8,1), p.getItems(), "items copy matches original");

        supplied.put(7, 999);                   // mutate caller’s map
        assertEquals(3, p.getItems().get(7),    "internal map must be unaffected by caller mutation");
    }

    @Test
    @DisplayName("constructorWithoutItems_shouldCreateEmptyItemMap_andSetPriceToMinusOne_andLeavePurchaseIncomplete")
    void constructorWithoutItems_shouldCreateEmptyItemMap_andSetPriceToMinusOne_andLeavePurchaseIncomplete() {
        Purchase p = new Purchase(11, 111, 222, anyAddress());

        assertAll(
            () -> assertEquals(0, p.getItems().size(), "items should be empty"),
            () -> assertFalse(p.isCompleted(),         "purchase starts incomplete")
        );
    }

    /* ---------------------------------------------------------------------- */
    /*  addItem / removeItem behaviour                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName("addItem_whenItemIsNew_shouldInsertWithExactQuantity")
    void addItem_whenItemIsNew_shouldInsertWithExactQuantity() {
        Purchase p = new Purchase(1, 2, 3, anyAddress());
        p.addItem(55, 4);
        assertEquals(4, p.getItems().get(55));
    }

    @Test
    @DisplayName("addItem_whenItemAlreadyExists_shouldAccumulateQuantitiesAtomically")
    void addItem_whenItemAlreadyExists_shouldAccumulateQuantitiesAtomically() {
        Purchase p = new Purchase(1, 2, 3, anyAddress());
        p.addItem(42, 2);
        p.addItem(42, 5);
        assertEquals(7, p.getItems().get(42));
    }

    @Test
    @DisplayName("removeItem_whenRemovingLessThanExistingQuantity_shouldOnlyDecreaseQuantity")
    void removeItem_whenRemovingLessThanExistingQuantity_shouldOnlyDecreaseQuantity() {
        Purchase p = new Purchase(1, 2, 3, anyAddress());
        p.addItem(99, 10);
        p.removeItem(99, 3);
        assertEquals(7, p.getItems().get(99));
    }

    @Test
    @DisplayName("removeItem_whenRemovingEqualOrGreaterThanExistingQuantity_shouldRemoveEntryCompletely")
    void removeItem_whenRemovingEqualOrGreaterThanExistingQuantity_shouldRemoveEntryCompletely() {
        Purchase p = new Purchase(1, 2, 3, anyAddress());
        p.addItem(77, 4);
        p.removeItem(77, 4);                           // equal
        assertFalse(p.getItems().containsKey(77));

        p.addItem(77, 5);
        p.removeItem(77, 99);                          // greater
        assertFalse(p.getItems().containsKey(77));
    }

    /* ---------------------------------------------------------------------- */
    /*  Immutability of getItems                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName("getItems_shouldReturnUnmodifiableCopy_soExternalModificationThrowsUnsupportedOperationException")
    void getItems_shouldReturnUnmodifiableCopy_soExternalModificationThrowsUnsupportedOperationException() {
        Purchase p = new Purchase(1, 2, 3, anyAddress());
        p.addItem(1, 1);

        Map<Integer,Integer> exposed = p.getItems();
        assertThrows(UnsupportedOperationException.class, () -> exposed.put(2, 2));
    }

    /* ---------------------------------------------------------------------- */
    /*  Address mutability                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName("setAddress_shouldImmediatelyChangeShippingAddress_andGetterShouldReflectNewValue")
    void setAddress_shouldImmediatelyChangeShippingAddress_andGetterShouldReflectNewValue() {
        Purchase p = new Purchase(1, 2, 3, anyAddress());
        Address newAddr = new Address().withCountry("USA").withCity("NYC");
        p.setAddress(newAddr);
        assertEquals(newAddr, p.getShippingAddress());
    }

    /* ---------------------------------------------------------------------- */
    /*  completePurchase / cancelPurchase logic                               */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName("completePurchase_shouldMarkAsCompleted_setTimestamp_andReturnReceipt_afterCallingGenerateReceiptExactlyOnce")
    void completePurchase_shouldMarkAsCompleted_setTimestamp_andReturnReceipt_afterCallingGenerateReceiptExactlyOnce() {
        Purchase spy = spy(new Purchase(1, 2, 3, anyAddress()));
        Reciept dummy = mock(Reciept.class);
        doReturn(dummy).when(spy).generateReciept();

        Reciept receipt = spy.completePurchase();

        verify(spy, times(1)).generateReciept();
        assertSame(dummy, receipt);
        assertTrue(spy.isCompleted(), "isCompleted flag should be true");
        assertNotNull(spy.getTimeOfCompletion(), "timestamp should be set");
    }

    @Test
    @DisplayName("cancelPurchase_shouldMarkAsIncomplete_clearTimestamp_andReturnReceipt_afterCallingGenerateReceiptExactlyOnce")
    void cancelPurchase_shouldMarkAsIncomplete_clearTimestamp_andReturnReceipt_afterCallingGenerateReceiptExactlyOnce() {
        Purchase spy = spy(new Purchase(1, 2, 3, anyAddress()));

        // 1️⃣ Complete first (will invoke generateReciept once)
        spy.completePurchase();

        // 2️⃣ Clear invocation history so we only count calls made *after* this point
        clearInvocations(spy);

        // 3️⃣ Stub generateReciept AFTER clearing
        Reciept dummy = mock(Reciept.class);
        doReturn(dummy).when(spy).generateReciept();

        // 4️⃣ Cancel and verify
        Reciept receipt = spy.cancelPurchase();

        verify(spy, times(1)).generateReciept();   // now exactly once
        assertSame(dummy, receipt);
        assertFalse(spy.isCompleted(), "isCompleted flag should be false");
        assertNull(spy.getTimeOfCompletion(), "timestamp should be cleared");
    }


    @Test
    @DisplayName("generateReciept_whenPurchaseIncomplete_shouldReturnReceiptWithNullCompletionTimeField")
    void generateReciept_whenPurchaseIncomplete_shouldReturnReceiptWithNullCompletionTimeField() throws Exception {
        Purchase p = new Purchase(1, 2, 3, anyAddress());
        Reciept r = p.generateReciept();

        /* Reflection avoids depending on Reciept API details */
        var timeField = r.getClass().getDeclaredField("timeOfCompletion");
        timeField.setAccessible(true);
        assertNull(timeField.get(r));
    }

    @Test
    @DisplayName("generateReciept_whenPurchaseCompleted_shouldReturnReceiptWithNonNullCompletionTimeField")
    void generateReciept_whenPurchaseCompleted_shouldReturnReceiptWithNonNullCompletionTimeField() throws Exception {
        Purchase p = new Purchase(1, 2, 3, anyAddress());
        p.completePurchase();
        Reciept r = p.generateReciept();

        var timeField = r.getClass().getDeclaredField("timeOfCompletion");
        timeField.setAccessible(true);
        assertNotNull(timeField.get(r));
    }

    /* ---------------------------------------------------------------------- */
    /*  Concurrency sanity check                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName("addItem_calledConcurrentlyFromMultipleThreads_shouldNotLoseUpdates_andFinalQuantityShouldEqualThreadCount")
    void addItem_calledConcurrentlyFromMultipleThreads_shouldNotLoseUpdates_andFinalQuantityShouldEqualThreadCount() throws Exception {
        int threads = 200;
        Purchase p = new Purchase(1, 2, 3, anyAddress());

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                p.addItem(999, 1);
                latch.countDown();
            });
        }

        latch.await();          // wait for all tasks
        pool.shutdown();

        assertEquals(threads, p.getItems().get(999),
                     "quantity should equal number of successful concurrent adds");
    }

    /* ---------------------------------------------------------------------- */
    /*  Timestamp accuracy (optional, quick sanity)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName("completePurchase_shouldSetTimeOfCompletionToCurrentMomentWithinAcceptableToleranceOf50Milliseconds")
    void completePurchase_shouldSetTimeOfCompletionToCurrentMomentWithinAcceptableToleranceOf50Milliseconds() {
        Purchase p = new Purchase(1, 2, 3, anyAddress());

        LocalDateTime before = LocalDateTime.now();
        p.completePurchase();
        LocalDateTime after  = LocalDateTime.now();

        assertTrue(!p.getTimeOfCompletion().isBefore(before)
                && !p.getTimeOfCompletion().isAfter(after.plusNanos(50_000_000)),
                "timestamp should fall between 'before' and 'after + 50 ms'");
    }

    /* ---------------------------------------------------------------------- */
    /*  Additional concurrency-focused tests                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName(
        "removeItem_calledConcurrentlyByManyThreads_shouldNeverAllowNegativeQuantityAndItemShouldDisappearWhenCountReachesZero"
    )
    void removeItem_calledConcurrentlyByManyThreads_shouldNeverAllowNegativeQuantityAndItemShouldDisappearWhenCountReachesZero()
            throws Exception {

        int initialQty = 500;
        Purchase p = new Purchase(1, 2, 3, anyAddress());
        p.addItem(111, initialQty);                 // preload

        int threads = initialQty;                   // one thread per unit to remove
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                p.removeItem(111, 1);
                latch.countDown();
            });
        }

        latch.await();
        pool.shutdown();

        assertFalse(p.getItems().containsKey(111),
                    "item should be completely removed after all concurrent decrements");
    }

    @Test
    @DisplayName(
        "concurrentAddsAndRemovesOnSameItem_shouldYieldExactlyAddsMinusRemovesNeverBelowZero"
    )
    void concurrentAddsAndRemovesOnSameItem_shouldYieldExactlyAddsMinusRemovesNeverBelowZero()
            throws Exception {

        int adds    = 1_000;
        int removes =   400;
        Purchase p  = new Purchase(1, 2, 3, anyAddress());

        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(adds + removes);

        /* launch adders */
        for (int i = 0; i < adds; i++) {
            pool.submit(() -> {
                p.addItem(222, 1);
                latch.countDown();
            });
        }
        /* launch removers */
        for (int i = 0; i < removes; i++) {
            pool.submit(() -> {
                p.removeItem(222, 1);
                latch.countDown();
            });
        }

        latch.await();
        pool.shutdown();

        int expected = adds - removes;
        assertEquals(expected, p.getItems().get(222),
                    "final quantity must equal total adds minus total removes");
    }

    /* ---------------------------------------------------------------------- */
    /*  getAddress method tests                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName("getAddress_whenSetInConstructor_shouldReturnExactSameAddressObject")
    void getAddress_whenSetInConstructor_shouldReturnExactSameAddressObject() {
        Address originalAddress = anyAddress();
        Purchase p = new Purchase(1, 2, 3, originalAddress);

        Address retrievedAddress = p.getAddress();

        assertSame(originalAddress, retrievedAddress, "getAddress should return the exact same object reference");
    }

    @Test
    @DisplayName("getAddress_whenSetInParameterizedConstructor_shouldReturnCorrectAddress")
    void getAddress_whenSetInParameterizedConstructor_shouldReturnCorrectAddress() {
        Address expectedAddress = new Address()
            .withCountry("USA")
            .withCity("New York")
            .withStreet("5th Avenue")
            .withHouseNumber("123")
            .withZipCode("10001");
        
        Map<Integer, Integer> items = Map.of(1, 2, 3, 4);
        Purchase p = new Purchase(10, 20, 30, items, 99.5, expectedAddress);

        Address retrievedAddress = p.getAddress();

        assertSame(expectedAddress, retrievedAddress);
    }

    @Test
    @DisplayName("getAddress_whenUsingDefaultConstructor_shouldReturnDefaultAddress")
    void getAddress_whenUsingDefaultConstructor_shouldReturnDefaultAddress() {
        Purchase p = new Purchase();

        Address retrievedAddress = p.getAddress();

        assertNotNull(retrievedAddress, "default constructor should initialize address");
        assertAll("default address should have N/A values",
            () -> assertEquals("N/A", retrievedAddress.getCountry()),
            () -> assertEquals("N/A", retrievedAddress.getCity()),
            () -> assertEquals("N/A", retrievedAddress.getStreet()),
            () -> assertEquals("N/A", retrievedAddress.getHouseNumber()),
            () -> assertEquals("N/A", retrievedAddress.getApartmentNumber()),
            () -> assertEquals("N/A", retrievedAddress.getZipCode())
        );
    }

    @Test
    @DisplayName("getAddress_afterSetAddress_shouldReturnNewAddress")
    void getAddress_afterSetAddress_shouldReturnNewAddress() {
        Purchase p = new Purchase(1, 2, 3, anyAddress());
        
        Address newAddress = new Address()
            .withCountry("Canada")
            .withCity("Toronto")
            .withStreet("Queen Street")
            .withHouseNumber("456")
            .withZipCode("M5V 2A8");
        
        p.setAddress(newAddress);

        Address retrievedAddress = p.getAddress();

        assertSame(newAddress, retrievedAddress, "getAddress should return the updated address");
    }

    @Test
    @DisplayName("getAddress_withNullAddress_shouldReturnNull")
    void getAddress_withNullAddress_shouldReturnNull() {
        Purchase p = new Purchase(1, 2, 3, anyAddress());
        
        p.setAddress(null);

        assertNull(p.getAddress(), "getAddress should return null when address is set to null");
    }

    @Test
    @DisplayName("getAddress_shouldReturnMutableAddressReference_allowingDirectModification")
    void getAddress_shouldReturnMutableAddressReference_allowingDirectModification() {
        Address originalAddress = anyAddress();
        Purchase p = new Purchase(1, 2, 3, originalAddress);

        Address retrievedAddress = p.getAddress();
        retrievedAddress.withCountry("Modified Country");

        assertEquals("Modified Country", p.getAddress().getCountry(), 
            "modifications to returned address should affect the purchase's address");
    }

    @Test
    @DisplayName("getAddress_afterMultipleSetAddressCalls_shouldReturnLatestAddress")
    void getAddress_afterMultipleSetAddressCalls_shouldReturnLatestAddress() {
        Purchase p = new Purchase(1, 2, 3, anyAddress());

        Address firstAddress = new Address().withCountry("First").withCity("City1");
        Address secondAddress = new Address().withCountry("Second").withCity("City2");
        Address thirdAddress = new Address().withCountry("Third").withCity("City3");

        p.setAddress(firstAddress);
        p.setAddress(secondAddress);
        p.setAddress(thirdAddress);

        Address retrievedAddress = p.getAddress();

        assertSame(thirdAddress, retrievedAddress, "getAddress should return the most recently set address");
        assertEquals("Third", retrievedAddress.getCountry());
        assertEquals("City3", retrievedAddress.getCity());
    }

    @Test
    @DisplayName("getAddress_shouldBeConsistentWithGetShippingAddress")
    void getAddress_shouldBeConsistentWithGetShippingAddress() {
        Address testAddress = anyAddress();
        Purchase p = new Purchase(1, 2, 3, testAddress);

        Address fromGetAddress = p.getAddress();
        Address fromGetShippingAddress = p.getShippingAddress();

        assertSame(fromGetAddress, fromGetShippingAddress, 
            "getAddress() and getShippingAddress() should return the same object reference");
    }

    @Test
    @DisplayName("getAddress_afterPurchaseCompletion_shouldStillReturnSameAddress")
    void getAddress_afterPurchaseCompletion_shouldStillReturnSameAddress() {
        Address originalAddress = anyAddress();
        Purchase p = new Purchase(1, 2, 3, originalAddress);

        p.completePurchase();

        Address retrievedAddress = p.getAddress();

        assertSame(originalAddress, retrievedAddress, 
            "completing purchase should not affect the address reference");
    }

    @Test
    @DisplayName("getAddress_afterPurchaseCancellation_shouldStillReturnSameAddress")
    void getAddress_afterPurchaseCancellation_shouldStillReturnSameAddress() {
        Address originalAddress = anyAddress();
        Purchase p = new Purchase(1, 2, 3, originalAddress);

        p.completePurchase();
        p.cancelPurchase();

        Address retrievedAddress = p.getAddress();

        assertSame(originalAddress, retrievedAddress, 
            "cancelling purchase should not affect the address reference");
    }

    @Test
    @DisplayName("getAddress_calledMultipleTimes_shouldReturnConsistentReference")
    void getAddress_calledMultipleTimes_shouldReturnConsistentReference() {
        Address originalAddress = anyAddress();
        Purchase p = new Purchase(1, 2, 3, originalAddress);

        Address first = p.getAddress();
        Address second = p.getAddress();
        Address third = p.getAddress();

        assertAll("multiple calls should return same reference",
            () -> assertSame(first, second),
            () -> assertSame(second, third),
            () -> assertSame(first, third),
            () -> assertSame(originalAddress, first)
        );
    }
}