package DomainLayerTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.DomainLayer.ShoppingCart;
import com.example.app.DomainLayer.User;
import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.ApplicationLayer.OurRuntime;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    public void testUserShoppingCartAndPayment() {
        User u = new User(0){};
        assertNotNull(u.getShoppingCart());

        ShoppingCart sc = new ShoppingCart();
        sc.addItem(5, 50, 2);
        u.setShoppingCart(sc);
        assertSame(sc, u.getShoppingCart());
        // stub PaymentMethod implementation
        PaymentMethod pm = mock(PaymentMethod.class);
        u.setPaymentMethod(pm);
        assertSame(pm, u.getPaymentMethod());

        Address addr = new Address()
            .withCountry("C").withCity("Ci").withStreet("St")
            .withApartmentNumber(1).withZipCode("Z");
        u.setAddress(addr);
        assertSame(addr, u.getAddress());

        u.setAddress("X","Y","Z",2,"P");
        assertNotNull(u.getAddress());
    }

    @Test
    void testUpdateShoppingCartItemQuantity() {
        // Add an item to the cart
        cart.addItem(1, 101, 2);
        assertTrue(cart.hasItemOfShop(1, 101));

        // Update quantity to increase
        user.updateShoppingCartItemQuantity(1, 101, true);
        assertEquals(3, cart.getItems().get(1).get(101));

        // Update quantity to decrease
        user.updateShoppingCartItemQuantity(1, 101, false);
        assertEquals(2, cart.getItems().get(1).get(101));

    }

    @Test
    void testUpdateShoppingCartItemQuantityThrowsExceptionForNonExistentItem() {
        // Attempt to update quantity of an item not in the cart
        assertThrows(OurRuntime.class, () -> {
            user.updateShoppingCartItemQuantity(1, 999, true);
        });
    }

    @Test
    void testremoveItemFromCart() {
        // Add an item to the cart
        cart.addItem(1, 101, 2);
        assertTrue(cart.hasItemOfShop(1, 101));

        // Remove the item
        user.removeShoppingCartItem(1, 101);
        assertFalse(cart.hasItemOfShop(1, 101));
    }

    @Test
    void testRemoveItemFromCartThrowsExceptionForNonExistentItem() {
        // Attempt to remove an item not in the cart
        assertThrows(OurRuntime.class, () -> {
            user.removeShoppingCartItem(1, 999);
        });
    }
}
