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
  Goal,
  Habit,
  Note,
  Project,
  Task,
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
  scheduleTaskForToday: (id: string) => Promise<void>;
  toggleHabit: (id: string) => Promise<void>;
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
    scheduleTaskForToday,
    toggleHabit,
    resetData,
  }), [
    snapshot, isReady, error, captureOpen, captureType, openCapture, closeCapture, createFromCapture,
    toggleTask, scheduleTaskForToday, toggleHabit, resetData,
  ]);

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function usePersonalOS(): AppContextValue {
  const context = useContext(AppContext);
  if (!context) throw new Error("usePersonalOS must be used inside AppProvider");
  return context;
}
