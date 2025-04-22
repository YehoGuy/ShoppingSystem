package ApplicationLayer.User;

import java.util.HashMap;
import java.util.List;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.LoggerService;
import ApplicationLayer.AuthTokenService;

import DomainLayer.Member;
import DomainLayer.Roles.PermissionsEnum;
import DomainLayer.Roles.Role;
import DomainLayer.User;
import DomainLayer.Roles.PermissionsEnum;
import DomainLayer.Roles.Role;
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
        try {
            LoggerService.logMethodExecution("getUserById", id);
            User user = userRepository.getUserById(id);
            LoggerService.logMethodExecutionEnd("getUserById", user);
            return user;
        } catch (Exception e) {
            LoggerService.logError("getUserById", e, id);
            throw new RuntimeException("Error fetching user with ID " + id + ": " + e.getMessage(), e);
        }
    }

    public void addMember(String username, String password, String email, String phoneNumber, String address) {
        try {
            LoggerService.logMethodExecution("addMember", username, password, email, phoneNumber, address);
            userRepository.addMember(username, password, email, phoneNumber, address);
            LoggerService.logMethodExecutionEndVoid("addMember");
        } catch (Exception e) {
            LoggerService.logError("addMember", e, username, email, phoneNumber, address);
            throw new RuntimeException("Error adding member: " + e.getMessage(), e);
        }
    }

    public void updateMemberUsername(int id, String username) {
        try {
            LoggerService.logMethodExecution("updateMemberUsername", id, username);
            validateMemberId(id);
            userRepository.updateMemberUsername(id, username);
            LoggerService.logMethodExecutionEndVoid("updateMemberUsername");
        } catch (Exception e) {
            LoggerService.logError("updateMemberUsername", e, id, username);
            throw new RuntimeException("Error updating username for user ID " + id + ": " + e.getMessage(), e);
        }
    }

    public void updateMemberPassword(int id, String password) {
        try {
            LoggerService.logMethodExecution("updateMemberPassword", id, password);
            validateMemberId(id);
            userRepository.updateMemberPassword(id, password);
            LoggerService.logMethodExecutionEndVoid("updateMemberPassword");
        } catch (Exception e) {
            LoggerService.logError("updateMemberPassword", e, id);
            throw new RuntimeException("Error updating password for user ID " + id + ": " + e.getMessage(), e);
        }
    }

    public void updateMemberEmail(int id, String email) {
        try {
            LoggerService.logMethodExecution("updateMemberEmail", id, email);
            validateMemberId(id);
            userRepository.updateMemberEmail(id, email);
            LoggerService.logMethodExecutionEndVoid("updateMemberEmail");
        } catch (Exception e) {
            LoggerService.logError("updateMemberEmail", e, id, email);
            throw new RuntimeException("Error updating email for user ID " + id + ": " + e.getMessage(), e);
        }
    }

    public void updateMemberPhoneNumber(int id, String phoneNumber) {
        try {
            LoggerService.logMethodExecution("updateMemberPhoneNumber", id, phoneNumber);
            validateMemberId(id);
            userRepository.updateMemberPhoneNumber(id, phoneNumber);
            LoggerService.logMethodExecutionEndVoid("updateMemberPhoneNumber");
        } catch (Exception e) {
            LoggerService.logError("updateMemberPhoneNumber", e, id, phoneNumber);
            throw new RuntimeException("Error updating phone number for user ID " + id + ": " + e.getMessage(), e);
        }
    }

    public void updateMemberAddress(int id, String address) {
        try {
            LoggerService.logMethodExecution("updateMemberAddress", id, address);
            validateMemberId(id);
            userRepository.updateMemberAddress(id, address);
            LoggerService.logMethodExecutionEndVoid("updateMemberAddress");
        } catch (Exception e) {
            LoggerService.logError("updateMemberAddress", e, id, address);
            throw new RuntimeException("Error updating address for user ID " + id + ": " + e.getMessage(), e);
        }
    }

    public void validateMemberId(int id) {

        if (id <= 0) {
            throw new IllegalArgumentException("Invalid user ID: " + id);
        }
        if (!userRepository.getUserMapping().containsKey(id)) {
            throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
        }
        User user = userRepository.getUserById(id);
        if (!(user instanceof Member)) {
            throw new IllegalArgumentException("User with ID " + id + " is not a member.");

        }
    }





    public String loginAsGuest() {
        try {
            LoggerService.logMethodExecution("loginAsGuest");
            int id = userRepository.addGuest(); // Assuming this method returns the ID of the new guest user
            if (id < 0) {
                throw new IllegalArgumentException("Failed to create a guest user.");
            }
            String token = authTokenService.generateAuthToken("");
            LoggerService.logMethodExecutionEnd("loginAsGuest", token);
            return token;
        } 
        catch (Exception e) {
            LoggerService.logError("loginAsGuest", e);
            throw new RuntimeException("Error logging in as guest: " + e.getMessage(), e);
        }
    }

    public String loginAsMember(String username, String password, int id_if_guest) {
        LoggerService.logMethodExecution("loginAsMember", username, password, id_if_guest);
        String token = null;
        try {
            if (username == null || password == null) {
                LoggerService.logError("loginAsMember", new IllegalArgumentException("Username and password cannot be null."));
                throw new IllegalArgumentException("Username and password cannot be null.");
            }
            if (username.isEmpty() || password.isEmpty()) {
                LoggerService.logError("loginAsMember", new IllegalArgumentException("Username and password cannot be empty."));
                throw new IllegalArgumentException("Username and password cannot be empty.");
            }
            int loginAsMember_id = userRepository.isUsernameAndPasswordValid(username, password);
            if (loginAsMember_id > 0) { // valid login attempt
                if (id_if_guest == -1) { // if the user is not a guest, it's their initial login
                    token = authTokenService.generateAuthToken(username); // Generate a token for the member
                    LoggerService.logMethodExecutionEnd("loginAsMember", loginAsMember_id);
                    return token; // Return the ID of the logged-in member    
                } else if (userRepository.isGuestById(id_if_guest)) { // ensure the given id matches a guest in the data
                    // merge the guest cart with the member cart
                    User member = userRepository.getUserById(loginAsMember_id);
                    User guest = userRepository.getUserById(id_if_guest);
                    member.mergeShoppingCart(guest.getShoppingCart());
                    // remove the guest user from the data
                    userRepository.removeUserById(id_if_guest);
                    token = authTokenService.generateAuthToken(username); // Generate a token for the member
                    LoggerService.logMethodExecutionEnd("loginAsMember", loginAsMember_id);
                    return token; 
                } else {
                    throw new IllegalArgumentException("The given id does not match a guest in the data. Probably it is a member id!");
                }
            }
        } catch (Exception e) {
            LoggerService.logError("loginAsMember", e, username, password, id_if_guest);
            throw new RuntimeException("Error logging in as member: " + e.getMessage(), e);
        }
        return null; // Indicate failure to log in as a member
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

    public String signUp(String username, String password, String email, String phoneNumber, String address) {
        try {
            if (userRepository.isUsernameTaken(username)) {
                LoggerService.logError("signUp", new IllegalArgumentException("Username is already taken."));
                throw new IllegalArgumentException("Username is already taken.");
            }
            String token = authTokenService.generateAuthToken(username); // Generate a token for the member
            LoggerService.logMethodExecution("signUp", username, password, email, phoneNumber, address);
            userRepository.addMember(username, password, email, phoneNumber, address);
            return token;
        } catch (Exception e) {
            LoggerService.logError("signUp", e, username, password, email, phoneNumber, address);
            throw new RuntimeException("Error signing up: " + e.getMessage(), e);
        }
    }

    public String logout(String token){
        try {
            LoggerService.logMethodExecution("logout", token);
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (userRepository.isGuestById(id)) {
                userRepository.removeUserById(id); // Remove guest from the repository
            }
            authTokenService.Logout(token); // Logout the user by removing the token
            LoggerService.logMethodExecutionEnd("logout", true);
            return loginAsGuest(); // Generate a new guest token
        } catch (Exception e) {
            LoggerService.logError("logout", e, token);
            return null; // Indicate failure to logout
        }
    }



    public HashMap<Integer, PermissionsEnum[]> getPermitionsByShop(int senderID, int shopId) {
        try {
            
            LoggerService.logMethodExecution("getPermitionsByShop", shopId);
            
            

            HashMap<Integer, PermissionsEnum[]> permissions = new HashMap<>();
            for (Member member : userRepository.getMembersList()) {
                if (member.getRoles() != null) {
                    for (Role role : member.getRoles()) {
                        if (role.getShopId() == shopId) {
                            permissions.put(member.getMemberId(), role.getPermissions());
                        }
                    }
                }
            }
            LoggerService.logMethodExecutionEnd("getPermitionsByShop", permissions);
            return permissions;
        } catch (Exception e) {
            LoggerService.logError("getPermitionsByShop", e, shopId);
            throw new RuntimeException("Error fetching permissions for shop ID " + shopId + ": " + e.getMessage(), e);
        }
        
    } 

    public void changePermitions(int memberId, int shopId, PermissionsEnum[] permissions) {
        try {

            LoggerService.logMethodExecution("changePermitions", memberId, shopId, permissions);
            for (Member member : userRepository.getMembersList()) {
                if (member.getMemberId() == memberId) {
                    for (Role role : member.getRoles()) {
                        if (role.getShopId() == shopId) {
                            role.setPermissions(permissions);
                            LoggerService.logMethodExecutionEndVoid("changePermitions");
                            return;
                        }
                    }
                }
            }
            LoggerService.logMethodExecutionEndVoid("changePermitions");
        } catch (Exception e) {
            LoggerService.logError("changePermitions", e, memberId, shopId, permissions);
            throw new RuntimeException("Error changing permissions for member ID " + memberId + ": " + e.getMessage(), e);
        }
    }

    public void makeManagerOfStore(int assigne, int memberId, int shopId, PermissionsEnum[] permissions) {
        try {
            if (!userRepository.isOwner(assigne, shopId)) {
                LoggerService.logError("makeManagerOfStore", new IllegalArgumentException("Member ID " + assigne + " is not an owner of shop ID " + shopId));
                throw new IllegalArgumentException("Member ID " + assigne + " is not an owner of shop ID " + shopId);  
            }
            LoggerService.logMethodExecution("makeManagerOfStore", memberId, shopId, permissions);
            for (Member member : userRepository.getMembersList()) {
                if (member.getMemberId() == memberId) {
                    Role role = new Role(assigne, shopId, permissions);
                    member.addRoleToPending(role);
                    LoggerService.logMethodExecutionEndVoid("makeManagerOfStore");
                    return;
                }
            }
            LoggerService.logMethodExecutionEndVoid("makeManagerOfStore");
        } catch (Exception e) {
            LoggerService.logError("makeManagerOfStore", e, memberId, shopId, permissions);
            throw new RuntimeException("Error making manager of store for member ID " + memberId + ": " + e.getMessage(), e);
        }   
    }

    public void removeManagerOfStore(int memberId, int shopId) {
        try {

            if (!userRepository.isOwner(memberId, shopId)) {
                LoggerService.logError("removeManagerOfStore", new IllegalArgumentException("Member ID " + memberId + " is not an owner of shop ID " + shopId));
                throw new IllegalArgumentException("Member ID " + memberId + " is not an owner of shop ID " + shopId);  
            }

            LoggerService.logMethodExecution("removeManagerOfStore", memberId, shopId);
            for (Member member : userRepository.getMembersList()) {
                if (member.getMemberId() == memberId) {
                    List<Role> roles = member.getRoles();
                    roles.removeIf(role -> role.getShopId() == shopId);
                    LoggerService.logMethodExecutionEndVoid("removeManagerOfStore");
                    
                    removeAllAssiged(memberId, shopId);
                }
            }
            

            LoggerService.logMethodExecutionEndVoid("removeManagerOfStore");
        } catch (Exception e) {
            LoggerService.logError("removeManagerOfStore", e, memberId, shopId);
            throw new RuntimeException("Error removing manager of store for member ID " + memberId + ": " + e.getMessage(), e);
        }   
    }

    private void removeAllAssiged(int assigne, int shopId) {
        for(Member m : userRepository.getMembersList()){
            for(Role r : m.getRoles()){
                if(r.getAssigneeId() == assigne && r.getShopId() == shopId){
                    m.getRoles().remove(r);
                    removeAllAssiged(m.getMemberId(), shopId);
                }
            }
        }
    }


    public void makeStoreOwner(int assigne, int memberId, int shopId){
        try {
            LoggerService.logMethodExecution("makeStoreOwner", memberId, shopId);
            if(!userRepository.isOwner(assigne, shopId)) {
                LoggerService.logError("makeStoreOwner", new IllegalArgumentException("Member ID " + assigne + " is not an owner of shop ID " + shopId));
                throw new IllegalArgumentException("Member ID " + assigne + " is not an owner of shop ID " + shopId);  
            }
            
            for (Member member : userRepository.getMembersList()) {
                if (member.getMemberId() == memberId) {
                    Role role = new Role(assigne, shopId, null);
                    role.setOwnersPermissions();
                    member.addRoleToPending(role);
                    LoggerService.logMethodExecutionEndVoid("makeStoreOwner");
                    return;
                }
            }

            throw new IllegalArgumentException("Member ID " + memberId + " not found.");
        }catch(Exception e) {
            LoggerService.logError("makeStoreOwner", e, memberId, shopId);
            throw new RuntimeException("Error making store owner for member ID " + memberId + ": " + e.getMessage(), e);
        }
    }

    public void acceptRole(int memberId, Role role) {
        try {
            LoggerService.logMethodExecution("accseptRole", memberId, role);
            for (Member member : userRepository.getMembersList()) {
                if (member.getMemberId() == memberId) {
                    member.acceptRole(role);
                    LoggerService.logMethodExecutionEndVoid("accseptRole");
                    return;
                }
            }
            throw new IllegalArgumentException("Member ID " + memberId + " not found.");
        } catch (Exception e) {
            LoggerService.logError("accseptRole", e, memberId, role);
            throw new RuntimeException("Error accepting role for member ID " + memberId + ": " + e.getMessage(), e);
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
    public boolean hasPermission(int id, PermissionsEnum permission, int shopId) {
        try {
            if (userRepository.getUserMapping().containsKey(id)) {
                User user = userRepository.getUserById(id);
                validateMemberId(id);
                return ((Member)user).hasPermission(permission,shopId); // Check if the user has the specified permission
            } else {
                throw new IllegalArgumentException("User with ID " + id + " doesn't exist.");
            }
        } catch (Exception e) {
            return false; // Indicate failure to check permission
        }
    }
    

}
