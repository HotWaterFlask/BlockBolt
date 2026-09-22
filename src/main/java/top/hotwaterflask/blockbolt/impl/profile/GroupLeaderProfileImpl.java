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
import top.hotwaterflask.blockbolt.profile.PlayerProfile;
import top.hotwaterflask.blockbolt.profile.Profile;

/**
 * Implementation of {@link Profile}. Players are considered part of a group
 * when the {@link GroupSystem#isGroupLeader(Player, String)} method returns
 * true.
 *
 */
class GroupLeaderProfileImpl implements Profile {

    static final String GROUP_LEADER_KEY = "l";
    private String groupName;
    private final GroupSystem groupSystem;

    GroupLeaderProfileImpl(GroupSystem groupSystem, String groupName) {
        this.groupSystem = Preconditions.checkNotNull(groupSystem);
        this.groupName = Preconditions.checkNotNull(groupName);
    }

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

        GroupLeaderProfileImpl otherProfile = (GroupLeaderProfileImpl) other;
        return groupName.equalsIgnoreCase(otherProfile.groupName);
    }

    @Override
    public String getDisplayName() {
        return "+" + groupName + "+";
    }

    @Override
    public void getSaveObject(SecretSignEntry entry) {
        entry.setString(GROUP_LEADER_KEY, groupName);
    }

    @Override
    public int hashCode() {
        // Bits are inverted to avoid hash code collision with {@link
        // GroupSystem}.
        return ~groupName.toLowerCase(Locale.ROOT).hashCode();
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

        return groupSystem.isGroupLeader(player, groupName);
    }

    @Override
    public boolean isExpired(Date cutoffDate) {
        // Group leader profiles never expire
        return false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[name=" + groupName + "]";
    }

}
