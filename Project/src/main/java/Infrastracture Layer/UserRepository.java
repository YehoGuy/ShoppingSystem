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
        return userMapping.get(id);
    }

    public User getUser(User user) {
        return userMapping.get(user.getId()); // Assuming User has a method getId()
    }

    public void addUser(User user) {
        userMapping.put(user.getId(), user);
    }

    public void removeUserById(int id) {
        userMapping.remove(id);
    }

    public void removeUserByUserObject(User user) {
        userMapping.remove(user.getId());
    }
}