import java.util.HashMap;
import java.util.Map;
import DomainLayer.IUserRepository;
import DomainLayer.User;

// Assuming User is a class that has been defined elsewhere in your project
// and has a method getId() to retrieve the user's ID.

public class UserRepository implements IUserRepository {
    // A map to store users with their IDs as keys
    private Map<Integer, DomainLayer.User> userMapping;

    public UserRepository() {
        this.userMapping = new HashMap<>();
    }

    public User getUserById(int id) {
        if (!userMapping.containsKey(id)) {
            throw new IllegalArgumentException("User with ID " + user.getId() + " doesn't exist.");
        }
        return userMapping.get(id);
    }

    public User getUser(User user) {
        if (!userMapping.containsKey(user.getId())) {
            throw new IllegalArgumentException("User with ID " + user.getId() + " doesn't exist.");
        }
        return userMapping.get(user.getId()); // Assuming User has a method getId()
    }

    public void addUser(User user) {
        if (userMapping.containsKey(user.getId())) {
            throw new IllegalArgumentException("User with ID " + user.getId() + " already exists.");
        }
        userMapping.put(user.getId(), user);
    }

    public void updateUser(User user) {
        if (!userMapping.containsKey(user.getId())) {
            throw new IllegalArgumentException("User with ID " + user.getId() + " doesn't exist.");
        }
        removeUserByUserObject(user);
        userMapping.put(user.getId(), user);
    }


    public void removeUserById(int id) {
        if (!userMapping.containsKey(id)) {
            throw new IllegalArgumentException("User with ID " + user.getId() + " doesn't exist.");
        }
        userMapping.remove(id);
    }

    public void removeUserByUserObject(User user) {
        if (!userMapping.containsKey(user.getId())) {
            throw new IllegalArgumentException("User with ID " + user.getId() + " doesn't exist.");
        }
        userMapping.remove(user.getId());
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

    public void clear() {
        userMapping.clear();
    }




}