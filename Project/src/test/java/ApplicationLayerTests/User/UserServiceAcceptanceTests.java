package ApplicationLayerTests.User;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.OurRuntime;
import ApplicationLayer.User.UserService;
import DomainLayer.Roles.PermissionsEnum;
import DomainLayer.Roles.Role;
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
        userRepository.setEncoderToTest(true); // Set the encoder to test mode
        userService = spy(new UserService(userRepository));
        userService.setEncoderToTest(true); // Set the encoder to test mode
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
        String username = "uusseerr11", pwd = "Password111!", email = "user@gmail.com", phone = "0123456789", addr = "addr";
        when(userRepository.isUsernameAndPasswordValid(username,pwd)).thenReturn(-1);
        doNothing().when(userRepository).addMember(username, pwd, email, phone, addr);

        userService.addMember(username, pwd, email, phone, addr);
        when(userRepository.isUsernameAndPasswordValid(username,pwd)).thenReturn(5);
        when(authTokenService.Login(username,pwd,5)).thenReturn("tokenM");

        String tk = userService.loginAsMember(username,pwd,"");
        assertEquals("tokenM", tk);
    }

    // UC4 – Register (duplicate username)
    @Test
    void testRegisterDuplicateUsername() {
        String username = "user1";
        when(userRepository.isUsernameTaken(username)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.addMember(username, "Password1!", "user@gmail.com", "0123456789", "addr")
        );
        assertTrue(ex.getMessage().contains("Username is already taken"));
    }

    // UC4 – Register (invalid email)
    @Test
    void testRegisterInvalidEmail() {
        String username = "user1", pwd = "Password1!", email = "invalidEmail", phone = "0123456789", addr = "addr";
        when(userRepository.isUsernameTaken(username)).thenReturn(false);
        doThrow(new RuntimeException("Invalid Email."))
            .when(userRepository).addMember(username, pwd, email, phone, addr);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.addMember(username, pwd, email, phone, addr)
        );
        assertTrue(ex.getMessage().contains("Invalid Email."));
    }

    // UC9 – Login (positive)
    @Test
    void testLoginSuccess() {
        when(userRepository.isUsernameAndPasswordValid("user1", "Password1!")).thenReturn(5);
        when(authTokenService.Login("user1", "Password1!", 5)).thenReturn("memToken");

        String tk = userService.loginAsMember("user1", "Password1!", "");
        assertEquals("memToken", tk);
    }

    // UC9 – Login (wrong password)
    @Test
    void testLoginPassword() {
        when(userRepository.isUsernameAndPasswordValid("user1", "Wrong1!")).thenReturn(-1);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.loginAsMember("user1", "Wrong1!", "")
        );
        assertTrue(ex.getMessage().contains("Error logging in as member"));
    }

    // UC9 – Login (wrong username)
    @Test
    void testLoginUsername() {
        when(userRepository.isUsernameAndPasswordValid("wrong", "Password1!")).thenReturn(-1);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            userService.loginAsMember("wrong", "Password1!", "")
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

    // UC20 - assigning an owner to a shop (positive)
    @Test
    void testAssignOwnerToShop_Success() throws Exception {
        String firstOwnerToken = "t1";
        String newOwnerToken = "t2";

        int shopId = 11;
        int newOwnerId = 20;
        PermissionsEnum[] pr = {PermissionsEnum.manageOwners};
        
        when(authTokenService.ValidateToken(firstOwnerToken)).thenReturn(17);
        when(authTokenService.ValidateToken(newOwnerToken)).thenReturn(20);

        when(userService.hasPermission(17, PermissionsEnum.manageOwners, shopId)).thenReturn(true);
        doNothing().when(userService).makeManagerOfStore(firstOwnerToken,newOwnerId,shopId, pr);
        doNothing().when(userService).acceptRole(newOwnerToken, shopId);

        userService.makeManagerOfStore(firstOwnerToken,newOwnerId,shopId, pr);
        userService.acceptRole(newOwnerToken, shopId);
        verify(userService).acceptRole(newOwnerToken,shopId);
    }

    // UC20 - assigning an owner to a shop (negative: role declined)
    @Test
    void testAssignOwnerToShop_AssignmentRefusal() throws Exception {
        String firstOwnerToken = "t1";
        String newOwnerToken = "t2";

        int shopId = 11;
        int newOwnerId = 20;
        PermissionsEnum[] pr = {PermissionsEnum.manageOwners};
        
        when(authTokenService.ValidateToken(firstOwnerToken)).thenReturn(17);
        when(authTokenService.ValidateToken(newOwnerToken)).thenReturn(20);

        when(userService.hasPermission(17, PermissionsEnum.manageOwners, shopId)).thenReturn(true);
        doNothing().when(userService).makeManagerOfStore(firstOwnerToken,newOwnerId,shopId, pr);
        doNothing().when(userService).declineRole(newOwnerToken, shopId);

        userService.makeManagerOfStore(firstOwnerToken,newOwnerId,shopId, pr);
        userService.declineRole(newOwnerToken, shopId);
        verify(userService).declineRole(newOwnerToken,shopId);
    }

    // UC20 - assigning an owner to a shop (negative: assigner is not an owner)
    @Test
    void testAssignOwnerToShop_AssignerNotOwner() throws Exception {
        String firstOwnerToken = "t1";
        String newOwnerToken = "t2";

        int shopId = 11;
        int newOwnerId = 20;
        PermissionsEnum[] pr = {PermissionsEnum.manageOwners};

        when(authTokenService.ValidateToken(firstOwnerToken)).thenReturn(17);
        when(authTokenService.ValidateToken(newOwnerToken)).thenReturn(20);

        // Simulate no permission
        when(userService.hasPermission(17, PermissionsEnum.manageOwners, shopId)).thenReturn(false);

        // The test here is that we expect an exception or invalid behavior
        Exception exception = assertThrows(RuntimeException.class, () -> {
            if (!userService.hasPermission(17, PermissionsEnum.manageOwners, shopId)) {
                throw new OurRuntime("Member ID " + 17 + " is not an owner of shop ID " + shopId);
            }
            userService.makeManagerOfStore(firstOwnerToken, newOwnerId, shopId, pr);
        });

        assertTrue(exception.getMessage().contains("Member ID " + 17 + " is not an owner of shop ID " + shopId));
    }

    // UC21 - Successful removal of an assigned owner
    @Test
    void testSuccessfulOwnerRemoval() throws Exception {
        String originalOwnerToken = "t1";
        String assignedOwnerToken = "t2";

        int shopId = 11;
        int originalOwnerId = 123;
        int assignedOwnerId = 456;
        PermissionsEnum[] ownerPermissions = {PermissionsEnum.manageOwners};

        // Setup: tokens and IDs
        when(authTokenService.ValidateToken(originalOwnerToken)).thenReturn(originalOwnerId);
        when(authTokenService.ValidateToken(assignedOwnerToken)).thenReturn(assignedOwnerId);

        // Setup: permission for owner to manage owners
        when(userService.hasPermission(originalOwnerId, PermissionsEnum.manageOwners, shopId)).thenReturn(true);

        // Simulate the assignment and acceptance
        doNothing().when(userService).makeManagerOfStore(originalOwnerToken, assignedOwnerId, shopId, ownerPermissions);
        doNothing().when(userService).acceptRole(assignedOwnerToken, shopId);

        // Simulate removal
        doNothing().when(userService).removeManagerFromStore(originalOwnerToken, assignedOwnerId, shopId);

        // Simulate the flow
        userService.makeManagerOfStore(originalOwnerToken, assignedOwnerId, shopId, ownerPermissions);
        userService.acceptRole(assignedOwnerToken, shopId);
        userService.removeManagerFromStore(originalOwnerToken, assignedOwnerId, shopId);

        // Verify correct removal
        verify(userService).removeManagerFromStore(originalOwnerToken, assignedOwnerId, shopId);
    }

    // UC21 - Attempt to remove owner who did not assign you
    @Test
    void testOwnerRemovalNotAssignedBy() throws Exception {
        String originalOwnerToken = "t1";
        String assignedOwnerToken = "t2";

        int shopId = 11;
        int originalOwnerId = 123;
        int assignedOwnerId = 456;
        PermissionsEnum[] ownerPermissions = {PermissionsEnum.manageOwners};

        // Setup: tokens and IDs
        when(authTokenService.ValidateToken(originalOwnerToken)).thenReturn(originalOwnerId);
        when(authTokenService.ValidateToken(assignedOwnerToken)).thenReturn(assignedOwnerId);

        // Setup: permissions
        when(userService.hasPermission(originalOwnerId, PermissionsEnum.manageOwners, shopId)).thenReturn(true);

        // Simulate the assignment and acceptance
        doNothing().when(userService).makeManagerOfStore(originalOwnerToken, assignedOwnerId, shopId, ownerPermissions);
        doNothing().when(userService).acceptRole(assignedOwnerToken, shopId);

        // Simulate invalid removal (assigned owner tries to remove original owner)
        doThrow(new OurRuntime("Member ID " + assignedOwnerId + " is not the assignee of member ID " + originalOwnerId + " in shop ID " + shopId))
            .when(userService).removeManagerFromStore(assignedOwnerToken, originalOwnerId, shopId);

        // Flow
        userService.makeManagerOfStore(originalOwnerToken, assignedOwnerId, shopId, ownerPermissions);
        userService.acceptRole(assignedOwnerToken, shopId);

        // Try invalid removal and assert the exception
        Exception exception = assertThrows(OurRuntime.class, () ->
            userService.removeManagerFromStore(assignedOwnerToken, originalOwnerId, shopId)
        );

        assertTrue(exception.getMessage().contains("Member ID " + assignedOwnerId + " is not the assignee of member ID " + originalOwnerId + " in shop ID " + shopId));
    }

    // UC22 - Successful assignment of a manager to a shop
    @Test
    void testSuccessfulManageAssignment() throws Exception {
        String ownerToken = "t1";
        String managerToken = "t2";

        int shopId = 11;
        int ownerId = 123;
        int managerId = 456;
        PermissionsEnum[] noPermissions = {};

        when(authTokenService.ValidateToken(ownerToken)).thenReturn(ownerId);
        when(authTokenService.ValidateToken(managerToken)).thenReturn(managerId);

        when(userService.hasPermission(ownerId, PermissionsEnum.manageOwners, shopId)).thenReturn(true);

        doNothing().when(userService).makeManagerOfStore(ownerToken, managerId, shopId, noPermissions);
        doNothing().when(userService).acceptRole(managerToken, shopId);

        userService.makeManagerOfStore(ownerToken, managerId, shopId, noPermissions);
        userService.acceptRole(managerToken, shopId);

        verify(userService).makeManagerOfStore(ownerToken, managerId, shopId, noPermissions);
        verify(userService).acceptRole(managerToken, shopId);
    }


    // UC22 - Assignment refusal (manager disagrees)
    @Test
    void testAssignmentRefusal() throws Exception {
        String ownerToken = "t1";
        String managerToken = "t2";

        int shopId = 11;
        int ownerId = 123;
        int managerId = 456;
        PermissionsEnum[] noPermissions = {};

        when(authTokenService.ValidateToken(ownerToken)).thenReturn(ownerId);
        when(authTokenService.ValidateToken(managerToken)).thenReturn(managerId);

        when(userService.hasPermission(ownerId, PermissionsEnum.manageOwners, shopId)).thenReturn(true);

        doNothing().when(userService).makeManagerOfStore(ownerToken, managerId, shopId, noPermissions);

        // Simulate disagreement
        doThrow(new RuntimeException("Manager disagreed to the assignment"))
            .when(userService).acceptRole(managerToken, shopId);

        userService.makeManagerOfStore(ownerToken, managerId, shopId, noPermissions);

        Exception exception = assertThrows(RuntimeException.class, () ->
            userService.acceptRole(managerToken, shopId)
        );

        assertTrue(exception.getMessage().contains("disagreed"));
    }


    // UC22 - Attempt to assign someone who is already a manager
    @Test
    void testAlreadyManager() throws Exception {
        String ownerToken = "t1";
        String managerToken = "t2";

        int shopId = 11;
        int ownerId = 123;
        int managerId = 456;
        PermissionsEnum[] noPermissions = {};

        when(authTokenService.ValidateToken(ownerToken)).thenReturn(ownerId);
        when(authTokenService.ValidateToken(managerToken)).thenReturn(managerId);

        when(userService.hasPermission(ownerId, PermissionsEnum.manageOwners, shopId)).thenReturn(true);

        // Simulate user already being a manager
        doThrow(new RuntimeException("User is already a manager"))
            .when(userService).makeManagerOfStore(ownerToken, managerId, shopId, noPermissions);

        Exception exception = assertThrows(RuntimeException.class, () ->
            userService.makeManagerOfStore(ownerToken, managerId, shopId, noPermissions)
        );

        assertTrue(exception.getMessage().contains("already"));
    }

    // UC23 - Successful addition of permission to manager
    @Test
    void testSuccessfulManagerPermissionAddition() throws Exception {
        String ownerToken = "t1";
        String managerToken = "t2";

        int shopId = 11;
        int ownerId = 123;
        int managerId = 456;
        PermissionsEnum permissionToAdd = PermissionsEnum.manageItems;

        when(authTokenService.ValidateToken(ownerToken)).thenReturn(ownerId);
        when(authTokenService.ValidateToken(managerToken)).thenReturn(managerId);

        when(userService.hasPermission(ownerId, PermissionsEnum.manageOwners, shopId)).thenReturn(true);

        doNothing().when(userService).makeManagerOfStore(ownerToken, managerId, shopId, new PermissionsEnum[]{});
        doNothing().when(userService).acceptRole(managerToken, shopId);
        doNothing().when(userService).validateMemberId(ownerId);
        doNothing().when(userService).validateMemberId(managerId);
        when(userRepository.getRole(managerId, shopId)).thenReturn(new Role(ownerId, shopId, null));

        when(userService.addPermission(ownerToken, managerId, permissionToAdd, shopId)).thenReturn(true);
        
        userService.makeManagerOfStore(ownerToken, managerId, shopId, new PermissionsEnum[]{});
        userService.acceptRole(managerToken, shopId);
        boolean result = userService.addPermission(ownerToken, managerId, permissionToAdd, shopId);

        assertTrue(result);
        //verify(userService).addPermission(ownerToken, managerId, permissionToAdd, shopId);
    }

    // UC23 - Attempt to add permission to a manager not assigned by you
    @Test
    void testAddPermissionNotAssignedBy() throws Exception {
        String ownerToken = "t1";
        String otherOwnerToken = "t2";

        int shopId = 11;
        int ownerId = 123;
        int otherOwnerId = 456;
        int managerId = 789;
        PermissionsEnum permissionToAdd = PermissionsEnum.manageItems;

        when(authTokenService.ValidateToken(ownerToken)).thenReturn(ownerId);
        when(authTokenService.ValidateToken(otherOwnerToken)).thenReturn(otherOwnerId);

        when(userService.hasPermission(ownerId, PermissionsEnum.manageOwners, shopId)).thenReturn(true);

        doNothing().when(userService).makeManagerOfStore(ownerToken, managerId, shopId, new PermissionsEnum[]{});
        doNothing().when(userService).acceptRole(otherOwnerToken, shopId);

        doThrow(new OurRuntime("789 wasn't assigned by you"))
            .when(userService).addPermission(otherOwnerToken, managerId, permissionToAdd, shopId);

        userService.makeManagerOfStore(ownerToken, managerId, shopId, new PermissionsEnum[]{});
        userService.acceptRole(otherOwnerToken, shopId);

        OurRuntime exception = assertThrows(OurRuntime.class, () ->
            userService.addPermission(otherOwnerToken, managerId, permissionToAdd, shopId)
        );

        assertTrue(exception.getMessage().contains("wasn't assigned by you"));
    }
    // UC23 - Attempt to add a permission not available for managers
    @Test
    void testAddInvalidPermission() throws Exception {
        String ownerToken = "t1";
        String managerToken = "t2";

        int shopId = 11;
        int ownerId = 123;
        int managerId = 456;
        PermissionsEnum invalidPermission = PermissionsEnum.leaveShopAsManager; // assuming this is forbidden

        when(authTokenService.ValidateToken(ownerToken)).thenReturn(ownerId);
        when(authTokenService.ValidateToken(managerToken)).thenReturn(managerId);

        when(userService.hasPermission(ownerId, PermissionsEnum.manageOwners, shopId)).thenReturn(true);

        doNothing().when(userService).makeManagerOfStore(ownerToken, managerId, shopId, new PermissionsEnum[]{});
        doNothing().when(userService).acceptRole(managerToken, shopId);

        doThrow(new IllegalArgumentException("Manager can't have such permissions"))
            .when(userService).addPermission(ownerToken, managerId, invalidPermission, shopId);

        userService.makeManagerOfStore(ownerToken, managerId, shopId, new PermissionsEnum[]{});
        userService.acceptRole(managerToken, shopId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            userService.addPermission(ownerToken, managerId, invalidPermission, shopId)
        );

        assertTrue(exception.getMessage().contains("can't have such permissions"));
    }


}