package top.hotwaterflask.blockbolt.impl.event;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

import top.hotwaterflask.blockbolt.Permissions;
import top.hotwaterflask.blockbolt.SearchMode;
import top.hotwaterflask.blockbolt.Translator.Translation;
import top.hotwaterflask.blockbolt.impl.BlockBoltPluginImpl;
import top.hotwaterflask.blockbolt.impl.blockfinder.BlockFinder;
import top.hotwaterflask.blockbolt.location.IllegalLocationException;
import top.hotwaterflask.blockbolt.protection.Protection;

public final class BlockPlaceListener extends EventListener {

    private final Set<UUID> doNotSendChestHintPlayerIds = new HashSet<>();

    public BlockPlaceListener(BlockBoltPluginImpl plugin) {
        super(plugin);
    }

    /**
     * Prevents unauthorized players from placing blocks adjacent to or against protected blocks.
     * Also sends a chest hint when placing a chest.
     *
     * @param event
     *            The block place event.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        Optional<Protection> interferingProtection = willInterfereWith(player, event);
        if (interferingProtection.isPresent()) {
            // Not allowed to place a block here, would interfere with an existing protection
            if (!Permissions.has(player, Permissions.CAN_ADMIN)) {
                plugin.getTranslator().sendMessage(player, Translation.PROTECTION_NO_ACCESS, interferingProtection.get()
                        .getOwnerDisplayName());
                event.setCancelled(true);
                return;
            } else {
                plugin.getTranslator().sendMessage(player, Translation.PROTECTION_BYPASSED, interferingProtection.get()
                        .getOwnerDisplayName());
                return;
            }
        }

        // Check if sign placing hint must be shown for chest
        if (event.getBlockPlaced().getType() != Material.CHEST) {
            return;
        }

        if (!Permissions.has(player, Permissions.CAN_PROTECT)) {
            return;
        }

        try {
            plugin.getLocationCheckers().checkLocationAndPermission(player, event.getBlockPlaced());
        } catch (IllegalLocationException e) {
            return; // Cannot place protection here, so don't show hint
        }

        sendChestHint(player);
    }

    /**
     * Sends a chest hint. Includes a simple spam limiter.
     *
     * @param player
     *            The player.
     */
    private void sendChestHint(Player player) {
        UUID playerId = player.getUniqueId();
        synchronized (this.doNotSendChestHintPlayerIds) {
            if (this.doNotSendChestHintPlayerIds.contains(playerId)) {
                return;
            }
            plugin.getTranslator().sendMessage(player, Translation.PROTECTION_CHEST_HINT);

            // Temporarily suppress chest hint (120 seconds)
            this.doNotSendChestHintPlayerIds.add(playerId);
        }
        plugin.runLaterGlobally(() -> {
            synchronized (this.doNotSendChestHintPlayerIds) {
                this.doNotSendChestHintPlayerIds.remove(playerId);
            }
        }, 20 * 120);
    }

    /**
     * Checks if placing this block interferes with any nearby protected blocks owned by someone else.
     *
     * @param player
     *            The player placing the block.
     * @param event
     *            The place event.
     * @return The protection being interfered with, if any.
     */
    private Optional<Protection> willInterfereWith(Player player, BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        Block againstBlock = event.getBlockAgainst();

        // 1. Check if the block being placed against is protected by someone else
        if (againstBlock != null && !againstBlock.equals(placedBlock)) {
            Optional<Protection> againstProtection = getProtectionBySomeoneElse(player, againstBlock);
            if (againstProtection.isPresent()) {
                return againstProtection;
            }
        }

        // 2. Check all 6 surrounding faces of the placed block (including supporting blocks)
        for (BlockFace searchFace : BlockFinder.NORTH_EAST_SOUTH_WEST_UP_DOWN) {
            Block nearBlock = placedBlock.getRelative(searchFace);
            Optional<Protection> protection = getProtectionBySomeoneElse(player, nearBlock);
            if (protection.isPresent()) {
                return protection;
            }
        }

        return Optional.empty();
    }


}
