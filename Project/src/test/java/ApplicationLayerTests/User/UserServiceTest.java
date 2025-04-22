import ApplicationLayer.AuthTokenService;
import DomainLayer.IAuthTokenRepository;

import ApplicationLayer.User.UserService;
import DomainLayer.Member;
import DomainLayer.User;
import DomainLayer.Roles.PermissionsEnum;
import DomainLayer.Roles.Role;
import DomainLayerTests.AuthTokenTests;
import InfrastructureLayer.AuthTokenRepository;
import InfrastructureLayer.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

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
        userService = new UserService(userRepository);
        userService.setServices(authTokenService);
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
        assertEquals("newpassword", ((Member)member).getPassword());
        assertEquals("newemail@example.com", ((Member)member).getEmail());
        assertEquals("0987654321", ((Member)member).getPhoneNumber());
        assertEquals("456 Elm St", ((Member)member).getAddress());

    }

    @Test
    void testLoginAsGuest() {
        String token = userService.loginAsGuest(); // No token generated, but user added
        assertNotNull(token); // Might be empty string depending on implementation
        
        int userId = authTokenRepository.getUserIdByToken(token);
        assertTrue(userRepository.isGuestById(userId));    }

    @Test
    void testSignUpAndValidateMember() {
        String token = userService.signUp("user1", "pass1", "user1@mail.com", "123", "address");
        assertNotNull(token);

        int memberId = userRepository.isUsernameAndPasswordValid("user1", "pass1");
        assertTrue(memberId > 0);

        int validatedId = authTokenRepository.getUserIdByToken(token);
        assertEquals(memberId, validatedId);
    }

    @Test
    void testLogoutRemovesToken() throws Exception {
        String token = userService.loginAsGuest();
        int userId = authTokenRepository.getUserIdByToken(token);

        boolean result = userService.logout(token);
        assertTrue(result);

        int afterLogout = authTokenRepository.getUserIdByToken(token);
        assertEquals(-1, afterLogout);
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
        userService.addMember("noa", "pass", "noa@mail.com", "222", "address");
        int memberId = userRepository.isUsernameAndPasswordValid("noa", "pass");

        Role role = new Role(memberId, 1, new PermissionsEnum[]{});
        userService.addRole(memberId, role);

        userService.addPermission(memberId, PermissionsEnum.setPolicy);
        assertTrue(userService.hasPermission(memberId, PermissionsEnum.setPolicy, 1));

        userService.removePermission(memberId, PermissionsEnum.setPolicy);
        assertFalse(userService.hasPermission(memberId, PermissionsEnum.setPolicy, 1));
    }

    @Test
    void testGetPermitionsByShop() {
        userService.addMember("linda", "pass", "linda@mail.com", "777", "address");
        int memberId = userRepository.isUsernameAndPasswordValid("linda", "pass");

        PermissionsEnum[] perms = new PermissionsEnum[]{PermissionsEnum.manageItems, PermissionsEnum.setPolicy};
        Role role = new Role(memberId, 42, perms);
        userService.addRole(memberId, role);

        HashMap<Integer, PermissionsEnum[]> permsMap = userService.getPermitionsByShop(memberId, 42);
        assertTrue(permsMap.containsKey(memberId));
        assertArrayEquals(perms, permsMap.get(memberId));
    }

    @Test
    void testAcceptRole() {
        userService.addMember("ron", "pass", "ron@mail.com", "555", "address");
        int memberId = userRepository.isUsernameAndPasswordValid("ron", "pass");

        Role role = new Role(memberId, 10, new PermissionsEnum[]{PermissionsEnum.manageItems});
        Member member = (Member) userService.getUserById(memberId);
        member.addRoleToPending(role);

        userService.acceptRole(memberId, role);
        assertTrue(member.getRoles().contains(role));
    }

    @Test
    void testSignUpWithTakenUsername() {
        userService.addMember("takenUser", "pass", "email", "123", "address");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp("takenUser", "pass", "email", "123", "address");
        });

        String expectedMessage = "Username is already taken.";
        assertTrue(exception.getMessage().contains(expectedMessage));
    }

    @Test
    void testInvalidMemberIdValidation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.validateMemberId(-5);
        });

        assertTrue(exception.getMessage().contains("Invalid user ID"));
    }


}
