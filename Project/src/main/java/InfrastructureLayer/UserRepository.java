package InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import ApplicationLayer.OurRuntime;
import ApplicationLayer.Purchase.PaymentMethod;
import DomainLayer.Guest;
import DomainLayer.IUserRepository;
import DomainLayer.Member;
import DomainLayer.Roles.PermissionsEnum;
import DomainLayer.Roles.Role;
import DomainLayer.ShoppingCart;
import DomainLayer.User;

// Assuming User is a class that has been defined elsewhere in your project
// and has a method getId() to retrieve the user's ID.

public class UserRepository implements IUserRepository {
    // A map to store users with their IDs as keys
    private ConcurrentHashMap<Integer, DomainLayer.User> userMapping;
    private List<Integer> managers;
    AtomicInteger userIdCounter;

    public UserRepository() {
        this.userMapping = new ConcurrentHashMap<>();
        this.userIdCounter = new AtomicInteger(0); // Initialize the user ID counter
        this.managers = new CopyOnWriteArrayList<>(); // Initialize the managers list
        addMember("admin","admin","admin@mail.com", "0","admin st.");
        managers.add(isUsernameAndPasswordValid("admin", "admin"));
    }

    public void setEncoderToTest(boolean b) {
        passwordEncoderUtil.setIsTest(b); // Set the encoder to test mode
    }

    public User getUserById(int id) {
        if (!userMapping.containsKey(id)) {
            throw new OurRuntime("User with ID " + id + " doesn't exist.");
        }
        return userMapping.get(id);
    }

    public Member getMemberById(int id) {
        if (!userMapping.containsKey(id)) {
            throw new OurRuntime("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        if (user instanceof Member) {
            return (Member) user;
        } else {
            throw new OurRuntime("User with ID " + id + " is not a Member.");
        }
    }

    public boolean isAdmin(Integer id){
        return managers.contains(id);
    }

    public void addAdmin(Integer id) throws RuntimeException{
        if(managers.contains(id))
            throw new OurRuntime("All ready an admin");
        managers.add(id);
    }

    public void removeAdmin(Integer id) throws RuntimeException{
        if(id == isUsernameAndPasswordValid("admin", "admin"))
            throw new OurRuntime("cant remove admin from the user who created the system");
        managers.remove(id);
    }

    public List<Integer> getAllAdmins() {
        return managers;
    }

    public int addGuest() {
        int id = userIdCounter.incrementAndGet(); // Generate a new ID for the guest
        Guest guest = new Guest(id); // Assuming Guest is a subclass of User
        userMapping.put(id, guest); // Add the guest to the mapping
        if(!userMapping.containsKey(id) || userMapping.get(id) == null) {
            throw new IllegalArgumentException("Failed to create guest with ID " + id);
        }
        return id; // Return the ID of the newly created guest
    }

    public void addMember(String username, String password, String email, String phoneNumber, String address) {
        if(!email.contains("@") || email.isEmpty()){
            throw new OurRuntime("Invalid email address.");
        }

        int id = userIdCounter.incrementAndGet(); // Generate a new ID for the member
        User member = new Member(id, username, password, email, phoneNumber, address); // Assuming User has a constructor with these parameters
        userMapping.put(id, member); // Add the member to the mapping
    }

    public void updateMemberUsername(int id, String username) {
        if (!userMapping.containsKey(id)) {
            throw new OurRuntime("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        if (user instanceof Member) {
            ((Member) user).setUsername(username);
        } else {
            throw new OurRuntime("User with ID " + id + " is not a Member.");
        }
    }

    public void updateMemberPassword(int id, String password) {
        if (!userMapping.containsKey(id)) {
            throw new OurRuntime("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        if (user instanceof Member) {
            ((Member) user).setPassword(password);
        } else {
            throw new OurRuntime("User with ID " + id + " is not a Member.");
        }
    }

    public void updateMemberEmail(int id, String email) {
        if (!userMapping.containsKey(id)) {
            throw new OurRuntime("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        if (user instanceof Member) {
            ((Member) user).setEmail(email);
        } else {
            throw new OurRuntime("User with ID " + id + " is not a Member.");
        }
    }

    public void updateMemberPhoneNumber(int id, String phoneNumber) {
        if (!userMapping.containsKey(id)) {
            throw new OurRuntime("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        if (user instanceof Member) {
            ((Member) user).setPhoneNumber(phoneNumber);
        } else {
            throw new OurRuntime("User with ID " + id + " is not a Member.");
        }
    }

    public void updateMemberAddress(int id, String address) {
        if (!userMapping.containsKey(id)) {
            throw new OurRuntime("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        if (user instanceof Member) {
            ((Member) user).setAddress(address);
        } else {
            throw new OurRuntime("User with ID " + id + " is not a Member.");
        }
    }
    
    public int isUsernameAndPasswordValid(String username, String password) {
        for (User user : userMapping.values()) {
            if (user instanceof Member) {
                Member member = (Member) user;
                if (member.getUsername().equals(username) && passwordEncoderUtil.matches(password, member.getPassword())) {
                    return member.getMemberId(); // Return the ID of the member if username and password match
                }
            }
        }
        return -1; // Return -1 if no match is found
    }
    
    public boolean isUsernameTaken (String username) {
        for (User user : userMapping.values()) {
            if (user instanceof Member) {
                Member member = (Member) user;
                if (member.getUsername().equals(username)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isGuestById(int id) {
        if (!userMapping.containsKey(id)) {
            return false;
        }
        User user = userMapping.get(id);
        return user instanceof Guest;
    }

    public void removeUserById(int id) {
        if (!userMapping.containsKey(id)) {
            throw new OurRuntime("User with ID " + id + " doesn't exist.");
        }
        userMapping.remove(id);
    }

    public Map<Integer, User> getUserMapping() {
        return userMapping;
    }

    public List<User> getUsersList() {
        return new ArrayList<>(userMapping.values());
    }

    public List<Integer> getUsersIdsList() {
        return new ArrayList<>(userMapping.keySet());
    }

    public List<Guest> getGuestsList() {
        List<Guest> guestUsers = new ArrayList<>();
        for (User user : userMapping.values()) {
            if (user instanceof Guest) {
                guestUsers.add((Guest) user);
            }
        }
        return guestUsers;
    }

    @Override
    public List<Member> getMembersList() {
            List<Member> memberUsers = new ArrayList<>();
            for (User user : userMapping.values()) {
                if (user instanceof Member) {
                    memberUsers.add((Member) user);
                }
            }
            return memberUsers;
        }

    public void clear() {
        userMapping.clear();
    }


    public boolean isOwner(int id, int shopId) {
        if(getMembersList().stream().anyMatch(m -> m.getMemberId() == id &&
             m.getRoles().stream().anyMatch(r -> r.isOwner() && r.getShopId() == shopId))) {
                return true;
        }
        return false;
    }

    public boolean isFounder(int id, int shopId) {
        if(getMembersList().stream().anyMatch(m -> m.getMemberId() == id &&
             m.getRoles().stream().anyMatch(r -> r.isFounder() && r.getShopId() == shopId))) {
                return true; 
        }
        return false;
    }

    /**
     * Retrieves the shopping cart associated with a user by their unique ID.
     * 
     * @param id The unique identifier of the user.
     * @return The shopping cart associated with the user.
     */
    public ShoppingCart getShoppingCartById(int id) {
        if (!userMapping.containsKey(id)) {
            throw new OurRuntime("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        return user.getShoppingCart();
    }

    public void addItemToShoppingCart(int userId, int shopId, int itemId, int quantity) {
        if (!userMapping.containsKey(userId)) {
            throw new OurRuntime("User with ID " + userId + " doesn't exist.");
        }
        if (quantity <= 0) {
            throw new OurRuntime("Quantity must be greater than 0.");
        }
        User user = userMapping.get(userId);
        ShoppingCart shoppingCart = user.getShoppingCart();
        shoppingCart.addItem(shopId, itemId, quantity); 
    }

    public void removeItemFromShoppingCart(int userId, int shopId, int itemId) {
        if (!userMapping.containsKey(userId)) {
            throw new OurRuntime("User with ID " + userId + " doesn't exist.");
        }
        User user = userMapping.get(userId);
        ShoppingCart shoppingCart = user.getShoppingCart();
        shoppingCart.removeItem(shopId, itemId); 
    }

    public void updateItemQuantityInShoppingCart(int userId, int shopId, int itemId, int quantity) {
        if (!userMapping.containsKey(userId)) {
            throw new OurRuntime("User with ID " + userId + " doesn't exist.");
        }
        if (quantity <= 0) {
            throw new OurRuntime("Quantity must be greater than 0.");
        }
        User user = userMapping.get(userId);
        ShoppingCart shoppingCart = user.getShoppingCart();
        shoppingCart.removeItem(shopId, shopId);
        shoppingCart.addItem(shopId, itemId, quantity); 
    }

    public void clearShoppingCart(int userId) {
        if (!userMapping.containsKey(userId)) {
            throw new OurRuntime("User with ID " + userId + " doesn't exist.");
        }
        User user = userMapping.get(userId);
        ShoppingCart shoppingCart = user.getShoppingCart();
        shoppingCart.clearCart(); 
    }

    public Map<Integer, Integer> getBasket(int userId, int shopId) {
        if (!userMapping.containsKey(userId)) {
            throw new OurRuntime("User with ID " + userId + " doesn't exist.");
        }
        User user = userMapping.get(userId);
        ShoppingCart shoppingCart = user.getShoppingCart();
        return shoppingCart.getBasket(shopId); 
    }

    public void createBasket(int userId, int shopId) {
        if (!userMapping.containsKey(userId)) {
            throw new OurRuntime("User with ID " + userId + " doesn't exist.");
        }
        User user = userMapping.get(userId);
        ShoppingCart shoppingCart = user.getShoppingCart();
        shoppingCart.addBasket(shopId); 
    }

    public void setPermissions(int userId, int shopId, Role role, PermissionsEnum[] permissions) {
        Member member = getMemberById(userId);
        if (member == null) {
            throw new OurRuntime("User with ID " + userId + " doesn't exist.");
        }
        if (role == null) {
            throw new OurRuntime("Role cannot be null.");
        }
        if (permissions == null || permissions.length == 0) {
            throw new OurRuntime("Permissions cannot be null or empty.");
        }
        if (!member.getRoles().contains(role)) {
            throw new OurRuntime("User with ID " + userId + " does not have the specified role.");
        }
        role.setPermissions(permissions); // Assuming Role has a method to set permissions
    }

    public void addRoleToPending(int userId, Role role) {
        Member member = getMemberById(userId);
        if (member == null) {
            throw new OurRuntime("User with ID " + userId + " doesn't exist.");
        }
        if (role == null) {
            throw new OurRuntime("Role cannot be null.");
        }
        for (Role existingRole : member.getRoles()) {
            if (existingRole.getShopId() == role.getShopId()) {
                throw new OurRuntime("User with ID " + userId + " already has a role for this shop.");
            }
        }
        for (Role pendingRole : member.getPendingRoles())
            if (pendingRole.getShopId() == role.getShopId()) 
                    throw new OurRuntime("User with ID " + userId + " already has this role pending.");
        member.addRoleToPending(role); // Assuming Member has a method to add a role
    }

    public Role getRole(int memberId, int shopId) {
        Member member = getMemberById(memberId);
        if (member == null) {
            throw new OurRuntime("User with ID " + memberId + " doesn't exist.");
        }
        List<Role> roles = member.getRoles();
        for (Role role : roles) {
            if (role.getShopId() == shopId) {
                return role;
            }
        }
        throw new OurRuntime("User with ID " + memberId + " does not have a role for this shop.");
    }

    public Role getPendingRole(int memberId, int shopId) {
        Member member = getMemberById(memberId);
        if (member == null) {
            throw new OurRuntime("User with ID " + memberId + " doesn't exist.");
        }
        List<Role> pendingRoles = member.getPendingRoles();
        for (Role role : pendingRoles) {
            if (role.getShopId() == shopId) {
                return role;
            }
        }
        throw new OurRuntime("User with ID " + memberId + " does not have a pending role for this shop.");
    }

    public void removeRole(int memberId, int shopId) {
        Member member = getMemberById(memberId);
        if (member == null) {
            throw new OurRuntime("User with ID " + memberId + " doesn't exist.");
        }
        List<Role> roles = member.getRoles();
        for (Role role : roles) {
            if (role.getShopId() == shopId) {
                member.removeRole(role); // Assuming Member has a method to remove a role
                return;
            }
        }
        throw new OurRuntime("User with ID " + memberId + " does not have a role for this shop.");
    }

    public void acceptRole(int id, Role role) {
        if (role == null) {
            throw new OurRuntime("Role cannot be null.");
        }
        Member member = getMemberById(id);
        if (member == null) {
            throw new OurRuntime("User with ID " + id + " doesn't exist.");
        }
        member.acceptRole(role); // Assuming Member has a method to accept a role
    }

    public void addPermission(int userId, PermissionsEnum permission, int shopId) {
        Member member = getMemberById(userId);
        if (member == null) {
            throw new OurRuntime("User with ID " + userId + " doesn't exist.");
        }
        Role role = getRole(userId, shopId);
        if (role == null) {
            throw new OurRuntime("User with ID " + userId + " does not have a role for this shop.");
        }
        role.addPermission(permission); // Assuming Role has a method to add a permission
    }

    public void removePermission(int userId, PermissionsEnum permission, int shopId) {
        Member member = getMemberById(userId);
        if (member == null) {
            throw new OurRuntime("User with ID " + userId + " doesn't exist.");
        }
        Role role = getRole(userId, shopId);
        if (role == null) {
            throw new OurRuntime("User with ID " + userId + " does not have a role for this shop.");
        }
        role.removePermissions(permission); // Assuming Role has a method to remove a permission
    }

    public void setPaymentMethod(int userId, int shopId, PaymentMethod paymentMethod) {
        User user = userMapping.get(userId);
        if (user == null) {
            throw new OurRuntime("User with ID " + userId + " doesn't exist.");
        }
        user.setPaymentMethod(paymentMethod);
    }

    public void pay(int userId, int shopId, double payment) {
        User user = userMapping.get(userId);
        if (user == null) {
            throw new OurRuntime("User with ID " + userId + " doesn't exist.");
        }
        PaymentMethod paymentMethod = user.getPaymentMethod();
        if (paymentMethod == null) {
            throw new OurRuntime("Payment method not set for user with ID " + userId);
        }
        try {
            paymentMethod.processPayment(payment, shopId); // Assuming PaymentMethod has a method to process payment
        } catch (Exception e) {
            throw new OurRuntime("Payment failed: " + e.getMessage());
        }
    }
}