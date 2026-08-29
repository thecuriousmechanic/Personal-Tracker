import { noteRepository } from '@/database/repositories/note.repository';
import { resourceRepository } from '@/database/repositories/resource.repository';
import { learningItemRepository } from '@/database/repositories/learning_item.repository';
import { relationshipService } from '@/entities/relationships';
import { Note } from '@/entities/models/note';
import { Resource } from '@/entities/models/resource';
import { LearningItem } from '@/entities/models/learning_item';

export class KnowledgeService {
  async getNoteWithRelations(noteId: string) {
    const note = await noteRepository.getById(noteId);
    if (!note) return null;

    const relations = await relationshipService.getIncomingRelations('note', noteId);

    return {
      note,
      relations,
    };
  }

  async searchKnowledge(query: string) {
    const notes = await noteRepository.list();
    const filteredNotes = notes.filter(n =>
      n.title.toLowerCase().includes(query.toLowerCase()) ||
      n.content?.toLowerCase().includes(query.toLowerCase())
    );

    return filteredNotes;
  }

  async getLearningHierarchy(domainId: string) {
    const allItems = await learningItemRepository.list();
    const domain = allItems.find(i => i.id === domainId);

    const subjects = allItems.filter(i => i.parentId === domainId);

    return {
      domain,
      subjects: await Promise.all(subjects.map(async s => ({
        ...s,
        topics: allItems.filter(i => i.parentId === s.id),
      }))),
    };
  }
}

export const knowledgeService = new KnowledgeService();
