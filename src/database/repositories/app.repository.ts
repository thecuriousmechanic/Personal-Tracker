import type {
  ActivityEvent,
  AppSnapshot,
  EntityRelation,
  Goal,
  Habit,
  LearningItem,
  Note,
  Project,
  Task,
  Workout,
} from "@/types/entities";

export interface AppRepository {
  load(): Promise<AppSnapshot>;

  // Tasks
  saveTask(task: Task): Promise<void>;
  addTask(task: Task, activity: ActivityEvent): Promise<void>;
  deleteTask(id: string, activity?: ActivityEvent): Promise<void>;

  // Habits
  saveHabit(habit: Habit): Promise<void>;
  addHabit(habit: Habit, activity: ActivityEvent): Promise<void>;
  deleteHabit(id: string): Promise<void>;

  // Notes
  saveNote(note: Note): Promise<void>;
  addNote(note: Note, activity: ActivityEvent): Promise<void>;
  deleteNote(id: string): Promise<void>;

  // Learning Items
  saveLearningItem(item: LearningItem): Promise<void>;
  addLearningItem(item: LearningItem): Promise<void>;
  deleteLearningItem(id: string): Promise<void>;

  // Projects
  saveProject(project: Project): Promise<void>;
  addProject(project: Project, activity: ActivityEvent): Promise<void>;
  deleteProject(id: string): Promise<void>;

  // Goals
  saveGoal(goal: Goal): Promise<void>;
  addGoal(goal: Goal, activity: ActivityEvent): Promise<void>;
  deleteGoal(id: string): Promise<void>;

  // Workouts
  addWorkout(workout: Workout, activity: ActivityEvent): Promise<void>;
  deleteWorkout(id: string): Promise<void>;

  // Relations
  addRelation(relation: EntityRelation): Promise<void>;
  deleteRelation(id: string): Promise<void>;

  // Activity & Utilities
  addActivity(activity: ActivityEvent): Promise<void>;
  importSnapshot(snapshot: AppSnapshot): Promise<void>;
  reset(): Promise<AppSnapshot>;
}
