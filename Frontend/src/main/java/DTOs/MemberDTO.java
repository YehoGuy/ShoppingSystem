package DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Read‑model for member‐profile APIs.
 * (For registration you’d typically use a slimmer SignupRequestDTO.)
 */
public record MemberDTO(
        @Positive int memberId,
        @NotBlank String username,
        @Email    String email,
        @NotBlank String phoneNumber,
        LocalDateTime suspendedUntil,
        AddressDTO address,
        List<rolesDTO> roles,
        List<rolesDTO> pendingRoles,
        List<Integer> orderHistory,
        ShoppingCartDTO shoppingCart) {}