package InfrastructureLayerTests;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import DomainLayer.Purchase.Address;
import DomainLayer.Purchase.Purchase;
import InfrastructureLayer.PurchaseRepository;

class PurchaseRepositoryTests {

    private PurchaseRepository repository;

    @BeforeEach
    void setUp() {
        // Because PurchaseRepository is a singleton, we'd typically reset or mock it.
        // For demonstration, we can forcibly create a new instance via reflection
        // or reinitialize the storage, but here we use the public getInstance() method.
        repository = PurchaseRepository.getInstance();

        // If this test suite is run repeatedly in the same JVM, you might need a
        // reflection hack to reset purchaseIdCounter and clear the map to get consistent results.
        // For demonstration, we can also add logic that forcibly clears the map:
        // This is "hacky," but works in a test environment.
        // e.g. repository.purchaseStorage.clear(); // if it were visible or via reflection
        // We'll proceed under the assumption that each test run is isolated.
    }

    @Test
    void testAddAndGetPurchase() {
        Address address = new Address().withCountry("USA").withCity("Boston");
        int generatedId = repository.addPurchase(1, 10, Map.of(101, 2), address);

        Purchase retrieved = repository.getPurchaseById(generatedId);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.getUserId());
        assertEquals(10, retrieved.getStoreId());
        assertEquals(2, retrieved.getItems().get(101));
        assertEquals("Boston", retrieved.getShippingAddress().getCity());
    }

    @Test
    void testDeletePurchase() {
        int purchaseId = repository.addPurchase(2, 20, Map.of(202, 5), new Address());
        assertNotNull(repository.getPurchaseById(purchaseId));

        repository.deletePurchase(purchaseId);
        assertNull(repository.getPurchaseById(purchaseId), "Purchase should be removed");
    }

    @Test
    void testGetUserPurchases() {
        // Add two purchases by userId=3
        int pId1 = repository.addPurchase(3, 30, Map.of(301, 1), new Address());
        int pId2 = repository.addPurchase(3, 40, Map.of(401, 2), new Address());
        // Add one purchase by userId=4
        repository.addPurchase(4, 50, Map.of(501, 3), new Address());

        ArrayList<Purchase> user3Purchases = repository.getUserPurchases(3);
        assertEquals(2, user3Purchases.size());
        var ids = user3Purchases.stream().map(Purchase::getPurchaseId).toList();
        assertTrue(ids.contains(pId1) && ids.contains(pId2));
    }

    @Test
    void testGetStorePurchases() {
        // Add some purchases with storeId=99
        int pId1 = repository.addPurchase(5, 99, Map.of(101, 2), new Address());
        int pId2 = repository.addPurchase(6, 99, Map.of(202, 3), new Address());

        // Add purchase for storeId=100
        repository.addPurchase(7, 100, Map.of(303, 4), new Address());

        ArrayList<Purchase> store99Purchases = repository.getStorePurchases(99);
        assertEquals(2, store99Purchases.size());
        var ids = store99Purchases.stream().map(Purchase::getPurchaseId).toList();
        assertTrue(ids.contains(pId1) && ids.contains(pId2));
    }
}
