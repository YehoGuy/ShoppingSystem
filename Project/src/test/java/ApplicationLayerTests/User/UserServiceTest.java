package ApplicationLayerTests.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.User;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.InfrastructureLayer.AuthTokenRepository;
import com.example.app.InfrastructureLayer.UserRepository;

public class UserServiceTest {

    private UserRepository userRepository;
    private AuthTokenRepository authTokenRepository;
    private AuthTokenService authTokenService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        authTokenRepository = new AuthTokenRepository(); // Your real repo
        authTokenService = new AuthTokenService(authTokenRepository); // Real service
        userRepository = new UserRepository();
        userService = new UserService(userRepository, authTokenService);
    }

    @Test
    void getFirstAdminId() {
        int firstAdminId = userRepository.isUsernameAndPasswordValid("admin", "admin");
        assertEquals(1, firstAdminId); // Assuming the first admin has ID 1
    }

    @Test
    void testIsAdmin() {
        assertTrue(userService.isAdmin(userRepository.isUsernameAndPasswordValid("admin", "admin")));
    }

    @Test
    void addAdmin() {
        String token = userService.loginAsMember("admin", "admin", "");
        userService.addMember("username", "password", "email@email.com", "0123456789", "address");
        int userid = userRepository.isUsernameAndPasswordValid("username", "password");
        userService.makeAdmin(token, userid);
        assertTrue(userService.isAdmin(userid));
    }

    @Test
    void removeAdmin() {
        String token = userService.loginAsMember("admin", "admin", "");
        userService.addMember("username", "password", "email@email.com", "0123456789", "address");
        int userid = userRepository.isUsernameAndPasswordValid("username", "password");
        userService.makeAdmin(token, userid);
        assertTrue(userRepository.isAdmin(userid));
        userService.removeAdmin(token, userid);
        assertFalse(userRepository.isAdmin(userid));
    }

    @Test
    void testAddMemberAndGetUserById() {
        userService.addMember("john", "pass123", "john@example.com", "0123456789", "123 Main St");
        int memberId = userRepository.isUsernameAndPasswordValid("john", "pass123");
        User member = userService.getUserById(memberId);

        assertNotNull(member);
        assertEquals("john", ((Member) member).getUsername());
        assertEquals("john@example.com", ((Member) member).getEmail());
    }

    @Test
    void testUpdateMember() {
        userService.addMember("john", "pass123", "john@example.com", "1234567890", "123 Main St");
        int memberId = userRepository.isUsernameAndPasswordValid("john", "pass123");
        String token = authTokenService.Login("john", "pass123", memberId); // No token generated, but user added
        userService.updateMemberUsername(token, "newusername");
        userService.updateMemberPassword(token, "newpassword");
        userService.updateMemberEmail(token, "newemail@example.com");
        userService.updateMemberPhoneNumber(token, "0987654321");

        User member = userService.getUserById(memberId);
        assertEquals("newusername", ((Member) member).getUsername());
        assertEquals("newemail@example.com", ((Member) member).getEmail());
        assertEquals("0987654321", ((Member) member).getPhoneNumber());

    }

    @Test
    void testLoginAsGuest() {
        String token = userService.loginAsGuest(); // No token generated, but user added
        assertNotNull(token); // Might be empty string depending on implementation

        int userId = authTokenRepository.getUserIdByToken(token);
        assertTrue(userRepository.isGuestById(userId));
    }

    @Test
    void testSignUpMember() {
        userService.addMember("user1", "pass1", "user1@mail.com", "1234567890", "address");
        int memberId = userRepository.isUsernameAndPasswordValid("user1", "pass1");
        assertNotNull(memberId);
        assertTrue(memberId > 0);
    }

    @Test
    void testLogoutRemovesToken() throws Exception {
        String token = userService.loginAsGuest();

        token = userService.logout(token);
        assertNotNull(token); // Might be empty string depending on implementation
    }

    @Test
    void testInvalidMemberIdValidation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.validateMemberId(-5);
        });

        assertTrue(exception.getMessage().contains("Invalid user ID"));
    }

    @Test
    void testSetPaymentMethod() throws Exception {
        userService.addMember("john", "snow", "aaa@gmail.com", "1234567890", "address");
        String token = userService.loginAsMember("john", "snow", "");
        User user = userService.getUserById(userRepository.isUsernameAndPasswordValid("john", "snow"));
        PaymentMethod paymentMethod = Mockito.mock(PaymentMethod.class);
        userService.setPaymentMethod(token, paymentMethod, 1);
        // Verify that the payment method was set correctly
        assertEquals(paymentMethod, user.getPaymentMethod());
    }

    @Test
    void testPay() throws Exception {
        userService.addMember("john", "snow", "aaa@gmail.com", "123457890", "address");
        String token = userService.loginAsMember("john", "snow", "");
        PaymentMethod paymentMethod = Mockito.mock(PaymentMethod.class);
        userService.setPaymentMethod(token, paymentMethod, 1);
        // Verify that the payment was processed correctly
        assertTrue(userService.pay(token, 1, 100.0));
    }

    @Test
    void testConcurrentAddGuest() throws InterruptedException {
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    userRepository.addGuest();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // Wait for all tasks to finish
        executor.shutdown();

        assertEquals(threads + 1, userRepository.getUserMapping().size()); // +1 for the default admin
    }

    @Test
    void testConcurrentAddMembers() throws InterruptedException {
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    userRepository.addMember("user" + finalI, "password" + finalI, "email" + finalI + "@test.com",
                            "123456789", "address" + finalI);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threads + 1, userRepository.getUserMapping().size()); // +1 for default admin
    }

    @Test
    void testConcurrentAddRemoveAdmin() throws InterruptedException {
        int userId = userRepository.isUsernameAndPasswordValid("admin", "admin");

        // Add a second admin
        userRepository.addMember("secondAdmin", "password", "second@test.com", "123456789", "addr");
        int secondAdminId = userRepository.isUsernameAndPasswordValid("secondAdmin", "password");
        userRepository.addAdmin(secondAdminId);

        int threads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        try {
                            userRepository.addAdmin(secondAdminId);
                        } catch (Exception ignored) {
                        }
                    } else {
                        try {
                            userRepository.removeAdmin(secondAdminId);
                        } catch (Exception ignored) {
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // After concurrent add/remove, either admin is in or not
        assertTrue(userRepository.getAllAdmins().contains(userId)); // The system creator admin must always exist
    }

    @Test
    void testSetSuspendedSuccess() {
        // create a fresh user
        userService.addMember("suspendUser", "pass123", "suspend@mail.com", "0123456789", "addr");
        int memberId = userRepository.isUsernameAndPasswordValid("suspendUser", "pass123");
        // suspend for 24 hours
        LocalDateTime until = LocalDateTime.now().plusDays(1);
        userService.setSuspended(memberId, until);
        // now isSuspended should return true
        assertTrue(userService.isSuspended(memberId));
    }

    @Test
    void testSetSuspendedFailure() {
        // replace repository with a mock that throws
        UserRepository mockRepo = Mockito.mock(UserRepository.class);
        userService = new UserService(mockRepo, authTokenService);
        // stub to throw low‐level error
        Mockito.doThrow(new RuntimeException("DB error"))
                .when(mockRepo).setSuspended(Mockito.eq(42), Mockito.any(LocalDateTime.class));
        // calling setSuspended should be wrapped in OurRuntime
        OurRuntime ex = assertThrows(OurRuntime.class, () -> userService.setSuspended(42, LocalDateTime.now()));
        assertTrue(ex.getMessage().contains("Error setting suspension for user ID 42"));
    }

    // --- getNotificationsAndClear ---
    @Test
    void testGetNotificationsAndClearSuccess() throws Exception {
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        UserService svc = new UserService(mockRepo, mockAuth);

        String token = "tok123";
        when(mockAuth.ValidateToken(token)).thenReturn(77);

        svc.getNotificationsAndClear(token);

        verify(mockRepo).getNotificationsAndClear(77);
    }

    @Test
    void testGetNotificationsAndClearFailure() throws Exception {
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        UserService svc = new UserService(mockRepo, mockAuth);

        String token = "tokX";
        when(mockAuth.ValidateToken(token)).thenReturn(42);
        doThrow(new RuntimeException("DB down"))
                .when(mockRepo).getNotificationsAndClear(42);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.getNotificationsAndClear(token));
        assertTrue(ex.getMessage().contains("getNotificationsAndClear"));
    }

    // --- purchaseNotification ---
    @Test
    void testPurchaseNotificationSuccess() {
        // --- arrange ---
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService);

        Member owner = mock(Member.class);
        when(owner.getMemberId()).thenReturn(10);
        when(mockRepo.getOwners(5)).thenReturn(List.of(owner));

        // build the cart as exactly HashMap<Integer,HashMap<Integer,Integer>>
        HashMap<Integer, HashMap<Integer, Integer>> cart = new HashMap<>();
        HashMap<Integer, Integer> items = new HashMap<>();
        items.put(3, 2);
        cart.put(5, items);

        // --- act ---
        svc.purchaseNotification(cart);

        // --- assert ---
        verify(mockRepo).addNotification(10,
                "Item 3 Purchased",
                "Quantity: 2 purchased from your shop ID: 5");
    }

    @Test
    void testPurchaseNotificationFailure() {
        // --- arrange ---
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService);

        Member owner = mock(Member.class);
        when(owner.getMemberId()).thenReturn(9);
        when(mockRepo.getOwners(1)).thenReturn(List.of(owner));

        // build the cart with the right types
        HashMap<Integer, HashMap<Integer, Integer>> cart = new HashMap<>();
        HashMap<Integer, Integer> items = new HashMap<>();
        items.put(7, 3);
        cart.put(1, items);

        // stub addNotification to throw
        doThrow(new RuntimeException("oops"))
                .when(mockRepo).addNotification(eq(9), anyString(), anyString());

        // --- act & assert ---
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.purchaseNotification(cart));
        assertTrue(ex.getMessage().contains("purchaseNotification"));
    }

    // --- closeShopNotification ---
    @Test
    void testCloseShopNotificationSuccess() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService);

        Member owner = mock(Member.class);
        when(owner.getMemberId()).thenReturn(21);
        when(mockRepo.getOwners(11)).thenReturn(List.of(owner));

        svc.closeShopNotification(11);

        verify(mockRepo).addNotification(21,
                "Shop Closed",
                "Your shop ID: 11 has been closed.");
    }

    @Test
    void testCloseShopNotificationFailure() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService);

        when(mockRepo.getOwners(99))
                .thenThrow(new RuntimeException("network"));

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.closeShopNotification(99));
        assertTrue(ex.getMessage().contains("closeShopNotification"));
    }

    // --- removedAppointment ---
    @Test
    void testRemovedAppointmentWithoutShopSuccess() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService);

        svc.removedAppointment(5, "Dentist", null);
        verify(mockRepo).addNotification(5,
                "Appointment Removed",
                "Your appointment to: Dentist has been removed.");
    }

    @Test
    void testRemovedAppointmentWithShopSuccess() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService);

        svc.removedAppointment(6, "Checkup", 42);
        verify(mockRepo).addNotification(6,
                "Appointment Removed",
                "Your appointment to: Checkup in the shop 42 has been removed.");
    }

    @Test
    void testRemovedAppointmentFailure() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService);

        doThrow(new RuntimeException("boom"))
                .when(mockRepo).addNotification(anyInt(), anyString(), anyString());

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.removedAppointment(8, "X", null));
        assertTrue(ex.getMessage().contains("removedAppointment"));
    }

    // --- messageNotification ---
    @Test
    void testMessageNotificationFromShopSuccess() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService);

        svc.messageNotification(33, 55, true);
        verify(mockRepo).addNotification(33,
                "Message Received",
                "You have received a new message from the shop (id=55).");
    }

    @Test
    void testMessageNotificationFromUserSuccess() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService);

        svc.messageNotification(44, 0, false);
        verify(mockRepo).addNotification(44,
                "Message Received",
                "You have received a new message from the user (id=44).");
    }

    @Test
    void testMessageNotificationFailure() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService);

        doThrow(new RuntimeException("failMsg"))
                .when(mockRepo).addNotification(anyInt(), anyString(), anyString());

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.messageNotification(99, 0, false));
        assertTrue(ex.getMessage().contains("messageUserNotification"));
    }

    // --- validation helpers ---
    @Test
    void testIsValidUsernameEmailPhonePasswordAndDetails() {
        // username
        assertTrue(userService.isValidUsername("user_123"));
        assertFalse(userService.isValidUsername("ab"));        // too short
        assertFalse(userService.isValidUsername("bad!name"));  // invalid char

        // email
        assertTrue(userService.isValidEmail("x@y.com"));
        assertFalse(userService.isValidEmail("no-at-sign"));

        // phone
        assertTrue(userService.isValidPhoneNumber("+123-456789"));
        assertFalse(userService.isValidPhoneNumber("1234"));  // too short

        // password (always true in test mode)
        assertTrue(userService.isValidPassword("anything"));

        // combined details
        OurRuntime ex = assertThrows(OurRuntime.class, () ->
            userService.isValidDetails("ab", "", "bademail", "1234")
        );
        String msg = ex.getMessage();
        assertTrue(msg.contains("Invalid Username."));
        assertTrue(msg.contains("Invalid Phone Number."));
        assertTrue(msg.contains("Invalid Email."));
    }

    // --- getAllAdmins ACL ---
    @Test
    void testGetAllAdminsSuccessAndFailure() {
        // admin can
        String adminToken = userService.loginAsMember("admin", "admin", "");
        List<Integer> admins = userService.getAllAdmins(adminToken);
        int adminId = userRepository.isUsernameAndPasswordValid("admin", "admin");
        assertTrue(admins.contains(adminId));

        // non-admin cannot
        userService.addMember("bob","pwd","b@b.com","0123456789","addr");
        int bobId = userRepository.isUsernameAndPasswordValid("bob","pwd");
        String bobToken = authTokenService.Login("bob","pwd", bobId);
        assertThrows(OurRuntime.class, () ->
            userService.getAllAdmins(bobToken)
        );
    }

    // --- loginAsMember merges guest cart ---
    @Test
    void testLoginAsMemberMergesGuestCart() {
        // create a guest, add to cart
        String guestToken = userService.loginAsGuest();
        int guestId = authTokenRepository.getUserIdByToken(guestToken);
        userService.addItemToShoppingCart(guestToken, 1, 101, 2);

        // create a real member
        userService.addMember("alice","pwd","a@a.com","0123456789","addr");
        int aliceId = userRepository.isUsernameAndPasswordValid("alice","pwd");

        // login as member, merging
        String aliceToken = userService.loginAsMember("alice","pwd", guestToken);
        assertEquals(aliceId, authTokenRepository.getUserIdByToken(aliceToken));
        assertFalse(userRepository.isGuestById(guestId), "old guest should be removed");

        Map<Integer,Integer> basket = userService.getBasketItems(aliceToken, 1);
        assertEquals(2, basket.get(101).intValue());
    }

    // --- shopping cart ops by userId ---
    @Test
    void testGetClearAndRestoreUserShoppingCartById() {
        userService.addMember("tom","pwd","t@t.com","0123456789","addr");
        int tomId = userRepository.isUsernameAndPasswordValid("tom","pwd");

        // seed via repository directly
        userRepository.addItemToShoppingCart(tomId, 2, 201, 3);
        HashMap<Integer, HashMap<Integer,Integer>> cart = userService.getUserShoppingCartItems(tomId);
        assertEquals(3, cart.get(2).get(201).intValue());

        // clear → the entire cart should now be empty
        userService.clearUserShoppingCart(tomId);
        HashMap<Integer, HashMap<Integer,Integer>> empty = userService.getUserShoppingCartItems(tomId);
        assertTrue(empty.isEmpty(), "After clearing, no baskets should remain");

        // restore
        HashMap<Integer, HashMap<Integer,Integer>> restore = new HashMap<>();
        HashMap<Integer,Integer> items = new HashMap<>();
        items.put(202, 4);
        restore.put(3, items);
        userService.restoreUserShoppingCart(tomId, restore);
        HashMap<Integer, HashMap<Integer,Integer>> after = userService.getUserShoppingCartItems(tomId);
        assertEquals(4, after.get(3).get(202).intValue());
    }


    // --- getUserPaymentMethod and setPaymentMethod ---
    @Test
    void testGetUserPaymentMethod() {
        userService.addMember("sam","pwd","s@m.com","0123456789","addr");
        int samId = userRepository.isUsernameAndPasswordValid("sam","pwd");
        assertNull(userService.getUserPaymentMethod(samId));

        String samToken = userService.loginAsMember("sam","pwd", "");
        PaymentMethod pm = Mockito.mock(PaymentMethod.class);
        userService.setPaymentMethod(samToken, pm, 5);
        assertEquals(pm, userService.getUserPaymentMethod(samId));
    }

    // --- suspend and suspendedUsers ---
    @Test
    void testSetAndGetSuspended() {
        userService.addMember("lucy","pwd","l@l.com","0123456789","addr");
        int lucyId = userRepository.isUsernameAndPasswordValid("lucy","pwd");
        LocalDateTime until = LocalDateTime.now().plusDays(2);
        userService.setSuspended(lucyId, until);
        assertTrue(userService.isSuspended(lucyId));
        List<Integer> sus = userService.getSuspendedUsers();
        assertTrue(sus.contains(lucyId));
    }

    // --- logout issues a new guest token ---
    @Test
    void testLogoutIssuesNewGuestToken() {
        String guest1 = userService.loginAsGuest();
        String guest2 = userService.logout(guest1);
        assertNotNull(guest2);
        int newGuestId = authTokenRepository.getUserIdByToken(guest2);
        assertTrue(userRepository.isGuestById(newGuestId));
    }

    // --- validateMemberId edge cases ---
    @Test
    void testValidateMemberIdNonPositiveAndNonMember() {
        assertThrows(OurArg.class, () -> userService.validateMemberId(0));
        String guestToken = userService.loginAsGuest();
        int guestId = authTokenRepository.getUserIdByToken(guestToken);
        assertThrows(OurArg.class, () -> userService.validateMemberId(guestId));
    }

    // --- getPermitionsByShop positive & negative ---
    @Test
    void testGetPermitionsByShop() {
        // make 'admin' owner of shop 77
        String adminToken = userService.loginAsMember("admin","admin","");
        int adminId = userRepository.isUsernameAndPasswordValid("admin","admin");
        Role ownerRole = new Role(adminId, 77, new PermissionsEnum[]{ PermissionsEnum.manageOwners });
        ownerRole.setOwnersPermissions();
        userRepository.addRoleToPending(adminId, ownerRole);
        userRepository.acceptRole(adminId, ownerRole);

        // should succeed
        Map<Integer, PermissionsEnum[]> perms = userService.getPermitionsByShop(adminToken, 77);
        assertTrue(perms.containsKey(adminId));

       
    }


/*
    @Test
    void testConcurrentShoppingCartModifications() throws InterruptedException {
        userRepository.addMember("buyer", "pass", "buyer@test.com", "123", "addr");
        int buyerId = userRepository.isUsernameAndPasswordValid("buyer", "pass");

        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(100);

        for (int i = 0; i < 50; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    userRepository.addItemToShoppingCart(buyerId, 1, finalI, 1);
                } finally {
                    latch.countDown();
                }
            });
            executor.submit(() -> {
                try {
                    userRepository.clearShoppingCart(buyerId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Validate: shopping cart is still consistent (either empty or with items)
        Map<Integer, Integer> basket = userRepository.getBasket(buyerId, 1);
        assertNotNull(basket);
    }
    */
}
