import { BaseEntity } from '@/entities/models/base';

export type EntityType =
  | 'life_area'
  | 'goal'
  | 'project'
  | 'milestone'
  | 'task'
  | 'habit'
  | 'routine'
  | 'note'
  | 'resource'
  | 'learning_item'
  | 'metric'
  | 'workout';

export interface LifeArea extends BaseEntity {
  name: string;
  description?: string;
  icon?: string;
  color?: string;
  status: 'active' | 'archived';
}

export interface Goal extends BaseEntity {
  title: string;
  description?: string;
  status: 'active' | 'paused' | 'completed' | 'cancelled';
  targetValue?: number;
  currentValue?: number;
  unit?: string;
  startDate?: Date;
  targetDate?: Date;
}

export interface Project extends BaseEntity {
  title: string;
  description?: string;
  status: 'planning' | 'active' | 'paused' | 'completed' | 'archived';
  startDate?: Date;
  targetDate?: Date;
}

export interface Milestone extends BaseEntity {
  title: string;
  description?: string;
  status: 'pending' | 'in_progress' | 'completed';
  targetDate?: Date;
}

export interface Task extends BaseEntity {
  title: string;
  description?: string;
  status: 'inbox' | 'todo' | 'in_progress' | 'completed' | 'cancelled';
  priority: 'none' | 'low' | 'medium' | 'high' | 'urgent';
  dueDate?: Date;
  completedAt?: Date;
  parentTaskId?: string;
  position?: number;
}

export interface Habit extends BaseEntity {
  title: string;
  description?: string;
  frequencyType: 'daily' | 'weekly' | 'custom';
  targetCount?: number;
  active: boolean;
}

export interface HabitLog extends BaseEntity {
  habitId: string;
  completedAt: Date;
  value?: number;
  notes?: string;
}

export interface Routine extends BaseEntity {
  title: string;
  description?: string;
  active: boolean;
}

export interface RoutineStep extends BaseEntity {
  routineId: string;
  title: string;
  order: number;
  completed: boolean;
}

export interface Note extends BaseEntity {
  title: string;
  content?: string;
  format: 'markdown' | 'blocks';
  status: 'active' | 'archived';
}

export interface Resource extends BaseEntity {
  title: string;
  type: 'book' | 'article' | 'video' | 'course' | 'paper' | 'website' | 'document' | 'other';
  url?: string;
  author?: string;
  metadata?: Record<string, unknown>;
}

export interface LearningItem extends BaseEntity {
  parentId?: string;
  title: string;
  description?: string;
  type: 'domain' | 'subject' | 'topic' | 'concept';
  status: 'not_started' | 'learning' | 'reviewing' | 'mastered';
  mastery?: number;
  position?: number;
}

export interface Metric extends BaseEntity {
  name: string;
  unit: string;
  valueType: 'number' | 'duration' | 'boolean' | 'text';
  category?: string;
}

export interface MetricEntry extends BaseEntity {
  metricId: string;
  value: number;
  recordedAt: Date;
  notes?: string;
}

export interface Workout extends BaseEntity {
  title: string;
  startedAt?: Date;
  completedAt?: Date;
  notes?: string;
}

export interface Exercise extends BaseEntity {
  name: string;
  muscleGroup?: string;
  instructions?: string;
  imageUrl?: string;
  metadata?: Record<string, unknown>;
}

export interface WorkoutSet extends BaseEntity {
  workoutId: string;
  exerciseId: string;
  setNumber: number;
  weight?: number;
  reps?: number;
  duration?: number;
  completed: boolean;
}
