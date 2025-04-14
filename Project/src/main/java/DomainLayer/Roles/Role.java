import java.util.ArrayList;
/**
 * Interface representing a Role in the system.
 * A role is associated with a shop and an assignee, and it has a set of permissions.
 */

public interface Role {
    
    /**
     * The shop ID of the role.
     */
    private int shopId;
    /**
     * The assignee ID of the role.
     */
    private int assigneeId;
    /**
     * The permissions associated with the role.
     */
    private ArrayList<Integer> permissions;

    /**
     * Adds a permission to the role.
     * @param permissionId The ID of the permission to add.
     * @return A message indicating the result of the operation.
     */
    String addPermission(int permissionId);
    /**
     * Removes a permission from the role.
     * @param permissionId The ID of the permission to remove.
     * @return A message indicating the result of the operation.
     */
    String removePermission(int permissionId);
    /**
     * Checks if the role has a specific permission.
     * @param permissionId The ID of the permission to check.
     * @return True if the role has the permission, false otherwise.
     */
    boolean hasPermission(int permissionId);
    /**
     * Gets the shop id of the role.
     * @return A shop id.
     */
    int getShopId();
    /**
     * Gets the assignee id of the role.
     * @return An assignee id.
     */
    int getAssigneeId();

}