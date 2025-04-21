package DomainLayer;

import java.util.ArrayList;
import java.util.List;

import DomainLayer.Roles.PermissionsEnum;
import DomainLayer.Roles.Role;

public class Member extends User {
    final private int memberId;
    private List<Integer> orderHistory;// List of order IDs
    private String username; // Username of the user
    private String password; // Password of the user
    private String email; // Email address of the user
    private String phoneNumber; // Phone number of the user
    private String address; // Address of the user
    private List<Role> roles; // List of roles associated with the user
    private List<Role> pending_roles; // List of pending roles not yet confirmed/declined by the user
    
    public Member(int memberId, String username, String password, String email, String phoneNumber, String address) {
        super(memberId); // Call the User class constructor
        this.memberId = memberId; // Initialize member ID
        this.username = username; // Initialize username
        this.password = password; // Initialize password
        this.email = email; // Initialize email address
        this.phoneNumber = phoneNumber; // Initialize phone number
        this.address = address; // Initialize address
        this.orderHistory = new ArrayList<>(); // Initialize order history
        this.roles = new ArrayList<>(); // Initialize roles
        this.pending_roles = new ArrayList<>(); // Initialize pending roles

    }

    public int getMemberId() {
        return memberId; // Return the member ID
    }

    public String getUsername() {
        return username; // Return the username
    }

    public String getPassword() {
        return password; // Return the password
    }

    public String getEmail() {
        return email; // Return the email address
    }

    public String getPhoneNumber() {
        return phoneNumber; // Return the phone number
    }

    public String getAddress() {
        return address; // Return the address
    }

    public void setUsername(String username) {
        this.username = username; // Set the username
    }

    public void setPassword(String password) {
        this.password = password; // Set the password
    }

    public void setEmail(String email) {
        this.email = email; // Set the email address
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber; // Set the phone number
    }

    public void setAddress(String address) {
        this.address = address; // Set the address
    }

    public List<Integer> getOrderHistory() {
        return orderHistory; // Return the order history
    }

    public void addOrderToHistory(int orderId) {
        orderHistory.add(orderId); // Add an order ID to the order history
    }

    public List<Role> getRoles() {
        return roles; // Return the list of roles
    }

    public void addRoleToPending(Role role) {
        pending_roles.add(role); // Add a role to the list of roles
    }

    public void addRole(Role role) {
        roles.add(role); // Add a role to the list of roles
    }

    public void removeRole(Role role) {
        roles.remove(role); // Remove a role from the list of roles
    }

    public boolean hasRole(Role role) {
        return roles.contains(role); // Check if the user has a specific role
    }


    public boolean equals(Object obj) {
        if (this == obj) return true; // Check if the same object
        if (obj == null || getClass() != obj.getClass()) return false; // Check for null or different class
        Member member = (Member) obj; // Cast to Member
        return memberId == member.memberId; // Compare member IDs
    }

    public void acceptRole(Role role) {
        if (pending_roles.contains(role)) {
            pending_roles.remove(role); // Remove the role from pending roles
            roles.add(role); // Add the role to the list of roles
        } else {
            throw new IllegalArgumentException("Role not found in pending roles."); // Role not found in pending roles
        }
    }
    public void declineRole(Role role) {
        if (pending_roles.contains(role)) {
            pending_roles.remove(role); // Remove the role from pending roles
        } else {
            throw new IllegalArgumentException("Role not found in pending roles."); // Role not found in pending roles
        }
    }
    public void addPermission(PermissionsEnum permission) {
        // Add a permission to the user's roles (if applicable)
        for (Role role : roles) {
            role.addPermission(permission); // Add the permission to the role
        }
    }
    public void removePermission(PermissionsEnum permission) {
        // Remove a permission from the user's roles (if applicable)
        for (Role role : roles) {
            role.removePermissions(permission); // Remove the permission from the role
        }
    }
    public boolean hasPermission(PermissionsEnum permission, int shopdId) {
        // Check if the user has a specific permission through their roles
        for (Role role : roles) {
            if (role.getShopId()==shopdId && role.hasPermission(permission)) {
                return true; // User has the permission
            }
        }
        return false; // User does not have the permission

    }
}
