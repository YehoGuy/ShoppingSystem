package PresentationLayer.DTO.Role;

/**
 
Thin wrapper so we never expose the domain enum directly
across the network.  Its value is always the enum name.**/
public record PermissionDTO(String name) {

    /* Domain → DTO */
    public static PermissionDTO fromDomain(DomainLayer.Roles.PermissionsEnum p) {
        return new PermissionDTO(p.name());
    }

    /* DTO → Domain */
    public DomainLayer.Roles.PermissionsEnum toDomain() {
        return DomainLayer.Roles.PermissionsEnum.valueOf(name);
    }
}