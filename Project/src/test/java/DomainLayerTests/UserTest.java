package DomainLayerTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.ShoppingCart;
import com.example.app.DomainLayer.User;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

// A simple concrete subclass of User for testing purposes
class TestUser extends User {
    public TestUser(int cartId) {
        super(cartId);
    }
}

public class UserTest {

    private TestUser user;
    private ShoppingCart cart;

    @BeforeEach
    void setUp() {
        user = new TestUser(1);
        cart = user.getShoppingCart();
    }

    @Test
    void testGetAndSetShoppingCart() {
        ShoppingCart newCart = new ShoppingCart();
        user.setShoppingCart(newCart);
        assertEquals(newCart, user.getShoppingCart());
    }

    @Test
    void testMergeShoppingCartAddsItems() {
        // Add items to original cart
        cart.addItem(1, 101, 2);
        cart.addItem(2, 201, 3);

        // Create another cart with overlapping and new items
        ShoppingCart otherCart = new ShoppingCart();
        otherCart.addItem(1, 101, 1); // same shop/product
        otherCart.addItem(1, 102, 5); // new product same shop
        otherCart.addItem(3, 301, 7); // new shop

        user.mergeShoppingCart(otherCart);
        HashMap<Integer, HashMap<Integer, Integer>> items = cart.getItems();

        assertEquals(3, items.size());
        assertEquals(3, items.get(1).get(101)); // 2 + 1
        assertEquals(5, items.get(1).get(102)); // new
        assertEquals(3, items.get(2).get(201)); // unchanged
        assertEquals(7, items.get(3).get(301)); // new shop
    }

    @Test
    void testMergeShoppingCartWithNull() {
        // Should not throw any exception
        user.mergeShoppingCart(null);
        assertNotNull(user.getShoppingCart());
    }

    @Test
    void testGetPaymentMethodIsNullByDefault() {
        assertNull(user.getPaymentMethod());
    }
}
