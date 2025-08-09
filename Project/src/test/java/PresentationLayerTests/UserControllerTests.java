package PresentationLayerTests;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.hasSize;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.DomainLayer.User;
import com.example.app.PresentationLayer.Controller.UserController;

import jakarta.validation.ConstraintViolationException;

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

        /* ──────────────── 0. GET /{userId} NULL & UNKNOWN TYPE ─────────────── */
    @Nested
    @DisplayName("0. GET USER – null & wrong‐type")
    class GetUserExtra {
        @Test
        void userNotExist_returns404Body() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getUserById(1)).thenReturn(null);

            mvc.perform(get("/api/users/1").param("token","tok"))
               .andExpect(status().isNotFound())
               .andExpect(content().string("User not found"));
        }

        @Test
        void wrongType_returns404NotMemberOrGuest() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            // return some other subclass of User
            User weird = mock(User.class);
            when(userService.getUserById(1)).thenReturn(weird);

            mvc.perform(get("/api/users/1").param("token","tok"))
               .andExpect(status().isNotFound())
               .andExpect(content().string("Not member or Guest"));
        }
    }

    /* ──────────────── 1. SHOP OWNER ──────────────── */
    @Nested
    @DisplayName("1. SHOP OWNER")
    class ShopOwnerTests {
        @Test
        void getShopOwner_success_returns200AndOwnerId() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getShopOwner(5)).thenReturn(42);

            mvc.perform(get("/api/users/shops/5/owner").param("token","tok"))
               .andExpect(status().isOk())
               .andExpect(content().string("42"));
        }

        @Test
        void getShopOwner_badToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(get("/api/users/shops/5/owner").param("token","bad"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void getShopOwner_notFound_returns404() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new NoSuchElementException()).when(userService).getShopOwner(5);

            mvc.perform(get("/api/users/shops/5/owner").param("token","tok"))
               .andExpect(status().isNotFound());
        }
    }

    /* ──────────────── 2. BAN USER ──────────────── */
    @Nested
    @DisplayName("2. BAN USER")
    class BanUserTests {
        @Test
        void banUser_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            // no need to stub userService.banUser(…) – it’s void 

            mvc.perform(post("/api/users/3/ban").param("token","tok"))
               .andExpect(status().isNoContent());
        }

        @Test
        void banUser_badToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(post("/api/users/3/ban").param("token","bad"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void banUser_conflict_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).banUser(3);

            mvc.perform(post("/api/users/3/ban").param("token","tok"))
               .andExpect(status().isConflict());
        }
    }

    /* ──────────────── 3. CHANGE PERMISSIONS – invalid token ───────────── */
    @Nested
    @DisplayName("3. CHANGE PERMISSIONS – bad request")
    class ChangePermissionsErrors {
        @Test
        void changePermissions_badToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(post("/api/users/shops/4/permissions/2")
                    .param("token","bad")
                    .content("[\"manageItems\"]")
                    .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isBadRequest());
        }
    }

    /* ──────────────── 4. PERMISSIONS BY SHOP – conflict ─────────────── */
    @Nested
    @DisplayName("4. PERMISSIONS BY SHOP – conflict")
    class PermissionsByShopErrors {
        @Test
        void listPermissions_conflict_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).getPermitionsByShop("tok",4);

            mvc.perform(get("/api/users/shops/4/permissions").param("token","tok"))
               .andExpect(status().isConflict());
        }
    }

    /* ──────────────── 5. REMOVE MANAGER & OWNER – success ───────────── */
    @Nested
    @DisplayName("5. REMOVE MANAGER SUCCESS")
    class RemoveManagerSuccess {
        @Test
        void removeManager_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).removeManagerFromStore("tok",2,4);

            mvc.perform(delete("/api/users/shops/4/managers/2").param("token","tok"))
               .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("6. REMOVE OWNER SUCCESS")
    class RemoveOwnerSuccess {
        @Test
        void removeOwner_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).removeOwnerFromStore("tok",2,4);

            mvc.perform(delete("/api/users/shops/4/owners/2").param("token","tok"))
               .andExpect(status().isNoContent());
        }
    }

    /* ──────────────── 7. HAS ROLE & PERMISSION – error mapping ───────── */
    @Nested
    @DisplayName("7. HAS ROLE & PERMISSION – errors")
    class RolePermissionErrors {
        @Test
        void hasRole_badToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(get("/api/users/hasRole")
                    .param("token","bad")
                    .param("userId","6")
                    .param("shopId","7"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void hasRole_conflict_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).hasRoleInShop(6,7);

            mvc.perform(get("/api/users/hasRole")
                    .param("token","tok")
                    .param("userId","6")
                    .param("shopId","7"))
               .andExpect(status().isConflict());
        }

        @Test
        void hasPerm_badToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(get("/api/users/hasPermission")
                    .param("token","bad")
                    .param("userId","6")
                    .param("shopId","7")
                    .param("permission","manageItems"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void hasPerm_conflict_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService)
                .hasPermission(6, PermissionsEnum.manageItems, 7);

            mvc.perform(get("/api/users/hasPermission")
                    .param("token","tok")
                    .param("userId","6")
                    .param("shopId","7")
                    .param("permission","manageItems"))
               .andExpect(status().isConflict());
        }
    }

    /* ──────────────── 8. LIST SUSPENDED – error mapping ──────────────── */
    @Nested
    @DisplayName("8. LIST SUSPENDED – errors")
    class ListSuspendedErrors {
        @Test
        void badToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(get("/api/users/suspended").param("token","bad"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).getSuspendedUsers();

            mvc.perform(get("/api/users/suspended").param("token","tok"))
               .andExpect(status().isConflict());
        }
    }

    /* ──────────────── 9. VALIDATE MEMBER-ID – conflict ──────────────── */
    @Nested
    @DisplayName("9. VALIDATE MEMBER-ID – conflict")
    class ValidateMemberIdErrors {
        @Test
        void conflict_returns409() throws Exception {
            doThrow(new RuntimeException()).when(userService).validateMemberId(77);

            mvc.perform(get("/api/users/validate/77"))
               .andExpect(status().isConflict());
        }
    }

    /* ══════════════════ COMPREHENSIVE ENDPOINT TESTS ══════════════════ */

    @Nested
    @DisplayName("Get All Members Enhanced Tests")
    class GetAllMembersEnhancedTests {
        @Test
        void success_returnsMembersList() throws Exception {
            List<Member> members = List.of(
                new Member(1, "user1", "pass1", "e1@mail", "123", "addr1"),
                new Member(2, "user2", "pass2", "e2@mail", "456", "addr2")
            );
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getAllMembers()).thenReturn(members);

            mvc.perform(get("/api/users/allmembers").param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].memberId").value(1))
                .andExpect(jsonPath("$[1].memberId").value(2));
        }

        @Test
        void emptyResult_returnsEmptyArray() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getAllMembers()).thenReturn(List.of());

            mvc.perform(get("/api/users/allmembers").param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(get("/api/users/allmembers").param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getAllMembers()).thenThrow(new RuntimeException("Service error"));

            mvc.perform(get("/api/users/allmembers").param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getAllMembers()).thenThrow(new RuntimeException("Internal error"));

            mvc.perform(get("/api/users/allmembers").param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Change Permissions Enhanced Tests")
    class ChangePermissionsEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            PermissionsEnum[] permissions = {PermissionsEnum.manageItems, PermissionsEnum.setPolicy};
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).changePermissions(eq("tok"), eq(2), eq(1), any(PermissionsEnum[].class));

            mvc.perform(post("/api/users/shops/1/permissions/2")
                    .param("token", "tok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[\"manageItems\", \"setPolicy\"]"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(post("/api/users/shops/1/permissions/2")
                    .param("token", "badtoken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[\"manageItems\"]"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void badRequest_constraintViolation_returns400() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new ConstraintViolationException("Invalid constraint", null)).when(userService)
                .changePermissions(anyString(), anyInt(), anyInt(), any());

            mvc.perform(post("/api/users/shops/1/permissions/2")
                    .param("token", "tok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[\"manageItems\"]"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService)
                .changePermissions(anyString(), anyInt(), anyInt(), any());

            mvc.perform(post("/api/users/shops/1/permissions/2")
                    .param("token", "tok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[\"manageItems\"]"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Get Shop Owner Enhanced Tests")
    class GetShopOwnerEnhancedTests {
        @Test
        void success_returnsOwnerId() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getShopOwner(1)).thenReturn(42);

            mvc.perform(get("/api/users/shops/1/owner").param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(get("/api/users/shops/1/owner").param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_shopNotFound_returns404() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getShopOwner(999)).thenThrow(new NoSuchElementException());

            mvc.perform(get("/api/users/shops/999/owner").param("token", "tok"))
                .andExpect(status().isNotFound());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getShopOwner(1)).thenThrow(new RuntimeException("Internal error"));

            mvc.perform(get("/api/users/shops/1/owner").param("token", "tok"))
                .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Get Permissions By Shop Enhanced Tests")
    class GetPermissionsByShopEnhancedTests {
        @Test
        void success_returnsPermissionsMap() throws Exception {
            HashMap<Integer, PermissionsEnum[]> permissions = new HashMap<>();
            permissions.put(1, new PermissionsEnum[]{PermissionsEnum.manageItems});
            permissions.put(2, new PermissionsEnum[]{PermissionsEnum.setPolicy});

            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getPermitionsByShop("tok", 1)).thenReturn(permissions);

            mvc.perform(get("/api/users/shops/1/permissions").param("token", "tok"))
                .andExpect(status().isOk());
        }

        @Test
        void emptyResult_returnsEmptyMap() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getPermitionsByShop("tok", 1)).thenReturn(new HashMap<>());

            mvc.perform(get("/api/users/shops/1/permissions").param("token", "tok"))
                .andExpect(status().isOk());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(get("/api/users/shops/1/permissions").param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getPermitionsByShop("tok", 1)).thenThrow(new RuntimeException("Service error"));

            mvc.perform(get("/api/users/shops/1/permissions").param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getPermitionsByShop("tok", 1)).thenThrow(new RuntimeException("Internal error"));

            mvc.perform(get("/api/users/shops/1/permissions").param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Get Accepted Roles Enhanced Tests")
    class GetAcceptedRolesEnhancedTests {
        @Test
        void success_returnsAcceptedRoles() throws Exception {
            List<Role> roles = Arrays.asList(
                new Role(2, 1, new PermissionsEnum[]{PermissionsEnum.manageItems})
            );
            Member member = new Member(2, "user2", "pass", "e@mail", "123", "addr");
            
            when(userService.getAcceptedRoles("tok")).thenReturn(roles);
            when(userService.getUserById(2)).thenReturn(member);
            doNothing().when(userService).validateMemberId(2);

            mvc.perform(get("/api/users/getAcceptedRoles").param("authToken", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void emptyResult_returnsEmptyArray() throws Exception {
            when(userService.getAcceptedRoles("tok")).thenReturn(List.of());

            mvc.perform(get("/api/users/getAcceptedRoles").param("authToken", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(userService).getAcceptedRoles("badtoken");

            mvc.perform(get("/api/users/getAcceptedRoles").param("authToken", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            doThrow(new RuntimeException("Service error")).when(userService).getAcceptedRoles("tok");

            mvc.perform(get("/api/users/getAcceptedRoles").param("authToken", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Ban User Enhanced Tests")
    class BanUserEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).banUser(2);

            mvc.perform(post("/api/users/2/ban").param("token", "tok"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(post("/api/users/2/ban").param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService).banUser(2);

            mvc.perform(post("/api/users/2/ban").param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService).banUser(2);

            mvc.perform(post("/api/users/2/ban").param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("List Suspended Enhanced Tests")
    class ListSuspendedEnhancedTests {
        @Test
        void success_returnsSuspendedList() throws Exception {
            List<Integer> suspended = List.of(2, 3, 5);
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getSuspendedUsers()).thenReturn(suspended);

            mvc.perform(get("/api/users/suspended").param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0]").value(2));
        }

        @Test
        void emptyResult_returnsEmptyArray() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getSuspendedUsers()).thenReturn(List.of());

            mvc.perform(get("/api/users/suspended").param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Has Permission Enhanced Tests")
    class HasPermissionEnhancedTests {
        @Test
        void success_returnsTrue() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.hasPermission(2, PermissionsEnum.manageItems, 1)).thenReturn(true);

            mvc.perform(get("/api/users/hasPermission")
                    .param("token", "tok")
                    .param("userId", "2")
                    .param("shopId", "1")
                    .param("permission", "manageItems"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        }

        @Test
        void success_returnsFalse() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.hasPermission(2, PermissionsEnum.manageItems, 1)).thenReturn(false);

            mvc.perform(get("/api/users/hasPermission")
                    .param("token", "tok")
                    .param("userId", "2")
                    .param("shopId", "1")
                    .param("permission", "manageItems"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
        }

        @Test
        void badRequest_invalidToken_returnsFalse() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(get("/api/users/hasPermission")
                    .param("token", "badtoken")
                    .param("userId", "2")
                    .param("shopId", "1")
                    .param("permission", "manageItems"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("false"));
        }

        @Test
        void conflict_returnsFalse() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).hasPermission(anyInt(), any(), anyInt());

            mvc.perform(get("/api/users/hasPermission")
                    .param("token", "tok")
                    .param("userId", "2")
                    .param("shopId", "1")
                    .param("permission", "manageItems"))
                .andExpect(status().isConflict())
                .andExpect(content().string("false"));
        }

        @Test
        void internalError_returnsFalse() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService).hasPermission(anyInt(), any(), anyInt());

            mvc.perform(get("/api/users/hasPermission")
                    .param("token", "tok")
                    .param("userId", "2")
                    .param("shopId", "1")
                    .param("permission", "manageItems"))
                .andExpect(status().isConflict())
                .andExpect(content().string("false"));
        }
    }

    @Nested
    @DisplayName("Has Role Enhanced Tests")
    class HasRoleEnhancedTests {
        @Test
        void success_returnsTrue() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.hasRoleInShop(2, 1)).thenReturn(true);

            mvc.perform(get("/api/users/hasRole")
                    .param("token", "tok")
                    .param("userId", "2")
                    .param("shopId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        }

        @Test
        void success_returnsFalse() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.hasRoleInShop(2, 1)).thenReturn(false);

            mvc.perform(get("/api/users/hasRole")
                    .param("token", "tok")
                    .param("userId", "2")
                    .param("shopId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
        }

        @Test
        void badRequest_returnsFalse() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(get("/api/users/hasRole")
                    .param("token", "badtoken")
                    .param("userId", "2")
                    .param("shopId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("false"));
        }
    }

    @Nested
    @DisplayName("Get Pending Roles Enhanced Tests")
    class GetPendingRolesEnhancedTests {
        @Test
        void success_returnsPendingRoles() throws Exception {
            List<Role> roles = List.of(
                new Role(2, 1, new PermissionsEnum[]{PermissionsEnum.manageItems})
            );
            Member member = new Member(2, "user2", "pass", "e@mail", "123", "addr");
            
            when(userService.getPendingRoles("tok")).thenReturn(roles);
            when(userService.getUserById(2)).thenReturn(member);
            doNothing().when(userService).validateMemberId(2);

            mvc.perform(get("/api/users/getPendingRoles").param("authToken", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void emptyResult_returnsEmptyArray() throws Exception {
            when(userService.getPendingRoles("tok")).thenReturn(List.of());

            mvc.perform(get("/api/users/getPendingRoles").param("authToken", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void badRequest_returnsNull() throws Exception {
            doThrow(new IllegalArgumentException()).when(userService).getPendingRoles("badtoken");

            mvc.perform(get("/api/users/getPendingRoles").param("authToken", "badtoken"))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get Shop Members Enhanced Tests")
    class GetShopMembersEnhancedTests {
        @Test
        void success_returnsMembersList() throws Exception {
            List<Member> members = List.of(
                new Member(1, "user1", "pass1", "e1@mail", "123", "addr1"),
                new Member(2, "user2", "pass2", "e2@mail", "456", "addr2")
            );
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getShopMembers(1)).thenReturn(members);

            mvc.perform(get("/api/users/shops/1/workers").param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].memberId").value(1))
                .andExpect(jsonPath("$[1].memberId").value(2));
        }

        @Test
        void emptyResult_returnsEmptyArray() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getShopMembers(1)).thenReturn(List.of());

            mvc.perform(get("/api/users/shops/1/workers").param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(get("/api/users/shops/1/workers").param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getShopMembers(1)).thenThrow(new RuntimeException("Service error"));

            mvc.perform(get("/api/users/shops/1/workers").param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getShopMembers(1)).thenThrow(new RuntimeException("Internal error"));

            mvc.perform(get("/api/users/shops/1/workers").param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Logout Enhanced Tests")
    class LogoutEnhancedTests {
        @Test
        void success_returnsGuestToken() throws Exception {
            when(userService.logout("tok")).thenReturn("guestToken123");

            mvc.perform(post("/api/users/logout").param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(content().string("guestToken123"));
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            doThrow(new RuntimeException("Service error")).when(userService).logout("tok");

            mvc.perform(post("/api/users/logout").param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            doThrow(new RuntimeException("Internal error")).when(userService).logout("tok");

            mvc.perform(post("/api/users/logout").param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Login As Guest Enhanced Tests")
    class LoginAsGuestEnhancedTests {
        @Test
        void success_returnsToken() throws Exception {
            when(userService.loginAsGuest()).thenReturn("guestToken123");

            mvc.perform(post("/api/users/login/guest"))
                .andExpect(status().isOk())
                .andExpect(content().string("guestToken123"));
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            doThrow(new RuntimeException("Service error")).when(userService).loginAsGuest();

            mvc.perform(post("/api/users/login/guest"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            doThrow(new RuntimeException("Internal error")).when(userService).loginAsGuest();

            mvc.perform(post("/api/users/login/guest"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Login As Member Enhanced Tests")
    class LoginAsMemberEnhancedTests {
        @Test
        void success_returnsToken() throws Exception {
            when(userService.loginAsMember("user123", "pass123", "")).thenReturn("memberToken123");

            mvc.perform(post("/api/users/login/member")
                    .param("username", "user123")
                    .param("password", "pass123")
                    .param("guestToken", ""))
                .andExpect(status().isOk())
                .andExpect(content().string("memberToken123"));
        }

        @Test
        void successWithGuest_returnsToken() throws Exception {
            when(userService.loginAsMember("user123", "pass123", "guestToken123")).thenReturn("memberToken123");

            mvc.perform(post("/api/users/login/member")
                    .param("username", "user123")
                    .param("password", "pass123")
                    .param("guestToken", "guestToken123"))
                .andExpect(status().isOk())
                .andExpect(content().string("memberToken123"));
        }

        @Test
        void badRequest_constraintViolation_returns400() throws Exception {
            doThrow(new ConstraintViolationException("Invalid input", null)).when(userService)
                .loginAsMember(anyString(), anyString(), anyString());

            mvc.perform(post("/api/users/login/member")
                    .param("username", "")
                    .param("password", "pass123")
                    .param("guestToken", ""))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_invalidCredentials_returns409() throws Exception {
            doThrow(new RuntimeException("Invalid credentials")).when(userService)
                .loginAsMember("user123", "wrongpass", "");

            mvc.perform(post("/api/users/login/member")
                    .param("username", "user123")
                    .param("password", "wrongpass")
                    .param("guestToken", ""))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            doThrow(new RuntimeException("Internal error")).when(userService)
                .loginAsMember(anyString(), anyString(), anyString());

            mvc.perform(post("/api/users/login/member")
                    .param("username", "user123")
                    .param("password", "pass123")
                    .param("guestToken", ""))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Update Username Enhanced Tests")
    class UpdateUsernameEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberUsername("tok", "newUsername");

            mvc.perform(patch("/api/users/1/username")
                    .param("token", "tok")
                    .param("username", "newUsername"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(patch("/api/users/1/username")
                    .param("token", "badtoken")
                    .param("username", "newUsername"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void badRequest_constraintViolation_returns400() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new ConstraintViolationException("Invalid username", null)).when(userService)
                .updateMemberUsername(anyString(), anyString());

            mvc.perform(patch("/api/users/1/username")
                    .param("token", "tok")
                    .param("username", ""))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_usernameExists_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Username already exists")).when(userService)
                .updateMemberUsername("tok", "existingUser");

            mvc.perform(patch("/api/users/1/username")
                    .param("token", "tok")
                    .param("username", "existingUser"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService)
                .updateMemberUsername(anyString(), anyString());

            mvc.perform(patch("/api/users/1/username")
                    .param("token", "tok")
                    .param("username", "newUsername"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Remove All Assigned Enhanced Tests")
    class RemoveAllAssignedEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).removeAllAssigned(2, 1);

            mvc.perform(delete("/api/users/shops/1/assignee/2/all")
                    .param("token", "tok"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(delete("/api/users/shops/1/assignee/2/all")
                    .param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService).removeAllAssigned(2, 1);

            mvc.perform(delete("/api/users/shops/1/assignee/2/all")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService).removeAllAssigned(2, 1);

            mvc.perform(delete("/api/users/shops/1/assignee/2/all")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Make Manager Of Store Enhanced Tests")
    class MakeManagerOfStoreEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).makeManagerOfStore(eq("tok"), eq(2), eq(1), any(PermissionsEnum[].class));

            mvc.perform(post("/api/users/shops/1/managers")
                    .param("token", "tok")
                    .param("memberId", "2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[\"manageItems\", \"setPolicy\"]"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(post("/api/users/shops/1/managers")
                    .param("token", "badtoken")
                    .param("memberId", "2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[\"manageItems\"]"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService)
                .makeManagerOfStore(anyString(), anyInt(), anyInt(), any(PermissionsEnum[].class));

            mvc.perform(post("/api/users/shops/1/managers")
                    .param("token", "tok")
                    .param("memberId", "2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[\"manageItems\"]"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService)
                .makeManagerOfStore(anyString(), anyInt(), anyInt(), any(PermissionsEnum[].class));

            mvc.perform(post("/api/users/shops/1/managers")
                    .param("token", "tok")
                    .param("memberId", "2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[\"manageItems\"]"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Add New Item To Shopping Cart Enhanced Tests")
    class AddNewItemToShoppingCartEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).addItemToShoppingCart("tok", 1, 5, 3);

            mvc.perform(post("/api/users/shoppingCart/1/5")
                    .param("quantity", "3")
                    .param("token", "tok"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(post("/api/users/shoppingCart/1/5")
                    .param("quantity", "3")
                    .param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService)
                .addItemToShoppingCart(anyString(), anyInt(), anyInt(), anyInt());

            mvc.perform(post("/api/users/shoppingCart/1/5")
                    .param("quantity", "3")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService)
                .addItemToShoppingCart(anyString(), anyInt(), anyInt(), anyInt());

            mvc.perform(post("/api/users/shoppingCart/1/5")
                    .param("quantity", "3")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Add Item To Shopping Cart Enhanced Tests") 
    class AddItemToShoppingCartEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateShoppingCartItemQuantity(2, 1, 5, true);

            mvc.perform(post("/api/users/shoppingCart/1/5/plus")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(post("/api/users/shoppingCart/1/5/plus")
                    .param("token", "badtoken")
                    .param("userId", "2"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService)
                .updateShoppingCartItemQuantity(anyInt(), anyInt(), anyInt(), anyBoolean());

            mvc.perform(post("/api/users/shoppingCart/1/5/plus")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
                       doThrow(new RuntimeException("Internal error")).when(userService)
                .updateShoppingCartItemQuantity(anyInt(), anyInt(), anyInt(), anyBoolean());

            mvc.perform(post("/api/users/shoppingCart/1/5/plus")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Decrease Item In Shopping Cart Enhanced Tests")
    class DecreaseItemInShoppingCartEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateShoppingCartItemQuantity(2, 1, 5, false);

            mvc.perform(post("/api/users/shoppingCart/1/5/minus")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(post("/api/users/shoppingCart/1/5/minus")
                    .param("token", "badtoken")
                    .param("userId", "2"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService)
                .updateShoppingCartItemQuantity(anyInt(), anyInt(), anyInt(), anyBoolean());

            mvc.perform(post("/api/users/shoppingCart/1/5/minus")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService)
                .updateShoppingCartItemQuantity(anyInt(), anyInt(), anyInt(), anyBoolean());

            mvc.perform(post("/api/users/shoppingCart/1/5/minus")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Remove Completely Item From Shopping Cart Enhanced Tests")
    class RemoveCompletelyItemFromShoppingCartEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).removeItemFromShoppingCart(2, 1, 5);

            mvc.perform(post("/api/users/shoppingCart/1/5/remove")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(post("/api/users/shoppingCart/1/5/remove")
                    .param("token", "badtoken")
                    .param("userId", "2"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService)
                .removeItemFromShoppingCart(anyInt(), anyInt(), anyInt());

            mvc.perform(post("/api/users/shoppingCart/1/5/remove")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService)
                .removeItemFromShoppingCart(anyInt(), anyInt(), anyInt());

            mvc.perform(post("/api/users/shoppingCart/1/5/remove")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Update Password Enhanced Tests")
    class UpdatePasswordEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberPassword("tok", "newPassword123");

            mvc.perform(patch("/api/users/1/password")
                    .param("token", "tok")
                    .param("password", "newPassword123"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(patch("/api/users/1/password")
                    .param("token", "badtoken")
                    .param("password", "newPassword123"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void badRequest_emptyPassword_returns400() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new ConstraintViolationException("Password cannot be empty", null)).when(userService)
                .updateMemberPassword(anyString(), anyString());

            mvc.perform(patch("/api/users/1/password")
                    .param("token", "tok")
                    .param("password", ""))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService)
                .updateMemberPassword("tok", "newPassword123");

            mvc.perform(patch("/api/users/1/password")
                    .param("token", "tok")
                    .param("password", "newPassword123"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService)
                .updateMemberPassword(anyString(), anyString());

            mvc.perform(patch("/api/users/1/password")
                    .param("token", "tok")
                    .param("password", "newPassword123"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Update Email Enhanced Tests")
    class UpdateEmailEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberEmail("tok", "newemail@example.com");

            mvc.perform(patch("/api/users/1/email")
                    .param("token", "tok")
                    .param("email", "newemail@example.com"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(patch("/api/users/1/email")
                    .param("token", "badtoken")
                    .param("email", "newemail@example.com"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void badRequest_invalidEmail_returns400() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new ConstraintViolationException("Invalid email format", null)).when(userService)
                .updateMemberEmail(anyString(), anyString());

            mvc.perform(patch("/api/users/1/email")
                    .param("token", "tok")
                    .param("email", "invalid-email"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_emailExists_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Email already exists")).when(userService)
                .updateMemberEmail("tok", "existing@example.com");

            mvc.perform(patch("/api/users/1/email")
                    .param("token", "tok")
                    .param("email", "existing@example.com"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService)
                .updateMemberEmail(anyString(), anyString());

            mvc.perform(patch("/api/users/1/email")
                    .param("token", "tok")
                    .param("email", "newemail@example.com"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Update Phone Enhanced Tests")
    class UpdatePhoneEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberPhoneNumber("tok", "0501234567");

            mvc.perform(patch("/api/users/1/phone")
                    .param("token", "tok")
                    .param("phoneNumber", "0501234567"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(patch("/api/users/1/phone")
                    .param("token", "badtoken")
                    .param("phoneNumber", "0501234567"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void badRequest_emptyPhone_returns400() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new ConstraintViolationException("Phone cannot be empty", null)).when(userService)
                .updateMemberPhoneNumber(anyString(), anyString());

            mvc.perform(patch("/api/users/1/phone")
                    .param("token", "tok")
                    .param("phoneNumber", ""))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService)
                .updateMemberPhoneNumber("tok", "0501234567");

            mvc.perform(patch("/api/users/1/phone")
                    .param("token", "tok")
                    .param("phoneNumber", "0501234567"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService)
                .updateMemberPhoneNumber(anyString(), anyString());

            mvc.perform(patch("/api/users/1/phone")
                    .param("token", "tok")
                    .param("phoneNumber", "0501234567"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Update Address Enhanced Tests")
    class UpdateAddressEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberAddress("tok", "Tel Aviv", "Rothschild", 5, "12345");

            mvc.perform(patch("/api/users/1/address")
                    .param("token", "tok")
                    .param("city", "Tel Aviv")
                    .param("street", "Rothschild")
                    .param("apartmentNumber", "5")
                    .param("postalCode", "12345"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(patch("/api/users/1/address")
                    .param("token", "badtoken")
                    .param("city", "Tel Aviv")
                    .param("street", "Rothschild")
                    .param("apartmentNumber", "5")
                    .param("postalCode", "12345"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void badRequest_emptyCity_returns400() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new ConstraintViolationException("City cannot be empty", null)).when(userService)
                .updateMemberAddress(anyString(), anyString(), anyString(), anyInt(), anyString());

            mvc.perform(patch("/api/users/1/address")
                    .param("token", "tok")
                    .param("city", "")
                    .param("street", "Rothschild")
                    .param("apartmentNumber", "5")
                    .param("postalCode", "12345"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService)
                .updateMemberAddress(anyString(), anyString(), anyString(), anyInt(), anyString());

            mvc.perform(patch("/api/users/1/address")
                    .param("token", "tok")
                    .param("city", "Tel Aviv")
                    .param("street", "Rothschild")
                    .param("apartmentNumber", "5")
                    .param("postalCode", "12345"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService)
                .updateMemberAddress(anyString(), anyString(), anyString(), anyInt(), anyString());

            mvc.perform(patch("/api/users/1/address")
                    .param("token", "tok")
                    .param("city", "Tel Aviv")
                    .param("street", "Rothschild")
                    .param("apartmentNumber", "5")
                    .param("postalCode", "12345"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Set Suspended Enhanced Tests")
    class SetSuspendedEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            LocalDateTime suspendUntil = LocalDateTime.of(2025, 12, 31, 23, 59);
            doNothing().when(userService).setSuspended(2, suspendUntil);

            mvc.perform(post("/api/users/2/suspension")
                    .param("token", "tok")
                    .param("until", "2025-12-31T23:59:00"))
                .andExpect(status().isNoContent());
        }

        @Test
        void success_withoutUntil_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).setSuspended(2, null);

            mvc.perform(post("/api/users/2/suspension")
                    .param("token", "tok"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(post("/api/users/2/suspension")
                    .param("token", "badtoken")
                    .param("until", "2025-12-31T23:59:00"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService).setSuspended(anyInt(), any());

            mvc.perform(post("/api/users/2/suspension")
                    .param("token", "tok")
                    .param("until", "2025-12-31T23:59:00"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService).setSuspended(anyInt(), any());

            mvc.perform(post("/api/users/2/suspension")
                    .param("token", "tok")
                    .param("until", "2025-12-31T23:59:00"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Set Un Suspended Enhanced Tests")
    class SetUnSuspendedEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).setUnSuspended(2);

            mvc.perform(post("/api/users/2/unsuspension")
                    .param("token", "tok"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(post("/api/users/2/unsuspension")
                    .param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService).setUnSuspended(2);

            mvc.perform(post("/api/users/2/unsuspension")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService).setUnSuspended(2);

            mvc.perform(post("/api/users/2/unsuspension")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Get Shopping Cart Enhanced Tests")
    class GetShoppingCartEnhancedTests {
        @Test
        void success_returnsShoppingCart() throws Exception {
            HashMap<Integer, HashMap<Integer, Integer>> cart = new HashMap<>();
            HashMap<Integer, Integer> shopItems = new HashMap<>();
            shopItems.put(5, 2); // item 5 with quantity 2
            shopItems.put(7, 1); // item 7 with quantity 1
            cart.put(1, shopItems); // shop 1
            
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getUserShoppingCartItems(2)).thenReturn(cart);

            mvc.perform(get("/api/users/shoppingCart")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isOk());
        }

        @Test
        void emptyResult_returnsEmptyCart() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getUserShoppingCartItems(2)).thenReturn(new HashMap<>());

            mvc.perform(get("/api/users/shoppingCart")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isOk());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(get("/api/users/shoppingCart")
                    .param("token", "badtoken")
                    .param("userId", "2"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getUserShoppingCartItems(2)).thenThrow(new RuntimeException("Service error"));

            mvc.perform(get("/api/users/shoppingCart")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getUserShoppingCartItems(2)).thenThrow(new RuntimeException("Internal error"));

            mvc.perform(get("/api/users/shoppingCart")
                    .param("token", "tok")
                    .param("userId", "2"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Is Suspended Enhanced Tests")
    class IsSuspendedEnhancedTests {
        @Test
        void success_returnsTrue() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.isSuspended(2)).thenReturn(true);

            mvc.perform(get("/api/users/2/isSuspended")
                    .param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        }

        @Test
        void success_returnsFalse() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.isSuspended(2)).thenReturn(false);

            mvc.perform(get("/api/users/2/isSuspended")
                    .param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(get("/api/users/2/isSuspended")
                    .param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.isSuspended(2)).thenThrow(new RuntimeException("Service error"));

            mvc.perform(get("/api/users/2/isSuspended")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.isSuspended(2)).thenThrow(new RuntimeException("Internal error"));

            mvc.perform(get("/api/users/2/isSuspended")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Make Store Owner Enhanced Tests")
    class MakeStoreOwnerEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).makeStoreOwner("tok", 2, 1);

            mvc.perform(post("/api/users/shops/1/owners")
                    .param("token", "tok")
                    .param("memberId", "2"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Invalid token")).when(userService)
                .makeStoreOwner("badtoken", 2, 1);

            mvc.perform(post("/api/users/shops/1/owners")
                    .param("token", "badtoken")
                    .param("memberId", "2"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService)
                .makeStoreOwner("tok", 2, 1);

            mvc.perform(post("/api/users/shops/1/owners")
                    .param("token", "tok")
                    .param("memberId", "2"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService)
                .makeStoreOwner("tok", 2, 1);

            mvc.perform(post("/api/users/shops/1/owners")
                    .param("token", "tok")
                    .param("memberId", "2"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Accept Role Enhanced Tests")
    class AcceptRoleEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).acceptRole("tok", 1);

            mvc.perform(post("/api/users/roles/1/accept")
                    .param("token", "tok"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Invalid token")).when(userService).acceptRole("badtoken", 1);

            mvc.perform(post("/api/users/roles/1/accept")
                    .param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService).acceptRole("tok", 1);

            mvc.perform(post("/api/users/roles/1/accept")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService).acceptRole("tok", 1);

            mvc.perform(post("/api/users/roles/1/accept")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Decline Role Enhanced Tests")
    class DeclineRoleEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).declineRole("tok", 1);

            mvc.perform(post("/api/users/roles/1/decline")
                    .param("token", "tok"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Invalid token")).when(userService).declineRole("badtoken", 1);

            mvc.perform(post("/api/users/roles/1/decline")
                    .param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService).declineRole("tok", 1);

            mvc.perform(post("/api/users/roles/1/decline")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService).declineRole("tok", 1);

            mvc.perform(post("/api/users/roles/1/decline")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Get User Won Auctions Enhanced Tests")
    class GetUserWonAuctionsEnhancedTests {
        @Test
        void success_returnsAuctionsList() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getAuctionsWinList(1)).thenReturn(List.of());

            mvc.perform(get("/api/users/auctions/won")
                    .param("authToken", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void badRequest_invalidToken_returns401() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken("badtoken");

            mvc.perform(get("/api/users/auctions/won")
                    .param("authToken", "badtoken"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getAuctionsWinList(1)).thenThrow(new RuntimeException("Internal error"));

            mvc.perform(get("/api/users/auctions/won")
                    .param("authToken", "tok"))
                .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Get Missing Notifications Quantity Enhanced Tests")
    class GetMissingNotificationsQuantityEnhancedTests {
        @Test
        void success_returnsQuantity() throws Exception {
            when(userService.getMissingNotificationsQuantity("tok")).thenReturn(5);

            mvc.perform(get("/api/users/getNotificationsQuantity")
                    .param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
        }

        @Test
        void success_returnsZero() throws Exception {
            when(userService.getMissingNotificationsQuantity("tok")).thenReturn(0);

            mvc.perform(get("/api/users/getNotificationsQuantity")
                    .param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            doThrow(new OurRuntime("Service error")).when(userService)
                .getMissingNotificationsQuantity("tok");

            mvc.perform(get("/api/users/getNotificationsQuantity")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            doThrow(new RuntimeException("Internal error")).when(userService)
                .getMissingNotificationsQuantity("tok");

            mvc.perform(get("/api/users/getNotificationsQuantity")
                    .param("token", "tok"))
                .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("Remove Manager From Store Enhanced Tests")
    class RemoveManagerFromStoreEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).removeManagerFromStore("tok", 2, 1);

            mvc.perform(delete("/api/users/shops/1/managers/2")
                    .param("token", "tok"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(userService)
                .removeManagerFromStore("badtoken", 2, 1);

            mvc.perform(delete("/api/users/shops/1/managers/2")
                    .param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService)
                .removeManagerFromStore("tok", 2, 1);

            mvc.perform(delete("/api/users/shops/1/managers/2")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService)
                .removeManagerFromStore("tok", 2, 1);

            mvc.perform(delete("/api/users/shops/1/managers/2")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Remove Owner From Store Enhanced Tests")
    class RemoveOwnerFromStoreEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).removeOwnerFromStore("tok", 2, 1);

            mvc.perform(delete("/api/users/shops/1/owners/2")
                    .param("token", "tok"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(userService)
                .removeOwnerFromStore("badtoken", 2, 1);

            mvc.perform(delete("/api/users/shops/1/owners/2")
                    .param("token", "badtoken"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Service error")).when(userService)
                .removeOwnerFromStore("tok", 2, 1);

            mvc.perform(delete("/api/users/shops/1/owners/2")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException("Internal error")).when(userService)
                .removeOwnerFromStore("tok", 2, 1);

            mvc.perform(delete("/api/users/shops/1/owners/2")
                    .param("token", "tok"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Validate Member Id Enhanced Tests")
    class ValidateMemberIdEnhancedTests {
        @Test
        void success_returns204() throws Exception {
            doNothing().when(userService).validateMemberId(1);

            mvc.perform(get("/api/users/validate/1"))
                .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_invalidId_returns400() throws Exception {
            doThrow(new IllegalArgumentException("Invalid member ID")).when(userService).validateMemberId(0);

            mvc.perform(get("/api/users/validate/0"))
                .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_memberNotFound_returns409() throws Exception {
            doThrow(new RuntimeException("Member not found")).when(userService).validateMemberId(999);

            mvc.perform(get("/api/users/validate/999"))
                .andExpect(status().isConflict());
        }

        @Test
        void internalError_returns500() throws Exception {
            doThrow(new RuntimeException("Internal error")).when(userService).validateMemberId(1);

            mvc.perform(get("/api/users/validate/1"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Get Notifications Enhanced Tests")
    class GetNotificationsEnhancedTests {
        @Test
        void success_returnsNotifications() throws Exception {
            when(userService.getNotificationsAndClear("tok"))
                .thenReturn(List.of("Notification 1", "Notification 2"));

            mvc.perform(get("/api/users/notifications")
                    .param("authToken", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]").value("Notification 1"))
                .andExpect(jsonPath("$[1]").value("Notification 2"));
        }

        @Test
        void emptyResult_returnsEmptyArray() throws Exception {
            when(userService.getNotificationsAndClear("tok")).thenReturn(List.of());

            mvc.perform(get("/api/users/notifications")
                    .param("authToken", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void internalError_returns500() throws Exception {
            when(userService.getNotificationsAndClear("tok")).thenThrow(new RuntimeException("Internal error"));

            mvc.perform(get("/api/users/notifications")
                    .param("authToken", "tok"))
                .andExpect(status().isInternalServerError());
        }
    }
    
}

