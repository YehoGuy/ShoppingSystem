package DomainLayerTests;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DomainLayer.ShoppingCart;

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
        //test where the shop id is not in the basket
        assertThrows(OurRuntime.class, () -> {
            shoppingCart.removeItem(999, 101); // remove item 101 from basket 999 (which does not exist)
        }, "Removing an item from a non-existing basket should throw OurRuntime");

        //test where item is in bids
        Map<Integer, Integer> bids = new HashMap<>();
        bids.put(102, 1); // item 102 with quantity 1 in basket
        shoppingCart.addBid(1, bids); // add bids to basket 1
        assertTrue(shoppingCart.getItems().get(1).containsKey(102), "Item 102 should be in basket 1");
        shoppingCart.removeItem(1, 102); // remove item 102 from basket 1
        assertNull(shoppingCart.getItems().get(1).get(102), "Item 102 should be removed from basket 1");
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
        //test where item is in bids
        Map<Integer, Integer> bids = new HashMap<>();
        bids.put(102, 3); // item 102 with quantity 1 in basket
        shoppingCart.addBid(1, bids); // add bids to basket 1
        assertTrue(shoppingCart.getItems().get(1).containsKey(102), "Item 102 should be in basket 1");
        assertEquals(3, shoppingCart.getItems().get(1).get(102), "Item 102 should have quantity 3 in basket 1");
        assertThrows(OurRuntime.class, () -> {
            shoppingCart.updateProduct(1, 102, 5); // update item 101 in basket 1 to negative quantity
        }); 


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
        assertTrue(shoppingCart.getBasket(1).isEmpty(), "Basket 1 should not exist and return empty map");
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

    @Test
    public void testShoppingCartBasketOperations() {
        ShoppingCart cart = new ShoppingCart();
        assertTrue(cart.getBasket(1).isEmpty());

        cart.addBasket(1);
        assertNotNull(cart.getBasket(1));
        assertTrue(cart.getBasket(1).isEmpty());

        cart.addItem(1, 100, 2);
        Map<Integer,Integer> basket = cart.getBasket(1);
        assertEquals(2, basket.get(100));

        cart.addItem(1, 100, 3);
        assertEquals(5, cart.getBasket(1).get(100));

        cart.updateProduct(1, 100, 1);
        assertEquals(1, cart.getBasket(1).get(100));

        cart.removeItem(1, 100);
        assertFalse(cart.getBasket(1).containsKey(100));

        // setBasket requires a HashMap
        HashMap<Integer,Integer> newBasket = new HashMap<>();
        newBasket.put(200, 4);
        cart.setBasket(1, newBasket);
        assertEquals(4, cart.getBasket(1).get(200));

        // removeBasket and clearCart
        cart.removeBasket(1);
        assertTrue(cart.getBasket(1).isEmpty());
        cart.addBasket(2);
        cart.clearCart();
        assertTrue(cart.getBasket(2).isEmpty());
    }

    // @Test
    // public void testShoppingCartMergeAndRestore() {
    //     ShoppingCart a = new ShoppingCart();
    //     ShoppingCart b = new ShoppingCart();
    //     a.addItem(1, 10, 1);
    //     b.addItem(1, 10, 2);
    //     a.mergeCart(b);
    //     assertEquals(3, a.getBasket(1).get(10));

    //     // restoreCart adds quantities
    //     HashMap<Integer, HashMap<Integer,Integer>> snapshot = a.getItems();
    //     ShoppingCart c = new ShoppingCart();
    //     c.addItem(1, 10, 5);
    //     c.restoreCart(snapshot);
    //     assertEquals(8, c.getBasket(1).get(10));
    // }

    @Test
    public void testrestoreWithEmptyCart() {
        ShoppingCart cart = new ShoppingCart();
        ShoppingCart newCart = new ShoppingCart();
        newCart.restoreCart(cart.getItems());
        assertTrue(newCart.getItems().isEmpty(), "Restoring an empty cart should result in an empty cart");
    }

    @Test
    public void testGetCart2() {
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(1, 100, 2);
        cart.addItem(1, 101, 3);
        Map<Integer, Integer> basket = cart.getBasket(1);
        assertEquals(2, basket.get(100));
        assertEquals(3, basket.get(101));
        assertEquals(2, cart.getItems().get(1).get(100));
        assertEquals(3, cart.getItems().get(1).get(101));
        assertNotNull(cart.getItems().get(1), "Items in basket 1 should not be null");
        assertTrue(cart.getItems().containsKey(1), "Items should contain basket 1");
    }

    @Test
    public void testGetItemsDeepCopy() {
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(3, 30, 7);
        HashMap<Integer, HashMap<Integer,Integer>> copy = cart.getItems();
        copy.get(3).put(30, 0);
        // original should remain unchanged
        assertEquals(7, cart.getBasket(3).get(30));
    }

    @Test
    public void testGetCart()
    {
        ShoppingCart cart = new ShoppingCart();
        cart.addItem(1, 100, 2);
        cart.addItem(1, 101, 3);
        Map<Integer, Integer> basket = cart.getBasket(1);
        assertEquals(2, basket.get(100));
        assertEquals(3, basket.get(101));
        assertEquals(2, cart.getItems().get(1).get(100));
        assertEquals(3, cart.getItems().get(1).get(101));
        assertNotNull(cart.getItems().get(1), "Items in basket 1 should not be null");
        assertTrue(cart.getItems().containsKey(1), "Items should contain basket 1");
    }



}
