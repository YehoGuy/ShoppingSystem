package DomainLayerTests;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import DomainLayer.ShoppingCart;

public class ShoppingCartTests {

    private ShoppingCart shoppingCart;

    @BeforeEach
    void setUp() {
        shoppingCart = new ShoppingCart();
    }    

    @Test
    void testAddBasket() {
        shoppingCart.addBasket(1);
        assertTrue(shoppingCart.getItems().containsKey(1), "Basket with ID 1 should be added");
    }

    @Test
    void testRemoveBasket() {
        shoppingCart.addBasket(1);
        shoppingCart.removeBasket(1);
        assertFalse(shoppingCart.getItems().containsKey(1), "Basket with ID 1 should be removed");
    }

    @Test
    void testAddItem() {
        shoppingCart.addItem(1, 101, 2);
        assertEquals(2, shoppingCart.getItems().get(1).get(101), "Item 101 should have quantity 2 in basket 1");
    }

    @Test
    void testAddItemExisting() {
        shoppingCart.addItem(1, 101, 2); // item 101 in basket 1 with quantity=2
        shoppingCart.addItem(1, 101, 3); // add 3 more of item 101 in basket 1
        assertEquals(5, shoppingCart.getItems().get(1).get(101), "Item 101 should have quantity 5 in basket 1");
    }

    @Test
    void testRemoveItem() {
        shoppingCart.addItem(1, 101, 5); // item 101 in basket 1 with quantity=5
        shoppingCart.removeItem(1, 101); // remove item 101 from basket 1
        assertNull(shoppingCart.getItems().get(1).get(101), "Item 101 should be removed from basket 1");
    }

    @Test
    void testRemoveItemAllQuantity() {
        shoppingCart.addItem(1, 101, 5); // item 101 in basket 1 with quantity=5
        shoppingCart.removeItem(1, 101); // remove item 101 from basket 1
        assertFalse(shoppingCart.getItems().get(1).containsKey(101), "Item 101 should be removed from basket 1");
    }

    @Test
    void testSetBasket() {
        HashMap<Integer, Integer> newBasket = new HashMap<>();
        newBasket.put(101, 2);
        shoppingCart.setBasket(1, newBasket);
        assertEquals(newBasket, shoppingCart.getItems().get(1), "Basket 1 should be set to newBasket");
    }

    @Test
    void testUpdateProduct() {
        shoppingCart.addItem(1, 101, 2); // item 101 in basket 1 with quantity=2
        shoppingCart.updateProduct(1, 101, 5); // update item 101 in basket 1 to quantity=5
        assertEquals(5, shoppingCart.getItems().get(1).get(101), "Item 101 should have quantity 5 in basket 1");
    }

    @Test
    void testMergeCart() {
        ShoppingCart otherCart = new ShoppingCart();
        otherCart.addItem(1, 101, 2);
        shoppingCart.mergeCart(otherCart);
        assertEquals(2, shoppingCart.getItems().get(1).get(101), "Item 101 should have quantity 2 in basket 1 after merge");
    }

    @Test
    void testClearCart() {
        shoppingCart.addItem(1, 101, 2); // item 101 in basket 1 with quantity=2
        shoppingCart.clearCart(); // clear the cart
        assertTrue(shoppingCart.getItems().isEmpty(), "Shopping cart should be empty after clear");
    }

    @Test
    void testGetBasket() {
        HashMap<Integer, Integer> basket = new HashMap<>();
        basket.put(101, 2);
        shoppingCart.setBasket(1, basket);
        assertEquals(basket, shoppingCart.getBasket(1), "Basket 1 should be retrieved correctly");
    }

    @Test
    void testGetBasketNotFound() {
        assertNull(shoppingCart.getBasket(1), "Basket 1 should not exist and return null");
    }

    @Test
    void testGetItems() {
        HashMap<Integer, Integer> basket = new HashMap<>();
        basket.put(101, 2);
        shoppingCart.setBasket(1, basket);
        assertEquals(basket, shoppingCart.getItems().get(1), "Items in basket 1 should be retrieved correctly");
    }

    @Test
    void testGetItemsNotFound() {
        assertNull(shoppingCart.getItems().get(1), "Items in basket 1 should not exist and return null");
    }
}
