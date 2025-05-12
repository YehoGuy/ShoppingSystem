package PresentationLayer.Controller;

import ApplicationLayer.User.UserService;
import ApplicationLayer.AuthTokenService;
import DomainLayer.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Base path: /api/users    (JSON in / JSON out)
 *
 * Endpoints:
 * 1. GET    /{userId}               params: token         → return User
 * 2. POST   /register               params: username, password, email, phoneNumber, address → 201
 * 3. POST   /{userId}/admin         params: token          → 204 assign admin
 * 4. DELETE /{userId}/admin         params: token          → 204 remove admin
 * 5. GET    /admins                 params: token          → 200 [ids]
 * 6. PATCH  /{userId}/username      params: token, username→204
 * 7. PATCH  /{userId}/password      params: token, password→204
 * 8. PATCH  /{userId}/email         params: token, email   →204
 * 9. PATCH  /{userId}/phone         params: token, phone   →204
 * 10.PATCH /{userId}/address       params: token, city, street, apartmentNumber, postalCode →204
 *
 * Error mapping (all endpoints):
 *   400 – Bad data / validation failure
 *   404 – Entity not found
 *   409 – Business rule conflict / permissions
 *   500 – Internal server error
 */
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserService userService;
    private final AuthTokenService authService;

    public UserController(UserService userService, AuthTokenService authService) {
        this.userService = userService;
        this.authService = authService;
        this.userService.setServices(authService);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(
            @PathVariable @Min(1) int userId,
            @RequestParam String token) {
        try {
            authService.ValidateToken(token);
            User user = userService.getUserById(userId);
            return ResponseEntity.ok(user);

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam @NotBlank @Size(min=3,max=20) String username,
            @RequestParam @NotBlank String password,
            @RequestParam @NotBlank String email,
            @RequestParam @NotBlank String phoneNumber,
            @RequestParam @NotBlank String address) {
        try {
            userService.addMember(username, password, email, phoneNumber, address);
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    @PostMapping("/{userId}/admin")
    public ResponseEntity<Void> makeAdmin(
            @PathVariable @Min(1) int userId,
            @RequestParam String token) {
        try {
            authService.ValidateToken(token);
            userService.makeAdmin(token, userId);
            return ResponseEntity.noContent().build();

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{userId}/admin")
    public ResponseEntity<Void> removeAdmin(
            @PathVariable @Min(1) int userId,
            @RequestParam String token) {
        try {
            authService.ValidateToken(token);
            userService.removeAdmin(token, userId);
            return ResponseEntity.noContent().build();

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/admins")
    public ResponseEntity<?> listAdmins(@RequestParam String token) {
        try {
            authService.ValidateToken(token);
            List<Integer> admins = userService.getAllAdmins(token);
            return ResponseEntity.ok(admins);

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    @PatchMapping("/{userId}/username")
    public ResponseEntity<Void> updateUsername(
            @PathVariable @Min(1) int userId,
            @RequestParam String token,
            @RequestParam @NotBlank @Size(min=3,max=20) String username) {
        try {
            authService.ValidateToken(token);
            userService.updateMemberUsername(token, username);
            return ResponseEntity.noContent().build();

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable @Min(1) int userId,
            @RequestParam String token,
            @RequestParam @NotBlank String password) {
        try {
            authService.ValidateToken(token);
            userService.updateMemberPassword(token, password);
            return ResponseEntity.noContent().build();

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{userId}/email")
    public ResponseEntity<Void> updateEmail(
            @PathVariable @Min(1) int userId,
            @RequestParam String token,
            @RequestParam @NotBlank String email) {
        try {
            authService.ValidateToken(token);
            userService.updateMemberEmail(token, email);
            return ResponseEntity.noContent().build();

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{userId}/phone")
    public ResponseEntity<Void> updatePhone(
            @PathVariable @Min(1) int userId,
            @RequestParam String token,
            @RequestParam @NotBlank String phoneNumber) {
        try {
            authService.ValidateToken(token);
            userService.updateMemberPhoneNumber(token, phoneNumber);
            return ResponseEntity.noContent().build();

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{userId}/address")
    public ResponseEntity<Void> updateAddress(
            @PathVariable @Min(1) int userId,
            @RequestParam String token,
            @RequestParam @NotBlank String city,
            @RequestParam @NotBlank String street,
            @RequestParam int apartmentNumber,
            @RequestParam(required=false) String postalCode) {
        try {
            authService.ValidateToken(token);
            userService.updateMemberAddress(token, city, street, apartmentNumber, postalCode);
            return ResponseEntity.noContent().build();

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
