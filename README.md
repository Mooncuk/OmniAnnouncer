# 📢 OmniAnnouncer

[![Engine](https://img.shields.io/badge/Minecraft-1.20.6--1.26.2-brightgreen.svg)](https://papermc.io)
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
