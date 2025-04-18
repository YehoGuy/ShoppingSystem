package DomainLayer.Roles;

public class Role {

    private final int assigneeId;
    private final int shopId;
    private PermissionsEnum[] permissions;

    public Role(int assigneeId, int shopId, PermissionsEnum[] permissions) {
        this.assigneeId = assigneeId;
        this.shopId = shopId;
        this.permissions = permissions;
    }

    public int getAssigneeId() {
        return assigneeId;
    }

    public int getShopId() {
        return shopId;
    }

    public PermissionsEnum[] getPermissions() {
        return permissions;
    }

    public void setPermissions(PermissionsEnum[] permissions) {
        this.permissions = permissions;
    }

    /**
     * Sets the roles for a founder in the shop.
     */
    public void setFoundersPermissions() {
        this.permissions = new PermissionsEnum[]{
                PermissionsEnum.manageItems,
                PermissionsEnum.setPolicy,
                PermissionsEnum.manageOwners,
                PermissionsEnum.leaveShopAsOwner,
                PermissionsEnum.manageManagers,
                PermissionsEnum.getStaffInfo,
                PermissionsEnum.handleMessages,
                PermissionsEnum.getHistory
        };
    }

    /**
     * Sets the roles for an owner in the shop.
     */
    public void setOwnersPermissions() {
        this.permissions = new PermissionsEnum[]{
                PermissionsEnum.manageItems,
                PermissionsEnum.setPolicy,
                PermissionsEnum.manageOwners,
                PermissionsEnum.leaveShopAsOwner,
                PermissionsEnum.manageManagers,
                PermissionsEnum.getStaffInfo,
                PermissionsEnum.handleMessages,
                PermissionsEnum.getHistory
        };
    }

    public boolean hasPermission(PermissionsEnum permission) {
        for (PermissionsEnum p : this.permissions) {
            if (p == permission) {
                return true;
            }
        }
        return false;
    }

    public void addPermission(PermissionsEnum permission) {
        for (PermissionsEnum p : this.permissions) {
            if (p == permission) {
                return; // Role already exists, no need to add it again
            }
        }
        PermissionsEnum[] newPermissions = new PermissionsEnum[this.permissions.length + 1];
        System.arraycopy(this.permissions, 0, newPermissions, 0, this.permissions.length);
        newPermissions[permissions.length] = permission;
        this.permissions = newPermissions;
    }

    public void removePermissions(PermissionsEnum permission) {
        int indexToRemove = -1;
        for (int i = 0; i < permissions.length; i++) {
            if (permissions[i] == permission) {
                indexToRemove = i;
                break;
            }
        }
        if (indexToRemove != -1) {
            PermissionsEnum[] newPermissions = new PermissionsEnum[permissions.length - 1];
            System.arraycopy(permissions, 0, newPermissions, 0, indexToRemove);
            System.arraycopy(permissions, indexToRemove + 1, newPermissions, indexToRemove, permissions.length - indexToRemove - 1);
            this.permissions = newPermissions;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Role{");
        sb.append("assigneeId=").append(assigneeId);
        sb.append(", shopId=").append(shopId);
        sb.append(", permissions=[");
        for (int i = 0; i < permissions.length; i++) {
            sb.append(permissions[i]);
            if (i < permissions.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

}