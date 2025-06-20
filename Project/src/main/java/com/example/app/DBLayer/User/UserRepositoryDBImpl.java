package com.example.app.DBLayer.User;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.app.ApplicationLayer.OurRuntime;
import com.example.app.ApplicationLayer.Purchase.PaymentMethod;
import com.example.app.DomainLayer.Guest;
import com.example.app.DomainLayer.IUserRepository;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.Notification;
import com.example.app.DomainLayer.Purchase.Address;
import com.example.app.DomainLayer.Purchase.Bid;
import com.example.app.DomainLayer.Purchase.BidReciept;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;
import com.example.app.DomainLayer.ShoppingCart;
import com.example.app.DomainLayer.User;
import com.example.app.InfrastructureLayer.PasswordEncoderUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
@Profile("!no-db & !test")
public class UserRepositoryDBImpl implements IUserRepository {
    // This class implements UserRepositoryDB interface directly

    private final UserRepositoryDB jpaRepo;

    PasswordEncoderUtil passwordEncoderUtil = new PasswordEncoderUtil(); // Use the password encoder utility

    private AtomicInteger idCounter = new AtomicInteger(0);

    private final String adminUsername;
    private final String adminPlainPassword;
    private final String adminEmail;
    private final String adminPhoneNumber;
    private final String adminAddress;


    @PersistenceContext
    private EntityManager entityManager;

    private final ConcurrentHashMap<Integer, Guest> guests;

    public UserRepositoryDBImpl( @Value("${admin.username:admin}") String adminUsername,
        @Value("${admin.password:admin}") String adminPlainPassword,
        @Value("${admin.email:admin@mail.com}") String adminEmail,
        @Value("${admin.phoneNumber:0}") String adminPhoneNumber,
        @Value("${admin.address:admin st.}") String adminAddress,
        @Lazy @Autowired UserRepositoryDB jpaRepo) {

        this.adminUsername       = adminUsername;
        this.adminPlainPassword  = adminPlainPassword;
        this.adminEmail          = adminEmail;
        this.adminPhoneNumber    = adminPhoneNumber;
        this.adminAddress        = adminAddress;
        this.jpaRepo = jpaRepo;
        this.guests = new ConcurrentHashMap<>();
    }    
    private volatile boolean adminInitialized = false;    
    private void ensureAdminExists() {
        if (!adminInitialized) {
            synchronized (this) {
                if (!adminInitialized) {
                    try {
                        // Check if admin already exists first
                        List<Member> existingMembers = jpaRepo.findAll();
                        boolean adminExists = existingMembers.stream()
                            .anyMatch(member -> "admin".equals(member.getUsername()));
                        
                        if (!adminExists) {
                            // Create admin using the default constructor to let ID be auto-generated
                            Member admin = new Member();
                            admin.setUsername(adminUsername);
                            admin.setPassword(passwordEncoderUtil.encode(adminPlainPassword));
                            admin.setEmail(adminEmail);
                            admin.setPhoneNumber(adminPhoneNumber);
                            admin.setAdmin(true);
                            
                            admin = jpaRepo.save(admin); // Save and get the admin with generated ID
                        } 
                        adminInitialized = true;
                    } catch (Exception e) {
                        // Log the exception but don't fail initialization
                        System.err.println("Warning: Could not initialize admin user: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } else {
            // Even if flag is set, verify admin still exists in database (for transaction rollback scenarios)
            try {
                List<Member> existingMembers = jpaRepo.findAll();
                boolean adminExists = existingMembers.stream()
                    .anyMatch(member -> "admin".equals(member.getUsername()));
                
                if (!adminExists) {
                    // Reset flag and re-create admin
                    adminInitialized = false;
                    ensureAdminExists(); // Recursive call to create admin
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not verify admin existence: " + e.getMessage());
            }
        }
    }

    @Override
    public PasswordEncoderUtil getPasswordEncoderUtil() {
        return this.passwordEncoderUtil; // Return the password encoder utility
    }

    @Override
    public void setEncoderToTest(boolean b) {
        this.passwordEncoderUtil.setIsTest(b); // Set the encoder to test mode
    }    @Override
    public User getUserById(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("User ID cannot be negative: " + id);
        }
        
        ensureAdminExists();
        
        User u = jpaRepo.findById(id).orElse(null);
        if(u != null) {
            return u;
        } else {
            u = guests.get(id);
            if (u == null) {
                throw new OurRuntime("User not found: " + id);
            }
            return u;
        }
    }    @Override
    public Map<Integer, User> getUserMapping() {
        ensureAdminExists();
        List<Member> users = jpaRepo.findAll();
        Map<Integer, User> userMap = new java.util.HashMap<>();
        for (User user : users) {
            if (user instanceof Member) {
                userMap.put(((Member) user).getMemberId(), user);
            }
        }
        // Add guests to the map
        for (Map.Entry<Integer, Guest> entry : guests.entrySet()) {
            userMap.put(entry.getKey(), entry.getValue());
        }
        return userMap;
    }

    @Override
    public Member getMemberById(int id) {
        User user = getUserById(id);
        if (user == null) {
            throw new OurRuntime("User not found: " + id);
        }
        if (user instanceof Member) {
            return (Member) user;
        } else {
            throw new OurRuntime("User is not a member: " + id);
        }
    }

    @Override
    public boolean isAdmin(Integer id) {
        User user = getUserById(id);
        if (user instanceof Member) {
            return ((Member) user).isAdmin();
        } else {
            throw new OurRuntime("User is not a member: " + id);
        }
    }

    @Override
    public void addAdmin(Integer id) throws RuntimeException {
        User user = getUserById(id);
        if (user instanceof Member) {
            ((Member) user).setAdmin(true);
            jpaRepo.save((Member) user);
        } else {
            throw new OurRuntime("User is not a member: " + id);
        }
    }
    
    @Override
    public void removeAdmin(Integer id) throws RuntimeException {
        if (id == isUsernameAndPasswordValid("admin", "admin"))
            throw new OurRuntime("cant remove admin from the user who created the system");

        User user = getUserById(id);
        if (user instanceof Member) {
            ((Member) user).setAdmin(false);
            jpaRepo.save((Member) user);
        } else {
            throw new OurRuntime("User is not a member: " + id);
        }
    }
    
    @Override
    public List<Integer> getAllAdmins() {
        return jpaRepo.findAll().stream()
                .filter(user -> user instanceof Member && ((Member) user).isAdmin())
                .map(user -> ((Member) user).getMemberId())
                .collect(Collectors.toList());
    }

    @Override
    public int addGuest() {
        int id = idCounter.incrementAndGet();
        Guest guest = new Guest(id);
        guests.put(id, guest);
        return id;
    }

    @Override
    public boolean isGuestById(int id) {
        return guests.containsKey(id);
    }    @Override
    public int addMember(String username, String password, String email, String phoneNumber, String address) {
        if(!email.contains("@") || email.isEmpty()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        
        int id = idCounter.incrementAndGet();

        Member member = new Member(id, username, password, email, phoneNumber, address);
        jpaRepo.save(member);
        return member.getMemberId();
    }

    @Override
    public void updateMemberUsername(int id, String username) {
        Member member = getMemberById(id);
        member.setUsername(username);
        jpaRepo.save(member);
    }

    @Override
    public void updateMemberPassword(int id, String password) {
        Member member = getMemberById(id);
        member.setPassword(password);
        jpaRepo.save(member);
    }

    @Override
    public void updateMemberEmail(int id, String email) {
        Member member = getMemberById(id);
        member.setEmail(email);
        jpaRepo.save(member);
    }

    @Override
    public void updateMemberPhoneNumber(int id, String phoneNumber) {
        Member member = getMemberById(id);
        member.setPhoneNumber(phoneNumber);
        jpaRepo.save(member);
    }

    @Override
    public void updateMemberAddress(int id, String city, String street, int apartmentNum, String postalCode) {
        Member member = getMemberById(id);
        Address address = new Address()
                .withCity(city)
                .withStreet(street)
                .withApartmentNumber(apartmentNum)
                .withZipCode(postalCode);
        member.setAddress(address);
        jpaRepo.save(member);
    }    @Override
    public int isUsernameAndPasswordValid(String username, String password) {
        ensureAdminExists();
        List<Member> members = jpaRepo.findAll();
        
        for (Member member : members) {
            
            boolean usernameMatch = member.getUsername().equals(username);
            boolean passwordMatch = passwordEncoderUtil.matches(password, member.getPassword());

            
            if (usernameMatch && passwordMatch) {
                return member.getMemberId();
            }
        }
        return -1; // Invalid credentials
    }

    @Override
    public boolean isUsernameTaken(String username) {
        return jpaRepo.findAll().stream()
                .anyMatch(member -> member.getUsername().equals(username));
    }

    @Override
    public void removeUserById(int id) {
        User user = getUserById(id);
        if (user instanceof Member) {
            jpaRepo.delete((Member) user);
        } else {
            guests.remove(id);
        }
    }

    @Override
    public void clear() {
        jpaRepo.deleteAll();
        guests.clear();
        idCounter.set(0); // Reset the ID counter
    }

    @Override
    public List<User> getUsersList() {
        List<Member> lst = jpaRepo.findAll().stream()
                .collect(Collectors.toList());

        List<User> users = new LinkedList<>();
        users.addAll(lst);
        users.addAll(guests.values());

        return users;
    }

    @Override
    public List<Integer> getUsersIdsList() {
        return getUserMapping().keySet().stream()
                .collect(Collectors.toList());
    }

    @Override
    public List<Guest> getGuestsList() {
        return guests.values().stream()
                .filter(user -> user instanceof Guest)
                .map(user -> (Guest) user)
                .collect(Collectors.toList());
    }

    @Override
    public List<Member> getMembersList() {
        return jpaRepo.findAll().stream()
                .filter(user -> user instanceof Member)
                .map(user -> (Member) user)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isOwner(int userId, int shopId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            List<Role> lst = ((Member) user).getRoles();
            return lst.stream().anyMatch(role -> role.getShopId() == shopId && role.isOwner());
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }

    @Override
    public boolean isFounder(int userId, int shopId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            List<Role> lst = ((Member) user).getRoles();
            return lst.stream().anyMatch(role -> role.getShopId() == shopId && role.isFounder());
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }    @Override
    public void setPermissions(int userId, int shopId, Role role, PermissionsEnum[] permissions) {
        if (role == null) {
            throw new OurRuntime("Role cannot be null.");
        }
        if (permissions == null || permissions.length == 0) {
            throw new OurRuntime("Permissions cannot be null or empty.");
        }
        
        User user = getUserById(userId);
        if (user instanceof Member) {
            Member member = (Member) user;
            if (!member.getRoles().contains(role)) {
                throw new OurRuntime("User with ID " + userId + " does not have the specified role.");
            }
            
            member.removeRole(role);
            role.setPermissions(permissions);
            member.addRole(role);
            jpaRepo.save(member);
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }@Override
    public void addRoleToPending(int userId, Role role) {
        if (role == null) {
            throw new OurRuntime("Role cannot be null.");
        }
        
        User user = getUserById(userId);
        if (user instanceof Member) {
            Member member = (Member) user;
            
            // Check if user already has a role for this shop
            for (Role existingRole : member.getRoles()) {
                if (existingRole.getShopId() == role.getShopId()) {
                    throw new OurRuntime("User with ID " + userId + " already has a role for this shop.");
                }
            }
            
            // Check if user already has a pending role for this shop
            for (Role pendingRole : member.getPendingRoles()) {
                if (pendingRole.getShopId() == role.getShopId()) {
                    throw new OurRuntime("User with ID " + userId + " already has this role pending.");
                }
            }
            
            member.addRoleToPending(role);
            jpaRepo.save(member);
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }

    @Override
    public Role getRole(int memberId, int shopId) {
        User user = getUserById(memberId);
        if (user instanceof Member) {
            List<Role> roles = ((Member) user).getRoles();
            Role role = roles.stream()
                    .filter(r -> r.getShopId() == shopId)
                    .findFirst()
                    .orElse(null);
            if (role != null) {
                return role;
            } else {
                throw new OurRuntime("Role not found for member: " + memberId + " in shop: " + shopId);
            }
        } else {
            throw new OurRuntime("User is not a member: " + memberId);
        }
    }    @Override
    public void removeRole(int memberId, int shopId) {
        User user = getUserById(memberId);
        if (user instanceof Member) {
            Member member = (Member) user;
            List<Role> roles = member.getRoles();
            boolean roleRemoved = roles.removeIf(role -> role.getShopId() == shopId);
            if (!roleRemoved) {
                throw new OurRuntime("User with ID " + memberId + " does not have a role for shop ID " + shopId);
            }
            member.setRoles(roles);
            jpaRepo.save(member);
        } else {
            throw new OurRuntime("User is not a member: " + memberId);
        }
    }@Override
    public Role getPendingRole(int memberId, int shopId) {
        User user = getUserById(memberId);
        if (user instanceof Member) {
            List<Role> pendingRoles = ((Member) user).getPendingRoles();
            return pendingRoles.stream()
                    .filter(role -> role.getShopId() == shopId)
                    .findFirst()
                    .orElseThrow(() -> new OurRuntime("User with ID " + memberId + " does not have a pending role for shop ID " + shopId));
        } else {
            throw new OurRuntime("User is not a member: " + memberId);
        }
    }@Override
    public void acceptRole(int id, Role role) {
        if (role == null) {
            throw new OurRuntime("Role cannot be null.");
        }
        
        User user = getUserById(id);
        if (user instanceof Member) {
            ((Member) user).acceptRole(role);
            jpaRepo.save((Member) user);
        } else {
            throw new OurRuntime("User is not a member: " + id);
        }
    }

    @Override
    public void declineRole(int id, Role role) {
        if (role == null) {
            throw new OurRuntime("Role cannot be null.");
        }
        
        User user = getUserById(id);
        if (user instanceof Member) {
            ((Member) user).declineRole(role);
            jpaRepo.save((Member) user);
        } else {
            throw new OurRuntime("User is not a member: " + id);
        }
    }

    @Override
    public void addPermission(int id, PermissionsEnum permission, int shopId) {
        User user = getUserById(id);
        if (user instanceof Member) {
            ((Member) user).addPermission(shopId, permission);
            jpaRepo.save((Member) user);
        } else {
            throw new OurRuntime("User is not a member: " + id);
        }
    }

    @Override
    public void removePermission(int id, PermissionsEnum permission, int shopId) {
        User user = getUserById(id);
        if (user instanceof Member) {
            ((Member) user).removePermission(shopId, permission);
            jpaRepo.save((Member) user);
        } else {
            throw new OurRuntime("User is not a member: " + id);
        }
    }

    @Override
    public List<Integer> getShopIdsByWorkerId(int userId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            return ((Member) user).getRoles().stream()
                    .map(Role::getShopId)
                    .distinct()
                    .collect(Collectors.toList());
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }

    @Override
    public List<Member> getShopMembers(int shopId) {
        return jpaRepo.findAll().stream()
                .filter(user -> user instanceof Member)
                .map(user -> (Member) user)
                .filter(member -> member.getRoles().stream()
                        .anyMatch(role -> role.getShopId() == shopId))
                .collect(Collectors.toList());
    }

    @Override
    public void addNotification(int userId, String title, String message) {
        User user = getUserById(userId);
        if (user == null) {
            throw new OurRuntime("User not found: " + userId);
        }
        if (user instanceof Member) {
            Notification notification = new Notification(title, message);
            ((Member) user).addNotification(notification);
            jpaRepo.save((Member) user);
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }

    @Override
    public List<String> getNotificationsAndClear(int userId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new OurRuntime("User not found: " + userId);
        }
        if (user instanceof Member) {
            return ((Member) user).getNotificationsAndClear();
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }

    @Override
    public List<Member> getOwners(int shopId) {
        return jpaRepo.findAll().stream()
                .filter(user -> user instanceof Member)
                .map(user -> (Member) user)
                .filter(member -> member.getRoles().stream()
                        .anyMatch(role -> role.getShopId() == shopId && role.isOwner()))
                .collect(Collectors.toList());
    }

    @Override
    public void setSuspended(int userId, LocalDateTime suspended) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            ((Member) user).setSuspended(suspended);
            jpaRepo.save((Member) user);
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }

    @Override
    public void setUnSuspended(int userId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            ((Member) user).setUnSuspended();
            jpaRepo.save((Member) user);
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }

    @Override
    public boolean isSuspended(int userId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            return ((Member) user).isSuspended();
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }

    @Override
    public List<Integer> getSuspendedUsers() {
        return jpaRepo.findAll().stream()
                .filter(user -> user instanceof Member && ((Member) user).isSuspended())
                .map(user -> ((Member) user).getMemberId())
                .collect(Collectors.toList());
    }

    @Override
    public void banUser(int userId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            LocalDateTime suspendedUntil = LocalDateTime.of(9999, 12, 31, 23, 59);
            ((Member) user).setSuspended(suspendedUntil);
            jpaRepo.save((Member) user);
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }

    @Override
    public List<Role> getPendingRoles(int userId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            return ((Member) user).getPendingRoles();
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }

    @Override
    public List<Member> getAllMembers() {
        return jpaRepo.findAll().stream()
                .filter(user -> user instanceof Member)
                .map(user -> (Member) user)
                .collect(Collectors.toList());
    }

    @Override
    public List<Role> getAcceptedRoles(int userId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            return ((Member) user).getRoles();
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }

    @Override
    public void updateShoppingCartItemQuantity(int userId, int shopID, int itemID, boolean b) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            Member member = (Member) user;
            ShoppingCart cart = member.getShoppingCart();
            Map<Integer, Integer> basket = cart.getBasket(shopID);
            if (basket == null || !basket.containsKey(itemID)) {
                throw new OurRuntime("No shopping cart found for shop ID: " + shopID);
            }
            if (b) {
                basket.put(itemID, basket.get(itemID) + 1);
            } else {
                if (basket.get(itemID) > 1) {
                    basket.put(itemID, basket.get(itemID) - 1);
                } else {
                    basket.remove(itemID);
                }
            }
            jpaRepo.save((Member) user);
        } else {
            Guest guest = guests.get(userId);
            if (guest == null) {
                throw new OurRuntime("Guest not found: " + userId);
            }
            ShoppingCart cart = guest.getShoppingCart();
            Map<Integer, Integer> basket = cart.getBasket(shopID);
            if (basket == null || !basket.containsKey(itemID)) {
                throw new OurRuntime("No shopping cart found for shop ID: " + shopID);
            }
            if (b) {
                basket.put(itemID, basket.get(itemID) + 1);
            } else {
                if (basket.get(itemID) > 1) {
                    basket.put(itemID, basket.get(itemID) - 1);
                } else {
                    basket.remove(itemID);
                }
            }
        }
    }

    @Override
    public void removeShoppingCartItem(int userId, int shopID, int itemID) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            Member member = (Member) user;
            ShoppingCart cart = member.getShoppingCart();
            Map<Integer, Integer> basket = cart.getBasket(shopID);
            if (basket == null || !basket.containsKey(itemID)) {
                throw new OurRuntime("No shopping cart found for shop ID: " + shopID);
            }
            basket.remove(itemID);
            jpaRepo.save(member);
        } else {
            Guest guest = guests.get(userId);
            if (guest == null) {
                throw new OurRuntime("Guest not found: " + userId);
            }
            ShoppingCart cart = guest.getShoppingCart();
            Map<Integer, Integer> basket = cart.getBasket(shopID);
            if (basket == null || !basket.containsKey(itemID)) {
                throw new OurRuntime("No shopping cart found for shop ID: " + shopID);
            }
            basket.remove(itemID);
        }
    }

    @Override
    public List<BidReciept> getAuctionsWinList(int userId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            return ((Member) user).getAuctionsWins();
        } else {
            throw new OurRuntime("User is not a member: " + userId);
        }
    }

    @Override
    public void addAuctionWinBidToShoppingCart(int winnerId, Bid bid){
        User user = getUserById(winnerId);
        if (user instanceof Member) {
            Member member = (Member) user;
            BidReciept bidReciept = bid.generateReciept();
            member.addAuctionWin(bidReciept);
            jpaRepo.save(member);
        } else {
            throw new OurRuntime("User is not a member: " + winnerId);
        }
    }

    @Override
    public ShoppingCart getShoppingCartById(int userId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            return ((Member) user).getShoppingCart();
        } else if (user instanceof Guest) {
            return ((Guest) user).getShoppingCart();
        } else {
            throw new OurRuntime("User is not a member or guest: " + userId);
        }
    }    @Override
    public void addItemToShoppingCart(int userId, int shopId, int itemId, int quantity) {
        if (quantity <= 0) {
            throw new OurRuntime("Quantity must be greater than 0.");
        }
        
        User user = getUserById(userId);
        if (user instanceof Member) {
            Member member = (Member) user;
            ShoppingCart cart = member.getShoppingCart();
            cart.addItem(shopId, itemId, quantity);
            jpaRepo.save(member);
        } else {
            Guest guest = guests.get(userId);
            if (guest == null) {
                throw new OurRuntime("Guest not found: " + userId);
            }
            ShoppingCart cart = guest.getShoppingCart();
            cart.addItem(shopId, itemId, quantity);
        }
    }

    @Override
    public void removeItemFromShoppingCart(int userId, int shopId, int itemId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            Member member = (Member) user;
            ShoppingCart cart = member.getShoppingCart();
            cart.removeItem(shopId, itemId);
            jpaRepo.save(member);
        } else {
            Guest guest = guests.get(userId);
            if (guest == null) {
                throw new OurRuntime("Guest not found: " + userId);
            }
            ShoppingCart cart = guest.getShoppingCart();
            cart.removeItem(shopId, itemId);
        }
    }

    @Override
    public void updateItemQuantityInShoppingCart(int userId, int shopId, int itemId, int quantity) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            Member member = (Member) user;
            ShoppingCart cart = member.getShoppingCart();
            cart.removeItem(shopId, itemId); // Remove the item first
            cart.addItem(shopId, itemId, quantity); // Then add it back with the new quantity
            jpaRepo.save(member);
        } else {
            Guest guest = guests.get(userId);
            if (guest == null) {
                throw new OurRuntime("Guest not found: " + userId);
            }
            ShoppingCart cart = guest.getShoppingCart();
            cart.removeItem(shopId, itemId); // Remove the item first
            cart.addItem(shopId, itemId, quantity); // Then add it back with the new quantity
        }
    }

    @Override
    public void clearShoppingCart(int userId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            Member member = (Member) user;
            member.getShoppingCart().clearCart();
            jpaRepo.save(member);
        } else {
            Guest guest = guests.get(userId);
            if (guest == null) {
                throw new OurRuntime("Guest not found: " + userId);
            }
            guest.getShoppingCart().clearCart();
        }
    }

    @Override
    public Map<Integer, Integer> getBasket(int userId, int shopId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            ShoppingCart cart = ((Member) user).getShoppingCart();
            return cart.getBasket(shopId);
        } else {
            Guest guest = guests.get(userId);
            if (guest == null) {
                throw new OurRuntime("Guest not found: " + userId);
            }
            return guest.getShoppingCart().getBasket(shopId);
        }
    }

    @Override
    public void createBasket(int userId, int shopId) {
        User user = getUserById(userId);
        if (user instanceof Member) {
            ShoppingCart cart = ((Member) user).getShoppingCart();
            cart.addBasket(shopId);
            jpaRepo.save((Member) user);
        } else {
            Guest guest = guests.get(userId);
            if (guest == null) {
                throw new OurRuntime("Guest not found: " + userId);
            }
            guest.getShoppingCart().addBasket(shopId);
        }
    }

    @Override
    public void setPaymentMethod(int userId, int shopId, PaymentMethod paymentMethod) {
        User user = getUserById(userId);
        if (user == null) {
            throw new OurRuntime("User not found: " + userId);
        }
        user.setPaymentMethod(paymentMethod);
    }

    @Override
    public int pay(int userId, double amount, String currency, String cardNumber, String expirationDateMonth, String expirationDateYear, String cardHolderName, String cvv, String id) {
        User user = getUserById(userId);
        if (user == null) {
            throw new OurRuntime("User not found: " + userId);
        }
        PaymentMethod paymentMethod = user.getPaymentMethod();
        if (paymentMethod == null) {
            throw new OurRuntime("Payment method not set for user: " + userId);
        }
        
        try {
            int pid = paymentMethod.processPayment(amount, currency, cardNumber, expirationDateMonth, expirationDateYear, cardHolderName, cvv, id);
            if (pid < 0) {
                throw new OurRuntime("Payment failed for user: " + userId);
            }
            return pid;
        } catch (Exception e) {
            throw new OurRuntime("Payment processing error for user: " + userId + ", error: " + e.getMessage(), e);
        }
    }

    @Override
    public void refund(int userId, int paymentId) {
        User user = getUserById(userId);
        if (user == null) {
            throw new OurRuntime("User not found: " + userId);
        }
        PaymentMethod paymentMethod = user.getPaymentMethod();
        if (paymentMethod == null) {
            throw new OurRuntime("Payment method not set for user: " + userId);
        }
        
        try {
            boolean b = paymentMethod.cancelPayment(paymentId);
            if (!b) {
                throw new OurRuntime("Refund failed for user: " + userId + ", payment ID: " + paymentId);
            }
        } catch (Exception e) {
            throw new OurRuntime("Refund processing error for user: " + userId + ", error: " + e.getMessage(), e);
        }
    }

    @Override
    public void addBidToShoppingCart(int userId, int shopId, Map<Integer, Integer> items)
    {
        User user = getUserById(userId);
        if (user instanceof Member) {
            Member member = (Member) user;
            ShoppingCart cart = member.getShoppingCart();
            cart.addBid(shopId, items);
            jpaRepo.save(member);
        } else {
            Guest guest = guests.get(userId);
            if (guest == null) {
                throw new OurRuntime("Guest not found: " + userId);
            }
            ShoppingCart cart = guest.getShoppingCart();
            cart.addBid(shopId, items);
        }
    }

    @Override
    public void updateUserInDB(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("Member cannot be null.");
        }
        
        
        jpaRepo.save(member);
        
    }
}
