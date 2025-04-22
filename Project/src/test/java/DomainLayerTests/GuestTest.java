import DomainLayer.Guest;
import DomainLayer.ShoppingCart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class GuestTest {

    private Guest guest;

    @BeforeEach
    void setUp() {
        guest = new Guest(1001);
    }

    @Test
    void testGuestIdInitialization() {
        assertEquals(1001, guest.getGuestId());
    }

    @Test
    void testSetAndGetShoppingCart() {
        ShoppingCart newCart = new ShoppingCart();
        guest.setShoppingCart(newCart);
        assertEquals(newCart, guest.getShoppingCart());
    }

    @Test
    void testMergeShoppingCartFunctionality() {
        ShoppingCart cart = guest.getShoppingCart();
        cart.addItem(1, 101, 2);

        ShoppingCart otherCart = new ShoppingCart();
        otherCart.addItem(1, 101, 3); // should merge quantities
        otherCart.addItem(2, 201, 5); // new shop

        guest.mergeShoppingCart(otherCart);

        HashMap<Integer, HashMap<Integer, Integer>> items = cart.getItems();

        assertEquals(2, items.size());
        assertEquals(5, items.get(1).get(101)); // 2 + 3
        assertEquals(5, items.get(2).get(201)); // new item
    }


    @Test
    void testPaymentMethodIsNullByDefault() {
        assertNull(guest.getPaymentMethod());
    }
}
