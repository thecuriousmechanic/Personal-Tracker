import { Model } from '@nozbe/watermelondb';
import { field, text, date, number } from '@nozbe/watermelondb/decorators';

export class Note extends Model {
  static table = 'notes';

  @text('title') title!: string;
  @text('content') content?: string;
  @text('format') format!: string;
  @text('status') status!: string;
  @text('user_id') userId!: string;
}
