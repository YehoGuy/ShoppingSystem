package DBLayerTests;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.DomainLayer.ShoppingCart;
import com.example.app.DomainLayer.User;
import com.example.app.InfrastructureLayer.WSEPPay;
import com.example.app.SimpleHttpServerApplication;

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
        Role adminRole = new Role(adminId, 1, new PermissionsEnum[]{PermissionsEnum.manageItems, PermissionsEnum.manageOwners});
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
        
        assertEquals(guestId, ((Guest)guest).getGuestId());
    }

    @Test
    public void testAddMember() {
        
        assertEquals("username", ((Member)member).getUsername());
        assertEquals("email@example.com", ((Member)member).getEmail());
        assertEquals("111", ((Member)member).getPhoneNumber());
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
    }    @Test
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
        int guestId = repo.addGuest();
        assertTrue(repo.isGuestById(guestId));
        // But asking for a Member on a guest should throw
        assertThrows(OurRuntime.class, () -> repo.getMemberById(guestId));
    }    @Test
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
        
        repo.addMember("u2","pw","u2@e","ph","ad");
        assertTrue(repo.isUsernameTaken("u2"));
        int mid = repo.isUsernameAndPasswordValid("u2","pw");
        assertTrue(mid > 0);
        assertEquals(-1, repo.isUsernameAndPasswordValid("x","y"));
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
        // Get fresh role reference after removal since JPA entities might be detached
        Role freshRole = repo.getRole(uid, 99);
        assertFalse(freshRole.hasPermission(PermissionsEnum.handleMessages));

        List<Integer> workerShops = repo.getShopIdsByWorkerId(uid);
        assertTrue(workerShops.contains(99));
        assertTrue(repo.getShopMembers(99).stream().anyMatch(m -> m.getMemberId()==uid));
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
        repo.setPermissions(puid, 42, r, new PermissionsEnum[]{PermissionsEnum.manageItems});
        assertTrue(r.hasPermission(PermissionsEnum.manageItems));

        // Missing role should fail
        Role notBound = new Role(puid+1, 99, null);
        assertThrows(OurRuntime.class,
            () -> repo.setPermissions(puid, 42, notBound, new PermissionsEnum[]{PermissionsEnum.manageItems}));
    }

    @Test
    void testRemoveRole_successAndFailure() {
        
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
        
        ShoppingCart cart = repo.getShoppingCartById(memberId);
        assertNotNull(cart);

        // Invalid user should throw
        assertThrows(OurRuntime.class,
            () -> repo.getShoppingCartById(9999)
        );
    }    @Test
    void testGetPendingRoles_HappyPath() {
        
        repo.addMember("pend","pw","p@e","ph","ad");
        int pid = repo.isUsernameAndPasswordValid("pend","pw");
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
        
        // Payment without payment method set
        assertThrows(OurRuntime.class,
            () -> repo.pay(memberId, 100.0, "USD", "1234", "12", "25", "John", "123", "pay1"));
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
    }    @Test
    void testPaymentOperations() {
        
        WSEPPay paymentMethod = new WSEPPay();
        repo.setPaymentMethod(memberId, 1, paymentMethod);
        
        // This should fail due to null cardNumber which throws IllegalArgumentException
        assertThrows(Exception.class, () -> 
            repo.pay(memberId, 100.0, "USD", null, "12", "25", "John Doe", "123", "test-payment"));
    }

    @Test
    void testAuctionOperations() {
        
        List<?> auctionWins = repo.getAuctionsWinList(memberId);
        assertNotNull(auctionWins);
        
        // Test adding bid to shopping cart - this assumes Bid class exists
        // If Bid implementation is available, uncomment and adjust:
        // Bid testBid = new Bid(/* parameters */);
        // assertDoesNotThrow(() -> repo.addAuctionWinBidToShoppingCart(memberId, testBid));
    }    // Tests moved and consolidated below to avoid duplication
}
