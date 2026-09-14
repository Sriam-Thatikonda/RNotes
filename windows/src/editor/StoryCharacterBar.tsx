import React from 'react';
import { MessageSquare, FileText, BookOpen, Plus } from 'lucide-react';
import { StoryCharacter } from '../models/note';

export type StoryViewMode = 'CHAT' | 'SCRIPT';

interface StoryCharacterBarProps {
  characters: StoryCharacter[];
  activeCharacterId: string | null;
  isSceneActive: boolean;
  storyViewMode: StoryViewMode;
  onSelectCharacter: (id: string) => void;
  onEditCharacter: (char: StoryCharacter) => void;
  onAddCharacter: () => void;
  onToggleScene: () => void;
  onToggleViewMode: () => void;
}

export const StoryCharacterBar: React.FC<StoryCharacterBarProps> = ({
  characters,
  activeCharacterId,
  isSceneActive,
  storyViewMode,
  onSelectCharacter,
  onEditCharacter,
  onAddCharacter,
  onToggleScene,
  onToggleViewMode,
}) => {
  return (
    <div className="story-character-bar">
      {/* View Mode Switcher Button (Chat vs Script) */}
      <button
        type="button"
        className="toolbar-btn"
        onClick={onToggleViewMode}
        title={storyViewMode === 'CHAT' ? 'Switch to Script View' : 'Switch to Chat View'}
        style={{ color: 'var(--color-primary)' }}
      >
        {storyViewMode === 'CHAT' ? <FileText size={18} /> : <MessageSquare size={18} />}
      </button>

      <div className="toolbar-divider" />

      {/* Character Chips */}
      {characters.map((char) => {
        const isActive = !isSceneActive && char.id === activeCharacterId;
        return (
          <div
            key={char.id}
            className={`character-chip ${isActive ? 'active' : ''}`}
            onClick={() => onSelectCharacter(char.id)}
            onContextMenu={(e) => {
              e.preventDefault();
              onEditCharacter(char);
            }}
            title={`${char.name} (Right-click to edit)`}
            style={{
              borderColor: isActive ? char.colorHex : 'var(--color-outline)',
              color: isActive ? char.colorHex : 'var(--color-on-surface)',
              backgroundColor: isActive ? `${char.colorHex}20` : 'transparent',
            }}
          >
            <span style={{ fontSize: '15px' }}>{char.avatarEmoji}</span>
            <span>{char.name}</span>
          </div>
        );
      })}

      {/* Scene / Narrator Beat Chip */}
      <button
        type="button"
        className={`character-chip ${isSceneActive ? 'active' : ''}`}
        onClick={onToggleScene}
        style={{
          borderColor: isSceneActive ? '#6366F1' : 'var(--color-outline)',
          color: isSceneActive ? '#6366F1' : 'var(--color-on-surface-variant)',
          backgroundColor: isSceneActive ? 'rgba(99, 102, 241, 0.2)' : 'transparent',
        }}
        title="Toggle Scene Description / Narrator Beat"
      >
        <BookOpen size={14} />
        <span>Scene</span>
      </button>

      {/* Add Character Button */}
      <button
        type="button"
        className="character-chip"
        onClick={onAddCharacter}
        style={{
          borderStyle: 'dashed',
          color: 'var(--color-primary)',
        }}
      >
        <Plus size={14} />
        <span>Character</span>
      </button>
    </div>
  );
};
