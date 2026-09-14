import React from 'react';
import { Pin, CheckSquare, MessageSquare } from 'lucide-react';
import { Note, NOTE_COLORS, richContentToPlainText } from '../models/note';

interface NoteCardProps {
  note: Note;
  isSelected: boolean;
  searchQuery: string;
  isDark: boolean;
  onClick: () => void;
}

export const NoteCard: React.FC<NoteCardProps> = ({
  note,
  isSelected,
  searchQuery,
  isDark,
  onClick,
}) => {
  const colorDef = NOTE_COLORS[note.color] || NOTE_COLORS.DEFAULT;
  const containerColor = isDark ? colorDef.darkContainer : colorDef.lightContainer;
  const borderColor = isDark ? colorDef.darkBorder : colorDef.lightBorder;

  const isStoryNote = note.content.noteType === 'STORY';
  const checklists = note.content.blocks.filter((b) => b.type === 'CHECKLIST');
  const checkedCount = checklists.filter((b) => b.isChecked).length;
  const dialogueCount = note.content.blocks.filter((b) => b.type === 'DIALOGUE').length;

  const plainText = richContentToPlainText(note.content);
  const displayTitle = note.title.trim() || 'Untitled Note';

  // Relative Date Formatting
  const formatTime = (ts: number): string => {
    const now = Date.now();
    const diffMs = now - ts;
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);

    if (diffMin < 1) return 'Just now';
    if (diffMin < 60) return `${diffMin}m ago`;
    if (diffHour < 24) return `${diffHour}h ago`;
    if (diffDay === 1) return 'Yesterday';
    if (diffDay < 7) return `${diffDay}d ago`;

    const d = new Date(ts);
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  };

  const highlightText = (text: string, query: string): React.ReactNode => {
    if (!query.trim()) return text;
    const parts = text.split(new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi'));
    return parts.map((part, i) =>
      part.toLowerCase() === query.toLowerCase() ? (
        <mark
          key={i}
          style={{
            backgroundColor: 'rgba(245, 158, 11, 0.35)',
            color: 'inherit',
            borderRadius: '2px',
            padding: '0 2px',
          }}
        >
          {part}
        </mark>
      ) : (
        part
      )
    );
  };

  return (
    <div
      className={`note-card ${isSelected ? 'selected' : ''}`}
      onClick={onClick}
      style={{
        backgroundColor: containerColor,
        borderColor: isSelected ? 'var(--color-primary)' : borderColor,
      }}
    >
      {/* Top Row: Dot & Badges */}
      <div className="card-top-row">
        {note.color !== 'DEFAULT' ? (
          <div className="card-dot" style={{ backgroundColor: colorDef.dotColor }} />
        ) : (
          <div style={{ width: '8px' }} />
        )}

        <div className="card-badges">
          {isStoryNote && <span className="story-pill">Story</span>}
          {note.isPinned && <Pin size={14} className="pin-icon" fill="currentColor" />}
        </div>
      </div>

      {/* Note Heading / Title */}
      <h4 className="card-title">
        {highlightText(displayTitle, searchQuery)}
      </h4>

      {/* Plain text preview if not empty */}
      {plainText.trim() && (
        <p className="card-preview">
          {highlightText(plainText.slice(0, 120), searchQuery)}
        </p>
      )}

      {/* Story Characters Avatars Row */}
      {isStoryNote && note.content.characters.length > 0 && (
        <div className="card-characters-row">
          {note.content.characters.slice(0, 4).map((char) => (
            <div
              key={char.id}
              className="character-avatar-bubble"
              style={{
                color: char.colorHex,
                backgroundColor: `${char.colorHex}20`,
                borderColor: char.colorHex,
              }}
              title={char.name}
            >
              {char.avatarEmoji}
            </div>
          ))}
          {note.content.characters.length > 4 && (
            <span style={{ fontSize: '11px', color: 'var(--color-on-surface-variant)' }}>
              +{note.content.characters.length - 4}
            </span>
          )}
        </div>
      )}

      {/* Bottom Metadata: Stat Badge & Relative Time */}
      <div className="card-bottom-row">
        <div>
          {isStoryNote && dialogueCount > 0 ? (
            <div className="card-stat-pill">
              <MessageSquare size={12} />
              <span>{dialogueCount} lines</span>
            </div>
          ) : checklists.length > 0 ? (
            <div className="card-stat-pill">
              <CheckSquare size={12} />
              <span>{checkedCount}/{checklists.length}</span>
            </div>
          ) : null}
        </div>

        <span>{formatTime(note.updatedAt)}</span>
      </div>
    </div>
  );
};
