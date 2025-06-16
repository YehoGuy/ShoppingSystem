package DBLayerTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.app.SimpleHttpServerApplication;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DBLayer.Shop.ShopRepositoryDBImpl;
import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Shop;
import com.example.app.DomainLayer.Shop.Discount.Discount;
import com.example.app.DomainLayer.Shop.Discount.Policy;
import com.example.app.DomainLayer.Shop.Discount.PolicyLeaf;
import com.example.app.InfrastructureLayer.WSEPShipping;

import jakarta.transaction.Transactional;

@SpringBootTest(classes = SimpleHttpServerApplication.class)
@ActiveProfiles({ "test" })
@Transactional
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
        Shop shop = repo.createShop("Test Shop2", null, new WSEPShipping());
        Shop retrievedShop = repo.getShop(shop.getId());
        assert retrievedShop != null;
        assert retrievedShop.getName().equals("Test Shop2");
        assert retrievedShop.getShippingMethod() instanceof WSEPShipping;
    }

    @Test
    void testGetShop_notExists_Failure() {
        assertThrows(OurRuntime.class, () -> repo.getShop(999));
    }

    @Test
    void testGetAllShops_Success() {
        Shop shop1 = repo.createShop("Shop 1", null, new WSEPShipping());
        Shop shop2 = repo.createShop("Shop 2", null, new WSEPShipping());
        List<Shop> allShops = repo.getAllShops();
        assert allShops.size() == 3;
        assert allShops.contains(shop1);
        assert allShops.contains(shop2);
    }

    @Test
    void testSetGlobalDiscount_Success() {
        repo.setGlobalDiscount(shop.getId(), 10, false);
        assert shop.getDiscounts().size() == 1;
        assert !shop.getDiscounts().get(0).isDouble();
    }

    @Test
    void testSetGlobalDiscount_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.setGlobalDiscount(999, 10, false));
    }

    @Test
    void testRemoveGlobalDiscount_Success() {
        repo.setGlobalDiscount(shop.getId(), 10, false);
        repo.removeGlobalDiscount(shop.getId());
        assert shop.getDiscounts().isEmpty();
    }

    @Test
    void testRemoveGlobalDiscount_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.removeGlobalDiscount(999));
    }

    @Test
    void testSetDiscountForItem_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.setDiscountForItem(shop.getId(), itemId, 15, false);
        assert shop.getDiscounts().size() == 1;
        assert shop.getDiscounts().get(0).getItemId() == itemId;
        assert shop.getDiscounts().get(0).getPercentage() == 15;
        assert !shop.getDiscounts().get(0).isDouble();
    }

    @Test
    void testSetDiscountForItem_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.setDiscountForItem(999, 1, 15, false));
    }

    @Test
    void testSetCategoryDiscount_Success() {
        repo.setCategoryDiscount(shop.getId(), ItemCategory.ELECTRONICS, 20, false);
        assert shop.getDiscounts().size() == 1;
        assert shop.getDiscounts().get(0).getItemCategory() == ItemCategory.ELECTRONICS;
        assert shop.getDiscounts().get(0).getPercentage() == 20;
        assert !shop.getDiscounts().get(0).isDouble();
    }

    @Test
    void testSetCategoryDiscount_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.setCategoryDiscount(999, ItemCategory.ELECTRONICS, 20, false));
    }

    @Test
    void testRemoveCategoryDiscount_Success() {
        repo.setCategoryDiscount(shop.getId(), ItemCategory.ELECTRONICS, 20, false);
        repo.removeCategoryDiscount(shop.getId(), ItemCategory.ELECTRONICS);
        assert shop.getDiscounts().isEmpty();
    }

    @Test
    void testRemoveCategoryDiscount_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.removeCategoryDiscount(999, ItemCategory.ELECTRONICS));
    }

    @Test
    void testRemoveItemDiscount_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.setDiscountForItem(shop.getId(), itemId, 15, false);
        repo.removeDiscountForItem(shop.getId(), itemId);
        assert shop.getDiscounts().isEmpty();
    }

    @Test
    void testRemoveItemDiscount_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.removeDiscountForItem(999, 1));
    }

    @Test
    void testAddReviewsToShop_Success() {
        repo.addReviewToShop(shop.getId(), 5, 5, "Great service!");
        repo.addReviewToShop(shop.getId(), 4, 4, "Good quality products.");
        assert shop.getReviews().size() == 2;
        assert shop.getReviews().get(0).getRating() == 5;
        assert shop.getReviews().get(1).getRating() == 4;
    }

    @Test
    void testAddReviewsToShop_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.addReviewToShop(999, 5, 5, "Great service!"));
    }

    @Test
    void testGetShopAverageRating_Success() {
        repo.addReviewToShop(shop.getId(), 5, 5, "Great service!");
        repo.addReviewToShop(shop.getId(), 4, 4, "Good quality products.");
        double averageRating = repo.getShopAverageRating(shop.getId());
        assert averageRating == 4.5;
    }

    @Test
    void testGetShopAverageRating_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.getShopAverageRating(999));
    }

    @Test
    void testAddItemToShop_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 7);
        assert shop.getItems().size() == 1;
        assert shop.getItems().get(0) == itemId;
        assert shop.getItemPrices().get(itemId) == 7;
        assert shop.getItemQuantities().get(itemId) == 5;
    }

    @Test
    void testAddItemToShop_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.addItemToShop(999, 1, 5, 7));
    }

    @Test
    void testAddSupplyToItem_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        repo.addSupplyToItem(shop.getId(), itemId, 5);
        assert shop.getItemQuantities().get(itemId) == 10;
        assert shop.getItemPrices().get(itemId) == 10;
    }

    @Test
    void testAddSupplyToItem_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.addSupplyToItem(999, 1, 5));
    }

    @Test
    void testUpdateItemPriceInShop_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        repo.updateItemPriceInShop(shop.getId(), itemId, 15);
        assert shop.getItemPrices().get(itemId) == 15;
    }

    @Test
    void testUpdateItemPriceInShop_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.updateItemPriceInShop(999, 1, 15));
    }

    @Test
    void testRemoveItemFromShop_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        repo.removeItemFromShop(shop.getId(), itemId);
        assert !shop.getItems().contains(itemId);
        assert !shop.getItemPrices().containsKey(itemId);
        assert !shop.getItemQuantities().containsKey(itemId);
    }

    @Test
    void testRemoveItemFromShop_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.removeItemFromShop(999, 1));
    }

    @Test
    void testGetItemQuantityFromShop_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        int quantity = repo.getItemQuantityFromShop(shop.getId(), itemId);
        assert quantity == 5;
    }

    @Test
    void testGetItemQuantityFromShop_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.getItemQuantityFromShop(999, 1));
    }

    @Test
    void testCloseShop_Success() {
        repo.closeShop(shop.getId());
        assert shop.isClosed();
    }

    @Test
    void testCloseShop_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.closeShop(999));
    }

    @Test
    void checkSupplyAvailability_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        boolean isAvailable = repo.checkSupplyAvailability(shop.getId(), itemId);
        assert isAvailable;
    }

    @Test
    void checkSupplyAvailability_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.checkSupplyAvailability(999, 1));
    }

    @Test
    void checkSupplyAvailabilityAndAcquire_returnTrue_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        boolean isAvailable = repo.checkSupplyAvailabilityAndAqcuire(shop.getId(), itemId, 1);
        assert isAvailable;
    }

    @Test
    void checkSupplyAvailabilityAndAcquire_returnFalse_Failure() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        boolean isAvailable = repo.checkSupplyAvailabilityAndAqcuire(shop.getId(), itemId, 6);
        assert !isAvailable;
    }

    @Test
    void checkSupplyAvailabilityAndAcquire_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.checkSupplyAvailabilityAndAqcuire(999, 1, 1));
    }

    @Test
    void removeSupply_triedToRemoveMoreThanAvailable_Failure() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        assertThrows(OurRuntime.class, () -> repo.removeSupply(shop.getId(), itemId, 6));
    }

    @Test
    void removeSupply_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.removeSupply(999, 1, 2));
    }

    @Test
    void testGetItemsByShop_Success() {
        int itemId1 = 1; // Assuming item with ID 1 exists
        int itemId2 = 2; // Assuming item with ID 2 exists
        repo.addItemToShop(shop.getId(), itemId1, 5, 10); // Add first item
        repo.addItemToShop(shop.getId(), itemId2, 3, 15); // Add second item
        List<Integer> items = repo.getItemsByShop(shop.getId());
        assert items.size() == 2;
        assert items.contains(itemId1);
        assert items.contains(itemId2);
    }

    @Test
    void testGetItemsByShop_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.getItemsByShop(999));
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
        assert items.size() == 3;
        assert items.contains(itemId1);
        assert items.contains(itemId2);
        assert items.contains(itemId3);
    }

    @Test
    void testAddSupply_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        repo.addSupply(shop.getId(), itemId, 5);
        assert shop.getItemQuantities().get(itemId) == 10;
    }

    @Test
    void testAddSupply_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.addSupply(999, 1, 5));
    }

    @Test
    void testPurchaseItems_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        repo.setGlobalDiscount(shop.getId(), 10, false); // Set a global discount
        double totalCost = repo.purchaseItems(Map.of(itemId, 2), new HashMap<>(), shop.getId());
        assert totalCost == 18; // 2 items at 10 each with 10% discount
    }

    @Test
    void testPurchaseItems_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.purchaseItems(Map.of(1, 2), new HashMap<>(), 999));
    }

    @Test
    void testRollBackPurchase_Success() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        repo.setGlobalDiscount(shop.getId(), 10, false); // Set a global discount
        Map<Integer, Integer> purchaseList = Map.of(itemId, 2);
        repo.purchaseItems(purchaseList, new HashMap<>(), shop.getId());
        repo.rollBackPurchase(purchaseList, shop.getId());
        assert shop.getItemQuantities().get(itemId) == 5; // Should revert to original quantity
    }

    @Test
    void testRollBackPurchase_InvalidShop_Failure() {
        int itemId = 1; // Assuming item with ID 1 exists
        repo.addItemToShop(shop.getId(), itemId, 5, 10); // Add item first
        Map<Integer, Integer> purchaseList = Map.of(itemId, 2);
        assertThrows(OurRuntime.class, () -> repo.rollBackPurchase(purchaseList, 999));
    }

    @Test
    void testGetDiscounts_Success() {
        repo.setGlobalDiscount(shop.getId(), 10, false);
        List<Discount> discounts = repo.getDiscounts(shop.getId());
        assert discounts.size() == 1;
        assert discounts.get(0).getPercentage() == 10;
        assert !discounts.get(0).isDouble();
    }

    @Test
    void testGetDiscounts_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.getDiscounts(999));
    }

    @Test
    void testSetDiscountPolicy_Success() {
        Policy policy = new PolicyLeaf(10, 10, ItemCategory.AUTOMOTIVE, 10.0);
        repo.setDiscountPolicy(shop.getId(), policy);
        assertEquals(shop.getPolicies().get(0), policy);
    }

    @Test
    void testSetDiscountPolicy_InvalidShop_Failure() {
        assertThrows(OurRuntime.class, () -> repo.setDiscountPolicy(999, new PolicyLeaf()));
    }

}
