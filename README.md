# RNotes (Notoir)

A modern, privacy-first, offline note-taking and storyboarding system featuring rich block formatting, checklists, visual novel dialogue note-taking, master password encryption, and local SQLite persistence.

This repository hosts two **completely independent applications** sharing the same design language:
1. **Android Application** (`app/`) — Native Android app written in Kotlin & Jetpack Compose.
2. **Windows Desktop Application** (`windows/`) — Native Windows desktop application built with React 18, TypeScript, Tauri (v2), and SQLite.

---

## Repository Architecture & Project Boundary

```text
RNotes Git Repository
│
├── app/                     ← ANDROID NATIVE APPLICATION
│   ├── src/main/java/...    ← Kotlin & Jetpack Compose
│   └── build.gradle.kts     ← Gradle build configuration
│
├── windows/                 ← WINDOWS DESKTOP APPLICATION
│   ├── src/                 ← React 18 & TypeScript frontend
│   ├── src-tauri/           ← Tauri v2 Rust desktop shell
│   ├── package.json         ← Node dependencies & scripts
│   └── README.md            ← Dedicated Windows desktop guide
│
├── .gitignore
└── README.md
```

> [!IMPORTANT]
> **Strict Independence**:
> - The Android and Windows applications are **independent products** inside this Git repository.
> - There is **NO cloud synchronization, NO shared backend, and NO shared runtime dependencies**.
> - Each application stores its data in its respective platform's local SQLite database.

---

## Feature Comparison

| Feature | Android RNotes (`app/`) | Windows RNotes (`windows/`) |
|---|:---:|:---:|
| **Platform** | Android 8.0+ (API 26+) | Windows 10 / 11 (x64) |
| **UI Framework** | Jetpack Compose + Material 3 | React 18 + TypeScript + Vite |
| **Desktop Shell** | Android Native Activity | Tauri v2 (MSVC / WebView2) |
| **Local Database** | Room (SQLite) | SQLite (`tauri-plugin-sql`) |
| **Storymode Dialogue Notes** | Yes (Chat bubble & Script views) | Yes (Chat bubble & Script views) |
| **Rich Blocks & Checklists** | Yes | Yes |
| **7 Curated Note Palettes** | Yes | Yes |
| **Master Password Vault Lock** | Yes (PBKDF2-SHA256, 20k iter) | Yes (PBKDF2-SHA256, 20k iter) |
| **Manual Lock Button & Shortcut**| Yes | Yes (`Ctrl + L` & sidebar button) |
| **Autosave** | Yes | Yes (500ms debounce) |
| **Backup Export & Import** | JSON Container v1 | JSON Container v1 |
| **Offline Guarantee** | 100% Offline (No cloud) | 100% Offline (No cloud) |

---

## 1. Windows Desktop Application

For full documentation, architecture diagrams, and detailed guides, refer to [**`windows/README.md`**](windows/README.md).

### Quick Start (Windows)
```powershell
# Navigate to windows directory
cd windows

# Install dependencies
npm install

# Run in development mode (browser preview)
npm run dev

# Run in desktop Tauri mode
npm run tauri dev

# Compile standalone release executable (.exe)
npm run build:desktop
```
The compiled standalone executable will be located at:
```text
windows/src-tauri/target/release/rnotes-desktop.exe (~11.9 MB)
```

---

## 2. Android Application

### Quick Start (Android)
Prerequisites: Android Studio, JDK 17, and Android SDK 35.

```powershell
# Build debug APK from project root
./gradlew assembleDebug

# Install on connected device or emulator
./gradlew installDebug
```
The compiled debug APK will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Core Highlights

### 1. Conversational Storymode Notes
Take notes as characters in a dialogue or visual novel screenplay:
* Create characters with custom emoji avatars, roles, and accent colors.
* Bottom-docked speaker switcher bar.
* In two-character conversations, pressing `Enter` automatically alternates speakers (`Character A -> Character B`).
* Switch between **Chat Bubble View** and **Screenplay Script View** with a single click.
* Scene / Narrator cards for setting direction.

### 2. Privacy & Master Password Vault Lock
* Protected notes cannot be viewed without the master password.
* Hashing utilizes **PBKDF2-SHA256** with **20,000 iterations** and a **16-byte cryptographically secure random salt**.
* **Zero Recovery Backdoor**: If the password is forgotten, notes cannot be accessed, protecting personal privacy.
* Instant manual lock via the **Lock** icon or the **`Ctrl + L`** shortcut on Windows.
* Full background obscuring: When locked, the underlying app is blurred and notes are omitted from the DOM.

### 3. Rich Formatting & 7 Curated Colors
* Checklists with completed item strikethrough and card progress indicators.
* Bulleted and numbered lists with automatic continuation.
* 7 curated container and border colors: Default, Ocean Blue, Sage Green, Sunset Amber, Coral Red, Lavender Purple, and Nordic Teal.

---

## Security & Data Privacy

* Notes are never sent to external servers or cloud providers.
* No telemetry, crash reporters, or third-party tracking libraries are included.
* All data is stored in platform-standard application data paths:
  * **Android**: Room database within private app storage.
  * **Windows**: `%APPDATA%\com.baverika.rnotes\rnotes.db`.
