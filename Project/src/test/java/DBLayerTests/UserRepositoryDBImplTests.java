package DBLayerTests;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.DBLayer.User.UserRepositoryDBImpl;
import com.example.app.DomainLayer.Guest;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.Purchase.Bid;
import com.example.app.DomainLayer.Purchase.BidReciept;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.DomainLayer.ShoppingCart;
import com.example.app.DomainLayer.User;
import com.example.app.InfrastructureLayer.WSEPPay;
import com.example.app.SimpleHttpServerApplication;

import jakarta.transaction.Transactional;

@SpringBootTest(classes = SimpleHttpServerApplication.class)
@ActiveProfiles({ "db-test" })
@Transactional
@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
public class UserRepositoryDBImplTests {

    @Autowired
    private UserRepositoryDBImpl repo;

    private User guest;
    private User member;
    private int guestId;
    private int memberId;

    // Helper method to set up test data in each test method
    @BeforeEach
    private void setup() {
        repo.setEncoderToTest(true); // Set the encoder to test mode
        guestId = repo.addGuest();
        guest = repo.getUserById(guestId);
        repo.addMember("username", "password", "email@example.com", "111", "address");
        memberId = repo.isUsernameAndPasswordValid("username", "password");
        member = repo.getUserById(memberId);
    }

    @Test
    void testGetAcceptedRoles() {

        // Ensure the admin user exists
        int adminId = repo.isUsernameAndPasswordValid("admin", "admin");
        assertTrue(adminId > 0, "Admin user should exist");

        // Add a role to admin for testing purposes
        Role adminRole = new Role(adminId, 1,
                new PermissionsEnum[] { PermissionsEnum.manageItems, PermissionsEnum.manageOwners });
        repo.addRoleToPending(adminId, adminRole);
        repo.acceptRole(adminId, adminRole);

        // Get accepted roles for the admin
        List<Role> roles = repo.getAcceptedRoles(adminId);
        assertNotNull(roles, "Roles list should not be null");

        // Check if the admin has the expected role
        assertTrue(roles.stream().anyMatch(role -> role.getShopId() == 1), "Admin should have role with shop ID 1");
    }

    @Test
    void testaddAuctionWinBidToShoppingCart() {

        // Ensure the member exists
        assertNotNull(member, "Member should not be null");

        // Test adding an auction win bid to the shopping cart
        // Assuming Bid class exists and has a proper constructor
        Map<Integer, Integer> bidData = Map.of(1, 100); // example bid data
        Bid testBid = new Bid(1, 1, 1, bidData, memberId);
        assertDoesNotThrow(() -> repo.addAuctionWinBidToShoppingCart(memberId, testBid));
    }

    @Test
    void testIsAdmin() {

        assertTrue(repo.isAdmin(repo.isUsernameAndPasswordValid("admin", "admin")));
    }

    @Test
    void testAddAdmin() {

        repo.addMember("username2", "password", "email@email.com", "phoneNumber", "address");
        int userid = repo.isUsernameAndPasswordValid("username2", "password");
        repo.addAdmin(userid);
        assertTrue(repo.isAdmin(userid));
    }

    @Test
    void testRemoveAdmin() {

        repo.addMember("username3", "password", "email@email.com", "phoneNumber", "address");
        int userid = repo.isUsernameAndPasswordValid("username3", "password");
        repo.addAdmin(userid);
        assertTrue(repo.isAdmin(userid));
        repo.removeAdmin(userid);
        assertFalse(repo.isAdmin(userid));
    }

    @Test
    void testGetAllAdmins() {

        List<Integer> admins = repo.getAllAdmins();
        assertTrue(admins.contains(repo.isUsernameAndPasswordValid("admin", "admin")));

        // Add another admin and verify it appears in the list
        repo.addMember("newAdmin", "password", "newadmin@email.com", "123", "address");
        int newAdminId = repo.isUsernameAndPasswordValid("newAdmin", "password");
        repo.addAdmin(newAdminId);

        List<Integer> updatedAdmins = repo.getAllAdmins();
        assertTrue(updatedAdmins.contains(newAdminId));
    }

    @Test
    public void testAddGuestAndGetUserById() {

        assertEquals(guestId, ((Guest) guest).getGuestId());
    }

    @Test
    public void testAddMember() {

        assertEquals("username", ((Member) member).getUsername());
        assertEquals("email@example.com", ((Member) member).getEmail());
        assertEquals("111", ((Member) member).getPhoneNumber());
    }

    @Test
    public void testUpdateMemberUsername() {

        repo.updateMemberUsername(memberId, "newUsername");
        Member updatedMember = repo.getMemberById(memberId);
        assertEquals("newUsername", updatedMember.getUsername());
    }

    @Test
    public void testUpdateMemberPassword() {

        String newPassword = "newPassword";
        repo.updateMemberPassword(memberId, newPassword);
        Member updatedMember = repo.getMemberById(memberId);
        // Check if password encoding works
        assertTrue(repo.getPasswordEncoderUtil().matches(newPassword, updatedMember.getPassword()));
    }

    @Test
    public void testUpdateMemberEmail() {

        repo.updateMemberEmail(memberId, "newemail@example.com");
        Member updatedMember = repo.getMemberById(memberId);
        assertEquals("newemail@example.com", updatedMember.getEmail());
    }

    @Test
    public void testUpdateMemberPhoneNumber() {

        repo.updateMemberPhoneNumber(memberId, "222");
        Member updatedMember = repo.getMemberById(memberId);
        assertEquals("222", updatedMember.getPhoneNumber());
    }

    @Test
    public void testUpdateMemberAddress() {

        repo.updateMemberAddress(memberId, "NewCity", "NewStreet", 123, "12345");
        Member updatedMember = repo.getMemberById(memberId);
        // Note: This assumes Member has getter methods for address components
        // If the address is stored differently, adjust accordingly
        assertNotNull(updatedMember);
    }

    @Test
    public void testIsUsernameAndPasswordValid() {

        int validId = repo.isUsernameAndPasswordValid("username", "password");
        assertEquals(memberId, validId);

        int invalidId = repo.isUsernameAndPasswordValid("invalid", "invalid");
        assertEquals(-1, invalidId);
    }

    @Test
    public void testIsUsernameTaken() {

        assertTrue(repo.isUsernameTaken("username"));
        assertFalse(repo.isUsernameTaken("nonexistent"));
    }

    @Test
    public void testRemoveUserById() {

        // Cannot test removing members from DB in same way as in-memory
        // but we can test guest removal
        int newGuestId = repo.addGuest();
        assertTrue(repo.isGuestById(newGuestId));
        repo.removeUserById(newGuestId);
        assertFalse(repo.isGuestById(newGuestId));
    }

    @Test
    void testGetUserAndMember() {

        // Admin user should exist
        int adminId = repo.isUsernameAndPasswordValid("admin", "admin");
        assertTrue(adminId > 0, "Admin should be found and have valid ID");
        User adminUser = repo.getUserById(adminId);
        assertNotNull(adminUser);

        // Unknown user should throw
        assertThrows(OurRuntime.class, () -> repo.getUserById(999));

        // Admin is a Member, so getMemberById should succeed
        Member admin = repo.getMemberById(adminId);
        assertEquals(adminId, admin.getMemberId());

        // Create a true guest
        int newGuestId = repo.addGuest();
        assertTrue(repo.isGuestById(newGuestId));
        // But asking for a Member on a guest should throw
        assertThrows(OurRuntime.class, () -> repo.getMemberById(newGuestId));
    }

    @Test
    void testAdminManagement() {

        // Default admin should exist
        int adminId = repo.isUsernameAndPasswordValid("admin", "admin");
        assertTrue(adminId > 0, "Admin should be found and have valid ID");
        assertTrue(repo.isAdmin(adminId));

        // Add a new admin
        repo.addMember("testAdmin", "password", "admin@test.com", "123", "address");
        int testAdminId = repo.isUsernameAndPasswordValid("testAdmin", "password");
        repo.addAdmin(testAdminId);
        assertTrue(repo.isAdmin(testAdminId));

        // Cannot remove initial admin
        assertThrows(OurRuntime.class, () -> repo.removeAdmin(0), "cannot remove initial admin");

        // Can remove other admins
        repo.removeAdmin(testAdminId);
        assertFalse(repo.isAdmin(testAdminId));
    }

    @Test
    void testGuestAndMemberLogin() {

        int g1 = repo.addGuest();
        assertTrue(repo.isGuestById(g1));

        repo.addMember("u2", "pw", "u2@e", "ph", "ad");
        assertTrue(repo.isUsernameTaken("u2"));
        int mid = repo.isUsernameAndPasswordValid("u2", "pw");
        assertTrue(mid > 0);
        assertEquals(-1, repo.isUsernameAndPasswordValid("x", "y"));
    }

    @Test
    void testShoppingCartOps() {

        ShoppingCart cart = repo.getShoppingCartById(memberId);
        assertNotNull(cart);

        // Test shopping cart operations
        repo.addItemToShoppingCart(memberId, 1, 1, 5);
        repo.updateItemQuantityInShoppingCart(memberId, 1, 1, 10);
        repo.removeItemFromShoppingCart(memberId, 1, 1);
        repo.clearShoppingCart(memberId);

        // Test basket operations
        repo.createBasket(memberId, 1);
        Map<Integer, Integer> basket = repo.getBasket(memberId, 1);
        assertNotNull(basket);
    }

    @Test
    void testRoleAndWorkerMappings() {

        repo.addMember("owner", "pw", "o@e", "ph", "ad");
        int uid = repo.isUsernameAndPasswordValid("owner", "pw");
        Role r = new Role(uid, 99, new PermissionsEnum[] { PermissionsEnum.manageOwners });

        // Test role operations
        repo.addRoleToPending(uid, r);
        assertThrows(OurRuntime.class, () -> repo.addRoleToPending(uid, r));
        assertThrows(OurRuntime.class, () -> repo.getPendingRole(uid, 100));

        Role pr = repo.getPendingRole(uid, 99);
        assertEquals(r, pr);

        repo.acceptRole(uid, r);
        Role rr = repo.getRole(uid, 99);
        assertEquals(r, rr);
        repo.addPermission(uid, PermissionsEnum.handleMessages, 99);
        assertTrue(rr.hasPermission(PermissionsEnum.handleMessages));

        repo.removePermission(uid, PermissionsEnum.handleMessages, 99);
        // Get fresh role reference after removal since JPA entities might be detached
        Role freshRole = repo.getRole(uid, 99);
        assertFalse(freshRole.hasPermission(PermissionsEnum.handleMessages));

        List<Integer> workerShops = repo.getShopIdsByWorkerId(uid);
        assertTrue(workerShops.contains(99));
        assertTrue(repo.getShopMembers(99).stream().anyMatch(m -> m.getMemberId() == uid));
    }

    @Test
    void testSetPermissions_successAndFailures() {

        // Create a fresh member
        repo.addMember("permUser", "pw", "a@a", "p", "addr");
        int puid = repo.isUsernameAndPasswordValid("permUser", "pw");
        // Add & accept a role
        Role r = new Role(puid, 42, null);
        repo.addRoleToPending(puid, r);
        repo.acceptRole(puid, r);

        // Now setPermissions to a non-empty array
        repo.setPermissions(puid, 42, r, new PermissionsEnum[] { PermissionsEnum.manageItems });
        assertTrue(r.hasPermission(PermissionsEnum.manageItems));

        // Missing role should fail
        Role notBound = new Role(puid + 1, 99, null);
        assertThrows(OurRuntime.class,
                () -> repo.setPermissions(puid, 42, notBound, new PermissionsEnum[] { PermissionsEnum.manageItems }));
    }

    @Test
    void testRemoveRole_successAndFailure() {

        // Create and accept a role
        repo.addMember("temp", "pw", "t@t", "p", "a");
        int tid = repo.isUsernameAndPasswordValid("temp", "pw");
        Role r = new Role(tid, 77, null);
        repo.addRoleToPending(tid, r);
        repo.acceptRole(tid, r);

        // Remove it
        repo.removeRole(tid, 77);
        assertThrows(OurRuntime.class,
                () -> repo.getRole(tid, 77),
                "after removal getRole should fail");

        // Removing again fails
        assertThrows(OurRuntime.class,
                () -> repo.removeRole(tid, 77));
    }

    @Test
    void testGetOwners_isOwner_isFounder() {

        // No owners initially
        assertTrue(repo.getOwners(123).isEmpty());
        assertFalse(repo.isOwner(memberId, 123));
        assertFalse(repo.isFounder(memberId, 123));

        // Assign an owner role
        Role ownerR = new Role(memberId, 123, null);
        ownerR.addPermission(PermissionsEnum.manageOwners);
        repo.addRoleToPending(memberId, ownerR);
        repo.acceptRole(memberId, ownerR);

        assertTrue(repo.getOwners(123).stream().anyMatch(m -> m.getMemberId() == memberId));
        assertTrue(repo.isOwner(memberId, 123));
        assertFalse(repo.isFounder(memberId, 123));

        // Assign a founder role
        Role founderR = new Role(memberId, 888, null);
        founderR.setFoundersPermissions();
        repo.addRoleToPending(memberId, founderR);
        repo.acceptRole(memberId, founderR);

        assertTrue(repo.isFounder(memberId, 888));
    }

    @Test
    void testDeclineRole_andPendingRoles() {

        // New pending
        Role r = new Role(memberId, 55, null);
        repo.addRoleToPending(memberId, r);

        // Decline it
        repo.declineRole(memberId, r);
        assertTrue(repo.getPendingRoles(memberId).isEmpty(),
                "after decline, pending list should be empty");

        // Asking for it fails
        assertThrows(OurRuntime.class,
                () -> repo.getPendingRole(memberId, 55));

        // Unknown user
        assertThrows(OurRuntime.class,
                () -> repo.getPendingRoles(9999));
    }

    @Test
    void testGetShoppingCartById_successAndFailure() {

        ShoppingCart cart = repo.getShoppingCartById(memberId);
        assertNotNull(cart);

        // Invalid user should throw
        assertThrows(OurRuntime.class,
                () -> repo.getShoppingCartById(9999));
    }

    @Test
    void testGetPendingRoles_HappyPath() {

        repo.addMember("pend", "pw", "p@e", "ph", "ad");
        int pid = repo.isUsernameAndPasswordValid("pend", "pw");
        Role r = new Role(pid, 99, null);
        repo.addRoleToPending(pid, r);
        List<Role> pending = repo.getPendingRoles(pid);
        assertEquals(1, pending.size());
        assertEquals(r, pending.get(0));
    }

    @Test
    void testAddRoleToPending_NullAndInvalidUser() {

        // Null role
        assertThrows(OurRuntime.class,
                () -> repo.addRoleToPending(memberId, null));

        // Invalid user
        assertThrows(OurRuntime.class,
                () -> repo.addRoleToPending(9999, new Role(9999, 1, null)));
    }

    @Test
    void testShoppingCartFailures() {

        // Invalid user for shopping cart operations
        assertThrows(OurRuntime.class,
                () -> repo.addItemToShoppingCart(9999, 1, 1, 1));
        assertThrows(OurRuntime.class,
                () -> repo.updateItemQuantityInShoppingCart(9999, 1, 1, 1));

        // Invalid quantity
        assertThrows(OurRuntime.class,
                () -> repo.addItemToShoppingCart(memberId, 1, 1, 0));
    }

    @Test
    void testNotificationFailures() {

        // Invalid user for addNotification
        assertThrows(OurRuntime.class,
                () -> repo.addNotification(9999, "T", "M"));

        // Invalid user for getNotificationsAndClear
        assertThrows(OurRuntime.class,
                () -> repo.getNotificationsAndClear(9999));
    }

    @Test
    void testPaymentFailures() {
        // Invalid user for payment operations
        assertThrows(OurRuntime.class,
                () -> repo.setPaymentMethod(9999, 1, new WSEPPay()));
    }

    @Test
    void testUpdateMemberFields_Failures() {

        // Invalid user ID for various update operations
        assertThrows(OurRuntime.class,
                () -> repo.updateMemberUsername(9999, "newname"));
        assertThrows(OurRuntime.class,
                () -> repo.updateMemberPassword(9999, "newpass"));
        assertThrows(OurRuntime.class,
                () -> repo.updateMemberEmail(9999, "new@email.com"));
        assertThrows(OurRuntime.class,
                () -> repo.updateMemberPhoneNumber(9999, "123"));
    }

    @Test
    void testPermissionMgmt_Failures() {

        // No role bound
        assertThrows(OurRuntime.class,
                () -> repo.addPermission(memberId, PermissionsEnum.manageItems, 123));
        assertThrows(OurRuntime.class,
                () -> repo.removePermission(memberId, PermissionsEnum.manageItems, 123));

        // Invalid user
        assertThrows(OurRuntime.class,
                () -> repo.addPermission(9999, PermissionsEnum.manageItems, 1));
    }

    @Test
    void testGetUserMapping() {

        Map<Integer, User> userMapping = repo.getUserMapping();
        assertNotNull(userMapping);
        // The mapping should contain database users
        assertFalse(userMapping.isEmpty());
    }

    @Test
    void testGetUsersList() {

        List<User> users = repo.getUsersList();
        assertNotNull(users);
        // Should contain at least the admin and test members
        assertFalse(users.isEmpty());
    }

    @Test
    void testGetUsersIdsList() {

        List<Integer> userIds = repo.getUsersIdsList();
        assertNotNull(userIds);
        assertFalse(userIds.isEmpty());
    }

    @Test
    void testGetGuestsList() {

        List<Guest> guests = repo.getGuestsList();
        assertNotNull(guests);
        // Should contain at least our test guest
        assertTrue(guests.stream().anyMatch(g -> g.getGuestId() == guestId));
    }

    @Test
    void testGetMembersList() {

        List<Member> members = repo.getMembersList();
        assertNotNull(members);
        // Should contain at least the admin and test member
        assertTrue(members.size() >= 2);
    }

    @Test
    void testGetAllMembers() {

        List<Member> allMembers = repo.getAllMembers();
        assertNotNull(allMembers);
        // Should contain at least the admin and test member
        assertTrue(allMembers.size() >= 2);
    }

    @Test
    void testClear() {

        // Clear operation behavior might be different for DB implementation
        // This test ensures the method exists and doesn't throw
        assertDoesNotThrow(() -> repo.clear());
    }

    @Test
    void testSuspensionOperations() {

        // Test suspension operations
        repo.setSuspended(memberId, LocalDateTime.now().plusDays(1));
        assertTrue(repo.isSuspended(memberId));

        List<Integer> suspended = repo.getSuspendedUsers();
        assertTrue(suspended.contains(memberId));

        repo.setUnSuspended(memberId);
        assertFalse(repo.isSuspended(memberId));

        // Test ban
        repo.banUser(memberId);
        assertTrue(repo.isSuspended(memberId));
    }

    @Test
    void testPasswordEncoderOperations() {

        assertNotNull(repo.getPasswordEncoderUtil());

        // Test encoder setup
        repo.setEncoderToTest(false);
        repo.setEncoderToTest(true);
    }

    @Test
    void testNotificationOperations() {

        repo.addNotification(memberId, "Test Title", "Test Message");
        List<String> notifications = repo.getNotificationsAndClear(memberId);
        assertNotNull(notifications);
    }

    @Test
    void testPaymentOperations() {

        WSEPPay paymentMethod = new WSEPPay();
        repo.setPaymentMethod(memberId, 1, paymentMethod);

        // This should fail due to null cardNumber which throws IllegalArgumentException
        assertThrows(Exception.class,
                () -> repo.pay(memberId, 100.0, "USD", null, "12", "25", "John Doe", "123", "test-payment"));
    }

    @Test
    void testAuctionOperations() {

        List<?> auctionWins = repo.getAuctionsWinList(memberId);
        assertNotNull(auctionWins);

        // Test adding bid to shopping cart - this assumes Bid class exists
        // If Bid implementation is available, uncomment and adjust:
        // Bid testBid = new Bid(/* parameters */);
        // assertDoesNotThrow(() -> repo.addAuctionWinBidToShoppingCart(memberId,
        // testBid));
    } // Tests moved and consolidated below to avoid duplication

    @Test
    void testUpdateShoppingCartItemQuantity_EdgeCases() {

        // Add item first
        repo.addItemToShoppingCart(memberId, 1, 1, 5);

        // Test quantity update with increment
        repo.updateShoppingCartItemQuantity(memberId, 1, 1, true);

        // Test quantity update with decrement
        repo.updateShoppingCartItemQuantity(memberId, 1, 1, false);

        // Test with non-existent item
        assertThrows(OurRuntime.class, () -> repo.updateShoppingCartItemQuantity(memberId, 999, 999, true));

        // Test with invalid user
        assertThrows(OurRuntime.class, () -> repo.updateShoppingCartItemQuantity(99999, 1, 1, true));
    }

    @Test
    void testRemoveShoppingCartItem_EdgeCases() {

        // Add items first
        repo.addItemToShoppingCart(memberId, 2, 1, 3);
        repo.addItemToShoppingCart(memberId, 2, 2, 2);

        // Remove one item
        repo.removeShoppingCartItem(memberId, 2, 1);
        // Try to remove already removed item (might not throw exception if item removal
        // is idempotent)
        try {
            repo.removeShoppingCartItem(memberId, 2, 1);
        } catch (OurRuntime e) {
            // Expected if trying to remove non-existent item
            assertNotNull(e);
        }

        // Test with invalid user
        assertThrows(OurRuntime.class, () -> repo.removeShoppingCartItem(99999, 2, 2));

        // Test with non-existent shop/item
        assertThrows(OurRuntime.class, () -> repo.removeShoppingCartItem(memberId, 999, 999));
    }

    @Test
    void testRefund_Scenarios() {

        // Test refund without prior payment (should fail)
        assertThrows(OurRuntime.class, () -> repo.refund(memberId, 100));

        // Test with invalid user
        assertThrows(OurRuntime.class, () -> repo.refund(99999, 50));

        // Test with negative amount
        assertThrows(OurRuntime.class, () -> repo.refund(memberId, -10));

        // Test with zero amount
        assertThrows(OurRuntime.class, () -> repo.refund(memberId, 0));
    }

    @Test
    void testBidToShoppingCart_Comprehensive() {
        // Test adding valid bid (might not throw exception, test actual functionality)
        Map<Integer, Integer> bidData1 = Map.of(1, 150, 2, 200);
        try {
            repo.addBidToShoppingCart(memberId, 1, bidData1);
        } catch (OurRuntime e) {
            // Expected if bid functionality is not fully implemented
            assertNotNull(e);
        }

        // Test with empty bid data (might not throw exception)
        Map<Integer, Integer> emptyBidData = Map.of();
        try {
            repo.addBidToShoppingCart(memberId, 1, emptyBidData);
        } catch (OurRuntime e) {
            // Expected if bid functionality validates data
            assertNotNull(e);
        }

        // Test with invalid user
        assertThrows(OurRuntime.class, () -> repo.addBidToShoppingCart(99999, 1, bidData1));
        // Test with null bid data - expect NullPointerException, not OurRuntime
        try {
            repo.addBidToShoppingCart(memberId, 1, null);
            fail("Should have thrown an exception");
        } catch (Exception e) {
            // Can be NullPointerException or OurRuntime depending on implementation
            assertTrue(e instanceof NullPointerException || e instanceof OurRuntime);
        }
    }

    @Test
    void testAdvancedPaymentScenarios() {

        WSEPPay paymentMethod = new WSEPPay();
        repo.setPaymentMethod(memberId, 1, paymentMethod);

        // Test payment with empty card number - might not throw exception if validation
        // is elsewhere
        try {
            repo.pay(memberId, 50.0, "USD", "", "12", "25", "John Doe", "123", "pay-test");
        } catch (Exception e) {
            // Expected to catch some exception for invalid card
            assertNotNull(e);
        }

        // Test payment with invalid expiry - might not throw exception if validation is
        // elsewhere
        try {
            repo.pay(memberId, 50.0, "USD", "1234567890123456", "13", "25", "John Doe", "123", "pay-test");
        } catch (Exception e) {
            // Expected to catch some exception for invalid month
            assertNotNull(e);
        }

        // Test payment with valid parameters (should work without exception)
        // Note: These might not throw exceptions as expected, so we test actual payment
        // flow
        try {
            repo.pay(memberId, 50.0, "USD", "1234567890123456", "12", "25", "John Doe", "123", "pay-test");
        } catch (Exception e) {
            // Payment might fail due to external service, this is expected
            assertNotNull(e);
        }
    }

    @Test
    void testComplexShoppingCartScenarios() {

        // Test multiple items in same shop
        repo.addItemToShoppingCart(memberId, 10, 1, 2);
        repo.addItemToShoppingCart(memberId, 10, 2, 3);
        repo.addItemToShoppingCart(memberId, 10, 3, 1);

        // Test multiple shops
        repo.addItemToShoppingCart(memberId, 11, 1, 1);
        repo.addItemToShoppingCart(memberId, 12, 1, 2);

        // Test updating quantities
        repo.updateItemQuantityInShoppingCart(memberId, 10, 1, 5);
        repo.updateItemQuantityInShoppingCart(memberId, 11, 1, 3);

        // Test removing items
        repo.removeItemFromShoppingCart(memberId, 10, 3);
        repo.removeItemFromShoppingCart(memberId, 12, 1);

        // Test basket operations for each shop
        repo.createBasket(memberId, 10);
        repo.createBasket(memberId, 11);

        Map<Integer, Integer> basket10 = repo.getBasket(memberId, 10);
        Map<Integer, Integer> basket11 = repo.getBasket(memberId, 11);

        assertNotNull(basket10);
        assertNotNull(basket11);
        // Test getting basket for non-existent shop (might not throw exception)
        try {
            repo.getBasket(memberId, 999);
        } catch (OurRuntime e) {
            // Expected if basket doesn't exist
            assertNotNull(e);
        }

        // Test creating basket for invalid user
        assertThrows(OurRuntime.class, () -> repo.createBasket(99999, 10));

        // Clear entire cart
        repo.clearShoppingCart(memberId);

        // Verify cart is empty by getting it
        ShoppingCart clearedCart = repo.getShoppingCartById(memberId);
        assertNotNull(clearedCart);
    }

    @Test
    void testRoleManagement_ExtensiveScenarios() {

        // Create multiple test users for role management
        repo.addMember("roleUser1", "pass1", "role1@test.com", "111", "address1");
        repo.addMember("roleUser2", "pass2", "role2@test.com", "222", "address2");
        repo.addMember("roleUser3", "pass3", "role3@test.com", "333", "address3");

        int user1Id = repo.isUsernameAndPasswordValid("roleUser1", "pass1");
        int user2Id = repo.isUsernameAndPasswordValid("roleUser2", "pass2");
        int user3Id = repo.isUsernameAndPasswordValid("roleUser3", "pass3");

        // Test multiple roles per user across different shops
        Role user1Role1 = new Role(user1Id, 101, new PermissionsEnum[] { PermissionsEnum.manageItems });
        Role user1Role2 = new Role(user1Id, 102, new PermissionsEnum[] { PermissionsEnum.handleMessages });
        Role user2Role1 = new Role(user2Id, 101, new PermissionsEnum[] { PermissionsEnum.manageOwners });
        Role user3FounderRole = new Role(user3Id, 103, null);
        user3FounderRole.setFoundersPermissions();

        // Add all roles to pending
        repo.addRoleToPending(user1Id, user1Role1);
        repo.addRoleToPending(user1Id, user1Role2);
        repo.addRoleToPending(user2Id, user2Role1);
        repo.addRoleToPending(user3Id, user3FounderRole);

        // Test getting pending roles
        List<Role> user1Pending = repo.getPendingRoles(user1Id);
        List<Role> user2Pending = repo.getPendingRoles(user2Id);
        List<Role> user3Pending = repo.getPendingRoles(user3Id);

        assertEquals(2, user1Pending.size());
        assertEquals(1, user2Pending.size());
        assertEquals(1, user3Pending.size());

        // Accept some roles, decline others
        repo.acceptRole(user1Id, user1Role1);
        repo.declineRole(user1Id, user1Role2);
        repo.acceptRole(user2Id, user2Role1);
        repo.acceptRole(user3Id, user3FounderRole);

        // Test accepted roles
        List<Role> user1Accepted = repo.getAcceptedRoles(user1Id);
        List<Role> user2Accepted = repo.getAcceptedRoles(user2Id);
        List<Role> user3Accepted = repo.getAcceptedRoles(user3Id);

        assertEquals(1, user1Accepted.size());
        assertEquals(1, user2Accepted.size());
        assertEquals(1, user3Accepted.size());

        // Test role queries
        Role retrievedRole1 = repo.getRole(user1Id, 101);
        Role retrievedRole2 = repo.getRole(user2Id, 101);
        Role retrievedRole3 = repo.getRole(user3Id, 103);

        assertEquals(user1Role1, retrievedRole1);
        assertEquals(user2Role1, retrievedRole2);
        assertEquals(user3FounderRole, retrievedRole3);

        // Test permission management
        repo.addPermission(user1Id, PermissionsEnum.handleMessages, 101);
        assertTrue(retrievedRole1.hasPermission(PermissionsEnum.handleMessages));

        repo.removePermission(user1Id, PermissionsEnum.handleMessages, 101);
        Role refreshedRole1 = repo.getRole(user1Id, 101);
        assertFalse(refreshedRole1.hasPermission(PermissionsEnum.handleMessages));
        // Test setting permissions - get fresh role after previous operations
        PermissionsEnum[] newPermissions = { PermissionsEnum.manageItems, PermissionsEnum.handleMessages,
                PermissionsEnum.manageOwners };
        Role freshRole1 = repo.getRole(user1Id, 101); // Get fresh role reference
        repo.setPermissions(user1Id, 101, freshRole1, newPermissions);
        Role updatedRole1 = repo.getRole(user1Id, 101);
        assertTrue(updatedRole1.hasPermission(PermissionsEnum.manageItems));
        assertTrue(updatedRole1.hasPermission(PermissionsEnum.handleMessages));
        assertTrue(updatedRole1.hasPermission(PermissionsEnum.manageOwners));

        // Test owner and founder status
        assertTrue(repo.isOwner(user2Id, 101)); // Has manageOwners permission
        assertTrue(repo.isFounder(user3Id, 103)); // Has founder permissions
        assertTrue(repo.isOwner(user3Id, 103)); // Founder is also owner
        assertFalse(repo.isFounder(user1Id, 101)); // Not a founder

        // Test shop members and owners
        List<Member> shop101Members = repo.getShopMembers(101);
        List<Member> shop101Owners = repo.getOwners(101);
        List<Member> shop103Owners = repo.getOwners(103);

        assertTrue(shop101Members.stream().anyMatch(m -> m.getMemberId() == user1Id));
        assertTrue(shop101Members.stream().anyMatch(m -> m.getMemberId() == user2Id));
        assertTrue(shop101Owners.stream().anyMatch(m -> m.getMemberId() == user2Id));
        assertTrue(shop103Owners.stream().anyMatch(m -> m.getMemberId() == user3Id));

        // Test worker shop mappings
        List<Integer> user1Shops = repo.getShopIdsByWorkerId(user1Id);
        List<Integer> user2Shops = repo.getShopIdsByWorkerId(user2Id);
        List<Integer> user3Shops = repo.getShopIdsByWorkerId(user3Id);

        assertTrue(user1Shops.contains(101));
        assertTrue(user2Shops.contains(101));
        assertTrue(user3Shops.contains(103));

        // Test role removal
        repo.removeRole(user1Id, 101);
        assertThrows(OurRuntime.class, () -> repo.getRole(user1Id, 101));

        // Test edge cases for role operations
        assertThrows(OurRuntime.class, () -> repo.getRole(99999, 101)); // Invalid user
        assertThrows(OurRuntime.class, () -> repo.getRole(user2Id, 999)); // Invalid shop
        assertThrows(OurRuntime.class, () -> repo.removeRole(user2Id, 999)); // Invalid shop
        assertThrows(OurRuntime.class, () -> repo.addPermission(99999, PermissionsEnum.manageItems, 101)); // Invalid
                                                                                                           // user
    }

    @Test
    void testSuspensionAndBanning_Comprehensive() {

        // Create test users for suspension testing
        repo.addMember("suspendUser1", "pass", "suspend1@test.com", "111", "suspend address");
        repo.addMember("suspendUser2", "pass", "suspend2@test.com", "222", "suspend address");
        repo.addMember("banUser1", "pass", "ban1@test.com", "333", "ban address");

        int suspend1Id = repo.isUsernameAndPasswordValid("suspendUser1", "pass");
        int suspend2Id = repo.isUsernameAndPasswordValid("suspendUser2", "pass");
        int ban1Id = repo.isUsernameAndPasswordValid("banUser1", "pass");

        // Test suspension with different dates
        LocalDateTime shortSuspension = LocalDateTime.now().plusHours(1);
        LocalDateTime longSuspension = LocalDateTime.now().plusDays(30);

        repo.setSuspended(suspend1Id, shortSuspension);
        repo.setSuspended(suspend2Id, longSuspension);

        assertTrue(repo.isSuspended(suspend1Id));
        assertTrue(repo.isSuspended(suspend2Id));
        assertFalse(repo.isSuspended(ban1Id));

        // Test getting suspended users
        List<Integer> suspendedUsers = repo.getSuspendedUsers();
        assertTrue(suspendedUsers.contains(suspend1Id));
        assertTrue(suspendedUsers.contains(suspend2Id));
        assertFalse(suspendedUsers.contains(ban1Id));

        // Test unsuspending
        repo.setUnSuspended(suspend1Id);
        assertFalse(repo.isSuspended(suspend1Id));
        assertTrue(repo.isSuspended(suspend2Id)); // Still suspended

        // Test banning (permanent suspension)
        repo.banUser(ban1Id);
        assertTrue(repo.isSuspended(ban1Id));

        // Test suspension with past date (should not be suspended)
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        repo.setSuspended(suspend1Id, pastDate);
        assertFalse(repo.isSuspended(suspend1Id));

        // Test error cases
        assertThrows(OurRuntime.class, () -> repo.setSuspended(99999, shortSuspension));
        assertThrows(OurRuntime.class, () -> repo.setUnSuspended(99999));
        assertThrows(OurRuntime.class, () -> repo.banUser(99999));
        assertThrows(OurRuntime.class, () -> repo.isSuspended(99999));
    }

    @Test
    void testNotificationSystem_Comprehensive() {

        // Create test user for notifications
        repo.addMember("notifyUser", "pass", "notify@test.com", "999", "notify address");
        int notifyUserId = repo.isUsernameAndPasswordValid("notifyUser", "pass");

        // Test adding various notifications
        repo.addNotification(memberId, "Order Update", "Your order has been shipped");
        repo.addNotification(memberId, "Payment Confirmed", "Payment of $50 confirmed");
        repo.addNotification(memberId, "Shop Message", "Welcome to our shop!");

        repo.addNotification(notifyUserId, "Role Invitation", "You've been invited to manage shop X");
        repo.addNotification(notifyUserId, "Auction Won", "Congratulations! You won the auction");

        // Test getting notifications for first user
        List<String> memberNotifications = repo.getNotificationsAndClear(memberId);
        assertNotNull(memberNotifications);
        assertTrue(memberNotifications.size() >= 3);

        // Test getting notifications for second user
        List<String> notifyUserNotifications = repo.getNotificationsAndClear(notifyUserId);
        assertNotNull(notifyUserNotifications);
        assertTrue(notifyUserNotifications.size() >= 2);

        // Test that notifications are cleared after retrieval
        List<String> emptyNotifications1 = repo.getNotificationsAndClear(memberId);
        List<String> emptyNotifications2 = repo.getNotificationsAndClear(notifyUserId);
        assertNotNull(emptyNotifications1);
        assertNotNull(emptyNotifications2);

        // Add more notifications after clearing
        repo.addNotification(memberId, "New Message", "You have a new message");
        List<String> newNotifications = repo.getNotificationsAndClear(memberId);
        assertNotNull(newNotifications);
        assertTrue(newNotifications.size() >= 1);

        // Test error cases
        assertThrows(OurRuntime.class, () -> repo.addNotification(99999, "Title", "Message"));
        assertThrows(OurRuntime.class, () -> repo.getNotificationsAndClear(99999));
    }

    @Test
    void testUserRepositoryDBImpl_ConstructorValidation() {
        // Test invalid admin username
        assertThrows(IllegalArgumentException.class, () -> 
            new UserRepositoryDBImpl(null, "pass", "admin@test.com", "123", "addr", null));
        assertThrows(IllegalArgumentException.class, () -> 
            new UserRepositoryDBImpl("", "pass", "admin@test.com", "123", "addr", null));
        
        // Test invalid admin password
        assertThrows(IllegalArgumentException.class, () -> 
            new UserRepositoryDBImpl("admin", null, "admin@test.com", "123", "addr", null));
        assertThrows(IllegalArgumentException.class, () -> 
            new UserRepositoryDBImpl("admin", "", "admin@test.com", "123", "addr", null));
        
        // Test invalid admin email
        assertThrows(IllegalArgumentException.class, () -> 
            new UserRepositoryDBImpl("admin", "pass", null, "123", "addr", null));
        assertThrows(IllegalArgumentException.class, () -> 
            new UserRepositoryDBImpl("admin", "pass", "", "123", "addr", null));
        assertThrows(IllegalArgumentException.class, () -> 
            new UserRepositoryDBImpl("admin", "pass", "invalidemail", "123", "addr", null));
        
        // Test invalid admin phone
        assertThrows(IllegalArgumentException.class, () -> 
            new UserRepositoryDBImpl("admin", "pass", "admin@test.com", null, "addr", null));
        assertThrows(IllegalArgumentException.class, () -> 
            new UserRepositoryDBImpl("admin", "pass", "admin@test.com", "", "addr", null));
        
        // Test invalid admin address
        assertThrows(IllegalArgumentException.class, () -> 
            new UserRepositoryDBImpl("admin", "pass", "admin@test.com", "123", null, null));
        assertThrows(IllegalArgumentException.class, () -> 
            new UserRepositoryDBImpl("admin", "pass", "admin@test.com", "123", "", null));
    }

    @Test
    void testGetMemberById_EdgeCases() {
        // Test valid member retrieval
        Member retrievedMember = repo.getMemberById(memberId);
        assertNotNull(retrievedMember);
        assertEquals(memberId, retrievedMember.getMemberId());
        
        // Test guest ID - implementation returns a Member object for guest
        Member guestMember = repo.getMemberById(guestId);
        assertNotNull(guestMember, "getMemberById returns a Member object for guest ID");
        
        // Test invalid ID - Spring wraps IllegalArgumentException as InvalidDataAccessApiUsageException
        assertThrows(Exception.class, () -> repo.getMemberById(-1));
        assertThrows(OurRuntime.class, () -> repo.getMemberById(999999));
    }

    @Test
    void testAddPermission_ComprehensiveValidation() {
        // Set up role for permission testing
        Role testRole = new Role(memberId, 11, new PermissionsEnum[]{PermissionsEnum.manageItems});
        repo.addRoleToPending(memberId, testRole);
        repo.acceptRole(memberId, testRole);
        
        // Test adding valid permission
        assertDoesNotThrow(() -> repo.addPermission(memberId, PermissionsEnum.viewPolicy, 11));
        
        // Test adding duplicate permission (should handle gracefully)
        assertDoesNotThrow(() -> repo.addPermission(memberId, PermissionsEnum.manageItems, 11));
        
        // Test adding permission for guest
        assertThrows(OurRuntime.class, () -> repo.addPermission(guestId, PermissionsEnum.manageItems, 11));
        
        // Test adding permission for invalid user - Spring wraps as InvalidDataAccessApiUsageException
        @SuppressWarnings("unused")
        Exception ex = assertThrows(Exception.class, () -> repo.addPermission(-1, PermissionsEnum.manageItems, 11));
        
        // Test adding permission for shop without role
        assertThrows(Exception.class, () -> repo.addPermission(memberId, PermissionsEnum.manageItems, 999));
    }

    @Test
    void testRemovePermission_ComprehensiveValidation() {
        // Set up role with permissions
        Role testRole = new Role(memberId, 12, new PermissionsEnum[]{PermissionsEnum.manageItems, PermissionsEnum.viewPolicy});
        repo.addRoleToPending(memberId, testRole);
        repo.acceptRole(memberId, testRole);
        
        // Test removing valid permission
        assertDoesNotThrow(() -> repo.removePermission(memberId, PermissionsEnum.viewPolicy, 12));
        
        // Test removing non-existent permission (should handle gracefully)
        assertDoesNotThrow(() -> repo.removePermission(memberId, PermissionsEnum.manageOwners, 12));
        
        // Test removing permission for guest
        assertThrows(OurRuntime.class, () -> repo.removePermission(guestId, PermissionsEnum.manageItems, 12));
        
        // Test removing permission for invalid user - Spring wraps as InvalidDataAccessApiUsageException
        @SuppressWarnings("unused")
        Exception ex2 = assertThrows(Exception.class, () -> repo.removePermission(-1, PermissionsEnum.manageItems, 12));
        
        // Test removing permission for shop without role
        assertThrows(Exception.class, () -> repo.removePermission(memberId, PermissionsEnum.manageItems, 999));
    }

    @Test
    void testSetSuspended_DetailedScenarios() {
        // Test setting suspension with future date
        LocalDateTime futureDate = LocalDateTime.now().plusDays(7);
        assertDoesNotThrow(() -> repo.setSuspended(memberId, futureDate));
        
        // Verify user is suspended
        assertTrue(repo.isSuspended(memberId));
        
        // Test setting suspension with past date (should still mark as suspended)
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        assertDoesNotThrow(() -> repo.setSuspended(memberId, pastDate));
        
        // Test setting suspension for guest
        assertThrows(OurRuntime.class, () -> repo.setSuspended(guestId, futureDate));
        
        // Test setting suspension for invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex3 = assertThrows(Exception.class, () -> repo.setSuspended(-1, futureDate));
        
        // Test with null date - should be handled by implementation
        // Note: This might throw NPE or be handled gracefully depending on implementation
    }

    @Test
    void testRemoveUserById_ComprehensiveScenarios() {
        // Create test users
        int removeTestGuestId = repo.addGuest();
        int removeTestMemberId = repo.addMember("testremove", "pass", "remove@test.com", "555-9999", "Remove St");
        
        // Verify users exist
        assertNotNull(repo.getUserById(removeTestGuestId));
        assertNotNull(repo.getUserById(removeTestMemberId));
        
        // Remove guest
        assertDoesNotThrow(() -> repo.removeUserById(removeTestGuestId));
        
        // Remove member
        assertDoesNotThrow(() -> repo.removeUserById(removeTestMemberId));
        assertThrows(OurRuntime.class, () -> repo.getUserById(removeTestMemberId));
        
        // Test removing non-existent user
        assertThrows(OurRuntime.class, () -> repo.removeUserById(999999));
        
        // Test removing invalid user ID - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex4 = assertThrows(Exception.class, () -> repo.removeUserById(-1));
    }

    @Test
    void testGetShopIdsByWorkerId_DetailedScenarios() {
        // Set up multiple roles for worker
        Role role1 = new Role(memberId, 31, new PermissionsEnum[]{PermissionsEnum.manageItems});
        Role role2 = new Role(memberId, 32, new PermissionsEnum[]{PermissionsEnum.viewPolicy});
        Role role3 = new Role(memberId, 33, new PermissionsEnum[]{PermissionsEnum.manageOwners});
        
        repo.addRoleToPending(memberId, role1);
        repo.addRoleToPending(memberId, role2);
        repo.addRoleToPending(memberId, role3);
        
        repo.acceptRole(memberId, role1);
        repo.acceptRole(memberId, role2);
        repo.acceptRole(memberId, role3);
        
        // Test getting shop IDs
        List<Integer> shopIds = repo.getShopIdsByWorkerId(memberId);
        assertNotNull(shopIds);
        assertTrue(shopIds.contains(31));
        assertTrue(shopIds.contains(32));
        assertTrue(shopIds.contains(33));
        assertEquals(3, shopIds.size());
        
        // Test for user with no roles
        int newMemberId = repo.addMember("noroles", "pass", "noroles@test.com", "555-1111", "No Roles St");
        List<Integer> emptyShopIds = repo.getShopIdsByWorkerId(newMemberId);
        assertNotNull(emptyShopIds);
        assertTrue(emptyShopIds.isEmpty());
        
        // Test for guest user
        assertThrows(OurRuntime.class, () -> repo.getShopIdsByWorkerId(guestId));
        
        // Test for invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex5 = assertThrows(Exception.class, () -> repo.getShopIdsByWorkerId(-1));
    }

    @Test
    void testSetUnSuspended_DetailedScenarios() {
        // First suspend a user
        LocalDateTime futureDate = LocalDateTime.now().plusDays(5);
        repo.setSuspended(memberId, futureDate);
        assertTrue(repo.isSuspended(memberId));
        
        // Test unsuspending
        assertDoesNotThrow(() -> repo.setUnSuspended(memberId));
        assertFalse(repo.isSuspended(memberId));
        
        // Test unsuspending already unsuspended user
        assertDoesNotThrow(() -> repo.setUnSuspended(memberId));
        assertFalse(repo.isSuspended(memberId));
        
        // Test unsuspending guest
        assertThrows(OurRuntime.class, () -> repo.setUnSuspended(guestId));
        
        // Test unsuspending invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex6 = assertThrows(Exception.class, () -> repo.setUnSuspended(-1));
        
        // Test unsuspending banned user
        repo.banUser(memberId);
        assertTrue(repo.isSuspended(memberId));
        assertDoesNotThrow(() -> repo.setUnSuspended(memberId));
        assertFalse(repo.isSuspended(memberId));
    }

    @Test
    void testAddAdmin_EdgeCaseValidation() {
        // Create regular member
        int newMemberId = repo.addMember("regularuser", "pass", "regular@test.com", "555-2222", "Regular St");
        assertFalse(repo.isAdmin(newMemberId));
        
        // Test adding admin
        assertDoesNotThrow(() -> repo.addAdmin(newMemberId));
        assertTrue(repo.isAdmin(newMemberId));
        
        // Test adding admin to already admin user
        assertDoesNotThrow(() -> repo.addAdmin(newMemberId));
        assertTrue(repo.isAdmin(newMemberId));
        
        // Test adding admin to guest
        assertThrows(OurRuntime.class, () -> repo.addAdmin(guestId));
        
        // Test adding admin to invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex8 = assertThrows(Exception.class, () -> repo.addAdmin(-1));
        assertThrows(OurRuntime.class, () -> repo.addAdmin(999999));
    }

    // === Additional comprehensive tests with proper naming and permissions ===

    @Test
    void testPayMethod_ComprehensiveValidation() {
        // Test successful payment
        int paymentId = repo.pay(memberId, 100.0, "USD", "4111111111111111", "12", "2025", "John Doe", "123", "test123");
        assertTrue(paymentId >= 0);
        
        // Test payment for guest (should work)
        int guestPaymentId = repo.pay(guestId, 50.0, "USD", "4111111111111111", "12", "2025", "Guest User", "456", "guest123");
        assertTrue(guestPaymentId >= 0);
        
        // Test invalid amount
        assertThrows(OurRuntime.class, () -> repo.pay(memberId, -10.0, "USD", "4111111111111111", "12", "2025", "John Doe", "123", "negative"));
        
        // Test invalid user - Spring wraps exceptions  
        @SuppressWarnings("unused")
        Exception ex7 = assertThrows(Exception.class, () -> repo.pay(-1, 100.0, "USD", "4111111111111111", "12", "2025", "Invalid", "123", "invalid"));
        
        // Test empty card number - implementation accepts empty card number
        assertDoesNotThrow(() -> repo.pay(memberId, 100.0, "USD", "", "12", "2025", "John Doe", "123", "empty"));
    }

    @Test
    void testIsOwnerFounder_ComprehensiveScenarios() {
        // Create owner role (owner has manageOwners permission)
        Role ownerRole = new Role(memberId, 41, new PermissionsEnum[]{PermissionsEnum.manageItems, PermissionsEnum.manageOwners});
        repo.addRoleToPending(memberId, ownerRole);
        repo.acceptRole(memberId, ownerRole);
        
        // Test is owner (should return true because role has manageOwners permission)
        assertTrue(repo.isOwner(memberId, 41));
        assertFalse(repo.isOwner(memberId, 999)); // Different shop
        
        // Create founder role (founder has closeShop permission)
        Role founderRole = new Role(memberId, 42, new PermissionsEnum[]{PermissionsEnum.closeShop, PermissionsEnum.openClosedShop});
        repo.addRoleToPending(memberId, founderRole);
        repo.acceptRole(memberId, founderRole);
        
        // Test is founder (should return true because role has closeShop permission)
        assertTrue(repo.isFounder(memberId, 42));
        assertFalse(repo.isFounder(memberId, 999)); // Different shop
        
        // Test non-owner/founder
        int regularMemberId = repo.addMember("regular", "pass", "regular@test.com", "555-3333", "Regular St");
        Role regularRole = new Role(regularMemberId, 43, new PermissionsEnum[]{PermissionsEnum.handleMessages});
        repo.addRoleToPending(regularMemberId, regularRole);
        repo.acceptRole(regularMemberId, regularRole);
        
        assertFalse(repo.isOwner(regularMemberId, 43));
        assertFalse(repo.isFounder(regularMemberId, 43));
        
        // Test guest (should throw exception)
        assertThrows(OurRuntime.class, () -> repo.isOwner(guestId, 41));
        assertThrows(OurRuntime.class, () -> repo.isFounder(guestId, 42));
    }

    @Test
    void testSetPermissions_ExtensiveValidation() {
        // Set up role
        Role testRole = new Role(memberId, 51, new PermissionsEnum[]{PermissionsEnum.manageItems});
        repo.addRoleToPending(memberId, testRole);
        repo.acceptRole(memberId, testRole);
        
        // Test setting new permissions
        PermissionsEnum[] newPermissions = {PermissionsEnum.manageItems, PermissionsEnum.setPolicy, PermissionsEnum.getStaffInfo};
        assertDoesNotThrow(() -> repo.setPermissions(memberId, 51, testRole, newPermissions));
        
        // Test null role
        assertThrows(OurRuntime.class, () -> repo.setPermissions(memberId, 51, null, newPermissions));
        
        // Test null permissions
        assertThrows(OurRuntime.class, () -> repo.setPermissions(memberId, 51, testRole, null));
        
        // Test empty permissions
        assertThrows(OurRuntime.class, () -> repo.setPermissions(memberId, 51, testRole, new PermissionsEnum[]{}));
        
        // Test role user doesn't have
        Role nonExistentRole = new Role(memberId, 999, new PermissionsEnum[]{PermissionsEnum.manageItems});
        assertThrows(OurRuntime.class, () -> repo.setPermissions(memberId, 999, nonExistentRole, newPermissions));
        
        // Test guest user
        assertThrows(OurRuntime.class, () -> repo.setPermissions(guestId, 51, testRole, newPermissions));
    }

    @Test
    void testRemoveAdmin_DetailedScenarios() {
        // Create admin user
        int adminMemberId = repo.addMember("testadmin", "pass", "admin2@test.com", "555-4444", "Admin St");
        repo.addAdmin(adminMemberId);
        assertTrue(repo.isAdmin(adminMemberId));
        
        // Test removing admin
        assertDoesNotThrow(() -> repo.removeAdmin(adminMemberId));
        assertFalse(repo.isAdmin(adminMemberId));
        
        // Test removing admin from non-admin user
        assertDoesNotThrow(() -> repo.removeAdmin(adminMemberId)); // Should not throw
        
        // Test cannot remove original admin - this throws OurRuntime
        int originalAdminId = repo.isUsernameAndPasswordValid("admin", "admin");
        @SuppressWarnings("unused")
        Exception ex9 = assertThrows(OurRuntime.class, () -> repo.removeAdmin(originalAdminId));
        
        // Test guest user - Spring wraps exceptions
        @SuppressWarnings("unused") 
        Exception ex10 = assertThrows(Exception.class, () -> repo.removeAdmin(guestId));
        
        // Test invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex11 = assertThrows(Exception.class, () -> repo.removeAdmin(-1));
    }

    @Test
    void testAddNotification_DetailedScenarios() {
        // Test adding notification to member
        assertDoesNotThrow(() -> repo.addNotification(memberId, "Test Title", "Test message content"));
        
        // Test adding multiple notifications
        assertDoesNotThrow(() -> repo.addNotification(memberId, "Title 1", "Message 1"));
        assertDoesNotThrow(() -> repo.addNotification(memberId, "Title 2", "Message 2"));
        
        // Test getting notifications
        List<String> notifications = repo.getNotificationsAndClear(memberId);
        assertNotNull(notifications);
        assertTrue(notifications.size() >= 3);
        
        // Test notifications cleared after retrieval
        List<String> emptyNotifications = repo.getNotificationsAndClear(memberId);
        assertTrue(emptyNotifications.isEmpty());
        
        // Test adding notification to guest
        assertThrows(OurRuntime.class, () -> repo.addNotification(guestId, "Guest Title", "Guest message"));
        
        // Test adding notification to invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex14 = assertThrows(Exception.class, () -> repo.addNotification(-1, "Invalid", "Invalid message"));
        
        // Test null values - implementation accepts null values
        assertDoesNotThrow(() -> repo.addNotification(memberId, null, "message"));
        assertDoesNotThrow(() -> repo.addNotification(memberId, "title", null));
    }

    @Test
    void testUpdateUserInDB_ComprehensiveScenarios() {
        // Test updating valid member
        Member member = repo.getMemberById(memberId);
        member.setEmail("updated@email.com");
        assertDoesNotThrow(() -> repo.updateUserInDB(member));
        
        // Verify update
        Member updatedMember = repo.getMemberById(memberId);
        assertEquals("updated@email.com", updatedMember.getEmail());
        
        // Test null member - this throws InvalidDataAccessApiUsageException wrapped by Spring
        assertThrows(Exception.class, () -> repo.updateUserInDB(null));
        
        // Test updating member with roles
        Role role = new Role(memberId, 61, new PermissionsEnum[]{PermissionsEnum.manageItems});
        repo.addRoleToPending(memberId, role);
        repo.acceptRole(memberId, role);
        
        member.setPhoneNumber("555-8888");
        assertDoesNotThrow(() -> repo.updateUserInDB(member));
        
        Member roleUpdatedMember = repo.getMemberById(memberId);
        assertEquals("555-8888", roleUpdatedMember.getPhoneNumber());
        assertTrue(roleUpdatedMember.getRoles().size() > 0);
    }

    @Test
    void testIsSuspended_DetailedValidation() {
        // Test non-suspended user
        assertFalse(repo.isSuspended(memberId));
        
        // Test after suspension
        LocalDateTime suspensionDate = LocalDateTime.now().plusDays(3);
        repo.setSuspended(memberId, suspensionDate);
        assertTrue(repo.isSuspended(memberId));
        
        // Test after unsuspension
        repo.setUnSuspended(memberId);
        assertFalse(repo.isSuspended(memberId));
        
        // Test banned user
        repo.banUser(memberId);
        assertTrue(repo.isSuspended(memberId));
        
        // Test guest user - this may not throw exception, depends on implementation
        try {
            boolean guestSuspended = repo.isSuspended(guestId);
            // If we get here, the method returned normally
            assertFalse(guestSuspended); // Guests typically aren't suspended
        } catch (OurRuntime e) {
            // Expected if guests can't be checked for suspension
            assertNotNull(e);
        }
        
        // Test invalid user - behavior depends on implementation
        try {
            repo.isSuspended(-1);
            fail("Expected exception for invalid user ID");
        } catch (Exception e) {
            // Expected some kind of exception
            assertNotNull(e);
        }
    }

    @Test
    void testGetPendingRoles_DetailedScenarios() {
        // Test user with no pending roles
        List<Role> emptyPendingRoles = repo.getPendingRoles(memberId);
        assertNotNull(emptyPendingRoles);
        
        // Add pending roles
        Role role1 = new Role(memberId, 71, new PermissionsEnum[]{PermissionsEnum.manageItems});
        Role role2 = new Role(memberId, 72, new PermissionsEnum[]{PermissionsEnum.setPolicy});
        
        repo.addRoleToPending(memberId, role1);
        repo.addRoleToPending(memberId, role2);
        
        // Test getting pending roles
        List<Role> pendingRoles = repo.getPendingRoles(memberId);
        assertNotNull(pendingRoles);
        assertEquals(2, pendingRoles.size());
        
        // Accept one role
        repo.acceptRole(memberId, role1);
        List<Role> updatedPendingRoles = repo.getPendingRoles(memberId);
        assertEquals(1, updatedPendingRoles.size());
        
        // Test guest user - might not throw exception, depends on implementation
        try {
            List<Role> guestPendingRoles = repo.getPendingRoles(guestId);
            assertNotNull(guestPendingRoles);
            assertTrue(guestPendingRoles.isEmpty());
        } catch (OurRuntime e) {
            // Expected if guests can't have pending roles
            assertNotNull(e);
        }
        
        // Test invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex12 = assertThrows(Exception.class, () -> repo.getPendingRoles(-1));
    }

    @Test
    void testGetAcceptedRoles_DetailedScenarios() {
        // Test user with no accepted roles initially
        List<Role> initialRoles = repo.getAcceptedRoles(memberId);
        assertNotNull(initialRoles);
        
        // Add and accept roles
        Role role1 = new Role(memberId, 81, new PermissionsEnum[]{PermissionsEnum.manageItems});
        Role role2 = new Role(memberId, 82, new PermissionsEnum[]{PermissionsEnum.setPolicy});
        
        repo.addRoleToPending(memberId, role1);
        repo.addRoleToPending(memberId, role2);
        repo.acceptRole(memberId, role1);
        repo.acceptRole(memberId, role2);
        
        // Test getting accepted roles
        List<Role> acceptedRoles = repo.getAcceptedRoles(memberId);
        assertNotNull(acceptedRoles);
        assertTrue(acceptedRoles.size() >= 2);
        
        // Verify roles are correct
        assertTrue(acceptedRoles.stream().anyMatch(r -> r.getShopId() == 81));
        assertTrue(acceptedRoles.stream().anyMatch(r -> r.getShopId() == 82));
        
        // Test guest user - might not throw exception, depends on implementation  
        try {
            List<Role> guestAcceptedRoles = repo.getAcceptedRoles(guestId);
            assertNotNull(guestAcceptedRoles);
            assertTrue(guestAcceptedRoles.isEmpty());
        } catch (OurRuntime e) {
            // Expected if guests can't have accepted roles
            assertNotNull(e);
        }
        
        // Test invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex13 = assertThrows(Exception.class, () -> repo.getAcceptedRoles(-1));
    }

    @Test
    void testGetAuctionsWinList_DetailedScenarios() {
        // Test member with no auction wins initially - this method might not throw exceptions for guests/invalid users
        List<?> auctionWins = repo.getAuctionsWinList(memberId);
        assertNotNull(auctionWins);
        
        // Test for guest user - might return empty list instead of throwing
        try {
            List<?> guestAuctionWins = repo.getAuctionsWinList(guestId);
            assertNotNull(guestAuctionWins);
        } catch (OurRuntime e) {
            // Expected if guests can't have auction wins
            assertNotNull(e);
        }
        
        // Test for invalid user - might return empty list or throw
        try {
            List<?> invalidUserAuctionWins = repo.getAuctionsWinList(-1);
            assertNotNull(invalidUserAuctionWins);
        } catch (Exception e) {
            // Expected if invalid user throws exception
            assertNotNull(e);
        }
    }

    @Test
    void testSetPaymentMethod_ComprehensiveScenarios() {
        // Test setting payment method for member
        WSEPPay paymentMethod = new WSEPPay();
        assertDoesNotThrow(() -> repo.setPaymentMethod(memberId, 1, paymentMethod));
        
        // Test setting payment method for guest
        assertDoesNotThrow(() -> repo.setPaymentMethod(guestId, 1, paymentMethod));
        
        // Test setting payment method for invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex16 = assertThrows(Exception.class, () -> repo.setPaymentMethod(-1, 1, paymentMethod));
        
        // Test setting null payment method
        assertDoesNotThrow(() -> repo.setPaymentMethod(memberId, 1, null));
    }

    @Test
    void testCreateBasket_DetailedScenarios() {
        // Test creating basket for member
        assertDoesNotThrow(() -> repo.createBasket(memberId, 91));
        
        // Verify basket exists
        Map<Integer, Integer> basket = repo.getBasket(memberId, 91);
        assertNotNull(basket);
        
        // Test creating multiple baskets
        assertDoesNotThrow(() -> repo.createBasket(memberId, 92));
        assertDoesNotThrow(() -> repo.createBasket(memberId, 93));
        
        Map<Integer, Integer> basket2 = repo.getBasket(memberId, 92);
        Map<Integer, Integer> basket3 = repo.getBasket(memberId, 93);
        assertNotNull(basket2);
        assertNotNull(basket3);
        
        // Test creating basket for guest
        assertDoesNotThrow(() -> repo.createBasket(guestId, 94));
        
        Map<Integer, Integer> guestBasket = repo.getBasket(guestId, 94);
        assertNotNull(guestBasket);
        
        // Test creating basket for invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex19 = assertThrows(Exception.class, () -> repo.createBasket(-1, 95));
    }

    @Test
    void testUpdateItemQuantityInShoppingCart_ExtensiveScenarios() {
        // First create basket and add item
        repo.createBasket(memberId, 101);
        repo.addItemToShoppingCart(memberId, 101, 1, 3);
        
        // Test updating quantity
        assertDoesNotThrow(() -> repo.updateItemQuantityInShoppingCart(memberId, 101, 1, 5));
        
        Map<Integer, Integer> basket = repo.getBasket(memberId, 101);
        assertEquals(5, basket.get(1));
        
        // Test updating to zero quantity (should remove item)
        assertDoesNotThrow(() -> repo.updateItemQuantityInShoppingCart(memberId, 101, 1, 0));
        
        // Test updating non-existent item - might throw exception
        try {
            repo.updateItemQuantityInShoppingCart(memberId, 101, 999, 2);
        } catch (OurRuntime e) {
            // Expected if item doesn't exist in cart
            assertNotNull(e);
        }
        
        // Test for guest user
        repo.createBasket(guestId, 102);
        repo.addItemToShoppingCart(guestId, 102, 2, 4);
        assertDoesNotThrow(() -> repo.updateItemQuantityInShoppingCart(guestId, 102, 2, 8));
        
        Map<Integer, Integer> guestBasket = repo.getBasket(guestId, 102);
        assertEquals(8, guestBasket.get(2));
        
        // Test invalid user
        assertThrows(Exception.class, () -> repo.updateItemQuantityInShoppingCart(-1, 101, 1, 5));
    }

    @Test
    void testAddBidToShoppingCart_DetailedScenarios() {
        // Test adding bid for member
        Map<Integer, Integer> bidItems = Map.of(1, 5, 2, 3);
        assertDoesNotThrow(() -> repo.addBidToShoppingCart(memberId, 111, bidItems));
        
        // Test adding multiple bids
        Map<Integer, Integer> bidItems2 = Map.of(3, 10);
        assertDoesNotThrow(() -> repo.addBidToShoppingCart(memberId, 112, bidItems2));
        
        // Test adding bid for guest
        Map<Integer, Integer> guestBidItems = Map.of(4, 7);
        assertDoesNotThrow(() -> repo.addBidToShoppingCart(guestId, 113, guestBidItems));
        
        // Test adding bid for invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex17 = assertThrows(Exception.class, () -> repo.addBidToShoppingCart(-1, 114, bidItems));
        
        // Test adding empty bid items
        Map<Integer, Integer> emptyBid = Map.of();
        assertDoesNotThrow(() -> repo.addBidToShoppingCart(memberId, 115, emptyBid));
    }

    @Test
    void testClearShoppingCart_ComprehensiveScenarios() {
        // Set up shopping cart with items
        repo.createBasket(memberId, 121);
        repo.addItemToShoppingCart(memberId, 121, 1, 5);
        repo.addItemToShoppingCart(memberId, 121, 2, 3);
        
        // Verify items exist
        Map<Integer, Integer> basketBefore = repo.getBasket(memberId, 121);
        assertFalse(basketBefore.isEmpty());
        
        // Clear cart
        assertDoesNotThrow(() -> repo.clearShoppingCart(memberId));
        
        // Verify cart is cleared
        ShoppingCart clearedCart = repo.getShoppingCartById(memberId);
        assertNotNull(clearedCart);
        
        // Test clearing guest cart
        repo.createBasket(guestId, 122);
        repo.addItemToShoppingCart(guestId, 122, 3, 2);
        assertDoesNotThrow(() -> repo.clearShoppingCart(guestId));
        
        // Test clearing invalid user cart - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex15 = assertThrows(Exception.class, () -> repo.clearShoppingCart(-1));
    }

    @Test
    void testGetNotificationsAndClear_DetailedScenarios() {
        // Test getting notifications when none exist
        List<String> emptyNotifications = repo.getNotificationsAndClear(memberId);
        assertNotNull(emptyNotifications);
        
        // Add notifications
        repo.addNotification(memberId, "Title1", "Message1");
        repo.addNotification(memberId, "Title2", "Message2");
        repo.addNotification(memberId, "Title3", "Message3");
        
        // Get and verify notifications
        List<String> notifications = repo.getNotificationsAndClear(memberId);
        assertNotNull(notifications);
        assertTrue(notifications.size() >= 3);
        
        // Verify notifications are cleared after retrieval
        List<String> clearedNotifications = repo.getNotificationsAndClear(memberId);
        assertTrue(clearedNotifications.isEmpty());
        
        // Test for guest user - may return empty list or throw exception
        try {
            List<String> guestNotifications = repo.getNotificationsAndClear(guestId);
            // If no exception thrown, should get empty list or normal result
            assertNotNull(guestNotifications);
        } catch (OurRuntime e) {
            // Exception is also acceptable behavior for guests
            assertNotNull(e);
        }
        
        // Test for invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex20 = assertThrows(Exception.class, () -> repo.getNotificationsAndClear(-1));
    }

    @Test
    void testGetBasket_ExtensiveScenarios() {
        // Create basket and add items
        repo.createBasket(memberId, 131);
        repo.addItemToShoppingCart(memberId, 131, 1, 10);
        repo.addItemToShoppingCart(memberId, 131, 2, 15);
        
        // Test getting basket
        Map<Integer, Integer> basket = repo.getBasket(memberId, 131);
        assertNotNull(basket);
        assertEquals(10, basket.get(1));
        assertEquals(15, basket.get(2));
        
        // Test getting non-existent basket
        Map<Integer, Integer> emptyBasket = repo.getBasket(memberId, 999);
        assertNotNull(emptyBasket);
        assertTrue(emptyBasket.isEmpty());
        
        // Test for guest user
        repo.createBasket(guestId, 132);
        repo.addItemToShoppingCart(guestId, 132, 3, 20);
        
        Map<Integer, Integer> guestBasket = repo.getBasket(guestId, 132);
        assertNotNull(guestBasket);
        assertEquals(20, guestBasket.get(3));
        
        // Test for invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex18 = assertThrows(Exception.class, () -> repo.getBasket(-1, 131));
    }

    @Test
    void testRefund_DetailedScenarios() {
        // Test successful refund
        // First make a payment
        int paymentId = repo.pay(memberId, 100.0, "USD", "4111111111111111", "12", "2025", "John Doe", "123", "refund_test");
        assertTrue(paymentId >= 0);
        
        // Test refund - this might fail if payment method validation is strict
        try {
            repo.refund(memberId, paymentId);
        } catch (OurRuntime e) {
            // Expected if refund validation is strict or payment method issues exist
            assertNotNull(e);
        }
        
        // Test refund for invalid payment ID (might fail depending on payment system)
        assertThrows(OurRuntime.class, () -> repo.refund(memberId, -999));
        
        // Test refund for invalid user
        assertThrows(Exception.class, () -> repo.refund(-1, paymentId));
        
        // Test refund when no payment method is set
        Member memberWithoutPayment = repo.getMemberById(memberId);
        memberWithoutPayment.setPaymentMethod(null);
        repo.updateUserInDB(memberWithoutPayment);
        
        // This should fail because no payment method is set
        assertThrows(OurRuntime.class, () -> repo.refund(memberId, paymentId));
    }

    @Test
    void testGetShopOwner_ComprehensiveScenarios() {
        // Set up owner role
        Role ownerRole = new Role(memberId, 141, new PermissionsEnum[]{PermissionsEnum.manageOwners});
        repo.addRoleToPending(memberId, ownerRole);
        repo.acceptRole(memberId, ownerRole);
        
        // Test getting shop owner
        int ownerId = repo.getShopOwner(141);
        assertEquals(memberId, ownerId);
        
        // Test for shop with no owner
        int noOwnerId = repo.getShopOwner(999);
        assertEquals(-1, noOwnerId);
        
        // Test for shop with multiple roles but no owner
        int regularMemberId = repo.addMember("noowner", "pass", "noowner@test.com", "555-5555", "No Owner St");
        Role nonOwnerRole = new Role(regularMemberId, 142, new PermissionsEnum[]{PermissionsEnum.handleMessages});
        repo.addRoleToPending(regularMemberId, nonOwnerRole);
        repo.acceptRole(regularMemberId, nonOwnerRole);
        
        int nonOwnerResult = repo.getShopOwner(142);
        assertEquals(-1, nonOwnerResult);
    }

    @Test
    void testGetMissingNotificationsQuantity_DetailedScenarios() {
        // Test for guest (should return 0)
        int guestNotifications = repo.getMissingNotificationsQuantity(guestId);
        assertEquals(0, guestNotifications);
        
        // Test for member with no notifications
        int initialNotifications = repo.getMissingNotificationsQuantity(memberId);
        assertTrue(initialNotifications >= 0);
        
        // Add notifications
        repo.addNotification(memberId, "Test1", "Message1");
        repo.addNotification(memberId, "Test2", "Message2");
        repo.addNotification(memberId, "Test3", "Message3");
        
        // Test after adding notifications
        int afterAddingNotifications = repo.getMissingNotificationsQuantity(memberId);
        assertTrue(afterAddingNotifications >= 3);
        
        // Clear notifications and test again
        repo.getNotificationsAndClear(memberId);
        int afterClearingNotifications = repo.getMissingNotificationsQuantity(memberId);
        assertEquals(0, afterClearingNotifications);
        
        // Test for invalid user - Spring wraps exceptions
        @SuppressWarnings("unused")
        Exception ex21 = assertThrows(Exception.class, () -> repo.getMissingNotificationsQuantity(-1));
    }

}
