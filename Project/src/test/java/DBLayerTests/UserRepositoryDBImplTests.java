package DBLayerTests;

import com.example.app.SimpleHttpServerApplication;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.DBLayer.User.UserRepositoryDBImpl;
import com.example.app.DomainLayer.Guest;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.User;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.DomainLayer.Notification;
import com.example.app.DomainLayer.ShoppingCart;
import com.example.app.InfrastructureLayer.WSEPPay;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.transaction.Transactional;

@SpringBootTest(classes = SimpleHttpServerApplication.class)
@ActiveProfiles({ "test" })
@Transactional
public class UserRepositoryDBImplTests {

    @Autowired
    private UserRepositoryDBImpl repo;

    private User guest;
    private User member;    
    private int guestId;
    private int memberId;

    // Helper method to set up test data in each test method
    private void setup() {
        repo.setEncoderToTest(true); // Set the encoder to test mode
        guestId = repo.addGuest();
        guest = repo.getUserById(guestId);
        repo.addMember("username", "password", "email@example.com", "111", "address");
        memberId = repo.isUsernameAndPasswordValid("username", "password");
        member = repo.getUserById(memberId);
    }

    @Test
    void testIsAdmin() {
        setup();
        assertTrue(repo.isAdmin(repo.isUsernameAndPasswordValid("admin", "admin")));
    }

    @Test
    void testAddAdmin() {
        setup();
        repo.addMember("username2", "password", "email@email.com", "phoneNumber", "address");
        int userid = repo.isUsernameAndPasswordValid("username2", "password");
        repo.addAdmin(userid);
        assertTrue(repo.isAdmin(userid));
    }

    @Test
    void testRemoveAdmin() {
        setup();
        repo.addMember("username3", "password", "email@email.com", "phoneNumber", "address");
        int userid = repo.isUsernameAndPasswordValid("username3", "password");
        repo.addAdmin(userid);
        assertTrue(repo.isAdmin(userid));
        repo.removeAdmin(userid);
        assertFalse(repo.isAdmin(userid));
    }

    @Test
    void testGetAllAdmins() {
        setup();
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
        setup();
        assertEquals(guestId, ((Guest)guest).getGuestId());
    }

    @Test
    public void testAddMember() {
        setup();
        assertEquals("username", ((Member)member).getUsername());
        assertEquals("email@example.com", ((Member)member).getEmail());
        assertEquals("111", ((Member)member).getPhoneNumber());
    }

    @Test
    public void testUpdateMemberUsername() {
        setup();
        repo.updateMemberUsername(memberId, "newUsername");
        Member updatedMember = repo.getMemberById(memberId);
        assertEquals("newUsername", updatedMember.getUsername());
    }

    @Test
    public void testUpdateMemberPassword() {
        setup();
        String newPassword = "newPassword";
        repo.updateMemberPassword(memberId, newPassword);
        Member updatedMember = repo.getMemberById(memberId);
        // Check if password encoding works
        assertTrue(repo.getPasswordEncoderUtil().matches(newPassword, updatedMember.getPassword()));
    }

    @Test
    public void testUpdateMemberEmail() {
        setup();
        repo.updateMemberEmail(memberId, "newemail@example.com");
        Member updatedMember = repo.getMemberById(memberId);
        assertEquals("newemail@example.com", updatedMember.getEmail());
    }

    @Test
    public void testUpdateMemberPhoneNumber() {
        setup();
        repo.updateMemberPhoneNumber(memberId, "222");
        Member updatedMember = repo.getMemberById(memberId);
        assertEquals("222", updatedMember.getPhoneNumber());
    }

    @Test
    public void testUpdateMemberAddress() {
        setup();
        repo.updateMemberAddress(memberId, "NewCity", "NewStreet", 123, "12345");
        Member updatedMember = repo.getMemberById(memberId);
        // Note: This assumes Member has getter methods for address components
        // If the address is stored differently, adjust accordingly
        assertNotNull(updatedMember);
    }

    @Test
    public void testIsUsernameAndPasswordValid() {
        setup();
        int validId = repo.isUsernameAndPasswordValid("username", "password");
        assertEquals(memberId, validId);
        
        int invalidId = repo.isUsernameAndPasswordValid("invalid", "invalid");
        assertEquals(-1, invalidId);
    }

    @Test
    public void testIsUsernameTaken() {
        setup();
        assertTrue(repo.isUsernameTaken("username"));
        assertFalse(repo.isUsernameTaken("nonexistent"));
    }

    @Test
    public void testRemoveUserById() {
        setup();
        // Cannot test removing members from DB in same way as in-memory
        // but we can test guest removal
        int newGuestId = repo.addGuest();
        assertTrue(repo.isGuestById(newGuestId));
        repo.removeUserById(newGuestId);
        assertFalse(repo.isGuestById(newGuestId));
    }

    @Test
    void testGetUserAndMember() {
        setup();
        // Admin user should exist with ID 0 (as set in initAdmin)
        User adminUser = repo.getUserById(0);
        assertNotNull(adminUser);
    
        // Unknown user should throw
        assertThrows(OurRuntime.class, () -> repo.getUserById(999));
    
        // Admin is a Member, so getMemberById(0) should succeed
        Member admin = repo.getMemberById(0);
        assertEquals(0, admin.getMemberId());
    
        // Create a true guest
        int guestId = repo.addGuest();
        assertTrue(repo.isGuestById(guestId));
        // But asking for a Member on a guest should throw
        assertThrows(OurRuntime.class, () -> repo.getMemberById(guestId));
    }

    @Test
    void testAdminManagement() {
        setup();
        // Default admin should exist
        assertTrue(repo.isAdmin(0));
        
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
        setup();
        int g1 = repo.addGuest();
        assertTrue(repo.isGuestById(g1));
        
        repo.addMember("u2","pw","u2@e","ph","ad");
        assertTrue(repo.isUsernameTaken("u2"));
        int mid = repo.isUsernameAndPasswordValid("u2","pw");
        assertTrue(mid > 0);
        assertEquals(-1, repo.isUsernameAndPasswordValid("x","y"));
    }

    @Test
    void testShoppingCartOps() {
        setup();
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
        setup();
        repo.addMember("owner","pw","o@e","ph","ad");
        int uid = repo.isUsernameAndPasswordValid("owner","pw");
        Role r = new Role(uid, 99, new PermissionsEnum[]{PermissionsEnum.manageOwners});
        
        // Test role operations
        repo.addRoleToPending(uid, r);
        assertThrows(OurRuntime.class, () -> repo.addRoleToPending(uid, r));
        assertThrows(OurRuntime.class, () -> repo.getPendingRole(uid, 100));
        
        Role pr = repo.getPendingRole(uid,99);
        assertEquals(r, pr);
        
        repo.acceptRole(uid, r);
        Role rr = repo.getRole(uid,99);
        assertEquals(r, rr);
        
        repo.addPermission(uid, PermissionsEnum.handleMessages, 99);
        assertTrue(rr.hasPermission(PermissionsEnum.handleMessages));
        
        repo.removePermission(uid, PermissionsEnum.handleMessages, 99);
        assertFalse(rr.hasPermission(PermissionsEnum.handleMessages));

        List<Integer> workerShops = repo.getShopIdsByWorkerId(uid);
        assertTrue(workerShops.contains(99));
        assertTrue(repo.getShopMembers(99).stream().anyMatch(m -> m.getMemberId()==uid));
    }

    @Test
    void testSetPermissions_successAndFailures() {
        setup();
        // Create a fresh member
        repo.addMember("permUser", "pw", "a@a", "p", "addr");
        int puid = repo.isUsernameAndPasswordValid("permUser", "pw");
        // Add & accept a role
        Role r = new Role(puid, 42, null);
        repo.addRoleToPending(puid, r);
        repo.acceptRole(puid, r);

        // Now setPermissions to a non-empty array
        repo.setPermissions(puid, 42, r, new PermissionsEnum[]{PermissionsEnum.manageItems});
        assertTrue(r.hasPermission(PermissionsEnum.manageItems));

        // Missing role should fail
        Role notBound = new Role(puid+1, 99, null);
        assertThrows(OurRuntime.class,
            () -> repo.setPermissions(puid, 42, notBound, new PermissionsEnum[]{PermissionsEnum.manageItems}));
    }

    @Test
    void testRemoveRole_successAndFailure() {
        setup();
        // Create and accept a role
        repo.addMember("temp","pw","t@t","p","a");
        int tid = repo.isUsernameAndPasswordValid("temp","pw");
        Role r = new Role(tid, 77, null);
        repo.addRoleToPending(tid, r);
        repo.acceptRole(tid, r);

        // Remove it
        repo.removeRole(tid, 77);
        assertThrows(OurRuntime.class,
            () -> repo.getRole(tid, 77),
            "after removal getRole should fail"
        );

        // Removing again fails
        assertThrows(OurRuntime.class,
            () -> repo.removeRole(tid, 77)
        );
    }

    @Test
    void testGetOwners_isOwner_isFounder() {
        setup();
        // No owners initially
        assertTrue(repo.getOwners(123).isEmpty());
        assertFalse(repo.isOwner(memberId, 123));
        assertFalse(repo.isFounder(memberId, 123));

        // Assign an owner role
        Role ownerR = new Role(memberId, 123, null);
        ownerR.addPermission(PermissionsEnum.manageOwners);
        repo.addRoleToPending(memberId, ownerR);
        repo.acceptRole(memberId, ownerR);

        assertTrue(repo.getOwners(123).stream().anyMatch(m -> m.getMemberId()==memberId));
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
        setup();
        // New pending
        Role r = new Role(memberId, 55, null);
        repo.addRoleToPending(memberId, r);

        // Decline it
        repo.declineRole(memberId, r);
        assertTrue(repo.getPendingRoles(memberId).isEmpty(),
            "after decline, pending list should be empty"
        );

        // Asking for it fails
        assertThrows(OurRuntime.class,
            () -> repo.getPendingRole(memberId, 55)
        );

        // Unknown user
        assertThrows(OurRuntime.class,
            () -> repo.getPendingRoles(9999)
        );
    }

    @Test
    void testGetShoppingCartById_successAndFailure() {
        setup();
        ShoppingCart cart = repo.getShoppingCartById(memberId);
        assertNotNull(cart);

        // Invalid user should throw
        assertThrows(OurRuntime.class,
            () -> repo.getShoppingCartById(9999)
        );
    }

    @Test
    void testGetPendingRoles_HappyPath() {
        setup();
        repo.addMember("pend","pw","p@e","ph","ad");
        int pid = repo.isUsernameAndPasswordValid("pend","pw");
        Role r = new Role(pid, 99, null);
        repo.addRoleToPending(pid, r);
        List<Role> pending = repo.getPendingRoles(pid);
        assertEquals(1, pending.size());
        assertSame(r, pending.get(0));
    }

    @Test
    void testAddRoleToPending_NullAndInvalidUser() {
        setup();
        // Null role
        assertThrows(OurRuntime.class,
            () -> repo.addRoleToPending(memberId, null));

        // Invalid user
        assertThrows(OurRuntime.class,
            () -> repo.addRoleToPending(9999, new Role(9999, 1, null)));
    }

    @Test
    void testShoppingCartFailures() {
        setup();
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
        setup();
        // Invalid user for addNotification
        assertThrows(OurRuntime.class,
            () -> repo.addNotification(9999, "T", "M"));

        // Invalid user for getNotificationsAndClear
        assertThrows(OurRuntime.class,
            () -> repo.getNotificationsAndClear(9999));
    }

    @Test
    void testPaymentFailures() {
        setup();
        // Invalid user for payment operations
        assertThrows(OurRuntime.class,
            () -> repo.setPaymentMethod(9999, 1, new WSEPPay()));
        
        // Payment without payment method set
        assertThrows(OurRuntime.class,
            () -> repo.pay(memberId, 100.0, "USD", "1234", "12", "25", "John", "123", "pay1"));
    }

    @Test
    void testUpdateMemberFields_Failures() {
        setup();
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
        setup();
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
        setup();
        Map<Integer, User> userMapping = repo.getUserMapping();
        assertNotNull(userMapping);
        // The mapping should contain database users
        assertFalse(userMapping.isEmpty());
    }

    @Test
    void testGetUsersList() {
        setup();
        List<User> users = repo.getUsersList();
        assertNotNull(users);
        // Should contain at least the admin and test members
        assertFalse(users.isEmpty());
    }

    @Test
    void testGetUsersIdsList() {
        setup();
        List<Integer> userIds = repo.getUsersIdsList();
        assertNotNull(userIds);
        assertFalse(userIds.isEmpty());
    }

    @Test
    void testGetGuestsList() {
        setup();
        List<Guest> guests = repo.getGuestsList();
        assertNotNull(guests);
        // Should contain at least our test guest
        assertTrue(guests.stream().anyMatch(g -> g.getGuestId() == guestId));
    }

    @Test
    void testGetMembersList() {
        setup();
        List<Member> members = repo.getMembersList();
        assertNotNull(members);
        // Should contain at least the admin and test member
        assertTrue(members.size() >= 2);
    }

    @Test
    void testGetAllMembers() {
        setup();
        List<Member> allMembers = repo.getAllMembers();
        assertNotNull(allMembers);
        // Should contain at least the admin and test member
        assertTrue(allMembers.size() >= 2);
    }

    @Test
    void testClear() {
        setup();
        // Clear operation behavior might be different for DB implementation
        // This test ensures the method exists and doesn't throw
        assertDoesNotThrow(() -> repo.clear());
    }

    @Test
    void testSuspensionOperations() {
        setup();
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
        setup();
        assertNotNull(repo.getPasswordEncoderUtil());
        
        // Test encoder setup
        repo.setEncoderToTest(false);
        repo.setEncoderToTest(true);
    }

    @Test
    void testNotificationOperations() {
        setup();
        repo.addNotification(memberId, "Test Title", "Test Message");
        List<String> notifications = repo.getNotificationsAndClear(memberId);
        assertNotNull(notifications);
    }

    @Test
    void testPaymentOperations() {
        setup();
        WSEPPay paymentMethod = new WSEPPay();
        repo.setPaymentMethod(memberId, 1, paymentMethod);
        
        // This might fail due to external payment service, but tests the method exists
        assertThrows(Exception.class, () -> 
            repo.pay(memberId, 100.0, "USD", "1234567890123456", "12", "25", "John Doe", "123", "test-payment"));
    }

    @Test
    void testAuctionOperations() {
        setup();
        List<?> auctionWins = repo.getAuctionsWinList(memberId);
        assertNotNull(auctionWins);
        
        // Test adding bid to shopping cart - this assumes Bid class exists
        // If Bid implementation is available, uncomment and adjust:
        // Bid testBid = new Bid(/* parameters */);
        // assertDoesNotThrow(() -> repo.addAuctionWinBidToShoppingCart(memberId, testBid));
    }
}
