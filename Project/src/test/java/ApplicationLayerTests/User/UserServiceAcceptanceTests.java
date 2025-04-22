package ApplicationLayerTests.User;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.User.UserService;
import DomainLayer.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceAcceptanceTests {
    private IUserRepository userRepository;
    private AuthTokenService authTokenService;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        userRepository       = mock(IUserRepository.class);
        authTokenService     = mock(AuthTokenService.class);
        // create a spy so we can stub loginAsGuest
        userService          = spy(new UserService(userRepository));
        userService.setServices(authTokenService);
    }

    // UC2 – Guest Entry (positive)
    @Test
    public void testGuestEntrySuccess() {
        when(userRepository.addGuest()).thenReturn(1);
        when(authTokenService.AuthenticateGuest(1)).thenReturn("guestToken");

        String token = userService.loginAsGuest();

        assertNotNull(token);
        assertEquals("guestToken", token);
    }

    // UC2 – Guest Entry (negative)
    @Test
    public void testGuestEntryError() {
        when(userRepository.addGuest()).thenReturn(-1);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> userService.loginAsGuest()
        );
        assertTrue(ex.getMessage().contains("Failed to create a guest user"));
    }

    // UC3 – Guest Exit (positive)
    @Test
    public void testGuestExitSuccess() throws Exception {
        String oldToken = "guestToken";
        int guestId     = 2;

        // stub ValidateToken to return guestId
        doReturn(guestId).when(authTokenService).ValidateToken(oldToken);
        when(userRepository.isGuestById(guestId)).thenReturn(true);
        // stub the second loginAsGuest call to return new token
        doReturn("newGuestToken").when(userService).loginAsGuest();

        String result = userService.logout(oldToken);

        verify(userRepository).removeUserById(guestId);
        verify(authTokenService).Logout(oldToken);
        assertEquals("newGuestToken", result);
    }

    // UC3 – Guest Exit (negative)
    @Test
    public void testGuestExitError() throws Exception {
        String badToken = "bad";

        when(authTokenService.ValidateToken(badToken))
            .thenThrow(new Exception("Token expired"));

        String result = userService.logout(badToken);
        assertNull(result);
    }

    // UC4 – Register (positive)
    @Test
    public void testRegisterSuccess() {
        String username = "u1", pwd = "p1", email = "e@ex", phone = "123", addr = "a";
        when(userRepository.isUsernameTaken(username)).thenReturn(false);
        when(authTokenService.generateAuthToken(username)).thenReturn("tokenM");
        doNothing().when(userRepository).addMember(username, pwd, email, phone, addr);

        String token = userService.signUp(username, pwd, email, phone, addr);
        assertEquals("tokenM", token);
    }

    // UC4 – Register (duplicate username)
    @Test
    public void testRegisterDuplicateUsername() {
        String username = "u1";
        when(userRepository.isUsernameTaken(username)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.signUp(username, "p", "e", "ph", "ad")
        );
        assertTrue(ex.getMessage().contains("Username is already taken"));
    }

    // UC4 – Register (Invalid email)
    @Test
    public void testRegisterInvalidEmail() {
        String username = "u1", pwd = "p1", email = "invalidEmail", phone = "123", addr = "a";
        when(userRepository.isUsernameTaken(username)).thenReturn(false);
        // Stub the void addMember(...) to throw due to invalid email
        doThrow(new RuntimeException("Invalid email format"))
        .when(userRepository)
        .addMember(username, pwd, email, phone, addr);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.signUp(username, pwd, email, phone, addr)
        );
        assertTrue(ex.getMessage().contains("Invalid email format"));
    }

    // UC9 – Login (positive)
    @Test
    public void testLoginSuccess() {
        String user = "u1", pwd = "p1";
        when(userRepository.isUsernameAndPasswordValid(user, pwd)).thenReturn(5);
        when(authTokenService.generateAuthToken(user)).thenReturn("memToken");

        String token = userService.loginAsMember(user, pwd, -1);
        assertEquals("memToken", token);
    }

    // UC9 – Login (wrong Password)
    @Test
    public void testLoginPassword() {
        String user = "u1", pwd = "wrong";
        when(userRepository.isUsernameAndPasswordValid(user, pwd)).thenReturn(-1);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.loginAsMember(user, pwd, -1)
        );
        assertTrue(ex.getMessage().contains("Error logging in as member"));
    }

    //UC9 – Login (wrong Username)
    @Test
    public void testLoginUsername() {
        String user = "wrong", pwd = "p1";
        when(userRepository.isUsernameAndPasswordValid(user, pwd)).thenReturn(-1);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.loginAsMember(user, pwd, -1)
        );
        assertTrue(ex.getMessage().contains("Error logging in as member"));
    }
}
