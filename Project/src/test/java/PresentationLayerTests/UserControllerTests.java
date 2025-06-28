package PresentationLayerTests;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.PresentationLayer.Controller.UserController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive slice tests for UserController.
 */
@WebMvcTest(controllers = UserController.class)
@ContextConfiguration(classes = UserControllerTests.TestBootApp.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTests {

    @SpringBootApplication(scanBasePackages = "com.example.app.PresentationLayer")
    static class TestBootApp {
    }

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthTokenService authService;

    @Nested
    @DisplayName("1. GET USER")
    class GetUser {
        @Test
        void success_returns200AndUser() throws Exception {
            Member m = new Member(1, "user", "pass", "e@mail", "123456", "addr");
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getUserById(1)).thenReturn(m);

            mvc.perform(get("/api/users/1").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").value(1));
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(get("/api/users/1").param("token", "bad"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getUserById(1)).thenThrow(new NoSuchElementException());

            mvc.perform(get("/api/users/1").param("token", "tok"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getUserById(1)).thenThrow(new RuntimeException());

            mvc.perform(get("/api/users/1").param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("2. REGISTER")
    class Register {
        @Test
        void success_returns201() throws Exception {
            doReturn("iLoveYourMama").when(userService).addMember(anyString(), anyString(), anyString(), anyString(),
                    anyString());

            mvc.perform(post("/api/users/register")
                    .param("username", "u123")
                    .param("password", "p")
                    .param("email", "e@mail")
                    .param("phoneNumber", "123")
                    .param("address", "addr"))
                    .andExpect(status().isCreated());
        }

        @Test
        void badRequest_invalidParams_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(userService)
                    .addMember(anyString(), anyString(), anyString(), anyString(), anyString());

            mvc.perform(post("/api/users/register")
                    .param("username", "usr") // too short
                    .param("password", "p")
                    .param("email", "e@mail")
                    .param("phoneNumber", "123")
                    .param("address", "addr"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_duplicate_returns409() throws Exception {
            doThrow(new RuntimeException()).when(userService)
                    .addMember(anyString(), anyString(), anyString(), anyString(), anyString());

            mvc.perform(post("/api/users/register")
                    .param("username", "u123")
                    .param("password", "p")
                    .param("email", "e@mail")
                    .param("phoneNumber", "123")
                    .param("address", "addr"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("3. ADMIN ROLE")
    class AdminRole {
        @Test
        void makeAdmin_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).makeAdmin("tok", 2);

            mvc.perform(post("/api/users/2/admin").param("token", "tok"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void removeAdmin_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).removeAdmin("tok", 2);

            mvc.perform(delete("/api/users/2/admin").param("token", "tok"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_illegalArg_returns400() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new IllegalArgumentException()).when(userService).makeAdmin("tok", 2);

            mvc.perform(post("/api/users/2/admin").param("token", "tok"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_notAllowed_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).removeAdmin("tok", 2);

            mvc.perform(delete("/api/users/2/admin").param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("4. LIST ADMINS")
    class ListAdmins {
        @Test
        void success_returns200AndList() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getAllAdmins("tok")).thenReturn(Arrays.asList(1, 2, 3));

            mvc.perform(get("/api/users/admins").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value(1));
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(get("/api/users/admins").param("token", "bad"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("5. UPDATE FIELDS")
    class UpdateFields {
        @Test
        void updateUsername_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberUsername("tok", "newUser");

            mvc.perform(patch("/api/users/1/username")
                    .param("token", "tok")
                    .param("username", "newUser"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void updatePassword_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberPassword("tok", "newPass");

            mvc.perform(patch("/api/users/1/password")
                    .param("token", "tok")
                    .param("password", "newPass"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void updateEmail_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberEmail("tok", "new@mail");

            mvc.perform(patch("/api/users/1/email")
                    .param("token", "tok")
                    .param("email", "new@mail"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void updatePhone_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberPhoneNumber("tok", "555");

            mvc.perform(patch("/api/users/1/phone")
                    .param("token", "tok")
                    .param("phoneNumber", "555"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void updateAddress_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberAddress("tok", "City", "Street", 10, null);

            mvc.perform(patch("/api/users/1/address")
                    .param("token", "tok")
                    .param("city", "City")
                    .param("street", "Street")
                    .param("apartmentNumber", "10"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("6. ERROR MAPPING")
    class ErrorMapping {
        @Test
        void updateUsername_conflict_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).updateMemberUsername("tok", "user");

            mvc.perform(patch("/api/users/1/username")
                    .param("token", "tok")
                    .param("username", "user"))
                    .andExpect(status().isConflict());
        }
    }

    /* ═══════════════════ NEW ENDPOINTS – TESTS ═══════════════════ */

    @Nested
    @DisplayName("7. ISADMIN")
    class IsAdmin {
        @Test
        void isAdmin_true_returns200() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(9);
            when(userService.isAdmin(5)).thenReturn(true);

            mvc.perform(get("/api/users/5/isAdmin").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        @Test
        void isAdmin_badToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(get("/api/users/5/isAdmin").param("token", "bad"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("8. ENCODER TESTODE")
    class EncoderMode {
        @Test
        void enableEncoderTestMode_returns204() throws Exception {
            doNothing().when(userService).setEncoderToTest(true);

            mvc.perform(post("/api/users/encoder/testMode").param("enable", "true"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void encoderMode_internalError_returns500() throws Exception {
            doThrow(new RuntimeException()).when(userService).setEncoderToTest(anyBoolean());

            mvc.perform(post("/api/users/encoder/testMode").param("enable", "false"))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("9. VALIDATE MEMBER‑ID")
    class ValidateMemberId {
        @Test
        void validateMemberId_ok_returns204() throws Exception {
            doNothing().when(userService).validateMemberId(77);

            mvc.perform(get("/api/users/validate/77"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void validateMemberId_illegal_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(userService).validateMemberId(0);

            mvc.perform(get("/api/users/validate/0"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("10. LOGIN – GUEST & MEMBER")
    class LoginEndpoints {
        @Test
        void guestLogin_success_returnsToken() throws Exception {
            when(userService.loginAsGuest()).thenReturn("guest‑tok");

            mvc.perform(post("/api/users/login/guest"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("guest‑tok"));
        }

        @Test
        void guestLogin_conflict_returns409() throws Exception {
            doThrow(new RuntimeException()).when(userService).loginAsGuest();

            mvc.perform(post("/api/users/login/guest"))
                    .andExpect(status().isConflict());
        }

        @Test
        void memberLogin_success_returnsToken() throws Exception {
            when(userService.loginAsMember("u", "p", "g")).thenReturn("member‑tok");

            mvc.perform(post("/api/users/login/member")
                    .param("username", "u").param("password", "p").param("guestToken", "g"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("member‑tok"));
        }

        @Test
        void memberLogin_badCredentials_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(userService).loginAsMember(anyString(), anyString(),
                    anyString());

            mvc.perform(post("/api/users/login/member")
                    .param("username", "u").param("password", "bad").param("guestToken", "g"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("11. LOGOUT")
    class Logout {
        @Test
        void logout_success_returnsNewGuestToken() throws Exception {
            when(userService.logout("member‑tok")).thenReturn("new‑guest");

            mvc.perform(post("/api/users/logout").param("token", "member‑tok"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("new‑guest"));
        }

        @Test
        void logout_conflict_returns409() throws Exception {
            doThrow(new RuntimeException()).when(userService).logout(anyString());

            mvc.perform(post("/api/users/logout").param("token", "x"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("12. PERMISSIONS BY SHOP")
    class PermissionsByShop {
        @Test
        void listPermissions_success_returnsMap() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            HashMap<Integer, PermissionsEnum[]> map = new HashMap<>();
            map.put(2, new PermissionsEnum[] { PermissionsEnum.manageItems });
            when(userService.getPermitionsByShop("tok", 4)).thenReturn(map);

            mvc.perform(get("/api/users/shops/4/permissions").param("token", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.['2'][0]").value("manageItems"));
        }

        @Test
        void listPermissions_badToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(get("/api/users/shops/4/permissions").param("token", "bad"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("13. CHANGE PERMISSIONS")
    class ChangePermissions {
        @Test
        void changePermissions_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            PermissionsEnum[] arr = { PermissionsEnum.manageItems };

            mvc.perform(post("/api/users/shops/4/permissions/2")
                    .param("token", "tok")
                    .content("[\"manageItems\"]")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }

        @Test
        void changePermissions_conflict_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService)
                    .changePermissions(eq("tok"), eq(2), eq(4), any());

            mvc.perform(post("/api/users/shops/4/permissions/2")
                    .param("token", "tok")
                    .content("[\"manageItems\"]")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("14. MANAGER ASSIGN/REMOVE")
    class ManagerEndpoints {
        @Test
        void makeManager_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);

            mvc.perform(post("/api/users/shops/4/managers")
                    .param("token", "tok").param("memberId", "2")
                    .content("[\"manageItems\"]").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }

        @Test
        void removeManager_conflict_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).removeManagerFromStore("tok", 2, 4);

            mvc.perform(delete("/api/users/shops/4/managers/2").param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("15. OWNER ASSIGN/REMOVE")
    class OwnerEndpoints {
        @Test
        void makeOwner_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);

            mvc.perform(post("/api/users/shops/4/owners")
                    .param("token", "tok").param("memberId", "2"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void removeOwner_conflict_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).removeOwnerFromStore("tok", 2, 4);

            mvc.perform(delete("/api/users/shops/4/owners/2").param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("16. REMOVE ALL ASSIGNED")
    class RemoveAllAssigned {
        @Test
        void removeAllAssigned_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);

            mvc.perform(delete("/api/users/shops/4/assignee/2/all").param("token", "tok"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void removeAllAssigned_conflict_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).removeAllAssigned(2, 4);

            mvc.perform(delete("/api/users/shops/4/assignee/2/all").param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("17. ACCEPT / DECLINE ROLE")
    class RoleDecision {
        @Test
        void acceptRole_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);

            mvc.perform(post("/api/users/roles/4/accept").param("token", "tok"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void declineRole_conflict_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).declineRole("tok", 4);

            mvc.perform(post("/api/users/roles/4/decline").param("token", "tok"))
                    .andExpect(status().isConflict());
        }
    }
                                            
    @Nested
    @DisplayName("18. ALL MEMBERS")
    class AllMembers {
        @Test
        void success_returns200AndList() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            Member m = new Member(1, "u1", "p", "e@mail", "123", "addr");
            when(userService.getAllMembers()).thenReturn(Arrays.asList(m));

            mvc.perform(get("/api/users/allmembers").param("token", "tok"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].memberId").value(1));
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(get("/api/users/allmembers").param("token", "bad"))
            .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).getAllMembers();

            mvc.perform(get("/api/users/allmembers").param("token", "tok"))
            .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("19. SHOP WORKERS")
    class ShopWorkers {
        @Test
        void success_returns200AndWorkers() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            Member m = new Member(2, "shopGuy", "p", "e", "ph", "ad");
            when(userService.getShopMembers(42)).thenReturn(List.of(m));

            mvc.perform(get("/api/users/shops/42/workers").param("token", "tok"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].memberId").value(2));
        }
        @Test
        void badToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());
            mvc.perform(get("/api/users/shops/1/workers").param("token","bad"))
            .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("20. ACCEPTED ROLES")
    class AcceptedRoles {
        @Test
        void badToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException())
                .when(userService).getAcceptedRoles(anyString());

            mvc.perform(get("/api/users/getAcceptedRoles")
                        .param("authToken", "bad"))
            .andExpect(status().isBadRequest());
        }

        @Test
        void success_returns200AndDTOs() throws Exception {
            Role accepted = new Role(
                7,
                5,
                new PermissionsEnum[]{ PermissionsEnum.manageItems }
            );
            when(userService.getAcceptedRoles("tok"))
                .thenReturn(List.of(accepted));

            Member bob = new Member(5, "bob", "pw", "bob@mail", "000", "addr");
            when(userService.getUserById(anyInt())).thenReturn(bob);
            doNothing().when(userService).validateMemberId(anyInt());

            mvc.perform(get("/api/users/getAcceptedRoles")
                        .param("authToken", "tok"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].shopId").value(5))
            .andExpect(jsonPath("$[0].roleName").value("manager"))
            .andExpect(jsonPath("$[0].permissions[0]").value("manageItems"))
            .andExpect(jsonPath("$[0].userName").value("bob"));
        }
    }

    @Nested
    @DisplayName("21. PENDING ROLES")
    class PendingRoles {
        @Test
        void success_returns200AndDTOs() throws Exception {
            Role pending = new Role(9, 8, new PermissionsEnum[]{ PermissionsEnum.manageOwners });
            when(userService.getPendingRoles("tok"))
                .thenReturn(List.of(pending));

            Member alice = new Member(8, "alice", "pw", "alice@mail", "111", "home");
            when(userService.getUserById(anyInt())).thenReturn(alice);
            doNothing().when(userService).validateMemberId(anyInt());

            mvc.perform(get("/api/users/getPendingRoles")
                        .param("authToken", "tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].shopId").value(8))
               .andExpect(jsonPath("$[0].roleName").value("founder"))
               .andExpect(jsonPath("$[0].permissions[0]").value("manageOwners"))
               .andExpect(jsonPath("$[0].userName").value("alice"));
        }
    }

    @Nested
    @DisplayName("22. NOTIFICATIONS")
    class Notifications {
        @Test
        void success_returns200AndList() throws Exception {
            when(userService.getNotificationsAndClear("tok"))
                .thenReturn(List.of("note1","note2"));

            mvc.perform(get("/api/users/notifications").param("authToken","tok"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("note1"));
        }
    }

    @Nested
    @DisplayName("23/24. SUSPEND & UNSUSPEND")
    class SuspendEndpoints {
        @Test
        void suspend_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);

            mvc.perform(post("/api/users/7/suspension")
                    .param("token","tok")
                    .param("until","2025-06-28T12:00:00"))
            .andExpect(status().isNoContent());
        }

        @Test
        void unsuspend_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);

            mvc.perform(post("/api/users/7/unsuspension")
                    .param("token","tok"))
            .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("25. IS SUSPENDED")
    class IsSuspended {
        @Test
        void true_returns200() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.isSuspended(3)).thenReturn(true);

            mvc.perform(get("/api/users/3/isSuspended").param("token","tok"))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));
        }
    }

    @Nested
    @DisplayName("26. LIST SUSPENDED")
    class ListSuspended {
        @Test
        void success_returns200AndIds() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getSuspendedUsers()).thenReturn(List.of(2,3,4));

            mvc.perform(get("/api/users/suspended").param("token","tok"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[1]").value(3));
        }
    }

    @Nested
    @DisplayName("27-30. SHOPPING CART")
    class ShoppingCartTests {
        @Test
        void getCart_returns200AndMap() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            HashMap<Integer,HashMap<Integer,Integer>> cart = new HashMap<>();
            cart.put(5, new HashMap<>(Map.of(11, 2)));
            when(userService.getUserShoppingCartItems(99)).thenReturn(cart);

            mvc.perform(get("/api/users/shoppingCart")
                    .param("token","tok")
                    .param("userId","99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.['5']['11']").value(2));
        }

        @Test
        void addNewItem_returns204() throws Exception {
            mvc.perform(post("/api/users/shoppingCart/5/11")
                    .param("quantity","3")
                    .param("token","tok"))
            .andExpect(status().isNoContent());
        }

        @Test
        void incrementItem_returns204() throws Exception {
            mvc.perform(post("/api/users/shoppingCart/5/11/plus")
                    .param("token","tok")
                    .param("userId","99"))
            .andExpect(status().isNoContent());
        }

        @Test
        void decrementItem_returns204() throws Exception {
            mvc.perform(post("/api/users/shoppingCart/5/11/minus")
                    .param("token","tok")
                    .param("userId","99"))
            .andExpect(status().isNoContent());
        }

        @Test
        void removeItem_returns204() throws Exception {
            mvc.perform(post("/api/users/shoppingCart/5/11/remove")
                    .param("token","tok")
                    .param("userId","99"))
            .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("31. HAS ROLE & 32. HAS PERMISSION")
    class RolePermissionChecks {
        @Test
        void hasRole_true_returns200() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.hasRoleInShop(6, 7)).thenReturn(true);

            mvc.perform(get("/api/users/hasRole")
                    .param("token","tok")
                    .param("userId","6")
                    .param("shopId","7"))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));
        }

        @Test
        void hasPerm_true_returns200() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.hasPermission(6, PermissionsEnum.manageItems, 7))
                .thenReturn(true);

            mvc.perform(get("/api/users/hasPermission")
                    .param("token","tok")
                    .param("userId","6")
                    .param("shopId","7")
                    .param("permission","manageItems"))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));
        }
    }
}
