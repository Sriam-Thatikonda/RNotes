# RNotes — Windows Desktop Application

An offline-first, privacy-focused Windows desktop version of **RNotes**, engineered with **React 18**, **TypeScript**, **Vite**, **Tauri (v2)**, and **SQLite**.

RNotes Windows faithfully reproduces the design language, note models, rich block behaviors, and storymode experience of the Android RNotes application, while being adapted as a desktop experience.

---

## Table of Contents
1. [Project Boundary & Offline Guarantee](#1-project-boundary--offline-guarantee)
2. [Technology Stack](#2-technology-stack)
3. [Architecture & Directory Structure](#3-architecture--directory-structure)
4. [Core Features](#4-core-features)
   - [A. Two-Pane Desktop Layout & Empty State](#a-two-pane-desktop-layout--empty-state)
   - [B. Master Password Protection & Vault Lock](#b-master-password-protection--vault-lock)
   - [C. Rich Block Editor & Checklists](#c-rich-block-editor--checklists)
   - [D. Storymode Notes (Dialogue & Screenplay)](#d-storymode-notes-dialogue--screenplay)
   - [E. Deletion Confirmation & Instant Undo](#e-deletion-confirmation--instant-undo)
   - [F. 7 Curated Note Palettes](#f-7-curated-note-palettes)
   - [G. SQLite Persistence & Auto-Save](#g-sqlite-persistence--auto-save)
   - [H. Backup Export & Import (JSON)](#h-backup-export--import-json)
5. [Keyboard Shortcuts](#5-keyboard-shortcuts)
6. [Data Storage & Security](#6-data-storage--security)
7. [Getting Started & Development Commands](#7-getting-started--development-commands)
8. [Production Build](#8-production-build)
9. [Troubleshooting](#9-troubleshooting)

---

## 1. Project Boundary & Offline Guarantee

> [!IMPORTANT]
> **Complete Application Isolation**:
> - The Windows desktop application is **completely standalone** and self-contained inside `windows/`.
> - It does **NOT** share runtime data, sync services, cloud accounts, or dependencies with the Android application in `app/`.
> - The Android application remains 100% untouched, fully functional, and independently buildable.

```text
                 RNotes Git Repository
                         │
              ┌──────────┴──────────┐
              │                     │
              ▼                     ▼
       Android RNotes         Windows RNotes
              │                     │
        Kotlin/Compose         React/TypeScript
              │                     │
            Room                 SQLite
              │                     │
         Android only          Windows only
              │                     │
              └──── NO SYNC ────────┘
```

### Offline & Privacy Guarantees
- **No Cloud Dependencies**: Works without Wi-Fi, Ethernet, Microsoft accounts, or Google accounts.
- **No Analytics / No Telemetry**: Zero network requests, tracking scripts, or remote telemetry.
- **Local Data Only**: All notes, character definitions, and settings remain on your local machine.

---

## 2. Technology Stack

| Layer | Technology | Rationale |
|---|---|---|
| **Desktop Shell** | [Tauri (v2.1)](https://tauri.app/) | Minimal memory footprint (~30MB RAM), native Windows WebView2, lightweight single binary. |
| **Frontend UI** | [React 18](https://react.dev/) + [TypeScript](https://www.typescriptlang.org/) | Type-safe, component-driven UI architecture. |
| **Build Tooling** | [Vite 6](https://vitejs.dev/) | Sub-second HMR and optimized production bundling. |
| **Local Database** | [SQLite](https://www.sqlite.org/) via `@tauri-apps/plugin-sql` | Reliable, transactional ACID storage in local AppData. |
| **Cryptography** | W3C Web Crypto API (`crypto.subtle`) | PBKDF2-SHA256 with 20,000 iterations for secure master password hashing. |
| **Styling** | Vanilla CSS Design System | Curated CSS custom properties for Light/Dark themes and 7 note palettes without heavy UI library bloat. |
| **Icons** | [Lucide React](https://lucide.dev/) | Clean, stroke-consistent icons visually matching Android Material Rounded icons. |

---

## 3. Architecture & Directory Structure

```text
windows/
├── src/
│   ├── components/
│   │   ├── Sidebar.tsx               ← Left pane: Search, filters, note cards, theme/lock buttons
│   │   ├── NoteCard.tsx              ← Note preview card with color border, badges, and avatars
│   │   ├── EmptyEditorState.tsx      ← Right pane empty window with shortcuts legend
│   │   ├── LockScreen.tsx            ← Fullscreen vault lock screen with shake animation
│   │   ├── SetupPasswordModal.tsx    ← Master password setup with warning acknowledgement
│   │   ├── SecuritySettingsModal.tsx ← Security settings (toggle lock, change/remove password)
│   │   ├── ConfirmationDialog.tsx    ← Safety modal for note and message deletions
│   │   ├── CharacterModal.tsx        ← Add/edit story characters with emoji picker & colors
│   │   └── Snackbar.tsx              ← Bottom notification with instant UNDO action
│   ├── editor/
│   │   ├── Editor.tsx                ← Master editor pane with title, color picker, autosave
│   │   ├── RichBlockItem.tsx         ← Paragraphs, checklists, bullets, numbered lists
│   │   ├── StoryCharacterBar.tsx     ← Bottom-docked speaker switcher & Chat/Script toggles
│   │   └── StoryDialogueBlockItem.tsx ← Chat bubbles and Screenplay script formatting
│   ├── database/
│   │   └── db.ts                     ← SQLite service (%APPDATA%\com.baverika.rnotes\rnotes.db)
│   ├── models/
│   │   └── note.ts                   ← Note, NoteColor, RichContent, RichBlock, StoryCharacter
│   ├── services/
│   │   ├── passwordService.ts        ← PBKDF2-SHA256 offline vault encryption & hashing
│   │   └── exportImportService.ts    ← JSON backup export/import (Android Notoir schema v1)
│   ├── styles/
│   │   ├── theme.css                 ← Design tokens, light/dark themes, 7 note container palettes
│   │   └── layout.css                ← Master-detail two-pane layout, modals, animations
│   ├── App.tsx                       ← Desktop application root, shortcuts & lock state
│   └── main.tsx                      ← Vite entrypoint
├── src-tauri/
│   ├── capabilities/
│   │   └── default.json              ← Permissions for Tauri core and SQL plugin
│   ├── icons/                        ← High-res Windows icons (icon.ico, 32x32.png, 128x128.png)
│   ├── src/
│   │   └── main.rs                   ← Native Rust entrypoint registering SQL plugin
│   ├── Cargo.toml                    ← Rust dependencies (tauri v2.1, tauri-plugin-sql v2.4)
│   └── tauri.conf.json               ← Window dimensions (1280x800, min 800x600) and config
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

---

## 4. Core Features

### A. Two-Pane Desktop Layout & Empty State
- **Master-Detail View**: Fixed sidebar on the left and responsive editor pane on the right.
- **Dedicated Empty Window (`EmptyEditorState`)**: When no note is selected (or after pressing `Escape`), the right pane presents a clean empty state with action buttons and keyboard shortcuts.
- **Real-Time Search**: Instant search filtering matching note titles and text contents with case-insensitivity.
- **Filter Chips**: Filter notes by **All**, **Notes**, or **Stories** with active pill counters.
- **Pinned Notes**: Pinned notes remain grouped at the top with pin indicators.

### B. Master Password Protection & Vault Lock
- **PBKDF2 Hashing**: Offline password hashing using W3C Web Crypto API with **20,000 iterations** and a **16-byte random salt**, matching Android's `PasswordManager.kt`.
- **Irrecoverable Password Warning**: Mandatory warning acknowledgment checkbox during password setup emphasizing that no email reset or backdoor exists.
- **Vault Lock Screen**: Fullscreen lock with error shake animation, password visibility toggle, and instant submission on `Enter`.
- **Complete Privacy Obscuring**: When locked or setting up a password, the underlying application is blurred by **28px**, dimmed to 8% opacity, and disabled (`pointer-events: none`). Zero note text is rendered in the DOM while locked.
- **Manual Lock Button & Shortcut**: Dedicated Lock button in the sidebar header and global **`Ctrl + L`** shortcut to immediately lock the app.
- **Security Dialog**: Toggle lock on/off, change master password, or remove password completely.

### C. Rich Block Editor & Checklists
- **Paragraphs**: Clean distraction-free multiline text editing.
- **Interactive Checklists**: Checkboxes with completion strikethrough, dimming animations, and checklist progress indicators on cards (`✓ 3/5`).
- **Bullets & Numbered Lists**: Automatic list continuation on `Enter`.
- **Smart Line Handling**:
  - Pressing `Enter` splits text at cursor position.
  - Pressing `Backspace` on an empty list item reverts it to a paragraph or merges it with the preceding block.

### D. Storymode Notes (Dialogue & Screenplay)
- **Conversational Capture**: Create characters with emoji avatars (🧙, 🤖, 🕵️, 👩‍🚀, etc.), custom accent colors, and roles.
- **Bottom-Docked Character Bar**: 1-click speaker switching with active speaker glow.
- **Smart Conversational Flow**: When typing dialogue between 2 characters, pressing `Enter` automatically alternates speakers (`Character A -> Character B`).
- **Dual Presentation Views**:
  - **Chat Bubble View**: Alternating left/right speech bubbles with character avatar, colored name, parenthetical emotion tags (`(whispering)`), and trash delete button.
  - **Screenplay Script View**: Classic screenplay format with centered character names, parentheticals, and dialogue blocks.
- **Scene / Narrator Beats**: Full-width italic cards for setting descriptions and stage directions.

### E. Deletion Confirmation & Instant Undo
- **Safety Dialog**: Confirmation modal before deleting any note, dialogue bubble, or scene beat.
- **Top App Bar Undo/Redo**: Prominent **Undo** (`Ctrl+Z`) and **Redo** (`Ctrl+Y` / `Ctrl+Shift+Z`) buttons.
- **Instant Snackbar**: Bottom snackbar with `"UNDO"` button appears when a note or message is deleted.

### F. 7 Curated Note Palettes
Accurately reproduces Android RNotes' 7 container and border colors in both light and dark themes:

| Palette Name | Light Container | Dark Container | Accent |
|---|---|---|---|
| **Default** | `#FFFFFF` | `#1C1C20` | Charcoal |
| **Ocean Blue** | `#F0F6FF` | `#172033` | `#3B82F6` |
| **Sage Green** | `#F0FDF4` | `#14291E` | `#10B981` |
| **Sunset Amber** | `#FFFBEB` | `#2D2312` | `#F59E0B` |
| **Coral Red** | `#FEF2F2` | `#2E1517` | `#EF4444` |
| **Lavender Purple** | `#FAF5FF` | `#231738` | `#8B5CF6` |
| **Nordic Teal** | `#F0FDFA` | `#132828` | `#14B8A6` |

### G. SQLite Persistence & Auto-Save
- **Database Location**: Stored safely in `%APPDATA%\com.baverika.rnotes\rnotes.db`.
- **Autosave Engine**: 500ms debounced auto-save on typing, plus immediate sync on note selection switch or window blur.
- **Dev Fallback**: Seamless fallback to in-browser storage when running purely in Vite web preview mode.

### H. Backup Export & Import (JSON)
- **100% Schema Equivalence**: Exports and imports JSON container files matching Android's `NotoirExportContainer` schema version 1.
- **Portable Backups**: Back up all notes, characters, formatting, timestamps, and colors into a single `.json` file at any time via the sidebar header.

---

## 5. Keyboard Shortcuts

| Shortcut | Action | Scope |
|---|---|---|
| `Ctrl + N` | Create a new standard note | Global |
| `Ctrl + Shift + N` | Create a new storymode note | Global |
| `Ctrl + F` | Focus search bar | Global |
| `Ctrl + L` | Immediately lock vault | Global |
| `Esc` | Deselect active note (return to empty screen) | Global |
| `Ctrl + Z` | Undo previous edit | Editor |
| `Ctrl + Y` / `Ctrl + Shift + Z` | Redo previous edit | Editor |
| `Enter` | Split block / Alternate dialogue speaker / Continue list | Editor |
| `Backspace` | Unindent / Revert empty list to paragraph | Editor |

---

## 6. Data Storage & Security

### Database Location
The SQLite database is stored in the standard Windows user application data directory:
```text
C:\Users\<YourUsername>\AppData\Roaming\com.baverika.rnotes\rnotes.db
```

### Table Schema
```sql
CREATE TABLE IF NOT EXISTS notes (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    contentJson TEXT NOT NULL,
    plainText TEXT NOT NULL,
    color TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    isPinned INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notes_updatedAt ON notes(updatedAt DESC);
CREATE INDEX IF NOT EXISTS idx_notes_isPinned ON notes(isPinned DESC);
CREATE INDEX IF NOT EXISTS idx_notes_title ON notes(title);
```

### Security Architecture
- Notes remain on your local disk unexposed to external services.
- Passwords are never stored in plaintext; only the PBKDF2 hash and salt are saved in local storage settings.
- Parameterized SQL queries are used for all database operations to prevent injection.

---

## 7. Getting Started & Development Commands

### Prerequisites
1. **Node.js**: v18 or newer
2. **Rust & Cargo**: Installed via [rustup.rs](https://rustup.rs/)
3. **Visual Studio C++ Build Tools**: MSVC compiler component (Visual Studio 2019/2022 or Build Tools)

### Installation
```powershell
cd windows
npm install
```

### Run in Development (Web Preview)
Runs Vite development server in the browser:
```powershell
npm run dev
```

### Run in Desktop Mode (Tauri)
Launches the native Windows desktop application with hot-reloading:
```powershell
npm run tauri dev
```

---

## 8. Production Build

To compile the native Windows release executable:
```powershell
npm run build:desktop
```
This builds an optimized standalone executable without requiring external MSI installer tooling:
```text
windows/src-tauri/target/release/rnotes-desktop.exe (~11.9 MB)
```

To build full MSI/NSIS installer packages:
```powershell
npm run build
npx tauri build
```

---

## 9. Troubleshooting

### 1. `cargo check` fails with `icon.ico not found`
Icons must exist in `windows/src-tauri/icons/`. Run:
```powershell
npx tauri icon app-icon.svg
```
This generates all required `.ico` and `.png` resolutions automatically.

### 2. Password prompt appears every time the app launches
This is the default security behavior when **Vault Password Lock** is enabled. To disable startup locking:
1. Click the **Shield** icon in the sidebar header.
2. Toggle off **Vault Password Lock** (requires entering your current master password).
3. The app will now open directly without asking for a password, while keeping your password safely stored if you ever want to re-enable it.

### 3. Forgot Master Password
As clearly stated during setup, RNotes uses zero-knowledge local hashing. There is no reset email or backdoor. If a password is forgotten, the only way to reset the app lock is to clear the local security settings in local storage.
