package top.hotwaterflask.blockbolt.impl.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.google.common.base.Preconditions;

import top.hotwaterflask.blockbolt.BlockBoltPlugin;
import top.hotwaterflask.blockbolt.Permissions;
import top.hotwaterflask.blockbolt.ProtectionSign;
import top.hotwaterflask.blockbolt.SignType;
import top.hotwaterflask.blockbolt.Translator.Translation;
import top.hotwaterflask.blockbolt.profile.Profile;
import top.hotwaterflask.blockbolt.protection.Protection;

/**
 * Handles /deadbolt, /lockette, /blockbolt, /blocklocker commands.
 */
public final class BlockBoltCommand implements TabExecutor {

    private final BlockBoltPlugin plugin;

    public BlockBoltCommand(BlockBoltPlugin plugin) {
        this.plugin = Preconditions.checkNotNull(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")) {
            return reloadCommand(sender);
        }

        if (sub.equals("help")) {
            sendHelp(sender, label);
            return true;
        }

        // Check if it's a line modification command: /deadbolt <2|3|4> <text...>
        if (sub.equals("2") || sub.equals("3") || sub.equals("4") || sub.equals("1")) {
            if (!(sender instanceof Player)) {
                plugin.getTranslator().sendMessage(sender, Translation.COMMAND_CANNOT_BE_USED_BY_CONSOLE);
                return true;
            }
            int lineNumber = Integer.parseInt(sub);
            String newText = (args.length > 1) ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "";
            return handleEditLine((Player) sender, lineNumber, newText);
        }

        sendHelp(sender, label);
        return true;
    }

    private boolean handleEditLine(Player player, int lineNumber, String newText) {
        if (lineNumber == 1 && !Permissions.has(player, Permissions.CAN_ADMIN)) {
            plugin.getTranslator().sendMessage(player, Translation.COMMAND_LINE_NUMBER_OUT_OF_BOUNDS);
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(6, FluidCollisionMode.NEVER);
        if (targetBlock == null) {
            plugin.getTranslator().sendMessage(player, Translation.PROTECTION_NOT_NEARBY);
            return true;
        }

        Block signBlock = null;
        Optional<Protection> protectionOpt = Optional.empty();

        if (targetBlock.getState() instanceof Sign) {
            signBlock = targetBlock;
            protectionOpt = plugin.getProtectionFinder().findProtection(signBlock);
        } else {
            protectionOpt = plugin.getProtectionFinder().findProtection(targetBlock);
            if (protectionOpt.isPresent()) {
                Collection<ProtectionSign> signs = protectionOpt.get().getSigns();
                if (!signs.isEmpty()) {
                    // Pick the sign
                    signBlock = signs.iterator().next().getLocation().getBlock();
                }
            }
        }

        if (!protectionOpt.isPresent() || signBlock == null || !(signBlock.getState() instanceof Sign)) {
            plugin.getTranslator().sendMessage(player, Translation.COMMAND_NO_SIGN_SELECTED);
            return true;
        }

        Protection protection = protectionOpt.get();
        Profile profile = plugin.getProfileFactory().fromPlayer(player);

        if (!protection.isOwner(profile) && !Permissions.has(player, Permissions.CAN_ADMIN)) {
            plugin.getTranslator().sendMessage(player, Translation.PROTECTION_NO_ACCESS, protection.getOwnerDisplayName());
            return true;
        }

        Sign signState = (Sign) signBlock.getState();
        SignSide front = signState.getSide(Side.FRONT);
        SignSide back = signState.getSide(Side.BACK);
        Side side = (front.getLine(0).isEmpty() && !back.getLine(0).isEmpty()) ? Side.BACK : Side.FRONT;
        SignSide targetSide = (side == Side.BACK) ? back : front;

        // Line 2 of a main sign represents the owner and cannot be edited by non-admins
        Optional<SignType> signType = plugin.getSignParser().getSignType(signState, side);
        if (signType.filter(SignType::isMainSign).isPresent() && lineNumber == 2 && !Permissions.has(player, Permissions.CAN_ADMIN)) {
            plugin.getTranslator().sendMessage(player, Translation.COMMAND_CANNOT_EDIT_OWNER);
            return true;
        }

        targetSide.setLine(lineNumber - 1, newText);
        signState.update();

        plugin.getProtectionUpdater().update(protection);

        plugin.getTranslator().sendMessage(player, Translation.COMMAND_UPDATED_SIGN);
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "========== [ BlockBolt ] ==========");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " <2|3|4> <text>" + ChatColor.GRAY + " - " + plugin.getTranslator().getWithoutColor(Translation.COMMAND_UPDATED_SIGN));
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - " + plugin.getTranslator().getWithoutColor(Translation.COMMAND_PLUGIN_RELOADED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            if (Permissions.has(sender, Permissions.CAN_ADMIN)) {
                list.add("1");
            }
            list.addAll(Arrays.asList("2", "3", "4", "reload", "help"));
            list.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)));
            return list;
        }
        if (args.length == 2 && (args[0].equals("1") || args[0].equals("2") || args[0].equals("3") || args[0].equals("4"))) {
            List<String> suggestions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                suggestions.add(p.getName());
            }
            suggestions.add("[Everyone]");
            suggestions.add("[More Users]");
            suggestions.add("[Timer:3]");
            suggestions.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)));
            return suggestions;
        }
        return Collections.emptyList();
    }

    private boolean reloadCommand(CommandSender sender) {
        if (!Permissions.has(sender, Permissions.CAN_RELOAD)) {
            plugin.getTranslator().sendMessage(sender, Translation.COMMAND_NO_PERMISSION);
            return true;
        }

        plugin.reload();
        plugin.getLogger().info(plugin.getTranslator().getWithoutColor(Translation.COMMAND_PLUGIN_RELOADED));
        if (!(sender instanceof ConsoleCommandSender)) {
            plugin.getTranslator().sendMessage(sender, Translation.COMMAND_PLUGIN_RELOADED);
        }
        return true;
    }

}
