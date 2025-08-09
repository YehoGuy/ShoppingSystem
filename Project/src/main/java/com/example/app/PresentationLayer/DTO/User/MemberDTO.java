package com.example.app.PresentationLayer.DTO.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.PresentationLayer.DTO.Purchase.AddressDTO;
import com.example.app.PresentationLayer.DTO.Role.RoleDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

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
    public static MemberDTO fromDomain(com.example.app.DomainLayer.Member m, ShoppingCartDTO shoppingCart) {
        List<RoleDTO> roles = new ArrayList<>();
        for (com.example.app.DomainLayer.Roles.Role role : m.getRoles()) {
            List<String> permissions = Arrays.stream(role.getPermissions())
                        .map(PermissionsEnum::name)
                        .toList();
                String rolename = "manager";
                for(PermissionsEnum permission : role.getPermissions()){
                    if(permission == PermissionsEnum.manageOwners){
                        rolename = "founder";
                        break;
                    }
                    if(permission == PermissionsEnum.manageManagers && rolename != "founder"){
                        rolename = "owner";
                        break;
                    }
                }
            RoleDTO roleDTO = new RoleDTO(role.getShopId(), rolename, permissions, m.getUsername());
            roles.add(roleDTO);
        }
        List<RoleDTO> pendingRoles = new ArrayList<>();
        for (com.example.app.DomainLayer.Roles.Role role : m.getPendingRoles()) {
            List<String> permissions = Arrays.stream(role.getPermissions())
                        .map(PermissionsEnum::name)
                        .toList();
            String rolename = "manager";
            for(PermissionsEnum permission : role.getPermissions()){
                if(permission == PermissionsEnum.manageOwners){
                    rolename = "founder";
                    break;
                }
                if(permission == PermissionsEnum.manageManagers && rolename != "founder"){
                    rolename = "owner";
                    break;
                }
            }
            RoleDTO roleDTO = new RoleDTO(role.getShopId(), rolename, permissions, m.getUsername());
            pendingRoles.add(roleDTO);
        }

        return new MemberDTO(
                m.getMemberId(),
                m.getUsername(),
                m.getEmail(),
                m.getPhoneNumber(),
                m.getSuspended(),
                m.getAddress() != null ? AddressDTO.fromDomain(m.getAddress()) : null,
                roles,
                pendingRoles,
                m.getOrderHistory(),
                shoppingCart
        );
    }

    /* -------- DTO ➜ Domain (use with care—password not included) -------- */
    public com.example.app.DomainLayer.Member toDomain(String password) {
        // Password supplied by caller (e.g., registration flow)
        com.example.app.DomainLayer.Member m = new com.example.app.DomainLayer.Member(
                memberId, username, password, email, phoneNumber,
                address != null ? address.toDomain() : null);

        // restore collections
        List<Role> roles = new ArrayList<>();
        for (RoleDTO role : this.roles) {
            Role r = new Role(memberId, role.getShopId(), role.getPermissions()
                    .stream()
                    .map(PermissionsEnum::valueOf)
                    .toArray(PermissionsEnum[]::new));
            roles.add(r);
        }
        List<Role> pendingRoles = new ArrayList<>();
        for (RoleDTO role : this.pendingRoles) {
            Role r = new Role(memberId, role.getShopId(), role.getPermissions()
                    .stream()
                    .map(PermissionsEnum::valueOf)
                    .toArray(PermissionsEnum[]::new));
            pendingRoles.add(r);
        }

        m.setRoles(roles);
        m.setPendingRoles(pendingRoles);
        m.setOrderHistory(orderHistory);
        if (shoppingCart != null) m.mergeShoppingCart(shoppingCart.toDomain());
        if (suspendedUntil != null) m.setSuspended(suspendedUntil);

        return m;
    }
}