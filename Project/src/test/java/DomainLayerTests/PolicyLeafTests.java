package DomainLayerTests;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Discount.PolicyLeaf;

public class PolicyLeafTests {

    private final Map<Integer,Integer> items = Map.of(1,2, 2,1);
    private final Map<Integer,Double> prices = Map.of(1,100.0, 2,50.0);
    private final Map<Integer,ItemCategory> categories = Map.of(1, ItemCategory.ELECTRONICS, 2, ItemCategory.BOOKS);

    @Test
    void testThresholdWithItemTrue() {
        PolicyLeaf pl = new PolicyLeaf(2, 1, null, null);
        assertTrue(pl.test(items, prices, categories));
    }

    @Test
    void testThresholdWithItemFalse() {
        PolicyLeaf pl = new PolicyLeaf(3, 1, null, null);
        assertFalse(pl.test(items, prices, categories));
    }

    @Test
    void testThresholdWithCategoryTrue() {
        PolicyLeaf pl = new PolicyLeaf(1, null, ItemCategory.ELECTRONICS, null);
        assertTrue(pl.test(items, prices, categories));
    }

    @Test
    void testThresholdWithCategoryFalse() {
        PolicyLeaf pl = new PolicyLeaf(3, null, ItemCategory.BOOKS, null);
        assertFalse(pl.test(items, prices, categories));
    }

    @Test
    void testBasketValueTrue() {
        // total = 2*100 + 1*50 = 250
        PolicyLeaf pl = new PolicyLeaf(null, null, null, 200.0);
        assertTrue(pl.test(items, prices, categories));
    }

    @Test
    void testBasketValueFalse() {
        PolicyLeaf pl = new PolicyLeaf(null, null, null, 300.0);
        assertFalse(pl.test(items, prices, categories));
    }

    @Test
    void testDefaultCase() {
        PolicyLeaf pl = new PolicyLeaf();
        assertTrue(pl.test(items, prices, categories));
    }

    @Test
    void testGetters() {
        PolicyLeaf pl = new PolicyLeaf(2, 1, ItemCategory.BOOKS, 150.0);
        assertEquals(2, pl.getThreshold());
        assertEquals(1, pl.getItemId());
        assertEquals(ItemCategory.BOOKS, pl.getCategory());
        assertEquals(150.0, pl.getBasketValue());
    }
}
