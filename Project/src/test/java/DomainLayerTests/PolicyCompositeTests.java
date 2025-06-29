package DomainLayerTests;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

import com.example.app.DomainLayer.Item.ItemCategory;
import com.example.app.DomainLayer.Shop.Operator;
import com.example.app.DomainLayer.Shop.Discount.PolicyComposite;
import com.example.app.DomainLayer.Shop.Discount.PolicyLeaf;

public class PolicyCompositeTests {

    private final Map<Integer,Integer> items = Map.of(1,1);
    private final Map<Integer,Double> prices = Map.of(1,100.0);
    private final Map<Integer,ItemCategory> categories = Map.of(1, ItemCategory.ELECTRONICS);

    @Test
    void testNoPolicies() {
        PolicyComposite pc = new PolicyComposite(Operator.AND);
        assertTrue(pc.test(items, prices, categories));
        assertEquals(Operator.AND, pc.getOperator());
        assertNull(pc.getPolicy1());
        assertNull(pc.getPolicy2());
    }

    @Test
    void testSinglePolicyConstructor() {
        PolicyLeaf leaf = new PolicyLeaf(1, 1, null, null);
        PolicyComposite pc = new PolicyComposite(leaf, Operator.OR);
        assertEquals(Operator.OR, pc.getOperator());
        assertSame(leaf, pc.getPolicy1());
        assertNull(pc.getPolicy2());
        assertTrue(pc.test(items, prices, categories));
    }

    @Test
    void testSinglePolicyAddOnly() {
        PolicyLeaf leaf = new PolicyLeaf(5, 1, null, null);
        PolicyComposite pc = new PolicyComposite(Operator.OR);
        pc.addPolicy(leaf);
        assertEquals(Operator.OR, pc.getOperator());
        assertSame(leaf, pc.getPolicy1());
        assertNull(pc.getPolicy2());
        assertFalse(pc.test(items, prices, categories));
    }

    @Test
    void testCompositeTwoPoliciesAndOperator() {
        PolicyLeaf t = new PolicyLeaf(1, 1, null, null);
        PolicyLeaf f = new PolicyLeaf(2, 1, null, null);
        PolicyComposite pc = new PolicyComposite(t, f, Operator.AND);
        assertEquals(Operator.AND, pc.getOperator());
        assertSame(t, pc.getPolicy1());
        assertSame(f, pc.getPolicy2());
        assertFalse(pc.test(items, prices, categories));
    }

    @Test
    void testCompositeTwoPoliciesOrOperator() {
        PolicyLeaf t = new PolicyLeaf(1, 1, null, null);
        PolicyLeaf f = new PolicyLeaf(2, 1, null, null);
        PolicyComposite pc = new PolicyComposite(t, f, Operator.OR);
        assertTrue(pc.test(items, prices, categories));
    }

    @Test
    void testCompositeTwoPoliciesXorOperator() {
        PolicyLeaf t = new PolicyLeaf(1, 1, null, null);
        PolicyLeaf f = new PolicyLeaf(2, 1, null, null);
        PolicyComposite pc = new PolicyComposite(t, f, Operator.XOR);
        assertTrue(pc.test(items, prices, categories));
    }

    @Test
    void testAddPolicyOverflow() {
        PolicyComposite pc = new PolicyComposite(Operator.AND);
        pc.addPolicy(new PolicyLeaf());
        pc.addPolicy(new PolicyLeaf());
        assertThrows(IllegalStateException.class, () -> pc.addPolicy(new PolicyLeaf()));
    }

    @Test
    void testInvalidOperatorViaReflection() throws Exception {
        PolicyComposite pc = new PolicyComposite(new PolicyLeaf(), new PolicyLeaf(), Operator.AND);
        var opField = PolicyComposite.class.getDeclaredField("operator");
        opField.setAccessible(true);
        opField.set(pc, null);
        assertThrows(NullPointerException.class, () -> pc.test(items, prices, categories));
    }

    @Test
    void testDefaultConstructorAndGetters() {
        PolicyComposite pc = new PolicyComposite();
        assertNull(pc.getPolicy1());
        assertNull(pc.getPolicy2());
        assertEquals(Operator.AND, pc.getOperator());
        assertTrue(pc.test(items, prices, categories));
    }
}
