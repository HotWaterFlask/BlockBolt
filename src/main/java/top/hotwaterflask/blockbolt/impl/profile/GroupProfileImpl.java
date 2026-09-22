package top.hotwaterflask.blockbolt.impl.profile;

import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.google.common.base.Preconditions;

import top.hotwaterflask.blockbolt.SecretSignEntry;
import top.hotwaterflask.blockbolt.group.GroupSystem;
import top.hotwaterflask.blockbolt.profile.GroupProfile;
import top.hotwaterflask.blockbolt.profile.PlayerProfile;
import top.hotwaterflask.blockbolt.profile.Profile;

/**
 * Implementation of {@link GroupProfile}. Players are considered part of a
 * group when the name of their scoreboard team matches this group, or when they
 * have the permission node for this group.
 *
 */
class GroupProfileImpl implements GroupProfile {

    static final String GROUP_KEY = "g";
    private String groupName;
    private final GroupSystem groupSystem;

    GroupProfileImpl(GroupSystem groupSystem, String groupName) {
        this.groupSystem = Preconditions.checkNotNull(groupSystem);
        this.groupName = Preconditions.checkNotNull(groupName);
    }

    /**
     * We compare uuids or names. Objects are equal if the uuids are present in
     * both objects and are equal, or if the uuids are present in neither
     * objects and are not equal.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (getClass() != other.getClass()) {
            return false;
        }

        GroupProfileImpl otherProfile = (GroupProfileImpl) other;
        return groupName.equalsIgnoreCase(otherProfile.groupName);
    }

    @Override
    public String getDisplayName() {
        return "[" + groupName + "]";
    }

    @Override
    public void getSaveObject(SecretSignEntry entry) {
        entry.setString(GROUP_KEY, groupName);
    }

    @Override
    public int hashCode() {
        return groupName.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public boolean includes(Profile other) {
        if (!(other instanceof PlayerProfile)) {
            return false;
        }

        PlayerProfile playerProfile = (PlayerProfile) other;
        Player player;
        if (playerProfile.getUniqueId().isPresent()) {
            player = Bukkit.getPlayer(playerProfile.getUniqueId().get());
        } else {
            player = Bukkit.getPlayerExact(playerProfile.getDisplayName());
        }
        if (player == null) {
            return false;
        }

        return groupSystem.isInGroup(player, groupName);
    }

    @Override
    public boolean isExpired(Date cutoffDate) {
        // Group profiles never expire
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[name=" + groupName + "]";
    }

}
