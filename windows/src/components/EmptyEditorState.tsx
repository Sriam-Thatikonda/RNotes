import React from 'react';
import { Plus, BookOpen, FileText, Search, Lock, Undo2 } from 'lucide-react';

interface EmptyEditorStateProps {
  onCreateNote: () => void;
  onCreateStory: () => void;
}

export const EmptyEditorState: React.FC<EmptyEditorStateProps> = ({
  onCreateNote,
  onCreateStory,
}) => {
  return (
    <main className="editor-pane empty-editor-pane">
      <div className="empty-state-content">
        {/* Central Icon Badge */}
        <div className="empty-state-icon-wrapper">
          <FileText size={40} className="empty-state-primary-icon" />
        </div>

        <h2 className="empty-state-title">No Note Selected</h2>
        <p className="empty-state-subtitle">
          Choose a note from the sidebar to view and edit, or start something new.
        </p>

        {/* Primary Action Buttons */}
        <div className="empty-state-actions">
          <button className="btn btn-primary empty-action-btn" onClick={onCreateNote}>
            <Plus size={18} />
            <span>New Note</span>
            <span className="btn-shortcut-chip">Ctrl+N</span>
          </button>

          <button className="btn btn-secondary empty-action-btn" onClick={onCreateStory}>
            <BookOpen size={17} />
            <span>New Story</span>
            <span className="btn-shortcut-chip">Ctrl+Shift+N</span>
          </button>
        </div>

        {/* Keyboard Shortcuts Reference Card */}
        <div className="shortcuts-legend-card">
          <div className="shortcuts-legend-title">Keyboard Shortcuts</div>
          <div className="shortcuts-legend-grid">
            <div className="shortcut-legend-item">
              <span className="shortcut-key">Ctrl + N</span>
              <span className="shortcut-desc">New note</span>
            </div>
            <div className="shortcut-legend-item">
              <span className="shortcut-key">Ctrl + Shift + N</span>
              <span className="shortcut-desc">New story</span>
            </div>
            <div className="shortcut-legend-item">
              <span className="shortcut-key">Ctrl + F</span>
              <span className="shortcut-desc">Search notes</span>
            </div>
            <div className="shortcut-legend-item">
              <span className="shortcut-key">Ctrl + L</span>
              <span className="shortcut-desc">Lock vault</span>
            </div>
            <div className="shortcut-legend-item">
              <span className="shortcut-key">Ctrl + Z</span>
              <span className="shortcut-desc">Undo edit</span>
            </div>
            <div className="shortcut-legend-item">
              <span className="shortcut-key">Esc</span>
              <span className="shortcut-desc">Deselect note</span>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
};
