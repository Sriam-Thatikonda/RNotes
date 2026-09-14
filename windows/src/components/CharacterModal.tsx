import React, { useState } from 'react';
import { StoryCharacter, generateId } from '../models/note';

interface CharacterModalProps {
  isOpen: boolean;
  characterToEdit?: StoryCharacter | null;
  onSave: (character: StoryCharacter) => void;
  onDelete?: (id: string) => void;
  onClose: () => void;
}

const PRESET_EMOJIS = [
  '🧙', '🤖', '🕵️', '👩‍🚀', '👑', '🦊', '⚔️', '🎭',
  '💼', '🐱', '🧛', '🧑‍🔬', '🐉', '🚀', '🎸', '🎯'
];

const PRESET_COLORS = [
  '#6366F1', '#3B82F6', '#10B981', '#F59E0B',
  '#EF4444', '#8B5CF6', '#EC4899', '#14B8A6'
];

export const CharacterModal: React.FC<CharacterModalProps> = ({
  isOpen,
  characterToEdit,
  onSave,
  onDelete,
  onClose,
}) => {
  if (!isOpen) return null;

  const [name, setName] = useState(characterToEdit?.name || '');
  const [emoji, setEmoji] = useState(characterToEdit?.avatarEmoji || '👤');
  const [color, setColor] = useState(characterToEdit?.colorHex || '#6366F1');
  const [role, setRole] = useState(characterToEdit?.role || '');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    onSave({
      id: characterToEdit?.id || generateId(),
      name: name.trim(),
      avatarEmoji: emoji,
      colorHex: color,
      role: role.trim() || null,
    });
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" style={{ width: '460px' }} onClick={(e) => e.stopPropagation()}>
        <h3 className="modal-title">
          {characterToEdit ? 'Edit Character' : 'Add New Character'}
        </h3>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {/* Preview Avatar & Name */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div
              style={{
                width: '56px',
                height: '56px',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '26px',
                backgroundColor: `${color}25`,
                border: `2px solid ${color}`,
              }}
            >
              {emoji}
            </div>
            <div style={{ flex: 1 }}>
              <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-on-surface-variant)' }}>
                Character Name
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Gandalf"
                required
                autoFocus
                style={{
                  width: '100%',
                  height: '38px',
                  padding: '0 12px',
                  borderRadius: 'var(--radius-sm)',
                  border: '1px solid var(--color-outline)',
                  backgroundColor: 'var(--color-surface-variant)',
                  outline: 'none',
                }}
              />
            </div>
          </div>

          {/* Role / Title */}
          <div>
            <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-on-surface-variant)' }}>
              Role or Title (Optional)
            </label>
            <input
              type="text"
              value={role}
              onChange={(e) => setRole(e.target.value)}
              placeholder="e.g. Lead, Mentor, Villain"
              style={{
                width: '100%',
                height: '36px',
                padding: '0 12px',
                borderRadius: 'var(--radius-sm)',
                border: '1px solid var(--color-outline)',
                backgroundColor: 'var(--color-surface-variant)',
                outline: 'none',
              }}
            />
          </div>

          {/* Emoji Presets */}
          <div>
            <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-on-surface-variant)' }}>
              Choose Avatar
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(8, 1fr)', gap: '6px', marginTop: '6px' }}>
              {PRESET_EMOJIS.map((preset) => (
                <button
                  type="button"
                  key={preset}
                  onClick={() => setEmoji(preset)}
                  style={{
                    height: '36px',
                    borderRadius: '8px',
                    border: emoji === preset ? `2px solid ${color}` : '1px solid var(--color-outline)',
                    backgroundColor: emoji === preset ? `${color}20` : 'transparent',
                    fontSize: '18px',
                    cursor: 'pointer',
                  }}
                >
                  {preset}
                </button>
              ))}
            </div>
          </div>

          {/* Color Palette */}
          <div>
            <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--color-on-surface-variant)' }}>
              Accent Color
            </label>
            <div style={{ display: 'flex', gap: '8px', marginTop: '6px' }}>
              {PRESET_COLORS.map((hex) => (
                <button
                  type="button"
                  key={hex}
                  onClick={() => setColor(hex)}
                  style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '50%',
                    backgroundColor: hex,
                    border: color === hex ? '3px solid var(--color-primary)' : '1px solid transparent',
                    cursor: 'pointer',
                    boxShadow: color === hex ? '0 0 0 2px var(--color-surface)' : 'none',
                  }}
                />
              ))}
            </div>
          </div>

          {/* Modal Actions */}
          <div className="modal-actions" style={{ marginTop: '12px' }}>
            {characterToEdit && onDelete && (
              <button
                type="button"
                className="btn-danger"
                style={{ marginRight: 'auto' }}
                onClick={() => {
                  onDelete(characterToEdit.id);
                  onClose();
                }}
              >
                Delete
              </button>
            )}
            <button type="button" className="btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn-primary">
              {characterToEdit ? 'Save Changes' : 'Add Character'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
