import { Model } from '@nozbe/watermelondb';
import { field, text, date, number } from '@nozbe/watermelondb/decorators';

export class Exercise extends Model {
  static table = 'exercises';

  @text('name') name!: string;
  @text('muscle_group') muscleGroup?: string;
  @text('instructions') instructions?: string;
  @text('image_url') imageUrl?: string;
  @text('metadata') metadata?: string;
}
