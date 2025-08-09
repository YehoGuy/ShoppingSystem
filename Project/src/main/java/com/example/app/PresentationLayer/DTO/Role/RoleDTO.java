package com.example.app.PresentationLayer.DTO.Role;


import java.util.List;

public class RoleDTO {
    private int shopId; // The ID of the role.
    private String roleName; // The name of the role.
    private List<String> permissions; // A list of permissions associated with the role.
    private String userName; // The name of the user associated with the role.

    public RoleDTO(int shopId, String roleName, List<String> permissions, String userName) {
        this.shopId = shopId;
        this.roleName = roleName;
        this.permissions = permissions;
        this.userName = userName;
    }

    public int getShopId() {
        return shopId;
    }
    
    public String getRoleName() {
        return roleName;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public String getUserName() {
        return userName;
    }

    
    

}