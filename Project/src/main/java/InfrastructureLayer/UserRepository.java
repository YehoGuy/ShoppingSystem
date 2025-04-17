package InfrastructureLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import DomainLayer.Guest;
import DomainLayer.IUserRepository;
import DomainLayer.User;
import DomainLayer.Member;

// Assuming User is a class that has been defined elsewhere in your project
// and has a method getId() to retrieve the user's ID.

public class UserRepository implements IUserRepository {
    // A map to store users with their IDs as keys
    private Map<Integer, DomainLayer.User> userMapping;
    AtomicInteger userIdCounter;

    public UserRepository() {
        this.userMapping = new HashMap<>();
        this.userIdCounter = new AtomicInteger(0); // Initialize the user ID counter
    }

    public User getUserById(int id) {
        if (!userMapping.containsKey(id)) {
            throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
        }
        return userMapping.get(id);
    }

    public void addGuest() {
        int id = userIdCounter.incrementAndGet(); // Generate a new ID for the guest
        Guest guest = new Guest(id); // Assuming Guest is a subclass of User
        userMapping.put(id, guest); // Add the guest to the mapping
    }

    public void addMember(String username, String password, String email, String phoneNumber, String address) {
        int id = userIdCounter.incrementAndGet(); // Generate a new ID for the member
        User member = new Member(id, username, password, email, phoneNumber, address); // Assuming User has a constructor with these parameters
        userMapping.put(id, member); // Add the member to the mapping
    }

    public void updateMember(int id, String username, String password, String email, String phoneNumber, String address) {
        if (!userMapping.containsKey(id)) {
            throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
        }
        removeUserById(id);
        User user = new Member(id, username, password, email, phoneNumber, address); // Assuming User has a constructor with these parameters
        userMapping.put(id, user);
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

}