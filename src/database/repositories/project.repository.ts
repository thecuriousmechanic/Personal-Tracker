import { database } from '../index';
import { WatermelonRepository } from './watermelon.repository';
import { Project } from '@/entities/models/project';
import { IBaseRepository } from './base.repository';

export class ProjectRepository extends WatermelonRepository<Project> implements IBaseRepository<Project> {
  constructor() {
    super(database, Project);
  }
}

export const projectRepository = new ProjectRepository();
