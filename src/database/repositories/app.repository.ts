import type {
  ActivityEvent,
  AppSnapshot,
  Goal,
  Habit,
  Note,
  Project,
  Task,
} from "@/types/entities";

export interface AppRepository {
  load(): Promise<AppSnapshot>;
  saveTask(task: Task): Promise<void>;
  saveHabit(habit: Habit): Promise<void>;
  addTask(task: Task, activity: ActivityEvent): Promise<void>;
  addHabit(habit: Habit, activity: ActivityEvent): Promise<void>;
  addNote(note: Note, activity: ActivityEvent): Promise<void>;
  addProject(project: Project, activity: ActivityEvent): Promise<void>;
  addGoal(goal: Goal, activity: ActivityEvent): Promise<void>;
  addActivity(activity: ActivityEvent): Promise<void>;
  reset(): Promise<AppSnapshot>;
}
