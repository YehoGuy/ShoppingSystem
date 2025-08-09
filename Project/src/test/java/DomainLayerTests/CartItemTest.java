package DomainLayerTests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.CartItem;

public class CartItemTest {

    private CartItem cartItem;

    @BeforeEach
    public void setup() {
        cartItem = new CartItem(1, 100, 5);
    }

    @Test
    public void testSetShopId() {
        cartItem.setShopId(999);
        assertEquals(999, cartItem.getShopId());
    }

    @Test
    public void testSetShopIdNull() {
        cartItem.setShopId(null);
        assertNull(cartItem.getShopId());
    }

    @Test
    public void testSetProductId() {
        cartItem.setProductId(888);
        assertEquals(888, cartItem.getProductId());
    }

    @Test
    public void testSetProductIdNull() {
        cartItem.setProductId(null);
        assertNull(cartItem.getProductId());
    }

    @Test
    public void testSetQuantity() {
        cartItem.setQuantity(10);
        assertEquals(10, cartItem.getQuantity());
    }

    @Test
    public void testSetQuantityNull() {
        cartItem.setQuantity(null);
        assertNull(cartItem.getQuantity());
    }

    @Test
    public void testSetQuantityZero() {
        cartItem.setQuantity(0);
        assertEquals(0, cartItem.getQuantity());
    }

    @Test
    public void testEqualsWithSameObject() {
        assertTrue(cartItem.equals(cartItem));
    }

    @Test
    public void testEqualsWithNull() {
        assertFalse(cartItem.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        assertFalse(cartItem.equals("not a CartItem"));
    }

    @Test
    public void testEqualsWithSameShopIdAndProductId() {
        CartItem otherItem = new CartItem(1, 100, 10); // Different quantity but same shopId and productId
        assertFalse(cartItem.equals(otherItem));
    }

    @Test
    public void testEqualsWithDifferentShopId() {
        CartItem otherItem = new CartItem(2, 100, 5);
        assertFalse(cartItem.equals(otherItem));
    }

    @Test
    public void testEqualsWithDifferentProductId() {
        CartItem otherItem = new CartItem(1, 999, 5);
        assertFalse(cartItem.equals(otherItem));
    }

    @Test
    public void testEqualsWithNullShopIds() {
        CartItem item1 = new CartItem(null, 100, 5);
        CartItem item2 = new CartItem(null, 100, 3);
        assertFalse(item1.equals(item2));
    }

    @Test
    public void testEqualsWithOneNullShopId() {
        CartItem item1 = new CartItem(null, 100, 5);
        CartItem item2 = new CartItem(1, 100, 5);
        assertFalse(item1.equals(item2));
        assertFalse(item2.equals(item1));
    }

    @Test
    public void testEqualsWithNullProductIds() {
        CartItem item1 = new CartItem(1, null, 5);
        CartItem item2 = new CartItem(1, null, 3);
        assertFalse(item1.equals(item2));
    }

    @Test
    public void testEqualsWithOneNullProductId() {
        CartItem item1 = new CartItem(1, null, 5);
        CartItem item2 = new CartItem(1, 100, 5);
        assertFalse(item1.equals(item2));
        assertFalse(item2.equals(item1));
    }

    @Test
    public void testHashCodeConsistency() {
        int hash1 = cartItem.hashCode();
        int hash2 = cartItem.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCodeEqualObjects() {
        CartItem otherItem = new CartItem(1, 100, 10); // Same shopId and productId
        assertEquals(cartItem.hashCode(), otherItem.hashCode());
    }

    @Test
    public void testHashCodeWithNullShopId() {
        CartItem item = new CartItem(null, 100, 5);
        assertNotNull(item.hashCode()); // Should not throw exception
    }

    @Test
    public void testHashCodeWithNullProductId() {
        CartItem item = new CartItem(1, null, 5);
        assertNotNull(item.hashCode()); // Should not throw exception
    }

    @Test
    public void testHashCodeWithBothNull() {
        CartItem item = new CartItem(null, null, 5);
        assertEquals(0, item.hashCode());
    }

    @Test
    public void testUpdateItemQuantityPositiveValue() {
        boolean result = cartItem.updateItemQuantity(3);
        assertTrue(result);
        assertEquals(8, cartItem.getQuantity()); // 5 + 3 = 8
    }

    @Test
    public void testUpdateItemQuantityNegativeValue() {
        boolean result = cartItem.updateItemQuantity(-2);
        assertTrue(result);
        assertEquals(3, cartItem.getQuantity()); // 5 - 2 = 3
    }

    @Test
    public void testUpdateItemQuantityToZero() {
        boolean result = cartItem.updateItemQuantity(-5);
        assertFalse(result);
        assertEquals(0, cartItem.getQuantity()); // 5 - 5 = 0
    }

    @Test
    public void testUpdateItemQuantityFromZero() {
        cartItem.setQuantity(0);
        boolean result = cartItem.updateItemQuantity(3);
        assertTrue(result);
        assertEquals(3, cartItem.getQuantity());
    }

    @Test
    public void testUpdateItemQuantityZeroAddition() {
        boolean result = cartItem.updateItemQuantity(0);
        assertTrue(result);
        assertEquals(5, cartItem.getQuantity()); // No change
    }
}
