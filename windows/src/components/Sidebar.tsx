import React, { useState } from 'react';
import {
  Search,
  X,
  Plus,
  BookOpen,
  Sun,
  Moon,
  Pin,
  Download,
  Upload,
  Lock,
  Shield,
} from 'lucide-react';
import { Note } from '../models/note';
import { NoteCard } from './NoteCard';

export type NoteFilter = 'ALL' | 'STANDARD' | 'STORY';

interface SidebarProps {
  notes: Note[];
  selectedNoteId: string | null;
  searchQuery: string;
  selectedFilter: NoteFilter;
  isDark: boolean;
  onSearchChange: (query: string) => void;
  onFilterChange: (filter: NoteFilter) => void;
  onSelectNote: (id: string) => void;
  onCreateNote: () => void;
  onCreateStory: () => void;
  onToggleTheme: () => void;
  onExportNotes: () => void;
  onImportNotes: () => void;
  onLockApp: () => void;
  onOpenSecurity: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  notes,
  selectedNoteId,
  searchQuery,
  selectedFilter,
  isDark,
  onSearchChange,
  onFilterChange,
  onSelectNote,
  onCreateNote,
  onCreateStory,
  onToggleTheme,
  onExportNotes,
  onImportNotes,
  onLockApp,
  onOpenSecurity,
}) => {
  const filteredNotes = notes.filter((note) => {
    if (selectedFilter === 'STANDARD') return note.content.noteType !== 'STORY';
    if (selectedFilter === 'STORY') return note.content.noteType === 'STORY';
    return true;
  });

  const pinnedNotes = filteredNotes.filter((n) => n.isPinned);
  const unpinnedNotes = filteredNotes.filter((n) => !n.isPinned);

  return (
    <aside className="sidebar">
      {/* Header with Title & Action Controls */}
      <div className="sidebar-header">
        <div className="app-brand-row">
          <div className="app-title-group">
            <h1 className="app-title">R-Notes</h1>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <button
              className="action-icon-button"
              onClick={onLockApp}
              title="Lock Vault (Ctrl + L)"
            >
              <Lock size={17} />
            </button>
            <button
              className="action-icon-button"
              onClick={onOpenSecurity}
              title="Password & Security"
            >
              <Shield size={17} />
            </button>
            <button
              className="action-icon-button"
              onClick={onExportNotes}
              title="Backup / Export Notes (JSON)"
            >
              <Download size={17} />
            </button>
            <button
              className="action-icon-button"
              onClick={onImportNotes}
              title="Import Notes (JSON)"
            >
              <Upload size={17} />
            </button>
            <button
              className="action-icon-button"
              onClick={onToggleTheme}
              title={isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme'}
            >
              {isDark ? <Sun size={17} /> : <Moon size={17} />}
            </button>
          </div>
        </div>

        {/* Search Bar */}
        <div className="search-container">
          <Search size={16} className="search-icon" />
          <input
            type="text"
            className="search-input"
            placeholder="Search notes, dialogues & tasks..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
          />
          {searchQuery && (
            <button
              className="search-clear-btn"
              onClick={() => onSearchChange('')}
              title="Clear search"
            >
              <X size={14} />
            </button>
          )}
        </div>

        {/* Filter Chips */}
        <div className="filter-chips-row">
          <button
            className={`filter-chip ${selectedFilter === 'ALL' ? 'active' : ''}`}
            onClick={() => onFilterChange('ALL')}
          >
            All ({notes.length})
          </button>
          <button
            className={`filter-chip ${selectedFilter === 'STANDARD' ? 'active' : ''}`}
            onClick={() => onFilterChange('STANDARD')}
          >
            Notes ({notes.filter((n) => n.content.noteType !== 'STORY').length})
          </button>
          <button
            className={`filter-chip ${selectedFilter === 'STORY' ? 'active' : ''}`}
            onClick={() => onFilterChange('STORY')}
          >
            Stories ({notes.filter((n) => n.content.noteType === 'STORY').length})
          </button>
        </div>
      </div>

      {/* Scrollable List of Notes */}
      <div className="notes-scroll-list">
        {filteredNotes.length === 0 ? (
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '60px 20px',
              textAlign: 'center',
              color: 'var(--color-on-surface-variant)',
            }}
          >
            <p style={{ fontSize: '15px', fontWeight: 600, marginBottom: '6px' }}>
              {searchQuery ? 'No matching notes found' : 'No notes yet'}
            </p>
            <p style={{ fontSize: '13px', opacity: 0.7 }}>
              {searchQuery ? 'Try a different search term' : 'Click below to create your first note or story!'}
            </p>
          </div>
        ) : (
          <>
            {/* Pinned Notes Section */}
            {pinnedNotes.length > 0 && (
              <>
                <div className="list-section-header">
                  <Pin size={12} />
                  <span>Pinned</span>
                </div>
                {pinnedNotes.map((note) => (
                  <NoteCard
                    key={note.id}
                    note={note}
                    isSelected={note.id === selectedNoteId}
                    searchQuery={searchQuery}
                    isDark={isDark}
                    onClick={() => onSelectNote(note.id === selectedNoteId ? '' : note.id)}
                  />
                ))}
              </>
            )}

            {/* Other Notes Section */}
            {unpinnedNotes.length > 0 && (
              <>
                {pinnedNotes.length > 0 && (
                  <div className="list-section-header" style={{ marginTop: '8px' }}>
                    <span>Other Notes</span>
                  </div>
                )}
                {unpinnedNotes.map((note) => (
                  <NoteCard
                    key={note.id}
                    note={note}
                    isSelected={note.id === selectedNoteId}
                    searchQuery={searchQuery}
                    isDark={isDark}
                    onClick={() => onSelectNote(note.id === selectedNoteId ? '' : note.id)}
                  />
                ))}
              </>
            )}
          </>
        )}
      </div>

      {/* Bottom Fixed Action Buttons */}
      <div className="sidebar-footer">
        <button className="create-note-btn" onClick={onCreateNote}>
          <Plus size={18} />
          <span>New Note</span>
        </button>
        <button className="create-story-btn" onClick={onCreateStory}>
          <BookOpen size={17} />
          <span>Story</span>
        </button>
      </div>
    </aside>
  );
};
