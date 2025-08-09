package DTOs;

import java.time.LocalDateTime;
import java.util.List;

public class MemberDTO {

    private int memberId;
    private String username;
    private String email;
    private String phoneNumber;
    private LocalDateTime suspendedUntil;
    private AddressDTO address;
    private List<rolesDTO> roles;
    private List<rolesDTO> pendingRoles;
    private List<Integer> orderHistory;
    private ShoppingCartDTO shoppingCart;

    public MemberDTO() {}

    public MemberDTO(int memberId, String username, String email, String phoneNumber,
                     LocalDateTime suspendedUntil, AddressDTO address,
                     List<rolesDTO> roles, List<rolesDTO> pendingRoles,
                     List<Integer> orderHistory, ShoppingCartDTO shoppingCart) {
        this.memberId = memberId;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.suspendedUntil = suspendedUntil;
        this.address = address;
        this.roles = roles;
        this.pendingRoles = pendingRoles;
        this.orderHistory = orderHistory;
        this.shoppingCart = shoppingCart;
    }
    

public int getMemberId() {
        return memberId;
}

public void setMemberId(int memberId) {
        this.memberId = memberId;
}

public String getUsername() {
        return username;
}

public void setUsername(String username) {
        this.username = username;
}

public String getEmail() {
        return email;
}

public void setEmail(String email) {
        this.email = email;
}

public String getPhoneNumber() {
        return phoneNumber;
}

public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
}

public LocalDateTime getSuspendedUntil() {
        return suspendedUntil;
}

public void setSuspendedUntil(LocalDateTime suspendedUntil) {
        this.suspendedUntil = suspendedUntil;
}

public AddressDTO getAddress() {
        return address;
}

public void setAddress(AddressDTO address) {
        this.address = address;
}

public List<rolesDTO> getRoles() {
        return roles;
}

public void setRoles(List<rolesDTO> roles) {
        this.roles = roles;
}

public List<rolesDTO> getPendingRoles() {
        return pendingRoles;
}

public void setPendingRoles(List<rolesDTO> pendingRoles) {
        this.pendingRoles = pendingRoles;
}

public List<Integer> getOrderHistory() {
        return orderHistory;
}

public void setOrderHistory(List<Integer> orderHistory) {
        this.orderHistory = orderHistory;
}

public ShoppingCartDTO getShoppingCart() {
        return shoppingCart;
}

public void setShoppingCart(ShoppingCartDTO shoppingCart) {
        this.shoppingCart = shoppingCart;
}
    
}
