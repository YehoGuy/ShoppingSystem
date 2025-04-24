package InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import DomainLayer.Guest;
import DomainLayer.IUserRepository;
import DomainLayer.Member;
import DomainLayer.ShoppingCart;
import DomainLayer.User;

// Assuming User is a class that has been defined elsewhere in your project
// and has a method getId() to retrieve the user's ID.

public class UserRepository implements IUserRepository {
    // A map to store users with their IDs as keys
    private Map<Integer, DomainLayer.User> userMapping;
    private List<Integer> managers;
    AtomicInteger userIdCounter;

    public UserRepository() {
        this.userMapping = new ConcurrentHashMap<>();
        this.userIdCounter = new AtomicInteger(0); // Initialize the user ID counter
        this.managers = new CopyOnWriteArrayList<>(); // Initialize the managers list
        addMember("admin","admin","admin@mail.com", "0","admin st.");
        managers.add(isUsernameAndPasswordValid("admin", "admin"));
    }

    public User getUserById(int id) {
        if (!userMapping.containsKey(id)) {
            throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
        }
        return userMapping.get(id);
    }

    public boolean isAdmin(Integer id){
        return managers.contains(id);
    }

    public void addAdmin(Integer id) throws RuntimeException{
        if(managers.contains(id))
            throw new RuntimeException("All ready a manager");

        managers.add(id);
    }

    public void removeAdmin(Integer id) throws RuntimeException{
        if(id == isUsernameAndPasswordValid("admin", "admin"))
            throw new RuntimeException("cant remove admin from the user who created the system");

        managers.remove(id);
    }

    public List<Integer> getAllAdmins()
    {
        return managers;
    }

    public int addGuest() {
        int id = userIdCounter.incrementAndGet(); // Generate a new ID for the guest
        Guest guest = new Guest(id); // Assuming Guest is a subclass of User
        userMapping.put(id, guest); // Add the guest to the mapping
        if(!userMapping.containsKey(id) || userMapping.get(id) == null) {
            return -1;
        }
        return id; // Return the ID of the newly created guest
    }

    public void addMember(String username, String password, String email, String phoneNumber, String address) {
        if(!email.contains("@") || email.isEmpty()){
            throw new IllegalArgumentException("Invalid email address.");
        }

        int id = userIdCounter.incrementAndGet(); // Generate a new ID for the member
        User member = new Member(id, username, password, email, phoneNumber, address); // Assuming User has a constructor with these parameters
        userMapping.put(id, member); // Add the member to the mapping
    }

    public void updateMemberUsername(int id, String username) {
        if (!userMapping.containsKey(id)) {
            throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        if (user instanceof Member) {
            ((Member) user).setUsername(username);
        } else {
            throw new IllegalArgumentException("User with ID " + id + " is not a Member.");
        }
    }

    public void updateMemberPassword(int id, String password) {
        if (!userMapping.containsKey(id)) {
            throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        if (user instanceof Member) {
            ((Member) user).setPassword(password);
        } else {
            throw new IllegalArgumentException("User with ID " + id + " is not a Member.");
        }
    }

    public void updateMemberEmail(int id, String email) {
        if (!userMapping.containsKey(id)) {
            throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        if (user instanceof Member) {
            ((Member) user).setEmail(email);
        } else {
            throw new IllegalArgumentException("User with ID " + id + " is not a Member.");
        }
    }

    public void updateMemberPhoneNumber(int id, String phoneNumber) {
        if (!userMapping.containsKey(id)) {
            throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        if (user instanceof Member) {
            ((Member) user).setPhoneNumber(phoneNumber);
        } else {
            throw new IllegalArgumentException("User with ID " + id + " is not a Member.");
        }
    }

    public void updateMemberAddress(int id, String address) {
        if (!userMapping.containsKey(id)) {
            throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        if (user instanceof Member) {
            ((Member) user).setAddress(address);
        } else {
            throw new IllegalArgumentException("User with ID " + id + " is not a Member.");
        }
    }
    
    public int isUsernameAndPasswordValid(String username, String password) {
        for (User user : userMapping.values()) {
            if (user instanceof Member) {
                Member member = (Member) user;
                if (member.getUsername().equals(username) && member.getPassword().equals(password)) {
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
            throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
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
            throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
        }
        User user = userMapping.get(id);
        return user.getShoppingCart();
    }

    public void addItemToShoppingCart(int userId, int shopId, int itemId, int quantity) {
        if (!userMapping.containsKey(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " doesn't exist.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
        User user = userMapping.get(userId);
        ShoppingCart shoppingCart = user.getShoppingCart();
        shoppingCart.addItem(shopId, itemId, quantity); 
    }

    public void removeItemFromShoppingCart(int userId, int shopId, int itemId) {
        if (!userMapping.containsKey(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " doesn't exist.");
        }
        User user = userMapping.get(userId);
        ShoppingCart shoppingCart = user.getShoppingCart();
        shoppingCart.removeItem(shopId, itemId); 
    }

    public void updateItemQuantityInShoppingCart(int userId, int shopId, int itemId, int quantity) {
        if (!userMapping.containsKey(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " doesn't exist.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
        User user = userMapping.get(userId);
        ShoppingCart shoppingCart = user.getShoppingCart();
        shoppingCart.removeItem(shopId, shopId);
        shoppingCart.addItem(shopId, itemId, quantity); 
    }

    public void clearShoppingCart(int userId) {
        if (!userMapping.containsKey(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " doesn't exist.");
        }
        User user = userMapping.get(userId);
        ShoppingCart shoppingCart = user.getShoppingCart();
        shoppingCart.clearCart(); 
    }

    public Map<Integer, Integer> getBasket(int userId, int shopId) {
        if (!userMapping.containsKey(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " doesn't exist.");
        }
        User user = userMapping.get(userId);
        ShoppingCart shoppingCart = user.getShoppingCart();
        return shoppingCart.getBasket(shopId); 
    }

    public void createBasket(int userId, int shopId) {
        if (!userMapping.containsKey(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " doesn't exist.");
        }
        User user = userMapping.get(userId);
        ShoppingCart shoppingCart = user.getShoppingCart();
        shoppingCart.addBasket(shopId); 
    }

    
    

}