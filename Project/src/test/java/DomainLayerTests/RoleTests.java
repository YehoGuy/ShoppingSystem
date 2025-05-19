package DomainLayerTests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.app.DomainLayer.Roles.PermissionsEnum;
import com.example.app.DomainLayer.Roles.Role;

public class RoleTests {
    private Role nullPermRole;
    private Role initialPermRole;
    private PermissionsEnum[] initialPerms;

    @BeforeEach
    void setUp() {
        nullPermRole = new Role(1, 2, null);

        initialPerms = new PermissionsEnum[]{
            PermissionsEnum.manageItems,
            PermissionsEnum.setPolicy
        };
        initialPermRole = new Role(3, 4, initialPerms);
    }

    @Test
    void testConstructorWithNullPermissions() {
        assertEquals(1, nullPermRole.getAssigneeId());
        assertEquals(2, nullPermRole.getShopId());
        assertEquals(0, nullPermRole.getPermissions().length);
    }

    @Test
    void testConstructorCopiesPermissions() {
        PermissionsEnum[] perms = initialPermRole.getPermissions();
        assertArrayEquals(initialPerms, perms);
        assertNotSame(initialPerms, perms, "Must copy the array, not keep reference");
    }

    @Test
    void testGetPermissionsReturnsDefensiveCopy() {
        PermissionsEnum[] copy = initialPermRole.getPermissions();
        copy[0] = PermissionsEnum.manageOwners;
        // original must not change
        assertEquals(PermissionsEnum.manageItems, initialPermRole.getPermissions()[0]);
    }

    @Test
    void testSetPermissionsReplacesAndCopies() {
        PermissionsEnum[] newPerms = { PermissionsEnum.closeShop };
        nullPermRole.setPermissions(newPerms);

        PermissionsEnum[] got = nullPermRole.getPermissions();
        assertArrayEquals(newPerms, got);
        assertNotSame(newPerms, got);
    }

    @Test
    void testSetFoundersPermissionsIncludesAllFounderFlags() {
        Role r = new Role(0, 0, null);
        r.setFoundersPermissions();

        // founders must have closeShop (so isFounder=true)
        assertTrue(r.hasPermission(PermissionsEnum.closeShop));
        assertTrue(r.isFounder());

        // they also get manageOwners (so isOwner=true)
        assertTrue(r.hasPermission(PermissionsEnum.manageOwners));
        assertTrue(r.isOwner());
    }

    @Test
    void testSetOwnersPermissionsDoesNotIncludeFounder() {
        Role r = new Role(0, 0, null);
        r.setOwnersPermissions();

        // owners get manageOwners but not closeShop
        assertTrue(r.hasPermission(PermissionsEnum.manageOwners));
        assertFalse(r.hasPermission(PermissionsEnum.closeShop));

        assertTrue(r.isOwner());
        assertFalse(r.isFounder());
    }

    @Test
    void testHasPermissionWhenMissing() {
        assertFalse(nullPermRole.hasPermission(PermissionsEnum.getHistory));
    }

    @Test
    void testAddPermissionThenHasPermissionAndNoDuplication() {
        Role r = new Role(0,0,null);
        r.addPermission(PermissionsEnum.getHistory);
        assertTrue(r.hasPermission(PermissionsEnum.getHistory));

        int before = r.getPermissions().length;
        r.addPermission(PermissionsEnum.getHistory);
        assertEquals(before, r.getPermissions().length, "Adding existing perm must not duplicate");
    }

    @Test
    void testRemovePermissions() {
        Role r = new Role(0,0,new PermissionsEnum[]{
            PermissionsEnum.handleMessages,
            PermissionsEnum.getHistory
        });
        r.removePermissions(PermissionsEnum.handleMessages);

        assertFalse(r.hasPermission(PermissionsEnum.handleMessages));
        assertTrue(r.hasPermission(PermissionsEnum.getHistory));

        // removing a non‐existent perm must not throw
        r.removePermissions(PermissionsEnum.manageItems);
        assertFalse(r.hasPermission(PermissionsEnum.manageItems));
    }

    @Test
    void testToStringShowsIdsAndPerms() {
        String s = initialPermRole.toString();
        assertTrue(s.contains("assigneeId=3"));
        assertTrue(s.contains("shopId=4"));
        assertTrue(s.contains("manageItems"));
        assertTrue(s.contains("setPolicy"));
    }

    @Test
    void testToNotificationShowsOnlyPermissions() {
        String notif = initialPermRole.toNotification();
        assertTrue(notif.contains("manageItems"));
        assertTrue(notif.contains("setPolicy"));
        assertFalse(notif.contains("assigneeId="));
        assertFalse(notif.contains("shopId="));
    }

    @Test
    void testIsOwnerAndIsFounderDefaultsFalse() {
        assertFalse(nullPermRole.isOwner());
        assertFalse(nullPermRole.isFounder());
    }
}
