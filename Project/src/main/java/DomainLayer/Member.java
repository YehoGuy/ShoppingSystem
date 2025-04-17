package DomainLayer;

import java.util.ArrayList;
import java.util.List;

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

    public void addRole(Role role) {
        roles.add(role); // Add a role to the list of roles
    }

    public void removeRole(Role role) {
        roles.remove(role); // Remove a role from the list of roles
    }

    public boolean hasRole(Role role) {
        return roles.contains(role); // Check if the user has a specific role
    }
}
