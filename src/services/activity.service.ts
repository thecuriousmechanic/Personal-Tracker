import { database } from '@/database';
import { BaseEntity } from '@/entities/models/base';

export interface ActivityEvent extends BaseEntity {
  userId: string;
  entityType: string;
  entityId: string;
  action: string;
  metadata?: Record<string, unknown>;
  occurredAt: Date;
}

export class ActivityService {
  async logEvent(event: Partial<ActivityEvent>) {
    return await database.write(async () => {
      return await database.get('activity_events').create((record) => {
        Object.assign(record, event);
        record.occurredAt = new Date();
      });
    });
  }

  async getRecentActivity(userId: string, limit = 20) {
    const allEvents = await database.get('activity_events').query().fetch();
    return allEvents
      .filter(e => e.userId === userId)
      .sort((a, b) => (b.occurredAt?.getTime() || 0) - (a.occurredAt?.getTime() || 0))
      .slice(0, limit);
  }
}

export const activityService = new ActivityService();
