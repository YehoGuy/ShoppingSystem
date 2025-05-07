package DTOs;

public class RoleDTO {
    
    private String roleName; // Name of the role (e.g., "Admin", "User")
    private String description; // Description of the role

    public RoleDTO(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }
}
