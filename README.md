<<<<<<< HEAD
# 📢 OmniAnnouncer

[![Engine](https://img.shields.io/badge/Minecraft-1.20.6--26.2-brightgreen.svg)](https://papermc.io)
[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Folia-blue.svg)](https://github.com/PaperMC/Folia)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**OmniAnnouncer** is a modern, high-performance, and feature-rich announcement plugin designed for Minecraft servers. Built from the ground up to fully support both **Paper** and **Folia** multi-threaded server architectures with zero performance overhead.

---

## ✨ Features

* **📺 Multi-Channel Broadcasting:** Send announcements via Chat, Title/Subtitle, BossBar, or ActionBar.
* **⚡ ALL-IN-ONE Mode:** Trigger all communication channels simultaneously for critical server events.
* **⚡ Folia & Paper Native:** Fully asynchronous and region-aware scheduling logic to prevent thread lag.
* **🎨 Full RGB & Hex Colors:** Supports modern Hex formatting (`&#RRGGBB`) along with standard color codes (`&a-f`).
* **🖱️ Interactive Chat:** Hover tooltips and click actions (Open URL, Run Command, Suggest Command).
* **⏰ Scheduled Broadcasts:** Flexible task scheduling for timed automatic announcements.
* **🔊 Sound Effects:** Assign custom Spigot/Paper sound effects with pitch and volume controls.
* **🌍 Filtering:** Restrict announcements by world or custom permission nodes.
* **🧩 PlaceholderAPI:** Native support for standard placeholders.

---

## 🛠️ Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/omniannouncer reload` | Reloads configurations and active tasks | `omniannouncer.admin` |
| `/omniannouncer broadcast <name>` | Manually triggers a specified announcement | `omniannouncer.admin` |

> **Aliases:** `/announcement`, `/duyuru`

---

## ⚙️ Configuration Example (`config.yml`)

```yaml
announcements:
  discord_chat:
    enabled: true
    type: "CHAT"
    permission: ""
    worlds: []
    sound:
      enabled: true
      name: "BLOCK_NOTE_BLOCK_PLING"
      volume: 1.0
      pitch: 1.5
    chat:
      messages:
        - "&#AA00FF&m----------------------------------------"
        - "&#AA00FF&lDISCORD &f| &#55FF55Join Our Community!"
        - "&#AA00FF&m----------------------------------------"
      hover-text: "&#55FF55Click to join our Discord!"
      click-action: "OPEN_URL"
      click-value: "[https://discord.gg/yourserver](https://discord.gg/yourserver)"
=======
# 📢 OmniAnnouncer

[![Engine](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen.svg)](https://papermc.io)
[![Platform](https://img.shields.io/badge/Platform-Paper-blue.svg)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**OmniAnnouncer** is a modern, lightweight and fully configurable announcement plugin for Minecraft servers. It supports chat, title/subtitle, action bar and boss bar announcements, interactive hover/click events, scheduled broadcasts and PlaceholderAPI placeholders.

---

## ✨ Features

* **📺 Multi-channel broadcasting:** Chat, Title/Subtitle, ActionBar, BossBar or all of them at once (`ALL`).
* **⏰ Scheduled announcements:** Run an announcement every day or on specific days at a specific time.
* **🔁 Automatic rotation:** Broadcast enabled announcements in `SEQUENCE` or `RANDOM` mode.
* **🎨 Full RGB & Hex colors:** `&#RRGGBB` and standard `&a-f` color codes.
* **🖱️ Interactive chat:** Hover tooltips and click actions (`OPEN_URL`, `RUN_COMMAND`, `SUGGEST_COMMAND`).
* **🔊 Sound effects:** Per-announcement sound, volume and pitch.
* **🌍 Filtering:** Restrict announcements by world or permission.
* **🧩 PlaceholderAPI:** Native placeholder support when PlaceholderAPI is installed.
* **🕒 Timezone aware:** Scheduled announcements use the configured `settings.timezone`.

---

## 📦 Installation

1. Drop `OmniAnnouncer-1.0.0.jar` into your server's `plugins/` folder.
2. Start the server once to generate `plugins/OmniAnnouncer/config.yml`.
3. Edit the configuration and run `/duyuru reload`.

**Requirements:** Paper 1.21.x and Java 21 (PlaceholderAPI is optional).

---

## 🛠️ Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/duyuru help` | Shows the command list | `omniannouncer.admin` |
| `/duyuru list` | Lists all announcements with their status | `omniannouncer.admin` |
| `/duyuru announce <name>` | Broadcasts an announcement to the whole server | `omniannouncer.admin` |
| `/duyuru broadcast <name>` | Alias of `announce` | `omniannouncer.admin` |
| `/duyuru send <player> <name>` | Sends an announcement to a single player | `omniannouncer.admin` |
| `/duyuru preview <name>` | Shows an announcement only to yourself | `omniannouncer.admin` |
| `/duyuru reload` | Reloads the configuration and restarts tasks | `omniannouncer.reload` or `omniannouncer.admin` |

> **Aliases:** `/announcement`, `/duyuru`

Disabled announcements (`enabled: false`) cannot be broadcast with `announce`/`send`, but they can still be previewed with `preview`.

---

## ⚙️ Configuration (`config.yml`)

```yaml
settings:
  auto-broadcaster-enabled: true   # Enable automatic rotation
  interval-seconds: 300            # Delay between automatic broadcasts (seconds)
  broadcast-mode: "SEQUENCE"       # SEQUENCE or RANDOM
  timezone: "Europe/Istanbul"      # Used by scheduled announcements

messages:
  no-permission: "&#FF3333You do not have permission to use this command!"
  reload-success: "&#33FF88[OmniAnnouncer] Configuration reloaded successfully!"
  usage: "&#FFCC00Usage: &#FFFFFF/%label% <reload|announce|send|preview|list|help>"
  announcement-not-found: "&#FF3333No announcement found with the name '%name%'!"
  announcement-disabled: "&#FF3333The announcement '%name%' is currently disabled!"
  broadcast-success: "&#33FF88[OmniAnnouncer] '%name%' duyurusu basariyla yayinlandi."
  send-success: "&#33FF88[OmniAnnouncer] '%name%' duyurusu %player% oyuncusuna gonderildi."
  preview-success: "&#33FF88[OmniAnnouncer] '%name%' duyurusu onizlendi."
  player-not-found: "&#FF3333Player not found: %player%"
  player-only: "&#FF3333This command can only be used by players!"
  list-header: "&#FFCC00OmniAnnouncer Duyurulari:"
  list-entry: "&#FFFFFF- %name% &7[%status%]"
  list-active: "&aAktif"
  list-disabled: "&cDevre Disi"
  list-empty: "&#FFCC00Henuz tanimlanmis bir duyuru yok."
  help:
    - "&#FFCC00OmniAnnouncer Komutlari:"
    - "&#FFFFFF/%label% help &7- Bu yardim menusunu gosterir."
    - "&#FFFFFF/%label% list &7- Tum duyurulari ve durumlarini listeler."
    - "&#FFFFFF/%label% announce <duyuru> &7- Duyuruyu sunucuya yayinlar."
    - "&#FFFFFF/%label% send <oyuncu> <duyuru> &7- Duyuruyu sadece o oyuncuya gonderir."
    - "&#FFFFFF/%label% preview <duyuru> &7- Duyuruyu sadece kendi ekraninizda onizler."
    - "&#FFFFFF/%label% reload &7- Konfigurasyonu yeniler."

announcements:
  discord_chat:
    enabled: true
    type: "CHAT"                   # CHAT, ACTIONBAR, TITLE, BOSSBAR, ALL
    permission: ""                 # Empty = everyone
    worlds: []                     # Empty = all worlds
    sound:
      enabled: true
      name: "BLOCK_NOTE_BLOCK_PLING"
      volume: 1.0
      pitch: 1.5
    chat:
      messages:
        - "&#AA00FF&m----------------------------------------"
        - "&#AA00FF&lDISCORD &f| &#55FF55Join Our Community!"
        - "&7Chat with players, enter giveaways, and get support:"
        - "&#55FF55&nhttps://discord.gg/yourserver"
        - "&#AA00FF&m----------------------------------------"
      hover-text: "&#55FF55Click to join our Discord server!"
      click-action: "OPEN_URL"     # OPEN_URL, RUN_COMMAND, SUGGEST_COMMAND
      click-value: "https://discord.gg/yourserver"

scheduled-announcements:
  daily_event:
    enabled: true
    day: "EVERYDAY"                # EVERYDAY or MONDAY...SUNDAY
    time: "20:00"                  # HH:mm in settings.timezone
    type: "TITLE"
    permission: ""
    worlds: []
    sound:
      enabled: true
      name: "UI_TOAST_CHALLENGE_COMPLETE"
      volume: 1.0
      pitch: 1.0
    title:
      title: "&#00FF00&lDAILY EVENT STARTED!"
      subtitle: "&#FFFF00Chests have been refreshed in the mining zone."
      fade-in: 10                  # Ticks (20 ticks = 1 second)
      stay: 80
      fade-out: 20
```

### Announcement types

| Type | Description |
| :--- | :--- |
| `CHAT` | Multi-line chat message with optional hover/click |
| `TITLE` | Title and subtitle |
| `ACTIONBAR` | Text above the hotbar |
| `BOSSBAR` | Boss bar with configurable color, style and duration |
| `ALL` | Sends every configured channel at once (empty channels are skipped) |

> `duration-seconds` must be greater than `0`; otherwise the boss bar is not created.

---

## 🔨 Building from source

Maven 3.9+ and JDK 21 are required. The project uses the PaperMC repository, so no extra configuration is needed.

```bash
mvn clean package
```

The compiled plugin is written to `target/OmniAnnouncer-1.0.0.jar`.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
>>>>>>> e5a1b90 (chore: release v1.1.0 - Kyori Adventure, I18n and Folia scheduler support)
