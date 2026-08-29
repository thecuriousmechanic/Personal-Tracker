import { database } from '../index';
import { WatermelonRepository } from './watermelon.repository';
import { Goal } from '@/entities/models/goal';
import { IBaseRepository } from './base.repository';

export class GoalRepository extends WatermelonRepository<Goal> implements IBaseRepository<Goal> {
  constructor() {
    super(database, Goal);
  }
}

export const goalRepository = new GoalRepository();
