import { database } from '@/database';
import { EntityRelation } from '@/entities/models/entity_relation';
import { RelationshipType } from '@/entities/relationships/types';

export class RelationshipService {
  async createRelation(
    sourceType: string,
    sourceId: string,
    targetType: string,
    targetId: string,
    type: RelationshipType,
    metadata?: Record<string, unknown>
  ): Promise<EntityRelation> {
    return await database.write(async () => {
      return await database.get('entity_relations').create((record) => {
        record.sourceEntityType = sourceType;
        record.sourceEntityId = sourceId;
        record.targetEntityType = targetType;
        record.targetEntityId = targetId;
        record.relationshipType = type;
        record.metadata = metadata ? JSON.stringify(metadata) : undefined;
      });
    });
  }

  async deleteRelation(relationId: string): Promise<void> {
    const relation = await database.get('entity_relations').find(relationId);
    if (relation) {
      await database.write(async () => {
        await relation.markAsDeleted();
      });
    }
  }

  async getOutgoingRelations(entityType: string, entityId: string): Promise<EntityRelation[]> {
    return await database.get('entity_relations')
      .query(
        database.get('entity_relations').query().where('source_entity_type', entityType).where('source_entity_id', entityId)
      ).fetch();
  }

  async getIncomingRelations(entityType: string, entityId: string): Promise<EntityRelation[]> {
    return await database.get('entity_relations')
      .query(
        database.get('entity_relations').query().where('target_entity_type', entityType).where('target_entity_id', entityId)
      ).fetch();
  }

  async getRelatedEntities(entityType: string, entityId: string, relationshipType?: RelationshipType): Promise<{ type: string; id: string }[]> {
    const relations = await this.getOutgoingRelations(entityType, entityId);
    return relations
      .filter(r => !relationshipType || r.relationshipType === relationshipType)
      .map(r => ({ type: r.targetEntityType, id: r.targetEntityId }));
  }
}

export const relationshipService = new RelationshipService();
