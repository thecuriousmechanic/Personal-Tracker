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
import { PersonalOSDatabase } from "@/database/personal-os.database";
import { createSeedSnapshot } from "@/database/seed";
import type { AppRepository } from "./app.repository";

const SEEDED_KEY = "seeded-version";

export class DexieAppRepository implements AppRepository {
  constructor(private readonly database: PersonalOSDatabase) {}

  private async writeSeed(snapshot: AppSnapshot): Promise<void> {
    const db = this.database;
    await db.transaction("rw", db.tables, async () => {
      await Promise.all(db.tables.map((table) => table.clear()));
      await db.lifeAreas.bulkAdd(snapshot.lifeAreas);
      await db.goals.bulkAdd(snapshot.goals);
      await db.projects.bulkAdd(snapshot.projects);
      await db.tasks.bulkAdd(snapshot.tasks);
      await db.habits.bulkAdd(snapshot.habits);
      await db.notes.bulkAdd(snapshot.notes);
      await db.learningItems.bulkAdd(snapshot.learningItems);
      await db.metrics.bulkAdd(snapshot.metrics);
      await db.workouts.bulkAdd(snapshot.workouts);
      await db.relations.bulkAdd(snapshot.relations);
      await db.activities.bulkAdd(snapshot.activities);
      await db.settings.put({ key: SEEDED_KEY, value: "1" });
    });
  }

  private async ensureSeeded(): Promise<void> {
    const seeded = await this.database.settings.get(SEEDED_KEY);
    if (seeded) return;

    const populated = await Promise.all([
      this.database.tasks.count(),
      this.database.projects.count(),
      this.database.notes.count(),
    ]);
    if (populated.some((count) => count > 0)) {
      await this.database.settings.put({ key: SEEDED_KEY, value: "1" });
      return;
    }
    await this.writeSeed(createSeedSnapshot());
  }

  async load(): Promise<AppSnapshot> {
    await this.ensureSeeded();
    const [lifeAreas, goals, projects, tasks, habits, notes, learningItems, metrics, workouts, relations, activities] =
      await Promise.all([
        this.database.lifeAreas.toArray(),
        this.database.goals.toArray(),
        this.database.projects.toArray(),
        this.database.tasks.toArray(),
        this.database.habits.toArray(),
        this.database.notes.toArray(),
        this.database.learningItems.toArray(),
        this.database.metrics.toArray(),
        this.database.workouts.toArray(),
        this.database.relations.toArray(),
        this.database.activities.orderBy("occurredAt").reverse().toArray(),
      ]);
    return { lifeAreas, goals, projects, tasks, habits, notes, learningItems, metrics, workouts, relations, activities };
  }

  async saveTask(task: Task): Promise<void> {
    await this.database.tasks.put(task);
  }

  async saveHabit(habit: Habit): Promise<void> {
    await this.database.habits.put(habit);
  }

  async addTask(task: Task, activity: ActivityEvent): Promise<void> {
    await this.database.transaction("rw", this.database.tasks, this.database.activities, async () => {
      await this.database.tasks.add(task);
      await this.database.activities.add(activity);
    });
  }

  async addHabit(habit: Habit, activity: ActivityEvent): Promise<void> {
    await this.database.transaction("rw", this.database.habits, this.database.activities, async () => {
      await this.database.habits.add(habit);
      await this.database.activities.add(activity);
    });
  }

  async addNote(note: Note, activity: ActivityEvent): Promise<void> {
    await this.database.transaction("rw", this.database.notes, this.database.activities, async () => {
      await this.database.notes.add(note);
      await this.database.activities.add(activity);
    });
  }

  async addProject(project: Project, activity: ActivityEvent): Promise<void> {
    await this.database.transaction("rw", this.database.projects, this.database.activities, async () => {
      await this.database.projects.add(project);
      await this.database.activities.add(activity);
    });
  }

  async addGoal(goal: Goal, activity: ActivityEvent): Promise<void> {
    await this.database.transaction("rw", this.database.goals, this.database.activities, async () => {
      await this.database.goals.add(goal);
      await this.database.activities.add(activity);
    });
  }

  async deleteTask(id: string, activity?: ActivityEvent): Promise<void> {
    await this.database.transaction("rw", this.database.tasks, this.database.activities, async () => {
      await this.database.tasks.delete(id);
      if (activity) await this.database.activities.add(activity);
    });
  }

  async deleteHabit(id: string): Promise<void> {
    await this.database.habits.delete(id);
  }

  async saveNote(note: Note): Promise<void> {
    await this.database.notes.put(note);
  }

  async deleteNote(id: string): Promise<void> {
    await this.database.notes.delete(id);
  }

  async saveLearningItem(item: LearningItem): Promise<void> {
    await this.database.learningItems.put(item);
  }

  async addLearningItem(item: LearningItem): Promise<void> {
    await this.database.learningItems.add(item);
  }

  async deleteLearningItem(id: string): Promise<void> {
    await this.database.learningItems.delete(id);
  }

  async saveProject(project: Project): Promise<void> {
    await this.database.projects.put(project);
  }

  async deleteProject(id: string): Promise<void> {
    await this.database.projects.delete(id);
  }

  async saveGoal(goal: Goal): Promise<void> {
    await this.database.goals.put(goal);
  }

  async deleteGoal(id: string): Promise<void> {
    await this.database.goals.delete(id);
  }

  async addWorkout(workout: Workout, activity: ActivityEvent): Promise<void> {
    await this.database.transaction("rw", this.database.workouts, this.database.activities, async () => {
      await this.database.workouts.add(workout);
      await this.database.activities.add(activity);
    });
  }

  async deleteWorkout(id: string): Promise<void> {
    await this.database.workouts.delete(id);
  }

  async addRelation(relation: EntityRelation): Promise<void> {
    await this.database.relations.add(relation);
  }

  async deleteRelation(id: string): Promise<void> {
    await this.database.relations.delete(id);
  }

  async addActivity(activity: ActivityEvent): Promise<void> {
    await this.database.activities.add(activity);
  }

  async importSnapshot(snapshot: AppSnapshot): Promise<void> {
    await this.writeSeed(snapshot);
  }

  async reset(): Promise<AppSnapshot> {
    const snapshot = createSeedSnapshot();
    await this.writeSeed(snapshot);
    return snapshot;
  }
}

let repository: DexieAppRepository | undefined;

export function getAppRepository(): AppRepository {
  repository ??= new DexieAppRepository(new PersonalOSDatabase());
  return repository;
}
