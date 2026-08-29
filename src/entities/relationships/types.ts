export type RelationshipType =
  | 'belongs_to'
  | 'contributes_to'
  | 'supports'
  | 'requires'
  | 'related_to'
  | 'depends_on'
  | 'references'
  | 'blocks'
  | 'tracks'
  | 'part_of';

export interface EntityRelation {
  id: string;
  sourceEntityType: string;
  sourceEntityId: string;
  targetEntityType: string;
  targetEntityId: string;
  relationshipType: RelationshipType;
  metadata?: Record<string, unknown>;
  createdAt: Date;
}
