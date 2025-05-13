package ApplicationLayerTests.User;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.User;
import com.example.app.InfrastructureLayer.AuthTokenRepository;
import com.example.app.InfrastructureLayer.UserRepository;


public class UserServiceTest {

    private UserRepository userRepository;
    private AuthTokenRepository authTokenRepository;
    private AuthTokenService authTokenService;
    private UserService userService;


    @BeforeEach
    void setUp() {
        authTokenRepository = new AuthTokenRepository();  // Your real repo
        authTokenService = new AuthTokenService(authTokenRepository); // Real service
        userRepository = new UserRepository();
        userRepository.setEncoderToTest(true); // Set the encoder to test mode
        userService = new UserService(userRepository);
        userService.setServices(authTokenService);
        userService.setEncoderToTest(true); // Set the encoder to test mode
    }

    @Test
    void testIsAdmin()
    {
        assertTrue(userService.isAdmin(userRepository.isUsernameAndPasswordValid("admin", "admin")));
    }

    @Test
    void addAdmin()
    {
        String token = userService.loginAsMember("admin", "admin", "");
        userService.addMember("username", "password", "email@email.com", "0123456789", "address");
        int userid = userRepository.isUsernameAndPasswordValid("username", "password");
        userService.makeAdmin(token, userid);
        assertTrue(userService.isAdmin(userid));
    }

    @Test
    void removeAdmin()
    {
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
        assertEquals("john", ((Member)member).getUsername());
        assertEquals("john@example.com", ((Member)member).getEmail());
    }

    @Test
    void testUpdateMember() {
        userService.addMember("john", "pass123", "john@example.com", "1234567890", "123 Main St");
        int memberId = userRepository.isUsernameAndPasswordValid("john", "pass123");
        String token = authTokenService.Login("john", "pass123",memberId); // No token generated, but user added
        userService.updateMemberUsername(token, "newusername");
        userService.updateMemberPassword(token, "newpassword");
        userService.updateMemberEmail(token, "newemail@example.com");
        userService.updateMemberPhoneNumber(token, "0987654321");

        User member = userService.getUserById(memberId);
        assertEquals("newusername", ((Member)member).getUsername());
        assertEquals("newemail@example.com", ((Member)member).getEmail());
        assertEquals("0987654321", ((Member)member).getPhoneNumber());

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

    
    // @Test
    // void testAddAndCheckRole() {
    //     userService.addMember("sara", "pass", "sara@mail.com", "123", "address");
    //     int memberId = userRepository.isUsernameAndPasswordValid("sara", "pass");

    //     Role role = new Role(memberId, 1, new PermissionsEnum[]{PermissionsEnum.manageItems});
    //     userService.addRole(memberId, role);

    //     assertTrue(userService.hasRole(memberId, role));
    // }

    // @Test
    // void testAddAndRemovePermission() {
    //     String token = authTokenService.generateAuthToken("test");
    //     userService.addMember("test", "pass", "test@mail.com", "123", "address");
    //     int memberId = userRepository.isUsernameAndPasswordValid("test", "pass");
    //     Role role = new Role(memberId, 1, new PermissionsEnum[]{});
        

    //     assertTrue(userService.addRole(memberId, role));

    //     // Test adding permission
    //     assertTrue(userService.addPermission(token, memberId, PermissionsEnum.manageItems, 1));
    //     assertTrue(userService.hasPermission(memberId, PermissionsEnum.manageItems, 1));

    //     // Test removing permission
    //     assertTrue(userService.removePermission(token, memberId, PermissionsEnum.manageItems, 1));
    //     assertFalse(userService.hasPermission(memberId, PermissionsEnum.manageItems, 1));
    // }
    /* 
    @Test
    void testSignUpWithTakenUsername() {
        userService.addMember("takenUser", "pass", "email", "123", "address");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.signUp("takenUser", "pass", "email", "123", "address");
        });

        String expectedMessage = "Username is already taken.";
        assertTrue(exception.getMessage().contains(expectedMessage));
    }
        */

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



/*
    @Test
    void testConcurrentMakeManagerAndAcceptRole() throws InterruptedException {

        int ownerId = userRepository.isUsernameAndPasswordValid("admin", "admin");
        String ownerToken = "t1";
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    userRepository.addMember("manager" + finalI, "pass", "manager" + finalI + "@test.com", "123", "addr");
                    int newManagerId = userRepository.isUsernameAndPasswordValid("manager" + finalI, "pass");
                    userService.makeManagerOfStore(ownerToken, newManagerId, 1, new PermissionsEnum[]{});
                    userService.acceptRole(ownerToken, 1);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Validation: all managers added must have role
        long count = userRepository.getMembersList().stream()
                .filter(m -> m.getRoles().stream().anyMatch(r -> r.getShopId() == 1))
                .count();
        assertEquals(threads, count);
    }

    @Test
    void testConcurrentAddRemovePermissions() throws InterruptedException {

        userRepository.addMember("manager", "pass", "manager@test.com", "123", "addr");
        int managerId = userRepository.isUsernameAndPasswordValid("manager", "pass");
    
        userService.makeManagerOfStore("t1", managerId, 1, new PermissionsEnum[]{});
        userService.acceptRole("t1", 1);
    
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(100);
    
        for (int i = 0; i < 50; i++) {
            executor.submit(() -> {
                try {
                    userService.addPermission("t1", managerId, PermissionsEnum.manageItems, 1);
                } finally {
                    latch.countDown();
                }
            });
            executor.submit(() -> {
                try {
                    userService.removePermission("t1", managerId, PermissionsEnum.manageItems, 1);
                } finally {
                    latch.countDown();
                }
            });
        }
    
        latch.await();
        executor.shutdown();
    
        // Validation: no crash, permission is either there or not
        boolean hasPermission = userRepository.getRole(managerId, 1).hasPermission(PermissionsEnum.manageItems);
        assertTrue(hasPermission || !hasPermission); // No crash is success here
    }
    */

//,,,,
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
                    userRepository.addMember("user" + finalI, "password" + finalI, "email" + finalI + "@test.com", "123456789", "address" + finalI);
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
                        } catch (Exception ignored) {}
                    } else {
                        try {
                            userRepository.removeAdmin(secondAdminId);
                        } catch (Exception ignored) {}
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
