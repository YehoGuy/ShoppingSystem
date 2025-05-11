package DTOs;

import java.util.List;

public class rolesDTO {
    private String roleName; // The name of the role.
    private List<String> permissions; // A list of permissions associated with the role.
    private String shopName; // The name of the shop associated with the role.
    private String userName; // The name of the user associated with the role.

    public rolesDTO(String roleName, List<String> permissions, String shopName, String userName) {
        this.roleName = roleName;
        this.permissions = permissions;
        this.shopName = shopName;
        this.userName = userName;
    }

    public String getRoleName() {
        return roleName;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public String getShopName() {
        return shopName;
    }

    public String getUserName() {
        return userName;
    }


}
