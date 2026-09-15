import "fake-indexeddb/auto";
import { afterEach, describe, expect, it } from "vitest";
import { PersonalOSDatabase } from "@/database/personal-os.database";
import { DexieAppRepository } from "./dexie-app.repository";

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
});
