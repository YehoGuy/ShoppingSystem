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

    public void makeAdmin(String token, Integer id) {
        LoggerService.logMethodExecution("makeAdmin", id);
        try {
            int userId = authTokenService.ValidateToken(token);
            if (!isAdmin(userId)) {
                throw new OurRuntime("Only admins can make admins.");
            }
            if (id == null || id < 0) {
                throw new OurArg("The ID of the user to make admin is illegal.");
            }
            userRepository.addAdmin(id);
            LoggerService.logMethodExecutionEndVoid("makeAdmin");
        } catch (OurArg e) {
            LoggerService.logDebug("makeAdmin", e);
            throw new OurArg("makeAdmin: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("makeAdmin", e, id);
            throw new OurRuntime("Error making user " + id + " an admin: " + e.getMessage(), e);
        }
    }
    
    public void removeAdmin(String token, Integer id) {
        LoggerService.logMethodExecution("removeAdmin", id);
        try {
            int userId = authTokenService.ValidateToken(token);
            if (!isAdmin(userId)) {
                throw new OurArg("Only admins can remove admins.");
            }
            if (id == null || id < 0) {
                throw new OurArg("The ID of the user to remove as admin is illegal.");
            }
            userRepository.removeAdmin(id);
            LoggerService.logMethodExecutionEndVoid("removeAdmin");
        } catch (OurArg e) {
            LoggerService.logDebug("removeAdmin", e);
            throw new OurArg("removeAdmin: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("removeAdmin", e, id);
            throw new OurRuntime("Error removing user " + id + " as admin: " + e.getMessage(), e);
        }
    }
    
    public List<Integer> getAllAdmins(String token) {
        LoggerService.logMethodExecution("getAllAdmins");
        try {
            int userId = authTokenService.ValidateToken(token);
            if (!isAdmin(userId)) {
                throw new OurArg("Only admins can view the list of admins.");
            }
            List<Integer> admins = userRepository.getAllAdmins();
            LoggerService.logMethodExecutionEnd("getAllAdmins", admins);
            return admins;
        } catch (OurArg e) {
            LoggerService.logDebug("getAllAdmins", e);
            throw new OurArg("getAllAdmins: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getAllAdmins", e);
            throw new OurRuntime("Error retrieving admin list: " + e.getMessage(), e);
        }
    }
    
    public void setServices(AuthTokenService authTokenService) {
        LoggerService.logMethodExecution("setServices");
        this.authTokenService = authTokenService;
        LoggerService.logMethodExecutionEndVoid("setServices");
    }
    
    public User getUserById(int id) {
        LoggerService.logMethodExecution("getUserById", id);
        try {
            User user = userRepository.getUserById(id);
            LoggerService.logMethodExecutionEnd("getUserById", user);
            return user;
        } catch (OurArg e) {
            LoggerService.logDebug("getUserById", e);
            throw new OurArg("getUserById: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getUserById", e, id);
            throw new OurRuntime("Error fetching user with ID " + id + ": " + e.getMessage(), e);
        }
    }
    
    public void addMember(String username, String password, String email, String phoneNumber, String address) {
        LoggerService.logMethodExecution("addMember", username, email, phoneNumber, address);
        try {
            isValidDetails(username, password, email, phoneNumber);
            if (userRepository.isUsernameAndPasswordValid(username, password) != -1) {
                throw new OurArg("Username is already taken.");
            }
            String encryptedPassword = passwordEncoder.encode(password);
            userRepository.addMember(username, encryptedPassword, email, phoneNumber, address);
            LoggerService.logMethodExecutionEndVoid("addMember");
        } catch (OurArg e) {
            LoggerService.logDebug("addMember", e);
            throw new OurArg("addMember: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("addMember", e, username, email, phoneNumber, address);
            throw new OurRuntime("Error adding member: " + e.getMessage(), e);
        }
    }
    
    public void updateMemberUsername(String token, String username) {
        try {
            int id = authTokenService.ValidateToken(token);
            LoggerService.logMethodExecution("updateMemberUsername", id, username);
            validateMemberId(id);
            isValidUsername(username);
            userRepository.updateMemberUsername(id, username);
            LoggerService.logMethodExecutionEndVoid("updateMemberUsername");
        } catch (OurArg e) {
            LoggerService.logDebug("updateMemberUsername", e);
            throw new OurArg("updateMemberUsername: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("updateMemberUsername", e, username);
            throw new OurRuntime("Error updating username: " + e.getMessage(), e);
        }
    }
    
    public void updateMemberPassword(String token, String password) {
        try {
            int id = authTokenService.ValidateToken(token);
            LoggerService.logMethodExecution("updateMemberPassword", id);
            isValidPassword(password);
            String encryptedPassword = passwordEncoder.encode(password);
            validateMemberId(id);
            userRepository.updateMemberPassword(id, encryptedPassword);
            LoggerService.logMethodExecutionEndVoid("updateMemberPassword");
        } catch (OurArg e) {
            LoggerService.logDebug("updateMemberPassword", e);
            throw new OurArg("updateMemberPassword: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("updateMemberPassword", e);
            throw new OurRuntime("Error updating password: " + e.getMessage(), e);
        }
    }
    
    public void updateMemberEmail(String token, String email) {
        try {
            int id = authTokenService.ValidateToken(token);
            LoggerService.logMethodExecution("updateMemberEmail", id, email);
            validateMemberId(id);
            isValidEmail(email);
            userRepository.updateMemberEmail(id, email);
            LoggerService.logMethodExecutionEndVoid("updateMemberEmail");
        } catch (OurArg e) {
            LoggerService.logDebug("updateMemberEmail", e);
            throw new OurArg("updateMemberEmail: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("updateMemberEmail", e, email);
            throw new OurRuntime("Error updating email: " + e.getMessage(), e);
        }
    }
    
    public void updateMemberPhoneNumber(String token, String phoneNumber) {
        try {
            int id = authTokenService.ValidateToken(token);
            LoggerService.logMethodExecution("updateMemberPhoneNumber", id, phoneNumber);
            validateMemberId(id);
            isValidPhoneNumber(phoneNumber);
            userRepository.updateMemberPhoneNumber(id, phoneNumber);
            LoggerService.logMethodExecutionEndVoid("updateMemberPhoneNumber");
        } catch (OurArg e) {
            LoggerService.logDebug("updateMemberPhoneNumber", e);
            throw new OurArg("updateMemberPhoneNumber: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("updateMemberPhoneNumber", e, phoneNumber);
            throw new OurRuntime("Error updating phone number: " + e.getMessage(), e);
        }
    }
    
    public void updateMemberAddress(String token, String address) {
        try {
            int id = authTokenService.ValidateToken(token);
            LoggerService.logMethodExecution("updateMemberAddress", id, address);
            validateMemberId(id);
            userRepository.updateMemberAddress(id, address);
            LoggerService.logMethodExecutionEndVoid("updateMemberAddress");
        } catch (OurArg e) {
            LoggerService.logDebug("updateMemberAddress", e);
            throw new OurArg("updateMemberAddress: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("updateMemberAddress", e, address);
            throw new OurRuntime("Error updating address: " + e.getMessage(), e);
        }
    }
    
    

    public void validateMemberId(int id) {
        if (id <= 0) {
            throw new OurArg("validateMemberId: Invalid user ID: " + id);
        }
        if (!userRepository.getUserMapping().containsKey(id)) {
            throw new OurArg("validateMemberId: User with ID " + id + " doesn't exist.");
        }
        User user = userRepository.getUserById(id);
        if (!(user instanceof Member)) {
            throw new OurArg("validateMemberId: User with ID " + id + " is not a member.");
        }
    }
    
    public String loginAsGuest() {
        try {
            LoggerService.logMethodExecution("loginAsGuest");
            int id = userRepository.addGuest();
            if (id < 0) {
                throw new OurArg("Failed to create a guest user.");
            }
            String token = authTokenService.AuthenticateGuest(id);
            LoggerService.logMethodExecutionEnd("loginAsGuest", token);
            return token;
        } catch (OurArg e) {
            LoggerService.logDebug("loginAsGuest", e);
            throw new OurArg("loginAsGuest: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("loginAsGuest", e);
            throw new OurRuntime("Error logging in as guest: " + e.getMessage(), e);
        }
    }
    
    public String loginAsMember(String username, String password, String token_if_guest) {
        password = passwordEncoder.encode(password);
        LoggerService.logMethodExecution("loginAsMember", username, "ENCODED_PASSWORD", token_if_guest);
        try {
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                throw new OurArg("Username and password cannot be null or empty.");
            }
            int loginAsMember_id = userRepository.isUsernameAndPasswordValid(username, password);
            if (loginAsMember_id > 0) {
                if (token_if_guest.isEmpty()) {
                    String token = authTokenService.Login(username, password, loginAsMember_id);
                    LoggerService.logMethodExecutionEnd("loginAsMember", loginAsMember_id);
                    return token;
                } else {
                    int id = authTokenService.ValidateToken(token_if_guest);
                    User member = userRepository.getUserById(loginAsMember_id);
                    User guest = userRepository.getUserById(id);
                    member.mergeShoppingCart(guest.getShoppingCart());
                    userRepository.removeUserById(id);
                    String token = authTokenService.Login(username, password, loginAsMember_id);
                    LoggerService.logMethodExecutionEnd("loginAsMember", loginAsMember_id);
                    return token;
                }
            } else {
                throw new OurArg("Invalid username or password.");
            }
        } catch (OurArg e) {
            LoggerService.logDebug("loginAsMember", e);
            throw new OurArg("loginAsMember: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("loginAsMember", e, username, "ENCODED_PASSWORD", token_if_guest);
            throw new OurRuntime("Error logging in as member: " + e.getMessage(), e);
        }
    }
    
    public String logout(String token) {
        try {
            LoggerService.logMethodExecution("logout", token);
            int id = authTokenService.ValidateToken(token);
            if (userRepository.isGuestById(id)) {
                userRepository.removeUserById(id);
            }
            authTokenService.Logout(token);
            LoggerService.logMethodExecutionEnd("logout", true);
            return loginAsGuest();
        } catch (OurArg e) {
            LoggerService.logDebug("logout", e);
            throw new OurArg("logout: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("logout", e, token);
            throw new OurRuntime("logout: " + e.getMessage(), e);
        }
    }
    
    public HashMap<Integer, PermissionsEnum[]> getPermitionsByShop(String token, int shopId) {
        try {
            LoggerService.logMethodExecution("getPermitionsByShop", shopId);
            int id = authTokenService.ValidateToken(token);
            if (!userRepository.isOwner(id, shopId)) {
                throw new OurArg("Member ID " + id + " is not an owner of shop ID " + shopId);
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
        } catch (OurArg e) {
            LoggerService.logDebug("getPermitionsByShop", e);
            throw new OurArg("getPermitionsByShop: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getPermitionsByShop", e, shopId);
            throw new OurRuntime("getPermitionsByShop: " + e.getMessage(), e);
        }
    }
    
    public void changePermissions(String token, int memberId, int shopId, PermissionsEnum[] permissions) {
        try {
            LoggerService.logMethodExecution("changePermissions", token, memberId, shopId, permissions);
            int assigneeId = authTokenService.ValidateToken(token);
            if (!userRepository.isOwner(assigneeId, shopId)) {
                throw new OurArg("Member ID " + assigneeId + " is not an owner of shop ID " + shopId);
            }
            Member member = userRepository.getMemberById(memberId);
            for (Role role : member.getRoles()) {
                if (role.getShopId() == shopId) {
                    if (role.getAssigneeId() != assigneeId) {
                        throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + memberId + " in shop ID " + shopId);
                    }
                    for (PermissionsEnum permission : permissions) {
                        if (permission == null) {
                            throw new OurArg("Permission cannot be null.");
                        }
                        if (permission == PermissionsEnum.closeShop) {
                            throw new OurArg("Permission closeShop cannot be changed.");
                        }
                    }
                    userRepository.setPermissions(memberId, shopId, role, permissions);
                    LoggerService.logMethodExecutionEndVoid("changePermissions");
                    return;
                }
            }
            LoggerService.logMethodExecutionEndVoid("changePermissions");
        } catch (OurArg e) {
            LoggerService.logDebug("changePermissions", e);
            throw new OurArg("changePermissions: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("changePermissions", e);
            throw new OurRuntime("changePermissions: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("changePermissions", e, memberId, shopId, permissions);
            throw new OurRuntime("changePermissions: " + e.getMessage(), e);
        }
    }
    
    public void makeManagerOfStore(String token, int memberId, int shopId, PermissionsEnum[] permissions) {
        try {
            LoggerService.logMethodExecution("makeManagerOfStore", token, shopId, permissions);
            int assignee = authTokenService.ValidateToken(token);
            if (!userRepository.isOwner(assignee, shopId)) {
                throw new OurRuntime("makeManagerOfStore: Member ID " + assignee + " is not an owner of shop ID " + shopId);
            }
            Role role = new Role(assignee, shopId, permissions);
            userRepository.addRoleToPending(memberId, role);
            LoggerService.logMethodExecutionEndVoid("makeManagerOfStore");
        } catch (OurRuntime e) {
            LoggerService.logDebug("makeManagerOfStore", e);
            throw new OurRuntime("makeManagerOfStore: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("makeManagerOfStore", e);
            throw new OurArg("makeManagerOfStore: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("makeManagerOfStore", e, token, shopId, permissions);
            throw new OurRuntime("makeManagerOfStore: " + e.getMessage(), e);
        }
    }
    

    /**
     * Removes a manager from a store.
     * @param token The token of the user making the removal.
     * @param managerId The ID of the manager to be removed.
     * @param shopId The ID of the store from which the manager will be removed.
     * 
     * * @throws OurArg
     * * @throws OurRuntime
     */
    public void removeManagerFromStore(String token, int managerId, int shopId) {
        try {
            LoggerService.logMethodExecution("removeManagerFromStore", token, shopId);
            int assigneeId = authTokenService.ValidateToken(token);
            Role role = userRepository.getRole(managerId, shopId);
            if (role == null) {
                throw new OurRuntime("Member ID " + managerId + " is not a manager of shop ID " + shopId);
            }
            if (role.getAssigneeId() != assigneeId) {
                throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + managerId + " in shop ID " + shopId);
            }
            userRepository.removeRole(managerId, shopId);
            LoggerService.logMethodExecutionEndVoid("removeManagerFromStore");
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeManagerFromStore", e);
            throw new OurRuntime("removeManagerFromStore: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("removeManagerFromStore", e, token, shopId);
            throw new OurRuntime("removeManagerFromStore: " + e.getMessage(), e);
        }
    }
    
    public void removeOwnerFromStore(String token, int memberId, int shopId) {
        try {
            LoggerService.logMethodExecution("removeOwnerFromStore", token, shopId);
            Role role = userRepository.getRole(memberId, shopId);
            if (!role.isOwner()) {
                throw new OurRuntime("Member ID " + memberId + " is not an owner of shop ID " + shopId);
            }
            int assigneeId = authTokenService.ValidateToken(token);
            if (role.getAssigneeId() != assigneeId) {
                throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + memberId + " in shop ID " + shopId);
            }
            userRepository.removeRole(memberId, shopId);
            removeAllAssigned(memberId, shopId);
            LoggerService.logMethodExecutionEndVoid("removeOwnerFromStore");
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeOwnerFromStore", e);
            throw new OurRuntime("removeOwnerFromStore: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("removeOwnerFromStore", e, token, shopId);
            throw new OurRuntime("removeOwnerFromStore: " + e.getMessage(), e);
        }
    }
    
    private void removeAllAssigned(int assignee, int shopId) {
        try {
            for (Member member : userRepository.getMembersList()) {
                for (Role role : member.getRoles()) {
                    if (role.getShopId() == shopId && role.getAssigneeId() == assignee) {
                        userRepository.removeRole(member.getMemberId(), shopId);
                        if (role.isOwner()) {
                            removeAllAssigned(assignee, shopId);
                        }
                    }
                }
            }
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeAllAssigned", e);
            throw new OurRuntime("removeAllAssigned: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("removeAllAssigned", e, assignee, shopId);
            throw new OurRuntime("removeAllAssigned: " + e.getMessage(), e);
        }
    }
    
    public void makeStoreOwner(String token, int memberId, int shopId) {
        try {
            LoggerService.logMethodExecution("makeStoreOwner", token, shopId);
            int assigneeId = authTokenService.ValidateToken(token);
            if (!userRepository.isOwner(assigneeId, shopId)) {
                throw new OurRuntime("Member ID " + assigneeId + " is not an owner of shop ID " + shopId);
            }
            if (userRepository.getMemberById(memberId) == null) {
                throw new OurRuntime("Member ID " + memberId + " does not exist.");
            }
            Role role = new Role(assigneeId, shopId, null);
            role.setOwnersPermissions();
            userRepository.addRoleToPending(memberId, role);
            LoggerService.logMethodExecutionEndVoid("makeStoreOwner");
        } catch (OurRuntime e) {
            LoggerService.logDebug("makeStoreOwner", e);
            throw new OurRuntime("makeStoreOwner: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("makeStoreOwner", e, token, shopId);
            throw new OurRuntime("makeStoreOwner: " + e.getMessage(), e);
        }
    }
    
    public void acceptRole(String token, int shopId) {
        try {
            LoggerService.logMethodExecution("acceptRole", token, shopId);
            int memberId = authTokenService.ValidateToken(token);
            Role role = userRepository.getPendingRole(memberId, shopId);
            if (role == null) {
                throw new OurRuntime("Member ID " + memberId + " has no pending role for shop ID " + shopId);
            }
            userRepository.acceptRole(memberId, role);
            LoggerService.logMethodExecutionEndVoid("acceptRole");
        } catch (OurRuntime e) {
            LoggerService.logDebug("acceptRole", e);
            throw new OurRuntime("acceptRole: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("acceptRole", e, token, shopId);
            throw new OurRuntime("acceptRole: " + e.getMessage(), e);
        }
    }
    
    public void declineRole(String token, int shopId) {
        try {
            LoggerService.logMethodExecution("declineRole", token, shopId);
            int memberId = authTokenService.ValidateToken(token);
            Role role = userRepository.getPendingRole(memberId, shopId);
            if (role == null) {
                throw new OurRuntime("Member ID " + memberId + " has no pending role for shop ID " + shopId);
            }
            userRepository.declineRole(memberId, role);
            LoggerService.logMethodExecutionEndVoid("declineRole");
        } catch (OurRuntime e) {
            LoggerService.logDebug("declineRole", e);
            throw new OurRuntime("declineRole: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("declineRole", e, token, shopId);
            throw new OurRuntime("declineRole: " + e.getMessage(), e);
        }
    }
    
    public boolean addRole(int memberId, Role role) {
        try {
            LoggerService.logMethodExecution("addRole", memberId, role);
            validateMemberId(memberId);
            userRepository.addRoleToPending(memberId, role);
            LoggerService.logMethodExecutionEnd("addRole", true);
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("addRole", e);
            throw new OurRuntime("addRole: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("addRole", e, memberId, role);
            throw new OurRuntime("addRole: " + e.getMessage(), e);
        }
    }
    
    public boolean removeRole(int id, Role role) {
        try {
            LoggerService.logMethodExecution("removeRole", id, role);
            if (role == null) {
                throw new OurRuntime("Role cannot be null.");
            }
            validateMemberId(id);
            Role existingRole = userRepository.getRole(id, role.getShopId());
            if (existingRole == null) {
                throw new OurRuntime("Member ID " + id + " has no role for shop ID " + role.getShopId());
            }
            if (existingRole.getAssigneeId() != role.getAssigneeId()) {
                throw new OurRuntime("Member ID " + id + " is not the assignee of the role for shop ID " + role.getShopId());
            }
            userRepository.removeRole(id, role.getShopId());
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeRole", e);
            throw new OurRuntime("removeRole: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("removeRole", e, id, role);
            throw new OurRuntime("removeRole: " + e.getMessage(), e);
        }
    }
    
    public boolean hasRole(int id, Role role) {
        try {
            LoggerService.logMethodExecution("hasRole", id, role);
            if (role == null) {
                throw new OurRuntime("Role cannot be null.");
            }
            validateMemberId(id);
            Role existingRole = userRepository.getRole(id, role.getShopId());
            if (existingRole == null) {
                throw new OurRuntime("Member ID " + id + " has no role for shop ID " + role.getShopId());
            }
            if (existingRole.getAssigneeId() != role.getAssigneeId()) {
                throw new OurRuntime("Member ID " + id + " is not the assignee of the role for shop ID " + role.getShopId());
            }
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("hasRole", e);
            throw new OurRuntime("hasRole: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("hasRole", e, id, role);
            throw new OurRuntime("hasRole: " + e.getMessage(), e);
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
     * * @throws OurArg
     * * @throws OurRuntime
     */
    public boolean addPermission(String token, int id, PermissionsEnum permission, int shopId) {
        try {
            LoggerService.logMethodExecution("addPermission", token, id, permission, shopId);
            int assigneeId = authTokenService.ValidateToken(token);
            if (permission == null) {
                throw new OurRuntime("Permission cannot be null.");
            }
            validateMemberId(id);
            Role role = userRepository.getRole(id, shopId);
            if (role == null) {
                throw new OurRuntime("Member ID " + id + " has no role for shop ID " + shopId);
            }
            if (role.getAssigneeId() != assigneeId) {
                throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + id + " in shop ID " + shopId);
            }
            userRepository.addPermission(id, permission, shopId);
            LoggerService.logMethodExecutionEnd("addPermission", true);
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("addPermission", e);
            throw new OurRuntime("addPermission: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("addPermission", e, token, id, permission, shopId);
            throw new OurRuntime("addPermission: " + e.getMessage(), e);
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
     * * @throws OurArg
     * * @throws OurRuntime
     */
    public boolean removePermission(String token, int id, PermissionsEnum permission, int shopId) {
        try {
            LoggerService.logMethodExecution("removePermission", token, id, permission, shopId);
            int assigneeId = authTokenService.ValidateToken(token);
            if (permission == null) {
                throw new OurRuntime("Permission cannot be null.");
            }
            validateMemberId(id);
            Role role = userRepository.getRole(id, shopId);
            if (role == null) {
                throw new OurRuntime("Member ID " + id + " has no role for shop ID " + shopId);
            }
            if (role.getAssigneeId() != assigneeId) {
                throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + id + " in shop ID " + shopId);
            }
            userRepository.removePermission(id, permission, shopId);
            LoggerService.logMethodExecutionEnd("removePermission", true);
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("removePermission", e);
            throw new OurRuntime("removePermission: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("removePermission", e, token, id, permission, shopId);
            throw new OurRuntime("removePermission: " + e.getMessage(), e);
        }
    }
    /**
     * Checks if a member has a specific permission in a store.
     * @param id The ID of the member.
     * @param permission The permission to check.
     * @param shopId The ID of the store.
     * @return true if the member has the permission, false otherwise.
     * 
     * * @throws OurArg
     * * @throws OurRuntime
     */
    public boolean hasPermission(int id, PermissionsEnum permission, int shopId) {
        try {
            LoggerService.logMethodExecution("hasPermission", id, permission, shopId);
            if (!userRepository.getUserMapping().containsKey(id)) {
                throw new OurArg("User with ID " + id + " doesn't exist.");
            }
            validateMemberId(id);
            User user = userRepository.getUserById(id);
            boolean result = ((Member) user).hasPermission(permission, shopId);
            LoggerService.logMethodExecutionEnd("hasPermission", result);
            return result;
        } catch (OurArg e) {
            LoggerService.logDebug("hasPermission", e);
            throw new OurArg("hasPermission: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("hasPermission", e, id, permission, shopId);
            throw new OurRuntime("hasPermission: " + e.getMessage(), e);
        }
    }
    

    /**
     * Retrieves the shopping cart items for a user by their ID.
     * @param userId The ID of the user whose shopping cart items are to be retrieved.
     * @return A HashMap containing the items in the user's shopping cart. shopId -> <itemId -> quantity>
     * @throws OurArg if the user ID is invalid or the user is not a member.
     * @throws RuntimeException if an error occurs while fetching the shopping cart items.
     */
    public HashMap<Integer, HashMap<Integer, Integer>> getUserShoppingCartItems(int userId) {
        try {
            LoggerService.logMethodExecution("getUserShoppingCartItems", userId);
            HashMap<Integer, HashMap<Integer, Integer>> cart = userRepository.getShoppingCartById(userId).getItems();
            LoggerService.logMethodExecutionEnd("getUserShoppingCartItems", cart);
            return cart;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getUserShoppingCartItems", e);
            throw new OurRuntime("getUserShoppingCartItems: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logError("getUserShoppingCartItems", e, userId);
            throw new OurArg("getUserShoppingCartItems: Invalid user ID " + userId, e);
        } catch (Exception e) {
            LoggerService.logError("getUserShoppingCartItems", e, userId);
            throw new OurRuntime("getUserShoppingCartItems: " + e.getMessage(), e);
        }
    }
    
    /**
     * Clears the shopping cart for a user by their ID.
     * @param userId The ID of the user whose shopping cart is to be cleared.
     * @throws OurArg if the user ID is invalid or the user is not a member.
     * @throws RuntimeException if an error occurs while clearing the shopping cart.
     */
    public void clearUserShoppingCart(int userId) {
        try {
            LoggerService.logMethodExecution("clearUserShoppingCart", userId);
            userRepository.getShoppingCartById(userId).clearCart();
            LoggerService.logMethodExecutionEndVoid("clearUserShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("clearUserShoppingCart", e);
            throw new OurRuntime("clearUserShoppingCart: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("clearUserShoppingCart", e);
            throw new OurArg("clearUserShoppingCart: Invalid user ID " + userId, e);

        } catch (Exception e) {
            LoggerService.logError("clearUserShoppingCart", e, userId);
            throw new OurRuntime("clearUserShoppingCart: " + e.getMessage(), e);
        }
    }
    

    /*
     * Restores the shopping cart for a user by their ID.
     * * @param userId The ID of the user whose shopping cart is to be restored.
     * * @param items A HashMap containing the items to be restored in the shopping cart.
     * * * @throws OurArg if the user ID is invalid or the user is not a member.
     * * * @throws RuntimeException if an error occurs while restoring the shopping cart.
     */
    public void restoreUserShoppingCart(int userId, HashMap<Integer, HashMap<Integer,Integer>> items){
        try {
            LoggerService.logMethodExecution("restoreUserShoppingCart", userId, items);
            userRepository.getShoppingCartById(userId).restoreCart(items);
            LoggerService.logMethodExecutionEndVoid("restoreUserShoppingCart");
        } 
        catch (OurRuntime e) {
            LoggerService.logDebug("restoreUserShoppingCart", e);
            throw new OurRuntime("restoreUserShoppingCart: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("restoreUserShoppingCart", e);
            throw new OurArg("restoreUserShoppingCart: Invalid user ID " + userId, e);
        } catch (Exception e) {
            LoggerService.logError("restoreUserShoppingCart", e, userId, items);
            throw new OurRuntime("Error restoring shopping cart for user ID " + userId + ": " + e.getMessage(), e);
        }
    }

    /* 
        * Retrieves the payment method for a user by their ID.
        * @param userId The ID of the user whose payment method is to be retrieved.
        * @return The PaymentMethod object associated with the user.
        * @throws OurArg if the user ID is invalid or the user is not a member.
        * @throws RuntimeException if an error occurs while fetching the payment method.
        */

    public PaymentMethod getUserPaymentMethod(int userId) {
        try {
            LoggerService.logMethodExecution("getUserPaymentMethod", userId);
            PaymentMethod paymentMethod = userRepository.getUserById(userId).getPaymentMethod();
            LoggerService.logMethodExecutionEnd("getUserPaymentMethod", paymentMethod);
            return paymentMethod;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getUserPaymentMethod", e);
            throw new OurRuntime("getUserPaymentMethod: " + e.getMessage(), e);
        } catch(OurArg e) {
            LoggerService.logDebug("getUserPaymentMethod", e);
            throw new OurArg("getUserPaymentMethod: Invalid user ID " + userId, e);
        } catch (Exception e) {
            LoggerService.logError("getUserPaymentMethod", e, userId);
            throw new OurRuntime("getUserPaymentMethod: " + e.getMessage(), e);
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
            int userId = authTokenService.ValidateToken(token);
            userRepository.addItemToShoppingCart(userId, shopId, itemId, quantity);
            LoggerService.logMethodExecutionEndVoid("addItemToShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("addItemToShoppingCart", e);
            throw new OurRuntime("addItemToShoppingCart: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("addItemToShoppingCart", e);
            throw new OurArg("addItemToShoppingCart: Invalid user ID " + token, e);
        } catch (Exception e) {
            LoggerService.logError("addItemToShoppingCart", e, token, shopId, itemId, quantity);
            throw new OurRuntime("addItemToShoppingCart: " + e.getMessage(), e);
        }
    }
    

    /*
     * Removes an item from the shopping cart for a user by their token.
     * @param token The token of the user whose shopping cart item is to be removed.
     * @param shopId The ID of the shop from which the item is to be removed.
     * @param itemId The ID of the item to be removed from the shopping cart.
     * * @throws OurArg if the user ID is invalid or the user is not a member.
     * * @throws OurRuntime if an error occurs while removing the item from the shopping cart.
     * * @throws Exception if an unexpected error occurs.
     * 
     */
    public void removeItemFromShoppingCart(String token, int shopId, int itemId) {
        try {
            LoggerService.logMethodExecution("removeItemFromShoppingCart", token, shopId, itemId);
            int userId = authTokenService.ValidateToken(token);
            userRepository.removeItemFromShoppingCart(userId, shopId, itemId);
            LoggerService.logMethodExecutionEndVoid("removeItemFromShoppingCart");
        } catch (OurArg e) {
            LoggerService.logDebug("removeItemFromShoppingCart", e);
            throw new OurArg("removeItemFromShoppingCart: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeItemFromShoppingCart", e);
            throw new OurRuntime("removeItemFromShoppingCart: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("removeItemFromShoppingCart", e, token, shopId, itemId);
            throw new OurRuntime("removeItemFromShoppingCart: " + e.getMessage(), e);
        }
    }
    
    /**
     * Updates the quantity of an item in the shopping cart for a user by their token.
     * @param token The token of the user whose shopping cart item quantity is to be updated.
     * @param shopId The ID of the shop where the item quantity is to be updated.
     * @param itemId The ID of the item whose quantity is to be updated.
     * @param quantity The new quantity of the item in the shopping cart.
     * 
     * * @throws OurArg
     * * @throws OurRuntime
     */
    public void updateItemQuantityInShoppingCart(String token, int shopId, int itemId, int quantity) {
        try {
            LoggerService.logMethodExecution("updateItemQuantityInShoppingCart", token, shopId, itemId, quantity);
            int userId = authTokenService.ValidateToken(token);
            userRepository.updateItemQuantityInShoppingCart(userId, shopId, itemId, quantity);
            LoggerService.logMethodExecutionEndVoid("updateItemQuantityInShoppingCart");
        } catch (OurArg e) {
            LoggerService.logDebug("updateItemQuantityInShoppingCart", e);
            throw new OurArg("updateItemQuantityInShoppingCart: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("updateItemQuantityInShoppingCart", e);
            throw new OurRuntime("updateItemQuantityInShoppingCart: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("updateItemQuantityInShoppingCart", e, token, shopId, itemId, quantity);
            throw new OurRuntime("updateItemQuantityInShoppingCart: " + e.getMessage(), e);
        }
    }
    

    /**
     * Clears the shopping cart for a user by their token.
     * @param token The token of the user whose shopping cart is to be cleared.
     * 
     * * @throws OurArg
     * * @throws OurRuntime
     */
    void clearShoppingCart(String token) {
        try {
            LoggerService.logMethodExecution("clearShoppingCart", token);
            int userId = authTokenService.ValidateToken(token);
            userRepository.clearShoppingCart(userId);
            LoggerService.logMethodExecutionEndVoid("clearShoppingCart");
        } catch (OurArg e) {
            LoggerService.logDebug("clearShoppingCart", e);
            throw new OurArg("clearShoppingCart: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("clearShoppingCart", e);
            throw new OurRuntime("clearShoppingCart: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("clearShoppingCart", e, token);
            throw new OurRuntime("clearShoppingCart: " + e.getMessage(), e);
        }
    }
    

   /*
        * Retrieves the items in the shopping basket for a user by their token and shop ID.
        * @param token The token of the user whose shopping basket items are to be retrieved.
        * @param shopId The ID of the shop where the basket items are to be retrieved.
        * @return A HashMap containing the items in the user's shopping basket. itemId -> quantity
        * 
        * * @throws OurArg
        * * @throws OurRuntime
        */
    public Map<Integer, Integer> getBasketItems(String token, int shopId) {
        try {
            LoggerService.logMethodExecution("getBasketItems", token, shopId);
            int userId = authTokenService.ValidateToken(token);
            Map<Integer, Integer> items = userRepository.getBasket(userId, shopId);
            LoggerService.logMethodExecutionEnd("getBasketItems", items);
            return items;
        } catch (OurArg e) {
            LoggerService.logDebug("getBasketItems", e);
            throw new OurArg("getBasketItems: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("getBasketItems", e);
            throw new OurRuntime("getBasketItems: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getBasketItems", e, token, shopId);
            throw new OurRuntime("getBasketItems: " + e.getMessage(), e);
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
            int userId = authTokenService.ValidateToken(token);
            userRepository.createBasket(userId, shopId);
            LoggerService.logMethodExecutionEndVoid("addBasket");
        } catch (OurArg e) {
            LoggerService.logDebug("addBasket", e);
            throw new OurArg("addBasket: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("addBasket", e);
            throw new OurRuntime("addBasket: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("addBasket", e, token, shopId);
            throw new OurRuntime("addBasket: " + e.getMessage(), e);
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
            throw new OurRuntime(errorMsg.toString().trim());
        }
    }


    /**
     * Sets the payment method for a user by their token and shop ID.
     * @param token The token of the user whose payment method is to be set.
     * @param paymentMethod The PaymentMethod object to be set for the user.
     * @param shopId The ID of the shop where the payment method is being set.
     * 
     * * @throws OurRuntime
     * * @throws OurArg
     */
    public void setPaymentMethod(String token, PaymentMethod paymentMethod, int shopId) {
        try {
            LoggerService.logMethodExecution("setPaymentMethod", token, paymentMethod);
            int userId = authTokenService.ValidateToken(token);
    
            if (paymentMethod == null) {
                throw new OurRuntime("Payment method cannot be null.");
            }
    
            userRepository.setPaymentMethod(userId, shopId, paymentMethod);
            LoggerService.logMethodExecutionEndVoid("setPaymentMethod");
        } catch (OurArg e) {
            LoggerService.logDebug("setPaymentMethod", e);
            throw new OurArg("setPaymentMethod: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("setPaymentMethod", e);
            throw new OurRuntime("setPaymentMethod: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("setPaymentMethod", e, token, paymentMethod);
            throw new OurRuntime("setPaymentMethod: " + e.getMessage(), e);
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
     * * @throws OurRuntime, 
     *   @throws OurArg
     */
    public boolean pay(String token, int shopId, double payment) {
        try {
            LoggerService.logMethodExecution("pay", token, shopId, payment);
            int userId = authTokenService.ValidateToken(token);
            userRepository.pay(userId, shopId, payment);
            LoggerService.logMethodExecutionEnd("pay", true);
            return true;
        } catch (OurArg e) {
            LoggerService.logDebug("pay", e);
            throw new OurArg("pay: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("pay", e);
            throw new OurRuntime("pay: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("pay", e, token, shopId, payment);
            throw new OurRuntime("pay: " + e.getMessage(), e);
        }
    }
    

    //NO API ENDPOINT!
    public boolean refundPaymentAuto(String token, int shopId, double payment) {
        try {
            LoggerService.logMethodExecution("refundPaymentAuto", token, shopId, payment);
            int userId = authTokenService.ValidateToken(token);
            userRepository.refund(userId, shopId, payment);
            LoggerService.logMethodExecutionEnd("refundPaymentAuto", true);
            return true;
        } catch (OurArg e) {
            LoggerService.logDebug("refundPaymentAuto", e);
            throw new OurArg("refundPaymentAuto: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("refundPaymentAuto", e);
            throw new OurRuntime("refundPaymentAuto: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("refundPaymentAuto", e, token, shopId, payment);
            throw new OurRuntime("refundPaymentAuto: " + e.getMessage(), e);
        }
    }
    

    public boolean refundPaymentByStoreEmployee(String token, int userId, int shopId, double payment) {
        try {
            LoggerService.logMethodExecution("refundPaymentByStoreEmployee", token, shopId, payment);
            int initiatingUserId = authTokenService.ValidateToken(token);
            
            if (userRepository.getRole(initiatingUserId, shopId) == null) {
                throw new OurRuntime("Member ID " + initiatingUserId + " has no role for shop ID " + shopId);
            }
    
            userRepository.refund(userId, shopId, payment);
            LoggerService.logMethodExecutionEnd("refundPaymentByStoreEmployee", true);
            return true;
            
        } catch (OurArg e) {
            LoggerService.logDebug("refundPaymentByStoreEmployee", e);
            throw new OurArg("refundPaymentByStoreEmployee: " + e.getMessage(), e);
        } catch (OurRuntime e) {
            LoggerService.logDebug("refundPaymentByStoreEmployee", e);
            throw new OurRuntime("refundPaymentByStoreEmployee: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("refundPaymentByStoreEmployee", e, token, shopId, payment);
            throw new OurRuntime("refundPaymentByStoreEmployee: " + e.getMessage(), e);
        }
    }
    
}
