package top.hotwaterflask.blockbolt.impl.group;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import top.hotwaterflask.blockbolt.group.GroupSystem;

/**
 * Group system hooking into the Towny plugin.
 *
 */
public final class TownyGroupSystem extends GroupSystem {

    /**
     * Tests if the Towny plugin is installed.
     *
     * @return True if the Towny plugin is installed, false otherwise.
     */
    public static boolean isAvailable() {
        try {
            JavaPlugin.getProvidingPlugin(Towny.class);
            return true;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    @Override
    public boolean isGroupLeader(Player player, String groupName) {
        try {
            Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
            if (resident == null || !resident.hasTown()) {
                return false;
            }
            Town town = resident.getTown();
            if (town != null && town.getName().equalsIgnoreCase(groupName)) {
                if (town.isMayor(resident) || resident.hasTownRank("assistant")) {
                    return true;
                }
            }

            if (town != null && town.hasNation()) {
                Nation nation = town.getNation();
                if (nation != null && nation.getName().equalsIgnoreCase(groupName)) {
                    if (nation.isKing(resident) || nation.hasAssistant(resident)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isInGroup(Player player, String groupName) {
        try {
            Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
            if (resident == null || !resident.hasTown()) {
                return false;
            }
            Town town = resident.getTown();
            if (town != null && town.getName().equalsIgnoreCase(groupName)) {
                return true;
            }

            if (town != null && town.hasNation()) {
                Nation nation = town.getNation();
                if (nation != null && nation.getName().equalsIgnoreCase(groupName)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean keepOnReload() {
        // BlockLocker will re-add the group system
        return false;
    }

}
