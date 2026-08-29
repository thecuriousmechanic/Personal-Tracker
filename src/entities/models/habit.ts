import { Model } from '@nozbe/watermelondb';
import { field, text, date, number } from '@nozbe/watermelondb/decorators';

export class Habit extends Model {
  static table = 'habits';

  @text('title') title!: string;
  @text('description') description?: string;
  @text('frequency_type') frequencyType!: string;
  @number('target_count') targetCount?: number;
  @field('active') active!: boolean;
  @text('user_id') userId!: string;
}
