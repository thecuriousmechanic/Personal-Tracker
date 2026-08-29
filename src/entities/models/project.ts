import { Model } from '@nozbe/watermelondb';
import { field, text, date, number } from '@nozbe/watermelondb/decorators';

export class Project extends Model {
  static table = 'projects';

  @text('title') title!: string;
  @text('description') description?: string;
  @text('status') status!: string;
  @date('start_date') startDate?: Date;
  @date('target_date') targetDate?: Date;
  @text('user_id') userId!: string;
}
