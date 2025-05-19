
package InfrastructureLayerTests;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.DomainLayer.Guest;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.User;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.InfrastructureLayer.UserRepository;
import com.example.app.DomainLayer.Notification;
import com.example.app.DomainLayer.ShoppingCart;

    
public class UserRepositoryTests {

    private UserRepository repo;
    private User guest;
    private User member;    
    private int guestId;
    private int memberId;

    @BeforeEach
    public void setup() {
        repo = new UserRepository();    
        repo.setEncoderToTest(true); // Set the encoder to test mode
        guestId = repo.addGuest();  
        guest = repo.getUserById(guestId);
        repo.addMember("username", "password", "email@example.com", "111", "address");
        memberId = repo.isUsernameAndPasswordValid("username", "password");
        member = repo.getUserById(memberId);
    }

    @Test
    void testIsAdmin()
    {
        assertTrue(repo.isAdmin(repo.isUsernameAndPasswordValid("admin", "admin")));
    }

    @Test
    void addAdmin()
    {
        repo.addMember("username", "password", "email@email.com", "phoneNumber", "address");
        int userid = repo.isUsernameAndPasswordValid("username", "password");
        repo.addAdmin(userid);
        assertTrue(repo.isAdmin(userid));
    }

    @Test
    void removeAdmin()
    {
        repo.addMember("username", "password", "email@email.com", "phoneNumber", "address");
        int userid = repo.isUsernameAndPasswordValid("username", "password");
        repo.addAdmin(userid);
        assertTrue(repo.isAdmin(userid));
        repo.removeAdmin(userid);
        assertFalse(repo.isAdmin(userid));
    }

    @Test
    public void testAddGuestAndGetUserById() {
        assertEquals(guestId, ((Guest)guest).getGuestId());
    }

    @Test
    public void testAddMember() {
        assertEquals("username", ((Member)member).getUsername());
        assertEquals("password", ((Member)member).getPassword());
        assertEquals("email@example.com", ((Member)member).getEmail());
        assertEquals("111", ((Member)member).getPhoneNumber());
    }


    @Test
    public void testUpdateMemberUsername() {
        repo.updateMemberUsername(memberId, "newUsername");
        assertEquals("newUsername", ((Member)member).getUsername());
    }

    @Test
    public void testUpdateMemberPassword() {
        repo.updateMemberPassword(memberId, "newPassword");
        assertEquals("newPassword", ((Member)member).getPassword());
    }

    @Test
    public void testUpdateMemberEmail() {
        repo.updateMemberEmail(memberId, "newemail@example.com");
        assertEquals("newemail@example.com", ((Member)member).getEmail());
    }

    @Test
    public void testUpdateMemberPhoneNumber() {
        repo.updateMemberPhoneNumber(memberId, "123456789");
        assertEquals("123456789", ((Member)member).getPhoneNumber());
    }   


       
    @Test
    public void testIsUsernameAndPasswordValid() {
        assertNotEquals(-1, memberId);
        assertEquals(((Member)member).getMemberId(), memberId);
        int invalidUserId = repo.isUsernameAndPasswordValid("invalidUser", "invalidPass");
        assertEquals(-1, invalidUserId);
    }

    @Test
    public void testIsUsernameTaken() {
        assertTrue(repo.isUsernameTaken("username"));
        assertFalse(repo.isUsernameTaken("availableUsername"));
    }

    @Test
    public void testIsGuestById() {
        assertTrue(repo.isGuestById(guestId));
        assertFalse(repo.isGuestById(-1)); // Non-existent ID
    }

    @Test
    public void testRemoveUserById() {
        repo.removeUserById(guestId);
        List<Guest> guests = repo.getGuestsList();
        assertEquals(0, guests.size()); // Assuming only one guest is added in setup
        assertFalse(repo.isGuestById(guestId)); // Check if the guest is removed
    }

    
    
    @Test
    public void testGetUserMapping() {
        Map<Integer, User> userMapping = repo.getUserMapping();
        assertTrue(userMapping.containsKey(guestId));
        assertEquals(guest, userMapping.get(guestId));
    }

    @Test
    public void testGetUsersList() {
        List<User> users = repo.getUsersList();
        assertTrue(users.contains(guest));
    }

    @Test
    public void testGetUsersIdsList() {
        List<Integer> userIds = repo.getUsersIdsList();
        assertTrue(userIds.contains(guestId));
    }

    @Test
    public void testGetGuestsList() {
        List<Guest> guests = repo.getGuestsList();
        assertTrue(guests.contains((Guest)guest));
    }


    @Test
    public void testGetMembersList() {
        List<Member> members = repo.getMembersList();
        assertTrue(members.contains((Member)member));
        assertFalse(members.contains((Guest)guest));
        assertEquals(2, members.size()); // Assuming only one member is added in setup
    }

    @Test
    public void testClear() {
        repo.addGuest();
        repo.addGuest();
        repo.clear();
        assertTrue(repo.getUsersList().isEmpty());
    }

    @Test
    void testGetUserAndMember() {
        // userId=1 was created as a Member ("admin")
        User u = repo.getUserById(1);
        assertNotNull(u);
    
        // unknown user should throw
        assertThrows(OurRuntime.class, () -> repo.getUserById(999));
    
        // admin is a Member, so getMemberById(1) should succeed
        Member admin = repo.getMemberById(1);
        assertEquals(1, admin.getMemberId());
    
        // create a true guest
        int guestId = repo.addGuest();
        assertTrue(repo.isGuestById(guestId));
        // but asking for a Member on a guest should throw
        assertThrows(OurRuntime.class, () -> repo.getMemberById(guestId));
    
        // now add a real member and verify retrieval
        repo.addMember("m","p","m@x","ph","addr");
        int newMemberId = guestId + 1;
        Member m = repo.getMemberById(newMemberId);
        assertEquals(newMemberId, m.getMemberId());
    }
    

    @Test
    void testAdminManagement() {
        // default admin id=1
        assertTrue(repo.isAdmin(1));
        repo.addAdmin(2);
        assertTrue(repo.isAdmin(2));
        assertThrows(OurRuntime.class, () -> repo.addAdmin(2), "duplicate admin");
        assertThrows(OurRuntime.class, () -> repo.removeAdmin(1), "cannot remove initial admin");
        repo.removeAdmin(2);
        assertFalse(repo.isAdmin(2));
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
    void testRemoveAndClear() {
        int g = repo.addGuest();
        repo.removeUserById(g);
        assertThrows(OurRuntime.class, () -> repo.removeUserById(g));
        repo.clear();
        assertTrue(repo.getUsersList().isEmpty());
    }

    @Test
    void testShoppingCartOps() {
        repo.addMember("shopper","pw","a@b","ph","ad");
        int uid = repo.isUsernameAndPasswordValid("shopper","pw");
    
        // add 2 of item 100 in shop 10
        repo.addItemToShoppingCart(uid, 10, 100, 2);
        assertEquals(2, repo.getBasket(uid, 10).get(100));
    
        // update quantity: old 2 + new 5 = 7
        repo.updateItemQuantityInShoppingCart(uid, 10, 100, 5);
        assertEquals(7, repo.getBasket(uid, 10).get(100));
    
        // remove the only item → basket still exists but is empty
        repo.removeItemFromShoppingCart(uid, 10, 100);
        assertTrue(repo.getBasket(uid, 10).isEmpty(), "After removing the last item, the basket should be empty");
    
        // explicitly create a new basket
        repo.createBasket(uid, 20);
        assertNotNull(repo.getBasket(uid, 20));
    
        // clear all baskets → getBasket returns null
        repo.clearShoppingCart(uid);
        assertNull(repo.getBasket(uid, 20), "After clearShoppingCart, there should be no basket at all");
    }
    

    // @Test
    // void testSuspendAndNotificationsAndPayment() {
    //     repo.addMember("notifier","pw","n@e","ph","ad");
    //     int uid = repo.isUsernameAndPasswordValid("notifier","pw");
    //     repo.setSuspended(uid, LocalDateTime.now().plusDays(1));
    //     assertTrue(repo.isSuspended(uid));
    //     List<Integer> suspended = repo.getSuspendedUsers();
    //     assertTrue(suspended.contains(uid));

    //     // payment method
    //     PaymentMethod pm = new PaymentMethod() {
    //         @Override public void processPayment(double amt,int sid){}
    //         @Override public String getDetails(){return "d";}
    //         @Override public void refundPayment(double amt,int sid){}
    //         @Override public void processRefund(double r,int sid){}
    //     };
    //     repo.setPaymentMethod(uid,0,pm);
    //     assertEquals("d", repo.getUserById(uid).getPaymentMethod().getDetails());
    //     // refund and pay success
    //     repo.pay(uid,0,10.0);
    //     repo.refund(uid,0,5.0);

    //     // notifications
    //     repo.addNotification(uid,"T","M");
    //     List<Notification> notes = repo.getNotificationsAndClear(uid);
    //     assertEquals(1, notes.size());
    //     assertTrue(notes.get(0).getTitle().contains("T"));
    // }

    @Test
    void testRoleAndWorkerMappings() {
        repo.addMember("owner","pw","o@e","ph","ad");
        int uid = repo.isUsernameAndPasswordValid("owner","pw");
        Role r = new Role(uid, 99, new PermissionsEnum[]{PermissionsEnum.manageOwners});
        // pending roles / setPermissions / getRole
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

    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void testSetPermissions_successAndFailures() {
        // create a fresh member
        repo.addMember("permUser", "pw", "a@a", "p", "addr");
        int puid = repo.isUsernameAndPasswordValid("permUser", "pw");
        // add & accept a role
        Role r = new Role(puid, 42, null);
        repo.addRoleToPending(puid, r);
        repo.acceptRole(puid, r);

        // now setPermissions to a non-empty array
        repo.setPermissions(puid, 42, r, new PermissionsEnum[]{PermissionsEnum.manageItems});
        assertTrue(r.hasPermission(PermissionsEnum.manageItems));

        // missing role should fail
        Role notBound = new Role(puid+1, 99, null);
        assertThrows(OurRuntime.class,
            () -> repo.setPermissions(puid, 99, notBound, new PermissionsEnum[]{PermissionsEnum.manageItems})
        );

        // null role
        assertThrows(OurRuntime.class,
            () -> repo.setPermissions(puid, 42, null, new PermissionsEnum[]{PermissionsEnum.manageItems})
        );

        // empty permissions
        assertThrows(OurRuntime.class,
            () -> repo.setPermissions(puid, 42, r, new PermissionsEnum[0])
        );
    }

    @Test
    void testUpdateMemberAddress_successAndFailures() {
        // valid update
        repo.updateMemberAddress(memberId, "C1", "S1", 5, "Z1");
        Member m = repo.getMemberById(memberId);
        assertEquals("C1", m.getAddress().getCity());
        assertEquals("S1", m.getAddress().getStreet());
        // apartment number and zip code come back as Strings
        assertEquals("5", m.getAddress().getApartmentNumber());
        assertEquals("Z1", m.getAddress().getZipCode());

        // updating a guest should fail
        assertThrows(OurRuntime.class,
            () -> repo.updateMemberAddress(guestId, "X","Y",1,"Z")
        );

        // non–existent user
        assertThrows(OurRuntime.class,
            () -> repo.updateMemberAddress(9999, "X","Y",1,"Z")
        );
    }


    @Test
    void testRemoveRole_successAndFailure() {
        // create and accept a role
        repo.addMember("temp","pw","t@t","p","a");
        int tid = repo.isUsernameAndPasswordValid("temp","pw");
        Role r = new Role(tid, 77, null);
        repo.addRoleToPending(tid, r);
        repo.acceptRole(tid, r);

        // remove it
        repo.removeRole(tid, 77);
        assertThrows(OurRuntime.class,
            () -> repo.getRole(tid, 77),
            "after removal getRole should fail"
        );

        // removing again fails
        assertThrows(OurRuntime.class,
            () -> repo.removeRole(tid, 77)
        );
    }

    @Test
    void testGetOwners_isOwner_isFounder() {
        // no owners initially
        assertTrue(repo.getOwners(123).isEmpty());
        assertFalse(repo.isOwner(memberId, 123));
        assertFalse(repo.isFounder(memberId, 123));

        // assign an owner role
        Role ownerR = new Role(memberId, 123, null);
        ownerR.addPermission(PermissionsEnum.manageOwners);
        repo.addRoleToPending(memberId, ownerR);
        repo.acceptRole(memberId, ownerR);

        assertTrue(repo.getOwners(123).stream().anyMatch(m -> m.getMemberId()==memberId));
        assertTrue(repo.isOwner(memberId, 123));
        assertFalse(repo.isFounder(memberId, 123));

        // assign a founder role
        Role founderR = new Role(memberId, 888, null);
        founderR.setFoundersPermissions();
        repo.addRoleToPending(memberId, founderR);
        repo.acceptRole(memberId, founderR);

        assertTrue(repo.isFounder(memberId, 888));
    }

    @Test
    void testDeclineRole_andPendingRoles() {
        // new pending
        Role r = new Role(memberId, 55, null);
        repo.addRoleToPending(memberId, r);

        // decline it
        repo.declineRole(memberId, r);
        assertTrue(repo.getPendingRoles(memberId).isEmpty(),
            "after decline, pending list should be empty"
        );

        // asking for it fails
        assertThrows(OurRuntime.class,
            () -> repo.getPendingRole(memberId, 55)
        );

        // unknown user
        assertThrows(OurRuntime.class,
            () -> repo.getPendingRoles(9999)
        );
    }

    @Test
    void testGetShoppingCartById_successAndFailure() {
        // every new user has a cart
        ShoppingCart sc = repo.getShoppingCartById(memberId);
        assertNotNull(sc);

        // non-existent
        assertThrows(OurRuntime.class,
            () -> repo.getShoppingCartById(9999)
        );
    }

    // ─── addRoleToPending null & invalid-user branches ───────────────────────
    @Test
    void testAddRoleToPending_NullAndInvalidUser() {
        // null role
        assertThrows(OurRuntime.class,
            () -> repo.addRoleToPending(memberId, null),
            "adding null role should throw");

        // invalid user
        Role dummy = new Role(memberId, 1, null);
        assertThrows(OurRuntime.class,
            () -> repo.addRoleToPending(9999, dummy),
            "unknown user should throw");
    }

    // ─── addItemToShoppingCart & updateItemQuantityInShoppingCart failure ────
    @Test
    void testShoppingCartFailures() {
        // invalid user
        assertThrows(OurRuntime.class,
            () -> repo.addItemToShoppingCart(9999, 1, 1, 1));
        assertThrows(OurRuntime.class,
            () -> repo.updateItemQuantityInShoppingCart(9999, 1, 1, 1));

        // zero or negative quantity
        assertThrows(OurRuntime.class,
            () -> repo.addItemToShoppingCart(memberId, 1, 1, 0));
        assertThrows(OurRuntime.class,
            () -> repo.updateItemQuantityInShoppingCart(memberId, 1, 1, -5));
    }

    // ─── notification & clear failure ───────────────────────────────────────
    @Test
    void testNotificationFailures() {
        // invalid user for addNotification
        assertThrows(OurRuntime.class,
            () -> repo.addNotification(9999, "T", "M"));

        // invalid user for getNotificationsAndClear
        assertThrows(OurRuntime.class,
            () -> repo.getNotificationsAndClear(9999));
    }

    // ─── setPaymentMethod & pay/refund failure ──────────────────────────────
    @Test
    void testPaymentFailures() {
        // non-existent user
        PaymentMethod pm = new PaymentMethod() {
            @Override public void processPayment(double a,int s){}
            @Override public String getDetails(){return "";}
            @Override public void refundPayment(double a,int s){}
            @Override public void processRefund(double r,int s){}
        };
        assertThrows(OurRuntime.class,
            () -> repo.setPaymentMethod(9999, 0, pm));
        assertThrows(OurRuntime.class,
            () -> repo.pay(9999, 0, 1.0));
        assertThrows(OurRuntime.class,
            () -> repo.refund(9999, 0, 1.0));

        // user exists but no payment method set
        assertThrows(OurRuntime.class,
            () -> repo.pay(memberId, 0, 1.0));
        assertThrows(OurRuntime.class,
            () -> repo.refund(memberId, 0, 1.0));
    }

    // ─── updateMemberUsername/email/password/phone failure ────────────────
    @Test
    void testUpdateMemberFields_Failures() {
        // guest is not a member
        assertThrows(OurRuntime.class,
            () -> repo.updateMemberUsername(guestId, "x"));
        assertThrows(OurRuntime.class,
            () -> repo.updateMemberPassword(guestId, "x"));
        assertThrows(OurRuntime.class,
            () -> repo.updateMemberEmail(guestId, "e@e"));
        assertThrows(OurRuntime.class,
            () -> repo.updateMemberPhoneNumber(guestId, "123"));

        // non-existent user
        assertThrows(OurRuntime.class,
            () -> repo.updateMemberUsername(9999, "x"));
    }

    // ─── isSuspended & setSuspended failure & false branch ─────────────────
    @Test
    void testSuspensionBranches() {
        // non-existent user in setSuspended
        assertThrows(OurRuntime.class,
            () -> repo.setSuspended(9999, LocalDateTime.now()));

        // non-suspended existing user
        assertFalse(repo.isSuspended(memberId),
            "new member should not be suspended yet");

        // invalid for isSuspended
        assertFalse(repo.isSuspended(9999),
            "unknown user should return false");
    }

    // ─── getPendingRoles happy path ─────────────────────────────────────────
    @Test
    void testGetPendingRoles_HappyPath() {
        repo.addMember("pend","pw","p@e","ph","ad");
        int pid = repo.isUsernameAndPasswordValid("pend","pw");
        Role r = new Role(pid, 99, null);
        repo.addRoleToPending(pid, r);
        List<Role> pending = repo.getPendingRoles(pid);
        assertEquals(1, pending.size());
        assertSame(r, pending.get(0));
    }

    // ─── acceptRole & declineRole failure branches ─────────────────────────
    @Test
    void testAcceptDeclineRole_Failures() {
        // null role
        assertThrows(OurRuntime.class,
            () -> repo.acceptRole(memberId, null));
        assertThrows(OurRuntime.class,
            () -> repo.declineRole(memberId, null));

        // invalid user
        Role r = new Role(memberId, 1, null);
        assertThrows(OurRuntime.class,
            () -> repo.acceptRole(9999, r));
        assertThrows(OurRuntime.class,
            () -> repo.declineRole(9999, r));
    }

    // ─── addPermission/removePermission failure branches ────────────────────
    @Test
    void testPermissionMgmt_Failures() {
        // no role bound
        assertThrows(OurRuntime.class,
            () -> repo.addPermission(memberId, PermissionsEnum.manageItems, 123));
        assertThrows(OurRuntime.class,
            () -> repo.removePermission(memberId, PermissionsEnum.manageItems, 123));

        // invalid user
        assertThrows(OurRuntime.class,
            () -> repo.addPermission(9999, PermissionsEnum.manageItems, 1));
    }

}