import "fake-indexeddb/auto";
import { afterEach, describe, expect, it } from "vitest";
import { PersonalOSDatabase } from "@/database/personal-os.database";
import { DexieAppRepository } from "./dexie-app.repository";
import type { AppSnapshot, LearningItem } from "@/types/entities";

const databases: PersonalOSDatabase[] = [];

afterEach(async () => {
  await Promise.all(databases.map(async (database) => {
    database.close();
    await database.delete();
  }));
  databases.length = 0;
});

describe("DexieAppRepository", () => {
  it("seeds a connected workspace and persists a task transition", async () => {
    const database = new PersonalOSDatabase();
    databases.push(database);
    const repository = new DexieAppRepository(database);

    const initial = await repository.load();
    expect(initial.tasks.length).toBeGreaterThan(0);
    expect(initial.relations).toEqual(expect.arrayContaining([
      expect.objectContaining({ sourceEntityType: "task", targetEntityType: "project" }),
    ]));

    const task = initial.tasks.find((item) => item.status === "todo");
    expect(task).toBeDefined();
    if (!task) return;

    await repository.saveTask({ ...task, status: "completed", completedAt: new Date().toISOString() });
    const persisted = await repository.load();
    expect(persisted.tasks.find((item) => item.id === task.id)?.status).toBe("completed");
  });

  it("persists habit updates and deletions", async () => {
    const database = new PersonalOSDatabase();
    databases.push(database);
    const repository = new DexieAppRepository(database);

    const initial = await repository.load();
    const habit = initial.habits[0];
    expect(habit).toBeDefined();

    const updated = { ...habit, streak: 15, title: "Updated Habit Title" };
    await repository.saveHabit(updated);

    let reloaded = await repository.load();
    expect(reloaded.habits.find((h) => h.id === habit.id)?.streak).toBe(15);
    expect(reloaded.habits.find((h) => h.id === habit.id)?.title).toBe("Updated Habit Title");

    await repository.deleteHabit(habit.id);
    reloaded = await repository.load();
    expect(reloaded.habits.find((h) => h.id === habit.id)).toBeUndefined();
  });

  it("handles learning item lifecycle (add, update, delete)", async () => {
    const database = new PersonalOSDatabase();
    databases.push(database);
    const repository = new DexieAppRepository(database);

    await repository.load();

    const newItem: LearningItem = {
      id: "learn-quantum-computing",
      userId: "local-user",
      title: "Quantum Computing Foundations",
      type: "topic",
      status: "learning",
      mastery: 35,
      position: 10,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    await repository.addLearningItem(newItem);
    let snapshot = await repository.load();
    expect(snapshot.learningItems.find((li) => li.id === newItem.id)?.mastery).toBe(35);

    await repository.saveLearningItem({ ...newItem, mastery: 85, status: "mastered" });
    snapshot = await repository.load();
    const saved = snapshot.learningItems.find((li) => li.id === newItem.id);
    expect(saved?.mastery).toBe(85);
    expect(saved?.status).toBe("mastered");

    await repository.deleteLearningItem(newItem.id);
    snapshot = await repository.load();
    expect(snapshot.learningItems.find((li) => li.id === newItem.id)).toBeUndefined();
  });

  it("imports and restores an entire workspace snapshot", async () => {
    const database = new PersonalOSDatabase();
    databases.push(database);
    const repository = new DexieAppRepository(database);

    await repository.load();

    const customSnapshot: AppSnapshot = {
      lifeAreas: [],
      goals: [],
      projects: [],
      tasks: [
        {
          id: "imported-task-1",
          userId: "local-user",
          title: "Restored Task From JSON",
          status: "todo",
          priority: "high",
          position: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        },
      ],
      habits: [],
      notes: [],
      learningItems: [],
      metrics: [],
      workouts: [],
      relations: [],
      activities: [],
    };

    await repository.importSnapshot(customSnapshot);
    const restored = await repository.load();
    expect(restored.tasks.length).toBe(1);
    expect(restored.tasks[0].id).toBe("imported-task-1");
    expect(restored.tasks[0].title).toBe("Restored Task From JSON");
  });
});
