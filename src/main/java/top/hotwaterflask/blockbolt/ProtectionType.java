package top.hotwaterflask.blockbolt;

import top.hotwaterflask.blockbolt.protection.AttachedProtection;
import top.hotwaterflask.blockbolt.protection.ContainerProtection;
import top.hotwaterflask.blockbolt.protection.DoorProtection;

/**
 * The different types of protections.
 *
 */
public enum ProtectionType {
    /**
     * A container, represented by {@link ContainerProtection}.
     */
    CONTAINER,
    /**
     * A door, represented by {@link DoorProtection}.
     */
    DOOR,
    /**
     * A block where signs can also be attached to the block it is
     * hanging/standing on. Represented {@link AttachedProtection}.
     */
    ATTACHABLE;
}