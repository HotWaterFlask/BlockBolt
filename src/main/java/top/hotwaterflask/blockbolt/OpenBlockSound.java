package top.hotwaterflask.blockbolt;

import org.bukkit.Material;
import org.bukkit.Sound;

/**
 * Sounds for when a block is opened/closed.
 *
 */
public final class OpenBlockSound {

    @SuppressWarnings("deprecation")
    private static Sound getSoundOrFallback(String soundName, Sound fallback) {
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * Gets the sound for opening/closing the given material. For unknown
     * materials, a generic sound is returned.
     *
     * @param material
     *            The material.
     * @param open
     *            Whether the material is opened.
     * @return The sound.
     */
    public static Sound get(Material material, boolean open) {
        String name = material.name();

        if (name.contains("COPPER_DOOR")) {
            return getSoundOrFallback(open ? "BLOCK_COPPER_DOOR_OPEN" : "BLOCK_COPPER_DOOR_CLOSE",
                    open ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE);
        }
        if (name.contains("COPPER_TRAPDOOR")) {
            return getSoundOrFallback(open ? "BLOCK_COPPER_TRAPDOOR_OPEN" : "BLOCK_COPPER_TRAPDOOR_CLOSE",
                    open ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_TRAPDOOR_CLOSE);
        }
        if (name.equals("IRON_DOOR")) {
            return open ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE;
        }
        if (name.equals("IRON_TRAPDOOR")) {
            return open ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_TRAPDOOR_CLOSE;
        }

        if (name.endsWith("_FENCE_GATE")) {
            if (name.startsWith("CHERRY_")) {
                return getSoundOrFallback(open ? "BLOCK_CHERRY_WOOD_FENCE_GATE_OPEN" : "BLOCK_CHERRY_WOOD_FENCE_GATE_CLOSE",
                        open ? Sound.BLOCK_FENCE_GATE_OPEN : Sound.BLOCK_FENCE_GATE_CLOSE);
            }
            if (name.startsWith("BAMBOO_")) {
                return getSoundOrFallback(open ? "BLOCK_BAMBOO_WOOD_FENCE_GATE_OPEN" : "BLOCK_BAMBOO_WOOD_FENCE_GATE_CLOSE",
                        open ? Sound.BLOCK_FENCE_GATE_OPEN : Sound.BLOCK_FENCE_GATE_CLOSE);
            }
            if (name.startsWith("CRIMSON_") || name.startsWith("WARPED_")) {
                return getSoundOrFallback(open ? "BLOCK_NETHER_WOOD_FENCE_GATE_OPEN" : "BLOCK_NETHER_WOOD_FENCE_GATE_CLOSE",
                        open ? Sound.BLOCK_FENCE_GATE_OPEN : Sound.BLOCK_FENCE_GATE_CLOSE);
            }
            return open ? Sound.BLOCK_FENCE_GATE_OPEN : Sound.BLOCK_FENCE_GATE_CLOSE;
        }

        if (name.endsWith("_DOOR")) {
            if (name.startsWith("CHERRY_")) {
                return getSoundOrFallback(open ? "BLOCK_CHERRY_WOOD_DOOR_OPEN" : "BLOCK_CHERRY_WOOD_DOOR_CLOSE",
                        open ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE);
            }
            if (name.startsWith("BAMBOO_")) {
                return getSoundOrFallback(open ? "BLOCK_BAMBOO_WOOD_DOOR_OPEN" : "BLOCK_BAMBOO_WOOD_DOOR_CLOSE",
                        open ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE);
            }
            if (name.startsWith("CRIMSON_") || name.startsWith("WARPED_")) {
                return getSoundOrFallback(open ? "BLOCK_NETHER_WOOD_DOOR_OPEN" : "BLOCK_NETHER_WOOD_DOOR_CLOSE",
                        open ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE);
            }
            return open ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE;
        }

        if (name.endsWith("_TRAPDOOR")) {
            if (name.startsWith("CHERRY_")) {
                return getSoundOrFallback(open ? "BLOCK_CHERRY_WOOD_TRAPDOOR_OPEN" : "BLOCK_CHERRY_WOOD_TRAPDOOR_CLOSE",
                        open ? Sound.BLOCK_WOODEN_TRAPDOOR_OPEN : Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE);
            }
            if (name.startsWith("BAMBOO_")) {
                return getSoundOrFallback(open ? "BLOCK_BAMBOO_WOOD_TRAPDOOR_OPEN" : "BLOCK_BAMBOO_WOOD_TRAPDOOR_CLOSE",
                        open ? Sound.BLOCK_WOODEN_TRAPDOOR_OPEN : Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE);
            }
            if (name.startsWith("CRIMSON_") || name.startsWith("WARPED_")) {
                return getSoundOrFallback(open ? "BLOCK_NETHER_WOOD_TRAPDOOR_OPEN" : "BLOCK_NETHER_WOOD_TRAPDOOR_CLOSE",
                        open ? Sound.BLOCK_WOODEN_TRAPDOOR_OPEN : Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE);
            }
            return open ? Sound.BLOCK_WOODEN_TRAPDOOR_OPEN : Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE;
        }

        return open ? Sound.BLOCK_WOODEN_TRAPDOOR_OPEN : Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE;
    }
}
