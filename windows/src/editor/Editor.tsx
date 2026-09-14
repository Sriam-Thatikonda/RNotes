import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Undo2,
  Redo2,
  BookOpen,
  Pin,
  Trash2,
  Palette,
  Bold,
  Italic,
  Underline,
  Strikethrough,
  CheckSquare,
  List,
  ListOrdered,
} from 'lucide-react';
import {
  Note,
  NoteColor,
  NOTE_COLORS,
  BlockType,
  RichBlock,
  StoryCharacter,
  generateId,
} from '../models/note';
import { RichBlockItem } from './RichBlockItem';
import { StoryCharacterBar, StoryViewMode } from './StoryCharacterBar';
import { StoryDialogueBlockItem } from './StoryDialogueBlockItem';
import { ConfirmationDialog } from '../components/ConfirmationDialog';
import { CharacterModal } from '../components/CharacterModal';

interface EditorProps {
  note: Note;
  isDark: boolean;
  onSaveNote: (note: Note) => void;
  onDeleteNote: (id: string) => void;
  onShowSnackbar: (message: string, actionLabel?: string, onAction?: () => void) => void;
}

interface EditorSnapshot {
  title: string;
  content: Note['content'];
  color: NoteColor;
  isPinned: boolean;
}

export const Editor: React.FC<EditorProps> = ({
  note,
  isDark,
  onSaveNote,
  onDeleteNote,
  onShowSnackbar,
}) => {
  const [title, setTitle] = useState(note.title);
  const [content, setContent] = useState(note.content);
  const [color, setColor] = useState(note.color);
  const [isPinned, setIsPinned] = useState(note.isPinned);

  const [activeBlockIndex, setActiveBlockIndex] = useState<number>(0);
  const [storyViewMode, setStoryViewMode] = useState<StoryViewMode>('CHAT');
  const [activeCharacterId, setActiveCharacterId] = useState<string | null>(
    note.content.characters[0]?.id || null
  );

  const [showColorDropdown, setShowColorDropdown] = useState(false);
  const [showDeleteNoteDialog, setShowDeleteNoteDialog] = useState(false);
  const [blockIndexToDelete, setBlockIndexToDelete] = useState<number | null>(null);

  const [showCharacterModal, setShowCharacterModal] = useState(false);
  const [characterToEdit, setCharacterToEdit] = useState<StoryCharacter | null>(null);

  // Undo / Redo stacks
  const undoStackRef = useRef<EditorSnapshot[]>([]);
  const redoStackRef = useRef<EditorSnapshot[]>([]);
  const [canUndo, setCanUndo] = useState(false);
  const [canRedo, setCanRedo] = useState(false);

  // Sync state when incoming note changes
  useEffect(() => {
    setTitle(note.title);
    setContent(note.content);
    setColor(note.color);
    setIsPinned(note.isPinned);
    setActiveBlockIndex(0);
    setActiveCharacterId(note.content.characters[0]?.id || null);

    // Reset undo/redo on note change
    undoStackRef.current = [
      {
        title: note.title,
        content: JSON.parse(JSON.stringify(note.content)),
        color: note.color,
        isPinned: note.isPinned,
      },
    ];
    redoStackRef.current = [];
    setCanUndo(false);
    setCanRedo(false);
  }, [note.id]);

  // Push to Undo Stack
  const recordSnapshot = useCallback(
    (newTitle: string, newContent: Note['content'], newColor: NoteColor, newPinned: boolean) => {
      const snapshot: EditorSnapshot = {
        title: newTitle,
        content: JSON.parse(JSON.stringify(newContent)),
        color: newColor,
        isPinned: newPinned,
      };

      const last = undoStackRef.current[undoStackRef.current.length - 1];
      if (
        !last ||
        last.title !== snapshot.title ||
        JSON.stringify(last.content) !== JSON.stringify(snapshot.content) ||
        last.color !== snapshot.color ||
        last.isPinned !== snapshot.isPinned
      ) {
        undoStackRef.current.push(snapshot);
        redoStackRef.current = [];
        setCanUndo(undoStackRef.current.length > 1);
        setCanRedo(false);
      }
    },
    []
  );

  // Debounced Auto-Save
  const saveTimeoutRef = useRef<number | null>(null);
  const currentNoteRef = useRef({ title, content, color, isPinned });
  currentNoteRef.current = { title, content, color, isPinned };

  const triggerAutoSave = useCallback(() => {
    if (saveTimeoutRef.current) {
      window.clearTimeout(saveTimeoutRef.current);
    }
    saveTimeoutRef.current = window.setTimeout(() => {
      const { title, content, color, isPinned } = currentNoteRef.current;
      onSaveNote({
        ...note,
        title,
        content,
        color,
        isPinned,
        updatedAt: Date.now(),
      });
    }, 500);
  }, [note, onSaveNote]);

  // Flush on unmount
  useEffect(() => {
    return () => {
      if (saveTimeoutRef.current) {
        window.clearTimeout(saveTimeoutRef.current);
      }
      const { title, content, color, isPinned } = currentNoteRef.current;
      onSaveNote({
        ...note,
        title,
        content,
        color,
        isPinned,
        updatedAt: Date.now(),
      });
    };
  }, [note, onSaveNote]);

  // Undo implementation
  const handleUndo = useCallback(() => {
    if (undoStackRef.current.length > 1) {
      const current = undoStackRef.current.pop()!;
      redoStackRef.current.push(current);
      const prev = undoStackRef.current[undoStackRef.current.length - 1];

      setTitle(prev.title);
      setContent(JSON.parse(JSON.stringify(prev.content)));
      setColor(prev.color);
      setIsPinned(prev.isPinned);

      setCanUndo(undoStackRef.current.length > 1);
      setCanRedo(redoStackRef.current.length > 0);

      onSaveNote({
        ...note,
        title: prev.title,
        content: prev.content,
        color: prev.color,
        isPinned: prev.isPinned,
        updatedAt: Date.now(),
      });
    }
  }, [note, onSaveNote]);

  // Redo implementation
  const handleRedo = useCallback(() => {
    if (redoStackRef.current.length > 0) {
      const next = redoStackRef.current.pop()!;
      undoStackRef.current.push(next);

      setTitle(next.title);
      setContent(JSON.parse(JSON.stringify(next.content)));
      setColor(next.color);
      setIsPinned(next.isPinned);

      setCanUndo(true);
      setCanRedo(redoStackRef.current.length > 0);

      onSaveNote({
        ...note,
        title: next.title,
        content: next.content,
        color: next.color,
        isPinned: next.isPinned,
        updatedAt: Date.now(),
      });
    }
  }, [note, onSaveNote]);

  // Keyboard shortcuts listener
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey || e.metaKey) {
        if (e.key === 'z' && !e.shiftKey) {
          e.preventDefault();
          handleUndo();
        } else if ((e.key === 'z' && e.shiftKey) || e.key === 'y') {
          e.preventDefault();
          handleRedo();
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleUndo, handleRedo]);

  // Title change
  const handleTitleChange = (newTitle: string) => {
    setTitle(newTitle);
    recordSnapshot(newTitle, content, color, isPinned);
    triggerAutoSave();
  };

  // Block Text Change
  const handleBlockTextChange = (index: number, text: string) => {
    const newBlocks = [...content.blocks];
    newBlocks[index] = { ...newBlocks[index], text };
    const newContent = { ...content, blocks: newBlocks };
    setContent(newContent);
    recordSnapshot(title, newContent, color, isPinned);
    triggerAutoSave();
  };

  // Toggle Checklist item
  const handleToggleChecklist = (index: number) => {
    const newBlocks = [...content.blocks];
    newBlocks[index] = {
      ...newBlocks[index],
      isChecked: !newBlocks[index].isChecked,
    };
    const newContent = { ...content, blocks: newBlocks };
    setContent(newContent);
    recordSnapshot(title, newContent, color, isPinned);
    triggerAutoSave();
  };

  // Split block on Enter key
  const handleEnter = (index: number, before: string, after: string) => {
    const currentBlock = content.blocks[index];
    const newBlocks = [...content.blocks];

    // Current block keeps "before"
    newBlocks[index] = { ...currentBlock, text: before };

    let newBlockType: BlockType = 'PARAGRAPH';
    let newCharacterId: string | null = null;

    if (content.noteType === 'STORY') {
      newBlockType = 'DIALOGUE';
      // Auto-alternate speaker if 2 characters exist
      if (content.characters.length === 2 && currentBlock.type === 'DIALOGUE') {
        const other = content.characters.find((c) => c.id !== currentBlock.characterId);
        newCharacterId = other ? other.id : (currentBlock.characterId ?? null);
      } else {
        newCharacterId = activeCharacterId || content.characters[0]?.id || null;
      }
      setActiveCharacterId(newCharacterId);
    } else {
      // Inherit list types
      if (['CHECKLIST', 'BULLET', 'NUMBERED'].includes(currentBlock.type)) {
        newBlockType = currentBlock.type;
      }
    }

    const newBlock: RichBlock = {
      id: generateId(),
      type: newBlockType,
      text: after,
      isChecked: false,
      characterId: newCharacterId,
      spans: [],
    };

    newBlocks.splice(index + 1, 0, newBlock);
    const newContent = { ...content, blocks: newBlocks };
    setContent(newContent);
    setActiveBlockIndex(index + 1);
    recordSnapshot(title, newContent, color, isPinned);
    triggerAutoSave();
  };

  // Backspace on empty block
  const handleBackspaceOnEmpty = (index: number) => {
    const currentBlock = content.blocks[index];
    if (currentBlock.type !== 'PARAGRAPH' && content.noteType !== 'STORY') {
      // Revert list item to paragraph
      const newBlocks = [...content.blocks];
      newBlocks[index] = { ...currentBlock, type: 'PARAGRAPH' };
      const newContent = { ...content, blocks: newBlocks };
      setContent(newContent);
      recordSnapshot(title, newContent, color, isPinned);
      triggerAutoSave();
    } else if (content.blocks.length > 1) {
      // Remove block
      const newBlocks = [...content.blocks];
      newBlocks.splice(index, 1);
      const newContent = { ...content, blocks: newBlocks };
      setContent(newContent);
      setActiveBlockIndex(Math.max(0, index - 1));
      recordSnapshot(title, newContent, color, isPinned);
      triggerAutoSave();
    }
  };

  // Delete block action
  const confirmDeleteBlock = (index: number) => {
    const newBlocks = [...content.blocks];
    const deletedBlock = newBlocks[index];

    if (newBlocks.length > 1) {
      newBlocks.splice(index, 1);
    } else {
      newBlocks[0] = { ...newBlocks[0], text: '', parenthetical: null };
    }

    const newContent = { ...content, blocks: newBlocks };
    setContent(newContent);
    setActiveBlockIndex(Math.max(0, index - 1));
    recordSnapshot(title, newContent, color, isPinned);
    triggerAutoSave();

    // Show Undo Snackbar
    const isScene = deletedBlock.type === 'NARRATOR';
    onShowSnackbar(
      isScene ? 'Scene beat deleted' : 'Message deleted',
      'UNDO',
      () => handleUndo()
    );
  };

  // Switch Block Type (e.g. from bottom bar)
  const setBlockType = (type: BlockType) => {
    const newBlocks = [...content.blocks];
    const current = newBlocks[activeBlockIndex] || newBlocks[0];
    newBlocks[activeBlockIndex] = {
      ...current,
      type: current.type === type ? 'PARAGRAPH' : type,
    };
    const newContent = { ...content, blocks: newBlocks };
    setContent(newContent);
    recordSnapshot(title, newContent, color, isPinned);
    triggerAutoSave();
  };

  // Switch Note Color
  const handleSelectColor = (newColor: NoteColor) => {
    setColor(newColor);
    setShowColorDropdown(false);
    recordSnapshot(title, content, newColor, isPinned);
    triggerAutoSave();
  };

  // Toggle Note Pin
  const handleTogglePin = () => {
    const newPinned = !isPinned;
    setIsPinned(newPinned);
    recordSnapshot(title, content, color, newPinned);
    triggerAutoSave();
  };

  // Toggle Note Type (Standard vs Story)
  const handleToggleNoteType = () => {
    const nextType = content.noteType === 'STORY' ? 'STANDARD' : 'STORY';
    const newContent: Note['content'] = {
      ...content,
      noteType: nextType,
      characters:
        nextType === 'STORY' && content.characters.length === 0
          ? [
              {
                id: generateId(),
                name: 'Protagonist',
                avatarEmoji: '🧙',
                colorHex: '#6366F1',
                role: 'Lead',
              },
              {
                id: generateId(),
                name: 'Companion',
                avatarEmoji: '🗡️',
                colorHex: '#10B981',
                role: 'Ally',
              },
            ]
          : content.characters,
    };
    setContent(newContent);
    recordSnapshot(title, newContent, color, isPinned);
    triggerAutoSave();
  };

  // Character Bar: Select speaker
  const handleSelectSpeaker = (charId: string) => {
    setActiveCharacterId(charId);
    const newBlocks = [...content.blocks];
    const current = newBlocks[activeBlockIndex];
    if (current) {
      newBlocks[activeBlockIndex] = {
        ...current,
        type: 'DIALOGUE',
        characterId: charId,
      };
      const newContent = { ...content, blocks: newBlocks };
      setContent(newContent);
      recordSnapshot(title, newContent, color, isPinned);
      triggerAutoSave();
    }
  };

  // Character Bar: Toggle Scene beat
  const handleToggleSceneBeat = () => {
    const newBlocks = [...content.blocks];
    const current = newBlocks[activeBlockIndex];
    if (current) {
      const nextType: BlockType = current.type === 'NARRATOR' ? 'DIALOGUE' : 'NARRATOR';
      newBlocks[activeBlockIndex] = {
        ...current,
        type: nextType,
        characterId: nextType === 'DIALOGUE' ? activeCharacterId : null,
      };
      const newContent = { ...content, blocks: newBlocks };
      setContent(newContent);
      recordSnapshot(title, newContent, color, isPinned);
      triggerAutoSave();
    }
  };

  // Character Management (Add/Edit)
  const handleSaveCharacter = (char: StoryCharacter) => {
    const exists = content.characters.some((c) => c.id === char.id);
    let newChars: StoryCharacter[];
    if (exists) {
      newChars = content.characters.map((c) => (c.id === char.id ? char : c));
    } else {
      newChars = [...content.characters, char];
    }
    const newContent = { ...content, characters: newChars };
    setContent(newContent);
    if (!activeCharacterId) setActiveCharacterId(char.id);
    recordSnapshot(title, newContent, color, isPinned);
    triggerAutoSave();
  };

  const handleDeleteCharacter = (charId: string) => {
    const newChars = content.characters.filter((c) => c.id !== charId);
    const newContent = { ...content, characters: newChars };
    setContent(newContent);
    if (activeCharacterId === charId) {
      setActiveCharacterId(newChars[0]?.id || null);
    }
    recordSnapshot(title, newContent, color, isPinned);
    triggerAutoSave();
  };

  const activeBlock = content.blocks[activeBlockIndex] || content.blocks[0];
  const colorDef = NOTE_COLORS[color] || NOTE_COLORS.DEFAULT;
  const editorBg = isDark ? colorDef.darkContainer : colorDef.lightContainer;

  const targetBlockToDelete = blockIndexToDelete !== null ? content.blocks[blockIndexToDelete] : null;
  const isTargetScene = targetBlockToDelete?.type === 'NARRATOR';
  const targetSpeaker = content.characters.find((c) => c.id === targetBlockToDelete?.characterId)?.name || 'Speaker';

  return (
    <main className="editor-pane" style={{ backgroundColor: editorBg }}>
      {/* Top Header Bar */}
      <header className="editor-header">
        <div className="editor-header-left">
          {content.noteType === 'STORY' && (
            <div className="story-mode-indicator">
              <BookOpen size={14} />
              <span>Storymode</span>
            </div>
          )}
        </div>

        <div className="editor-header-actions">
          {/* Undo */}
          <button
            type="button"
            className="action-icon-button"
            onClick={handleUndo}
            disabled={!canUndo}
            title="Undo (Ctrl+Z)"
          >
            <Undo2 size={18} />
          </button>

          {/* Redo */}
          <button
            type="button"
            className="action-icon-button"
            onClick={handleRedo}
            disabled={!canRedo}
            title="Redo (Ctrl+Y)"
          >
            <Redo2 size={18} />
          </button>

          {/* Story Mode Toggle */}
          <button
            type="button"
            className="action-icon-button"
            onClick={handleToggleNoteType}
            title={content.noteType === 'STORY' ? 'Switch to Standard Note' : 'Convert to Storymode'}
            style={{ color: content.noteType === 'STORY' ? '#6366F1' : 'inherit' }}
          >
            <BookOpen size={18} />
          </button>

          {/* Note Color Dropdown */}
          <div style={{ position: 'relative' }}>
            <button
              type="button"
              className="action-icon-button"
              onClick={() => setShowColorDropdown(!showColorDropdown)}
              title="Change note color"
            >
              <div
                style={{
                  width: '18px',
                  height: '18px',
                  borderRadius: '50%',
                  backgroundColor: colorDef.dotColor,
                  border: '1.5px solid var(--color-outline)',
                }}
              />
            </button>

            {showColorDropdown && (
              <div
                style={{
                  position: 'absolute',
                  top: '40px',
                  right: 0,
                  backgroundColor: 'var(--color-surface)',
                  border: '1px solid var(--color-outline)',
                  borderRadius: '12px',
                  padding: '8px',
                  boxShadow: 'var(--shadow-md)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '4px',
                  zIndex: 50,
                  width: '160px',
                }}
              >
                {Object.values(NOTE_COLORS).map((c) => (
                  <button
                    key={c.key}
                    type="button"
                    onClick={() => handleSelectColor(c.key)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '10px',
                      padding: '6px 8px',
                      borderRadius: '6px',
                      border: 'none',
                      background: color === c.key ? 'var(--color-surface-variant)' : 'transparent',
                      cursor: 'pointer',
                      fontSize: '13px',
                      color: 'var(--color-on-surface)',
                      textAlign: 'left',
                    }}
                  >
                    <div
                      style={{
                        width: '14px',
                        height: '14px',
                        borderRadius: '50%',
                        backgroundColor: c.dotColor,
                      }}
                    />
                    <span>{c.displayName}</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Pin Toggle */}
          <button
            type="button"
            className="action-icon-button"
            onClick={handleTogglePin}
            title={isPinned ? 'Unpin note' : 'Pin note to top'}
            style={{ color: isPinned ? 'var(--color-primary)' : 'inherit' }}
          >
            <Pin size={18} fill={isPinned ? 'currentColor' : 'none'} />
          </button>

          {/* Delete Note */}
          <button
            type="button"
            className="action-icon-button"
            onClick={() => setShowDeleteNoteDialog(true)}
            title="Delete note"
          >
            <Trash2 size={18} />
          </button>
        </div>
      </header>

      {/* Editor Content Area */}
      <div className="editor-content-scroll">
        {/* Title Field */}
        <input
          type="text"
          className="editor-title-input"
          value={title}
          onChange={(e) => handleTitleChange(e.target.value)}
          placeholder="Title"
        />

        {/* Content Blocks */}
        <div className="blocks-container">
          {content.blocks.map((block, idx) => {
            if (content.noteType === 'STORY') {
              const char = content.characters.find((c) => c.id === block.characterId) || null;
              return (
                <StoryDialogueBlockItem
                  key={block.id}
                  block={block}
                  index={idx}
                  character={char}
                  characters={content.characters}
                  storyViewMode={storyViewMode}
                  isFocused={activeBlockIndex === idx}
                  onTextChanged={(text) => handleBlockTextChange(idx, text)}
                  onEnter={(before, after) => handleEnter(idx, before, after)}
                  onBackspaceOnEmpty={() => handleBackspaceOnEmpty(idx)}
                  onDeleteBlock={() => setBlockIndexToDelete(idx)}
                  onFocus={() => setActiveBlockIndex(idx)}
                />
              );
            }

            return (
              <RichBlockItem
                key={block.id}
                block={block}
                index={idx}
                isFocused={activeBlockIndex === idx}
                onTextChanged={(text) => handleBlockTextChange(idx, text)}
                onToggleChecklist={() => handleToggleChecklist(idx)}
                onEnter={(before, after) => handleEnter(idx, before, after)}
                onBackspaceOnEmpty={() => handleBackspaceOnEmpty(idx)}
                onFocus={() => setActiveBlockIndex(idx)}
              />
            );
          })}
        </div>
      </div>

      {/* Docked Bottom Toolbar */}
      <div className="editor-bottom-bar">
        {content.noteType === 'STORY' ? (
          <StoryCharacterBar
            characters={content.characters}
            activeCharacterId={activeCharacterId}
            isSceneActive={activeBlock.type === 'NARRATOR'}
            storyViewMode={storyViewMode}
            onSelectCharacter={handleSelectSpeaker}
            onEditCharacter={(char) => {
              setCharacterToEdit(char);
              setShowCharacterModal(true);
            }}
            onAddCharacter={() => {
              setCharacterToEdit(null);
              setShowCharacterModal(true);
            }}
            onToggleScene={handleToggleSceneBeat}
            onToggleViewMode={() =>
              setStoryViewMode(storyViewMode === 'CHAT' ? 'SCRIPT' : 'CHAT')
            }
          />
        ) : (
          <div className="formatting-toolbar-row">
            <button
              type="button"
              className={`toolbar-btn ${activeBlock.type === 'CHECKLIST' ? 'active' : ''}`}
              onClick={() => setBlockType('CHECKLIST')}
              title="Checklist"
            >
              <CheckSquare size={17} />
            </button>
            <button
              type="button"
              className={`toolbar-btn ${activeBlock.type === 'BULLET' ? 'active' : ''}`}
              onClick={() => setBlockType('BULLET')}
              title="Bullet List"
            >
              <List size={17} />
            </button>
            <button
              type="button"
              className={`toolbar-btn ${activeBlock.type === 'NUMBERED' ? 'active' : ''}`}
              onClick={() => setBlockType('NUMBERED')}
              title="Numbered List"
            >
              <ListOrdered size={17} />
            </button>

            <div className="toolbar-divider" />

            <button
              type="button"
              className="toolbar-btn"
              onClick={handleUndo}
              disabled={!canUndo}
              title="Undo"
            >
              <Undo2 size={17} />
            </button>
            <button
              type="button"
              className="toolbar-btn"
              onClick={handleRedo}
              disabled={!canRedo}
              title="Redo"
            >
              <Redo2 size={17} />
            </button>
          </div>
        )}
      </div>

      {/* Delete Note Confirmation Dialog */}
      <ConfirmationDialog
        isOpen={showDeleteNoteDialog}
        title="Delete Note?"
        message="Are you sure you want to delete this note? This will remove it permanently."
        confirmText="Delete"
        onConfirm={() => {
          setShowDeleteNoteDialog(false);
          onDeleteNote(note.id);
        }}
        onCancel={() => setShowDeleteNoteDialog(false)}
      />

      {/* Delete Block Confirmation Dialog */}
      <ConfirmationDialog
        isOpen={blockIndexToDelete !== null}
        title={isTargetScene ? 'Delete Scene Beat?' : 'Delete Message?'}
        message={
          targetBlockToDelete?.text?.trim()
            ? `Are you sure you want to delete this line by ${
                isTargetScene ? 'Scene' : targetSpeaker
              }?\n\n"${
                targetBlockToDelete.text.length > 80
                  ? targetBlockToDelete.text.substring(0, 80) + '...'
                  : targetBlockToDelete.text
              }"`
            : `Are you sure you want to delete this empty ${
                isTargetScene ? 'scene beat' : 'message'
              }?`
        }
        confirmText="Delete"
        onConfirm={() => {
          if (blockIndexToDelete !== null) {
            confirmDeleteBlock(blockIndexToDelete);
            setBlockIndexToDelete(null);
          }
        }}
        onCancel={() => setBlockIndexToDelete(null)}
      />

      {/* Character Add / Edit Modal */}
      <CharacterModal
        isOpen={showCharacterModal}
        characterToEdit={characterToEdit}
        onSave={handleSaveCharacter}
        onDelete={handleDeleteCharacter}
        onClose={() => setShowCharacterModal(false)}
      />
    </main>
  );
};
