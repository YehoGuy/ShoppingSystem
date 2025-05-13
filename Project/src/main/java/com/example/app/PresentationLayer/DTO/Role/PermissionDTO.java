package com.example.app.PresentationLayer.DTO.Role;

/**
 
Thin wrapper so we never expose the domain enum directly
across the network.  Its value is always the enum name.**/
public record PermissionDTO(String name) {

    /* Domain → DTO */
    public static PermissionDTO fromDomain(com.example.app.DomainLayer.Roles.PermissionsEnum p) {
        return new PermissionDTO(p.name());
    }

    /* DTO → Domain */
    public com.example.app.DomainLayer.Roles.PermissionsEnum toDomain() {
        return com.example.app.DomainLayer.Roles.PermissionsEnum.valueOf(name);
    }
}