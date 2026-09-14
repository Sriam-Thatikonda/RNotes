import { Note } from '../models/note';

export interface NotoirExportContainer {
  formatVersion: number;
  appName: string;
  exportedAt: number;
  notesCount: number;
  notes: Note[];
}

export function exportNotesToJsonFile(notes: Note[]): void {
  const container: NotoirExportContainer = {
    formatVersion: 1,
    appName: 'r-notes',
    exportedAt: Date.now(),
    notesCount: notes.length,
    notes,
  };

  const jsonStr = JSON.stringify(container, null, 2);
  const blob = new Blob([jsonStr], { type: 'application/json' });
  const url = URL.createObjectURL(blob);

  const dateStr = new Date().toISOString().split('T')[0];
  const a = document.createElement('a');
  a.href = url;
  a.download = `rnotes_backup_${dateStr}.json`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export async function importNotesFromJsonFile(file: File): Promise<Note[]> {
  const text = await file.text();
  let data: any;
  try {
    data = JSON.parse(text);
  } catch {
    throw new Error('Invalid JSON file format.');
  }

  // Verify structure
  if (!data || !Array.isArray(data.notes)) {
    throw new Error('Unrecognized backup format. Expected "notes" array.');
  }

  return data.notes as Note[];
}
