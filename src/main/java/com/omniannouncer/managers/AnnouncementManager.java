package com.omniannouncer.managers;

import com.omniannouncer.OmniAnnouncer;
import com.omniannouncer.utils.ColorUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AnnouncementManager {

    private final OmniAnnouncer plugin;
    private final List<String> announcementKeys = new ArrayList<>();
    private int currentIndex = 0;

    // Folia zamanlayıcı görevlerini tutmak için Object kullanıyoruz
    private Object autoBroadcastTask;
    private Object scheduleCheckTask;

    public AnnouncementManager(OmniAnnouncer plugin) {
        this.plugin = plugin;
    }

    public void loadAnnouncements() {
        announcementKeys.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("announcements");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (plugin.getConfig().getBoolean("announcements." + key + ".enabled", true)) {
                    announcementKeys.add(key);
                }
            }
        }
        startTasks();
    }

    public void stopTasks() {
        if (autoBroadcastTask != null) {
            // Folia ScheduledTask cancel
            if (autoBroadcastTask instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask) {
                ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) autoBroadcastTask).cancel();
            }
            autoBroadcastTask = null;
        }
        if (scheduleCheckTask != null) {
            if (scheduleCheckTask instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask) {
                ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) scheduleCheckTask).cancel();
            }
            scheduleCheckTask = null;
        }
    }

    private void startTasks() {
        stopTasks();

        if (plugin.getConfig().getBoolean("settings.auto-broadcaster-enabled", true) && !announcementKeys.isEmpty()) {
            long intervalSeconds = plugin.getConfig().getLong("settings.interval-seconds", 300);

            // Folia GlobalRegionScheduler kullanımı
            autoBroadcastTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    plugin,
                    task -> triggerNextBroadcast(),
                    intervalSeconds,
                    intervalSeconds
            );
        }

        // Dakikalık kontrol görevi
        scheduleCheckTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> checkScheduledAnnouncements(),
                1L,
                60L // Her 60 saniyede bir
        );
    }

    private void triggerNextBroadcast() {
        if (announcementKeys.isEmpty()) return;

        String mode = plugin.getConfig().getString("settings.broadcast-mode", "SEQUENCE");
        String key;

        if ("RANDOM".equalsIgnoreCase(mode)) {
            key = announcementKeys.get(new Random().nextInt(announcementKeys.size()));
        } else {
            if (currentIndex >= announcementKeys.size()) {
                currentIndex = 0;
            }
            key = announcementKeys.get(currentIndex);
            currentIndex++;
        }

        executeBroadcast("announcements." + key);
    }

    private void checkScheduledAnnouncements() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("scheduled-announcements");
        if (section == null) return;

        LocalDateTime now = LocalDateTime.now();
        String currentDay = now.getDayOfWeek().name();
        String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        for (String key : section.getKeys(false)) {
            String path = "scheduled-announcements." + key;
            if (!plugin.getConfig().getBoolean(path + ".enabled", true)) continue;

            String configDay = plugin.getConfig().getString(path + ".day", "EVERYDAY");
            String configTime = plugin.getConfig().getString(path + ".time", "20:00");

            boolean dayMatch = configDay.equalsIgnoreCase("EVERYDAY") || configDay.equalsIgnoreCase(currentDay);
            boolean timeMatch = configTime.equals(currentTime);

            if (dayMatch && timeMatch) {
                executeBroadcast(path);
            }
        }
    }

    public boolean executeBroadcastByName(String name) {
        String path = "announcements." + name;
        if (!plugin.getConfig().contains(path)) {
            path = "scheduled-announcements." + name;
            if (!plugin.getConfig().contains(path)) {
                return false;
            }
        }
        executeBroadcast(path);
        return true;
    }

    public void executeBroadcast(String path) {
        String type = plugin.getConfig().getString(path + ".type", "CHAT");
        String permission = plugin.getConfig().getString(path + ".permission", "");
        List<String> allowedWorlds = plugin.getConfig().getStringList(path + ".worlds");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!permission.isEmpty() && !player.hasPermission(permission)) {
                continue;
            }
            if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
                continue;
            }

            // Folia üzerinde oyuncu işlemlerini oyuncunun kendi thread'inde güvenle çalıştırmak:
            player.getScheduler().run(plugin, scheduledTask -> {
                playSound(player, path);

                switch (type.toUpperCase()) {
                    case "CHAT":
                        sendChat(player, path);
                        break;
                    case "TITLE":
                        sendTitle(player, path);
                        break;
                    case "ACTIONBAR":
                        sendActionBar(player, path);
                        break;
                    case "BOSSBAR":
                        sendBossBar(player, path);
                        break;
                    case "ALL":
                        sendChat(player, path);
                        sendTitle(player, path);
                        sendActionBar(player, path);
                        sendBossBar(player, path);
                        break;
                }
            }, null);
        }
    }

    private String parsePlaceholders(Player player, String text) {
        if (text == null) return "";
        if (plugin.isPapiEnabled()) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }
        return ColorUtils.colorize(text);
    }

    private void playSound(Player player, String path) {
        if (plugin.getConfig().getBoolean(path + ".sound.enabled", false)) {
            String soundName = plugin.getConfig().getString(path + ".sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
            float volume = (float) plugin.getConfig().getDouble(path + ".sound.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble(path + ".sound.pitch", 1.0);

            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void sendChat(Player player, String path) {
        List<String> messages = plugin.getConfig().getStringList(path + ".chat.messages");
        String hoverText = parsePlaceholders(player, plugin.getConfig().getString(path + ".chat.hover-text", ""));
        String clickAction = plugin.getConfig().getString(path + ".chat.click-action", "");
        String clickValue = parsePlaceholders(player, plugin.getConfig().getString(path + ".chat.click-value", ""));

        for (String msg : messages) {
            String parsedMsg = parsePlaceholders(player, msg);
            TextComponent component = new TextComponent(parsedMsg);

            if (!hoverText.isEmpty()) {
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));
            }

            if (!clickAction.isEmpty() && !clickValue.isEmpty()) {
                try {
                    ClickEvent.Action action = ClickEvent.Action.valueOf(clickAction.toUpperCase());
                    component.setClickEvent(new ClickEvent(action, clickValue));
                } catch (IllegalArgumentException ignored) {}
            }

            player.spigot().sendMessage(component);
        }
    }

    private void sendTitle(Player player, String path) {
        String title = parsePlaceholders(player, plugin.getConfig().getString(path + ".title.title", ""));
        String subtitle = parsePlaceholders(player, plugin.getConfig().getString(path + ".title.subtitle", ""));
        int fadeIn = plugin.getConfig().getInt(path + ".title.fade-in", 10);
        int stay = plugin.getConfig().getInt(path + ".title.stay", 70);
        int fadeOut = plugin.getConfig().getInt(path + ".title.fade-out", 20);

        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    private void sendActionBar(Player player, String path) {
        String message = parsePlaceholders(player, plugin.getConfig().getString(path + ".actionbar.message", ""));
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    private void sendBossBar(Player player, String path) {
        String title = parsePlaceholders(player, plugin.getConfig().getString(path + ".bossbar.title", ""));
        String colorStr = plugin.getConfig().getString(path + ".bossbar.color", "PURPLE");
        String styleStr = plugin.getConfig().getString(path + ".bossbar.style", "SOLID");
        int duration = plugin.getConfig().getInt(path + ".bossbar.duration-seconds", 10);

        BarColor color;
        BarStyle style;

        try {
            color = BarColor.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BarColor.PURPLE;
        }

        try {
            style = BarStyle.valueOf(styleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            style = BarStyle.SOLID;
        }

        BossBar bossBar = Bukkit.createBossBar(title, color, style);
        bossBar.addPlayer(player);

        // Folia Player Scheduler ile BossBar süresi güncellenmesi
        player.getScheduler().runAtFixedRate(plugin, new java.util.function.Consumer<io.papermc.paper.threadedregions.scheduler.ScheduledTask>() {
            int remaining = duration;

            @Override
            public void accept(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
                if (remaining <= 0 || !player.isOnline()) {
                    bossBar.removeAll();
                    task.cancel();
                    return;
                }
                bossBar.setProgress((double) remaining / duration);
                remaining--;
            }
        }, null, 1L, 20L);
    }
}