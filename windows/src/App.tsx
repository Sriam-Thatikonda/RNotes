import React, { useState, useEffect, useRef } from 'react';
import { Note, createNewNote } from './models/note';
import { getAllNotes, searchNotes, saveNote, deleteNote } from './database/db';
import { Sidebar, NoteFilter } from './components/Sidebar';
import { Editor } from './editor/Editor';
import { EmptyEditorState } from './components/EmptyEditorState';
import { Snackbar } from './components/Snackbar';
import { LockScreen } from './components/LockScreen';
import { SetupPasswordModal } from './components/SetupPasswordModal';
import { SecuritySettingsModal } from './components/SecuritySettingsModal';
import { PasswordService } from './services/passwordService';
import { exportNotesToJsonFile, importNotesFromJsonFile } from './services/exportImportService';
import './styles/theme.css';
import './styles/layout.css';

export const App: React.FC = () => {
  const [notes, setNotes] = useState<Note[]>([]);
  const [selectedNoteId, setSelectedNoteId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFilter, setSelectedFilter] = useState<NoteFilter>('ALL');

  // Security & Vault Lock States
  const [isLocked, setIsLocked] = useState<boolean>(() => PasswordService.isLockEnabled());
  const [showSetupPassword, setShowSetupPassword] = useState<boolean>(
    () => !PasswordService.isPasswordConfigured() && !PasswordService.isSetupDismissed()
  );
  const [showSecuritySettings, setShowSecuritySettings] = useState<boolean>(false);

  // Dark Theme detection and persistence
  const [isDark, setIsDark] = useState<boolean>(() => {
    const saved = localStorage.getItem('rnotes_theme');
    if (saved) return saved === 'dark';
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  });

  // Snackbar state
  const [snackbar, setSnackbar] = useState<{
    message: string | null;
    actionLabel?: string | null;
    onAction?: () => void;
  }>({ message: null });

  const snackbarTimerRef = useRef<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Apply theme to HTML attribute
  useEffect(() => {
    if (isDark) {
      document.documentElement.setAttribute('data-theme', 'dark');
      localStorage.setItem('rnotes_theme', 'dark');
    } else {
      document.documentElement.removeAttribute('data-theme');
      localStorage.setItem('rnotes_theme', 'light');
    }
  }, [isDark]);

  // Load initial notes from SQLite
  useEffect(() => {
    async function load() {
      const loaded = await getAllNotes();
      setNotes(loaded);
      // Only select a note initially if vault is not locked and no setup modal is blocking
      if (
        loaded.length > 0 &&
        !PasswordService.isLockEnabled() &&
        (PasswordService.isPasswordConfigured() || PasswordService.isSetupDismissed())
      ) {
        setSelectedNoteId(loaded[0].id);
      }
    }
    load();
  }, []);

  // Filter notes on search
  useEffect(() => {
    async function runSearch() {
      if (!searchQuery.trim()) {
        const loaded = await getAllNotes();
        setNotes(loaded);
      } else {
        const results = await searchNotes(searchQuery);
        setNotes(results);
      }
    }
    const timer = setTimeout(runSearch, 150);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  const showSnackbar = (message: string, actionLabel?: string, onAction?: () => void) => {
    if (snackbarTimerRef.current) {
      window.clearTimeout(snackbarTimerRef.current);
    }
    setSnackbar({ message, actionLabel, onAction });
    snackbarTimerRef.current = window.setTimeout(() => {
      setSnackbar({ message: null });
    }, 4500);
  };

  const handleLockApp = () => {
    if (PasswordService.isPasswordConfigured()) {
      setIsLocked(true);
      setSelectedNoteId(null);
    } else {
      setShowSetupPassword(true);
    }
  };

  const handleCreateNote = async () => {
    const newNote = createNewNote('STANDARD');
    await saveNote(newNote);
    const updated = await getAllNotes();
    setNotes(updated);
    setSelectedNoteId(newNote.id);
  };

  const handleCreateStory = async () => {
    const newStory = createNewNote('STORY');
    await saveNote(newStory);
    const updated = await getAllNotes();
    setNotes(updated);
    setSelectedNoteId(newStory.id);
  };

  const handleSaveNote = async (updatedNote: Note) => {
    await saveNote(updatedNote);
    setNotes((prev) =>
      prev.map((n) => (n.id === updatedNote.id ? updatedNote : n))
    );
  };

  const handleDeleteNote = async (id: string) => {
    const noteToDelete = notes.find((n) => n.id === id);
    await deleteNote(id);
    const remaining = notes.filter((n) => n.id !== id);
    setNotes(remaining);

    if (selectedNoteId === id) {
      setSelectedNoteId(remaining.length > 0 ? remaining[0].id : null);
    }

    if (noteToDelete) {
      showSnackbar('Note deleted', 'UNDO', async () => {
        await saveNote(noteToDelete);
        const restored = await getAllNotes();
        setNotes(restored);
        setSelectedNoteId(noteToDelete.id);
      });
    }
  };

  // Export / Import
  const handleExport = async () => {
    const all = await getAllNotes();
    exportNotesToJsonFile(all);
    showSnackbar(`Exported ${all.length} notes successfully`);
  };

  const handleImportFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      const importedNotes = await importNotesFromJsonFile(file);
      for (const note of importedNotes) {
        await saveNote(note);
      }
      const reloaded = await getAllNotes();
      setNotes(reloaded);
      if (importedNotes.length > 0) {
        setSelectedNoteId(importedNotes[0].id);
      }
      showSnackbar(`Imported ${importedNotes.length} notes successfully`);
    } catch (err: any) {
      showSnackbar(`Import failed: ${err.message || 'Error parsing file'}`);
    } finally {
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  // Keyboard Shortcuts (Ctrl+N, Ctrl+Shift+N, Ctrl+F, Ctrl+L, Escape)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Escape to deselect active note and view empty state
      if (e.key === 'Escape') {
        setSelectedNoteId(null);
        return;
      }

      if (e.ctrlKey || e.metaKey) {
        if (e.key.toLowerCase() === 'n') {
          e.preventDefault();
          if (e.shiftKey) {
            handleCreateStory();
          } else {
            handleCreateNote();
          }
        } else if (e.key.toLowerCase() === 'f') {
          e.preventDefault();
          const searchInput = document.querySelector<HTMLInputElement>('.search-input');
          searchInput?.focus();
        } else if (e.key.toLowerCase() === 'l') {
          e.preventDefault();
          handleLockApp();
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [notes]);

  const selectedNote = notes.find((n) => n.id === selectedNoteId) || null;

  return (
    <>
      {/* Hidden file input for JSON import */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".json,application/json"
        style={{ display: 'none' }}
        onChange={handleImportFileChange}
      />

      {/* Lock Screen Overlay when Vault is Locked */}
      {isLocked && (
        <LockScreen
          onUnlock={() => {
            setIsLocked(false);
            // On unlock, select first note if available
            if (notes.length > 0) {
              setSelectedNoteId(notes[0].id);
            }
          }}
        />
      )}

      {/* Initial Setup Password Modal */}
      <SetupPasswordModal
        isOpen={showSetupPassword}
        isInitialSetup={true}
        onClose={() => setShowSetupPassword(false)}
        onSuccess={() => {
          setShowSetupPassword(false);
          showSnackbar('Master password set successfully');
        }}
      />

      {/* Security & Password Settings Modal */}
      <SecuritySettingsModal
        isOpen={showSecuritySettings}
        onClose={() => setShowSecuritySettings(false)}
        onLockNow={() => {
          setShowSecuritySettings(false);
          handleLockApp();
        }}
        onOpenSetup={() => {
          setShowSecuritySettings(false);
          setShowSetupPassword(true);
        }}
      />

      {/* App Shell — Heavily blurred and disabled whenever locked or in setup */}
      <div
        className={`app-container ${
          isLocked || showSetupPassword ? 'app-obscured' : ''
        }`}
      >
        {/* Left Sidebar */}
        <Sidebar
          notes={isLocked ? [] : notes}
          selectedNoteId={selectedNoteId}
          searchQuery={searchQuery}
          selectedFilter={selectedFilter}
          isDark={isDark}
          onSearchChange={setSearchQuery}
          onFilterChange={setSelectedFilter}
          onSelectNote={(id) => setSelectedNoteId(id || null)}
          onCreateNote={handleCreateNote}
          onCreateStory={handleCreateStory}
          onToggleTheme={() => setIsDark(!isDark)}
          onExportNotes={handleExport}
          onImportNotes={() => fileInputRef.current?.click()}
          onLockApp={handleLockApp}
          onOpenSecurity={() => setShowSecuritySettings(true)}
        />

        {/* Right Editor Pane — Shows Editor or Empty State */}
        {selectedNote && !isLocked ? (
          <Editor
            key={selectedNote.id}
            note={selectedNote}
            isDark={isDark}
            onSaveNote={handleSaveNote}
            onDeleteNote={handleDeleteNote}
            onShowSnackbar={showSnackbar}
          />
        ) : (
          <EmptyEditorState
            onCreateNote={handleCreateNote}
            onCreateStory={handleCreateStory}
          />
        )}
      </div>

      {/* Global Snackbar */}
      <Snackbar
        message={snackbar.message}
        actionLabel={snackbar.actionLabel}
        onAction={snackbar.onAction}
      />
    </>
  );
};
