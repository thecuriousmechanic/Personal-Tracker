import type { AppSnapshot, Habit, Task } from "@/types/entities";
import { toLocalDate } from "@/utils/date";

export interface TodayOverview {
  dateKey: string;
  openTasks: Task[];
  completedTasks: Task[];
  habits: Habit[];
  completedHabits: Habit[];
  completionRate: number;
  focusCount: number;
}

const priorityRank: Record<Task["priority"], number> = {
  urgent: 5,
  high: 4,
  medium: 3,
  low: 2,
  none: 1,
};

export function getTodayOverview(snapshot: AppSnapshot, now = new Date()): TodayOverview {
  const dateKey = toLocalDate(now);
  const scheduled = snapshot.tasks
    .filter((task) => !task.deletedAt && task.dueDate === dateKey && task.status !== "cancelled")
    .sort((a, b) => priorityRank[b.priority] - priorityRank[a.priority] || a.position - b.position);
  const habits = snapshot.habits.filter((habit) => habit.active && !habit.deletedAt);
  const openTasks = scheduled.filter((task) => task.status !== "completed");
  const completedTasks = scheduled.filter((task) => task.status === "completed");
  const completedHabits = habits.filter((habit) => habit.completedDates.includes(dateKey));
  const total = scheduled.length + habits.length;
  const completed = completedTasks.length + completedHabits.length;

  return {
    dateKey,
    openTasks,
    completedTasks,
    habits,
    completedHabits,
    completionRate: total === 0 ? 0 : Math.round((completed / total) * 100),
    focusCount: openTasks.filter((task) => task.priority === "urgent" || task.priority === "high").length,
  };
}

export function getTaskCompletionRate(tasks: Task[]): number {
  const actionable = tasks.filter((task) => task.status !== "cancelled" && !task.deletedAt);
  if (actionable.length === 0) return 0;
  return Math.round((actionable.filter((task) => task.status === "completed").length / actionable.length) * 100);
}

export function getHabitConsistency(habits: Habit[], now = new Date(), days = 7): number {
  const active = habits.filter((habit) => habit.active && !habit.deletedAt);
  if (active.length === 0) return 0;

  const expected = active.length * days;
  const start = new Date(now);
  start.setHours(0, 0, 0, 0);
  start.setDate(start.getDate() - (days - 1));
  const completed = active.reduce(
    (sum, habit) => sum + habit.completedDates.filter((date) => new Date(`${date}T12:00:00`) >= start).length,
    0,
  );
  return Math.min(100, Math.round((completed / expected) * 100));
}
