package Domain;

/**
 * The RolesEnum enumeration defines various roles that can be assigned
 * within the shopping system. Each role represents a specific set of
 * permissions or actions that a user can perform.
 */
public enum PermissionsEnum {
    /**
     * enables additions, deletions, and modifications of items in the shop.
     */
    manageItems,
    /**
     * enables management of buying and discount policies in the shop.
     */
    setPolicy,
    /**
     * enables adding and removing owners.
     */
    manageOwners,
    /**
     * enables leaving the shop as an owner.
     * This permission is only relevant for owners.
     * when an owner is leaving the shop, all his employees are removed from the
     * shop.
     */
    leaveShopAsOwner,
    /**
     * enables leaving the shop as a manager.
     * This permission is only relevant for managers.
     */
    leaveShopAsManager,
    /**
     * enables adding and removing managers.
     * This permission is only relevant for owners.
     * also can set manager's permissions.
     */
    manageManagers,
    /**
     * enables closing the shop.
     * This permission is only relevant for founders.
     */
    closeShop,
    /**
     * enables opening the shop.
     * This permission is only relevant for founders.
     */
    openClosedShop,
    /**
     * enables getting staff information.
     */
    getStaffInfo,
    /**
     * enables getting messages from customers, and answering to them.
     */
    handleMessages,
    /**
     * enables getting buying history of the shop.
     */
    getHistory,

    /**
     * enables suspending users.
     */
    suspension
}
