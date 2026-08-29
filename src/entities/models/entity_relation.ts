import { Model } from '@nozbe/watermelondb';
import { field, text, date, number } from '@nozbe/watermelondb/decorators';
import { RelationshipType } from '@/entities/relationships/types';

export class EntityRelation extends Model {
  static table = 'entity_relations';

  @text('source_entity_type') sourceEntityType!: string;
  @text('source_entity_id') sourceEntityId!: string;
  @text('target_entity_type') targetEntityType!: string;
  @text('target_entity_id') targetEntityId!: string;
  @text('relationship_type') relationshipType!: RelationshipType;
  @text('metadata') metadata?: string;
}
