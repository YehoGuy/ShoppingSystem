package DomainLayer;

import java.util.List;
import java.util.Map;

import InfrastructureLayer.PasswordEncoderUtil;

public interface IUserRepository {

    // Basic user retrieval
    User getUserById(int id);
    Map<Integer, User> getUserMapping();       

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
    void updateMemberAddress(int id, String address);

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

    // Shopping cart operations
    ShoppingCart getShoppingCartById(int userId);
    void addItemToShoppingCart(int userId, int shopId, int itemId, int quantity);
    void removeItemFromShoppingCart(int userId, int shopId, int itemId);
    void updateItemQuantityInShoppingCart(int userId, int shopId, int itemId, int quantity);
    void clearShoppingCart(int userId);
    Map<Integer, Integer> getBasket(int userId, int shopId);
    void createBasket(int userId, int shopId);

    // Password encoding
    PasswordEncoderUtil passwordEncoderUtil = new PasswordEncoderUtil(); // Use the password encoder utility
    void setEncoderToTest(boolean isTest); // Set the encoder to test mode
}
