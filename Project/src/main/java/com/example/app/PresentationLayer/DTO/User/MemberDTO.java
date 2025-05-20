package com.example.app.PresentationLayer.DTO.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

import com.example.app.PresentationLayer.DTO.Purchase.AddressDTO;
import com.example.app.PresentationLayer.DTO.Role.RoleDTO;

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
        List<RoleDTO> roles,
        List<RoleDTO> pendingRoles,
        List<Integer> orderHistory,
        ShoppingCartDTO shoppingCart) {
    /* -------- Domain → DTO -------- */
    public static MemberDTO fromDomain(com.example.app.DomainLayer.Member m) {
        return new MemberDTO(
            m.getMemberId(),
            m.getUsername(),
            m.getEmail(),
            m.getPhoneNumber(),
            m.getSuspendedUntil(),
            m.getAddress() != null ? AddressDTO.fromDomain(m.getAddress()) : null,
            m.getRoles().stream().map(RoleDTO::fromDomain).toList(),
            m.getPendingRoles().stream().map(RoleDTO::fromDomain).toList(),
            m.getOrderHistory(),
            ShoppingCartDTO.fromDomain(m.getShoppingCart())
        );
    }
}
