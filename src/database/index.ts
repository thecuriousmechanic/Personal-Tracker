import { Database } from '@nozbe/watermelondb';
import SQLiteAdapter from '@nozbe/watermelondb/adapters/sqlite';
import schema from './schema/schema';
import { Task } from '@/entities/models/task';
import { LifeArea } from '@/entities/models/life_area';
import { Goal } from '@/entities/models/goal';
import { Project } from '@/entities/models/project';
import { Habit } from '@/entities/models/habit';
import { Note } from '@/entities/models/note';
import { LearningItem } from '@/entities/models/learning_item';
import { Metric } from '@/entities/models/metric';
import { Workout } from '@/entities/models/workout';
import { Exercise } from '@/entities/models/exercise';
import { WorkoutSet } from '@/entities/models/workout_set';
import { EntityRelation } from '@/entities/models/entity_relation';

const adapter = new SQLiteAdapter({
  schema,
  jsi: true, // Use JSI for better performance on mobile
  onSetUpError: error => {
    console.error('WatermelonDB setup error:', error);
  },
});

export const database = new Database({
  adapter,
  modelClasses: [
    Task,
    LifeArea,
    Goal,
    Project,
    Habit,
    Note,
    LearningItem,
    Metric,
    Workout,
    Exercise,
    WorkoutSet,
    EntityRelation,
  ],
});

export { adapter };
