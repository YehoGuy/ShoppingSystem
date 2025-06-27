package com.example.app.ApplicationLayer.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.app.ApplicationLayer.AuthTokenService;
import com.example.app.ApplicationLayer.LoggerService;
import com.example.app.ApplicationLayer.NotificationService;
import com.example.app.ApplicationLayer.OurArg;
import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.DomainLayer.IUserRepository;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.Notification;
import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.Bid;
import com.example.app.DomainLayer.Purchase.BidReciept;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.DomainLayer.User;
import com.example.app.InfrastructureLayer.PasswordEncoderUtil;

import jakarta.validation.constraints.Min;

@Service
public class UserService {

    private final IUserRepository userRepository;

    private AuthTokenService authTokenService;
    private NotificationService notificationService;

    private PasswordEncoderUtil passwordEncoder;

    public UserService(IUserRepository userRepository,
            AuthTokenService authTokenService, NotificationService notificationService) {
        this.passwordEncoder = new PasswordEncoderUtil();
        this.userRepository = userRepository;
        this.authTokenService = authTokenService;
        this.notificationService = notificationService;
        this.notificationService.setService(this);
    }

    public boolean isAdmin(Integer id) {
        return userRepository.isAdmin(id);
    }

    public void setEncoderToTest(boolean isTest) {
        passwordEncoder.setIsTest(isTest); // Set the encoder to test mode
    }

    public void makeAdmin(String token, Integer id) {
        // userId is the token's user id. and id is the id of the user to make admin
        LoggerService.logMethodExecution("makeAdmin", token, id);
        try {
            int userId = authTokenService.ValidateToken(token);
            if (isSuspended(userId)) {
                throw new OurRuntime("the user is suspended");
            }
            if (isAdmin(userId)) {
                if (id >= 0)
                    userRepository.addAdmin(id);
                else
                    throw new OurArg("the id of the user to make admin is illegal");
            } else
                throw new OurRuntime("only admins can make admins");
            LoggerService.logMethodExecutionEndVoid("makeAdmin");
        } catch (OurArg e) {
            LoggerService.logDebug("makeAdmin", e);
            throw new OurArg("makeAdmin: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurRuntime e) {
            LoggerService.logError("makeAdmin", e, token, id);
            throw new OurRuntime("makeAdmin: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("makeAdmin", e, token, id);
            throw new OurRuntime("makeAdmin: " + e);
        }
    }

    public void removeAdmin(String token, Integer id) {
        LoggerService.logMethodExecution("removeAdmin", token, id);
        try {
            int userId = authTokenService.ValidateToken(token);
            if (isSuspended(userId)) {
                LoggerService.logDebug("removeAdmin", new OurRuntime("Member ID " + userId + " is suspended."));
                throw new OurRuntime("the user is suspended");
            }
            if (isAdmin(userId)) {
                if (id >= 0) {
                    userRepository.removeAdmin(id);
                    removedAppointment(id, "Admin", null);
                } else {
                    LoggerService.logDebug("removeAdmin", new OurArg("the id of the user to make admin is illegal"));
                    throw new OurArg("the id of the user to make admin is illegal");
                }
            } else
                throw new OurRuntime("only admins can remove admins");
            LoggerService.logMethodExecutionEndVoid("removeAdmin");
        } catch (OurArg e) {
            LoggerService.logDebug("removeAdmin", e);
            throw new OurArg("removeAdmin: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurRuntime e) {
            LoggerService.logError("removeAdmin", e, token, id);
            throw new OurRuntime("removeAdmin: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removeAdmin", e, token, id);
            throw new OurRuntime("removeAdmin: " + e.getMessage(), e); // Rethrow the custom exception
        }
    }

    public List<Integer> getAllAdmins(String token) {
        LoggerService.logMethodExecution("getAllAdmins", token);
        try {
            List<Integer> lst;
            int userId = authTokenService.ValidateToken(token);
            if (isAdmin(userId)) {
                lst = userRepository.getAllAdmins();
            } else
                throw new OurRuntime("only admins can get ids of all admins");
            LoggerService.logMethodExecutionEnd("getAllAdmins", lst);
            return lst;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getAllAdmins", e);
            throw new OurRuntime("getAllAdmins: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("getAllAdmins", e);
            throw new OurArg("getAllAdmins: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("getAllAdmins", e, token);
            throw new OurRuntime("getAllAdmins: " + e.getMessage(), e);
        }
    }

    public User getUserById(int id) {
        try {
            LoggerService.logMethodExecution("getUserById", id);
            User user = userRepository.getUserById(id);
            LoggerService.logMethodExecutionEnd("getUserById", user);
            return user;
        } catch (OurArg e) {
            LoggerService.logDebug("getUserById", e);
            throw new OurArg("getUserById: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurRuntime e) {
            LoggerService.logError("getUserById", e, id);
            throw new OurRuntime("getUserById: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("getUserById", e, id);
            throw new OurRuntime("getUserById: " + e.getMessage(), e); // Rethrow the custom exception
        }
    }

    public List<Member> getAllMembers() {
        try {
            LoggerService.logMethodExecution("getAllUsers");
            List<Member> users = userRepository.getAllMembers();
            LoggerService.logMethodExecutionEnd("getAllUsers", users);
            return users;
        } catch (OurArg e) {
            LoggerService.logDebug("getAllUsers", e);
            throw new OurArg("getAllUsers: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurRuntime e) {
            LoggerService.logError("getAllUsers", e);
            throw new OurRuntime("getAllUsers: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("getAllUsers", e);
            throw new OurRuntime("getAllUsers: " + e.getMessage(), e); // Rethrow the custom exception
        }
    }

    public String addMember(String username, String password, String email, String phoneNumber, String address) {
        try {
            isValidDetails(username, password, email, phoneNumber); // Validate the input details
            if (userRepository.isUsernameAndPasswordValid(username, password) != -1) {
                LoggerService.logError("addMember", new OurArg("Username is already taken."));
                throw new OurArg("Username is already taken.");
            }
            String rawPassword = password;
            String encodedPassword = passwordEncoder.encode(password); // Encode the password using the
                                                                       // PasswordEncoderUtil
            LoggerService.logMethodExecution("addMember", username, password, email, phoneNumber, address);
            int userId = userRepository.addMember(username, encodedPassword, email, phoneNumber, address);
            String token = authTokenService.Login(username, rawPassword, userId);
            LoggerService.logMethodExecutionEndVoid("addMember");
            return token; // Return the generated token
        } catch (OurRuntime e) {
            LoggerService.logDebug("addMember", e);
            throw new OurRuntime("addMember: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("addMember", e);
            throw new OurArg("addMember: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("addMember", e, username, password, email, phoneNumber, address);
            throw new OurRuntime("addMember: " + e.getMessage(), e);
        }

    }

    public void updateMemberUsername(String token, String username) {
        try {
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            LoggerService.logMethodExecution("updateMemberUsername", id, username);
            if (isSuspended(id)) {
                throw new OurRuntime("the user is suspended");
            }
            validateMemberId(id);
            isValidUsername(username); // Validate the username
            userRepository.updateMemberUsername(id, username);
            LoggerService.logMethodExecutionEndVoid("updateMemberUsername");
        } catch (OurRuntime e) {
            LoggerService.logDebug("updateMemberUsername", e);
            throw new OurRuntime("updateMemberUsername: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("updateMemberUsername", e);
            throw new OurArg("updateMemberUsername: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("updateMemberUsername", e, username);
            throw new OurRuntime("updateMemberUsername: " + e.getMessage(), e);
        }
    }

    public void updateMemberPassword(String token, String password) {
        try {
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            LoggerService.logMethodExecution("updateMemberPassword", id, password);
            if (isSuspended(id)) {
                throw new OurRuntime("the user is suspended");
            }
            isValidPassword(password); // Validate the password
            password = passwordEncoder.encode(password); // Encode the password using the PasswordEncoderUtil
            validateMemberId(id);
            userRepository.updateMemberPassword(id, password);
            LoggerService.logMethodExecutionEndVoid("updateMemberPassword");
        } catch (OurRuntime e) {
            LoggerService.logDebug("updateMemberPassword", e);
            throw new OurRuntime("updateMemberPassword: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("updateMemberPassword", e);
            throw new OurArg("updateMemberPassword: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("updateMemberPassword", e, password);
            throw new OurRuntime("updateMemberPassword: " + e.getMessage(), e);
        }
    }

    public void updateMemberEmail(String token, String email) {
        try {
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            LoggerService.logMethodExecution("updateMemberEmail", id, email);
            validateMemberId(id);
            if (isSuspended(id)) {
                LoggerService.logDebug("updateMemberEmail", new OurRuntime("Member ID " + id + " is suspended."));
                throw new OurRuntime("the user is suspended");
            }
            isValidEmail(email); // Validate the email
            userRepository.updateMemberEmail(id, email);
            LoggerService.logMethodExecutionEndVoid("updateMemberEmail");
        } catch (OurRuntime e) {
            LoggerService.logDebug("updateMemberEmail", e);
            throw new OurRuntime("updateMemberEmail: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("updateMemberEmail", e);
            throw new OurArg("updateMemberEmail: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("updateMemberEmail", e, email);
            throw new OurRuntime("updateMemberEmail: " + e.getMessage(), e);
        }
    }

    public void updateMemberPhoneNumber(String token, String phoneNumber) {
        try {
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            LoggerService.logMethodExecution("updateMemberPhoneNumber", id, phoneNumber);
            validateMemberId(id);
            if (isSuspended(id)) {
                LoggerService.logDebug("updateMemberPhoneNumber", new OurRuntime("Member ID " + id + " is suspended."));
                throw new OurRuntime("the user is suspended");
            }
            isValidPhoneNumber(phoneNumber); // Validate the phone number
            userRepository.updateMemberPhoneNumber(id, phoneNumber);
            LoggerService.logMethodExecutionEndVoid("updateMemberPhoneNumber");
        } catch (OurRuntime e) {
            LoggerService.logDebug("updateMemberPhoneNumber", e);
            throw new OurRuntime("updateMemberPhoneNumber: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("updateMemberPhoneNumber", e);
            throw new OurArg("updateMemberPhoneNumber: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("updateMemberPhoneNumber", e, phoneNumber);
            throw new OurRuntime("updateMemberPhoneNumber: " + e.getMessage(), e);
        }
    }

    public void updateMemberAddress(String token, String city, String street, int apartmentNumber, String postalCode) {
        try {
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            LoggerService.logMethodExecution("updateMemberAddress", id, city, street, apartmentNumber, postalCode);
            validateMemberId(id);
            if (isSuspended(id)) {
                throw new OurRuntime("the user is suspended");
            }
            userRepository.updateMemberAddress(id, city, street, apartmentNumber, postalCode);
            LoggerService.logMethodExecutionEndVoid("updateMemberAddress");
        } catch (OurRuntime e) {
            LoggerService.logDebug("updateMemberAddress", e);
            throw new OurRuntime("updateMemberAddress: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("updateMemberAddress", e);
            throw new OurArg("updateMemberAddress: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("updateMemberAddress", e, city, street, apartmentNumber, postalCode);
            throw new OurRuntime("updateMemberAddress: " + e.getMessage(), e);
        }
    }

    public void validateMemberId(int id) {
        LoggerService.logMethodExecution("validateMemberId", id);
        if (id <= 0) {
            LoggerService.logDebug("validateMemberId", new OurArg("Invalid user ID: " + id));
            throw new OurArg("Invalid user ID: " + id);
        }
        if (!userRepository.getUserMapping().containsKey(id)) {
            LoggerService.logDebug("validateMemberId", new OurArg("User with ID " + id + " doesn't exist."));
            throw new OurArg("User with ID " + id + " doesn't exist.");
        }
        User user = userRepository.getUserById(id);
        if (!(user instanceof Member)) {
            LoggerService.logDebug("validateMemberId", new OurArg("User with ID " + id + " is not a member."));
            throw new OurArg("User with ID " + id + " is not a member.");
        }
        LoggerService.logMethodExecutionEndVoid("validateMemberId");
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
        } catch (OurRuntime e) {
            LoggerService.logDebug("loginAsGuest", e);
            throw new OurRuntime("loginAsGuest: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("loginAsGuest", e);
            throw new OurArg("loginAsGuest: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("loginAsGuest", e);
            throw new OurRuntime("loginAsGuest: " + e.getMessage(), e);
        }
    }

    public String loginAsMember(String username, String password, String token_if_guest) {
        LoggerService.logMethodExecution("loginAsMember", username, "****", token_if_guest);
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
                Member member = userRepository.getMemberById(loginAsMember_id);
                member.setConnected(true);
                if (token_if_guest == null || token_if_guest.equals("") || token_if_guest.isEmpty()) {
                    token = authTokenService.Login(username, password, loginAsMember_id);
                    
                    int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
                    //if (isSuspended(id)) 
                        //{throw new OurRuntime("the user is suspended");}

                    LoggerService.logMethodExecutionEnd("loginAsMember", loginAsMember_id);
                    return token; // Return the ID of the logged-in member
                } else {
                    int id = authTokenService.ValidateToken(token_if_guest); // guest id
                    // merge the guest cart with the member cart
                    User guest = userRepository.getUserById(id);
                    member.mergeShoppingCart(guest.getShoppingCart());
                    // remove the guest user from the data
                    userRepository.removeUserById(id);
                    token = authTokenService.Login(username, password, loginAsMember_id);
                    LoggerService.logMethodExecutionEnd("loginAsMember", loginAsMember_id);
                    return token;
                }
            } else {
                LoggerService.logError("loginAsMember", new OurArg("Invalid username or password."));
                throw new OurArg("Invalid username or password.");
            }
        } catch (OurRuntime e) {
            LoggerService.logDebug("loginAsMember", e);
            throw new OurRuntime("loginAsMember: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("loginAsMember", e);
            throw new OurArg("loginAsMember: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("loginAsMember", e, username, password, token_if_guest);
            throw new OurRuntime("loginAsMember: " + e.getMessage(), e);
        }
    }

    /*
     * public int loginAsMember(String username, String password, int id_if_guest) {
     * try {
     * if (username == null || password == null) {
     * throw new OurArg("Username and password cannot be null.");
     * }
     * if (username.isEmpty() || password.isEmpty()) {
     * throw new OurArg("Username and password cannot be empty.");
     * }
     * int loginAsMember_id = userRepository.isUsernameAndPasswordValid(username,
     * password);
     * if (loginAsMember_id>0)//valid login attempt
     * {
     * if (id_if_guest == -1) { //if the user is not a guest,its his initial logging
     * in and we just return the id of the member
     * return loginAsMember_id; // Return the ID of the logged-in member
     * }
     * else if (userRepository.isGuestById(id_if_guest)){ //we ensure that the given
     * id matches a guest in the data!
     * //we merge the guest cart with the member cart
     * User member = userRepository.getUserById(loginAsMember_id);
     * User guest = userRepository.getUserById(id_if_guest);
     * member.mergeShoppingCart(guest.getShoppingCart());
     * //we remove the guest user from the data
     * userRepository.removeUserById(id_if_guest);
     * // Return the ID of the logged-in member
     * return loginAsMember_id;
     * 
     * }
     * else {
     * throw new
     * OurArg("The given id does not match a guest in the data. probably it is a member id!"
     * );
     * }
     * 
     * }
     * 
     * 
     * }
     * catch (Exception e) {
     * return -1; // Indicate failure to log in as a member
     * }
     * }
     * 
     */

    // public String signUp(String username, String password, String email, String
    // phoneNumber, String address) {
    // password = passwordEncoder.encode(password); // Encode the password using the
    // PasswordEncoderUtil
    // try {
    // if (userRepository.isUsernameTaken(username)) {
    // LoggerService.logError("signUp", new OurArg("Username is already taken."));
    // throw new OurArg("Username is already taken.");
    // }
    // if (!email.contains("@")) {
    // throw new OurArg("Invalid email format.");
    // }
    // String token = authTokenService.generateAuthToken(username); // Generate a
    // token for the member
    // LoggerService.logMethodExecution("signUp", username, password, email,
    // phoneNumber, address);
    // userRepository.addMember(username, password, email, phoneNumber, address);
    // return token;
    // } catch (Exception e) {
    // LoggerService.logError("signUp", e, username, password, email, phoneNumber,
    // address);
    // throw new OurRuntime("Error signing up: " + e.getMessage(), e);
    // }
    // }

    public String logout(String token) {
        try {
            LoggerService.logMethodExecution("logout", token);
            int id = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (userRepository.isGuestById(id)) {
                userRepository.removeUserById(id); // Remove guest from the repository
            } else {
                Member member = userRepository.getMemberById(id);
                member.setConnected(false); // Set the member as disconnected
            }
            authTokenService.Logout(token); // Logout the user by removing the token

            String logoutToken = loginAsGuest();
            LoggerService.logMethodExecutionEnd("logout", logoutToken);
            return logoutToken; // Generate a new guest token
        } catch (OurRuntime e) {
            LoggerService.logDebug("logout", e);
            return null;
        } catch (OurArg e) {
            LoggerService.logDebug("logout", e);
            return null;
        } catch (Exception e) {
            LoggerService.logError("logout", e, token);
            return null;
        }
    }

    public HashMap<Integer, PermissionsEnum[]> getPermitionsByShop(String token, int shopId) {
        try {

            LoggerService.logMethodExecution("getPermitionsByShop", shopId);
            // int id = authTokenService.ValidateToken(token); // Validate the token and get
            // the user ID
            // if (!userRepository.isOwner(id, shopId)) {
            // throw new OurArg("Member ID " + token + " is not an owner of shop ID " +
            // shopId);
            // }

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
        } catch (OurRuntime e) {
            LoggerService.logDebug("getPermitionsByShop", e);
            throw new OurRuntime("getPermitionsByShop: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("getPermitionsByShop", e);
            throw new OurArg("getPermitionsByShop: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("getPermitionsByShop", e, shopId);
            throw new OurRuntime("getPermitionsByShop: " + e.getMessage(), e);
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
     *                    * @throws IllegalArgumentException
     *                    * @throws OurRuntime
     */
    public void changePermissions(String token, int memberId, int shopId, PermissionsEnum[] permissions) {
        try {
            LoggerService.logMethodExecution("changePermissions", token, memberId, shopId, permissions);
            int assigneeId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(assigneeId)) {
                throw new OurRuntime("the user is suspended");
            }
            if (!userRepository.isOwner(assigneeId, shopId)) {
                LoggerService.logDebug("changePermissions",
                        new OurRuntime("Member ID " + assigneeId + " is not an owner of shop ID " + shopId));
                throw new OurRuntime("Member ID " + assigneeId + " is not an owner of shop ID " + shopId);
            }
            Member member = userRepository.getMemberById(memberId);
            for (Role role : member.getRoles()) {
                if (role.getShopId() == shopId) {
                    if (role.getAssigneeId() != assigneeId) {
                        LoggerService.logDebug("changePermissions", new OurRuntime("Member ID " + assigneeId
                                + " is not the assignee of member ID " + memberId + " in shop ID " + shopId));
                        throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + memberId
                                + " in shop ID " + shopId);
                    }
                    for (PermissionsEnum permission : permissions) {
                        if (permission == null) {
                            LoggerService.logDebug("changePermissions", new OurRuntime("Permission cannot be null."));
                            throw new OurRuntime("Permission cannot be null.");
                        }
                        if (permission == PermissionsEnum.closeShop) {
                            LoggerService.logDebug("changePermissions",
                                    new OurRuntime("Permission closeShop cannot be changed."));
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
            throw new OurRuntime("changePermissions: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("changePermissions", e);
            throw new OurArg("changePermissions: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("changePermissions", e, memberId, shopId, permissions);
            throw new OurRuntime("changePermissions: " + e.getMessage(), e);
        }
    }

    /**
     * Assigns a member as a manager of a store with the specified permissions.
     * 
     * @param token       The token of the user making the assignment.
     * @param memberId    The ID of the member to be assigned as a manager.
     * @param shopId      The ID of the store where the member will be assigned as a
     *                    manager.
     * @param permissions The permissions to be granted to the manager.
     * 
     *                    * @throws IllegalArgumentException
     *                    * @throws OurRuntime
     */
    public void makeManagerOfStore(String token, int memberId, int shopId, PermissionsEnum[] permissions) {
        try {
            LoggerService.logMethodExecution("makeManagerOfStore", token, shopId, permissions);
            int assignee = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(assignee)) {
                throw new OurRuntime("the user is suspended");
            }
            if (!userRepository.isOwner(assignee, shopId)) {
                LoggerService.logDebug("makeManagerOfStore",
                        new OurRuntime("Member ID " + assignee + " is not an owner of shop ID " + shopId));
                throw new OurRuntime("Member ID " + assignee + " is not an owner of shop ID " + shopId);
            }
            Role role = new Role(assignee, shopId, permissions);
            userRepository.addRoleToPending(memberId, role);
            LoggerService.logMethodExecutionEndVoid("makeManagerOfStore");
        } catch (OurRuntime e) {
            LoggerService.logDebug("makeManagerOfStore", e);
            throw new OurRuntime("makeManagerOfStore: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("makeManagerOfStore", e);
            throw new OurArg("makeManagerOfStore: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("makeManagerOfStore", e, token, shopId, permissions);
            throw new OurRuntime("makeManagerOfStore: " + e.getMessage(), e);
        }
    }

    /**
     * Removes a manager from a store.
     * 
     * @param token     The token of the user making the removal.
     * @param managerId The ID of the manager to be removed.
     * @param shopId    The ID of the store from which the manager will be removed.
     * 
     *                  * @throws IllegalArgumentException
     *                  * @throws OurRuntime
     */
    public void removeManagerFromStore(String token, int managerId, int shopId) {
        try {
            LoggerService.logMethodExecution("removeManagerOfStore", token, shopId);
            int assigneeId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(assigneeId)) {
                LoggerService.logDebug("removeManagerOfStore",
                        new OurRuntime("Member ID " + assigneeId + " is suspended."));
                throw new OurRuntime("the user is suspended");
            }
            Role role = userRepository.getRole(managerId, shopId);
            if (role == null) {
                LoggerService.logDebug("removeManagerOfStore",
                        new OurRuntime("Member ID " + managerId + " is not a manager of shop ID " + shopId));
                throw new OurRuntime("Member ID " + managerId + " is not a manager of shop ID " + shopId);
            }
            if (role.getAssigneeId() != assigneeId) {
                LoggerService.logDebug("removeManagerOfStore", new OurRuntime("Member ID " + assigneeId
                        + " is not the assignee of member ID " + managerId + " in shop ID " + shopId));
                throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + managerId
                        + " in shop ID " + shopId);
            }
            userRepository.removeRole(managerId, shopId);
            removedAppointment(managerId, "Manager", shopId);
            LoggerService.logMethodExecutionEndVoid("removeManagerOfStore");
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeManagerOfStore", e);
            throw new OurRuntime("removeManagerOfStore: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("removeManagerOfStore", e);
            throw new OurArg("removeManagerOfStore: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removeManagerOfStore", e, token, shopId);
            throw new OurRuntime("removeManagerOfStore: " + e.getMessage(), e);
        }
    }

    /**
     * Removes an owner from a store.
     * 
     * @param token    The token of the user making the removal.
     * @param memberId The ID of the owner to be removed.
     * @param shopId   The ID of the store from which the owner will be removed.
     * 
     *                 * * @throws IllegalArgumentException
     *                 * * @throws OurRuntime
     */
    public void removeOwnerFromStore(String token, int memberId, int shopId) {
        try {
            LoggerService.logMethodExecution("removeOwnerFromStore", token, shopId);
            Role role = userRepository.getRole(memberId, shopId);
            if (!role.isOwner()) {
                LoggerService.logDebug("removeOwnerFromStore",
                        new OurRuntime("Member ID " + memberId + " is not an owner of shop ID " + shopId));
                throw new OurRuntime("Member ID " + memberId + " is not an owner of shop ID " + shopId);
            }
            int assigneeId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(assigneeId)) {
                throw new OurRuntime("the user is suspended");
            }
            if (role.getAssigneeId() != assigneeId) {
                LoggerService.logDebug("removeOwnerFromStore", new OurRuntime("Member ID " + assigneeId
                        + " is not the assignee of member ID " + memberId + " in shop ID " + shopId));
                throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + memberId
                        + " in shop ID " + shopId);
            }
            userRepository.removeRole(memberId, shopId);
            removeAllAssigned(memberId, shopId); // Remove all assigned roles for the member
            removedAppointment(memberId, "Owner", shopId);
            LoggerService.logMethodExecutionEndVoid("removeOwnerFromStore");
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeOwnerFromStore", e);
            throw new OurRuntime("removeOwnerFromStore: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("removeOwnerFromStore", e);
            throw new OurArg("removeOwnerFromStore: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removeOwnerFromStore", e, token, shopId);
            throw new OurRuntime("removeOwnerFromStore: " + e.getMessage(), e);
        }
    }

    public void removeAllAssigned(int assignee, int shopId) {
        try {
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
            throw new OurRuntime("removeAllAssigned: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("removeAllAssigned", e);
            throw new OurArg("removeAllAssigned: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removeAllAssigned", e, assignee, shopId);
            throw new OurRuntime("removeAllAssigned: " + e.getMessage(), e);
        }
    }

    /**
     * Assigns a member as a store owner.
     * 
     * @param token    The token of the user making the assignment.
     * @param memberId The ID of the member to be assigned as an owner.
     * @param shopId   The ID of the store where the member will be assigned as an
     *                 owner.
     * 
     *                 * @throws IllegalArgumentException
     *                 * @throws OurRuntime
     */
    public void makeStoreOwner(String token, int memberId, int shopId) {
        try {
            LoggerService.logMethodExecution("makeStoreOwner", token, shopId);
            int assigneeId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(assigneeId)) {
                LoggerService.logDebug("makeStoreOwner", new OurRuntime("Member ID " + assigneeId + " is suspended."));
                throw new OurRuntime("the user is suspended");
            }
            if (!userRepository.isOwner(assigneeId, shopId)) {
                LoggerService.logDebug("makeStoreOwner",
                        new OurRuntime("Member ID " + assigneeId + " is not an owner of shop ID " + shopId));
                throw new OurRuntime("Member ID " + assigneeId + " is not an owner of shop ID " + shopId);
            }
            if (userRepository.getMemberById(memberId) == null) {
                LoggerService.logDebug("makeStoreOwner", new OurRuntime("Member ID " + memberId + " does not exist."));
                throw new OurRuntime("Member ID " + memberId + " does not exist.");
            }
            Role role = new Role(assigneeId, shopId, null);
            role.setOwnersPermissions();
            userRepository.addRoleToPending(memberId, role); // Add the role to the member
            LoggerService.logMethodExecutionEndVoid("makeStoreOwner");
        } catch (OurRuntime e) {
            LoggerService.logDebug("makeStoreOwner", e);
            throw new OurRuntime("makeStoreOwner: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("makeStoreOwner", e);
            throw new OurArg("makeStoreOwner: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("makeStoreOwner", e, token, shopId);
            throw new OurRuntime("makeStoreOwner: " + e.getMessage(), e);
        }
    }

    /**
     * Accepts a pending role for a member.
     * 
     * @param token  The token of the user accepting the role.
     * @param shopId The ID of the store where the role is being accepted.
     * 
     *               * @throws IllegalArgumentException
     *               * @throws OurRuntime
     */
    public void acceptRole(String token, int shopId) {
        try {
            LoggerService.logMethodExecution("acceptRole", token, shopId);
            int memberId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(memberId)) {
                throw new OurRuntime("the user is suspended");
            }
            Role role = userRepository.getPendingRole(memberId, shopId);
            if (role == null) {
                LoggerService.logDebug("acceptRole",
                        new OurRuntime("Member ID " + memberId + " has no pending role for shop ID " + shopId));
                throw new OurRuntime("Member ID " + memberId + " has no pending role for shop ID " + shopId);
            }
            userRepository.acceptRole(memberId, role); // Accept the role for the member
            LoggerService.logMethodExecutionEndVoid("acceptRole");
        } catch (OurRuntime e) {
            LoggerService.logDebug("acceptRole", e);
            throw new OurRuntime("acceptRole: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("acceptRole", e);
            throw new OurArg("acceptRole: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("acceptRole", e, token, shopId);
            throw new OurRuntime("acceptRole: " + e.getMessage(), e);
        }
    }

    public void declineRole(String token, int shopId) {
        try {
            LoggerService.logMethodExecution("declineRole", token, shopId);
            int memberId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(memberId)) {
                throw new OurRuntime("the user is suspended");
            }
            Role role = userRepository.getPendingRole(memberId, shopId);
            if (role == null) {
                LoggerService.logDebug("declineRole",
                        new OurRuntime("Member ID " + memberId + " has no pending role for shop ID " + shopId));
                throw new OurRuntime("Member ID " + memberId + " has no pending role for shop ID " + shopId);
            }
            userRepository.declineRole(memberId, role); // Accept the role for the member
            LoggerService.logMethodExecutionEndVoid("declineRole");
        } catch (OurRuntime e) {
            LoggerService.logDebug("declineRole", e);
            throw new OurRuntime("declineRole: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("declineRole", e);
            throw new OurArg("EdeclineRole: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("declineRole", e, token, shopId);
            throw new OurRuntime("declineRole: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a role to a member.
     * 
     * @param memberId The ID of the member to whom the role is being added.
     * @param role     The role to be added to the member.
     * 
     *                 * @return true if the role was added successfully, false
     *                 otherwise.
     * 
     *                 * @throws IllegalArgumentException
     *                 * @throws OurRuntime
     */
    public boolean addRole(int memberId, Role role) {
        try {
            LoggerService.logMethodExecution("addRole", memberId, role);
            validateMemberId(memberId);
            if (isSuspended(memberId)) {
                LoggerService.logDebug("addRole", new OurRuntime("Member ID " + memberId + " is suspended."));
                throw new OurRuntime("the user is suspended");
            }
            userRepository.addRoleToPending(memberId, role); // Add the role to the member
            LoggerService.logMethodExecutionEnd("addRole", true);
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("addRole", e);
            throw new OurRuntime("addRole" + e.getMessage()); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("addRole", e);
            throw new OurArg("addRole" + e.getMessage()); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("addRole", e, memberId, role);
            throw new OurRuntime("addRole: " + e.getMessage(), e); // Indicate failure to add role
        }
    }

    public boolean addFounderRole(int memberId, Role role, int shopId) {
        try {
            LoggerService.logMethodExecution("addFounderRole", memberId, role, shopId);
            validateMemberId(memberId);
            if (isSuspended(memberId)) {
                LoggerService.logDebug("addFounderRole", new OurRuntime("Member ID " + memberId + " is suspended."));
                throw new OurRuntime("the user is suspended");
            }
            if (role == null) {
                LoggerService.logDebug("addFounderRole", new OurRuntime("Role cannot be null."));
                throw new OurRuntime("Role cannot be null.");
            }
            Member member = userRepository.getMemberById(memberId);
            if (member == null) {
                LoggerService.logDebug("addFounderRole", new OurRuntime("Member ID " + memberId + " does not exist."));
                throw new OurRuntime("Member ID " + memberId + " does not exist.");
            }
            member.addRole(role); // Add the role to the member
            userRepository.updateUserInDB(member); 
            LoggerService.logMethodExecutionEnd("addFounderRole", true);
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("addFounderRole", e);
            throw new OurRuntime("addFounderRole" + e.getMessage()); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("addFounderRole", e);
            throw new OurArg("addFounderRole" + e.getMessage()); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("addFounderRole", e, memberId, role, shopId);
            throw new OurRuntime("addFounderRole: " + e.getMessage(), e); // Indicate failure to add role
        }
    }

    /**
     * Removes a role from a member.
     * 
     * @param id   The ID of the member from whom the role is being removed.
     * @param role The role to be removed from the member.
     * 
     *             * @return true if the role was removed successfully, false
     *             otherwise.
     * 
     *             * @throws IllegalArgumentException
     *             * @throws OurRuntime
     */
    public boolean removeRole(int id, Role role) {
        try {
            LoggerService.logMethodExecution("removeRole", id, role);
            if (role == null) {
                LoggerService.logDebug("removeRole", new OurRuntime("Role cannot be null."));
                throw new OurRuntime("Role cannot be null.");
            }
            validateMemberId(id);
            if (isSuspended(id)) {
                throw new OurRuntime("the user is suspended");
            }
            Role existingRole = userRepository.getRole(id, role.getShopId());
            if (existingRole == null) {
                LoggerService.logDebug("removeRole",
                        new OurRuntime("Member ID " + id + " has no role for shop ID " + role.getShopId()));
                throw new OurRuntime("Member ID " + id + " has no role for shop ID " + role.getShopId());
            }
            if (existingRole.getAssigneeId() != role.getAssigneeId()) {
                LoggerService.logDebug("removeRole", new OurRuntime(
                        "Member ID " + id + " is not the assignee of the role for shop ID " + role.getShopId()));
                throw new OurRuntime(
                        "Member ID " + id + " is not the assignee of the role for shop ID " + role.getShopId());
            }
            int shopId = role.getShopId();
            String notification = role.toNotification();
            userRepository.removeRole(id, shopId); // Remove the role from the member
            removedAppointment(id, notification, shopId);
            LoggerService.logMethodExecutionEnd("removeRole", true);
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeRole", e);
            throw new OurRuntime("removeRole" + e.getMessage()); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("removeRole", e);
            throw new OurArg("removeRole" + e.getMessage()); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removeRole", e, id, role);
            throw new OurRuntime("removeRole: " + e.getMessage(), e); // Indicate failure to remove role
        }
    }

    /**
     * Checks if a member has a specific role.
     * 
     * @param id   The ID of the member to check.
     * @param role The role to check for the member.
     * 
     *             * @return true if the member has the specified role, false
     *             otherwise.
     * 
     *             * @throws IllegalArgumentException
     *             * @throws OurRuntime
     */
    public boolean hasRole(int id, Role role) {
        try {
            LoggerService.logMethodExecution("hasRole", id, role);
            if (role == null) {
                LoggerService.logDebug("hasRole", new OurRuntime("Role cannot be null."));
                throw new OurRuntime("Role cannot be null.");
            }
            validateMemberId(id);
            if (isSuspended(id)) {
                LoggerService.logMethodExecutionEnd("hasRole", false);
                return false;
            }
            Role existingRole = userRepository.getRole(id, role.getShopId());
            if (existingRole == null) {
                LoggerService.logDebug("hasRole",
                        new OurRuntime("Member ID " + id + " has no role for shop ID " + role.getShopId()));
                throw new OurRuntime("Member ID " + id + " has no role for shop ID " + role.getShopId());
            }
            if (existingRole.getAssigneeId() != role.getAssigneeId()) {
                LoggerService.logDebug("hasRole", new OurRuntime(
                        "Member ID " + id + " is not the assignee of the role for shop ID " + role.getShopId()));
                throw new OurRuntime(
                        "Member ID " + id + " is not the assignee of the role for shop ID " + role.getShopId());
            }
            LoggerService.logMethodExecutionEnd("hasRole", true);
            return true; // Member has the specified role
        } catch (OurRuntime e) {
            LoggerService.logDebug("hasRole", e);
            throw new OurRuntime("hasRole" + e.getMessage()); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("hasRole", e);
            throw new OurArg("hasRole" + e.getMessage()); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("hasRole", e, id, role);
            throw new OurRuntime("hasRole: " + e.getMessage(), e); // Indicate failure to check role
        }
    }

    /**
     * Adds a permission to a member.
     * 
     * @param token      The token of the user adding the permission.
     * @param id         The ID of the member to whom the permission is being added.
     * @param permission The permission to be added to the member.
     * @param shopId     The ID of the store where the permission is being added.
     * 
     *                   * @return true if the permission was added successfully,
     *                   false otherwise.
     * 
     *                   * @throws IllegalArgumentException
     *                   * @throws OurRuntime
     */
    public boolean addPermission(String token, int id, PermissionsEnum permission, int shopId) {
        try {
            LoggerService.logMethodExecution("addPermission", token, id, permission, shopId);
            int assigneeId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(assigneeId)) {
                throw new OurRuntime("the user is suspended");
            }
            if (permission == null) {
                LoggerService.logDebug("addPermission", new OurRuntime("Permission cannot be null."));
                throw new OurRuntime("Permission cannot be null.");
            }
            validateMemberId(id);
            if (userRepository.getRole(id, shopId) == null) {
                LoggerService.logDebug("addPermission",
                        new OurRuntime("Member ID " + id + " has no role for shop ID " + shopId));
                throw new OurRuntime("Member ID " + id + " has no role for shop ID " + shopId);
            }
            if (userRepository.getRole(id, shopId).getAssigneeId() != assigneeId) {
                LoggerService.logDebug("addPermission", new OurRuntime("Member ID " + assigneeId
                        + " is not the assignee of member ID " + id + " in shop ID " + shopId));
                throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + id
                        + " in shop ID " + shopId);
            }
            userRepository.addPermission(id, permission, shopId); // Add the permission to the member
            LoggerService.logMethodExecutionEnd("addPermission", true);
            return true; // Permission added successfully
        } catch (OurRuntime e) {
            LoggerService.logDebug("addPermission", e);
            throw new OurRuntime("addPermission" + e.getMessage()); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("addPermission", e);
            throw new OurArg("addPermission" + e.getMessage()); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("addPermission", e, token, id, permission, shopId);
            throw new OurRuntime("addPermission: " + e.getMessage(), e); // Indicate failure to add permission
        }
    }

    /**
     * Removes a permission from a member.
     * 
     * @param token      The token of the user removing the permission.
     * @param id         The ID of the member from whom the permission is being
     *                   removed.
     * @param permission The permission to be removed from the member.
     * @param shopId     The ID of the store where the permission is being removed.
     * 
     *                   * @return true if the permission was removed successfully,
     *                   false otherwise.
     * 
     *                   * @throws IllegalArgumentException
     *                   * @throws OurRuntime
     */
    public boolean removePermission(String token, int id, PermissionsEnum permission, int shopId) {
        try {
            LoggerService.logMethodExecution("removePermission", token, id, permission, shopId);
            int assigneeId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(assigneeId)) {
                throw new OurRuntime("the user is suspended");
            }
            if (permission == null) {
                LoggerService.logDebug("removePermission", new OurRuntime("Permission cannot be null."));
                throw new OurRuntime("Permission cannot be null.");
            }
            validateMemberId(id);
            if (userRepository.getRole(id, shopId) == null) {
                LoggerService.logDebug("removePermission",
                        new OurRuntime("Member ID " + id + " has no role for shop ID " + shopId));
                throw new OurRuntime("Member ID " + id + " has no role for shop ID " + shopId);
            }
            if (userRepository.getRole(id, shopId).getAssigneeId() != assigneeId) {
                LoggerService.logDebug("removePermission", new OurRuntime("Member ID " + assigneeId
                        + " is not the assignee of member ID " + id + " in shop ID " + shopId));
                throw new OurRuntime("Member ID " + assigneeId + " is not the assignee of member ID " + id
                        + " in shop ID " + shopId);
            }
            userRepository.removePermission(id, permission, shopId); // Add the permission to the member
            removedAppointment(id, permission.toString(), shopId);
            LoggerService.logMethodExecutionEnd("removePermission", true);
            return true; // Permission added successfully
        } catch (OurRuntime e) {
            LoggerService.logDebug("removePermission", e);
            throw new OurRuntime("removePermission" + e.getMessage()); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("removePermission", e);
            throw new OurArg("removePermission" + e.getMessage()); // Rethrow the custom exception

        } catch (Exception e) {
            LoggerService.logError("removePermission", e, token, id, permission, shopId);
            throw new OurRuntime("removePermission: " + e.getMessage(), e); // Indicate failure to remove permission
        }
    }

    public boolean hasPermission(int id, PermissionsEnum permission, int shopId) {
        try {
            LoggerService.logMethodExecution("hasPermission", id, permission, shopId);
            if (userRepository.getUserMapping().containsKey(id)) {
                User user = userRepository.getUserById(id);
                // if (userRepository.isSuspended(id)) {
                //     LoggerService.logMethodExecutionEnd("hasPermission", false);
                //     return false; // User is suspended, no permissions granted
                // }
                validateMemberId(id);
                // if (isSuspended(id)) {
                //     LoggerService.logMethodExecutionEnd("hasPermission", false);
                //     return false; // User is suspended, no permissions granted
                // }
                return ((Member) user).hasPermission(permission, shopId); // Check if the user has the specified
                                                                          // permission
            } else {
                LoggerService.logDebug("hasPermission", new OurRuntime("User with ID " + id + " doesn't exist."));
                return false; // User doesn't exist
            }
        } catch (OurRuntime e) {
            LoggerService.logDebug("hasPermission", e);
            return false; // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("hasPermission", e, id, permission, shopId);
            return false; // Indicate failure to check permission
        }
    }

    /**
     * Retrieves the shopping cart items for a user by their ID.
     * 
     * @param userId The ID of the user whose shopping cart items are to be
     *               retrieved.
     * @return A DeepCopy HashMap containing the shopping cart items for the user.
     *         shopId -> <itemId -> quantity>*
     */
    public HashMap<Integer, HashMap<Integer, Integer>> getUserShoppingCartItems(int userId) {
        try {
            LoggerService.logMethodExecution("getUserShoppingCart", userId);
            HashMap<Integer, HashMap<Integer, Integer>> cart = userRepository.getShoppingCartById(userId).getItems();
            LoggerService.logMethodExecutionEnd("getUserShoppingCart", cart);
            return cart;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getUserShoppingCart", e);
            throw new OurRuntime("getUserShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logError("getUserShoppingCart", e, userId);
            throw new OurArg("getUserShoppingCart: " + userId, e);
        } catch (Exception e) {
            LoggerService.logError("getUserShoppingCart", e, userId);
            throw new OurRuntime("getUserShoppingCart: " + e.getMessage(), e);
        }

    }

    /**
     * Clears the shopping cart for a user by their ID.
     * 
     * @param userId The ID of the user whose shopping cart is to be cleared.
     * @throws IllegalArgumentException if the user ID is invalid or the user is not
     *                                  a member.
     * @throws RuntimeException         if an error occurs while clearing the
     *                                  shopping cart.
     */
    public void clearUserShoppingCart(int userId) {
        try {
            LoggerService.logMethodExecution("clearUserShoppingCart", userId);
            userRepository.getShoppingCartById(userId).clearCart();
            LoggerService.logMethodExecutionEndVoid("clearUserShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("clearUserShoppingCart", e);
            throw new OurRuntime("clearUserShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("clearUserShoppingCart", e);
            throw new OurArg("clearUserShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("clearUserShoppingCart", e, userId);
            throw new OurRuntime("clearUserShoppingCart: " + e.getMessage(), e);
        }
    }

    public void clearUserShoppingCartByShopId(int userId, int shopId) {
        try {
            LoggerService.logMethodExecution("clearUserShoppingCartByShopId", userId);
            userRepository.getShoppingCartById(userId).removeBasket(shopId);
            LoggerService.logMethodExecutionEndVoid("clearUserShoppingCartByShopId");
        } catch (OurRuntime e) {
            LoggerService.logDebug("clearUserShoppingCartByShopId", e);
            throw new OurRuntime("clearUserShoppingCartByShopId: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("clearUserShoppingCartByShopId", e);
            throw new OurArg("clearUserShoppingCartByShopId: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("clearUserShoppingCartByShopId", e, userId);
            throw new OurRuntime("clearUserShoppingCartByShopId: " + e.getMessage(), e);
        }
    }


    /**
     * Restores the shopping cart for a user by their ID.
     * 
     * @param userId The ID of the user whose shopping cart is to be restored.
     * @param items  A HashMap containing the items to restore in the shopping cart.
     *               shopId -> <itemId -> quantity>
     * @throws IllegalArgumentException if the user ID is invalid or the user is not
     *                                  a member.
     * @throws RuntimeException         if an error occurs while restoring the
     *                                  shopping cart.
     * @throws NullPointerException     if the items HashMap is null.
     */
    public void restoreUserShoppingCart(int userId, HashMap<Integer, HashMap<Integer, Integer>> items) {
        try {
            LoggerService.logMethodExecution("restoreUserShoppingCart", userId, items);
            userRepository.getShoppingCartById(userId).restoreCart(items);
            LoggerService.logMethodExecutionEndVoid("restoreUserShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("restoreUserShoppingCart", e);
            throw new OurRuntime("restoreUserShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("restoreUserShoppingCart", e);
            throw new OurArg("restoreUserShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("restoreUserShoppingCart", e, userId, items);
            throw new OurRuntime("restoreUserShoppingCart: " + e.getMessage(), e);
        }
    }


    public void restoreUserShoppingCartByShopId(int userId, HashMap<Integer, HashMap<Integer, Integer>> items, int shopId) {
        try {
            LoggerService.logMethodExecution("restoreUserShoppingCartByShopId", userId, items, shopId);

            HashMap<Integer, HashMap<Integer, Integer>> itemsOfShopId = new HashMap<>();
            itemsOfShopId.put(shopId, items.get(shopId));
            
            userRepository.getShoppingCartById(userId).restoreCart(itemsOfShopId);
            LoggerService.logMethodExecutionEndVoid("restoreUserShoppingCartByShopId");
        } catch (OurRuntime e) {
            LoggerService.logDebug("restoreUserShoppingCartByShopId", e);
            throw new OurRuntime("restoreUserShoppingCartByShopId: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("restoreUserShoppingCartByShopId", e);
            throw new OurArg("restoreUserShoppingCartByShopId: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("restoreUserShoppingCartByShopId", e, userId, items);
            throw new OurRuntime("restoreUserShoppingCartByShopId: " + e.getMessage(), e);
        }
    }


    /**
     * Retrieves the payment method for a user by their ID.
     * 
     * @param userId The ID of the user whose payment method is to be retrieved.
     * @return The PaymentMethod object associated with the user.
     * @throws IllegalArgumentException if the user ID is invalid or the user is not
     *                                  a member.
     * @throws RuntimeException         if an error occurs while fetching the
     *                                  payment method.
     * @throws NullPointerException     if the payment method is null.
     */
    public PaymentMethod getUserPaymentMethod(int userId) {
        try {
            LoggerService.logMethodExecution("getUserPaymentMethod", userId);
            PaymentMethod paymentMethod = userRepository.getUserById(userId).getPaymentMethod();
            LoggerService.logMethodExecutionEnd("getUserPaymentMethod", paymentMethod);
            return paymentMethod;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getUserPaymentMethod", e);
            throw new OurRuntime("getUserPaymentMethod: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("getUserPaymentMethod", e);
            throw new OurArg("getUserPaymentMethod: " + e.getMessage(), e); // Rethrow the custom exception

        } catch (Exception e) {
            LoggerService.logError("getUserPaymentMethod", e, userId);
            throw new OurRuntime("getUserPaymentMethod: " + e.getMessage(), e);
        }
    }

    /**
     * adds an item to the shopping cart for a user by their token.
     * 
     * @param token
     * @param shopId
     * @param itemId
     * @param quantity
     * 
     *                 * @throws OurRuntime
     *                 * @throws Exception
     */
    public void addItemToShoppingCart(String token, int shopId, int itemId, int quantity) {
        try {
            LoggerService.logMethodExecution("addItemToShoppingCart", token, shopId, itemId, quantity);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(userId)) {
                throw new OurRuntime("the user is suspended");
            }
            userRepository.addItemToShoppingCart(userId, shopId, itemId, quantity);
            LoggerService.logMethodExecutionEndVoid("addItemToShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("addItemToShoppingCart", e);
            throw new OurRuntime("addItemToShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("addItemToShoppingCart", e);
            throw new OurArg("addItemToShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("addItemToShoppingCart", e, token, shopId, itemId, quantity);
            throw new OurRuntime("addItemToShoppingCart: " + e.getMessage(), e);
        }
    }

    /**
     * removes an item from the shopping cart for a user by their token.
     * 
     * @param token
     * @param shopId
     * @param itemId
     * 
     *               * @throws OurRuntime
     *               * @throws Exception
     */
    public void removeItemFromShoppingCart(String token, int shopId, int itemId) {
        try {
            LoggerService.logMethodExecution("removeItemFromShoppingCart", token, shopId, itemId);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(userId)) {
                throw new OurRuntime("the user is suspended");
            }
            userRepository.removeItemFromShoppingCart(userId, shopId, itemId);
            LoggerService.logMethodExecutionEndVoid("removeItemFromShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeItemFromShoppingCart", e);
            throw new OurRuntime("removeItemFromShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("removeItemFromShoppingCart", e);
            throw new OurArg("removeItemFromShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removeItemFromShoppingCart", e, token, shopId, itemId);
            throw new OurRuntime("removeItemFromShoppingCart: " + e.getMessage(), e);
        }
    }

    /**
     * updates the quantity of an item in the shopping cart for a user by their
     * token.
     * 
     * @param token
     * @param shopId
     * @param itemId
     * @param quantity
     * 
     *                 * @throws OurRuntime
     *                 * @throws Exception
     */
    public void updateItemQuantityInShoppingCart(String token, int shopId, int itemId, int quantity) {
        try {
            LoggerService.logMethodExecution("updateItemQuantityInShoppingCart", token, shopId, itemId, quantity);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(userId)) {
                throw new OurRuntime("the user is suspended");
            }
            userRepository.updateItemQuantityInShoppingCart(userId, shopId, itemId, quantity);
            LoggerService.logMethodExecutionEndVoid("updateItemQuantityInShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("updateItemQuantityInShoppingCart", e);
            throw new OurRuntime("updateItemQuantityInShoppingCart: " + e.getMessage(), e); // Rethrow the custom
                                                                                            // exception
        } catch (OurArg e) {
            LoggerService.logDebug("updateItemQuantityInShoppingCart", e);
            throw new OurArg("updateItemQuantityInShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("updateItemQuantityInShoppingCart", e, token, shopId, itemId, quantity);
            throw new OurRuntime("updateItemQuantityInShoppingCart: " + e.getMessage(), e);
        }
    }

    /**
     * Clears the shopping cart for a user by their token.
     * 
     * @param token The token of the user whose shopping cart is to be cleared.
     * 
     *              * @throws OurRuntime
     *              * @throws Exception
     */
    public void clearShoppingCart(String token) {
        try {
            LoggerService.logMethodExecution("clearShoppingCart", token);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            userRepository.clearShoppingCart(userId);
            LoggerService.logMethodExecutionEndVoid("clearShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("clearShoppingCart", e);
            throw new OurRuntime("clearShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("clearShoppingCart", e);
            throw new OurArg("clearShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("clearShoppingCart", e, token);
            throw new OurRuntime("clearShoppingCart: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the items in the shopping basket for a user by their token and shop
     * ID.
     * 
     * @param token  The token of the user whose shopping basket items are to be
     *               retrieved.
     * @param shopId The ID of the shop whose items are to be retrieved.
     * @return A HashMap containing the items in the shopping basket for the user.
     *         itemId -> quantity
     * 
     *         * @throws OurRuntime
     *         * @throws Exception
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
            throw new OurRuntime("getBasketItems: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("getBasketItems", e);
            throw new OurArg("getBasketItems: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("getBasketItems", e, token, shopId);
            throw new OurRuntime("getBasketItems: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a new shopping basket for a user by their token and shop ID.
     * 
     * @param token  The token of the user adding the basket.
     * @param shopId The ID of the shop where the basket is being added.
     * 
     *               * @throws OurRuntime
     *               * @throws Exception
     */
    public void addBasket(String token, int shopId) {
        try {
            LoggerService.logMethodExecution("addBasket", token, shopId);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            userRepository.createBasket(userId, shopId);
            LoggerService.logMethodExecutionEndVoid("addBasket");
        } catch (OurRuntime e) {
            LoggerService.logDebug("addBasket", e);
            throw new OurRuntime("addBasket: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("addBasket", e);
            throw new OurArg("addBasket: " + e.getMessage(), e); // Rethrow the custom exception
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
        /*
         * // Length check
         * if (password.length() < 8) {
         * return false;
         * }
         * 
         * // Regex for required rules
         * String pattern =
         * "^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$";
         * 
         * return password.matches(pattern);
         */
        return true; // For now, we are not validating the password format
    }

    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        // Regex:
        // ^\+? → optional + at the start
        // \d+ → one or more digits
        // (-\d+)? → optional single dash followed by digits
        // $ → end of string
        // Full length between 9 to 15 characters including dash/+ if present
        String pattern = "^\\+?\\d+(-\\d+)?$";   //// 2 \ or 1 \ ??

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
        String pattern = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"; //// 2 \ or 1 \ ??

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
     * 
     * @param token         The token of the user setting the payment method.
     * @param paymentMethod The PaymentMethod object to be set for the user.
     * @param shopId        The ID of the shop where the payment method is being
     *                      set.
     * 
     *                      * @throws OurRuntime
     *                      * @throws Exception
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
            throw new OurRuntime("setPaymentMethod: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("setPaymentMethod", e);
            throw new OurArg("setPaymentMethod: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("setPaymentMethod", e, token, paymentMethod);
            throw new OurRuntime("setPaymentMethod: " + e.getMessage(), e);
        }
    }

    public boolean addBidToUserShoppingCart(int userId, int shopId, Map<Integer, Integer> items) {
        try {
            LoggerService.logMethodExecution("addToUserShoppingCart", userId, items);
            userRepository.addBidToShoppingCart(userId, shopId, items); // Add the items to the user's shopping cart
            LoggerService.logMethodExecutionEndVoid("addToUserShoppingCart");
            return true; // Items added successfully
        } catch (OurRuntime e) {
            LoggerService.logDebug("addToUserShoppingCart", e);
            throw new OurRuntime("addToUserShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("addToUserShoppingCart", e);
            throw new OurArg("addToUserShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("addToUserShoppingCart", e, userId, items);
            throw new OurRuntime("addToUserShoppingCart: " + e.getMessage(), e);
        }
    }

    /**
     * Pays for user's order
     * 
     * @param token
     * @param shopId
     * @param payment
     * 
     *                * @return true if the payment was successful
     * 
     *                * @throws OurRuntime, Exception
     */
    public int pay(String token, int shopId, double amount, String currency, String cardNumber, String expirationDateMonth, String expirationDateYear, String cardHolderName, String cvv, String id) {
        try {
            LoggerService.logMethodExecution("pay", token, shopId, amount, currency, cardNumber, expirationDateMonth, expirationDateYear, cardHolderName, cvv, id);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (isSuspended(userId)) {
                throw new OurRuntime("the user is suspended");
            }
            int pid = userRepository.pay(userId, amount, currency, cardNumber, expirationDateMonth, expirationDateYear, cardHolderName, cvv, id); // Process the payment
            LoggerService.logMethodExecutionEnd("pay", true);
            return pid;
        } catch (OurRuntime e) {
            LoggerService.logDebug("pay", e);
            throw new OurRuntime("pay: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("pay", e);
            throw new OurArg("pay: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("pay", e, token, shopId, amount, currency, cardNumber, expirationDateMonth, expirationDateYear, cardHolderName, cvv, id);
            throw new OurRuntime("pay: " + e.getMessage(), e);
        }
    }

    // NO API ENDPOINT!
    public boolean refundPaymentAuto(String token, int paymnetID) {
        try {
            LoggerService.logMethodExecution("refundPayment", token, paymnetID);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            userRepository.refund(userId, paymnetID); // Set the payment method for the user
            LoggerService.logMethodExecutionEnd("refundPayment", true);
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("refundPayment", e);
            throw new OurRuntime("refundPayment: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("refundPayment", e);
            throw new OurArg("refundPayment: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("refundPayment", e, token, paymnetID);
            throw new OurRuntime("refundPayment: " + e.getMessage(), e);
        }

    }

    public boolean refundPaymentByStoreEmployee(String token, int userId, int shopId, int paymentId) {
        try {
            LoggerService.logMethodExecution("refundPaymentByStoreEmployee", token, shopId, paymentId);
            int initiatingUserId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            if (userRepository.getRole(initiatingUserId, shopId) == null) {
                LoggerService.logDebug("refundPaymentByStoreEmployee",
                        new OurRuntime("Member ID " + initiatingUserId + " has no role for shop ID " + shopId));
                throw new OurRuntime("Member ID " + initiatingUserId + " has no role for shop ID " + shopId);
            }
            userRepository.refund(userId, paymentId); // Set the payment method for the user
            LoggerService.logMethodExecutionEnd("refundPayment", true);
            return true;
        } catch (OurRuntime e) {
            LoggerService.logDebug("refundPayment", e);
            throw new OurRuntime("refundPayment: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("refundPayment", e);
            throw new OurArg("refundPayment: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("refundPayment", e, token, shopId, paymentId);
            throw new OurRuntime("refundPayment: " + e.getMessage(), e);
        }

    }

    // no API endpoint!
    public Address getUserShippingAddress(int userId) {
        try {
            LoggerService.logMethodExecution("getUserShippingAddress", userId);
            Address shippingAddress = userRepository.getUserById(userId).getAddress();
            LoggerService.logMethodExecutionEnd("getUserShippingAddress", shippingAddress);
            return shippingAddress;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getUserShippingAddress", e);
            throw new OurRuntime("getUserShippingAddress: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("getUserShippingAddress", e);
            throw new OurArg("getUserShippingAddress: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("getUserShippingAddress", e, userId);
            throw new OurRuntime("Error fetching shipping address for user ID " + userId + ": " + e.getMessage(), e);
        }
    }

    // Call it after login or when getting notifiactions when user is logged in
    public List<String> getNotificationsAndClear(String token) {
        try {
            LoggerService.logMethodExecution("getNotificationsAndClear", token);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            List<String> notificatList = userRepository.getNotificationsAndClear(userId);
            LoggerService.logMethodExecutionEndVoid("getNotificationsAndClear");
            return notificatList;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getNotificationsAndClear", e);
            throw new OurRuntime("getNotificationsAndClear: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("getNotificationsAndClear", e);
            throw new OurArg("getNotificationsAndClear: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("getNotificationsAndClear", e, token);
            throw new OurRuntime("getNotificationsAndClear: " + e.getMessage(), e);
        }
    }

    public void purchaseNotification(HashMap<Integer, HashMap<Integer, Integer>> cart) {
        try {
            LoggerService.logMethodExecution("purchaseNotification", cart);
            for (Map.Entry<Integer, HashMap<Integer, Integer>> entry : cart.entrySet()) {
                int shopId = entry.getKey();
                HashMap<Integer, Integer> items = entry.getValue();
                List<Member> owners = userRepository.getOwners(shopId);
                for (Member owner : owners) {
                    for (Map.Entry<Integer, Integer> itemEntry : items.entrySet()) {
                        int itemId = itemEntry.getKey();
                        int quantity = itemEntry.getValue();
                        this.notificationService.sendToUser(owner.getMemberId(), "Item " + itemId + " Purchased",
                                "Quantity: " + quantity + " purchased from your shop ID: " + shopId);
                    }
                }
            }
            LoggerService.logMethodExecutionEndVoid("purchaseNotification");
        } catch (OurRuntime e) {
            LoggerService.logDebug("purchaseNotification", e);
            throw new OurRuntime("purchaseNotification: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("purchaseNotification", e);
            throw new OurArg("purchaseNotification: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("purchaseNotification", e, cart);
            throw new OurRuntime("purchaseNotification: " + e.getMessage(), e);
        }
    }

    public void closeShopNotification(Integer shopId) {
        try {
            LoggerService.logMethodExecution("closeShopNotification", shopId);
            List<Member> owners = userRepository.getOwners(shopId);
            for (Member owner : owners) {
                this.notificationService.sendToUser(owner.getMemberId(), "Shop Closed",
                        "Your shop ID: " + shopId + " has been closed.");
            }
            LoggerService.logMethodExecutionEndVoid("closeShopNotification");
        } catch (OurRuntime e) {
            LoggerService.logDebug("closeShopNotification", e);
            throw new OurRuntime("closeShopNotification: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("closeShopNotification", e);
            throw new OurArg("closeShopNotification: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("closeShopNotification", e, shopId);
            throw new OurRuntime("closeShopNotification: " + e.getMessage(), e);
        }
    }

    public void removedAppointment(Integer memberId, String appointment, Integer shopId) {
        try {
            LoggerService.logMethodExecution("removedAppointment", memberId, appointment);
            if (shopId == null) {
                this.notificationService.sendToUser(memberId, "Appointment Removed",
                        "Your appointment to: " + appointment + " has been removed.");
            } else {
                this.notificationService.sendToUser(memberId, "Appointment Removed",
                        "Your appointment to: " + appointment + " in the shop " + shopId + " has been removed.");
            }
            LoggerService.logMethodExecutionEndVoid("removedAppointment");
        } catch (OurRuntime e) {
            LoggerService.logDebug("removedAppointment", e);
            throw new OurRuntime("removedAppointment: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("removedAppointment", e);
            throw new OurArg("removedAppointment: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("removedAppointment", e, memberId, appointment);
            throw new OurRuntime("removedAppointment: " + e.getMessage(), e);
        }
    }

    // if isFromShop is true, then the message is from the shop to the user
    // if isFromShop is false, then the message is from the user to the shop
    public void messageNotification(Integer memberId, Integer shopId, boolean isFromShop) {
        try {
            LoggerService.logMethodExecution("messageUserNotification", memberId);

            // build the exact payload your tests expect
            String payload;
            if (isFromShop) {
                payload = "You have received a new message from the shop (id=" + shopId + ").";
            } else {
                payload = "You have received a new message from the user (id=" + memberId + ").";
            }

            // now actually send the notification
            this.notificationService.sendToUser(
                memberId,
                "Message Received",
                payload
            );

            LoggerService.logMethodExecutionEndVoid("messageUserNotification");

        } catch (OurRuntime e) {
            LoggerService.logDebug("messageUserNotification", e);
            throw new OurRuntime("messageUserNotification: " + e.getMessage(), e);

        } catch (OurArg e) {
            LoggerService.logDebug("messageUserNotification", e);
            throw new OurArg("messageUserNotification: " + e.getMessage(), e);

        } catch (Exception e) {
            LoggerService.logError("messageUserNotification", e, memberId);
            throw new OurRuntime("messageUserNotification: " + e.getMessage(), e);
        }
    }

    // LocalDateTime is used to represent the date and time of suspension
    // LocalDateTime.max is used to represent a suspended user for an indefinite
    // period
    // LocalDateTime.now is used to represent a user who is not suspended
    // other LocalDateTime values are used to represent a user who is suspended for
    // a specific period
    public void setSuspended(int userId, LocalDateTime suspended) {
        try {
            LoggerService.logMethodExecution("setSuspended", userId, suspended);
            userRepository.setSuspended(userId, suspended);
            LoggerService.logMethodExecutionEndVoid("setSuspended");
        } catch (OurRuntime e) {
            LoggerService.logDebug("setSuspended", e);
            throw new OurRuntime("setSuspended: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("setSuspended", e);
            throw new OurArg("setSuspended: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("setSuspended", e, userId, suspended);
            throw new OurRuntime("Error setting suspension for user ID " + userId + ": " + e.getMessage(), e);
        }
    }
    
    public void setUnSuspended(int userId) {
        try {
            if(!isSuspended(userId)) {
                throw new OurRuntime("User is already unsuspended");
            }
            LoggerService.logMethodExecution("setUnSuspended", userId);
            userRepository.setUnSuspended(userId);
            LoggerService.logMethodExecutionEndVoid("setSuspended");
        } catch (OurRuntime e) {
            LoggerService.logDebug("setSuspended", e);
            throw new OurRuntime("setSuspended: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("setSuspended", e);
            throw new OurArg("setSuspended: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("setSuspended", e, userId);
            throw new OurRuntime("Error setting suspension for user ID " + userId + ": " + e.getMessage(), e);
        }
    }
    public boolean isSuspended(int userId) {
        try {
            LoggerService.logMethodExecution("isSuspended", userId);
            boolean isSuspended = userRepository.isSuspended(userId);
            LoggerService.logMethodExecutionEnd("isSuspended", isSuspended);
            return isSuspended;
        } catch (OurRuntime e) {
            LoggerService.logDebug("isSuspended", e);
            throw new OurRuntime("isSuspended: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("isSuspended", e);
            throw new OurArg("isSuspended: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("isSuspended", e, userId);
            throw new OurRuntime("isSuspended: " + e.getMessage(), e);
        }
    }

    public List<Integer> getSuspendedUsers() {
        try {
            LoggerService.logMethodExecution("getSuspendedUsers");
            List<Integer> suspendedUsers = userRepository.getSuspendedUsers();
            LoggerService.logMethodExecutionEnd("getSuspendedUsers", suspendedUsers);
            return suspendedUsers;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getSuspendedUsers", e);
            throw new OurRuntime("getSuspendedUsers: " + e.getMessage(), e);

        } catch (OurArg e) {
            LoggerService.logDebug("getSuspendedUsers", e);
            throw new OurArg("getSuspendedUsers: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getSuspendedUsers", e);
            throw new OurRuntime("getSuspendedUsers: " + e.getMessage(), e);
        }
    }

    public void banUser(int userId) {
        try {
            LoggerService.logMethodExecution("banUser", userId);
            userRepository.banUser(userId);
            LoggerService.logMethodExecutionEndVoid("banUser");
        } catch (OurRuntime e) {
            LoggerService.logDebug("banUser", e);
            throw new OurRuntime("banUser: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("banUser", e);
            throw new OurArg("banUser: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("banUser", e, userId);
            throw new OurRuntime("Error setting banUser for user ID " + userId + ": " + e.getMessage(), e);
        }
    }


    public List<Integer> getShopIdsByWorkerId(int userId) {
        try {
            LoggerService.logMethodExecution("getShopsByWorkerId", userId);
            List<Integer> shopIds = userRepository.getShopIdsByWorkerId(userId);
            LoggerService.logMethodExecutionEnd("getShopsByWorkerId", shopIds);
            return shopIds;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getShopsByUserId", e);
            throw new OurRuntime("getShopsByUserId: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("getShopsByUserId", e);
            throw new OurArg("getShopsByUserId: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getShopsByUserId", e, userId);
            throw new OurRuntime("getShopsByUserId: " + e.getMessage(), e);
        }
    }

    public List<Member> getShopMembers(int shopId) {
        try {
            LoggerService.logMethodExecution("getShopMembers", shopId);
            List<Member> members = userRepository.getShopMembers(shopId);
            LoggerService.logMethodExecutionEnd("getShopMembers", members);
            return members;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getShopMembers", e);
            throw new OurRuntime("getShopMembers: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("getShopMembers", e);
            throw new OurArg("getShopMembers: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getShopMembers", e, shopId);
            throw new OurRuntime("getShopMembers: " + e.getMessage(), e);
        }
    }

    public List<Role> getPendingRoles(String token) {
        try {
            LoggerService.logMethodExecution("getPendingRoles", token);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            List<Role> pendingRoles = userRepository.getPendingRoles(userId);
            LoggerService.logMethodExecutionEnd("getPendingRoles", pendingRoles);
            return pendingRoles;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getPendingRoles", e);
            throw new OurRuntime("getPendingRoles: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("getPendingRoles", e);
            throw new OurArg("getPendingRoles: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getPendingRoles", e, token);
            throw new OurRuntime("getPendingRoles: " + e.getMessage(), e);
        }
    }

    public List<Role> getAcceptedRoles(String token) {
        try {
            LoggerService.logMethodExecution("getAcceptedRoles", token);
            int userId = authTokenService.ValidateToken(token); // Validate the token and get the user ID
            List<Role> acceptedRoles = userRepository.getAcceptedRoles(userId);
            LoggerService.logMethodExecutionEnd("getAcceptedRoles", acceptedRoles);
            return acceptedRoles;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getAcceptedRoles", e);
            throw new OurRuntime("getAcceptedRoles: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("getAcceptedRoles", e);
            throw new OurArg("getAcceptedRoles: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getAcceptedRoles", e, token);
            throw new OurRuntime("getAcceptedRoles: " + e.getMessage(), e);
        }
    }

    public void addNotification(int userId, String title, String message) {
        try {
            LoggerService.logMethodExecution("addNotification", userId, message);
            userRepository.addNotification(userId, title, message);
            LoggerService.logMethodExecutionEndVoid("addNotification");
        } catch (OurRuntime e) {
            LoggerService.logDebug("addNotification", e);
            throw new OurRuntime("addNotification: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("addNotification", e);
            throw new OurArg("addNotification: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("addNotification", e, userId, message);
            throw new OurRuntime("addNotification: " + e.getMessage(), e);
        }
    }

    public void updateShoppingCartItemQuantity(int userId, int shopID, int itemID, boolean b) {
        try {
            LoggerService.logMethodExecution("updateShoppingCartItemQuantity", userId, shopID, itemID, b);
            userRepository.updateShoppingCartItemQuantity(userId, shopID, itemID, b);
            LoggerService.logMethodExecutionEndVoid("updateShoppingCartItemQuantity");
        } catch (OurRuntime e) {
            LoggerService.logDebug("updateShoppingCartItemQuantity", e);
            throw new OurRuntime("updateShoppingCartItemQuantity: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("updateShoppingCartItemQuantity", e);
            throw new OurArg("updateShoppingCartItemQuantity: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("updateShoppingCartItemQuantity", e, userId, shopID, itemID, b);
            throw new OurRuntime("updateShoppingCartItemQuantity: " + e.getMessage(), e);
        }
    }

    public void removeItemFromShoppingCart(int userId, int shopID, int itemID) {
        try {
            LoggerService.logMethodExecution("removeShoppingCartItem", userId, shopID, itemID);
            userRepository.removeShoppingCartItem(userId, shopID, itemID);
            LoggerService.logMethodExecutionEndVoid("removeShoppingCartItem");
        } catch (OurRuntime e) {
            LoggerService.logDebug("removeShoppingCartItem", e);
            throw new OurRuntime("removeShoppingCartItem: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("removeShoppingCartItem", e);
            throw new OurArg("removeShoppingCartItem: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("removeShoppingCartItem", e, userId, shopID, itemID);
            throw new OurRuntime("removeShoppingCartItem: " + e.getMessage(), e);
        }
    }

    public boolean hasRoleInShop(int userId, int shopId) {
        try {
            LoggerService.logMethodExecution("hasRoleInShop", userId, shopId);
            Member member = (Member) userRepository.getUserById(userId);
            if (member == null) {
                LoggerService.logDebug("hasRoleInShop", new OurRuntime("Member with ID " + userId + " not found."));
                throw new OurRuntime("Member with ID " + userId + " not found.");
            }
            boolean hasRole = member.getRoles().stream()
                    .anyMatch(role -> role.getShopId() == shopId);
            LoggerService.logMethodExecutionEnd("hasRoleInShop", hasRole);
            return hasRole;
        } catch (OurRuntime e) {
            LoggerService.logDebug("hasRoleInShop", e);
            throw new OurRuntime("hasRoleInShop: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("hasRoleInShop", e);
            throw new OurArg("hasRoleInShop: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("hasRoleInShop", e, userId, shopId);
            throw new OurRuntime("hasRoleInShop: " + e.getMessage(), e);
        }
    }

    public List<BidReciept> getAuctionsWinList(int userId) {
        try {
            LoggerService.logMethodExecution("getAuctionsWinList", userId);
            List<BidReciept> auctionsWinList = userRepository.getAuctionsWinList(userId);
            LoggerService.logMethodExecutionEnd("getAuctionsWinList", auctionsWinList);
            return auctionsWinList;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getAuctionsWinList", e);
            throw new OurRuntime("getAuctionsWinList: " + e.getMessage(), e);
        } catch (OurArg e) {
            LoggerService.logDebug("getAuctionsWinList", e);
            throw new OurArg("getAuctionsWinList: " + e.getMessage(), e);
        } catch (Exception e) {
            LoggerService.logError("getAuctionsWinList", e, userId);
            throw new OurRuntime("getAuctionsWinList: " + e.getMessage(), e);
        }
    }

    public void addAuctionWinBidToUserShoppingCart(int winnerId, Bid bid) {
        try {
            LoggerService.logMethodExecution("addAuctionWinBidToUserShoppingCart", winnerId, bid);
            userRepository.addAuctionWinBidToShoppingCart(winnerId, bid); // Add the auction win bid to the user's shopping cart
            LoggerService.logMethodExecutionEndVoid("addAuctionWinBidToUserShoppingCart");
        } catch (OurRuntime e) {
            LoggerService.logDebug("addAuctionWinBidToUserShoppingCart", e);
            throw new OurRuntime("addAuctionWinBidToUserShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (OurArg e) {
            LoggerService.logDebug("addAuctionWinBidToUserShoppingCart", e);
            throw new OurArg("addAuctionWinBidToUserShoppingCart: " + e.getMessage(), e); // Rethrow the custom exception
        } catch (Exception e) {
            LoggerService.logError("addAuctionWinBidToUserShoppingCart", e, winnerId, bid);
            throw new OurRuntime("addAuctionWinBidToUserShoppingCart: " + e.getMessage(), e);
        }
    }

    public int getShopOwner(int shopId) {
        try {
            LoggerService.logMethodExecution("getShopOwner", shopId);
            int shopOwner = userRepository.getShopOwner(shopId);
            LoggerService.logMethodExecutionEnd("getShopOwner", shopOwner);
            return shopOwner;
        } catch (OurRuntime e) {
            LoggerService.logDebug("getShopOwner", e);
            throw new OurRuntime("getShopOwner: " + e.getMessage(), e);
        } catch (OurArg e) {
                LoggerService.logDebug("getShopOwner", e);
                throw new OurArg("getShopOwner: " + e.getMessage(), e);
        } catch (Exception e) {
                    LoggerService.logError("getShopOwner", e, shopId);
                    throw new OurRuntime("getShopOwner: " + e.getMessage(), e);
        }
    }

    // public void clearAllBidsFromCloseShopByShopId(Integer shopId) {
    //     try{
    //         LoggerService.logMethodExecution("clearAllBidsFromCloseShopByShopId ", shopId);
    //         userRepository.clearAllBidsFromCloseShopByShopId(shopId);
    //         LoggerService.logMethodExecutionEndVoid("clearAllBidsFromCloseShopByShopId");
    //     }catch (OurRuntime e) {
    //         LoggerService.logDebug("clearAllBidsFromCloseShopByShopId", e);
    //         throw new OurRuntime("clearAllBidsFromCloseShopByShopId: " + e.getMessage(), e);
    //     } catch (OurArg e) {
    //             LoggerService.logDebug("clearAllBidsFromCloseShopByShopId", e);
    //             throw new OurArg("clearAllBidsFromCloseShopByShopId: " + e.getMessage(), e);
    //     } catch (Exception e) {
    //                 LoggerService.logError("clearAllBidsFromCloseShopByShopId", e, shopId);
    //                 throw new OurRuntime("clearAllBidsFromCloseShopByShopId: " + e.getMessage(), e);
    //     }
    // }

}
