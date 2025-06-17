package com.example.app.DomainLayer.Roles;

import java.util.Arrays;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

@Embeddable
public class Role {

    private final int assigneeId;
    private final int shopId;
    private PermissionsEnum[] permissions;
    @Transient
    private final Object lock = new Object(); // lock object for synchronization

    /**
     * Constructor for Role class.
     *
     * @param assigneeId  The ID of the user who gave the role.
     * @param shopId      The ID of the shop to which the role is assigned.
     * @param permissions  The permissions associated with the role.
     */
    public Role(int assigneeId, int shopId, PermissionsEnum[] permissions) {
        this.assigneeId = assigneeId;
        this.shopId = shopId;
        if (permissions == null) {
            this.permissions = new PermissionsEnum[0]; // Initialize with empty permissions if null
        }
        else {
            this.permissions = Arrays.copyOf(permissions, permissions.length);
        }
    }

    public Role() {
        this.assigneeId = -1; // Default value indicating no assignee
        this.shopId = -1; // Default value indicating no shop
        this.permissions = new PermissionsEnum[0]; // Default empty permissions
    }

    public int getAssigneeId() {
        return assigneeId;
    }

    public int getShopId() {
        return shopId;
    }

    public PermissionsEnum[] getPermissions() {
        synchronized (lock) {
            return Arrays.copyOf(permissions, permissions.length);
        }
    }

    public void setPermissions(PermissionsEnum[] permissions) {
        synchronized (lock) {
            this.permissions = Arrays.copyOf(permissions, permissions.length);
        }
    }

    public void setFoundersPermissions() {
        synchronized (lock) {
            this.permissions = new PermissionsEnum[]{
                    PermissionsEnum.manageItems,
                    PermissionsEnum.setPolicy,
                    PermissionsEnum.manageOwners,
                    PermissionsEnum.leaveShopAsOwner,
                    PermissionsEnum.manageManagers,
                    PermissionsEnum.getStaffInfo,
                    PermissionsEnum.handleMessages,
                    PermissionsEnum.getHistory,
                    PermissionsEnum.closeShop,
                    PermissionsEnum.openClosedShop,
                    PermissionsEnum.suspension
            };
        }
    }

    public void setOwnersPermissions() {
        synchronized (lock) {
            this.permissions = new PermissionsEnum[]{
                PermissionsEnum.manageItems,
                PermissionsEnum.setPolicy,
                PermissionsEnum.manageOwners,
                PermissionsEnum.leaveShopAsOwner,
                PermissionsEnum.manageManagers,
                PermissionsEnum.getStaffInfo,
                PermissionsEnum.handleMessages,
                PermissionsEnum.getHistory,
                PermissionsEnum.suspension
            };
        }
    }

    public boolean hasPermission(PermissionsEnum permission) {
        synchronized (lock) {
            for (PermissionsEnum p : this.permissions) {
                if (p == permission) {
                    return true;
                }
            }
            return false;
        }
    }

    public void addPermission(PermissionsEnum permission) {
        synchronized (lock) {
            for (PermissionsEnum p : this.permissions) {
                if (p == permission) {
                    return; // already exists
                }
            }
            PermissionsEnum[] newPermissions = Arrays.copyOf(this.permissions, this.permissions.length + 1);
            newPermissions[this.permissions.length] = permission;
            this.permissions = newPermissions;
        }
    }

    public void removePermissions(PermissionsEnum permission) {
        synchronized (lock) {
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
    }

    @Override
    public String toString() {
        synchronized (lock) {
            StringBuilder sb = new StringBuilder("Role{");
            sb.append("assigneeId=").append(assigneeId);
            sb.append(", shopId=").append(shopId);
            sb.append(", permissions=").append(Arrays.toString(permissions));
            sb.append("}");
            return sb.toString();
        }
    }

    public String toNotification(){
        synchronized (lock) {
            StringBuilder sb = new StringBuilder("Role{");
            sb.append("Permissions=").append(Arrays.toString(permissions));
            sb.append("}");
            return sb.toString();
        }
    }

    public boolean isOwner() {
        return hasPermission(PermissionsEnum.manageOwners);
    }    public boolean isFounder() {
        return hasPermission(PermissionsEnum.closeShop);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Role role = (Role) obj;
        
        if (assigneeId != role.assigneeId) return false;
        if (shopId != role.shopId) return false;
        
        synchronized (lock) {
            synchronized (role.lock) {
                return Arrays.equals(permissions, role.permissions);
            }
        }
    }

    @Override
    public int hashCode() {
        synchronized (lock) {
            int result = assigneeId;
            result = 31 * result + shopId;
            result = 31 * result + Arrays.hashCode(permissions);
            return result;
        }
    }

}
