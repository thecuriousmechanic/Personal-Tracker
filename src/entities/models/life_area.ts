import { Model } from '@nozbe/watermelondb';
import { field, text, date, number } from '@nozbe/watermelondb/decorators';

export class LifeArea extends Model {
  static table = 'life_areas';

  @text('name') name!: string;
  @text('description') description?: string;
  @text('icon') icon?: string;
  @text('color') color?: string;
  @text('status') status!: string;
  @text('user_id') userId!: string;
}
