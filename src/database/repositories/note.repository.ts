import { database } from '../index';
import { WatermelonRepository } from './watermelon.repository';
import { Note } from '@/entities/models/note';
import { IBaseRepository } from './base.repository';

export class NoteRepository extends WatermelonRepository<Note> implements IBaseRepository<Note> {
  constructor() {
    super(database, Note);
  }
}

export const noteRepository = new NoteRepository();
