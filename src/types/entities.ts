export const CURRENT_USER_ID = "local-user";

export type IsoDateTime = string;
export type LocalDate = string;

export interface BaseEntity {
  id: string;
  userId: string;
  createdAt: IsoDateTime;
  updatedAt: IsoDateTime;
  deletedAt?: IsoDateTime;
}

export type EntityType =
  | "life_area"
  | "goal"
  | "project"
  | "task"
  | "habit"
  | "note"
  | "learning_item"
  | "metric"
  | "workout";

export interface LifeArea extends BaseEntity {
  name: string;
  icon: string;
  color: string;
  status: "active" | "archived";
}

export interface Goal extends BaseEntity {
  title: string;
  description?: string;
  status: "active" | "paused" | "completed" | "cancelled";
  progress: number;
  targetDate?: LocalDate;
  lifeAreaId?: string;
}

export interface Project extends BaseEntity {
  title: string;
  description?: string;
  status: "planning" | "active" | "paused" | "completed" | "archived";
  progress: number;
  targetDate?: LocalDate;
  goalId?: string;
  lifeAreaId?: string;
  color: string;
}

export interface Task extends BaseEntity {
  title: string;
  description?: string;
  status: "inbox" | "todo" | "in_progress" | "completed" | "cancelled";
  priority: "none" | "low" | "medium" | "high" | "urgent";
  dueDate?: LocalDate;
  completedAt?: IsoDateTime;
  parentTaskId?: string;
  projectId?: string;
  position: number;
}

export interface Habit extends BaseEntity {
  title: string;
  description?: string;
  frequencyType: "daily" | "weekly" | "custom";
  targetCount: number;
  active: boolean;
  streak: number;
  completedDates: LocalDate[];
  color: string;
}

export interface Note extends BaseEntity {
  title: string;
  content: string;
  format: "markdown";
  status: "active" | "archived";
  tags: string[];
  linkedEntityCount: number;
}

export interface LearningItem extends BaseEntity {
  parentId?: string;
  title: string;
  description?: string;
  type: "domain" | "subject" | "topic" | "concept";
  status: "not_started" | "learning" | "reviewing" | "mastered";
  mastery: number;
  position: number;
}

export interface Metric extends BaseEntity {
  name: string;
  unit: string;
  valueType: "number" | "duration" | "boolean" | "text";
  category: string;
  currentValue: number;
  previousValue?: number;
  color: string;
}

export interface Workout extends BaseEntity {
  title: string;
  startedAt: IsoDateTime;
  completedAt?: IsoDateTime;
  durationMinutes: number;
  volumeKg: number;
}

export type RelationshipType =
  | "belongs_to"
  | "contributes_to"
  | "supports"
  | "requires"
  | "related_to"
  | "depends_on"
  | "references"
  | "blocks"
  | "tracks"
  | "part_of";

export interface EntityRelation {
  id: string;
  userId: string;
  sourceEntityType: EntityType;
  sourceEntityId: string;
  targetEntityType: EntityType;
  targetEntityId: string;
  relationshipType: RelationshipType;
  createdAt: IsoDateTime;
}

export interface ActivityEvent {
  id: string;
  userId: string;
  entityType: EntityType;
  entityId: string;
  action: "created" | "completed" | "reopened" | "scheduled";
  label: string;
  occurredAt: IsoDateTime;
}

export interface AppSnapshot {
  lifeAreas: LifeArea[];
  goals: Goal[];
  projects: Project[];
  tasks: Task[];
  habits: Habit[];
  notes: Note[];
  learningItems: LearningItem[];
  metrics: Metric[];
  workouts: Workout[];
  relations: EntityRelation[];
  activities: ActivityEvent[];
}

export type CaptureType = "task" | "note" | "project" | "goal" | "habit";

export interface CaptureInput {
  type: CaptureType;
  title: string;
  destination?: "inbox" | "today" | "tomorrow";
  priority?: Task["priority"];
}
