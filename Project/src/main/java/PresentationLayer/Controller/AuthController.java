package PresentationLayer.Controller;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ApplicationLayer.AuthTokenService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.ConstraintViolationException;

/**
 *  Base path: /api/auth         (all calls are JSON in / JSON out)
 *
 * 1. POST   /guest
 *    Params : guestId
 *    Success: 201 →  "eyJhbGciOi..."   (JWT token)
 *
 * 2. POST   /login
 *    Params : username, password, userId
 *    Success: 201 →  "eyJhbGciOi..."
 *
 * 3. POST   /logout
 *    Params : authToken
 *    Success: 204  (empty)
 *
 * 4. POST   /validate
 *    Params : authToken
 *    Success: 200 →  123          (userId extracted)
 *
 *  Error mapping (all endpoints)
 *    400 – Bad data / validation failure
 *    401 – Unauthorized / token expired or invalid
 *    404 – Token not found (logout / validate)
 *    500 – Unhandled server error
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthTokenService authService;

    public AuthController(AuthTokenService authService) {
        this.authService = authService;
    }

    /* ──────────────────────── GUEST AUTH ──────────────────────── */

    @PostMapping("/guest")
    public ResponseEntity<?> authenticateGuest(
            @RequestParam int guestId) {
        try {
            String token = authService.AuthenticateGuest(guestId);
            return ResponseEntity.status(HttpStatus.CREATED).body(token);

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Internal server error");
        }
    }

    /* ─────────────────────────── LOGIN ─────────────────────────── */

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam int userId) {
        try {
            String token = authService.Login(username, password, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(token);

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Internal server error");
        }
    }

    /* ────────────────────────── LOGOUT ─────────────────────────── */

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestParam String authToken) {
        try {
            authService.Logout(authToken);
            return ResponseEntity.noContent().build();

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (ExpiredJwtException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        } catch (IllegalArgumentException | ConstraintViolationException ex) {
            return ResponseEntity.badRequest().build();

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* ──────────────────────── VALIDATE ───────────────────────── */

    @PostMapping("/validate")
    public ResponseEntity<?> validate(
            @RequestParam String authToken) {
        try {
            Integer userId = authService.ValidateToken(authToken);
            return ResponseEntity.ok(userId);

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());

        } catch (ExpiredJwtException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token expired");

        } catch (IllegalArgumentException | ConstraintViolationException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (Exception ex) {
            // covers invalid or other JWT exceptions
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        }
    }
}
