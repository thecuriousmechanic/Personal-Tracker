import { database } from '../index';
import { WatermelonRepository } from './watermelon.repository';
import { LearningItem } from '@/entities/models/learning_item';
import { IBaseRepository } from './base.repository';

export class LearningItemRepository extends WatermelonRepository<LearningItem> implements IBaseRepository<LearningItem> {
  constructor() {
    super(database, LearningItem);
  }
}

export const learningItemRepository = new LearningItemRepository();
