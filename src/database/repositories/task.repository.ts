import { database } from '../index';
import { WatermelonRepository } from './watermelon.repository';
import { Task } from '@/entities/models/task';
import { IBaseRepository } from './base.repository';

export class TaskRepository extends WatermelonRepository<Task> implements IBaseRepository<Task> {
  constructor() {
    super(database, Task);
  }
}

export const taskRepository = new TaskRepository();
