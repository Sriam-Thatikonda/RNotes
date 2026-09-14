import React, { useRef, useEffect } from 'react';
import { Trash2, BookOpen } from 'lucide-react';
import { RichBlock, StoryCharacter } from '../models/note';
import { StoryViewMode } from './StoryCharacterBar';

interface StoryDialogueBlockItemProps {
  block: RichBlock;
  index: number;
  character?: StoryCharacter | null;
  characters: StoryCharacter[];
  storyViewMode: StoryViewMode;
  isFocused: boolean;
  onTextChanged: (text: string) => void;
  onEnter: (before: string, after: string) => void;
  onBackspaceOnEmpty: () => void;
  onDeleteBlock: () => void;
  onFocus: () => void;
}

export const StoryDialogueBlockItem: React.FC<StoryDialogueBlockItemProps> = ({
  block,
  index,
  character,
  characters,
  storyViewMode,
  isFocused,
  onTextChanged,
  onEnter,
  onBackspaceOnEmpty,
  onDeleteBlock,
  onFocus,
}) => {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-resize textarea height on text change and window resize
  useEffect(() => {
    const adjustHeight = () => {
      if (textareaRef.current) {
        textareaRef.current.style.height = 'auto';
        textareaRef.current.style.height = `${textareaRef.current.scrollHeight}px`;
      }
    };
    adjustHeight();
    window.addEventListener('resize', adjustHeight);
    return () => window.removeEventListener('resize', adjustHeight);
  }, [block.text]);

  useEffect(() => {
    if (isFocused && textareaRef.current) {
      textareaRef.current.focus();
    }
  }, [isFocused]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      const el = textareaRef.current;
      if (el) {
        const cursor = el.selectionStart;
        const text = block.text;
        const before = text.substring(0, cursor);
        const after = text.substring(cursor);
        onEnter(before, after);
      }
    } else if (e.key === 'Backspace' && block.text === '') {
      e.preventDefault();
      onBackspaceOnEmpty();
    }
  };

  const charColor = character?.colorHex || '#6366F1';
  const speakerName = character?.name || 'Speaker';

  // 1. Scene / Narrator Beat Block
  if (block.type === 'NARRATOR') {
    return (
      <div className="scene-beat-card" onClick={onFocus}>
        <div className="scene-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <BookOpen size={14} />
            <span>SCENE / NARRATOR</span>
          </div>
          {isFocused && (
            <button
              type="button"
              className="dialogue-trash-btn"
              onClick={(e) => {
                e.stopPropagation();
                onDeleteBlock();
              }}
              title="Delete Scene Beat"
            >
              <Trash2 size={15} />
            </button>
          )}
        </div>
        <textarea
          ref={textareaRef}
          className="scene-text-input"
          rows={1}
          value={block.text}
          onChange={(e) => onTextChanged(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={onFocus}
          placeholder="Describe the environment, action, or scene..."
        />
      </div>
    );
  }

  // 2. Script / Screenplay View Mode
  if (storyViewMode === 'SCRIPT') {
    return (
      <div className="script-dialogue-block" onClick={onFocus}>
        <div className="script-speaker-header">
          <span className="script-speaker-name" style={{ color: charColor }}>
            {speakerName.toUpperCase()}
          </span>
          {block.parenthetical && (
            <span className="parenthetical-tag">({block.parenthetical})</span>
          )}
          {isFocused && (
            <button
              type="button"
              className="dialogue-trash-btn"
              onClick={(e) => {
                e.stopPropagation();
                onDeleteBlock();
              }}
              title="Delete Message"
            >
              <Trash2 size={14} />
            </button>
          )}
        </div>
        <textarea
          ref={textareaRef}
          className="script-text-input"
          rows={1}
          value={block.text}
          onChange={(e) => onTextChanged(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={onFocus}
          placeholder="Dialogue..."
        />
      </div>
    );
  }

  // 3. Chat Bubble View Mode (Left / Right aligned)
  const isRightAligned = characters.length >= 2 && character?.id === characters[1]?.id;

  return (
    <div
      className={`dialogue-bubble-row ${isRightAligned ? 'right-aligned' : ''}`}
      onClick={onFocus}
    >
      {/* Left Avatar (if left-aligned) */}
      {!isRightAligned && (
        <div
          className="dialogue-avatar"
          style={{
            borderColor: charColor,
            color: charColor,
            backgroundColor: `${charColor}20`,
          }}
          title={speakerName}
        >
          {character?.avatarEmoji || '👤'}
        </div>
      )}

      {/* Bubble Card */}
      <div
        className={`dialogue-bubble-card ${isRightAligned ? 'right' : 'left'}`}
        style={{
          backgroundColor: `${charColor}14`,
          borderColor: `${charColor}35`,
        }}
      >
        <div className="dialogue-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span className="speaker-name" style={{ color: charColor }}>
              {speakerName}
            </span>
            {block.parenthetical && (
              <span className="parenthetical-tag">({block.parenthetical})</span>
            )}
          </div>

          {isFocused && (
            <button
              type="button"
              className="dialogue-trash-btn"
              onClick={(e) => {
                e.stopPropagation();
                onDeleteBlock();
              }}
              title="Delete Message"
            >
              <Trash2 size={15} />
            </button>
          )}
        </div>

        <textarea
          ref={textareaRef}
          className="block-text-input"
          rows={1}
          value={block.text}
          onChange={(e) => onTextChanged(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={onFocus}
          placeholder="Type message..."
        />
      </div>

      {/* Right Avatar (if right-aligned) */}
      {isRightAligned && (
        <div
          className="dialogue-avatar"
          style={{
            borderColor: charColor,
            color: charColor,
            backgroundColor: `${charColor}20`,
          }}
          title={speakerName}
        >
          {character?.avatarEmoji || '👤'}
        </div>
      )}
    </div>
  );
};
