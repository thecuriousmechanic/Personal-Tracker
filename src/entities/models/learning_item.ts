import { Model } from '@nozbe/watermelondb';
import { field, text, date, number } from '@nozbe/watermelondb/decorators';

export class LearningItem extends Model {
  static table = 'learning_items';

  @text('parent_id') parentId?: string;
  @text('title') title!: string;
  @text('description') description?: string;
  @text('type') type!: string;
  @text('status') status!: string;
  @number('mastery') mastery?: number;
  @number('position') position?: number;
  @text('user_id') userId!: string;
}
