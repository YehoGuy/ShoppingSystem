package PresentationLayer.DTO.Role;


import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RoleDTO(
        @Positive int assigneeId,
        @Positive int shopId,
        @NotEmpty List<PermissionDTO> permissions) {

    /*  --------------------- Domain → DTO --------------------- */
    public static RoleDTO fromDomain(DomainLayer.Roles.Role r) {
        return new RoleDTO(
                r.getAssigneeId(),
                r.getShopId(),
                // map enum array → DTO list
                java.util.Arrays.stream(r.getPermissions())
                                .map(PermissionDTO::fromDomain)
                                .toList());
    }

    /*  --------------------- DTO → Domain --------------------- */
    public DomainLayer.Roles.Role toDomain() {
        DomainLayer.Roles.PermissionsEnum[] perms =
                permissions.stream()
                           .map(PermissionDTO::toDomain)
                           .toArray(DomainLayer.Roles.PermissionsEnum[]::new);

        return new DomainLayer.Roles.Role(assigneeId, shopId, perms);
    }
}