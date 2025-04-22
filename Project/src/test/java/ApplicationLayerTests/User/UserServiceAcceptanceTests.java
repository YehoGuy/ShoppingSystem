package ApplicationLayerTests.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ApplicationLayer.User.UserService;
import ApplicationLayer.AuthTokenService;
import DomainLayer.ShoppingCart;
import InfrastructureLayer.UserRepository;

public class UserServiceAcceptanceTests {
    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthTokenService authTokenService;

    private UserService userService;
    private AutoCloseable mocks;

    private final String token = "token123";
    private final int userId = 42;
    private final int shopId = 7;
    private final int itemId = 99;
    private final int quantity = 3;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        userService = spy(new UserService(userRepository));
        userService.setServices(authTokenService);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
        clearInvocations(userRepository, authTokenService);
    }

    // UC2 – Guest Entry (positive)
    @Test
    void testGuestEntrySuccess() {
        when(userRepository.addGuest()).thenReturn(1);
        when(authTokenService.AuthenticateGuest(1)).thenReturn("guestToken");

        String token = userService.loginAsGuest();

        assertNotNull(token);
        assertEquals("guestToken", token);
    }

    // UC2 – Guest Entry (negative)
    @Test
    void testGuestEntryError() {
        when(userRepository.addGuest()).thenReturn(-1);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> userService.loginAsGuest()
        );
        assertTrue(ex.getMessage().contains("Failed to create a guest user"));
    }

    // UC3 – Guest Exit (positive)
    @Test
    void testGuestExitSuccess() throws Exception {
        String oldToken = token;
        int guestId = userId;

        doReturn(guestId).when(authTokenService).ValidateToken(oldToken);
        when(userRepository.isGuestById(guestId)).thenReturn(true);
        doReturn("newGuestToken").when(userService).loginAsGuest();

        String result = userService.logout(oldToken);

        verify(userRepository).removeUserById(guestId);
        verify(authTokenService).Logout(oldToken);
        assertEquals("newGuestToken", result);
    }

    // UC3 – Guest Exit (negative)
    @Test
    void testGuestExitError() throws Exception {
        when(authTokenService.ValidateToken(token))
            .thenThrow(new Exception("Token expired"));

        String result = userService.logout(token);
        assertNull(result);
    }

    // UC4 – Register (positive)
    @Test
    void testRegisterSuccess() {
        String username = "u1", pwd = "p1", email = "e@ex", phone = "123", addr = "a";
        when(userRepository.isUsernameTaken(username)).thenReturn(false);
        when(authTokenService.generateAuthToken(username)).thenReturn("tokenM");
        doNothing().when(userRepository).addMember(username, pwd, email, phone, addr);

        String tk = userService.signUp(username, pwd, email, phone, addr);
        assertEquals("tokenM", tk);
    }

    // UC4 – Register (duplicate username)
    @Test
    void testRegisterDuplicateUsername() {
        String username = "u1";
        when(userRepository.isUsernameTaken(username)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.signUp(username, "p", "e", "ph", "ad")
        );
        assertTrue(ex.getMessage().contains("Username is already taken"));
    }

    // UC4 – Register (invalid email)
    @Test
    void testRegisterInvalidEmail() {
        String username = "u1", pwd = "p1", email = "invalidEmail", phone = "123", addr = "a";
        when(userRepository.isUsernameTaken(username)).thenReturn(false);
        doThrow(new RuntimeException("Invalid email format"))
            .when(userRepository).addMember(username, pwd, email, phone, addr);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.signUp(username, pwd, email, phone, addr)
        );
        assertTrue(ex.getMessage().contains("Invalid email format"));
    }

    // UC9 – Login (positive)
    @Test
    void testLoginSuccess() {
        when(userRepository.isUsernameAndPasswordValid("u1", "p1")).thenReturn(5);
        when(authTokenService.generateAuthToken("u1")).thenReturn("memToken");

        String tk = userService.loginAsMember("u1", "p1", -1);
        assertEquals("memToken", tk);
    }

    // UC9 – Login (wrong password)
    @Test
    void testLoginPassword() {
        when(userRepository.isUsernameAndPasswordValid("u1", "wrong")).thenReturn(-1);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.loginAsMember("u1", "wrong", -1)
        );
        assertTrue(ex.getMessage().contains("Error logging in as member"));
    }

    // UC9 – Login (wrong username)
    @Test
    void testLoginUsername() {
        when(userRepository.isUsernameAndPasswordValid("wrong", "p1")).thenReturn(-1);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.loginAsMember("wrong", "p1", -1)
        );
        assertTrue(ex.getMessage().contains("Error logging in as member"));
    }

    // UC6 – Add Item to Cart (positive)
    @Test
    void testAddItemToCartSuccess() throws Exception {
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        doNothing().when(userRepository).addItemToShoppingCart(userId, shopId, itemId, quantity);

        userService.addItemToShoppingCart(token, shopId, itemId, quantity);
        verify(userRepository).addItemToShoppingCart(userId, shopId, itemId, quantity);
    }

    // UC6 – Add Item to Cart (negative: invalid token)
    @Test
    void testAddItemToCartInvalidToken() throws Exception {
        when(authTokenService.ValidateToken(token)).thenThrow(new RuntimeException("Invalid token"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.addItemToShoppingCart(token, shopId, itemId, quantity)
        );
        assertTrue(ex.getMessage().contains("Invalid token"));
    }

    // UC6 – Add Item to Cart (negative: repository error)
    @Test
    void testAddItemToCartRepoError() throws Exception {
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        doThrow(new RuntimeException("DB error"))
            .when(userRepository).addItemToShoppingCart(userId, shopId, itemId, quantity);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.addItemToShoppingCart(token, shopId, itemId, quantity)
        );
        assertTrue(ex.getMessage().contains("Error adding item to shopping cart"));
    }

    // UC7 – Remove Item from Cart (positive)
    @Test
    void testRemoveItemFromCartSuccess() throws Exception {
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        doNothing().when(userRepository).removeItemFromShoppingCart(userId, shopId, itemId);

        userService.removeItemFromShoppingCart(token, shopId, itemId);
        verify(userRepository).removeItemFromShoppingCart(userId, shopId, itemId);
    }

    // UC7 – Remove Item from Cart (negative: invalid token)
    @Test
    void testRemoveItemFromCartInvalidToken() throws Exception {
        when(authTokenService.ValidateToken(token)).thenThrow(new RuntimeException("Bad token"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.removeItemFromShoppingCart(token, shopId, itemId)
        );
        assertTrue(ex.getMessage().contains("Bad token"));
    }

    // UC7 – Remove Item from Cart (negative: repository error)
    @Test
    void testRemoveItemFromCartRepoError() throws Exception {
        when(authTokenService.ValidateToken(token)).thenReturn(userId);
        doThrow(new RuntimeException("Removal failed"))
            .when(userRepository).removeItemFromShoppingCart(userId, shopId, itemId);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.removeItemFromShoppingCart(token, shopId, itemId)
        );
        assertTrue(ex.getMessage().contains("Error removing item from shopping cart"));
    }

}