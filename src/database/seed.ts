import type {
  ActivityEvent,
  AppSnapshot,
  BaseEntity,
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
import { CURRENT_USER_ID } from "@/types/entities";
import { addDays, toLocalDate } from "@/utils/date";

function base(id: string, now: Date): BaseEntity {
  const timestamp = now.toISOString();
  return { id, userId: CURRENT_USER_ID, createdAt: timestamp, updatedAt: timestamp };
}

export function createSeedSnapshot(now = new Date()): AppSnapshot {
  const today = toLocalDate(now);
  const yesterday = toLocalDate(addDays(now, -1));
  const twoDaysAgo = toLocalDate(addDays(now, -2));
  const threeDaysAgo = toLocalDate(addDays(now, -3));
  const tomorrow = toLocalDate(addDays(now, 1));
  const nextWeek = toLocalDate(addDays(now, 7));

  const lifeAreas: LifeArea[] = [
    { ...base("area-work", now), name: "Work & craft", icon: "briefcase", color: "#6d8cff", status: "active" },
    { ...base("area-growth", now), name: "Learning", icon: "book", color: "#a78bfa", status: "active" },
    { ...base("area-health", now), name: "Health", icon: "heart", color: "#34d399", status: "active" },
  ];

  const goals: Goal[] = [
    {
      ...base("goal-personal-os", now),
      title: "Build a calm personal operating system",
      description: "Create one connected place to plan, learn, and measure progress.",
      status: "active",
      progress: 38,
      targetDate: toLocalDate(addDays(now, 60)),
      lifeAreaId: "area-work",
    },
    {
      ...base("goal-consistency", now),
      title: "Build durable health routines",
      status: "active",
      progress: 64,
      targetDate: toLocalDate(addDays(now, 90)),
      lifeAreaId: "area-health",
    },
  ];

  const projects: Project[] = [
    {
      ...base("project-foundation", now),
      title: "Personal OS foundation",
      description: "Offline data, entity relationships, and a fast daily loop.",
      status: "active",
      progress: 42,
      targetDate: nextWeek,
      goalId: "goal-personal-os",
      lifeAreaId: "area-work",
      color: "#6d8cff",
    },
    {
      ...base("project-systems", now),
      title: "Distributed systems study",
      description: "Turn reading notes into connected, reviewable concepts.",
      status: "active",
      progress: 61,
      targetDate: toLocalDate(addDays(now, 24)),
      lifeAreaId: "area-growth",
      color: "#a78bfa",
    },
  ];

  const tasks: Task[] = [
    {
      ...base("task-dashboard", now),
      title: "Shape the Today dashboard",
      description: "Make the next meaningful action unmistakable.",
      status: "completed",
      priority: "urgent",
      dueDate: today,
      completedAt: now.toISOString(),
      projectId: "project-foundation",
      position: 0,
    },
    {
      ...base("task-entities", now),
      title: "Review the core entity model",
      status: "todo",
      priority: "high",
      dueDate: today,
      projectId: "project-foundation",
      position: 1,
    },
    {
      ...base("task-notes", now),
      title: "Read distributed systems notes",
      status: "todo",
      priority: "medium",
      dueDate: today,
      projectId: "project-systems",
      position: 2,
    },
    {
      ...base("task-review", now),
      title: "Ten-minute weekly reset",
      status: "todo",
      priority: "low",
      dueDate: today,
      position: 3,
    },
    {
      ...base("task-capture-one", now),
      title: "Decide the first sync target",
      status: "inbox",
      priority: "none",
      position: 4,
    },
    {
      ...base("task-capture-two", now),
      title: "Collect nutrition tracking requirements",
      status: "inbox",
      priority: "none",
      position: 5,
    },
    {
      ...base("task-tomorrow", now),
      title: "Draft the relationship explorer",
      status: "todo",
      priority: "medium",
      dueDate: tomorrow,
      projectId: "project-foundation",
      position: 6,
    },
  ];

  const habits: Habit[] = [
    {
      ...base("habit-plan", now), title: "Plan the day", frequencyType: "daily", targetCount: 1,
      active: true, streak: 8, completedDates: [threeDaysAgo, twoDaysAgo, yesterday, today], color: "#6d8cff",
    },
    {
      ...base("habit-focus", now), title: "90 min deep work", frequencyType: "daily", targetCount: 1,
      active: true, streak: 4, completedDates: [threeDaysAgo, twoDaysAgo, yesterday], color: "#a78bfa",
    },
    {
      ...base("habit-train", now), title: "Move or train", frequencyType: "daily", targetCount: 1,
      active: true, streak: 6, completedDates: [twoDaysAgo, yesterday, today], color: "#34d399",
    },
    {
      ...base("habit-read", now), title: "Read for 20 minutes", frequencyType: "daily", targetCount: 1,
      active: true, streak: 3, completedDates: [twoDaysAgo, yesterday], color: "#f5b85b",
    },
  ];

  const notes: Note[] = [
    {
      ...base("note-architecture", now), title: "Personal OS architecture", format: "markdown", status: "active",
      content: "The product should connect execution, knowledge, health, and progress through shared entities—not isolated modules.",
      tags: ["architecture", "product"], linkedEntityCount: 3,
    },
    {
      ...base("note-retrieval", now), title: "Retrieval design notes", format: "markdown", status: "active",
      content: "Start with deterministic search and explicit backlinks. Add semantic retrieval only after the knowledge loop works.",
      tags: ["research", "knowledge"], linkedEntityCount: 2,
    },
    {
      ...base("note-training", now), title: "Training observations", format: "markdown", status: "active",
      content: "Consistency is improving. Keep volume steady and protect sleep before increasing intensity.",
      tags: ["health"], linkedEntityCount: 1,
    },
  ];

  const learningItems: LearningItem[] = [
    {
      ...base("learn-domain", now), title: "Computer science", type: "domain", status: "learning",
      mastery: 58, position: 0,
    },
    {
      ...base("learn-systems", now), parentId: "learn-domain", title: "Distributed systems", type: "subject",
      status: "learning", mastery: 61, position: 0,
    },
    {
      ...base("learn-consensus", now), parentId: "learn-systems", title: "Consensus", type: "topic",
      status: "reviewing", mastery: 72, position: 0,
    },
    {
      ...base("learn-replication", now), parentId: "learn-systems", title: "Replication", type: "topic",
      status: "learning", mastery: 46, position: 1,
    },
  ];

  const metrics: Metric[] = [
    { ...base("metric-sleep", now), name: "Sleep", unit: "hours", valueType: "duration", category: "recovery", currentValue: 7.4, previousValue: 6.9, color: "#8aa4ff" },
    { ...base("metric-focus", now), name: "Focus", unit: "hours", valueType: "duration", category: "productivity", currentValue: 3.5, previousValue: 2.8, color: "#a78bfa" },
    { ...base("metric-weight", now), name: "Weight", unit: "kg", valueType: "number", category: "body", currentValue: 77.8, previousValue: 78.2, color: "#34d399" },
  ];

  const workouts: Workout[] = [
    {
      ...base("workout-upper", now), title: "Upper strength", startedAt: addDays(now, -2).toISOString(),
      completedAt: addDays(now, -2).toISOString(), durationMinutes: 54, volumeKg: 5240,
    },
    {
      ...base("workout-run", now), title: "Easy run", startedAt: addDays(now, -5).toISOString(),
      completedAt: addDays(now, -5).toISOString(), durationMinutes: 32, volumeKg: 0,
    },
  ];

  const relations: EntityRelation[] = [
    { id: "rel-task-project", userId: CURRENT_USER_ID, sourceEntityType: "task", sourceEntityId: "task-entities", targetEntityType: "project", targetEntityId: "project-foundation", relationshipType: "belongs_to", createdAt: now.toISOString() },
    { id: "rel-project-goal", userId: CURRENT_USER_ID, sourceEntityType: "project", sourceEntityId: "project-foundation", targetEntityType: "goal", targetEntityId: "goal-personal-os", relationshipType: "contributes_to", createdAt: now.toISOString() },
    { id: "rel-note-project", userId: CURRENT_USER_ID, sourceEntityType: "note", sourceEntityId: "note-architecture", targetEntityType: "project", targetEntityId: "project-foundation", relationshipType: "supports", createdAt: now.toISOString() },
  ];

  const activities: ActivityEvent[] = [
    { id: "activity-task", userId: CURRENT_USER_ID, entityType: "task", entityId: "task-dashboard", action: "completed", label: "Shaped the Today dashboard", occurredAt: now.toISOString() },
    { id: "activity-note", userId: CURRENT_USER_ID, entityType: "note", entityId: "note-architecture", action: "created", label: "Captured Personal OS architecture", occurredAt: addDays(now, -1).toISOString() },
    { id: "activity-workout", userId: CURRENT_USER_ID, entityType: "workout", entityId: "workout-upper", action: "completed", label: "Completed Upper strength", occurredAt: addDays(now, -2).toISOString() },
  ];

  return { lifeAreas, goals, projects, tasks, habits, notes, learningItems, metrics, workouts, relations, activities };
}
