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