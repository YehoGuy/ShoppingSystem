package ApplicationLayer.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ApplicationLayer.AuthTokenService;
import ApplicationLayer.LoggerService;
import ApplicationLayer.OurArg;
import ApplicationLayer.OurRuntime;
import ApplicationLayer.Purchase.PaymentMethod;
import DomainLayer.IUserRepository;
import DomainLayer.Member;
import DomainLayer.Roles.PermissionsEnum;
import DomainLayer.Roles.Role;
import DomainLayer.User;
import InfrastructureLayer.PasswordEncoderUtil;

public class UserService {
    
    private final IUserRepository userRepository;

    private AuthTokenService authTokenService;

    private PasswordEncoderUtil passwordEncoder;

    public UserService(IUserRepository userRepository) {
        this.userRepository = userRepository;
        passwordEncoder = userRepository.passwordEncoderUtil;
    }

    public boolean isAdmin(Integer id)
    {
        return userRepository.isAdmin(id);
    }

    public void setEncoderToTest(boolean isTest) {
        passwordEncoder.setIsTest(isTest); // Set the encoder to test mode
    }

    public void makeAdmin(String token, Integer id)
    {
        // userId is the token's user id. and id is the id of the user to make admin
        LoggerService.logMethodExecution("makeAdmin", token, id);
        try {
            int userId = authTokenService.ValidateToken(token);
            if(isAdmin(userId)){
                if(id >= 0)
                    userRepository.addAdmin(id);
                else
                    throw new OurArg("the id of the user to make admin is illegal");
            }
            else 
                throw new RuntimeException("only admins can make admins");
            LoggerService.logMethodExecutionEndVoid("makeAdmin");
        } catch (Exception e) {
            LoggerService.logError("makeAdmin", e, token, id);
            throw new RuntimeException("Error making the user " + id + " an admin: " + e);
        }
    }

    public void removeAdmin(String token, Integer id)
    {
        LoggerService.logMethodExecution("removeAdmin", token, id);
        try {
            int userId = authTokenService.ValidateToken(token);
            if(isAdmin(userId)){
                if(id >= 0)
                    userRepository.removeAdmin(id);
                else
                    throw new OurArg("the id of the user to make admin is illegal");
            }
            else 
                throw new RuntimeException("only admins can remove admins");
            LoggerService.logMethodExecutionEndVoid("removeAdmin");
        } catch (Exception e) {
            LoggerService.logError("removeAdmin", e, token, id);
            throw new RuntimeException("Error removing the user " + id + " from being an admin: " + e);
        }
    }

    public List<Integer> getAllAdmins(String token)
    {
        LoggerService.logMethodExecution("getAllAdmins", token);
        try {
            List<Integer> lst;
            int userId = authTokenService.ValidateToken(token);
            if(isAdmin(userId)){
                lst = userRepository.getAllAdmins();
            }
            else 
                throw new RuntimeException("only admins can get ids of all admins");
            LoggerService.logMethodExecutionEndVoid("getAllAdmins");
            return lst;
        } catch (Exception e) {
            LoggerService.logError("getAllAdmins", e, token);
            throw new RuntimeException("Error getting ids of all admins: " + e);
        }
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
            isValidDetails(username, password, email, phoneNumber); // Validate the input details
            if(userRepository.isUsernameAndPasswordValid(username, password) != -1) {
                throw new OurArg("Username is already taken.");
            }
            password = passwordEncoder.encode(password); // Encode the password using the PasswordEncoderUtil
            LoggerService.logMethodExecution("addMember", username, password, email, phoneNumber, address);
            userRepository.addMember(username, password, email, phoneNumber, address);
            LoggerService.logMethodExecutionEndVoid("addMember");
        } catch (Exception e) {
            LoggerService.logError("addMember", e, username, email, phoneNumber, address);
            throw new RuntimeException("Error adding member: " + e.getMessage(), e);
        }
        
    }

    public void updateMemberUsername(String token, String username) {
        try {
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            LoggerService.logMethodExecution("updateMemberUsername", id, username);
            validateMemberId(id);
            isValidUsername(username); // Validate the username
            userRepository.updateMemberUsername(id, username);
            LoggerService.logMethodExecutionEndVoid("updateMemberUsername");
        } catch (Exception e) {
            LoggerService.logError("updateMemberUsername", e, username);
            throw new RuntimeException("Error updating username for token" + token + ": " + e.getMessage(), e);
        }
    }

    public void updateMemberPassword(String token, String password) {
        try {
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            isValidPassword(password); // Validate the password
            password = passwordEncoder.encode(password); // Encode the password using the PasswordEncoderUtil
            LoggerService.logMethodExecution("updateMemberPassword", id, password);
            validateMemberId(id);
            userRepository.updateMemberPassword(id, password);
            LoggerService.logMethodExecutionEndVoid("updateMemberPassword");
        } catch (Exception e) {
            LoggerService.logError("updateMemberPassword", e, password);
            throw new RuntimeException("Error updating password for for token" + token + ": " + e.getMessage(), e);
        }
    }

    public void updateMemberEmail(String token, String email) {
        try {
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            LoggerService.logMethodExecution("updateMemberEmail", id, email);
            validateMemberId(id);
            isValidEmail(email); // Validate the email
            userRepository.updateMemberEmail(id, email);
            LoggerService.logMethodExecutionEndVoid("updateMemberEmail");
        } catch (Exception e) {
            LoggerService.logError("updateMemberEmail", e, email);
            throw new RuntimeException("Error updating email for token" + token + ": " + e.getMessage(), e);
        }
    }

    public void updateMemberPhoneNumber(String token, String phoneNumber) {
        try {
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            LoggerService.logMethodExecution("updateMemberPhoneNumber", id, phoneNumber);
            validateMemberId(id);
            isValidPhoneNumber(phoneNumber); // Validate the phone number
            userRepository.updateMemberPhoneNumber(id, phoneNumber);
            LoggerService.logMethodExecutionEndVoid("updateMemberPhoneNumber");
        } catch (Exception e) {
            LoggerService.logError("updateMemberPhoneNumber", e, phoneNumber);
            throw new RuntimeException("Error updating phone number for token" + token + ": " + e.getMessage(), e);
        }
    }

    public void updateMemberAddress(String token, String address) {
        try {
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            LoggerService.logMethodExecution("updateMemberAddress", id, address);
            validateMemberId(id);
            userRepository.updateMemberAddress(id, address);
            LoggerService.logMethodExecutionEndVoid("updateMemberAddress");
        } catch (Exception e) {
            LoggerService.logError("updateMemberAddress", e, address);
            throw new RuntimeException("Error updating address for token" + token + ": " + e.getMessage(), e);
        }
    }

    public void validateMemberId(int id) {

        if (id <= 0) {
            throw new OurArg("Invalid user ID: " + id);
        }
        if (!userRepository.getUserMapping().containsKey(id)) {
            throw new OurArg("User with ID " + id + " doesn't exist.");
        }
        User user = userRepository.getUserById(id);
        if (!(user instanceof Member)) {
            throw new OurArg("User with ID " + id + " is not a member.");

        }
    }





    public String loginAsGuest() {
        try {
            LoggerService.logMethodExecution("loginAsGuest");
            int id = userRepository.addGuest(); // Assuming this method returns the ID of the new guest user
            if (id < 0) {
                throw new OurArg("Failed to create a guest user.");
            }
            String token = authTokenService.AuthenticateGuest(id);
            LoggerService.logMethodExecutionEnd("loginAsGuest", token);
            return token;
        } 
        catch (Exception e) {
            LoggerService.logError("loginAsGuest", e);
            throw new RuntimeException("Error logging in as guest: " + e.getMessage(), e);
        }
    }

    public String loginAsMember(String username, String password, String token_if_guest) {
        password = passwordEncoder.encode(password); // Encode the password using the PasswordEncoderUtil
        LoggerService.logMethodExecution("loginAsMember", username, password, token_if_guest);
        String token = null;
        try {
            if (username == null || password == null) {
                LoggerService.logError("loginAsMember", new OurArg("Username and password cannot be null."));
                throw new OurArg("Username and password cannot be null.");
            }
            if (username.isEmpty() || password.isEmpty()) {
                LoggerService.logError("loginAsMember", new OurArg("Username and password cannot be empty."));
                throw new OurArg("Username and password cannot be empty.");
            }
            int loginAsMember_id = userRepository.isUsernameAndPasswordValid(username, password);
            if (loginAsMember_id > 0) { // valid login attempt
                if (token_if_guest == "") { // if the user is not a guest, it's their initial login
                    token = authTokenService.Login(username,password,loginAsMember_id); // Generate a token for the member
                    LoggerService.logMethodExecutionEnd("loginAsMember", loginAsMember_id);
                    return token; // Return the ID of the logged-in member    
                } 
                else{ 
                    int id = authTokenService.ValidateToken(token_if_guest); // guest id
                    // merge the guest cart with the member cart
                    User member = userRepository.getUserById(loginAsMember_id);
                    User guest = userRepository.getUserById(id);
                    member.mergeShoppingCart(guest.getShoppingCart());
                    // remove the guest user from the data
                    userRepository.removeUserById(id);
                    token = authTokenService.Login(username, password, loginAsMember_id); // Generate a token for the member
                    LoggerService.logMethodExecutionEnd("loginAsMember", loginAsMember_id);
                    return token; 
                }
            }else{
                LoggerService.logError("loginAsMember", new OurArg("Invalid username or password."));
                throw new OurArg("Invalid username or password.");
            }
        } catch (Exception e) {
            LoggerService.logError("loginAsMember", e, username, password, token_if_guest);
            throw new RuntimeException("Error logging in as member: " + e.getMessage(), e);
        }
    }
    
   /*  
    public int loginAsMember(String username, String password, int id_if_guest) {
        try {
            if (username == null || password == null) {
                throw new OurArg("Username and password cannot be null.");
            }
            if (username.isEmpty() || password.isEmpty()) {
                throw new OurArg("Username and password cannot be empty.");
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
                    throw new OurArg("The given id does not match a guest in the data. probably it is a member id!");
                }
                
            }

            
        } 
        catch (Exception e) {
            return -1; // Indicate failure to log in as a member
        }
    }

    */

    // public String signUp(String username, String password, String email, String phoneNumber, String address) {
    //     password = passwordEncoder.encode(password); // Encode the password using the PasswordEncoderUtil
    //     try {
    //         if (userRepository.isUsernameTaken(username)) {
    //             LoggerService.logError("signUp", new OurArg("Username is already taken."));
    //             throw new OurArg("Username is already taken.");
    //         }
    //         if (!email.contains("@")) {
    //             throw new OurArg("Invalid email format.");
    //         }
    //         String token = authTokenService.generateAuthToken(username); // Generate a token for the member
    //         LoggerService.logMethodExecution("signUp", username, password, email, phoneNumber, address);
    //         userRepository.addMember(username, password, email, phoneNumber, address);
    //         return token;
    //     } catch (Exception e) {
    //         LoggerService.logError("signUp", e, username, password, email, phoneNumber, address);
    //         throw new RuntimeException("Error signing up: " + e.getMessage(), e);
    //     }
    // }

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



    public HashMap<Integer, PermissionsEnum[]> getPermitionsByShop(String token, int shopId) {
        try {
            
            LoggerService.logMethodExecution("getPermitionsByShop", shopId);
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (!userRepository.isOwner(id, shopId)) {
                throw new OurArg("Member ID " + token + " is not an owner of shop ID " + shopId);  
            }
            
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

    /**
     * change memberId's permissions in shopId to the given permissions.
     * only the member that gave him the role can change his permissions.
     * the token is the token of the member that gave him the role.
     * 
     * @param token
     * @param memberId
     * @param shopId
     * @param permissions
     * 
     * * @throws IllegalArgumentException
     * * @throws OurRuntime
     */
    public void changePermissions(String token, int memberId, int shopId, PermissionsEnum[] permissions) {
        try {
            LoggerService.logMethodExecution("changePermissions", token, memberId, shopId, permissions);
            int assigneeId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (!userRepository.isOwner(assigneeId, shopId)) {
                LoggerService.logDebug("changePermissions", new OurRuntime("Member ID " + assigneeId + " is not an owner of shop ID " + shopId));
                throw new OurRuntime("Member ID " + assigneeId + " is not an owner of shop ID " + shopId);  
            }
            Member member = userRepository.getMemberById(memberId);
            for (Role role : member.getRoles()) {
                if (role.getShopId() == shopId) {
                    if (role.getAssigneeId() != assigneeId) {
                        LoggerService.logDebug("changePermissions", new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + memberId + " in shop ID " + shopId));
                        throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + memberId + " in shop ID " + shopId);  
                    }
                    for(PermissionsEnum permission : permissions) {
                        if (permission == null) {
                            LoggerService.logDebug("changePermissions", new OurRuntime("Permission cannot be null."));
                            throw new OurRuntime("Permission cannot be null.");
                        }
                        if (permission == PermissionsEnum.closeShop) {
                            LoggerService.logDebug("changePermissions", new OurRuntime("Permission closeShop cannot be changed."));
                            throw new OurRuntime("Permission closeShop cannot be changed.");
                        }
                    }
                    userRepository.setPermissions(memberId, shopId, role, permissions);
                    LoggerService.logMethodExecutionEndVoid("changePermissions");
                    return;
                }
            }
            LoggerService.logMethodExecutionEndVoid("changePermissions");
        } catch (OurRuntime e) {
            LoggerService.logDebug("changePermissions", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("changePermissions", e, memberId, shopId, permissions);
            throw new RuntimeException("Error changing permissions for member ID " + memberId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Assigns a member as a manager of a store with the specified permissions.
     * @param token The token of the user making the assignment.
     * @param memberId The ID of the member to be assigned as a manager.
     * @param shopId The ID of the store where the member will be assigned as a manager.
     * @param permissions The permissions to be granted to the manager.
     * 
     * * @throws IllegalArgumentException
     * * @throws OurRuntime
     */
    public void makeManagerOfStore(String token, int memberId, int shopId, PermissionsEnum[] permissions) {
        try {
            LoggerService.logMethodExecution("makeManagerOfStore", token, shopId, permissions);
            int assignee = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (!userRepository.isOwner(assignee, shopId)) {
                LoggerService.logDebug("makeManagerOfStore", new OurRuntime("Member ID " + assignee + " is not an owner of shop ID " + shopId));
                throw new OurRuntime("Member ID " + assignee + " is not an owner of shop ID " + shopId);  
            }
            Role role = new Role(assignee, shopId, permissions);
            userRepository.addRoleToPending(memberId, role);
            LoggerService.logMethodExecutionEndVoid("makeManagerOfStore");
        } catch (OurRuntime e) {
            LoggerService.logDebug("makeManagerOfStore", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("makeManagerOfStore", e, token, shopId, permissions);
            throw new RuntimeException("Error making manager of store for member ID " + token + ": " + e.getMessage(), e);
        }   
    }

    /**
     * Removes a manager from a store.
     * @param token The token of the user making the removal.
     * @param managerId The ID of the manager to be removed.
     * @param shopId The ID of the store from which the manager will be removed.
     * 
     * * @throws IllegalArgumentException
     * * @throws OurRuntime
     */
    public void removeManagerFromStore(String token, int managerId, int shopId) {
        try {
            LoggerService.logMethodExecution("removeManagerOfStore", token, shopId);
            int assigneeId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            Role role = userRepository.getRole(managerId, shopId);
            if (role == null) {
                LoggerService.logDebug("removeManagerOfStore", new OurRuntime("Member ID " + managerId + " is not a manager of shop ID " + shopId));
                throw new OurRuntime("Member ID " + managerId + " is not a manager of shop ID " + shopId);  
            }
            if (role.getAssigneeId() != assigneeId) {
                LoggerService.logDebug("removeManagerOfStore", new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + managerId + " in shop ID " + shopId));
                throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + managerId + " in shop ID " + shopId);
            }
            userRepository.removeRole(managerId, shopId);
            LoggerService.logMethodExecutionEndVoid("removeManagerOfStore");
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeManagerOfStore", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removeManagerOfStore", e, token, shopId);
            throw new RuntimeException("Error removing manager of store for member ID " + token + ": " + e.getMessage(), e);
        }   
    }

    /**
     * Removes an owner from a store.
     * @param token The token of the user making the removal.
     * @param memberId The ID of the owner to be removed.
     * @param shopId The ID of the store from which the owner will be removed.
     * 
     * * * @throws IllegalArgumentException
     * * * @throws OurRuntime
     */
    public void removeOwnerFromStore(String token, int memberId, int shopId) {
        try {
            LoggerService.logMethodExecution("removeOwnerFromStore", token, shopId);
            Role role = userRepository.getRole(memberId, shopId);
            if (!role.isOwner()) {
                LoggerService.logDebug("removeOwnerFromStore", new OurRuntime("Member ID " + memberId + " is not an owner of shop ID " + shopId));
                throw new OurRuntime("Member ID " + memberId + " is not an owner of shop ID " + shopId);  
            }
            int assigneeId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (role.getAssigneeId() != assigneeId) {
                LoggerService.logDebug("removeOwnerFromStore", new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + memberId + " in shop ID " + shopId));
                throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + memberId + " in shop ID " + shopId);
            }
            userRepository.removeRole(memberId, shopId);
            removeAllAssigned(memberId, shopId); // Remove all assigned roles for the member
            LoggerService.logMethodExecutionEndVoid("removeOwnerFromStore");
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeOwnerFromStore", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removeOwnerFromStore", e, token, shopId);
            throw new RuntimeException("Error removing owner from store for member ID " + token + ": " + e.getMessage(), e);
        }
    }

    private void removeAllAssigned(int assignee, int shopId) {
        try{
            for (Member member : userRepository.getMembersList()) {
                for (Role role : member.getRoles()) {
                    if (role.getShopId() == shopId && role.getAssigneeId() == assignee) {
                        userRepository.removeRole(member.getMemberId(), shopId); // Remove the role from the member
                        if (role.isOwner()) {
                            removeAllAssigned(assignee, shopId);
                        }
                    }
                }
            }
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeAllAssigned", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removeAllAssigned", e, assignee, shopId);
            throw new RuntimeException("Error removing all assigned roles for member ID " + assignee + ": " + e.getMessage(), e);
        }
    }

    /**
     * Assigns a member as a store owner.
     * @param token The token of the user making the assignment.
     * @param memberId The ID of the member to be assigned as an owner.
     * @param shopId The ID of the store where the member will be assigned as an owner.
     * 
     * * @throws IllegalArgumentException
     * * @throws OurRuntime
     */
    public void makeStoreOwner(String token, int memberId, int shopId){
        try {
            LoggerService.logMethodExecution("makeStoreOwner", token, shopId);
            int assigneeId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if(!userRepository.isOwner(assigneeId, shopId)) {
                LoggerService.logDebug("makeStoreOwner", new OurRuntime("Member ID " + assigneeId + " is not an owner of shop ID " + shopId));
                throw new OurRuntime("Member ID " + assigneeId + " is not an owner of shop ID " + shopId);  
            }
            if(userRepository.getMemberById(memberId) == null) {
                LoggerService.logDebug("makeStoreOwner", new OurRuntime("Member ID " + memberId + " does not exist."));
                throw new OurRuntime("Member ID " + memberId + " does not exist.");
            }
            Role role = new Role(assigneeId, shopId, null);
            role.setOwnersPermissions();
            userRepository.addRoleToPending(memberId, role); // Add the role to the member
            LoggerService.logMethodExecutionEndVoid("makeStoreOwner");
        } catch (OurRuntime e) {
            LoggerService.logDebug("makeStoreOwner", e);
            throw e; // Rethrow the custom exception
        } catch(Exception e) {
            LoggerService.logError("makeStoreOwner", e, token, shopId);
            throw new RuntimeException("Error making store owner for member ID " + token + ": " + e.getMessage(), e);
        }
    }

    /**
     * Accepts a pending role for a member.
     * @param token The token of the user accepting the role.
     * @param shopId The ID of the store where the role is being accepted.
     * 
     * * @throws IllegalArgumentException
     * * @throws OurRuntime
     */
    public void acceptRole(String token, int shopId) {
        try {
            LoggerService.logMethodExecution("acceptRole", token, shopId);
            int memberId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            Member member = userRepository.getMemberById(memberId);
            Role role = userRepository.getPendingRole(memberId, shopId);
            if (role == null) {
                LoggerService.logDebug("acceptRole", new OurRuntime("Member ID " + memberId + " has no pending role for shop ID " + shopId));
                throw new OurRuntime("Member ID " + memberId + " has no pending role for shop ID " + shopId);  
            }
            userRepository.acceptRole(memberId, role); // Accept the role for the member
            LoggerService.logMethodExecutionEndVoid("acceptRole");
        } catch (OurRuntime e) {
            LoggerService.logDebug("acceptRole", e);
            throw new OurArg("Error accepting role for member ID " + token + ": " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("acceptRole", e, token, shopId);
            throw new RuntimeException("Error accepting role for member ID " + token + ": " + e.getMessage(), e);
        }
    }

    public void declineRole(String token, int shopId) {
        try {
            LoggerService.logMethodExecution("declineRole", token, shopId);
            int memberId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            Member member = userRepository.getMemberById(memberId);
            Role role = userRepository.getPendingRole(memberId, shopId);
            if (role == null) {
                LoggerService.logDebug("declineRole", new OurRuntime("Member ID " + memberId + " has no pending role for shop ID " + shopId));
                throw new OurRuntime("Member ID " + memberId + " has no pending role for shop ID " + shopId);  
            }
            userRepository.declineRole(memberId, role); // Accept the role for the member
            LoggerService.logMethodExecutionEndVoid("declineRole");
        } catch (OurRuntime e) {
            LoggerService.logDebug("declineRole", e);
            throw new OurArg("Error declineing role for member token " + token + ": " +e.getMessage(),e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("declineRole", e, token, shopId);
            throw new RuntimeException("Error declineing role for member token " + token + ": " + e.getMessage(), e);
        }
    }
         
    /**
     * Adds a role to a member.
     * @param memberId The ID of the member to whom the role is being added.
     * @param role The role to be added to the member.
     * 
     * * @return true if the role was added successfully, false otherwise.
     * 
     * * @throws IllegalArgumentException
     * * @throws OurRuntime
     */
    public boolean addRole(int memberId, Role role) {
        try {
            LoggerService.logMethodExecution("addRole", memberId, role);
            validateMemberId(memberId);
            userRepository.addRoleToPending(memberId, role); // Add the role to the member
            LoggerService.logMethodExecutionEnd("addRole", true);
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("addRole", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("addRole", e, memberId, role);
            throw new RuntimeException("Error adding role for user ID " + memberId + ": " + e.getMessage(), e); // Indicate failure to add role
        }
    }

    /**
     * Removes a role from a member.
     * @param id The ID of the member from whom the role is being removed.
     * @param role The role to be removed from the member.
     * 
     * * @return true if the role was removed successfully, false otherwise.
     * 
     * * @throws IllegalArgumentException
     * * @throws OurRuntime
     */
    public boolean removeRole(int id, Role role) {
        try {
            LoggerService.logMethodExecution("removeRole", id, role);
            if (role == null) {
                LoggerService.logDebug("removeRole", new OurRuntime("Role cannot be null."));
                throw new OurRuntime("Role cannot be null.");
            }
            validateMemberId(id);
            Role existingRole = userRepository.getRole(id, role.getShopId());
            if (existingRole == null) {
                LoggerService.logDebug("removeRole", new OurRuntime("Member ID " + id + " has no role for shop ID " + role.getShopId()));
                throw new OurRuntime("Member ID " + id + " has no role for shop ID " + role.getShopId());  
            }
            if (existingRole.getAssigneeId() != role.getAssigneeId()) {
                LoggerService.logDebug("removeRole", new OurRuntime("Member ID " + id + " is not the assignee of the role for shop ID " + role.getShopId()));
                throw new OurRuntime("Member ID " + id + " is not the assignee of the role for shop ID " + role.getShopId());  
            }
            userRepository.removeRole(id, role.getShopId()); // Remove the role from the member
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeRole", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removeRole", e, id, role);
            throw new RuntimeException("Error removing role for user ID " + id + ": " + e.getMessage(), e); // Indicate failure to remove role
        }
    }

    /**
     * Checks if a member has a specific role.
     * @param id The ID of the member to check.
     * @param role The role to check for the member.
     * 
     * * @return true if the member has the specified role, false otherwise.
     * 
     * * @throws IllegalArgumentException
     * * @throws OurRuntime
     */
    public boolean hasRole(int id, Role role) {
        try {
            LoggerService.logMethodExecution("hasRole", id, role);
            if (role == null) {
                LoggerService.logDebug("hasRole", new OurRuntime("Role cannot be null."));
                throw new OurRuntime("Role cannot be null.");
            }
            validateMemberId(id);
            Role existingRole = userRepository.getRole(id, role.getShopId());
            if (existingRole == null) {
                LoggerService.logDebug("hasRole", new OurRuntime("Member ID " + id + " has no role for shop ID " + role.getShopId()));
                throw new OurRuntime("Member ID " + id + " has no role for shop ID " + role.getShopId());}
            if (existingRole.getAssigneeId() != role.getAssigneeId()) {
                LoggerService.logDebug("hasRole", new OurRuntime("Member ID " + id + " is not the assignee of the role for shop ID " + role.getShopId()));
                throw new OurRuntime("Member ID " + id + " is not the assignee of the role for shop ID " + role.getShopId());
            }
            return true; // Member has the specified role
        } catch (OurRuntime e) {
            LoggerService.logDebug("hasRole", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("hasRole", e, id, role);
            throw new RuntimeException("Error checking role for user ID " + id + ": " + e.getMessage(), e); // Indicate failure to check role
        }
    }

    /**
     * Adds a permission to a member.
     * @param token The token of the user adding the permission.
     * @param id The ID of the member to whom the permission is being added.
     * @param permission The permission to be added to the member.
     * @param shopId The ID of the store where the permission is being added.
     * 
     * * @return true if the permission was added successfully, false otherwise.
     * 
     * * @throws IllegalArgumentException
     * * @throws OurRuntime
     */
    public boolean addPermission(String token, int id, PermissionsEnum permission, int shopId) {
        try {
            LoggerService.logMethodExecution("addPermission", token, id, permission, shopId);
            int assigneeId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (permission == null) {
                LoggerService.logDebug("addPermission", new OurRuntime("Permission cannot be null."));
                throw new OurRuntime("Permission cannot be null.");
            }
            validateMemberId(id);
            if (userRepository.getRole(id, shopId) == null) {
                LoggerService.logDebug("addPermission", new OurRuntime("Member ID " + id + " has no role for shop ID " + shopId));
                throw new OurRuntime("Member ID " + id + " has no role for shop ID " + shopId);  
            }
            if (userRepository.getRole(id, shopId).getAssigneeId() != assigneeId) {
                LoggerService.logDebug("addPermission", new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + id + " in shop ID " + shopId));
                throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + id + " in shop ID " + shopId);
            }
            userRepository.addPermission(id, permission, shopId); // Add the permission to the member
            LoggerService.logMethodExecutionEnd("addPermission", true);
            return true; // Permission added successfully
        } catch (OurRuntime e) {
            LoggerService.logDebug("addPermission", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("addPermission", e, token, id, permission, shopId);
            throw new RuntimeException("Error adding permission for user ID " + id + ": " + e.getMessage(), e); // Indicate failure to add permission
        }
    }

    /**
     * Removes a permission from a member.
     * @param token The token of the user removing the permission.
     * @param id The ID of the member from whom the permission is being removed.
     * @param permission The permission to be removed from the member.
     * @param shopId The ID of the store where the permission is being removed.
     * 
     * * @return true if the permission was removed successfully, false otherwise.
     * 
     * * @throws IllegalArgumentException
     * * @throws OurRuntime
     */
    public boolean removePermission(String token, int id, PermissionsEnum permission, int shopId) {
        try {
            LoggerService.logMethodExecution("removePermission", token, id, permission, shopId);
            int assigneeId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (permission == null) {
                LoggerService.logDebug("removePermission", new OurRuntime("Permission cannot be null."));
                throw new OurRuntime("Permission cannot be null.");
            }
            validateMemberId(id);
            if (userRepository.getRole(id, shopId) == null) {
                LoggerService.logDebug("removePermission", new OurRuntime("Member ID " + id + " has no role for shop ID " + shopId));
                throw new OurRuntime("Member ID " + id + " has no role for shop ID " + shopId);  
            }
            if (userRepository.getRole(id, shopId).getAssigneeId() != assigneeId) {
                LoggerService.logDebug("removePermission", new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + id + " in shop ID " + shopId));
                throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + id + " in shop ID " + shopId);
            }
            userRepository.removePermission(id, permission, shopId); // Add the permission to the member
            LoggerService.logMethodExecutionEnd("removePermission", true);
            return true; // Permission added successfully
        } catch (OurRuntime e) {
            LoggerService.logDebug("removePermission", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removePermission", e, token, id, permission, shopId);
            throw new RuntimeException("Error removing permission for user ID " + id + ": " + e.getMessage(), e); // Indicate failure to remove permission
        }
    }
    public boolean hasPermission(int id, PermissionsEnum permission, int shopId) {
        try {
            if (userRepository.getUserMapping().containsKey(id)) {
                User user = userRepository.getUserById(id);
                validateMemberId(id);
                return ((Member)user).hasPermission(permission,shopId); // Check if the user has the specified permission
            } else {
                throw new OurArg("User with ID " + id + " doesn't exist.");
            }
        } catch (Exception e) {
            return false; // Indicate failure to check permission
        }
    }

    /**
     * Retrieves the shopping cart items for a user by their ID.
     * @param userId The ID of the user whose shopping cart items are to be retrieved.
     * @return A DeepCopy HashMap containing the shopping cart items for the user. shopId -> <itemId -> quantity>*
     */
    public HashMap<Integer, HashMap<Integer,Integer>> getUserShoppingCartItems(int userId){
        try {
            LoggerService.logMethodExecution("getUserShoppingCart", userId);
            HashMap<Integer, HashMap<Integer,Integer>> cart = userRepository.getShoppingCartById(userId).getItems();
            LoggerService.logMethodExecutionEnd("getUserShoppingCart", cart);
            return cart;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getUserShoppingCart", e);
            throw e; // Rethrow the custom exception
        } catch (IllegalArgumentException e) {
            LoggerService.logError("getUserShoppingCart", e, userId);
            throw new OurArg("Invalid user ID: " + userId, e);
        } catch (Exception e) {
            LoggerService.logError("getUserShoppingCart", e, userId);
            throw new RuntimeException("Error fetching shopping cart for user ID " + userId + ": " + e.getMessage(), e);
        }
        
    }

    /**
     * Clears the shopping cart for a user by their ID.
     * @param userId The ID of the user whose shopping cart is to be cleared.
     * @throws IllegalArgumentException if the user ID is invalid or the user is not a member.
     * @throws RuntimeException if an error occurs while clearing the shopping cart.
     */
    public void clearUserShoppingCart(int userId){
        try {
            LoggerService.logMethodExecution("clearUserShoppingCart", userId);
            userRepository.getShoppingCartById(userId).clearCart();
            LoggerService.logMethodExecutionEndVoid("clearUserShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("clearUserShoppingCart", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("clearUserShoppingCart", e, userId);
            throw new RuntimeException("Error clearing shopping cart for user ID " + userId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Restores the shopping cart for a user by their ID.
     * @param userId The ID of the user whose shopping cart is to be restored.
     * @param items A HashMap containing the items to restore in the shopping cart. shopId -> <itemId -> quantity>
     * @throws IllegalArgumentException if the user ID is invalid or the user is not a member.
     * @throws RuntimeException if an error occurs while restoring the shopping cart.
     * @throws NullPointerException if the items HashMap is null.
     */
    public void restoreUserShoppingCart(int userId, HashMap<Integer, HashMap<Integer,Integer>> items){
        try {
            LoggerService.logMethodExecution("restoreUserShoppingCart", userId, items);
            userRepository.getShoppingCartById(userId).restoreCart(items);
            LoggerService.logMethodExecutionEndVoid("restoreUserShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("restoreUserShoppingCart", e);
            throw e;
        } catch (Exception e) {
            LoggerService.logError("restoreUserShoppingCart", e, userId, items);
            throw new RuntimeException("Error restoring shopping cart for user ID " + userId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the payment method for a user by their ID.
     * @param userId The ID of the user whose payment method is to be retrieved.
     * @return The PaymentMethod object associated with the user.
     * @throws IllegalArgumentException if the user ID is invalid or the user is not a member.
     * @throws RuntimeException if an error occurs while fetching the payment method.
     * @throws NullPointerException if the payment method is null.
     */
    public PaymentMethod getUserPaymentMethod(int userId) {
        try {
            LoggerService.logMethodExecution("getUserPaymentMethod", userId);
            PaymentMethod paymentMethod = userRepository.getUserById(userId).getPaymentMethod();
            LoggerService.logMethodExecutionEnd("getUserPaymentMethod", paymentMethod);
            return paymentMethod;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getUserPaymentMethod", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("getUserPaymentMethod", e, userId);
            throw new RuntimeException("Error fetching payment method for user ID " + userId + ": " + e.getMessage(), e);
        }
    }

    /**
     * adds an item to the shopping cart for a user by their token.
     * @param token
     * @param shopId
     * @param itemId
     * @param quantity
     * 
     * * @throws OurRuntime
     * * @throws Exception
     */
    public void addItemToShoppingCart(String token, int shopId, int itemId, int quantity) {
        try {
            LoggerService.logMethodExecution("addItemToShoppingCart", token, shopId, itemId, quantity);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            userRepository.addItemToShoppingCart(userId, shopId, itemId, quantity);
            LoggerService.logMethodExecutionEndVoid("addItemToShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("addItemToShoppingCart", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("addItemToShoppingCart", e, token, shopId, itemId, quantity);
            throw new RuntimeException("Error adding item to shopping cart for user ID " + token + ": " + e.getMessage(), e);
        }
    }

    /**
     * removes an item from the shopping cart for a user by their token.
     * @param token
     * @param shopId
     * @param itemId
     * 
     * * @throws OurRuntime
     * * @throws Exception
     */
    public void removeItemFromShoppingCart(String token, int shopId, int itemId) {
        try {
            LoggerService.logMethodExecution("removeItemFromShoppingCart", token, shopId, itemId);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            userRepository.removeItemFromShoppingCart(userId, shopId, itemId);
            LoggerService.logMethodExecutionEndVoid("removeItemFromShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeItemFromShoppingCart", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removeItemFromShoppingCart", e, token, shopId, itemId);
            throw new RuntimeException("Error removing item from shopping cart for user ID " + token + ": " + e.getMessage(), e);
        }
    }

    /**
     * updates the quantity of an item in the shopping cart for a user by their token.
     * @param token
     * @param shopId
     * @param itemId
     * @param quantity
     * 
     * * @throws OurRuntime
     * * @throws Exception
     */
    public void updateItemQuantityInShoppingCart(String token, int shopId, int itemId, int quantity) {
        try {
            LoggerService.logMethodExecution("updateItemQuantityInShoppingCart", token, shopId, itemId, quantity);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            userRepository.updateItemQuantityInShoppingCart(userId, shopId, itemId, quantity);
            LoggerService.logMethodExecutionEndVoid("updateItemQuantityInShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("updateItemQuantityInShoppingCart", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("updateItemQuantityInShoppingCart", e, token, shopId, itemId, quantity);
            throw new RuntimeException("Error updating item quantity in shopping cart for user ID " + token + ": " + e.getMessage(), e);
        }
    }

    /**
     * Clears the shopping cart for a user by their token.
     * @param token The token of the user whose shopping cart is to be cleared.
     * 
     * * @throws OurRuntime
     * * @throws Exception
     */
    public void clearShoppingCart(String token) {
        try {
            LoggerService.logMethodExecution("clearShoppingCart", token);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            userRepository.clearShoppingCart(userId);
            LoggerService.logMethodExecutionEndVoid("clearShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("clearShoppingCart", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("clearShoppingCart", e, token);
            throw new RuntimeException("Error clearing shopping cart for user ID " + token + ": " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the items in the shopping basket for a user by their token and shop ID.
     * @param token The token of the user whose shopping basket items are to be retrieved.
     * @param shopId The ID of the shop whose items are to be retrieved.
     * @return A HashMap containing the items in the shopping basket for the user. itemId -> quantity
     * 
     * * @throws OurRuntime
     * * @throws Exception
     */
    public Map<Integer, Integer> getBasketItems(String token, int shopId) {
        try {
            LoggerService.logMethodExecution("getBasketItems", token, shopId);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            Map<Integer, Integer> items = userRepository.getBasket(userId, shopId);
            LoggerService.logMethodExecutionEnd("getBasketItems", items);
            return items;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getBasketItems", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("getBasketItems", e, token, shopId);
            throw new RuntimeException("Error fetching basket items for user ID " + token + ": " + e.getMessage(), e);
        }
    }

    /**
     * Adds a new shopping basket for a user by their token and shop ID.
     * @param token The token of the user adding the basket.
     * @param shopId The ID of the shop where the basket is being added.
     * 
     * * @throws OurRuntime
     * * @throws Exception
     */
    public void addBasket(String token, int shopId) {
        try {
            LoggerService.logMethodExecution("addBasket", token, shopId);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            userRepository.createBasket(userId, shopId);
            LoggerService.logMethodExecutionEndVoid("addBasket");
        } catch (OurRuntime e) {
            LoggerService.logDebug("addBasket", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("addBasket", e, token, shopId);
            throw new RuntimeException("Error adding basket for user ID " + token + ": " + e.getMessage(), e);
        }
    }


    public boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        if (username.length() < 3 || username.length() > 20) {
            return false;
        }

        String pattern = "^[a-zA-Z0-9_]+$";

        return username.matches(pattern);
    }
    public boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
/* 
        // Length check
        if (password.length() < 8) {
            return false;
        }

        // Regex for required rules
        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

        return password.matches(pattern);
*/
        return true; // For now, we are not validating the password format
    }
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        // Regex:
        // ^\+?           → optional + at the start
        // \d+            → one or more digits
        // (-\d+)?        → optional single dash followed by digits
        // $              → end of string
        // Full length between 9 to 15 characters including dash/+ if present
        String pattern = "^\\+?\\d+(-\\d+)?$";

        if (!phoneNumber.trim().matches(pattern)) {
            return false;
        }

        // Check total length (after validating format)
        if (phoneNumber.length() < 9 || phoneNumber.length() > 15) {
            return false;
        }

        return true;
    }
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        // Regex for a standard email format
        String pattern = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        return email.trim().matches(pattern);
    }
    public void isValidDetails(String username, String password, String email, String phoneNumber) {
        StringBuilder errorMsg = new StringBuilder();

        if (!isValidUsername(username)) {
            errorMsg.append("Invalid Username.");
        }
        if (!isValidPassword(password)) {
            errorMsg.append("Invalid Password.");
        }
        if (!isValidPhoneNumber(phoneNumber)) {
            errorMsg.append("Invalid Phone Number.");
        }
        if (!isValidEmail(email)) {
            errorMsg.append("Invalid Email.");
        }

        if (errorMsg.length() > 0) {
            throw new RuntimeException(errorMsg.toString().trim());
        }
    }

   /**
     * Sets the payment method for a user by their token and shop ID.
     * @param token The token of the user setting the payment method.
     * @param paymentMethod The PaymentMethod object to be set for the user.
     * @param shopId The ID of the shop where the payment method is being set.
     * 
     * * @throws OurRuntime
     * * @throws Exception
     */
    public void setPaymentMethod(String token, PaymentMethod paymentMethod, int shopId) {
        try {
            LoggerService.logMethodExecution("setPaymentMethod", token, paymentMethod);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (paymentMethod == null) {
                LoggerService.logDebug("setPaymentMethod", new OurRuntime("Payment method cannot be null."));
                throw new OurRuntime("Payment method cannot be null.");
            }
            userRepository.setPaymentMethod(userId, shopId, paymentMethod); // Set the payment method for the user
            LoggerService.logMethodExecutionEndVoid("setPaymentMethod");
        } catch (OurRuntime e) {
            LoggerService.logDebug("setPaymentMethod", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("setPaymentMethod", e, token, paymentMethod);
            throw new RuntimeException("Error setting payment method for user ID " + token + ": " + e.getMessage(), e);
        }
    }

    /**
     * Pays for user's order
     * @param token
     * @param shopId
     * @param payment
     * 
     * * @return true if the payment was successful
     * 
     * * @throws OurRuntime, Exception
     */
    public boolean pay(String token, int shopId, double payment){
        try {
            LoggerService.logMethodExecution("pay", token, shopId, payment);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            userRepository.pay(userId, shopId, payment); // Set the payment method for the user
            LoggerService.logMethodExecutionEnd("pay", true);
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("pay", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("pay", e, token, shopId, payment);
            throw new RuntimeException("Error setting payment method for user ID " + token + ": " + e.getMessage(), e);
        }
    }

    //NO API ENDPOINT!
    public boolean refundPaymentAuto(String token, int shopId, double payment){
        try {
            LoggerService.logMethodExecution("refundPayment", token, shopId, payment);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            userRepository.refund(userId, shopId, payment); // Set the payment method for the user
            LoggerService.logMethodExecutionEnd("refundPayment", true);
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("refundPayment", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("refundPayment", e, token, shopId, payment);
            throw new RuntimeException("Error setting payment method for user ID " + token + ": " + e.getMessage(), e);
        }
        
    }

    public boolean refundPaymentByStoreEmployee(String token, int userId, int shopId, double payment){
        try {
            LoggerService.logMethodExecution("refundPaymentByStoreEmployee", token, shopId, payment);
            int initiatingUserId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (userRepository.getRole(initiatingUserId, shopId) == null) {
                LoggerService.logDebug("refundPaymentByStoreEmployee", new OurRuntime("Member ID " + initiatingUserId + " has no role for shop ID " + shopId));
                throw new OurRuntime("Member ID " + initiatingUserId + " has no role for shop ID " + shopId);  
            }
            userRepository.refund(userId, shopId, payment); // Set the payment method for the user
            LoggerService.logMethodExecutionEnd("refundPayment", true);
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("refundPayment", e);
            throw e; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("refundPayment", e, token, shopId, payment);
            throw new RuntimeException("Error setting payment method for user ID " + token + ": " + e.getMessage(), e);
        }
        
    }
}
