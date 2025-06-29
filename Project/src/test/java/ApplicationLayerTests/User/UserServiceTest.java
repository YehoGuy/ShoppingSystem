package ApplicationLayerTests.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.AbstractMessageChannel;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.NotificationService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.IUserRepository;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.Notification;
import com.example.app.DomainLayer.ShoppingCart;
import com.example.app.DomainLayer.User;
import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.InfrastructureLayer.AuthTokenRepository;
import com.example.app.InfrastructureLayer.UserRepository;

import org.mockito.quality.Strictness;

import com.example.app.InfrastructureLayer.WSEPPay;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    private UserRepository userRepository;
    private AuthTokenRepository authTokenRepository;
    private AuthTokenService authTokenService;
    private UserService userService;

    @InjectMocks
    private NotificationService notificationService;

    @Mock(lenient = true)
    SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        String adminUsername = "admin";
        String adminPlainPassword = "admin";
        String adminEmail = "admin@mail.com";
        String adminPhoneNumber = "0";
        String adminAddress = "admin st.";

        authTokenRepository = new AuthTokenRepository(); // Your real repo
        authTokenService = new AuthTokenService(authTokenRepository); // Real service
        userRepository = new UserRepository(adminUsername, adminPlainPassword, adminEmail, adminPhoneNumber,
                adminAddress);
        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));
        notificationService = mock(NotificationService.class);
        userService = new UserService(userRepository, authTokenService, notificationService);
        notificationService.setService(userService);

        userRepository.setEncoderToTest(true);
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
        // Mock the PaymentMethod for testing
        PaymentMethod paymentMethod = Mockito.mock(PaymentMethod.class);
        userService.addMember("john", "snow", "aaa@gmail.com", "123457890", "address");
        String token = userService.loginAsMember("john", "snow", "");
        userService.setPaymentMethod(token, paymentMethod, 1);
        when(paymentMethod.processPayment(anyDouble(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString()))
                .thenReturn(10005); // Simulate successful payment processing
        // Verify that the payment was processed correctly
        int id = (userService.pay(token, 1, 100.0, "cardNumber", "expiryDate", "cvv", "holderName", "holderID",
                "address", "zipCode"));
        assertTrue(id >= 10000 && id <= 100000);
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
        userService = new UserService(mockRepo, authTokenService, notificationService);
        // stub to throw low‐level error
        Mockito.doThrow(new RuntimeException("DB error"))
                .when(mockRepo).setSuspended(Mockito.eq(42), Mockito.any(LocalDateTime.class));
        // calling setSuspended should be wrapped in OurRuntime
        OurRuntime ex = assertThrows(OurRuntime.class, () -> userService.setSuspended(42, LocalDateTime.now()));
        assertTrue(ex.getMessage().contains("Error setting suspension for user ID 42: DB error"));
    }

    // --- getNotificationsAndClear ---
    @Test
    void testGetNotificationsAndClearSuccess() throws Exception {
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        NotificationService mockNotify = mock(NotificationService.class);
        UserService svc = new UserService(mockRepo, mockAuth, mockNotify);

        String token = "tok123";
        when(mockAuth.ValidateToken(token)).thenReturn(77);

        svc.getNotificationsAndClear(token);

        verify(mockRepo).getNotificationsAndClear(77);
    }

    @Test
    void testGetNotificationsAndClearFailure() throws Exception {
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        NotificationService mockNotify = mock(NotificationService.class);
        UserService svc = new UserService(mockRepo, mockAuth, mockNotify);

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
        UserService svc = new UserService(mockRepo, authTokenService, notificationService);

        notificationService.setService(svc);

        Member owner = mock(Member.class);
        when(owner.getMemberId()).thenReturn(10);
        when(mockRepo.getOwners(5)).thenReturn(List.of(owner));
        when(mockRepo.getUserById(10)).thenReturn(owner);

        // build the cart as exactly HashMap<Integer,HashMap<Integer,Integer>>
        HashMap<Integer, HashMap<Integer, Integer>> cart = new HashMap<>();
        HashMap<Integer, Integer> items = new HashMap<>();
        items.put(3, 2);
        cart.put(5, items);

        // --- act ---
        svc.purchaseNotification(cart);

        // --- assert ---
        verify(notificationService).sendToUser(10,
                "Item 3 Purchased",
                "Quantity: 2 purchased from your shop ID: 5");
    }

    @Test
    void testPurchaseNotificationFailure() {
        // --- arrange ---
        NotificationService notificationService = mock(NotificationService.class);
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService, notificationService);

        notificationService.setService(svc);

        Member owner = mock(Member.class);
        when(owner.getMemberId()).thenReturn(9);
        when(mockRepo.getOwners(1)).thenReturn(List.of(owner));

        // build the cart with the right types
        HashMap<Integer, HashMap<Integer, Integer>> cart = new HashMap<>();
        HashMap<Integer, Integer> items = new HashMap<>();
        items.put(7, 3);
        cart.put(1, items);

        doThrow(new RuntimeException("oops"))
                .when(notificationService).sendToUser(eq(9), anyString(), anyString());

        // --- act & assert ---
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.purchaseNotification(cart));
        assertTrue(ex.getMessage().contains("purchaseNotification"));
    }

    // --- closeShopNotification ---
    @Test
    void testCloseShopNotificationSuccess() {
        // --- arrange ---
        NotificationService notificationService = mock(NotificationService.class);
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService, notificationService);

        notificationService.setService(svc);

        Member owner = mock(Member.class);
        when(owner.getMemberId()).thenReturn(21);
        when(mockRepo.getOwners(11)).thenReturn(List.of(owner));

        svc.closeShopNotification(11);

        verify(notificationService).sendToUser(21,
                "Shop Closed",
                "Your shop ID: 11 has been closed.");
    }

    @Test
    void testCloseShopNotificationFailure() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService, notificationService);

        when(mockRepo.getOwners(99))
                .thenThrow(new RuntimeException("network"));

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.closeShopNotification(99));
        assertTrue(ex.getMessage().contains("closeShopNotification"));
    }

    // --- removedAppointment ---
    @Test
    void testRemovedAppointmentWithoutShopSuccess() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService, notificationService);

        notificationService.setService(svc);

        Member user = mock(Member.class);
        when(user.getMemberId()).thenReturn(5);
        when(mockRepo.getUserById(5)).thenReturn(user);

        svc.removedAppointment(5, "Dentist", null);
        verify(notificationService).sendToUser(5,
                "Appointment Removed",
                "Your appointment to: Dentist has been removed.");
    }

    @Test
    void testRemovedAppointmentWithShopSuccess() {
        NotificationService notificationService = mock(NotificationService.class);
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService, notificationService);
        notificationService.setService(svc);

        svc.removedAppointment(6, "Checkup", 42);
        verify(notificationService).sendToUser(6,
                "Appointment Removed",
                "Your appointment to: Checkup in the shop 42 has been removed.");
    }

    @Test
    void testRemovedAppointmentFailure() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, authTokenService, notificationService);

        notificationService.setService(svc);

        doThrow(new RuntimeException("boom"))
                .when(notificationService).sendToUser(anyInt(), anyString(), anyString());

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.removedAppointment(8, "X", null));
        assertTrue(ex.getMessage().contains("removedAppointment"));
    }

    // --- messageNotification ---
    @Test
    void testMessageNotificationFromShopSuccess() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(userRepository, authTokenService, notificationService);

        notificationService.setService(svc);

        svc.messageNotification(33, 55, true);
        verify(notificationService).sendToUser(33,
                "Message Received",
                "You have received a new message from the shop (id=55).");
    }

    @Test
    void testMessageNotificationFromUserSuccess() {
        // 1) mock the repo and stub existence
        UserRepository mockRepo = mock(UserRepository.class);
        when(mockRepo.getMemberById(44)).thenReturn(mock(Member.class));
        // (or, if your service does findById(...), stub that:
        // when(mockRepo.findById(44)).thenReturn(Optional.of(new User(44, /*…*/)));

        // 2) pass the mock into the service constructor
        UserService svc = new UserService(mockRepo, authTokenService, notificationService);

        // 3) wire up the notificationService to point back at svc
        notificationService.setService(svc);

        // 4) call the “from user” path
        svc.messageNotification(44, 0, false);

        // 5) verify the notification was sent
        verify(notificationService).sendToUser(
                44,
                "Message Received",
                "You have received a new message from the user (id=44).");
    }

    @Test
    void testMessageNotificationFailure() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(userRepository, authTokenService, notificationService);

        notificationService.setService(svc);

        doThrow(new RuntimeException("failMsg"))
                .when(notificationService).sendToUser(anyInt(), anyString(), anyString());

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.messageNotification(99, 0, false));
        assertTrue(ex.getMessage().contains("messageUserNotification"));
    }

    // --- validation helpers ---
    @Test
    void testIsValidUsernameEmailPhonePasswordAndDetails() {
        // username
        assertTrue(userService.isValidUsername("user_123"));
        assertFalse(userService.isValidUsername("ab")); // too short
        assertFalse(userService.isValidUsername("bad!name")); // invalid char

        // email
        assertTrue(userService.isValidEmail("x@y.com"));
        assertFalse(userService.isValidEmail("no-at-sign"));

        // phone
        assertTrue(userService.isValidPhoneNumber("+123-456789"));
        assertFalse(userService.isValidPhoneNumber("1234")); // too short

        // password (always true in test mode)
        assertTrue(userService.isValidPassword("anything"));

        // combined details
        OurRuntime ex = assertThrows(OurRuntime.class, () -> userService.isValidDetails("ab", "", "bademail", "1234"));
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
        userService.addMember("bob", "pwd", "b@b.com", "0123456789", "addr");
        int bobId = userRepository.isUsernameAndPasswordValid("bob", "pwd");
        String bobToken = authTokenService.Login("bob", "pwd", bobId);
        assertThrows(OurRuntime.class, () -> userService.getAllAdmins(bobToken));
    }

    // --- loginAsMember merges guest cart ---
    @Test
    void testLoginAsMemberMergesGuestCart() {
        // create a guest, add to cart
        String guestToken = userService.loginAsGuest();
        int guestId = authTokenRepository.getUserIdByToken(guestToken);
        userService.addItemToShoppingCart(guestToken, 1, 101, 2);

        // create a real member
        userService.addMember("alice", "pwd", "a@a.com", "0123456789", "addr");
        int aliceId = userRepository.isUsernameAndPasswordValid("alice", "pwd");

        // login as member, merging
        String aliceToken = userService.loginAsMember("alice", "pwd", guestToken);
        assertEquals(aliceId, authTokenRepository.getUserIdByToken(aliceToken));
        assertFalse(userRepository.isGuestById(guestId), "old guest should be removed");

        Map<Integer, Integer> basket = userService.getBasketItems(aliceToken, 1);
        assertEquals(2, basket.get(101).intValue());
    }

    // --- shopping cart ops by userId ---
    @Test
    void testGetClearAndRestoreUserShoppingCartById() {
        userService.addMember("tom", "pwd", "t@t.com", "0123456789", "addr");
        int tomId = userRepository.isUsernameAndPasswordValid("tom", "pwd");

        // seed via repository directly
        userRepository.addItemToShoppingCart(tomId, 2, 201, 3);
        HashMap<Integer, HashMap<Integer, Integer>> cart = userService.getUserShoppingCartItems(tomId);
        assertEquals(3, cart.get(2).get(201).intValue());

        // clear → the entire cart should now be empty
        userService.clearUserShoppingCart(tomId);
        HashMap<Integer, HashMap<Integer, Integer>> empty = userService.getUserShoppingCartItems(tomId);
        assertTrue(empty.isEmpty(), "After clearing, no baskets should remain");

        // restore
        HashMap<Integer, HashMap<Integer, Integer>> restore = new HashMap<>();
        HashMap<Integer, Integer> items = new HashMap<>();
        items.put(202, 4);
        restore.put(3, items);
        userService.restoreUserShoppingCart(tomId, restore);
        HashMap<Integer, HashMap<Integer, Integer>> after = userService.getUserShoppingCartItems(tomId);
        assertEquals(4, after.get(3).get(202).intValue());
    }

    // --- getUserPaymentMethod and setPaymentMethod ---
    @Test
    void testGetUserPaymentMethod() {
        userService.addMember("sam", "pwd", "s@m.com", "0123456789", "addr");
        int samId = userRepository.isUsernameAndPasswordValid("sam", "pwd");
        assertNull(userService.getUserPaymentMethod(samId));

        String samToken = userService.loginAsMember("sam", "pwd", "");
        PaymentMethod pm = Mockito.mock(PaymentMethod.class);
        userService.setPaymentMethod(samToken, pm, 5);
        assertEquals(pm, userService.getUserPaymentMethod(samId));
    }

    // --- suspend and suspendedUsers ---
    @Test
    void testSetAndGetSuspended() {
        userService.addMember("lucy", "pwd", "l@l.com", "0123456789", "addr");
        int lucyId = userRepository.isUsernameAndPasswordValid("lucy", "pwd");
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
        String adminToken = userService.loginAsMember("admin", "admin", "");
        int adminId = userRepository.isUsernameAndPasswordValid("admin", "admin");
        Role ownerRole = new Role(adminId, 77, new PermissionsEnum[] { PermissionsEnum.manageOwners });
        ownerRole.setOwnersPermissions();
        userRepository.addRoleToPending(adminId, ownerRole);
        userRepository.acceptRole(adminId, ownerRole);

        // should succeed
        Map<Integer, PermissionsEnum[]> perms = userService.getPermitionsByShop(adminToken, 77);
        assertTrue(perms.containsKey(adminId));

    }

    // --- changePermissions ---
    @Test
    void testChangePermissions_Success() throws Exception {
        // arrange
        String token = "tok";
        int ownerId = 1,
                targetId = 2,
                shopId = 5;
        PermissionsEnum[] newPerms = { PermissionsEnum.manageItems };

        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        NotificationService mockNotify = mock(NotificationService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(ownerId);
        when(mockRepo.isSuspended(ownerId)).thenReturn(false);
        when(mockRepo.isOwner(ownerId, shopId)).thenReturn(true);

        // set up a “pending” role on the target member, assigned by ownerId
        Member member = mock(Member.class);
        Role existingRole = new Role(ownerId, shopId, new PermissionsEnum[] { PermissionsEnum.manageItems });
        when(mockRepo.getMemberById(targetId)).thenReturn(member);
        when(member.getRoles()).thenReturn(List.of(existingRole));

        UserService svc = new UserService(mockRepo, mockAuth, mockNotify);

        // act + assert no exception
        assertDoesNotThrow(() -> svc.changePermissions(token, targetId, shopId, newPerms));

        // verify we passed exactly that Role instance into setPermissions
        verify(mockRepo).setPermissions(
                eq(targetId),
                eq(shopId),
                eq(existingRole),
                eq(newPerms));
    }

    @Test
    void testChangePermissions_NotOwner() throws Exception {
        String token = "tok";
        int userId = 1, targetId = 2, shopId = 5;

        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        NotificationService mockNotify = mock(NotificationService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(userId);
        when(mockRepo.isOwner(userId, shopId)).thenReturn(false);

        UserService svc = new UserService(mockRepo, mockAuth, mockNotify);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.changePermissions(token, targetId, shopId,
                        new PermissionsEnum[] { PermissionsEnum.manageItems }));
        assertTrue(ex.getMessage().contains("not an owner"));
    }

    // --- removePermission ---
    @Test
    void testRemovePermission_Success() throws Exception {
        String token = "t";
        int assignee = 1,
                targetId = 2,
                shopId = 7;
        PermissionsEnum perm = PermissionsEnum.handleMessages;

        // --- arrange mocks ---
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        NotificationService mockNotify = mock(NotificationService.class);

        // token → assignee
        when(mockAuth.ValidateToken(token)).thenReturn(assignee);
        // not suspended
        when(mockRepo.isSuspended(assignee)).thenReturn(false);

        // make targetId a “real” Member for validateMemberId(...)
        Member member = mock(Member.class);
        Map<Integer, User> mapping = new HashMap<>();
        mapping.put(targetId, member);
        when(mockRepo.getUserMapping()).thenReturn(mapping);
        when(mockRepo.getUserById(targetId)).thenReturn(member);

        // repository has exactly one Role for (targetId, shopId), assigned by our
        // assignee
        Role role = new Role(assignee, shopId, new PermissionsEnum[] { perm });
        when(mockRepo.getRole(targetId, shopId)).thenReturn(role);

        UserService svc = new UserService(mockRepo, mockAuth, mockNotify);

        // --- act & assert ---
        boolean result = svc.removePermission(token, targetId, perm, shopId);
        assertTrue(result);

        // verify we called through to the repo
        verify(mockRepo).removePermission(targetId, perm, shopId);
    }

    @Test
    void testRemovePermission_NullPermission() throws Exception {
        String token = "t";
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        NotificationService mockNotify = mock(NotificationService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(1);

        UserService svc = new UserService(mockRepo, mockAuth, mockNotify);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removePermission(token, 2, null, 7));
        assertTrue(ex.getMessage().contains("Permission cannot be null"));
    }

    // --- removeRole ---
    @Test
    void testRemoveRole_Success() {
        int id = 3;
        int shopId = 8;

        // our Role’s constructor takes (assigneeId, shopId, perms),
        // so by passing assigneeId == id the "assignee" check passes:
        Role role = new Role(id, shopId, null);

        // mocks
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        NotificationService mockNotify = mock(NotificationService.class);

        // make validateMemberId(id) pass:
        Member dummyMember = mock(Member.class);
        when(mockRepo.getUserMapping()).thenReturn(Map.of(id, dummyMember));
        when(mockRepo.getUserById(id)).thenReturn(dummyMember);

        // make sure isSuspended(id) → false
        when(mockRepo.isSuspended(id)).thenReturn(false);

        // getRole(id, shopId) returns our same role
        when(mockRepo.getRole(id, shopId)).thenReturn(role);

        UserService svc = new UserService(mockRepo, mockAuth, mockNotify);

        // exercise
        boolean ok = svc.removeRole(id, role);

        // verify
        assertTrue(ok);
        verify(mockRepo).removeRole(id, shopId);
    }

    @Test
    void testRemoveRole_NullRole() {
        UserService svc = new UserService(mock(UserRepository.class), mock(AuthTokenService.class),
                notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.removeRole(1, null));
        assertTrue(ex.getMessage().contains("Role cannot be null"));
    }

    // --- makeManagerOfStore & removeManagerFromStore ---
    @Test
    void testMakeAndRemoveManager_Success() throws Exception {
        String token = "a";
        int ownerId = 10, mgrId = 20, shopId = 30;
        PermissionsEnum[] perms = { PermissionsEnum.handleMessages };

        // mocks
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        NotificationService mockNotify = mock(NotificationService.class);

        // auth + owner check
        when(mockAuth.ValidateToken(token)).thenReturn(ownerId);
        when(mockRepo.isOwner(ownerId, shopId)).thenReturn(true);

        // clear‐out anything in removeManagerFromStore
        when(mockRepo.getRole(mgrId, shopId))
                .thenReturn(new Role(ownerId, shopId, perms)); // assignee == ownerId
        // no members list needed here, removeAllAssigned is only in removeOwner...

        UserService svc = new UserService(mockRepo, mockAuth, mockNotify);

        // 1) make manager
        assertDoesNotThrow(() -> svc.makeManagerOfStore(token, mgrId, shopId, perms));
        verify(mockRepo).addRoleToPending(eq(mgrId), any(Role.class));

        // 2) remove manager
        assertDoesNotThrow(() -> svc.removeManagerFromStore(token, mgrId, shopId));
        verify(mockRepo).removeRole(mgrId, shopId);
    }

    // --- makeStoreOwner & removeOwnerFromStore ---
    @Test
    void testMakeAndRemoveStoreOwner_Success() throws Exception {
        String token = "tk";
        int owner = 1, newOwner = 2, shopId = 50;

        // mocks
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        NotificationService mockNotify = mock(NotificationService.class);

        // auth + isOwner
        when(mockAuth.ValidateToken(token)).thenReturn(owner);
        when(mockRepo.isOwner(owner, shopId)).thenReturn(true);
        // member existence check
        when(mockRepo.getMemberById(newOwner)).thenReturn(mock(Member.class));
        // stub out downstream of removeOwner
        when(mockRepo.getMembersList()).thenReturn(List.of()); // avoid removeAllAssigned loops

        UserService svc = new UserService(mockRepo, mockAuth, mockNotify);

        // make owner
        assertDoesNotThrow(() -> svc.makeStoreOwner(token, newOwner, shopId));
        verify(mockRepo).addRoleToPending(eq(newOwner), any(Role.class));

        // prepare for removal: getRole(newOwner, shopId) must look “owner”
        Role r = new Role(owner, shopId, null);
        r.setOwnersPermissions();
        when(mockRepo.getRole(newOwner, shopId)).thenReturn(r);

        // remove owner
        assertDoesNotThrow(() -> svc.removeOwnerFromStore(token, newOwner, shopId));
        verify(mockRepo).removeRole(newOwner, shopId);
    }

    // --- hasRole ---
    @Test
    void testHasRole_Success() {
        int id = 5, shopId = 60;
        // build Role so that getAssigneeId()==id
        Role r = new Role(id, shopId, null);

        // mocks
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        NotificationService mockNotify = mock(NotificationService.class);

        // make validateMemberId(id) pass
        Member fakeMember = mock(Member.class);
        when(mockRepo.getUserMapping()).thenReturn(Map.of(id, fakeMember));
        when(mockRepo.getUserById(id)).thenReturn(fakeMember);
        when(mockRepo.isSuspended(id)).thenReturn(false);

        // stub out getRole
        when(mockRepo.getRole(id, shopId)).thenReturn(r);

        UserService svc = new UserService(mockRepo, mockAuth, mockNotify);

        // exercise
        boolean result = svc.hasRole(id, r);

        // verify
        assertTrue(result);
    }

    @Test
    void testHasRole_NullRole() {
        UserService svc = new UserService(mock(UserRepository.class), mock(AuthTokenService.class),
                notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.hasRole(1, null));
        assertTrue(ex.getMessage().contains("Role cannot be null"));
    }

    // --- acceptRole & declineRole ---
    @Test
    void testAcceptDeclineRole_Success() throws Exception {
        String token = "tk";
        int memberId = 3, shopId = 77;
        Role r = new Role(memberId, shopId, null);

        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(memberId);
        when(mockRepo.getPendingRole(memberId, shopId)).thenReturn(r);

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.acceptRole(token, shopId));
        verify(mockRepo).acceptRole(memberId, r);

        when(mockRepo.getPendingRole(memberId, shopId)).thenReturn(r);
        assertDoesNotThrow(() -> svc.declineRole(token, shopId));
        verify(mockRepo).declineRole(memberId, r);
    }

    // --- refundPaymentByStoreEmployee ---
    @Test
    void testRefundPaymentByStoreEmployee_Success() throws Exception {
        String token = "t";
        int emp = 4, uid = 5, shopId = 6;

        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(emp);
        when(mockRepo.getRole(emp, shopId)).thenReturn(new Role(emp, shopId, null));

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        boolean ok = svc.refundPaymentByStoreEmployee(token, uid, shopId, 12);
        assertTrue(ok);
        verify(mockRepo).refund(uid, 12);
    }

    @Test
    void testRefundPaymentByStoreEmployee_NoRole() throws Exception {
        String token = "t";
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(1);
        when(mockRepo.getRole(1, 9)).thenReturn(null);

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.refundPaymentByStoreEmployee(token, 2, 9, 5));
        assertTrue(ex.getMessage().contains("has no role"));
    }

    // --- updateItemQuantityInShoppingCart ---
    @Test
    void testUpdateItemQuantityInShoppingCart_Success() throws Exception {
        String token = "t";
        int userId = 2, shopId = 3, itemId = 4, qty = 7;

        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(userId);

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.updateItemQuantityInShoppingCart(token, shopId, itemId, qty));
        verify(mockRepo).updateItemQuantityInShoppingCart(userId, shopId, itemId, qty);
    }

    // --- removeAllAssigned (public) ---
    @Test
    void testRemoveAllAssigned_Success() {
        int assignee = 5, shopId = 9;
        // create a role whose assigneeId == 5
        Role child = new Role(assignee, shopId, null);

        Member m = mock(Member.class);
        when(m.getRoles()).thenReturn(List.of(child));
        when(m.getMemberId()).thenReturn(6);

        UserRepository mockRepo = mock(UserRepository.class);
        when(mockRepo.getMembersList()).thenReturn(List.of(m));

        // authTokenService isn’t used here, so just stub one in
        UserService svc = new UserService(mockRepo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        svc.removeAllAssigned(assignee, shopId);

        // should have removed exactly that role
        verify(mockRepo).removeRole(6, shopId);
    }

    // --- refundPaymentAuto ---
    @Test
    void testRefundPaymentAuto_Success() throws Exception {
        String token = "t";
        int userId = 3, shopId = 4;

        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(userId);

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        boolean ok = svc.refundPaymentAuto(token, 1);
        assertTrue(ok);
        verify(mockRepo).refund(userId, 1);
    }

    // --- addBasket ---
    @Test
    void testAddBasket_Success() throws Exception {
        String token = "t";
        int userId = 7, shopId = 8;
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(userId);

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.addBasket(token, shopId));
        verify(mockRepo).createBasket(userId, shopId);
    }

    // --- getPendingRoles ---
    @Test
    void testGetPendingRoles_Success() throws Exception {
        String token = "tk";
        int userId = 12;
        Role r1 = new Role(userId, 101, null);

        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(userId);
        when(mockRepo.getPendingRoles(userId)).thenReturn(List.of(r1));

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        List<Role> lst = svc.getPendingRoles(token);
        assertEquals(1, lst.size());
        assertSame(r1, lst.get(0));
    }

    // --- clearShoppingCart ---
    @Test
    void testClearShoppingCart_Success() throws Exception {
        String token = "t";
        int userId = 15;
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(userId);

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.clearShoppingCart(token));
        verify(mockRepo).clearShoppingCart(userId);
    }

    // --- getUserShippingAddress ---
    @Test
    void testGetUserShippingAddress_Success() {
        int userId = 20;
        com.example.app.DomainLayer.Purchase.Address addr = mock(com.example.app.DomainLayer.Purchase.Address.class);

        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockRepo.getUserById(userId)).thenReturn(mock(com.example.app.DomainLayer.User.class));
        when(mockRepo.getUserById(userId).getAddress()).thenReturn(addr);

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        assertSame(addr, svc.getUserShippingAddress(userId));
    }

    // --- getShopIdsByWorkerId & getShopMembers ---
    @Test
    void testShopQueries() {
        int userId = 30, shopId = 31;
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockRepo.getShopIdsByWorkerId(userId)).thenReturn(List.of(shopId));
        Member m = mock(Member.class);
        when(mockRepo.getShopMembers(shopId)).thenReturn(List.of(m));

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        assertTrue(svc.getShopIdsByWorkerId(userId).contains(shopId));
        assertTrue(svc.getShopMembers(shopId).contains(m));
    }

    // --- updateMemberAddress ---
    @Test
    void testUpdateMemberAddress_Success() throws Exception {
        String token = "tok";
        int userId = 42;
        String city = "Metropolis", street = "Main St", postal = "12345";
        int apt = 99;

        // mocks
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);

        // token → userId
        when(mockAuth.ValidateToken(token)).thenReturn(userId);
        // make validateMemberId pass
        Member realMember = mock(Member.class);
        when(mockRepo.getUserMapping()).thenReturn(Map.of(userId, realMember));
        when(mockRepo.getUserById(userId)).thenReturn(realMember);

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        // should not throw
        assertDoesNotThrow(() -> svc.updateMemberAddress(token, city, street, apt, postal));
        verify(mockRepo).updateMemberAddress(userId, city, street, apt, postal);
    }

    @Test
    void testUpdateMemberAddress_Failure_InvalidMember() throws Exception {
        String token = "tok";
        // mock repo with no mapping → validateMemberId will fail
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(7);

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class,
                () -> svc.updateMemberAddress(token, "C", "S", 1, "P"));
        assertTrue(ex.getMessage().contains("updateMemberAddress"));
    }

    // --- getAcceptedRoles ---
    @Test
    void testGetAcceptedRoles_Success() throws Exception {
        String token = "tk";
        int userId = 5;
        Role r1 = new Role(userId, 99, null);

        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(userId);
        when(mockRepo.getAcceptedRoles(userId)).thenReturn(List.of(r1));

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        List<Role> result = svc.getAcceptedRoles(token);
        assertEquals(1, result.size());
        assertSame(r1, result.get(0));
    }

    @Test
    void testGetAcceptedRoles_Failure_NoPending() throws Exception {
        String token = "tk";
        int userId = 5;

        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(userId);
        when(mockRepo.getAcceptedRoles(userId)).thenThrow(new RuntimeException("db"));

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.getAcceptedRoles(token));
        assertTrue(ex.getMessage().contains("getAcceptedRoles"));
    }

    // --- getAllMembers ---
    @Test
    void testGetAllMembers_Success() {
        Member m1 = mock(Member.class), m2 = mock(Member.class);
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService dummyAuth = mock(AuthTokenService.class);

        when(mockRepo.getAllMembers()).thenReturn(List.of(m1, m2));
        UserService svc = new UserService(mockRepo, dummyAuth, notificationService);
        notificationService.setService(svc);

        List<Member> all = svc.getAllMembers();
        assertEquals(2, all.size());
        assertTrue(all.contains(m1) && all.contains(m2));
    }

    @Test
    void testGetAllMembers_Failure() {
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService dummyAuth = mock(AuthTokenService.class);
        when(mockRepo.getAllMembers()).thenThrow(new RuntimeException("boom"));

        UserService svc = new UserService(mockRepo, dummyAuth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                svc::getAllMembers);
        assertTrue(ex.getMessage().contains("getAllUsers"));
    }

    @Test
    void testAddRemoveItemAndGetBasket() {
        String token = userService.loginAsGuest();
        // Add item
        userService.addItemToShoppingCart(token, 1, 200, 5);
        Map<Integer, Integer> basket = userService.getBasketItems(token, 1);
        assertEquals(5, basket.get(200).intValue());
        // Remove item
        userService.removeItemFromShoppingCart(token, 1, 200);
        basket = userService.getBasketItems(token, 1);
        assertFalse(basket.containsKey(200));
    }

    @Test
    void testGetBasketItemsInvalidToken() {
        assertThrows(OurRuntime.class, () -> userService.getBasketItems("invalid", 1));
    }

    @Test
    void testLoginAsMemberNullUsername() {
        assertThrows(OurArg.class, () -> userService.loginAsMember(null, "pass", ""));
    }

    @Test
    void testLoginAsMemberEmptyPassword() {
        assertThrows(OurArg.class, () -> userService.loginAsMember("user", "", ""));
    }

    @Test
    void testLoginAsMemberInvalidCredentials() {
        // No such user
        assertThrows(OurArg.class, () -> userService.loginAsMember("noone", "wrong", ""));
    }

    @Test
    void testIsValidPasswordNullOrEmpty() {
        assertFalse(userService.isValidPassword(null));
        assertFalse(userService.isValidPassword(""));
        assertTrue(userService.isValidPassword("nonempty"));
    }

    @Test
    void testLogoutInvalidTokenReturnsNull() {
        String result = userService.logout("bad_token");
        assertNull(result);
    }

    @Test
    void testIsValidPassword_NullOrEmpty() {
        assertFalse(userService.isValidPassword(null));
        assertFalse(userService.isValidPassword(""));
    }

    @Test
    void testIsValidUsername_EdgeCases() {
        assertFalse(userService.isValidUsername(null));
        assertFalse(userService.isValidUsername(""));
        assertFalse(userService.isValidUsername("a"));
        assertFalse(userService.isValidUsername("toolongusername_exceedslimit"));
        assertTrue(userService.isValidUsername("user_name123"));
    }

    @Test
    void testIsValidPhoneNumber_EdgeCases() {
        assertFalse(userService.isValidPhoneNumber(null));
        assertFalse(userService.isValidPhoneNumber(""));
        assertFalse(userService.isValidPhoneNumber("123"));
        assertFalse(userService.isValidPhoneNumber("+1234567890123456"));
        assertTrue(userService.isValidPhoneNumber("+1234567890"));
        assertTrue(userService.isValidPhoneNumber("123456789"));
    }

    @Test
    void testIsValidEmail_EdgeCases() {
        assertFalse(userService.isValidEmail(null));
        assertFalse(userService.isValidEmail(""));
        assertFalse(userService.isValidEmail("user@"));
        assertFalse(userService.isValidEmail("@domain.com"));
        assertTrue(userService.isValidEmail("user.name+tag@sub.example.co.uk"));
    }

    @Test
    void testLogout_InvalidToken() {
        assertNull(userService.logout("invalid-token"));
    }

    @Test
    void testAddItemAndRemoveItemShoppingCart() throws Exception {
        userService.addMember("joe", "pwd", "joe@mail.com", "0123456789", "addr");
        int joeId = userRepository.isUsernameAndPasswordValid("joe", "pwd");
        String token = userService.loginAsMember("joe", "pwd", "");

        userService.addItemToShoppingCart(token, 5, 42, 3);
        Map<Integer, Integer> basket = userService.getBasketItems(token, 5);
        assertEquals(3, basket.get(42).intValue());

        userService.removeItemFromShoppingCart(token, 5, 42);
        assertTrue(userService.getBasketItems(token, 5).isEmpty());
    }

    @Test
    void testSetPaymentMethod_NullThrows() throws Exception {
        userService.addMember("sam", "pwd", "s@m.com", "0123456789", "addr");
        String token = userService.loginAsMember("sam", "pwd", "");
        OurRuntime ex = assertThrows(OurRuntime.class, () -> userService.setPaymentMethod(token, null, 1));
        assertTrue(ex.getMessage().contains("Payment method cannot be null"));
    }

    @Test
    void testPay_SuspendedUser() {
        userService.addMember("max", "pwd", "m@m.com", "0123456789", "addr");
        int id = userRepository.isUsernameAndPasswordValid("max", "pwd");
        String token = userService.loginAsMember("max", "pwd", "");
        userService.setSuspended(id, LocalDateTime.now().plusDays(1));
        OurRuntime ex = assertThrows(OurRuntime.class, () -> userService.pay(token, 1, 50.0, "cardNumber", "expiryDate",
                "cvv", "holderName", "holderID", "address", "zipCode"));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    @Test
    void testAddPermission_Success() throws Exception {
        String token = "tok";
        int assignee = 1,
                target = 2,
                shopId = 7;
        PermissionsEnum perm = PermissionsEnum.manageItems;

        // mocks
        var mockRepo = mock(UserRepository.class);
        var mockAuth = mock(AuthTokenService.class);

        // auth + not‐suspended
        when(mockAuth.ValidateToken(token)).thenReturn(assignee);
        when(mockRepo.isSuspended(assignee)).thenReturn(false);

        // make validateMemberId(target) pass:
        Member targetMember = mock(Member.class);
        when(mockRepo.getUserMapping()).thenReturn(Map.of(target, targetMember));
        when(mockRepo.getUserById(target)).thenReturn(targetMember);

        // stub getRole(...) so it looks like our assignee assigned the role:
        Role existingRole = new Role(assignee, shopId, new PermissionsEnum[] {});
        when(mockRepo.getRole(target, shopId)).thenReturn(existingRole);

        // exercise
        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        boolean result = svc.addPermission(token, target, perm, shopId);

        // verify
        assertTrue(result);
        verify(mockRepo).addPermission(target, perm, shopId);
    }

    @Test
    void testAddPermission_NullPermission() throws Exception {
        String token = "t";
        var mockRepo = mock(UserRepository.class);
        var mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken(token)).thenReturn(1);

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.addPermission(token, 2, null, 1));
        assertTrue(ex.getMessage().contains("Permission cannot be null"));
    }

    @Test
    void testHasPermission_UserNotExist() {
        assertFalse(userService.hasPermission(9999, PermissionsEnum.closeShop, 1));
    }

    @Test
    void testAddMember_InvalidDetails() {
        AuthTokenService auth = new AuthTokenService(null);
        UserService svc = new UserService(userRepository, auth, notificationService);
        notificationService.setService(svc);

        assertThrows(com.example.app.ApplicationLayer.OurRuntime.class,
                () -> svc.addMember("a", "b", "bademail", "123", "addr"));
    }

    @Test
    void testLoginAsMember_NullUsernameOrPassword() {
        UserService svc = new UserService(mock(UserRepository.class), mock(AuthTokenService.class),
                notificationService);
        notificationService.setService(svc);
        assertThrows(com.example.app.ApplicationLayer.OurArg.class,
                () -> svc.loginAsMember(null, "pass", "guestToken"));
        assertThrows(java.lang.IllegalArgumentException.class,
                () -> svc.loginAsMember("user", null, "guestToken"));
    }

    @Test
    void testLoginAsMember_InvalidCredentials() {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(repo.isUsernameAndPasswordValid("no", "user")).thenReturn(-1);
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        assertThrows(com.example.app.ApplicationLayer.OurArg.class,
                () -> svc.loginAsMember("no", "user", ""));
    }

    @Test
    void testLogout_InvalidTokenReturnsNull() {
        UserService svc = new UserService(mock(UserRepository.class), mock(AuthTokenService.class),
                notificationService);
        notificationService.setService(svc);
        assertNull(svc.logout("invalid-token"));
    }

    @Test
    void testAddItemToShoppingCart_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(1);
        when(repo.isSuspended(1)).thenReturn(true);
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.addItemToShoppingCart(token, 1, 1, 1));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    @Test
    void testRemoveItemFromShoppingCart_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(2);
        when(repo.isSuspended(2)).thenReturn(true);
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeItemFromShoppingCart(token, 1, 1));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    @Test
    void testClearShoppingCart_StillClearsWhenSuspended() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(4);
        when(repo.isSuspended(4)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        // Even if suspended, clearShoppingCart should still call through
        assertDoesNotThrow(() -> svc.clearShoppingCart(token));
        verify(repo).clearShoppingCart(4);
    }

    @Test
    void testUpdateMemberUsername_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(99);
        when(repo.isSuspended(99)).thenReturn(true);
        when(repo.getUserMapping()).thenReturn(Map.of(99, mock(Member.class)));
        when(repo.getUserById(99)).thenReturn(mock(Member.class));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.updateMemberUsername(token, "newuser"));
        assertTrue(ex.getMessage().contains("updateMemberUsername"));
    }

    @Test
    void testChangePermissions_NullPermission() throws Exception {
        String token = "tok";
        int owner = 1, target = 2, shopId = 5;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(owner);
        when(repo.isSuspended(owner)).thenReturn(false);
        when(repo.isOwner(owner, shopId)).thenReturn(true);
        Member member = mock(Member.class);
        Role r = new Role(owner, shopId, new PermissionsEnum[] { PermissionsEnum.manageItems });
        when(repo.getMemberById(target)).thenReturn(member);
        when(member.getRoles()).thenReturn(java.util.List.of(r));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.changePermissions(token, target, shopId, new PermissionsEnum[] { null }));
        assertTrue(ex.getMessage().contains("Permission cannot be null"));
    }

    @Test
    void testGetBasketItems_ThrowsOnRepoError() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(7);
        when(repo.getBasket(7, 1)).thenThrow(new RuntimeException("boom"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.getBasketItems(token, 1));
        assertTrue(ex.getMessage().contains("getBasketItems"));
    }

    // --- updateItemQuantityInShoppingCart ---
    @Test
    void testUpdateItemQuantity_ThrowsWhenSuspended() throws Exception {
        String token = "t";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(2);
        when(repo.isSuspended(2)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.updateItemQuantityInShoppingCart(token, 5, 6, 7));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    // --- getAllMembers empty list ---
    @Test
    void testGetAllMembers_EmptyList() {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(repo.getAllMembers()).thenReturn(List.of());

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        List<Member> all = svc.getAllMembers();
        assertTrue(all.isEmpty());
    }

    // --- removeRole throws when not assignee ---
    @Test
    void testRemoveRole_ThrowsWhenNotAssignee() {
        int userId = 10, shopId = 20;
        // The "parameter" role is assumed to have been assigned by userId,
        // but the existing role in the repo was assigned by 5 instead.
        Role paramRole = new Role(userId, shopId, null);
        Role existingRole = new Role(5, shopId, null);

        UserRepository repo = mock(UserRepository.class);
        // Make validateMemberId pass:
        Member member = mock(Member.class);
        when(repo.getUserMapping()).thenReturn(Map.of(userId, member));
        when(repo.getUserById(userId)).thenReturn(member);
        // Not suspended:
        when(repo.isSuspended(userId)).thenReturn(false);
        // Repo returns an existing role with a different assignee:
        when(repo.getRole(userId, shopId)).thenReturn(existingRole);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.removeRole(userId, paramRole));
        assertTrue(ex.getMessage().contains("is not the assignee of the role"));
    }

    // --- addPermission throws when suspended ---
    @Test
    void testAddPermission_ThrowsWhenSuspended() throws Exception {
        String token = "t";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(7);
        when(repo.isSuspended(7)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.addPermission(token, 8, PermissionsEnum.manageItems, 9));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    // --- updateMemberPassword success & suspended ---
    @Test
    void testUpdateMemberPassword_Success() throws Exception {
        String token = "tok";
        int id = 11;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        Member m = mock(Member.class);

        when(auth.ValidateToken(token)).thenReturn(id);
        when(repo.isSuspended(id)).thenReturn(false);
        when(repo.getUserMapping()).thenReturn(Map.of(id, m));
        when(repo.getUserById(id)).thenReturn(m);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.updateMemberPassword(token, "newpass"));
        verify(repo).updateMemberPassword(eq(id), anyString());
    }

    // --- messageNotification throws on repo error ---
    @Test
    void testMessageNotificationFromShop_ThrowsOnError() {
        UserRepository repo = mock(UserRepository.class);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        doThrow(new RuntimeException("err"))
                .when(notificationService).sendToUser(anyInt(), anyString(), anyString());

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.messageNotification(13, 14, true));
        assertTrue(ex.getMessage().contains("messageUserNotification"));
    }

    // --- removeAllAssigned multiple members ---
    @Test
    void testRemoveAllAssigned_MultipleLevels() {
        int assignee = 2, shop = 3;
        Role r1 = new Role(assignee, shop, null);
        Role r2 = new Role(assignee, shop, null);
        Member m1 = mock(Member.class), m2 = mock(Member.class);
        when(m1.getMemberId()).thenReturn(4);
        when(m2.getMemberId()).thenReturn(5);
        when(m1.getRoles()).thenReturn(List.of(r1));
        when(m2.getRoles()).thenReturn(List.of(r2));

        UserRepository repo = mock(UserRepository.class);
        when(repo.getMembersList()).thenReturn(List.of(m1, m2));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        svc.removeAllAssigned(assignee, shop);
        verify(repo).removeRole(4, shop);
        verify(repo).removeRole(5, shop);
    }

    // --- hasRole returns false when no existing role ---
    @Test
    void testHasRole_ThrowsWhenNoRole() {
        int id = 6, shop = 7;
        Role r = new Role(id, shop, null);

        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenReturn(Map.of(id, mock(Member.class)));
        when(repo.getUserById(id)).thenReturn(mock(Member.class));
        when(repo.isSuspended(id)).thenReturn(false);
        when(repo.getRole(id, shop)).thenReturn(null);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.hasRole(id, r));
        assertTrue(ex.getMessage().contains("has no role"));
    }

    // --- refundPaymentAuto throws on repo error ---
    @Test
    void testRefundPaymentAuto_ThrowsOnRepoError() throws Exception {
        String token = "t";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(8);
        doThrow(new RuntimeException("db"))
                .when(repo).refund(8, 9);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.refundPaymentAuto(token, 9));
        assertTrue(ex.getMessage().contains("refundPayment"));
    }

    // --- updateMemberEmail & phone throws when suspended ---
    @Test
    void testUpdateMemberEmail_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        Member m = mock(Member.class);

        when(auth.ValidateToken(token)).thenReturn(9);
        when(repo.getUserMapping()).thenReturn(Map.of(9, m));
        when(repo.getUserById(9)).thenReturn(m);
        when(repo.isSuspended(9)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.updateMemberEmail(token, "x@y.com"));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    @Test
    void testUpdateMemberPhoneNumber_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        Member m = mock(Member.class);

        when(auth.ValidateToken(token)).thenReturn(10);
        when(repo.getUserMapping()).thenReturn(Map.of(10, m));
        when(repo.getUserById(10)).thenReturn(m);
        when(repo.isSuspended(10)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.updateMemberPhoneNumber(token, "1234567890"));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    // --- removeOwnerFromStore throws when not assignee ---
    @Test
    void testRemoveOwnerFromStore_ThrowsWhenNotAssignee() throws Exception {
        String token = "tok";
        int member = 14, shop = 15;
        Role role = new Role(13, shop, null);

        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(repo.getRole(member, shop)).thenReturn(role);
        when(auth.ValidateToken(token)).thenReturn(16);
        when(repo.isSuspended(16)).thenReturn(false);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeOwnerFromStore(token, member, shop));
        assertTrue(ex.getMessage().contains("removeOwnerFromStore"));
    }

    // --- addBasket does NOT throw when suspended (current behavior) ---
    @Test
    void testAddBasket_DoesNotThrowWhenSuspended() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        // auth.ValidateToken → userId = 17
        when(auth.ValidateToken(token)).thenReturn(17);
        // mark that user as suspended
        when(repo.isSuspended(17)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        // Should not throw, and should still call createBasket
        assertDoesNotThrow(() -> svc.addBasket(token, 21));
        verify(repo).createBasket(17, 21);
    }

    // --- hasPermission false when suspended or no mapping ---
    @Test
    void testHasPermission_ReturnsFalseWhenSuspendedOrNoUser() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenReturn(Map.of());
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        assertFalse(svc.hasPermission(999, PermissionsEnum.manageItems, 1));
    }

    // --- getPendingRoles throws when repo error ---
    @Test
    void testGetPendingRoles_ThrowsOnError() throws Exception {
        String token = "tk";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(18);
        when(repo.getPendingRoles(18)).thenThrow(new RuntimeException("db"));

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.getPendingRoles(token));
        assertTrue(ex.getMessage().contains("getPendingRoles"));
    }

    // --- acceptRole & declineRole throw when no pending ---
    @Test
    void testAcceptRole_ThrowsWhenNoPending() throws Exception {
        String token = "tk";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(19);
        when(repo.getPendingRole(19, 31)).thenReturn(null);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.acceptRole(token, 31));
        assertTrue(ex.getMessage().contains("has no pending role"));
    }

    @Test
    void testDeclineRole_ThrowsWhenNoPending() throws Exception {
        String token = "tk";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(20);
        when(repo.getPendingRole(20, 32)).thenReturn(null);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.declineRole(token, 32));
        assertTrue(ex.getMessage().contains("has no pending role"));
    }

    // --- makeStoreOwner throws when repo error on addRoleToPending ---
    @Test
    void testMakeStoreOwner_ThrowsOnRepoError() throws Exception {
        String token = "tk";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(21);
        when(repo.isOwner(21, 33)).thenReturn(true);
        doThrow(new RuntimeException("db"))
                .when(repo).addRoleToPending(eq(22), any(Role.class));

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.makeStoreOwner(token, 22, 33));
        assertTrue(ex.getMessage().contains("makeStoreOwner"));
    }

    // --- pay ---
    @Test
    void testPay_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(1);
        when(repo.isSuspended(1)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.pay(token, 100, 50.0, "cardNumber", "expiryDate", "cvv", "holderName", "holderID", "address",
                        "zipCode"));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    @Test
    void testPay_ThrowsOnRepoError() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(2);
        when(repo.isSuspended(2)).thenReturn(false);
        doThrow(new RuntimeException("DB fail")).when(repo).pay(2, 75.0, "cardNumber", "expiryDate", "cvv",
                "holderName", "holderID", "address", "zipCode");

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.pay(token, 200, 75.0, "cardNumber", "expiryDate", "cvv", "holderName", "holderID", "address",
                        "zipCode"));
        assertTrue(ex.getMessage().contains("pay: DB fail"));
    }

    // --- updateItemQuantityInShoppingCart ---
    @Test
    void testUpdateItemQuantityInShoppingCart_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(3);
        when(repo.isSuspended(3)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.updateItemQuantityInShoppingCart(token, 10, 5, 2));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    @Test
    void testUpdateItemQuantityInShoppingCart_ThrowsOnRepoError() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(4);
        when(repo.isSuspended(4)).thenReturn(false);
        doThrow(new RuntimeException("Cart fail")).when(repo)
                .updateItemQuantityInShoppingCart(4, 11, 6, 3);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.updateItemQuantityInShoppingCart(token, 11, 6, 3));
        assertTrue(ex.getMessage().contains("updateItemQuantityInShoppingCart: Cart fail"));
    }

    // --- updateMemberAddress ---
    @Test
    void testUpdateMemberAddress_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(5);
        when(repo.isSuspended(5)).thenReturn(true);
        when(repo.getUserMapping()).thenReturn(Map.of(5, mock(com.example.app.DomainLayer.Member.class)));
        when(repo.getUserById(5)).thenReturn(mock(com.example.app.DomainLayer.Member.class));

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.updateMemberAddress(token, "C", "S", 1, "P"));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    // --- removePermission ---
    @Test
    void testRemovePermission_ThrowsWhenNoRole() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(6);
        when(repo.isSuspended(6)).thenReturn(false);
        // no role for target 7/shop 100
        when(repo.getRole(7, 100)).thenReturn(null);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class,
                () -> svc.removePermission(token, 7, PermissionsEnum.manageItems, 100));
        assertTrue(ex.getMessage().contains("removePermission"));
    }

    // --- removeRole ---
    @Test
    void testRemoveRole_ThrowsWhenNoExistingRole() {
        int id = 8, shop = 50;
        Role role = new Role(id, shop, null);

        UserRepository repo = mock(UserRepository.class);
        // valid member mapping
        when(repo.getUserMapping()).thenReturn(Map.of(id, mock(com.example.app.DomainLayer.Member.class)));
        when(repo.getUserById(id)).thenReturn(mock(com.example.app.DomainLayer.Member.class));
        when(repo.isSuspended(id)).thenReturn(false);
        // getRole returns null
        when(repo.getRole(id, shop)).thenReturn(null);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeRole(id, role));
        assertTrue(ex.getMessage().contains("has no role"));
    }

    // --- addPermission ---
    @Test
    void testAddPermission_ThrowsWhenNoRole() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(9);
        when(repo.isSuspended(9)).thenReturn(false);
        when(repo.getRole(10, 200)).thenReturn(null);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class,
                () -> svc.addPermission(token, 10, PermissionsEnum.manageItems, 200));
        assertTrue(ex.getMessage().contains("addPermission"));
    }

    // --- updateMemberPassword ---
    @Test
    void testUpdateMemberPassword_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(11);
        when(repo.isSuspended(11)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.updateMemberPassword(token, "newpass"));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    // --- makeManagerOfStore ---
    @Test
    void testMakeManagerOfStore_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(12);
        when(repo.isSuspended(12)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.makeManagerOfStore(token, 13, 300, new PermissionsEnum[] {}));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    @Test
    void testMakeManagerOfStore_ThrowsWhenNotOwner() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(14);
        when(repo.isSuspended(14)).thenReturn(false);
        when(repo.isOwner(14, 400)).thenReturn(false);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.makeManagerOfStore(token, 15, 400, new PermissionsEnum[] {}));
        assertTrue(ex.getMessage().contains("not an owner"));
    }

    // --- hasPermission ---
    @Test
    void testHasPermission_ReturnsFalseWhenSuspended() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenReturn(Map.of(19, mock(com.example.app.DomainLayer.Member.class)));
        when(repo.isSuspended(19)).thenReturn(true);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        boolean result = svc.hasPermission(19, PermissionsEnum.closeShop, 600);
        assertFalse(result);
    }

    // --- acceptRole & declineRole ---
    @Test
    void testAcceptRole_ThrowsWhenNoPendingRole() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(20);
        when(repo.getPendingRole(20, 700)).thenReturn(null);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.acceptRole(token, 700));
        assertTrue(ex.getMessage().contains("has no pending role"));
    }

    @Test
    void testDeclineRole_ThrowsWhenNoPendingRole() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(21);
        when(repo.getPendingRole(21, 800)).thenReturn(null);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.declineRole(token, 800));
        assertTrue(ex.getMessage().contains("has no pending role"));
    }

    // --- makeStoreOwner ---
    @Test
    void testMakeStoreOwner_ThrowsWhenNotOwner() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(22);
        when(repo.isSuspended(22)).thenReturn(false);
        when(repo.isOwner(22, 900)).thenReturn(false);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.makeStoreOwner(token, 23, 900));
        assertTrue(ex.getMessage().contains("not an owner"));
    }

    // --- clearShoppingCart should always delegate to repo.clearShoppingCart ---
    @Test
    void testClearShoppingCart_AlwaysInvokesRepo() throws Exception {
        String token = "tok";
        int userId = 24;

        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(userId);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        // No exception is expected
        assertDoesNotThrow(() -> svc.clearShoppingCart(token));

        // And we should have cleared the cart on the repo
        verify(repo).clearShoppingCart(userId);
    }

    // --- validateMemberId branches ---
    @Test
    void testValidateMemberId_Nonexistent() {
        UserService svc = new UserService(mock(UserRepository.class), mock(AuthTokenService.class),
                notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class,
                () -> svc.validateMemberId(99));
        assertTrue(ex.getMessage().contains("User with ID 99 doesn't exist"));
    }

    @Test
    void testValidateMemberId_NotMember() {
        UserRepository repo = mock(UserRepository.class);
        com.example.app.DomainLayer.User notMember = mock(com.example.app.DomainLayer.User.class);
        when(repo.getUserMapping()).thenReturn(Map.of(10, notMember));
        when(repo.getUserById(10)).thenReturn(notMember);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.validateMemberId(10));
        assertTrue(ex.getMessage().contains("is not a member"));
    }

    // --- isValidDetails valid case ---
    @Test
    void testIsValidDetails_Success() {
        UserService svc = new UserService(mock(UserRepository.class), mock(AuthTokenService.class),
                notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.isValidDetails("goodUser", "whatever", "user@example.com", "+1234567890"));
    }

    // --- getUserShoppingCartItems error branch ---
    @Test
    void testGetUserShoppingCartItems_ThrowsOnRepoError() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getShoppingCartById(5)).thenThrow(new RuntimeException("db"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.getUserShoppingCartItems(5));
        assertTrue(ex.getMessage().contains("getUserShoppingCart"));
    }

    // --- changePermissions: closeShop not allowed ---
    @Test
    void testChangePermissions_CloseShopPermissionNotAllowed() throws Exception {
        String token = "tok";
        int owner = 1, target = 2, shopId = 5;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(owner);
        when(repo.isSuspended(owner)).thenReturn(false);
        when(repo.isOwner(owner, shopId)).thenReturn(true);

        Member member = mock(Member.class);
        Role existing = new Role(owner, shopId, new PermissionsEnum[] { PermissionsEnum.manageItems });
        when(repo.getMemberById(target)).thenReturn(member);
        when(member.getRoles()).thenReturn(List.of(existing));

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.changePermissions(token, target, shopId,
                        new PermissionsEnum[] { PermissionsEnum.closeShop }));
        assertTrue(ex.getMessage().contains("Permission closeShop cannot be changed"));
    }

    // --- changePermissions: no matching role should be silent ---
    @Test
    void testChangePermissions_NoMatchingRole() throws Exception {
        String token = "tok";
        int owner = 1, target = 2, shopId = 5;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(owner);
        when(repo.isSuspended(owner)).thenReturn(false);
        when(repo.isOwner(owner, shopId)).thenReturn(true);

        Member member = mock(Member.class);
        when(repo.getMemberById(target)).thenReturn(member);
        when(member.getRoles()).thenReturn(List.of()); // no roles at all

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.changePermissions(token, target, shopId,
                new PermissionsEnum[] { PermissionsEnum.manageItems }));
        verify(repo, never()).setPermissions(anyInt(), anyInt(), any(Role.class), any());
    }

    // --- addRole success & suspended branches ---
    @Test
    void testAddRole_Success() {
        int memberId = 3;
        Role role = new Role(1, 5, new PermissionsEnum[] {});
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenReturn(Map.of(memberId, mock(Member.class)));
        when(repo.getUserById(memberId)).thenReturn(mock(Member.class));
        when(repo.isSuspended(memberId)).thenReturn(false);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        boolean ok = svc.addRole(memberId, role);
        assertTrue(ok);
        verify(repo).addRoleToPending(memberId, role);
    }

    @Test
    void testAddRole_ThrowsWhenSuspended() {
        int memberId = 4;
        Role role = new Role(1, 6, new PermissionsEnum[] {});
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenReturn(Map.of(memberId, mock(Member.class)));
        when(repo.getUserById(memberId)).thenReturn(mock(Member.class));
        when(repo.isSuspended(memberId)).thenReturn(true);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.addRole(memberId, role));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    // --- removeManagerFromStore null-role branch ---
    @Test
    void testRemoveManagerFromStore_NotManager() throws Exception {
        String token = "tok";
        int mgr = 10, shopId = 20;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(5);
        when(repo.isSuspended(5)).thenReturn(false);
        when(repo.getRole(mgr, shopId)).thenReturn(null);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeManagerFromStore(token, mgr, shopId));
        assertTrue(ex.getMessage().contains("is not a manager"));
    }

    // --- removeOwnerFromStore non-owner-role branch ---
    @Test
    void testRemoveOwnerFromStore_NotOwnerRole() throws Exception {
        String token = "tok";
        int memberId = 7, shopId = 8;
        Role role = new Role(memberId, shopId, null); // default is not owner
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(repo.getRole(memberId, shopId)).thenReturn(role);
        when(auth.ValidateToken(token)).thenReturn(memberId);
        when(repo.isSuspended(memberId)).thenReturn(false);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeOwnerFromStore(token, memberId, shopId));
        assertTrue(ex.getMessage().contains("is not an owner"));
    }

    // --- makeAdmin & removeAdmin when suspended ---
    @Test
    void testMakeAdmin_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        int targetId = 5;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(1);
        when(repo.isSuspended(1)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.makeAdmin(token, targetId));
        assertTrue(ex.getMessage().contains("makeAdmin"));
    }

    @Test
    void testRemoveAdmin_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        int targetId = 5;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(2);
        when(repo.isSuspended(2)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeAdmin(token, targetId));
        assertTrue(ex.getMessage().contains("removeAdmin"));
    }

    // --- addMember duplicate username ---
    @Test
    void testAddMember_DuplicateUsername() {
        IUserRepository repo = mock(IUserRepository.class);
        when(repo.isUsernameAndPasswordValid("dup", "pass")).thenReturn(123);
        AuthTokenService auth = mock(AuthTokenService.class);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class,
                () -> svc.addMember("dup", "pass", "e@x.com", "0123456789", "addr"));
        assertTrue(ex.getMessage().contains("Username is already taken"));
    }

    // --- getUserById error branch ---
    @Test
    void testGetUserById_ThrowsOnRepoError() {
        IUserRepository repo = mock(IUserRepository.class);
        when(repo.getUserById(99)).thenThrow(new RuntimeException("oops"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.getUserById(99));
        assertTrue(ex.getMessage().contains("getUserById:"));
    }

    // --- addItemToShoppingCart invalid token ---
    @Test
    void testAddItemToShoppingCart_InvalidToken() throws Exception {
        String token = "bad";
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenThrow(new OurArg("no such token"));

        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class,
                () -> svc.addItemToShoppingCart(token, 1, 1, 1));
        assertTrue(ex.getMessage().contains("addItemToShoppingCart"));
    }

    // --- removeItemFromShoppingCart invalid token ---
    @Test
    void testRemoveItemFromShoppingCart_InvalidToken() throws Exception {
        String token = "bad";
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenThrow(new OurRuntime("auth failure"));

        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeItemFromShoppingCart(token, 1, 1));
        assertTrue(ex.getMessage().contains("removeItemFromShoppingCart"));
    }

    // --- clearShoppingCart(token) when repo fails ---
    @Test
    void testClearShoppingCart_ThrowsOnRepoError() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(7);
        doThrow(new RuntimeException("db")).when(repo).clearShoppingCart(7);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.clearShoppingCart(token));
        assertTrue(ex.getMessage().contains("clearShoppingCart: db"));
    }

    // --- removeManagerFromStore when suspended ---
    @Test
    void testRemoveManagerFromStore_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        int mgr = 5, shopId = 10;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(1);
        when(repo.isSuspended(1)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeManagerFromStore(token, mgr, shopId));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    // --- makeStoreOwner when suspended ---
    @Test
    void testMakeStoreOwner_ThrowsWhenSuspended() throws Exception {
        String token = "tok";
        int newOwner = 2, shopId = 20;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(3);
        when(repo.isSuspended(3)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.makeStoreOwner(token, newOwner, shopId));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    // --- getSuspendedUsers throws on repo error ---
    @Test
    void testGetSuspendedUsers_ThrowsOnRepoError() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getSuspendedUsers()).thenThrow(new RuntimeException("dbfail"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                svc::getSuspendedUsers);
        assertTrue(ex.getMessage().contains("getSuspendedUsers"));
    }

    // --- isSuspended throws on repo error ---
    @Test
    void testIsSuspended_ThrowsOnRepoError() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.isSuspended(7)).thenThrow(new RuntimeException("boom"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.isSuspended(7));
        assertTrue(ex.getMessage().contains("isSuspended: boom"));
    }

    // --- clearUserShoppingCart by id throws on repo error ---
    @Test
    void testClearUserShoppingCartById_ThrowsOnRepoError() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getShoppingCartById(8)).thenThrow(new RuntimeException("cartfail"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.clearUserShoppingCart(8));
        assertTrue(ex.getMessage().contains("clearUserShoppingCart: cartfail"));
    }

    // --- restoreUserShoppingCart null items ---
    @Test
    void testRestoreUserShoppingCart_NullItems() {
        UserRepository repo = mock(UserRepository.class);
        com.example.app.DomainLayer.ShoppingCart cart = mock(com.example.app.DomainLayer.ShoppingCart.class);
        when(repo.getShoppingCartById(9)).thenReturn(cart);
        // make restoreCart(null) throw so that UserService wraps it
        doThrow(new RuntimeException("restoreFail"))
                .when(cart).restoreCart(null);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.restoreUserShoppingCart(9, null));
        assertTrue(ex.getMessage().contains("restoreUserShoppingCart: restoreFail"));
    }

    // --- setPaymentMethod repo error ---
    @Test
    void testSetPaymentMethod_ThrowsOnRepoError() throws Exception {
        String token = "tok";
        PaymentMethod pm = mock(PaymentMethod.class);
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(11);
        doThrow(new RuntimeException("pmfail"))
                .when(repo).setPaymentMethod(11, 1, pm);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.setPaymentMethod(token, pm, 1));
        assertTrue(ex.getMessage().contains("setPaymentMethod: pmfail"));
    }

    // --- refundPaymentByStoreEmployee repo error ---
    @Test
    void testRefundPaymentByStoreEmployee_ThrowsOnRepoError() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(12);
        when(repo.isSuspended(12)).thenReturn(false);
        when(repo.getRole(12, 15)).thenReturn(new Role(12, 15, null));
        doThrow(new RuntimeException("refundfail"))
                .when(repo).refund(99, 15);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.refundPaymentByStoreEmployee(token, 99, 15, 15));
        assertTrue(ex.getMessage().contains("refundPayment: refundfail"));
    }

    // --- loginAsMember success (initial login) ---
    @Test
    void testLoginAsMember_SuccessInitialLogin() {
        IUserRepository repo = mock(IUserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(repo.isUsernameAndPasswordValid("u", "p")).thenReturn(50);
        when(auth.Login("u", "p", 50)).thenReturn("tok50");
        when(repo.getMemberById(anyInt())).thenReturn(mock(Member.class));

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        svc.setEncoderToTest(true); // ensure password is not altered

        String token = svc.loginAsMember("u", "p", "");
        assertEquals("tok50", token);
    }

    // --- getUserShippingAddress throws on repo error ---
    @Test
    void testGetUserShippingAddress_ThrowsOnRepoError() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserById(13)).thenThrow(new RuntimeException("addrfail"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.getUserShippingAddress(13));
        assertTrue(ex.getMessage().contains("Error fetching shipping address for user ID 13"));
    }

    // --- loginAsGuest: repo.addGuest throws → returns null ---
    @Test
    void testLoginAsGuest_RepoThrows() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.addGuest()).thenThrow(new RuntimeException("addFail"));
        AuthTokenService auth = mock(AuthTokenService.class);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.loginAsGuest());
        assertTrue(ex.getMessage().contains("loginAsGuest: addFail"));
    }

    // --- loginAsGuest: addGuest returns negative → returns null ---
    @Test
    void testLoginAsGuest_NegativeId() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.addGuest()).thenReturn(-1);
        AuthTokenService auth = mock(AuthTokenService.class);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class,
                () -> svc.loginAsGuest());
        assertTrue(ex.getMessage().contains("loginAsGuest"));
    }

    // --- logout: removeUserById throws → returns null ---
    @Test
    void testLogout_RemoveUserThrows() throws Exception {
        String guestToken = "gt";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(guestToken)).thenReturn(42);
        when(repo.isGuestById(42)).thenReturn(true);
        doThrow(new RuntimeException("rmFail")).when(repo).removeUserById(42);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertNull(svc.logout(guestToken));
    }

    // --- logout: auth.Logout throws → returns null ---
    @Test
    void testLogout_AuthLogoutThrows() throws Exception {
        String guestToken = "gt";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(guestToken)).thenReturn(43);
        when(repo.isGuestById(43)).thenReturn(false);
        doThrow(new RuntimeException("logoutFail")).when(auth).Logout(guestToken);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertNull(svc.logout(guestToken));
    }

    // --- getAllAdmins: auth.ValidateToken throws OurArg ---
    @Test
    void testGetAllAdmins_AuthThrows() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class, () -> svc.getAllAdmins("bad"));
        assertTrue(ex.getMessage().contains("getAllAdmins"));
    }

    // --- getShopIdsByWorkerId: repo throws → OurRuntime ---
    @Test
    void testGetShopIdsByWorkerId_ThrowsOnRepoError() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getShopIdsByWorkerId(7)).thenThrow(new RuntimeException("fail"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.getShopIdsByWorkerId(7));
        assertTrue(ex.getMessage().contains("getShopsByUserId"));
    }

    // --- getShopMembers: repo throws → OurRuntime ---
    @Test
    void testGetShopMembers_ThrowsOnRepoError() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getShopMembers(9)).thenThrow(new RuntimeException("fail"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.getShopMembers(9));
        assertTrue(ex.getMessage().contains("getShopMembers"));
    }

    // --- getPendingRoles: repo throws → OurRuntime ---
    @Test
    void testGetPendingRoles_ThrowsOnRepoError() throws Exception {
        String token = "tk";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(50);
        when(repo.getPendingRoles(50)).thenThrow(new RuntimeException("dbErr"));

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.getPendingRoles(token));
        assertTrue(ex.getMessage().contains("getPendingRoles"));
    }

    // --- removeOwnerFromStore: getRole returns null → OurRuntime ---
    @Test
    void testRemoveOwnerFromStore_NullRole() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(repo.getRole(5, 6)).thenReturn(null);
        when(auth.ValidateToken(token)).thenReturn(5);
        when(repo.isSuspended(5)).thenReturn(false);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.removeOwnerFromStore(token, 5, 6));
        assertTrue(ex.getMessage().contains("removeOwnerFromStore"));
    }

    // --- hasPermission: mapping contains non-Member → returns false ---
    @Test
    void testHasPermission_NonMemberMapping() {
        UserRepository repo = mock(UserRepository.class);
        com.example.app.DomainLayer.User notMember = mock(com.example.app.DomainLayer.User.class);

        when(repo.getUserMapping()).thenReturn(Map.of(100, notMember));
        when(repo.isSuspended(100)).thenReturn(false);
        when(repo.getUserById(100)).thenReturn(notMember);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertFalse(svc.hasPermission(100, PermissionsEnum.manageItems, 1));
    }

    // --- getAllMembers: repo throws OurArg → propagates OurArg ---
    @Test
    void testGetAllMembers_ThrowsOurArg() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getAllMembers()).thenThrow(new OurArg("argFail"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class, svc::getAllMembers);
        assertTrue(ex.getMessage().contains("getAllUsers"));
    }

    // --- getUserById: repo throws OurArg → propagates OurArg ---
    @Test
    void testGetUserById_ThrowsOurArg() {
        IUserRepository repo = mock(IUserRepository.class);
        when(repo.getUserById(42)).thenThrow(new OurArg("bad id"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class, () -> svc.getUserById(42));
        assertTrue(ex.getMessage().contains("getUserById"));
    }

    // --- getUserPaymentMethod: repo.getUserById throws OurArg → propagates OurArg
    // ---
    @Test
    void testGetUserPaymentMethod_ThrowsOurArg() {
        IUserRepository repo = mock(IUserRepository.class);
        when(repo.getUserById(7)).thenThrow(new OurArg("noUser"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class, () -> svc.getUserPaymentMethod(7));
        assertTrue(ex.getMessage().contains("getUserPaymentMethod"));
    }

    // --- addBasket: invalid token should bubble up OurArg ---
    @Test
    void testAddBasket_InvalidToken_ThrowsOurArg() throws Exception {
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        UserRepository mockRepo = mock(UserRepository.class);
        when(mockAuth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class, () -> svc.addBasket("bad", 123));
        assertTrue(ex.getMessage().contains("addBasket"));
    }

    // --- addBasket: repo.createBasket throws → wrap in OurRuntime ---
    @Test
    void testAddBasket_RepoError_ThrowsOurRuntime() throws Exception {
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        UserRepository mockRepo = mock(UserRepository.class);
        when(mockAuth.ValidateToken("tok")).thenReturn(1);
        doThrow(new RuntimeException("fail"))
                .when(mockRepo).createBasket(1, 3);

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.addBasket("tok", 3));
        assertTrue(ex.getMessage().contains("addBasket: fail"));
    }

    // --- updateItemQuantityInShoppingCart: invalid token branch ---
    @Test
    void testUpdateItemQuantity_InvalidToken_ThrowsOurArg() throws Exception {
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        UserRepository mockRepo = mock(UserRepository.class);
        when(mockAuth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class,
                () -> svc.updateItemQuantityInShoppingCart("bad", 1, 2, 3));
        assertTrue(ex.getMessage().contains("updateItemQuantityInShoppingCart"));
    }

    // --- clearUserShoppingCart(int): success branch ---
    @Test
    void testClearUserShoppingCartById_Success() {
        UserRepository mockRepo = mock(UserRepository.class);
        com.example.app.DomainLayer.ShoppingCart cart = mock(com.example.app.DomainLayer.ShoppingCart.class);
        when(mockRepo.getShoppingCartById(9)).thenReturn(cart);

        UserService svc = new UserService(mockRepo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.clearUserShoppingCart(9));
        verify(cart).clearCart();
    }

    // --- restoreUserShoppingCart: success branch ---
    @Test
    void testRestoreUserShoppingCart_Success() {
        UserRepository mockRepo = mock(UserRepository.class);
        com.example.app.DomainLayer.ShoppingCart cart = mock(com.example.app.DomainLayer.ShoppingCart.class);
        when(mockRepo.getShoppingCartById(7)).thenReturn(cart);

        UserService svc = new UserService(mockRepo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        HashMap<Integer, HashMap<Integer, Integer>> items = new HashMap<>();
        assertDoesNotThrow(() -> svc.restoreUserShoppingCart(7, items));
        verify(cart).restoreCart(items);
    }

    // --- loginAsGuest: AuthenticateGuest throws → wrap in OurRuntime ---
    @Test
    void testLoginAsGuest_AuthFails() {
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockRepo.addGuest()).thenReturn(42);
        when(mockAuth.AuthenticateGuest(42)).thenThrow(new RuntimeException("authfail"));

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, svc::loginAsGuest);
        assertTrue(ex.getMessage().contains("loginAsGuest:"));
    }

    // --- loginAsMember: isUsernameAndPasswordValid returns 0 → OurArg ---
    @Test
    void testLoginAsMember_ZeroId_ThrowsOurArg() {
        IUserRepository repo = mock(IUserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(repo.isUsernameAndPasswordValid("u", "p")).thenReturn(0);
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        svc.setEncoderToTest(true);

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.loginAsMember("u", "p", ""));
        assertTrue(ex.getMessage().contains("loginAsMember"));
    }

    // --- loginAsMember: bad guest-token on merge → OurArg ---
    @Test
    void testLoginAsMember_BadGuestToken_ThrowsOurArg() throws Exception {
        IUserRepository repo = mock(IUserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        // valid member
        when(repo.isUsernameAndPasswordValid("u", "p")).thenReturn(5);
        // guest token validation fails
        when(auth.ValidateToken("badGuest")).thenThrow(new OurArg("noGuest"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        svc.setEncoderToTest(true);
        when(repo.getMemberById(5)).thenReturn(mock(Member.class));

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.loginAsMember("u", "p", "badGuest"));
        assertTrue(ex.getMessage().contains("loginAsMember"));
    }

    // --- loginAsMember: removeUserById throws → OurRuntime ---
    @Test
    void testLoginAsMember_RemoveGuestFails_ThrowsOurRuntime() throws Exception {
        IUserRepository repo = mock(IUserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(repo.isUsernameAndPasswordValid("u", "p")).thenReturn(5);
        when(auth.ValidateToken("gTok")).thenReturn(99);
        // removeUserById blows up
        doThrow(new RuntimeException("rmFail"))
                .when(repo).removeUserById(99);
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        svc.setEncoderToTest(true);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.loginAsMember("u", "p", "gTok"));
        assertTrue(ex.getMessage().contains("loginAsMember:"));
    }

    // --- addPermission: invalid token → OurArg ---
    @Test
    void testAddPermission_InvalidToken_ThrowsOurArg() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.addPermission("bad", 1, PermissionsEnum.manageItems, 2));
        assertTrue(ex.getMessage().contains("addPermission"));
    }

    // --- removePermission: invalid token → OurArg ---
    @Test
    void testRemovePermission_InvalidToken_ThrowsOurArg() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurRuntime("authErr"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removePermission("bad", 1, PermissionsEnum.manageItems, 2));
        assertTrue(ex.getMessage().contains("removePermission"));
    }

    // --- clearShoppingCart(String): invalid token → OurArg ---
    @Test
    void testClearShoppingCart_InvalidToken_ThrowsOurArg() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.clearShoppingCart("bad"));
        assertTrue(ex.getMessage().contains("clearShoppingCart"));
    }

    // --- updateMemberUsername: invalid token → OurArg ---
    @Test
    void testUpdateMemberUsername_InvalidToken_ThrowsOurArg() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.updateMemberUsername("bad", "newName"));
        assertTrue(ex.getMessage().contains("updateMemberUsername"));
    }

    // --- updateMemberEmail: invalid token → OurArg ---
    @Test
    void testUpdateMemberEmail_InvalidToken_ThrowsOurArg() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.updateMemberEmail("bad", "x@y.com"));
        assertTrue(ex.getMessage().contains("updateMemberEmail"));
    }

    // --- updateMemberPhoneNumber: invalid token → OurArg ---
    @Test
    void testUpdateMemberPhoneNumber_InvalidToken_ThrowsOurArg() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.updateMemberPhoneNumber("bad", "0123456789"));
        assertTrue(ex.getMessage().contains("updateMemberPhoneNumber"));
    }

    // --- setPaymentMethod: invalid token → OurArg ---
    @Test
    void testSetPaymentMethod_InvalidToken_ThrowsOurArg() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.setPaymentMethod("bad", mock(PaymentMethod.class), 1));
        assertTrue(ex.getMessage().contains("setPaymentMethod"));
    }

    // --- pay: invalid token → OurArg ---
    @Test
    void testPay_InvalidToken_ThrowsOurArg() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.pay("bad", 1, 10.0, "", "", "", "", "", "", ""));
        assertTrue(ex.getMessage().contains("pay"));
    }

    // --- refundPaymentAuto: invalid token → OurArg ---
    @Test
    void testRefundPaymentAuto_InvalidToken_ThrowsOurArg() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.refundPaymentAuto("bad", 1));
        assertTrue(ex.getMessage().contains("refundPayment"));
    }

    // --- refundPaymentByStoreEmployee: invalid token → OurArg ---
    @Test
    void testRefundPaymentByStoreEmployee_InvalidToken_ThrowsOurArg() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.refundPaymentByStoreEmployee("bad", 2, 3, 4));
        assertTrue(ex.getMessage().contains("refundPayment"));
    }

    // --- getNotificationsAndClear: invalid token → OurArg ---
    @Test
    void testGetNotificationsAndClear_InvalidToken_ThrowsOurArg() throws Exception {
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class,
                () -> svc.getNotificationsAndClear("bad"));
        assertTrue(ex.getMessage().contains("getNotificationsAndClear"));
    }

    // --- purchaseNotification: empty cart → no exception ---
    @Test
    void testPurchaseNotification_EmptyCart_DoesNothing() {
        UserService svc = new UserService(mock(UserRepository.class), mock(AuthTokenService.class),
                notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.purchaseNotification(new HashMap<>()));
    }

    // --- closeShopNotification: no owners → no exception ---
    @Test
    void testCloseShopNotification_EmptyOwners_DoesNothing() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getOwners(123)).thenReturn(List.of());
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        assertDoesNotThrow(() -> svc.closeShopNotification(123));
    }

    // --- addRole: non-existent member → OurArg from validateMemberId ---
    @Test
    void testAddRole_NonexistentMember_ThrowsOurArg() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenReturn(Map.of()); // no IDs
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        Role r = new Role(1, 2, new PermissionsEnum[] {});
        OurArg ex = assertThrows(OurArg.class, () -> svc.addRole(1, r));
        assertTrue(ex.getMessage().contains("addRole"));
    }

    // --- hasPermission: getUserMapping throws → false ---
    @Test
    void testHasPermission_MappingError_ReturnsFalse() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenThrow(new RuntimeException("boom"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        assertFalse(svc.hasPermission(1, PermissionsEnum.closeShop, 2));
    }

    // --- acceptRole(String, int) ---
    @Test
    void testAcceptRole_NoPending_ThrowsOurRuntime() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(7);
        when(repo.getPendingRole(7, 42)).thenReturn(null);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.acceptRole(token, 42));
        assertTrue(ex.getMessage().contains("has no pending role"));
    }

    @Test
    void testAcceptRole_Success() throws Exception {
        String token = "tok";
        int memberId = 5, shopId = 99;
        Role pending = new Role(memberId, shopId, null);

        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(memberId);
        when(repo.getPendingRole(memberId, shopId)).thenReturn(pending);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.acceptRole(token, shopId));
        verify(repo).acceptRole(memberId, pending);
    }

    // --- declineRole(String, int) ---
    @Test
    void testDeclineRole_NoPending_ThrowsOurRuntime() throws Exception {
        String token = "t";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(8);
        when(repo.getPendingRole(8, 15)).thenReturn(null);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.declineRole(token, 15));
        assertTrue(ex.getMessage().contains("has no pending role"));
    }

    @Test
    void testDeclineRole_Success() throws Exception {
        String token = "t";
        int memberId = 6, shopId = 17;
        Role pending = new Role(memberId, shopId, null);

        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(memberId);
        when(repo.getPendingRole(memberId, shopId)).thenReturn(pending);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.declineRole(token, shopId));
        verify(repo).declineRole(memberId, pending);
    }

    @Test
    void testAddRole_SuspendedMember_ThrowsOurRuntime() {
        int memberId = 3;
        Role r = new Role(1, 2, new PermissionsEnum[] {});

        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenReturn(Map.of(memberId, mock(Member.class)));
        when(repo.getUserById(memberId)).thenReturn(mock(Member.class));
        when(repo.isSuspended(memberId)).thenReturn(true);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.addRole(memberId, r));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    // --- clearShoppingCart(String) ---
    @Test
    void testClearShoppingCart_IncludesValidateToken() throws Exception {
        String token = "x";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(4);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        // normal
        assertDoesNotThrow(() -> svc.clearShoppingCart(token));
        verify(repo).clearShoppingCart(4);

        // repo throws
        doThrow(new RuntimeException("boom")).when(repo).clearShoppingCart(4);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.clearShoppingCart(token));
        assertTrue(ex.getMessage().contains("clearShoppingCart: boom"));
    }

    @Test
    void testGetUserShippingAddress_RepoThrows() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserById(13)).thenThrow(new RuntimeException("nope"));

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.getUserShippingAddress(13));
        assertTrue(ex.getMessage().contains("Error fetching shipping address"));
    }

    // --- isSuspended(int) ---
    @Test
    void testIsSuspended_ReturnsValueAndThrows() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.isSuspended(2)).thenReturn(true);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertTrue(svc.isSuspended(2));

        when(repo.isSuspended(3)).thenThrow(new RuntimeException("fail"));
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.isSuspended(3));
        assertTrue(ex.getMessage().contains("isSuspended: fail"));
    }

    // --- messageNotification(Integer,Integer,boolean) ---
    @Test
    void testMessageNotification_FromShopAndUser() {
        UserRepository repo = mock(UserRepository.class);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        // from shop
        svc.messageNotification(21, 33, true);
        verify(notificationService).sendToUser(21, "Message Received",
                "You have received a new message from the shop (id=33).");

        // from user
        svc.messageNotification(22, 0, false);
        verify(notificationService).sendToUser(22, "Message Received",
                "You have received a new message from the user (id=22).");
    }

    @Test
    void testMessageNotification_RepoThrows() {
        UserRepository repo = mock(UserRepository.class);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        doThrow(new RuntimeException("msgFail"))
                .when(notificationService).sendToUser(anyInt(), anyString(), anyString());

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.messageNotification(5, 1, true));
        assertTrue(ex.getMessage().contains("messageUserNotification"));
    }

    // --- getShopIdsByWorkerId(int) ---
    @Test
    void testGetShopIdsByWorkerId_SuccessAndFailure() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getShopIdsByWorkerId(7)).thenReturn(List.of(100, 101));

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertEquals(List.of(100, 101), svc.getShopIdsByWorkerId(7));

        when(repo.getShopIdsByWorkerId(8)).thenThrow(new RuntimeException("db"));
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.getShopIdsByWorkerId(8));
        assertTrue(ex.getMessage().contains("getShopsByUserId"));
    }

    // --- getShopMembers(int) ---
    @Test
    void testGetShopMembers_SuccessAndFailure() {
        Member m = mock(Member.class);
        UserRepository repo = mock(UserRepository.class);
        when(repo.getShopMembers(55)).thenReturn(List.of(m));

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertEquals(List.of(m), svc.getShopMembers(55));

        when(repo.getShopMembers(56)).thenThrow(new RuntimeException("oops"));
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.getShopMembers(56));
        assertTrue(ex.getMessage().contains("getShopMembers"));
    }

    // --- addPermission(String,int,PermissionsEnum,int) ---
    @Test
    void testAddPermission_SuccessAndNoRoleAndRepoThrows() throws Exception {
        String token = "tok";
        int assignee = 1, target = 2, shopId = 3;
        PermissionsEnum perm = PermissionsEnum.manageItems;

        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(assignee);
        when(repo.isSuspended(assignee)).thenReturn(false);

        // valid mapping + role
        Member member = mock(Member.class);
        when(repo.getUserMapping()).thenReturn(Map.of(target, member));
        when(repo.getUserById(target)).thenReturn(member);
        Role role = new Role(assignee, shopId, new PermissionsEnum[] {});
        when(repo.getRole(target, shopId)).thenReturn(role);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertTrue(svc.addPermission(token, target, perm, shopId));
        verify(repo).addPermission(target, perm, shopId);

        // no existing role
        when(repo.getRole(target, shopId)).thenReturn(null);
        OurRuntime ex1 = assertThrows(OurRuntime.class,
                () -> svc.addPermission(token, target, perm, shopId));
        assertTrue(ex1.getMessage().contains("has no role"));

        // repo throws
        when(repo.getRole(target, shopId)).thenReturn(role);
        doThrow(new RuntimeException("permFail"))
                .when(repo).addPermission(target, perm, shopId);
        OurRuntime ex2 = assertThrows(OurRuntime.class,
                () -> svc.addPermission(token, target, perm, shopId));
        assertTrue(ex2.getMessage().contains("addPermission: permFail"));
    }

    // --- clearUserShoppingCart(int) ---
    @Test
    void testClearUserShoppingCart_ById_SuccessAndThrows() {
        int uid = 9;
        ShoppingCart cart = mock(ShoppingCart.class);
        UserRepository repo = mock(UserRepository.class);
        when(repo.getShoppingCartById(uid)).thenReturn(cart);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.clearUserShoppingCart(uid));
        verify(cart).clearCart();

        when(repo.getShoppingCartById(uid)).thenThrow(new RuntimeException("cartErr"));
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.clearUserShoppingCart(uid));
        assertTrue(ex.getMessage().contains("clearUserShoppingCart: cartErr"));
    }

    // --- removeAdmin(String, Integer) ---
    @Test
    void testRemoveAdmin_SuccessAndErrors() throws Exception {
        String token = "tok";
        int adminId = 1, target = 2;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(adminId);
        when(repo.isSuspended(adminId)).thenReturn(false);
        when(repo.isAdmin(adminId)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        // success
        assertDoesNotThrow(() -> svc.removeAdmin(token, target));
        verify(repo).removeAdmin(target);

        // not admin
        when(repo.isAdmin(adminId)).thenReturn(false);
        OurRuntime ex1 = assertThrows(OurRuntime.class,
                () -> svc.removeAdmin(token, target));
        assertTrue(ex1.getMessage().contains("only admins"));

        // repo throws
        when(repo.isAdmin(adminId)).thenReturn(true);
        doThrow(new RuntimeException("rmFail")).when(repo).removeAdmin(target);
        OurRuntime ex2 = assertThrows(OurRuntime.class,
                () -> svc.removeAdmin(token, target));
        assertTrue(ex2.getMessage().contains("removeAdmin: rmFail"));
    }

    // --- hasRole(int, Role) ---
    @Test
    void testHasRole_SuspendedAndWrongAssignee() {
        int id = 5, shop = 6;
        Role r = new Role(id, shop, null);

        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenReturn(Map.of(id, mock(Member.class)));
        when(repo.getUserById(id)).thenReturn(mock(Member.class));
        when(repo.isSuspended(id)).thenReturn(true);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertFalse(svc.hasRole(id, r));

        // wrong assignee
        when(repo.isSuspended(id)).thenReturn(false);
        when(repo.getRole(id, shop)).thenReturn(new Role(id + 1, shop, null));
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.hasRole(id, r));
        assertTrue(ex.getMessage().contains("is not the assignee"));
    }

    // --- removePermission(String,int,PermissionsEnum,int) ---
    @Test
    void testRemovePermission_SuccessNoRoleAndRepoThrows() throws Exception {
        String token = "t";
        int assignee = 1, target = 2, shopId = 7;
        PermissionsEnum perm = PermissionsEnum.handleMessages;

        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(assignee);
        when(repo.isSuspended(assignee)).thenReturn(false);

        // mapping + role
        Member member = mock(Member.class);
        when(repo.getUserMapping()).thenReturn(Map.of(target, member));
        when(repo.getUserById(target)).thenReturn(member);
        Role existing = new Role(assignee, shopId, new PermissionsEnum[] { perm });
        when(repo.getRole(target, shopId)).thenReturn(existing);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertTrue(svc.removePermission(token, target, perm, shopId));
        verify(repo).removePermission(target, perm, shopId);

        // no role
        when(repo.getRole(target, shopId)).thenReturn(null);
        OurRuntime ex1 = assertThrows(OurRuntime.class,
                () -> svc.removePermission(token, target, perm, shopId));
        assertTrue(ex1.getMessage().contains("has no role"));

        // repo throws
        when(repo.getRole(target, shopId)).thenReturn(existing);
        doThrow(new RuntimeException("rmPermFail"))
                .when(repo).removePermission(target, perm, shopId);
        OurRuntime ex2 = assertThrows(OurRuntime.class,
                () -> svc.removePermission(token, target, perm, shopId));
        assertTrue(ex2.getMessage().contains("removePermission: rmPermFail"));
    }

    // --- makeAdmin(String,Integer) ---
    @Test
    void testMakeAdmin_SuccessAndErrors() throws Exception {
        String token = "a";
        int admin = 1, target = 2;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(admin);
        when(repo.isSuspended(admin)).thenReturn(false);
        when(repo.isAdmin(admin)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.makeAdmin(token, target));
        verify(repo).addAdmin(target);

        // not admin
        when(repo.isAdmin(admin)).thenReturn(false);
        OurRuntime ex1 = assertThrows(OurRuntime.class,
                () -> svc.makeAdmin(token, target));
        assertTrue(ex1.getMessage().contains("only admins"));

        // repo throws
        when(repo.isAdmin(admin)).thenReturn(true);
        doThrow(new RuntimeException("addFail")).when(repo).addAdmin(target);
        OurRuntime ex2 = assertThrows(OurRuntime.class,
                () -> svc.makeAdmin(token, target));
        assertTrue(ex2.getMessage().contains("makeAdmin: java.lang.RuntimeException: addFail"));
    }

    // --- getUserShoppingCartItems(int) ---
    @Test
    void testGetUserShoppingCartItems_SuccessAndThrows() {
        int uid = 12;
        ShoppingCart cart = mock(ShoppingCart.class);
        HashMap<Integer, HashMap<Integer, Integer>> items = new HashMap<>();
        when(cart.getItems()).thenReturn(items);

        UserRepository repo = mock(UserRepository.class);
        when(repo.getShoppingCartById(uid)).thenReturn(cart);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertSame(items, svc.getUserShoppingCartItems(uid));

        when(repo.getShoppingCartById(uid)).thenThrow(new RuntimeException("oops"));
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.getUserShoppingCartItems(uid));
        assertTrue(ex.getMessage().contains("getUserShoppingCart"));
    }

    // --- getSuspendedUsers() ---
    @Test
    void testGetSuspendedUsers_SuccessAndThrows() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getSuspendedUsers()).thenReturn(List.of(2, 3));

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertEquals(List.of(2, 3), svc.getSuspendedUsers());

        when(repo.getSuspendedUsers()).thenThrow(new RuntimeException("dbErr"));
        OurRuntime ex = assertThrows(OurRuntime.class,
                svc::getSuspendedUsers);
        assertTrue(ex.getMessage().contains("getSuspendedUsers"));
    }

    // --- getUserPaymentMethod(int) ---
    @Test
    void testGetUserPaymentMethod_NullNonNullAndThrows() {
        int uid = 14;
        PaymentMethod pm = mock(PaymentMethod.class);

        UserRepository repo = mock(UserRepository.class);
        User user = mock(User.class);
        when(repo.getUserById(uid)).thenReturn(user).thenThrow(new RuntimeException("fail"));
        when(user.getPaymentMethod()).thenReturn(pm);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        // first call returns pm
        assertSame(pm, svc.getUserPaymentMethod(uid));

        // second call throws
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.getUserPaymentMethod(uid));
        assertTrue(ex.getMessage().contains("getUserPaymentMethod"));
    }

    // --- addBasket(String, int) ---
    @Test
    void testAddBasket_SuccessAndErrors() throws Exception {
        String token = "tok";
        int uid = 7, shop = 8;

        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(uid);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.addBasket(token, shop));
        verify(repo).createBasket(uid, shop);

        // repo throws
        doThrow(new RuntimeException("cbFail"))
                .when(repo).createBasket(uid, shop);
        OurRuntime ex1 = assertThrows(OurRuntime.class,
                () -> svc.addBasket(token, shop));
        assertTrue(ex1.getMessage().contains("addBasket: cbFail"));

        // invalid token
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        OurArg ex2 = assertThrows(OurArg.class,
                () -> svc.addBasket("bad", shop));
        assertTrue(ex2.getMessage().contains("addBasket"));
    }

    // --- logout(String) ---
    @Test
    void testLogout_SuccessAsGuestRemovalAndNewGuestIssued() throws Exception {
        // arrange
        String oldGuest = "oldTk";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        AuthTokenRepository infra = new AuthTokenRepository();
        AuthTokenService realAuth = new AuthTokenService(infra);

        // when we Validate oldGuest → userId=1
        when(auth.ValidateToken(oldGuest)).thenReturn(1);
        // treat *any* userId as a guest
        when(repo.isGuestById(anyInt())).thenReturn(true);

        // override loginAsGuest to call the real infra
        UserService svc = new UserService(repo, auth, notificationService) {
            @Override
            public String loginAsGuest() {
                return realAuth.generateAuthToken("g");
            }
        };
        notificationService.setService(svc);

        // act
        String newGuest = svc.logout(oldGuest);

        // assert
        assertNotNull(newGuest);
        int newId = infra.getUserIdByToken(newGuest);
        // now repo.isGuestById(newId) → true
        assertTrue(repo.isGuestById(newId));
    }

    @Test
    void testLogout_RemoveUserThrows_IsSwallowedAndReturnsNull() throws Exception {
        String old = "g";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(old)).thenReturn(42);
        when(repo.isGuestById(42)).thenReturn(true);
        doThrow(new RuntimeException("rmFail")).when(repo).removeUserById(42);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertNull(svc.logout(old));
    }

    @Test
    void testLogout_LogoutThrows_IsSwallowedAndReturnsNull() throws Exception {
        String old = "g2";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(old)).thenReturn(43);
        when(repo.isGuestById(43)).thenReturn(false);
        doThrow(new RuntimeException("logoutFail")).when(auth).Logout(old);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertNull(svc.logout(old));
    }

    // --- getAllAdmins(String) ---
    @Test
    void testGetAllAdmins_SuccessAndNonAdmin() throws Exception {
        String token = "t";
        int adminId = 5;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(adminId);
        when(repo.isAdmin(adminId)).thenReturn(true);
        when(repo.getAllAdmins()).thenReturn(List.of(5, 6, 7));

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        List<Integer> all = svc.getAllAdmins(token);
        assertEquals(List.of(5, 6, 7), all);

        // non-admin
        when(repo.isAdmin(adminId)).thenReturn(false);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.getAllAdmins(token));
        assertTrue(ex.getMessage().contains("only admins"));
    }

    @Test
    void testGetAllAdmins_AuthThrowsAndRepoError() throws Exception {
        // auth throws
        AuthTokenService auth1 = mock(AuthTokenService.class);
        when(auth1.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc1 = new UserService(mock(UserRepository.class), auth1, notificationService);
        notificationService.setService(svc1);
        assertThrows(OurArg.class, () -> svc1.getAllAdmins("bad"));

        // repo throws
        String tok = "tt";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth2 = mock(AuthTokenService.class);
        when(auth2.ValidateToken(tok)).thenReturn(1);
        when(repo.isAdmin(1)).thenReturn(true);
        when(repo.getAllAdmins()).thenThrow(new RuntimeException("db"));
        NotificationService notificationService2 = mock(NotificationService.class);
        UserService svc2 = new UserService(repo, auth2, notificationService2);
        notificationService2.setService(svc2);

        OurRuntime ex2 = assertThrows(OurRuntime.class,
                () -> svc2.getAllAdmins(tok));
        assertTrue(ex2.getMessage().contains("getAllAdmins"));
    }

    // --- makeStoreOwner(String,int,int) ---
    @Test
    void testMakeStoreOwner_SuccessAndAllErrorBranches() throws Exception {
        String token = "t0";
        int owner = 10, newOwner = 20, shop = 30;
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        // success
        when(auth.ValidateToken(token)).thenReturn(owner);
        when(repo.isOwner(owner, shop)).thenReturn(true);
        when(repo.getMemberById(newOwner)).thenReturn(mock(Member.class));
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.makeStoreOwner(token, newOwner, shop));
        verify(repo).addRoleToPending(eq(newOwner), any(Role.class));

        // not owner
        when(repo.isOwner(owner, shop)).thenReturn(false);
        OurRuntime ex1 = assertThrows(OurRuntime.class,
                () -> svc.makeStoreOwner(token, newOwner, shop));
        assertTrue(ex1.getMessage().contains("not an owner"));

        // suspended
        when(repo.isOwner(owner, shop)).thenReturn(true);
        when(repo.isSuspended(owner)).thenReturn(true);
        OurRuntime ex2 = assertThrows(OurRuntime.class,
                () -> svc.makeStoreOwner(token, newOwner, shop));
        assertTrue(ex2.getMessage().contains("the user is suspended"));

        // member not exist
        when(repo.isSuspended(owner)).thenReturn(false);
        when(repo.getMemberById(newOwner)).thenReturn(null);
        OurRuntime ex3 = assertThrows(OurRuntime.class,
                () -> svc.makeStoreOwner(token, newOwner, shop));
        assertTrue(ex3.getMessage().contains("does not exist"));

        // repo.addRoleToPending throws
        when(repo.getMemberById(newOwner)).thenReturn(mock(Member.class));
        doThrow(new RuntimeException("pendFail"))
                .when(repo).addRoleToPending(eq(newOwner), any(Role.class));
        OurRuntime ex4 = assertThrows(OurRuntime.class,
                () -> svc.makeStoreOwner(token, newOwner, shop));
        assertTrue(ex4.getMessage().contains("makeStoreOwner: "));
    }

    // --- addMember(String,String,String,String,String) ---
    @Test
    void testAddMember_SuccessAndDuplicateAndInvalidAndErrors() throws Exception {
        IUserRepository repo = mock(IUserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        svc.setEncoderToTest(true);

        // --- success case with valid details ---
        when(repo.isUsernameAndPasswordValid("user", "pass")).thenReturn(-1);
        when(repo.addMember("user", "pass", "e@mail.com", "0123456789", "addr")).thenReturn(99);
        when(auth.Login("user", "pass", 99)).thenReturn("tk99");

        String tok = svc.addMember("user", "pass", "e@mail.com", "0123456789", "addr");
        assertEquals("tk99", tok);

        // --- duplicate username ---
        when(repo.isUsernameAndPasswordValid("dup", "p")).thenReturn(50);
        OurArg exDup = assertThrows(OurArg.class,
                () -> svc.addMember("dup", "p", "e@mail.com", "0123456789", "addr"));
        assertTrue(exDup.getMessage().contains("Username is already taken."));

        // --- invalid details: too‐short username, bad email, bad phone ---
        OurRuntime exInv = assertThrows(OurRuntime.class,
                () -> svc.addMember("ab", "pass", "bad", "123", "addr"));
        String msg = exInv.getMessage();
        assertTrue(msg.contains("Invalid Username."));
        assertTrue(msg.contains("Invalid Phone Number."));
        assertTrue(msg.contains("Invalid Email."));

        // --- repo.addMember throws ---
        when(repo.isUsernameAndPasswordValid("new", "p")).thenReturn(-1);
        doThrow(new RuntimeException("dbFail"))
                .when(repo).addMember("new", "p", "x@x.com", "0123456789", "addr");
        OurRuntime exDb = assertThrows(OurRuntime.class,
                () -> svc.addMember("new", "p", "x@x.com", "0123456789", "addr"));
        assertTrue(exDb.getMessage().contains("addMember:"));

        // --- auth.Login throws ---
        when(repo.isUsernameAndPasswordValid("ok", "p")).thenReturn(-1);
        when(repo.addMember("ok", "p", "a@b.com", "0123456789", "addr")).thenReturn(100);
        doThrow(new RuntimeException("authFail"))
                .when(auth).Login("ok", "p", 100);
        OurRuntime exAuth = assertThrows(OurRuntime.class,
                () -> svc.addMember("ok", "p", "a@b.com", "0123456789", "addr"));
        assertTrue(exAuth.getMessage().contains("addMember:"));
    }

    @Test
    void testAddAndRemoveAdmin() {
        String token = userService.loginAsMember("admin", "admin", "");
        userService.addMember("userA", "passA", "a@mail.com", "0123456789", "addr");
        int userId = userRepository.isUsernameAndPasswordValid("userA", "passA");

        userService.makeAdmin(token, userId);
        assertTrue(userService.isAdmin(userId));

        userService.removeAdmin(token, userId);
        assertFalse(userService.isAdmin(userId));
    }

    @Test
    void testMemberCRUDAndLogin() throws Exception {
        userService.addMember("john", "pass123", "john@example.com", "0123456789", "123 Main St");
        int memberId = userRepository.isUsernameAndPasswordValid("john", "pass123");
        User member = userService.getUserById(memberId);
        assertNotNull(member);
        assertEquals("john", ((Member) member).getUsername());

        String token = authTokenService.Login("john", "pass123", memberId);
        userService.updateMemberUsername(token, "johnny");
        userService.updateMemberPassword(token, "newpass");
        userService.updateMemberEmail(token, "johnny@example.com");
        userService.updateMemberPhoneNumber(token, "0987654321");

        Member updated = (Member) userService.getUserById(memberId);
        assertEquals("johnny", updated.getUsername());
        assertEquals("johnny@example.com", updated.getEmail());
        assertEquals("0987654321", updated.getPhoneNumber());
    }

    @Test
    void testGuestLoginAndLogout() throws Exception {
        String guestToken = userService.loginAsGuest();
        assertNotNull(guestToken);
        int guestId = authTokenRepository.getUserIdByToken(guestToken);
        assertTrue(userRepository.isGuestById(guestId));

        String newGuest = userService.logout(guestToken);
        assertNotNull(newGuest);
        int newId = authTokenRepository.getUserIdByToken(newGuest);
        assertTrue(userRepository.isGuestById(newId));
    }

    @Test
    void testSetPaymentAndPay() throws Exception {
        userService.addMember("alice", "pwd", "a@a.com", "0123456789", "addr");
        String token = userService.loginAsMember("alice", "pwd", "");
        PaymentMethod pm = mock(PaymentMethod.class);
        when(pm.processPayment(anyDouble(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString()))
                .thenReturn(10005);

        userService.setPaymentMethod(token, pm, 1);
        assertEquals(pm, userService.getUserPaymentMethod(userRepository.isUsernameAndPasswordValid("alice", "pwd")));

        int id = userService.pay(token, 1, 100.0, "John Doe", "1234567890123456", "12/25", "123", "123 Main St", "City",
                "12345");
        assertTrue(id >= 10000 && id <= 100000, "failed id:" + id); // Assuming payment IDs are in this range
    }

    @Test
    void testShoppingCartOperations() throws Exception {
        String guestToken = userService.loginAsGuest();
        int guestId = authTokenRepository.getUserIdByToken(guestToken);

        userService.addItemToShoppingCart(guestToken, 1, 101, 2);
        Map<Integer, Integer> basket = userService.getBasketItems(guestToken, 1);
        assertEquals(2, basket.get(101));

        userService.removeItemFromShoppingCart(guestToken, 1, 101);
        assertTrue(userService.getBasketItems(guestToken, 1).isEmpty());

        userService.clearShoppingCart(guestToken);
    }

    @Test
    void testConcurrencyGuestsAndMembers() throws InterruptedException {
        final int threads = 50;

        // --- concurrent add guests ---
        ExecutorService ex1 = Executors.newFixedThreadPool(threads);
        CountDownLatch latch1 = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            ex1.submit(() -> {
                userRepository.addGuest();
                latch1.countDown();
            });
        }
        latch1.await();
        ex1.shutdown();

        // after guests: default admin + threads guests
        int sizeAfterGuests = userRepository.getUserMapping().size();
        assertEquals(threads + 1, sizeAfterGuests);

        // --- concurrent add members ---
        ExecutorService ex2 = Executors.newFixedThreadPool(threads);
        CountDownLatch latch2 = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            ex2.submit(() -> {
                userRepository.addMember(
                        "user" + idx, "pass" + idx,
                        "email" + idx + "@test.com", "123456789", "addr");
                latch2.countDown();
            });
        }
        latch2.await();
        ex2.shutdown();

        // now we expect sizeAfterGuests + threads more members
        assertEquals(sizeAfterGuests + threads, userRepository.getUserMapping().size());
    }

    @Test
    void testSuspension() {
        userService.addMember("bob", "pwd", "b@b.com", "1234567890", "addr");
        int bobId = userRepository.isUsernameAndPasswordValid("bob", "pwd");
        LocalDateTime until = LocalDateTime.now().plusDays(1);

        userService.setSuspended(bobId, until);
        assertTrue(userService.isSuspended(bobId));
        List<Integer> suspended = userService.getSuspendedUsers();
        assertTrue(suspended.contains(bobId));
    }

    @Test
    void testRoleAndPermissionFlows() throws Exception {
        // add role, accept/decline
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        String token = "tok";
        int uid = 5, shop = 10;

        when(mockAuth.ValidateToken(token)).thenReturn(uid);
        Role pending = new Role(uid, shop, null);
        when(mockRepo.getPendingRole(uid, shop)).thenReturn(pending);

        svc.acceptRole(token, shop);
        verify(mockRepo).acceptRole(uid, pending);

        svc.declineRole(token, shop);
        verify(mockRepo).declineRole(uid, pending);
    }

    @Test
    void testGetAllAdminsACL() throws Exception {
        String adminToken = userService.loginAsMember("admin", "admin", "");
        List<Integer> admins = userService.getAllAdmins(adminToken);
        assertTrue(admins.contains(userRepository.isUsernameAndPasswordValid("admin", "admin")));

        userService.addMember("charlie", "pwd", "c@c.com", "0123456789", "addr");
        String charlieToken = authTokenService.Login("charlie", "pwd",
                userRepository.isUsernameAndPasswordValid("charlie", "pwd"));
        assertThrows(OurRuntime.class, () -> userService.getAllAdmins(charlieToken));
    }

    // --- changePermissions ---
    @Test
    void testChangePermissions_ThrowsWhenSuspended() throws Exception {
        String token = "t";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken(token)).thenReturn(1);
        when(repo.isSuspended(1)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.changePermissions(token, 2, 5, new PermissionsEnum[] { PermissionsEnum.manageItems }));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    @Test
    void testChangePermissions_InvalidToken() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));

        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class,
                () -> svc.changePermissions("bad", 2, 5, new PermissionsEnum[] { PermissionsEnum.manageItems }));
        assertTrue(ex.getMessage().contains("changePermissions"));
    }

    // --- removeManagerFromStore ---
    @Test
    void testRemoveManagerFromStore_InvalidToken() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));

        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class, () -> svc.removeManagerFromStore("bad", 10, 20));
        assertTrue(ex.getMessage().contains("removeManagerOfStore"));
    }

    // --- removeAllAssigned ---
    @Test
    void testRemoveAllAssigned_EmptyListDoesNothing() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getMembersList()).thenReturn(List.of());
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        // should not throw
        assertDoesNotThrow(() -> svc.removeAllAssigned(99, 123));
    }

    // --- acceptRole & declineRole invalid token ---
    @Test
    void testAcceptRole_InvalidToken() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));

        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class, () -> svc.acceptRole("bad", 77));
        assertTrue(ex.getMessage().contains("acceptRole"));
    }

    @Test
    void testDeclineRole_InvalidToken() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));

        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class, () -> svc.declineRole("bad", 77));
        assertTrue(ex.getMessage().contains("declineRole"));
    }

    // --- removeOwnerFromStore invalid token ---
    @Test
    void testRemoveOwnerFromStore_InvalidToken() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));

        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.removeOwnerFromStore("bad", 5, 6));
        assertTrue(ex.getMessage().contains("removeOwnerFromStore"));
    }

    // --- makeManagerOfStore invalid token ---
    @Test
    void testMakeManagerOfStore_InvalidToken() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));

        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class, () -> svc.makeManagerOfStore("bad", 10, 20, new PermissionsEnum[] {}));
        assertTrue(ex.getMessage().contains("makeManagerOfStore"));
    }

    // --- hasRole on non‐member mapping ---
    @Test
    void testHasRole_NonMemberMapping() {
        UserRepository repo = mock(UserRepository.class);
        // mapping contains a User (but not a Member)
        com.example.app.DomainLayer.User notMember = mock(com.example.app.DomainLayer.User.class);
        when(repo.getUserMapping()).thenReturn(Map.of(100, notMember));
        when(repo.isSuspended(100)).thenReturn(false);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        Role r = new Role(100, 1, null);

        OurArg ex = assertThrows(OurArg.class, () -> svc.hasRole(100, r));
        assertTrue(ex.getMessage().contains("User with ID 100 is not a member"));
    }

    // --- updateMemberPassword invalid token ---
    @Test
    void testUpdateMemberPassword_InvalidToken() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class, () -> svc.updateMemberPassword("bad", "newpass"));
        assertTrue(ex.getMessage().contains("updateMemberPassword"));
    }

    // --- restoreUserShoppingCart null cart reference ---
    @Test
    void testRestoreUserShoppingCart_NullCart_Throws() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getShoppingCartById(9)).thenReturn(null);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.restoreUserShoppingCart(9, new HashMap<>()));
        assertTrue(ex.getMessage().contains("restoreUserShoppingCart"));
    }

    // --- removePermission invalid token ---
    @Test
    void testRemovePermission_InvalidToken() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class, () -> svc.removePermission("bad", 2, PermissionsEnum.manageItems, 1));
        assertTrue(ex.getMessage().contains("removePermission"));
    }

    // --- makeAdmin invalid token ---
    @Test
    void testMakeAdmin_InvalidToken() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);

        OurArg ex = assertThrows(OurArg.class, () -> svc.makeAdmin("bad", 5));
        assertTrue(ex.getMessage().contains("makeAdmin"));
    }

    // --- getUserById success path ---
    @Test
    void testGetUserById_Success() {
        IUserRepository repo = mock(IUserRepository.class);
        User u = mock(User.class);
        when(repo.getUserById(42)).thenReturn(u);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertSame(u, svc.getUserById(42));
    }

    // --- loginAsGuest success path ---
    @Test
    void testLoginAsGuest_Success() {
        AuthTokenRepository infra = new AuthTokenRepository();
        AuthTokenService realAuth = new AuthTokenService(infra);

        // stub repo to return a known guest ID
        UserRepository repo = mock(UserRepository.class);
        when(repo.addGuest()).thenReturn(123);

        // spy AuthTokenService so AuthenticateGuest actually populates 'infra'
        AuthTokenService auth = mock(AuthTokenService.class);
        doAnswer(inv -> realAuth.AuthenticateGuest(inv.getArgument(0)))
                .when(auth).AuthenticateGuest(anyInt());

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        String token = svc.loginAsGuest();

        assertNotNull(token);
        assertEquals(123, infra.getUserIdByToken(token));
    }

    // --- logout non‐guest branch issues new guest ---
    @Test
    void testLogout_NonGuestSuccess() throws Exception {
        // existing token for a non-guest
        String old = "t0";
        AuthTokenRepository infra = new AuthTokenRepository();
        AuthTokenService realAuth = new AuthTokenService(infra);

        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        // validate old token → user 50, who is not a guest
        when(auth.ValidateToken(old)).thenReturn(50);
        when(repo.isGuestById(50)).thenReturn(false);

        // stub out the guest-creation path (unused for non-guest logout)
        when(repo.addGuest()).thenReturn(60);
        doAnswer(inv -> realAuth.AuthenticateGuest(inv.getArgument(0)))
                .when(auth).AuthenticateGuest(anyInt());

        // stub out token invalidation
        doAnswer(inv -> {
            realAuth.Logout(old);
            return null;
        }).when(auth).Logout(old);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        String next = svc.logout(old);

        // Current implementation returns null for non-guest logout
        assertNull(next, "Non-guest logout should return null");
    }

    // --- getShopMembers failure path ---
    @Test
    void testGetShopMembers_ThrowsOnError() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getShopMembers(9)).thenThrow(new RuntimeException("fail"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.getShopMembers(9));
        assertTrue(ex.getMessage().contains("getShopMembers"));
    }

    // --- getSuspendedUsers empty list ---
    @Test
    void testGetSuspendedUsers_EmptyList() {
        UserRepository repo = mock(UserRepository.class);
        when(repo.getSuspendedUsers()).thenReturn(List.of());
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        List<Integer> sus = svc.getSuspendedUsers();
        assertTrue(sus.isEmpty());
    }

    // --- addRole repo failure ---
    @Test
    void testAddRole_RepoThrows() {
        int memberId = 3;
        Role r = new Role(1, 5, new PermissionsEnum[] {});
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenReturn(Map.of(memberId, mock(Member.class)));
        when(repo.getUserById(memberId)).thenReturn(mock(Member.class));
        when(repo.isSuspended(memberId)).thenReturn(false);
        doThrow(new RuntimeException("fail")).when(repo).addRoleToPending(memberId, r);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.addRole(memberId, r));
        assertTrue(ex.getMessage().contains("addRole"));
    }

    @Test
    void testRemoveManagerFromStore_Success() throws Exception {
        String token = "t";
        int owner = 1, mgr = 2, shop = 3;

        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(owner);
        when(repo.isSuspended(owner)).thenReturn(false);
        // user 2 has a manager‐role assigned by user 1 on shop 3
        Role role = new Role(owner, shop, new PermissionsEnum[] { PermissionsEnum.manageItems });
        when(repo.getRole(mgr, shop)).thenReturn(role);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.removeManagerFromStore(token, mgr, shop));
        verify(repo).removeRole(mgr, shop);
    }

    @Test
    void testUpdateMemberAddress_InvalidMemberMappingThrows() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(10);
        // no such member in mapping
        when(repo.getUserMapping()).thenReturn(Map.of());

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertThrows(OurArg.class, () -> svc.updateMemberAddress(token, "City", "Street", 1, "Postal"));
    }

    @Test
    void testUpdateMemberPassword_InvalidMemberMappingThrows() throws Exception {
        String token = "tok";
        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(20);
        // mapping missing
        when(repo.getUserMapping()).thenReturn(Map.of());

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertThrows(OurArg.class, () -> svc.updateMemberPassword(token, "newpass"));
    }

    @Test
    void testGetUserShoppingCartItems_SuccessById() {
        int userId = 7;
        ShoppingCart cart = mock(ShoppingCart.class);
        HashMap<Integer, HashMap<Integer, Integer>> items = new HashMap<>();
        when(cart.getItems()).thenReturn(items);

        UserRepository repo = mock(UserRepository.class);
        when(repo.getShoppingCartById(userId)).thenReturn(cart);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertSame(items, svc.getUserShoppingCartItems(userId));
    }

    @Test
    void testHasRole_NonexistentUserMappingThrows() {
        int id = 99, shop = 1;
        Role r = new Role(id, shop, null);

        UserRepository repo = mock(UserRepository.class);
        // simulate missing user
        when(repo.getUserMapping()).thenReturn(Map.of());
        when(repo.isSuspended(id)).thenReturn(false);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertThrows(OurArg.class, () -> svc.hasRole(id, r));
    }

    @Test
    void testAddRole_NullRole() {
        UserService svc = new UserService(mock(UserRepository.class), mock(AuthTokenService.class),
                notificationService);
        notificationService.setService(svc);
        OurArg ex = assertThrows(OurArg.class, () -> svc.addRole(1, null));
        assertTrue(ex.getMessage().contains("addRole"));
    }

    @Test
    void testHasPermission_SuccessAndMissingPermission() {
        int userId = 1, shopId = 2;
        PermissionsEnum perm = PermissionsEnum.manageItems;

        // mock the Member and two Role instances
        Member m = mock(Member.class);
        Role rWithPerm = new Role(userId, shopId, new PermissionsEnum[] { perm });
        Role rWithoutPerm = new Role(userId, shopId, new PermissionsEnum[] {});

        UserRepository repo = mock(UserRepository.class);
        // stub mapping and suspension checks
        when(repo.getUserMapping()).thenReturn(Map.of(userId, m));
        when(repo.isSuspended(userId)).thenReturn(false);
        when(repo.getUserById(userId)).thenReturn(m);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        // CASE: member has a role but it doesn’t grant the permission
        when(m.getRoles()).thenReturn(List.of(rWithoutPerm));
        assertFalse(svc.hasPermission(userId, perm, shopId),
                "Expected hasPermission to return false when the role lacks it");
    }

    @Test
    void testChangePermissions_EmptyPerms_NoMatchingRoles() throws Exception {
        String token = "tok";
        int owner = 1, target = 2, shopId = 3;

        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(owner);
        when(repo.isSuspended(owner)).thenReturn(false);
        when(repo.isOwner(owner, shopId)).thenReturn(true);

        Member m = mock(Member.class);
        when(repo.getMemberById(target)).thenReturn(m);
        when(m.getRoles()).thenReturn(List.of()); // no existing roles

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        // should simply no‐op, not call setPermissions
        assertDoesNotThrow(() -> svc.changePermissions(token, target, shopId, new PermissionsEnum[] {}));
        verify(repo, never()).setPermissions(anyInt(), anyInt(), any(Role.class), any());
    }

    @Test
    void testRemoveOwnerFromStore_RepoError() throws Exception {
        String token = "tk";
        int owner = 1, newOwner = 2, shopId = 3;

        UserRepository repo = mock(UserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        when(auth.ValidateToken(token)).thenReturn(owner);
        when(repo.isSuspended(owner)).thenReturn(false);
        Role role = new Role(owner, shopId, null);
        role.setOwnersPermissions();
        when(repo.getRole(newOwner, shopId)).thenReturn(role);

        // removeRole blows up
        doThrow(new RuntimeException("dbFail"))
                .when(repo).removeRole(newOwner, shopId);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeOwnerFromStore(token, newOwner, shopId));
        assertTrue(ex.getMessage().contains("removeOwnerFromStore"));
    }

    @Test
    void testGetNotificationsAndClear_ReturnsList() throws Exception {
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);

        String token = "tok";
        when(mockAuth.ValidateToken(token)).thenReturn(10);
        List<String> notes = List.of("note1", "note2");
        when(mockRepo.getNotificationsAndClear(10)).thenReturn(notes);

        List<String> result = svc.getNotificationsAndClear(token);
        assertEquals(2, result.size());
        assertTrue(result.contains("note1") && result.contains("note2"));
    }

    @Test
    void testCloseShopNotification_MultipleOwners() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        Member o1 = mock(Member.class), o2 = mock(Member.class);
        when(o1.getMemberId()).thenReturn(3);
        when(o2.getMemberId()).thenReturn(4);
        when(mockRepo.getOwners(7)).thenReturn(List.of(o1, o2));

        svc.closeShopNotification(7);

        verify(notificationService).sendToUser(3,
                "Shop Closed",
                "Your shop ID: 7 has been closed.");
        verify(notificationService).sendToUser(4,
                "Shop Closed",
                "Your shop ID: 7 has been closed.");
    }

    @Test
    void testPurchaseNotification_MultipleShopsAndItems() {
        UserRepository mockRepo = mock(UserRepository.class);
        UserService svc = new UserService(mockRepo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        Member owner5 = mock(Member.class), owner6 = mock(Member.class);
        when(owner5.getMemberId()).thenReturn(11);
        when(owner6.getMemberId()).thenReturn(12);
        when(mockRepo.getOwners(5)).thenReturn(List.of(owner5));
        when(mockRepo.getOwners(6)).thenReturn(List.of(owner6));

        HashMap<Integer, HashMap<Integer, Integer>> cart = new HashMap<>();
        HashMap<Integer, Integer> items5 = new HashMap<>();
        items5.put(2, 1);
        cart.put(5, items5);
        HashMap<Integer, Integer> items6 = new HashMap<>();
        items6.put(3, 4);
        cart.put(6, items6);

        svc.purchaseNotification(cart);

        verify(notificationService).sendToUser(11,
                "Item 2 Purchased",
                "Quantity: 1 purchased from your shop ID: 5");
        verify(notificationService).sendToUser(12,
                "Item 3 Purchased",
                "Quantity: 4 purchased from your shop ID: 6");
    }

    @Test
    void testGetPendingRoles_EmptyList() throws Exception {
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken("tok")).thenReturn(20);
        when(mockRepo.getPendingRoles(20)).thenReturn(List.of());

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        List<Role> lst = svc.getPendingRoles("tok");
        assertTrue(lst.isEmpty());
    }

    @Test
    void testGetAcceptedRoles_EmptyList() throws Exception {
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken("tok")).thenReturn(30);
        when(mockRepo.getAcceptedRoles(30)).thenReturn(List.of());

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        List<Role> lst = svc.getAcceptedRoles("tok");
        assertTrue(lst.isEmpty());
    }

    @Test
    void testGetAllAdmins_EmptyList() throws Exception {
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        when(mockAuth.ValidateToken("tok")).thenReturn(1);
        when(mockRepo.isAdmin(1)).thenReturn(true);
        when(mockRepo.getAllAdmins()).thenReturn(List.of());

        UserService svc = new UserService(mockRepo, mockAuth, notificationService);
        notificationService.setService(svc);
        List<Integer> admins = svc.getAllAdmins("tok");
        assertTrue(admins.isEmpty());
    }

    // --- hasPermission: mapping throws (e.g. malformed repo) → false ---
    @Test
    void testHasPermission_MappingErrorReturnsFalse() {
        int userId = 5, shopId = 6;
        PermissionsEnum perm = PermissionsEnum.closeShop;

        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenThrow(new RuntimeException("boom"));

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        // we swallow mapping errors and return false
        assertFalse(svc.hasPermission(userId, perm, shopId),
                "Expected false when getUserMapping() fails");
    }

    // --- hasPermission: non-Member in mapping → false silently ---
    @Test
    void testHasPermission_NonMemberMappingReturnsFalse() {
        int userId = 9, shopId = 10;
        PermissionsEnum perm = PermissionsEnum.manageOwners;

        // mapping contains a User (not a Member)
        com.example.app.DomainLayer.User notMember = mock(com.example.app.DomainLayer.User.class);
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenReturn(Map.of(userId, notMember));
        when(repo.isSuspended(userId)).thenReturn(false);
        when(repo.getUserById(userId)).thenReturn(notMember);

        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertFalse(svc.hasPermission(userId, perm, shopId),
                "Expected false when the mapped user isn’t a Member");
    }

    // --- acceptRole: invalid token → OurArg ---
    @Test
    void testAcceptRole_InvalidToken_ThrowsOurArg() throws Exception {
        AuthTokenService badAuth = mock(AuthTokenService.class);
        when(badAuth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mock(UserRepository.class), badAuth, notificationService);
        notificationService.setService(svc);

        assertThrows(OurArg.class, () -> svc.acceptRole("bad", 42),
                "acceptRole should bubble up an OurArg on invalid token");
    }

    // --- acceptRole: repo.getPendingRole throws → OurRuntime ---
    @Test
    void testAcceptRole_RepoError_ThrowsOurRuntime() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("tok")).thenReturn(7);
        UserRepository repo = mock(UserRepository.class);
        when(repo.getPendingRole(7, 99)).thenThrow(new RuntimeException("dbErr"));

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.acceptRole("tok", 99));
        assertTrue(ex.getMessage().contains("acceptRole"));
    }

    // --- declineRole: invalid token → OurArg ---
    @Test
    void testDeclineRole_InvalidToken_ThrowsOurArg() throws Exception {
        AuthTokenService badAuth = mock(AuthTokenService.class);
        when(badAuth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mock(UserRepository.class), badAuth, notificationService);
        notificationService.setService(svc);

        assertThrows(OurArg.class, () -> svc.declineRole("bad", 17),
                "declineRole should bubble up an OurArg on invalid token");
    }

    // --- removeOwnerFromStore: null role → OurRuntime ---
    @Test
    void testRemoveOwnerFromStore_NullRole_ThrowsOurRuntime() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("tok")).thenReturn(10);
        UserRepository repo = mock(UserRepository.class);
        when(repo.isSuspended(10)).thenReturn(false);
        when(repo.getRole(2, 5)).thenReturn(null);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeOwnerFromStore("tok", 2, 5));
        assertTrue(ex.getMessage().contains("removeOwnerFromStore"));
    }

    // --- removeOwnerFromStore: not‐an‐owner role → OurRuntime ---
    @Test
    void testRemoveOwnerFromStore_NotOwnerRole_ThrowsOurRuntime() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("tok")).thenReturn(8);
        UserRepository repo = mock(UserRepository.class);
        when(repo.isSuspended(8)).thenReturn(false);

        // role exists but without owner perms
        Role notOwner = new Role(8, 66, null);
        when(repo.getRole(9, 66)).thenReturn(notOwner);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeOwnerFromStore("tok", 9, 66));
        assertTrue(ex.getMessage().contains("is not an owner"));
    }

    // --- removeOwnerFromStore: repo.removeRole throws → OurRuntime ---
    @Test
    void testRemoveOwnerFromStore_RepoRemoveFails_ThrowsOurRuntime() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("tok")).thenReturn(3);
        UserRepository repo = mock(UserRepository.class);
        when(repo.isSuspended(3)).thenReturn(false);

        Role owner = new Role(3, 7, null);
        owner.setOwnersPermissions();
        when(repo.getRole(4, 7)).thenReturn(owner);
        doThrow(new RuntimeException("cant remove")).when(repo).removeRole(4, 7);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeOwnerFromStore("tok", 4, 7));
        assertTrue(ex.getMessage().contains("removeOwnerFromStore"));
    }

    // --- updateMemberAddress: invalid token → OurArg ---
    @Test
    void testUpdateMemberAddress_InvalidToken_ThrowsOurArg() throws Exception {
        AuthTokenService badAuth = mock(AuthTokenService.class);
        when(badAuth.ValidateToken("zzz")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mock(UserRepository.class), badAuth, notificationService);
        notificationService.setService(svc);

        assertThrows(OurArg.class,
                () -> svc.updateMemberAddress("zzz", "City", "St", 1, "P"),
                "updateMemberAddress should bubble OurArg on invalid token");
    }

    // --- updateMemberAddress: suspended user → OurRuntime ---
    @Test
    void testUpdateMemberAddress_Suspended_ThrowsOurRuntime() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("tok")).thenReturn(20);
        UserRepository repo = mock(UserRepository.class);

        // mapping + suspended
        Member m = mock(Member.class);
        when(repo.getUserMapping()).thenReturn(Map.of(20, m));
        when(repo.getUserById(20)).thenReturn(m);
        when(repo.isSuspended(20)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.updateMemberAddress("tok", "C", "S", 2, "P"));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    // --- updateMemberAddress: no such member → OurArg ---
    @Test
    void testUpdateMemberAddress_NoMember_ThrowsOurArg() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("tok")).thenReturn(30);
        UserRepository repo = mock(UserRepository.class);
        when(repo.getUserMapping()).thenReturn(Map.of()); // empty

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        assertThrows(OurArg.class,
                () -> svc.updateMemberAddress("tok", "X", "Y", 3, "Z"));
    }

    // --- getUserById: repo throws RuntimeException → OurRuntime ---
    @Test
    void testGetUserById_RepoThrowsRuntime_ThrowsOurRuntime() {
        IUserRepository repo = mock(IUserRepository.class);
        when(repo.getUserById(99)).thenThrow(new RuntimeException("oops"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.getUserById(99));
        assertTrue(ex.getMessage().contains("getUserById:"));
    }

    // --- getUserById: repo throws OurArg → propagates OurArg ---
    @Test
    void testGetUserById_RepoThrowsOurArg_Propagates() {
        IUserRepository repo = mock(IUserRepository.class);
        when(repo.getUserById(42)).thenThrow(new OurArg("bad id"));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        assertThrows(OurArg.class, () -> svc.getUserById(42));
    }

    // --- removeManagerFromStore: null role → OurRuntime ---
    @Test
    void testRemoveManagerFromStore_NullRole_ThrowsOurRuntime() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("tok")).thenReturn(99);
        UserRepository repo = mock(UserRepository.class);
        when(repo.isSuspended(99)).thenReturn(false);
        when(repo.getRole(7, 8)).thenReturn(null);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeManagerFromStore("tok", 7, 8));
        assertTrue(ex.getMessage().contains("is not a manager"));
    }

    // --- removeManagerFromStore: suspended user → OurRuntime ---
    @Test
    void testRemoveManagerFromStore_Suspended_ThrowsOurRuntime() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("tok")).thenReturn(55);
        UserRepository repo = mock(UserRepository.class);
        when(repo.isSuspended(55)).thenReturn(true);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeManagerFromStore("tok", 60, 6));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    // --- getBasketItems: success path ---
    @Test
    void testGetBasketItems_SuccessPath() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("tkn")).thenReturn(3);

        UserRepository repo = mock(UserRepository.class);
        Map<Integer, Integer> basket = Map.of(101, 5);
        when(repo.getBasket(3, 1)).thenReturn(basket);

        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);
        Map<Integer, Integer> result = svc.getBasketItems("tkn", 1);
        assertEquals(5, result.get(101));
    }

    // --- getPendingRoles: invalid token → OurArg ---
    @Test
    void testGetPendingRoles_InvalidToken_ThrowsOurArg() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);

        assertThrows(OurArg.class, () -> svc.getPendingRoles("bad"));
    }

    // --- getAcceptedRoles: invalid token → OurArg ---
    @Test
    void testGetAcceptedRoles_InvalidToken_ThrowsOurArg() throws Exception {
        AuthTokenService auth = mock(AuthTokenService.class);
        when(auth.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mock(UserRepository.class), auth, notificationService);
        notificationService.setService(svc);

        assertThrows(OurArg.class, () -> svc.getAcceptedRoles("bad"));
    }

    @Test
    void testLogout_GuestRemovesAndIssuesNewToken() throws Exception {
        // make a real guest in the real repos
        String oldGuest = userService.loginAsGuest();
        int oldId = authTokenRepository.getUserIdByToken(oldGuest);
        assertTrue(userRepository.isGuestById(oldId));

        String newGuest = userService.logout(oldGuest);
        assertNotNull(newGuest);
        int newId = authTokenRepository.getUserIdByToken(newGuest);
        assertTrue(userRepository.isGuestById(newId));
        assertNotEquals(oldId, newId);
    }

    // --- updateMemberUsername(String,String) ---
    @Test
    void testUpdateMemberUsername_Success() throws Exception {
        userService.addMember("jon", "pw", "j@e.com", "0123456789", "addr");
        int id = userRepository.isUsernameAndPasswordValid("jon", "pw");
        String tok = authTokenService.Login("jon", "pw", id);

        userService.updateMemberUsername(tok, "jonny");
        Member m = (Member) userService.getUserById(id);
        assertEquals("jonny", m.getUsername());
    }

    @Test
    void testUpdateMemberUsername_SuspendedThrows() throws Exception {
        UserRepository mockRepo = mock(UserRepository.class);
        AuthTokenService mockAuth = mock(AuthTokenService.class);
        NotificationService mockNotify = mock(NotificationService.class);
        when(mockAuth.ValidateToken("tok")).thenReturn(7);
        when(mockRepo.isSuspended(7)).thenReturn(true);
        when(mockRepo.getUserMapping()).thenReturn(Map.of(7, mock(Member.class)));
        when(mockRepo.getUserById(7)).thenReturn(mock(Member.class));
        UserService svc = new UserService(mockRepo, mockAuth, mockNotify);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.updateMemberUsername("tok", "new"));
        assertTrue(ex.getMessage().contains("updateMemberUsername"));
    }

    // --- makeManagerOfStore(String,int,int,PermissionsEnum[]) ---
    @Test
    void testMakeManagerOfStore_NotOwnerThrows() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("t")).thenReturn(1);
        when(r.isSuspended(1)).thenReturn(false);
        when(r.isOwner(1, 5)).thenReturn(false);
        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.makeManagerOfStore("t", 2, 5, new PermissionsEnum[] {}));
        assertTrue(ex.getMessage().contains("not an owner"));
    }

    @Test
    void testMakeManagerOfStore_SuspendedThrows() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("t")).thenReturn(9);
        when(r.isSuspended(9)).thenReturn(true);
        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.makeManagerOfStore("t", 2, 5, new PermissionsEnum[] {}));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    @Test
    void testMakeManagerOfStore_Success() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("t")).thenReturn(1);
        when(r.isSuspended(1)).thenReturn(false);
        when(r.isOwner(1, 5)).thenReturn(true);
        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);

        assertDoesNotThrow(
                () -> svc.makeManagerOfStore("t", 2, 5, new PermissionsEnum[] { PermissionsEnum.handleMessages }));
        verify(r).addRoleToPending(eq(2), any(Role.class));
    }

    // --- changePermissions(String,int,int,PermissionsEnum[]) ---
    @Test
    void testChangePermissions_InvalidTokenThrows() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);

        assertThrows(OurArg.class,
                () -> svc.changePermissions("bad", 2, 5, new PermissionsEnum[] { PermissionsEnum.manageItems }));
    }

    @Test
    void testChangePermissions_NotOwnerThrows() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("tok")).thenReturn(1);
        when(r.isSuspended(1)).thenReturn(false);
        when(r.isOwner(1, 5)).thenReturn(false);
        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.changePermissions("tok", 2, 5, new PermissionsEnum[] { PermissionsEnum.manageItems }));
        assertTrue(ex.getMessage().contains("not an owner"));
    }

    @Test
    void testChangePermissionsHandleMessages_Success() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("tok")).thenReturn(1);
        when(r.isSuspended(1)).thenReturn(false);
        when(r.isOwner(1, 5)).thenReturn(true);

        Member m = mock(Member.class);
        Role existing = new Role(1, 5, new PermissionsEnum[] { PermissionsEnum.handleMessages });
        when(r.getMemberById(2)).thenReturn(m);
        when(m.getRoles()).thenReturn(List.of(existing));

        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(
                () -> svc.changePermissions("tok", 2, 5, new PermissionsEnum[] { PermissionsEnum.manageItems }));
        verify(r).setPermissions(2, 5, existing, new PermissionsEnum[] { PermissionsEnum.manageItems });
    }

    // --- removeRole(int, Role) ---
    @Test
    void testRemoveRole_NullRoleThrows() {
        UserService svc = new UserService(mock(UserRepository.class), mock(AuthTokenService.class),
                notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.removeRole(1, null));
        assertTrue(ex.getMessage().contains("Role cannot be null"));
    }

    @Test
    void testRemoveRole_NotAssigneeThrows() {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        Role param = new Role(3, 8, null);
        Role existing = new Role(4, 8, null);

        Member m = mock(Member.class);
        when(r.getUserMapping()).thenReturn(Map.of(3, m));
        when(r.getUserById(3)).thenReturn(m);
        when(r.isSuspended(3)).thenReturn(false);
        when(r.getRole(3, 8)).thenReturn(existing);

        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.removeRole(3, param));
        assertTrue(ex.getMessage().contains("is not the assignee"));
    }

    @Test
    void testGetPermitionsByShop_Success() throws Exception {
        // arrange
        IUserRepository r = mock(IUserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);

        when(a.ValidateToken("t")).thenReturn(1);
        when(r.isOwner(1, 5)).thenReturn(true);

        Member m = mock(Member.class);
        when(m.getMemberId()).thenReturn(2);
        Role role = new Role(
                /* assigneeId */ 1,
                /* shopId */ 5,
                /* perms */ new PermissionsEnum[] { PermissionsEnum.manageItems });
        when(m.getRoles()).thenReturn(List.of(role));

        when(r.getMembersList()).thenReturn(List.of(m));

        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);

        // act
        Map<Integer, PermissionsEnum[]> perms = svc.getPermitionsByShop("t", 5);

        // assert
        assertEquals(1, perms.size());
        assertArrayEquals(
                new PermissionsEnum[] { PermissionsEnum.manageItems },
                perms.get(2));
    }

    @Test
    void testGetAllMembers_RepoThrows() {
        UserRepository r = mock(UserRepository.class);
        when(r.getAllMembers()).thenThrow(new RuntimeException("boom"));
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, svc::getAllMembers);
        assertTrue(ex.getMessage().contains("getAllUsers"));
    }

    // --- declineRole(String,int) ---
    @Test
    void testDeclineRole_NoPendingThrows() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("t")).thenReturn(4);
        when(r.getPendingRole(4, 9)).thenReturn(null);
        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.declineRole("t", 9));
        assertTrue(ex.getMessage().contains("has no pending role"));
    }

    // --- removeOwnerFromStore(String,int,int) ---
    @Test
    void testRemoveOwnerFromStore_InvalidTokenThrows() throws Exception {
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mock(UserRepository.class), a, notificationService);
        notificationService.setService(svc);

        assertThrows(OurRuntime.class, () -> svc.removeOwnerFromStore("bad", 1, 2));
    }

    @Test
    void testRemoveOwnerFromStore_NullRoleThrows() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("t")).thenReturn(1);
        when(r.isSuspended(1)).thenReturn(false);
        when(r.getRole(2, 3)).thenReturn(null);
        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.removeOwnerFromStore("t", 2, 3));
        assertTrue(ex.getMessage().contains("removeOwnerFromStore"));
    }

    @Test
    void testRemoveOwnerFromStore_NotOwnerThrows() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        Role notOwner = new Role(4, 5, null);
        when(a.ValidateToken("t")).thenReturn(4);
        when(r.isSuspended(4)).thenReturn(false);
        when(r.getRole(6, 5)).thenReturn(notOwner);
        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.removeOwnerFromStore("t", 6, 5));
        assertTrue(ex.getMessage().contains("is not an owner"));
    }

    @Test
    void testRemoveOwnerFromStore_Success() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        Role own = new Role(1, 2, null);
        own.setOwnersPermissions();
        when(a.ValidateToken("t")).thenReturn(1);
        when(r.isSuspended(1)).thenReturn(false);
        when(r.getRole(3, 2)).thenReturn(own);
        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);

        assertDoesNotThrow(() -> svc.removeOwnerFromStore("t", 3, 2));
        verify(r).removeRole(3, 2);
    }

    // --- updateMemberAddress(String,String,String,int,String) ---
    @Test
    void testUpdateMemberAddress_InvalidTokenThrows() throws Exception {
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mock(UserRepository.class), a, notificationService);
        notificationService.setService(svc);

        assertThrows(OurArg.class, () -> svc.updateMemberAddress("bad", "C", "S", 1, "P"));
    }

    @Test
    void testUpdateMemberAddress_SuspendedThrows() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        Member m = mock(Member.class);
        when(a.ValidateToken("t")).thenReturn(9);
        when(r.getUserMapping()).thenReturn(Map.of(9, m));
        when(r.getUserById(9)).thenReturn(m);
        when(r.isSuspended(9)).thenReturn(true);
        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.updateMemberAddress("t", "C", "S", 1, "P"));
        assertTrue(ex.getMessage().contains("the user is suspended"));
    }

    @Test
    void testGetUserById_RepoRuntimeThrows() {
        IUserRepository r = mock(IUserRepository.class);
        when(r.getUserById(99)).thenThrow(new RuntimeException("oops"));
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.getUserById(99));
        assertTrue(ex.getMessage().contains("getUserById:"));
    }

    @Test
    void testGetUserById_RepoOurArgPropagates() {
        IUserRepository r = mock(IUserRepository.class);
        when(r.getUserById(7)).thenThrow(new OurArg("no"));
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        assertThrows(OurArg.class, () -> svc.getUserById(7));
    }

    // --- removeManagerFromStore(String,int,int) ---
    @Test
    void testRemoveManagerFromStore_InvalidTokenThrows() throws Exception {
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("bad")).thenThrow(new OurArg("noAuth"));
        UserService svc = new UserService(mock(UserRepository.class), a, notificationService);
        notificationService.setService(svc);

        assertThrows(OurArg.class, () -> svc.removeManagerFromStore("bad", 2, 3));
    }

    @Test
    void testRemoveManagerFromStore_NullRoleThrows() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("t")).thenReturn(5);
        when(r.isSuspended(5)).thenReturn(false);
        when(r.getRole(6, 7)).thenReturn(null);
        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.removeManagerFromStore("t", 6, 7));
        assertTrue(ex.getMessage().contains("is not a manager"));
    }

    // --- removedAppointment(Integer,String,Integer) ---
    @Test
    void testRemovedAppointment_NoShop() {
        UserRepository r = mock(UserRepository.class);
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.removedAppointment(5, "Dentist", null));
        verify(notificationService).sendToUser(5, "Appointment Removed",
                "Your appointment to: Dentist has been removed.");
    }

    @Test
    void testRemovedAppointment_WithShop() {
        UserRepository r = mock(UserRepository.class);
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        svc.removedAppointment(6, "Checkup", 42);
        verify(notificationService).sendToUser(6, "Appointment Removed",
                "Your appointment to: Checkup in the shop 42 has been removed.");
    }

    @Test
    void testRemovedAppointment_FailureWrapped() {
        UserRepository r = mock(UserRepository.class);
        doThrow(new RuntimeException("boom"))
                .when(notificationService).sendToUser(anyInt(), anyString(), anyString());
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.removedAppointment(7, "X", null));
        assertTrue(ex.getMessage().contains("removedAppointment"));
    }

    // --- closeShopNotification(Integer) ---
    @Test
    void testCloseShopNotification_EmptyOwnersNoError() {
        UserRepository r = mock(UserRepository.class);
        when(r.getOwners(11)).thenReturn(List.of());
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        assertDoesNotThrow(() -> svc.closeShopNotification(11));
    }

    @Test
    void testCloseShopNotification_SuccessAndFailure() {
        UserRepository r = mock(UserRepository.class);
        Member o = mock(Member.class);
        when(o.getMemberId()).thenReturn(21);
        when(r.getOwners(11)).thenReturn(List.of(o));
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        svc.closeShopNotification(11);
        verify(notificationService).sendToUser(21, "Shop Closed", "Your shop ID: 11 has been closed.");

        when(r.getOwners(99)).thenThrow(new RuntimeException("err"));
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.closeShopNotification(99));
        assertTrue(ex.getMessage().contains("closeShopNotification"));
    }

    // --- getBasketItems(String,int) ---
    @Test
    void testGetBasketItems_InvalidTokenThrows() {
        assertThrows(OurRuntime.class, () -> userService.getBasketItems("bad", 1));
    }

    @Test
    void testGetBasketItems_Success() throws Exception {
        UserRepository r = mock(UserRepository.class);
        AuthTokenService a = mock(AuthTokenService.class);
        when(a.ValidateToken("tkn")).thenReturn(3);
        when(r.getBasket(3, 1)).thenReturn(Map.of(101, 5));

        UserService svc = new UserService(r, a, notificationService);
        notificationService.setService(svc);
        assertEquals(5, svc.getBasketItems("tkn", 1).get(101));
    }

    // --- restoreUserShoppingCart(int,HashMap) ---
    @Test
    void testRestoreUserShoppingCart_NullCartThrows() {
        UserRepository r = mock(UserRepository.class);
        when(r.getShoppingCartById(9)).thenReturn(null);
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.restoreUserShoppingCart(9, new HashMap<>()));
        assertTrue(ex.getMessage().contains("restoreUserShoppingCart"));
    }

    // --- setSuspended(int,LocalDateTime) & isSuspended(int) ---

    @Test
    void testSetSuspended_RepoErrorWrapped() {
        UserRepository r = mock(UserRepository.class);
        doThrow(new RuntimeException("db")).when(r).setSuspended(eq(5), any(LocalDateTime.class));
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.setSuspended(5, LocalDateTime.now()));
        assertTrue(ex.getMessage().contains("Error setting suspension"));
    }

    @Test
    void testIsSuspended_RepoThrowsWrapped() {
        UserRepository r = mock(UserRepository.class);
        when(r.isSuspended(6)).thenThrow(new RuntimeException("fail"));
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.isSuspended(6));
        assertTrue(ex.getMessage().contains("isSuspended: fail"));
    }

    // --- getUserShippingAddress(int) ---
    @Test
    void testGetUserShippingAddress_RepoThrowsWrapped() {
        UserRepository r = mock(UserRepository.class);
        when(r.getUserById(8)).thenThrow(new RuntimeException("nope"));
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.getUserShippingAddress(8));
        assertTrue(ex.getMessage().contains("Error fetching shipping address"));
    }

    // --- messageNotification(Integer,Integer,boolean) ---
    @Test
    void testMessageNotification_ShopAndUser() {
        UserRepository r = mock(UserRepository.class);
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        svc.messageNotification(10, 5, true);
        verify(notificationService).sendToUser(10, "Message Received",
                "You have received a new message from the shop (id=5).");

        svc.messageNotification(11, 0, false);
        verify(notificationService).sendToUser(11, "Message Received",
                "You have received a new message from the user (id=11).");
    }

    @Test
    void testMessageNotification_FailureWrapped() {
        UserRepository r = mock(UserRepository.class);
        doThrow(new RuntimeException("err")).when(notificationService).sendToUser(anyInt(), anyString(), anyString());
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class,
                () -> svc.messageNotification(12, 3, true));
        assertTrue(ex.getMessage().contains("messageUserNotification"));
    }

    // --- getShopIdsByWorkerId(int) ---
    @Test
    void testGetShopIdsByWorkerId_FailureAndSuccess() {
        UserRepository r = mock(UserRepository.class);
        when(r.getShopIdsByWorkerId(7)).thenReturn(List.of(100, 101));
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertEquals(List.of(100, 101), svc.getShopIdsByWorkerId(7));

        when(r.getShopIdsByWorkerId(8)).thenThrow(new RuntimeException("db"));
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.getShopIdsByWorkerId(8));
        assertTrue(ex.getMessage().contains("getShopsByUserId"));
    }

    // --- getShopMembers(int) ---
    @Test
    void testGetShopMembers_FailureAndSuccess() {
        Member m = mock(Member.class);
        UserRepository r = mock(UserRepository.class);
        when(r.getShopMembers(55)).thenReturn(List.of(m));
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertEquals(List.of(m), svc.getShopMembers(55));

        when(r.getShopMembers(56)).thenThrow(new RuntimeException("oops"));
        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.getShopMembers(56));
        assertTrue(ex.getMessage().contains("getShopMembers"));
    }

    // --- clearUserShoppingCart(int) & getUserShoppingCartItems(int) ---
    @Test
    void testClearAndGetUserShoppingCartItemsById() {
        ShoppingCart c = mock(ShoppingCart.class);
        when(c.getItems()).thenReturn(new HashMap<>());
        UserRepository r = mock(UserRepository.class);
        when(r.getShoppingCartById(9)).thenReturn(c);
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        // clear
        assertDoesNotThrow(() -> svc.clearUserShoppingCart(9));
        verify(c).clearCart();

        // get items
        assertSame(c.getItems(), svc.getUserShoppingCartItems(9));
    }

    @Test
    void testGetUserShoppingCartItems_RepoErrorWrapped() {
        UserRepository r = mock(UserRepository.class);
        when(r.getShoppingCartById(10)).thenThrow(new RuntimeException("fail"));
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        OurRuntime ex = assertThrows(OurRuntime.class, () -> svc.getUserShoppingCartItems(10));
        assertTrue(ex.getMessage().contains("getUserShoppingCart"));
    }

    // --- getSuspendedUsers() ---
    @Test
    void testGetSuspendedUsers_SuccessAndFailure() {
        UserRepository r = mock(UserRepository.class);
        when(r.getSuspendedUsers()).thenReturn(List.of(2, 3));
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertEquals(List.of(2, 3), svc.getSuspendedUsers());

        when(r.getSuspendedUsers()).thenThrow(new RuntimeException("dbErr"));
        OurRuntime ex = assertThrows(OurRuntime.class, svc::getSuspendedUsers);
        assertTrue(ex.getMessage().contains("getSuspendedUsers"));
    }

    // --- removeAllAssigned(int,int) ---
    @Test
    void testRemoveAllAssigned_EmptyDoesNothing() {
        UserRepository r = mock(UserRepository.class);
        when(r.getMembersList()).thenReturn(List.of());
        UserService svc = new UserService(r, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        assertDoesNotThrow(() -> svc.removeAllAssigned(1, 2));
    }

    @Test
    void testRemoveAllAssigned_RemovesEveryMatching() {
        Role r1 = new Role(5, 10, null), r2 = new Role(5, 10, null);
        Member m1 = mock(Member.class), m2 = mock(Member.class);
        when(m1.getMemberId()).thenReturn(100);
        when(m2.getMemberId()).thenReturn(101);
        when(m1.getRoles()).thenReturn(List.of(r1));
        when(m2.getRoles()).thenReturn(List.of(r2));

        UserRepository repo = mock(UserRepository.class);
        when(repo.getMembersList()).thenReturn(List.of(m1, m2));
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        svc.removeAllAssigned(5, 10);
        verify(repo).removeRole(100, 10);
        verify(repo).removeRole(101, 10);
    }

    @Test
    void closeShopNotification_sendsOneNotificationPerOwner() {
        // mock the interface
        IUserRepository repo = mock(IUserRepository.class);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        int shopId = 42;
        Member owner1 = mock(Member.class);
        Member owner2 = mock(Member.class);
        when(owner1.getMemberId()).thenReturn(101);
        when(owner2.getMemberId()).thenReturn(202);
        when(repo.getOwners(shopId)).thenReturn(List.of(owner1, owner2));

        svc.closeShopNotification(shopId);

        // 1) the getOwners(...) call
        verify(repo).getOwners(shopId);

        verify(notificationService).sendToUser(
                eq(101),
                eq("Shop Closed"),
                eq("Your shop ID: 42 has been closed."));
        verify(notificationService).sendToUser(
                eq(202),
                eq("Shop Closed"),
                eq("Your shop ID: 42 has been closed."));

        // no other interactions
        verifyNoMoreInteractions(repo);
    }

    @Test
    void getBasketItems_returnsRepositoryBasket() throws Exception {
        // 1. Mock the interface and AuthTokenService
        IUserRepository repo = mock(IUserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);

        // 2. Inject both mocks into your service
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        // 3. Stub ValidateToken on the *same* auth mock
        String token = "tok";
        int shopId = 7;
        int userId = 11;
        when(auth.ValidateToken(token)).thenReturn(userId);

        // 4. Stub the repository basket call
        Map<Integer, Integer> basket = Map.of(5, 2, 8, 1);
        when(repo.getBasket(userId, shopId)).thenReturn(basket);

        // 5. Execute and assert
        Map<Integer, Integer> result = svc.getBasketItems(token, shopId);
        assertSame(basket, result);
    }

    @Test
    void restoreUserShoppingCart_delegatesToShoppingCart() {
        UserRepository repo = mock(UserRepository.class);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        int userId = 13;
        @SuppressWarnings("unchecked")
        HashMap<Integer, HashMap<Integer, Integer>> items = new HashMap<>();
        ShoppingCart cart = mock(ShoppingCart.class);
        when(repo.getShoppingCartById(userId)).thenReturn(cart);

        svc.restoreUserShoppingCart(userId, items);
        verify(cart).restoreCart(items);
    }

    @Test
    void setSuspended_and_isSuspended_roundTripsToRepository() {
        UserRepository repo = mock(UserRepository.class);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        int userId = 21;
        LocalDateTime until = LocalDateTime.now().plusDays(1);

        svc.setSuspended(userId, until);
        verify(repo).setSuspended(userId, until);

        when(repo.isSuspended(userId)).thenReturn(true);
        assertTrue(svc.isSuspended(userId));

        when(repo.isSuspended(userId)).thenReturn(false);
        assertFalse(svc.isSuspended(userId));
    }

    @Test
    void getUserShippingAddress_returnsUserAddress() {
        UserRepository repo = mock(UserRepository.class);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        int userId = 31;
        Address addr = new Address();
        User u = mock(User.class);
        when(u.getAddress()).thenReturn(addr);
        when(repo.getUserById(userId)).thenReturn(u);

        Address result = svc.getUserShippingAddress(userId);
        assertSame(addr, result);
    }

    @Test
    void messageNotification_fromShop_and_fromUser_variants() {
        UserRepository repo = mock(UserRepository.class);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        // from shop
        svc.messageNotification(55, 66, true);
        verify(notificationService).sendToUser(
                eq(55),
                eq("Message Received"),
                eq("You have received a new message from the shop (id=66)."));

        // from user
        svc.messageNotification(77, 0, false);
        verify(notificationService).sendToUser(
                eq(77),
                eq("Message Received"),
                eq("You have received a new message from the user (id=77)."));

        verifyNoMoreInteractions(repo);
    }

    @Test
    void getShopIdsByWorkerId_and_getShopMembers_passThroughRepo() {
        UserRepository repo = mock(UserRepository.class);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        int userId = 88, shopId = 99;
        List<Integer> shops = List.of(3, 4, 5);
        List<Member> members = List.of(mock(Member.class), mock(Member.class));

        when(repo.getShopIdsByWorkerId(userId)).thenReturn(shops);
        when(repo.getShopMembers(shopId)).thenReturn(members);

        assertSame(shops, svc.getShopIdsByWorkerId(userId));
        assertSame(members, svc.getShopMembers(shopId));
    }

    @Test
    void clearUserShoppingCart_and_getUserShoppingCartItems_and_getUserShoppingCart_delegation() {
        // 1) Mock the interface, not the concrete class
        IUserRepository repo = mock(IUserRepository.class);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);

        int userId = 100;
        ShoppingCart cart = mock(ShoppingCart.class);

        // stub getShoppingCartById(...)
        when(repo.getShoppingCartById(userId)).thenReturn(cart);

        // clearUserShoppingCart should call clearCart()
        svc.clearUserShoppingCart(userId);
        verify(cart).clearCart();

        // build a real HashMap<HashMap<...>> for getItems()
        HashMap<Integer, HashMap<Integer, Integer>> items = new HashMap<>();
        HashMap<Integer, Integer> inner = new HashMap<>();
        inner.put(10, 2);
        items.put(1, inner);

        // stub cart.getItems() to return that HashMap
        when(cart.getItems()).thenReturn(items);

        // now getUserShoppingCartItems should return exactly that HashMap
        HashMap<Integer, HashMap<Integer, Integer>> result = svc.getUserShoppingCartItems(userId);
        assertSame(items, result);
    }

    @Test
    void getSuspendedUsers_passesThroughRepo() {
        UserRepository repo = mock(UserRepository.class);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        List<Integer> suspended = List.of(2, 4, 6);
        when(repo.getSuspendedUsers()).thenReturn(suspended);
        assertSame(suspended, svc.getSuspendedUsers());
    }

    @Test
    void removeAllAssigned_removesOnlyMatchingRoles() {
        UserRepository repo = mock(UserRepository.class);
        UserService svc = new UserService(repo, mock(AuthTokenService.class), notificationService);
        notificationService.setService(svc);
        int assignee = 200, shopId = 300, memberId = 400;
        Member m = mock(Member.class);
        Role role = mock(Role.class);

        when(role.getShopId()).thenReturn(shopId);
        when(role.getAssigneeId()).thenReturn(assignee);
        when(role.isOwner()).thenReturn(false);

        when(m.getRoles()).thenReturn(List.of(role));
        when(m.getMemberId()).thenReturn(memberId);

        when(repo.getMembersList()).thenReturn(List.of(m));

        svc.removeAllAssigned(assignee, shopId);
        verify(repo).removeRole(memberId, shopId);
    }

    @Test
    void removeAllAssigned_removesAllRolesAssignedByAssignee() {
        // 1) Arrange: mock the repo & service
        IUserRepository repo = mock(IUserRepository.class);
        AuthTokenService auth = mock(AuthTokenService.class);
        UserService svc = new UserService(repo, auth, notificationService);
        notificationService.setService(svc);

        int assignee = 7;
        int shopId = 3;

        // 2) Build two fake members, each with exactly one Role assigned by 'assignee'
        // in 'shopId'
        Member memberA = mock(Member.class);
        when(memberA.getMemberId()).thenReturn(100);
        Role roleA = new Role(assignee, shopId,
                new PermissionsEnum[] { PermissionsEnum.manageItems });
        when(memberA.getRoles()).thenReturn(List.of(roleA));

        Member memberB = mock(Member.class);
        when(memberB.getMemberId()).thenReturn(200);
        Role roleB = new Role(assignee, shopId,
                new PermissionsEnum[] { PermissionsEnum.manageItems });
        when(memberB.getRoles()).thenReturn(List.of(roleB));

        // 3) Stub getMembersList on the repo
        when(repo.getMembersList()).thenReturn(List.of(memberA, memberB));

        // 4) Act
        svc.removeAllAssigned(assignee, shopId);

        // 5) Assert: getMembersList + exactly one removeRole per member, nothing else
        verify(repo).getMembersList();
        verify(repo).removeRole(100, shopId);
        verify(repo).removeRole(200, shopId);
        verifyNoMoreInteractions(repo);
    }
}