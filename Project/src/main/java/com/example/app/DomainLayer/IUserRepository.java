package com.example.app.DomainLayer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.InfrastructureLayer.PasswordEncoderUtil;

public interface IUserRepository {

    // Basic user retrieval
    User getUserById(int id);

    Map<Integer, User> getUserMapping();

    Member getMemberById(int id);

    // manager actions
    boolean isAdmin(Integer id);

    void addAdmin(Integer id) throws RuntimeException;

    void removeAdmin(Integer id) throws RuntimeException;

    List<Integer> getAllAdmins();

    // Guest management
    int addGuest();

    boolean isGuestById(int id);

    // Member management
    void addMember(String username,
            String password,
            String email,
            String phoneNumber,
            String address);

    void updateMemberUsername(int id, String username);

    void updateMemberPassword(int id, String password);

    void updateMemberEmail(int id, String email);

    void updateMemberPhoneNumber(int id, String phoneNumber);

    void updateMemberAddress(int id, String city, String street, int apartmentNum, String postalCode);

    // Credentials & existence checks
    int isUsernameAndPasswordValid(String username, String password);

    boolean isUsernameTaken(String username);

    // Removal / cleanup
    void removeUserById(int id);

    void clear();

    // Listing
    List<User> getUsersList();

    List<Integer> getUsersIdsList();

    List<Guest> getGuestsList();

    List<Member> getMembersList();

    // Role checks
    boolean isOwner(int memberId, int shopId);

    boolean isFounder(int memberId, int shopId);

    // Role operations
    void setPermissions(int userId, int shopId, Role role, PermissionsEnum[] permissions);

    void addRoleToPending(int userId, Role role);

    Role getRole(int memberId, int shopId);

    void removeRole(int memberId, int shopId);

    Role getPendingRole(int memberId, int shopId);

    void acceptRole(int id, Role role);

    void declineRole(int id, Role role);

    void addPermission(int id, PermissionsEnum permission, int shopId);

    void removePermission(int id, PermissionsEnum permission, int shopId);

    List<Integer> getShopIdsByWorkerId(int userId);

    List<Member> getShopMembers(int shopId);

    // Shopping cart operations
    ShoppingCart getShoppingCartById(int userId);

    void addItemToShoppingCart(int userId, int shopId, int itemId, int quantity);

    void removeItemFromShoppingCart(int userId, int shopId, int itemId);

    void updateItemQuantityInShoppingCart(int userId, int shopId, int itemId, int quantity);

    void clearShoppingCart(int userId);

    Map<Integer, Integer> getBasket(int userId, int shopId);

    void createBasket(int userId, int shopId);

    void setPaymentMethod(int userId, int shopId, PaymentMethod paymentMethod);

    void pay(int userId, int shopId, double payment);

    void refund(int userId, int shopId, double refund);

    // Password encoding
    PasswordEncoderUtil passwordEncoderUtil = new PasswordEncoderUtil(); // Use the password encoder utility

    void setEncoderToTest(boolean isTest); // Set the encoder to test mode

    void addNotification(int userId, String title, String message);

    List<Notification> getNotificationsAndClear(int userId);

    public List<Member> getOwners(int shopId);

    void setSuspended(int userId, LocalDateTime suspended); // Set a user as suspended

    boolean isSuspended(int userId); // Check if a user is suspended

    List<Integer> getSuspendedUsers(); // Get a list of suspended users

    List<Role> getPendingRoles(int userId);

    List<Member> getAllMembers();
}
