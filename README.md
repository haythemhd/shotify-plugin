# Shotify — Screen Capture & Share for Android Studio

> Record your Android device screen, take screenshots, and share them instantly — all without leaving Android Studio.

![Plugin version](https://img.shields.io/badge/version-1.0.0-blue)
![IDE compatibility](https://img.shields.io/badge/IDE-Android%20Studio%202024.1%2B-green)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## What it does

Shotify adds a **Tool Window** and **keyboard shortcuts** to Android Studio that let you:

- **Record your device screen** as an MP4 video via ADB — start with one click, stop and get an upload link automatically
- **Take instant screenshots** as PNG — captured, uploaded, and URL copied in under 2 seconds
- **Upload automatically** to [Cloudinary](https://cloudinary.com) (persistent, free tier) or [0x0.st](https://0x0.st) (anonymous, no account needed)
- **Copy the URL** to your clipboard the moment the upload finishes
- **Get an IDE notification** with a clickable link and an "Open in Browser" button
---

## Screenshots

<!-- Add your screenshots here -->
_Screenshots coming soon._

---

## Installation

### From JetBrains Marketplace

1. Open **Settings → Plugins → Marketplace**
2. Search for **Shotify**
3. Click **Install** and restart the IDE

### Build from source

```bash
git clone https://github.com/itemhd/shotify-plugin
cd shotify-plugin
./gradlew buildPlugin
# Output: build/distributions/shotify-plugin-1.0.0.zip
```

Then install the `.zip` via **Settings → Plugins → ⚙️ → Install Plugin from Disk**.

---

## Configuration

Open **Settings → Tools → Shotify** and choose your upload service:

| Service | Setup required | Notes |
|---|---|---|
| **0x0.st** | None | Anonymous, files may expire after 30 days of inactivity |
| **Cloudinary** | Cloud name + Upload preset | Free tier, files are permanent |

### Cloudinary setup (2 minutes)

1. Create a free account at [cloudinary.com](https://cloudinary.com)
2. Your **Cloud name** is shown on your dashboard
3. Go to **Settings → Upload** and create an **unsigned** upload preset
4. Paste both values into Shotify settings

---

## Usage

### Tool Window

1. Open **View → Tool Windows → Shotify** (or click the tab at the bottom)
2. Make sure your Android device is connected (`adb devices` should show it)
3. Hit **▶ Start Recording** — recording begins on the device
4. Hit **⏹ Stop & Upload** — video is pulled, uploaded, URL copied to clipboard
5. Or hit **📷 Screenshot** for an instant capture

### Keyboard shortcuts

| Action | Shortcut |
|---|---|
| Start Recording | `Ctrl+Shift+R` |
| Stop Recording & Upload | `Ctrl+Shift+T` |
| Take Screenshot & Upload | `Ctrl+Shift+S` |

### Tools menu

**Tools → Shotify → [action]**

---

## How it works

```
┌─────────────────────────────────────────────────────────┐
│  Android Studio                                         │
│                                                         │
│  ┌──────────┐    ADB        ┌──────────────────────┐   │
│  │ Shotify  │ ──────────► │ Android Device       │   │
│  │ Plugin   │ ◄────────── │ screenrecord / screencap│   │
│  └────┬─────┘   pull file  └──────────────────────┘   │
│       │                                                 │
│       │ HTTP (OkHttp)                                   │
│       ▼                                                 │
│  ┌─────────────────┐     ┌───────────┐                 │
│  │  Cloudinary     │ OR  │  0x0.st   │                 │
│  │  (your account) │     │ (anonymous│                 │
│  └────────┬────────┘     └─────┬─────┘                 │
│           └──────┬─────────────┘                       │
│                  │ URL                                  │
│                  ▼                                      │
│         Clipboard + IDE Notification                    │
└─────────────────────────────────────────────────────────┘
```

### Key behaviors

- All network and ADB operations run on `Dispatchers.IO` — **the EDT is never blocked**
- Uploads retry up to **3 times** with exponential backoff (1 s, 2 s, 4 s)
- No credentials are ever hardcoded — Cloudinary keys live only in the Settings UI
- Temporary files are cleaned up automatically after 1 hour

---

## Requirements

- **IDE:** Android Studio Hedgehog (2023.1.1) or later
- **JDK:** 17 or later
- **Android device:** API 21+ connected via ADB (USB or Wi-Fi)
- `adb devices` must list your device before starting a recording

---

## Development

```bash
# Run Android Studio sandbox with the plugin loaded
./gradlew runIde

# Run unit tests
./gradlew test

# Build the distributable ZIP
./gradlew buildPlugin
```

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| IntelliJ Platform SDK | 2024.1 | Plugin API |
| ddmlib | 30.0.4 | ADB communication |
| OkHttp3 | 4.11.0 | HTTP uploads |
| kotlinx-coroutines | 1.7.3 | Async without blocking the EDT |
| kotlin-logging-jvm | 5.1.0 | Structured logging |
| JUnit 5 | 5.10.0 | Unit tests |
| Mockito-Kotlin | 5.1.0 | Mocking |
| MockWebServer | 4.11.0 | HTTP mocking in tests |

---

## Known limitations

- `adb screenrecord` is capped at **3 minutes** per recording (Android OS limit)
- 0x0.st may delete files inactive for more than 30 days
- The ADB connection must be established before starting a recording
- Multi-device support is not yet implemented (first connected device is used)

---

## Roadmap

- [x] Multi-device support (pick which device to record)
- [ ] History of generated URLs in the Tool Window
- [ ] Visible timer during recording
- [ ] GitHub Issues integration — attach captures directly to a new issue

---

## Support

If Shotify saves you time, a coffee is always appreciated:

[![Ko-fi](https://img.shields.io/badge/Ko--fi-support-ff5e5b?logo=ko-fi&logoColor=white)](https://ko-fi.com/itemhd)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-support-ffdd00?logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/itemhd)

---

## License

[MIT](LICENSE)