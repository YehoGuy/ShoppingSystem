
package InfrastructureLayerTests;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import DomainLayer.Guest;
import DomainLayer.Member;
import DomainLayer.User;
import InfrastructureLayer.UserRepository;
    
public class UserRepositoryTests {

    private UserRepository repo;
    private User guest;
    private User member;    
    private int guestId;
    private int memberId;

    @BeforeEach
    public void setup() {
        repo = new UserRepository();    
        guestId = repo.addGuest();  
        guest = repo.getUserById(guestId);
        repo.addMember("username", "password", "email@example.com", "111", "address");
        memberId = repo.isUsernameAndPasswordValid("username", "password");
        member = repo.getUserById(memberId);
    }

    @Test
    public void testAddGuestAndGetUserById() {
        assertEquals(guestId, ((Guest)guest).getGuestId());
    }

    @Test
    public void testAddMember() {
        assertEquals("username", ((Member)member).getUsername());
        assertEquals("password", ((Member)member).getPassword());
        assertEquals("email@example.com", ((Member)member).getEmail());
        assertEquals("111", ((Member)member).getPhoneNumber());
        assertEquals("address", ((Member)member).getAddress());
    }


    @Test
    public void testUpdateMemberUsername() {
        repo.updateMemberUsername(memberId, "newUsername");
        assertEquals("newUsername", ((Member)member).getUsername());
    }

    @Test
    public void testUpdateMemberPassword() {
        repo.updateMemberPassword(memberId, "newPassword");
        assertEquals("newPassword", ((Member)member).getPassword());
    }

    @Test
    public void testUpdateMemberEmail() {
        repo.updateMemberEmail(memberId, "newemail@example.com");
        assertEquals("newemail@example.com", ((Member)member).getEmail());
    }

    @Test
    public void testUpdateMemberPhoneNumber() {
        repo.updateMemberPhoneNumber(memberId, "123456789");
        assertEquals("123456789", ((Member)member).getPhoneNumber());
    }   

    @Test
    public void testUpdateMemberAddress() {
        repo.updateMemberAddress(memberId, "newAddress");
        assertEquals("newAddress", ((Member)member).getAddress());
    }


       
    @Test
    public void testIsUsernameAndPasswordValid() {
        assertNotEquals(-1, memberId);
        assertEquals(((Member)member).getMemberId(), memberId);
        int invalidUserId = repo.isUsernameAndPasswordValid("invalidUser", "invalidPass");
        assertEquals(-1, invalidUserId);
    }

    @Test
    public void testIsUsernameTaken() {
        assertTrue(repo.isUsernameTaken("username"));
        assertFalse(repo.isUsernameTaken("availableUsername"));
    }

    @Test
    public void testIsGuestById() {
        assertTrue(repo.isGuestById(guestId));
        assertFalse(repo.isGuestById(-1)); // Non-existent ID
    }

    @Test
    public void testRemoveUserById() {
        repo.removeUserById(guestId);
        List<Guest> guests = repo.getGuestsList();
        assertEquals(0, guests.size()); // Assuming only one guest is added in setup
        assertFalse(repo.isGuestById(guestId)); // Check if the guest is removed
    }

    
    
    @Test
    public void testGetUserMapping() {
        Map<Integer, User> userMapping = repo.getUserMapping();
        assertTrue(userMapping.containsKey(guestId));
        assertEquals(guest, userMapping.get(guestId));
    }

    @Test
    public void testGetUsersList() {
        List<User> users = repo.getUsersList();
        assertTrue(users.contains(guest));
    }

    @Test
    public void testGetUsersIdsList() {
        List<Integer> userIds = repo.getUsersIdsList();
        assertTrue(userIds.contains(guestId));
    }

    @Test
    public void testGetGuestsList() {
        List<Guest> guests = repo.getGuestsList();
        assertTrue(guests.contains((Guest)guest));
    }


    @Test
    public void testGetMembersList() {
        List<Member> members = repo.getMembersList();
        assertTrue(members.contains((Member)member));
        assertFalse(members.contains((Guest)guest));
        assertEquals(1, members.size()); // Assuming only one member is added in setup
    }

    @Test
    public void testClear() {
        repo.addGuest();
        repo.addGuest();
        repo.clear();
        assertTrue(repo.getUsersList().isEmpty());
    }

}