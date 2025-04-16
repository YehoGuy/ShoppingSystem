package main.java.DomainLayer;

import java.util.List;
import java.util.Map;

/**
 * Interface for managing user data in a repository.
 * Provides methods for adding, updating, removing, and retrieving users.
 */
public interface IUserRepository {

    /**
     * Retrieves a user by their unique ID.
     * 
     * @param id The unique identifier of the user.
     * @return The user associated with the given ID, or null if not found.
     */
    public User getUserById(int id);

    /**
     * Retrieves a user by their user object.
     * 
     * @param user The user object to search for.
     * @return The user matching the given user object, or null if not found.
     */
    public User getUser(User user);

    /**
     * Adds a new user to the repository.
     * 
     * @param user The user to be added.
     */
    public void addUser(User user);

    /**
     * Updates the details of an existing user in the repository.
     * 
     * @param user The user with updated information.
     */
    public void updateUser(User user);

    /**
     * Removes a user from the repository by their unique ID.
     * 
     * @param id The unique identifier of the user to be removed.
     */
    public void removeUserById(int id);

    /**
     * Removes a user from the repository using their user object.
     * 
     * @param user The user object to be removed.
     */
    public void removeUserByUserObject(User user);

    /**
     * Retrieves a mapping of user IDs to user objects.
     * 
     * @return A map where the key is the user ID and the value is the user object.
     */
    public Map<Integer, User> getUserMapping();

    /**
     * Retrieves a list of all users in the repository.
     * 
     * @return A list containing all user objects.
     */
    public List<User> getUsersList();

    /**
     * Retrieves a list of all user IDs in the repository.
     * 
     * @return A list containing all user IDs.
     */
    public List<Integer> getUsersIdsList();

    /**
     * Clears all user data from the repository.
     */
    public void clear();
}