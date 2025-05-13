package DomainLayerTests;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;

public class MemberTest {

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member(1, "user1", "pass1", "user1@mail.com", "1234567890", "Address 1");
    }

    @Test
    void testMemberInitialization() {
        assertEquals(1, member.getMemberId());
        assertEquals("user1", member.getUsername());
        assertEquals("pass1", member.getPassword());
        assertEquals("user1@mail.com", member.getEmail());
        assertEquals("1234567890", member.getPhoneNumber());
    }

    @Test
    void testSetters() {
        member.setUsername("newUser");
        member.setPassword("newPass");
        member.setEmail("new@mail.com");
        member.setPhoneNumber("999");

        assertEquals("newUser", member.getUsername());
        assertEquals("newPass", member.getPassword());
        assertEquals("new@mail.com", member.getEmail());
        assertEquals("999", member.getPhoneNumber());
    }

    @Test
    void testOrderHistory() {
        member.addOrderToHistory(1001);
        member.addOrderToHistory(1002);

        List<Integer> history = member.getOrderHistory();
        assertEquals(2, history.size());
        assertTrue(history.contains(1001));
        assertTrue(history.contains(1002));
    }

    @Test
    void testAddAndRemoveRole() {
        Role role = new Role(1, 10, new PermissionsEnum[]{PermissionsEnum.manageItems});
        member.addRole(role);
        assertTrue(member.hasRole(role));

        member.removeRole(role);
        assertFalse(member.hasRole(role));
    }

    @Test
    void testAddRoleToPendingAndAccept() {
        Role role = new Role(1, 20, new PermissionsEnum[]{PermissionsEnum.getHistory});
        member.addRoleToPending(role);

        assertThrows(IllegalArgumentException.class, () -> member.acceptRole(new Role(1, 21, new PermissionsEnum[]{})));

        member.acceptRole(role);
        assertTrue(member.hasRole(role));
    }

    @Test
    void testDeclineRole() {
        Role role = new Role(1, 30, new PermissionsEnum[]{PermissionsEnum.setPolicy});
        member.addRoleToPending(role);

        assertThrows(IllegalArgumentException.class, () -> member.declineRole(new Role(1, 31, new PermissionsEnum[]{})));

        member.declineRole(role);
        assertFalse(member.getRoles().contains(role)); // Not accepted
    }

    @Test
    void testAddRemoveHasPermission() {
        Role role = new Role(1, 50, new PermissionsEnum[]{PermissionsEnum.manageItems});
        member.addRole(role);

        assertTrue(member.hasPermission(PermissionsEnum.manageItems, 50));
        assertFalse(member.hasPermission(PermissionsEnum.setPolicy, 50));

        member.addPermission(50, PermissionsEnum.setPolicy);
        assertTrue(member.hasPermission(PermissionsEnum.setPolicy, 50));

        member.removePermission(50, PermissionsEnum.manageItems);
        assertFalse(member.hasPermission(PermissionsEnum.manageItems, 50));
    }

    @Test
    void testEquals() {
        Member sameMember = new Member(1, "otherUser", "otherPass", "other@mail.com", "000", "Other Address");
        Member differentMember = new Member(2, "diffUser", "diffPass", "diff@mail.com", "111", "Diff Address");

        assertTrue(member.equals(sameMember));
        assertFalse(member.equals(differentMember));
    }
}
