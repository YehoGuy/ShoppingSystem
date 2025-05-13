package PresentationLayerTests;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.PresentationLayer.Controller.AuthController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.ExpiredJwtException;

/**
 * Comprehensive slice tests for AuthController.
 */
@WebMvcTest(controllers = AuthController.class)
@ContextConfiguration(classes = AuthControllerTests.TestBootApp.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTests {

    @SpringBootApplication(scanBasePackages = "com.example.app.PresentationLayer")
    static class TestBootApp {
    }

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AuthTokenService authService;

    @Nested
    @DisplayName("1. GUEST AUTHENTICATION")
    class GuestAuthTests {
        @Test
        void success_returns201AndToken() throws Exception {
            when(authService.AuthenticateGuest(123)).thenReturn("token123");

            mvc.perform(post("/api/auth/guest")
                    .param("guestId", "123"))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("token123"));
        }

        @Test
        void badRequest_returns400() throws Exception {
            when(authService.AuthenticateGuest(anyInt()))
                    .thenThrow(new IllegalArgumentException("invalid"));

            mvc.perform(post("/api/auth/guest").param("guestId", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_returns409() throws Exception {
            when(authService.AuthenticateGuest(anyInt()))
                    .thenThrow(new RuntimeException("exists"));

            mvc.perform(post("/api/auth/guest").param("guestId", "123"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("2. LOGIN")
    class LoginTests {
        @Test
        void success_returns201AndToken() throws Exception {
            when(authService.Login(eq("user"), eq("pass"), eq(42)))
                    .thenReturn("jwt-token");

            mvc.perform(post("/api/auth/login")
                    .param("username", "user")
                    .param("password", "pass")
                    .param("userId", "42"))
                    .andExpect(status().isCreated())
                    .andExpect(content().string("jwt-token"));
        }

        @Test
        void badRequest_returns400() throws Exception {
            when(authService.Login(anyString(), anyString(), anyInt()))
                    .thenThrow(new IllegalArgumentException());

            mvc.perform(post("/api/auth/login")
                    .param("username", "u")
                    .param("password", "p")
                    .param("userId", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void conflict_returns409() throws Exception {
            when(authService.Login(anyString(), anyString(), anyInt()))
                    .thenThrow(new RuntimeException());

            mvc.perform(post("/api/auth/login")
                    .param("username", "user")
                    .param("password", "pass")
                    .param("userId", "42"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("3. LOGOUT")
    class LogoutTests {
        @Test
        void success_returns204() throws Exception {
            doNothing().when(authService).Logout(eq("tok"));

            mvc.perform(post("/api/auth/logout")
                    .param("authToken", "tok"))
                    .andExpect(status().isNoContent());
        }

        @Test
        void notFound_returns404() throws Exception {
            doThrow(new NoSuchElementException()).when(authService).Logout(eq("tok"));

            mvc.perform(post("/api/auth/logout").param("authToken", "tok"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void unauthorized_expiredJwt() throws Exception {
            doThrow(new ExpiredJwtException(null, null, "expired")).when(authService).Logout(eq("tok"));

            mvc.perform(post("/api/auth/logout").param("authToken", "tok"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void badRequest_returns400() throws Exception {
            doThrow(new IllegalArgumentException()).when(authService).Logout(eq("tok"));

            mvc.perform(post("/api/auth/logout").param("authToken", "tok"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("4. VALIDATE TOKEN")
    class ValidateTests {
        @Test
        void success_returns200AndUserId() throws Exception {
            when(authService.ValidateToken(eq("tok"))).thenReturn(77);

            mvc.perform(post("/api/auth/validate").param("authToken", "tok"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("77"));
        }

        @Test
        void notFound_returns404() throws Exception {
            when(authService.ValidateToken(eq("tok")))
                    .thenThrow(new NoSuchElementException());

            mvc.perform(post("/api/auth/validate").param("authToken", "tok"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void expired_returns401() throws Exception {
            when(authService.ValidateToken(eq("tok")))
                    .thenThrow(new ExpiredJwtException(null, null, "exp"));

            mvc.perform(post("/api/auth/validate").param("authToken", "tok"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void badRequest_returns400() throws Exception {
            when(authService.ValidateToken(eq("tok")))
                    .thenThrow(new IllegalArgumentException());

            mvc.perform(post("/api/auth/validate").param("authToken", "tok"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void invalidJwt_returns401() throws Exception {
            when(authService.ValidateToken(eq("tok")))
                    .thenThrow(new RuntimeException("invalid"));

            mvc.perform(post("/api/auth/validate").param("authToken", "tok"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
