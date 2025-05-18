package DomainLayerTests;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Notification;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.ShoppingCart;
import com.example.app.DomainLayer.User;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;

public class DomainLayerTests {

    // ----- ShoppingCart -----

    @Test
    public void testShoppingCartBasketOperations() {
        ShoppingCart cart = new ShoppingCart();
        assertNull(cart.getBasket(1));

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
        assertNull(cart.getBasket(1));
        cart.addBasket(2);
        cart.clearCart();
        assertNull(cart.getBasket(2));
    }

    @Test
    public void testShoppingCartMergeAndRestore() {
        ShoppingCart a = new ShoppingCart();
        ShoppingCart b = new ShoppingCart();
        a.addItem(1, 10, 1);
        b.addItem(1, 10, 2);
        a.mergeCart(b);
        assertEquals(3, a.getBasket(1).get(10));

        // restoreCart adds quantities
        HashMap<Integer, HashMap<Integer,Integer>> snapshot = a.getItems();
        ShoppingCart c = new ShoppingCart();
        c.addItem(1, 10, 5);
        c.restoreCart(snapshot);
        assertEquals(8, c.getBasket(1).get(10));
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

    // ----- User -----

    @Test
    public void testUserShoppingCartAndPayment() {
        User u = new User(0){};
        assertNotNull(u.getShoppingCart());

        ShoppingCart sc = new ShoppingCart();
        sc.addItem(5, 50, 2);
        u.setShoppingCart(sc);
        assertSame(sc, u.getShoppingCart());

        // stub PaymentMethod implementation
        PaymentMethod pm = new PaymentMethod() {
            @Override public void processPayment(double amount, int shopId) {}
            @Override public String getDetails() { return "stub"; }
            @Override public void refundPayment(double amount, int shopId) {}
            @Override public void processRefund(double refund, int shopId) {}
        };
        u.setPaymentMethod(pm);
        assertSame(pm, u.getPaymentMethod());
        assertEquals("stub", u.getPaymentMethod().getDetails());

        Address addr = new Address()
            .withCountry("C").withCity("Ci").withStreet("St")
            .withApartmentNumber(1).withZipCode("Z");
        u.setAddress(addr);
        assertSame(addr, u.getAddress());

        u.setAddress("X","Y","Z",2,"P");
        assertNotNull(u.getAddress());
    }

    // ----- Member -----

    @Test
    public void testMemberBasicFieldsAndSuspend() {
        Member m = new Member(1, "user","pass","e@x","123", "addr");
        assertEquals(1, m.getMemberId());
        assertEquals("user", m.getUsername());
        assertEquals("pass", m.getPassword());
        assertEquals("e@x", m.getEmail());
        assertEquals("123", m.getPhoneNumber());
        assertFalse(m.isSuspended());

        LocalDateTime future = LocalDateTime.now().plusDays(1);
        m.setSuspended(future);
        assertTrue(m.isSuspended());

        m.setUsername("u2");
        m.setPassword("p2");
        m.setEmail("e2");
        m.setPhoneNumber("456");
        assertEquals("u2", m.getUsername());
        assertEquals("p2", m.getPassword());
        assertEquals("e2", m.getEmail());
        assertEquals("456", m.getPhoneNumber());
    }

    @Test
    public void testMemberOrderHistory() {
        Member m = new Member(2,"u","p","e","ph","addr");
        assertTrue(m.getOrderHistory().isEmpty());
        m.addOrderToHistory(100);
        List<Integer> hist = m.getOrderHistory();
        assertEquals(1, hist.size());
        assertEquals(100, hist.get(0));
    }

    @Test
    public void testMemberRolesAndPending() {
        Member m = new Member(3,"u","p","e","ph","addr");
        Role r1 = new Role(3,10,null);
        m.addRole(r1);
        assertTrue(m.getRoles().contains(r1));
        assertTrue(m.hasRole(r1));
        m.removeRole(r1);
        assertFalse(m.hasRole(r1));

        Role p = new Role(3,20,null);
        m.addRoleToPending(p);
        assertTrue(m.getPendingRoles().contains(p));
        m.acceptRole(p);
        assertTrue(m.getRoles().contains(p));
        assertFalse(m.getPendingRoles().contains(p));

        assertThrows(IllegalArgumentException.class, () ->
            m.declineRole(new Role(0,0,null))
        );
    }

    @Test
    public void testMemberPermissionManagement() {
        Member m = new Member(4,"u","p","e","ph","addr");
        Role r = new Role(4,30,null);
        m.addRole(r);
        assertFalse(m.hasPermission(PermissionsEnum.manageItems,30));
        r.addPermission(PermissionsEnum.manageItems);
        assertTrue(m.hasPermission(PermissionsEnum.manageItems,30));
        m.removePermission(30, PermissionsEnum.manageItems);
        assertFalse(m.hasPermission(PermissionsEnum.manageItems,30));
        assertThrows(RuntimeException.class, () -> m.addPermission(99, PermissionsEnum.manageItems));
    }

    @Test
    public void testMemberEquals() {
        Member m1 = new Member(5,"u","p","e","ph","addr");
        Member m2 = new Member(5,"x","y","z","w","addr");
        Member m3 = new Member(6,"u","p","e","ph","addr");
        assertEquals(m1, m2);
        assertNotEquals(m1, m3);
        assertNotEquals(m1, null);
        assertNotEquals(m1, new Object());
    }

    // ----- Notification -----

    @Test
    public void testNotificationGetSetAndToString() {
        Notification n = new Notification("T","M");
        assertEquals("T", n.getTitle());
        assertEquals("M", n.getMessage());

        n.setTitle("T2");
        n.setMessage("M2");
        assertTrue(n.toString().contains("T2"));
        assertTrue(n.toString().contains("M2"));
    }
}
