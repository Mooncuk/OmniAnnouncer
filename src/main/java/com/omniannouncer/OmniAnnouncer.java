<<<<<<< HEAD
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
=======
package com.omniannouncer;

import com.omniannouncer.commands.AnnounceCommand;
import com.omniannouncer.managers.AnnouncementManager;
import com.omniannouncer.managers.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class OmniAnnouncer extends JavaPlugin {

    private static OmniAnnouncer instance;
    private LanguageManager languageManager;
    private AnnouncementManager announcementManager;
    private boolean papiEnabled;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // 1. Dil yöneticisini başlat ve dil dosyalarını yükle
        languageManager = new LanguageManager(this);
        languageManager.loadLanguages();

        // 2. PlaceholderAPI entegrasyonu
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiEnabled = true;
            getLogger().info("PlaceholderAPI integration enabled!");
        } else {
            papiEnabled = false;
            getLogger().info("PlaceholderAPI not found, running without placeholder support.");
        }

        // 3. Duyuru yöneticisini başlat
        announcementManager = new AnnouncementManager(this);
        announcementManager.loadAnnouncements();
        getServer().getPluginManager().registerEvents(announcementManager, this);

        // 4. Komut kaydı (Ana komut: announcement, takma adlar: announce, omniannounce, duyuru)
        AnnounceCommand cmdExecutor = new AnnounceCommand(this);
        PluginCommand command = getCommand("announcement");
        if (command != null) {
            command.setExecutor(cmdExecutor);
            command.setTabCompleter(cmdExecutor);
        } else {
            getLogger().warning("Could not register command 'announcement'!");
        }

        getLogger().info("OmniAnnouncer v" + getPluginMeta().getVersion() + " successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (announcementManager != null) {
            announcementManager.stopTasks();
        }
        // Plugin reload (PlugMan vb.) sonrasinda eski instance'a referans kalmasin.
        instance = null;
        getLogger().info("OmniAnnouncer disabled.");
    }

    public static OmniAnnouncer getInstance() {
        return instance;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public AnnouncementManager getAnnouncementManager() {
        return announcementManager;
    }

    public boolean isPapiEnabled() {
        return papiEnabled;
    }
}
>>>>>>> e5a1b90 (chore: release v1.1.0 - Kyori Adventure, I18n and Folia scheduler support)
