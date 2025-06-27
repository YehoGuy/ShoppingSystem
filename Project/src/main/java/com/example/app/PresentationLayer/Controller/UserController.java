package com.example.app.PresentationLayer.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Guest;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.DomainLayer.User;
import com.example.app.DomainLayer.Purchase.BidReciept;
import com.example.app.PresentationLayer.DTO.Purchase.BidRecieptDTO;
import com.example.app.PresentationLayer.DTO.Role.RoleDTO;
import com.example.app.PresentationLayer.DTO.User.GuestDTO;
import com.example.app.PresentationLayer.DTO.User.MemberDTO;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Base path: /api/users (JSON in / JSON out)
 *
 * Endpoints:
 * 1. GET /{userId} params: token → 200 MemberDTO / GuestDTO
 * 2. POST /register params: username, password, email, phoneNumber, address →
 * 201
 * 3. POST /{userId}/admin params: token → 204 assign admin
 * 4. DELETE /{userId}/admin params: token → 204 remove admin
 * 5. GET /admins params: token → 200 [ids]
 * 6. PATCH /{userId}/username params: token, username → 204
 * 7. PATCH /{userId}/password params: token, password → 204
 * 8. PATCH /{userId}/email params: token, email → 204
 * 9. PATCH /{userId}/phone params: token, phone → 204
 * 10. PATCH /{userId}/address params: token, city, street, apartmentNumber,
 * postalCode → 204
 *
 * 11. GET /{userId}/isAdmin params: token → 200 boolean
 * 12. POST /encoder/testMode params: enable → 204
 * 13. GET /validate/{memberId} – → 204
 * 14. POST /login/guest – → 200 token
 * 15. POST /login/member params: username, password, [guestToken] → 200 token
 * 16. POST /logout params: token → 200 guestToken
 *
 * 17. GET /shops/{shopId}/permissions params: token → 200
 * {memberId:[permissions]}
 * 18. PATCH /shops/{shopId}/permissions/{memberId} params: token,
 * body [PermissionsEnum[]] → 204
 * 19. POST /shops/{shopId}/managers params: token, memberId,
 * body [PermissionsEnum[]] → 204 make manager
 * 20. DELETE /shops/{shopId}/managers/{memberId} params: token → 204 remove
 * manager
 * 21. POST /shops/{shopId}/owners params: token, memberId → 204 make owner
 * 22. DELETE /shops/{shopId}/owners/{memberId} params: token → 204 remove owner
 * 23. DELETE /shops/{shopId}/assignee/{assigneeId}/all params: token → 204
 * remove all assignments
 *
 * 24. POST /roles/{shopId}/accept params: token → 204 accept role
 * 25. POST /roles/{shopId}/decline params: token → 204 decline role
 * 26. PATCH /{userId}/suspension params: token, until(ISO-8601) → 204
 * 27. GET /{userId}/suspension params: token → 200 boolean
 * 28. GET /suspended params: token → 200 [ids]
 * 29. GET /shops/{shopId}/workers params: token → 200 [MemberDTO]
 * 30. GET /getPendingRoles params: token → 200 [RoleDTO]
 * 31. GET /allusers params: token → 200 [MemberDTO]
 *
 * Error mapping (all endpoints):
 * 400 – Bad data / validation failure
 * 404 – Entity not found
 * 409 – Business rule conflict / permissions
 * 500 – Internal server error
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
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(
            @PathVariable @Min(1) int userId,
            @RequestParam String token) {
        try {
            authService.ValidateToken(token);
            User user = userService.getUserById(userId);
            if (user == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            if (user instanceof Member) {
                Member member = (Member) user;
                MemberDTO userDTO = MemberDTO.fromDomain(member, null);
                return ResponseEntity.ok(userDTO);
            } else if (user instanceof Guest) {
                Guest guest = (Guest) user;
                GuestDTO userDTO = GuestDTO.fromDomain(guest, null);
                return ResponseEntity.ok(userDTO);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not member or Guest");
            }
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

    @GetMapping("/allmembers")
    public ResponseEntity<?> getAllMembers(@RequestParam String token) {
        try {
            authService.ValidateToken(token);
            List<Member> members = userService.getAllMembers();
            List<MemberDTO> membersDTO = new ArrayList<>();
            for (Member member : members) {
                membersDTO.add(MemberDTO.fromDomain(member, null));
            }
            return ResponseEntity.ok(membersDTO);

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }

    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestParam @NotBlank @Size(min = 3, max = 20) String username,
            @RequestParam @NotBlank String password,
            @RequestParam @NotBlank String email,
            @RequestParam @NotBlank String phoneNumber,
            @RequestParam @NotBlank String address) {
        try {
            String token = userService.addMember(username, password, email, phoneNumber, address);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already taken");
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(token);

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
            @RequestParam @NotBlank @Size(min = 3, max = 20) String username) {
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
            @RequestParam(required = false) String postalCode) {
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

    @GetMapping("/{userId}/isAdmin")
    public ResponseEntity<?> isAdmin(
            @PathVariable @Min(1) int userId,
            @RequestParam String token) {

        try {
            authService.ValidateToken(token); // only authenticated callers
            boolean res = userService.isAdmin(userId);
            return ResponseEntity.ok(res);

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    /* -------------------------------------------------- */
    /* Switch encoder to test mode (mainly for QA) */
    /* -------------------------------------------------- */
    @PostMapping("/encoder/testMode")
    public ResponseEntity<Void> setEncoderToTest(@RequestParam boolean enable) {
        try {
            userService.setEncoderToTest(enable);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/validate/{memberId}")
    public ResponseEntity<Void> validateMemberId(@PathVariable @Min(1) int memberId) {
        try {
            userService.validateMemberId(memberId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/login/guest")
    public ResponseEntity<?> loginAsGuest() {
        try {
            String token = userService.loginAsGuest();
            return ResponseEntity.ok(token);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    @PostMapping("/login/member")
    public ResponseEntity<?> loginAsMember(
            @RequestParam @NotBlank String username,
            @RequestParam @NotBlank String password,
            @RequestParam(defaultValue = "") String guestToken) {

        try {
            String token = userService.loginAsMember(username, password, guestToken);
            return ResponseEntity.ok(token);
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String token) {
        try {
            String newGuestToken = userService.logout(token);
            return ResponseEntity.ok(newGuestToken);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    @GetMapping("/shops/{shopId}/permissions")
    public ResponseEntity<?> getPermissionsByShop(
            @PathVariable @Min(1) int shopId,
            @RequestParam String token) {

        try {
            authService.ValidateToken(token);
            HashMap<Integer, PermissionsEnum[]> map = userService.getPermitionsByShop(token, shopId);
            return ResponseEntity.ok(map);
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    @PostMapping(path = "/shops/{shopId}/permissions/{memberId}", consumes = "application/json")
    public ResponseEntity<Void> changePermissions(
            @PathVariable @Min(1) int shopId,
            @PathVariable @Min(1) int memberId,
            @RequestParam String token,
            @RequestBody PermissionsEnum[] permissions) {

        try {
            authService.ValidateToken(token);
            userService.changePermissions(token, memberId, shopId, permissions);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(path = "/shops/{shopId}/managers", consumes = "application/json")
    public ResponseEntity<Void> makeManagerOfStore(
            @PathVariable @Min(1) int shopId,
            @RequestParam @Min(1) int memberId,
            @RequestParam String token,
            @RequestBody PermissionsEnum[] permissions) {

        try {
            authService.ValidateToken(token);
            userService.makeManagerOfStore(token, memberId, shopId, permissions);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/shops/{shopId}/managers/{memberId}")
    public ResponseEntity<Void> removeManagerFromStore(
            @PathVariable @Min(1) int shopId,
            @PathVariable @Min(1) int memberId,
            @RequestParam String token) {

        try {
            userService.removeManagerFromStore(token, memberId, shopId);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/shops/{shopId}/owners")
    public ResponseEntity<Void> makeStoreOwner(
            @PathVariable @Min(1) int shopId,
            @RequestParam @Min(1) int memberId,
            @RequestParam String token) {

        try {
            userService.makeStoreOwner(token, memberId, shopId);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/shops/{shopId}/owners/{memberId}")
    public ResponseEntity<Void> removeOwnerFromStore(
            @PathVariable @Min(1) int shopId,
            @PathVariable @Min(1) int memberId,
            @RequestParam String token) {

        try {
            userService.removeOwnerFromStore(token, memberId, shopId);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/shops/{shopId}/owner")
    public ResponseEntity<Integer> getShopOwner(
            @PathVariable @Min(1) int shopId,
            @RequestParam("token") String token) {
        try {
            // only authenticated callers
            authService.ValidateToken(token);
            // delegate to your service layer
            int ownerId = userService.getShopOwner(shopId);
            return ResponseEntity.ok(ownerId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (NoSuchElementException ex) {
            // shop not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/shops/{shopId}/assignee/{assigneeId}/all")
    public ResponseEntity<Void> removeAllAssigned(
            @PathVariable @Min(1) int shopId,
            @PathVariable @Min(1) int assigneeId,
            @RequestParam String token) {

        try {
            authService.ValidateToken(token); // ensure caller is authenticated
            userService.removeAllAssigned(assigneeId, shopId);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/roles/{shopId}/accept")
    public ResponseEntity<Void> acceptRole(
            @PathVariable @Min(1) int shopId,
            @RequestParam String token) {

        try {
            userService.acceptRole(token, shopId);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/roles/{shopId}/decline")
    public ResponseEntity<Void> declineRole(
            @PathVariable @Min(1) int shopId,
            @RequestParam String token) {

        try {
            userService.declineRole(token, shopId);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
     * Suspend or unsuspend a user (admin-only token).
     * Pass an ISO-8601 timestamp in `until`. To unsuspend, omit the param.
     */
    @PostMapping("/{userId}/suspension")
    public ResponseEntity<Void> setSuspended(
            @PathVariable @Min(1) int userId,
            @RequestParam String token,
            @RequestParam(name = "until", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime suspendedUntil) {

        try {
            authService.ValidateToken(token);
            userService.setSuspended(userId, suspendedUntil); // null = lift suspension
            return ResponseEntity.noContent().build();

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /*
     * Suspend or unsuspend a user (admin-only token).
     * Pass an ISO-8601 timestamp in `until`. To unsuspend, omit the param.
     */
    @PostMapping("/{userId}/unsuspension")
    public ResponseEntity<Void> setUnSuspended(
            @PathVariable @Min(1) int userId,
            @RequestParam String token) {

        try {
            authService.ValidateToken(token);
            userService.setUnSuspended(userId); // null = lift suspension
            return ResponseEntity.noContent().build();

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /* Check whether the user is currently suspended. */
    @GetMapping("/{userId}/isSuspended")
    public ResponseEntity<?> isSuspended(
            @PathVariable @Min(1) int userId,
            @RequestParam String token) {

        try {
            authService.ValidateToken(token);
            boolean result = userService.isSuspended(userId);
            return ResponseEntity.ok(result);

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    /* List IDs of all users that are presently suspended. */
    @GetMapping("/suspended")
    public ResponseEntity<?> listSuspended(@RequestParam String token) {

        try {
            authService.ValidateToken(token);
            List<Integer> ids = userService.getSuspendedUsers();
            return ResponseEntity.ok(ids);

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    /*
     * Suspend or unsuspend a user (admin-only token).
     * Pass an ISO-8601 timestamp in `until`. To unsuspend, omit the param.
     */
    @PostMapping("/{userId}/ban")
    public ResponseEntity<Void> banUser(
            @PathVariable @Min(1) int userId,
            @RequestParam String token) {
        try {
            authService.ValidateToken(token);
            userService.banUser(userId); // null = lift suspension
            return ResponseEntity.noContent().build();

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/shops/{shopId}/workers")
    public ResponseEntity<?> getShopMembers(
            @PathVariable @Min(1) int shopId,
            @RequestParam String token) {

        try {
            authService.ValidateToken(token);
            List<Member> members = userService.getShopMembers(shopId);
            List<MemberDTO> membersDTO = new ArrayList<>();
            for (Member member : members) {
                membersDTO.add(MemberDTO.fromDomain(member, null));
            }
            return ResponseEntity.ok(membersDTO);

        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    @GetMapping("/getAcceptedRoles")
    public ResponseEntity<?> getAcceptedRoles(@RequestParam("authToken") String token) {
        try {
            List<Role> acceptedRoles = userService.getAcceptedRoles(token);
            List<RoleDTO> acceptedRolesDTO = new ArrayList<>();
            for (Role role : acceptedRoles) {
                int shopId = role.getShopId();
                User assignee = userService.getUserById(role.getAssigneeId());
                userService.validateMemberId(role.getAssigneeId());
                String username = ((Member) assignee).getUsername();
                List<String> permissions = Arrays.stream(role.getPermissions())
                        .map(PermissionsEnum::name)
                        .toList();
                String rolename = "manager";
                for (PermissionsEnum permission : role.getPermissions()) {
                    if (permission == PermissionsEnum.manageOwners) {
                        rolename = "founder";
                        break;
                    }
                    if (permission == PermissionsEnum.manageManagers && rolename != "founder") {
                        rolename = "owner";
                        break;
                    }
                }
                RoleDTO roleDTO = new RoleDTO(shopId, rolename, permissions, username);
                acceptedRolesDTO.add(roleDTO);
            }
            return ResponseEntity.ok(acceptedRolesDTO);
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/getPendingRoles")
    public ResponseEntity<List<RoleDTO>> getPendingRoles(@RequestParam("authToken") String token) {
        try {
            List<Role> pendingRoles = userService.getPendingRoles(token);
            List<RoleDTO> pendingRolesDTO = new ArrayList<>();
            for (Role role : pendingRoles) {
                int shopId = role.getShopId();
                User assignee = userService.getUserById(role.getAssigneeId());
                userService.validateMemberId(role.getAssigneeId());
                String username = ((Member) assignee).getUsername();
                List<String> permissions = Arrays.stream(role.getPermissions())
                        .map(PermissionsEnum::name)
                        .toList();
                String rolename = "manager";
                for (PermissionsEnum permission : role.getPermissions()) {
                    if (permission == PermissionsEnum.manageOwners) {
                        rolename = "founder";
                        break;
                    }
                    if (permission == PermissionsEnum.manageManagers && rolename != "founder") {
                        rolename = "owner";
                        break;
                    }
                }
                RoleDTO roleDTO = new RoleDTO(shopId, rolename, permissions, username);
                pendingRolesDTO.add(roleDTO);
            }
            return ResponseEntity.ok(pendingRolesDTO);
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<String>> getNotifications(@RequestParam("authToken") String token) {
        try {
            List<String> notes = userService.getNotificationsAndClear(token);
            return ResponseEntity.ok(notes);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/shoppingCart")
    public ResponseEntity<HashMap<Integer, HashMap<Integer, Integer>>> getShoppingCart(@RequestParam String token,
            @RequestParam int userId) {
        try {
            authService.ValidateToken(token);
            HashMap<Integer, HashMap<Integer, Integer>> cart = userService.getUserShoppingCartItems(userId);
            return ResponseEntity.ok(cart);
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/shoppingCart/{shopID}/{itemID}")
    public ResponseEntity<Void> addNewItemToShoppingCart(
            @RequestParam int quantity,
            @RequestParam String token,
            @PathVariable int shopID,
            @PathVariable int itemID) {
        try {
            authService.ValidateToken(token);
            userService.addItemToShoppingCart(token, shopID, itemID, quantity);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/shoppingCart/{shopID}/{itemID}/plus")
    public ResponseEntity<Void> addItemToShoppingCart(
            @RequestParam String token,
            @RequestParam int userId,
            @PathVariable int shopID,
            @PathVariable int itemID) {
        try {
            authService.ValidateToken(token);
            userService.updateShoppingCartItemQuantity(userId, shopID, itemID, true);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/shoppingCart/{shopID}/{itemID}/minus")
    public ResponseEntity<Void> decreaseItemInShoppingCart(
            @RequestParam String token,
            @RequestParam int userId,
            @PathVariable int shopID,
            @PathVariable int itemID) {
        try {
            authService.ValidateToken(token);
            userService.updateShoppingCartItemQuantity(userId, shopID, itemID, false);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/shoppingCart/{shopID}/{itemID}/remove")
    public ResponseEntity<Void> removeCompletelyItemFromShoppingCart(
            @RequestParam String token,
            @RequestParam int userId,
            @PathVariable int shopID,
            @PathVariable int itemID) {
        try {
            authService.ValidateToken(token);
            userService.removeItemFromShoppingCart(userId, shopID, itemID);
            return ResponseEntity.noContent().build();
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/hasRole")
    public ResponseEntity<Boolean> hasRole(
            @RequestParam String token,
            @RequestParam int userId,
            @RequestParam int shopId) {
        try {
            authService.ValidateToken(token);
            boolean hasRole = userService.hasRoleInShop(userId, shopId);
            return ResponseEntity.ok(hasRole);
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(false);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(false);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    @GetMapping("/hasPermission")
    public ResponseEntity<Boolean> hasPermission(
            @RequestParam String token,
            @RequestParam int userId,
            @RequestParam int shopId,
            @RequestParam PermissionsEnum permission) {
        try {
            authService.ValidateToken(token);
            boolean hasPermission = userService.hasPermission(userId, permission, shopId);
            return ResponseEntity.ok(hasPermission);
        } catch (ConstraintViolationException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(false);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(false);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    @GetMapping("/auctions/won")
    public ResponseEntity<List<BidRecieptDTO>> getUserWonAuctions(
            @RequestParam String authToken) {
        try {
            int userId = authService.ValidateToken(authToken);
            List<BidReciept> won = userService.getAuctionsWinList(userId);
            // map domain‐model receipts → DTOs
            List<BidRecieptDTO> dtos = won.stream()
                    .map(BidRecieptDTO::fromDomain)
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException ex) {
            // token invalid
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/getNotificationsQuantity")
    public ResponseEntity<Integer> getMissingNotificationsQuantity(@RequestParam String token) {
        try {
            return ResponseEntity.ok(userService.getMissingNotificationsQuantity(token));
        } catch (OurArg | OurRuntime ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
