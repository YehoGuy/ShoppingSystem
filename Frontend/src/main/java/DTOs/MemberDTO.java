package DTOs;

import java.util.List;


public class MemberDTO {
    
    private int memberId; // Unique identifier for the member
    private String username; // Username of the member
    private String password; // Password of the member
    private String email; // Email address of the member
    private String phoneNumber; // Phone number of the member
    private List<rolesDTO> roles; // List of roles associated with the user
    private List<Integer> orderHistory;// List of order IDs
    private List<rolesDTO> pending_roles; // List of pending roles not yet confirmed/declined by the user

    public MemberDTO(int memberId, String username, String password, String email, String phoneNumber, List<rolesDTO> roles,
            List<Integer> orderHistory, List<rolesDTO> pending_roles) {
        this.memberId = memberId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        
        this.roles = roles;
        this.orderHistory = orderHistory;
        this.pending_roles = pending_roles;


    }

    public int getMemberId() {
        return memberId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public List<rolesDTO> getRoles() {
        return roles;
    }

    public List<Integer> getOrderHistory() {
        return orderHistory;
    }

    public List<rolesDTO> getPendingRoles() {
        return pending_roles;
    }
}
