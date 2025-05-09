package PresentationLayerTests;

import java.util.Arrays;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.User.UserService;
import DomainLayer.Member;
import PresentationLayer.Controller.UserController;

/**
 * Comprehensive slice tests for UserController.
 */
@WebMvcTest(controllers = UserController.class)
@ContextConfiguration(classes = UserControllerTests.TestBootApp.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTests {

    @SpringBootApplication(scanBasePackages = "PresentationLayer")
    static class TestBootApp {}

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

            mvc.perform(get("/api/users/1").param("token","tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.memberId").value(1));
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(get("/api/users/1").param("token","bad"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void notFound_returns404() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getUserById(1)).thenThrow(new NoSuchElementException());

            mvc.perform(get("/api/users/1").param("token","tok"))
               .andExpect(status().isNotFound());
        }

        @Test
        void conflict_serviceError_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getUserById(1)).thenThrow(new RuntimeException());

            mvc.perform(get("/api/users/1").param("token","tok"))
               .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("2. REGISTER")
    class Register {
        @Test
        void success_returns201() throws Exception {
            doNothing().when(userService).addMember(anyString(), anyString(), anyString(), anyString(), anyString());

            mvc.perform(post("/api/users/register")
                    .param("username","u123")
                    .param("password","p")
                    .param("email","e@mail")
                    .param("phoneNumber","123")
                    .param("address","addr"))
               .andExpect(status().isCreated());
        }

        @Test
        void badRequest_invalidParams_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(userService)
                .addMember(anyString(), anyString(), anyString(), anyString(), anyString());

            mvc.perform(post("/api/users/register")
                    .param("username","usr") // too short
                    .param("password","p")
                    .param("email","e@mail")
                    .param("phoneNumber","123")
                    .param("address","addr"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_duplicate_returns409() throws Exception {
            doThrow(new RuntimeException()).when(userService)
                .addMember(anyString(), anyString(), anyString(), anyString(), anyString());

            mvc.perform(post("/api/users/register")
                    .param("username","u123")
                    .param("password","p")
                    .param("email","e@mail")
                    .param("phoneNumber","123")
                    .param("address","addr"))
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

            mvc.perform(post("/api/users/2/admin").param("token","tok"))
               .andExpect(status().isNoContent());
        }

        @Test
        void removeAdmin_success_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).removeAdmin("tok", 2);

            mvc.perform(delete("/api/users/2/admin").param("token","tok"))
               .andExpect(status().isNoContent());
        }

        @Test
        void badRequest_illegalArg_returns400() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new IllegalArgumentException()).when(userService).makeAdmin("tok", 2);

            mvc.perform(post("/api/users/2/admin").param("token","tok"))
               .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_notAllowed_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).removeAdmin("tok", 2);

            mvc.perform(delete("/api/users/2/admin").param("token","tok"))
               .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("4. LIST ADMINS")
    class ListAdmins {
        @Test
        void success_returns200AndList() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            when(userService.getAllAdmins("tok")).thenReturn(Arrays.asList(1,2,3));

            mvc.perform(get("/api/users/admins").param("token","tok"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0]").value(1));
        }

        @Test
        void badRequest_invalidToken_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).ValidateToken(anyString());

            mvc.perform(get("/api/users/admins").param("token","bad"))
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
                    .param("token","tok")
                    .param("username","newUser"))
               .andExpect(status().isNoContent());
        }

        @Test
        void updatePassword_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberPassword("tok", "newPass");

            mvc.perform(patch("/api/users/1/password")
                    .param("token","tok")
                    .param("password","newPass"))
               .andExpect(status().isNoContent());
        }

        @Test
        void updateEmail_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberEmail("tok", "new@mail");

            mvc.perform(patch("/api/users/1/email")
                    .param("token","tok")
                    .param("email","new@mail"))
               .andExpect(status().isNoContent());
        }

        @Test
        void updatePhone_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberPhoneNumber("tok", "555");

            mvc.perform(patch("/api/users/1/phone")
                    .param("token","tok")
                    .param("phoneNumber","555"))
               .andExpect(status().isNoContent());
        }

        @Test
        void updateAddress_returns204() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doNothing().when(userService).updateMemberAddress("tok", "City", "Street", 10, null);

            mvc.perform(patch("/api/users/1/address")
                    .param("token","tok")
                    .param("city","City")
                    .param("street","Street")
                    .param("apartmentNumber","10"))
               .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("6. ERROR MAPPING")
    class ErrorMapping {
        @Test
        void updateUsername_conflict_returns409() throws Exception {
            when(authService.ValidateToken("tok")).thenReturn(1);
            doThrow(new RuntimeException()).when(userService).updateMemberUsername("tok","user");

            mvc.perform(patch("/api/users/1/username")
                    .param("token","tok")
                    .param("username","user"))
               .andExpect(status().isConflict());
        }
    }
}
