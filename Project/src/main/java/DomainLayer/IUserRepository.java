package DomainLayer;

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
     * Adds a new guest user to the repository.
     */
    public void addGuest();

    /**
     * Adds a new member user to the repository.
     * @param username The username of the member.
     * @param password The password of the member.
     * @param email The email address of the member.
     * @param phoneNumber The phone number of the member.
     * @param address The address of the member.
     */
    public void addMember(String username, String password, String email, String phoneNumber, String address);

    /**
     * Updates the details of an existing member.
     * @param id The unique identifier of the member to be updated.
     * @param username The new username of the member.
     * @param password The new password of the member.
     * @param email The new email address of the member.
     * @param phoneNumber The new phone number of the member.
     * @param address The new address of the member.
     */
    public void updateMember(int id, String username, String password, String email, String phoneNumber, String address);

    /**
     * Removes a user from the repository by their unique ID.
     * 
     * @param id The unique identifier of the user to be removed.
     */
    public void removeUserById(int id);

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
     * Retrieves a list of all guest users in the repository.
     * 
     * @return A list containing all guest user objects.
     */
    public List<Guest> getGuestsList();

    /**
     * Retrieves a list of all member users in the repository.
     * 
     * @return A list containing all member user objects.
     */
    public List<Member> getMembersList();

    /**
     * Clears all user data from the repository.
     */
    public void clear();
}