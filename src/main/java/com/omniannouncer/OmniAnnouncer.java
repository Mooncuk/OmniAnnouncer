package com.omniannouncer;

import com.omniannouncer.commands.AnnounceCommand;
import com.omniannouncer.managers.AnnouncementManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class OmniAnnouncer extends JavaPlugin {

    private static OmniAnnouncer instance;
    private AnnouncementManager announcementManager;
    private boolean papiEnabled;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiEnabled = true;
            getLogger().info("PlaceholderAPI integration enabled!");
        } else {
            papiEnabled = false;
            getLogger().info("PlaceholderAPI not found, running without placeables support.");
        }

        announcementManager = new AnnouncementManager(this);
        announcementManager.loadAnnouncements();

        AnnounceCommand announceCommand = new AnnounceCommand(this);
        if (getCommand("omniannouncer") != null) {
            getCommand("omniannouncer").setExecutor(announceCommand);
            getCommand("omniannouncer").setTabCompleter(announceCommand);
        }

        getLogger().info("OmniAnnouncer v" + getDescription().getVersion() + " successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (announcementManager != null) {
            announcementManager.stopTasks();
        }
        getLogger().info("OmniAnnouncer disabled.");
    }

    public static OmniAnnouncer getInstance() {
        return instance;
    }

    public AnnouncementManager getAnnouncementManager() {
        return announcementManager;
    }

    public boolean isPapiEnabled() {
        return papiEnabled;
    }
}