package top.hotwaterflask.blockbolt.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import top.hotwaterflask.blockbolt.ProfileFactory;
import top.hotwaterflask.blockbolt.ProtectionSign;
import top.hotwaterflask.blockbolt.ProtectionUpdater;
import top.hotwaterflask.blockbolt.SignParser;
import top.hotwaterflask.blockbolt.profile.PlayerProfile;
import top.hotwaterflask.blockbolt.profile.Profile;
import top.hotwaterflask.blockbolt.protection.Protection;

public class ProtectionUpdaterImpl implements ProtectionUpdater {

    private final Server server;
    private final SignParser signParser;
    private final ProfileFactory profileFactory;

    public ProtectionUpdaterImpl(Server server, SignParser signParser, ProfileFactory profileFactory) {
        this.server = Objects.requireNonNull(server, "server");
        this.signParser = Objects.requireNonNull(signParser, "signParser");
        this.profileFactory = Objects.requireNonNull(profileFactory, "profileFactory");
    }

    @Nullable
    private PlayerProfile getUpdatedProfile(PlayerProfile profile) {
        if (profile.getUniqueId().isPresent()) {
           Player player = server.getPlayer(profile.getUniqueId().get());
           if (player != null && !player.getName().equals(profile.getDisplayName())) {
               // Found a changed name
               return profileFactory.fromPlayer(player);
           }
           return null;
        } else {
            // Found a missing unique id
            String name = profile.getDisplayName();
            if (name.isEmpty()) {
                return null; // Empty line, ignore
            }
            Player player = server.getPlayerExact(name);
            if (player == null) {
                return null; // No player online with that name, lookup failed
            }
            return profileFactory.fromPlayer(player);
        }
    }

    @Override
    public void update(Protection protection) {
        for (ProtectionSign protectionSign : protection.getSigns()) {
            updateProtectionSign(protectionSign);
        }
    }

    @Override
    public void update(Protection protection, boolean newProtection) {
        update(protection);
    }

    @Nullable
    private List<Profile> updateProfiles(ProtectionSign protectionSign) {
        List<Profile> updatedProfiles = null;

        int i = -1; // Will be 0 at first iteration
        for (Profile profile : protectionSign.getProfiles()) {
            i++;

            if (!(profile instanceof PlayerProfile)) {
                continue;
            }
            PlayerProfile updatedProfile = getUpdatedProfile((PlayerProfile) profile);
            if (updatedProfile == null) {
                continue; // Nothing to update
            }

            if (updatedProfiles == null) {
                // Need to initialize list
                updatedProfiles = new ArrayList<>(protectionSign.getProfiles());
            }
            updatedProfiles.set(i, updatedProfile);
        }

        return updatedProfiles;
    }

    private void updateProtectionSign(ProtectionSign protectionSign) {
        List<Profile> updatedProfiles = updateProfiles(protectionSign);

        if (updatedProfiles != null) {
            protectionSign = protectionSign.withProfiles(updatedProfiles);
        }

        if (updatedProfiles != null || protectionSign.requiresResave()) {
            signParser.saveSign(protectionSign);
        }
    }
}
