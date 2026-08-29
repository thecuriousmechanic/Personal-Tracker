import { Model } from '@nozbe/watermelondb';
import { field, text, date, number } from '@nozbe/watermelondb/decorators';

export class Workout extends Model {
  static table = 'workouts';

  @text('title') title!: string;
  @date('started_at') startedAt?: Date;
  @date('completed_at') completedAt?: Date;
  @text('notes') notes?: string;
  @text('user_id') userId!: string;
}
