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

    /* -------- Domain ➜ DTO -------- */
    public static MemberDTO fromDomain(com.example.app.DomainLayer.Member m) {
        return new MemberDTO(
                m.getMemberId(),
                m.getUsername(),
                m.getEmail(),
                m.getPhoneNumber(),
                // Member#isSuspended() hides the instant; expose the timestamp instead
                m.isSuspended() ? LocalDateTime.now().plusYears(100) : null,
                m.getAddress() != null ? AddressDTO.fromDomain(m.getAddress()) : null,
                m.getRoles().stream().map(RoleDTO::fromDomain).toList(),
                m.getPendingRoles().stream().map(RoleDTO::fromDomain).toList(),
                m.getOrderHistory(),
                ShoppingCartDTO.fromDomain(m.getShoppingCart()));
    }

    /* -------- DTO ➜ Domain (use with care—password not included) -------- */
    public com.example.app.DomainLayer.Member toDomain(String password) {
        // Password supplied by caller (e.g., registration flow)
        com.example.app.DomainLayer.Member m = new com.example.app.DomainLayer.Member(
                memberId, username, password, email, phoneNumber,
                address != null ? address.toDomain() : null);

        // restore collections
        m.setRoles(roles.stream().map(RoleDTO::toDomain).toList());
        m.setPendingRoles(pendingRoles.stream().map(RoleDTO::toDomain).toList());
        m.setOrderHistory(orderHistory);
        if (shoppingCart != null) m.mergeShoppingCart(shoppingCart.toDomain());
        if (suspendedUntil != null) m.setSuspended(suspendedUntil);

        return m;
    }
}