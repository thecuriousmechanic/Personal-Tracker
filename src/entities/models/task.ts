import { Model } from '@nozbe/watermelondb';
import { field, text, date, number } from '@nozbe/watermelondb/decorators';

export class Task extends Model {
  static table = 'tasks';

  @text('title') title!: string;
  @text('description') description?: string;
  @text('status') status!: string;
  @text('priority') priority!: string;
  @date('due_date') dueDate?: Date;
  @date('completed_at') completedAt?: Date;
  @text('parent_task_id') parentTaskId?: string;
  @number('position') position?: number;
  @text('user_id') userId!: string;
}
