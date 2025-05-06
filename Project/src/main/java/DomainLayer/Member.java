package DomainLayer;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import DomainLayer.Purchase.Address;
import DomainLayer.Roles.PermissionsEnum;
import DomainLayer.Roles.Role;

public class Member extends User {
    final private int memberId;
    private volatile String username; // Username of the user
    private volatile String password; // Password of the user
    private volatile String email; // Email address of the user
    private volatile String phoneNumber; // Phone number of the user
    private volatile LocalDateTime suspended; // Suspension status of the user
    
    private final List<Role> roles; // List of roles associated with the user
    private final List<Integer> orderHistory;// List of order IDs
    private final List<Role> pending_roles; // List of pending roles not yet confirmed/declined by the user
    private final List<Notification> notifications; // List of notifications for the user

    private final Object rolesLock = new Object();
    private final Object pendingRolesLock = new Object();
    private final Object orderHistoryLock = new Object();
    private final Object notificationsLock = new Object();  

    public Member(int memberId, String username, String password, String email, String phoneNumber, String addressToRemove) {
        super(memberId); // Call the User class constructor
        this.memberId = memberId; // Initialize member ID
        this.username = username; // Initialize username
        this.password = password; // Initialize password
        this.email = email; // Initialize email address
        this.phoneNumber = phoneNumber; // Initialize phone number
        this.suspended = LocalDateTime.now(); // Initialize suspension status (not suspended)
        this.orderHistory = new CopyOnWriteArrayList<>(); // Initialize order history
        this.roles = new CopyOnWriteArrayList<>(); // Initialize roles
        this.pending_roles = new CopyOnWriteArrayList<>(); // Initialize pending roles
        this.notifications = new CopyOnWriteArrayList<>(); // Initialize notifications

    }

    public Member(int memberId, String username, String password, String email, String phoneNumber, Address address) {
        super(memberId); // Call the User class constructor
        this.memberId = memberId; // Initialize member ID
        this.username = username; // Initialize username
        this.password = password; // Initialize password
        this.email = email; // Initialize email address
        this.phoneNumber = phoneNumber; // Initialize phone number
        this.suspended = LocalDateTime.now(); // Initialize suspension status (not suspended)
        this.orderHistory = new CopyOnWriteArrayList<>(); // Initialize order history
        this.roles = new CopyOnWriteArrayList<>(); // Initialize roles
        this.pending_roles = new CopyOnWriteArrayList<>(); // Initialize pending roles
        this.address = address;
        this.notifications = new CopyOnWriteArrayList<>(); // Initialize notifications
    }

    public int getMemberId() {
        return memberId; // Return the member ID
    }

    public String getUsername() {
        return username; // Return the username
    }

    public String getPassword() {
        return password; // Return the password
    }

    public String getEmail() {
        return email; // Return the email address
    }

    public String getPhoneNumber() {
        return phoneNumber; // Return the phone number
    }

    public Boolean isSuspended() {
        return suspended.isAfter(LocalDateTime.now()); // Check if the user is suspended
    }

    public void setSuspended(LocalDateTime suspended) {
        this.suspended = suspended; // Set the suspension status
    }

    public synchronized void setUsername(String username) {
        this.username = username; // Set the username
    }

    public synchronized void setPassword(String password) {
        this.password = password; // Set the password
    }

    public synchronized void setEmail(String email) {
        this.email = email; // Set the email address
    }

    public synchronized void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber; // Set the phone number
    }

    public List<Integer> getOrderHistory() {
        synchronized (orderHistoryLock) {
            return new CopyOnWriteArrayList<>(orderHistory);
        } // Return a copy of the order history
    }

    public void addOrderToHistory(int orderId) {
        synchronized (orderHistoryLock) {
            orderHistory.add(orderId);
        } // Add an order ID to the order history
    }

    public List<Role> getRoles() {
        synchronized (rolesLock) {
            return new CopyOnWriteArrayList<>(roles);
        } // Return a copy of the roles
    }

    public void addRoleToPending(Role role) {
        synchronized (pendingRolesLock) {
            pending_roles.add(role);
        } // Add a role to the list of pending roles
    }

    public void addRole(Role role) {
        synchronized (rolesLock) {
            roles.add(role);
        } // Add a role to the list of roles
    }

    public void removeRole(Role role) {
        synchronized (rolesLock) {
            roles.remove(role);
        } // Remove a role from the list of roles
    }

    public boolean hasRole(Role role) {
        synchronized (rolesLock) {
            return roles.contains(role);
        } // Check if the user has a specific role
    }


    public boolean equals(Object obj) {
        if (this == obj) return true; // Check if the same object
        if (obj == null || getClass() != obj.getClass()) return false; // Check for null or different class
        Member member = (Member) obj; // Cast to Member
        return memberId == member.memberId; // Compare member IDs
    }

    public void acceptRole(Role role) {
        synchronized (pendingRolesLock) {
            if (pending_roles.contains(role)) {
                pending_roles.remove(role);
                synchronized (rolesLock) {
                    roles.add(role);
                }
            } else {
                throw new IllegalArgumentException("Role not found in pending roles.");
            }
        }
    }

    public void declineRole(Role role) {
        synchronized (pendingRolesLock) {
            if (pending_roles.contains(role)) {
                pending_roles.remove(role);
            } else {
                throw new IllegalArgumentException("Role not found in pending roles.");
            }
        }
    }

    public void addPermission(int shopId, PermissionsEnum permission) {
        synchronized (rolesLock) {
            for (Role role : roles) {
                if (role.getShopId() == shopId) {
                    role.addPermission(permission);
                    return;
                }
            }
        }
        throw new RuntimeException("member has no role in this shop");
    }

    public void removePermission(int shopId, PermissionsEnum permission) {
        synchronized (rolesLock) {
            for (Role role : roles) {
                if (role.getShopId() == shopId) {
                    role.removePermissions(permission);
                    return;
                }
            }
        }
    }
    public boolean hasPermission(PermissionsEnum permission, int shopId) {
        synchronized (rolesLock) {
            for (Role role : roles) {
                if (role.getShopId() == shopId && role.hasPermission(permission)) {
                    return true;
                }
            }
        }
        return false;

    }

    public List<Role> getPendingRoles() {
        synchronized (pendingRolesLock) {
            return new CopyOnWriteArrayList<>(pending_roles);
        } // Return a copy of the pending roles
    }

    public void setPendingRoles(List<Role> newPendingRoles) {
        synchronized (pendingRolesLock) {
            this.pending_roles.clear();
            this.pending_roles.addAll(newPendingRoles);
        }
    }
    
    public void setRoles(List<Role> newRoles) {
        synchronized (rolesLock) {
            this.roles.clear();
            this.roles.addAll(newRoles);
        }
    }

    public void setOrderHistory(List<Integer> newOrderHistory) {
        synchronized (orderHistoryLock) {
            this.orderHistory.clear();
            this.orderHistory.addAll(newOrderHistory);
        }
    }

    public void addNotification(Notification notification) {
        synchronized (notificationsLock) {
            notifications.add(notification);
        }
    }

    /**
     * Atomically returns all pending notifications and clears them,
     * without losing any that arrive concurrently.
     */
    public List<Notification> getNotificationsAndClear() {
        synchronized (notificationsLock) {
            // snapshot current list
            List<Notification> snapshot = new ArrayList<>(notifications);
            // clear the underlying list
            notifications.clear();
            // return what was there
            return snapshot;
        }
    }
}
