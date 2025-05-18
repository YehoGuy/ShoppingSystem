package DomainLayerTests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Discount.CategoryDiscount;
import com.example.app.DomainLayer.Shop.Discount.GlobalDiscount;
import com.example.app.DomainLayer.Shop.Discount.PolicyComposite;
import com.example.app.DomainLayer.Shop.Discount.PolicyLeaf;
import com.example.app.DomainLayer.Shop.Discount.SingleDiscount;
import com.example.app.DomainLayer.Shop.Discount.TriPredicate;
import com.example.app.DomainLayer.Shop.Operator;

public class DiscountShopTests {

    private Map<Integer,Integer> items;
    private Map<Integer,AtomicInteger> prices;
    private Map<Integer,Double> discounted;
    private Map<Integer,ItemCategory> categories;

    @BeforeEach
    public void setup() {
        items = new HashMap<>();
        prices = new HashMap<>();
        discounted = new HashMap<>();
        categories = new HashMap<>();
        // two items: id=1 qty=2 price=100, id=2 qty=1 price=50
        items.put(1, 2);
        items.put(2, 1);
        prices.put(1, new AtomicInteger(100));
        prices.put(2, new AtomicInteger(50));
        discounted.put(1, 100.0);
        discounted.put(2, 50.0);
        categories.put(1, ItemCategory.ELECTRONICS);
        categories.put(2, ItemCategory.BOOKS);
    }

    // ----- SingleDiscount -----

    @Test
    public void testSingleDiscount_NoItemInCart() {
        // itemId=3 not in cart → no change
        var sd = new SingleDiscount(3, 30, null, false);
        var result = sd.applyDiscounts(items, prices, discounted, categories);
        assertSame(discounted, result);
    }

    @Test
    public void testSingleDiscount_ApplyNonDouble() {
        // 30% off item 1 → new price = min(100, 100*0.7)=70
        var sd = new SingleDiscount(1, 30, null, false);
        var result = sd.applyDiscounts(items, prices, discounted, categories);
        assertEquals(70.0, result.get(1));
        // item 2 untouched
        assertEquals(50.0, result.get(2));
    }

    @Test
    public void testSingleDiscount_ApplyDouble() {
        // simulate a prior discount to 80
        discounted.put(1, 80.0);
        var sd = new SingleDiscount(1, 50, null, true);
        var result = sd.applyDiscounts(items, prices, discounted, categories);
        // double-branch: 80 * 0.5 = 40
        assertEquals(40.0, result.get(1));
    }

    @Test
    public void testSingleDiscount_InvalidPercentage() {
        assertThrows(IllegalArgumentException.class, () ->
            new SingleDiscount(1, -5, null, false)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new SingleDiscount(1, 150, null, true)
        );
    }

    @Test
    public void testSingleDiscount_PolicyBlocks() {
        // policy that always returns false
        var leaf = new PolicyLeaf((i,p,c) -> false);
        var sd = new SingleDiscount(1, 50, leaf, false);
        var result = sd.applyDiscounts(items, prices, discounted, categories);
        // no change since policy fails
        assertEquals(100.0, result.get(1));
    }

    // ----- CategoryDiscount -----

    @Test
    public void testCategoryDiscount_NonMatchingCategory() {
        // discount only applies to BOOKS but item1=ELECTRONICS
        var cd = new CategoryDiscount(ItemCategory.BOOKS, 20, null, false);
        var result = cd.applyDiscounts(items, prices, discounted, categories);
        assertEquals(100.0, result.get(1));
        assertEquals(40.0, result.get(2)); // item2 is BOOKS but qty=1>0 should be discounted to 40
    }

    @Test
    public void testCategoryDiscount_MatchingCategoryNonDouble() {
        var cd = new CategoryDiscount(ItemCategory.ELECTRONICS, 50, null, false);
        var result = cd.applyDiscounts(items, prices, discounted, categories);
        // 100 * 0.5 = 50
        assertEquals(50.0, result.get(1));
    }

    @Test
    public void testCategoryDiscount_MatchingCategoryDouble() {
        discounted.put(1, 80.0);
        var cd = new CategoryDiscount(ItemCategory.ELECTRONICS, 50, null, true);
        var result = cd.applyDiscounts(items, prices, discounted, categories);
        // 80 * 0.5 = 40
        assertEquals(40.0, result.get(1));
    }

    @Test
    public void testCategoryDiscount_InvalidPercentage() {
        assertThrows(IllegalArgumentException.class, () ->
            new CategoryDiscount(ItemCategory.BOOKS, -1, null, false)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new CategoryDiscount(ItemCategory.BOOKS, 101, null, true)
        );
    }

    @Test
    public void testCategoryDiscount_PolicyBlocks() {
        var leaf = new PolicyLeaf((i,p,c) -> false);
        var cd = new CategoryDiscount(ItemCategory.ELECTRONICS, 50, leaf, false);
        var result = cd.applyDiscounts(items, prices, discounted, categories);
        assertEquals(100.0, result.get(1));
    }

    // ----- GlobalDiscount -----

    @Test
    public void testGlobalDiscount_ApplyNonDouble() {
        var gd = new GlobalDiscount(10, null, false);
        var result = gd.applyDiscounts(items, prices, discounted, categories);
        // item1: 100 → 90; item2: 50 → 45
        assertEquals(90.0, result.get(1));
        assertEquals(45.0, result.get(2));
    }

    @Test
    public void testGlobalDiscount_ApplyDouble() {
        discounted.put(1, 80.0);
        discounted.put(2, 30.0);
        var gd = new GlobalDiscount(50, null, true);
        var result = gd.applyDiscounts(items, prices, discounted, categories);
        // item1: 80 → 40; item2: 30 → 15
        assertEquals(40.0, result.get(1));
        assertEquals(15.0, result.get(2));
    }

    @Test
    public void testGlobalDiscount_InvalidPercentage() {
        assertThrows(IllegalArgumentException.class, () ->
            new GlobalDiscount(-5, null, false)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new GlobalDiscount(150, null, true)
        );
    }

    @Test
    public void testGlobalDiscount_PolicyBlocks() {
        var leaf = new PolicyLeaf((i,p,c) -> false);
        var gd = new GlobalDiscount(50, leaf, false);
        var result = gd.applyDiscounts(items, prices, discounted, categories);
        assertEquals(100.0, result.get(1));
        assertEquals(50.0, result.get(2));
    }

    // ----- PolicyLeaf -----

    @Test
    public void testPolicyLeaf_Predicate() {
        TriPredicate<Map<Integer,Integer>,Map<Integer,Double>,Map<Integer,ItemCategory>> pred =
            (i,p,c) -> i.get(1) == 2;
        var leaf = new PolicyLeaf(pred);
        assertTrue(leaf.test(items, discounted, categories));
        items.put(1, 1);
        assertFalse(leaf.test(items, discounted, categories));
    }

    // ----- PolicyComposite -----

    @Test
    public void testPolicyComposite_NoPolicies() {
        var pc = new PolicyComposite(Operator.AND);
        // with neither policy1 nor policy2 set → returns true
        assertTrue(pc.test(items, discounted, categories));
    }

    @Test
    public void testPolicyComposite_SinglePolicy() {
        var leaf = new PolicyLeaf((i,p,c) -> i.containsKey(2));
        var pc = new PolicyComposite(leaf, Operator.OR);
        assertTrue(pc.test(items, discounted, categories));
        items.remove(2);
        assertFalse(pc.test(items, discounted, categories));
    }

    @Test
    public void testPolicyComposite_AND_OR_XOR() {
        var leafTrue = new PolicyLeaf((i,p,c) -> true);
        var leafFalse = new PolicyLeaf((i,p,c) -> false);

        var andComp = new PolicyComposite(leafTrue, leafFalse, Operator.AND);
        assertFalse(andComp.test(items, discounted, categories));

        var orComp = new PolicyComposite(leafTrue, leafFalse, Operator.OR);
        assertTrue(orComp.test(items, discounted, categories));

        var xorComp = new PolicyComposite(leafTrue, leafFalse, Operator.XOR);
        assertTrue(xorComp.test(items, discounted, categories));

        // both true for XOR → false
        var bothTrue = new PolicyComposite(leafTrue, leafTrue, Operator.XOR);
        assertFalse(bothTrue.test(items, discounted, categories));
    }

    @Test
    public void testPolicyComposite_addPolicy_exceptions() {
        var pc = new PolicyComposite(Operator.OR);
        pc.addPolicy(new PolicyLeaf((i,p,c) -> true));
        pc.addPolicy(new PolicyLeaf((i,p,c) -> true));
        assertThrows(IllegalStateException.class, () ->
            pc.addPolicy(new PolicyLeaf((i,p,c) -> false))
        );
    }
}
