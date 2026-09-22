package top.hotwaterflask.blockbolt.impl.event;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import top.hotwaterflask.blockbolt.ProtectionCache;
import top.hotwaterflask.blockbolt.SearchMode;
import top.hotwaterflask.blockbolt.impl.BlockBoltPluginImpl;
import top.hotwaterflask.blockbolt.impl.blockfinder.BlockFinder;
import top.hotwaterflask.blockbolt.profile.PlayerProfile;
import top.hotwaterflask.blockbolt.profile.Profile;
import top.hotwaterflask.blockbolt.protection.Protection;

abstract class EventListener implements Listener {

    final BlockBoltPluginImpl plugin;

    EventListener(BlockBoltPluginImpl plugin) {
        Validate.notNull(plugin);
        this.plugin = plugin;
    }

    boolean anyProtected(Collection<Block> blocks) {
        for (Block block : blocks) {
            if (isProtected(block)) {
                return true;
            }
        }
        return false;
    }

    boolean isExpired(Protection protection) {
        Optional<Date> cutoffDate = plugin.getChestSettings().getChestExpireDate();
        if (cutoffDate.isPresent()) {
            return protection.isExpired(cutoffDate.get());
        }
        return false;
    }

    Optional<Protection> getProtectionBySomeoneElse(Player player, Block block) {
        return getProtectionBySomeoneElse(player, block, SearchMode.ALL);
    }

    Optional<Protection> getProtectionBySomeoneElse(Player player, Block block, SearchMode searchMode) {
        return plugin.getProtectionFinder().findProtection(block, searchMode).filter(protection -> {
            if (isExpired(protection)) {
                return false;
            }
            PlayerProfile playerProfile = plugin.getProfileFactory().fromPlayer(player);
            return !protection.isOwner(playerProfile);
        });
    }

    boolean isProtected(Block block) {
        return plugin.getProtectionFinder().findProtection(block).isPresent();
    }

    boolean isProtectedOrAdjacent(Block block) {
        if (isProtected(block)) {
            return true;
        }
        for (BlockFace face : BlockFinder.NORTH_EAST_SOUTH_WEST_UP_DOWN) {
            if (plugin.getProtectionFinder().findProtection(block.getRelative(face), SearchMode.NO_SUPPORTING_BLOCKS).isPresent()) {
                return true;
            }
        }
        return false;
    }

    boolean isRedstoneDenied(Block block) {
        ProtectionCache.CacheFlag flag = plugin.getProtectionCache().getAllowed(block, ProtectionCache.CacheType.REDSTONE);
        if (flag != ProtectionCache.CacheFlag.MISS_CACHE) {
            return flag == ProtectionCache.CacheFlag.NOT_ALLOWED;
        } else {
            Optional<Protection> protection = plugin.getProtectionFinder().findProtection(block, SearchMode.MAIN_BLOCKS_ONLY);
            if (protection.isEmpty()) {
                plugin.getProtectionCache().setAllowed(block, ProtectionCache.CacheType.REDSTONE,true);
                return false;
            }
            Profile redstone = plugin.getProfileFactory().fromRedstone();
            boolean allowed = protection.get().isAllowed(redstone);
            plugin.getProtectionCache().setAllowed(block, ProtectionCache.CacheType.REDSTONE, allowed);
            return !allowed;
        }
    }
}
