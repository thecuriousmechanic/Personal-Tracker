import { Database, Model, Query } from '@nozbe/watermelondb';
import { BaseEntity } from '@/entities/models/base';
import { IBaseRepository } from './base.repository';

export class WatermelonRepository<T extends Model & BaseEntity> implements IBaseRepository<T> {
  constructor(
    protected db: Database,
    protected modelClass: any
  ) {}

  async getById(id: string): Promise<T | null> {
    try {
      return await this.db.get(this.modelClass.table, id);
    } catch (e) {
      return null;
    }
  }

  async list(filter?: any): Promise<T[]> {
    const query = this.db.get(this.modelClass.table).query();
    if (filter) {
      // Simplified filter logic for MVP
      // In a real app, this would be more robust
      return await query.fetch();
    }
    return await query.fetch();
  }

  async create(data: Partial<T>): Promise<T> {
    return await this.db.write(async () => {
      return await this.db.get(this.modelClass.table).create((record) => {
        Object.assign(record, data);
      });
    });
  }

  async update(id: string, data: Partial<T>): Promise<T> {
    const record = await this.getById(id);
    if (!record) throw new Error(`Record not found: ${id}`);

    return await this.db.write(async () => {
      await record.update((rec) => {
        Object.assign(rec, data);
      });
      return record;
    });
  }

  async delete(id: string): Promise<void> {
    const record = await this.getById(id);
    if (!record) return;

    await this.db.write(async () => {
      await record.markAsDeleted(); // Soft delete as per spec
    });
  }
}
