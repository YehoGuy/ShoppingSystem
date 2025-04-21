package ApplicationLayer.User;
import ApplicationLayer.AuthTokenService;
import DomainLayer.Member;
import DomainLayer.Roles.PermissionsEnum;
import DomainLayer.Roles.Role;
import DomainLayer.User;
import InfrastructureLayer.UserRepository;

public class UserService {
    private final UserRepository userRepository;
    private AuthTokenService authTokenService;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setServices(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    public User getUserById(int id) {
        return userRepository.getUserById(id);
    }
    
    public void addMember(String username, String password, String email, String phoneNumber, String address) {
        userRepository.addMember(username, password, email, phoneNumber, address);
    }
    
    public void updateMemberUsername(int id, String username) {
        validateMemberId(id);
        userRepository.updateMemberUsername(id, username);
    }

    public void updateMemberPassword(int id, String password) {
        validateMemberId(id);
        userRepository.updateMemberPassword(id, password);
    }

    public void updateMemberEmail(int id, String email) {
        validateMemberId(id);
        userRepository.updateMemberEmail(id, email);
    }

    public void updateMemberPhoneNumber(int id, String phoneNumber) {
        validateMemberId(id);
        userRepository.updateMemberPhoneNumber(id, phoneNumber);
    }

    public void updateMemberAddress(int id, String address) {
        validateMemberId(id);
        userRepository.updateMemberAddress(id, address);
    }

    public void validateMemberId(int id) {
        if (!userRepository.getUserMapping().containsKey(id)) {
            throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
        }
        User user = userRepository.getUserById(id);
        if (!(user instanceof Member)) {
            throw new IllegalArgumentException("User with ID " + id + " is not a member.");
        }
    }





    public int loginAsGuest() {
        try {
            int id = userRepository.addGuest(); // Assuming this method returns the ID of the new guest user
            if (id < 0) {
                throw new IllegalArgumentException("Failed to create a guest user.");
            }
            return id;
        } 
        catch (Exception e) {
            return -1; // Indicate failure to create a guest user
        }
    }

    public int loginAsMember(String username, String password, int id_if_guest) {
        try {
            if (username == null || password == null) {
                throw new IllegalArgumentException("Username and password cannot be null.");
            }
            if (username.isEmpty() || password.isEmpty()) {
                throw new IllegalArgumentException("Username and password cannot be empty.");
            }
            int loginAsMember_id = userRepository.isUsernameAndPasswordValid(username, password);
            if (loginAsMember_id > 0) { // valid login attempt
                if (id_if_guest == -1) { // if the user is not a guest, it's their initial login
                    return loginAsMember_id; // Return the ID of the logged-in member    
                } else if (userRepository.isGuestById(id_if_guest)) { // ensure the given id matches a guest in the data
                    // merge the guest cart with the member cart
                    User member = userRepository.getUserById(loginAsMember_id);
                    User guest = userRepository.getUserById(id_if_guest);
                    member.mergeShoppingCart(guest.getShoppingCart());
                    // remove the guest user from the data
                    userRepository.removeUserById(id_if_guest);
                    // Return the ID of the logged-in member
                    return loginAsMember_id; 
                } else {
                    throw new IllegalArgumentException("The given id does not match a guest in the data. Probably it is a member id!");
                }
            }
        } catch (Exception e) {
            return -1; // Indicate failure to log in as a member
        }
        return -1; // Default return value for unhandled cases
    }
    
   /*  
    public int loginAsMember(String username, String password, int id_if_guest) {
        try {
            if (username == null || password == null) {
                throw new IllegalArgumentException("Username and password cannot be null.");
            }
            if (username.isEmpty() || password.isEmpty()) {
                throw new IllegalArgumentException("Username and password cannot be empty.");
            }
            int loginAsMember_id = userRepository.isUsernameAndPasswordValid(username, password);
            if (loginAsMember_id>0)//valid login attempt 
            {
                if (id_if_guest == -1) { //if the user is not a guest,its his initial logging in and we just return the id of the member
                    return loginAsMember_id; // Return the ID of the logged-in member    
                }
                else if (userRepository.isGuestById(id_if_guest)){ //we ensure that the given id matches a guest in the data!
                    //we merge the guest cart with the member cart
                    User member = userRepository.getUserById(loginAsMember_id);
                    User guest = userRepository.getUserById(id_if_guest);
                    member.mergeShoppingCart(guest.getShoppingCart());
                    //we remove the guest user from the data
                    userRepository.removeUserById(id_if_guest);
                    // Return the ID of the logged-in member
                    return loginAsMember_id; 

                }
                else {
                    throw new IllegalArgumentException("The given id does not match a guest in the data. probably it is a member id!");
                }
                
            }

            
        } 
        catch (Exception e) {
            return -1; // Indicate failure to log in as a member
        }
    }

    */

    public boolean signUp(String username, String password, String email, String phoneNumber, String address) {
        try {
            if (userRepository.isUsernameTaken(username)) {
                throw new IllegalArgumentException("Username is already taken.");
            }
            userRepository.addMember(username, password, email, phoneNumber, address);
            return true;
        } catch (Exception e) {
            return false; // Indicate failure to sign up
        }
    }

    public boolean logout(int id){
        try {
            if (userRepository.isGuestById(id)) {
                userRepository.removeUserById(id); // Remove guest from the repository
            }
            return true; // Logout successful
        } catch (Exception e) {
            return false; // Indicate failure to log out
        }
    }

    public boolean addRole(int id, Role role) {
        try {
            if (userRepository.getUserMapping().containsKey(id)) {
                User user = userRepository.getUserById(id);
                validateMemberId(id);
                ((Member)user).addRole(role);
                return true; // Role added successfully
            } else {
                throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
            }
        } catch (Exception e) {
            return false; // Indicate failure to add role
        }
    }

    public boolean removeRole(int id, Role role) {
        try {
            if (userRepository.getUserMapping().containsKey(id)) {
                User user = userRepository.getUserById(id);
                validateMemberId(id);
                ((Member)user).removeRole(role);
                return true; // Role removed successfully
            } else {
                throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
            }
        } catch (Exception e) {
            return false; // Indicate failure to remove role
        }
    }

    public boolean hasRole(int id, Role role) {
        try {
            if (userRepository.getUserMapping().containsKey(id)) {
                User user = userRepository.getUserById(id);
                validateMemberId(id);
                return ((Member)user).hasRole(role); // Check if the user has the specified role
            } else {
                throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
            }
        } catch (Exception e) {
            return false; // Indicate failure to check role
        }
    }

    public boolean addPermission(int id, PermissionsEnum permission) {
        try {
            if (userRepository.getUserMapping().containsKey(id)) {
                User user = userRepository.getUserById(id);
                validateMemberId(id);
                ((Member)user).addPermission(permission); // Add permission to the user
                return true; // Permission added successfully
            } else {
                throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
            }
        } catch (Exception e) {
            return false; // Indicate failure to add permission
        }
    }
    public boolean removePermission(int id, PermissionsEnum permission) {
        try {
            if (userRepository.getUserMapping().containsKey(id)) {
                User user = userRepository.getUserById(id);
                validateMemberId(id);
                ((Member)user).removePermission(permission); // Remove permission from the user
                return true; // Permission removed successfully
            } else {
                throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
            }
        } catch (Exception e) {
            return false; // Indicate failure to remove permission
        }
    }
    public boolean hasPermission(int id, PermissionsEnum permission) {
        try {
            if (userRepository.getUserMapping().containsKey(id)) {
                User user = userRepository.getUserById(id);
                validateMemberId(id);
                return ((Member)user).hasPermission(permission); // Check if the user has the specified permission
            } else {
                throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
            }
        } catch (Exception e) {
            return false; // Indicate failure to check permission
        }
    }
    
}
