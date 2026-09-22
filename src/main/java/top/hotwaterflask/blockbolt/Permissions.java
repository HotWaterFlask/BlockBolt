package top.hotwaterflask.blockbolt;

import java.util.Locale;

import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public final class Permissions {
    public static final String CAN_BYPASS = "blockbolt.bypass";
    public static final String CAN_ADMIN = "blockbolt.admin";
    public static final String CAN_PROTECT = "blockbolt.protect";
    public static final String CAN_RELOAD = "blockbolt.reload";
    public static final String CAN_WILDERNESS = "blockbolt.wilderness";

    public static final String LEGACY_CAN_BYPASS = "blocklocker.bypass";
    public static final String LEGACY_CAN_ADMIN = "blocklocker.admin";
    public static final String LEGACY_CAN_PROTECT = "blocklocker.protect";
    public static final String LEGACY_CAN_RELOAD = "blocklocker.reload";
    public static final String LEGACY_CAN_WILDERNESS = "blocklocker.wilderness";

    private static final String GROUP_PREFIX = "blockbolt.group.";
    private static final String LEGACY_GROUP_PREFIX = "blocklocker.group.";

    /**
     * Checks if a permissible has a permission node (supporting both blockbolt and legacy blocklocker nodes).
     *
     * @param permissible The player or console.
     * @param permission The permission to check.
     * @return True if permitted.
     */
    public static boolean has(Permissible permissible, String permission) {
        if (permissible.hasPermission(permission)) {
            return true;
        }
        if (permission.startsWith("blockbolt.")) {
            return permissible.hasPermission(permission.replaceFirst("blockbolt\\.", "blocklocker."));
        }
        return false;
    }

    /**
     * Gets the permission node that a player needs to have to be considered
     * part of a group.
     *
     * @param groupName
     *            The name of the group.
     * @return The permission node.
     */
    public static Permission getGroupNode(String groupName) {
        return new Permission(GROUP_PREFIX + groupName.toLowerCase(Locale.ROOT), PermissionDefault.FALSE);
    }

    public static Permission getLegacyGroupNode(String groupName) {
        return new Permission(LEGACY_GROUP_PREFIX + groupName.toLowerCase(Locale.ROOT), PermissionDefault.FALSE);
    }

    private Permissions() {
        // No instances!
    }
}

