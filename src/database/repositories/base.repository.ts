import { BaseEntity } from '@/entities/models/base';

export interface IBaseRepository<T extends BaseEntity> {
  getById(id: string): Promise<T | null>;
  list(filter?: any): Promise<T[]>;
  create(data: Partial<T>): Promise<T>;
  update(id: string, data: Partial<T>): Promise<T>;
  delete(id: string): Promise<void>;
}
