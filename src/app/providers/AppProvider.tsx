"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { getAppRepository } from "@/database/repositories/dexie-app.repository";
import type { AppRepository } from "@/database/repositories/app.repository";
import type {
  ActivityEvent,
  AppSnapshot,
  CaptureInput,
  CaptureType,
  EntityRelation,
  Goal,
  Habit,
  LearningItem,
  Note,
  Project,
  Task,
  Workout,
} from "@/types/entities";
import { CURRENT_USER_ID } from "@/types/entities";
import { addDays, toLocalDate } from "@/utils/date";

const emptySnapshot: AppSnapshot = {
  lifeAreas: [],
  goals: [],
  projects: [],
  tasks: [],
  habits: [],
  notes: [],
  learningItems: [],
  metrics: [],
  workouts: [],
  relations: [],
  activities: [],
};

interface AppContextValue {
  snapshot: AppSnapshot;
  isReady: boolean;
  error?: string;
  captureOpen: boolean;
  captureType: CaptureType;
  openCapture: (type?: CaptureType) => void;
  closeCapture: () => void;
  createFromCapture: (input: CaptureInput) => Promise<void>;
  toggleTask: (id: string) => Promise<void>;
  updateTask: (task: Task) => Promise<void>;
  deleteTask: (id: string) => Promise<void>;
  scheduleTaskForToday: (id: string) => Promise<void>;
  toggleHabit: (id: string) => Promise<void>;
  updateHabit: (habit: Habit) => Promise<void>;
  deleteHabit: (id: string) => Promise<void>;
  updateNote: (note: Note) => Promise<void>;
  deleteNote: (id: string) => Promise<void>;
  updateLearningItem: (item: LearningItem) => Promise<void>;
  createLearningItem: (item: Omit<LearningItem, "id" | "userId" | "createdAt" | "updatedAt">) => Promise<void>;
  deleteLearningItem: (id: string) => Promise<void>;
  updateProject: (project: Project) => Promise<void>;
  deleteProject: (id: string) => Promise<void>;
  updateGoal: (goal: Goal) => Promise<void>;
  deleteGoal: (id: string) => Promise<void>;
  logWorkout: (workout: Omit<Workout, "id" | "userId" | "createdAt" | "updatedAt">) => Promise<void>;
  deleteWorkout: (id: string) => Promise<void>;
  addRelation: (relation: Omit<EntityRelation, "id" | "userId" | "createdAt">) => Promise<void>;
  deleteRelation: (id: string) => Promise<void>;
  importSnapshot: (snapshot: AppSnapshot) => Promise<void>;
  resetData: () => Promise<void>;
}

const AppContext = createContext<AppContextValue | null>(null);

function createActivity(
  entityType: ActivityEvent["entityType"],
  entityId: string,
  action: ActivityEvent["action"],
  label: string,
  occurredAt: string,
): ActivityEvent {
  return {
    id: crypto.randomUUID(),
    userId: CURRENT_USER_ID,
    entityType,
    entityId,
    action,
    label,
    occurredAt,
  };
}

export function AppProvider({ children }: { children: ReactNode }) {
  const repository = useRef<AppRepository | null>(null);
  const [snapshot, setSnapshot] = useState<AppSnapshot>(emptySnapshot);
  const [isReady, setIsReady] = useState(false);
  const [error, setError] = useState<string>();
  const [captureOpen, setCaptureOpen] = useState(false);
  const [captureType, setCaptureType] = useState<CaptureType>("task");

  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        repository.current = getAppRepository();
        const data = await repository.current.load();
        if (active) setSnapshot(data);
      } catch (loadError) {
        console.error(loadError);
        if (active) setError("Local storage could not be opened. Your changes may not persist.");
      } finally {
        if (active) setIsReady(true);
      }
    };
    void load();
    return () => {
      active = false;
    };
  }, []);

  const openCapture = useCallback((type: CaptureType = "task") => {
    setCaptureType(type);
    setCaptureOpen(true);
  }, []);

  const closeCapture = useCallback(() => setCaptureOpen(false), []);

  const createFromCapture = useCallback(async (input: CaptureInput) => {
    const store = repository.current;
    if (!store) return;
    const now = new Date();
    const timestamp = now.toISOString();
    const entityBase = {
      id: crypto.randomUUID(),
      userId: CURRENT_USER_ID,
      createdAt: timestamp,
      updatedAt: timestamp,
    };

    try {
      if (input.type === "task") {
        const destination = input.destination ?? "inbox";
        const task: Task = {
          ...entityBase,
          title: input.title,
          status: destination === "inbox" ? "inbox" : "todo",
          priority: input.priority ?? "none",
          dueDate:
            destination === "today"
              ? toLocalDate(now)
              : destination === "tomorrow"
                ? toLocalDate(addDays(now, 1))
                : undefined,
          position: snapshot.tasks.length,
        };
        const activity = createActivity("task", task.id, "created", `Captured “${task.title}”`, timestamp);
        await store.addTask(task, activity);
        setSnapshot((current) => ({
          ...current,
          tasks: [...current.tasks, task],
          activities: [activity, ...current.activities],
        }));
      } else if (input.type === "habit") {
        const habit: Habit = {
          ...entityBase,
          title: input.title,
          frequencyType: "daily",
          targetCount: 1,
          active: true,
          streak: 0,
          completedDates: [],
          color: "#a78bfa",
        };
        const activity = createActivity("habit", habit.id, "created", `Started habit “${habit.title}”`, timestamp);
        await store.addHabit(habit, activity);
        setSnapshot((current) => ({ ...current, habits: [...current.habits, habit], activities: [activity, ...current.activities] }));
      } else if (input.type === "note") {
        const note: Note = {
          ...entityBase,
          title: input.title,
          content: "",
          format: "markdown",
          status: "active",
          tags: [],
          linkedEntityCount: 0,
        };
        const activity = createActivity("note", note.id, "created", `Captured note “${note.title}”`, timestamp);
        await store.addNote(note, activity);
        setSnapshot((current) => ({ ...current, notes: [note, ...current.notes], activities: [activity, ...current.activities] }));
      } else if (input.type === "project") {
        const project: Project = {
          ...entityBase,
          title: input.title,
          status: "planning",
          progress: 0,
          color: "#6d8cff",
        };
        const activity = createActivity("project", project.id, "created", `Started project “${project.title}”`, timestamp);
        await store.addProject(project, activity);
        setSnapshot((current) => ({ ...current, projects: [project, ...current.projects], activities: [activity, ...current.activities] }));
      } else {
        const goal: Goal = {
          ...entityBase,
          title: input.title,
          status: "active",
          progress: 0,
        };
        const activity = createActivity("goal", goal.id, "created", `Set goal “${goal.title}”`, timestamp);
        await store.addGoal(goal, activity);
        setSnapshot((current) => ({ ...current, goals: [goal, ...current.goals], activities: [activity, ...current.activities] }));
      }
      setError(undefined);
      setCaptureOpen(false);
    } catch (writeError) {
      console.error(writeError);
      setError("That change could not be saved. Please try again.");
      throw writeError;
    }
  }, [snapshot.tasks.length]);

  const toggleTask = useCallback(async (id: string) => {
    const store = repository.current;
    const currentTask = snapshot.tasks.find((task) => task.id === id);
    if (!store || !currentTask) return;
    const now = new Date().toISOString();
    const completed = currentTask.status !== "completed";
    const nextTask: Task = {
      ...currentTask,
      status: completed ? "completed" : "todo",
      completedAt: completed ? now : undefined,
      updatedAt: now,
    };
    const activity = createActivity(
      "task",
      id,
      completed ? "completed" : "reopened",
      `${completed ? "Completed" : "Reopened"} “${currentTask.title}”`,
      now,
    );
    setSnapshot((current) => ({
      ...current,
      tasks: current.tasks.map((task) => task.id === id ? nextTask : task),
      activities: [activity, ...current.activities],
    }));
    try {
      await store.saveTask(nextTask);
      await store.addActivity(activity);
    } catch (writeError) {
      console.error(writeError);
      setSnapshot((current) => ({ ...current, tasks: current.tasks.map((task) => task.id === id ? currentTask : task) }));
      setError("Task status could not be saved.");
    }
  }, [snapshot.tasks]);

  const scheduleTaskForToday = useCallback(async (id: string) => {
    const store = repository.current;
    const currentTask = snapshot.tasks.find((task) => task.id === id);
    if (!store || !currentTask) return;
    const now = new Date();
    const timestamp = now.toISOString();
    const nextTask: Task = { ...currentTask, status: "todo", dueDate: toLocalDate(now), updatedAt: timestamp };
    const activity = createActivity("task", id, "scheduled", `Scheduled “${currentTask.title}” for today`, timestamp);
    await store.saveTask(nextTask);
    await store.addActivity(activity);
    setSnapshot((current) => ({
      ...current,
      tasks: current.tasks.map((task) => task.id === id ? nextTask : task),
      activities: [activity, ...current.activities],
    }));
  }, [snapshot.tasks]);

  const toggleHabit = useCallback(async (id: string) => {
    const store = repository.current;
    const currentHabit = snapshot.habits.find((habit) => habit.id === id);
    if (!store || !currentHabit) return;
    const now = new Date();
    const today = toLocalDate(now);
    const wasCompleted = currentHabit.completedDates.includes(today);
    const nextHabit: Habit = {
      ...currentHabit,
      completedDates: wasCompleted
        ? currentHabit.completedDates.filter((date) => date !== today)
        : [...currentHabit.completedDates, today],
      streak: Math.max(0, currentHabit.streak + (wasCompleted ? -1 : 1)),
      updatedAt: now.toISOString(),
    };
    setSnapshot((current) => ({
      ...current,
      habits: current.habits.map((habit) => habit.id === id ? nextHabit : habit),
    }));
    try {
      await store.saveHabit(nextHabit);
    } catch (writeError) {
      console.error(writeError);
      setSnapshot((current) => ({ ...current, habits: current.habits.map((habit) => habit.id === id ? currentHabit : habit) }));
      setError("Habit status could not be saved.");
    }
  }, [snapshot.habits]);

  const updateTask = useCallback(async (task: Task) => {
    const store = repository.current;
    if (!store) return;
    const updated = { ...task, updatedAt: new Date().toISOString() };
    setSnapshot((current) => ({
      ...current,
      tasks: current.tasks.map((t) => (t.id === task.id ? updated : t)),
    }));
    await store.saveTask(updated);
  }, []);

  const deleteTask = useCallback(async (id: string) => {
    const store = repository.current;
    if (!store) return;
    const task = snapshot.tasks.find((t) => t.id === id);
    const timestamp = new Date().toISOString();
    const activity = task ? createActivity("task", id, "completed", `Removed “${task.title}”`, timestamp) : undefined;
    setSnapshot((current) => ({
      ...current,
      tasks: current.tasks.filter((t) => t.id !== id),
      activities: activity ? [activity, ...current.activities] : current.activities,
    }));
    await store.deleteTask(id, activity);
  }, [snapshot.tasks]);

  const updateHabit = useCallback(async (habit: Habit) => {
    const store = repository.current;
    if (!store) return;
    const updated = { ...habit, updatedAt: new Date().toISOString() };
    setSnapshot((current) => ({
      ...current,
      habits: current.habits.map((h) => (h.id === habit.id ? updated : h)),
    }));
    await store.saveHabit(updated);
  }, []);

  const deleteHabit = useCallback(async (id: string) => {
    const store = repository.current;
    if (!store) return;
    setSnapshot((current) => ({
      ...current,
      habits: current.habits.filter((h) => h.id !== id),
    }));
    await store.deleteHabit(id);
  }, []);

  const updateNote = useCallback(async (note: Note) => {
    const store = repository.current;
    if (!store) return;
    const updated = { ...note, updatedAt: new Date().toISOString() };
    setSnapshot((current) => ({
      ...current,
      notes: current.notes.map((n) => (n.id === note.id ? updated : n)),
    }));
    await store.saveNote(updated);
  }, []);

  const deleteNote = useCallback(async (id: string) => {
    const store = repository.current;
    if (!store) return;
    setSnapshot((current) => ({
      ...current,
      notes: current.notes.filter((n) => n.id !== id),
    }));
    await store.deleteNote(id);
  }, []);

  const updateLearningItem = useCallback(async (item: LearningItem) => {
    const store = repository.current;
    if (!store) return;
    const updated = { ...item, updatedAt: new Date().toISOString() };
    setSnapshot((current) => ({
      ...current,
      learningItems: current.learningItems.map((li) => (li.id === item.id ? updated : li)),
    }));
    await store.saveLearningItem(updated);
  }, []);

  const createLearningItem = useCallback(async (input: Omit<LearningItem, "id" | "userId" | "createdAt" | "updatedAt">) => {
    const store = repository.current;
    if (!store) return;
    const timestamp = new Date().toISOString();
    const item: LearningItem = {
      ...input,
      id: crypto.randomUUID(),
      userId: CURRENT_USER_ID,
      createdAt: timestamp,
      updatedAt: timestamp,
    };
    setSnapshot((current) => ({
      ...current,
      learningItems: [...current.learningItems, item],
    }));
    await store.addLearningItem(item);
  }, []);

  const deleteLearningItem = useCallback(async (id: string) => {
    const store = repository.current;
    if (!store) return;
    setSnapshot((current) => ({
      ...current,
      learningItems: current.learningItems.filter((li) => li.id !== id),
    }));
    await store.deleteLearningItem(id);
  }, []);

  const updateProject = useCallback(async (project: Project) => {
    const store = repository.current;
    if (!store) return;
    const updated = { ...project, updatedAt: new Date().toISOString() };
    setSnapshot((current) => ({
      ...current,
      projects: current.projects.map((p) => (p.id === project.id ? updated : p)),
    }));
    await store.saveProject(updated);
  }, []);

  const deleteProject = useCallback(async (id: string) => {
    const store = repository.current;
    if (!store) return;
    setSnapshot((current) => ({
      ...current,
      projects: current.projects.filter((p) => p.id !== id),
      tasks: current.tasks.map((t) => (t.projectId === id ? { ...t, projectId: undefined } : t)),
    }));
    await store.deleteProject(id);
  }, []);

  const updateGoal = useCallback(async (goal: Goal) => {
    const store = repository.current;
    if (!store) return;
    const updated = { ...goal, updatedAt: new Date().toISOString() };
    setSnapshot((current) => ({
      ...current,
      goals: current.goals.map((g) => (g.id === goal.id ? updated : g)),
    }));
    await store.saveGoal(updated);
  }, []);

  const deleteGoal = useCallback(async (id: string) => {
    const store = repository.current;
    if (!store) return;
    setSnapshot((current) => ({
      ...current,
      goals: current.goals.filter((g) => g.id !== id),
      projects: current.projects.map((p) => (p.goalId === id ? { ...p, goalId: undefined } : p)),
    }));
    await store.deleteGoal(id);
  }, []);

  const logWorkout = useCallback(async (input: Omit<Workout, "id" | "userId" | "createdAt" | "updatedAt">) => {
    const store = repository.current;
    if (!store) return;
    const timestamp = new Date().toISOString();
    const workout: Workout = {
      ...input,
      id: crypto.randomUUID(),
      userId: CURRENT_USER_ID,
      createdAt: timestamp,
      updatedAt: timestamp,
    };
    const activity = createActivity("workout", workout.id, "created", `Completed workout “${workout.title}”`, timestamp);
    setSnapshot((current) => ({
      ...current,
      workouts: [workout, ...current.workouts],
      activities: [activity, ...current.activities],
    }));
    await store.addWorkout(workout, activity);
  }, []);

  const deleteWorkout = useCallback(async (id: string) => {
    const store = repository.current;
    if (!store) return;
    setSnapshot((current) => ({
      ...current,
      workouts: current.workouts.filter((w) => w.id !== id),
    }));
    await store.deleteWorkout(id);
  }, []);

  const addRelation = useCallback(async (input: Omit<EntityRelation, "id" | "userId" | "createdAt">) => {
    const store = repository.current;
    if (!store) return;
    const relation: EntityRelation = {
      ...input,
      id: crypto.randomUUID(),
      userId: CURRENT_USER_ID,
      createdAt: new Date().toISOString(),
    };
    setSnapshot((current) => ({
      ...current,
      relations: [...current.relations, relation],
    }));
    await store.addRelation(relation);
  }, []);

  const deleteRelation = useCallback(async (id: string) => {
    const store = repository.current;
    if (!store) return;
    setSnapshot((current) => ({
      ...current,
      relations: current.relations.filter((r) => r.id !== id),
    }));
    await store.deleteRelation(id);
  }, []);

  const importSnapshot = useCallback(async (data: AppSnapshot) => {
    const store = repository.current;
    if (!store) return;
    await store.importSnapshot(data);
    setSnapshot(data);
  }, []);

  const resetData = useCallback(async () => {
    if (!repository.current) return;
    const data = await repository.current.reset();
    setSnapshot(data);
    setError(undefined);
  }, []);

  const value = useMemo<AppContextValue>(() => ({
    snapshot,
    isReady,
    error,
    captureOpen,
    captureType,
    openCapture,
    closeCapture,
    createFromCapture,
    toggleTask,
    updateTask,
    deleteTask,
    scheduleTaskForToday,
    toggleHabit,
    updateHabit,
    deleteHabit,
    updateNote,
    deleteNote,
    updateLearningItem,
    createLearningItem,
    deleteLearningItem,
    updateProject,
    deleteProject,
    updateGoal,
    deleteGoal,
    logWorkout,
    deleteWorkout,
    addRelation,
    deleteRelation,
    importSnapshot,
    resetData,
  }), [
    snapshot, isReady, error, captureOpen, captureType, openCapture, closeCapture, createFromCapture,
    toggleTask, updateTask, deleteTask, scheduleTaskForToday, toggleHabit, updateHabit, deleteHabit,
    updateNote, deleteNote, updateLearningItem, createLearningItem, deleteLearningItem,
    updateProject, deleteProject, updateGoal, deleteGoal,
    logWorkout, deleteWorkout, addRelation, deleteRelation, importSnapshot, resetData,
  ]);

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function usePersonalOS(): AppContextValue {
  const context = useContext(AppContext);
  if (!context) throw new Error("usePersonalOS must be used inside AppProvider");
  return context;
}
