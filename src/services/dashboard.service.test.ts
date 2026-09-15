import { describe, expect, it } from "vitest";
import { getHabitConsistency, getTaskCompletionRate, getTodayOverview } from "./dashboard.service";
import type { AppSnapshot, Habit, Task } from "@/types/entities";

const base = {
  userId: "user",
  createdAt: "2026-09-03T00:00:00.000Z",
  updatedAt: "2026-09-03T00:00:00.000Z",
};

function snapshot(tasks: Task[], habits: Habit[] = []): AppSnapshot {
  return {
    lifeAreas: [], goals: [], projects: [], tasks, habits, notes: [], learningItems: [],
    metrics: [], workouts: [], relations: [], activities: [],
  };
}

describe("today dashboard selectors", () => {
  it("sorts open work by priority and includes completion progress", () => {
    const tasks: Task[] = [
      { ...base, id: "low", title: "Low", status: "todo", priority: "low", dueDate: "2026-09-03", position: 0 },
      { ...base, id: "high", title: "High", status: "todo", priority: "high", dueDate: "2026-09-03", position: 1 },
      { ...base, id: "done", title: "Done", status: "completed", priority: "medium", dueDate: "2026-09-03", position: 2 },
      { ...base, id: "later", title: "Later", status: "todo", priority: "urgent", dueDate: "2026-09-04", position: 3 },
    ];

    const result = getTodayOverview(snapshot(tasks), new Date("2026-09-03T10:00:00"));

    expect(result.openTasks.map((task) => task.id)).toEqual(["high", "low"]);
    expect(result.completedTasks).toHaveLength(1);
    expect(result.completionRate).toBe(33);
  });

  it("calculates task completion and seven-day habit consistency", () => {
    const tasks: Task[] = [
      { ...base, id: "one", title: "One", status: "completed", priority: "none", position: 0 },
      { ...base, id: "two", title: "Two", status: "todo", priority: "none", position: 1 },
    ];
    const habits: Habit[] = [{
      ...base,
      id: "habit",
      title: "Read",
      frequencyType: "daily",
      targetCount: 1,
      active: true,
      streak: 3,
      completedDates: ["2026-09-01", "2026-09-02", "2026-09-03"],
      color: "#8b5cf6",
    }];

    expect(getTaskCompletionRate(tasks)).toBe(50);
    expect(getHabitConsistency(habits, new Date("2026-09-03T12:00:00"))).toBe(43);
  });
});
