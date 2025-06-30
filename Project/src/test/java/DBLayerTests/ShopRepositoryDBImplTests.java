package DBLayerTests;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DBLayer.Shop.ShopRepositoryDBImpl;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Discount.Discount;
import com.example.app.DomainLayer.Shop.Discount.Policy;
import com.example.app.DomainLayer.Shop.Discount.PolicyLeaf;
import com.example.app.DomainLayer.Shop.Shop;
import com.example.app.InfrastructureLayer.WSEPShipping;
import com.example.app.SimpleHttpServerApplication;

import jakarta.transaction.Transactional;

@SpringBootTest(classes = SimpleHttpServerApplication.class)
@ActiveProfiles({ "db-test" })
@Transactional
@SuppressWarnings("unused")
public class ShopRepositoryDBImplTests {

    @Autowired
    private ShopRepositoryDBImpl repo;

    private Shop shop;

    @BeforeEach
    public void setup() {
        shop = repo.createShop("Test Shop", null, new WSEPShipping());
    }

    @Test
    void testCreateAndGetShop_Success() {
        Shop newShop = repo.createShop("Test Shop2", null, new WSEPShipping());
        Shop retrievedShop = repo.getShop(newShop.getId());
        assertTrue(retrievedShop != null);
        assertEquals("Test Shop2", retrievedShop.getName());
        assertTrue(retrievedShop.getShippingMethod() instanceof WSEPShipping);
    }

    @Test
    void testGetShop_notExists_Failure() {
        OurRuntime exception = assertThrows(OurRuntime.class, () -> repo.getShop(999));
        assertTrue(exception.getMessage().contains("Shop not found") || exception.getMessage().contains("999"));
    }

    @Test
    void testGetAllShops_Success() {
        Shop shop1 = repo.createShop("Shop 1", null, new WSEPShipping());
        Shop shop2 = repo.createShop("Shop 2", null, new WSEPShipping());
        List<Shop> allShops = repo.getAllShops();
        assertEquals(3, allShops.size());
        assertTrue(allShops.contains(shop1));
        assertTrue(allShops.contains(shop2));
    }

    @Test
    void testSetGlobalDiscount_Success() {
        repo.setGlobalDiscount(shop.getId(), 10, false);
        assertEquals(1, shop.getDiscounts().size());
        assertFalse(shop.getDiscounts().get(0).isDouble());
    }

    @Test
    void testSetGlobalDiscount_InvalidShop_Failure() {
        OurRuntime ex1 = assertThrows(OurRuntime.class, () -> repo.setGlobalDiscount(999, 10, false));
    }

    @Test
    void testRemoveGlobalDiscount_Success() {
        repo.setGlobalDiscount(shop.getId(), 10, false);
        repo.removeGlobalDiscount(shop.getId());
        assertTrue(shop.getDiscounts().isEmpty());
    }

    @Test
    void testRemoveGlobalDiscount_InvalidShop_Failure() {
        OurRuntime ex2 = assertThrows(OurRuntime.class, () -> repo.removeGlobalDiscount(999));
    }

    @Test
    void testSetDiscountForItem_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.setDiscountForItem(shop.getId(), itemId, 15, false);
        assertEquals(1, shop.getDiscounts().size());
        assertEquals(itemId, shop.getDiscounts().get(0).getItemId());
        assertEquals(15, shop.getDiscounts().get(0).getPercentage());
        assertFalse(shop.getDiscounts().get(0).isDouble());
    }

    @Test
    void testSetDiscountForItem_InvalidShop_Failure() {
        OurRuntime ex3 = assertThrows(OurRuntime.class, () -> repo.setDiscountForItem(999, 1, 15, false));
    }

    @Test
    void testSetCategoryDiscount_Success() {
        repo.setCategoryDiscount(shop.getId(), ItemCategory.ELECTRONICS, 20, false);
        assertEquals(1, shop.getDiscounts().size());
        assertEquals(ItemCategory.ELECTRONICS, shop.getDiscounts().get(0).getItemCategory());
        assertEquals(20, shop.getDiscounts().get(0).getPercentage());
        assertFalse(shop.getDiscounts().get(0).isDouble());
    }

    @Test
    void testSetCategoryDiscount_InvalidShop_Failure() {
        OurRuntime ex4 = assertThrows(OurRuntime.class, () -> repo.setCategoryDiscount(999, ItemCategory.ELECTRONICS, 20, false));
    }

    @Test
    void testRemoveCategoryDiscount_Success() {
        repo.setCategoryDiscount(shop.getId(), ItemCategory.ELECTRONICS, 20, false);
        repo.removeCategoryDiscount(shop.getId(), ItemCategory.ELECTRONICS);
        assertTrue(shop.getDiscounts().isEmpty());
    }

    @Test
    void testRemoveCategoryDiscount_InvalidShop_Failure() {
        OurRuntime ex5 = assertThrows(OurRuntime.class, () -> repo.removeCategoryDiscount(999, ItemCategory.ELECTRONICS));
    }

    @Test
    void testRemoveItemDiscount_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.setDiscountForItem(shop.getId(), itemId, 15, false);
        repo.removeDiscountForItem(shop.getId(), itemId);
        assertTrue(shop.getDiscounts().isEmpty());
    }

    @Test
    void testRemoveItemDiscount_InvalidShop_Failure() {
        OurRuntime ex6 = assertThrows(OurRuntime.class, () -> repo.removeDiscountForItem(999, 1));
    }

    @Test
    void testAddReviewsToShop_Success() {
        repo.addReviewToShop(shop.getId(), 5, 5, "Great service!");
        repo.addReviewToShop(shop.getId(), 4, 4, "Good quality products.");
        assertEquals(2, shop.getReviews().size());
        assertEquals(5, shop.getReviews().get(0).getRating());
        assertEquals(4, shop.getReviews().get(1).getRating());
    }

    @Test
    void testAddReviewsToShop_InvalidShop_Failure() {
        OurRuntime ex7 = assertThrows(OurRuntime.class, () -> repo.addReviewToShop(999, 5, 5, "Great service!"));
    }

    @Test
    void testGetShopAverageRating_Success() {
        repo.addReviewToShop(shop.getId(), 5, 5, "Great service!");
        repo.addReviewToShop(shop.getId(), 4, 4, "Good quality products.");
        double averageRating = repo.getShopAverageRating(shop.getId());
        assertEquals(4.5, averageRating, 0.001);
    }

    @Test
    void testGetShopAverageRating_InvalidShop_Failure() {
        OurRuntime ex8 = assertThrows(OurRuntime.class, () -> repo.getShopAverageRating(999));
    }

    @Test
    void testAddItemToShop_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 7);
        assertEquals(1, shop.getItems().size());
        assertEquals(itemId, (int) shop.getItems().get(0));
        assertEquals(7.0, shop.getItemPrices().get(itemId), 0.001);
        assertEquals(5, (int) shop.getItemQuantities().get(itemId));
    }

    @Test
    void testAddItemToShop_InvalidShop_Failure() {
        OurRuntime ex9 = assertThrows(OurRuntime.class, () -> repo.addItemToShop(999, 1, 5, 7));
    }

    @Test
    void testAddSupplyToItem_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        repo.addSupplyToItem(shop.getId(), itemId, 5);
        assertEquals(10, (int) shop.getItemQuantities().get(itemId));
        assertEquals(10.0, shop.getItemPrices().get(itemId), 0.001);
    }

    @Test
    void testAddSupplyToItem_InvalidShop_Failure() {
        OurRuntime ex10 = assertThrows(OurRuntime.class, () -> repo.addSupplyToItem(999, 1, 5));
    }

    @Test
    void testUpdateItemPriceInShop_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        repo.updateItemPriceInShop(shop.getId(), itemId, 15);
        assertEquals(15.0, shop.getItemPrices().get(itemId), 0.001);
    }

    @Test
    void testUpdateItemPriceInShop_InvalidShop_Failure() {
        OurRuntime ex11 = assertThrows(OurRuntime.class, () -> repo.updateItemPriceInShop(999, 1, 15));
    }

    @Test
    void testRemoveItemFromShop_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        repo.removeItemFromShop(shop.getId(), itemId);
        assertFalse(shop.getItems().contains(itemId));
        assertFalse(shop.getItemPrices().containsKey(itemId));
        assertFalse(shop.getItemQuantities().containsKey(itemId));
    }

    @Test
    void testRemoveItemFromShop_InvalidShop_Failure() {
        OurRuntime ex12 = assertThrows(OurRuntime.class, () -> repo.removeItemFromShop(999, 1));
    }

    @Test
    void testGetItemQuantityFromShop_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        int quantity = repo.getItemQuantityFromShop(shop.getId(), itemId);
        assertEquals(5, quantity);
    }

    @Test
    void testGetItemQuantityFromShop_InvalidShop_Failure() {
        OurRuntime ex13 = assertThrows(OurRuntime.class, () -> repo.getItemQuantityFromShop(999, 1));
    }

    @Test
    void testCloseShop_Success() {
        repo.closeShop(shop.getId());
        assertTrue(shop.isClosed());
    }

    @Test
    void testCloseShop_InvalidShop_Failure() {
        OurRuntime ex14 = assertThrows(OurRuntime.class, () -> repo.closeShop(999));
    }

    @Test
    void checkSupplyAvailability_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        boolean isAvailable = repo.checkSupplyAvailability(shop.getId(), itemId);
        assertTrue(isAvailable);
    }

    @Test
    void checkSupplyAvailability_InvalidShop_Failure() {
        OurRuntime ex15 = assertThrows(OurRuntime.class, () -> repo.checkSupplyAvailability(999, 1));
    }

    @Test
    void checkSupplyAvailabilityAndAcquire_returnTrue_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        boolean isAvailable = repo.checkSupplyAvailabilityAndAqcuire(shop.getId(), itemId, 1);
        assertTrue(isAvailable);
    }

    @Test
    void checkSupplyAvailabilityAndAcquire_returnFalse_Failure() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        boolean isAvailable = repo.checkSupplyAvailabilityAndAqcuire(shop.getId(), itemId, 6);
        assertFalse(isAvailable);
    }

    @Test
    void checkSupplyAvailabilityAndAcquire_InvalidShop_Failure() {
        OurRuntime ex16 = assertThrows(OurRuntime.class, () -> repo.checkSupplyAvailabilityAndAqcuire(999, 1, 1));
    }

    @Test
    void removeSupply_triedToRemoveMoreThanAvailable_Failure() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        OurRuntime ex17 = assertThrows(OurRuntime.class, () -> repo.removeSupply(shop.getId(), itemId, 6));
    }

    @Test
    void removeSupply_InvalidShop_Failure() {
        OurRuntime ex18 = assertThrows(OurRuntime.class, () -> repo.removeSupply(999, 1, 2));
    }

    @Test
    void testGetItemsByShop_Success() {
        int itemId1 = 1; // Assuming item with ID 1 exists
        int itemId2 = 2; // Assuming item with ID 2 exists
        repo.addItemToShop(shop.getId(), itemId1, 5, 10); // Add first item
        repo.addItemToShop(shop.getId(), itemId2, 3, 15); // Add second item
        List<Integer> items = repo.getItemsByShop(shop.getId());
        assertEquals(2, items.size());
        assertTrue(items.contains(itemId1));
        assertTrue(items.contains(itemId2));
    }

    @Test
    void testGetItemsByShop_InvalidShop_Failure() {
        OurRuntime ex19 = assertThrows(OurRuntime.class, () -> repo.getItemsByShop(999));
    }

    @Test
    void testGetItems_Success() {
        int itemId1 = 1;
        int itemId2 = 2;
        repo.addItemToShop(shop.getId(), itemId1, 5, 10); // Add first item
        repo.addItemToShop(shop.getId(), itemId2, 3, 15); // Add second item
        Shop shop2 = repo.createShop("Another Shop", null, new WSEPShipping());
        int itemId3 = 3;
        repo.addItemToShop(shop2.getId(), itemId3, 4, 20); // Add item to another shop
        List<Integer> items = repo.getItems();
        assertEquals(3, items.size());
        assertTrue(items.contains(itemId1));
        assertTrue(items.contains(itemId2));
        assertTrue(items.contains(itemId3));
    }

    @Test
    void testAddSupply_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        repo.addSupply(shop.getId(), itemId, 5);
        assertEquals(10, (int) shop.getItemQuantities().get(itemId));
    }

    @Test
    void testAddSupply_InvalidShop_Failure() {
        OurRuntime ex20 = assertThrows(OurRuntime.class, () -> repo.addSupply(999, 1, 5));
    }

    @Test
    void testPurchaseItems_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        repo.setGlobalDiscount(shop.getId(), 10, false); // Set a global discount
        double totalCost = repo.purchaseItems(Map.of(itemId, 2), new HashMap<>(), shop.getId());
        assertEquals(18.0, totalCost, 0.001); // 2 items at 10 each with 10% discount
    }

    @Test
    void testPurchaseItems_InvalidShop_Failure() {
        OurRuntime ex21 = assertThrows(OurRuntime.class, () -> repo.purchaseItems(Map.of(1, 2), new HashMap<>(), 999));
    }

    @Test
    void testRollBackPurchase_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        repo.setGlobalDiscount(shop.getId(), 10, false); // Set a global discount
        Map<Integer, Integer> purchaseList = Map.of(itemId, 2);
        repo.purchaseItems(purchaseList, new HashMap<>(), shop.getId());
        repo.rollBackPurchase(purchaseList, shop.getId());
        assertEquals(5, (int) shop.getItemQuantities().get(itemId)); // Should revert to original quantity
    }

    @Test
    void testRollBackPurchase_InvalidShop_Failure() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        Map<Integer, Integer> purchaseList = Map.of(itemId, 2);
        OurRuntime ex22 = assertThrows(OurRuntime.class, () -> repo.rollBackPurchase(purchaseList, 999));
    }

    @Test
    void testGetDiscounts_Success() {
        repo.setGlobalDiscount(shop.getId(), 10, false);
        List<Discount> discounts = repo.getDiscounts(shop.getId());
        assertEquals(1, discounts.size());
        assertEquals(10, discounts.get(0).getPercentage());
        assertFalse(discounts.get(0).isDouble());
    }

    @Test
    void testGetDiscounts_InvalidShop_Failure() {
        OurRuntime ex23 = assertThrows(OurRuntime.class, () -> repo.getDiscounts(999));
    }

    @Test
    void testSetDiscountPolicy_Success() {
        Policy policy = new PolicyLeaf(10, 10, ItemCategory.AUTOMOTIVE, 10.0);
        repo.setDiscountPolicy(shop.getId(), policy);
        assertEquals(shop.getPolicies().size(), 1);
    }

    @Test
    void testSetDiscountPolicy_InvalidShop_Failure() {
        OurRuntime ex24 = assertThrows(OurRuntime.class, () -> repo.setDiscountPolicy(999, new PolicyLeaf()));
    }

    /* ---------------------------------------------------------------------- */
    /*  checkPolicy tests                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    void testCheckPolicy_Success() {
        HashMap<Integer, HashMap<Integer, Integer>> cart = new HashMap<>();
        HashMap<Integer, Integer> shopItems = new HashMap<>();
        shopItems.put(1, 2); // itemId 1, quantity 2
        cart.put(shop.getId(), shopItems);
        
        boolean result = repo.checkPolicy(cart, "test-token");
        
        // Based on the implementation, this should always return true (placeholder)
        assertTrue(result);
    }

    @Test
    void testCheckPolicy_EmptyCart_Success() {
        HashMap<Integer, HashMap<Integer, Integer>> emptyCart = new HashMap<>();
        
        boolean result = repo.checkPolicy(emptyCart, "test-token");
        
        assertTrue(result);
    }

    @Test
    void testCheckPolicy_NullToken_Success() {
        HashMap<Integer, HashMap<Integer, Integer>> cart = new HashMap<>();
        HashMap<Integer, Integer> shopItems = new HashMap<>();
        shopItems.put(1, 1);
        cart.put(shop.getId(), shopItems);
        
        boolean result = repo.checkPolicy(cart, null);
        
        assertTrue(result);
    }

    /* ---------------------------------------------------------------------- */
    /*  checkSupplyAvailability tests                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    void testCheckSupplyAvailability_ItemAvailable_Success() {
        int itemId = 1;
        repo.addItemToShop(shop.getId(), itemId, 5, 10);
        
        boolean isAvailable = repo.checkSupplyAvailability(shop.getId(), itemId);
        
        assertTrue(isAvailable);
    }

    @Test
    void testCheckSupplyAvailability_ItemNotAvailable_ReturnsFalse() {
        int itemId = 1;
        
        boolean isAvailable = repo.checkSupplyAvailability(shop.getId(), itemId);
        
        assertFalse(isAvailable);
    }

    @Test
    void testCheckSupplyAvailability_ItemNotExist_ReturnsFalse() {
        int nonExistentItemId = 999;
        
        boolean isAvailable = repo.checkSupplyAvailability(shop.getId(), nonExistentItemId);
        
        assertFalse(isAvailable);
    }

    /* ---------------------------------------------------------------------- */
    /*  getAllClosedShops tests                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    void testGetAllClosedShops_NoClosedShops_ReturnsEmpty() {
        List<Shop> closedShops = repo.getAllClosedShops();
        
        assertTrue(closedShops.isEmpty());
    }

    @Test
    void testGetAllClosedShops_SomeClosedShops_ReturnsOnlyClosed() {
        Shop shop1 = repo.createShop("Open Shop", null, new WSEPShipping());
        Shop shop2 = repo.createShop("Closed Shop 1", null, new WSEPShipping());
        Shop shop3 = repo.createShop("Closed Shop 2", null, new WSEPShipping());
        
        repo.closeShop(shop2.getId());
        repo.closeShop(shop3.getId());
        
        List<Shop> closedShops = repo.getAllClosedShops();
        
        assertEquals(2, closedShops.size());
        assertTrue(closedShops.stream().allMatch(Shop::isClosed));
        assertTrue(closedShops.contains(shop2));
        assertTrue(closedShops.contains(shop3));
        assertFalse(closedShops.contains(shop1));
    }

    /* ---------------------------------------------------------------------- */
    /*  getAllOpenShops tests                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    void testGetAllOpenShops_AllShopsOpen_ReturnsAll() {
        Shop shop1 = repo.createShop("Open Shop 1", null, new WSEPShipping());
        Shop shop2 = repo.createShop("Open Shop 2", null, new WSEPShipping());
        
        List<Shop> openShops = repo.getAllOpenShops();
        
        assertTrue(openShops.size() >= 3); // At least the setup shop + 2 new ones
        assertTrue(openShops.stream().noneMatch(Shop::isClosed));
        assertTrue(openShops.contains(shop));
        assertTrue(openShops.contains(shop1));
        assertTrue(openShops.contains(shop2));
    }

    @Test
    void testGetAllOpenShops_SomeShopsClosed_ReturnsOnlyOpen() {
        Shop shop1 = repo.createShop("Open Shop", null, new WSEPShipping());
        Shop shop2 = repo.createShop("Closed Shop", null, new WSEPShipping());
        
        repo.closeShop(shop2.getId());
        
        List<Shop> openShops = repo.getAllOpenShops();
        
        assertTrue(openShops.stream().noneMatch(Shop::isClosed));
        assertTrue(openShops.contains(shop));
        assertTrue(openShops.contains(shop1));
        assertFalse(openShops.contains(shop2));
    }

    /* ---------------------------------------------------------------------- */
    /*  getClosedShops tests                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    void testGetClosedShops_NoClosedShops_ReturnsEmptyList() {
        List<Integer> closedShopIds = repo.getClosedShops();
        
        assertTrue(closedShopIds.isEmpty());
    }

    @Test
    void testGetClosedShops_SomeClosedShops_ReturnsClosedIds() {
        Shop shop1 = repo.createShop("Open Shop", null, new WSEPShipping());
        Shop shop2 = repo.createShop("Closed Shop 1", null, new WSEPShipping());
        Shop shop3 = repo.createShop("Closed Shop 2", null, new WSEPShipping());
        
        repo.closeShop(shop2.getId());
        repo.closeShop(shop3.getId());
        
        List<Integer> closedShopIds = repo.getClosedShops();
        
        assertEquals(2, closedShopIds.size());
        assertTrue(closedShopIds.contains(shop2.getId()));
        assertTrue(closedShopIds.contains(shop3.getId()));
        assertFalse(closedShopIds.contains(shop1.getId()));
    }

    @Test
    void testGetClosedShops_ReturnsImmutableList() {
        Shop shop1 = repo.createShop("Closed Shop", null, new WSEPShipping());
        repo.closeShop(shop1.getId());
        
        List<Integer> closedShopIds = repo.getClosedShops();
        
        // Should throw UnsupportedOperationException since it returns unmodifiable list
        UnsupportedOperationException ex25 = assertThrows(UnsupportedOperationException.class, () -> closedShopIds.add(999));
    }

    /* ---------------------------------------------------------------------- */
    /*  getItems tests (already partially covered)                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void testGetItems_MultipleShopsWithDuplicateItems_ReturnsAllItems() {
        repo.addItemToShop(shop.getId(), 1, 5, 10);
        repo.addItemToShop(shop.getId(), 2, 3, 15);
        
        Shop shop2 = repo.createShop("Another Shop", null, new WSEPShipping());
        repo.addItemToShop(shop2.getId(), 1, 2, 12); // Same item ID as in first shop
        repo.addItemToShop(shop2.getId(), 3, 4, 20);
        
        List<Integer> items = repo.getItems();
        
        assertEquals(4, items.size()); // Should include duplicates
        assertEquals(2, Collections.frequency(items, 1)); // Item 1 appears twice
    }

    /* ---------------------------------------------------------------------- */
    /*  getPolicies tests                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    void testGetPolicies_NoPolicies_ReturnsEmptyList() {
        List<Policy> policies = repo.getPolicies(shop.getId());
        
        assertTrue(policies.isEmpty());
    }

    @Test
    void testGetPolicies_InvalidShop_Failure() {
        OurRuntime ex26 = assertThrows(OurRuntime.class, () -> repo.getPolicies(999));
    }

    /* ---------------------------------------------------------------------- */
    /*  removeSupply tests (additional scenarios)                             */
    /* ---------------------------------------------------------------------- */

    @Test
    void testRemoveSupply_ExactQuantity_Success() {
        int itemId = 1;
        repo.addItemToShop(shop.getId(), itemId, 5, 10);
        
        repo.removeSupply(shop.getId(), itemId, 5);
        
        int remainingQuantity = repo.getItemQuantityFromShop(shop.getId(), itemId);
        assertEquals(0, remainingQuantity);
    }

    @Test
    void testRemoveSupply_ItemNotExists_Failure() {
        int nonExistentItemId = 999;
        
        OurRuntime ex27 = assertThrows(OurRuntime.class, () -> repo.removeSupply(shop.getId(), nonExistentItemId, 1));
    }

    /* ---------------------------------------------------------------------- */
    /*  shipPurchase tests                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    void testShipPurchase_ValidShippingDetails_Success() {
        boolean result = repo.shipPurchase(
            "John Doe", 
            shop.getId(), 
            "USA", 
            "New York", 
            "5th Avenue", 
            "10001"
        );
        
        assertTrue(result); // Assuming WSEPShipping returns successful processing
    }

    @Test
    void testShipPurchase_InvalidShop_Failure() {
        OurRuntime ex28 = assertThrows(OurRuntime.class, () -> repo.shipPurchase(
            "John Doe", 
            999, // Invalid shop ID
            "USA", 
            "New York", 
            "5th Avenue", 
            "10001"
        ));
    }



    @Test
    void testShipPurchase_EmptyAddressFields_Success() {
        boolean result = repo.shipPurchase(
            "Jane Doe", 
            shop.getId(), 
            "", // Empty country
            "", // Empty city
            "", // Empty street
            ""  // Empty postal code
        );
        
        // Should handle empty fields (depends on WSEPShipping implementation)
        assert result;
    }

    /* ---------------------------------------------------------------------- */
    /*  updatePurchasePolicy tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    void testUpdatePurchasePolicy_Success() {
        // Note: Based on implementation, this method is just a placeholder
        repo.updatePurchasePolicy(shop.getId(), null);
        
        // Since the method doesn't actually do anything, we just verify it doesn't throw
        assert true;
    }

    @Test
    void testUpdatePurchasePolicy_InvalidShop_NoException() {
        // Since the method doesn't check for valid shop ID, it shouldn't throw
        repo.updatePurchasePolicy(999, null);
        
        assert true;
    }

    /* ---------------------------------------------------------------------- */
    /*  updateShop tests                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    void testUpdateShop_ValidShop_Success() {
        // The updateShop method is private, but we can test it indirectly through other methods
        repo.addItemToShop(shop.getId(), 1, 5, 10); // This calls updateShop internally
        
        Shop retrievedShop = repo.getShop(shop.getId());
        assert retrievedShop.getItems().contains(1);
    }

    @Test
    void testUpdateShop_ShopStateChanges_PersistsCorrectly() {
        // Test that shop state changes are properly persisted
        int itemId = 1;
        repo.addItemToShop(shop.getId(), itemId, 5, 10);
        repo.setGlobalDiscount(shop.getId(), 15, false);
        
        Shop retrievedShop = repo.getShop(shop.getId());
        assert retrievedShop.getItems().contains(itemId);
        assert retrievedShop.getItemQuantities().get(itemId) == 5;
        assert retrievedShop.getItemPrices().get(itemId) == 10;
        assert retrievedShop.getDiscounts().size() == 1;
        assert retrievedShop.getDiscounts().get(0).getPercentage() == 15;
    }

}
