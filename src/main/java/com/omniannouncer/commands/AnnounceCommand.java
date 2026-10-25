<<<<<<< HEAD
package com.omniannouncer.commands;

import com.omniannouncer.OmniAnnouncer;
import com.omniannouncer.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class AnnounceCommand implements CommandExecutor, TabCompleter {

    private final OmniAnnouncer plugin;

    public AnnounceCommand(OmniAnnouncer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("omniannouncer.admin")) {
            String noPerm = plugin.getConfig().getString("messages.no-permission", "&cYou do not have permission!");
            sender.sendMessage(ColorUtils.colorize(noPerm));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getAnnouncementManager().loadAnnouncements();
            String reloadSuccess = plugin.getConfig().getString("messages.reload-success", "&aReload completed.");
            sender.sendMessage(ColorUtils.colorize(reloadSuccess));
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("broadcast")) {
            String announcementName = args[1];
            boolean success = plugin.getAnnouncementManager().executeBroadcastByName(announcementName);

            if (!success) {
                String notFound = plugin.getConfig().getString("messages.announcement-not-found", "&cAnnouncement not found!");
                sender.sendMessage(ColorUtils.colorize(notFound));
            }
            return true;
        }

        String usage = plugin.getConfig().getString("messages.usage", "&eUsage: /omniannouncer [reload|broadcast]");
        sender.sendMessage(ColorUtils.colorize(usage));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
            completions.add("broadcast");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("broadcast")) {
            if (plugin.getConfig().getConfigurationSection("announcements") != null) {
                completions.addAll(plugin.getConfig().getConfigurationSection("announcements").getKeys(false));
            }
            if (plugin.getConfig().getConfigurationSection("scheduled-announcements") != null) {
                completions.addAll(plugin.getConfig().getConfigurationSection("scheduled-announcements").getKeys(false));
            }
        }

        return completions;
    }
}
=======
package com.omniannouncer.commands;

import com.omniannouncer.OmniAnnouncer;
import com.omniannouncer.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AnnounceCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "help", "list", "announce", "broadcast", "send", "preview", "reload"
    );

    private final OmniAnnouncer plugin;

    public AnnounceCommand(OmniAnnouncer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // Reload icin omniannouncer.reload yetkisi de yeterlidir.
        if ("reload".equals(sub)) {
            if (!sender.hasPermission("omniannouncer.admin") && !sender.hasPermission("omniannouncer.reload")) {
                plugin.getLanguageManager().sendWithFallback(sender, "no-permission", "&cYou do not have permission to use this command!");
                return true;
            }

            plugin.reloadConfig();
            plugin.getLanguageManager().loadLanguages();
            // Reload sirasinda acik kalan BossBar'lari ve gorevleri temizle
            plugin.getAnnouncementManager().cleanup();
            plugin.getAnnouncementManager().loadAnnouncements();
            plugin.getLanguageManager().sendWithFallback(sender, "reload-success", "&a[OmniAnnouncer] Configuration and languages reloaded successfully!");
            return true;
        }

        if (!sender.hasPermission("omniannouncer.admin")) {
            plugin.getLanguageManager().sendWithFallback(sender, "no-permission", "&cYou do not have permission to use this command!");
            return true;
        }

        switch (sub) {
            case "help":
                sendHelp(sender, label);
                break;
            case "list":
                listAnnouncements(sender);
                break;
            case "announce":
            case "broadcast":
                announce(sender, args, label);
                break;
            case "preview":
                preview(sender, args, label);
                break;
            case "send":
                sendToPlayer(sender, args, label);
                break;
            default:
                sendHelp(sender, label);
                break;
        }
        return true;
    }

    private void announce(CommandSender sender, String[] args, String label) {
        if (args.length < 2) {
            sendUsage(sender, label);
            return;
        }

        String name = args[1];
        if (plugin.getAnnouncementManager().findAnnouncementPath(name) == null) {
            sendNotFound(sender, name);
            return;
        }
        if (!plugin.getAnnouncementManager().isAnnouncementEnabled(name)) {
            sendDisabled(sender, name);
            return;
        }

        plugin.getAnnouncementManager().executeBroadcastByName(name);
        plugin.getLanguageManager().sendWithFallback(sender, "broadcast-success",
                "&a[OmniAnnouncer] Announcement '%name%' has been broadcasted successfully.",
                "%name%", name);
    }

    private void preview(CommandSender sender, String[] args, String label) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().sendWithFallback(sender, "player-only", "&cThis command can only be used by players!");
            return;
        }
        if (args.length < 2) {
            sendUsage(sender, label);
            return;
        }

        String name = args[1];
        if (plugin.getAnnouncementManager().findAnnouncementPath(name) == null) {
            sendNotFound(sender, name);
            return;
        }

        // Onizlemede devre disi duyurular da gonderilebilir (duzenleme kolayligi).
        plugin.getAnnouncementManager().executeBroadcastByName(name, (Player) sender);
        plugin.getLanguageManager().sendWithFallback(sender, "preview-success",
                "&a[OmniAnnouncer] Previewing announcement '%name%'.",
                "%name%", name);
    }

    private void sendToPlayer(CommandSender sender, String[] args, String label) {
        if (args.length < 3) {
            sendUsage(sender, label);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getLanguageManager().sendWithFallback(sender, "player-not-found",
                    "&cPlayer not found: %player%",
                    "%player%", args[1]);
            return;
        }

        String name = args[2];
        if (plugin.getAnnouncementManager().findAnnouncementPath(name) == null) {
            sendNotFound(sender, name);
            return;
        }
        if (!plugin.getAnnouncementManager().isAnnouncementEnabled(name)) {
            sendDisabled(sender, name);
            return;
        }

        plugin.getAnnouncementManager().executeBroadcastByName(name, target);
        plugin.getLanguageManager().sendWithFallback(sender, "send-success",
                "&a[OmniAnnouncer] Announcement '%name%' has been sent to %player%.",
                "%name%", name,
                "%player%", target.getName());
    }

    private void listAnnouncements(CommandSender sender) {
        List<String> entries = new ArrayList<>();
        collectEntries("announcements", entries);
        collectEntries("scheduled-announcements", entries);

        if (entries.isEmpty()) {
            plugin.getLanguageManager().sendWithFallback(sender, "list-empty", "&#FFCC00There are no announcements configured yet.");
            return;
        }

        plugin.getLanguageManager().sendWithFallback(sender, "list-header", "&#FFCC00OmniAnnouncer Announcements:");
        for (String entry : entries) {
            sender.sendMessage(ColorUtils.toComponent(entry));
        }
    }

    private void collectEntries(String sectionPath, List<String> entries) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionPath);
        if (section == null) {
            return;
        }

        String entryFormat = plugin.getLanguageManager().getRawMessage("list-entry", "&#FFFFFF- %name% &7[%status%]");
        String activeStatus = plugin.getLanguageManager().getRawMessage("list-active", "&aActive");
        String disabledStatus = plugin.getLanguageManager().getRawMessage("list-disabled", "&cDisabled");

        for (String key : section.getKeys(false)) {
            boolean enabled = plugin.getConfig().getBoolean(sectionPath + "." + key + ".enabled", true);
            String status = enabled ? activeStatus : disabledStatus;

            entries.add(entryFormat
                    .replace("%name%", key)
                    .replace("%status%", status));
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        List<String> lines = plugin.getLanguageManager().getMessageList("help");
        if (lines.isEmpty()) {
            lines = Arrays.asList(
                    "&#FFCC00OmniAnnouncer Commands:",
                    "&#FFFFFF/%label% help &7- Displays this help menu.",
                    "&#FFFFFF/%label% list &7- Lists all announcements and their status.",
                    "&#FFFFFF/%label% announce <name> &7- Broadcasts an announcement to the server.",
                    "&#FFFFFF/%label% send <player> <name> &7- Sends an announcement only to the specified player.",
                    "&#FFFFFF/%label% preview <name> &7- Previews an announcement on your screen only.",
                    "&#FFFFFF/%label% reload &7- Reloads configuration and language files."
            );
        }

        for (String line : lines) {
            sender.sendMessage(ColorUtils.toComponent(line.replace("%label%", label)));
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        plugin.getLanguageManager().sendWithFallback(sender, "usage",
                "&#FFCC00Usage: &#FFFFFF/%label% <reload|announce|send|preview|list|help>",
                "%label%", label);
    }

    private void sendNotFound(CommandSender sender, String name) {
        plugin.getLanguageManager().sendWithFallback(sender, "announcement-not-found",
                "&#FF3333No announcement found with the name '%name%'!",
                "%name%", name);
    }

    private void sendDisabled(CommandSender sender, String name) {
        plugin.getLanguageManager().sendWithFallback(sender, "announcement-disabled",
                "&#FF3333The announcement '%name%' is currently disabled!",
                "%name%", name);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("omniannouncer.admin")) {
            if (args.length == 1 && sender.hasPermission("omniannouncer.reload")) {
                addIfMatches(completions, "reload", args[0]);
            }
            return completions;
        }

        if (args.length == 1) {
            for (String sub : SUBCOMMANDS) {
                addIfMatches(completions, sub, args[0]);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            if ("send".equals(sub)) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    addIfMatches(completions, player.getName(), args[1]);
                }
            } else if ("announce".equals(sub) || "broadcast".equals(sub) || "preview".equals(sub)) {
                for (String name : plugin.getAnnouncementManager().getEnabledAnnouncementNames()) {
                    addIfMatches(completions, name, args[1]);
                }
            }
        } else if (args.length == 3 && "send".equalsIgnoreCase(args[0])) {
            for (String name : plugin.getAnnouncementManager().getEnabledAnnouncementNames()) {
                addIfMatches(completions, name, args[2]);
            }
        }

        return completions;
    }

    private void addIfMatches(List<String> completions, String value, String prefix) {
        if (value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
            completions.add(value);
        }
    }
}
>>>>>>> e5a1b90 (chore: release v1.1.0 - Kyori Adventure, I18n and Folia scheduler support)
