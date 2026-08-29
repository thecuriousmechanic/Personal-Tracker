import { database } from '../index';
import { WatermelonRepository } from './watermelon.repository';
import { Habit } from '@/entities/models/habit';
import { IBaseRepository } from './base.repository';

export class HabitRepository extends WatermelonRepository<Habit> implements IBaseRepository<Habit> {
  constructor() {
    super(database, Habit);
  }
}

export const habitRepository = new HabitRepository();
