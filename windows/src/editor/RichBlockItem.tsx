import React, { useRef, useEffect } from 'react';
import { CheckSquare, Square } from 'lucide-react';
import { RichBlock } from '../models/note';

interface RichBlockItemProps {
  block: RichBlock;
  index: number;
  isFocused: boolean;
  onTextChanged: (text: string) => void;
  onToggleChecklist: () => void;
  onEnter: (before: string, after: string) => void;
  onBackspaceOnEmpty: () => void;
  onFocus: () => void;
}

export const RichBlockItem: React.FC<RichBlockItemProps> = ({
  block,
  index,
  isFocused,
  onTextChanged,
  onToggleChecklist,
  onEnter,
  onBackspaceOnEmpty,
  onFocus,
}) => {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-resize height on text change and window resize
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

  return (
    <div className="block-row">
      {/* Prefix Icon / Bullet / Number */}
      <div className="block-prefix">
        {block.type === 'CHECKLIST' && (
          <button
            type="button"
            className={`checkbox-btn ${block.isChecked ? 'checked' : ''}`}
            onClick={onToggleChecklist}
            title={block.isChecked ? 'Mark incomplete' : 'Mark complete'}
          >
            {block.isChecked ? <CheckSquare size={19} /> : <Square size={19} />}
          </button>
        )}
        {block.type === 'BULLET' && <span className="bullet-dot">•</span>}
        {block.type === 'NUMBERED' && (
          <span className="numbered-label">{index + 1}.</span>
        )}
      </div>

      {/* Editable Block Text Area */}
      <div className="block-input-wrapper">
        <textarea
          ref={textareaRef}
          className={`block-text-input ${block.isChecked && block.type === 'CHECKLIST' ? 'completed' : ''}`}
          rows={1}
          value={block.text}
          onChange={(e) => onTextChanged(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={onFocus}
          placeholder={index === 0 && block.text === '' ? 'Type your notes here...' : ''}
        />
      </div>
    </div>
  );
};
