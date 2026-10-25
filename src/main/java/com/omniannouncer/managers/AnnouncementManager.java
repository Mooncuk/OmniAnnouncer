<<<<<<< HEAD
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
=======
package com.omniannouncer.managers;

import com.omniannouncer.OmniAnnouncer;
import com.omniannouncer.utils.ColorUtils;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnnouncementManager implements Listener {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MINUTE_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OmniAnnouncer plugin;
    private final List<String> announcementKeys = new ArrayList<>();
    private int currentIndex = 0;

    // Folia / Paper zamanlayıcı görevleri
    private ScheduledTask autoBroadcastTask;
    private ScheduledTask scheduleCheckTask;

    // Oyuncu başına aktif BossBar ve onu güncelleyen görev
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> bossBarTasks = new ConcurrentHashMap<>();

    // Zamanlanmış duyuruların en son hangi dakikada çalıştırıldığını tutar (yyyy-MM-dd HH:mm)
    private final Map<String, String> lastRunMinute = new ConcurrentHashMap<>();

    // Saat dilimi önbelleği
    private String cachedZoneName;
    private ZoneId cachedZone;

    // Hatalı ses uyarısı önbelleği
    private final Set<String> warnedSounds = ConcurrentHashMap.newKeySet();

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
            autoBroadcastTask.cancel();
            autoBroadcastTask = null;
        }
        if (scheduleCheckTask != null) {
            scheduleCheckTask.cancel();
            scheduleCheckTask = null;
        }
        cleanup();
    }

    /**
     * Aktif tüm BossBar'ları oyuncuların ekranından kaldırır ve güncelleme görevlerini iptal eder.
     */
    public void cleanup() {
        for (ScheduledTask task : bossBarTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        bossBarTasks.clear();

        for (BossBar bossBar : activeBossBars.values()) {
            bossBar.removeAll();
        }
        activeBossBars.clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeBossBar(event.getPlayer().getUniqueId());
    }

    private void startTasks() {
        stopTasks();

        if (plugin.getConfig().getBoolean("settings.auto-broadcaster-enabled", true) && !announcementKeys.isEmpty()) {
            long intervalSeconds = Math.max(1L, plugin.getConfig().getLong("settings.interval-seconds", 300L));
            long intervalTicks = intervalSeconds * 20L;

            autoBroadcastTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    plugin,
                    task -> triggerNextBroadcast(),
                    intervalTicks,
                    intervalTicks
            );
        }

        // Dakikalık zamanlanmış duyuru kontrol görevi
        scheduleCheckTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> checkScheduledAnnouncements(),
                20L,
                20L * 60L
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
        if (section == null) {
            pruneOfflineBossBars();
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(resolveZone());
        String currentDay = now.getDayOfWeek().name();
        String currentTime = now.format(TIME_FORMAT);
        String currentMinuteKey = now.format(MINUTE_KEY_FORMAT);

        for (String key : section.getKeys(false)) {
            String path = "scheduled-announcements." + key;
            if (!plugin.getConfig().getBoolean(path + ".enabled", true)) continue;

            String configDay = plugin.getConfig().getString(path + ".day", "EVERYDAY").trim();
            String configTime = plugin.getConfig().getString(path + ".time", "20:00").trim();

            boolean dayMatch = configDay.equalsIgnoreCase("EVERYDAY") || configDay.equalsIgnoreCase(currentDay);
            boolean timeMatch = configTime.equals(currentTime);

            if (!dayMatch || !timeMatch) continue;

            // Aynı dakika içinde ikinci kez çalıştırmayı engelle
            String previousRun = lastRunMinute.get(path);
            if (currentMinuteKey.equals(previousRun)) continue;

            lastRunMinute.put(path, currentMinuteKey);
            executeBroadcast(path);
        }

        pruneOfflineBossBars();
    }

    private ZoneId resolveZone() {
        String zoneName = plugin.getConfig().getString("settings.timezone", "Europe/Istanbul");
        if (zoneName == null || zoneName.isBlank()) {
            zoneName = "Europe/Istanbul";
        } else {
            zoneName = zoneName.trim();
        }

        if (zoneName.equals(cachedZoneName) && cachedZone != null) {
            return cachedZone;
        }

        ZoneId resolved;
        try {
            resolved = ZoneId.of(zoneName);
        } catch (Exception e) {
            plugin.getLogger().warning("Gecersiz saat dilimi: '" + zoneName + "'. Sunucu varsayilan saat dilimi kullanilacak.");
            resolved = ZoneId.systemDefault();
        }

        cachedZoneName = zoneName;
        cachedZone = resolved;
        return resolved;
    }

    public String findAnnouncementPath(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String trimmed = name.trim();

        String path = "announcements." + trimmed;
        if (plugin.getConfig().contains(path)) {
            return path;
        }
        path = "scheduled-announcements." + trimmed;
        if (plugin.getConfig().contains(path)) {
            return path;
        }

        String matchedKey = findKeyIgnoreCase("announcements", trimmed);
        if (matchedKey != null) {
            return "announcements." + matchedKey;
        }

        matchedKey = findKeyIgnoreCase("scheduled-announcements", trimmed);
        if (matchedKey != null) {
            return "scheduled-announcements." + matchedKey;
        }

        return null;
    }

    private String findKeyIgnoreCase(String sectionPath, String name) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionPath);
        if (section == null) {
            return null;
        }
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase(name)) {
                return key;
            }
        }
        return null;
    }

    public boolean isAnnouncementEnabled(String name) {
        String path = findAnnouncementPath(name);
        return path != null && plugin.getConfig().getBoolean(path + ".enabled", true);
    }

    public List<String> getEnabledAnnouncementNames() {
        Set<String> names = new LinkedHashSet<>();
        collectEnabledNames("announcements", names);
        collectEnabledNames("scheduled-announcements", names);
        return new ArrayList<>(names);
    }

    private void collectEnabledNames(String sectionPath, Set<String> target) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionPath);
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            if (plugin.getConfig().getBoolean(sectionPath + "." + key + ".enabled", true)) {
                target.add(key);
            }
        }
    }

    public boolean executeBroadcastByName(String name) {
        return executeBroadcastByName(name, null);
    }

    public boolean executeBroadcastByName(String name, Player target) {
        String path = findAnnouncementPath(name);
        if (path == null) {
            return false;
        }
        executeBroadcast(path, target);
        return true;
    }

    public void executeBroadcast(String path) {
        executeBroadcast(path, null);
    }

    /**
     * Akıllı kanal tespiti ve geriye dönük uyumlu duyuru yürütme motoru.
     */
    public void executeBroadcast(String path, Player target) {
        String rawType = plugin.getConfig().getString(path + ".type");
        String type = rawType != null ? rawType.trim().toUpperCase(Locale.ROOT) : "";

        String permission = plugin.getConfig().getString(path + ".permission", "").trim();
        List<String> allowedWorlds = plugin.getConfig().getStringList(path + ".worlds");

        List<Player> recipients = new ArrayList<>();
        if (target != null) {
            if (target.isOnline()) {
                recipients.add(target);
            }
        } else {
            recipients.addAll(Bukkit.getOnlinePlayers());
        }

        for (Player player : recipients) {
            if (target == null) {
                if (!permission.isEmpty() && !player.hasPermission(permission)) {
                    continue;
                }
                if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
                    continue;
                }
            }

            player.getScheduler().run(plugin, scheduledTask -> {
                if (!player.isOnline()) {
                    return;
                }

                // Opsiyonel Ses Kontrolü
                playSound(player, path);

                // Geriye Dönük Uyumluluk ve Akıllı Kanal Tespiti
                boolean legacySpecific = false;
                if (!type.isEmpty() && !type.equals("ALL")) {
                    switch (type) {
                        case "CHAT":
                            if (hasChat(path)) sendChat(player, path);
                            legacySpecific = true;
                            break;
                        case "TITLE":
                            if (hasTitle(path)) sendTitle(player, path);
                            legacySpecific = true;
                            break;
                        case "ACTIONBAR":
                            if (hasActionBar(path)) sendActionBar(player, path);
                            legacySpecific = true;
                            break;
                        case "BOSSBAR":
                            if (hasBossBar(path)) sendBossBar(player, path);
                            legacySpecific = true;
                            break;
                        default:
                            break;
                    }
                }

                // Akıllı Mod: Tanımlı olan tüm kanalları otomatik gönder
                if (!legacySpecific) {
                    if (hasChat(path)) {
                        sendChat(player, path);
                    }
                    if (hasTitle(path)) {
                        sendTitle(player, path);
                    }
                    if (hasActionBar(path)) {
                        sendActionBar(player, path);
                    }
                    if (hasBossBar(path)) {
                        sendBossBar(player, path);
                    }
                }
            }, null);
        }
    }

    private boolean hasText(String path) {
        String text = plugin.getConfig().getString(path);
        return text != null && !text.isEmpty();
    }

    public boolean hasChat(String path) {
        if (!plugin.getConfig().contains(path + ".chat")) {
            return false;
        }
        List<String> msgs = plugin.getConfig().getStringList(path + ".chat.messages");
        if (msgs != null && !msgs.isEmpty()) {
            return true;
        }
        return hasText(path + ".chat.message");
    }

    public boolean hasTitle(String path) {
        if (!plugin.getConfig().contains(path + ".title")) {
            return false;
        }
        return hasText(path + ".title.title") || hasText(path + ".title.subtitle");
    }

    public boolean hasActionBar(String path) {
        if (!plugin.getConfig().contains(path + ".actionbar")) {
            return false;
        }
        return hasText(path + ".actionbar.message");
    }

    public boolean hasBossBar(String path) {
        if (!plugin.getConfig().contains(path + ".bossbar")) {
            return false;
        }
        return hasText(path + ".bossbar.title");
    }

    private String parsePlaceholders(Player player, String text) {
        if (text == null) return "";

        // 1. Senaryo A: PlaceholderAPI entegrasyonu (try-catch korumalı)
        if (plugin.isPapiEnabled()) {
            try {
                text = PlaceholderAPI.setPlaceholders(player, text);
            } catch (Throwable t) {
                plugin.getLogger().warning("PlaceholderAPI ayristirma hatasi: " + t.getMessage());
            }
        }

        // 2. Senaryo B: Dahili Yedek (Fallback) - Hem %player% hem {player} degiskenlerini coz
        if (player != null) {
            String name = player.getName();
            text = text.replace("%player%", name)
                       .replace("{player}", name);
        } else {
            text = text.replace("%player%", "")
                       .replace("{player}", "");
        }

        return text;
    }

    /**
     * Opsiyonel ses çalma. 'sound' bloğu yoksa veya enabled: false ise çalınmaz.
     */
    private void playSound(Player player, String path) {
        if (!plugin.getConfig().contains(path + ".sound")) {
            return;
        }

        // Blok varsa ve açıkça false yapılmadıysa varsayılan true kabul edilir
        if (!plugin.getConfig().getBoolean(path + ".sound.enabled", true)) {
            return;
        }

        String soundName = plugin.getConfig().getString(path + ".sound.name");
        if (soundName == null || soundName.isBlank()) {
            return;
        }

        float volume = (float) plugin.getConfig().getDouble(path + ".sound.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble(path + ".sound.pitch", 1.0);

        String trimmed = soundName.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        Sound sound = null;

        // 1. Standart Bukkit Sound Enum denemesi (Örn: BLOCK_NOTE_BLOCK_PLING)
        try {
            sound = Sound.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NoSuchFieldError ignored) {
        }

        // 2. Noktalı Minecraft formatı denemesi (Örn: block_note_block_pling -> block.note_block.pling)
        if (sound == null) {
            try {
                String dotted = normalized.contains(":") ? normalized : "minecraft:" + normalized.replace('_', '.');
                NamespacedKey key = NamespacedKey.fromString(dotted);
                if (key != null) {
                    sound = Registry.SOUNDS.get(key);
                }
            } catch (Exception ignored) {
            }
        }

        // 3. Doğrudan NamespacedKey denemesi
        if (sound == null) {
            try {
                NamespacedKey key = normalized.contains(":") ? NamespacedKey.fromString(normalized) : NamespacedKey.minecraft(normalized);
                if (key != null) {
                    sound = Registry.SOUNDS.get(key);
                }
            } catch (Exception ignored) {
            }
        }

        if (sound != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
            return;
        }

        // 4. Adventure Sound Fallback (Özel kaynak paketi sesleri)
        try {
            String keyStr = normalized.contains(":") ? normalized : "minecraft:" + normalized;
            Key advKey = Key.key(keyStr);
            net.kyori.adventure.sound.Sound advSound = net.kyori.adventure.sound.Sound.sound(
                    advKey,
                    net.kyori.adventure.sound.Sound.Source.MASTER,
                    volume,
                    pitch
            );
            player.playSound(advSound);
        } catch (Exception e) {
            if (warnedSounds.add(normalized)) {
                plugin.getLogger().warning("Gecersiz ses ismi: '" + soundName + "' (" + path + "). Ses calinamadi.");
            }
        }
    }

    private void sendChat(Player player, String path) {
        List<String> messages = plugin.getConfig().getStringList(path + ".chat.messages");
        if (messages.isEmpty()) {
            String single = plugin.getConfig().getString(path + ".chat.message");
            if (single != null && !single.isEmpty()) {
                messages = Collections.singletonList(single);
            }
        }

        String hoverText = parsePlaceholders(player, plugin.getConfig().getString(path + ".chat.hover-text", ""));
        String clickAction = plugin.getConfig().getString(path + ".chat.click-action", "").trim();
        String clickValue = parsePlaceholders(player, plugin.getConfig().getString(path + ".chat.click-value", ""));

        Component hoverComponent = !hoverText.isEmpty() ? ColorUtils.toComponent(hoverText) : null;

        ClickEvent clickEvent = null;
        if (!clickAction.isEmpty() && !clickValue.isEmpty()) {
            switch (clickAction.toUpperCase(Locale.ROOT)) {
                case "OPEN_URL":
                    clickEvent = ClickEvent.openUrl(clickValue);
                    break;
                case "RUN_COMMAND":
                    clickEvent = ClickEvent.runCommand(clickValue);
                    break;
                case "SUGGEST_COMMAND":
                    clickEvent = ClickEvent.suggestCommand(clickValue);
                    break;
                case "COPY_TO_CLIPBOARD":
                    clickEvent = ClickEvent.copyToClipboard(clickValue);
                    break;
                default:
                    break;
            }
        }

        for (String msg : messages) {
            String parsedMsg = parsePlaceholders(player, msg);
            Component line = ColorUtils.toComponent(parsedMsg);

            if (hoverComponent != null) {
                line = line.hoverEvent(HoverEvent.showText(hoverComponent));
            }
            if (clickEvent != null) {
                line = line.clickEvent(clickEvent);
            }

            player.sendMessage(line);
        }
    }

    /**
     * Akıllı varsayılanlar: fade-in: 10, stay: 60, fade-out: 10 ticks.
     */
    private void sendTitle(Player player, String path) {
        String titleRaw = parsePlaceholders(player, plugin.getConfig().getString(path + ".title.title", ""));
        String subtitleRaw = parsePlaceholders(player, plugin.getConfig().getString(path + ".title.subtitle", ""));

        if (titleRaw.isEmpty() && subtitleRaw.isEmpty()) {
            return;
        }

        int fadeInTicks = Math.max(0, plugin.getConfig().getInt(path + ".title.fade-in", 10));
        int stayTicks = Math.max(0, plugin.getConfig().getInt(path + ".title.stay", 60));
        int fadeOutTicks = Math.max(0, plugin.getConfig().getInt(path + ".title.fade-out", 10));

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeInTicks * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(fadeOutTicks * 50L)
        );

        Title advTitle = Title.title(
                ColorUtils.toComponent(titleRaw),
                ColorUtils.toComponent(subtitleRaw),
                times
        );

        player.showTitle(advTitle);
    }

    private void sendActionBar(Player player, String path) {
        String message = parsePlaceholders(player, plugin.getConfig().getString(path + ".actionbar.message", ""));
        if (message.isEmpty()) {
            return;
        }
        player.sendActionBar(ColorUtils.toComponent(message));
    }

    /**
     * Akıllı varsayılanlar: color: PURPLE, style: SOLID, duration: 10 saniye.
     */
    private void sendBossBar(Player player, String path) {
        String rawTitle = parsePlaceholders(player, plugin.getConfig().getString(path + ".bossbar.title", ""));
        if (rawTitle.isEmpty()) {
            return;
        }

        int duration = plugin.getConfig().getInt(path + ".bossbar.duration-seconds", 10);
        if (duration <= 0) {
            duration = 10;
        }

        String colorStr = plugin.getConfig().getString(path + ".bossbar.color", "PURPLE");
        String styleStr = plugin.getConfig().getString(path + ".bossbar.style", "SOLID");

        BarColor color;
        BarStyle style;

        try {
            color = BarColor.valueOf(colorStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            color = BarColor.PURPLE;
        }

        try {
            style = BarStyle.valueOf(styleStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            style = BarStyle.SOLID;
        }

        UUID playerId = player.getUniqueId();
        removeBossBar(playerId);

        String coloredTitle = ColorUtils.colorize(rawTitle);
        BossBar bossBar = Bukkit.createBossBar(coloredTitle, color, style);
        bossBar.setProgress(1.0D);
        bossBar.addPlayer(player);
        activeBossBars.put(playerId, bossBar);

        final int totalDuration = duration;
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, new java.util.function.Consumer<ScheduledTask>() {
            int remaining = totalDuration;

            @Override
            public void accept(ScheduledTask scheduledTask) {
                if (remaining <= 0 || !player.isOnline()) {
                    removeBossBar(playerId);
                    return;
                }
                bossBar.setProgress(Math.max(0.0D, Math.min(1.0D, (double) remaining / totalDuration)));
                remaining--;
            }
        }, null, 1L, 20L);

        if (task != null) {
            bossBarTasks.put(playerId, task);
        } else {
            removeBossBar(playerId);
        }
    }

    public void removeBossBar(UUID playerId) {
        ScheduledTask task = bossBarTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        BossBar bossBar = activeBossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    private void pruneOfflineBossBars() {
        if (activeBossBars.isEmpty()) return;

        for (UUID playerId : new ArrayList<>(activeBossBars.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                ScheduledTask task = bossBarTasks.remove(playerId);
                if (task != null) {
                    task.cancel();
                }
                activeBossBars.remove(playerId);
            }
        }
    }
}
>>>>>>> e5a1b90 (chore: release v1.1.0 - Kyori Adventure, I18n and Folia scheduler support)
