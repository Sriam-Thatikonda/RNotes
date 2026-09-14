import { Note, NoteColor, RichContent, richContentToPlainText } from '../models/note';

let tauriDb: any = null;
let isInitialized = false;

function isTauriEnvironment(): boolean {
  return typeof window !== 'undefined' && ('__TAURI_INTERNALS__' in window || '__TAURI__' in window);
}

const LOCAL_STORAGE_KEY = 'rnotes_desktop_db_fallback';

export async function initDb(): Promise<void> {
  if (isInitialized) return;

  if (isTauriEnvironment()) {
    try {
      const Database = (await import('@tauri-apps/plugin-sql')).default;
      tauriDb = await Database.load('sqlite:rnotes.db');

      await tauriDb.execute(`
        CREATE TABLE IF NOT EXISTS notes (
          id TEXT PRIMARY KEY,
          title TEXT NOT NULL,
          contentJson TEXT NOT NULL,
          plainText TEXT NOT NULL,
          color TEXT NOT NULL,
          createdAt INTEGER NOT NULL,
          updatedAt INTEGER NOT NULL,
          isPinned INTEGER NOT NULL DEFAULT 0
        );
      `);

      await tauriDb.execute(`
        CREATE INDEX IF NOT EXISTS idx_notes_updatedAt ON notes(updatedAt DESC);
      `);
      await tauriDb.execute(`
        CREATE INDEX IF NOT EXISTS idx_notes_isPinned ON notes(isPinned DESC);
      `);
      await tauriDb.execute(`
        CREATE INDEX IF NOT EXISTS idx_notes_title ON notes(title);
      `);
    } catch (err) {
      console.warn('Tauri SQL plugin could not load, falling back to local storage', err);
      tauriDb = null;
    }
  }

  isInitialized = true;
}

export async function getAllNotes(): Promise<Note[]> {
  await initDb();

  if (tauriDb) {
    const rows = await tauriDb.select(`
      SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC
    `);
    return rows.map(entityToNote);
  }

  const stored = localStorage.getItem(LOCAL_STORAGE_KEY);
  if (!stored) return [];
  try {
    const notes: Note[] = JSON.parse(stored);
    return notes.sort((a, b) => {
      if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
      return b.updatedAt - a.updatedAt;
    });
  } catch {
    return [];
  }
}

export async function searchNotes(query: string): Promise<Note[]> {
  await initDb();
  const trimmed = query.trim().toLowerCase();
  if (!trimmed) {
    return getAllNotes();
  }

  if (tauriDb) {
    const rows = await tauriDb.select(
      `
      SELECT * FROM notes
      WHERE LOWER(title) LIKE $1 OR LOWER(plainText) LIKE $1
      ORDER BY isPinned DESC, updatedAt DESC
      `,
      [`%${trimmed}%`]
    );
    return rows.map(entityToNote);
  }

  const all = await getAllNotes();
  return all.filter((note) => {
    const plain = richContentToPlainText(note.content).toLowerCase();
    return note.title.toLowerCase().includes(trimmed) || plain.includes(trimmed);
  });
}

export async function getNoteById(id: string): Promise<Note | null> {
  await initDb();

  if (tauriDb) {
    const rows = await tauriDb.select('SELECT * FROM notes WHERE id = $1 LIMIT 1', [id]);
    if (rows.length > 0) {
      return entityToNote(rows[0]);
    }
    return null;
  }

  const all = await getAllNotes();
  return all.find((n) => n.id === id) || null;
}

export async function saveNote(note: Note): Promise<void> {
  await initDb();
  const plainText = richContentToPlainText(note.content);
  const contentJson = JSON.stringify(note.content);

  if (tauriDb) {
    await tauriDb.execute(
      `
      INSERT INTO notes (id, title, contentJson, plainText, color, createdAt, updatedAt, isPinned)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
      ON CONFLICT(id) DO UPDATE SET
        title = excluded.title,
        contentJson = excluded.contentJson,
        plainText = excluded.plainText,
        color = excluded.color,
        updatedAt = excluded.updatedAt,
        isPinned = excluded.isPinned;
      `,
      [
        note.id,
        note.title,
        contentJson,
        plainText,
        note.color,
        note.createdAt,
        note.updatedAt,
        note.isPinned ? 1 : 0,
      ]
    );
    return;
  }

  const all = await getAllNotes();
  const index = all.findIndex((n) => n.id === note.id);
  if (index >= 0) {
    all[index] = note;
  } else {
    all.unshift(note);
  }
  localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(all));
}

export async function deleteNote(id: string): Promise<void> {
  await initDb();

  if (tauriDb) {
    await tauriDb.execute('DELETE FROM notes WHERE id = $1', [id]);
    return;
  }

  const all = await getAllNotes();
  const filtered = all.filter((n) => n.id !== id);
  localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(filtered));
}

export async function togglePin(id: string): Promise<Note | null> {
  const note = await getNoteById(id);
  if (!note) return null;
  const updated: Note = {
    ...note,
    isPinned: !note.isPinned,
    updatedAt: Date.now(),
  };
  await saveNote(updated);
  return updated;
}

function entityToNote(row: any): Note {
  let content: RichContent;
  try {
    content = JSON.parse(row.contentJson);
  } catch {
    content = {
      blocks: [{ id: '1', type: 'PARAGRAPH', text: row.plainText || '', spans: [] }],
      noteType: 'STANDARD',
      characters: [],
    };
  }

  return {
    id: row.id,
    title: row.title,
    content,
    color: (row.color as NoteColor) || 'DEFAULT',
    createdAt: Number(row.createdAt),
    updatedAt: Number(row.updatedAt),
    isPinned: Boolean(row.isPinned),
  };
}
