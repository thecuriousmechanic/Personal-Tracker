import Dexie, { type Table } from "dexie";
import type {
  ActivityEvent,
  EntityRelation,
  Goal,
  Habit,
  LearningItem,
  LifeArea,
  Metric,
  Note,
  Project,
  Task,
  Workout,
} from "@/types/entities";

interface Setting {
  key: string;
  value: string;
}

export class PersonalOSDatabase extends Dexie {
  lifeAreas!: Table<LifeArea, string>;
  goals!: Table<Goal, string>;
  projects!: Table<Project, string>;
  tasks!: Table<Task, string>;
  habits!: Table<Habit, string>;
  notes!: Table<Note, string>;
  learningItems!: Table<LearningItem, string>;
  metrics!: Table<Metric, string>;
  workouts!: Table<Workout, string>;
  relations!: Table<EntityRelation, string>;
  activities!: Table<ActivityEvent, string>;
  settings!: Table<Setting, string>;

  constructor() {
    super("personal-os");
    this.version(1).stores({
      lifeAreas: "id, userId, status, updatedAt",
      goals: "id, userId, status, targetDate, lifeAreaId, updatedAt",
      projects: "id, userId, status, targetDate, goalId, lifeAreaId, updatedAt",
      tasks: "id, userId, status, dueDate, priority, projectId, parentTaskId, updatedAt",
      habits: "id, userId, active, frequencyType, updatedAt, *completedDates",
      notes: "id, userId, status, updatedAt, *tags",
      learningItems: "id, userId, parentId, type, status, updatedAt",
      metrics: "id, userId, category, updatedAt",
      workouts: "id, userId, completedAt, updatedAt",
      relations: "id, userId, [sourceEntityType+sourceEntityId], [targetEntityType+targetEntityId], relationshipType",
      activities: "id, userId, entityType, entityId, action, occurredAt",
      settings: "key",
    });
  }
}
