package ApplicationLayerTests.User;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.User.UserService;
import DomainLayer.Member;
import DomainLayer.Roles.PermissionsEnum;
import DomainLayer.Roles.Role;
import DomainLayer.User;
import InfrastructureLayer.AuthTokenRepository;
import InfrastructureLayer.UserRepository;


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
        String token = userService.loginAsMember("admin", "admin", -1);
        userService.addMember("username", "password", "email@email.com", "phoneNumber", "address");
        int userid = userRepository.isUsernameAndPasswordValid("username", "password");
        userService.makeAdmin(token, userid);
        assertTrue(userService.isAdmin(userid));
    }

    @Test
    void removeAdmin()
    {
        String token = userService.loginAsMember("admin", "admin", -1);
        userService.addMember("username", "password", "email@email.com", "phoneNumber", "address");
        int userid = userRepository.isUsernameAndPasswordValid("username", "password");
        userService.makeAdmin(token, userid);
        assertTrue(userRepository.isAdmin(userid));
        userService.removeAdmin(token, userid);
        assertFalse(userRepository.isAdmin(userid));
    }


    @Test
    void testAddMemberAndGetUserById() {
        userService.addMember("john", "pass123", "john@example.com", "1234567890", "123 Main St");
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

        userService.updateMemberUsername(memberId, "newusername");
        userService.updateMemberPassword(memberId, "newpassword");
        userService.updateMemberEmail(memberId, "newemail@example.com");
        userService.updateMemberPhoneNumber(memberId, "0987654321");
        userService.updateMemberAddress(memberId, "456 Elm St");

        User member = userService.getUserById(memberId);
        assertEquals("newusername", ((Member)member).getUsername());
        assertEquals("newemail@example.com", ((Member)member).getEmail());
        assertEquals("0987654321", ((Member)member).getPhoneNumber());
        assertEquals("456 Elm St", ((Member)member).getAddress());

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
        String token = userService.signUp("user1", "pass1", "user1@mail.com", "123", "address");
        assertNotNull(token);

        int memberId = userRepository.isUsernameAndPasswordValid("user1", "pass1");
        assertTrue(memberId > 0);
    }

    @Test
    void testLogoutRemovesToken() throws Exception {
        String token = userService.loginAsGuest();
        int userId = authTokenRepository.getUserIdByToken(token);

        token = userService.logout(token);
        assertNotNull(token); // Might be empty string depending on implementation
    }

    
    @Test
    void testAddAndCheckRole() {
        userService.addMember("sara", "pass", "sara@mail.com", "123", "address");
        int memberId = userRepository.isUsernameAndPasswordValid("sara", "pass");

        Role role = new Role(memberId, 1, new PermissionsEnum[]{PermissionsEnum.manageItems});
        userService.addRole(memberId, role);

        assertTrue(userService.hasRole(memberId, role));
    }

    @Test
    void testAddAndRemovePermission() {
        userService.addMember("test", "pass", "test@mail.com", "123", "address");
        int memberId = userRepository.isUsernameAndPasswordValid("test", "pass");
        Role role = new Role(memberId, 1, new PermissionsEnum[]{});
        
        assertTrue(userService.addRole(memberId, role));

        // Test adding permission
        assertTrue(userService.addPermission(memberId, PermissionsEnum.manageItems, 1));
        assertTrue(userService.hasPermission(memberId, PermissionsEnum.manageItems, 1));

        // Test removing permission
        assertTrue(userService.removePermission(memberId, PermissionsEnum.manageItems, 1));
        assertFalse(userService.hasPermission(memberId, PermissionsEnum.manageItems, 1));
    }
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


}
