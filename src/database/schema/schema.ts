import { appSchema, tableSchema } from '@nozbe/watermelondb';

export default appSchema {
  version: 1,
  tables: [
    tableSchema({
      name: 'users',
      columns: [
        { name: 'email', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'profiles',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'display_name', type: 'string', isOptional: true },
        { name: 'avatar_url', type: 'string', isOptional: true },
        { name: 'timezone', type: 'string', isOptional: true },
        { name: 'locale', type: 'string', isOptional: true },
        { name: 'theme', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'life_areas',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'name', type: 'string' },
        { name: 'description', type: 'string', isOptional: true },
        { name: 'icon', type: 'string', isOptional: true },
        { name: 'color', type: 'string', isOptional: true },
        { name: 'status', type: 'string' },
      ],
    }),
    tableSchema({
      name: 'goals',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'title', type: 'string' },
        { name: 'description', type: 'string', isOptional: true },
        { name: 'status', type: 'string' },
        { name: 'target_value', type: 'number', isOptional: true },
        { name: 'current_value', type: 'number', isOptional: true },
        { name: 'unit', type: 'string', isOptional: true },
        { name: 'start_date', type: 'string', isOptional: true },
        { name: 'target_date', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'projects',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'title', type: 'string' },
        { name: 'description', type: 'string', isOptional: true },
        { name: 'status', type: 'string' },
        { name: 'start_date', type: 'string', isOptional: true },
        { name: 'target_date', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'milestones',
      columns: [
        { name: 'project_id', type: 'string', isIndexed: true },
        { name: 'title', type: 'string' },
        { name: 'description', type: 'string', isOptional: true },
        { name: 'status', type: 'string' },
        { name: 'target_date', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'tasks',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'title', type: 'string' },
        { name: 'description', type: 'string', isOptional: true },
        { name: 'status', type: 'string' },
        { name: 'priority', type: 'string' },
        { name: 'due_date', type: 'string', isOptional: true },
        { name: 'completed_at', type: 'string', isOptional: true },
        { name: 'parent_task_id', type: 'string', isOptional: true, isIndexed: true },
        { name: 'position', type: 'number', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'habits',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'title', type: 'string' },
        { name: 'description', type: 'string', isOptional: true },
        { name: 'frequency_type', type: 'string' },
        { name: 'target_count', type: 'number', isOptional: true },
        { name: 'active', type: 'boolean' },
      ],
    }),
    tableSchema({
      name: 'habit_logs',
      columns: [
        { name: 'habit_id', type: 'string', isIndexed: true },
        { name: 'completed_at', type: 'string' },
        { name: 'value', type: 'number', isOptional: true },
        { name: 'notes', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'routines',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'title', type: 'string' },
        { name: 'description', type: 'string', isOptional: true },
        { name: 'active', type: 'boolean' },
      ],
    }),
    tableSchema({
      name: 'routine_steps',
      columns: [
        { name: 'routine_id', type: 'string', isIndexed: true },
        { name: 'title', type: 'string' },
        { name: 'order', type: 'number' },
        { name: 'completed', type: 'boolean' },
      ],
    }),
    tableSchema({
      name: 'notes',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'title', type: 'string' },
        { name: 'content', type: 'string', isOptional: true },
        { name: 'format', type: 'string' },
        { name: 'status', type: 'string' },
      ],
    }),
    tableSchema({
      name: 'note_blocks',
      columns: [
        { name: 'note_id', type: 'string', isIndexed: true },
        { name: 'type', type: 'string' },
        { name: 'content', type: 'string' },
        { name: 'position', type: 'number' },
      ],
    }),
    tableSchema({
      name: 'resources',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'title', type: 'string' },
        { name: 'type', type: 'string' },
        { name: 'url', type: 'string', isOptional: true },
        { name: 'author', type: 'string', isOptional: true },
        { name: 'metadata', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'learning_items',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'parent_id', type: 'string', isOptional: true, isIndexed: true },
        { name: 'title', type: 'string' },
        { name: 'description', type: 'string', isOptional: true },
        { name: 'type', type: 'string' },
        { name: 'status', type: 'string' },
        { name: 'mastery', type: 'number', isOptional: true },
        { name: 'position', type: 'number', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'metrics',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'name', type: 'string' },
        { name: 'unit', type: 'string' },
        { name: 'value_type', type: 'string' },
        { name: 'category', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'metric_entries',
      columns: [
        { name: 'metric_id', type: 'string', isIndexed: true },
        { name: 'value', type: 'number' },
        { name: 'recorded_at', type: 'string' },
        { name: 'notes', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'workouts',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'title', type: 'string' },
        { name: 'started_at', type: 'string', isOptional: true },
        { name: 'completed_at', type: 'string', isOptional: true },
        { name: 'notes', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'exercises',
      columns: [
        { name: 'name', type: 'string' },
        { name: 'muscle_group', type: 'string', isOptional: true },
        { name: 'instructions', type: 'string', isOptional: true },
        { name: 'image_url', type: 'string', isOptional: true },
        { name: 'metadata', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'workout_sets',
      columns: [
        { name: 'workout_id', type: 'string', isIndexed: true },
        { name: 'exercise_id', type: 'string', isIndexed: true },
        { name: 'set_number', type: 'number' },
        { name: 'weight', type: 'number', isOptional: true },
        { name: 'reps', type: 'number', isOptional: true },
        { name: 'duration', type: 'number', isOptional: true },
        { name: 'completed', type: 'boolean' },
      ],
    }),
    tableSchema({
      name: 'entity_relations',
      columns: [
        { name: 'source_entity_type', type: 'string', isIndexed: true },
        { name: 'source_entity_id', type: 'string', isIndexed: true },
        { name: 'target_entity_type', type: 'string', isIndexed: true },
        { name: 'target_entity_id', type: 'string', isIndexed: true },
        { name: 'relationship_type', type: 'string' },
        { name: 'metadata', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'tags',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'name', type: 'string' },
        { name: 'color', type: 'string', isOptional: true },
      ],
    }),
    tableSchema({
      name: 'entity_tags',
      columns: [
        { name: 'entity_type', type: 'string', isIndexed: true },
        { name: 'entity_id', type: 'string', isIndexed: true },
        { name: 'tag_id', type: 'string', isIndexed: true },
      ],
    }),
    tableSchema({
      name: 'activity_events',
      columns: [
        { name: 'user_id', type: 'string', isIndexed: true },
        { name: 'entity_type', type: 'string', isIndexed: true },
        { name: 'entity_id', type: 'string', isIndexed: true },
        { name: 'action', type: 'string' },
        { name: 'metadata', type: 'string', isOptional: true },
        { name: 'occurred_at', type: 'string' },
      ],
    }),
  ],
});
