export type NoteType = 'STANDARD' | 'STORY';

export type BlockType =
  | 'PARAGRAPH'
  | 'BULLET'
  | 'NUMBERED'
  | 'CHECKLIST'
  | 'DIALOGUE'
  | 'NARRATOR';

export type SpanType = 'BOLD' | 'ITALIC' | 'UNDERLINE' | 'STRIKETHROUGH' | 'COLOR';

export interface RichSpan {
  start: number;
  end: number;
  type: SpanType;
  colorHex?: string | null;
}

export interface RichBlock {
  id: string;
  type: BlockType;
  text: string;
  isChecked?: boolean;
  spans?: RichSpan[];
  characterId?: string | null;
  parenthetical?: string | null;
}

export interface StoryCharacter {
  id: string;
  name: string;
  avatarEmoji: string;
  colorHex: string;
  role?: string | null;
}

export interface RichContent {
  blocks: RichBlock[];
  noteType: NoteType;
  characters: StoryCharacter[];
}

export type NoteColor =
  | 'DEFAULT'
  | 'BLUE'
  | 'GREEN'
  | 'AMBER'
  | 'CORAL'
  | 'PURPLE'
  | 'TEAL';

export interface NoteColorDef {
  key: NoteColor;
  displayName: string;
  hex: string;
  lightContainer: string;
  darkContainer: string;
  lightBorder: string;
  darkBorder: string;
  dotColor: string;
}

export const NOTE_COLORS: Record<NoteColor, NoteColorDef> = {
  DEFAULT: {
    key: 'DEFAULT',
    displayName: 'Default',
    hex: '#71717A',
    lightContainer: '#FFFFFF',
    darkContainer: '#1C1C20',
    lightBorder: '#E4E4E7',
    darkBorder: '#2E2E34',
    dotColor: '#71717A',
  },
  BLUE: {
    key: 'BLUE',
    displayName: 'Ocean Blue',
    hex: '#3B82F6',
    lightContainer: '#F0F6FF',
    darkContainer: '#172033',
    lightBorder: '#BFDBFE',
    darkBorder: '#1E3A8A',
    dotColor: '#3B82F6',
  },
  GREEN: {
    key: 'GREEN',
    displayName: 'Sage Green',
    hex: '#10B981',
    lightContainer: '#F0FDF4',
    darkContainer: '#14291E',
    lightBorder: '#BBF7D0',
    darkBorder: '#065F46',
    dotColor: '#10B981',
  },
  AMBER: {
    key: 'AMBER',
    displayName: 'Sunset Amber',
    hex: '#F59E0B',
    lightContainer: '#FFFBEB',
    darkContainer: '#2D2312',
    lightBorder: '#FDE68A',
    darkBorder: '#78350F',
    dotColor: '#F59E0B',
  },
  CORAL: {
    key: 'CORAL',
    displayName: 'Coral Red',
    hex: '#EF4444',
    lightContainer: '#FEF2F2',
    darkContainer: '#2E1517',
    lightBorder: '#FECACA',
    darkBorder: '#7F1D1D',
    dotColor: '#EF4444',
  },
  PURPLE: {
    key: 'PURPLE',
    displayName: 'Lavender Purple',
    hex: '#8B5CF6',
    lightContainer: '#FAF5FF',
    darkContainer: '#231738',
    lightBorder: '#DDD6FE',
    darkBorder: '#4C1D95',
    dotColor: '#8B5CF6',
  },
  TEAL: {
    key: 'TEAL',
    displayName: 'Nordic Teal',
    hex: '#14B8A6',
    lightContainer: '#F0FDFA',
    darkContainer: '#132828',
    lightBorder: '#99F6E4',
    darkBorder: '#115E59',
    dotColor: '#14B8A6',
  },
};

export interface Note {
  id: string;
  title: string;
  content: RichContent;
  color: NoteColor;
  createdAt: number;
  updatedAt: number;
  isPinned: boolean;
}

export function generateId(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

export function createEmptyRichContent(noteType: NoteType = 'STANDARD'): RichContent {
  return {
    blocks: [
      {
        id: generateId(),
        type: 'PARAGRAPH',
        text: '',
        isChecked: false,
        spans: [],
      },
    ],
    noteType,
    characters:
      noteType === 'STORY'
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
        : [],
  };
}

export function createNewNote(noteType: NoteType = 'STANDARD'): Note {
  const now = Date.now();
  return {
    id: generateId(),
    title: '',
    content: createEmptyRichContent(noteType),
    color: 'DEFAULT',
    createdAt: now,
    updatedAt: now,
    isPinned: false,
  };
}

export function richContentToPlainText(content: RichContent): string {
  const charMap = new Map<string, StoryCharacter>();
  content.characters.forEach((c) => charMap.set(c.id, c));

  return content.blocks
    .map((block) => {
      if (block.type === 'DIALOGUE') {
        const speaker = block.characterId ? charMap.get(block.characterId)?.name || 'Speaker' : 'Speaker';
        const parenthetical = block.parenthetical ? ` (${block.parenthetical})` : '';
        return `${speaker}${parenthetical}: ${block.text}`;
      } else if (block.type === 'NARRATOR') {
        return `[${block.text}]`;
      } else if (block.type === 'CHECKLIST') {
        return `${block.isChecked ? '- [x]' : '- [ ]'} ${block.text}`;
      } else if (block.type === 'BULLET') {
        return `• ${block.text}`;
      }
      return block.text;
    })
    .join('\n');
}
