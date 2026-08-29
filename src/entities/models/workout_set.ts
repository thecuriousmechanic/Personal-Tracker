import { Model } from '@nozbe/watermelondb';
import { field, text, date, number } from '@nozbe/watermelondb/decorators';

export class WorkoutSet extends Model {
  static table = 'workout_sets';

  @text('workout_id') workoutId!: string;
  @text('exercise_id') exerciseId!: string;
  @number('set_number') setNumber!: number;
  @number('weight') weight?: number;
  @number('reps') reps?: number;
  @number('duration') duration?: number;
  @field('completed') completed!: boolean;
}
