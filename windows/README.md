# RNotes — Windows Desktop Application

A modern, offline-first Windows desktop version of **RNotes**, developed with **React**, **TypeScript**, **Vite**, **Tauri**, and **SQLite**.

---

## 1. Project Boundary & Independence

> [!IMPORTANT]
> **Zero Shared Runtime Dependencies**:
> - The Windows desktop application is **completely standalone** and isolated in this `windows/` directory.
> - It does **not** share runtime data, sync services, or application code with the existing Android application in `app/`.
> - The Android application remains 100% untouched and functional.

This desktop app works completely offline without:
* No cloud synchronization
* No backend servers
* No internet requirement
* No user accounts or login

---

## 2. Technology Stack

- **Desktop Framework**: Tauri (v2)
- **Frontend**: React 18 + TypeScript + Vite
- **Database**: SQLite (via `tauri-plugin-sql`, stored in user application data)
- **Icons**: Lucide Icons (matching Android Material Rounded styles)
- **Styling**: Native CSS architecture with curated light/dark themes and 7 custom note container palettes.

---

## 3. Architecture Overview

```text
windows/
├── src/
│   ├── components/            ← Sidebar, NoteCard, Dialogs, Modals, Snackbar
│   ├── editor/                ← RichBlockItem, StoryCharacterBar, StoryDialogueBlockItem, Editor
│   ├── database/              ← SQLite Database layer & migrations
│   ├── models/                ← Note, NoteColor, RichContent, RichBlock, StoryCharacter
│   ├── services/              ← JSON Backup Export and Import services
│   ├── styles/                ← theme.css (light/dark tokens), layout.css
│   ├── App.tsx                ← Two-pane Desktop Master-Detail Layout
│   └── main.tsx               ← React entrypoint
├── src-tauri/
│   ├── src/main.rs            ← Native Windows Tauri entrypoint
│   ├── Cargo.toml             ← Rust dependencies (tauri, tauri-plugin-sql)
│   └── tauri.conf.json        ← Window, bundle, and plugin configuration
├── package.json
├── vite.config.ts
├── tsconfig.json
└── README.md
```

---

## 4. Key Features & Behavioral Equivalence with Android

### A. Two-Pane Desktop Layout
- Left sidebar with search bar, filter chips (`All`, `Notes`, `Stories`), pinned notes section, and note cards.
- Right pane displaying the selected note editor with autosave.

### B. Rich Notes & Checklists
- Paragraphs, interactive checklists with completed strikethrough, bullet points, and numbered lists.
- 7 curated note container & border colors (Default, Ocean Blue, Sage Green, Sunset Amber, Coral Red, Lavender Purple, Nordic Teal).

### C. Storymode Notes
- Conversational note-taking between characters.
- Character bar docked at the bottom with active speaker highlight.
- Dual view modes: **Chat Bubble View** and **Screenplay Script View**.
- Scene / Narrator beats.
- Smart `Enter` key: when typing dialogue with 2 characters, pressing Enter automatically alternates the speaker (`Char A -> Char B`).
- Character manager: customize name, emoji avatar, color, and role.

### D. Deletion Safety & Undo/Redo
- Confirmation dialog before deleting any message, scene beat, or note.
- Prominent **Undo** (`Ctrl+Z`) and **Redo** (`Ctrl+Y` / `Ctrl+Shift+Z`) in the Top App Bar.
- Instant **Snackbar** with "UNDO" button on message/note deletion.

### E. Database Persistence & Offline Guarantee
- SQLite table `notes` stored at `%APPDATA%\com.baverika.rnotes\rnotes.db`.
- Automatic 500ms debounced auto-save + immediate flush on note switch / exit.
- Full backup export and import using standard JSON container format version 1.

---

## 5. Development & Build Commands

### Prerequisites
- Node.js (v18+)
- Rust & Cargo (via `rustup`)
- Visual Studio C++ Build Tools

### Installation
```powershell
cd windows
npm install
```

### Run in Development (Web / Browser Preview)
```powershell
npm run dev
```

### Run in Desktop Tauri Mode
```powershell
npm run tauri dev
```

### Compile Production Desktop Binary
```powershell
npm run tauri build
```
The compiled Windows executable (`.exe` and `.msi` installer) will be generated in `src-tauri/target/release/bundle/`.
