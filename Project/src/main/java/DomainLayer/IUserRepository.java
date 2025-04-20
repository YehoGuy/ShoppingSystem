package DomainLayer;
import java.util.List;
import java.util.Map;

/**
 * Interface for managing user data in a repository.
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
    public int addGuest();

    /**
     * Adds a new member user to the repository.
     * 
     * @param username The username of the member.
     * @param password The password of the member.
     * @param email The email address of the member.
     * @param phoneNumber The phone number of the member.
     * @param address The address of the member.
     */
    public void addMember(String username, String password, String email, String phoneNumber, String address);

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

    /**
     * Updates the phone number of a member user by their unique ID.
     * 
     * @param id The unique identifier of the member.
     * @param phoneNumber The new phone number to be updated.
     */
    public void updateMemberPhoneNumber(int id, String phoneNumber);

    /**
     * Updates the address of a member user by their unique ID.
     * 
     * @param id The unique identifier of the member.
     * @param address The new address to be updated.
     */
    public void updateMemberAddress(int id, String address);

    /**
     * Validates if the provided username and password match a member in the repository.
     * 
     * @param username The username to validate.
     * @param password The password to validate.
     * @return The unique ID of the member if valid, or -1 if invalid.
     */
    public int isUsernameAndPasswordValid(String username, String password);

    /**
     * Checks if a username is already taken by a member in the repository.
     * 
     * @param username The username to check.
     * @return True if the username is taken, false otherwise.
     */
    public boolean isUsernameTaken(String username);

    /**
     * Checks if a user with the given ID is a guest.
     * 
     * @param id The unique identifier of the user.
     * @return True if the user is a guest, false otherwise.
     */
    public boolean isGuestById(int id);
}