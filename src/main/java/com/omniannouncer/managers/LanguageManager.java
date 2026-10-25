package com.omniannouncer.managers;

import com.omniannouncer.OmniAnnouncer;
import com.omniannouncer.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LanguageManager {

    private final OmniAnnouncer plugin;
    private FileConfiguration activeLangConfig;
    private FileConfiguration fallbackLangConfig;
    private String currentLanguage = "en";

    public LanguageManager(OmniAnnouncer plugin) {
        this.plugin = plugin;
    }

    /**
     * Dil dosyalarını diskte yoksa oluşturur ve seçili dili yükler.
     */
    public void loadLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "languages");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // Varsayılan dil dosyalarını diskte yoksa kopyala
        saveDefaultLanguageFile("languages/en.yml");
        saveDefaultLanguageFile("languages/tr.yml");

        // Fallback dil olarak languages/en.yml dosyasını yükle
        File fallbackFile = new File(langFolder, "en.yml");
        if (fallbackFile.exists()) {
            fallbackLangConfig = YamlConfiguration.loadConfiguration(fallbackFile);
        } else {
            fallbackLangConfig = new YamlConfiguration();
        }

        // Jar içindeki en.yml'i de defaults olarak fallback'e ata (tam hata toleransı)
        try (InputStream stream = plugin.getResource("languages/en.yml")) {
            if (stream != null) {
                YamlConfiguration jarEn = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
                fallbackLangConfig.setDefaults(jarEn);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Jar icindeki varsayilan languages/en.yml yuklenemedi: " + e.getMessage());
        }

        // Aktif dili belirle
        this.currentLanguage = plugin.getConfig().getString("settings.language", "en").trim().toLowerCase();
        File activeFile = new File(langFolder, currentLanguage + ".yml");

        if (!activeFile.exists()) {
            // Eğer config'deki dil dosyası diskte yoksa, jar içinde var mı diye bak ve kaydetmeyi dene
            String resourcePath = "languages/" + currentLanguage + ".yml";
            if (plugin.getResource(resourcePath) != null) {
                plugin.saveResource(resourcePath, false);
            }
        }

        if (activeFile.exists()) {
            activeLangConfig = YamlConfiguration.loadConfiguration(activeFile);
            plugin.getLogger().info("Dil yuklendi: " + currentLanguage + " (" + activeFile.getName() + ")");
        } else {
            plugin.getLogger().warning("Dil dosyasi bulunamadi: " + activeFile.getPath() + ". Varsayilan dil (en) kullanilacak.");
            activeLangConfig = fallbackLangConfig;
            this.currentLanguage = "en";
        }
    }

    private void saveDefaultLanguageFile(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            try {
                plugin.saveResource(resourcePath, false);
            } catch (Exception e) {
                plugin.getLogger().warning(resourcePath + " kaydedilirken hata olustu: " + e.getMessage());
            }
        }
    }

    /**
     * Belirtilen anahtar için mesajı döndürür. Aktif dilde yoksa en.yml fallback'e bakar.
     */
    public String getRawMessage(String key, String defaultFallback) {
        String fullKey = key.startsWith("messages.") ? key : "messages." + key;

        if (activeLangConfig != null) {
            String val = activeLangConfig.getString(fullKey);
            if (val != null && !val.isEmpty()) {
                return val;
            }
        }

        if (fallbackLangConfig != null) {
            String val = fallbackLangConfig.getString(fullKey);
            if (val != null && !val.isEmpty()) {
                return val;
            }
        }

        return defaultFallback != null ? defaultFallback : key;
    }

    public String getRawMessage(String key) {
        return getRawMessage(key, key);
    }

    /**
     * Mesaj listesi döndürür (örneğin help menüsü).
     */
    public List<String> getMessageList(String key) {
        String fullKey = key.startsWith("messages.") ? key : "messages." + key;

        if (activeLangConfig != null) {
            List<String> list = activeLangConfig.getStringList(fullKey);
            if (list != null && !list.isEmpty()) {
                return new ArrayList<>(list);
            }
        }

        if (fallbackLangConfig != null) {
            List<String> list = fallbackLangConfig.getStringList(fullKey);
            if (list != null && !list.isEmpty()) {
                return new ArrayList<>(list);
            }
        }

        return Collections.emptyList();
    }

    /**
     * Yer tutucuları (%key% -> val) uygulayarak mesajı formatlar.
     */
    public String format(String key, String... replacements) {
        return formatWithFallback(key, key, replacements);
    }

    public String formatWithFallback(String key, String defaultFallback, String... replacements) {
        String msg = getRawMessage(key, defaultFallback);
        if (replacements != null) {
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                if (replacements[i] != null && replacements[i + 1] != null) {
                    msg = msg.replace(replacements[i], replacements[i + 1]);
                }
            }
        }
        return msg;
    }

    /**
     * Mesajı renk kodlarıyla ve yer tutucularla renklendirip gönderir.
     */
    public void send(CommandSender sender, String key, String... replacements) {
        sendWithFallback(sender, key, key, replacements);
    }

    public void sendWithFallback(CommandSender sender, String key, String defaultFallback, String... replacements) {
        if (sender == null) return;
        String formatted = formatWithFallback(key, defaultFallback, replacements);
        Component component = ColorUtils.toComponent(formatted);
        sender.sendMessage(component);
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
