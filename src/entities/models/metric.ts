import { Model } from '@nozbe/watermelondb';
import { field, text, date, number } from '@nozbe/watermelondb/decorators';

export class Metric extends Model {
  static table = 'metrics';

  @text('name') name!: string;
  @text('unit') unit!: string;
  @text('value_type') valueType!: string;
  @text('category') category?: string;
  @text('user_id') userId!: string;
}
