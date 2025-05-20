package com.example.app.PresentationLayer.DTO.Role;


import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RoleDTO(
        @Positive int shopId,
        String roleName,
        @NotEmpty List<PermissionDTO> permissions,
        String shopName,
         String userName) {

    /*  --------------------- Domain → DTO --------------------- */
    public static RoleDTO fromDomain(com.example.app.DomainLayer.Roles.Role r) {
        // return new RoleDTO(
            // r.getShopId(),
            // r.getRoleName(),
            // r.getPermissions(),
            // r.getShopName(),
            // r.getUserName()
        // );
        //TODO: Implement the conversion from Domain to DTO
        return null;
    }
}