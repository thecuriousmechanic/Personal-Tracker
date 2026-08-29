import { Model } from '@nozbe/watermelondb';
import { field, text, date, number } from '@nozbe/watermelondb/decorators';

export class Goal extends Model {
  static table = 'goals';

  @text('title') title!: string;
  @text('description') description?: string;
  @text('status') status!: string;
  @number('target_value') targetValue?: number;
  @number('current_value') currentValue?: number;
  @text('unit') unit?: string;
  @date('start_date') startDate?: Date;
  @date('target_date') targetDate?: Date;
  @text('user_id') userId!: string;
}
